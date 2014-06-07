package com.tesis.datacollector;

public class ServiceState {
	private boolean gpsIsOn = false;
	private boolean serviceIsWorking = false;
	private boolean initializing = false;
	
	public boolean getGpsIsOn() {
		return gpsIsOn;
	}
	public void setGpsIsOn(boolean gpsIsOn) {
		this.gpsIsOn = gpsIsOn;
	}
	public boolean getServiceIsWorking() {
		return serviceIsWorking;
	}
	public void setServiceIsWorking(boolean serviceIsWorking) {
		this.serviceIsWorking = serviceIsWorking;
	}
	public boolean isInitializing() {
		return initializing;
	}
	public void setInitializing(boolean initializing) {
		this.initializing = initializing;
	}	
}
