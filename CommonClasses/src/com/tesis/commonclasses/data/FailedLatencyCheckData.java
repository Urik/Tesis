package com.tesis.commonclasses.data;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;

public class FailedLatencyCheckData extends PerformanceData {

	public FailedLatencyCheckData(Float currentSignal,
			Float batteryLevel, Location location, String operatorName,
			String phoneNumber, Long downloadLatency) {
		super("internet_check", currentSignal, batteryLevel, location, operatorName,
				phoneNumber);
		}

	@Override
	public JSONObject getAsJson() {
		JSONObject response = super.getAsJson();
		try {
			response.put("download_time", 0l);
			return response;
		} catch (JSONException e) {
			e.printStackTrace();
			return new JSONObject();
		}
	}
}
