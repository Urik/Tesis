package com.tesis.commonclasses.obtainers;

import android.telephony.SignalStrength;

public class SignalChangedArgs {
	private SignalStrength oldSignalStrength;
	private SignalStrength newSignalStrength;
	
	public SignalChangedArgs(SignalStrength oldSignal, SignalStrength newSignal) {
		oldSignalStrength = oldSignal;
		newSignalStrength = newSignal;
	}

	public SignalStrength getOldSignalStrength() {
		return oldSignalStrength;
	}

	public SignalStrength getNewSignalStrength() {
		return newSignalStrength;
	}
	
	
}
