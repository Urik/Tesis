package com.tesis.commonclasses.data;

import android.util.Log;
import com.tesis.commonclasses.Constants;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class TimeSynchronizationData extends Data {
    private final Date time;
    public TimeSynchronizationData(Date currentDate, String phoneNumber) {
        super("time_synchronization", phoneNumber);
        time = currentDate;
    }

    @Override
    public JSONObject getAsJson() {
        JSONObject data =  super.getAsJson();
        try {
            data.put("current_time", time);
            return data;
        } catch (JSONException e) {
            Log.e(Constants.LogTag, "There was an error serializing the time");
            e.printStackTrace();
            return new JSONObject();
        }
    }
}
