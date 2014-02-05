package com.tesis.datacollector;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.tesis.datacollector.listeners.CallIsInProgressListener;
import com.tesis.commonclasses.listeners.EventsProducer;
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

    private Collection<CallIsInProgressListener> listeners;
    private int actualState;

    public OutgoingCallsMonitor(Context context){
        super();
        this.context = context;
        listeners = new HashSet<CallIsInProgressListener>();
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
                }

                previousState = PhoneState.Idle;
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                previousState = PhoneState.OffHook;
                this.initDate = new Date();
                fireEvent(initDate, lastCalledNumber);
                Log.d("tesis", "Comenzando llamada a las: " + initDate.getTime());
                Toast.makeText(context, "Phone state off hook", Toast.LENGTH_LONG).show();
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                previousState = PhoneState.Ringing;
                Toast.makeText(context, "Phone state Ringing", Toast.LENGTH_LONG).show();
                break;
            default:
                break;
        }
    }

    private void fireEvent(Date initDate, String lastCalledNumber) {
        for (CallIsInProgressListener listener : listeners) {
            listener.handleCallIsInProgress(initDate, lastCalledNumber);
        }
    }

    @Override
    public void addListener(CallIsInProgressListener listener) {
        listeners.add(listener);
    }

    @Override
    public boolean removeListener(CallIsInProgressListener listener) {
        return listeners.remove(listener);
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
