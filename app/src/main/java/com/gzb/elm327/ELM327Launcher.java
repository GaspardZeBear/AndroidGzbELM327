package com.gzb.elm327;

import static com.gzb.elm327.MainActivity.BTSTATUS;
import static com.gzb.elm327.MainActivity.CONNECTING_STATUS;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

public class ELM327Launcher extends Thread {

    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    int counter=0;
    MainActivity mainActivity;
    private ConnectedThread mConnectedThread=null; // bluetooth background worker thread to send and receive data
    private ELM327Poller mPollerThread=null;
    private BluetoothAdapter mBTAdapter;
    private View view;
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path
    private String btInfo;
    private Handler mHandler;
    private String btName;


    public ELM327Launcher() {
            super();
    }

    public ELM327Launcher(MainActivity mainActivity, String btInfo, BluetoothAdapter mBTAdapter, Handler mHandler) {
        super();
        this.mainActivity=mainActivity;
        this.btInfo=btInfo;
        this.mBTAdapter=mBTAdapter;
        this.mHandler=mHandler;
        Log.d("ELM327Launcher","ELM327Launcher created " );
    }

    @Override
    public void run() {
        //this.mainActivity.setmBluetoothStatusText("Connecting");
        // Get the device MAC address, which is the last 17 chars in the View
        Log.d("ELM327Launcher","run method called" );
        //String info = ((TextView) view).getText().toString();
        final String address = btInfo.substring(btInfo.length() - 17);
        final String btName = btInfo.substring(0,btInfo.length() - 17);
        mHandler.obtainMessage(BTSTATUS, -1, -1,"Try " + btName)
                .sendToTarget();

        // This thread will start :
        // - a bluetooth communication thread : writes and receive datas on bluetooth socket
        // - a Polling Thread (send :
        boolean fail = false;
        BluetoothDevice device = mBTAdapter.getRemoteDevice(address);
            try {
                mBTSocket = createBluetoothSocket(device);
            } catch (IOException e) {
                fail = true;
                //Toast.makeText(getBaseContext(), getString(R.string.ErrSockCrea), Toast.LENGTH_SHORT).show();
                mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                        .sendToTarget();
                Log.d("Main","Create BT Exception " + e.getMessage());
            }
            // Establish the Bluetooth socket connection.
            Log.d("Main","BT Socket created ");
            try {
                Log.d("Main","BT Socket connect ");
                mBTSocket.connect();
            } catch (IOException e) {
                Log.d("ELM327Launcher","BT Socket connect Exception " + e.getMessage());
                try {
                    fail = true;
                    Log.d("Main","BT Socket closing due to exception ");
                    mBTSocket.close();
                    mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                            .sendToTarget();
                } catch (IOException e2) {
                    mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                            .sendToTarget();
                    Log.d("ELM327Launcher","BT Socket close Exception " + e2.getMessage());
                }
            }
            if(!fail) {
                mConnectedThread = new ConnectedThread(mBTSocket, mHandler);
                mConnectedThread.start();
                mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, btName)
                        .sendToTarget();
                // Starts the thread will will emit ELM327 commands
                //SystemClock.sleep(500);
                mPollerThread=new ELM327Poller(mConnectedThread);
                mPollerThread.start();
                mHandler.obtainMessage(BTSTATUS, -1, -1,"Cnx " + btName)
                        .sendToTarget();
            } else {
                mHandler.obtainMessage(BTSTATUS, -1, -1,"Fail " + btName)
                        .sendToTarget();
            }
            Log.d("ELM327Launcher","ELM327Launcher created ConnectedThread and ELM327Poller" );
    }


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e("ELM327Launcher", "Could not create Insecure RFComm Connection",e);
            mHandler.obtainMessage(BTSTATUS, -1, -1,"Failed Insecure " + btName)
                    .sendToTarget();
        }
        return  device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }

    public void Xrun() {
        while (true) {
            Log.d("TestThread", "Executing : counter" + String.valueOf(counter));
            if ( mainActivity != null ) {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.setRaw("Alive");
                    }//public void run() {setRaw());
                });

            }
            SystemClock.sleep(5000);
        }
    }

    public void incrementCounter() {
        counter++;
        Log.d("EML327Launcher ", "Executing : counter" + String.valueOf(counter));
    }


    public void stopThreads() {
        counter++;
        Log.d("EML327Launcher", "Stopping thread : count : " + String.valueOf(counter));
        if ( mConnectedThread != null) {
            mConnectedThread.interrupt();
        }
        if ( mPollerThread != null) {
            mPollerThread.interrupt();
        }
        mHandler.obtainMessage(BTSTATUS, -1, -1,"Pen " + btName)
                .sendToTarget();
    }

}
