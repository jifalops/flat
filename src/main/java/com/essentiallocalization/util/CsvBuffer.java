package com.essentiallocalization.util;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import au.com.bytecode.opencsv.bean.CsvToBean;

/**
 * CsvBuffer: A thread-safe store for csv-style data (List<String[]>).
 * Can be used as a simple buffer or with a backing file.
 * Depends on the OpenCSV library.
 */
public class CsvBuffer implements Closeable {
    private static final String TAG = CsvBuffer.class.getSimpleName();

    protected final List<String[]> mData = new ArrayList<String[]>();
    private CSVWriter mWriter;
    private volatile boolean mIsClosed;

    /**
     * If used with the same File as addAllFromFile(), this should be called afterwards.
     */
    public void setWriteThroughFile(final File file, boolean append) throws IOException {
        if (mWriter != null) {
            close();
        }
        mWriter = new CSVWriter(new FileWriter(file, append));
        mIsClosed = false;
    }

    public void add(final String[] line) {
        String[] clone = line.clone();
        synchronized (mData) {
            mData.add(clone);
        }
        if (mWriter != null) {
            synchronized (mWriter) {
                if (mIsClosed == false) {
                    mWriter.writeNext(clone); // TODO blocking??
                }
            }
        }
    }

    public void addAll(final List<String[]> lines) {
        List<String[]> data;
        data = new ArrayList<String[]>(lines.size());
        Collections.copy(data, lines);
        synchronized (mData) {
            mData.addAll(data);
        }
        if (mWriter != null) {
            synchronized (mWriter) {
                if (mIsClosed == false) {
                    mWriter.writeAll(data); // TODO blocking??
                }
            }
        }
    }

    /**
     * If used with the same File as setWriteThroughFile(), this should be called first.
     */
    public void addAllFromFile(final File file, final Runnable onContentsLoaded) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                CSVReader reader = null;
                try {
                    reader = new CSVReader(new FileReader(file));
                    addAll(reader.readAll());
                } catch (IOException e) {
                    Log.e(TAG, "Error reading CSV file.");
                } finally {
                    tryClose(reader);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void lines) {
                if (onContentsLoaded != null) {
                    onContentsLoaded.run();
                }
            }
        }.execute();
    }

    /**
     * Flush data in the write-through file.
     */
    public boolean flush() throws IOException {
        if (mWriter != null) {
            synchronized (mWriter) {
                if (mIsClosed == false) {
                    mWriter.flush(); // TODO blocking ??
                }
            }
            return true;
        }
        return false;
    }

    public String[] get(int index) {
        synchronized (mData) {
            return mData.get(index).clone();
        }
    }

    public List<String[]> getAll() {
        List<String[]> data;
        synchronized (mData) {
            data = new ArrayList<String[]>(mData.size());
            Collections.copy(data, mData);
        }
        return data;
    }

    public void writeAll(final File file, final boolean append, final Runnable onFileWritten) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                CSVWriter writer = null;
                try {
                    writer = new CSVWriter(new FileWriter(file, append));
                    synchronized (mData) {
                        writer.writeAll(mData);
                    }
                    writer.flush();
                } catch (IOException e) {
                    Log.e(TAG, "Error writing CSV file.");
                } finally {
                    tryClose(writer);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void lines) {
                if (onFileWritten != null) {
                    onFileWritten.run();
                }
            }
        }.execute();
    }

    private static void tryClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                Log.e(TAG, "Error trying to close Closeable.");
            }
        }
    }

    /**
     * Closes the write-through file if set.
     */
    @Override
    public void close() throws IOException {
        flush();
        mIsClosed = true;
        if (mWriter != null) {
            synchronized (mWriter) {
                mWriter.close();
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        synchronized (mData) {
            for (String[] s : mData) {
                sb.append(TextUtils.join(", ", s)).append("\n");
            }
        }
        return sb.toString();
    }

    public JSONArray toJson() {
        JSONArray rows = new JSONArray();
        JSONArray fields;
//        try {
            synchronized (mData) {
                for (String[] s : mData) {
                    fields = new JSONArray(Arrays.asList(s));
                    rows.put(fields);
                }
            }
//        } catch (JSONException e) {
//            Log.e(TAG, "Failed converting to JSON.");
//        }
        return rows;
    }
}
