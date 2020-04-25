package fdf.af.tamir.btapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.view.View.OnClickListener;
import android.support.v7.app.ActionBarActivity;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

public class MainActivity extends ActionBarActivity implements OnClickListener {



    Button buttonmagic;
    Switch onOff;
    ProgressBar loadingBar;
    TextView creditView;
    Typeface myTypeface;

    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private static final String BT_NAME = "HC-06";
    private boolean pressed = false;
    private boolean abort = false;
    private boolean isConnected = false;
    private boolean openClosePointer = false;
    private boolean threadIsRunning = false;
    private Intent turnOn;
    private BluetoothDevice mMyDevice;
    private static final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private BluetoothSocket mSocket;
    private BufferedReader mmBInStream;
    //private Handler mBluetoothHandler = new Handler();
    //private Runnable mConnectRunnable;
    private Thread connectThread;


    private BroadcastReceiver mReceiver;

    ListView lv;
    ArrayList deviceList;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Buttons!
        onOff = (Switch) findViewById(R.id.onoffswitch);
        onOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    TurnOnBlueTooth();
                } else {
                    TurnOffBlueTooth();
                }
            }
        });
        buttonmagic = (Button) findViewById(R.id.buttonmagic);
        buttonmagic.setOnClickListener(this);
        //---
        loadingBar = (ProgressBar) findViewById(R.id.loadingBar);
        creditView = (TextView) findViewById(R.id.creditView);

        myTypeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Thin.ttf");
        buttonmagic.setTypeface(myTypeface);
        onOff.setTypeface(myTypeface);
        creditView.setTypeface(myTypeface);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceList = new ArrayList();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //threadIsRunning = false;
        if (mBluetoothAdapter.isEnabled()) {
            onOff.setChecked(true);
        }
        else {
            onOff.setChecked(false);
        }

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            onOff.setChecked(false);
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            break;
                        case BluetoothAdapter.STATE_ON:
                            onOff.setChecked(true);
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            break;
                    }
                }
            }
        };

        openClosePointer = false;
        pressed = false;
        buttonmagic.setText("Gate");
        loadingBar.setVisibility(View.INVISIBLE);
        isConnected = false;

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSocket != null && mSocket.isConnected()) {
            try {
                mSocket.close();
            } catch (IOException e) {
                return;
            }
        }

        unregisterReceiver(mReceiver);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            /*
            case R.id.buttonconnect:
                openClosePointer = false;
                if (!threadIsRunning)
                    Connect();
                return; */

            case R.id.buttonmagic:
                handleMagicButton();
                return;
        }
    }

    private void handleMagicButton() {
        if (pressed) {
            openClosePointer = false;
            pressed = false;
            buttonmagic.setText("Gate");
            loadingBar.setVisibility(View.INVISIBLE);
        } else {

            if (!mBluetoothAdapter.isEnabled()) {
                TurnOnBlueTooth();
            } else {
                pressed = true;
                buttonmagic.setText("Cancel");
                openClosePointer = true;
                if (!threadIsRunning) {
                    if (isConnected) {
                        Open_Close();
                        buttonmagic.setText("Gate");
                    } else if (!threadIsRunning)
                        Connect();
                } else {
                    loadingBar.setVisibility(View.VISIBLE);
                }
            }
        }
    }


    private void Open_Close() {
        if (mmOutStream != null) {
            try {
                mmOutStream.write('1');
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Writing failed", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Toast.makeText(getApplicationContext(), "Not connected", Toast.LENGTH_SHORT).show();
        }
    }

    private void Connect() {
        if (mBluetoothAdapter.isEnabled() && !threadIsRunning && (mSocket == null || !mSocket.isConnected())) {
            pairedDevices = mBluetoothAdapter.getBondedDevices();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice bt : pairedDevices) {
                    if (bt.getName().equals(BT_NAME)) {
                        mMyDevice = bt;
                        //Toast.makeText(getApplicationContext(), "Device Found", Toast.LENGTH_SHORT).show();
                        threadIsRunning = true;
                        loadingBar.setVisibility(View.VISIBLE);
                        connectThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ConnectToDevice();
                                runOnUiThread(new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      connectFinished();
                                                      threadIsRunning = false;
                                                  }
                                              }
                                );
                            }
                        });
                        connectThread.start();
                        break;
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), "No Paired Devices", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void connectFinished() {
        if (openClosePointer)
            Open_Close();
        openClosePointer = false;
        loadingBar.setVisibility(View.INVISIBLE);
        buttonmagic.setText("Gate");
        pressed = false;
    }


    private void TurnOffBlueTooth() {
        if (mSocket != null && mSocket.isConnected()) {
            try {
                mSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Socket didn't close", Toast.LENGTH_LONG).show();
            }
        }
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            //Toast.makeText(getApplicationContext(), "Turned off", Toast.LENGTH_LONG).show();
        } else {
            //Toast.makeText(getApplicationContext(), "Already off", Toast.LENGTH_LONG).show();
        }
        if (!deviceList.isEmpty()) {
            deviceList.clear();
            final ArrayAdapter mArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList);
            lv.setAdapter(mArrayAdapter);
        }
        isConnected = false;
        pressed = false;
        abort = false;
    }

    private void TurnOnBlueTooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            //Toast.makeText(getApplicationContext(), "Turning on", Toast.LENGTH_LONG).show();
        } else {
            //Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
        }
    }

    private void ConnectToDevice() {
        UUID uuid = UUID.fromString(MY_UUID);
        mBluetoothAdapter.cancelDiscovery();
        try {
            mSocket = mMyDevice.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Connection succeeded!", Toast.LENGTH_SHORT).show();
                }
            });
            // @TODO Maybe change to secure?
        } catch (IOException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Connection failed", Toast.LENGTH_SHORT).show();
                }
            });
            try {
                mSocket.close();
            } catch (IOException eio) {
            }
            return;
        }

        InputStream tmpIn;
        OutputStream tmpOut;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = mSocket.getInputStream();
            tmpOut = mSocket.getOutputStream();
        } catch (IOException e) {
            //Toast.makeText(getApplicationContext(), "Something failed", Toast.LENGTH_SHORT).show();
            return;
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        //InputStreamReader isr = ;
        mmBInStream = new BufferedReader(new InputStreamReader(mmInStream));
        isConnected = true;
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://fdf.af.tamir.btapp/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://fdf.af.tamir.btapp/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    /*
    private class ConnectToBT extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            Connect();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!abort && isConnected) {
                Open_Close();
            }

            abort = false;
            buttonmagic.setText("Gate");
            pressed = false;
        }
    }
    */
}


