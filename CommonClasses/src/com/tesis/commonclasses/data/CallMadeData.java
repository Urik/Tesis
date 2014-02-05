package com.tesis.commonclasses.data;

import android.location.Location;

import com.tesis.commonclasses.data.PerformanceData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class CallMadeData extends PerformanceData {

    private final Long timeOfCall;
    private final String destinationNumber;

    public CallMadeData(float currentSignal, float batteryLevel, Location location, Long timeOfCall, String destinationNumber, String operatorName, String phoneNumber) {
        super("call_made", currentSignal, batteryLevel, location, operatorName, phoneNumber);
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
