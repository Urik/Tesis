package com.tesis.datacollector;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.tesis.commonclasses.listeners.EventsProducer;
import com.tesis.datacollector.listeners.CallEndedListener;
import com.tesis.datacollector.listeners.CallIsInProgressListener;
import com.tesis.datacollector.listeners.PhoneIsMakingACallListener;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

public class OutgoingCallsMonitor extends PhoneStateListener implements EventsProducer<CallIsInProgressListener>,PhoneIsMakingACallListener {
    private Context context;
    private Date initDate = null;
    private Date finishDate = null;
    private String lastCalledNumber;
    private long timeToEstablish = 0;
    private PhoneState previousState;
    private OutgoingCallsMonitorHelper outgoingCallsMonitor;

    private Collection<CallIsInProgressListener> callInProgressListeners;
    private Collection<CallEndedListener> callEndedListeners;
    private int actualState;

    public OutgoingCallsMonitor(Context context){
        super();
        this.context = context;
        callInProgressListeners = new HashSet<CallIsInProgressListener>();
        callEndedListeners = new HashSet<CallEndedListener>();
        outgoingCallsMonitor = new OutgoingCallsMonitorHelper(context);
        outgoingCallsMonitor.addListener(this);
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber){
        super.onCallStateChanged(state, incomingNumber);
        actualState = state;
        switch (state){
            case TelephonyManager.CALL_STATE_IDLE:
                if(previousState == PhoneState.OffHook){
                    this.finishDate = new Date();
                    Log.d("tesis", "Finalizando llamada a las: " + finishDate.getTime());
                    this.timeToEstablish = (finishDate.getTime() - initDate.getTime());
                    fireCallEndedEvent(finishDate, lastCalledNumber);
                }

                previousState = PhoneState.Idle;
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                previousState = PhoneState.OffHook;
                this.initDate = new Date();
                fireCallInProgressEvent(initDate, lastCalledNumber);
                Log.d("tesis", "Comenzando llamada a las: " + initDate.getTime());
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                previousState = PhoneState.Ringing;
                break;
            default:
                break;
        }
    }

    private void fireCallEndedEvent(Date date, String destionationNumber) {
    	for (CallEndedListener listener : callEndedListeners) {
    		listener.handleCallEnded(date, destionationNumber);
    	}
	}

	private void fireCallInProgressEvent(Date initDate, String lastCalledNumber) {
        for (CallIsInProgressListener listener : callInProgressListeners) {
            listener.handleCallIsInProgress(initDate, lastCalledNumber);
        }
    }

    @Override
    public void addListener(CallIsInProgressListener listener) {
        callInProgressListeners.add(listener);
    }
    
    public void addListener(CallEndedListener listener) {
        callEndedListeners.add(listener);
    }

    @Override
    public boolean removeListener(CallIsInProgressListener listener) {
        return callInProgressListeners.remove(listener);
    }

    @Override
    public void startListening() {
        outgoingCallsMonitor.startListening();

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
        telephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void stopListening() {
        outgoingCallsMonitor.stopListening();

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
        telephonyManager.listen(this, LISTEN_NONE);
    }

    @Override
    public void handlePhoneIsMakingACall(String number, Date date) {
        this.lastCalledNumber = number;
        Log.d("tesis", "Phone is making a call disparado a las " + date.getTime());
    }

	public int getActualState() {
		return actualState;
	}
}

enum PhoneState {
    OffHook, Idle, Ringing
}
