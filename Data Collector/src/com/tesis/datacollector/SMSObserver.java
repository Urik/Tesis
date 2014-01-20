package com.tesis.datacollector;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;

import com.tesis.commonclasses.listeners.EventsProducer;
import com.tesis.datacollector.listeners.SMSSentListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SMSObserver extends ContentObserver implements EventsProducer<SMSSentListener> {
    private List<SMSSentListener> listeners = new ArrayList<SMSSentListener>();

    private static final int MESSAGE_TYPE_QUEUED = 6;
    private static final int MESSAGE_TYPE_SENDING = 4;
    private static final int MESSAGE_TYPE_SENT = 2;
    private static final String CONTENT_SMS = "content://sms";

    Context context;
    int times = 0;
    private Date initDate = null;
    private Date finishDate = null;
    private long timeToEstablish = 0;


    public SMSObserver(Handler handler, Context context) {
        super(handler);
        this.context = context;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        Uri uriSMSURI = Uri.parse(CONTENT_SMS);
        Cursor cur = context.getContentResolver().query(uriSMSURI, null, null, null, null);
        if(cur != null){
            cur.moveToNext();

            int type = cur.getInt(cur.getColumnIndex("type"));
            if (!isOutgoingMessage(type)){
                return;
            }
            String content = cur.getString(cur.getColumnIndex("body"));
            if(times == 0 && type == MESSAGE_TYPE_QUEUED){
                this.initDate = new Date();
                Toast.makeText(context, "En cola: " + content + " " + type, Toast.LENGTH_LONG).show();
                times++;
            }else if(times == 1 && type == MESSAGE_TYPE_SENDING){
                    Toast.makeText(context, "Enviando: " + content + " " + type, Toast.LENGTH_LONG).show();
                    times++;
                }else if(times == 2 && type == MESSAGE_TYPE_SENT){
                    String destinationNumber = cur.getString(cur.getColumnIndex("_id"));
                    this.finishDate = new Date();
                    this.timeToEstablish = (finishDate.getTime() - initDate.getTime());
                    content = "Enviado: " + content + " Tiempo de envio: " + timeToEstablish + " ms" + " " + type;
                    Toast.makeText(context, content, Toast.LENGTH_LONG).show();
                    times = 0;
                    handleSmsSent(timeToEstablish, initDate, finishDate);
                }
        }
    }

    private boolean isOutgoingMessage(int type) {
        return type == MESSAGE_TYPE_SENT || type == MESSAGE_TYPE_SENDING || type == MESSAGE_TYPE_QUEUED;
    }

    private void handleSmsSent(long timeToEstablishInMs, Date initDate, Date finishDate) {
        for (SMSSentListener listener : listeners) {
            listener.handleSMSSent(timeToEstablishInMs, initDate, finishDate);
        }
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
