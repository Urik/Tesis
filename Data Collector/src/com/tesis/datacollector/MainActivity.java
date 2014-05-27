package com.tesis.datacollector;

import com.tesis.commonclasses.Constants;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
	private static final String STOP_BUTTON_STATE = "stop_button_state";
	private static final String START_BUTTON_STATE = "start_button_state";
	private static final String START_GPS_BUTTON_STATE = "start_gps_button_state";
	private static final String STOP_GPS_BUTTON_STATE = "stop_gps_button_state";
	private ComponentName componentName;
	private ActivityHandler handler = new ActivityHandler();
	private static Button startServiceButton;
	private Button stopServiceButton;
	private Button startGPSButton;
	private Button stopGPSButton;
	
	private Messenger serviceMessenger;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		componentName = new ComponentName(this, MyAdminReceiver.class);

		startServiceButton = (Button) findViewById(R.id.startServiceButton);
		startServiceButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startServiceButton.setEnabled(false);
				stopServiceButton.setEnabled(true);
				Intent startServiceIntent = new Intent(MainActivity.this,
						DataCollectorService.class);
				startServiceIntent.putExtra("MESSENGER", new Messenger(handler));
				startService(startServiceIntent);
			}
		});

		stopServiceButton = (Button) findViewById(R.id.stopServiceButton);
		stopServiceButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				stopServiceButton.setEnabled(false);
				stopService(new Intent(MainActivity.this,
						DataCollectorService.class));
			}
		});

		Button preferencesButton = (Button) findViewById(R.id.preferencesButton);
		preferencesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(MainActivity.this,
						SettingsActivity.class);
				intent.putExtra("MESSENGER", new Messenger(handler));
				startActivity(intent);
			}
		});
		
		startGPSButton = (Button) findViewById(R.id.enableGPSButton);
		startGPSButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				stopGPSButton.setEnabled(true);
				startGPSButton.setEnabled(false);
				Message startGPSMessage = Message.obtain();
				startGPSMessage.what = Constants.StartGPS;
				try {
					serviceMessenger.send(startGPSMessage);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
		
		stopGPSButton = (Button) findViewById(R.id.disableGPSButton);
		stopGPSButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				stopGPSButton.setEnabled(false);
				startGPSButton.setEnabled(true);
				Message stopGPSMessage = Message.obtain();
				stopGPSMessage.what = Constants.StopGPS;
				try {
					serviceMessenger.send(stopGPSMessage);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			try {
				boolean startButtonState = savedInstanceState.getBoolean(START_BUTTON_STATE);
				boolean stopButtonState = savedInstanceState.getBoolean(STOP_BUTTON_STATE);
				boolean startGPSButtonState = savedInstanceState.getBoolean(START_GPS_BUTTON_STATE);
				boolean stopGPSButtonState = savedInstanceState.getBoolean(STOP_GPS_BUTTON_STATE);
				
				startServiceButton.setEnabled(startButtonState);
				stopServiceButton.setEnabled(stopButtonState);
				startGPSButton.setEnabled(startGPSButtonState);
				stopGPSButton.setEnabled(stopGPSButtonState);
			} catch (Exception e) {
				Log.e(Constants.LogTag, "Error loading previous state objects");
			}
		}
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.d(Constants.LogTag, "Saving the UI state");
		outState.putBoolean(START_BUTTON_STATE, startServiceButton.isEnabled());
		outState.putBoolean(STOP_BUTTON_STATE, stopServiceButton.isEnabled());
		outState.putBoolean(START_GPS_BUTTON_STATE, startGPSButton.isEnabled());
		outState.putBoolean(STOP_GPS_BUTTON_STATE, stopGPSButton.isEnabled());
	}
	
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}
	
	public class ActivityHandler extends Handler{

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case Constants.ServiceLaunched:
					stopServiceButton.setEnabled(true);
					startServiceButton.setEnabled(false);
					startGPSButton.setEnabled(true);
					stopGPSButton.setEnabled(false);
					serviceMessenger = msg.replyTo;
					break;
				case Constants.ServiceLaunchFailed:
					startServiceButton.setEnabled(true);
					stopServiceButton.setEnabled(false);
					startGPSButton.setEnabled(false);
					stopGPSButton.setEnabled(false);
					break;
			}
		}

	}
}