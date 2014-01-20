package com.tesis.commonclasses.data;

import android.util.Log;

import com.tesis.commonclasses.Constants;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by joaquin on 11/4/13.
 */
public class DataList {
    private List<JSONObject> dataPackList;

    public DataList() {
        dataPackList = new ArrayList<JSONObject>();
    }

    public void addToPack(JSONObject data){
        dataPackList.add(data);
    }

    public void setDataPackList(List<JSONObject> dataPackList) {
        this.dataPackList = dataPackList;
    }

    public void sendDataListAndClearIfSuccessful() throws URISyntaxException {
    	if (dataPackList.size() == 0) return;
    	
        try {
        	ArrayList<JSONObject> auxList = new ArrayList<JSONObject>(dataPackList);
        	JSONArray data = new JSONArray();
            for (JSONObject packet: dataPackList) {
            	data.put(packet);
            }
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(new URI(Constants.ServerAddress + "record.php"));

            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("data", data.toString()));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            if (response.getStatusLine().getStatusCode() == 200)
            {
                dataPackList.removeAll(auxList);
            }
            Log.d(Constants.LogTag, "*****Paquete JSON Enviado al server: " + response.toString());
        
        } catch (ClientProtocolException e) {
            Log.e(Constants.LogTag, "*****Client Protocol Exception" + e.toString());
        } catch (IOException e) {
            Log.e(Constants.LogTag, "*****IOException" + e.toString());
        }
    }
}
