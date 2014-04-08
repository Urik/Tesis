package com.tesis.datacollector.listeners;

import java.util.Date;

public interface SMSSentListener {
    void handleSMSSent(long timeInMs, Date initDate, Date finishDate, String destinationNumber);
}
