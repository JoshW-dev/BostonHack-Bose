package com.example.gettingstartedbose;

import android.app.Application;
import android.util.Log;

import com.bose.wearable.BoseWearable;
import com.bose.wearable.Config;

public class App extends Application {

    @Override
    public void onCreate() {
        System.out.println("Started");
        super.onCreate();
        BoseWearable.configure(this, new Config.Builder().build());
        Log.d("myTag", "App has started.");
    }
}
