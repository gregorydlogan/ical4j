package net.fortuna.ical4j;

import static org.junit.Assert.assertEquals;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.property.RRule;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import junit.framework.TestCase;

public class OpencastTests extends TestCase {

  private static Logger logger = LoggerFactory.getLogger(OpencastTests.class);
  private final TimeZone utc = TimeZone.getTimeZone("UTC");
  private final TimeZone cet = TimeZone.getTimeZone("CET");

  public void testCalculateDaysChange() throws ParseException {
    java.util.Calendar start;
    java.util.Calendar end;
    long durationMillis;
    String days;
    List<Period> periods;

    start = java.util.Calendar.getInstance(cet);
    start.set(2016, Calendar.MARCH, 24, 12, 5);
    end = java.util.Calendar.getInstance(cet);
    end.set(2016, Calendar.MARCH, 30, start.get(java.util.Calendar.HOUR_OF_DAY), 10);
    durationMillis = (end.get(java.util.Calendar.MINUTE) - start.get(java.util.Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TH,FR,SA,SU"; // --> A day before when switch to UCT (0-2)
    periods = generatePeriods(cet, start, end, days, durationMillis);
    assertEquals(5, periods.size());
    assetCorrectDates(start, periods);
  }

  private void assetCorrectDates(Calendar start, List<Period> periods) {
    start.set(Calendar.MILLISECOND, 0);
    for (Period p : periods) {
      assertEquals("Incorrect start date", start.getTime().getTime(), p.getStart().getTime());
      start.add(Calendar.DATE, 1);
    }
  }

  public void testCalculateDSTSpringForwardChange() throws ParseException {
    Calendar start;
    Calendar end;
    long durationMillis;
    String days;

    // CET->CEST test (March 25 is CET->CEST)
    TimeZone cetCest = TimeZone.getTimeZone("Europe/Berlin");

    //On Sunday, March 27, 2:00 am CET->CEST
    start = Calendar.getInstance(cetCest);
    start.set(2016, Calendar.MARCH, 15, 0, 5);
    end = Calendar.getInstance(cetCest);
    end.set(2016, Calendar.APRIL, 11, start.get(Calendar.HOUR_OF_DAY), 10);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TH,FR,SA,SU";
    doDSTChangeOverTest(cetCest, start, end, days, durationMillis);
  }

  public void testCalculateDSTFallBackChange() throws ParseException {
    Calendar start;
    Calendar end;
    long durationMillis;
    String days;
    TimeZone cetCest = TimeZone.getTimeZone("Europe/Berlin");

    //On Sunday, October 30, 3:00 am, CEST->CET
    start = Calendar.getInstance(cetCest);
    start.set(2016, Calendar.OCTOBER, 20, 0, 5);
    end = Calendar.getInstance(cetCest);
    end.set(2016, Calendar.NOVEMBER, 8, start.get(Calendar.HOUR_OF_DAY), 10);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TU,WE,TH,FR,SA,SU";
    doDSTChangeOverTest(cetCest, start, end, days, durationMillis);
  }

  private void doDSTChangeOverTest(TimeZone tz, Calendar start,
          Calendar end, String days, long durationMillis) throws ParseException  {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EE MMM dd HH:mm:ss zzz yyyy");
    simpleDateFormat.setTimeZone(tz);

    List<Period> periods = generatePeriods(cet, start, end, days, durationMillis);
    for (Period p : periods) {
      logger.debug(p.toString());
    }
    assertEquals("Incorrect number of scheduled events!", 20, periods.size());
    for (Period d : periods) {
      Calendar instance = Calendar.getInstance(tz);
      long time = d.getStart().getTime();
      instance.setTime(new Date(time));
      logger.debug("Instance {}, calendar hour {}, calendar min {}, zone {}",
              simpleDateFormat.format(instance.getTime()),
              instance.get(Calendar.HOUR_OF_DAY),
              instance.get(Calendar.MINUTE),
              instance.getTimeZone().getID());
      assertEquals( "Incorrect start time?", 0, instance.get(Calendar.HOUR_OF_DAY));
    }
  }

  private List<Period> generatePeriods(TimeZone tz, java.util.Calendar start, java.util.Calendar end, String days, Long duration)
          throws ParseException {
    java.util.Calendar utcDate = java.util.Calendar.getInstance(utc);
    start.setTimeZone(tz);
    end.setTimeZone(tz);
    utcDate.setTime(start.getTime());
    RRule rRule = new RRule(generateRule(days, utcDate.get(java.util.Calendar.HOUR_OF_DAY), utcDate.get(
            java.util.Calendar.MINUTE)));
    return calculatePeriods(start.getTime(), end.getTime(), duration, rRule, tz);
  }

  private String generateRule(String days, int hour, int minute) {
    return String.format("FREQ=WEEKLY;BYDAY=%s;BYHOUR=%d;BYMINUTE=%d", days, hour, minute);
  }

  public static List<Period> calculatePeriods(Date startUtc, Date endUtc, long duration, RRule rRule, TimeZone tz) {
    List<Period> events = new LinkedList<>();
    TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();

    logger.debug("Inbound start of recurrence {} and end of recurrence {}", startUtc, endUtc);
    org.joda.time.DateTime startInTz = new org.joda.time.DateTime(startUtc).toDateTime(org.joda.time.DateTimeZone.forTimeZone(tz));
    org.joda.time.DateTime endInTz = new org.joda.time.DateTime(endUtc).toDateTime(org.joda.time.DateTimeZone.forTimeZone(tz));

    DateTime periodStartTz = new DateTime(startInTz.toDate());
    DateTime periodEndTz = new DateTime(endInTz.toDate());

    java.util.Calendar endCalendarTz = java.util.Calendar.getInstance(tz);
    endCalendarTz.setTime(periodEndTz);

    java.util.Calendar calendarTz = java.util.Calendar.getInstance(tz);
    calendarTz.setTime(periodStartTz);

    calendarTz.set(java.util.Calendar.DAY_OF_MONTH, endCalendarTz.get(java.util.Calendar.DAY_OF_MONTH));
    calendarTz.set(java.util.Calendar.MONTH, endCalendarTz.get(java.util.Calendar.MONTH));
    calendarTz.set(java.util.Calendar.YEAR, endCalendarTz.get(java.util.Calendar.YEAR));
    periodEndTz.setTime(calendarTz.getTime().getTime() + duration);
    duration = duration % (org.joda.time.DateTimeConstants.MILLIS_PER_DAY);

    logger.debug("1-Looking at recurrences for {} to {}, duration {}", periodStartTz.getTime(), periodEndTz.getTime(), duration);
    // Have to change the TimeZone to UTC for the rRule.getRecur() to work correctly in a non-global TimeZone
    periodStartTz.setTimeZone(registry.getTimeZone("UTC"));
    periodEndTz.setTimeZone(registry.getTimeZone("UTC"));

    // Special case for first Sunday in a DST change recurrence
    // Sunday DST day bug: https://github.com/ical4j/ical4j/issues/117
    boolean firstSundaySpecialCase = false;
    if (((tz.inDaylightTime(periodStartTz) && !tz.inDaylightTime(periodEndTz))
            || (!tz.inDaylightTime(periodStartTz) && tz.inDaylightTime(periodEndTz)))) {
      firstSundaySpecialCase = true;
    }

    for (Object date : rRule.getRecur().getDates(periodStartTz, periodEndTz, net.fortuna.ical4j.model.parameter.Value.DATE_TIME)) {
      Date d = (Date) date;
      net.fortuna.ical4j.model.DateTime datePeriod = new net.fortuna.ical4j.model.DateTime(d);
      java.util.Calendar cDate = java.util.Calendar.getInstance(registry.getTimeZone("UTC"));
      cDate.setTime(datePeriod);
      java.util.Calendar tzDate  = java.util.Calendar.getInstance(tz);
      tzDate.setTime(datePeriod);
      logger.debug("Looking at recurrence date {}, {}", d);
      // Adjust for DST regardless of end time DST
      if (tz.inDaylightTime(periodStartTz)) {
        d.setTime(d.getTime() + tz.getDSTSavings()); //Adjust for DST
        // Special case for first Sunday
        // Sunday DST day bug: https://github.com/ical4j/ical4j/issues/117
        if (!tz.inDaylightTime(d)
                && cDate.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY
                && firstSundaySpecialCase) {
          d.setTime(d.getTime() + tz.getDSTSavings());
          firstSundaySpecialCase = false;
        }
      } else if (tz.inDaylightTime(d)  // Otherwise only adjust special case
              && cDate.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY
              && firstSundaySpecialCase) {
        // Special case for first Sunday
        // Sunday DST day bug: https://github.com/ical4j/ical4j/issues/117
        d.setTime(d.getTime() - tz.getDSTSavings());
        firstSundaySpecialCase = false;
      }
      java.util.Calendar cal =  new java.util.Calendar.Builder().setTimeZone(tz).setInstant(d).build();
      cal.setTimeZone(tz);
      // update with the updated d
      cDate.setTime(d);
      Period p = new Period(new net.fortuna.ical4j.model.DateTime(cDate.getTimeInMillis()),
              new net.fortuna.ical4j.model.DateTime(cDate.getTimeInMillis() + duration));

      events.add(p);
      logger.trace("Adding date {} period '{}'", d, p.toString());
    }
    return events;
  }
}
