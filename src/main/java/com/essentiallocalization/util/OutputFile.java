package com.essentiallocalization.util;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A simple wrapper for FileWriter that opens in append mode
 * and supplies a truncate method. It also attempts to flush
 * the FileWriter whenever it is appended or closed.
 */
public final class OutputFile {
    private static final String TAG = OutputFile.class.getSimpleName();

    private final File mFile;
    private FileWriter mWriter;
    private boolean mClosed;

    public OutputFile(File outFile) throws IOException {
        mFile = outFile;
        mWriter = new FileWriter(mFile, true);
    }

    public synchronized void append(CharSequence csq) throws IOException {
        mWriter.append(csq);
        mWriter.flush();
    }


    public synchronized void close() throws IOException {
        mWriter.flush();
        mWriter.close();
        mClosed = true;
    }

    public synchronized void truncate() throws IOException {
        mWriter.close();
        mWriter = new FileWriter(mFile);
        mWriter.close();
        mWriter = new FileWriter(mFile, true);
    }

    public File getFile() {
        return mFile;
    }

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