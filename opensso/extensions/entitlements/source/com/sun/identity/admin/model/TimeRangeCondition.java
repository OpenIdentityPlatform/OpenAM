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
 * $Id: TimeRangeCondition.java,v 1.7 2009/08/09 06:04:20 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.TimeCondition;
import java.io.Serializable;
import java.util.Formatter;

public class TimeRangeCondition
        extends ViewCondition
        implements Serializable {

    private int startHour = 0;
    private int startMinute = 0;
    private String startPeriod = "AM";
    private int endHour = 0;
    private int endMinute = 0;
    private String endPeriod = "AM";

    public EntitlementCondition getEntitlementCondition() {
        TimeCondition tc = new TimeCondition();
        tc.setDisplayType(getConditionType().getName());
        String startETime = getETimeString(startHour, startMinute, startPeriod);
        tc.setStartTime(startETime);
        String endETime = getETimeString(endHour, endMinute, endPeriod);
        tc.setEndTime(endETime);

        return tc;
    }

    private String getETimeString(int hour, int min, String period) {
        StringBuffer time = new StringBuffer();
        Formatter f = new Formatter(time);
        int hour24;
        if (period.equals("AM")) {
            hour24 = hour;
        } else {
            hour24 = hour+12;
        }

        f.format("%02d:%02d", hour24, min);
        return time.toString();

    }

    @Override
    public String toString() {
        String startETime = getETimeString(startHour, startMinute, startPeriod);
        String endETime = getETimeString(endHour, endMinute, endPeriod);

        return getTitle() + ":{" + startETime + ">" + endETime + "}";
    }

    public int getStartHour() {
        return startHour;
    }

    public void setStartHour(int startHour) {
        this.startHour = startHour;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(int startMinute) {
        this.startMinute = startMinute;
    }

    public int getEndHour() {
        return endHour;
    }

    public void setEndHour(int endHour) {
        this.endHour = endHour;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public void setEndMinute(int endMinute) {
        this.endMinute = endMinute;
    }

    public String getStartPeriod() {
        return startPeriod;
    }

    public void setStartPeriod(String startPeriod) {
        this.startPeriod = startPeriod;
    }

    public String getEndPeriod() {
        return endPeriod;
    }

    public void setEndPeriod(String endPeriod) {
        this.endPeriod = endPeriod;
    }
}
