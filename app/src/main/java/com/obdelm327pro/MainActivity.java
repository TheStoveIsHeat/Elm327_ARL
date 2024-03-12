package com.obdelm327pro;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.AppBarLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
//for converting to CSV (new code)
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.IOException;
//for sending the data via wifi (new code)
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
//more for the server
import android.os.AsyncTask;
import java.io.BufferedWriter;
import java.io.FileInputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final int MESSAGE_STATE_CHANGE = 1;

    /*0	Automatic protocol detection
   1	SAE J1850 PWM (41.6 kbaud)
   2	SAE J1850 VPW (10.4 kbaud)
   3	ISO 9141-2 (5 baud init, 10.4 kbaud)
   4	ISO 14230-4 KWP (5 baud init, 10.4 kbaud)
   5	ISO 14230-4 KWP (fast init, 10.4 kbaud)
   6	ISO 15765-4 CAN (11 bit ID, 500 kbaud)
   7	ISO 15765-4 CAN (29 bit ID, 500 kbaud)
   8	ISO 15765-4 CAN (11 bit ID, 250 kbaud) - used mainly on utility vehicles and Volvo
   9	ISO 15765-4 CAN (29 bit ID, 250 kbaud) - used mainly on utility vehicles and Volvo


    01 04 - ENGINE_LOAD
    01 05 - ENGINE_COOLANT_TEMPERATURE
    01 0C - ENGINE_RPM
    01 0D - VEHICLE_SPEED
    01 0F - INTAKE_AIR_TEMPERATURE
    01 10 - MASS_AIR_FLOW
    01 11 - THROTTLE_POSITION_PERCENTAGE
    01 1F - ENGINE_RUN_TIME
    01 2F - FUEL_LEVEL
    01 46 - AMBIENT_AIR_TEMPERATURE
    01 51 - FUEL_TYPE
    01 5E - FUEL_CONSUMPTION_1
    01 5F - FUEL_CONSUMPTION_2

   */
    //for collecting vehicle speed
    ArrayList<Integer> km_speed = new ArrayList<Integer>();
    String saveLocation = "/storage/emulated/0/Download";
    String fileName = "pid_data.csv";

    //String random VIN

    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    protected final static char[] dtcLetters = {'P', 'C', 'B', 'U'};
    protected final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static final String[] PIDS = {
            "01", "02", "03", "04", "05", "06", "07", "08",
            "09", "0A", "0B", "0C", "0D", "0E", "0F", "10",
            "11", "12", "13", "14", "15", "16", "17", "18",
            "19", "1A", "1B", "1C", "1D", "1E", "1F", "20"};

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final float APPBAR_ELEVATION = 14f;
    private static boolean actionbar = true;
    final List<String> commandslist = new ArrayList<String>();

    final List<Double> avgconsumption = new ArrayList<Double>();
    final List<String> troubleCodesArray = new ArrayList<String>();
    MenuItem itemtemp;

    BluetoothDevice currentdevice;
    boolean commandmode = false, initialized = false, m_getPids = false, tryconnect = false, defaultStart = false;
    String devicename = null, deviceprotocol = null;

    String[] initializeCommands;
    Intent serverIntent = null;
    TroubleCodes troubleCodes;
    String VOLTAGE = "ATRV",
            PROTOCOL = "ATDP",
            RESET = "ATZ",
            PIDS_SUPPORTED20 = "0100",
            ENGINE_COOLANT_TEMP = "0105",  //A-40
            ENGINE_RPM = "010C",  //((A*256)+B)/4
            ENGINE_LOAD = "0104",  // A*100/255
            VEHICLE_SPEED = "010D",  //A
            INTAKE_AIR_TEMP = "010F",  //A-40
            MAF_AIR_FLOW = "0110", //MAF air flow rate 0 - 655.35	grams/sec ((256*A)+B) / 100  [g/s]
            ENGINE_OIL_TEMP = "015C",  //A-40
            FUEL_RAIL_PRESSURE = "0122", // ((A*256)+B)*0.079
            INTAKE_MAN_PRESSURE = "010B", //Intake manifold absolute pressure 0 - 255 kPa
            CONT_MODULE_VOLT = "0142",  //((A*256)+B)/1000
            AMBIENT_AIR_TEMP = "0146",  //A-40
            CATALYST_TEMP_B1S1 = "013C",  //(((A*256)+B)/10)-40
            STATUS_DTC = "0101", //Status since DTC Cleared
            THROTTLE_POSITION = "0111", //Throttle position 0 -100 % A*100/255
            OBD_STANDARDS = "011C", //OBD standards this vehicle
            FUEL_LEVEL = "012F", //Fuel level???
            PIDS_SUPPORTED = "0120"; //PIDs supported
    Toolbar toolbar;
    AppBarLayout appbar;
    String trysend = null;
    private PowerManager.WakeLock wl;
    private Menu menu;
    private EditText mOutEditText;
    private Button mSendButton, mRetrieveDB, mTroublecodes, mSendtoDB, mSavetoCSV;
    private ListView mConversationView;
    private TextView engineLoad, Fuel, voltage, coolantTemperature, Status, Loadtext, Volttext, Temptext, Centertext, Info, Airtemp_text, airTemperature, Maf_text, Maf, speed;
    private String mConnectedDeviceName = "Ecu";
    private int rpmval = 0, intakeairtemp = 0, ambientairtemp = 0, coolantTemp = 0, mMaf = 0,
            engineoiltemp = 0, b1s1temp = 0, Enginetype = 0, FaceColor = 0,
            whichCommand = 0, m_dedectPids = 0, connectcount = 0, trycount = 0;
    private int mEnginedisplacement = 1500;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mBtService = null;
    private ObdWifiManager mWifiService = null;

    StringBuilder inStream = new StringBuilder();

    // The Handler that gets information back from the BluetoothChatService
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;

    private final Handler mWifiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:

                    switch (msg.arg1) {
                        case ObdWifiManager.STATE_CONNECTED:
                            Status.setText(getString(R.string.title_connected_to, "ELM327 WIFI"));
                            try {
                                //changing menu text items
                                itemtemp = menu.findItem(R.id.menu_connect_wifi);
                                itemtemp.setTitle(R.string.disconnectwifi);
                            } catch (Exception e) {
                            }
                            tryconnect = false;
                            //resetValues();
                            sendEcuMessage(RESET);
                            break;
                        case ObdWifiManager.STATE_CONNECTING:
                            Status.setText(R.string.title_connecting);
                            Info.setText(R.string.tryconnectwifi);
                            break;
                        case ObdWifiManager.STATE_NONE:
                            Status.setText(R.string.title_not_connected);
                            itemtemp = menu.findItem(R.id.menu_connect_wifi);
                            itemtemp.setTitle(R.string.connectwifi);
                            if (mWifiService != null) mWifiService.disconnect();
                            mWifiService = null;
                            //resetValues();
                            break;
                    }
                    break;
                case MESSAGE_WRITE:

                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);

                    if (commandmode || !initialized) {
                        mConversationArrayAdapter.add("Command:  " + writeMessage);
                    }

                    break;

                case MESSAGE_READ:

                    String tmpmsg = clearMsg(msg);

                    Info.setText(tmpmsg);

                    if (tmpmsg.contains(RSP_ID.NODATA.response) || tmpmsg.contains(RSP_ID.ERROR.response)) {

                        try {
                            String command = tmpmsg.substring(0, 4);

                            if (isHexadecimal(command)) {
                                removePID(command);
                            }

                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    if (commandmode || !initialized){
                        mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + tmpmsg);
                    }
                    //after intializations stop being read, we analyze the messages in OBD
                    analyzeMsg(msg);
                    break;

                case MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;

                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private final Handler mBtHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:

                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:

                            Status.setText(getString(R.string.title_connected_to, mConnectedDeviceName));
                            Info.setText(R.string.title_connected);
                            try {
                                itemtemp = menu.findItem(R.id.menu_connect_bt);
                                itemtemp.setTitle(R.string.disconnectbt);
                                Info.setText(R.string.title_connected);
                            } catch (Exception e) {
                            }

                            tryconnect = false;
                            //resetValues();
                            sendEcuMessage(RESET);

                            break;
                        case BluetoothService.STATE_CONNECTING:
                            Status.setText(R.string.title_connecting);
                            Info.setText(R.string.tryconnectbt);
                            break;
                        case BluetoothService.STATE_LISTEN:

                        case BluetoothService.STATE_NONE:

                            Status.setText(R.string.title_not_connected);
                            itemtemp = menu.findItem(R.id.menu_connect_bt);
                            itemtemp.setTitle(R.string.connectbt);
                            if (tryconnect) {
                                mBtService.connect(currentdevice);
                                connectcount++;
                                if (connectcount >= 2) {
                                    tryconnect = false;
                                }
                            }
                            //resetValues();

                            break;
                    }
                    break;
                case MESSAGE_WRITE:

                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);

                    if (commandmode || !initialized) {
                        mConversationArrayAdapter.add("Command:  " + writeMessage);
                    }

                    break;
                case MESSAGE_READ:

                    String tmpmsg = clearMsg(msg);

                    //logging the read message to logcat to see the result (for demo)
                    Log.i("BluetoothDebug", "Received message: " + tmpmsg);

                    Info.setText(tmpmsg);

                    /*if (tmpmsg.contains(RSP_ID.NODATA.response) || tmpmsg.contains(RSP_ID.ERROR.response)) {

                        try{
                            String command = tmpmsg.substring(0,4);

                            if(isHexadecimal(command))
                            {
                                removePID(command);
                            }

                        }catch(Exception e)
                        {
                            Toast.makeText(getApplicationContext(), e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        }
                    }*/

                    if (commandmode || !initialized) {
                        mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + tmpmsg);
                    }

                    analyzeMsg(msg);

                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void removePID(String pid) {
        int index = commandslist.indexOf(pid);

        if (index != -1) {
            commandslist.remove(index);
            Info.setText("Removed pid: " + pid);
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // If the action is a key-up event on the return key, send the message
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                        String message = view.getText().toString();
                        sendEcuMessage(message);
                    }
                    return true;
                }
            };

    public static boolean isHexadecimal(String text) {
        text = text.trim();

        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F'};

        int hexDigitsCount = 0;

        for (char symbol : text.toCharArray()) {
            for (char hexDigit : hexDigits) {
                if (symbol == hexDigit) {
                    hexDigitsCount++;
                    break;
                }
            }
        }

        return true ? hexDigitsCount == text.length() : false;
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gauges);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        appbar = (AppBarLayout) findViewById(R.id.appbar);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        wl.acquire(10 * 60 * 1000L /*10 minutes*/);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Status = (TextView) findViewById(R.id.Status);
        engineLoad = (TextView) findViewById(R.id.Load);
        Fuel = (TextView) findViewById(R.id.Fuel);
        coolantTemperature = (TextView) findViewById(R.id.Temp);
        voltage = (TextView) findViewById(R.id.Volt);
        Loadtext = (TextView) findViewById(R.id.Load_text);
        Temptext = (TextView) findViewById(R.id.Temp_text);
        Volttext = (TextView) findViewById(R.id.Volt_text);
        Centertext = (TextView) findViewById(R.id.Center_text);
        Info = (TextView) findViewById(R.id.info);
        Airtemp_text = (TextView) findViewById(R.id.Airtemp_text);
        airTemperature = (TextView) findViewById(R.id.Airtemp);
        Maf_text = (TextView) findViewById(R.id.Maf_text);
        Maf = (TextView) findViewById(R.id.Maf);
        speed = (TextView) findViewById(R.id.Speeds);


        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mRetrieveDB = (Button) findViewById(R.id.button_retrievedb);
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendtoDB = (Button) findViewById(R.id.button_sendDB);
        mSavetoCSV = (Button) findViewById(R.id.button_saveCSV);
        mTroublecodes = (Button) findViewById(R.id.button_troublecodes);
        mConversationView = (ListView) findViewById(R.id.in);


        troubleCodes = new TroubleCodes();

        visibleCMD();

        //ATZ reset all
        //ATDP Describe the current Protocol
        //ATAT0-1-2 Adaptive Timing Off - daptive Timing Auto1 - daptive Timing Auto2
        //ATE0-1 Echo Off - Echo On
        //ATSP0 Set Protocol to Auto and save it
        //ATMA Monitor All
        //ATL1-0 Linefeeds On - Linefeeds Off
        //ATH1-0 Headers On - Headers Off
        //ATS1-0 printing of Spaces On - printing of Spaces Off
        //ATAL Allow Long (>7 byte) messages
        //ATRD Read the stored data
        //ATSTFF Set time out to maximum
        //ATSTHH Set timeout to 4ms

        initializeCommands = new String[]{"ATL0", "ATE1", "ATH1", "ATAT1", "ATSTFF", "ATI", "ATDP", "ATSP0", "0100"};

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
        } else {
            if (mBtService != null) {
                if (mBtService.getState() == BluetoothService.STATE_NONE) {
                    mBtService.start();
                }
            }
        }

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Get the Item from ListView
                View view = super.getView(position, convertView, parent);

                // Initialize a TextView for ListView each Item
                TextView tv = (TextView) view.findViewById(R.id.listText);

                // Set the text color of TextView (ListView Item)
                tv.setTextColor(Color.parseColor("#3ADF00"));
                tv.setTextSize(10);

                // Generate ListView Item using TextView
                return view;
            }
        };

        mConversationView.setAdapter(mConversationArrayAdapter);

        mRetrieveDB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mConversationArrayAdapter.add("User: Requesting current data from database...");
                //String sPIDs = "0100";
                //m_getPids = false;
                //sendEcuMessage(sPIDs);
                mConversationArrayAdapter.add("User: !!Not Yet Implemented!!");
            }
        });
        // Initialize the send button with a listener that for click events

        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                String message = mOutEditText.getText().toString();
                sendEcuMessage(message);
            }
        });

        mSendtoDB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Assume you have a File object named csvFile representing your CSV file
                File csvFile = new File(saveLocation, fileName);
                mConversationArrayAdapter.add("User: Grabbing csv file at \"" + csvFile + "\"...");
                mConversationArrayAdapter.add("User: Sending csv file to database...");
                // Execute the AsyncTask to send data to the server
                //insertData(csvFile);
                CSVConsume.send(saveLocation+fileName);
                //mConversationArrayAdapter.add("User: Success! Deleting local csv file");
            }
        });

        mSavetoCSV.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //IDK HOW TO DO FUNCTIONS/CLASSES IN JAVA BUT THE CSV CALL WOULD GO HERE
                //mConversationArrayAdapter.clear();' //OLD LINE OF CODE FOR CLEARING CMD LIST
                mConversationArrayAdapter.add("User: Saving data to CSV file at \"" + saveLocation + "\"...");

                // Save the data to CSV file (new code to save to CSV file)
                //String csvData = PID + "," + A + "," + B + "\n";
                String avg_value = calculateAvgList(km_speed);

                //For vin?
                sendEcuMessage("0902");
                //For fuel level
                sendEcuMessage("012F");

                //Generate random Trip# each time button is pressed, to be stored into CSV

                /* If wanted, we could take manual input for something
                String driverID = mOutEditText.getText().toString();
                mConversationArrayAdapter.add(driverID);
                 */

                /*CSV Format
                1. Trip, pre-fuel, post-fuel, pre_mpg, post_mpg, pre_mileage, post_mileage, VIN, driver, date, time
                2. VIN, IdleTime, FuelRate, EngineOnTime, MPG, date, time
                */

                //For loop for saving each of those in a specific order to a csv file
                //calling func to save data to csv file
                CSVConsume.saveToCSV(fileName, avg_value);
                km_speed.clear(); //clearing the array list 

                mConversationArrayAdapter.add("User: Success! File named \""+ fileName +"\"");
            }
        });

        mTroublecodes.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String troubleCodes = "03";
                sendEcuMessage(troubleCodes);
            }
        });

        mOutEditText.setOnEditorActionListener(mWriteListener);

        RelativeLayout rlayout = (RelativeLayout) findViewById(R.id.mainscreen);
        rlayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
               //Actionbar click
            }
        });
        
        getPreferences();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        this.menu = menu;

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menu_connect_bt:

                if (mWifiService != null) {
                    if (mWifiService.isConnected()) {
                        Toast.makeText(getApplicationContext(), "First Disconnect WIFI Device.", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }

                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    //new code
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                    }
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT, null);

                }

                if (mBtService == null) setupChat();

                if (item.getTitle().equals("Use Bluetooth OBDII")) {
                    // Launch the DeviceListActivity to see devices and do scan
                    serverIntent = new Intent(this, DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                } else {
                    if (mBtService != null)
                    {
                        mBtService.stop();
                        item.setTitle(R.string.connectbt);
                    }
                }

                return true;
            case R.id.menu_connect_wifi:

                if (item.getTitle().equals("Use WiFi OBDII")) {

                    if (mWifiService == null)
                    {
                        mWifiService = new ObdWifiManager(this, mWifiHandler);
                    }

                    if (mWifiService != null) {
                        if (mWifiService.getState() == ObdWifiManager.STATE_NONE) {
                            mWifiService.connect();
                        }
                    }
                } else {
                    if (mWifiService != null)
                    {
                        mWifiService.disconnect();
                        item.setTitle(R.string.connectwifi);
                    }
                }

                return true;
                //case to enter the pids screen
            case R.id.menu_terminal:

                if (item.getTitle().equals("View Stats")) {
                    commandmode = true;
                    invisibleCMD();
                    item.setTitle(R.string.terminal);
                } else {
                    visibleCMD();
                    item.setTitle(R.string.pids);
                    commandmode = false;
                    sendEcuMessage(VOLTAGE); //may not need this
                }
                return true;

            case R.id.menu_settings:

                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, Prefs.class);
                startActivity(serverIntent);

                return true;
            case R.id.menu_exit:
                exit();

                return true;
            //case R.id.menu_reset:
            //    resetValues();
            //    return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //for bluetooth
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == MainActivity.RESULT_OK) {
                    connectDevice(data);
                }
                break;

            case REQUEST_ENABLE_BT:

                if (mBtService == null) setupChat();

                if (resultCode == MainActivity.RESULT_OK) {
                    serverIntent = new Intent(this, DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                } else {
                    Toast.makeText(getApplicationContext(), "BT device not enabled", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    ///////////////////////////////////////////////////////////////////////

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setDefaultOrientation();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        getPreferences();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mBtService != null) mBtService.stop();
        if (mWifiService != null)mWifiService.disconnect();

        wl.release();
    }

    @Override
    public void onStart() {
        super.onStart();
        getPreferences();
        setDefaultOrientation();
        resetValues();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

//when the back button is pressed
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (!commandmode) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setMessage("Are you sure you want exit?");
                alertDialogBuilder.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                exit();
                            }
                        });

                alertDialogBuilder.setNegativeButton("cancel",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {

                            }
                        });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            } else {
                commandmode = false;
                visibleCMD();
                MenuItem item = menu.findItem(R.id.menu_terminal);
                item.setTitle(R.string.terminal);
                sendEcuMessage(VOLTAGE); //may not need this
            }

            return false;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void exit() {
        if (mBtService != null) mBtService.stop();
        wl.release();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void getPreferences() {

            SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(getBaseContext());

            FaceColor = Integer.parseInt(preferences.getString("FaceColor", "0"));



            mEnginedisplacement = Integer.parseInt(preferences.getString("Enginedisplacement", "1500"));

            m_dedectPids = Integer.parseInt(preferences.getString("DedectPids", "0"));

            if (m_dedectPids == 0) {

                commandslist.clear();

                int i = 0;

                commandslist.add(i, VOLTAGE);

                if (preferences.getBoolean("checkboxENGINE_RPM", true)) {
                    commandslist.add(i, ENGINE_RPM);
                    i++;
                }

                if (preferences.getBoolean("checkboxVEHICLE_SPEED", true)) {
                    commandslist.add(i, VEHICLE_SPEED);
                    i++;
                }

                if (preferences.getBoolean("checkboxENGINE_LOAD", true)) {
                    commandslist.add(i, ENGINE_LOAD);
                    i++;
                }

                if (preferences.getBoolean("checkboxENGINE_COOLANT_TEMP", true)) {
                    commandslist.add(i, ENGINE_COOLANT_TEMP);
                    i++;
                }

                if (preferences.getBoolean("checkboxINTAKE_AIR_TEMP", true)) {
                    commandslist.add(i, INTAKE_AIR_TEMP);
                    i++;
                }

                if (preferences.getBoolean("checkboxMAF_AIR_FLOW", true)) {
                    commandslist.add(i, MAF_AIR_FLOW);
                }

                whichCommand = 0;
            }
    }

    private void setDefaultOrientation() {

        try {
            setTextSize();

        } catch (Exception e) {
        }
    }

    private void setTextSize() {
        int textSize = 14;
        int newTextSize = 12;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindow().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        Status.setTextSize(newTextSize);
        Fuel.setTextSize(textSize + 2);
        coolantTemperature.setTextSize(textSize);
        engineLoad.setTextSize(textSize);
        voltage.setTextSize(textSize);
        Temptext.setTextSize(textSize);
        Loadtext.setTextSize(textSize);
        Volttext.setTextSize(textSize);
        Airtemp_text.setTextSize(textSize);
        airTemperature.setTextSize(textSize);
        Maf_text.setTextSize(textSize);
        Maf.setTextSize(textSize);
        Info.setTextSize(newTextSize);
        speed.setTextSize(newTextSize);
    }

    public void invisibleCMD() {
        mConversationView.setVisibility(View.INVISIBLE);
        mOutEditText.setVisibility(View.INVISIBLE);
        mSendButton.setVisibility(View.INVISIBLE);
        mRetrieveDB.setVisibility(View.INVISIBLE);
        mTroublecodes.setVisibility(View.INVISIBLE);
        mSendtoDB.setVisibility(View.INVISIBLE);
        mSavetoCSV.setVisibility(View.INVISIBLE);

        engineLoad.setVisibility(View.VISIBLE);
        Fuel.setVisibility(View.VISIBLE);
        voltage.setVisibility(View.VISIBLE);
        coolantTemperature.setVisibility(View.VISIBLE);
        Loadtext.setVisibility(View.VISIBLE);
        Volttext.setVisibility(View.VISIBLE);
        Temptext.setVisibility(View.VISIBLE);
        Centertext.setVisibility(View.VISIBLE);
        Info.setVisibility(View.VISIBLE);
        //pids
        Airtemp_text.setVisibility(View.VISIBLE);
        airTemperature.setVisibility(View.VISIBLE);
        Maf_text.setVisibility(View.VISIBLE);
        Maf.setVisibility(View.VISIBLE);
        speed.setVisibility(View.VISIBLE);
    }

    public void visibleCMD() {
        engineLoad.setVisibility(View.INVISIBLE);
        Fuel.setVisibility(View.INVISIBLE);
        voltage.setVisibility(View.INVISIBLE);
        coolantTemperature.setVisibility(View.INVISIBLE);
        Loadtext.setVisibility(View.INVISIBLE);
        Volttext.setVisibility(View.INVISIBLE);
        Temptext.setVisibility(View.INVISIBLE);
        Centertext.setVisibility(View.INVISIBLE);
        Info.setVisibility(View.INVISIBLE);
        //pids
        Airtemp_text.setVisibility(View.INVISIBLE);
        airTemperature.setVisibility(View.INVISIBLE);
        Maf_text.setVisibility(View.INVISIBLE);
        Maf.setVisibility(View.INVISIBLE);
        speed.setVisibility(View.INVISIBLE);

        mConversationView.setVisibility(View.VISIBLE);
        mOutEditText.setVisibility(View.VISIBLE);
        mSendButton.setVisibility(View.VISIBLE);
        mRetrieveDB.setVisibility(View.VISIBLE);
        mTroublecodes.setVisibility(View.VISIBLE);
        mSendtoDB.setVisibility(View.VISIBLE);
        mSavetoCSV.setVisibility(View.VISIBLE);
    }

//class to send data to server (performs network operation in the background)
//private static class SendDataToServerTask extends AsyncTask<File, Void, Void> {
//    @Override
//    protected Void doInBackground(File... params) {
//        // params[0] contains the data you want to send
//        File fileToSend = params[0];
//
//        try {
//            // replace "your_server_url" with your actual server URL
//            URL url = new URL("66.211.207.130:3306");
//            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
//
//            // set the connection properties
//            urlConnection.setRequestMethod("POST");
//            urlConnection.setDoOutput(true);
//
//            // create a FileInputStream for the file
//            FileInputStream fileInputStream = new FileInputStream(fileToSend);
//
//            // write the data to the output stream
//            OutputStream os = urlConnection.getOutputStream();
//            byte[] buffer = new byte[4096];
//            int bytesRead;
//
//            // read data from the file and write it to the output stream
//            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
//                os.write(buffer, 0, bytesRead);
//            }
//
//            // close streams
//            fileInputStream.close();
//            os.close();
//
//            // get the response from the server (optional)
//            int responseCode = urlConnection.getResponseCode();
//            // you can handle the response code or server response here (did nothing with it)
//            urlConnection.disconnect();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//}

    public void insertData(File csvFile) {

        class SendDataToServerTask extends AsyncTask<File, Void, String> {
            @Override
            protected String doInBackground(File... params) {

                File csvFile = params[0];

                try {
                    //PHP script to handle CSV file
                    URL url = new URL("http://example.com/insert_data.php");
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    // set the connection properties
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestMethod("POST");

                    // Set Content-Type to multipart/form-data
                    urlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + "*****");

                    // Get the output stream of the connection
                    OutputStream outputStream = urlConnection.getOutputStream();
                    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));

                    // Attach CSV file
                    FileInputStream fileInputStream = new FileInputStream(csvFile);
                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    //close streams
                    fileInputStream.close();
                    outputStream.flush();
                    outputStream.close();

                    // get the response from the server (optional)
                    int responseCode = urlConnection.getResponseCode();
                    // Handle the response code or server response here (did nothing with it)
                    urlConnection.disconnect();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return "Data Inserted Successfully";
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                Toast.makeText(MainActivity.this, "Data Submit Successfully", Toast.LENGTH_LONG).show();
            }
        }

        SendDataToServerTask sendPostReqAsyncTask = new SendDataToServerTask();
        sendPostReqAsyncTask.execute(csvFile);
    }

//resetting the text values of the pids
    public void resetValues() {
        engineLoad.setText("0 %");
        voltage.setText("0 V");
        coolantTemperature.setText("0 C°");
        Info.setText("");
        airTemperature.setText("0 C°");
        Maf.setText("0 g/s");
        Fuel.setText("0 - 0 l/h");
        //cause the elm to reinitialize, and clear the array of text
        m_getPids = false;
        whichCommand = 0;
        trycount = 0;
        initialized = false;
        defaultStart = false;
        avgconsumption.clear();
        mConversationArrayAdapter.clear();
    }
    //for connecting the bluetooth
    private void connectDevice(Intent data) {
        tryconnect = true;
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        try {
            // Attempt to connect to the device
            mBtService.connect(device);
            currentdevice = device;

        } catch (Exception e) {
        }
    }

    private void setupChat() {
        // Initialize the BluetoothChatService to perform bluetooth connections
        mBtService = new BluetoothService(this, mBtHandler);
    }

    private void sendEcuMessage(String message) {
        if( mWifiService != null)
        {
            if(mWifiService.isConnected())
            {
                try {
                    if (!message.isEmpty()) {
                        message = message + "\r";
                        byte[] send = message.getBytes();
                        mWifiService.write(send);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }
        else if (mBtService != null)
        {
            // Check that we're actually connected before trying anything
            if (mBtService.getState() != BluetoothService.STATE_CONNECTED) {
                //Toast.makeText(this, R.string.not_connected, Toast.LENGTH_LONG).show();
                return;
            }
            try {
                if (!message.isEmpty()) {

                    message = message + "\r";
                    // Get the message bytes and tell the BluetoothChatService to write
                    byte[] send = message.getBytes();
                    mBtService.write(send);
                }
            } catch (Exception e) {
                Log.w("ECU","Error getting message: " + e.getMessage());
            }
        }
    }

    private void sendInitCommands() {
        if (initializeCommands.length != 0) {

            if (whichCommand < 0) {
                whichCommand = 0;
            }

            String send = initializeCommands[whichCommand];
            sendEcuMessage(send);

            if (whichCommand == initializeCommands.length - 1) {
                initialized = true;
                whichCommand = 0;
                sendDefaultCommands();
            } else {
                whichCommand++;
            }
        }
    }

    private void sendDefaultCommands() {

        if (!commandslist.isEmpty()) {
            //Old condition -> commandslist.size() != 0

            if (whichCommand < 0) {
                whichCommand = 0;
            }

            String send = commandslist.get(whichCommand);
            sendEcuMessage(send);

            if (whichCommand >= commandslist.size() - 1) {
                whichCommand = 0;
            } else {
                whichCommand++;
            }
        }
    }

    //removing certain substrings to clean up the message
    private String clearMsg(Message msg) {
        String tmpmsg = msg.obj.toString();

        tmpmsg = tmpmsg.replace("null", "");
        tmpmsg = tmpmsg.replaceAll("\\s", ""); //removes all [ \t\n\x0B\f\r]
        tmpmsg = tmpmsg.replaceAll(">", "");
        tmpmsg = tmpmsg.replaceAll("SEARCHING...", "");
        tmpmsg = tmpmsg.replaceAll("ATZ", "");
        tmpmsg = tmpmsg.replaceAll("ATI", "");
        tmpmsg = tmpmsg.replaceAll("atz", "");
        tmpmsg = tmpmsg.replaceAll("ati", "");
        tmpmsg = tmpmsg.replaceAll("ATDP", "");
        tmpmsg = tmpmsg.replaceAll("atdp", "");
        tmpmsg = tmpmsg.replaceAll("ATRV", "");
        tmpmsg = tmpmsg.replaceAll("atrv", "");

        return tmpmsg;
    }

    private void checkPids(String tmpmsg) {
        //check if 41 is present in the message, then set index to start from that number in the message and read to the length to check for the message
        if (tmpmsg.contains("41")) {
            //Old condition -> tmpmsg.indexOf("41") != -1
            int index = tmpmsg.indexOf("41");

            String pidmsg = tmpmsg.substring(index, tmpmsg.length());

            if (pidmsg.contains("4100")) {
                //printing the supported pids to the terminal
                setPidsSupported(pidmsg);
                return;
            } else
            {
                //print the pid msg to terminal
                mConversationArrayAdapter.add(pidmsg);
            }
        }
    }

    //new code for calculating the averages of array lists and converting it to a string
    public static String calculateAvgList(ArrayList<Integer> list) {
        //if theres nothing in the list
        if (list == null || list.isEmpty()) {
            return "N/A";
        }

        int sum = 0;
        for (int num : list) {
            sum += num;
        }

        double answer = (double) sum / list.size();
        String s = Double.toString(answer);
        return s;
    }

    private void analyzeMsg(Message msg) {
        //cleaning the message
        String tmpmsg = clearMsg(msg);
        //printing the voltage to terminal
        generateVolt(tmpmsg);
        //getting the device name and the protocol (SAE or ISO)
        getElmInfo(tmpmsg);
        //if the elm is not initialized
        if (!initialized) {
            sendInitCommands();
        } else {
            //check if 41 is present in the message, then set index to start from that number in the message and read to the length to check for the message
            checkPids(tmpmsg);

            if (!m_getPids && m_dedectPids == 1) {
                String sPIDs = "0100";
                sendEcuMessage(sPIDs);
                return;
            }

            if (commandmode) {
                getFaultInfo(tmpmsg);
                return;
            }

            try {
                analyzePIDS(tmpmsg);
            } catch (Exception e) {
                String errorMessage = "Error : " + e.getMessage();
                Info.setText(errorMessage);
            }

            sendDefaultCommands();
        }
    }

    private void getFaultInfo(String tmpmsg) {

            String substr = "43";
            //looking for starting position of 43 within tmpmsg string
            int index = tmpmsg.indexOf(substr);

            if (index == -1)
            {
                substr = "47";
                index = tmpmsg.indexOf(substr);
            }

            if (index != -1) {

                tmpmsg = tmpmsg.substring(index, tmpmsg.length());

                if (tmpmsg.substring(0, 2).equals(substr)) {

                    performCalculations(tmpmsg);

                    String faultCode = null;
                    String faultDesc = null;

                    if (!troubleCodesArray.isEmpty()) {
                        //Old condition -> troubleCodesArray.size() > 0

                        for (int i = 0; i < troubleCodesArray.size(); i++) {
                            faultCode = troubleCodesArray.get(i);
                            faultDesc = troubleCodes.getFaultCode(faultCode);

                            Log.e(TAG, "Fault Code: " + substr + " : " + faultCode + " desc: " + faultDesc);

                            if (faultCode != null && faultDesc != null) {
                                mConversationArrayAdapter.add(mConnectedDeviceName + ":  TroubleCode -> " + faultCode + "\n" + faultDesc);
                            } else if (faultCode != null && faultDesc == null) {
                                mConversationArrayAdapter.add(mConnectedDeviceName + ":  TroubleCode -> " + faultCode +
                                        "\n" + "Definition not found for code: " + faultCode);
                            }
                        }
                    } else {
                        faultCode = "No error found...";
                        mConversationArrayAdapter.add(mConnectedDeviceName + ":  TroubleCode -> " + faultCode);
                    }
                }
            }
    }
//for performing the fault code
    protected void performCalculations(String fault) {

        final String result = fault;
        String workingData = "";
        int startIndex = 0;
        troubleCodesArray.clear();

        try{

            if(result.contains("43")) {
                //result.indexOf("43") != -1
                workingData = result.replaceAll("^43|[\r\n]43|[\r\n]", "");
            }else if(result.contains("47")) {
                //result.indexOf("47") != -1
                workingData = result.replaceAll("^47|[\r\n]47|[\r\n]", "");
            }

            for (int begin = startIndex; begin < workingData.length(); begin += 4) {
                String dtc = "";
                byte b1 = hexStringToByteArray(workingData.charAt(begin));
                int ch1 = ((b1 & 0xC0) >> 6);
                int ch2 = ((b1 & 0x30) >> 4);
                dtc += dtcLetters[ch1];
                dtc += hexArray[ch2];
                dtc += workingData.substring(begin + 1, begin + 4);

                if (dtc.equals("P0000")) {
                    continue;
                }

                troubleCodesArray.add(dtc);
            }
        }catch(Exception e)
        {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }

    private byte hexStringToByteArray(char s) {
        return (byte) ((Character.digit(s, 16) << 4));
    }

    //getting the device name and the protocol
    private void getElmInfo(String tmpmsg) {

        if (tmpmsg.contains("ELM") || tmpmsg.contains("elm")) {
            devicename = tmpmsg;
        }

        if (tmpmsg.contains("SAE") || tmpmsg.contains("ISO")
                || tmpmsg.contains("sae") || tmpmsg.contains("iso") || tmpmsg.contains("AUTO")) {
            deviceprotocol = tmpmsg;
        }

        if (deviceprotocol != null && devicename != null) {
            devicename = devicename.replaceAll("STOPPED", "");
            deviceprotocol = deviceprotocol.replaceAll("STOPPED", "");
            String statusMessage = devicename + " " + deviceprotocol;
            Status.setText(statusMessage);
        }
    }

//printing the supported pids to the terminal
    private void setPidsSupported(String buffer) {

        String infoMessage = "Trying to get available pids : " + String.valueOf(trycount);
        Info.setText(infoMessage);
        trycount++;

        StringBuilder flags = new StringBuilder();
        String buf = buffer;//.toString();
        buf = buf.trim();
        buf = buf.replace("\t", "");
        buf = buf.replace(" ", "");
        buf = buf.replace(">", "");

        if (buf.indexOf("4100") == 0 || buf.indexOf("4120") == 0) {

            for (int i = 0; i < 8; i++) {
                String tmp = buf.substring(i + 4, i + 5);
                int data = Integer.valueOf(tmp, 16).intValue();
//                String retStr = Integer.toBinaryString(data);
                if ((data & 0x08) == 0x08) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }

                if ((data & 0x04) == 0x04) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }

                if ((data & 0x02) == 0x02) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }

                if ((data & 0x01) == 0x01) {
                    flags.append("1");
                } else {
                    flags.append("0");
                }
            }

            commandslist.clear();
            commandslist.add(0, VOLTAGE);
            int pid = 1;

            StringBuilder supportedPID = new StringBuilder();
            supportedPID.append("Supported PIDS:\n");
            for (int j = 0; j < flags.length(); j++) {
                if (flags.charAt(j) == '1') {
                    supportedPID.append(" " + PIDS[j] + " ");
                    if (!PIDS[j].contains("11") && !PIDS[j].contains("01") && !PIDS[j].contains("20")) {
                        commandslist.add(pid, "01" + PIDS[j]);
                        pid++;
                    }
                }
            }
            m_getPids = true;
            mConversationArrayAdapter.add(mConnectedDeviceName + ": " + supportedPID.toString());
            whichCommand = 0;
            sendEcuMessage("ATRV"); //may not need this

        } else {

            return;
        }
    }

    private double calculateAverage(List<Double> listAvg) {
        Double sum = 0.0;
        for (Double val : listAvg) {
            sum += val;
        }
        return sum.doubleValue() / listAvg.size();
    }


    private void analyzePIDS(String dataRecieved) {

        int A = 0;
        int B = 0;
        int PID = 0;

        if ((dataRecieved != null) && (dataRecieved.matches("^[0-9A-F]+$"))) {
            //removes spaces
            dataRecieved = dataRecieved.trim();

            int index = dataRecieved.indexOf("41");
            int index09 = dataRecieved.indexOf("49");

            String tmpmsg = null;
            //calculating values for mode 01
            if (index != -1) {

                tmpmsg = dataRecieved.substring(index, dataRecieved.length());

                if (tmpmsg.substring(0, 2).equals("41")) {

                    PID = Integer.parseInt(tmpmsg.substring(2, 4), 16);
                    A = Integer.parseInt(tmpmsg.substring(4, 6), 16);
                    B = Integer.parseInt(tmpmsg.substring(6, 8), 16);

                    calculateEcuValues(PID, A, B);

                }
            }
            else if (index09 != -1) {

                tmpmsg = dataRecieved.substring(index09, dataRecieved.length());

                if (tmpmsg.substring(0, 2).equals("49")) {

                    PID = Integer.parseInt(tmpmsg.substring(2, 4), 16);
                    A = Integer.parseInt(tmpmsg.substring(4, 6), 16);
                    B = Integer.parseInt(tmpmsg.substring(6, 8), 16);

                    calculateEcuValues(PID, A, B);

                    // Save the data to CSV file (new code to save to CSV file)
                    //may need to change location of code, since values may only be copied from func call
                    //String csvData = PID + "," + A + "," + B + "\n";
                    //calling func to save data to csv file
                    //saveDataToCSV("pid_data.csv", csvData);
                }
            }
        }
    }

//printing the voltage to terminal
    private void generateVolt(String msg) {

        String VoltText = null;

        if ((msg != null) && (msg.matches("\\s*[0-9]{1,2}([.][0-9]{1,2})\\s*"))) {

            VoltText = msg + "V";

            mConversationArrayAdapter.add(mConnectedDeviceName + ": " + msg + "V");

        } else if ((msg != null) && (msg.matches("\\s*[0-9]{1,2}([.][0-9]{1,2})?V\\s*"))) {

            VoltText = msg;

            mConversationArrayAdapter.add(mConnectedDeviceName + ": " + msg);
        }
        //updating text
        if (VoltText != null) {
            voltage.setText(VoltText);
        }
    }
//calculating the pids
    private void calculateEcuValues(int PID, int A, int B) {
        Log.d("EcuValues", "Processing PID: " + PID + ", A: " + A + ", B: " + B);

        double val = 0;
        int intval = 0;
        int tempC = 0;

        switch (PID) {

            case 4://PID(04): Engine Load

                // A*100/255
                val = A * 100 / 255;
                int calcLoad = (int) val;
                //for setting text to int value
                engineLoad.setText(Integer.toString(calcLoad) + " %");
                //adding string to terminal
                mConversationArrayAdapter.add("Engine Load: " + Integer.toString(calcLoad) + " %");

                double FuelFlowLH = (mMaf * calcLoad * mEnginedisplacement / 1000.0 / 714.0) + 0.8;

                if(calcLoad == 0)
                    FuelFlowLH = 0;

                avgconsumption.add(FuelFlowLH);

                String fuelMessage = String.format("%10.1f", calculateAverage(avgconsumption)).trim() + " l/h";
                Fuel.setText(fuelMessage);
                mConversationArrayAdapter.add("Fuel Consumption: " + String.format("%10.1f", calculateAverage(avgconsumption)).trim() + " l/h");
                break;

            case 5://PID(05): Coolant Temperature

                // A-40
                tempC = A - 40;
                coolantTemp = tempC;
                String coolantMessage = Integer.toString(coolantTemp) + " C°";
                coolantTemperature.setText(coolantMessage);
                mConversationArrayAdapter.add("Enginetemp: " + Integer.toString(tempC) + " C°");

                break;

            case 11://PID(0B)

                // A
                mConversationArrayAdapter.add("Intake Man Pressure: " + Integer.toString(A) + " kPa");

                break;

            case 12: //PID(0C): RPM

                //((A*256)+B)/4
                val = ((A * 256) + B) / 4;
                intval = (int) val;
                rpmval = intval;
//                rpm.setTargetValue(intval / 100);
                //new code to add to array
                mConversationArrayAdapter.add("Engine Speed: " + Integer.toString(rpmval) + " rpm");

                break;


            case 13://PID(0D): KM

                // A
//                speed.setTargetValue(A);
                //new code to add to array
                mConversationArrayAdapter.add("Vehicle Speed: " + Integer.toString(A) + " km/h");
                //new code to add to an array list for the averages to be sent to DB
                km_speed.add(A);
                break;

            case 15://PID(0F): Intake Temperature

                // A - 40
                tempC = A - 40;
                intakeairtemp = tempC;
                String airMessage = Integer.toString(intakeairtemp) + " C°";
                airTemperature.setText(airMessage);
                mConversationArrayAdapter.add("Intakeairtemp: " + Integer.toString(intakeairtemp) + " C°");

                break;

            case 16://PID(10): Maf

                // ((256*A)+B) / 100  [g/s]
                val = ((256 * A) + B) / 100;
                mMaf = (int) val;
                String mafMessage = Integer.toString(intval) + " g/s";
                Maf.setText(mafMessage);
                mConversationArrayAdapter.add("Maf Air Flow: " + Integer.toString(mMaf) + " g/s");

                break;

            case 17://PID(11)

                //A*100/255
                val = A * 100 / 255;
                intval = (int) val;
                mConversationArrayAdapter.add(" Throttle position: " + Integer.toString(intval) + " %");

                break;

            case 35://PID(23)

                // ((A*256)+B)*0.079
                val = ((A * 256) + B) * 0.079;
                intval = (int) val;
                mConversationArrayAdapter.add("Fuel Rail Pressure: " + Integer.toString(intval) + " kPa");

                break;

            case 49://PID(31)

                //(256*A)+B km
                val = (A * 256) + B;
                intval = (int) val;
                mConversationArrayAdapter.add("Distance traveled: " + Integer.toString(intval) + " km");

                break;

            case 70://PID(46)

                // A-40 [DegC]
                tempC = A - 40;
                ambientairtemp = tempC;
                mConversationArrayAdapter.add("Ambientairtemp: " + Integer.toString(ambientairtemp) + " C°");

                break;

            case 92://PID(5C)

                //A-40
                tempC = A - 40;
                engineoiltemp = tempC;
                mConversationArrayAdapter.add("Engineoiltemp: " + Integer.toString(engineoiltemp) + " C°");

                break;

            default:
        }
    }

    enum RSP_ID {
        PROMPT(">"),
        OK("OK"),
        MODEL("ELM"),
        NODATA("NODATA"),
        SEARCH("SEARCHING"),
        ERROR("ERROR"),
        NOCONN("UNABLE"),
        NOCONN_MSG("UNABLE TO CONNECT"),
        NOCONN2("NABLETO"),
        CANERROR("CANERROR"),
        CONNECTED("ECU CONNECTED"),
        BUSBUSY("BUSBUSY"),
        BUSY("BUSY"),
        BUSERROR("BUSERROR"),
        BUSINIERR("BUSINIT:ERR"),
        BUSINIERR2("BUSINIT:BUS"),
        BUSINIERR3("BUSINIT:...ERR"),
        BUS("BUS"),
        FBERROR("FBERROR"),
        DATAERROR("DATAERROR"),
        BUFFERFULL("BUFFERFULL"),
        STOPPED("STOPPED"),
        RXERROR("<"),
        QMARK("?"),
        UNKNOWN("");
        private String response;

        RSP_ID(String response) {
            this.response = response;
        }

        @Override
        public String toString() {
            return response;
        }
    }
}