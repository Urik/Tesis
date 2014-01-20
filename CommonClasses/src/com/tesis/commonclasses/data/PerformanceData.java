package com.tesis.commonclasses.data;

import android.location.Location;

import org.json.JSONObject;

public abstract class PerformanceData extends Data {
    private Float currentSignal;
    private Double locationLat;
    private Double locationLon;
    private Float batteryLevel;
    private String operatorName;
    private String phoneNumber;
    private Long downloadLatency;

    protected PerformanceData(String typeOfData, Float currentSignal, Float batteryLevel, Location location, String operatorName, String phoneNumber, Long downloadLatency) {
        super(typeOfData, phoneNumber);
        this.currentSignal = currentSignal;
        this.batteryLevel = batteryLevel;
        if(location != null){
            this.locationLat = location.getLatitude();
            this.locationLon = location.getLongitude();
        }
        else{
            this.locationLat = 0d;
            this.locationLon = 0d;
        }
        this.operatorName = operatorName;
        this.phoneNumber = phoneNumber;
        this.downloadLatency = downloadLatency;
    }

    @Override
    protected JSONObject getAsJson() {
        try {
            JSONObject jsonObject = super.getAsJson();
            jsonObject.put("currentSignal", currentSignal);
            jsonObject.put("locationLat", locationLat);
            jsonObject.put("locationLon", locationLon);
            jsonObject.put("batteryLevel", batteryLevel);
            jsonObject.put("operatorName", operatorName);
            jsonObject.put("phoneNumber", phoneNumber);
            jsonObject.put("downloadLatency", downloadLatency);

            return jsonObject;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    @Override
    public String toString() {
        return getAsJson().toString();
    }
}