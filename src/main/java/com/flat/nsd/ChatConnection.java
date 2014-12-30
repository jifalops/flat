/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flat.nsd;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class  ChatConnection {

    private Handler mUpdateHandler;
    private ChatServer mChatServer;
    private ChatClient mChatClient;

//    private InetAddress address;
//    private int serverPort;

    private static final String TAG = ChatConnection.class.getSimpleName();

    private Socket mConnectionSocket;
    private AtomicInteger mPort = new AtomicInteger(-1);
    private NsdHelper mHelper;

    public ChatConnection(NsdHelper helper, Handler handler) {
        mHelper = helper;
        mUpdateHandler = handler;
        mChatServer = new ChatServer(handler);
    }

    public void tearDown() {
        if (mChatServer != null) mChatServer.tearDown();
        if (mChatClient != null) mChatClient.tearDown();
    }

    public void connectToServer(InetAddress address, int port) {
        if (mChatClient != null) {
            Log.v(TAG, "mChatClient is not null.");
        }
//        this.address = address;
//        this.serverPort = port;
        mChatClient = new ChatClient(address, port);
    }

    public void sendMessage(String msg) {
        if (mChatClient != null) {
            mChatClient.sendMessage(msg);
        } else {
            Log.d(TAG, "Cannot send message to null client, server has not accepted connection.");
        }
    }
    
    public int getLocalPort() {
        return mPort.get();
    }
    
    public void setLocalPort(int port) {
        mPort.set(port);
    }
    

    public synchronized void updateMessages(String msg, boolean local) {
        Log.v(TAG, "Updating message: " + msg);

        if (local) {
            msg = "me: " + msg;
        } else {
            msg = toString() + ": " + msg;
        }

        Bundle messageBundle = new Bundle();
        messageBundle.putString("msg", msg);

        Message message = new Message();
        message.setData(messageBundle);
        mUpdateHandler.sendMessage(message);

    }

    private synchronized void setServerSocket(Socket socket) {
        if (socket == null) {
            Log.e(TAG, "setServerSocket() being called on null socket, exiting.");
            return;
        } else {
            if (mConnectionSocket == null) {
                Log.v(TAG, "mServerSocket is null");
            } else if (mConnectionSocket.isConnected()) {
                Log.d(TAG, "server socket is connected, closing...");
                try {
                    mConnectionSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing chat connection (server socket).");
                }
            }
        }
        Log.i(TAG, "setServerSocket() being called on " + getSocketInfo(socket));
        mConnectionSocket = socket;
    }

    private synchronized void setClientSocket(Socket socket) {
        if (socket == null) {
            Log.e(TAG, "setClientSocket() being called on null socket, exiting.");
            return;
        } else {
            if (mConnectionSocket == null) {
                Log.v(TAG, "mClientSocket is null");
            } else if (mConnectionSocket.isConnected()) {
                Log.d(TAG, "client socket is connected, closing...");
                try {
                    mConnectionSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing chat connection (client socket).");
                }
            }
        }
        Log.i(TAG, "setClientSocket() being called on " + getSocketInfo(socket));
        mConnectionSocket = socket;
    }


    private synchronized Socket getServerSocket() {
        return mConnectionSocket;
    }
    private synchronized Socket getClientSocket() {
        return mConnectionSocket;
    }

    private class ChatServer {
        ServerSocket socket = null;
        Thread thread = null;

        public ChatServer(Handler handler) {
            thread = new Thread(new ServerThread());
            thread.start();
        }

        public void tearDown() {
            if (thread != null) thread.interrupt();
            if (mChatServer == null) {
                Log.d(TAG, "Attempted to tearDown null chat server");
                return;
            }
            try {
                socket.close();
            } catch (IOException ioe) {
                Log.e(TAG, "Error when closing server socket.");
            }
        }

        class ServerThread implements Runnable {

            @Override
            public void run() {

                try {
                    // Since discovery will happen via Nsd, we don't need to care which port is
                    // used.  Just grab an available one  and advertise it via Nsd.
                    socket = new ServerSocket(0);
                    setLocalPort(socket.getLocalPort());
                    
                    while (!Thread.currentThread().isInterrupted()) {
                        Log.i(TAG, "ServerSocket Created, awaiting connection. " + getSocketInfo(socket, ChatServer.this, ServerThread.this));
                        setServerSocket(socket.accept());
                        Log.e(TAG, "Server connected to " + getSocketInfo(getServerSocket(), ChatServer.this, ServerThread.this));
                        if (mChatClient == null) {
                            int port = mConnectionSocket.getPort();
                            InetAddress address = mConnectionSocket.getInetAddress();
                            connectToServer(address, port);
                        }
                        mHelper.initializeAdvertisingConnection();
                        break; // TODO since the example used only one ChatConnection instance, this loop may not be needed (each instance gets one ChatServer and one ChatClient
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error creating ServerSocket");
                    //e.printStackTrace();
                }
            }
        }
    }

    private class ChatClient {

        private InetAddress mAddress;
        private int PORT;

        private final String CLIENT_TAG = "ChatClient";

        private Thread mSendThread;
        private Thread mRecThread;

        public ChatClient(InetAddress address, int port) {

            Log.d(CLIENT_TAG, "Creating chatClient");
            this.mAddress = address;
            this.PORT = port;

            mSendThread = new Thread(new SendingThread());
            mSendThread.start();
        }

        class SendingThread implements Runnable {

            BlockingQueue<String> mMessageQueue;
            private int QUEUE_CAPACITY = 10;

            public SendingThread() {
                mMessageQueue = new ArrayBlockingQueue<String>(QUEUE_CAPACITY);
            }

            @Override
            public void run() {
                try {
                    if (getClientSocket() == null) {
                        setClientSocket(new Socket(mAddress, PORT));
                        Log.d(CLIENT_TAG, "Client-side socket initialized.");
                        Log.e(CLIENT_TAG, "Client connection established to " + getSocketInfo(getClientSocket(), ChatClient.this, SendingThread.this));

                    } else {
                        Log.d(CLIENT_TAG, "Socket already initialized, skipping.");
                    }


                    mRecThread = new Thread(new ReceivingThread());
                    mRecThread.start();

                } catch (UnknownHostException e) {
                    Log.e(CLIENT_TAG, "Initializing socket failed, UHE" + e.getMessage());
                } catch (IOException e) {
                    Log.e(CLIENT_TAG, "Initializing socket failed, IOE." + e.getMessage());
                }

                while (true) {
                    try {
                        String msg = mMessageQueue.take();
                        sendMessage(msg);
                    } catch (InterruptedException ie) {
                        Log.d(CLIENT_TAG, "Message sending loop interrupted, exiting");
                    }
                }
            }
        }

        /**
         * The tricky thread
         */
        class ReceivingThread implements Runnable {

            @Override
            public void run() {

                BufferedReader input;
                try {
                    input = new BufferedReader(new InputStreamReader(
                            mConnectionSocket.getInputStream()));
                    while (!Thread.currentThread().isInterrupted()) {

                        String messageStr = null;
                        messageStr = input.readLine();
                        if (messageStr != null) {
                            Log.d(CLIENT_TAG, "Msg: " + messageStr + "\nfrom " + getSocketInfo(getClientSocket(), ChatClient.this, ReceivingThread.this));
                            updateMessages(messageStr, false);
                        } else {
                            Log.e(CLIENT_TAG, "Null message for " + getSocketInfo(getClientSocket(), ChatClient.this, ReceivingThread.this));
                            break;
                        }
                    }
                    input.close();

                } catch (IOException e) {
                    Log.e(CLIENT_TAG, "Server loop error: " + e.getMessage());
                }
            }
        }

        public void tearDown() {
            if (getClientSocket() != null) {
                try {
                    getClientSocket().close();
                } catch (IOException ioe) {
                    Log.e(CLIENT_TAG, "Error when closing client socket.");
                }
            }
        }

        public void sendMessage(String msg) {
            try {
                Socket socket = getClientSocket();
                if (socket == null) {
                    Log.d(CLIENT_TAG, "Socket is null, wtf?");
                } else if (socket.getOutputStream() == null) {
                    Log.d(CLIENT_TAG, "Socket output stream is null, wtf?");
                }

                PrintWriter out = new PrintWriter(
                        new BufferedWriter(
                                new OutputStreamWriter(getClientSocket().getOutputStream())), true);
                out.println(msg);
                out.flush();
                updateMessages(msg, true);
            } catch (UnknownHostException e) {
                Log.d(CLIENT_TAG, "Unknown Host");
            } catch (IOException e) {
                Log.d(CLIENT_TAG, "I/O Exception");
            } catch (Exception e) {
                Log.d(CLIENT_TAG, "Exception during sendMessage()");
            }
            Log.v(CLIENT_TAG, "Client sent message: " + msg);
        }
    }

    public String getSocketInfo(Socket socket, Object... objects) {
        String s = "{no socket info}";
        try {
            s = socket.getInetAddress().getHostAddress()+":"+socket.getPort()+" (local "+socket.getLocalPort()+").\n"
                    + ChatConnection.this.toString() + "\n";
            s += TextUtils.join("\n", objects);
        } catch (NullPointerException ignored) {}
        return s;
    }
    public String getSocketInfo(ServerSocket socket, Object... objects) {
        String s = "{no socket info}";
        try {
            s = socket.getInetAddress().getHostAddress()+":"+socket.getLocalPort()+" (local).\n"
                    + ChatConnection.this.toString() + "\n";
            s += TextUtils.join("\n", objects);
        } catch (NullPointerException ignored) {}
        return s;
    }
}
