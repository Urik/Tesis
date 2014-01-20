package com.tesis.datacollector.listeners;

import java.util.Date;

public interface CallIsInProgressListener {
    void handleCallIsInProgress(Date startDate, String destination);
}
