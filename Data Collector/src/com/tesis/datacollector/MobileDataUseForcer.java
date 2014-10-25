package com.tesis.datacollector;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.tesis.commonclasses.Constants;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MobileDataUseForcer {
	/**
     * Enable mobile connection for a specific address
     * @param context a Context (application or activity)
     * @param address the address to enable
     * @return true for success, else false
     */
	static class MobileConnectionAsync extends AsyncTask<Void, Void, Boolean> {
		private final Context context;
		private final String address;
		public MobileConnectionAsync(Context context, String address) {
			this.context = context;
			this.address = address;
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {
			ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	        String TAG_LOG = Constants.LogTag;
	        if (null == connectivityManager) {
	            Log.d(TAG_LOG, "ConnectivityManager is null, cannot try to force a mobile connection");
	            return false;
	        }

	        //check if mobile connection is available and connected
	        State state = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_HIPRI).getState();
	        Log.d(TAG_LOG, "TYPE_MOBILE_HIPRI network state: " + state);
	        if (0 == state.compareTo(State.CONNECTED) || 0 == state.compareTo(State.CONNECTING)) {
	            return true;
	        }

	        //activate mobile connection in addition to other connection already activated
	        int resultInt = connectivityManager.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, "enableHIPRI");
	        Log.d(TAG_LOG, "startUsingNetworkFeature for enableHIPRI result: " + resultInt);

	        //-1 means errors
	        // 0 means already enabled
	        // 1 means enabled
	        // other values can be returned, because this method is vendor specific
	        if (-1 == resultInt) {
	            Log.e(TAG_LOG, "Wrong result of startUsingNetworkFeature, maybe problems");
	            return false;
	        }
	        if (0 == resultInt) {
	            Log.d(TAG_LOG, "No need to perform additional network settings");
	            return true;
	        }

	        //find the host name to route
	        String hostName = extractAddressFromUrl(address);
	        Log.d(TAG_LOG, "Source address: " + address);
	        Log.d(TAG_LOG, "Destination host address to route: " + hostName);
	        if (TextUtils.isEmpty(hostName)) hostName = address;

	        //create a route for the specified address
	        int hostAddress = lookupHost(hostName);
	        if (-1 == hostAddress) {
	            Log.e(TAG_LOG, "Wrong host address transformation, result was -1");
	            return false;
	        }
	        //wait some time needed to connection manager for waking up
	        try {
	            for (int counter=0; counter<30; counter++) {
	                State checkState = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_HIPRI).getState();
	                if (0 == checkState.compareTo(State.CONNECTED))
	                    break;
	                Thread.sleep(1000);
	            }
	        } catch (InterruptedException e) {
	            //nothing to do
	        }
	        boolean resultBool = connectivityManager.requestRouteToHost(ConnectivityManager.TYPE_MOBILE_HIPRI, hostAddress);
	        Log.d(TAG_LOG, "requestRouteToHost result: " + resultBool);
	        if (!resultBool)
	            Log.e(TAG_LOG, "Wrong requestRouteToHost result: expected true, but was false");

	        return resultBool;
		}
		
	}
    public static boolean forceMobileConnectionForAddress(Context context, String address) {
        try {
			return new MobileConnectionAsync(context, address).execute().get();
		} catch (Exception e) {
			return false;
		}
    }
    
    /**
     * This method extracts from address the hostname
     * @param url eg. http://some.where.com:8080/sync
     * @return some.where.com
     */
    private static String extractAddressFromUrl(String url) {
        String urlToProcess = null;

        //find protocol
        int protocolEndIndex = url.indexOf("://");
        if(protocolEndIndex>0) {
            urlToProcess = url.substring(protocolEndIndex + 3);
        } else {
            urlToProcess = url;
        }

        // If we have port number in the address we strip everything
        // after the port number
        int pos = urlToProcess.indexOf(':');
        if (pos >= 0) {
            urlToProcess = urlToProcess.substring(0, pos);
        }

        // If we have resource location in the address then we strip
        // everything after the '/'
        pos = urlToProcess.indexOf('/');
        if (pos >= 0) {
            urlToProcess = urlToProcess.substring(0, pos);
        }

        // If we have ? in the address then we strip
        // everything after the '?'
        pos = urlToProcess.indexOf('?');
        if (pos >= 0) {
            urlToProcess = urlToProcess.substring(0, pos);
        }
        return urlToProcess;
    }

    /**
     * Transform host name in int value used by {@link ConnectivityManager.requestRouteToHost}
     * method
     *
     * @param hostname
     * @return -1 if the host doesn't exists, elsewhere its translation
     * to an integer
     */
    private static int lookupHost(String hostname) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            return -1;
        }
        byte[] addrBytes;
        int addr;
        addrBytes = inetAddress.getAddress();
        addr = ((addrBytes[3] & 0xff) << 24)
                | ((addrBytes[2] & 0xff) << 16)
                | ((addrBytes[1] & 0xff) << 8 )
                |  (addrBytes[0] & 0xff);
        return addr;
    }
}
