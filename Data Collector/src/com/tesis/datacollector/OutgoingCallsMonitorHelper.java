package com.tesis.datacollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.tesis.commonclasses.Constants;
import com.tesis.commonclasses.listeners.EventsProducer;
import com.tesis.datacollector.listeners.PhoneIsMakingACallListener;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class OutgoingCallsMonitorHelper extends BroadcastReceiver implements EventsProducer<PhoneIsMakingACallListener> {
    private final Set<PhoneIsMakingACallListener> outgoingCallsListeners = new HashSet<PhoneIsMakingACallListener>();
    private final Context context;

    public OutgoingCallsMonitorHelper(Context context) {
        this.context= context;
    }

    public void addListener(PhoneIsMakingACallListener listener) {
        outgoingCallsListeners.add(listener);
    }

    public boolean removeListener(PhoneIsMakingACallListener listener) {
        return outgoingCallsListeners.remove(listener);
    }

    public void startListening() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        context.registerReceiver(this, filter);
    }

    public void stopListening() {
    	try {
    		context.unregisterReceiver(this);
    	} catch (RuntimeException e) {
    		Log.d(Constants.LogTag, "Unregistering an unregistered receiver");
    	}
    }

    private void handleOutgoingCall(String number, Date date) {
        for (PhoneIsMakingACallListener listener : outgoingCallsListeners) {
            listener.handlePhoneIsMakingACall(number, date);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        Date date = new Date();

        handleOutgoingCall(number, date);
    }
}
