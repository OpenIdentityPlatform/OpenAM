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
 * $Id: DaysOfWeekCondition.java,v 1.11 2009/08/09 06:04:20 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.OrCondition;
import com.sun.identity.entitlement.TimeCondition;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.faces.model.SelectItem;

public class DaysOfWeekCondition
        extends ViewCondition
        implements Serializable {

    public static String[] DAYS = new String[]{"mon", "tue", "wed", "thu", "fri", "sat", "sun"};
    private String[] selectedDays = new String[7];

    public EntitlementCondition getEntitlementCondition() {
        if (selectedDays.length > 1) {
            OrCondition oc = new OrCondition();
            oc.setDisplayType("or");
            Set<EntitlementCondition> orConditions = new HashSet<EntitlementCondition>();
            for (String day : selectedDays) {
                TimeCondition tc = new TimeCondition();
                tc.setDisplayType(getConditionType().getName());
                tc.setStartDay(day);
                tc.setEndDay(day);
                orConditions.add(tc);
            }
            oc.setEConditions(orConditions);
            return oc;
        } else if (selectedDays.length > 0) {
            TimeCondition tc = new TimeCondition();
            tc.setDisplayType(getConditionType().getName());
            if (selectedDays.length > 0) {
                String day = selectedDays[0];
                tc.setStartDay(day);
                tc.setEndDay(day);
            }
            return tc;
        }
        return null;
    }

    public List<SelectItem> getDayItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        for (String day : DAYS) {
            Resources r = new Resources();
            String label = r.getString(this, day);
            SelectItem si = new SelectItem(day, label);
            items.add(si);
        }

        return items;
    }

    public String[] getSelectedDays() {
        return selectedDays;
    }

    public void setSelectedDays(String[] selectedDays) {
        this.selectedDays = selectedDays;
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append(getTitle());
        b.append(":{");

        for (int i = 0; i < selectedDays.length; i++) {
            Resources r = new Resources();
            String label = r.getString(this, selectedDays[i]);
            b.append(label);
            if (i < selectedDays.length - 1) {
                b.append(",");
            }
        }
        b.append("}");

        return b.toString();
    }
}
