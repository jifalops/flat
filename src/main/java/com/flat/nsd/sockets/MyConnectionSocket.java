package com.flat.nsd.sockets;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * This class uses two threads (send and receive) to communicate over a socket. If either thread
 * fails the socket will be closed and the instance of this class will be finished. Calling
 * cancel() on this class will interrupt the two threads as well as close the socket, but finish()
 * will not be called.
 *
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class MyConnectionSocket implements SocketController {
    private static final String TAG = MyConnectionSocket.class.getSimpleName();


    private Socket socket;
    private synchronized void setSocket(Socket s) {
        socket = s;
        setConnected(true);
    }
    public synchronized Socket getSocket() {
        return socket;
    }

    // TODO check state of threads instead of member var
    private boolean connected;
    @Override public synchronized boolean isConnected() { return connected; }
    private synchronized void setConnected(boolean conn) {
        connected = conn;
        if (conn) {
            for (ConnectionListener l : listeners) {
                l.onConnected(this, getSocket());
            }
        }
    }

    private boolean canceled;
    @Override public synchronized boolean isCanceled() { return canceled; }
    @Override public synchronized void cancel() {
        canceled = true;
        close();
    }

    private boolean finished;
    @Override public synchronized boolean isFinished() { return finished; }
    private synchronized void finish() {
        finished = true;
        close();
        for (ConnectionListener l : listeners) {
            l.onFinished(this);
        }
    }

    private boolean closed;
    private synchronized void close() {
        if (!closed && socket != null) {
            try {
                receiveThread.interrupt();
                sendThread.interrupt();
                socket.close();
                setConnected(false);
                closed = true;
            } catch (IOException e) {
                Log.e(TAG, "Failed to close socket.");
            }
        }
    }

    private final InetAddress address;
    public InetAddress getAddress() { return address; }

    private final int port;
    public int getPort() { return port; }

    public MyConnectionSocket(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }


    private Thread sendThread;
    private Thread receiveThread;
    private boolean started;
    @Override
    public synchronized void start() {
        if (!started && !isConnected() && !closed) {
            if (sendThread != null) {
                sendThread.interrupt();
            }
            if (receiveThread != null) {
                receiveThread.interrupt();
            }
            sendThread = new Thread(new SendingThread());
            sendThread.start();
            started = true;
        }
    }

    public void sendMessage(String msg) {
        try {
            if (socket == null) {
                Log.d(TAG, "Socket is null, wtf?");
            } else if (socket.getOutputStream() == null) {
                Log.d(TAG, "Socket output stream is null, wtf?");
            }

            PrintWriter out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())), true);
            out.println(msg);
            out.flush();

            for (ConnectionListener l : listeners) {
                l.onMessageSent(this, msg);
            }
        } catch (UnknownHostException e) {
            Log.d(TAG, "Unknown Host");
        } catch (IOException e) {
            Log.d(TAG, "I/O Exception");
        } catch (Exception e) {
            Log.d(TAG, "Exception during sendMessage()");
        }
        Log.v(TAG, "Sent message: " + msg);
    }

    private class SendingThread implements Runnable {
        private final String TAG = SendingThread.class.getSimpleName();
        final BlockingQueue<String> queue = new ArrayBlockingQueue<String>(10);

        @Override
        public void run() {
            try {
                if (getSocket() == null) {
                    Log.d(TAG, "Socket is null, creating another at " + address.getHostAddress()+":"+port);
                    setSocket(new Socket(address, port));
                }

                // will use the socket just created
                receiveThread = new Thread(new ReceivingThread());
                receiveThread.start();

            } catch (UnknownHostException e) {
                Log.e(TAG, "Initializing socket failed, UHE" + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "Initializing socket failed, IOE." + e.getMessage());
            }

            while (!closed && !Thread.currentThread().isInterrupted()) {
                try {
                    String msg = queue.take();
                    sendMessage(msg);
                } catch (InterruptedException ie) {
                    Log.d(TAG, "Message sending loop interrupted, exiting");
                }
            }

            if (!isCanceled()) {
                finish();
            }
        }
    }

    private class ReceivingThread implements Runnable {
        private final String TAG = ReceivingThread.class.getSimpleName();

        @Override
        public void run() {
            BufferedReader input;
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String msg;
                while (!closed && !Thread.currentThread().isInterrupted()) {
                    msg = input.readLine();
                    if (msg != null) {
                        for (ConnectionListener l : listeners) {
                            l.onMessageReceived(MyConnectionSocket.this, msg);
                        }
                    } else {
                        Log.e(TAG, "Null message for " + Sockets.toString(socket) + ", exiting.");
                        break;
                    }
                }
                input.close();

            } catch (IOException e) {
                Log.d(TAG, "Error running receive thread, " + e.getMessage());
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
    public static interface ConnectionListener {
        /** called on server thread */
        void onConnected(MyConnectionSocket mcs, Socket socket);
        /** called on send thread. */
        void onMessageSent(MyConnectionSocket mcs, String msg);
        /** called on receive thread. */
        void onMessageReceived(MyConnectionSocket mcs, String msg);
        /** called on send or receive thread. */
        void onFinished(MyConnectionSocket socket);
    }
    // a List of unique listener instances.
    private final List<ConnectionListener> listeners = new ArrayList<ConnectionListener>(1);
    public boolean registerListener(ConnectionListener l) {
        if (listeners.contains(l)) return false;
        listeners.add(l);
        return true;
    }
    public boolean unregisterListener(ConnectionListener l) {
        return listeners.remove(l);
    }
    public int unregisterListeners() {
        int size = listeners.size();
        listeners.clear();
        return size;
    }
}
