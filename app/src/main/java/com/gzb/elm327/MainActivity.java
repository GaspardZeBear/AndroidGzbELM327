package com.gzb.elm327;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    // GUI Components
    private TextView mBluetoothStatus;
    private TextView mSpeed;
    private TextView mRpm;
    private TextView offset;
    private Button mScanBtn;
    private Button mOffBtn;
    private Button mListPairedDevicesBtn;
    private Button mDiscoverBtn;
    private Button minusButton;
    private Button minus10Button;
    private Button zeroButton;
    private Button plusButton;
    private Button plus10Button;
    private ListView mDevicesListView;
    private CheckBox mLED1;
    ToggleButton[] toggleButtons;
    HashMap<ToggleButton,Integer> speeds;
    long locationChangedTimeMillis;
    float maxSpeed;
    float speedOffset;
    float readSpeed;

    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;

    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothStatus = (TextView)findViewById(R.id.bluetooth_status);
        mSpeed = (TextView) findViewById(R.id.speed);
        mRpm = (TextView) findViewById(R.id.rpm);
        offset = (TextView)findViewById(R.id.offset);
        mScanBtn = (Button)findViewById(R.id.scan);
        mOffBtn = (Button)findViewById(R.id.off);
        mDiscoverBtn = (Button)findViewById(R.id.discover);
        mListPairedDevicesBtn = (Button)findViewById(R.id.paired_btn);
        mLED1 = (CheckBox)findViewById(R.id.checkbox_led_1);
        minusButton=(Button)findViewById(R.id.minus);
        minus10Button=(Button)findViewById(R.id.minus10);
        zeroButton=(Button)findViewById(R.id.zero);
        plusButton=(Button)findViewById(R.id.plus);
        plus10Button=(Button)findViewById(R.id.plus10);

        toggleButtons = new ToggleButton[9];
        toggleButtons[0] = (ToggleButton)findViewById(R.id.s30);
        toggleButtons[1] = (ToggleButton)findViewById(R.id.s50);
        toggleButtons[2] = (ToggleButton)findViewById(R.id.s70);
        toggleButtons[3] = (ToggleButton)findViewById(R.id.s80);
        toggleButtons[4] = (ToggleButton)findViewById(R.id.s90);
        toggleButtons[5] = (ToggleButton)findViewById(R.id.s100);
        toggleButtons[6] = (ToggleButton)findViewById(R.id.s110);
        toggleButtons[7] = (ToggleButton)findViewById(R.id.s120);
        toggleButtons[8] = (ToggleButton)findViewById(R.id.s130);

        speeds=new HashMap<ToggleButton,Integer>();
        speeds.put((ToggleButton)findViewById(R.id.s30),30);
        speeds.put((ToggleButton)findViewById(R.id.s50),50);
        speeds.put((ToggleButton)findViewById(R.id.s70),70);
        speeds.put((ToggleButton)findViewById(R.id.s80),80);
        speeds.put((ToggleButton)findViewById(R.id.s90),90);
        speeds.put((ToggleButton)findViewById(R.id.s100),100);
        speeds.put((ToggleButton)findViewById(R.id.s110),110);
        speeds.put((ToggleButton)findViewById(R.id.s120),120);
        speeds.put((ToggleButton)findViewById(R.id.s130),130);

        maxSpeed=50;
        readSpeed=0;

        mBTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        mDevicesListView = (ListView)findViewById(R.id.devices_list_view);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Ask for location permission if not already allowed
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        for (ToggleButton t : toggleButtons) {
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for (ToggleButton t1 : toggleButtons) {
                        t1.setBackgroundColor(Color.LTGRAY);
                        t1.setPadding(50,5,5,5);
                    }
                    Log.d("xxx", "onClick: " + speeds.get(t).toString());
                    if (t.isChecked()) {
                        t.setBackgroundColor(Color.YELLOW);
                        maxSpeed=speeds.get(t);
                        //binding.offset.setText(String.format("%.0f",speedOffset));

                        mSpeed.setText(String.format("%.0f",readSpeed));
                        //binding.setpoint.setText(String.format("%.0f",maxSpeed+maxSpeedOffset));
                    } else {
                        t.setBackgroundColor(Color.LTGRAY);
                        maxSpeed=999;
                        //binding.offset.setText(String.format("%.0f",speedOffset));
                        //binding.readSpeed.setText("+Inf");
                    }

                    Log.d("xxx", "onClick: maxSpeed " + String.valueOf(maxSpeed));
                }
            });
        };


        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speedOffset++ ;
                offset.setText(String.format("%.0f",speedOffset));
                //binding.setpoint.setText(String.format("%.0f",maxSpeed+maxSpeedOffset));
                mSpeed.setText(String.format("%.0f",readSpeed));
            }
        });

        plus10Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speedOffset += 10 ;
                offset.setText(String.format("%.0f",speedOffset));
                mSpeed.setText(String.format("%.0f",readSpeed));
                //binding.setpoint.setText(String.format("%.0f",maxSpeed+maxSpeedOffset));
            }
        });

        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
        public void onClick(View v) {
                speedOffset-- ;
                offset.setText(String.format("%.0f",speedOffset));
                mSpeed.setText(String.format("%.0f",readSpeed));
                //binding.setpoint.setText(String.format("%.0f",maxSpeed+maxSpeedOffset));
            }
        });

        minus10Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speedOffset -= 10  ;
                offset.setText(String.format("%.0f",speedOffset));
                mSpeed.setText(String.format("%.0f",readSpeed));
                //binding.setpoint.setText(String.format("%.0f",maxSpeed+maxSpeedOffset));
            }
        });


        zeroButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speedOffset=0 ;
                offset.setText(String.format("%.0f",speedOffset));
                mSpeed.setText(String.format("%.0f",readSpeed));
                //binding.setpoint.setText(String.format("%.0f",maxSpeed+maxSpeedOffset));
            }
        });

        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg){
                if(msg.what == MESSAGE_READ){
                    //String readMessage = null;
                    //readMessage = new String((byte[]) msg.obj, StandardCharsets.US_ASCII);
                    //readMessage="Hello Gzb";
                    //readMessage=(String)msg.obj;
                    //Log.d("GUI handleMessage READ", "MSG : " + readMessage);
                    byte[] buffer = new byte[1024];
                    Arrays.fill(buffer,(byte)0x00);
                    buffer=(byte[])msg.obj;
                    String str = new String(buffer, 0, buffer.length,StandardCharsets.US_ASCII);
                    //mSpeed.setText(readMessage);
                    ELM327Response resp=new ELM327Response(str);
                    Log.d("Main","pidAlias" + resp.getPidAlias());
                    if (resp.getPidAlias().equals("SPEED")){
                        readSpeed=resp.getPidVal();

                        float currentSpeed = readSpeed + speedOffset  ;
                        if (currentSpeed > maxSpeed) {
                            mSpeed.setTextColor(Color.RED);
                            mSpeed.setBackgroundColor(Color.rgb(200,50,50));
                            try {
                                ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                            } catch (Exception e) {
                                Log.d("Main", "ToneGenerator: " + e.getMessage());
                            }
                        } else {
                            mSpeed.setTextColor(Color.GREEN);
                            mSpeed.setBackgroundColor(Color.rgb(0,200,50));
                        }

                        mSpeed.setText(String.valueOf(resp.getPidVal()));
                    } else if (resp.getPidAlias().equals("RPM")) {
                        mRpm.setText(String.valueOf(resp.getPidVal()/4));
                    } else {
                        int i=0;
                    }
                }

                if(msg.what == CONNECTING_STATUS){
                    char[] sConnected;
                    if(msg.arg1 == 1)
                        mBluetoothStatus.setText(getString(R.string.BTConnected) + msg.obj);
                    else
                        mBluetoothStatus.setText(getString(R.string.BTconnFail));
                }
            }
        };

        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText(getString(R.string.sBTstaNF));
            Toast.makeText(getApplicationContext(),getString(R.string.sBTdevNF),Toast.LENGTH_SHORT).show();
        }
        else {
            mLED1.setOnClickListener(new View.OnClickListener(){
                //byte[] rpm={0x01,0x0C};
                @Override

                public void onClick(View v){
                   if (mConnectedThread != null) {//First check to make sure thread created
                            //mConnectedThread.write("ATE0\r\n");
                            mConnectedThread.write("010D\r\n");
                            SystemClock.sleep(100);
                   }
                   if (mConnectedThread != null) {
                            mConnectedThread.write("010C\r\n");
                            SystemClock.sleep(100);
                   }
                }
            });

            mScanBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn();
                }
            });

            mOffBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    bluetoothOff();
                }
            });

            mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    listPairedDevices();
                }
            });

            mDiscoverBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    discover();
                }
            });
        }
    }

    private void bluetoothOn(){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText(getString(R.string.BTEnable));
            Toast.makeText(getApplicationContext(),getString(R.string.sBTturON),Toast.LENGTH_SHORT).show();

        }
        else{
            Toast.makeText(getApplicationContext(),getString(R.string.BTisON), Toast.LENGTH_SHORT).show();
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data){
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                mBluetoothStatus.setText(getString(R.string.sEnabled));
            }
            else
                mBluetoothStatus.setText(getString(R.string.sDisabled));
        }
    }

    private void bluetoothOff(){
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText(getString(R.string.sBTdisabl));
        Toast.makeText(getApplicationContext(),"Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    private void discover(){
        // Check if the device is already discovering
        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),getString(R.string.DisStop),Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), getString(R.string.DisStart), Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(), getString(R.string.BTnotOn), Toast.LENGTH_SHORT).show();
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getApplicationContext(), "BroadcastReceiver.onReceive() fired", Toast.LENGTH_SHORT).show();
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private void listPairedDevices(){
        mBTArrayAdapter.clear();
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), getString(R.string.show_paired_devices), Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), getString(R.string.BTnotOn), Toast.LENGTH_SHORT).show();
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d("Main","OnItemClickListener.onItemClick() fired");
            if(!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), getString(R.string.BTnotOn), Toast.LENGTH_SHORT).show();
                return;
            }

            mBluetoothStatus.setText(getString(R.string.cConnet));
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) view).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread() {
                @Override
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    try {
                        mBTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), getString(R.string.ErrSockCrea), Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), getString(R.string.ErrSockCrea), Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(!fail) {
                        mConnectedThread = new ConnectedThread(mBTSocket, mHandler);
                        mConnectedThread.start();
                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();

            new Thread() {
                @Override
                public void run() {
                    boolean fail = false;
                    while (true) {
                        Log.d("MainPolling","Executing");
                        if (mConnectedThread != null) {//First check to make sure thread created
                            //mConnectedThread.write("ATE0\r\n");
                            mConnectedThread.write("010C\n");
                            SystemClock.sleep(100);
                        } else {
                            Log.d("MainPolling","Not connected");
                            SystemClock.sleep(5000);
                        }
                        if (mConnectedThread != null) {//First check to make sure thread created
                            //mConnectedThread.write("ATE0\r\n");
                            mConnectedThread.write("010D\n");
                            SystemClock.sleep(100);
                        } else {
                            Log.d("MainPolling","Not connected");
                            SystemClock.sleep(5000);
                        }
                        SystemClock.sleep(1000);
                    }
                };
            }.start();
        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
        }
        return  device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }
}
