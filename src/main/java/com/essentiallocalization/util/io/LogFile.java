package com.essentiallocalization.util.io;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Jake on 9/20/13.
 */
public final class LogFile {
    private static final String TAG = LogFile.class.getSimpleName();

    public static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    public static final int
            ASSERT  = Log.ASSERT,
            ERROR   = Log.ERROR,
            WARN    = Log.WARN,
            INFO    = Log.INFO,
            DEBUG   = Log.DEBUG,
            VERBOSE = Log.VERBOSE;

    private static final String HEADER;
    static {
        String[] header = {
            quote("Nanos"),
            quote("Nanos-stamp"),
            quote("Timestamp"),
            quote("Lvl"),
            quote("Level"),
            quote("Tag"),
            quote("Message"),
        };
        HEADER = TextUtils.join(",", header);
    }


    private BufferedReader mReader;
    private final OutputFile mLog;
    private int mLevel;
    private String mTag;

    public LogFile(final File logFile) throws IOException {
        boolean needsHeader = false;
        if (!logFile.exists() || logFile.length() == 0) {
            needsHeader = true;
        }
        mLog = new OutputFile(logFile);
        if (needsHeader) {
            writeHeader();
        }
        mLevel = Log.VERBOSE;
        mTag = TAG;
        resetReader();
    }

    private void resetReader() {
        try {
            if (mReader != null) {
                mReader.close();
            }
            mReader = new BufferedReader(new FileReader(getFile()));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to reset BufferedReader.", e);
        } catch (IOException e) {
            Log.e(TAG, "Failed to reset BufferedReader.", e);
        }
    }

    public synchronized void setTag(String tag) {
        mTag = tag;
    }

    public synchronized String getTag() {
        return mTag;
    }

    public synchronized void setLevel(int level) {
        mLevel = level;
    }

    public synchronized int getLevel() {
        return mLevel;
    }

    public synchronized boolean isLoggable(int level) {
        return level >= mLevel;
    }

    public synchronized void clear() {
        try {
            mLog.truncate();
            writeHeader();
            resetReader();
        } catch (IOException e) {
            Log.e(TAG, "Failed to clear log.", e);
        }
    }

    public synchronized void close() {
        try {
            mLog.close();
            mReader.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to close log file", e);
        }
    }

    public synchronized List<String> readRaw() {
        List<String> list = new ArrayList<String>();
        String read;
        try {
            while ((read = mReader.readLine()) != null) {
                list.add(read);
            }
            resetReader();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read log file", e);
        }
        return list;
    }

    public synchronized List<String[]> read() {
        List<String> raw = readRaw();
        List<String[]> list = new ArrayList<String[]>(raw.size());
        for (String line : raw) {
            list.add(decodeCsvLine(line));
        }
        return list;
    }

    private synchronized void writeHeader() {
        try {
            mLog.append(HEADER + "\n");
        } catch (IOException e) {
            Log.e(TAG, "Failed writing header.", e);
        }
    }

    private synchronized boolean write(int level, String tag, String msg) {
        if (!isLoggable(level)) return false;

        String levelString = "";
        switch (level) {
            case ASSERT:    levelString = "ASSERT";     break;
            case ERROR:     levelString = "ERROR";      break;
            case WARN:      levelString = "WARN";       break;
            case INFO:      levelString = "INFO";       break;
            case DEBUG:     levelString = "DEBUG";      break;
            case VERBOSE:   levelString = "VERBOSE";    break;
        }

        long nanos = System.nanoTime();
        String[] parts = new String[] {
            String.valueOf(nanos),
            SDF.format(new Date(nanos / 1000)),
            SDF.format(new Date()),
            String.valueOf(level),
            levelString,
            tag,
            msg
        };

        try {
            mLog.append(encodeCsvLine(parts) + "\n");
        } catch (IOException e) {
            Log.e(TAG, "Failed write to log file.", e);
            return false;
        }

        return true;
    }

    /** Add the double-quote character before and after the given string */
    public static String quote(String text) {
        return "\"" + text + "\"";
    }

    /** Remove the first and last character of the string. */
    public static String unquote(String text) {
        return text.substring(1, text.length() - 1);
    }

    /** Doubles backslashes and existing double-quotes, return result in double-quotes. */
    public static String encodeCsv(String text) {
        return quote(text.replace("\\", "\\\\").replace("\"", "\"\""));
    }

    /** Removes starting and ending double-quote, then turn double backslashes and double-quotes into singles. */
    public static String decodeCsv(String text) {
        return unquote(text).replace("\\\\", "\\").replace("\"\"", "\"");
    }

    /** Doubles backslashes and existing double-quotes, return result in double-quotes. */
    public static String encodeCsvLine(final String[] parts) {
        for (int i = 0; i < parts.length; i++) {
            parts[i] = encodeCsv(parts[i]);
        }
        return TextUtils.join(",", parts);
    }

    /** Removes starting and ending double-quote, then turn double backslashes and double-quotes into singles. */
    public static String[] decodeCsvLine(String line) {
        // doing this manual because couldn't make working regex
        List<String> parts = new ArrayList<String>();
        int start = 0;
        int i = 1;      // start on 2nd char
        int end;
        while ((i < line.length()) && (i = line.indexOf("\"", i)) != -1) {
            if (i < line.length() - 1 && line.charAt(i + 1) == '"') {
                // found double double-quote, skip to next char
                i += 2;
            } else {
                // found single double-quote, end of entry
                end = i;
                parts.add(line.substring(start, end + 1));
                start = end + 2;    // skip comma
                i = start + 1;      // start looking after the next starting double-quote
            }
        }
        String[] result = parts.toArray(new String[parts.size()]);
        for (i = 0; i < result.length; i++) {
            result[i] = decodeCsv(result[i]);
        }
        return result;
    }

    public File getFile() {
        return mLog.getFile();
    }

    @Override
    public String toString() {
        return mLog.toString();
    }

    public synchronized void a(String msg) {
        write(ASSERT, mTag, msg);
    }
    public synchronized void a(String tag, String msg) {
        write(ASSERT, tag, msg);
    }

    public synchronized void e(String msg) {
        write(ERROR, mTag, msg);
    }
    public synchronized void e(String tag, String msg) {
        write(ERROR, tag, msg);
    }

    public synchronized void w(String msg) {
        write(WARN, mTag, msg);
    }
    public synchronized void w(String tag, String msg) {
        write(WARN, tag, msg);
    }

    public synchronized void i(String msg) {
        write(INFO, mTag, msg);
    }
    public synchronized void i(String tag, String msg) {
        write(INFO, tag, msg);
    }

    public synchronized void d(String msg) {
        write(DEBUG, mTag, msg);
    }
    public synchronized void d(String tag, String msg) {
        write(DEBUG, tag, msg);
    }

    public synchronized void v(String msg) {
        write(VERBOSE, mTag, msg);
    }
    public synchronized void v(String tag, String msg) {
        write(VERBOSE, tag, msg);
    }
}
