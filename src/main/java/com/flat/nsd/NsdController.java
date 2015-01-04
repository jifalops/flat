package com.flat.nsd;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.flat.sockets.MyConnectionSocket;
import com.flat.sockets.MyServerSocket;
import com.flat.sockets.MySocketManager;
import com.flat.sockets.Sockets;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public class NsdController {
    private static final String TAG = NsdController.class.getSimpleName();

    private final NsdHelper nsdHelper;
    private final MySocketManager socketManager;

    public NsdController(Context ctx, String serviceName, NsdHelper.NsdServiceFilter filter) {
        nsdHelper = new NsdHelper(ctx, serviceName, filter);
        socketManager = new MySocketManager();
    }

    public NsdHelper getNsdHelper() {
        return nsdHelper;
    }

    public MySocketManager getSocketManager() {
        return socketManager;
    }

    public void enableNsd() {
        nsdHelper.registerListener(nsdListener);
        nsdHelper.initializeNsd();

        socketManager.registerListener(socketListener);
        socketManager.startServer();
    }

    public void disableNsd() {
        socketManager.stopServer();
        socketManager.stopConnections();
        nsdHelper.unregisterService();
        socketManager.unregisterListener(socketListener);
        nsdHelper.unregisterListener(nsdListener);
    }


    private final MySocketManager.SocketListener socketListener = new MySocketManager.SocketListener() {

        @Override
        public void onServerAcceptedClientSocket(MyServerSocket mss, Socket socket) {
            Log.i(TAG, "Server accepted socket to " + Sockets.toString(socket));
        }

        @Override
        public void onServerFinished(MyServerSocket mss) {
            Log.v(TAG, "Server on port " + mss.getPort() + " closed. It had accepted " + mss.getAcceptCount() + " sockets total.");
        }

        @Override
        public void onServerSocketListening(MyServerSocket mss, ServerSocket ss) {
            Log.v(TAG, "Server now listening on port " + ss.getLocalPort());
            nsdHelper.registerService(ss.getLocalPort());
        }

        @Override
        public void onMessageSent(MyConnectionSocket mcs, String msg) {
            Log.v(TAG, "Sent message to " + Sockets.toString(mcs.getSocket()));
        }

        @Override
        public void onMessageReceived(MyConnectionSocket mcs, String msg) {
            Log.v(TAG, "Received message from " + Sockets.toString(mcs.getSocket()));
        }

        @Override
        public void onClientFinished(MyConnectionSocket mcs) {
            Log.v(TAG, "Client finished: " + Sockets.toString(mcs.getAddress(), mcs.getPort()));
        }

        @Override
        public void onClientSocketCreated(MyConnectionSocket mcs, Socket socket) {
            Log.v(TAG, "Client socket created for " + Sockets.toString(socket));
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

//    /**
//     * Allow other objects to react to events.
//     */
//    public static abstract class NsdContollerListener implements NsdHelper.NsdHelperListener, MySocketManager.SocketListener {}
//    private final List<NsdContollerListener> listeners = new ArrayList<NsdContollerListener>(1);
//    public boolean registerListener(NsdContollerListener l) {
//        if (listeners.contains(l)) return false;
//        return listeners.add(l);
//    }
//    public boolean unregisterListener(NsdContollerListener l) {
//        return listeners.remove(l);
//    }
}
