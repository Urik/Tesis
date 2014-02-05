package com.tesis.commonclasses.data;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.tesis.commonclasses.Constants;
import com.tesis.commonclasses.EmailSender;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;

/**
 * Created by joaquin on 11/4/13.
 */
public class DataList {
    private List<JSONObject> dataPackList;
    private List<JSONObject> dataForDispatching;
    private Context context;
    
    public DataList(Context context) {
    	this.context = context;
        dataPackList = new ArrayList<JSONObject>();
        dataForDispatching = new ArrayList<JSONObject>();
    }

    public void addToPack(JSONObject data){
        dataPackList.add(data);
    }

    public void setDataPackList(List<JSONObject> dataPackList) {
        this.dataPackList = dataPackList;
    }

    public void sendDataListAndClearIfSuccessful() throws URISyntaxException {
    	try {
	    	if (dataPackList.size() != 0) {
		    	ArrayList<JSONObject> auxList = new ArrayList<JSONObject>(dataPackList);
		    	JSONArray data = new JSONArray();
		        for (JSONObject packet: auxList) {
		        	data.put(packet);
		        }
		        dataPackList.removeAll(auxList);
		        JSONObject dispatchPacket = new JSONObject();
				dispatchPacket.put("dispatch_date", new Date());
				dispatchPacket.put("data", data);
		        
		        dataForDispatching.add(dispatchPacket);
	    	}
	//            HttpClient httpclient = new DefaultHttpClient();
	//            HttpPost httppost = new HttpPost(new URI(Constants.ServerAddress + "record.php"));
	//
	//            // Add your data
	//            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
	//            nameValuePairs.add(new BasicNameValuePair("data", data.toString()));
	//            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	//
	//            // Execute HTTP Post Request
	//            HttpResponse response = httpclient.execute(httppost);
	//            if (response.getStatusLine().getStatusCode() == 200)
	//            {
	//                dataPackList.removeAll(auxList);
	//            }
	        //Log.d(Constants.LogTag, "*****Paquete JSON Enviado al server: " + response.toString());
	    	
	//            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
	//            	File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "call_log.txt");
	//            	FileOutputStream fileOutputStream = new FileOutputStream(outputFile, true);
	//	        	String string = data.toString() + "\n\r";
	//	
	//	        	PrintWriter printWriter = new PrintWriter(fileOutputStream);
	//	        	printWriter.write(string);
	//	        	printWriter.flush();
	//	        	printWriter.close();
	//	        	dataPackList.removeAll(auxList);
	//            }
	    	
	    	if (dataForDispatching.size() == 0) return;
	    	
	    	List<JSONObject> auxDataForDispatching = new ArrayList<JSONObject>(dataForDispatching);
	    	JSONArray dispatchPacket = new JSONArray();
	    	for (JSONObject dataPacket : auxDataForDispatching) {
	    		dispatchPacket.put(dataPacket);
	    	}
			EmailSender.sendEmail(dispatchPacket.toString(4));
			dataForDispatching.removeAll(auxDataForDispatching);
		} catch (JSONException e) {
			Log.e(Constants.LogTag, "Error parsing JSON");
		} catch (MessagingException e) {
			Log.e(Constants.LogTag, "Error sending email");
		}
	    	
    }
}
