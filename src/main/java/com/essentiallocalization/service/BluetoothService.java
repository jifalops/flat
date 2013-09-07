package com.essentiallocalization.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;

import com.essentiallocalization.util.Installation;
import com.essentiallocalization.util.Jog;

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
    public static final int BUFFER_SIZE = 1024;
    private static final String SERVICE_NAME = "EssentialLocalizationBluetoothService";

    private BluetoothAdapter mAdapter;
    private List<BluetoothDevice> mDiscoveredDevices;

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private UUID mUuid;

    @Override
    public void onCreate() {
        super.onCreate();

        mUuid = Installation.uuid(this);

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


        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.close(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.close(); mConnectedThread = null;}

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }


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

    private void manageConnectedSocket(BluetoothSocket socket) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.close(); mConnectThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        mConnectedThread.write("Test data".getBytes());
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, mUuid);
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

                    // Don't cancel the server = multiple connections can be accepted (diff channels)?
                    //cancel();
                    //break;
                }
            }
        }

        public void close() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Jog.d("BT unable to cancel server socket.", e, BluetoothService.this);
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
                tmp = device.createRfcommSocketToServiceRecord(mUuid);
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

            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        public void close() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Jog.d("BT unable to cancel client socket.", e, BluetoothService.this);
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
            } catch (IOException e) {
                Jog.d("BT error getting I/O streams.", e, BluetoothService.this);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[BUFFER_SIZE];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    Jog.i("Received " + bytes + " bytes: " + new String(buffer), BluetoothService.this);
                } catch (IOException e) {
                    Jog.d("BT error reading input stream (disconnected).", e, BluetoothService.this);
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Jog.d("BT error writing to output stream.", e, BluetoothService.this);
            }
        }

        public void close() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Jog.d("BT error closing connection socket.", e, BluetoothService.this);
            }
        }
    }
}
