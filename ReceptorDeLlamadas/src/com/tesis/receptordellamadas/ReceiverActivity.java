package com.tesis.receptordellamadas;

import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;

import com.tesis.receptordellamadas.R;
import com.tesis.receptordellamadas.R.layout;
import com.tesis.receptordellamadas.listeners.CallReceivedListener;
import com.tesis.commonclasses.Constants;
import com.tesis.commonclasses.obtainers.BatteryLevelInspector;
import com.tesis.commonclasses.obtainers.PhoneSignalMonitor;
import com.tesis.commonclasses.obtainers.SignalChangedArgs;
import com.tesis.commonclasses.data.CallReceivedData;
import com.tesis.commonclasses.data.DataList;
import com.tesis.commonclasses.listeners.SignalChangedListener;

import org.joda.time.DateTime;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReceiverActivity extends Activity implements CallReceivedListener,SignalChangedListener {
//    private IncomingCallsMonitor incomingCallsMonitor;
//    private PhoneSignalMonitor signalMonitor;
//    private float currentSignal;
//    private String operatorName;
//    private String mPhoneNumber;
//    private TimeSynchronizer timeSynchronizer;
//    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();    

    //Lista de paquetes
    DataList dataList = new DataList(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//
//        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
//        this.incomingCallsMonitor = new IncomingCallsMonitor(telephonyManager);
//        signalMonitor = new PhoneSignalMonitor(telephonyManager);
//        this.operatorName = telephonyManager.getNetworkOperatorName();
//        this.mPhoneNumber = telephonyManager.getLine1Number();
//
//        timeSynchronizer = new TimeSynchronizer(this, mPhoneNumber, Constants.ServerAddress);
//        timeSynchronizer.synchronizeTime();
//
//        signalMonitor.addListener(this);
//        signalMonitor.startListening();
//
//        incomingCallsMonitor.addListener(this);
//        incomingCallsMonitor.startListening();
        //executor.scheduleAtFixedRate(getSendToServerTask(), 1800, 0, TimeUnit.SECONDS); //30 minutes
    }

//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }
//
    @Override
    public void handleCallHasBeenReceived(String incomingNumber, DateTime time) {
//        BatteryLevelInspector batteryInspector = new BatteryLevelInspector(this);
//        float batteryLevel = batteryInspector.getBatteryLevelAsPercentage();
//
//        CallReceivedData callData = new CallReceivedData(currentSignal, batteryLevel, operatorName, mPhoneNumber, incomingNumber);
//        JSONObject obj = callData.getAsJson();
//        dataList.addToPack(obj);
    }
//
//
	@Override
	public void handleSignalChanged(SignalChangedArgs args) {
//        int strength = args.getNewSignalStrength().getGsmSignalStrength();
//        currentSignal = strength;
	}
//	
	private Runnable getSendToServerTask() {
		return new Runnable() {
//			
			@Override
			public void run() {
//				try {
//					dataList.sendDataListAndClearIfSuccessful();
//				} catch (URISyntaxException e) {
//					Log.e(Constants.LogTag, "Error sending data to the server: " + e);
//					e.printStackTrace();
//				}
			}
		};
		
	}
}
