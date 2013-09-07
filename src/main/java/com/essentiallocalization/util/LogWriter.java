package com.essentiallocalization.util;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by Jake on 8/28/13.
 */
public final class LogWriter {
    private static final String TAG = LogWriter.class.getSimpleName();

    private final File mFile;
    private FileWriter mWriter;
    private boolean mClosed;

    public LogWriter(File logFile) {
        mFile = logFile;
        mWriter = null;
        try {
            mWriter = new FileWriter(mFile, true);
        } catch (IOException e) {
            Log.e(TAG, "Error initializing LogWriter.", e);
        }
    }

    public void append(CharSequence csq) {
        try {
            mWriter.append(csq);
            mWriter.flush();
        } catch (IOException e) {
            Log.e(TAG, "Unable to write to log file.", e);
        }
    }


    public void close() {
        mClosed = true;
        try {
            mWriter.flush();
            mWriter.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to cancel log file.", e);
        }
    }

    public void truncate() {
        try {
            mWriter.close();
            mWriter = new FileWriter(mFile);
            mWriter.close();
            mWriter = new FileWriter(mFile, true);
        } catch (IOException e) {
            Log.e(TAG, "Error clearing Log.", e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (!mClosed) {
            close();
            Log.wtf(TAG, "The file was never closed!");
        }
        super.finalize();
    }
}
