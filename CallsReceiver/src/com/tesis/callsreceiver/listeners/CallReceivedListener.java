package com.tesis.callsreceiver.listeners;

import java.util.Date;

public interface CallReceivedListener {
    void handleCallHasBeenReceived(String incomingNumber, Date time);
}
