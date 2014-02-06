package com.essentiallocalization.util.io;

import android.os.Environment;
import android.util.Log;

import com.essentiallocalization.util.lifecycle.Cancelable;
import com.essentiallocalization.util.lifecycle.Finishable;

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
public class SnoopFilter extends Thread implements Cancelable, Finishable {
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
//    private static final String KEY_TIMESTAMP = "timestamp";
//    private static final String KEY_MESSAGE = "message";

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


    /** All methods are called on the SnoopFilter thread. */
    public static interface SnoopFilterListener extends FinishListener {
        /** Called on the SnoopFilter thread. */
        void onMessageFound(long ts, byte[] msg);
    }

    public static final String DEFUALT_SNOOP_NAME = "btsnoop_hci.log";
    public static final File DEFAULT_SNOOP_FILE = new File(Environment.getExternalStorageDirectory(), DEFUALT_SNOOP_NAME);

    private int mTask;
    private long mTimestamp;
    private int mPayloadSize;

    private final File mSnoopFile;
//    private final File mOutFile;
//    private final BufferedOutputStream mOutStream;
    private final BufferedInputStream mInStream;
//    private final Handler mHandler;
    private final SnoopFilterListener mListener;
    private final String mFilter;

    private volatile int mPacketsRead, mMessagesFound;
    private boolean mCanceled, mFinished;

    public SnoopFilter(File snoopFile, /*File outFile,*/ String msgPrefix, SnoopFilterListener listener) throws IOException {
        mSnoopFile = snoopFile;
//        mOutFile = outFile;
        mFilter = msgPrefix;
        mListener = listener;

        mInStream = new BufferedInputStream(new FileInputStream(snoopFile));
//        mOutStream = new BufferedOutputStream(new FileOutputStream(outFile));

//        mHandler = new Handler() {
//            @Override
//            public void handleMessage(Message msg) {
//                switch (msg.what) {
//                    case MSG_MESSAGE_FOUND:
//                        Bundle data = msg.getData();
//                        if (mListener != null) mListener.onMessageFound(data.getLong(KEY_TIMESTAMP), data.getString(KEY_MESSAGE));
//                        break;
//                }
//            }
//        };

        mTask = TASK_READ_FILE_HEADER;
    }

    public int getPacketsRead() {
        return mPacketsRead;
    }

    public int getMessagesFound() {
        return mMessagesFound;
    }

    private boolean resetStream() {
        try { mInStream.reset(); }
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

    @Override
    public synchronized void cancel() {
        mCanceled = true;
        close();
    }

    @Override
    public synchronized boolean isCanceled() {
        return mCanceled;
    }

    private synchronized void finish() {
        mFinished = true;
        Log.d(TAG, "The snoop filter has finished.");
        close();
        mListener.onFinished();
    }

    @Override
    public synchronized boolean isFinished() {
        return mFinished;
    }


    private void close() {
        try {
            mInStream.close();
//            mOutStream.flush();
//            mOutStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed closing input or output files.");
        }

//        mHandler.removeCallbacksAndMessages(null);
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
        while (!isCanceled() && !mFinished) {
            if (readFileHeader()) {
                while (!isCanceled() && !mFinished) {
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
                finish();
                return false;
            }
            mInStream.mark(FILE_HEADER_SIZE);
            try {
                bytesRead = mInStream.read(fileHeader);
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
                finish();
                return false;
            }
            mInStream.mark(PACKET_HEADER_SIZE);

            try {
                bytesRead = mInStream.read(packetHeader);
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
        byte[] payload = new byte[mPayloadSize], message;
        int bytesRead, msgStart;

        int attempts = 0;
        while (!isCanceled()) {
            if (++attempts >= MAX_ATTEMPTS) {
                finish();
                return false;
            }
            mInStream.mark(mPayloadSize);

            try {
                bytesRead = mInStream.read(payload);
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
                    msgStart = new String(payload, "ISO88591").indexOf(mFilter);
                    if (msgStart > -1) {
                        // Uncomment to exclude the filter string from the result
                        //msgStart += mFilter.length();
                        ++mMessagesFound;

                        message = Arrays.copyOfRange(payload, msgStart, mPayloadSize - 1); // -1 for the RFCOMM fcs byte (checksum)

                        mListener.onMessageFound(mTimestamp, message);

//                        try {
//                            mOutStream.write(message);
//                        } catch (IOException e) {
//                            Log.e(TAG, "Failed to write message to outfile.");
//                        }
//
//
//                        Bundle data = new Bundle();
//                        data.putLong(KEY_TIMESTAMP, mTimestamp);
//                        data.putString(KEY_MESSAGE, new String(message));
//
//                        Message msg = mHandler.obtainMessage(MSG_MESSAGE_FOUND);
//                        msg.setData(data);
//                        msg.sendToTarget();
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
        try {
            close();
        } catch (Throwable t) {
            Log.e(TAG, "Exception in finalize().");
        }
        finally {
            super.finalize();
        }
    }
}
