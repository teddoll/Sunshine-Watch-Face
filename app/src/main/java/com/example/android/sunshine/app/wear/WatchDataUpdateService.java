package com.example.android.sunshine.app.wear;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Deque;
import java.util.LinkedList;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class WatchDataUpdateService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
    };
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;

    private static final String TAG = WatchDataUpdateService.class.getSimpleName();

    private static final String PREFS = WatchDataUpdateService.class.getName() + ".prefs";

    private static final String PREFS_ID = "id";
    private static final String PREFS_MAX = "max";
    private static final String PREFS_MIN = "min";

    private GoogleApiClient mGoogleApiClient;
    private SharedPreferences prefs;

    private Deque<PutDataMapRequest> queue;
    public WatchDataUpdateService() {
        super("WatchDataUpdateService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.blockingConnect();
        }
        if (prefs == null) {
            prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        }

        checkWeatherUpdate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    private void checkWeatherUpdate() {
        Log.d(TAG, "checkWeatherUpdate");
        // Get today's data from the ContentProvider
        String location = Utility.getPreferredLocation(this);
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                location, System.currentTimeMillis());
        Cursor data = getContentResolver().query(weatherForLocationUri, FORECAST_COLUMNS, null,
                null, WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");
        if (data == null) {
            return;
        }
        if (!data.moveToFirst()) {
            data.close();
            return;
        }

        processWeatherData(data);

    }

    private void processWeatherData(Cursor data) {
        // Extract the weather data from the Cursor
        int weatherId = data.getInt(INDEX_WEATHER_ID);
        double maxTemp = data.getDouble(INDEX_MAX_TEMP);
        double minTemp = data.getDouble(INDEX_MIN_TEMP);
        String formattedMaxTemperature = Utility.formatTemperature(this, maxTemp);
        String formattedMinTemperature = Utility.formatTemperature(this, minTemp);
        data.close();
        if (dataHasChanged(weatherId, formattedMaxTemperature, formattedMinTemperature)) {
            updateWeather(weatherId, formattedMaxTemperature, formattedMinTemperature);
        }


    }

    private boolean dataHasChanged(int weatherId, String weatherMax, String weatherMin) {
        Log.d(TAG, "dataHasChanged");
        int id = prefs.getInt(PREFS_ID, 0);
        String max = prefs.getString(PREFS_MAX, "");
        String min = prefs.getString(PREFS_MIN, "");
        boolean ret = weatherId != id || !weatherMax.equals(max) || !weatherMin.equals(min);
        Log.d(TAG, "dataHasChanged: " + ret);
        return ret;
    }

    private void updateWeather(int weatherId, String weatherMax, String weatherMin) {
        Log.d(TAG, "updateWeather");
        PutDataMapRequest request = PutDataMapRequest.create("/update");
        request.getDataMap().putInt("id", weatherId);
        request.getDataMap().putString("max", weatherMax);
        request.getDataMap().putString("min", weatherMin);
        request.getDataMap().putLong("Time", System.currentTimeMillis());
        if(mGoogleApiClient.isConnected()) {
            Log.d(TAG, "updateWeather: connected putting item");
            Wearable.DataApi.putDataItem(mGoogleApiClient, request.asPutDataRequest());
        } else {
            Log.d(TAG, "updateWeather waiting for connected adding to queue");
            if(queue == null) {
                queue = new LinkedList<>();
            }
            queue.add(request);
        }


    }


    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        if(queue != null) {
            while(!queue.isEmpty()) {
                Log.d(TAG, "onConnected: putting item");
                Wearable.DataApi.putDataItem(mGoogleApiClient, queue.pop().asPutDataRequest());
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
