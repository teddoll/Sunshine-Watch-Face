package com.example.android.sunshine.app.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WatchDataUpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("WatchDataUpdateReceiver", "received updated");
        context.startService(new Intent(context, WatchDataUpdateService.class));
    }
}
