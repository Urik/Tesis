package com.tesis.datacollector;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.Seconds;

import com.tesis.commonclasses.Constants;
import com.tesis.commonclasses.listeners.EventsProducer;
import com.tesis.datacollector.listeners.SMSSentListener;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;

public class SmsSender extends BroadcastReceiver implements EventsProducer<SMSSentListener>{

	private final SmsManager smsManager;
	private final Context ctx;
	private List<SMSSentListener> listeners;
	
	public SmsSender(Context ctx) {
		this.ctx = ctx;
		smsManager = SmsManager.getDefault();
		listeners = new ArrayList<SMSSentListener>();
	}
	
	public void sendSms(String destinationNumber, String content) {
		Intent data = new Intent("SMS_SENT");
		data.putExtra("DESTINATION", destinationNumber);
		data.putExtra("START_DATE", new DateTime());
		PendingIntent smsSentIntent = PendingIntent.getBroadcast(ctx, 0, data, 0);
		ctx.registerReceiver(this, new IntentFilter("SMS_SENT"));
		smsManager.sendTextMessage(destinationNumber, null, content, smsSentIntent, null);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		DateTime sentMoment = new DateTime();
		DateTime originMoment = (DateTime) intent.getExtras().get("START_DATE");
		String destination = intent.getStringExtra("DESTINATION");
		long sentDuration = Math.abs(new Period(sentMoment, originMoment).toStandardDuration().getMillis());
		for(SMSSentListener listener : listeners) {
			listener.handleSMSSent(sentDuration, originMoment.toDate(), sentMoment.toDate(), destination);
		}
		ctx.unregisterReceiver(this);
	}

	@Override
	public void addListener(SMSSentListener listener) {
		listeners.add(listener);
	}

	@Override
	public boolean removeListener(SMSSentListener listener) {
		return listeners.remove(listener);
	}

	@Override
	public void startListening() {
	}

	@Override
	public void stopListening() {
	}

}
