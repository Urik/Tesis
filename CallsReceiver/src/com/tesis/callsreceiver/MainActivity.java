package com.tesis.callsreceiver;

import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.view.Menu;

import com.tesis.callsreceiver.listeners.CallReceivedListener;
import com.tesis.commonclasses.Constants;
import com.tesis.commonclasses.obtainers.BatteryLevelInspector;
import com.tesis.commonclasses.obtainers.PhoneSignalMonitor;
import com.tesis.commonclasses.obtainers.SignalChangedArgs;
import com.tesis.commonclasses.TimeSynchronizer;
import com.tesis.commonclasses.data.CallData;
import com.tesis.commonclasses.data.DataList;
import com.tesis.commonclasses.listeners.SignalChangedListener;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Date;

public class MainActivity extends Activity implements CallReceivedListener,SignalChangedListener {
    private IncomingCallsMonitor incomingCallsMonitor;
    private PhoneSignalMonitor signalMonitor;
    private float currentSignal;
    private String operatorName;
    private String mPhoneNumber;
    private TimeSynchronizer timeSynchronizer;

    //Lista de paquetes
    DataList dataList = new DataList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_callsreceiver);

        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
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
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void handleCallHasBeenReceived(String incomingNumber, Date time) {
        BatteryLevelInspector batteryInspector = new BatteryLevelInspector(this);
        float batteryLevel = batteryInspector.getBatteryLevelAsPercentage();

        CallData callData = new CallData(currentSignal, batteryLevel, null, (new Date()).getTime(), incomingNumber, operatorName, mPhoneNumber, 0l);
        JSONObject obj = callData.getAsJson();
        dataList.addToPack(obj);
        try {
            dataList.sendDataListAndClearIfSuccessful();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleSignalChanged(SignalChangedArgs args) {
        int strength = args.getNewSignalStrength().getGsmSignalStrength();
        currentSignal = strength;
    }

}
