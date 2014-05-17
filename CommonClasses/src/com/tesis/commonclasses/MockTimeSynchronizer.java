package com.tesis.commonclasses;

import org.joda.time.DateTime;

public class MockTimeSynchronizer implements TimeSynchronizer {

	@Override
	public boolean synchronize() {
		return true;
	}

	@Override
	public DateTime getDate() {
		return new DateTime().plusMonths(1);
	}

}
