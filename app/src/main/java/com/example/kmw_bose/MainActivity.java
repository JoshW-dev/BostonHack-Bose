package com.example.kmw_bose;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.bose.blecore.BluetoothManager;
import com.bose.blecore.ScanError;
import com.bose.blecore.Session;
import com.bose.bosewearableui.DeviceConnectorActivity;
import com.bose.wearable.BoseWearable;
import com.bose.wearable.sensordata.GestureIntent;
import com.bose.wearable.sensordata.SensorIntent;
import com.bose.wearable.services.wearablesensor.GestureType;
import com.bose.wearable.services.wearablesensor.SamplePeriod;
import com.bose.wearable.services.wearablesensor.SensorType;
import com.bose.wearable.wearabledevice.WearableDevice;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.ArraySet;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final int AUTO_CONNECT_TIMEOUT = 5; // In seconds, use 0 to disable automatic reconnection
    private static final int REQUEST_CODE_CONNECTOR = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CONNECTOR) {
            if (resultCode == Activity.RESULT_OK) {
                String deviceAddress = data.getStringExtra(DeviceConnectorActivity.CONNECTED_DEVICE);
                BluetoothManager btManager = BoseWearable.getInstance().bluetoothManager();
                Session session = btManager.session(btManager.deviceByAddress(deviceAddress));
                WearableDevice wearableDevice = (WearableDevice) session.device();

                // session is opened at this point and ready to use.
                // It is up to the application to close the session when it is no longer needed.
            } else if (resultCode == DeviceConnectorActivity.RESULT_SCAN_ERROR) {
                ScanError scanError = (ScanError) data.getSerializableExtra(DeviceConnectorActivity.FAILURE_REASON);
                // An error occurred when searching for a device.
                // Present an error to the user.
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // The user cancelled the search operation.
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.M)
    private void connect() {
        // Tell the Connector UI which sensors and gestures are required by the app
        Set<SensorType> sensorTypes = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            sensorTypes = new ArraySet<>(
                    Arrays.asList(SensorType.ACCELEROMETER,
                            SensorType.ROTATION_VECTOR)
            );
        }
        SensorIntent sensorIntent = new SensorIntent(sensorTypes, Collections.singleton(SamplePeriod._20_MS));

        GestureIntent gestureIntent = new GestureIntent(Collections.singleton(GestureType.INPUT));

        // Start the connector Activity
        Intent intent = DeviceConnectorActivity.newIntent(this, AUTO_CONNECT_TIMEOUT,
                sensorIntent, gestureIntent);

        startActivityForResult(intent, REQUEST_CODE_CONNECTOR);
    }
}
