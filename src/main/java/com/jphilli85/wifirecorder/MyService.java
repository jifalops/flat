package com.jphilli85.wifirecorder;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
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
    /** Methods are called on a background thread. */
    public static interface Listener {
        void onScanRecorded(int count);
    }

    public static final String ACTION_SAVE_PHOTO = MyService.class.getName() + ".ACTION_SAVE_PHOTO";
//    private static final String ACTION_VIEW_PHOTOS = MyService.class.getName() + ".ACTION_VIEW_PHOTOS";
//    public static final String ACTION_LABEL = MyService.class.getName() + ".ACTION_LABEL";
//    public static final String ACTION_RECORD_WRITTEN = MyService.class.getName() + ".ACTION_RECORD_WRITTEN";
//    public static final String ACTION_VIEW_LOG = MyService.class.getName() + ".ACTION_VIEW_LOG";
    private static final String ACTION_CLEAR_LOG = MyService.class.getName() + ".ACTION_CLEAR_LOG";
//    public static final String ACTION_START_RECORDING = MyService.class.getName() + ".ACTION_START_RECORDING";
//    public static final String ACTION_STOP_RECORDING = MyService.class.getName() + ".ACTION_STOP_RECORDING";
//    public static final String EXTRA_SCAN_COUNT = "scan_count";
//    public static final String EXTRA_RECORD = "record";
    public static final String EXTRA_PHOTO = "photo";
//    public static final String EXTRA_LABEL = "label";

    private static final String LOG_TAG = MyService.class.getSimpleName(); // Android's log
    private static final String LOG_FILE = "data.csv";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    private static final Listener DUMMY_LISTENER = new Listener() {
        @Override public void onScanRecorded(int count) {}
    };


    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private WifiManager mWifiManager;
    private File mBaseDir;
    private File mLogFile;
    private FileWriter mLogWriter;
    private int mScanCount;
    private volatile String mLabel;
    private MessageReceiver mWifiReceiver;
    private MessageReceiver mCommandReceiver;
    private final IBinder mBinder = new LocalBinder();
    private boolean mIsRecording;
    private boolean mIsPersistent;
    private Listener mListener = DUMMY_LISTENER;
    private MediaScannerConnection mMediaScannerConnection;
    private MediaScannerConnection.MediaScannerConnectionClient mMediaScannerClient;

    public class LocalBinder extends Binder {
        MyService getService() {
            return MyService.this;
        }
    }

    private class MessageReceiver extends BroadcastReceiver {
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
                            + sr.SSID + "," + sr.level + "," + mLabel + "\n");
                }


                try {
                    mLogWriter.append(sb.toString());
                    mLogWriter.flush();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error writing log record.", e);
                }

                mListener.onScanRecorded(mScanCount);

                mWifiManager.startScan();

            } else if (intent.getAction().equals(ACTION_SAVE_PHOTO)) {

                try {
                    Bitmap photo = intent.getParcelableExtra(EXTRA_PHOTO);
                    FileOutputStream out = new FileOutputStream(
                            new File(mBaseDir, SDF.format(new Date()) + " " + mLabel + ".png"));
                    photo.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error saving photo.", e);
                }

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
        Toast.makeText(this, "Service Creating", Toast.LENGTH_SHORT).show();
        HandlerThread thread = new HandlerThread("WifiRecorderServiceThread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        mLabel = "";

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mBaseDir = getExternalFilesDir(null);
        mLogFile = new File(mBaseDir, LOG_FILE);

        mLogWriter = null;

        try {
            mLogWriter = new FileWriter(mLogFile, true);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error initializing LogWriter.", e);
        }


        mCommandReceiver = new MessageReceiver();
        mWifiReceiver = new MessageReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SAVE_PHOTO);
        filter.addAction(ACTION_CLEAR_LOG);
        registerReceiver(mCommandReceiver, filter);


        mMediaScannerClient = new MediaScannerConnection.MediaScannerConnectionClient() {
            @Override
            public void onMediaScannerConnected() {
                mMediaScannerConnection.scanFile(mLogFile.getAbsolutePath(), "image/*");
            }

            @Override
            public void onScanCompleted(String s, Uri uri) {
                try {
                    if (uri != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(uri);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
                finally {
                    mMediaScannerConnection.disconnect();
                    mMediaScannerConnection = null;
                }
            }
        };

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service Starting", Toast.LENGTH_SHORT).show();

        mIsPersistent = true;

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(this, "Service Bound", Toast.LENGTH_SHORT).show();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mListener = DUMMY_LISTENER;
        Toast.makeText(this, "Service Unbound", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mCommandReceiver);
        mIsPersistent = false;
        setRecording(false);

        try {
            mLogWriter.flush();
            mLogWriter.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to close log file.", e);
        }
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_SHORT).show();
    }

    public void setListener(Listener listener) {
        if (listener == null) mListener = DUMMY_LISTENER;
        else mListener = listener;
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public boolean isPersistent() {
        return mIsPersistent;
    }

    public void setLabel(String label) {
        mLabel = label;
    }

    public void setRecording(boolean recording) {
        if (recording && !mIsRecording) {
            registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            if (!mWifiManager.startScan()) {
                Log.e(LOG_TAG, "Could not initiate scan.");
            }
            mIsRecording = true;
        }
        else if (!recording && mIsRecording) {
            unregisterReceiver(mWifiReceiver);
            mIsRecording = false;
        }
    }

    public File getBaseDir() {
        return mBaseDir;
    }

    public File getLogFile() {
        return mLogFile;
    }



    public void clearLog() {
        sendBroadcast(new Intent(ACTION_CLEAR_LOG));
    }



    public void viewPhotos() {
        //TODO show in gallery
//        if (mMediaScannerConnection != null) {
//            mMediaScannerConnection.disconnect();
//        }
//        mMediaScannerConnection = new MediaScannerConnection(this, mMediaScannerClient);
//        mMediaScannerConnection.connect();


    }
}
