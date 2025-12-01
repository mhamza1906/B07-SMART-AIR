package com.example.smart_air;

import android.os.Handler;
import android.os.Looper;

public class CooldownHelper {

    public interface CooldownCallback {
        void onCooldownFinished();
    }

    public static void startCooldown(long durationMillis, CooldownCallback callback) {
        new Handler(Looper.getMainLooper()).postDelayed(callback::onCooldownFinished, durationMillis);
    }
}
