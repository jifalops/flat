package com.flat.util.io;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents a InputStream/OutputStream pair as a connection.
 */
public class StreamConnection extends BasicConnection {
    private static final String TAG = StreamConnection.class.getSimpleName();

    public static interface Sendable {
        int length();
        byte[] onSend();
    }

    public static interface StreamListener {
        /** Called on separate thread (sendAndEventLooper). */
        void onDataReceived(long time, byte[] data);
    }

    private final String mName;
    private final InputStream mIn;
    private final OutputStream mOut;
    private final int mBufferSize;
    private StreamListener mListener;
    protected final Handler mSendAndEventHandler;

    private boolean mCanceled;

    public StreamConnection(String name, InputStream in, OutputStream out, int bufferSize, Looper sendAndEventLooper) {
        mName = name;
        mIn = in;
        mOut = out;
        mBufferSize = bufferSize;
        mSendAndEventHandler = new Handler(sendAndEventLooper);
    }

    public void setStreamConnectionListener(StreamListener listener) {
        mListener = listener;
    }

    public final String getName() {
        return mName;
    }

    public void send(final byte[] data) throws IOException {
        send(new Sendable() {
            @Override
            public int length() {
                return data.length;
            }

            @Override
            public byte[] onSend() {
                return data;
            }
        });
    }

    public void send(final Sendable data) throws IOException {
        if (isDisconnected()) {
            Log.w(TAG, "Cannot send to disconnected stream (" + mName + ").");
            return;
        }

        if (data.length() > mBufferSize) {
            Log.w(TAG, "Sending data larger than input buffer size (" + mName + ").");
        }

        mSendAndEventHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mOut.write(data.onSend());
                } catch (IOException e) {
                    Log.e(TAG, "Failed sending data to " + mName);
                }
            }
        });
    }

    private final Thread mStreamListener = new Thread() {
        @Override
        public void run() {
            final byte[] buffer = new byte[mBufferSize];
            while (!isCanceled()) {
                try {
                    mIn.read(buffer);
                    final long time = System.nanoTime();
                    mSendAndEventHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mListener != null) {
                                mListener.onDataReceived(time, buffer.clone());
                            }
                        }
                    });

                } catch (IOException e) {
                    Log.w(TAG, "Disconnected from " + mName);
                    setState(Connection.STATE_DISCONNECTED);
                    break;
                }
            }
        }
    };


    @Override
    public void start() {
        mStreamListener.start();
    }

    @Override
    public synchronized  void cancel() {
        mCanceled = true;
        close();
    }

    @Override
    public synchronized boolean isCanceled() {
        return mCanceled;
    }

    private void close() {
        try {
            mIn.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to close input stream to " + mName + "!");
        }

        try {
            mOut.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to close output stream to " + mName + "!");
        }

        mSendAndEventHandler.removeCallbacksAndMessages(null);
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
