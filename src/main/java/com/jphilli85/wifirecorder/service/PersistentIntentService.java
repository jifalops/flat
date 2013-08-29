package com.jphilli85.wifirecorder.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

/**
 * Created by jake on 8/14/13.
 */
public abstract class PersistentIntentService extends Service {
    private static final String LOG_TAG = PersistentIntentService.class.getSimpleName();

    protected Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private final IBinder mBinder = new PersistentIntentServiceBinder();
    private final IntentReceiver mReceiver = new IntentReceiver();
    protected IntentFilter mFilter;

    protected abstract void onHandleIntent(Intent intent);

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread(getClass().getName(), getThreadPriority());
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        mFilter = new IntentFilter();
    }

    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {
        return getStartType();
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return mBinder;
    }

    // Activities use this to get an instance of the service.
    public final class PersistentIntentServiceBinder extends Binder {
        PersistentIntentService getService() {
            return PersistentIntentService.this;
        }
    }

    private final class IntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, android.content.Intent intent) {
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
            onHandleIntent(intent);
        }
    }

    protected int getThreadPriority() {
        return android.os.Process.THREAD_PRIORITY_BACKGROUND;
    }

    protected int getStartType() {
        return Service.START_STICKY;
    }

    public final Intent registerReceiver() {
        return registerReceiver(mReceiver, mFilter);
    }

    public final void unregisterReceiver() {
        unregisterReceiver(mReceiver);
    }
}
