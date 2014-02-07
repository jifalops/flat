package com.essentiallocalization;

import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;

import com.essentiallocalization.connection.DataPacket;
import com.essentiallocalization.connection.bluetooth.BluetoothConnectionManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * Created by Jake on 2/6/14.
 */
public class TimingLog implements PacketLog {
    private static final String TAG = TimingLog.class.getSimpleName();

    public static interface TimeLogListener {
        void onInitialized();
        void onReadAll(List<String[]> lines);
    }

    private final File mFile;
    private final SparseArray<Integer> mConnectionCounts;
    private CSVWriter mWriter;
    private CSVReader mReader;
    private final TimeLogListener mListener;

    public TimingLog(File file, boolean append, TimeLogListener listener) throws IOException {
        mFile = file;
        open(append);
        mListener = listener;
        mConnectionCounts = new SparseArray<Integer>(BluetoothConnectionManager.MAX_CONNECTIONS);
        findConnectionCounts();
    }

    public void log(DataPacket dp, int javaDist, int hciDist) {
        mWriter.writeNext(new Record(dp, mConnectionCounts.get(dp.dest), javaDist, hciDist).toStringArray());
    }

    // todo: might be done on ui thread
    public void readAll() throws IOException {
        new AsyncTask<Void, Void, List<String[]>>() {
            @Override
            protected List<String[]> doInBackground(Void... params) {
                List<String[]> lines = null;
                try {
                    lines = mReader.readAll();
                } catch (IOException e) {
                    Log.e(TAG, "Error reading log file.");
                }
                return lines;
            }

            @Override
            protected void onPostExecute(List<String[]> lines) {
                super.onPostExecute(lines);
                if (mListener != null) mListener.onReadAll(lines);
            }
        }.execute();
    }

    public void clear() throws IOException {
        close();
        open(false);
        close();
        open(true);
    }

    public void incConnectionCount(byte dest) {
        int count = mConnectionCounts.get(dest) + 1;
        mConnectionCounts.put(dest, count);
    }

    private void open(boolean append) throws IOException {
        mWriter = new CSVWriter(new FileWriter(mFile, append));
        mReader = new CSVReader(new FileReader(mFile));
    }

    @Override
    public void close() throws IOException {
        mWriter.close();
        mReader.close();
        mWriter = null;
        mReader = null;
    }

    private void findConnectionCounts() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    List<String[]> mLines = mReader.readAll();
                    Record r;
                    for (String[] fields : mLines) {
                        r = new Record(fields);
                        mConnectionCounts.put(Integer.valueOf(r.dest), Integer.valueOf(r.connCount));
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading log file.");
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (mListener != null) mListener.onInitialized();
            }
        }.execute();
    }
    
    private static class Record {
        String src, dest, connCount, pktIndex, 
                javaSrcSent, javaDestReceived, javaDestSent, javaSrcReceived,
                hciSrcSent, hciDestReceived, hciDestSent, hciSrcReceived,
                javaDist, hciDist;
        Record(String[] r) {
            src              = r[0];
            dest             = r[1];
            connCount        = r[2];
            pktIndex         = r[3];
            javaSrcSent      = r[4];
            javaDestReceived = r[5];
            javaDestSent     = r[6];
            javaSrcReceived  = r[7];
            hciSrcSent       = r[8];
            hciDestReceived  = r[9];
            hciDestSent      = r[10];
            hciSrcReceived   = r[11];
            javaDist         = r[12];
            hciDist          = r[13];
        }

        Record(DataPacket dp, int connCount, int javaDist, int hciDist) {
            this.src = String.valueOf(dp.src);
            this.dest = String.valueOf(dp.dest);
            this.connCount = String.valueOf(connCount);
            this.pktIndex = String.valueOf(dp.pktIndex);
            this.javaSrcSent = String.valueOf(dp.javaSrcSent);
            this.javaDestReceived = String.valueOf(dp.javaDestReceived);
            this.javaDestSent = String.valueOf(dp.javaDestSent);
            this.javaSrcReceived = String.valueOf(dp.javaSrcReceived);
            this.hciSrcSent = String.valueOf(dp.hciSrcSent);
            this.hciDestReceived = String.valueOf(dp.hciDestReceived);
            this.hciDestSent = String.valueOf(dp.hciDestSent);
            this.hciSrcReceived = String.valueOf(dp.hciSrcReceived);
            this.javaDist = String.valueOf(javaDist);
            this.hciDist = String.valueOf(hciDist);
        }

        public String[] toStringArray() {
            return new String[] {
                src,
                dest,
                connCount,
                pktIndex,
                javaSrcSent,
                javaDestReceived,
                javaDestSent,
                javaSrcReceived,
                hciSrcSent,
                hciDestReceived,
                hciDestSent,
                hciSrcReceived,
                javaDist,
                hciDist
            };
        }
    }
}
