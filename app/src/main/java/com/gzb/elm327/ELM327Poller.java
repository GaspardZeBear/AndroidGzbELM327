package com.gzb.elm327;

import android.os.SystemClock;
import android.util.Log;

import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ELM327Poller extends Thread{
    private ConnectedThread mConnectedThread;
    private boolean active;

    public ELM327Poller(ConnectedThread mConnectedThread) {
        this.mConnectedThread=mConnectedThread;
        if (mConnectedThread != null) {
            mConnectedThread.setEmitter(this);
        }
        activate();
    }

    public void initSequence() throws Exception {
        write("ATZ\r\n",1000);
        write("ATH1\r\n",1000);
        write("ATE0\r\n",1000);
        write("0100\r\n",6000);
    }

    public void write(String str0,int timer) throws Exception {
        byte[] ascii = str0.getBytes(StandardCharsets.US_ASCII);
        String asciiString = Arrays.toString(ascii);

        Log.d("ELM327Poller", "Ascii :  <" + asciiString + ">");

        String str=new String(ascii,StandardCharsets.US_ASCII);
        if (mConnectedThread != null) {
            try {
                Log.d("ELM327Poller", " write() data  " + str);
                mConnectedThread.write(str);
            } catch (Exception e) {
                Log.d("ELM327Poller", " write() Exception " + e.getMessage());
                throw new Exception(e);
            };
        } else {
            Log.d("ELM327Poller", "Not connected");
            SystemClock.sleep(5000);
        }
        SystemClock.sleep(timer);
    }

    public void post(byte[] buffer, int len) {
        String str = new String(buffer, 0, len, StandardCharsets.US_ASCII);
        Log.d("ELM327Poller", " post : got data  " + str);
    }

    public void run() {
        boolean fail = false;
        try {
            initSequence();
        } catch ( Exception e ) {
            fail=true;
        }
        //while (true) {
        while (!Thread.currentThread().isInterrupted() && !fail) {
            if (!active) {
                    Log.d("ELM327Poller", "Inactive");
                    //mConnectedThread.sendFake();
                    SystemClock.sleep(5000);
                    continue;
             }
            try {
                    Log.d("ELM327Poller", "Executing run() ");
                    write("010C\r\n", 1000);
                    write("010D\r\n", 1000);
                    // write("010D0C\r\n", 1000);
                    //SystemClock.sleep(500);
            }  catch (Exception e) {
                    Log.d("ELM327Poller","run() Exception " + e.getMessage());
                    fail=true;
                    break;
            }
        }
        Log.d("ELM327Poller","Exiting run() ");
    };

    public void toggle() {
        active=!active;
        if (!active) {
            //mConnectedThread.sendFake();
        }
    }

    public void activate() {
       active=true;
    }
    public void inactivate() {
       active=false;
    }
    public boolean getActive() {
        return(active);
    }
}
