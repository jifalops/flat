package com.essentiallocalization.connection.bluetooth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Reads a snoop file in a separate thread for payload data that matches a given pattern.
 * When found, a callback method is called on the thread where this instance was created,
 * and is passed the timestamp and message. The message passed will begin with the character
 * AFTER the matched pattern.
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

    private static final int MAX_ATTEMPTS = 5;

    public static interface Listener {
        void onMessageFound(long ts, String msg);
    }

//    private final File mFile;
    private final BufferedInputStream mStream;
    private final Handler mHandler;
    private final Listener mListener;
    private final Pattern mPattern;

    private volatile int mPacketsRead, mMessagesFound;
    private boolean mCanceled, mFailed;

    public SnoopFilter(File file, Pattern pattern, Listener listener) throws IOException {
//        mFile = file;
        mPattern = pattern;
        mListener = listener;

        mStream = new BufferedInputStream(new FileInputStream(file));

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_MESSAGE_FOUND:
                        Bundle data = msg.getData();
                        mListener.onMessageFound(data.getLong(KEY_TIMESTAMP), data.getString(KEY_MESSAGE));
                        break;
                }
            }
        };
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
        byte[] fileHeader, packetHeader, payload, message;
        int payloadSize, msgStart, bytesRead, attempts;

        fileHeader = new byte[FILE_HEADER_SIZE];
        packetHeader = new byte[PACKET_HEADER_SIZE];
        payload = new byte[MAX_PAYLOAD_SIZE];

        // Read the file header.
        attempts = 0;
        while (true) {
            if (++attempts >= MAX_ATTEMPTS) {
                fail();
                return;
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
            } else {
                break;
            }
        }

        // Read packets.
        while (!isCanceled()) {

            // Read packet header.
            attempts = 0;
            while (true) {
                if (++attempts >= MAX_ATTEMPTS) {
                    fail();
                    return;
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
                } else {
                    break;
                }
            }

            payloadSize = ByteBuffer.wrap(packetHeader, PAYLOAD_SIZE_OFFSET, PAYLOAD_SIZE_SIZE).getInt();
            if (payloadSize < 1) {
                Log.e(TAG, "Invalid payload size.");
            }

            // Read payload data.
            attempts = 0;
            while (true) {
                if (++attempts >= MAX_ATTEMPTS) {
                    fail();
                    return;
                }
                mStream.mark(payloadSize);

                try {
                    bytesRead = mStream.read(payload, 0, payloadSize);
                    if (bytesRead != payloadSize) {
                        Log.e(TAG, "Payload was not the correct length.");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading payload from stream.");
                    bytesRead = -1;
                }
                if (bytesRead == -1) {
                    resetStream();
                } else {
                    break;
                }
            }

            ++mPacketsRead;

            // Parse the message.
            try {
                msgStart = mPattern.matcher(ByteBuffer.wrap(payload, 0, payloadSize)
                        .asCharBuffer()).end();

                ++mMessagesFound;

                message = Arrays.copyOfRange(payload, msgStart, payloadSize - 1); // -1 for the RFCOMM fcs byte (checksum)
                long timestamp = ByteBuffer.wrap(packetHeader, TIME_OFFSET, TIME_SIZE).getLong();

                Bundle data = new Bundle();
                data.putLong(KEY_TIMESTAMP, timestamp);
                data.putString(KEY_MESSAGE, new String(message));

                Message msg = mHandler.obtainMessage(MSG_MESSAGE_FOUND);
                msg.setData(data);
                msg.sendToTarget();

            } catch (IllegalStateException ignored) {} // no match found.
        }
    }


}
