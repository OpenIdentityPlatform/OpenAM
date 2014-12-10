/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2006 Sun Microsystems Inc
 */
/*
 * Portions Copyright 2011-2014 ForgeRock AS
 */

package org.forgerock.openam.entitlement.conditions.environment;

import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.util.Pair;
import org.forgerock.util.time.TimeService;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import static com.sun.identity.entitlement.EntitlementException.*;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.*;

public class SimpleTimeCondition extends EntitlementConditionAdaptor {

    /**
     * Key that is used to define current time that is passed in the {@code env} parameter while invoking
     * {@code getConditionDecision} method of the {@code SimpleTimeCondition}. Value for the key should be a
     * {@code Long} object whose value is time in milliseconds since epoch. If no value is given for this, it is assumed
     * to be the current system time
     *
     * @see #ENFORCEMENT_TIME_ZONE
     */
    public static final String REQUEST_TIME = "requestTime";

    /**
     * <p>Key that is used in a {@code SimpleTimeCondition} to define the time zone basis to evaluate the policy.</p>
     *
     * <p>The value corresponding to the key has to be a one element {@code Set} where the element is a {@code String}
     * that is one of the standard timezone IDs supported by java or a {@code String} of the pattern
     * {@code GMT[+|-]hh[[:]mm]} here. If the value is not a valid time zone id and does not match the pattern
     * {@code GMT[+|-]hh[[:]mm]}, it would default to GMT
     *
     *  @see java.util.TimeZone
     */
    public static final String ENFORCEMENT_TIME_ZONE = "enforcementTimeZone";

    /**
     * Key that is used to define the time zone that is passed in the {@code env parameter while invoking
     * {@codegetConditionDecision} method of a {@code SimpleTimeCondition} Value for the key should be a {@codeTimeZone}
     * object. This would be used only if the {@code ENFORCEMENT_TIME_ZONE} is not defined for the
     * {@code SimpleTimeCondition}.
     *
     *  @see #ENFORCEMENT_TIME_ZONE
     *  @see java.util.TimeZone
     */
    public static final String REQUEST_TIME_ZONE = "requestTimeZone";

    private static final String DATE_FORMAT = "yyyy:MM:dd";

    private static final String[] DAYS_OF_WEEK = {"", "sun", "mon", "tue", "wed", "thu", "fri", "sat"};

    private final Debug debug;
    private final TimeService timeService;
    private final DateFormat dateFormat;

    private String startTime;
    private String endTime;
    private String startDay;
    private String endDay;
    private String startDate;
    private String endDate;
    private String enforcementTimeZone;

    private int startHour = -1;
    private int startMinute;
    private int endHour = -1;
    private int endMinute;
    private int startDayInt = -1;
    private int endDayInt = -1;
    private Calendar startDateCal;
    private Calendar endDateCal;
    private TimeZone enforcementTZ;

    /**
     * Constructs a new SimpleTimeCondition instance.
     */
    public SimpleTimeCondition() {
        this(PrivilegeManager.debug, new TimeService() {
            @Override
            public long now() {
                return System.currentTimeMillis();
            }

            @Override
            public long since(long l) {
                return now() - l;
            }
        });
    }

    /**
     * Constructs a new SimpleTimeCondition instance.
     *
     * @param debug A Debug instance.
     * @param timeService An instance of the TimeService.
     */
    SimpleTimeCondition(Debug debug, TimeService timeService) {
        this.debug = debug;
        this.timeService = timeService;
        dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setLenient(false);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);

            if (!jo.isNull(ENFORCEMENT_TIME_ZONE)) {
                setEnforcementTimeZone(jo.getString(ENFORCEMENT_TIME_ZONE));
            }

            if (!jo.isNull(START_TIME)) {
                setStartTime(jo.getString(START_TIME));
            }
            if (!jo.isNull(END_TIME)) {
                setEndTime(jo.getString(END_TIME));
            }
            if (!jo.isNull(START_DAY)) {
                setStartDay(jo.getString(START_DAY));
            }
            if (!jo.isNull(END_DAY)) {
                setEndDay(jo.getString(END_DAY));
            }

            if (!jo.isNull(START_DATE)) {
                setStartDate(jo.getString(START_DATE));
            }
            if (!jo.isNull(END_DATE)) {
                setEndDate(jo.getString(END_DATE));
            }
        } catch (Exception e) {
            debug.message("SimpleTimeCondition: Failed to set state", e);
        }
    }

    /**
     * Converts time in {@code String} format to an int.
     *
     * @param timeString The string to parse.
     */
    private int parseTimeString(String timeString) {
        StringTokenizer st = new StringTokenizer(timeString, ":");
        if (st.countTokens() != 2) {
            return -1;
        }
        String token1 = st.nextToken();
        String token2 = st.nextToken();
        int hour;
        int minute;
            hour = Integer.parseInt(token1);
            minute = Integer.parseInt(token2);
        if (hour < 0 || hour > 24 || minute < 0 || minute > 59) {
            return -1;
        }
        return hour * 60 + minute;
    }

    /**
     * Converts day in {@code String} format to an int based on the {@code DAYS_OF_WEEK}.
     *
     * @param dayString The string to parse.
     */
    private int parseDayString(String dayString) {
        int day = -1;
        String dayStringLc = dayString.toLowerCase();
        for (int i = 1; i < 8; i++) {
            if (DAYS_OF_WEEK[i].equals(dayStringLc)) {
                day = i;
                break;
            }
        }
        return day;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return toString();
    }

    @Override
    public void validate() throws EntitlementException {

        //Check if the required key(s) are defined
        if (startTime == null && startDay == null && startDate == null) {
            if (debug.errorEnabled()) {
                debug.error("SimpleTimeCondition.validateProperties(): at least one of time properties MUST be "
                        + "defined, " + START_DATE + ", " + START_TIME + ", " + START_DAY);
            }
            throw new EntitlementException(AT_LEAST_ONE_OF_TIME_PROPS_SHOULD_BE_DEFINED,
                    START_DATE + ", " + START_TIME + ", " + START_DAY);
        }

        if (startTime != null && endTime == null) {
            if (debug.errorEnabled()) {
                debug.error("SimpleTimeCondition.validateProperties(): property pair not defined, " + START_TIME + ", "
                        + END_TIME);
            }
            throw new EntitlementException(PAIR_PROPERTY_NOT_DEFINED, START_TIME, END_TIME);
        }
        if (startTime == null && endTime != null) {
            if (debug.errorEnabled()) {
                debug.error("SimpleTimeCondition.validateProperties(): property pair not defined, " + END_TIME + ", "
                        + START_TIME);
            }
            throw new EntitlementException(PAIR_PROPERTY_NOT_DEFINED, END_TIME, START_TIME);
        }

        if (startDay != null && endDay == null) {
            if (debug.errorEnabled()) {
                debug.error("SimpleTimeCondition.validateProperties(): property pair not defined, " + START_DAY + ", "
                        + END_DAY);
            }
            throw new EntitlementException(PAIR_PROPERTY_NOT_DEFINED, START_DAY, END_DAY);
        }
        if (startDay == null && endDay != null) {
            if (debug.errorEnabled()) {
                debug.error("SimpleTimeCondition.validateProperties(): property pair not defined, " + END_DAY + ", "
                        + START_DAY);
            }
            throw new EntitlementException(PAIR_PROPERTY_NOT_DEFINED, END_DAY, START_DAY);
        }

        if (startDate != null && endDate == null) {
            if (debug.errorEnabled()) {
                debug.error("SimpleTimeCondition.validateProperties(): property pair not defined, " + START_DATE + ", "
                        + END_DATE);
            }
            throw new EntitlementException(PAIR_PROPERTY_NOT_DEFINED, START_DATE, END_DATE);
        }
        if (startDate == null && endDate != null) {
            if (debug.errorEnabled()) {
                debug.error("SimpleTimeCondition.validateProperties(): property pair not defined, " + END_DATE + ", "
                        + START_DATE);
            }
            throw new EntitlementException(PAIR_PROPERTY_NOT_DEFINED, END_DATE, START_DATE);
        }

        if (startDate != null) {
            if (startDateCal == null || endDateCal == null) {
                if (debug.errorEnabled()) {
                    debug.error("SimpleTimeCondition.validateProperties(): property pair not defined: " +
                            START_DATE + ", "  + END_DATE + " - cannot generate calendar.");
                }
                throw new EntitlementException(PAIR_PROPERTY_NOT_DEFINED, END_DATE, START_DATE);
            } else {
                if (startDateCal.getTime().getTime() > endDateCal.getTime().getTime()) {
                    if (debug.errorEnabled()) {
                        debug.error("SimpleTimeCondition.validateProperties(): START DATE after END DATE");
                    }
                    throw new EntitlementException(START_DATE_AFTER_END_DATE,
                            startDateCal.getTime(), endDateCal.getTime());
                }
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConditionDecision evaluate(String realm, Subject subject, String resourceName, Map<String, Set<String>> env)
            throws EntitlementException {

        boolean allowed = false;
        long currentGmt = timeService.now();
        String currentGmtString = getValue(env.get(REQUEST_TIME));
        if (currentGmtString != null) {
            currentGmt = Long.parseLong(currentGmtString);
        }

        TimeZone timeZone = enforcementTZ;
        if (timeZone == null) {
            String timeZoneString = getValue(env.get(REQUEST_TIME_ZONE));
            if (timeZoneString != null) {
                timeZone = TimeZone.getTimeZone(timeZoneString);
            } else {
                timeZone = TimeZone.getDefault();
            }
        }

        Pair<Long, Long> effectiveRange = getEffectiveRange(currentGmt, timeZone);
        if (debug.messageEnabled()) {
            debug.message("At SimpleTimeCondition.getConditionDecision(): effectiveRange = "
                    + new Date(effectiveRange.getFirst()) + "," + new Date(effectiveRange.getSecond()));
        }
        long timeToLive = Long.MAX_VALUE;
        if (currentGmt >= effectiveRange.getFirst() && currentGmt <= effectiveRange.getSecond()) {
            allowed = true;
            timeToLive = effectiveRange.getSecond();
        } else if (currentGmt < effectiveRange.getFirst()) {
            timeToLive = effectiveRange.getFirst();
        }
        return new ConditionDecision(allowed, Collections.<String, Set<String>>emptyMap(), timeToLive);
    }

    /**
     * @see PolicyRequestHandler#convertEnvParams
     */
    @SuppressWarnings("unchecked")
    private String getValue(Object value) {

        if (value instanceof String) {
            return (String) value; //REQUEST_TIME_ZONE
        } else if (value instanceof Set) { //general case
            return ((Set<String>) value).iterator().next();
        } else if (value instanceof Long) { //REQUEST_TIME
            return String.valueOf(value);
        }

        return null;
    }

    /**
     * Using the start and end times and dates get the effective start and end date in the {@code timeZone} specified
     * using the current time in GMT {@code currentGmt}.
     *
     * @param currentGmt The current GMT.
     * @param timeZone The time zone.
     * @return Pair of {@code Long}s. First entry being the start of range and the second the end of range.
     */
    private Pair<Long, Long> getEffectiveRange(long currentGmt, TimeZone timeZone) {

        Calendar calendar = new GregorianCalendar(timeZone);
        calendar.setTime(new Date(currentGmt));

        long timeStart = Long.MIN_VALUE;
        if (startHour != -1) {
            calendar.set(Calendar.HOUR_OF_DAY, startHour);
            calendar.set(Calendar.MINUTE, startMinute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            timeStart = calendar.getTime().getTime();
        }

        long timeEnd = Long.MAX_VALUE;
        if (endHour != -1) {
            calendar.set(Calendar.HOUR_OF_DAY, endHour);
            calendar.set(Calendar.MINUTE, endMinute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            timeEnd = calendar.getTime().getTime();
        }
        if (timeEnd < timeStart) {
            Calendar cal = new GregorianCalendar(timeZone);
            if (currentGmt < timeStart) {
                cal.setTime(new Date(timeStart));
                cal.roll(Calendar.DAY_OF_YEAR, false);
                timeStart = cal.getTime().getTime();
            } else {
                cal.setTime(new Date(timeEnd));
                cal.roll(Calendar.DAY_OF_YEAR, true);
                timeEnd = cal.getTime().getTime();
            }
        }

        long dayStart = Long.MIN_VALUE;
        /* need to set the date again on the calendar object
        Reason was discovered to be the fact that when HOUR_OF_DAY etc
        are set on the calendar the WEEK_OF_MONTH, DAY_OF_THE_WEEK
        gets initialized to "?" , and once that happens the below
        calendar.set(Calendar.DAY_OF_WEEK, startDay) does not seem
        to set the DAY_OF_WEEK, since the fields which it needs to set
        on the basis of like WEEK_OF_MONTH, WEEK_OF_YEAR are lost  too.
          Setting the date again reinitializes all the necessary fields
        to be able to set "DAY_OF_WEEK" correctly below.
        */

        calendar.setTime(new Date(currentGmt));
        if (startDayInt != -1) {
            calendar.set(Calendar.DAY_OF_WEEK, startDayInt);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            dayStart = calendar.getTime().getTime();
        }

        long dayEnd = Long.MAX_VALUE;
        if (endDayInt != -1) {
            calendar.set(Calendar.DAY_OF_WEEK, endDayInt);
            calendar.set(Calendar.HOUR_OF_DAY, 24);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            dayEnd = calendar.getTime().getTime();
        }
        if (dayEnd <= dayStart) {
            Calendar cal = new GregorianCalendar(timeZone);
            if (currentGmt < dayStart) {
                cal.setTime(new Date(dayStart));
                cal.roll(Calendar.WEEK_OF_YEAR, false);
                dayStart = cal.getTime().getTime();
            } else {
                cal.setTime(new Date(dayEnd));
                cal.roll(Calendar.WEEK_OF_YEAR, true);
                dayEnd = cal.getTime().getTime();
                if (dayEnd <= dayStart) {
                    // week is rolling over year, see issue 4326
                    int ye = cal.get(Calendar.YEAR);
                    cal.set(Calendar.YEAR, ye + 1);
                    cal.set(Calendar.DAY_OF_WEEK, endDayInt);
                    cal.set(Calendar.HOUR_OF_DAY, 24);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    dayEnd = cal.getTime().getTime();
                }
            }
        }

        long dateStart = Long.MIN_VALUE;
        if (startDateCal != null) {
            calendar.set(Calendar.YEAR, startDateCal.get(Calendar.YEAR));
            calendar.set(Calendar.MONTH, startDateCal.get(Calendar.MONTH));
            calendar.set(Calendar.DAY_OF_MONTH, startDateCal.get(Calendar.DAY_OF_MONTH));
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            dateStart = calendar.getTime().getTime();
        }

        long dateEnd = Long.MAX_VALUE;
        if (endDateCal != null) {
            calendar.set(Calendar.YEAR, endDateCal.get(Calendar.YEAR));
            calendar.set(Calendar.MONTH, endDateCal.get(Calendar.MONTH));
            calendar.set(Calendar.DAY_OF_MONTH, endDateCal.get(Calendar.DAY_OF_MONTH));
            calendar.set(Calendar.HOUR_OF_DAY, 24);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            dateEnd = calendar.getTime().getTime();
        }

        long rangeStart = Long.MIN_VALUE;
        if (timeStart > rangeStart) {
            rangeStart = timeStart;
        }
        if (dayStart > rangeStart) {
            rangeStart = dayStart;
        }
        if (dateStart > rangeStart) {
            rangeStart = dateStart;
        }

        long rangeEnd = Long.MAX_VALUE;
        if (timeEnd < rangeEnd) {
            rangeEnd = timeEnd;
        }
        if (dayEnd < rangeEnd)  {
            rangeEnd = dayEnd;
        }
        if (dateEnd < rangeEnd) {
            rangeEnd = dateEnd;
        }

        return Pair.of(rangeStart, rangeEnd);
    }

    private JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        jo.put(START_TIME, startTime);
        jo.put(END_TIME, endTime);
        jo.put(START_DAY, startDay);
        jo.put(END_DAY, endDay);
        jo.put(START_DATE, startDate);
        jo.put(END_DATE, endDate);
        jo.put(ENFORCEMENT_TIME_ZONE, enforcementTimeZone);
        return jo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String s = null;
        try {
            s = toJSONObject().toString(2);
        } catch (JSONException e) {
            PrivilegeManager.debug.error("SimpleTimeCondition.toString()", e);
        }
        return s;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        int startTimeInt = parseTimeString(startTime);
        startHour = startTimeInt / 60;
        startMinute = startTimeInt - startHour * 60;
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        int endTimeInt = parseTimeString(endTime);
        endHour = endTimeInt / 60;
        endMinute = endTimeInt - endHour * 60;
        this.endTime = endTime;
    }

    public String getStartDay() {
        return startDay;
    }

    public void setStartDay(String startDay) {
        startDayInt = parseDayString(startDay);
        this.startDay = startDay;
    }

    public String getEndDay() {
        return endDay;
    }

    public void setEndDay(String endDay) {
        endDayInt = parseDayString(endDay);
        this.endDay = endDay;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        try {
            Date date = dateFormat.parse(startDate);
            startDateCal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            startDateCal.setTime(date);
            this.startDate = startDate;
        } catch (ParseException e) {
            debug.error("SimpleTimeCondition.setStartDate()", e);
        }
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        try {
            Date date = dateFormat.parse(endDate);
            endDateCal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            endDateCal.setTime(date);
            this.endDate = endDate;
        } catch (ParseException e) {
            debug.error("SimpleTimeCondition.setEndDate()", e);
        }
    }

    public String getEnforcementTimeZone() {
        return enforcementTimeZone;
    }

    public void setEnforcementTimeZone(String enforcementTimeZone) {
        this.enforcementTZ = TimeZone.getTimeZone(enforcementTimeZone);
        this.enforcementTimeZone = enforcementTimeZone;
    }
}
