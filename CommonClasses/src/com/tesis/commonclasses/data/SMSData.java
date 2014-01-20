package com.tesis.commonclasses.data;

/**
 * Created by joaquin on 11/4/13.
 */
import android.location.Location;

import com.tesis.commonclasses.data.PerformanceData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class SMSData extends PerformanceData {

    private final Long timeOfSend;
    private final Long dateOfSend;

    public SMSData(float currentSignal, float batteryLevel, Location location, Long timeOfSend, String destinationNumber, String operatorName, Long dateOfSend, String phoneNumber, Long downloadLatency) {
        super("SMS", currentSignal, batteryLevel, location, operatorName, phoneNumber, downloadLatency);
        this.timeOfSend = timeOfSend;
        this.dateOfSend = dateOfSend;

    }

    @Override
    public JSONObject getAsJson() {
        try {
            JSONObject jsonObject = super.getAsJson();
            jsonObject.put("timeOfSend", timeOfSend);
            jsonObject.put("dateOfSend", dateOfSend);
            return jsonObject;
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
}