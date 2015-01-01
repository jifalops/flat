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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.flat.R;
import com.flat.nsd.sockets.SocketHandler;

import java.net.ServerSocket;

public class NsdChatActivity extends Activity {

    NsdHelper mNsdHelper;

    private TextView mStatusView;
//    private final MySocketManager.SocketListener socketListener = new MySocketManager.SocketListener() {
//        @Override
//        public void onServerConnected(MyServerSocket server, Socket socket) {
//
//        }
//
//        @Override
//        public void onServerFinished(MyServerSocket server) {
//
//        }
//
//        @Override
//        public void onNewServerSocket(MyServerSocket mss, ServerSocket ss) {
//
//        }
//
//        @Override
//        public void onMessageSent(MyConnectionSocket socket, String msg) {
//
//        }
//
//        @Override
//        public void onMessageReceived(MyConnectionSocket socket, String msg) {
//            addChatLine(msg);
//        }
//
//        @Override
//        public void onClientFinished(MyConnectionSocket socket) {
//
//        }
//
//        @Override
//        public void onClientConnected(MyConnectionSocket mcs, Socket socket) {
//            addChatLine(socket.getInetAddress().getHostAddress() + " has joined.");
//        }
//    };

    private final SocketHandler.ConnectionListener connectionListener = new SocketHandler.ConnectionListener() {
        @Override
        public void onListening(ServerSocket ss) {
            addChatLine("Listening on port " + ss.getLocalPort());
        }

        @Override
        public void onConnected(SocketHandler.SocketConnection conn) {
            addChatLine("Connected to " + conn.getAddress()+":"+conn.getPort());
        }

        @Override
        public void onMessageSent(SocketHandler.SocketConnection conn, String msg) {
            addChatLine("@" + conn.getAddress() + ": " + msg);
        }

        @Override
        public void onMessageReceived(SocketHandler.SocketConnection conn, String msg) {
            addChatLine(conn.getAddress() + ": " + msg);
        }
    };

    public static final String TAG = "NsdChat";

    //ChatConnection mRegistrationConnection;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nsd_activity);
        mStatusView = (TextView) findViewById(R.id.status);

        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeNsd();
        mNsdHelper.start();
    }

    @Override
    protected void onDestroy() {
        mNsdHelper.stop();
        super.onDestroy();
    }




    public void clickAdvertise(View v) {
//        advertise();
    }

    public void clickDiscover(View v) {
        mNsdHelper.discoverServices();
    }

    public void clickConnect(View v) {
//        NsdServiceInfo service = mNsdHelper.getChosenServiceInfo();
//        if (service != null) {
//            Log.d(TAG, "Connecting...");
//            mConnection.connectToServer(service.getHost(),
//                    service.getPort());
//        } else {
//            Log.d(TAG, "No service to connect to!");
//        }
//        mNsdHelper.retryConnections();
    }

    public void clickSend(View v) {
        EditText messageView = (EditText) this.findViewById(R.id.chatInput);
        if (messageView != null) {
            String messageString = messageView.getText().toString();
            if (!messageString.isEmpty()) {
                mNsdHelper.send(messageString);
            }
            messageView.setText("");
        }
    }

    public void addChatLine(final String line) {
        mStatusView.post(new Runnable() {
            @Override
            public void run() {
                mStatusView.append("\n" + line);
            }
        });
    }

    @Override
    protected void onPause() {
        if (mNsdHelper != null) {
            mNsdHelper.stopDiscovery();
        }
        //MySocketManager.getInstance().unregisterListener(socketListener);
        mNsdHelper.getSocketHandler().unregisterListener(connectionListener);
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
       // MySocketManager.getInstance().registerListener(socketListener);
        mNsdHelper.getSocketHandler().registerListener(connectionListener);
        if (mNsdHelper != null) {
            mNsdHelper.discoverServices();
        }
    }
}
