package com.tesis.commonclasses;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
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
		long offset = SystemClock.elapsedRealtime() - sntpClient.getNtpTimeReference();
		long realTime = sntpClient.getNtpTime() + offset;
        return new DateTime(realTime);
	}

}
