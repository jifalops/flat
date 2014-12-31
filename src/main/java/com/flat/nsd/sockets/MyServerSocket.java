package com.flat.nsd.sockets;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages listening for an incoming socket connection by using a ServerSocket and
 * serverSocket.accept(). After accepting an incoming socket, the serverSocket is closed, the
 * state changes to connected (calling connection listeners), and then this instance is finished
 * (again calling finished listeners). If serverSocket.accept() fails, finish() will still be called.
 *
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class MyServerSocket implements ConnectionController {
    private static final String TAG = MyServerSocket.class.getSimpleName();


    private ServerSocket serverSocket;
    private synchronized void setServerSocket(ServerSocket ss) { serverSocket = ss; }
    public synchronized ServerSocket getServerSocket() {
        return serverSocket;
    }

    private Socket acceptedSocket;
    private synchronized void setAcceptedSocket(Socket s) { acceptedSocket = s; }
    public synchronized Socket getAcceptedSocket() {
        return acceptedSocket;
    }

    private int port;
    public void setPort(int p) { port = p; }
    public int getPort() { return port; }


    private boolean connected;
    @Override public synchronized boolean isConnected() { return connected; }
    private synchronized void setConnected(boolean conn) {
        connected = conn;
        if (conn) {
            for (MyServerSocketListener l : listeners) {
                l.onConnected(this);
            }
        }
    }

    private boolean canceled;
    @Override public synchronized boolean isCanceled() { return canceled; }
    @Override public synchronized void cancel() {
        canceled = true;
        closeServer();
    }

    private boolean finished;
    @Override public synchronized boolean isFinished() { return finished; }
    private synchronized void finish() {
        finished = true;
        closeServer();
        for (MyServerSocketListener l : listeners) {
            l.onFinished(this);
        }
    }

    private boolean closed;
    /** Closes ServerSocket only, not the accepted Socket */
    private synchronized void closeServer() {
        if (!closed && serverSocket != null) {
            try {
                serverSocket.close();
                setConnected(false);
                closed = true;
            } catch (IOException e) {
                Log.e(TAG, "Failed to close server socket.");
            }
        }
    }


    private Thread thread;
    private boolean started;
    @Override
    public synchronized void start() {
        if (!started && !isConnected() && !closed) {
            if (thread != null) {
                thread.interrupt();
            }
            thread = new Thread(new ServerThread());
            thread.start();
            started = true;
        }
    }

    private class ServerThread implements Runnable {
        @Override
        public void run() {
            try {
                if (!isConnected() && !closed) {
                    setServerSocket(new ServerSocket(port));
                    Log.i(TAG, "Server " + Sockets.toString(serverSocket) + " created, awaiting connection...");
                    setAcceptedSocket(serverSocket.accept());
                    Log.i(TAG, "Server " + Sockets.toString(serverSocket) + " is accepting connection to " + Sockets.toString(acceptedSocket));
                    closeServer();
                    setConnected(true);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error creating server socket");
            } finally {
                if (!isCanceled()) {
                    finish();
                }
            }
        }
    }




    /**
     * Allow other objects to react to events.
     */
    public static interface MyServerSocketListener {
        /**
         * Called on separate thread (this). Implementations should
         * return whether the connection was accepted or not.
         */
        void onConnected(MyServerSocket server);
        /** called on separate thread (this). */
        void onFinished(MyServerSocket server);
    }
    // a List of unique listener instances.
    private final List<MyServerSocketListener> listeners = new ArrayList<MyServerSocketListener>(1);
    public boolean registerListener(MyServerSocketListener l) {
        if (listeners.contains(l)) return false;
        listeners.add(l);
        return true;
    }
    public boolean unregisterListener(MyServerSocketListener l) {
        return listeners.remove(l);
    }
    public int unregisterListeners() {
        int size = listeners.size();
        listeners.clear();
        return size;
    }
}
