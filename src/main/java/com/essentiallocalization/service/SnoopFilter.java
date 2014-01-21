package com.essentiallocalization.service;

import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Reads a snoop file in a separate thread for payload data that begins with a specific string.
 * When found, a callback method is called on the thread where this instance was created,
 * and is passed the timestamp and message. The message passed will begin with the character
 * AFTER the prefix string.
 */
public class SnoopFilter extends Thread {
    private static final String TAG = SnoopFilter.class.getSimpleName();
                                            // Big endian
//    static final class FileHeader {
//        String id;                          // 8 bytes
//        int version;                        // 4 bytes
//        int dataLinkType;                   // 4 bytes
//    }
//
//    static final class PacketHeader {
//        int origLen;                        // 4 bytes
//        int incLen;                         // 4 bytes
//        int recLen;                         // 4 bytes
//        int drops;                          // 4 bytes
//        int sec;                            // 4 bytes
//        int usec;                           // 4 bytes
//        byte[] data;                        // incLen bytes
//    }

    // Handler messages
    private static final int MSG_MESSAGE_FOUND = 1;

    // Bundle keys (for handler messages)
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_MESSAGE = "message";

    private static final int FILE_HEADER_SIZE = 16;
    private static final int PACKET_HEADER_SIZE = 24;
    private static final int MAX_PAYLOAD_SIZE = 1024;

    private static final int PAYLOAD_SIZE_OFFSET = 4;
    private static final int PAYLOAD_SIZE_SIZE = 4;
    private static final int TIME_OFFSET = 16;
    private static final int TIME_SIZE = 8;

    private static final int MAX_ATTEMPTS = Integer.MAX_VALUE;

    private static final int TASK_READ_FILE_HEADER = 1;
    private static final int TASK_READ_PACKET_HEADER = 2;
    private static final int TASK_READ_PACKET_PAYLOAD = 3;

    public static interface Listener {
        void onMessageFound(long ts, String msg);
    }

    private int mTask;
    private long mTimestamp;
    private int mPayloadSize;

    private final File mSnoopFile;
    private  FileObserver mObserver;
    private final BufferedInputStream mStream;
    private final Handler mHandler;
    private final Listener mListener;
    private final String mFilter;

    private volatile int mPacketsRead, mMessagesFound;
    private boolean mCanceled, mFailed;

    public SnoopFilter(File mFile, String msgPrefix, Listener listener) throws IOException {
        mSnoopFile = mFile;
        mFilter = msgPrefix;
        mListener = listener;

        mStream = new BufferedInputStream(new FileInputStream(mFile));



        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_MESSAGE_FOUND:
                        Bundle data = msg.getData();
                        if (mListener != null) mListener.onMessageFound(data.getLong(KEY_TIMESTAMP), data.getString(KEY_MESSAGE));
                        break;
                }
            }
        };

        mTask = TASK_READ_FILE_HEADER;
    }

    public int getPacketsRead() {
        return mPacketsRead;
    }

    public int getMessagesFound() {
        return mMessagesFound;
    }

    private boolean resetStream() {
        try { mStream.reset(); }
        catch (IOException e) {
            Log.e(TAG, "Failed to reset stream.");
            return false;
        }
        waitForStream();
        return true;
    }

    private void waitForStream() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted");
        }
    }

    public synchronized void cancel() {
        if (mObserver != null) {
            mObserver.stopWatching();
            mObserver = null;
        }

        mCanceled = true;
    }
    public synchronized boolean isCanceled() {
        return mCanceled;
    }

    private synchronized void fail() {
        mFailed = true;
        Log.e(TAG, "The snoop filter has failed!");
    }
    public synchronized boolean isFailed() {
        return mFailed;
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    @Override
    public void run() {
//        mObserver = new FileObserver(mSnoopFile.toString(), FileObserver.MODIFY) {
//            @Override
//            public void onEvent(int event, String path) {
//                if (isCanceled() || isFailed()) return;
//                switch (mTask) {
//                case TASK_READ_FILE_HEADER:
//                    readFileHeader();
//                    break;
//                case TASK_READ_PACKET_HEADER:
//                    readPacketHeader();
//                    break;
//                case TASK_READ_PACKET_PAYLOAD:
//                    readPacketPayload();
//                }
//            }
//        };
//        mObserver.startWatching();
        while (!isCanceled() && !isFailed()) {
            if (readFileHeader()) {
                while (!isCanceled() && !isFailed()) {
                    readPacketHeader();
                    readPacketPayload();
                }
            }
        }
    }

    private boolean readFileHeader() {
        byte[] fileHeader = new byte[FILE_HEADER_SIZE];
        int bytesRead;

        int attempts = 0;
        while (!isCanceled()) {
            if (++attempts >= MAX_ATTEMPTS) {
                fail();
                return false;
            }
            mStream.mark(FILE_HEADER_SIZE);
            try {
                bytesRead = mStream.read(fileHeader);
                if (bytesRead != FILE_HEADER_SIZE) {
                    Log.e(TAG, "File header was not the correct length.");
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not read file header.");
                bytesRead = -1;
            }
            if (bytesRead == -1) {
                resetStream();
//                return false;
            } else {
                mTask = TASK_READ_PACKET_HEADER;
                return true;
//                readPacketHeader();
            }
        }
        return false;
    }

    private boolean readPacketHeader() {
        byte[] packetHeader = new byte[PACKET_HEADER_SIZE];
        int bytesRead;

        int attempts = 0;
        while (!isCanceled()) {
            if (++attempts >= MAX_ATTEMPTS) {
                fail();
                return false;
            }
            mStream.mark(PACKET_HEADER_SIZE);

            try {
                bytesRead = mStream.read(packetHeader);
                if (bytesRead != PACKET_HEADER_SIZE) {
                    Log.e(TAG, "Packet header was not the correct length.");
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not read file header.");
                bytesRead = -1;
            }
            if (bytesRead == -1) {
                resetStream();
//                return false;
            } else {
                mPayloadSize = ByteBuffer.wrap(packetHeader, PAYLOAD_SIZE_OFFSET, PAYLOAD_SIZE_SIZE).getInt();
                if (mPayloadSize < 1) {
                    Log.e(TAG, "Invalid payload size.");
                }

                mTimestamp = ByteBuffer.wrap(packetHeader, TIME_OFFSET, TIME_SIZE).getLong();

                mTask = TASK_READ_PACKET_PAYLOAD;
                return true;
//                readPacketPayload();
            }
        }
        return false;
    }

    private boolean readPacketPayload() {
        byte[] payload = new byte[MAX_PAYLOAD_SIZE], message;
        int bytesRead, msgStart;

        int attempts = 0;
        while (!isCanceled()) {
            if (++attempts >= MAX_ATTEMPTS) {
                fail();
                return false;
            }
            mStream.mark(mPayloadSize);

            try {
                bytesRead = mStream.read(payload, 0, mPayloadSize);
                if (bytesRead != mPayloadSize) {
                    Log.e(TAG, "Payload was not the correct length.");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading payload from stream.");
                bytesRead = -1;
            }
            if (bytesRead == -1) {
                resetStream();
//                return false;
            } else {
                ++mPacketsRead;

                // Parse the message.
                try {
                    msgStart = new String(payload, 0, mPayloadSize, "ISO88591").indexOf(mFilter);
                    if (msgStart > -1) {
                        msgStart += mFilter.length();
                        ++mMessagesFound;

                        message = Arrays.copyOfRange(payload, msgStart, mPayloadSize - 1); // -1 for the RFCOMM fcs byte (checksum)

                        Bundle data = new Bundle();
                        data.putLong(KEY_TIMESTAMP, mTimestamp);
                        data.putString(KEY_MESSAGE, new String(message));

                        Message msg = mHandler.obtainMessage(MSG_MESSAGE_FOUND);
                        msg.setData(data);
                        msg.sendToTarget();
                    }
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "Failed string encoding.");
                }

                mTask = TASK_READ_PACKET_HEADER;
                return true;
//                readPacketHeader();
            }
        }
        return false;
    }

    @Override
    protected void finalize() throws Throwable {
        try{
            cancel();
        } finally {
            super.finalize();
        }
    }
}
