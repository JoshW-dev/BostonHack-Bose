package com.example.gettingstartedbose;

import android.app.Application;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.bose.wearable.BoseWearable;
import com.bose.wearable.Config;
public class VolFunc {
    public static void changeVol() {
        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        System.out.println("This function changes volume based on head rotation");
//        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

    }
}
