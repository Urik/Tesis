package com.tesis.receptordellamadas;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.json.JSONObject;

import com.tesis.commonclasses.Constants;
import com.tesis.commonclasses.SynchronizedClock;
import com.tesis.commonclasses.data.CallReceivedData;
import com.tesis.commonclasses.data.DataList;
import com.tesis.commonclasses.listeners.SignalChangedListener;
import com.tesis.commonclasses.obtainers.BatteryLevelInspector;
import com.tesis.commonclasses.obtainers.PhoneSignalMonitor;
import com.tesis.commonclasses.obtainers.SignalChangedArgs;
import com.tesis.receptordellamadas.listeners.CallReceivedListener;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallsBlockerService extends Service implements CallReceivedListener,SignalChangedListener {

	private IncomingCallsMonitor incomingCallsMonitor;
    private PhoneSignalMonitor signalMonitor;
    private float currentSignal;
    private String operatorName;
    private String mPhoneNumber;
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);    
    private TelephonyManager telephonyManager;

    //Lista de paquetes
    private DataList dataList;
	private WifiLock wifiLock;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    dataList = DataList.load(this);
		WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
	    wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL , "MyWifiLock");
	    wifiLock.acquire();
		
		Intent resultIntent = new Intent(this, MainActivity.class);
        Notification notification = new NotificationCompat.Builder(this)
        	.setSmallIcon(R.drawable.ic_launcher)
        	.setContentTitle("Calls Receiver")
        	.setContentText("Tesis Calls Receiveris working")
        	.setContentIntent(PendingIntent.getActivity(this, 1, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT))
        	.build();
        startForeground(1, notification);
        
        boolean sync = SynchronizedClock.synchronize();
        if (!sync) {
        	stopSelf();
        }
        
        ScheduledFuture<?> synchronizerFuture = executor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				SynchronizedClock.synchronize();
			}
		}, 0, 60, TimeUnit.SECONDS);
		this.telephonyManager = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
        this.incomingCallsMonitor = new IncomingCallsMonitor(telephonyManager);
        signalMonitor = new PhoneSignalMonitor(telephonyManager);
        this.operatorName = telephonyManager.getNetworkOperatorName();
        this.mPhoneNumber = "2233036317";

        signalMonitor.addListener(this);
        signalMonitor.startListening();

        incomingCallsMonitor.addListener(this);
        incomingCallsMonitor.startListening();
        executor.scheduleWithFixedDelay(getSendToServerTask(), 0, 600, TimeUnit.SECONDS); //10 minutes
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		incomingCallsMonitor.stopListening();
		signalMonitor.stopListening();
		executor.shutdownNow();
		if (wifiLock != null) {
			wifiLock.release();
		}
		ExecutorService localExecutor = Executors.newSingleThreadExecutor();
		Future<?> executionFuture = localExecutor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					dataList.sendDataListAndClearIfSuccessful();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				dataList.save();
			}
		});
		try {
			executionFuture.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	@Override
    public void handleCallHasBeenReceived(String incomingNumber, DateTime time) {
    	try {
            // Java reflection to gain access to TelephonyManager's
            // ITelephony getter

            Class<?> c = Class.forName(telephonyManager.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            Object telephonyService = m.invoke(telephonyManager);
            Method endCallMethod = telephonyService.getClass().getDeclaredMethod("endCall");
            endCallMethod.setAccessible(true);
            endCallMethod.invoke(telephonyService);
        } catch (Exception e) {
            e.printStackTrace();
        }

        BatteryLevelInspector batteryInspector = new BatteryLevelInspector(this);
        float batteryLevel = batteryInspector.getBatteryLevelAsPercentage();

        CallReceivedData callData = new CallReceivedData(currentSignal, batteryLevel, operatorName, mPhoneNumber, incomingNumber);
        JSONObject obj = callData.getAsJson();
        dataList.addToPack(obj);
    }


	@Override
	public void handleSignalChanged(SignalChangedArgs args) {
        int strength = args.getNewSignalStrength().getGsmSignalStrength();
        currentSignal = strength;
	}

	private Runnable getSendToServerTask() {
		return new Runnable() {
			@Override
			public void run() {
				try {
					dataList.sendDataListAndClearIfSuccessful();
				} catch (Throwable e) {
					Log.e(Constants.LogTag, "Error sending data to the server: " + e);
					e.printStackTrace();
				}
			}
		};
	}

}
