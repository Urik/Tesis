package com.tesis.commonclasses.data;

import android.location.Location;

import com.tesis.commonclasses.data.PerformanceData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class CallData extends PerformanceData {

    private final Long timeOfCall;
    private final String destinationNumber;

    public CallData(float currentSignal, float batteryLevel, Location location, Long timeOfCall, String destinationNumber, String operatorName, String phoneNumber, Long downloadLatency) {
        super("call", currentSignal, batteryLevel, location, operatorName, phoneNumber, downloadLatency);
        this.timeOfCall = timeOfCall;
        this.destinationNumber = destinationNumber;
    }

    @Override
    public JSONObject getAsJson() {
        try {
            JSONObject jsonObject = super.getAsJson();
            jsonObject.put("timeOfCall", timeOfCall.toString());
            jsonObject.put("destinationNumber", destinationNumber);
            return jsonObject;
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
}
