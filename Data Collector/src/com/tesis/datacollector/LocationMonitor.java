package com.tesis.datacollector;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;

import com.tesis.commonclasses.listeners.EventsProducer;
import com.tesis.datacollector.listeners.GPSSignalLostListener;
import com.tesis.datacollector.listeners.LocationChangedListener;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LocationMonitor implements EventsProducer<LocationChangedListener>, GPSSignalLostListener {
    private final Context context;
    private Set<LocationChangedListener> locationChangedListeners = new HashSet<LocationChangedListener>();
    private Set<GPSSignalLostListener> signalLostListeners = new HashSet<GPSSignalLostListener>();
    private LocationListener locationListener;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private boolean locationHasBeenSet = false;
    private final GPSStatusListener gpsStatusListener = new GPSStatusListener();
	private LocationManager locationManager;

    public LocationMonitor(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        executor.schedule(getLocationAcquisitionTimeoutRunnable(), 5, TimeUnit.MINUTES);
        gpsStatusListener.addListener(this);
    }

	private Runnable getLocationAcquisitionTimeoutRunnable() {
		return new Runnable() {
			@Override
			public void run() {
				if (!locationHasBeenSet) {
					handleGPSAcquisitionTimeout();
				}
			}

			private void handleGPSAcquisitionTimeout() {
				stopListening();
				executor.schedule(new Runnable() {
					@Override
					public void run() {
						startListening();
					}
				}, 5, TimeUnit.MINUTES);
			}
		};
	}

    public void startListening() {
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
            	locationHasBeenSet = true;
            	//This needs to be called when the location changes so that the gpsStatusListener can check if the GPS signal has been fixed.
            	gpsStatusListener.setLastLocationInMillis(SystemClock.elapsedRealtime());
                handleLocationChanged(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {
                turnGPSOn();
            }
        };
        
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
	        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
	        locationManager.addGpsStatusListener(gpsStatusListener);
        }
    }

    public void stopListening() {
    	if (locationManager != null) {
	        locationManager.removeGpsStatusListener(gpsStatusListener);
	        locationManager.removeUpdates(locationListener);
	        executor.shutdownNow();
	        locationManager = null;
	        locationListener = null;
    	}
    }

    public void addListener(LocationChangedListener listener) {
        locationChangedListeners.add(listener);
    }

    @Override
    public boolean removeListener(LocationChangedListener listener) {
        return locationChangedListeners.remove(listener);
    }
    
    public void addListener(GPSSignalLostListener listener) {
    	signalLostListeners.add(listener);
    }
    
    public boolean removeListener(GPSSignalLostListener listener) {
    	return signalLostListeners.remove(listener);
    }

    private void handleLocationChanged(Location location) {
        for (LocationChangedListener listener : locationChangedListeners) {
            listener.handleLocationChanged(location);
        }
    }
    
    @Override
	public void handleGPSSignalLost() {
    	for (GPSSignalLostListener listener : signalLostListeners) {
    		listener.handleGPSSignalLost();
    	}
	}

    public void turnGPSOn()
    {
        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        intent.putExtra("enabled", true);
        this.context.sendBroadcast(intent);

        String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if(!provider.contains("gps")){ //if gps is disabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            this.context.sendBroadcast(poke);

        }

        for (LocationChangedListener listener : locationChangedListeners) {
            listener.handleGpsOn();
        }
    }

    public void turnGPSOff()
    {
        String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if(provider.contains("gps")){ //if gps is enabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            this.context.sendBroadcast(poke);
        }
    }
    
    public boolean isGPSEnabled() {
    	return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
