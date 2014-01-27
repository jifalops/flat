package com.essentiallocalization.util;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents a InputStream/OutputStream pair as a connection.
 */
public final class StreamConnection implements Startable {
    private final String TAG = StreamConnection.class.getSimpleName();

    public static interface Sendable {
        int length();
        byte[] onSend();
    }

    public static interface Listener {
        /** Called on separate thread. */
        void onDisconnected(String name);
        /** Called on separate thread. */
        void onDataReceived(long time, byte[] data);
    }

    private final String mName;
    private final InputStream mIn;
    private final OutputStream mOut;
    private final int mBufferSize;
    private final Listener mListener;

    private boolean mDisconnected;

    public StreamConnection(String name, InputStream in, OutputStream out, int bufferSize, Listener listener) {
        mName = name;
        mIn = in;
        mOut = out;
        mBufferSize = bufferSize;
        mListener = listener;
    }

    public synchronized boolean isDisconnected() {
        return mDisconnected;
    }

    private synchronized void setDisconnected(boolean disconnected) {
        mDisconnected = disconnected;
        if (mDisconnected) {
            mListener.onDisconnected(mName);
        }
    }

    /** Attempts I/O on current thread! */
    public synchronized void send(final byte[] data) throws IOException {
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

    /** Attempts I/O on current thread! */
    public synchronized void send(Sendable data) throws IOException {
        if (mDisconnected) {
            Log.w(TAG, "Cannot send to disconnected stream (" + mName + ").");
            return;
        }

        if (data.length() > mBufferSize) {
            Log.w(TAG, "Sending data larger than input buffer size (" + mName + ").");
        }

        try {
            mOut.write(data.onSend());
        } catch (IOException e) {
            Log.e(TAG, "Failed sending data to " + mName);
            throw e;
        }
    }

    private final Thread mStreamListener = new Thread() {
        @Override
        public void run() {
            byte[] buffer = new byte[mBufferSize];
            long time;
            while (true) {
                try {
                    mIn.read(buffer);
                    time = System.nanoTime();
                    mListener.onDataReceived(time, buffer.clone());
                } catch (IOException e) {
                    Log.w(TAG, "Disconnected from " + mName);
                    setDisconnected(true);
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
    public void stop() {
        close();
    }

    @Override
    public boolean canRestart() {
        return false;
    }

    private synchronized void close() {
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
    }
}
