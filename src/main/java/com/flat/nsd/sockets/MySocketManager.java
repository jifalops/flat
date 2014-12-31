package com.flat.nsd.sockets;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.net.InetAddress;
import java.net.ServerSocket;
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

    /*
     * Simple Singleton
     */
    private MySocketManager() {}
    private static final MySocketManager instance = new MySocketManager();
    public static MySocketManager getInstance() { return instance; }



    private final List<MyConnectionSocket> connections = Collections.synchronizedList(new ArrayList<MyConnectionSocket>());
    private final List<MyServerSocket> servers = Collections.synchronizedList(new ArrayList<MyServerSocket>());


    public int sendMessage(String msg) {
        int count = 0;
        for (MyConnectionSocket mcs : connections) {
            mcs.sendMessage(msg);
            ++count;
        }
        return count;
    }

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
        Log.d(TAG, count + " connections to " + serviceName);
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
            for (MyServerSocket mss : servers) {
                mss.cancel();
            }
            servers.clear();
            for (MyConnectionSocket mcs : connections) {
                mcs.cancel();
            }
            connections.clear();
        }
    }

    private void initializeServer() {
        if (!running) return;
        MyServerSocket server = new MyServerSocket();
        server.registerListener(serverListener);
        servers.add(server);
        server.start();
    }

    private void retryConnection(MyConnectionSocket mcs) {
        if (!running) return;
        connections.remove(mcs);
        connectTo(mcs.getAddress(), mcs.getPort());
    }

    public void connectTo(InetAddress address, int port) {
        if (!running) return;
        MyConnectionSocket conn = new MyConnectionSocket(address, port);
        conn.registerListener(connectionSocketListener);
        connections.add(conn);
        conn.start();
    }

    public void connectTo(Socket socket) {
        if (!running) return;
        MyConnectionSocket conn = new MyConnectionSocket(socket);
        conn.registerListener(connectionSocketListener);
        connections.add(conn);
        conn.start();
    }

    private final MyServerSocket.ServerListener serverListener = new MyServerSocket.ServerListener() {
        @Override
        public void onNewServerSocket(MyServerSocket mss, ServerSocket ss) {
            notifyHandler(newServer, null, mss);
        }

        @Override
        public void onConnected(MyServerSocket mss, Socket socket) {
            connectTo(socket);
            notifyHandler(serverConnected, null, mss);
        }

        @Override
        public void onFinished(MyServerSocket mss) {
            initializeServer();
            notifyHandler(serverFinished, null, mss);
        }
    };

    private final MyConnectionSocket.ConnectionListener connectionSocketListener = new MyConnectionSocket.ConnectionListener() {
        @Override
        public void onConnected(MyConnectionSocket mcs, Socket socket) {
            notifyHandler(clientConnected, null, mcs);
        }

        @Override
        public void onMessageSent(MyConnectionSocket mcs, String s) {
            notifyHandler(sent, s, mcs);
        }

        @Override
        public void onMessageReceived(MyConnectionSocket mcs, String s) {
            notifyHandler(received, s, mcs);
        }

        @Override
        public void onFinished(MyConnectionSocket mcs) {
            notifyHandler(clientFinished, null, mcs);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
            retryConnection(mcs);
        }
    };

    private void notifyHandler(int type, String s, Object obj) {
        Bundle data = new Bundle();
        data.putString("string", s);
        Message msg = handler.obtainMessage();
        msg.setData(data);
        msg.arg1 = type;
        msg.obj = obj;
        handler.sendMessage(msg);
    }

    private final int serverConnected=1, serverFinished=2, newServer=3, sent=4, received=5, clientFinished=6, clientConnected=7;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MyServerSocket mss;
            MyConnectionSocket mcs;
            switch (msg.arg1) {
                case serverConnected:
                    mss = (MyServerSocket) msg.obj;
                    for (SocketListener l : listeners) {
                        l.onServerConnected(mss, mss.getAcceptedSocket());
                    }
                    break;
                case serverFinished:
                    mss = (MyServerSocket) msg.obj;
                    for (SocketListener l : listeners) {
                        l.onServerFinished(mss);
                    }
                    break;
                case newServer:
                    mss = (MyServerSocket) msg.obj;
                    for (SocketListener l : listeners) {
                        l.onNewServerSocket(mss, mss.getServerSocket());
                    }
                    break;
                case sent:
                    mcs = (MyConnectionSocket) msg.obj;
                    for (SocketListener l : listeners) {
                        l.onMessageSent(mcs, msg.getData().getString("string"));
                    }
                    break;
                case received:
                    mcs = (MyConnectionSocket) msg.obj;
                    for (SocketListener l : listeners) {
                        l.onMessageReceived(mcs, msg.getData().getString("string"));
                    }
                    break;
                case clientFinished:
                    mcs = (MyConnectionSocket) msg.obj;
                    for (SocketListener l : listeners) {
                        l.onClientFinished(mcs);
                    }
                    break;
                case clientConnected:
                    mcs = (MyConnectionSocket) msg.obj;
                    for (SocketListener l : listeners) {
                        l.onClientConnected(mcs, mcs.getSocket());
                    }
                    break;
            }
        }
    };


    /**
     * Allow other objects to react to events. Called on main thread.
     */
    public static interface SocketListener {
        void onServerConnected(MyServerSocket mss, Socket socket);
        void onServerFinished(MyServerSocket mss);
        void onNewServerSocket(MyServerSocket mss, ServerSocket socket);
        void onMessageSent(MyConnectionSocket mcs, String msg);
        void onMessageReceived(MyConnectionSocket mcs, String msg);
        void onClientFinished(MyConnectionSocket mcs);
        void onClientConnected(MyConnectionSocket mcs, Socket socket);
    }
    // a List of unique listener instances.
    private final List<SocketListener> listeners = new ArrayList<SocketListener>(1);
    public boolean registerListener(SocketListener l) {
        if (listeners.contains(l)) return false;
        listeners.add(l);
        return true;
    }
    public boolean unregisterListener(SocketListener l) {
        return listeners.remove(l);
    }
}
