/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: TimeConditionHelper.java,v 1.2 2008/06/25 05:43:06 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.console.policy;

import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.policy.plugins.SimpleTimeCondition;
import com.sun.web.ui.view.html.CCDropDownMenu;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class TimeConditionHelper
{
    public static final String STARTDATE = "StartDate";
    public static final String ENDDATE = "EndDate";
    public static final String STARTHOUR = "StartHour";
    public static final String STARTMINUTE = "StartMinute";
    public static final String STARTSESSION = "StartSession";
    public static final String ENDHOUR = "EndHour";
    public static final String ENDMINUTE = "EndMinute";
    public static final String ENDSESSION = "EndSession";
    public static final String STARTDAY = "StartDay";
    public static final String ENDDAY = "EndDay";
    public static final String STANDARDTIMEZONE = "StandardTimeZone";
    public static final String CUSTOMTIMEZONE = "CustomTimeZone";
    public static final String RADIOTIMEZONE = "radioTimeZone";
    private static String DATE_FORMAT = "MM/dd/yyyy";
    private static final String PLUGIN_DATE_FORMAT = "yyyy:MM:dd";
    private static final Pattern RFC_822_PATTERN = Pattern.compile("(-|\\+)[0-9]{4}");

    private static TimeConditionHelper instance = new TimeConditionHelper();

    private TimeConditionHelper() {
    }

    public static TimeConditionHelper getInstance() {
        return instance;
    }

    public String getConditionXML(boolean bCreate, boolean readonly) {
        String xml = null;

        if (bCreate) {
            xml = "com/sun/identity/console/propertyPMConditionTime.xml";
        } else {
            xml = (readonly) ?
                "com/sun/identity/console/propertyPMConditionTime_Readonly.xml":
                "com/sun/identity/console/propertyPMConditionTime.xml";
        }

        return AMAdminUtils.getStringFromInputStream(
            getClass().getClassLoader().getResourceAsStream(xml));
    }

    public String getMissingValuesMessage() {
        return "policy.condition.missing.time.condition.value";
    }

    public void setTimeZoneOptions(
        boolean canModify,
        ConditionOpViewBeanBase viewBean,
        AMModel model
    ) {
        if (canModify) {
            CCDropDownMenu list = (CCDropDownMenu)viewBean.getChild(
                STANDARDTIMEZONE);
            OptionList optList = new OptionList();

            String ids[] = TimeZone.getAvailableIDs();
            Set set = new HashSet(ids.length*2);
            Map map = new HashMap(ids.length*2);

            for (int i = 0; i < ids.length; i++) {
                String displayName = model.getLocalizedString(ids[i]);
                set.add(displayName);
                map.put(displayName, ids[i]);
            }

            List sorted = AMFormatUtils.sortItems(set, model.getUserLocale());

            for (Iterator iter = sorted.iterator(); iter.hasNext(); ) {
                String displayName = (String)iter.next();
                optList.add(displayName, (String)map.get(displayName));
            }

            list.setOptions(optList);
        }
    }

    public Map getConditionValues(ConditionOpViewBeanBase viewBean, Map values){
        Map map = new HashMap(values);

        if (getDateTime(viewBean, STARTDATE, true, map) &&
            getDateTime(viewBean, ENDDATE, false, map)
        ) {
            copyStartToEndIfEndIsMissing(map, STARTDATE, ENDDATE);

            if (getTime(viewBean, true, map) && getTime(viewBean, false, map)) {
                copyStartToEndIfEndIsMissing(
                    map, SimpleTimeCondition.START_TIME,
                    SimpleTimeCondition.END_TIME);

                getDay(viewBean, true, map);
                getDay(viewBean, false, map);
                copyStartToEndIfEndIsMissing(map,
                    SimpleTimeCondition.START_DAY, SimpleTimeCondition.END_DAY);

                if (!getTimeZoneValue(viewBean, map)) {
                    map = null;
                }
            } else {
                map = null;
            }
        } else {
            map = null;
        }

        return map;
    }

    private void copyStartToEndIfEndIsMissing(
        Map map,
        String startAttrName,
        String endAttrName
    ) {
        Set startSet = (Set)map.get(startAttrName);
        Set endSet = (Set)map.get(endAttrName);

        if ((startSet != null) && (endSet == null)) {
            map.put(endAttrName, startSet);
        } else if ((endSet != null) && (startSet == null)) {
            map.put(startAttrName, endSet);
        }
    }

    private boolean getDateTime(
        ConditionOpViewBeanBase viewBean,
        String childName,
        boolean bStart,
        Map values
    ) {        
        boolean valid = false;
        AMPropertySheetModel propModel = viewBean.getPropertySheetModel();
        String date = (String)propModel.getValue(childName);
        String formatedDate = getDateTime(viewBean, date, bStart);

        if (formatedDate != null) {
            if (formatedDate.length() > 0) {
                Set set = new HashSet(2);
                set.add(formatedDate);
                values.put(childName, set);
            }
            valid = true;
        }
        return valid;
    }

    private void getDay(
        ConditionOpViewBeanBase viewBean,
        boolean bStart,
        Map values
    ) {
        AMPropertySheetModel propModel = viewBean.getPropertySheetModel();
        String day = (bStart) ? (String)propModel.getValue(STARTDAY) :
            (String)propModel.getValue(ENDDAY);
        if (day.length() > 0) {
            Set set = new HashSet();
            set.add(day);
            values.put((bStart) ? SimpleTimeCondition.START_DAY :
                SimpleTimeCondition.END_DAY, set);
        }
    }

    private boolean getTime(
        ConditionOpViewBeanBase viewBean,
        boolean bStart,
        Map values
    ) {
        AMPropertySheetModel propModel = viewBean.getPropertySheetModel();
        boolean valid = false;

        String hours = (bStart) ? (String)propModel.getValue(STARTHOUR) :
            (String)propModel.getValue(ENDHOUR);
        String minutes = (bStart) ? (String)propModel.getValue(STARTMINUTE) :
            (String)propModel.getValue(ENDMINUTE);
        String session = (bStart) ? (String)propModel.getValue(STARTSESSION) :
            (String)propModel.getValue(ENDSESSION);

        hours = hours.trim();
        minutes = minutes.trim();

        if ((hours.length() > 0) && (minutes.length() > 0)) {
            try {
                int hr = Integer.parseInt(hours); 
                int mm = Integer.parseInt(minutes); 

                if (session.equals("pm") && (hr < 12)) {
                    hr += 12;
                }

                if ((hr >= 0) && (hr <= 23) && (mm >= 0) && (mm <= 59)) {
                    hours = (hr < 10) ? "0" + hr : Integer.toString(hr);
                    minutes = (mm < 10) ? "0" + mm : Integer.toString(mm);

                    Set set = new HashSet();
                    set.add(hours + ":" + minutes);

                    values.put((bStart) ? SimpleTimeCondition.START_TIME :
                        SimpleTimeCondition.END_TIME, set);
                    valid = true;
                } else {
                    viewBean.setErrorMessage((bStart)?
                        "policy.condition.time.invalid.starttime" :
                        "policy.condition.time.invalid.endtime");
                }
            } catch (NumberFormatException e) {
                viewBean.setErrorMessage((bStart)?
                    "policy.condition.time.invalid.starttime" :
                    "policy.condition.time.invalid.endtime");
            }
        } else {
            valid = true;
        }

        return valid;
    }

    private String formatDate(String strDate) {
        String formatedDate = "";
        if ((strDate != null) && (strDate.trim().length() > 0)) {
            DateFormat dff = new SimpleDateFormat(PLUGIN_DATE_FORMAT);
            dff.setLenient(false);
            try {
                Date date = dff.parse(strDate);
                DateFormat df = new SimpleDateFormat(DATE_FORMAT);
                df.setLenient(false);
                formatedDate = df.format(date);
            } catch (ParseException e) {
                AMModelBase.debug.warning("TimeConditionHelper.formatDate", e);
            }
        }
        return formatedDate;
    }

    public String getDateTime(
        ConditionOpViewBeanBase viewBean,
        String strDate,
        boolean bStart
    ) {
        String formatedDate = "";

        if ((strDate != null) && (strDate.trim().length() > 0)) {
            DateFormat df = new SimpleDateFormat(DATE_FORMAT);
            df.setLenient(false);

            try {
                Date date = df.parse(strDate);
                DateFormat dff = new SimpleDateFormat(PLUGIN_DATE_FORMAT);
                formatedDate = dff.format(date);
            } catch (ParseException e) {
                AMModelBase.debug.warning("TimeConditionHelper.getDateTime", e);
                viewBean.setErrorMessage((bStart)?
                    "policy.condition.time.invalid.startdate" :
                    "policy.condition.time.invalid.enddate");
                formatedDate = null;
            }
        }

        return formatedDate;
    }

    private boolean getTimeZoneValue(
        ConditionOpViewBeanBase viewBean,
        Map values
    ){
        boolean valid = false;
        AMPropertySheetModel propModel = viewBean.getPropertySheetModel();
        String radio = (String)propModel.getValue(RADIOTIMEZONE);

        if ((radio != null) && (radio.length() > 0)) {
            String tz = null;
            if (radio.equals("standard")) {
                tz = (String)propModel.getValue(STANDARDTIMEZONE);
                valid = true;
            } else {
                tz = (String) propModel.getValue(CUSTOMTIMEZONE);
                if (RFC_822_PATTERN.matcher(tz).matches()) {
                    tz = getNormalizedTimeZone("GMT" + tz);
                    valid = true;
                } else {
                    valid = isValidTimeZone(tz);
                }
                if (!valid) {
                    viewBean.setErrorMessage(
                        "policy.condition.time.invalid.timezone");
                }
            }

            if (valid) {
                Set set = new HashSet();
                set.add(tz);
                values.put(SimpleTimeCondition.ENFORCEMENT_TIME_ZONE,
                    set);
            }
        } else {
            valid = true;
        }

        return valid;
    }

    public void setDay(
        ConditionOpViewBeanBase viewBean,
        boolean bStart,
        String strDay
    ) {
        if ((strDay != null) && (strDay.length() > 0)) {
            AMPropertySheetModel propModel = viewBean.getPropertySheetModel();
            propModel.setValue((bStart) ? STARTDAY : ENDDAY, strDay);
        }
    }

    public void setDate(
        ConditionOpViewBeanBase viewBean,
        boolean bStart,
        String strDate, AMModel model
    ) {
        TimeConditionHelper.DATE_FORMAT =
                model.getLocalizedString("policy.condition.time.dateformat");

        if(TimeConditionHelper.DATE_FORMAT == null ||
                TimeConditionHelper.DATE_FORMAT.length() == 0)
            TimeConditionHelper.DATE_FORMAT = "MM/dd/yyyy";

        AMPropertySheetModel propModel = viewBean.getPropertySheetModel();
        propModel.setValue((bStart) ? STARTDATE : ENDDATE, formatDate(strDate));
    }

    public void setTime(
        ConditionOpViewBeanBase viewBean,
        boolean bStart,
        String strTime
    ) {
        int idx = strTime.indexOf(":");
        if (idx != -1) {
            String hours = strTime.substring(0, idx);
            String minutes = strTime.substring(idx+1);

            try {
                int hr = Integer.parseInt(hours);
                int mm = Integer.parseInt(minutes);
                boolean ampm = (hr >= 12);

                if (ampm && (hr > 12)) {
                    hr -= 12;
                }

                AMPropertySheetModel propModel =
                    viewBean.getPropertySheetModel();
                propModel.setValue((bStart) ? STARTHOUR : ENDHOUR,
                    Integer.toString(hr));
                propModel.setValue((bStart) ? STARTMINUTE : ENDMINUTE,
                    Integer.toString(mm));
                propModel.setValue((bStart) ? STARTSESSION : ENDSESSION,
                    (ampm) ? "pm" : "am");
            } catch (NumberFormatException e) {
                AMModelBase.debug.error("TimeConditionHelper.setTime", e);
            }
        }
    }

    public void setTimeZone(
        ConditionOpViewBeanBase viewBean,
        boolean canModify,
        String tz
    ) {
        if ((tz != null) && (tz.trim().length() > 0)) {
            AMPropertySheetModel propModel = viewBean.getPropertySheetModel();

            if (canModify) {
                CCDropDownMenu list = (CCDropDownMenu)viewBean.getChild(
                    STANDARDTIMEZONE);
                OptionList optList = list.getOptions();

                if ((optList != null) && optList.hasValue(tz)) {
                    propModel.setValue(STANDARDTIMEZONE, tz);
                    propModel.setValue(RADIOTIMEZONE, "standard");
                } else {
                    propModel.setValue(CUSTOMTIMEZONE, tz);
                    propModel.setValue(RADIOTIMEZONE, "custom");
                }
            } else {
                String ids[] = TimeZone.getAvailableIDs();
                boolean contains = false;

                for (int i = 0; (i < ids.length) && !contains; i++) {
                    contains = ids[i].equals(tz);
                }

                if (contains) {
                    propModel.setValue(STANDARDTIMEZONE, tz);
                    propModel.setValue(RADIOTIMEZONE, "standard");
                } else {
                    propModel.setValue(CUSTOMTIMEZONE, tz);
                    propModel.setValue(RADIOTIMEZONE, "custom");
                }

            }
        }
    }

    private boolean isValidTimeZone(String tz) {
        boolean valid = false;

        if ((tz == null) || (tz.trim().length() == 0)) {
            valid = true;
        } else {
            TimeZone t = TimeZone.getTimeZone(tz);
            String id = t.getID();
            valid = id.equals(tz);
        }

        return valid;
    }

    private String getNormalizedTimeZone(String tz) {
        TimeZone t = TimeZone.getTimeZone(tz);
        return t.getID();
    }
}
