package com.tesis.commonclasses.data;

import android.location.Location;
import android.util.TimeFormatException;

import com.tesis.commonclasses.TesisTimeFormatter;
import com.tesis.commonclasses.data.PerformanceData;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class CallMadeData extends PerformanceData {

    private final DateTime timeOfCall;
    private final String destinationNumber;

    public CallMadeData(float currentSignal, float batteryLevel, Location location, DateTime timeOfCall, String destinationNumber, String operatorName, String phoneNumber) {
        super("call", currentSignal, batteryLevel, location, operatorName, phoneNumber);
        this.timeOfCall = timeOfCall;
        this.destinationNumber = destinationNumber;
    }

    @Override
    public JSONObject getAsJson() {
        try {
            JSONObject jsonObject = super.getAsJson();
            jsonObject.put("targetNumber", destinationNumber);
            jsonObject.put("incoming", 0);
            return jsonObject;
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
}
