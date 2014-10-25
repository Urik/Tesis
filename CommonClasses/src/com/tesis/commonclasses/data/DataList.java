package com.tesis.commonclasses.data;

import android.content.Context;
import android.util.Log;

import com.tesis.commonclasses.Constants;
import com.tesis.commonclasses.SynchronizedClock;
import com.tesis.commonclasses.TesisTimeFormatter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class DataList {
    private List<JSONObject> dataPackList;
    private List<JSONObject> dataForDispatching;
    private Context context;
    
    public final static String SERIALIZATION_FILENAME = "data.json";
    
    public DataList(Context context) {
    	this.context = context;
        dataPackList = new ArrayList<JSONObject>();
        dataForDispatching = new ArrayList<JSONObject>();
    }

    public synchronized void addToPack(JSONObject data){
        dataPackList.add(data);
    }

    public synchronized void setDataPackList(List<JSONObject> dataPackList) {
        this.dataPackList = dataPackList;
    }

    public void sendDataListAndClearIfSuccessful() throws URISyntaxException {
    	try {
	    	if (dataPackList.size() != 0) {
		    	addDataToDispatchList();
	    	}
	    	
	    	if (dataForDispatching.size() == 0) return;
	    	
	    	List<JSONObject> auxDataForDispatching = new ArrayList<JSONObject>(dataForDispatching);
	    	JSONArray dispatchPacket = new JSONArray();
	    	for (JSONObject dataPacket : auxDataForDispatching) {
	    		dispatchPacket.put(dataPacket);
	    	}
	    	
	    	HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(new URI(Constants.ServerAddress + "tesis/record.php"));

            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("data", dispatchPacket.toString(4)));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	
            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity, "UTF-8");
            System.out.println(responseString);
            
            if (response.getStatusLine().getStatusCode() == 200) {
            	dataForDispatching.removeAll(auxDataForDispatching);
            }
		} catch (JSONException e) {
			Log.e(Constants.LogTag, "Error parsing JSON");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    	
    }

	private void addDataToDispatchList() throws JSONException {
		if (dataPackList.size() == 0) return;
		ArrayList<JSONObject> auxList = new ArrayList<JSONObject>(dataPackList);
		JSONArray data = new JSONArray();
		for (JSONObject packet: auxList) {
			data.put(packet);
		}
		dataPackList.removeAll(auxList);
		JSONObject dispatchPacket = new JSONObject();
		DateTime currentTime = SynchronizedClock.getCurrentTime();
		dispatchPacket.put("dispatchDate", currentTime.toString(TesisTimeFormatter.getFormatter()));
		dispatchPacket.put("epochDate", currentTime.getMillis());
		dispatchPacket.put("data", data);
		
		dataForDispatching.add(dispatchPacket);
	}
	
	public synchronized void save() {
		try {
			FileOutputStream fos = context.openFileOutput(SERIALIZATION_FILENAME, Context.MODE_PRIVATE);
			addDataToDispatchList();
			if (dataForDispatching.size() == 0) return;
			List<JSONObject> auxDataForDispatching = new ArrayList<JSONObject>(dataForDispatching);
	    	JSONArray dispatchPacket = new JSONArray();
	    	for (JSONObject dataPacket : auxDataForDispatching) {
	    		dispatchPacket.put(dataPacket);
	    	}
	    	
	    	fos.write(dispatchPacket.toString().getBytes());
	    	fos.close();
		} catch (JSONException e) {
			Log.e(Constants.LogTag, "Unable to serialize DataList: " + e);
		} catch (IOException e) {
			Log.e(Constants.LogTag, "Unable to write JSON to file: " + e);
		}
	}
	
	public static DataList load(Context context) {
		DataList dataList = new DataList(context);
		try {
			FileInputStream fis = context.openFileInput(SERIALIZATION_FILENAME);
			byte[] data = new byte[1000000];
			int read = fis.read(data);
			ArrayList<Byte> auxBytes = new ArrayList<Byte>();
			String jsonizedData = new String(data);
			JSONArray auxDataContainer = new JSONArray(jsonizedData);
			ArrayList<JSONObject> dispatchPacketsList = new ArrayList<JSONObject>(auxDataContainer.length());
			for (int i = 0; i < auxDataContainer.length(); i++) {
				dispatchPacketsList.add(auxDataContainer.getJSONObject(i));
			}
			dataList.setDataForDispatching(dispatchPacketsList);
		} catch (FileNotFoundException e) {
			Log.d(Constants.LogTag, "List file was not found: " + e);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return dataList;
	}

	public List<JSONObject> getDataForDispatching() {
		return dataForDispatching;
	}

	public void setDataForDispatching(List<JSONObject> dataForDispatching) {
		this.dataForDispatching = dataForDispatching;
	}
}
