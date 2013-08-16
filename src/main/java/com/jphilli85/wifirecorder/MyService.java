package com.jphilli85.wifirecorder;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
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
    public static final String ACTION_PHOTO = MyService.class.getName() + ".ACTION_PHOTO";
    public static final String ACTION_LABEL = MyService.class.getName() + ".ACTION_LABEL";
    public static final String ACTION_RECORD_WRITTEN = MyService.class.getName() + ".ACTION_RECORD_WRITTEN";
    public static final String ACTION_VIEW_LOG = MyService.class.getName() + ".ACTION_VIEW_LOG";
    public static final String ACTION_CLEAR_LOG = MyService.class.getName() + ".ACTION_CLEAR_LOG";
    public static final String ACTION_START_RECORDING = MyService.class.getName() + ".ACTION_START_RECORDING";
    public static final String ACTION_STOP_RECORDING = MyService.class.getName() + ".ACTION_STOP_RECORDING";
    public static final String EXTRA_SCAN_COUNT = "scan_count";
    public static final String EXTRA_RECORD = "record";
    public static final String EXTRA_PHOTO = "photo";
    public static final String EXTRA_LABEL = "label";

    private static final String LOG_TAG = MyService.class.getSimpleName();

    private static final String BASE_DIR = "Wifi Records";
    private static final String LOG_FILE = "wifirecords.csv.txt";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    public static final int STATE_RUNNING = 0x1;
    public static final int STATE_SCANNING = 0x2;


    private static int sState;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private WifiManager mWifiManager;
    private File mBaseDir;
    private File mLogFile;
    private FileWriter mLogWriter;
    private int mScanCount;
    private String mMarkLabel;
    private MessageReceiver mWifiReceiver;
    private MessageReceiver mCommandReceiver;


    public static boolean isRunning() {
        return (sState & STATE_RUNNING) == STATE_RUNNING;
    }

    public static boolean isScanning() {
        return (sState & STATE_SCANNING) == STATE_SCANNING;
    }

    private final class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message msg = mServiceHandler.obtainMessage();
            msg.obj = intent;
            mServiceHandler.sendMessage(msg);
        }
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

                Intent broadcastIntent = new Intent(ACTION_RECORD_WRITTEN);
                broadcastIntent.putExtra(EXTRA_SCAN_COUNT, mScanCount);
                broadcastIntent.putExtra(EXTRA_RECORD, sb.toString());
                sendBroadcast(broadcastIntent);

                mWifiManager.startScan();

            } else if (intent.getAction().equals(ACTION_START_RECORDING)) {
                registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                if (!mWifiManager.startScan()) {
                    Log.e(LOG_TAG, "Could not initiate scan.");
                }
                sState |= STATE_SCANNING;
            } else if (intent.getAction().equals(ACTION_STOP_RECORDING)) {
                unregisterReceiver(mWifiReceiver);
                try { mLogWriter.flush(); }
                catch (IOException ignored) {}
                sState &= ~STATE_SCANNING;
            } else if (intent.getAction().equals(ACTION_LABEL)) {
                mMarkLabel = intent.getStringExtra(EXTRA_LABEL);
            } else if (intent.getAction().equals(ACTION_PHOTO)) {
                mMarkLabel = intent.getStringExtra(EXTRA_LABEL);
                try {
                    Bitmap photo = intent.getParcelableExtra(EXTRA_PHOTO);
                    FileOutputStream out = new FileOutputStream(new File(mBaseDir, SDF.format(new Date()) + " " + mMarkLabel + ".png"));
                    photo.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error saving photo.", e);
                }
            } else if (intent.getAction().equals(ACTION_VIEW_LOG)) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.fromFile(mLogFile), "text/plain");
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { mLogWriter.flush(); }
                catch (IOException ignored) {}
                startActivity(i);
            } else if (intent.getAction().equals(ACTION_CLEAR_LOG)) {
                try {
                    mLogWriter.close();
                    mLogWriter = new FileWriter(mLogFile);
                    mLogWriter.close();
                    mLogWriter = new FileWriter(mLogFile, true);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error clearing Log.", e);
                }
            }
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
        mLogFile = new File(mBaseDir, LOG_FILE);
        mLogWriter = null;

        try {
            mLogWriter = new FileWriter(mLogFile, true);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error initializing LogWriter.", e);
        }

        mMarkLabel = "";

        mCommandReceiver = new MessageReceiver();
        mWifiReceiver = new MessageReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_LABEL);
        filter.addAction(ACTION_PHOTO);
        filter.addAction(ACTION_VIEW_LOG);
        filter.addAction(ACTION_CLEAR_LOG);
        filter.addAction(ACTION_START_RECORDING);
        filter.addAction(ACTION_STOP_RECORDING);
        registerReceiver(mCommandReceiver, filter);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sState |= STATE_RUNNING;

        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();



        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        sState = 0;
        unregisterReceiver(mCommandReceiver);
        if (isScanning()) unregisterReceiver(mWifiReceiver);
        try {
            mLogWriter.flush();
            mLogWriter.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to close log file.", e);
        }
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();

    }
}
