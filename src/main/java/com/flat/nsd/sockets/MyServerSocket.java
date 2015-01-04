package com.flat.nsd.sockets;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages listening for an incoming socket connection by using a ServerSocket and
 * serverSocket.accept(). After accepting an incoming socket, onClientSocketAccepted() is called
 * on the registered listeners. When the end of the thread is reached (from error or by calling stop()),
 * onFinished() is called on the registered listeners.
 *
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class MyServerSocket {
    private static final String TAG = MyServerSocket.class.getSimpleName();

    private Socket acceptedSocket;
    public synchronized Socket getAcceptedSocket() { return acceptedSocket; }
    public synchronized void setAcceptedSocket(Socket socket) {
        acceptedSocket = socket;
        for (ServerListener l : listeners) {
            l.onServerAcceptedClientSocket(this, socket);
        }
    }

    private ServerSocket serverSocket;
    private synchronized void setServerSocket(ServerSocket ss) {
        serverSocket = ss;
        port = ss.getLocalPort();
        for (ServerListener l : listeners) {
            l.onServerSocketListening(this, ss);
        }
    }
    public synchronized ServerSocket getServerSocket() {
        return serverSocket;
    }



    private boolean finished;
    public synchronized boolean isFinished() { return finished; }
    private synchronized void finish() {
        finished = true;
        stop();
        for (ServerListener l : listeners) {
            l.onFinished(this);
        }
    }

    private int port;
    public int getPort() { return port; }

    private Thread thread;


    public void start() {
        start(port);
    }
    public void start(int port) {
        stop();
        this.port = port;
        thread = new Thread(new ServerThread());
        thread.start();
    }
    public void stop() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close server socket.");
            }
        }
    }

    private int acceptCount;
    public int getAcceptCount() { return acceptCount; }

    private int acceptLimit = Integer.MAX_VALUE;
    public void setAcceptLimit(int limit) { acceptLimit = limit; }
    public int getAcceptLimit() { return acceptLimit; }


    private class ServerThread implements Runnable {
        @Override
        public void run() {
            Socket client;
            try {
                setServerSocket(new ServerSocket(port));
            } catch (IOException e) {
                Log.e(TAG, "Error creating server socket");
            }
            try {
                while (acceptCount < acceptLimit && !Thread.currentThread().isInterrupted()) {
                    client = getServerSocket().accept();
                    ++acceptCount;
                    setAcceptedSocket(client);
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed to accept socket on port " + port + " for client #" + (acceptCount + 1));
            }
            finish();
        }
    }


    /**
     * Allow other objects to react to events.
     */
    public static interface ServerListener {
        /** called on server thread */
        void onServerAcceptedClientSocket(MyServerSocket mss, Socket socket);
        /** called on server thread */
        void onFinished(MyServerSocket mss);
        /** called on server thread */
        void onServerSocketListening(MyServerSocket mss, ServerSocket ss);
    }
    // a List of unique listener instances.
    private final List<ServerListener> listeners = new ArrayList<ServerListener>(1);
    public boolean registerListener(ServerListener l) {
        if (listeners.contains(l)) return false;
        listeners.add(l);
        return true;
    }
    public boolean unregisterListener(ServerListener l) {
        return listeners.remove(l);
    }
}
