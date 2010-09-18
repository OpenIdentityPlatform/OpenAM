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
 * $Id: TimezoneCondition.java,v 1.7 2009/08/09 06:04:20 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.TimeCondition;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import javax.faces.model.SelectItem;

public class TimezoneCondition
    extends ViewCondition
    implements Serializable {

    private String timezoneId;
    private List<String> timezoneIds;

    public EntitlementCondition getEntitlementCondition() {
        TimeCondition tc = new TimeCondition();
        tc.setDisplayType(getConditionType().getName());
        tc.setEnforcementTimeZone(timezoneId);

        return tc;
    }

    public List<SelectItem> getTimezoneIdItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        for (String id: getTimezoneIds()) {
            TimeZone tz = TimeZone.getTimeZone(id);
            SelectItem si = new SelectItem();
            si.setValue(id);
            si.setLabel(tz.getID());

            items.add(si);
        }

        return items;
    }

    public String getTimezoneTitle() {
        String title = null;
        if (timezoneId != null && timezoneId.length() > 0) {
            TimeZone tz = TimeZone.getTimeZone(timezoneId);
            if (tz != null) {
                title = tz.getDisplayName();
            }
        }
        return title;
    }

    public List<String> getTimezoneIds() {
        if (timezoneIds == null || timezoneIds.size() == 0) {
            String[] timezoneIdArray = TimeZone.getAvailableIDs();
            Arrays.sort(timezoneIdArray);
            timezoneIds = Arrays.asList(timezoneIdArray);
        }
        return timezoneIds;
    }

    public void setTimezoneIds(List<String> timezoneIds) {
        this.timezoneIds = timezoneIds;
    }

    public String getTimezoneId() {
        if (timezoneId == null) {
            timezoneId = TimeZone.getDefault().getID();
        }
        return timezoneId;
    }

    public void setTimezoneId(String timezoneId) {
        this.timezoneId = timezoneId;
    }

    @Override
    public String toString() {
        Resources r = new Resources();
        Locale l = r.getLocale();
        String s = getTitle();
        if (timezoneId != null) {
            TimeZone tz = TimeZone.getTimeZone(timezoneId);
            s = r.getString(this, "toString", s, tz.getID(), tz.getDisplayName(l));
        }

        return s;
    }
}
