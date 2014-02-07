package com.essentiallocalization.connection;

import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.essentiallocalization.util.io.StreamConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages sending Messages back and forth, internally breaking them into Packets.
 */
public class PacketConnection extends StreamConnection implements PacketList, StreamConnection.StreamListener {
    private static final String TAG = PacketConnection.class.getSimpleName();

//    /** arg1 = to, arg2 = pktIndex, obj = ActiveConnection */
//    public static final int MSG_SENT_PACKET = 1;
//    /** arg1 = to, arg2 = msgIndex, obj = ActiveConnection */
//    public static final int MSG_SENT_MSG = 2;
//    /** arg1 = to, arg2 = pktIndex, obj = ActiveConnection */
//    public static final int MSG_RECEIVED_PACKET = 3;
//    /** arg1 = to, arg2 = msgIndex, obj = ActiveConnection */
//    public static final int MSG_RECEIVED_MSG = 4;
//    /** arg1 = to, arg2 = pktIndex, obj = ActiveConnection */
//    public static final int MSG_CONFIRMED_PACKET = 5;
//    /** arg1 = to, arg2 = msgIndex, obj = ActiveConnection */
//    public static final int MSG_CONFIRMED_MSG = 6;
//    /** arg1 = to, arg2 = -1, obj = ActiveConnection */
//    public static final int MSG_DISCONNECTED = 7;

    /**
     * Events are called based with their related java timestamps completed.
     * HCI timestamps may or may not be completed.
     */
    public static interface PacketListener {
        /** Called on separate thread (sendAndEventLooper). */
        void onDataPacketReceived(PacketConnection pc, DataPacket dp);
        /** Called on separate thread (sendAndEventLooper). */
        void onJavaTimingComplete(PacketConnection pc, DataPacket dp);
    }


    private final byte mSrc, mDest;

    private final List<DataPacket> mPackets;
    private final List<Message> mMessages;
    private final SparseArray<String[]> mPartialMessages;

    private PacketListener mListener;

    private int mSentPacketIndex;
    private int mSentMessageIndex;

    public PacketConnection(byte src, byte dest, InputStream in, OutputStream out, Looper sendAndEventLooper) {
        super(src + "to" + dest, in, out, Packet.BUFFER_SIZE, sendAndEventLooper);
        mSrc = src;
        mDest = dest;

        mPackets = new ArrayList<DataPacket>();
        mMessages = new ArrayList<Message>();
        mPartialMessages = new SparseArray<String[]>();

        setStreamConnectionListener(this);
    }

    public void setPacketConnectionListener(PacketListener listener) {
        mListener = listener;
    }

    public final byte getSrc() {
        return mSrc;
    }

    public final byte getDest() {
        return mDest;
    }

    public final List<DataPacket> getPackets() {
        return mPackets;
    }

    public final List<Message> getMessages() {
        return mMessages;
    }

    
    public int send(String msg) throws IOException, Message.MessageTooLongException {
        if (msg == null || msg.length() == 0) {
            Log.w(TAG, "Cannot send null or empty message");
        }

        Message message = new Message(msg);
        byte msgParts = (byte) message.size();
        byte attempt = 1;
        DataPacket p;
        for (byte msgPart = 0; msgPart < msgParts; ++msgPart) {
            p = new DataPacket();
            p.src = mSrc;
            p.dest = mDest;
            p.pktIndex = mSentPacketIndex;
            p.msgIndex = mSentMessageIndex;
            p.msgPart = msgPart;
            p.msgParts = msgParts;
            p.attempt = attempt;
            p.payload = message.get(msgPart);

            send(p);

            ++mSentPacketIndex;

            mPackets.add(p);
//            mSendAndEventHandler.obtainMessage(MSG_SENT_PACKET, mDest, mSentPackets.size() - 1, this).sendToTarget();
        }

        ++mSentMessageIndex;

        mMessages.add(message);
//        mSendAndEventHandler.obtainMessage(MSG_SENT_MSG, mDest, mSentMessages.size() - 1, this).sendToTarget();
        return msgParts;
    }

    public void send(final Packet packet) throws IOException {
        send(new Sendable() {
            @Override
            public int length() {
                return packet.size();
            }

            @Override
            public byte[] onSend() {
                return packet.getBytes(true);
            }
        });
    }


    //
    // StreamConnection.Listener methods
    //

    @Override
    public void onDataReceived(long javaTime, byte[] data) {
        int size;
        try {
            size = Packet.getSize(data);
            if (data.length < size) {
                Log.e(TAG, "Unexpected data size: " + data.length + " (expecting " + size + ").");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        data = Arrays.copyOfRange(data, 0, size);
        DataPacket dp;
        AckPacket ap;
        AckTimePacket atp;
        switch (Packet.getType(data)) {
            case Packet.TYPE_DATA:
                // Data packet received
                dp = new DataPacket(data);
                if (findPacket(dp.src, dp.dest, dp.pktIndex, dp.javaSrcSent) == null) {
                    if (dp.dest == mSrc) {
                        dp.javaDestReceived = javaTime;
                        mPackets.add(dp);
                    } else {
                        Log.e(TAG, "Processing data packet not for this device");
                    }
                } else {
                    Log.e(TAG, "Received duplicate data packet.");
                }
//                Log.v(TAG, "Received packet " + dp.pktIndex + " (" + dp.msgPart + " of " + dp.msgParts + ")");

                if (mListener != null) {
                    // java time for ack is ready
                    mListener.onDataPacketReceived(this, dp);
                }

                //
                // Handle messages broken into multiple packets
                //
                String part = new String(dp.payload);
                // xTODO, this is ambiguous because the source is unknown (who's msgIndex?)
                // Actually this represents a single connection
                String[] parts = mPartialMessages.get(dp.msgIndex, new String[dp.msgParts]);
                parts[dp.msgPart] = part;
                mPartialMessages.put(dp.msgIndex, parts);

//                mSendAndEventHandler.obtainMessage(MSG_RECEIVED_PACKET, mDest, mPackets.size() - 1, PacketConnection.this).sendToTarget();

                if (dp.msgPart == dp.msgParts - 1) {
                    String message = TextUtils.join("", parts);
                    try {
                        mMessages.add(new Message(message));
                    } catch (Message.MessageTooLongException e) {
                        Log.e(TAG, "Message too long for Packet buffer.");
                    }
                    mPartialMessages.delete(dp.msgIndex);
//                    mSendAndEventHandler.obtainMessage(MSG_RECEIVED_MSG, mDest, mMessages.size() - 1, PacketConnection.this).sendToTarget();
                }
                break;
            case Packet.TYPE_ACK:
                // Received an ack packet back from the destination (we are the original sender)
                ap = new AckPacket(data);
                dp = findPacket(ap.src, ap.dest, ap.pktIndex, ap.javaSrcSent);
                if (dp == null) {
                    Log.e(TAG, "Received ack without data packet.");
                } else if (dp.src == mSrc) {
                    dp.hciDestReceived = ap.hciDestReceived;
                    dp.javaDestReceived = ap.javaDestReceived;
                    dp.javaDestSent = ap.javaDestSent;
                    dp.javaSrcReceived = javaTime;
                } else {
                    Log.e(TAG, "Processing ack packet not for this device");
                }
//                 Log.v(TAG, "Received ack for packet " + ap.pktIndex + " from " + ap.dest);

//                mSendAndEventHandler.obtainMessage(MSG_CONFIRMED_PACKET, mDest, ap.pktIndex, PacketConnection.this).sendToTarget();

                if (ap.msgPart == ap.msgParts - 1) {
//                    mSendAndEventHandler.obtainMessage(MSG_CONFIRMED_MSG, mDest, ap.msgIndex, PacketConnection.this).sendToTarget();
                }
                break;

            case Packet.TYPE_ACK_TIME:
                // Received a packet containing the HCI time that the corresponding ack packet was sent (we are original sender).
                // Unlike the java time, the HCI time sent can only be retrieved AFTER the packet is sent.
                atp = new AckTimePacket(data);
                dp = findPacket(atp.src, atp.dest, atp.pktIndex, atp.javaSrcSent);
                if (dp == null) {
                    Log.e(TAG, "Received ack time without data packet.");
                } else if (dp.src == mSrc) {
                    dp.hciDestSent = atp.hciDestSent;
                    if (mListener != null
                            && dp.javaSrcSent != 0 && dp.javaDestReceived != 0
                            && dp.javaDestSent != 0 && dp.javaSrcReceived != 0) {
                        mListener.onJavaTimingComplete(this, dp);
                    }
                } else {
                    Log.e(TAG, "Processing ack time packet not for this device");
                }
//                Log.v(TAG, "Received ack time for packet " + ap.pktIndex + " from " + ap.dest);
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
