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

package com.flat.networkservicediscovery;

import android.app.Activity;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.flat.R;
import com.flat.localization.LocMan;
import com.flat.sockets.MyConnectionSocket;
import com.flat.sockets.MyServerSocket;
import com.flat.sockets.MySocketManager;
import com.flat.sockets.Sockets;

import java.net.ServerSocket;
import java.net.Socket;

public class NsdTestingActivity extends Activity {

    NsdHelper mNsdHelper;

    private TextView mStatusView;

    public static final String TAG = NsdTestingActivity.class.getSimpleName();

    private final MySocketManager socketManager = new MySocketManager();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nsd_activity);
        mStatusView = (TextView) findViewById(R.id.status);

        mNsdHelper = new NsdHelper(this, LocMan.NSD_SERVICE_PREFIX + LocMan.getInstance(this).getLocalNodeId(), new NsdHelper.NsdServiceFilter() {
            @Override
            public boolean isAcceptableService(NsdServiceInfo info) {
                return info.getServiceName().startsWith(LocMan.NSD_SERVICE_PREFIX);
            }
        });
        mNsdHelper.initializeNsd();

        socketManager.startServer();
    }

    @Override
    protected void onDestroy() {
        mNsdHelper.unregisterService();
        socketManager.stopServer();
        socketManager.stopConnections();
        super.onDestroy();
    }


    public void clickSend(View v) {
        EditText messageView = (EditText) this.findViewById(R.id.chatInput);
        if (messageView != null) {
            String messageString = messageView.getText().toString();
            if (!messageString.isEmpty()) {
                socketManager.send(messageString);
            }
            messageView.setText("");
        }
    }

    public void addChatLine(final String line) {
        mStatusView.append("\n" + line);
    }

    @Override
    protected void onPause() {
        if (mNsdHelper != null) {
            mNsdHelper.stopDiscovery();
            mNsdHelper.unregisterListener(nsdListener);
        }
        socketManager.unregisterListener(socketListener);
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        socketManager.registerListener(socketListener);
        if (mNsdHelper != null) {
            mNsdHelper.registerListener(nsdListener);
            mNsdHelper.discoverServices();
        }
    }




    private final MySocketManager.SocketListener socketListener = new MySocketManager.SocketListener() {

        @Override
        public void onServerAcceptedClientSocket(MyServerSocket mss, Socket socket) {
            Log.i(TAG, "Server accepted socket to " + Sockets.toString(socket));
            addChatLine("Server accepted socket to " + Sockets.toString(socket));
        }

        @Override
        public void onServerFinished(MyServerSocket mss) {
            Log.v(TAG, "Server on port " + mss.getPort() + " closed. It had accepted " + mss.getAcceptCount() + " sockets total.");
            addChatLine("Server on port " + mss.getPort() + " closed. It had accepted " + mss.getAcceptCount() + " sockets total.");
        }

        @Override
        public void onServerSocketListening(MyServerSocket mss, ServerSocket ss) {
            Log.v(TAG, "Server now listening on port " + ss.getLocalPort());
            addChatLine("Server now listening on port " + ss.getLocalPort());
            mNsdHelper.registerService(ss.getLocalPort());
        }

        @Override
        public void onMessageSent(MyConnectionSocket mcs, String msg) {
            Log.v(TAG, "Sent message to " + Sockets.toString(mcs.getSocket()));
            addChatLine("Sent message to " + Sockets.toString(mcs.getSocket()));
            addChatLine(msg);
        }

        @Override
        public void onMessageReceived(MyConnectionSocket mcs, String msg) {
            Log.v(TAG, "Received message from " + Sockets.toString(mcs.getSocket()));
            addChatLine("Received message from " + Sockets.toString(mcs.getSocket()));
            addChatLine(msg);
        }

        @Override
        public void onClientFinished(MyConnectionSocket mcs) {
            Log.v(TAG, "Client finished: " + Sockets.toString(mcs.getAddress(), mcs.getPort()));
            addChatLine("Client finished: " + Sockets.toString(mcs.getAddress(), mcs.getPort()));
        }

        @Override
        public void onClientSocketCreated(MyConnectionSocket mcs, Socket socket) {
            Log.v(TAG, "Client socket created for " + Sockets.toString(socket));
            addChatLine("Client socket created for " + Sockets.toString(socket));
        }
    };

    private final NsdHelper.NsdHelperListener nsdListener = new NsdHelper.NsdHelperListener() {
        @Override
        public void onServiceRegistered(NsdServiceInfo info) {

        }

        @Override
        public void onAcceptableServiceResolved(NsdServiceInfo info) {
            socketManager.startConnection(new MyConnectionSocket(info.getHost(), info.getPort()));
        }
    };
}
