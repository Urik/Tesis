package com.tesis.datacollector;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    private ComponentName componentName;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        componentName = new ComponentName(this, MyAdminReceiver.class);

        Button startServiceButton = (Button) findViewById(R.id.startServiceButton);
        startServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	Intent policyIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);  
                policyIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,  
                            componentName);  
                policyIntent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,  
                            "Additional text explaining why this needs to be added.");  
                startActivityForResult(policyIntent, 1);
                startService(new Intent(MainActivity.this, DataCollectorService.class));
            }
        });

        Button stopServiceButton = (Button) findViewById(R.id.stopServiceButton);
        stopServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(MainActivity.this, DataCollectorService.class));
            }
        });

        Button preferencesButton = (Button) findViewById(R.id.preferencesButton);
        preferencesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }
}