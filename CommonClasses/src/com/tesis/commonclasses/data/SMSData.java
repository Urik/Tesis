package com.tesis.commonclasses.data;

/**
 * Created by joaquin on 11/4/13.
 */
import android.location.Location;

import com.tesis.commonclasses.TesisTimeFormatter;
import com.tesis.commonclasses.data.PerformanceData;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class SMSData extends PerformanceData {

    private final Long timeOfSend;
    private final DateTime dateOfSend;
    private final String destinationNumber;

    public SMSData(float currentSignal, float batteryLevel, Location location, Long timeOfSend, String operatorName, DateTime dateOfSend, String phoneNumber, String destinationNumber) {
        super("sms", currentSignal, batteryLevel, location, operatorName, phoneNumber);
        this.timeOfSend = timeOfSend;
        this.dateOfSend = dateOfSend;
    	this.destinationNumber = destinationNumber;
    }

    @Override
    public JSONObject getAsJson() {
        try {
            JSONObject jsonObject = super.getAsJson();
            jsonObject.put("sendingTime", timeOfSend);
            jsonObject.put("targetNumber", destinationNumber);
            return jsonObject;
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
}