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
    private static final String TAG = RemoteConnection.class.getSimpleName();

    public static final int MSG_SENT = 1;
    public static final int MSG_RECEIVED = 2;
    public static final int MSG_CONFIRMED = 3;
    public static final int MSG_DISCONNECTED = 4;

    private final Handler mHandler;

    private final byte mFrom, mTo;
    private final InputStream mIn;
    private final OutputStream mOut;
    private final List<DataPacket> mSentPackets;
    private final List<DataPacket> mReceivedPackets;
    private final List<Message> mSentMessages;
    private final List<Message> mReceivedMessages;
    private final SparseArray<String[]> mPartialReceivedMessages;
    private boolean mDisconnected;

    public RemoteConnection(byte from, byte to, InputStream in, OutputStream out, Handler handler) {
        mFrom = from;
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

    public synchronized int sendTestMessage() throws IOException {
        try {
            return sendMessage(Packet.TYPE_TEST, new Message("Test msg"));
        } catch (Message.MessageTooLongException ignored) {}
        return 0;
    }

    public synchronized int sendMessage(Message message) throws IOException {
        return sendMessage(Packet.TYPE_MSG, message);
    }

    private synchronized int sendMessage(byte type, Message message) throws  IOException {
        if (mDisconnected || message == null) return 0;
        int msgIndex = mSentMessages.size();
        byte msgParts = (byte) (message.parts.size() - 1);
        byte msgAttempt = 1;
        DataPacket packet;
        for (int i = 0; i < msgParts; i++) {
            packet = new DataPacket(type, mSentPackets.size(), msgIndex, msgAttempt, (byte) i, msgParts, message.parts.get(i));
            sendPacket(packet);
            mSentPackets.add(packet);
        }
        mSentMessages.add(message);
        mHandler.obtainMessage(MSG_SENT, message.toString()).sendToTarget();
        return msgParts;
    }

    private synchronized void sendPacket(Packet packet) throws IOException {
        if (mDisconnected) return;
        try {
            mOut.write(packet.getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Error sending packet.");
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
                    Log.e(TAG, "Disconnected.", e);
                    synchronized (RemoteConnection.this) {
                        mDisconnected = true;
                    }
                    mHandler.obtainMessage(MSG_DISCONNECTED).sendToTarget();
                    break;
                }
            }
        }

        private void processPacket(byte[] data) {
            switch (data[Packet.INDEX_OF_TYPE]) {
                case Packet.TYPE_ACK:
                    AckPacket ap = new AckPacket(data);
                    mSentPackets.get(ap.packetIndex).confirmed = System.nanoTime();
                    mSentPackets.get(ap.packetIndex).received = ap.received;
                    if (ap.msgPart == ap.msgParts) {
                        mHandler.obtainMessage(MSG_CONFIRMED, ap.msgIndex).sendToTarget();
                    }
                    break;
                case Packet.TYPE_TEST:
                case Packet.TYPE_MSG:
                    DataPacket dp = new DataPacket(data);
                    dp.received = System.nanoTime();
                    mReceivedPackets.add(dp);
                    try {
                        sendPacket(new AckPacket(dp));
                    } catch (IOException e) {
                        Log.e(TAG, "Error sending Ack packet.");
                    }

                    String part = new String(dp.payload);
                    String[] parts = mPartialReceivedMessages.get(dp.msgIndex, new String[dp.msgParts + 1]);
                    parts[dp.msgPart] = part;
                    mPartialReceivedMessages.put(dp.msgIndex, parts);

                    if (dp.msgPart == dp.msgParts) {
                        String message = TextUtils.join("", parts);
                        try {
                            mReceivedMessages.add(new Message(message));
                        } catch (Message.MessageTooLongException ignored) {}
                        mPartialReceivedMessages.delete(dp.msgIndex);
                        mHandler.obtainMessage(MSG_RECEIVED, message).sendToTarget();
                    }
                    break;
            }
        }
    };
}
