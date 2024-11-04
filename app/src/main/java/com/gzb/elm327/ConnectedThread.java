package com.gzb.elm327;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;

public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private final Handler mHandler;
    private int writeFailCounter;
    private int writeCounter;
    private int receiveCounter;
    private ELM327Poller emitter;

    public ConnectedThread(BluetoothSocket socket, Handler handler) {
        mmSocket = socket;
        mHandler = handler;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        writeFailCounter=0;
        receiveCounter=0;
        writeCounter=0;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    @Override
    public void run() {
        // Keep listening to the InputStream until an exception occurs
        //while (true) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Read from the InputStream
                int bytesCount = mmInStream.available();
                if (bytesCount != 0) {
                    receiveCounter++;
                    byte[] buffer = new byte[1024];
                    Arrays.fill(buffer, (byte) 0x00);
                    SystemClock.sleep(500); //pause and wait for rest of data. Adjust this depending on your sending speed.
                    bytesCount = mmInStream.available(); // how many bytes are ready to be read?
                    bytesCount = mmInStream.read(buffer, 0, bytesCount); // record how many bytes we actually read
                    mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytesCount, -1, buffer).sendToTarget(); // Send the obtained bytes to the UI activity
                    if (emitter != null) {
                        emitter.post(buffer, bytesCount);
                    }
                } else {
                    Log.d("ConnectedThread", "No available data");
                    int sleepTime = 500;
                    if (emitter != null && !emitter.getActive()) {
                        sleepTime = 5000;
                    }
                    SystemClock.sleep(sleepTime);
                }
            } catch (InterruptedIOException eInt) {
                Log.d("ConnectedThread", "Interrupted");
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

        }
        Log.d("ConnectedThread", "Broke run() ");
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(String input) throws Exception {
        //Log.d("WRITE","Thread WRITE value : " + input);
        writeCounter++;
        Log.d("ConnectedThread","writeCounter :  " + String.valueOf(writeCounter) + " receiveCounter " + String.valueOf(receiveCounter));

        // ugly, but just an indication
        if (emitter.getActive()) {
            writeCounter+=0;
            receiveCounter+=0;
        }
        //byte[] bytes = input.getBytes();
        byte[] bytes = input.getBytes(StandardCharsets.US_ASCII); // converts entered String into bytes
        try {
            mmOutStream.write(bytes);
            if (writeFailCounter > 0) {
                writeFailCounter--;
            }
        } catch (IOException e) {
            mHandler.obtainMessage(MainActivity.MESSAGE_WRITE_FAILED, "Failed").sendToTarget();
            writeFailCounter++;
            Log.d("ConnectedThread","Write failed :  " + e.getMessage());
            if (writeFailCounter > 10 ) {
                Log.d("ConnectedThread","Write failed cancel  ");
                cancel();
                throw new Exception("Socket closed");
            }
        }
    }

    public void setEmitter(ELM327Poller emitter) {
        this.emitter=emitter;
    }

    public void sendFake() {
        byte[] fakeRpm = "7E804410C0000".getBytes(StandardCharsets.US_ASCII);
        byte[] fakeSpeed = "7E803410D00".getBytes(StandardCharsets.US_ASCII);
        mHandler.obtainMessage(MainActivity.MESSAGE_READ, fakeSpeed.length, -1, fakeSpeed).sendToTarget();
        mHandler.obtainMessage(MainActivity.MESSAGE_READ, fakeRpm.length, -1, fakeRpm).sendToTarget();
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.d("ConnectedThread","Close socket failed :  " + e.getMessage());
        }
    }
}