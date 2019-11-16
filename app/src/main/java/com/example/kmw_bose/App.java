package com.example.kmw_bose;

import android.app.Application;

import com.bose.wearable.BoseWearable;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BoseWearable.configure(this);
    }
}