package com.tesis.receptordellamadas;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.tesis.receptordellamadas.listeners.CallReceivedListener;
import com.tesis.commonclasses.listeners.EventsProducer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class IncomingCallsMonitor extends PhoneStateListener implements EventsProducer<CallReceivedListener>{
    private List<CallReceivedListener> listeners = new ArrayList<CallReceivedListener>();
    private final TelephonyManager telephonyManager;

    public IncomingCallsMonitor(TelephonyManager telephonyManager) {
        this.telephonyManager = telephonyManager;
    }

    @Override
    public void addListener(CallReceivedListener listener) {
        listeners.add(listener);
    }

    @Override
    public boolean removeListener(CallReceivedListener listener) {
        return listeners.remove(listener);
    }

    @Override
    public void startListening() {
        telephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void stopListening() {
        telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        if(state == TelephonyManager.CALL_STATE_RINGING) {
            handleCallReceived(incomingNumber, new Date());
        }
    }

    private void handleCallReceived(String incomingNumber, Date date) {
        for (CallReceivedListener listener : listeners) {
            listener.handleCallHasBeenReceived(incomingNumber, date);
        }
    }
}
