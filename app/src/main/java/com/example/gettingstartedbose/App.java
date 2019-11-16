package com.example.gettingstartedbose;

import android.app.Application;

import com.bose.wearable.BoseWearable;
import com.bose.wearable.Config;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BoseWearable.configure(this, new Config.Builder().build());
    }
}
