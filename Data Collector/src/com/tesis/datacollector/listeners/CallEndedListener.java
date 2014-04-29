package com.tesis.datacollector.listeners;

import java.util.Date;

public interface CallEndedListener {
	void handleCallEnded(Date date, String destinationNumber);
}
