package com.tesis.commonclasses;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.http.AndroidHttpClient;
import android.util.Log;

import com.tesis.commonclasses.Constants;
import com.tesis.commonclasses.data.TimeSynchronizationData;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

public class TimeSynchronizer extends BroadcastReceiver {
    private final String serverUrl;
    private String phoneNumber;

    public TimeSynchronizer(Context context, String mPhoneNumber, String serverUrl) {
        this.serverUrl = serverUrl;
        phoneNumber = mPhoneNumber;

        context.registerReceiver(this, new IntentFilter(Intent.ACTION_TIME_CHANGED));
    }

    public void synchronizeTime() {
        Thread synchronizeTimeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(Constants.LogTag, "Synchronizing time.");
                try {
                    sendTimeToServer(new TimeSynchronizationData(new Date(), phoneNumber).getAsJson());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        synchronizeTimeThread.start();
    }

    private void sendTimeToServer(JSONObject timeHolder) throws IOException {
        HttpPost httpPost = new HttpPost(serverUrl);
        httpPost.setHeader("time", timeHolder.toString());

        HttpClient client = AndroidHttpClient.newInstance("tesis_agent");
        client.execute(httpPost);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        synchronizeTime();
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
