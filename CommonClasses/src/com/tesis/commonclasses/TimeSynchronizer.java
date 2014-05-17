package com.tesis.commonclasses;

import org.joda.time.DateTime;

public interface TimeSynchronizer {
	public boolean synchronize();
	public DateTime getDate();
}
