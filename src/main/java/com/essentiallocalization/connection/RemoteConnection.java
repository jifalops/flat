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
 * Created by Jake on 9/14/13.
 */
public class RemoteConnection {
    private final String TAG;

    public static final int MSG_SENT = 1;
    public static final int MSG_RECEIVED = 2;
    public static final int MSG_CONFIRMED = 3;
    public static final int MSG_DISCONNECTED = 4;

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

    public RemoteConnection(byte to, InputStream in, OutputStream out, Handler handler) {
        TAG = "RemoteConnection to " + to;
        mTo = to;
        mIn = in;
        mOut = out;
        mSentPackets = new ArrayList<DataPacket>();
        mReceivedPackets = new ArrayList<DataPacket>();
        mSentMessages = new ArrayList<Message>();
        mReceivedMessages = new ArrayList<Message>();
        mPartialReceivedMessages = new SparseArray<String[]>();
        mHandler = handler;
        mListenerThread.start();
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
        }
        mSentMessages.add(message);
        mHandler.obtainMessage(MSG_SENT, mTo, mSentMessages.size() - 1, message.toString()).sendToTarget();
        return msgParts;
    }

    private synchronized void sendPacket(Packet packet) throws IOException {
        if (mDisconnected) return;
        try {
            mOut.write(packet.getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Error sending packet " + packet.packetIndex);
            throw e;
        }
    }


    private final Thread mListenerThread = new Thread() {
        @Override
        public void run() {
            byte[] buffer = new byte[Packet.BUFFER_SIZE];
            while (true) {
                try {
                    mIn.read(buffer);
                    processPacket(buffer);
                } catch (IOException e) {
                    Log.w(TAG, "Disconnected");
                    synchronized (RemoteConnection.this) {
                        mDisconnected = true;
                    }
                    mHandler.obtainMessage(MSG_DISCONNECTED, mTo).sendToTarget();
                    break;
                }
            }
        }

        private void processPacket(byte[] data) {
            switch (Packet.getType(data)) {
                case Packet.TYPE_ACK:
                    AckPacket ap = new AckPacket(data);
                    Log.v(TAG, "Received ack for packet " + ap.packetIndex);
                    DataPacket dataPacket = mSentPackets.get(ap.packetIndex);
                    dataPacket.confirmed = System.nanoTime();
                    dataPacket.received = ap.received;
                    if (ap.msgPart == ap.msgParts - 1) {
                        mHandler.obtainMessage(MSG_CONFIRMED, mTo, ap.msgIndex, dataPacket).sendToTarget();
                    }
                    break;
                case Packet.TYPE_TEST:
                case Packet.TYPE_MSG:
                    DataPacket dp = new DataPacket(data);
                    Log.v(TAG, "Received packet " + dp.packetIndex + " (" + dp.msgPart + " of " + dp.msgParts + ")");
                    dp.received = System.nanoTime();
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

                    if (dp.msgPart == dp.msgParts - 1) {
                        String message = TextUtils.join("", parts);
                        try {
                            mReceivedMessages.add(new Message(message));
                        } catch (Message.MessageTooLongException ignored) {}
                        mPartialReceivedMessages.delete(dp.msgIndex);
                        mHandler.obtainMessage(MSG_RECEIVED, mTo, mReceivedMessages.size() - 1, message).sendToTarget();
                    }
                    break;
            }
        }
    };


}
