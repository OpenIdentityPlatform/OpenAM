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
 * $Id: DaysOfWeekConditionType.java,v 1.5 2009/06/04 11:49:14 veiming Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.CircularArrayList;
import com.sun.identity.entitlement.TimeCondition;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DaysOfWeekConditionType
    extends TimeConditionType
    implements Serializable {
    public ViewCondition newViewCondition() {
        ViewCondition vc = new DaysOfWeekCondition();
        vc.setConditionType(this);

        return vc;
    }

    public ViewCondition newViewCondition(TimeCondition tc) {
        assert(tc.getStartDay() != null);
        assert(tc.getEndDay() != null);

        DaysOfWeekCondition dowc = (DaysOfWeekCondition)newViewCondition();

        List<String> days = new CircularArrayList(Arrays.asList(DaysOfWeekCondition.DAYS));
        List<String> selectedDays = new ArrayList<String>();

        String startDay = tc.getStartDay();
        String endDay = tc.getEndDay();

        for (int i = days.indexOf(startDay); days.get(i).equals(endDay); i++) {
            selectedDays.add(days.get(i));
        }
        dowc.setSelectedDays(selectedDays.toArray(new String[0]));
        
        return dowc;
    }

}
