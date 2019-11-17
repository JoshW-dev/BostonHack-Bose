package com.example.KMWapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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
import com.bose.wearable.sensordata.Quaternion;
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

import android.provider.MediaStore;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.Timer;

public class MainActivity extends AppCompatActivity {

    private static final int AUTO_CONNECT_TIMEOUT = 5; // In seconds, use 0 to disable automatic reconnection
    private static final int REQUEST_CODE_CONNECTOR = 1;

    DecimalFormat df = new DecimalFormat("#.####");

    public boolean first = true;
    public boolean awareMode = true;
    public double time_1 =0;
    public double time_2 =0;
    public double dt =0;
    public Quaternion initialOrientation;
    public Quaternion currentOrientation;
    public boolean volumeToggle;
    public View state;
    public View gesture;
    public ImageView listen;
    public ImageView mute;
    public Button disable;
    public ImageView add;
    public ImageView subtract;
    //both quats
    public double yawDiff =0;

    public int driftedCount =0;//keep track of small yaw drift to reset heading naturally
    public double yawDrift =0;
    public double roll=0;
    public int initVOl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        disable = (Button)findViewById(R.id.disable);
        state = findViewById(R.id.textView4);
        gesture = findViewById(R.id.textView5);
        listen = findViewById(R.id.Listen);
        mute = findViewById(R.id.Mute);
        add = findViewById(R.id.add);
        subtract = findViewById(R.id.minus);
//        Intent photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        startActivityForResult(photoIntent, );
    }

    public void connect(View v) {
        Set<SensorType> sensorTypeSet = new ArraySet<>(Arrays.asList(SensorType.ACCELEROMETER, SensorType.GYROSCOPE, SensorType.ROTATION_VECTOR, SensorType.GAME_ROTATION_VECTOR));

        SensorIntent sensorIntent = new SensorIntent(sensorTypeSet, Collections.singleton(SamplePeriod._320_MS));

        GestureIntent gestureIntent = new GestureIntent(Collections.singleton(GestureType.INPUT));

        Intent intent = DeviceConnectorActivity.newIntent(this, AUTO_CONNECT_TIMEOUT, sensorIntent, gestureIntent);

        startActivityForResult(intent, REQUEST_CODE_CONNECTOR);

        gesture.setVisibility(View.VISIBLE);
        state.setVisibility(View.VISIBLE);
        listen.setVisibility(View.VISIBLE);

    }
    AudioManager audio;

    public void buttonOnClick() {
        initVOl = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int curVol = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        int minVol = (int) (curVol*0.6);
        //Button button=(Button)v;
        //((Button) v).setText("clicked");
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, Math.min(minVol, (int)(maxVol*0.45)), 0); //AudioManager.ADJUST_MUTE

    }
    public void buttonOnClick2() {
       // Button button=(Button)v;
        //((Button) v).setText("clicked");
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, initVOl, 1);
    }
    public void buttonOnClickAwareness(View v) {
        awareMode =! awareMode;

        if(awareMode == false){
            disable.setText("Enable");
        } else {
            disable.setText("Disable");
        }
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
            TextView gyro = findViewById(R.id.textView4);
            TextView gam = findViewById(R.id.textView5);
            switch (sensorData.sensorType()) {
                case ACCELEROMETER:
//                     Handle accelerometer reading
                 //   Log.d("Accelerometer", sensorData.toString());
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

                    //quat shit
                    if(first){
                        initialOrientation= sensorData.quaternion();
                        first = false;
                        volumeToggle = true;
                    }//first is measured once then constant
                    currentOrientation = sensorData.quaternion();
                    Quaternion diff = quatDifference(initialOrientation, currentOrientation);//quaternion rotation difference between initial orientation and current

/*
                    Log.d("Quat", "current: " + currentOrientation);
                    Log.d("Eul", "eul diff (x,y,z): "
                            + diff.xRotation()*180/3.1415 + ", "
                            +diff.yRotation()*180/3.1415 + ", "
                            + diff.zRotation()*180/3.1415);//yaw diff from initial
  */


                    yawDrift = diff.zRotation();//in rads
                    roll = currentOrientation.yRotation()*180/3.14159;//in deg
                    if(awareMode) {
                        if (yawDrift * 180 / 3.1415 > 40) {
                            driftedCount = 0;
                            gyro.setText("Aware");
                            if (volumeToggle == true) {
                                buttonOnClick();
                                listen.setVisibility(View.INVISIBLE);
                                mute.setVisibility(View.VISIBLE);
                                volumeToggle = false;
                            }
                        } else if (yawDrift * 180 / 3.1415 < -40) {
                            driftedCount = 0;
                            gyro.setText("Aware");
                            if (volumeToggle == true) {
                                buttonOnClick();
                                listen.setVisibility(View.INVISIBLE);
                                mute.setVisibility(View.VISIBLE);
                                volumeToggle = false;
                            }
                        } else {
                            gyro.setText("Focused");
                            driftedCount++;
                            if (volumeToggle == false) {
                                buttonOnClick2();
                                listen.setVisibility(View.VISIBLE);
                                mute.setVisibility(View.INVISIBLE);
                                volumeToggle = true;
                            }
                        }
                        if (driftedCount > 15 && yawDrift * 180 / 3.1415 < 20) {
                            initialOrientation = currentOrientation;
                            driftedCount = 0;
                        }
                    }
                        if(roll>25){
                            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, 1, 0); //raise volume
                            if(subtract.getVisibility() != View.INVISIBLE){
                                subtract.setVisibility(View.INVISIBLE);
                            }
                            if(add.getVisibility() != View.VISIBLE) {
                                add.setVisibility(View.VISIBLE);
                            }

                        }else
                        if(roll<-20){
                            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, -1, 0); //raise volume
                            if(add.getVisibility() != View.INVISIBLE){
                                add.setVisibility(View.INVISIBLE);
                            }
                            if(subtract.getVisibility() != View.VISIBLE) {
                                subtract.setVisibility(View.VISIBLE);
                            }

                        }else {
                            if(add.getVisibility() != View.INVISIBLE){
                                add.setVisibility(View.INVISIBLE);
                            }
                            if(subtract.getVisibility() != View.INVISIBLE){
                                subtract.setVisibility(View.INVISIBLE);
                            }
                        }

                    //       Log.d("Game", "_________________________________");


//                    Log.d("Game", "x: " + sensorData.quaternion().zRotation());
                    break;
                case GYROSCOPE:
//                     Handle gyroscope reading
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
            if(gestureData.type().toString().equals("Double Tap")){
                initialOrientation = currentOrientation;
                Log.d("Heading", ""+ "reset to current");
            }

            }


    };//on sensor read
    public Quaternion quatDifference( Quaternion quat2, Quaternion quat1){
        //calculate and return quat1-quat2 diff
        //ie go from quat2 to quat 1
        double q1a = quat1.w();
        double q1b = quat1.x();
        double q1c = quat1.y();
        double q1d = quat1.z();

        Quaternion quat2C = quat2.inverted();

        double q2a = quat2C.w();
        double q2b= quat2C.x();
        double q2c = quat2C.y();
        double q2d = quat2C.z();

        final double w = q1a * q2a - q1b * q2b - q1c * q2c - q1d * q2d;
        final double x = q1a * q2b + q1b * q2a + q1c * q2d - q1d * q2c;
        final double y = q1a * q2c - q1b * q2d + q1c * q2a + q1d * q2b;
        final double z = q1a * q2d + q1b * q2c - q1c * q2b + q1d * q2a;

        Quaternion quatOut = new Quaternion(x,y,z,w);
        return quatOut;
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
