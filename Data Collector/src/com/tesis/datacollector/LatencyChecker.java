package com.tesis.datacollector;

import android.os.AsyncTask;

import com.tesis.commonclasses.Constants;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;

public class LatencyChecker extends AsyncTask<Void, Void, Long>{

	private Runnable exceptionHandler;
	
	public LatencyChecker(Runnable exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}
	@Override
	protected Long doInBackground(Void... params) {
        String host = Constants.LatencyTestAddress;
        long time = 0l;
        HttpGet request = new HttpGet(host);
        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, 3000);
        HttpClient httpClient = new DefaultHttpClient(httpParameters);
        try {
	        for(int i=0; i<5; i++)
	        {
	            long BeforeTime = System.currentTimeMillis();
	            HttpResponse response = httpClient.execute(request);
	            long AfterTime = System.currentTimeMillis();
	            Long TimeDifference = AfterTime - BeforeTime;
	            time += TimeDifference;
	        }
        } catch (IOException e) {
        	exceptionHandler.run();
        	return null;
        }
        return time/5;
    }
}