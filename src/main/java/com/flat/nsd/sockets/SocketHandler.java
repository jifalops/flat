package com.flat.nsd.sockets;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public class SocketHandler {
    private static final String TAG = SocketHandler.class.getSimpleName();

    private ServerSocket mServerSocket;
    private synchronized void setServerSocket(ServerSocket ss) {
        setServerPort(ss.getLocalPort());
        mServerSocket = ss;
        for (ConnectionListener l : listeners) {
            l.onListening(ss);
        }
    }
    private synchronized ServerSocket getServerSocket() {
        return mServerSocket;
    }


    private final List<SocketConnection> mConnections = Collections.synchronizedList(new ArrayList<SocketConnection>());
    private void addConnection(Socket client) {
        SocketConnection conn = new SocketConnection(client);
        mConnections.add(conn);
        conn.start();
        for (ConnectionListener l : listeners) {
            l.onConnected(conn);
        }
    }
    public void addConnection(InetAddress address, int port) {
        SocketConnection conn = new SocketConnection(address, port);
        mConnections.add(conn);
        conn.start();
        for (ConnectionListener l : listeners) {
            l.onConnected(conn);
        }
    }

    private int mServerPort;
    public void setServerPort(int port) {
        mServerPort = port;
    }
    public int getServerPort() {
        return mServerPort;
    }

    private int mServerAcceptCount;
    public int getServerAcceptCount() { return mServerAcceptCount; }

    private int mServerAcceptLimit = Integer.MAX_VALUE;
    public void setServerAcceptLimit(int limit) { mServerAcceptLimit = limit; }
    public int getServerAcceptLimit() { return mServerAcceptLimit; }

    private Thread mServerThread;

    public void startServer() {
        if (mServerThread != null) {
            mServerThread.interrupt();
        }
        mServerThread = new Thread(new ServerThread());
        mServerThread.start();
    }

    public void stopServer() {
        if (mServerThread != null) {
            mServerThread.interrupt();
        }
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close server socket.");
            }
        }
    }

    public void send(String msg) {
        for (SocketConnection conn : mConnections) {
            conn.queue.add(msg);
        }
    }


    private class ServerThread implements Runnable {
        @Override
        public void run() {
            Socket client;
            try {
                setServerSocket(new ServerSocket(mServerPort));
            } catch (IOException e) {
                Log.e(TAG, "Error creating server socket");
            }
            try {
                while (mServerAcceptCount < mServerAcceptLimit && !Thread.currentThread().isInterrupted()) {
                    client = mServerSocket.accept();
                    ++mServerAcceptCount;
                    addConnection(client);
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed to accept socket on port "+mServerPort+" for client #" + (mServerAcceptCount+1));
            }
        }
    }


    public class SocketConnection {
        private Socket connection;
        private final InetAddress address;
        private final int port;
        private Thread sendThread;
        private Thread receiveThread;
        private final BlockingQueue<String> queue = new ArrayBlockingQueue<String>(10);

        public SocketConnection(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }

        public SocketConnection(Socket client) {
            this(client.getInetAddress(), client.getPort());
            connection = client;
        }

        public Socket getSocket() {
            return connection;
        }
        public InetAddress getAddress() {
            return address;
        }
        public int getPort() {
            return port;
        }

        public void start() {
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
            if (sendThread != null) {
                sendThread.interrupt();
            }
            if (receiveThread != null) {
                receiveThread.interrupt();
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close connection socket.");
                }
            }
        }

        private void sendMessage(String msg) {
            try {
                if (connection == null) {
                    Log.d(TAG, "Socket is null, wtf?");
                } else if (connection.getOutputStream() == null) {
                    Log.d(TAG, "Socket output stream is null, wtf?");
                }

                PrintWriter out = new PrintWriter(
                        new BufferedWriter(
                                new OutputStreamWriter(connection.getOutputStream())), true);
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



        private class SendThread implements Runnable {
            @Override
            public void run() {
                try {
                    if (connection == null || connection.isClosed()) {
                        Log.d(TAG, "Socket is null or closed, creating another at " + address.getHostAddress() + ":" + port);
                        connection = new Socket(address, port);
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
                        String msg = queue.take();
                        sendMessage(msg);
                    } catch (InterruptedException ie) {
                        Log.d(TAG, "Message sending loop interrupted, exiting");
                    }
                }
            }
        }


        private class ReceiveThread implements Runnable {
            @Override
            public void run() {
                BufferedReader input;
                try {
                    input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String msg;
                    while (!Thread.currentThread().isInterrupted()) {
                        msg = input.readLine();
                        if (msg != null) {
                            for (ConnectionListener l : listeners) {
                                l.onMessageReceived(SocketConnection.this, msg);
                            }
                        } else {
                            Log.e(TAG, "Null message for " + Sockets.toString(connection) + ", exiting.");
                            break;
                        }
                    }
                    input.close();

                } catch (IOException e) {
                    Log.d(TAG, "Error running receive thread, " + e.getMessage());
                }
            }
        }
    }

    /**
     * Allow other objects to react to events.
     */
    public static interface ConnectionListener {
        /** called on server thread */
        void onListening(ServerSocket ss);
        /** called on server thread */
        void onConnected(SocketConnection conn);
        /** called on send thread. */
        void onMessageSent(SocketConnection conn, String msg);
        /** called on receive thread. */
        void onMessageReceived(SocketConnection conn, String msg);
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




    /** Assumes the serviceName contains an IP */
    public int countConnectionsTo(String serviceName) {
        int count = 0;
        synchronized (mConnections) {
            for (SocketConnection conn : mConnections) {
                if (serviceName.contains(conn.getAddress().getHostAddress())) {
                    ++count;
                }
            }
        }
        Log.d(TAG, count + " connections to " + serviceName);
        return count;
    }
}
