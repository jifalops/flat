package com.flat.nsd.sockets;

import android.text.TextUtils;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class Sockets {
    public static String toString(ServerSocket socket, Object... objects) {
        String s = "{no socket info}";
        try {
            s = socket.getInetAddress().getHostAddress()+":"+socket.getLocalPort()+" (local).";
            if (objects.length > 0) {
                s += "\n" + TextUtils.join("\n", objects);
            }
        } catch (NullPointerException ignored) {}
        return s;
    }

    public static String toString(Socket socket, Object... objects) {
        String s = "{no socket info}";
        try {
            s = socket.getInetAddress().getHostAddress()+":"+socket.getPort()+" (local "+socket.getLocalPort()+").";
            if (objects.length > 0) {
                s += "\n" + TextUtils.join("\n", objects);
            }
        } catch (NullPointerException ignored) {}
        return s;
    }

    public static String toString(InetAddress address, int port) {
        String s = "{no socket info}";
        try {
            s = address.getHostAddress()+":"+port;
        } catch (NullPointerException ignored) {}
        return s;
    }
}
