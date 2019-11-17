package com.example.KMWapp;

import android.app.Application;
import android.util.Log;
import android.content.Context;
import android.media.AudioManager;
import com.bose.wearable.BoseWearable;
import com.bose.wearable.Config;

public class App extends Application {

    @Override
    public void onCreate() {
        System.out.println("Started");
        super.onCreate();
        BoseWearable.configure(this, new Config.Builder().build());
        AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    }
}
