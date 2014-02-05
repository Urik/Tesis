package com.tesis.receptordellamadas;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.tesis.receptordellamadas.R;
import com.tesis.receptordellamadas.R.layout;
import com.tesis.receptordellamadas.listeners.CallReceivedListener;
import com.tesis.commonclasses.Constants;
import com.tesis.commonclasses.obtainers.BatteryLevelInspector;
import com.tesis.commonclasses.obtainers.PhoneSignalMonitor;
import com.tesis.commonclasses.obtainers.SignalChangedArgs;
import com.tesis.commonclasses.TimeSynchronizer;
import com.tesis.commonclasses.data.CallReceivedData;
import com.tesis.commonclasses.data.DataList;
import com.tesis.commonclasses.listeners.SignalChangedListener;

import org.json.JSONObject;

import java.io.Console;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Button startServiceButton = (Button) findViewById(R.id.buttonComenzarABloquear);
        startServiceButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startService(new Intent(MainActivity.this, CallsBlockerService.class));
			}
		});
        
        Button stopServiceButton = (Button) findViewById(R.id.buttonDejarDeBloquear);
        stopServiceButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stopService(new Intent(MainActivity.this, CallsBlockerService.class));
			}
		});
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
