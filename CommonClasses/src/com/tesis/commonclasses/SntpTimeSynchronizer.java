package com.tesis.commonclasses;

import org.joda.time.DateTime;

import android.os.SystemClock;

public class SntpTimeSynchronizer implements TimeSynchronizer {
	SntpClient sntpClient = new SntpClient();
	
	@Override
	public boolean synchronize() {
        return sntpClient.requestTime("ar.pool.ntp.org", 10000);
	}

	@Override
	public DateTime getDate() {
		long realTime = sntpClient.getNtpTime() + SystemClock.elapsedRealtime() - sntpClient.getNtpTimeReference();
        return new DateTime(realTime);
	}

}
