package com.jphilli85.wifirecorder.service;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;

import com.jphilli85.wifirecorder.util.Jog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Jake on 8/29/13.
 */
public final class BluetoothService extends PersistentIntentService {
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_DISCOVERABLE = 2;

    private static final String SERVICE_NAME = "Essential Localization Bluetooth Service";
    private static final UUID SERVICE_UUID = UUID.fromString(BluetoothService.class.getName());

    private BluetoothAdapter mAdapter;
    private List<BluetoothDevice> mDiscoveredDevices;

    @Override
    public void onCreate() {
        super.onCreate();

        mDiscoveredDevices = new ArrayList<BluetoothDevice>();
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            Jog.a("Device does not support Bluetooth", this, true);
        }

//        mFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
//        mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//        mFilter.addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
//        mFilter.addAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//        mFilter.addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);


        mFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        mFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mFilter.addAction(BluetoothDevice.ACTION_FOUND);

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);

            String s;
            switch (state) {
                case BluetoothAdapter.STATE_CONNECTED:      s = "connected";        break;
                case BluetoothAdapter.STATE_CONNECTING:     s = "connecting";       break;
                case BluetoothAdapter.STATE_DISCONNECTED:   s = "disconnected";     break;
                case BluetoothAdapter.STATE_DISCONNECTING:  s = "disconnecting";    break;
                case BluetoothAdapter.STATE_OFF:            s = "off";              break;
                case BluetoothAdapter.STATE_ON:             s = "on";               break;
                case BluetoothAdapter.STATE_TURNING_OFF:    s = "turning off";      break;
                case BluetoothAdapter.STATE_TURNING_ON:     s = "turning on";       break;
                default:                                    s = "unknown";          break;
            }
            Jog.v("BT state: " + s, this);

        } else if (action.equals(BluetoothDevice.ACTION_FOUND)) {

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            BluetoothClass btClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
            mDiscoveredDevices.add(device);

        } else if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

            int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
            int prevMode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, -1);
            String s;
            switch (mode) {
                case BluetoothAdapter.SCAN_MODE_CONNECTABLE:                s = "connectable";              break;
                case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:   s = "connectable discoverable"; break;
                case BluetoothAdapter.SCAN_MODE_NONE:                       s = "none";                     break;
                default:                                                    s = "unknown";                  break;
            }
            Jog.v("BT scan mode: " + s, this);
        }
    }

    public void enableBluetooth(Activity activity) {
        if (!mAdapter.isEnabled()) {
            activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        }
    }

    /** Will enable Bluetooth if it is not already enabled. */
    public void enableDiscoverability(Activity activity) {
        if (!mAdapter.isDiscovering()) {
            mDiscoveredDevices.clear(); // TODO this should be called in onActivityResult()
            activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), REQUEST_DISCOVERABLE);
        }
    }

    public boolean startDiscovery() {
        return mAdapter.startDiscovery();
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
//        socket. TODO fffffffffffffffffffffffff
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID);
            } catch (IOException e) {
                Jog.d("BT error getting server socket", e, BluetoothService.this);
            }
            mmServerSocket = tmp;
        }

        @Override
        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    // blocks thread
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Jog.d("BT error while waiting to accept connection", e, BluetoothService.this);
                    break;
                }

                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);

                    // Don't close the server = multiple connections can be accepted (diff channels)?
                    //close();
                    //break;
                }
            }
        }

        public void close() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Jog.d("BT unable to close server socket.", e, BluetoothService.this);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(SERVICE_UUID);
            } catch (IOException e) {
                Jog.d("BT error getting client socket", e, BluetoothService.this);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException e) {
                Jog.d("BT client socket connection error", e, BluetoothService.this);
                close();
                return;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        public void close() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Jog.d("BT unable to close client socket.", e, BluetoothService.this);
            }
        }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
