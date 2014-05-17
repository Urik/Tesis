package com.tesis.commonclasses.data;

import android.location.Location;
import android.util.TimeFormatException;

import com.tesis.commonclasses.TesisTimeFormatter;
import com.tesis.commonclasses.data.PerformanceData;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class CallMadeData extends PerformanceData {

    private final DateTime timeOfCall;
    private DateTime timeOfFinalization = null;
    private final String destinationNumber;

    public CallMadeData(float currentSignal, float batteryLevel, Location location, DateTime timeOfCall, String destinationNumber, String operatorName, String phoneNumber) {
        super("call", currentSignal, batteryLevel, location, operatorName, phoneNumber);
        this.timeOfCall = timeOfCall;
        this.destinationNumber = destinationNumber;
    }
    
    public void setTimeOfFinalization(DateTime time) {
    	this.timeOfFinalization = time;
    }

    @Override
    public JSONObject getAsJson() {
        try {
            JSONObject jsonObject = super.getAsJson();
            jsonObject.put("targetNumber", destinationNumber);
            jsonObject.put("incoming", 0);
            if (timeOfFinalization != null) {
            	long duration = timeOfFinalization.getMillis() - timeOfCall.getMillis();
            	jsonObject.put("callTime", duration);
            }
            return jsonObject;
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
}
