package com.tesis.receptordellamadas;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import com.tesis.commonclasses.Constants;
import com.tesis.commonclasses.TimeSynchronizer;
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
import android.content.Intent;
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
    private TimeSynchronizer timeSynchronizer;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();    
    private TelephonyManager telephonyManager;

    //Lista de paquetes
    DataList dataList = new DataList(this);
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		Intent resultIntent = new Intent(this, MainActivity.class);
        Notification notification = new NotificationCompat.Builder(this)
        	.setSmallIcon(R.drawable.ic_launcher)
        	.setContentTitle("Calls Receiver")
        	.setContentText("Tesis Calls Receiveris working")
        	.setContentIntent(PendingIntent.getActivity(this, 1, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT))
        	.build();
        startForeground(1, notification);
        
		this.telephonyManager = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
        this.incomingCallsMonitor = new IncomingCallsMonitor(telephonyManager);
        signalMonitor = new PhoneSignalMonitor(telephonyManager);
        this.operatorName = telephonyManager.getNetworkOperatorName();
        this.mPhoneNumber = telephonyManager.getLine1Number();

        timeSynchronizer = new TimeSynchronizer(this, mPhoneNumber, Constants.ServerAddress);
        timeSynchronizer.synchronizeTime();

        signalMonitor.addListener(this);
        signalMonitor.startListening();

        incomingCallsMonitor.addListener(this);
        incomingCallsMonitor.startListening();
        executor.scheduleWithFixedDelay(getSendToServerTask(), 0, 120, TimeUnit.SECONDS); //30 minutes
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		incomingCallsMonitor.stopListening();
		signalMonitor.stopListening();
		executor.shutdownNow();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	@Override
    public void handleCallHasBeenReceived(String incomingNumber, Date time) {
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
