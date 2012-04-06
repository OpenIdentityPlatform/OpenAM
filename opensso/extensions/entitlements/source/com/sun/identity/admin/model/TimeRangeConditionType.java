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
 * $Id: TimeRangeConditionType.java,v 1.5 2009/07/31 19:41:06 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import com.sun.identity.entitlement.TimeCondition;
import java.io.Serializable;

public class TimeRangeConditionType
        extends TimeConditionType
        implements Serializable {

    private static class Time {
        int hour;
        int min;
        String period;

        public Time(String timeString) {
            assert (timeString != null);

            String[] timeArray = timeString.split(":");
            assert (timeArray.length == 2);

            int h = Integer.valueOf(timeArray[0]);
            int m = Integer.valueOf(timeArray[1]);

            if (h < 12) {
                hour = Integer.valueOf(timeArray[0]);
                period = "AM";
            } else {
                hour = Integer.valueOf(timeArray[0]) - 12;
                period = "PM";
            }
            min = Integer.valueOf(timeArray[1]);
        }
    }

    public ViewCondition newViewCondition() {
        ViewCondition vc = new TimeRangeCondition();
        vc.setConditionType(this);

        return vc;
    }

    public ViewCondition newViewCondition(TimeCondition tc) {
        TimeRangeCondition trc = (TimeRangeCondition) newViewCondition();
        Time startTime = new Time(tc.getStartTime());
        Time endTime = new Time(tc.getEndTime());

        trc.setStartHour(startTime.hour);
        trc.setStartMinute(startTime.min);
        trc.setStartPeriod(startTime.period);
        trc.setEndHour(endTime.hour);
        trc.setEndMinute(endTime.min);
        trc.setEndPeriod(endTime.period);

        return trc;
    }
}
