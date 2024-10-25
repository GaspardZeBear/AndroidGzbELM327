package com.gzb.elm327;

import android.os.SystemClock;
import android.util.Log;

public class ELM327Poller extends Thread{
    private ConnectedThread mConnectedThread;
    private boolean active;

    public ELM327Poller(ConnectedThread mConnectedThread) {
        this.mConnectedThread=mConnectedThread;
        activate();
    }

    public void run() {
        boolean fail = false;
        while (true) {
            if (!active) {
                Log.d("ELM327Poller", "Inactive");
                SystemClock.sleep(5000);
                continue;
            }
           try {
               Log.d("ELM327Poller", "Executing");
               if (mConnectedThread != null) {//First check to make sure thread created
                   //mConnectedThread.write("ATE0\r\n");
                   mConnectedThread.write("010C\n");
                   SystemClock.sleep(100);
               } else {
                   Log.d("ELM327Poller", "Not connected");
                   SystemClock.sleep(5000);
               }
               if (mConnectedThread != null) {//First check to make sure thread created
                   //mConnectedThread.write("ATE0\r\n");
                   mConnectedThread.write("010D\n");
                   SystemClock.sleep(100);
               } else {
                   Log.d("ELM327Poller", "Not connected");
                   SystemClock.sleep(5000);
               }
               SystemClock.sleep(1000);
           } catch (Exception e) {
               Log.d("ELM327Poller","Exception " + e.getMessage());
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
}
