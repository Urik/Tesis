package com.tesis.datacollector;

import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.os.SystemClock;

import com.tesis.commonclasses.listeners.EventsProducer;
import com.tesis.datacollector.listeners.GPSSignalLostListener;

import java.util.ArrayList;
import java.util.List;

public class GPSStatusListener implements Listener, EventsProducer<GPSSignalLostListener> {
	private List<GPSSignalLostListener> listeners = new ArrayList<GPSSignalLostListener>();
	private boolean gpsIsFixed = false;
	private Long lastLocationInMillis = null;

	@Override
	public void onGpsStatusChanged(int event) {
		switch (event) {
		case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
			if (lastLocationInMillis != null) {
				gpsIsFixed = (SystemClock.elapsedRealtime() - lastLocationInMillis) < 10000;
				if (!gpsIsFixed) {
					for (GPSSignalLostListener listener : listeners) {
						listener.handleGPSSignalLost();
					}
				}
			}
			break;
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			gpsIsFixed = true;

		default:
			break;
		}
	}
	
	public void setLastLocationInMillis(long time) {
		lastLocationInMillis = time;
	}

	@Override
	public void addListener(GPSSignalLostListener listener) {
		listeners.add(listener);
	}

	@Override
	public boolean removeListener(GPSSignalLostListener listener) {
		return listeners.remove(listener);
	}

	@Override
	public void startListening() {
	}

	@Override
	public void stopListening() {
	}

}
