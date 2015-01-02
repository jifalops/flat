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
 * fails the socket will be closed and the instance of this class will be finished. An instance of
 * this class can be used multiple times, but it is restricted to the address and port it was
 * originally created for.
 *
 * @author Jacob Phillips (12/2014, jphilli85 at gmail)
 */
public class MyConnectionSocket {
    private static final String TAG = MyConnectionSocket.class.getSimpleName();


    private Socket socket;
    private final InetAddress address;
    private final int port;
    private Thread sendThread;
    private Thread receiveThread;
    private final BlockingQueue<String> sendQueue = new ArrayBlockingQueue<String>(10);

    public MyConnectionSocket(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public MyConnectionSocket(Socket client) {
        this(client.getInetAddress(), client.getPort());
        socket = client;
    }

    private void setSocket(Socket s) {
        socket = s;
        for (ConnectionListener l : listeners) {
            l.onSocketCreated(this, s);
        }
    }

    public Socket getSocket() {
        return socket;
    }
    public InetAddress getAddress() {
        return address;
    }
    public int getPort() {
        return port;
    }

    private boolean shouldFinish;
    public void start() {
        finished = false;
        shouldFinish = true;
        if (sendThread != null) {
            sendThread.interrupt();
        }
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        sendThread = new Thread(new SendThread());
        sendThread.start();
    }

    public void stop() {
        shouldFinish = false;
        if (sendThread != null) {
            sendThread.interrupt();
            sendThread = null;
        }
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close connection socket.");
            }
        }
    }

    public boolean send(String msg) {
        if (sendThread != null) {
            try {
                sendQueue.add(msg);
                return true;
            }
            catch (IllegalStateException ignored) {}
        }
        return false;
    }

    private boolean finished;
    public synchronized boolean isFinished() { return finished; }
    private synchronized void finish() {
        if (finished || !shouldFinish) return;
        finished = true;
        stop();
        for (ConnectionListener l : listeners) {
            l.onFinished(this);
        }
    }

    private class SendThread implements Runnable {
        @Override
        public void run() {
            try {
                if (socket == null || socket.isClosed()) {
                    Log.d(TAG, "Socket is null or closed, creating another at " + address.getHostAddress() + ":" + port);
                    setSocket(new Socket(address, port));
                }

                // will use the socket just created
                receiveThread = new Thread(new ReceiveThread());
                receiveThread.start();

            } catch (UnknownHostException e) {
                Log.e(TAG, "Initializing socket failed, UHE" + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "Initializing socket failed, IOE." + e.getMessage());
            }

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String msg = sendQueue.take();
                    sendMessage(msg);
                } catch (InterruptedException ie) {
                    Log.d(TAG, "Message sending loop interrupted, exiting");
                }
            }
            finish();
        }

        private void sendMessage(String msg) {
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
                    l.onMessageSent(MyConnectionSocket.this, msg);
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
    }


    private class ReceiveThread implements Runnable {
        @Override
        public void run() {
            BufferedReader input;
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String msg;
                while (!Thread.currentThread().isInterrupted()) {
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
            }
            finish();
        }
    }




    /**
     * Allow other objects to react to events.
     */
    public static interface ConnectionListener {
        /** called on send thread */
        void onSocketCreated(MyConnectionSocket mcs, Socket socket);
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
}
