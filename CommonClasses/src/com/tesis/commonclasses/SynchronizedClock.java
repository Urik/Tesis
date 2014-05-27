package com.tesis.commonclasses;


import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.JodaTimePermission;
import org.joda.time.Period;

import android.os.AsyncTask;
import android.os.SystemClock;

public class SynchronizedClock {
	private static Long averageSyncOffset = 0l;
	private static int averageCounter = 0;
	
	private final static Object lock = new Object();
	
	private static TimeSynchronizer timeSynchronizer = new SntpTimeSynchronizer();
	
	public static DateTime getCurrentTime() {
		synchronized(lock)
		{
			if (averageSyncOffset == 0l) {
				synchronize();
			}
			return new DateTime().plus(averageSyncOffset);
		}
	}

	public static boolean synchronize() {
		ExecutorService executor = Executors.newCachedThreadPool();
		Future<Boolean> synchronizationFuture = executor.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				DateTime serverTime = null;
				boolean serverTimeHasBeenFound = false;
				int timeOutCounter = 0;
				while (!serverTimeHasBeenFound && timeOutCounter < 10) {
					serverTimeHasBeenFound = timeSynchronizer.synchronize();
			        if (serverTimeHasBeenFound) {
			        	serverTime = timeSynchronizer.getDate();
			        } else {
			        	try {
			        	    Thread.sleep(1000);
			        	} catch(InterruptedException ex) {
			        	    Thread.currentThread().interrupt();
			        	}
			        }
			        timeOutCounter++;
				}
				boolean timeWasSynchronized = false;
				if (serverTime != null) {
					DateTime localTime = new DateTime();
					Period period = new Period(localTime, serverTime);
					synchronized(lock) {
						averageSyncOffset = ((averageSyncOffset * averageCounter) + period.toStandardDuration().getMillis()) / (averageCounter + 1);
						averageCounter++;
					}
					timeWasSynchronized = true;
				}
				
				return timeWasSynchronized;
			}
		});
		try {
			return synchronizationFuture.get();
		} catch (Exception e) {
			System.out.println(e.toString());
			return false;
		}
	}	
	
	public static TimeSynchronizer getTimeSynchronizer() {
		return timeSynchronizer;
	}

	public static void setTimeSynchronizer(TimeSynchronizer timeSynchronizer) {
		SynchronizedClock.timeSynchronizer = timeSynchronizer;
	}

	private static class NtpClient extends AsyncTask<Void, Void, DateTime> {
		@Override
		protected DateTime doInBackground(Void... arg0) {
			while (true) {
				SntpClient sntpClient = new SntpClient();
		        boolean timeObtained = sntpClient.requestTime("ar.pool.ntp.org", 10000);
		        if (timeObtained) {
		        	long realTime = sntpClient.getNtpTime() + SystemClock.elapsedRealtime() - sntpClient.getNtpTimeReference();
			        return new DateTime(realTime);
		        } else {
		        	try {
		        	    Thread.sleep(1000);
		        	} catch(InterruptedException ex) {
		        	    Thread.currentThread().interrupt();
		        	}
		        }

			}
		}
	}
}
