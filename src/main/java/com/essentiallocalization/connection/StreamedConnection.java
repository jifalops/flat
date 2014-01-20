package com.essentiallocalization.connection;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a InputStream/OutputStream pair as a connection
 * which can send Messages back and forth, internally breaking them into Packets.
 */
public final class StreamedConnection {
    private final String TAG;

    /** arg1 = to, arg2 = packetIndex, obj = ActiveConnection */
    public static final int MSG_SENT_PACKET = 1;
    /** arg1 = to, arg2 = msgIndex, obj = ActiveConnection */
    public static final int MSG_SENT_MSG = 2;
    /** arg1 = to, arg2 = packetIndex, obj = ActiveConnection */
    public static final int MSG_RECEIVED_PACKET = 3;
    /** arg1 = to, arg2 = msgIndex, obj = ActiveConnection */
    public static final int MSG_RECEIVED_MSG = 4;
    /** arg1 = to, arg2 = packetIndex, obj = ActiveConnection */
    public static final int MSG_CONFIRMED_PACKET = 5;
    /** arg1 = to, arg2 = msgIndex, obj = ActiveConnection */
    public static final int MSG_CONFIRMED_MSG = 6;
    /** arg1 = to, arg2 = -1, obj = ActiveConnection */
    public static final int MSG_DISCONNECTED = 7;

    private final Handler mHandler;
    private final byte mTo;
    private final InputStream mIn;
    private final OutputStream mOut;
    private final List<DataPacket> mSentPackets;
    private final List<DataPacket> mReceivedPackets;
    private final List<Message> mSentMessages;
    private final List<Message> mReceivedMessages;
    private final SparseArray<String[]> mPartialReceivedMessages;
    private boolean mDisconnected;

    public StreamedConnection(byte to, InputStream in, OutputStream out, Handler handler) {
        TAG = "ActiveConnection to " + to;
        mTo = to;
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
            mHandler.obtainMessage(MSG_DISCONNECTED, mTo, -1, this).sendToTarget();
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

    public synchronized int sendMessage(Message message) throws IOException {
        return sendMessage(Packet.TYPE_MSG, message);
    }

    private synchronized int sendMessage(byte type, Message message) throws  IOException {
        if (mDisconnected || message == null) return 0;
        int msgIndex = mSentMessages.size();
        byte msgParts = (byte) message.size();
        byte msgAttempt = 1;
        DataPacket packet;
        for (byte msgPart = 0; msgPart < msgParts; msgPart++) {
            packet = new DataPacket(type, mSentPackets.size(), msgIndex, msgAttempt, msgPart, msgParts, message.get(msgPart));
            sendPacket(packet);
            mSentPackets.add(packet);
            mHandler.obtainMessage(MSG_SENT_PACKET, mTo, mSentPackets.size() - 1, this).sendToTarget();
        }
        mSentMessages.add(message);
        mHandler.obtainMessage(MSG_SENT_MSG, mTo, mSentMessages.size() - 1, this).sendToTarget();
        return msgParts;
    }

    synchronized void sendPacket(Packet packet) throws IOException {
        if (mDisconnected) return;
        try {
            mOut.write(packet.onSend());
        } catch (IOException e) {
            Log.e(TAG, "Error sending packet " + packet.packetIndex);
            throw e;
        }
    }


    private final Thread mStreamListener = new Thread() {
        @Override
        public void run() {
            byte[] buffer = new byte[Packet.BUFFER_SIZE];
            while (true) {
                try {
                    mIn.read(buffer);
                    long time = System.nanoTime();
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
                case Packet.TYPE_ACK:
                    AckPacket ap = new AckPacket(data);
//                    Log.v(TAG, "Received ack for packet " + ap.packetIndex);
                    DataPacket dataPacket = mSentPackets.get(ap.packetIndex);
                    dataPacket.confirmed = time;
                    dataPacket.received = ap.received;
                    dataPacket.resent = ap.resent;

                    mHandler.obtainMessage(MSG_CONFIRMED_PACKET, mTo, dataPacket.packetIndex, StreamedConnection.this).sendToTarget();

                    if (ap.msgPart == ap.msgParts - 1) {
                        mHandler.obtainMessage(MSG_CONFIRMED_MSG, mTo, ap.msgIndex, StreamedConnection.this).sendToTarget();
                    }
                    break;
                case Packet.TYPE_TEST: // Fall through
                case Packet.TYPE_MSG:
                    DataPacket dp = new DataPacket(data);
//                    Log.v(TAG, "Received packet " + dp.packetIndex + " (" + dp.msgPart + " of " + dp.msgParts + ")");
                    dp.received = time;
                    mReceivedPackets.add(dp);
                    try {
                        sendPacket(new AckPacket(dp));
                    } catch (IOException e) {
                        Log.e(TAG, "Error sending Ack packet for packetIndex " + dp.packetIndex);
                    }

                    String part = new String(dp.payload);
                    String[] parts = mPartialReceivedMessages.get(dp.msgIndex, new String[dp.msgParts]);
                    parts[dp.msgPart] = part;
                    mPartialReceivedMessages.put(dp.msgIndex, parts);

                    mHandler.obtainMessage(MSG_RECEIVED_PACKET, mTo, mReceivedPackets.size() - 1, StreamedConnection.this).sendToTarget();

                    if (dp.msgPart == dp.msgParts - 1) {
                        String message = TextUtils.join("", parts);
                        try {
                            mReceivedMessages.add(new Message(message));
                        } catch (Message.MessageTooLongException e) {
                            Log.e(TAG, "Message too long for Packet buffer.");
                        }
                        mPartialReceivedMessages.delete(dp.msgIndex);
                        mHandler.obtainMessage(MSG_RECEIVED_MSG, mTo, mReceivedMessages.size() - 1, StreamedConnection.this).sendToTarget();
                    }
                    break;
            }
        }
    };
}
