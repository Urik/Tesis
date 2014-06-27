package com.tesis.commonclasses;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.joda.time.DateTime;

import android.os.SystemClock;

public class NtpTimeSynchronizer implements TimeSynchronizer {
	NTPUDPClient cli = new NTPUDPClient();
	long offset = -1;
	public NtpTimeSynchronizer() {
		cli.setDefaultTimeout(10000);
	}
	@Override
	public boolean synchronize() {
		try{
			InetAddress address = InetAddress.getByName("ar.pool.ntp.org");
			TimeInfo time = null;
			time = cli.getTime(address);
			time.computeDetails();
			offset = time.getOffset();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public DateTime getDate() {
		return new DateTime().plusMillis((int)offset);
	}

}
