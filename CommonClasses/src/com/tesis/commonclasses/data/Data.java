package com.tesis.commonclasses.data;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONObject;

import com.tesis.commonclasses.SynchronizedClock;
import com.tesis.commonclasses.TesisTimeFormatter;

public abstract class Data {
	private String typeOfData;
	private String phoneNumber;
	private DateTime dateCreated;
	
	public Data(String typeOfData, String phoneNumber) {
		super();
		this.typeOfData = typeOfData;
		this.phoneNumber = phoneNumber;
		this.dateCreated = SynchronizedClock.getCurrentTime();
	}

	protected JSONObject getAsJson() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", typeOfData);
            jsonObject.put("sourceNumber", phoneNumber);            
            jsonObject.put("dateCreated", dateCreated.toString(TesisTimeFormatter.getFormatter()));

            return jsonObject;
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}
