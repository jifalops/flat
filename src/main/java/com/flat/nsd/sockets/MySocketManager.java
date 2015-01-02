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
 * The socket manager takes care of the higher level aspects of maintaining client/server sockets
 * such as attempting to retry client connections that fail.
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
    private MyServerSocket server;

    public int send(String msg) {
        int count = 0;
        for (MyConnectionSocket mcs : connections) {
            if (mcs.send(msg)) ++count;                 // TODO could use one shared sending thread
        }
        return count;
    }

    /** Assumes the serviceName contains an IP */
    public synchronized int countConnectionsTo(String serviceName) {
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

    public synchronized boolean hasAddress(InetAddress address) {
        for (MyConnectionSocket mcs : connections) {
            if (mcs.getAddress().getHostAddress().equals(address.getHostAddress())) {
                return true;
            }
        }
        return false;
    }


    public synchronized void startServer() {
        stopServer();
        server = new MyServerSocket();
        server.registerListener(serverListener);
        server.start();
    }

    public synchronized void stopServer() {
        if (server != null) {
            server.stop();
            server.unregisterListener(serverListener);
        }
    }

    public synchronized boolean startConnection(MyConnectionSocket mcs) {
        if (hasAddress(mcs.getAddress())) return false;
        mcs.registerListener(connectionSocketListener);
        connections.add(mcs);
        mcs.start();
        return true;
    }

    public synchronized void stopConnections() {
        for (MyConnectionSocket mcs : connections) {
            mcs.stop();
            mcs.unregisterListener(connectionSocketListener);
        }
        connections.clear();
    }


    private final MyServerSocket.ServerListener serverListener = new MyServerSocket.ServerListener() {
        @Override
        public void onServerSocketListening(MyServerSocket mss, ServerSocket ss) {
            notifyHandler(serverListening, null, mss);
        }

        @Override
        public void onServerAcceptedClientSocket(MyServerSocket mss, Socket socket) {
            startConnection(new MyConnectionSocket(socket));
            notifyHandler(serverAcceptedClientSocket, null, mss);
        }

        @Override
        public void onFinished(MyServerSocket mss) {
            notifyHandler(serverFinished, null, mss);
        }
    };

    private final MyConnectionSocket.ConnectionListener connectionSocketListener = new MyConnectionSocket.ConnectionListener() {
        @Override
        public void onSocketCreated(MyConnectionSocket mcs, Socket socket) {
            notifyHandler(clientCreatedSocket, null, mcs);
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
            connections.remove(mcs);
            notifyHandler(clientFinished, null, mcs);
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

    private final int serverAcceptedClientSocket =1, serverFinished=2, serverListening =3, sent=4, received=5, clientFinished=6, clientCreatedSocket =7;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MyServerSocket mss;
            MyConnectionSocket mcs;
            switch (msg.arg1) {
                case serverAcceptedClientSocket:
                    mss = (MyServerSocket) msg.obj;
                    for (SocketListener l : listeners) {
                        l.onServerAcceptedClientSocket(mss, mss.getAcceptedSocket());
                    }
                    break;
                case serverFinished:
                    mss = (MyServerSocket) msg.obj;
                    for (SocketListener l : listeners) {
                        l.onServerFinished(mss);
                    }
                    break;
                case serverListening:
                    mss = (MyServerSocket) msg.obj;
                    for (SocketListener l : listeners) {
                        l.onServerSocketListening(mss, mss.getServerSocket());
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
                case clientCreatedSocket:
                    mcs = (MyConnectionSocket) msg.obj;
                    for (SocketListener l : listeners) {
                        l.onClientSocketCreated(mcs, mcs.getSocket());
                    }
                    break;
            }
        }
    };


    /**
     * Allow other objects to react to events. Called on main thread.
     */
    public static interface SocketListener {
        void onServerAcceptedClientSocket(MyServerSocket mss, Socket socket);
        void onServerFinished(MyServerSocket mss);
        void onServerSocketListening(MyServerSocket mss, ServerSocket socket);
        void onMessageSent(MyConnectionSocket mcs, String msg);
        void onMessageReceived(MyConnectionSocket mcs, String msg);
        void onClientFinished(MyConnectionSocket mcs);
        void onClientSocketCreated(MyConnectionSocket mcs, Socket socket);
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
