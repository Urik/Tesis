package com.tesis.commonclasses.data;

import org.json.JSONException;
import org.json.JSONObject;

import com.tesis.commonclasses.Constants;

import android.util.Log;

public class CallReceivedData extends PerformanceData {
	private String sourceNumber;
	
	public CallReceivedData(Float currentSignal, Float batteryLevel, String operatorName, String phoneNumber, String sourceNumber) {
		super("call_received", currentSignal, batteryLevel, null, operatorName, phoneNumber);
		this.sourceNumber = sourceNumber;
	}

	@Override
	public JSONObject getAsJson() {
		JSONObject json = super.getAsJson();
		try {
			json.put("sourceNumber", sourceNumber);
		} catch (JSONException e) {
			Log.e(Constants.LogTag, "Error creating a call_received object: " + e);
			e.printStackTrace();
		}
		return json;
	}
	
	
}
