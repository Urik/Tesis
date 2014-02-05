package com.tesis.commonclasses.data;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;

public class InternetCheckData extends PerformanceData {
	private final long downloadTime;
	
	public InternetCheckData(Float currentSignal, float batteryLevel, Location location, String operatorName,
			String phoneNumber, long downloadTimeInMs) {
		super("internet_check", currentSignal, batteryLevel, location, operatorName,
				phoneNumber);
		this.downloadTime = downloadTimeInMs;
	}

	@Override
	public JSONObject getAsJson() {
		JSONObject jsonData = super.getAsJson();
		try {
			jsonData.put("download_time", downloadTime);
			return jsonData;
		} catch (JSONException e) {
			return new JSONObject();
		}
	}
}
