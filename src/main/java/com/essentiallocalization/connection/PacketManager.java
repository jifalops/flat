package com.essentiallocalization.connection;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.essentiallocalization.util.Startable;
import com.essentiallocalization.util.StreamConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages sending Messages back and forth, internally breaking them into Packets.
 */
public final class PacketManager implements StreamConnection.Listener, SnoopPacketReader.Listener, Startable {
    private final String TAG;

    /** arg1 = to, arg2 = pktIndex, obj = ActiveConnection */
    public static final int MSG_SENT_PACKET = 1;
    /** arg1 = to, arg2 = msgIndex, obj = ActiveConnection */
    public static final int MSG_SENT_MSG = 2;
    /** arg1 = to, arg2 = pktIndex, obj = ActiveConnection */
    public static final int MSG_RECEIVED_PACKET = 3;
    /** arg1 = to, arg2 = msgIndex, obj = ActiveConnection */
    public static final int MSG_RECEIVED_MSG = 4;
    /** arg1 = to, arg2 = pktIndex, obj = ActiveConnection */
    public static final int MSG_CONFIRMED_PACKET = 5;
    /** arg1 = to, arg2 = msgIndex, obj = ActiveConnection */
    public static final int MSG_CONFIRMED_MSG = 6;
    /** arg1 = to, arg2 = -1, obj = ActiveConnection */
    public static final int MSG_DISCONNECTED = 7;

    private final Handler mHandler;
    private final byte mSrc, mDest;
    private final InputStream mIn;
    private final OutputStream mOut;
    private final List<DataPacket> mSentPackets;
    private final List<DataPacket> mReceivedPackets;
    private final List<Message> mSentMessages;
    private final List<Message> mReceivedMessages;
    private final SparseArray<String[]> mPartialReceivedMessages;
    private boolean mDisconnected;

    public PacketManager(byte src, byte dest, InputStream in, OutputStream out, Handler handler) {
        TAG = "ActiveConnection to " + dest;
        mSrc = src;
        mDest = dest;
        mIn = in;
        mOut = out;
        mSentPackets = new ArrayList<DataPacket>();
        mReceivedPackets = new ArrayList<DataPacket>();
        mSentMessages = new ArrayList<Message>();
        mReceivedMessages = new ArrayList<Message>();
        mPartialReceivedMessages = new SparseArray<String[]>();
        mHandler = handler;
        mStreamListener.start();
    }

    public List<DataPacket> getSentPackets() {
        return mSentPackets;
    }

    public List<DataPacket> getReceivedPackets() {
        return mReceivedPackets;
    }

    public List<Message> getSentMessages() {
        return mSentMessages;
    }

    public List<Message> getReceivedMessages() {
        return mReceivedMessages;
    }


    public synchronized boolean isDisconnected() {
        return mDisconnected;
    }

    private synchronized void setDisconnected(boolean disconnected) {
        mDisconnected = disconnected;
        if (mDisconnected) {
            mHandler.obtainMessage(MSG_DISCONNECTED, mDest, -1, this).sendToTarget();
        }
    }

    public synchronized void close() {
        try {
            mIn.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to close input stream!");
        }

        try {
            mOut.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to close output stream!");
        }
        mHandler.removeCallbacksAndMessages(null);
    }

    public synchronized int sendMessage(Message message) throws  IOException {
        if (mDisconnected || message == null) return 0;
        int msgIndex = mSentMessages.size();
        byte msgParts = (byte) message.size();
        byte attempt = 1;
        DataPacket packet;
        for (byte msgPart = 0; msgPart < msgParts; msgPart++) {
            packet = new DataPacket();
            packet.src = mSrc;
            packet.dest = mDest;
            packet.pktIndex = mSentPackets.size();
            packet.msgIndex = msgIndex;
            packet.msgPart = msgPart;
            packet.msgParts = msgParts;
            packet.attempt = attempt;
            packet.payload = message.get(msgPart);

            sendPacket(packet);
            mSentPackets.add(packet);
            mHandler.obtainMessage(MSG_SENT_PACKET, mDest, mSentPackets.size() - 1, this).sendToTarget();
        }
        mSentMessages.add(message);
        mHandler.obtainMessage(MSG_SENT_MSG, mDest, mSentMessages.size() - 1, this).sendToTarget();
        return msgParts;
    }

    synchronized void sendPacket(Packet packet) throws IOException {
        if (mDisconnected) return;
        try {
            mOut.write(packet.getBytes(true));
        } catch (IOException e) {
            Log.e(TAG, "Error sending packet " + packet.pktIndex);
            throw e;
        }
    }


    private final Thread mStreamListener = new Thread() {
        @Override
        public void run() {
            byte[] buffer = new byte[Packet.BUFFER_SIZE];
            long time;
            while (true) {
                try {
                    mIn.read(buffer);
                    time = System.nanoTime();
                    processPacket(time, buffer);
                } catch (IOException e) {
                    Log.w(TAG, "Disconnected");
                    setDisconnected(true);
                    break;
                }
            }
        }

        private void processPacket(long time, byte[] data) {
            switch (Packet.getType(data)) {

                // Received a data packet from the sender (we are destination)
                case Packet.TYPE_DATA:

                    // Store transaction
                    DataPacket dp = new DataPacket(data);
                    mReceivedPackets.add(dp);
//                    Log.v(TAG, "Received packet " + dp.pktIndex + " (" + dp.msgPart + " of " + dp.msgParts + ")");

                    sendAck(time, dp);

                    //
                    // Handle messages broken into multiple packets
                    //
                    String part = new String(dp.payload);
                    // xTODO, this is ambiguous because the source is unknown (who's msgIndex?)
                    // Actually this represents a single connection
                    String[] parts = mPartialReceivedMessages.get(dp.msgIndex, new String[dp.msgParts]);
                    parts[dp.msgPart] = part;
                    mPartialReceivedMessages.put(dp.msgIndex, parts);

                    mHandler.obtainMessage(MSG_RECEIVED_PACKET, mDest, mReceivedPackets.size() - 1, PacketManager.this).sendToTarget();

                    if (dp.msgPart == dp.msgParts - 1) {
                        String message = TextUtils.join("", parts);
                        try {
                            mReceivedMessages.add(new Message(message));
                        } catch (Message.MessageTooLongException e) {
                            Log.e(TAG, "Message too long for Packet buffer.");
                        }
                        mPartialReceivedMessages.delete(dp.msgIndex);
                        mHandler.obtainMessage(MSG_RECEIVED_MSG, mDest, mReceivedMessages.size() - 1, PacketManager.this).sendToTarget();
                    }
                    break;

                // Received an ack packet back from the destination (we are the original sender)
                case Packet.TYPE_ACK:
                    AckPacket ap = new AckPacket(data);
//                    Log.v(TAG, "Received ack for packet " + ap.pktIndex + " from " + ap.dest);

                    updateDataPacket(time, ap);

                    mHandler.obtainMessage(MSG_CONFIRMED_PACKET, mDest, ap.pktIndex, PacketManager.this).sendToTarget();

                    if (ap.msgPart == ap.msgParts - 1) {
                        mHandler.obtainMessage(MSG_CONFIRMED_MSG, mDest, ap.msgIndex, PacketManager.this).sendToTarget();
                    }
                    break;

                // Received a packet containing the HCI time that the corresponding ack packet was sent (we are original sender).
                // Unlike the java time, the HCI time sent can only be retrieved AFTER the packet is sent.
                case Packet.TYPE_ACK_TIME:
                    AckTimePacket atp = new AckTimePacket(data);
//                    Log.v(TAG, "Received ack time for packet " + ap.pktIndex + " from " + ap.dest);

                    updateDataPacket(atp);
                    break;
            }
        }

        private void sendAck(long javaTime, DataPacket dp) {
            try {
                AckPacket ap = new AckPacket(dp);
                ap.javaDestReceived = javaTime;
                ap.hciDestReceived = getHciTimestamp(dp.type, dp.src, dp.pktIndex);
                sendPacket(ap);
            } catch (IOException e) {
                Log.e(TAG, "Error sending Ack for packet " + dp.pktIndex + " from " + dp.src);
            }
        }

        private void updateDataPacket(long javaTime, AckPacket ap) {
            DataPacket dp = mSentPackets.get(ap.pktIndex);
            dp.hciDestReceived = ap.hciDestReceived;
            dp.javaDestReceived = ap.javaDestReceived;
            dp.javaDestSent = ap.javaDestSent;
            dp.javaSrcReceived = javaTime;
        }

        private void updateDataPacket(AckTimePacket atp) {
            DataPacket dp = mSentPackets.get(atp.pktIndex);
            dp.hciDestSent = atp.hciDestSent;
        }

        private long getHciTimestamp(byte type, byte src, int pktIndex) {
            long time = 0;
            // TODO
            return time;
        }
    };


    //
    // SnoopPacketReader.Listener methods
    //

    @Override
    public void onSendAck(AckPacket ap) {

    }

    @Override
    public void onSendAckTime(AckTimePacket atp) {

    }

    @Override
    public void onPacketCompleted(DataPacket dp) {

    }

    //
    // StreamConnection.Listener methods
    //

    @Override
    public void onDisconnected(String name) {

    }

    @Override
    public void onDataReceived(long time, byte[] data) {

    }

    //
    // Startable methods
    //

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean canRestart() {
        return false;
    }
}
