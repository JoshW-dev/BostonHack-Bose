package com.example.KMWapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;

import com.bose.blecore.BluetoothManager;
import com.bose.blecore.DeviceException;
import com.bose.blecore.Session;
import com.bose.blecore.SessionDelegate;
import com.bose.bosewearableui.DeviceConnectorActivity;
import com.bose.wearable.BoseWearable;
import com.bose.wearable.BoseWearableException;
import com.bose.wearable.sensordata.GestureData;
import com.bose.wearable.sensordata.GestureIntent;
import com.bose.wearable.sensordata.SensorIntent;
import com.bose.wearable.sensordata.SensorValue;
import com.bose.wearable.services.wearablesensor.GestureConfiguration;
import com.bose.wearable.services.wearablesensor.GestureType;
import com.bose.wearable.services.wearablesensor.SamplePeriod;
import com.bose.wearable.services.wearablesensor.SensorConfiguration;
import com.bose.wearable.services.wearablesensor.SensorType;
import com.bose.wearable.wearabledevice.BaseWearableDeviceListener;
import com.bose.wearable.wearabledevice.WearableDevice;
import com.bose.wearable.wearabledevice.WearableDeviceListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Button;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int AUTO_CONNECT_TIMEOUT = 5; // In seconds, use 0 to disable automatic reconnection
    private static final int REQUEST_CODE_CONNECTOR = 1;

    DecimalFormat df = new DecimalFormat("#.####");

    public double time_1 =0;
    public double time_2 =0;
    public double dt =0;
    public double yaw_1 =0;
    public double yaw_2 =0;
    public double dYaw =0;
    public double yawIntegral =0;
    public int initVOl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        TextView textview = findViewById(R.id.textView3);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect();
            }
        });
        audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    }

    private void connect() {
        Set<SensorType> sensorTypeSet = new ArraySet<>(Arrays.asList(SensorType.ACCELEROMETER, SensorType.GYROSCOPE, SensorType.ROTATION_VECTOR, SensorType.GAME_ROTATION_VECTOR));

        SensorIntent sensorIntent = new SensorIntent(sensorTypeSet, Collections.singleton(SamplePeriod._320_MS));

        GestureIntent gestureIntent = new GestureIntent(Collections.singleton(GestureType.INPUT));

        Intent intent = DeviceConnectorActivity.newIntent(this, AUTO_CONNECT_TIMEOUT, sensorIntent, gestureIntent);

        startActivityForResult(intent, REQUEST_CODE_CONNECTOR);
    }
    AudioManager audio;

    public void buttonOnClick(View v) {
        initVOl = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        Button button=(Button)v;
        ((Button) v).setText("clicked");
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);

    }
    public void buttonOnClick2(View v) {
        Button button=(Button)v;
        ((Button) v).setText("clicked");
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, initVOl, 1);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == REQUEST_CODE_CONNECTOR) {
            if (resultCode == Activity.RESULT_OK) {
                String deviceAddress = data.getStringExtra(DeviceConnectorActivity.CONNECTED_DEVICE);
                BluetoothManager btManager = BoseWearable.getInstance().bluetoothManager();
                Session session = btManager.session(btManager.deviceByAddress(deviceAddress));
                WearableDevice wearableDevice = (WearableDevice) session.device();

                session.callback(new SessionDelegate() {
                    @Override
                    public void sessionConnected(@NonNull Session session) {
                        Log.d("MainActivity", "session connected " + session.device().toString());
                    }

                    @Override
                    public void sessionClosed(int statusCode) {
                        Log.d("MainActivity", "session closed " + statusCode);
                    }

                    @Override
                    public void sessionError(@NonNull DeviceException e) {
                        Log.d("MainActivity", "session error " + e.getMessage());
                    }
                });

                wearableDevice.addListener(wearableDeviceListener);

                // Enable double tap gesture
                GestureConfiguration config = wearableDevice.gestureConfiguration()
                        .disableAll()
                        .gestureEnabled(GestureType.DOUBLE_TAP, true)
                        .gestureEnabled(GestureType.SINGLE_TAP, true)
                        .gestureEnabled(GestureType.HEAD_NOD, true)
                        .gestureEnabled(GestureType.HEAD_SHAKE, true);


                wearableDevice.changeGestureConfiguration(config);

                // Enable accelerometer and gyroscope
                SamplePeriod samplePeriod = SamplePeriod._320_MS;
                SensorConfiguration configuration = wearableDevice.sensorConfiguration()
                        .disableAll()
                        .enableSensor(SensorType.ACCELEROMETER, samplePeriod)
                        .enableSensor(SensorType.GYROSCOPE, samplePeriod)
                        .enableSensor(SensorType.ROTATION_VECTOR, samplePeriod)
                        .enableSensor(SensorType.GAME_ROTATION_VECTOR, samplePeriod);
                wearableDevice.changeSensorConfiguration(configuration);
            } else if (resultCode == DeviceConnectorActivity.RESULT_SCAN_ERROR) {
//                ScanError scanError = (ScanError) data.getSerializableExtra(DeviceConnectorActivity.FAILURE_REASON);

                // pop-up error for the user
//                Snackbar.make(getWindow().getDecorView().getRootView(), "error connecting", Snackbar.LENGTH_SHORT).show();
            } else if (resultCode == DeviceConnectorActivity.RESULT_CANCELED) {

                // pop up user canceled dialog
//                Snackbar.make(getWindow().getDecorView().getRootView(), "connection cancelled", Snackbar.LENGTH_SHORT).show();
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    WearableDeviceListener wearableDeviceListener = new BaseWearableDeviceListener() {

        @Override
        public void onSensorConfigurationRead(@NonNull SensorConfiguration sensorConfiguration) {
            // Sensor configuration has been updated.
        }

        @Override
        public void onSensorConfigurationChanged(@NonNull SensorConfiguration sensorConfiguration) {
            // Sensor configuration change was accepted.
        }

        @Override
        public void onSensorConfigurationError(@NonNull BoseWearableException wearableException) {
            // Sensor configuration change was rejected with the specified exception.
        }

        @Override
        public void onSensorDataRead(@NonNull SensorValue sensorData) {
            TextView accel = findViewById(R.id.textView3);
            TextView gyro = findViewById(R.id.textView4);
            TextView gam = findViewById(R.id.textView5);
            TextView rot = findViewById(R.id.textView6);
            switch (sensorData.sensorType()) {
                case ACCELEROMETER:
//                     Handle accelerometer reading
                 //   Log.d("Accelerometer", sensorData.toString());
                    accel.setText(sensorData.vector().toString());
                    if (sensorData.vector() == null) {
                        Log.d("Accelerometer", "vector value null");
                        return;
                    }

                  //  Log.d("Accelerometer", "x: " + sensorData.vector().x());
//                    Log.d("Accelerometer", "y: " + sensorData.vector().y());
//                    Log.d("Accelerometer", "z: " + sensorData.vector().z());

                    break;
                case ROTATION_VECTOR:
                    // Handle gyroscope reading
                   rot.setText(sensorData.quaternion().toString());
                  //  Log.d("Rotation", sensorData.toString());
                    if (sensorData.quaternion() == null) {
                        Log.d("Rotation", "Q value null");
                        return;
                    }

                 //   Log.d("Rotation", "x: " + sensorData.quaternion().pitch());
                    break;
                case GAME_ROTATION_VECTOR:
                    // Handle gyroscope reading

//                    Log.d("Game", sensorData.toString());
                    if (sensorData.quaternion() == null) {
                        Log.d("Game", "Q value null");
                        return;
                    }

//                    Log.d("Game", "x: " + sensorData.quaternion().xRotation());
//                    Log.d("Game", "_________________________________");

                    gam.setText("X: "+String.valueOf(df.format(sensorData.quaternion().x())) + "\n"
                                    +"Y: "+String.valueOf(df.format(sensorData.quaternion().y())) + "\n"
                            +"Z: "+String.valueOf(df.format(sensorData.quaternion().z())) + "\n"
                            +"W: "+String.valueOf(df.format(sensorData.quaternion().w())));
                 //   Log.d("Time", "stamp: " + sensorData.timestamp());
                    time_1 = time_2;
                    time_2 =sensorData.timestamp();
                    dt = (time_2-time_1)/1000;// in s

                    yaw_1 = yaw_2;
                    yaw_2 = sensorData.quaternion().zRotation();
                    dYaw =yaw_2-yaw_1;
                    //Log.d("time", "dt: " + dt);


                    //       Log.d("Game", "_________________________________");


//                    Log.d("Game", "x: " + sensorData.quaternion().zRotation());
                    break;
                case GYROSCOPE:
//                     Handle gyroscope reading
               gyro.setText(sensorData.vector().toString());

              //      Log.d("Gyroscope", sensorData.toString());
                    if (sensorData.vector() == null) {
                        Log.d("Gyroscope", "Q value null");
                        return;
                    }

             //       Log.d("Gyroscope", "x: " + sensorData.vector().x());
                    break;
            }
        }


        //gestures
        @Override
        public void onGestureConfigurationRead(@NonNull GestureConfiguration gestureConfiguration) {
            // Gesture configuration has been updated,
        }

        @Override
        public void onGestureConfigurationChanged(@NonNull GestureConfiguration gestureConfiguration) {
            // Gesture configuration change was accepted.
        }

        @Override
        public void onGestureConfigurationError(@NonNull BoseWearableException wearableException) {
            // Gesture configuration change was rejected with the specified exception.
        }

        @Override
        public void onGestureDataRead(@NonNull GestureData gestureData) {
            // Gesture received.
            Log.d("Gesture", "" + gestureData.toString());
        }

    };//on sensor read

    public double[] quatConjugate(double[] quatIn){
        double w = quatIn[0];
        double x = quatIn[1];
        double y = quatIn[2];
        double z = quatIn[3];
        double quatMag = Math.pow(w,2)+ Math.pow(x,2)+Math.pow(y,2)+Math.pow(z,2);
        double[] quatOut = {w/quatMag, -x/quatMag, -y/quatMag, -z/quatMag};
        return quatOut;
    }
    public double[] quatDifference(double[] quat1, double[] quat2){
        //calculate and return quat2-quat1 diff
        double q1a = quat1[0];
        double q1b = quat1[1];
        double q1c = quat1[2];
        double q1d = quat1[3];

        double[] quat2C = quatConjugate(quat2);

        double q2a = quat2C[0];
        double q2b= quat2C[1];
        double q2c = quat2C[2];
        double q2d = quat2C[3];

        final double w = q1a * q2a - q1b * q2b - q1c * q2c - q1d * q2d;
        final double x = q1a * q2b + q1b * q2a + q1c * q2d - q1d * q2c;
        final double y = q1a * q2c - q1b * q2d + q1c * q2a + q1d * q2b;
        final double z = q1a * q2d + q1b * q2c - q1c * q2b + q1d * q2a;

        double[] quatOut = {w,x,y,z};
        return quatOut;
    }
    public double [] quatToEuler(double[] quat){

        double w = quat[0];
        double x = quat[1];
        double y = quat[2];
        double z = quat[3];

        double sqw = w*w;
        double sqx = x*x;
        double sqy = y*y;
        double sqz = z*z;
        double yaw = Math.atan2(2.0 * (x*y + z*w),(sqx - sqy - sqz + sqw));
        double roll = Math.atan2(2.0 * (y*z + x*w),(-sqx - sqy + sqz + sqw));
        double pitch= Math.asin(-2.0 * (x*z - y*w));

        double[] eul = {roll, pitch, yaw};
        return eul;
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
}
