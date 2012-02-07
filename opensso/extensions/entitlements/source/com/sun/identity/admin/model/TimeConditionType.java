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
 * $Id: TimeConditionType.java,v 1.6 2009/08/09 06:04:20 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.TimeCondition;
import java.util.ArrayList;
import java.util.List;

public abstract class TimeConditionType extends ConditionType {

    public abstract ViewCondition newViewCondition(TimeCondition tc);

    public ViewCondition newViewCondition(EntitlementCondition ec, ConditionFactory conditionTypeFactory) {
        assert (ec instanceof TimeCondition);
        TimeCondition tc = (TimeCondition) ec;
        TimeConditionType tct;
        List<ViewCondition> timeViewConditions = new ArrayList<ViewCondition>();

        if (tc.getStartDate() != null && tc.getStartDate().length() > 0) {
            tct = (TimeConditionType) conditionTypeFactory.getConditionType("dateRange");
            assert (tct != null);
            ViewCondition vc = tct.newViewCondition(tc);
            timeViewConditions.add(vc);
        }

        if (tc.getStartTime() != null && tc.getStartTime().length() > 0) {
            tct = (TimeConditionType) conditionTypeFactory.getConditionType("timeRange");
            assert (tct != null);
            ViewCondition vc = tct.newViewCondition(tc);
            timeViewConditions.add(vc);
        }

        if (tc.getStartDay() != null && tc.getStartDay().length() > 0) {
            tct = (TimeConditionType) conditionTypeFactory.getConditionType("daysOfWeek");
            assert (tct != null);
            ViewCondition vc = tct.newViewCondition(tc);
            timeViewConditions.add(vc);
        }

        if (tc.getEnforcementTimeZone() != null && tc.getEnforcementTimeZone().length() > 0) {
            tct = (TimeConditionType) conditionTypeFactory.getConditionType("timezone");
            assert (tct != null);
            ViewCondition vc = tct.newViewCondition(tc);
            timeViewConditions.add(vc);
        }

        ViewCondition newVc = null;

        if (timeViewConditions.size() > 1) {
            ConditionType ct = conditionTypeFactory.getConditionType("and");
            assert (ct != null);
            AndViewCondition avc = (AndViewCondition) ct.newViewCondition();
            avc.setViewConditions(timeViewConditions);
            newVc = avc;
        } else if (timeViewConditions.size() == 1) {
            newVc = timeViewConditions.get(0);
        }

        return newVc;
    }
}
