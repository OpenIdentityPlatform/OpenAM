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
 * $Id: DateRangeConditionType.java,v 1.2 2009/10/13 16:04:40 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import com.sun.identity.entitlement.TimeCondition;
import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class DateRangeConditionType
        extends TimeConditionType
        implements Serializable {

    private static class Date {
        int year;
        int month;
        int day;

        public Date(String dateString) {
            assert (dateString != null && dateString.length() > 0);

            String[] dateArray = dateString.split(":");
            assert (dateArray.length == 3);

            year = Integer.valueOf(dateArray[0]);
            month = Integer.valueOf(dateArray[1]) - 1;
            day = Integer.valueOf(dateArray[2]);
        }
    }

    public ViewCondition newViewCondition() {
        ViewCondition vc = new DateRangeCondition();
        vc.setConditionType(this);

        return vc;
    }

    public ViewCondition newViewCondition(TimeCondition tc) {
        DateRangeCondition drc = (DateRangeCondition) newViewCondition();

        Date startDate = new Date(tc.getStartDate());
        Calendar startCal = new GregorianCalendar(startDate.year, startDate.month, startDate.day);
        drc.setStartDate(startCal.getTime());

        if (tc.getEndDate() == null || tc.getEndDate().length() == 0) {
            drc.setEndDate(new java.util.Date());
            drc.setEndDateDisabled(true);
        } else {
            Date endDate = new Date(tc.getEndDate());
            Calendar endCal = new GregorianCalendar(endDate.year, endDate.month, endDate.day);
            drc.setEndDate(endCal.getTime());
        }

        return drc;
    }
}
