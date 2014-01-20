package com.tesis.commonclasses.data;

import java.util.Date;

import org.json.JSONObject;

public abstract class Data {
	private String typeOfData;
	private String phoneNumber;
	private Date dateCreated;
	
	public Data(String typeOfData, String phoneNumber) {
		super();
		this.typeOfData = typeOfData;
		this.phoneNumber = phoneNumber;
		this.dateCreated = new Date();
	}

	protected JSONObject getAsJson() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("typeOfData", typeOfData);
            jsonObject.put("phoneNumber", phoneNumber);
            jsonObject.put("dateCreated", dateCreated);

            return jsonObject;
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}
