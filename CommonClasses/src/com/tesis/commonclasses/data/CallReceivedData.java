package com.tesis.commonclasses.data;

import org.json.JSONException;
import org.json.JSONObject;

import com.tesis.commonclasses.Constants;

import android.util.Log;

public class CallReceivedData extends PerformanceData {
	private String callerNumber;
	
	public CallReceivedData(Float currentSignal, Float batteryLevel, String operatorName, String phoneNumber, String callerNumber) {
		super("call", currentSignal, batteryLevel, null, operatorName, phoneNumber);
		this.callerNumber = callerNumber;
	}

	@Override
	public JSONObject getAsJson() {
		JSONObject json = super.getAsJson();
		try {
			json.put("targetNumber", callerNumber);
			json.put("incoming", 1);
		} catch (JSONException e) {
			Log.e(Constants.LogTag, "Error creating a call_received object: " + e);
			e.printStackTrace();
		}
		return json;
	}
	
	
}
