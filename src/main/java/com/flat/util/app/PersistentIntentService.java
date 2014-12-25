package com.flat.util.app;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.*;

/**
 *
 */
public abstract class PersistentIntentService extends Service {

    private HandlerThread mHandlerThread;
    private Handler mServiceHandler;
    private final LocalBinder mBinder = new LocalBinder();
    private volatile boolean mIsPersistent;
    private volatile boolean mIsRegistered;
    private volatile boolean mIsEnabled;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message msg = mServiceHandler.obtainMessage();
            msg.obj = intent;
            mServiceHandler.sendMessage(msg);
        }
    };

    /** Called on service's thread */
    protected abstract void onHandleIntent(Intent intent);

    @Override
    public void onCreate() {
        mHandlerThread = new HandlerThread(getClass().getName(), getThreadPriority());
        mHandlerThread.start();

        mServiceHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                final Intent intent = (Intent) msg.obj;
                onHandleIntent(intent);
            }
        };
    }

    @Override
    public void onDestroy() {
        if (mIsRegistered) {
            unregisterReceiver();
        }
        mServiceHandler.removeCallbacksAndMessages(null);
        mHandlerThread.quitSafely();
        mIsPersistent = false;
        super.onDestroy();
    }

    public final Looper getLooper() {
        return mHandlerThread.getLooper();
    }

    public final boolean isPersistent() {
        return mIsPersistent;
    }

    public final boolean isRegistered() {
        return mIsRegistered;
    }

    public boolean isEnabled() { return mIsEnabled; }

    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    @Override
    public boolean stopService(Intent name) {
        mIsPersistent = false;
        return super.stopService(name);
    }

    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {
        mIsPersistent = true;
        return getStartType();
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return mBinder;
    }

    protected int getThreadPriority() {
        return android.os.Process.THREAD_PRIORITY_BACKGROUND;
    }

    protected int getStartType() {
        return Service.START_STICKY;
    }

    public Intent registerReceiver(IntentFilter filter) {
        mIsRegistered = true;
        return registerReceiver(mReceiver, filter);
    }

    public void unregisterReceiver() {
        mIsRegistered = false;
        unregisterReceiver(mReceiver);
    }

    // Activities use this to get an instance of the service.
    public final class LocalBinder extends Binder {
        public PersistentIntentService getService() {
            return PersistentIntentService.this;
        }
    }
}
