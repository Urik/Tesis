package com.tesis.commonclasses.obtainers;

import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import com.tesis.commonclasses.listeners.EventsProducer;
import com.tesis.commonclasses.listeners.SignalChangedListener;
import com.tesis.commonclasses.obtainers.SignalChangedArgs;

import java.util.HashSet;
import java.util.Set;

public class PhoneSignalMonitor extends PhoneStateListener implements EventsProducer<SignalChangedListener> {

	private SignalStrength oldStrength;
	private Set<SignalChangedListener> listeners = new HashSet<SignalChangedListener>();
    private TelephonyManager manager;

    public PhoneSignalMonitor(TelephonyManager tel) {
        manager = tel;
    }

	@Override
	public void onSignalStrengthsChanged(SignalStrength newSignalStrength) {
		super.onSignalStrengthsChanged(newSignalStrength);
		SignalStrength oldSignal = oldStrength;
        this.oldStrength = newSignalStrength;
        if (oldSignal == null || oldSignal.getGsmSignalStrength() != newSignalStrength.getGsmSignalStrength()) {
            SignalChangedArgs args = new SignalChangedArgs(oldSignal, newSignalStrength);
            broadcastSignalChanged(args);
        }
	}

    public void startListening() {
        manager.listen(this, LISTEN_SIGNAL_STRENGTHS);
    }

    public void stopListening() {
        manager.listen(this, LISTEN_NONE);
    }

    public void addListener(SignalChangedListener listener){
        listeners.add(listener);
    }

    public boolean removeListener(SignalChangedListener listener) {
        return listeners.remove(listener);
    }

	public SignalStrength getSignalStrength() {
		return oldStrength;
	}

    private void broadcastSignalChanged(SignalChangedArgs args) {
        for (SignalChangedListener listener : listeners) {
            listener.handleSignalChanged(args);
        }
    }
}
