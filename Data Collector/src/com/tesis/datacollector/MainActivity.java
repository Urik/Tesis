package com.tesis.datacollector;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.tesis.commonclasses.Constants;

public class MainActivity extends Activity {
	private static final String STOP_BUTTON_STATE = "stop_button_state";
	private static final String START_BUTTON_STATE = "start_button_state";
	private static final String START_GPS_BUTTON_STATE = "start_gps_button_state";
	private static final String STOP_GPS_BUTTON_STATE = "stop_gps_button_state";
	private ComponentName componentName;
	private ActivityHandler messagesHandler = new ActivityHandler();
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
				startServiceIntent.putExtra("MESSENGER", new Messenger(messagesHandler));
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
				intent.putExtra("MESSENGER", new Messenger(messagesHandler));
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
	protected void onResume() {
		if (DataCollectorService.instance != null) {
			ServiceState state = DataCollectorService.instance.getState();
			boolean initializing = state.isInitializing();
			boolean serviceIsWorking = state.getServiceIsWorking();
			boolean gpsIsWorking = state.getGpsIsOn();
			if (!initializing && !serviceIsWorking) {
				setNotInitializedState();
			} else {
				stopServiceButton.setEnabled(initializing || serviceIsWorking);
				startServiceButton.setEnabled(!serviceIsWorking && !initializing);
				startGPSButton.setEnabled(!gpsIsWorking && serviceIsWorking);
				stopGPSButton.setEnabled(gpsIsWorking && serviceIsWorking);
			}
		}
		super.onResume();
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
	
	private void setNotInitializedState() {
		startServiceButton.setEnabled(true);
		stopServiceButton.setEnabled(false);
		startGPSButton.setEnabled(false);
		stopGPSButton.setEnabled(false);
	}
	
	public class ActivityHandler extends Handler{

		@Override
		public void handleMessage(final Message msg) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					switch (msg.what) {
						case Constants.ServiceLaunchedSuccesfully:
							stopServiceButton.setEnabled(true);
							startServiceButton.setEnabled(false);
							startGPSButton.setEnabled(true);
							stopGPSButton.setEnabled(false);
							serviceMessenger = msg.replyTo;
							break;
						case Constants.ServiceLaunchFailed:
							setNotInitializedState();
							break;
					}
				}
			});
		}
	}
}