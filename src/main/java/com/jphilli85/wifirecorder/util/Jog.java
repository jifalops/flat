package com.jphilli85.wifirecorder.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Jake on 8/29/13.
 */
public class Jog {
    private static final String
            TAG = Jog.class.getName(),
            LOG_PROPERY = "log.tag." + Jog.class.getPackage().getName() + ".Jog.Log",
            TOAST_PROPERY = "log.tag." + Jog.class.getPackage().getName() + "Jog.Toast";

    public static final int 
            ASSERT  = Log.ASSERT,
            ERROR   = Log.ERROR,
            WARN    = Log.WARN,
            INFO    = Log.INFO,
            DEBUG   = Log.DEBUG,
            VERBOSE = Log.VERBOSE;

    private Jog() {}

    static {
        // TODO, check if setting system properties is working. Use static fields if not.
        setLogLevel(INFO);
        setToastLevel(INFO);
    }

    private static int log(int level, String msg, Throwable tr, Context toastCtx, boolean longToast) {

        if (toastCtx != null && isShowable(level)) {
            int dur = longToast ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
            Toast.makeText(toastCtx, msg, dur).show();
        }

        if (isLoggable(level)) {
            // get outside caller
            String tag = "";
            try { tag = new Exception().getStackTrace()[2].getClass().getSimpleName(); }
            catch (Exception ignored) {}

            switch (level) {
                case ASSERT:
                    if (tr == null) return Log.wtf(tag, msg);
                    else            return Log.wtf(tag, msg, tr);
                case ERROR:
                    if (tr == null) return Log.e(tag, msg);
                    else            return Log.e(tag, msg, tr);
                case WARN:
                    if (tr == null) return Log.w(tag, msg);
                    else            return Log.w(tag, msg, tr);
                case INFO:
                    if (tr == null) return Log.i(tag, msg);
                    else            return Log.i(tag, msg, tr);
                case DEBUG:
                    if (tr == null) return Log.d(tag, msg);
                    else            return Log.d(tag, msg, tr);
                case VERBOSE:
                    if (tr == null) return Log.v(tag, msg);
                    else            return Log.v(tag, msg, tr);
            }
        }
        return 0;
    }

    public static void setLogLevel(int level) {
        System.setProperty(LOG_PROPERY, String.valueOf(level));
    }

    public static int getLogLevel() {
        String prop = System.getProperty(LOG_PROPERY);
        int level = prop == null ? -1 : Integer.valueOf(prop);
        if (level < VERBOSE) {
            Log.wtf(TAG, "got invalid log level: " + level);
        }
        return level;
    }

    public static boolean isLoggable(int level) {
        boolean loggable = Log.isLoggable(LOG_PROPERY, level);
        boolean check = level >= getLogLevel();
        if (check != loggable) {
            Log.wtf(TAG, "Jog.isLoggable() does not agree with Log.isLoggable()");
        }
        return loggable;
    }

    public static void setToastLevel(int level) {
        System.setProperty(TOAST_PROPERY, String.valueOf(level));
    }

    public static int getToastLevel() {
        String prop = System.getProperty(TOAST_PROPERY);
        int level = prop == null ? -1 : Integer.valueOf(prop);
        if (level < VERBOSE) {
            Log.wtf(TAG, "got invalid toast level: " + level);
        }
        return level;
    }

    public static boolean isShowable(int level) {
        return level >= getToastLevel();
    }

    public static int a(String msg) {
        return log(ASSERT, msg, null, null, false);
    }
    public static int a(String msg, Throwable tr) {
        return log(ASSERT, msg, tr, null, false);
    }
    public static int a(String msg, Context toastCtx) {
        return log(ASSERT, msg, null, toastCtx, false);
    }
    public static int a(String msg, Context toastCtx, boolean longToast) {
        return log(ASSERT, msg, null, toastCtx, longToast);
    }
    public static int a(String msg, Throwable tr, Context toastCtx) {
        return log(ASSERT, msg, tr, toastCtx, false);
    }
    public static int a(String msg, Throwable tr, Context toastCtx, boolean longToast) {
        return log(ASSERT, msg, tr, toastCtx, longToast);
    }
    

    public static int e(String msg) {
        return log(ERROR, msg, null, null, false);
    }
    public static int e(String msg, Throwable tr) {
        return log(ERROR, msg, tr, null, false);
    }
    public static int e(String msg, Context toastCtx) {
        return log(ERROR, msg, null, toastCtx, false);
    }
    public static int e(String msg, Context toastCtx, boolean longToast) {
        return log(ERROR, msg, null, toastCtx, longToast);
    }
    public static int e(String msg, Throwable tr, Context toastCtx) {
        return log(ERROR, msg, tr, toastCtx, false);
    }
    public static int e(String msg, Throwable tr, Context toastCtx, boolean longToast) {
        return log(ERROR, msg, tr, toastCtx, longToast);
    }


    public static int w(String msg) {
        return log(WARN, msg, null, null, false);
    }
    public static int w(String msg, Throwable tr) {
        return log(WARN, msg, tr, null, false);
    }
    public static int w(String msg, Context toastCtx) {
        return log(WARN, msg, null, toastCtx, false);
    }
    public static int w(String msg, Context toastCtx, boolean longToast) {
        return log(WARN, msg, null, toastCtx, longToast);
    }
    public static int w(String msg, Throwable tr, Context toastCtx) {
        return log(WARN, msg, tr, toastCtx, false);
    }
    public static int w(String msg, Throwable tr, Context toastCtx, boolean longToast) {
        return log(WARN, msg, tr, toastCtx, longToast);
    }


    public static int i(String msg) {
        return log(INFO, msg, null, null, false);
    }
    public static int i(String msg, Throwable tr) {
        return log(INFO, msg, tr, null, false);
    }
    public static int i(String msg, Context toastCtx) {
        return log(INFO, msg, null, toastCtx, false);
    }
    public static int i(String msg, Context toastCtx, boolean longToast) {
        return log(INFO, msg, null, toastCtx, longToast);
    }
    public static int i(String msg, Throwable tr, Context toastCtx) {
        return log(INFO, msg, tr, toastCtx, false);
    }
    public static int i(String msg, Throwable tr, Context toastCtx, boolean longToast) {
        return log(INFO, msg, tr, toastCtx, longToast);
    }


    public static int d(String msg) {
        return log(DEBUG, msg, null, null, false);
    }
    public static int d(String msg, Throwable tr) {
        return log(DEBUG, msg, tr, null, false);
    }
    public static int d(String msg, Context toastCtx) {
        return log(DEBUG, msg, null, toastCtx, false);
    }
    public static int d(String msg, Context toastCtx, boolean longToast) {
        return log(DEBUG, msg, null, toastCtx, longToast);
    }
    public static int d(String msg, Throwable tr, Context toastCtx) {
        return log(DEBUG, msg, tr, toastCtx, false);
    }
    public static int d(String msg, Throwable tr, Context toastCtx, boolean longToast) {
        return log(DEBUG, msg, tr, toastCtx, longToast);
    }


    public static int v(String msg) {
        return log(VERBOSE, msg, null, null, false);
    }
    public static int v(String msg, Throwable tr) {
        return log(VERBOSE, msg, tr, null, false);
    }
    public static int v(String msg, Context toastCtx) {
        return log(VERBOSE, msg, null, toastCtx, false);
    }
    public static int v(String msg, Context toastCtx, boolean longToast) {
        return log(VERBOSE, msg, null, toastCtx, longToast);
    }
    public static int v(String msg, Throwable tr, Context toastCtx) {
        return log(VERBOSE, msg, tr, toastCtx, false);
    }
    public static int v(String msg, Throwable tr, Context toastCtx, boolean longToast) {
        return log(VERBOSE, msg, tr, toastCtx, longToast);
    }
}
