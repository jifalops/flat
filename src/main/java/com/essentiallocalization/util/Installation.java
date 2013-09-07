package com.essentiallocalization.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

/**
 * Created by Jake on 9/1/13.
 */
public class Installation {
    private static final String TAG = Installation.class.getSimpleName();
    private static final String FILE_NAME = "installation.uuid";
    private static String sUuid = null;

    public synchronized static String id(Context context) {
        if (sUuid == null) {
            File f = new File(context.getFilesDir(), FILE_NAME);
            try {
                if (!f.exists()) {
                    writeInstallationFile(f);
                }
                sUuid = readInstallationFile(f);
            } catch (Exception e) {
                Log.wtf(TAG, "unable to read/write UUID file", e);
                throw new RuntimeException(e);
            }
        }
        return sUuid;
    }

    public synchronized static UUID uuid(Context context) {
        return UUID.fromString(id(context));
    }

    private static void writeInstallationFile(File installation) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
    }

    private static String readInstallationFile(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }
}