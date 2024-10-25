package com.gzb.elm327;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private final Handler mHandler;

    public ConnectedThread(BluetoothSocket socket, Handler handler) {
        mmSocket = socket;
        mHandler = handler;
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

    public int getCharPos(char[] tab, int val,int defaultPos) {
        int pos=defaultPos;
        for (int i = 0; i < tab.length ; i++) {
            if (tab[i] == (int)val) {
                pos=i;
                break;
            }
        };
        return(pos);

    }

    public String getAsciiHexaString(String str) {
        StringBuilder clean = new StringBuilder();
        char[] charArray = str.toCharArray();
        // Find response : between "LF" and ">" (elm327 prompt)
        int lfPos=getCharPos(charArray,0x0A,0);
        int gtPos=getCharPos(charArray,0x3e, charArray.length);
        for (int i = lfPos; i < gtPos ; i++) {
        //for (int i = crPos; i < charArray.length ; i++) {
        //for (int i = 0; i < charArray.length ; i++) {
            //Log.d("Convert", "i: " + String.valueOf(i) + " val <" + charArray[i] + "> + hexa " + Integer.toHexString((int)charArray[i]));
            try {
                //int intValue = Integer.parseInt(charArray[i]);
                int intValue = charArray[i];
                // Remove all controls chars
                if ( (intValue >= 48 && intValue < 58) || (intValue >= 65 && intValue < 71) ) {
                    clean.append(Character.toChars(intValue));
                }

            }
            catch (NumberFormatException nfe) {
                Log.d("Convert", "NumberFormatException: " + nfe.getMessage());
            }
        }
        return clean.toString();
    }

    @Override
    public void run() {
        //byte[] buffer = new byte[1024];  // buffer store for the stream
        //int bytesCount; // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                int bytesCount = mmInStream.available();
                if(bytesCount != 0) {
                    byte[] buffer = new byte[1024];
                    Arrays.fill(buffer,(byte)0x00);
                    SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                    bytesCount = mmInStream.available(); // how many bytes are ready to be read?
                    bytesCount = mmInStream.read(buffer, 0, bytesCount); // record how many bytes we actually read
                    //String str = new String(buffer, 0, bytesCount,StandardCharsets.US_ASCII);
                    //Log.d("Handler READ","READ count " + String.valueOf(bytesCount) + " value : " + str);

                    //String strClean=getAsciiHexaString(str);
                    //Log.d("Handler READ","READ count clean" + String.valueOf(bytesCount) + " value : " + strClean);

                    mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytesCount, -1, buffer).sendToTarget(); // Send the obtained bytes to the UI activity
                    //mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytesCount, -1, strClean).sendToTarget(); // Send the obtained string !! to the UI activity
                }
            } catch (IOException e) {
                e.printStackTrace();

                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(String input) {
        Log.d("WRITE","Thread WRITE value : " + input);
        byte[] bytes = input.getBytes();           //converts entered String into bytes
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }

    public void write(byte[] bytes) {
        //byte[] bytes = input.getBytes();           //converts entered String into bytes
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }

    public void writeRPM() {
        byte[] rpm={0x01,0x0C};
        try {
            mmOutStream.write(rpm);
        } catch (IOException e) { }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}