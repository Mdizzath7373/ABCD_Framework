package com.eit.abcdframework.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TimeZoneServices {
	private static Logger LOGGER = LoggerFactory.getLogger(TimeZoneServices.class);

//	public static Date getDateInTimeZone(Date date) {
//		String timeZoneId = "Asia/Riyadh";
//		TimeZone tz = TimeZone.getTimeZone(timeZoneId);
//		Calendar cal = Calendar.getInstance(tz);
//		cal.setTime(date); // Set the time using setTime method
//		return cal.getTime();
//	}
	public static void main(String[] args) {
		try {

			Duration totalDuration = DatatypeFactory.newInstance().newDuration(true, 0, 0, 0, 5, 45, 55);
			Duration secondDuration = DatatypeFactory.newInstance().newDuration(true, 0, 0, 0, 23, 30, 55);
			totalDuration.add(secondDuration);
			Duration thirdDuration = DatatypeFactory.newInstance().newDuration(true, 0, 0, 0, 6, 45, 55);
			totalDuration.add(thirdDuration);
//			System.out.println(totalDuration.getDays() + ":"
//					+ totalDuration.getHours() + ":"
//					+ totalDuration.getMinutes() + ":"
//					+ totalDuration.getSeconds());

//			System.out.println(TimeZoneServices.getDate(TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh")));
			System.out.println(TimeZoneServices.getDateInTimeZoneforSKT(new Date(),"Asia/Riyadh"));
            System.out.println(TimeZoneServices.getDateInTimeZoneforSKT(new Date(),"Asia/Kolkata"));
		} catch (DatatypeConfigurationException e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}

	}

	public static String getTimeINYYYYMMddssa(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
		String timeStampStr1 = sdf.format(date);
//		
		return timeStampStr1;
	}

	public static Date getDateTZ(String str) {
		Date date = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
			date = sdf.parse(str);
		} catch (Exception e) {

		}
		return date;
	}

	public static Date getDate(String str) {
		Date date = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			date = sdf.parse(str);
		} catch (Exception e) {

		}
		return date;
	}

	public static String getDate(Date date) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			return sdf.format(date);
		} catch (Exception e) {

		}
		return null;
	}

	public static String getDate1(Date date) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd");
			return sdf.format(date);
		} catch (Exception e) {

		}
		return null;
	}

	public static String getStrTZ(Date date) {
		String str = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
			str = sdf.format(date);
		} catch (Exception e) {

		}
		return str;
	}

	public static String getStrTZDay(Date date) {
		String str = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("EEE MMMM dd, yyyy");
			str = sdf.format(date);
		} catch (Exception e) {

		}
		return str;
	}

	public static String[] getTime(String timezone) {
		Calendar GMTTime = new GregorianCalendar(TimeZone.getTimeZone(timezone));
		String hour = String.valueOf(GMTTime.get(Calendar.HOUR));
		String minute = String.valueOf(GMTTime.get(Calendar.MINUTE));
		String second = String.valueOf(GMTTime.get(Calendar.SECOND));
		second = Integer.parseInt(second) > 9 ? second : "0" + second;
		String date = String.valueOf(GMTTime.get(Calendar.DATE)) + "-" + String.valueOf(GMTTime.get(Calendar.MONTH) + 1)
				+ "-" + String.valueOf(GMTTime.get(Calendar.YEAR));

		String time[] = { date, hour, minute, second };

		return time;
	}

	public static Date getDateInTimeZone() {
		Date currentDate = new Date();
		return getDateInTimeZone(currentDate);
	}

	public static Date getDateInTimeZoneforSKT(String region) {
		Date currentDate = new Date();
		return getDateInTimeZoneforSKT(currentDate, region);
	}

	public static Date getDateInTimeZoneforSKT(Date date, String region) {
		String timeZoneId = region;
		TimeZone tz = TimeZone.getTimeZone(timeZoneId);
		// Calendar mbCal = new
		// GregorianCalendar(TimeZone.getTimeZone(timeZoneId));
		Calendar mbCal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId));
		mbCal.setTimeInMillis(date.getTime());

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, mbCal.get(Calendar.YEAR));
		cal.set(Calendar.MONTH, mbCal.get(Calendar.MONTH));
		cal.set(Calendar.DAY_OF_MONTH, mbCal.get(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, mbCal.get(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, mbCal.get(Calendar.MINUTE));
		cal.set(Calendar.SECOND, mbCal.get(Calendar.SECOND));
		cal.set(Calendar.MILLISECOND, mbCal.get(Calendar.MILLISECOND));

		return cal.getTime();
	}

	public static Date getDateInTimeZone(Date date) {
		String timeZoneId = "Asia/Riyadh";
		TimeZone tz = TimeZone.getTimeZone(timeZoneId);
		// Calendar mbCal = new
		// GregorianCalendar(TimeZone.getTimeZone(timeZoneId));
		Calendar mbCal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId));
		mbCal.setTimeInMillis(date.getTime());

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, mbCal.get(Calendar.YEAR));
		cal.set(Calendar.MONTH, mbCal.get(Calendar.MONTH));
		cal.set(Calendar.DAY_OF_MONTH, mbCal.get(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, mbCal.get(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, mbCal.get(Calendar.MINUTE));
		cal.set(Calendar.SECOND, mbCal.get(Calendar.SECOND));
		cal.set(Calendar.MILLISECOND, mbCal.get(Calendar.MILLISECOND));

		return cal.getTime();
	}

	public static Date getCurrentDate(String timezone) {
		Calendar GMTTime = new GregorianCalendar(TimeZone.getTimeZone(timezone));
		return GMTTime.getTime();
	}

	public static String getStrDZ(Date date) {
		String str = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			str = sdf.format(date);
		} catch (Exception e) {

		}
		return str;
	}

	public static Date getDateTDZ(String str) {
		Date date = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			date = sdf.parse(str);
		} catch (Exception e) {

		}
		return date;
	}

	public static String getStrDTSZ(Date date) {
		String str = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a");
			str = sdf.format(date);
		} catch (Exception e) {

		}
		return str;
	}

	public static Date getDateDZ(String str) {
		Date date = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a");
			date = sdf.parse(str);
		} catch (Exception e) {

		}
		return date;
	}

	public static Date getDateInddHHmmssFormat(String str) {
		Date date = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd:HH:mm:ss");
			date = sdf.parse(str);
		} catch (Exception e) {

		}
		return date;
	}

	public static long addy(String durationString) {
		try {
			long days;
			long hours;
			long minutes;
			long seconds;
			String[] durationArray = durationString.split(":");
			days = (Integer.valueOf(durationArray[0])) * (3600 * 24);
			hours = (Integer.valueOf(durationArray[1])) * 3600;
			minutes = (Integer.valueOf(durationArray[2])) * 60;
			seconds = Integer.valueOf(durationArray[3]);
			long TotalValue = days + hours + minutes + seconds;
			return TotalValue;
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0;
		}

	}

	public static String getTimeINYYYYMMddss2(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");
		String timeStampStr1 = sdf.format(date);
		return timeStampStr1;
	}

	public static String getTimeINYYYYMMddss(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String timeStampStr1 = sdf.format(date);
		return timeStampStr1;
	}

	public static String getTimeINYYYYMMddssSSS(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		String timeStampStr1 = sdf.format(date);
		return timeStampStr1;
	}

	public static String getStrTZDateTime(Date date) {
		String str = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a");
			str = sdf.format(date);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return str;
	}

	public static Date getDateTZDDMMYYYY(String str) {
		Date date = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			date = sdf.parse(str);
		} catch (Exception e) {
		}
		return date;
	}

	public static String getDateTZDDMMYYYYHHMMSS(String str) {
		Date eventTimeStamp = null;
		String date = null;
		try {
			SimpleDateFormat ddmmyyyy = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			SimpleDateFormat yyyymmdd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
			eventTimeStamp = yyyymmdd.parse(str);
			date = ddmmyyyy.format(eventTimeStamp);
		} catch (Exception e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return date;
	}

	public static String getAMTo24Hr(String strdate) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd H:mm");
			SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
			Date date2 = sdf1.parse(strdate);
			String timeStampStr1 = sdf.format(date2);
//			
			return timeStampStr1;
		} catch (Exception e) {

			return null;
		}
	}

	public static Date getDateYYYYMMDDHHMMSS(String str) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			Date timeStampStr1 = sdf.parse(str);
			return timeStampStr1;
		} catch (Exception e) {
			return null;
		}

	}

	public static Date getDateYYYYMMDDHHMMSSSSS(String str) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		try {
			Date timeStampStr1 = sdf.parse(str);
			return timeStampStr1;
		} catch (Exception e) {
			return null;
		}
	}

	public static Date getDateHHMMSSToDate(Date date) {
		Date str = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			str = sdf.parse(sdf.format(date));
		} catch (Exception e) {

		}
		return str;
	}

	public static String getDateTStr(Date date) {
		String str = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			str = sdf.format(date);
		} catch (Exception e) {

		}
		return str;
	}

	public static int addyWithOutDay(String durationString) {
		int hours;
		int minutes;
		int seconds;
		String[] durationArray = durationString.split(":");
		hours = (Integer.valueOf(durationArray[0])) * 3600;
		minutes = (Integer.valueOf(durationArray[1])) * 60;
		seconds = Integer.valueOf(durationArray[2]);
		int TotalValue = hours + minutes + seconds;
		return TotalValue;

	}

	public static String getPdfDate(String strdate1) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
			SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
			Date date2 = sdf1.parse(strdate1);
			String timeStampStr1 = sdf.format(date2);
//			
			return timeStampStr1;
		} catch (Exception e) {
			// TODO: handle exception

			return null;
		}
	}

	public static Date getDateTimeZone(Date date, String timeZoneId) {
		Calendar mbCal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId));
		mbCal.setTimeInMillis(date.getTime());
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, mbCal.get(Calendar.YEAR));
		cal.set(Calendar.MONTH, mbCal.get(Calendar.MONTH));
		cal.set(Calendar.DAY_OF_MONTH, mbCal.get(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, mbCal.get(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, mbCal.get(Calendar.MINUTE));
		cal.set(Calendar.SECOND, mbCal.get(Calendar.SECOND));
		cal.set(Calendar.MILLISECOND, mbCal.get(Calendar.MILLISECOND));
		return cal.getTime();
	}


	public static Calendar getDateForTimeZones(Date date, String timeZoneId) {
		Calendar mbCal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId));
		mbCal.setTimeInMillis(date.getTime());
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, mbCal.get(Calendar.YEAR));
		cal.set(Calendar.MONTH, mbCal.get(Calendar.MONTH));
		cal.set(Calendar.DAY_OF_MONTH, mbCal.get(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, mbCal.get(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, mbCal.get(Calendar.MINUTE));
		cal.set(Calendar.SECOND, mbCal.get(Calendar.SECOND));
		cal.set(Calendar.MILLISECOND, mbCal.get(Calendar.MILLISECOND));
		return cal;
	}

	public static Date getDateYYYYMMDDHHMM(String str) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		try {
			Date timeStampStr1 = sdf.parse(str);
			return timeStampStr1;
		} catch (Exception e) {
			return null;
		}

	}

	public static String getDateYYYYMMDD(String str) {
		String str1 = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date timeStampStr1 = sdf.parse(str);
			str1 = sdf.format(timeStampStr1);
			return str1;
		} catch (Exception e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return null;
		}
	}

	public static Date getCurrentUtcTime() {
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
			simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			SimpleDateFormat localDateFormat = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
			return localDateFormat.parse(simpleDateFormat.format(new Date()));
		} catch (Exception e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return null;
		}
	}

	public static Date getDateddMMyyHHmmss(String str) {

		try {
			return new SimpleDateFormat("ddMMyyHHmmss").parse(str);
		} catch (Exception e) {
			return null;
		}
	}

	public static Date getStartDateOfMonth(int year, int month) {

		try {
			Calendar calendar = Calendar.getInstance();
			calendar.set(year, month - 1, 4, 0, 0, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
			Date beg = calendar.getTime();
			return beg;
		} catch (Exception e) {
			return new Date();
		}
	}

	public static Date getEndDateOfMonth(int year, int month) {

		try {
			Calendar calendar1 = Calendar.getInstance();
			calendar1.set(year, month - 1, 4, 23, 59, 59);
			calendar1.set(Calendar.DAY_OF_MONTH, calendar1.getActualMaximum(Calendar.DAY_OF_MONTH));
			Date end = calendar1.getTime();
			return end;
		} catch (Exception e) {
			return new Date();
		}
	}

	public static String getDiffDates(String fromDate, String toDate) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long time_difference = 0;

		long days_difference = 0;

		long years_difference = 0;

		long seconds_difference = 0;

		long minutes_difference = 0;

		long hours_difference = 0;

		try {
			Date from = sdf.parse(fromDate);
			Date to = sdf.parse(toDate);

			time_difference = to.getTime() - from.getTime();

			// Calucalte time difference in days using TimeUnit class
			days_difference = TimeUnit.MILLISECONDS.toDays(time_difference) % 365;
//	             years_difference = TimeUnit.MILLISECONDS.toDays(time_difference) / 365l;  
			// Calucalte time difference in seconds using TimeUnit class
			seconds_difference = TimeUnit.MILLISECONDS.toSeconds(time_difference) % 60;
			// Calucalte time difference in minutes using TimeUnit class
			minutes_difference = TimeUnit.MILLISECONDS.toMinutes(time_difference) % 60;
			// Calucalte time difference in hours using TimeUnit class
			hours_difference = TimeUnit.MILLISECONDS.toHours(time_difference) % 24;

		} catch (Exception excep) {
			excep.printStackTrace();
		}
		return days_difference + ":" + hours_difference + ":" + minutes_difference + ":" + seconds_difference;
	}
}
