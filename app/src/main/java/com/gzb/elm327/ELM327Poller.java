package com.gzb.elm327;

import android.os.SystemClock;
import android.util.Log;

import java.nio.charset.StandardCharsets;

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

    public void initSequence() {
        write("ATZ\n",2000);
        write("ATH1\n",2000);
        write("0100\n",6000);
    }

    public void write(String str,int timer) {
        if (mConnectedThread != null) {
            try {
                Log.d("ELM327Poller", " write() data  " + str);
                mConnectedThread.write(str);
            } catch (Exception e) {
                Log.d("ELM327Poller", " write() Exception " + e.getMessage());
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
        initSequence();
        while (true) {
            if (!active) {
                Log.d("ELM327Poller", "Inactive");
                SystemClock.sleep(5000);
                continue;
            }
           try {
               Log.d("ELM327Poller", "Executing run() ");
               write("010C\n",500);
               write("010D\n",500);
               SystemClock.sleep(500);
           } catch (Exception e) {
               Log.d("ELM327Poller","run() Exception " + e.getMessage());
               break;
           }
        }
        Log.d("ELM327Poller","Exiting run() ");
    };

    public void toggle() {
        active=!active;
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
