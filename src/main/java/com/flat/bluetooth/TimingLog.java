package com.flat.bluetooth;

import android.util.SparseArray;

import com.flat.bluetooth.connection.DataPacket;
import com.flat.util.CsvBuffer;
import com.flat.util.Util;

import java.io.File;
import java.io.IOException;

/**
 * Created by Jake on 2/6/14.
 */
public class TimingLog extends CsvBuffer {
    private static final String TAG = TimingLog.class.getSimpleName();

    private final SparseArray<Integer> mConnectionCounts;

    public TimingLog(File file, boolean append, Runnable onInitialized) throws IOException {
        mConnectionCounts = new SparseArray<Integer>(BluetoothConnectionManager.MAX_CONNECTIONS);
        if (append) {
            // TODO call on separate thread
            addAllFromFile(file);
            findConnectionCounts();
        }
        setWriteThroughFile(file, append);
    }

    public void add(DataPacket dp, double javaDist, double hciDist) throws IOException {
       add(new Record(dp, mConnectionCounts.get(dp.dest), javaDist, hciDist).toStringArray());
    }

    public void incConnectionCount(byte dest) {
        Integer count = mConnectionCounts.get(dest);
        if (count == null || count < 1) {
            mConnectionCounts.put(dest, 1);
        } else {
            mConnectionCounts.put(dest, count + 1);
        }
    }

    private void findConnectionCounts() {
        Record r;
        for (String[] fields : mData) {
            try {
                r = new Record(fields);
                mConnectionCounts.put(Integer.valueOf(r.dest), Integer.valueOf(r.connCount));
            } catch (IllegalArgumentException e) {
                // stop weird error
            }
        }
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
            javaDist         = Util.Format.newBasic2dec().format(r[12]);
            hciDist          = Util.Format.newBasic2dec().format(r[13]);
        }

        Record(DataPacket dp, int connCount, double javaDist, double hciDist) {
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
            this.javaDist = Util.Format.newBasic2dec().format(javaDist);
            this.hciDist = Util.Format.newBasic2dec().format(hciDist);
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
