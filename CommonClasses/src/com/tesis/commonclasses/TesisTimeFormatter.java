package com.tesis.commonclasses;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


public class TesisTimeFormatter {
	public static DateTimeFormatter getFormatter() {
		return DateTimeFormat.forPattern("yyyyMMddHHmmss.SSSS").withZoneUTC();
	}
}
