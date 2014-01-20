package com.tesis.datacollector;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

public class TurnOffScreenActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Window window = getWindow();
    	LayoutParams windowAttributes = window.getAttributes();
    	windowAttributes.screenBrightness = 0;
    	getWindow().setAttributes(windowAttributes);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.turn_off_screen, menu);
		return true;
	}

}
