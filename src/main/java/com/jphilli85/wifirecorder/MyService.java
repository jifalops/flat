package com.jphilli85.wifirecorder;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by jake on 8/14/13.
 */
public class MyService extends Service {
//    public static final String ACTION_STOP = MyService.class.getName() + ".ACTION_STOP";
//    public static final String ACTION_ON_STOPPED = MyService.class.getName() + ".ACTION_ON_STOPPED";
    public static final String EXTRA_SCAN_COUNT = "scan_count";
    public static final String EXTRA_RECORD = "record";
    public static final String EXTRA_PHOTO = "photo";
    public static final String EXTRA_LABEL = "label";

    private static final String LOG_TAG = MyService.class.getSimpleName();

    private static final String BASE_DIR = "Wifi Records";
    private static final String LOG_FILE = "wifirecords.csv";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    private static boolean sIsRunning;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private WifiManager mWifiManager;
    private File mBaseDir;
    private FileWriter mLogWriter;
    private int mScanCount;
    private String mMarkLabel;
    private WifiReceiver mReceiver;

    public static abstract class WifiReceiver extends BroadcastReceiver {
        public static final String ACTION_WIFI = "com.jphilli85.wifirecorder.ACTION_WIFI";
        public static final String ACTION_PHOTO = "com.jphilli85.wifirecorder.ACTION_PHOTO";
        public static final String ACTION_LABEL = "com.jphilli85.wifirecorder.ACTION_LABEL";
    }

    public static boolean isRunning() {
        return sIsRunning;
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            Intent intent = (Intent) msg.obj;

            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                ++mScanCount;
                List<ScanResult> results = mWifiManager.getScanResults ();
                StringBuilder sb = new StringBuilder();
                for (ScanResult sr : results) {
                    sb.append(mScanCount + "," + SDF.format(new Date()) + "," + sr.BSSID + ","
                            + sr.SSID + "," + sr.level + "," + mMarkLabel + "\n");
                }

                try {
                    mLogWriter.append(sb.toString());
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error writing log record.", e);
                }

                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(WifiReceiver.ACTION_WIFI);
                broadcastIntent.putExtra(EXTRA_SCAN_COUNT, mScanCount);
                broadcastIntent.putExtra(EXTRA_RECORD, sb.toString());
                sendBroadcast(broadcastIntent);

                mWifiManager.startScan();

            } else if (intent.getAction().equals(WifiReceiver.ACTION_LABEL)) {

                mMarkLabel = intent.getStringExtra(EXTRA_LABEL);

            } else if (intent.getAction().equals(WifiReceiver.ACTION_PHOTO)) {
                mMarkLabel = intent.getStringExtra(EXTRA_LABEL);
                try {
                    Bitmap photo = intent.getParcelableExtra(EXTRA_PHOTO);
                    FileOutputStream out = new FileOutputStream(new File(mBaseDir, SDF.format(new Date()) + " " + mMarkLabel + ".png"));
                    photo.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error saving photo.", e);
                }
            }
//            else if (intent.getAction().equals(ACTION_STOP)) {
//
//                stopSelf();
//
//                Intent broadcastIntent = new Intent();
//                broadcastIntent.setAction(ACTION_ON_STOPPED);
//                sendBroadcast(broadcastIntent);
//            }
        }
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("WifiRecorderServiceThread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);


        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mBaseDir = new File(getExternalFilesDir(null) + File.separator + BASE_DIR);
        mBaseDir.mkdirs();
        mLogWriter = null;

        try {
            mLogWriter = new FileWriter(new File(mBaseDir, LOG_FILE), true);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error initializing LogWriter.", e);
        }

        mMarkLabel = "";

    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        sIsRunning = true;

        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();


        mReceiver = new WifiReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // For each start request, send a message to start a job and deliver the
                // start ID so we know which request we're stopping when we finish the job
                Message msg = mServiceHandler.obtainMessage();
                msg.arg1 = startId;
                msg.obj = intent;
                mServiceHandler.sendMessage(msg);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiReceiver.ACTION_LABEL);
        filter.addAction(WifiReceiver.ACTION_PHOTO);
//        filter.addAction(ACTION_STOP);
        registerReceiver(mReceiver, filter);


        if (!mWifiManager.startScan()) {
            Log.e(LOG_TAG, "Could not initiate scan.");
        }

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        sIsRunning = false;
        unregisterReceiver(mReceiver);
        try {
            mLogWriter.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to close log file.", e);
        }
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();

    }
}
