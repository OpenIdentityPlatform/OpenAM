/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SimpleTimeCondition.java,v 1.5 2010/01/05 22:00:26 dillidorai Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.plugins;

import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.ResBundleUtils;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;

import java.util.*;
import java.text.*;

/**
 * The class <code>SimpleTimeCondition</code>  is a plugin implementation
 * of <code>Condition</code>. This lets you define the time range
 * of week days and/or date range during which a policy applies. 
 *
 */
public class SimpleTimeCondition implements Condition {

    /** Key that is used to define current time that is passed in the
     *  <code>env</code> parameter while invoking <code>getConditionDecision
     *  </code> method of the <code>SimpleTimeCondition</code>. Value for the
     *  key should be a <code>Long</code> object whose value is time in
     *  milliseconds since epoch. If no value is given for this, it is assumed
     *  to be the current system time
     *
     *	@see #getConditionDecision(SSOToken, Map)
     *	@see #ENFORCEMENT_TIME_ZONE
     */
    public static final String REQUEST_TIME = "requestTime";
    
    private static final Debug DEBUG 
        = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    private static final String DATE_FORMAT = "yyyy:MM:dd";

    private static final String[] DAYS_OF_WEEK = { "", "sun", "mon", "tue", 
        "wed", "thu", "fri", "sat"};

    private Map properties;
    private int startTime = -1;
    private int startHour = -1;
    private int startMinute;
    private int endTime = -1;
    private int endHour = -1;
    private int endMinute;
    private int startDay = -1;
    private int endDay = -1;
    private int startDate[] = {-1,0,0};
    private int endDate[] = {-1,0,0};
    private TimeZone enforcementTimeZone;

    private static List propertyNames = new ArrayList(7);

    static {
        propertyNames.add(START_TIME);
        propertyNames.add(END_TIME);
        propertyNames.add(START_DAY);
        propertyNames.add(END_DAY);
        propertyNames.add(START_DATE);
        propertyNames.add(END_DATE);
        propertyNames.add(ENFORCEMENT_TIME_ZONE);
    }
    
    /** No argument constructor
     */
    public SimpleTimeCondition() {
    }

    /**
     * Returns a list of property names for the condition.
     *
     * @return list of property names
     */
    public List getPropertyNames() {
        return (new ArrayList(propertyNames));
    }

    /**
     * Returns the syntax for a property name
     * @see com.sun.identity.policy.Syntax
     *
     * @param property property name
     *
     * @return <code>Syntax<code> for the property name
     */
    public Syntax getPropertySyntax(String property) {
        return Syntax.NONE;
    }

    /**
     * Gets the display name for the property name.
     * The <code>locale</code> variable could be used by the plugin to
     * customize the display name for the given locale.
     * The <code>locale</code> variable could be <code>null</code>, in which
     * case the plugin must use the default locale.
     *
     * @param property property name
     * @param locale locale for which the property name must be customized
     * @return display name for the property name
     * @throws PolicyException is unable to gte the display name.
     */
    public String getDisplayName(String property, Locale locale)
       throws PolicyException {
        return "";
    }

    /**
     * Returns a set of valid values given the property name. This method
     * is called if the property Syntax is either the SINGLE_CHOICE or
     * MULTIPLE_CHOICE.
     *
     * @param property property name
     * @return Set of valid values for the property.
     * @exception PolicyException if unable to get the Syntax.
     */
    public Set getValidValues(String property) throws PolicyException {
        return Collections.EMPTY_SET;
    }

    /** Sets the properties of the condition.
     *  Evaluation of <code>ConditionDecision</code> is influenced by these
     *  properties.
     * @param properties the properties of the condition that governs
     *        whether a policy applies. This plugin uses the property values
     *        defined for keys <code>START_TIME</code>, <code>END_TIME</code>,
     *        <code>START_DAY</code>, <code>END_DAY</code>,
     *        <code>START_DATE</code>, <code>END_DATE</code>. The properties
     *        should have at least one of the keys <code>START_TIME</code>,
     *        <code>START_DAY</code> or <code>START_DATE</code>.
     *        The values of the keys should be Set where each element is a
     *        String that conforms to the format dictated by
     *        <code>START_TIME</code>, <code>START_DAY</code> and
     *        <code>START_DATE</code>. Each of the above key, if present,
     *        should be accompanied by the key <code>END_TIME</code>,
     *        <code>END_DAY</code>, <code>END_DATE</code> respectively.
     *
     * @throws PolicyException if properties is null or does not contain
     *        keys as described above or does not follow the required pattern
     *
     * @see #START_TIME
     * @see #END_TIME
     * @see #START_DAY
     * @see #END_DAY
     * @see #START_DATE
     * @see #END_DATE
     * @see #getConditionDecision(SSOToken, Map)
     */
    public void setProperties(Map properties) throws PolicyException {
        this.properties = properties;
        startTime = -1;
        startHour = -1;
        endTime = -1;
        endHour = -1;
        startDay = -1;
        endDay = -1;
        startDate[0]= -1;
        endDate[0] = -1;
        validateProperties();
    }
    
    /** Gets the properties of the condition.
     * @return unmodifiable map view of properties that govern the 
     *          evaluation of  the condition.
     *          Please note that properties is  not cloned before returning
     * @see #setProperties(Map)
     */
    public Map getProperties() {
        return (properties == null)
                ? null : Collections.unmodifiableMap(properties);
    }
    
    /**
     * This method validates the properties set using the <code>setProperties
     * </code> method. It checks for the presence of the required keys.
     * It Validates the key values to conform to the expected pattern and
     * also makes sure no other invalid key is being set.
     * @see #START_TIME
     * @see #END_TIME
     * @see #START_DAY
     * @see #END_DAY
     * @see #START_DATE
     * @see #END_DATE
     * @see #ENFORCEMENT_TIME_ZONE
     * @see #DAYS_OF_WEEK
     * @see #setProperties(Map)
     */ 
    private boolean validateProperties() throws PolicyException {
        if ( (properties == null) || ( properties.keySet() == null) ) {
            throw new PolicyException(ResBundleUtils.rbName,
                    "properties_not_initialized", null, null);
        }

        Set keySet = properties.keySet();
        //Check if the required key(s) are defined
        if ( !keySet.contains(START_TIME) && !keySet.contains(START_DAY)
            && !keySet.contains(START_DATE) && !keySet.contains(DAYS_OF_WEEK) ) 
        {
            String[] args = { START_DATE + "," + START_TIME + "," + START_DAY };
            throw new PolicyException(ResBundleUtils.rbName,
                    "at_least_one_of_the_properties_should_be_defined", args,
                    null);
        }

        //Check if all the keys are valid 
        Iterator keys = keySet.iterator();
        while ( keys.hasNext()) {
            String key = (String) keys.next();
            if ( !START_TIME.equals(key) && !START_DAY.equals(key) &&
                    !START_DATE.equals(key) && !END_TIME.equals(key) 
                    && !END_DAY.equals(key) && !END_DATE.equals(key) 
                    && !DAYS_OF_WEEK.equals(key) 
                    && !ENFORCEMENT_TIME_ZONE.equals(key) ) {
                String args[] = { key };
                throw new PolicyException(ResBundleUtils.rbName,
                        "attempt_to_set_invalid_property", args, null);
            }
        }

        //validate enforcementTimeZone
        Set enforcementTimeZoneSet = (Set) properties.get(
            ENFORCEMENT_TIME_ZONE);
        if ( (enforcementTimeZoneSet != null) &&
                    (!enforcementTimeZoneSet.isEmpty())  ) {
            String enforcementTimeZoneString = 
                    (String) enforcementTimeZoneSet.iterator().next();
            enforcementTimeZone =
                    TimeZone.getTimeZone(enforcementTimeZoneString);
        }

        //validate times
        Set startTimeSet = (Set) properties.get(START_TIME);
        Set endTimeSet = (Set) properties.get(END_TIME);
        if (  (startTimeSet != null) && (endTimeSet == null) ) {
            String[] args = { START_TIME, END_TIME };
            throw new PolicyException(ResBundleUtils.rbName,
                     "pair_property_not_defined", args, null);
        }
        if ( (startTimeSet == null) && (endTimeSet != null) ) {
            String[] args = { END_TIME, START_TIME };
            throw new PolicyException(ResBundleUtils.rbName,
                     "pair_property_not_defined", args, null);
        }
        if ( startTimeSet != null ) {
            validateTimes(startTimeSet, endTimeSet);
        }

        //validate days
        Set startDaySet = (Set) properties.get(START_DAY);
        Set endDaySet = (Set) properties.get(END_DAY);
        if ( (startDaySet != null) && (endDaySet == null) ) {
            String[] args = { START_DAY, END_DAY };
            throw new PolicyException(ResBundleUtils.rbName,
                     "pair_property_not_defined", args, null);
        }
        if ( (startDaySet == null) && (endDaySet != null) ) {
            String[] args = { END_DAY, START_DAY };
            throw new PolicyException(ResBundleUtils.rbName,
                     "pair_property_not_defined", args, null);
        }
        if ( startDaySet != null ) {
            validateDays(startDaySet, endDaySet);
        }

        //validate dates
        Set startDateSet = (Set) properties.get(START_DATE);
        Set endDateSet = (Set) properties.get(END_DATE);
        if ( (startDateSet != null) && (endDateSet == null) ) {
            String[] args = { START_DATE, END_DATE };
            throw new PolicyException(ResBundleUtils.rbName,
                     "pair_property_not_defined", args, null);
        }
        if ( (startDateSet == null) && (endDateSet != null) ) {
            String[] args = { END_DATE, START_DATE };
            throw new PolicyException(ResBundleUtils.rbName,
                     "pair_property_not_defined", args, null);
        }
        if ( startDateSet != null ) {
            validateDates(startDateSet, endDateSet);
        }

        return true;

    }

    /**
     * extracts <code>startDay</code> and <code>endDay</code> from the
     * respective <code>Set</code>, throws Exception for invalid data
     * or format.
     * @see #START_DAY
     * @see #END_DAY
     */

    private boolean validateDays(Set startDaySet, Set endDaySet) 
            throws PolicyException {
        if ( startDaySet.size() != 1 ) {
            String[] args = { START_DAY };
            throw new PolicyException(ResBundleUtils.rbName,
                    "property_does_not_allow_multiple_values", args, null);
        }
        if ( endDaySet.size() != 1 ) {
            String[] args = { END_DAY };
            throw new PolicyException(ResBundleUtils.rbName,
                    "property_does_not_allow_multiple_values", args, null);
        }
        String startDayString = (String) (startDaySet.iterator().next());
        startDay = parseDayString(startDayString);
        String endDayString = (String) (endDaySet.iterator().next());
        endDay = parseDayString(endDayString);
        return true;
    }

    /**
     * extracts <code>startDate</code> and <code>endDate</code> from the
     * respective <code>Set</code>, throws Exception for invalid data
     * like startDate > endDate or format.
     * @see #START_DATE
     * @see #END_DATE
     */
    private boolean validateDates(Set startDateSet, Set endDateSet) 
            throws PolicyException {
        if ( startDateSet.size() != 1 ) {
            String[] args = { START_DATE };
            throw new PolicyException(ResBundleUtils.rbName,
                "property_does_not_allow_multiple_values", args, null);
        }
        if ( endDateSet.size() != 1 ) {
            String[] args = { END_DATE };
            throw new PolicyException(ResBundleUtils.rbName,
                "property_does_not_allow_multiple_values", args, null);
        }
        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        df.setLenient(false);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        String startDateString = (String) (startDateSet.iterator().next());
        String endDateString = (String) (endDateSet.iterator().next());
        Date date1 = null;
        Date date2= null;
        try {
            date1 = df.parse(startDateString);
        } catch (Exception e) {
            String[] args = { START_DATE, startDateString };
            throw new PolicyException(ResBundleUtils.rbName,
                "invalid_property_value", args, e);
        }
        try {
            date2 = df.parse(endDateString);
        } catch (Exception e) {
            String[] args = { END_DATE, endDateString};
            throw new PolicyException(ResBundleUtils.rbName,
                "invalid_property_value", args, e);
        }
        if ( date1.getTime() > date2.getTime() ) {
            throw new PolicyException(ResBundleUtils.rbName,
                "start_date_can_not_be_larger_than_end_date", null, null);
        }
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.setTime(date1);
        startDate[0] = cal.get(Calendar.YEAR);
        startDate[1] = cal.get(Calendar.MONTH);
        startDate[2] = cal.get(Calendar.DAY_OF_MONTH);
        cal.setTime(date2);
        endDate[0] = cal.get(Calendar.YEAR);
        endDate[1] = cal.get(Calendar.MONTH);
        endDate[2] = cal.get(Calendar.DAY_OF_MONTH);
        return true;
    }


    /**
     * extracts <code>startTime</code> <code>startHour</code>, <code>
     * startMinute</code>, <code>endTime</code> <code>endHour</code>,
     * <code>endMinute</code> from the respective <code>Set</code>, 
     * @exception  PolicyException for invalid data.
     * @see #START_TIME
     * @see #END_TIME
     */
    private boolean validateTimes(Set startTimeSet, Set endTimeSet) 
            throws PolicyException {
        if ( startTimeSet.size() != 1 ) {
            String[] args = { START_TIME };
            throw new PolicyException(ResBundleUtils.rbName,
                "property_does_not_allow_multiple_values",
                args, null);
        }
        if ( endTimeSet.size() != 1 ) {
            String[] args = { END_TIME };
            throw new PolicyException(ResBundleUtils.rbName,
                "property_does_not_allow_multiple_values",
                args, null);
        }
        String startTimeString = (String) (startTimeSet.iterator().next());
        startTime = parseTimeString(startTimeString);
        startHour = startTime / 60;
        startMinute = startTime - startHour * 60;
        String endTimeString = (String) (endTimeSet.iterator().next());
        endTime = parseTimeString(endTimeString);
        endHour = endTime / 60;
        endMinute = endTime - endHour * 60;
        return true;
    }

    /**
     * Converts time in <code>String</code> format to an int.
     * @exception PolicyException for invalid data.
     */

    int parseTimeString(String timeString) throws PolicyException {
        StringTokenizer st = new StringTokenizer(timeString, ":");
        if ( st.countTokens() != 2 ) {
            String[] args = { "time", timeString };
            throw new PolicyException(ResBundleUtils.rbName,
                "invalid_property_value", args, null);
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
            throw new PolicyException(ResBundleUtils.rbName,
                "invalid_property_value", args, null);
        }
        if ( (hour < 0) || (hour > 24) || (minute < 0) || (minute > 59) ) {
            String[] args = { "time", timeString };
            throw new PolicyException(ResBundleUtils.rbName,
                "invalid_property_value", args, null);
        }
        return hour * 60 + minute;
    }

    /**
     * Converts day in <code>String</code> format to an int.
     * based on the <code>DAYS_OF_WEEK</code>
     * @exception PolicyException for invalid data.
     * @see #DAYS_OF_WEEK
     */
    int parseDayString(String dayString) throws PolicyException {
        int day = -1;
        String dayStringLc = dayString.toLowerCase();
        for ( int i = 1; i < 8; i++ ) {
            if ( DAYS_OF_WEEK[i].equals(dayStringLc ) ) {
                day = i;
                break;
            }
        }
        if ( day == -1 ) {
            String[] args = { "day", dayString };
            throw new PolicyException(ResBundleUtils.rbName,
                "invalid_property_value", args, null);
        }
        return day;
    }

    /**
     * Returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        SimpleTimeCondition theClone = null;
        try {
            theClone = (SimpleTimeCondition) super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }
        if (startDate != null) {
            theClone.startDate = new int[startDate.length];
            System.arraycopy(startDate, 0, theClone.startDate, 0,
                startDate.length);
        }
        if (endDate != null) {
            theClone.endDate = new int[endDate.length];
            System.arraycopy(endDate, 0, theClone.endDate, 0, endDate.length);
        }
        if (enforcementTimeZone != null) {
            theClone.enforcementTimeZone =
                (TimeZone) enforcementTimeZone.clone();
        }
        if (properties != null) {
            theClone.properties = new HashMap();
            Iterator it = properties.keySet().iterator();
            while (it.hasNext()) {
                Object o = it.next();
                Set values = new HashSet();
                values.addAll((Set) properties.get(o));
                theClone.properties.put(o, values);
            }
        }
        return theClone;
    }

    /**
     * Using the start and end times and dates get the effective 
     * start and end date in the <code>timeZone</code> specified
     * using the current time in GMT ( <code>currentGmt</code>).
     * @return long[] Array of <code>long</code> of size 2 where
     * end range.
     */

    private long[] getEffectiveRange(long currentGmt, TimeZone timeZone) {
        long[] effectiveRange = new long[2];

        Calendar calendar = new GregorianCalendar(timeZone);
        calendar.setTime( new Date(currentGmt) );

        long timeStart = Long.MIN_VALUE;
        if ( startHour != -1 ) {
            calendar.set(Calendar.HOUR_OF_DAY, startHour);
            calendar.set(Calendar.MINUTE, startMinute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            timeStart = calendar.getTime().getTime();
        }

        long timeEnd = Long.MAX_VALUE;
        if ( endHour != -1 ) {
            calendar.set(Calendar.HOUR_OF_DAY, endHour);
            calendar.set(Calendar.MINUTE, endMinute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            timeEnd = calendar.getTime().getTime();
        }
        if( timeEnd < timeStart) {
            Calendar cal = new GregorianCalendar(timeZone);
            if ( currentGmt < timeStart) {
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
        if ( startDay != -1 ) {
            calendar.set(Calendar.DAY_OF_WEEK, startDay);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            dayStart = calendar.getTime().getTime();
        }

        long dayEnd = Long.MAX_VALUE;
        if ( endDay != -1 ) {
            calendar.set(Calendar.DAY_OF_WEEK, endDay);
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
                    cal.set(Calendar.DAY_OF_WEEK, endDay);
                    cal.set(Calendar.HOUR_OF_DAY, 24);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    dayEnd = cal.getTime().getTime();
                }
            }
        }

        long dateStart = Long.MIN_VALUE;
        if ( startDate[0] != -1 ) {
            calendar.set(Calendar.YEAR, startDate[0]);
            calendar.set(Calendar.MONTH, startDate[1]);
            calendar.set(Calendar.DAY_OF_MONTH, startDate[2]);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            dateStart = calendar.getTime().getTime();
        }

        long dateEnd = Long.MAX_VALUE;
        if ( endDate[0] != -1 ) {
            calendar.set(Calendar.YEAR, endDate[0]);
            calendar.set(Calendar.MONTH, endDate[1]);
            calendar.set(Calendar.DAY_OF_MONTH, endDate[2]);
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
     * Gets the decision computed by this condition object, based on the 
     * map of environment parameters 
     * A policy would be evaluated only
     * if the decision of condition is allow. Please note that
     * this only affects whether a policy is evaluated or not.
     * The decision of condition could be an "allow" and
     * the policy could evaluate to "deny". 
     *
     * @param token single sign on token of the user
     *
     * @param env request specific environment map of key/value pairs
     *        <code>SimpleTimeCondition</code> looks for values of keys
     *        <code>REQUEST_TIME</code> and <code>REQUEST_TIME_ZONE</code>
     *        in the <code>env</code> map. If value is not defined
     *        <code>REQUEST_TIME_ZONE</code>, it is assumed to be current system
     *        time. <code>REQUEST_TIME_ZONE</code> is used only if
     *        <code>ENFORCEMENT_TIME_ZONE</code> is not defined in the condition
     *        definition in the policy. If both
     *        <code>ENFORCEMENT_TIME_ZONE</code> and
     *        <code>REQUEST_TIME_ZONE</code> are not defined default system time
     *        zone is used.
     *
     * @return the condition decision. The condition decision encapsulates
     *         whether a policy applies for the request and advice messages
     *         generated by the condition and a time to live indicating
     *         how long this decision is valid
     *
     * @throws PolicyException if the condition has not been initialized
     *         with a successful call to <code>setProperties(Map)</code> and/or
     *         the value of keys <code>REQUEST_TIME and/or
     *         <code>REQUEST_TIME_ZONE</code> are invalid.
     * @throws SSOException if the token is invalid
     * @see #setProperties(Map)
     * @see #REQUEST_TIME
     * @see #REQUEST_TIME_ZONE
     * @see #ENFORCEMENT_TIME_ZONE
     * @see com.sun.identity.policy.ConditionDecision
     */
    public ConditionDecision getConditionDecision(SSOToken token, Map env) 
            throws PolicyException, SSOException {
        boolean allowed = false; 
        long currentGmt = System.currentTimeMillis();
        if (env != null) {
            Long currentGmtLong = (Long) env.get(REQUEST_TIME);
            if ( currentGmtLong != null ) {
                currentGmt = currentGmtLong.longValue();
            } 
        }

        TimeZone timeZone = enforcementTimeZone;
        if ( (timeZone == null) && (env != null) ) {
            timeZone = (TimeZone) env.get(REQUEST_TIME_ZONE);
            if ( timeZone == null ) {
                timeZone = TimeZone.getDefault();
            }
        }
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }

        long[] effectiveRange = getEffectiveRange(currentGmt, timeZone);
        if ( DEBUG.messageEnabled()) {
            DEBUG.message("At SimpleTimeCondition.getConditionDecision():"
            + " effectiveRange = " + new Date(effectiveRange[0]) + ","
            + new Date(effectiveRange[1]));
        }
        long timeToLive = Long.MAX_VALUE;
        if ( (currentGmt >= effectiveRange[0]) 
                && ( currentGmt <= effectiveRange[1]) ) {
            allowed = true;
            timeToLive = effectiveRange[1];
        } else if ( currentGmt < effectiveRange[0] ) {
            timeToLive = effectiveRange[0];
        }
        return new ConditionDecision(allowed, timeToLive);
    }
        
}
