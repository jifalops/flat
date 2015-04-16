package com.flat.aa;

import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.flat.localization.node.NodeMessage;
import com.flat.localization.node.RemoteNode;
import com.flat.networkservicediscovery.NsdController;
import com.flat.sockets.MyConnectionSocket;
import com.flat.sockets.MyServerSocket;

import org.json.JSONException;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Jacob Phillips.
 */
public class NsdEventHandler extends NsdController.NsdContollerListener {
    static final String TAG = NsdEventHandler.class.getSimpleName();


    NodeManager nodeManager = NodeManager.getInstance();


    @Override
    public void onServiceRegistered(NsdServiceInfo info) {

    }

    @Override
    public void onAcceptableServiceResolved(NsdServiceInfo info) {

    }

    @Override
    public void onServerAcceptedClientSocket(MyServerSocket mss, Socket socket) {
        handleNewConnection(socket);
    }

    @Override
    public void onServerFinished(MyServerSocket mss) {

    }

    @Override
    public void onServerSocketListening(MyServerSocket mss, ServerSocket socket) {

    }

    @Override
    public void onMessageSent(MyConnectionSocket mcs, String msg) {

    }

    @Override
    public void onMessageReceived(MyConnectionSocket mcs, String msg) {
        try {
            handleReceivedMessage(mcs, NodeMessage.from(msg));
        } catch (JSONException e) {
            Log.e(TAG, "JSON exception converting received message.", e);
        }
    }

    @Override
    public void onClientFinished(MyConnectionSocket mcs) {
        Log.v(TAG, "lost connection to " + mcs.getAddress().getHostAddress());
        Node n = nodeManager.getNodeByConnection(mcs);
        if (n != null) n.setConnection(null);
    }

    @Override
    public void onClientSocketCreated(MyConnectionSocket mcs, Socket socket) {
        handleNewConnection(socket);
    }

    void handleNewConnection(Socket socket) {

    }

    void handleReceivedMessage(MyConnectionSocket conn, NodeMessage msg) {

    }
}
