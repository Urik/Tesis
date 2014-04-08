package com.tesis.receptordellamadas.listeners;

import java.util.Date;

import org.joda.time.DateTime;

public interface CallReceivedListener {
    void handleCallHasBeenReceived(String incomingNumber, DateTime time);
}
