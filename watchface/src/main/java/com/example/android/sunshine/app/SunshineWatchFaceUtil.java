package com.example.android.sunshine.app;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

/**
 * Created by teddydoll on 2/14/16.
 */
public class SunshineWatchFaceUtil {

    protected static final String PATH = "/update";

    private static class DataItemResultCallback implements ResultCallback<DataApi.DataItemResult> {

        private final FetchConfigDataMapCallback mCallback;

        public DataItemResultCallback(FetchConfigDataMapCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onResult(DataApi.DataItemResult dataItemResult) {
            if (dataItemResult.getStatus().isSuccess()) {
                if (dataItemResult.getDataItem() != null) {
                    DataItem configDataItem = dataItemResult.getDataItem();
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
                    DataMap config = dataMapItem.getDataMap();
                    mCallback.onConfigDataMapFetched(config);
                } else {
                    mCallback.onConfigDataMapFetched(new DataMap());
                }
            }
        }
    }

    public static int getArtResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return R.drawable.ic_status;
    }

    /**
     * Callback interface to perform an action with the current config {@link DataMap} for
     * {@link SunshineWatchFaceService}.
     */
    public interface FetchConfigDataMapCallback {
        /**
         * Callback invoked with the current config {@link DataMap} for
         * {@link SunshineWatchFaceService}.
         */
        void onConfigDataMapFetched(DataMap config);
    }

    /**
     * Asynchronously fetches the current config {@link DataMap} for {@link SunshineWatchFaceService}
     * and passes it to the given callback.
     * <p/>
     * If the current config {@link DataItem} doesn't exist, it isn't created and the callback
     * receives an empty DataMap.
     */
    public static void fetchConfigDataMap(final GoogleApiClient client,
                                          final FetchConfigDataMapCallback callback) {
//        Log.d("fetchConfigDataMap", "fetchConfigDataMap");
//        PendingResult<DataItemBuffer> results = Wearable.DataApi.getDataItems(client);
//        results.setResultCallback(new ResultCallback<DataItemBuffer>() {
//            @Override
//            public void onResult(@NonNull DataItemBuffer dataItems) {
//                Log.d("fetchConfigDataMap", "onResult " + dataItems );
//                for (DataItem dataItem : dataItems) {
//                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
//                    Log.d("fetchConfigDataMap", dataMapItem.getUri().toString() + ": " + dataMapItem.getDataMap());
//                }
//
//                dataItems.release();
//            }
//        });


        Wearable.NodeApi.getConnectedNodes(client).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(@NonNull NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                List<Node> nodes = getConnectedNodesResult.getNodes();
                if (nodes.size() > 0) {
                    String node = nodes.get(0).getId();
                    Uri uri = new Uri.Builder()
                            .scheme(PutDataRequest.WEAR_URI_SCHEME)
                            .path(SunshineWatchFaceUtil.PATH)
                            .authority(node)
                            .build();
                    Log.d("SunshineWatchFaceUtil", uri.toString());
                    Wearable.DataApi.getDataItem(client, uri)
                            .setResultCallback(new DataItemResultCallback(callback));
                }
            }
        });
    }
}
