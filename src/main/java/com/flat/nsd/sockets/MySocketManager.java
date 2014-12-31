package com.flat.nsd.sockets;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The socket manager takes care of the higher level aspects of maintaining client/server sockets.
 * It will create a new server socket whenever the existing one has connected, and it will attempt
 * to retry client connections that fail.
 *
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class MySocketManager {
    private static final String TAG = MySocketManager.class.getSimpleName();

    private MyServerSocket server;
    private final List<MyConnectionSocket> connections = Collections.synchronizedList(new ArrayList<MyConnectionSocket>());

    /** Assumes the serviceName contains an IP */
    public int countConnectionsTo(String serviceName) {
        int count = 0;
        synchronized (connections) {
            for (MyConnectionSocket mcs : connections) {
                if (serviceName.contains(mcs.getAddress().getHostAddress())) {
                    ++count;
                }
            }
        }
        return count;
    }

    private boolean running;
    public void start() {
        if (!running) {
            running = true;
            initializeServer();
        }
    }
    public void stop() {
        if (running) {
            running = false;
            server.cancel();
            for (MyConnectionSocket mcs : connections) {
                mcs.cancel();
            }
            connections.clear();
        }
    }

    private void initializeServer() {
        if (!running) return;
        server = new MyServerSocket();
        server.registerListener(serverListener);
        server.start();
    }

    private void addNewConnection(Socket s) {
        if (!running) return;
        MyConnectionSocket conn = new MyConnectionSocket(s);
        conn.registerListener(connectionSocketListener);
        connections.add(conn);
        conn.start();
    }

    private void retryConnection(MyConnectionSocket mcs) {
        if (!running) return;
        connections.remove(mcs);
        try {
            addNewConnection(new Socket(mcs.getAddress(), mcs.getPort()));
        } catch (IOException e) {
            Log.e(TAG, "Failed retrying connection to " + Sockets.toString(mcs.getAddress(), mcs.getPort()));
        }
    }

    private final MyServerSocket.MyServerSocketListener serverListener = new MyServerSocket.MyServerSocketListener() {
        @Override
        public void onConnected(MyServerSocket server, Socket socket) {
            addNewConnection(socket);
        }

        @Override
        public void onFinished(MyServerSocket server) {
            initializeServer();
        }
    };

    private final MyConnectionSocket.MyConnectionSocketListener connectionSocketListener = new MyConnectionSocket.MyConnectionSocketListener() {
        @Override
        public void onMessageSent(MyConnectionSocket socket, String msg) {
            Message m = handler.obtainMessage();
            m.arg1 = 1; // sent
            m.obj = msg;
            handler.sendMessage(m);
        }

        @Override
        public void onMessageReceived(MyConnectionSocket socket, String msg) {
            Message m = handler.obtainMessage();
            m.obj = msg;
            handler.sendMessage(m);
        }

        @Override
        public void onFinished(MyConnectionSocket socket) {
            retryConnection(socket);
        }
    };

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String s = (String) msg.obj;
            if (msg.arg1 == 1) {
                for (MessageListener l : listeners) {
                    l.onMessageSent(s);
                }
            } else {
                for (MessageListener l : listeners) {
                    l.onMessageReceived(s);
                }
            }
        }
    };


    /**
     * Allow other objects to react to events.
     */
    public static interface MessageListener {
        /** called on main thread. */
        void onMessageSent(String msg);
        /** called on main thread. */
        void onMessageReceived(String msg);
    }
    // a List of unique listener instances.
    private final List<MessageListener> listeners = new ArrayList<MessageListener>(1);
    public boolean registerListener(MessageListener l) {
        if (listeners.contains(l)) return false;
        listeners.add(l);
        return true;
    }
    public boolean unregisterListener(MessageListener l) {
        return listeners.remove(l);
    }
    public int unregisterListeners() {
        int size = listeners.size();
        listeners.clear();
        return size;
    }
}
