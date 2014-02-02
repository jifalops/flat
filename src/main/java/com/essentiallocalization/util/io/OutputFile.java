package com.essentiallocalization.util.io;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A simple wrapper for FileWriter that opens in append mode
 * and supplies a truncate method. It can also flush the
 * FileWriter whenever it is appended (write-through).
 */
public final class OutputFile {
    private static final String TAG = OutputFile.class.getSimpleName();

    private final File mFile;
    private FileWriter mWriter;
    private boolean mClosed;
    private boolean mWriteThrough;

    public OutputFile(File outFile) throws IOException {
        mFile = outFile;
        mWriter = new FileWriter(mFile, true);
    }

    public synchronized void append(CharSequence csq) throws IOException {
        mWriter.append(csq);
        if (mWriteThrough) {
            mWriter.flush();
        }
    }


    public synchronized void close() throws IOException {
        try { mWriter.flush(); }
        finally {
            mClosed = true;
            mWriter.close();
        }
    }

    public synchronized void truncate() throws IOException {
        mWriter.close();
        mWriter = new FileWriter(mFile);
        mWriter.close();
        mWriter = new FileWriter(mFile, true);
    }

    public synchronized void setWriteThrough(boolean enabled) {
        mWriteThrough = enabled;
    }

    public synchronized boolean isWriteThrough() {
        return mWriteThrough;
    }

    public synchronized File getFile() {
        return mFile;
    }
    public synchronized FileWriter getWriter() { return mWriter; }

    @Override
    protected synchronized void finalize() throws Throwable {
        if (!mClosed) {
            close();
            Log.e(TAG, "The file was never closed!");
        }
        super.finalize();
    }

    @Override
    public String toString() {
        return mFile.getAbsolutePath();
    }
}