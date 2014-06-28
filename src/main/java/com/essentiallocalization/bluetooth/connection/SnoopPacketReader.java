package com.essentiallocalization.bluetooth.connection;

import android.util.Log;

import com.essentiallocalization.util.io.SnoopFilter;
import com.essentiallocalization.util.lifecycle.Finishable;
import com.essentiallocalization.util.lifecycle.Startable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jake on 1/26/14.
 */
public class SnoopPacketReader implements PacketList, SnoopFilter.SnoopFilterListener, Startable {
    private static final String TAG = SnoopPacketReader.class.getSimpleName();

    @Override
    public void onFinished() {
        mListener.onFinished();
    }

    @Override
    public void start() {
        mFilter.start();
    }

    @Override
    public void cancel() {
        mFilter.cancel();
    }

    @Override
    public boolean isCanceled() {
        return mFilter.isCanceled();
    }

    /**
     * Events are called based with their related HCI timestamps completed.
     * Java timestamps may or may not be completed.
     * All methods are called on a separate thread (the underlying SnoopFilter thread).
     */
    public static interface SnoopPacketListener extends Finishable.FinishListener {
        /** called on separate (SnoopFilter) thread. */
        void onDataReceived(DataPacket dp);
        /** called on separate (SnoopFilter) thread. */
        void onSendAckTime(AckTimePacket atp);
        /** called on separate (SnoopFilter) thread. */
        void onHciTimingComplete(DataPacket dp);
    }


    private final List<DataPacket> mPackets;
    private final byte mSrc;
    private final SnoopPacketListener mListener;
    private final SnoopFilter mFilter;

    public SnoopPacketReader(File snoopFile, byte src, SnoopPacketListener listener) throws IOException {
        mSrc = src;
        mListener = listener;
        mPackets = new ArrayList<DataPacket>();
        mFilter = new SnoopFilter(snoopFile, Packet.PREPEND, this);
    }

    public File getSnoopFile() {
        return mFilter.getSnoopFile();
    }

    @Override
    public void onMessageFound(long hciTime, byte[] packet, int origSize) {
        int size;
        try {
            size = Packet.getSize(packet);
            if (packet.length < size) {
                Log.e(TAG, "Unexpected data size: " + packet.length + " (expecting " + size + ").");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        DataPacket dp;
        AckPacket ap;
        AckTimePacket atp;
        switch (Packet.getType(packet)) {
            case Packet.TYPE_DATA:
                dp = new DataPacket(packet);
                if (findPacket(dp.src, dp.dest, dp.pktIndex, dp.javaSrcSent) == null) {
                    if (dp.src == mSrc) {
                        // Data packet sent
                        dp.hciSrcSent = hciTime;

                        // add to list
                        mPackets.add(dp);
                    } else if (dp.dest == mSrc) {
                        // Data packet received
                        dp.hciDestReceived = hciTime;

                        // add to list
                        mPackets.add(dp);

                        // hci time for ack is ready
                        mListener.onDataReceived(dp);
                    } else {
                        Log.e(TAG, "Processing data packet not for this device");
                    }
                } else {
                    Log.e(TAG, "Received duplicate data packet.");
                }
                break;
            case Packet.TYPE_ACK:
                ap = new AckPacket(packet);
                dp = findPacket(ap.src, ap.dest, ap.pktIndex, ap.javaSrcSent);
                if (dp == null) {
                    Log.e(TAG, "Received ack without data packet.");
                } else {
                    // Note: src and dest stay the same as on the original DataPacket.
                    if (dp.src == mSrc) {
                        // Ack was received, update DataPacket from it.
                        dp.hciDestReceived = ap.hciDestReceived;
                        dp.javaDestReceived = ap.javaDestReceived;
                        dp.javaDestSent = ap.javaDestSent;
                    } else if (dp.dest == mSrc) {
                        // Ack was sent. Update our received data packet info
                        dp.hciDestSent = hciTime;

                        // hci ack time is ready
                        atp = new AckTimePacket(dp);
                        atp.hciDestSent = hciTime;
                        mListener.onSendAckTime(atp);
                    } else {
                        Log.e(TAG, "Processing ack packet not for this device");
                    }
                }
                break;
            case Packet.TYPE_ACK_TIME:
                atp = new AckTimePacket(packet);
                dp = findPacket(atp.src, atp.dest, atp.pktIndex, atp.javaSrcSent);
                if (dp == null) {
                    Log.e(TAG, "Received ack time without data packet.");
                } else {
                    // Note: src and dest stay the same as on the original DataPacket.
                    if (dp.src == mSrc) {
                        // Ack time was received, update DataPacket info.
                        dp.hciDestSent = atp.hciDestSent;
                        dp.hciSrcReceived = hciTime;

                        // Packet has complete hci timestamps. Do check before notifying.
                        if (dp.hciSrcSent != 0 && dp.hciDestReceived != 0
                                && dp.hciDestSent != 0 && dp.hciSrcReceived != 0) {
                            if (!mPackets.remove(dp)) {
                                Log.e(TAG, "Failed to remove completed packet");
                            }
                            mListener.onHciTimingComplete(dp);
                        } else {
                            Log.e(TAG, "Packet was expected to be HCI complete.");
                        }
                    } else if (dp.dest == mSrc) {
                        // Ack time packet was sent. No action needed.
                    } else {
                        Log.e(TAG, "Processing ack time packet not for this device");
                    }
                }
                break;
        }
    }

    @Override
    public DataPacket findPacket(byte src, byte dest, int pktIndex, long javaSrcSent) {
        for (DataPacket dp : mPackets) {
            if (dp.src == src && dp.dest == dest && dp.pktIndex == pktIndex && dp.javaSrcSent == javaSrcSent) {
                return dp;
            }
        }
        return null;
    }
}
