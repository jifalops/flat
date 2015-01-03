package com.flat.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.net.nsd.NsdServiceInfo;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.flat.localization.Controller;
import com.flat.localization.Model;
import com.flat.localization.node.Node;
import com.flat.localization.coordinatesystem.LocationAlgorithm;
import com.flat.localization.signal.Signal;
import com.flat.nsd.NsdHelper;
import com.flat.nsd.sockets.MyConnectionSocket;
import com.flat.nsd.sockets.MyServerSocket;
import com.flat.nsd.sockets.MySocketManager;
import com.flat.nsd.sockets.Sockets;

import java.net.ServerSocket;
import java.net.Socket;

public class AppController extends Application {

	public static final String TAG = AppController.class.getSimpleName();

	private RequestQueue mRequestQueue;

	private static AppController sInstance;
    public static synchronized AppController getInstance() {
        return sInstance;
    }

    // Main power switch in AppServiceFragment
    private boolean enabled;

    private SharedPreferences prefs;

    private Controller controller;
    private Model model;

    private MySocketManager socketManager;
    private NsdHelper mNsdHelper;


	@Override
	public void onCreate() {
		super.onCreate();
		sInstance = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        controller = Controller.getInstance(this); // will populate the model.
        model = Model.getInstance();
        model.registerListener(modelListener); // TODO need to unregister?

        mNsdHelper = new NsdHelper(this, nsdCallbacks);
        socketManager = MySocketManager.getInstance();
    }

	public RequestQueue getRequestQueue() {
		if (mRequestQueue == null) {
			mRequestQueue = Volley.newRequestQueue(getApplicationContext());
		}
		return mRequestQueue;
	}

	public <T> void addToRequestQueue(Request<T> req, String tag) {
		// set the default tag if tag is empty
		req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
		getRequestQueue().add(req);
	}

	public <T> void addToRequestQueue(Request<T> req) {
		req.setTag(TAG);
		getRequestQueue().add(req);
	}

	public void cancelPendingRequests(Object tag) {
		if (mRequestQueue != null) {
			mRequestQueue.cancelAll(tag);
		}
	}

    public void enableNsd() {
        mNsdHelper.initializeNsd();
        socketManager.registerListener(socketListener);
        socketManager.startServer();
    }

    public void disableNsd() {
        socketManager.stopServer();
        socketManager.stopConnections();
        mNsdHelper.unregisterService();
        socketManager.unregisterListener(socketListener);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        if (enabled) {
            enableNsd();
            for (Signal s : model.getSignals()) {
                if (prefs.getBoolean(s.getName(), false)) {
                    s.enable(this);
                }
            }
            for (LocationAlgorithm la : model.getAlgorithms()) {
                if (prefs.getBoolean(la.getName(), false)) {
                    la.setEnabled(true);
                }
            }
        } else {
            disableNsd();
            for (Signal s : model.getSignals()) {
                s.disable(this);
            }
            for (LocationAlgorithm la : model.getAlgorithms()) {
                la.setEnabled(false);
            }
        }
    }

    private final Model.ModelListener modelListener = new Model.ModelListener() {
        @Override
        public void onNodeAdded(Node n) {
            n.readPrefs(prefs);
        }
    };


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
            mNsdHelper.registerService(ss.getLocalPort());
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

    private final NsdHelper.Callbacks nsdCallbacks = new NsdHelper.Callbacks() {
        @Override
        public void onAcceptableServiceResolved(NsdServiceInfo info) {
            socketManager.startConnection(new MyConnectionSocket(info.getHost(), info.getPort()));
        }
    };
}
