/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: TimeCondition.java,v 1.4 2010/01/05 22:00:26 dillidorai Exp $
 */
package com.sun.identity.entitlement;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import javax.security.auth.Subject;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * EntitlementCondition to represent time based constraint
 * @author dorai
 */
public class TimeCondition extends EntitlementConditionAdaptor {
    /**
     * Key that is used to define current time that is passed in the
     *  <code>env</code> parameter while invoking <code>getConditionDecision
     *  </code> method of the <code>SimpleTimeCondition</code>. Value for the
     *  key should be a <code>Long</code> object whose value is time in
     *  milliseconds since epoch. If no value is given for this, it is assumed
     *  to be the current system time
     *
     *  @see com.sun.identity.policy.interfaces.Condition#getConditionDecision(com.iplanet.sso.SSOToken, java.util.Map)
     *  @see com.sun.identity.policy.plugins.SimpleTimeCondition#ENFORCEMENT_TIME_ZONE
     */
    public static final String REQUEST_TIME = "requestTime";

    /**
     * Key that is used to define the time zone that is passed in
     *  the <code>env</code> parameter while invoking
     *  <code>getConditionDecision</code> method of a
     *  <code>SimpleTimeCondition</code>
     *  Value for the key should be a <code>TimeZone</code> object. This
     *  would be used only if the <code>ENFORCEMENT_TIME_ZONE</code> is not
     *  defined for the <code>SimpleTimeCondition</code>
     *
     *  @see com.sun.identity.policy.interfaces.Condition#getConditionDecision(com.iplanet.sso.SSOToken, java.util.Map)
     *  @see com.sun.identity.policy.plugins.SimpleTimeCondition#ENFORCEMENT_TIME_ZONE
     *  @see java.util.TimeZone
     */
    public static final String REQUEST_TIME_ZONE = "requestTimeZone";
    
    private static final String DATE_FORMAT = "yyyy:MM:dd";
    private static final String[] DAYS_OF_WEEK = { "", "sun", "mon", "tue",
        "wed", "thu", "fri", "sat"};

    private String startTime;
    private String endTime;
    private String startDay;
    private String endDay;
    private String startDate;
    private String endDate;
    private String enforcementTimeZone;
    private String pConditionName;

    /**
     * Constructs an TimeCondition
     */
    public TimeCondition() {
    }

    /**
     * Constructs IPCondition object:w
     *
     * @param startTime
     * @param endTime
     * @param startDay
     * @param endDay
     */
    public TimeCondition(String startTime, String endTime,
            String startDay, String endDay) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.startDay = startDay;
        this.endDay = endDay;
    }

    /**
     * Returns state of the object
     * @return state of the object encoded as string
     */
    public String getState() {
        return toString();
    }

    /**
     * Sets state of the object
     * @param state State of the object encoded as string
     */
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);
            startTime = jo.optString("startTime");
            endTime = (jo.has("endTime")) ? jo.optString("endTime") : null;
            startDay = jo.optString("startDay");
            endDay = jo.optString("endDay");
            startDate = jo.optString("startDate");
            endDate = (jo.has("endDate")) ? jo.optString("endDate") : null;
            pConditionName = jo.optString("pConditionName");
            enforcementTimeZone = jo.optString("enforcementTimeZone");
        } catch (JSONException joe) {
        }
    }

    /**
     * Returns <code>ConditionDecision</code> of
     * <code>EntitlementCondition</code> evaluation
     *
     * @param realm Realm name.
     * @param subject EntitlementCondition who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return <code>ConditionDecision</code> of
     * <code>EntitlementCondition</code> evaluation
     * @throws com.sun.identity.entitlement,  EntitlementException in case
     * of any error
     */
    public ConditionDecision evaluate(
        String realm,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment)
        throws EntitlementException {
        boolean allowed = false;
        long currentGmt = System.currentTimeMillis();


        TimeZone timeZone = TimeZone.getDefault();
        if (environment != null) {
            String time = getProperty(environment, REQUEST_TIME);
            if (time != null) {
                try {
                    currentGmt = Long.parseLong(time);
                } catch (NumberFormatException e) {
                    //ignore
                }
            }

            String tZone = getProperty(environment, REQUEST_TIME_ZONE);
            if (tZone != null) {
                timeZone = TimeZone.getTimeZone(enforcementTimeZone);
            }
        }

        long[] effectiveRange = getEffectiveRange(currentGmt, timeZone);
        Map<String, Set<String>> advices = new HashMap<String, Set<String>>();

        if ((currentGmt >= effectiveRange[0]) &&
            (currentGmt <= effectiveRange[1])
        ) {
            allowed = true;
            long timeToLive = effectiveRange[1] - currentGmt;
            Set<String> setDuration = new HashSet<String>(2);
            Set<String> setMaxTime = new HashSet<String>(2);
            setDuration.add(Long.toString(timeToLive));
            setMaxTime.add(Long.toString(effectiveRange[1]));
            advices.put(ConditionDecision.TIME_TO_LIVE, setDuration);
            advices.put(ConditionDecision.MAX_TIME, setMaxTime);
        } else if (currentGmt < effectiveRange[0]) {
            long timeToLive = effectiveRange[0] - currentGmt;
            Set<String> setDuration = new HashSet<String>(2);
            Set<String> setMaxTime = new HashSet<String>(2);
            setDuration.add(Long.toString(timeToLive));
            setMaxTime.add(Long.toString(effectiveRange[0]));
            advices.put(ConditionDecision.TIME_TO_LIVE, setDuration);
            advices.put(ConditionDecision.MAX_TIME, setMaxTime);
        }

        return new ConditionDecision(allowed, advices);
    }

    private static String getProperty(
        Map<String, Set<String>> environment,
        String name
    ) {
        Set<String> set = environment.get(name);
        return ((set != null) && !set.isEmpty()) ? set.iterator().next() :null;
    }

    /**
     * @return the startTime
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the endTime
     */
    public String getEndTime() {
        return endTime;
    }

    /**
     * @param endTime the endTime to set
     */
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    /**
     * @return the startDay
     */
    public String getStartDay() {
        return startDay;
    }

    /**
     * @param startDay the startDay to set
     */
    public void setStartDay(String startDay) {
        this.startDay = startDay;
    }

    /**
     * @return the endDay
     */
    public String getEndDay() {
        return endDay;
    }

    /**
     * @param endDay the endDay to set
     */
    public void setEndDay(String endDay) {
        this.endDay = endDay;
    }

    /**
     * @return the startDate
     */
    public String getStartDate() {
        return startDate;
    }

    /**
     * @param startDate the startDate to set
     */
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    /**
     * @return the endDate
     */
    public String getEndDate() {
        return endDate;
    }

    /**
     * @param endDate the endDate to set
     */
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    /**
     * @return the enforcementTimeZone
     */
    public String getEnforcementTimeZone() {
        return enforcementTimeZone;
    }

    /**
     * @param enforcementTimeZone the enforcementTimeZone to set
     */
    public void setEnforcementTimeZone(String enforcementTimeZone) {
        this.enforcementTimeZone = enforcementTimeZone;
    }

    /**
     * Returns OpenSSO policy subject name of the object
     * @return subject name as used in OpenSSO policy,
     * this is releavant only when UserECondition was created from
     * OpenSSO policy Condition
     */
    public String getPConditionName() {
        return pConditionName;
    }

    /**
     * Sets OpenSSO policy subject name of the object
     * @param pConditionName subject name as used in OpenSSO policy,
     * this is releavant only when UserECondition was created from
     * OpenSSO policy Condition
     */
    public void setPConditionName(String pConditionName) {
        this.pConditionName = pConditionName;
    }

    /**
     * Returns JSONObject mapping of the object
     * @return JSONObject mapping  of the object
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        jo.put("startTime", startTime);

        if (endTime != null) {
            jo.put("endTime", endTime);
        }

        jo.put("startDay", startDay);
        jo.put("endDay", endDay);
        jo.put("startDate", startDate);

        if (endDate != null) {
            jo.put("endDate", endDate);
        }
        jo.put("enforcementTimeZone", enforcementTimeZone);
        jo.put("pConditionName", pConditionName);
        return jo;
    }

    /**
     * Returns <code>true</code> if the passed in object is equal to this object
     * @param obj object to check for equality
     * @return  <code>true</code> if the passed in object is equal to this object
     */
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
        TimeCondition object = (TimeCondition) obj;
        if (startDay == null) {
            if (object.getStartDay() != null) {
                return false;
            }
        } else {
            if (!startDay.equals(object.getStartDay())) {
                return false;
            }
        }
        if (getEndDay() == null) {
            if (object.getEndDay() != null) {
                return false;
            }
        } else {
            if (!endDay.equals(object.getEndDay())) {
                return false;
            }
        }
        if (startDate == null) {
            if (object.getStartDate() != null) {
                return false;
            }
        } else {
            if (!startDate.equals(object.getStartDate())) {
                return false;
            }
        }
        if (getEndDate() == null) {
            if (object.getEndDate() != null) {
                return false;
            }
        } else {
            if (!endDate.equals(object.getEndDate())) {
                return false;
            }
        }

        if (getEnforcementTimeZone() == null) {
            if (object.getEnforcementTimeZone() != null) {
                return false;
            }
        } else {
            if (!enforcementTimeZone.equals(object.getEnforcementTimeZone())) {
                return false;
            }
        }

        if (getPConditionName() == null) {
            if (object.getPConditionName() != null) {
                return false;
            }
        } else {
            if (!pConditionName.equals(object.getPConditionName())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns hash code of the object
     * @return hash code of the object
     */
    @Override
    public int hashCode() {
        int code = super.hashCode();
        if (startTime != null) {
            code += startTime.hashCode();
        }
        if (startDate != null) {
            code += startDate.hashCode();
        }
        if (startDay != null) {
            code += startDay.hashCode();
        }
        if (endTime != null) {
            code += endTime.hashCode();
        }
        if (endDate != null) {
            code += endDate.hashCode();
        }
        if (endDay != null) {
            code += endDay.hashCode();
        }
        if (enforcementTimeZone != null) {
            code += enforcementTimeZone.hashCode();
        }
        if (pConditionName != null) {
            code += pConditionName.hashCode();
        }
        return code;
    }

    /**
     * Returns string representation of the object
     * @return string representation of the object
     */
    @Override
    public String toString() {
        String s = null;
        try {
            s = toJSONObject().toString(2);
        } catch (JSONException e) {
            PrivilegeManager.debug.error("TimeCondition.toString()", e);
        }
        return s;
    }

    private long getTime(Calendar calendar, String time, long defaultVal)
        throws EntitlementException {
        if ((time == null) || (time.length() == 0)) {
            return defaultVal;
        }

        int t = parseTimeString(time);
        if (t == -1) {
            return defaultVal;
        }

        int hours = t / 60;
        int minutes = t - hours * 60;
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime().getTime();
    }

    /**
     * Using the start and end times and dates get the effective
     * start and end date in the <code>timeZone</code> specified
     * using the current time in GMT ( <code>currentGmt</code>).
     * @return long[] Array of <code>long</code> of size 2 where
     * end range.
     */
    private long[] getEffectiveRange(long currentGmt, TimeZone timeZone)
        throws EntitlementException {
        long[] effectiveRange = new long[2];

        Calendar calendar = new GregorianCalendar(timeZone);
        calendar.setTime( new Date(currentGmt) );

        long timeStart = getTime(calendar, startTime, Long.MIN_VALUE);
        long timeEnd = getTime(calendar, endTime, Long.MAX_VALUE);

        if( timeEnd < timeStart) {
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

        calendar.setTime( new Date(currentGmt) );
        int startD = parseDayString(startDay);
        if (startD != -1) {
            calendar.set(Calendar.DAY_OF_WEEK, startD);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            dayStart = calendar.getTime().getTime();
        }

        long dayEnd = Long.MAX_VALUE;
        int endD = parseDayString(endDay);
        if (endD != -1) {
            calendar.set(Calendar.DAY_OF_WEEK, endD);
            calendar.set(Calendar.HOUR_OF_DAY, 24);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            dayEnd = calendar.getTime().getTime();
        }
        if( dayEnd <= dayStart) {
            Calendar cal = new GregorianCalendar(timeZone);
            if ( currentGmt < dayStart) {
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
                    cal.set(Calendar.DAY_OF_WEEK, endD);
                    cal.set(Calendar.HOUR_OF_DAY, 24);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    dayEnd = cal.getTime().getTime();
                }
            }
        }

        long dateStart = Long.MIN_VALUE;
        DateArray dateArray = validateDates(startDate, endDate);

        if (dateArray.startDateArray[0] != -1) {
            calendar.set(Calendar.YEAR, dateArray.startDateArray[0]);
            calendar.set(Calendar.MONTH, dateArray.startDateArray[1]);
            calendar.set(Calendar.DAY_OF_MONTH, dateArray.startDateArray[2]);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            dateStart = calendar.getTime().getTime();
        }

        long dateEnd = Long.MAX_VALUE;
        if (dateArray.endDateArray[0] != -1) {
            calendar.set(Calendar.YEAR, dateArray.endDateArray[0]);
            calendar.set(Calendar.MONTH, dateArray.endDateArray[1]);
            calendar.set(Calendar.DAY_OF_MONTH, dateArray.endDateArray[2]);
            calendar.set(Calendar.HOUR_OF_DAY, 24);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            dateEnd = calendar.getTime().getTime();
        }

        long rangeStart = Long.MIN_VALUE;
        if ( timeStart > rangeStart ) {
            rangeStart = timeStart;
        }
        if ( dayStart > rangeStart ) {
            rangeStart = dayStart;
        }
        if ( dateStart > rangeStart ) {
            rangeStart = dateStart;
        }

        long rangeEnd = Long.MAX_VALUE;
        if ( timeEnd < rangeEnd ) {
            rangeEnd = timeEnd;
        }
        if ( dayEnd < rangeEnd )  {
            rangeEnd = dayEnd;
        }
        if ( dateEnd < rangeEnd ) {
            rangeEnd = dateEnd;
        }

        effectiveRange[0] = rangeStart;
        effectiveRange[1] = rangeEnd;

        return effectiveRange;
    }

    /**
     * Converts time in <code>String</code> format to an int.
     * @exception PolicyException for invalid data.
     */
    private static int parseTimeString(String timeString)
        throws EntitlementException {
        if ((timeString == null) || (timeString.length() == 0)) {
            return -1;
        }
        StringTokenizer st = new StringTokenizer(timeString, ":");
        if ( st.countTokens() != 2 ) {
            String[] args = { "time", timeString };
            throw new EntitlementException(400, args);
        }
        String token1 = st.nextToken();
        String token2 = st.nextToken();
        int hour = -1;
        int minute = -1;
        try {
            hour = Integer.parseInt(token1);
            minute = Integer.parseInt(token2);
        } catch ( Exception e) {
            String[] args = { "time", timeString };
            throw new EntitlementException(400, args);
        }
        if ( (hour < 0) || (hour > 24) || (minute < 0) || (minute > 59) ) {
            String[] args = { "time", timeString };
            throw new EntitlementException(400, args);
        }
        return hour * 60 + minute;
    }

    /**
     * Converts day in <code>String</code> format to an int.
     * based on the <code>DAYS_OF_WEEK</code>
     * @see #DAYS_OF_WEEK
     */
    private static int parseDayString(String dayString) {
        int day = -1;
        if ((dayString != null) && (dayString.length() > 0)) {
            String dayStringLc = dayString.toLowerCase();
            for (int i = 1; i < 8; i++) {
                if (DAYS_OF_WEEK[i].equals(dayStringLc)) {
                    day = i;
                    break;
                }
            }
        }
        return day;
    }

    /**
     * extracts <code>startDate</code> and <code>endDate</code> from the
     * respective <code>Set</code>, throws Exception for invalid data
     * like startDate > endDate or format.
     * @see #START_DATE
     * @see #END_DATE
     */
    private DateArray validateDates(String startDate, String endDate)
        throws EntitlementException {
        DateArray dateArray = new DateArray();

        if ((startDate == null) || (startDate.length() == 0)) {
            return dateArray;
        }

        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        df.setLenient(false);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));

        Date dateStart = getDate(df, startDate);
        Date dateEnd = ((endDate == null) || (endDate.length() == 0)) ? null :
            getDate(df, endDate);

        if (dateEnd != null) {
            if (dateStart.getTime() > dateEnd.getTime()) {
                String[] args = {startDate, endDate};
                throw new EntitlementException(402, args);
            }
        }

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        
        cal.setTime(dateStart);
        dateArray.startDateArray[0] = cal.get(Calendar.YEAR);
        dateArray.startDateArray[1] = cal.get(Calendar.MONTH);
        dateArray.startDateArray[2] = cal.get(Calendar.DAY_OF_MONTH);

        if (dateEnd != null) {
            cal.setTime(dateEnd);
            dateArray.endDateArray[0] = cal.get(Calendar.YEAR);
            dateArray.endDateArray[1] = cal.get(Calendar.MONTH);
            dateArray.endDateArray[2] = cal.get(Calendar.DAY_OF_MONTH);
        }
        
        return dateArray;
    }

    private Date getDate(DateFormat df, String date)
        throws EntitlementException {
        try {
            return df.parse(date);
        } catch (Exception e) {
            String[] args = {startDate};
            throw new EntitlementException(401, args, e);
        }
    }

    class DateArray {
        int startDateArray[] = {-1,0,0};
        int endDateArray[] = {-1,0,0};
    }
}
