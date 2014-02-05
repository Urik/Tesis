package com.tesis.commonclasses.data;

import org.json.JSONObject;

import android.location.Location;

public class FailedLatencyCheckData extends PerformanceData {

	public FailedLatencyCheckData(Float currentSignal,
			Float batteryLevel, Location location, String operatorName,
			String phoneNumber, Long downloadLatency) {
		super("failed_data", currentSignal, batteryLevel, location, operatorName,
				phoneNumber);
		}

	@Override
	public JSONObject getAsJson() {
		return super.getAsJson();
	}
	
	
}
