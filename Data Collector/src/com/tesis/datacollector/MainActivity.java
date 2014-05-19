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
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
	private ComponentName componentName;
	private ActivityHandler handler = new ActivityHandler();
	private static Button startServiceButton;
	private Button stopServiceButton;

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
					break;
				case Constants.ServiceLaunchFailed:
					startServiceButton.setEnabled(true);
					stopServiceButton.setEnabled(false);
					break;
			}
		}

	}
}