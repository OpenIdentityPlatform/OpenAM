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
 * $Id: AttributesPolicySummary.java,v 1.3 2009/06/05 20:36:40 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.model.AttributesBean.AttributeType;
import java.util.ArrayList;
import java.util.List;

public abstract class AttributesPolicySummary extends PolicySummary {

    public AttributesPolicySummary(PolicyWizardBean policyWizardBean) {
        super(policyWizardBean);
    }

    public abstract AttributeType getAttributeType();

    public int getCount() {
        int count = 0;
        for (AttributesBean ab: getPolicyWizardBean().getPrivilegeBean().getAttributesBeans()) {
            if (ab.getAttributeType() == getAttributeType()) {
                int i = ab.getViewAttributes().size();
                count += i;
            }
        }
        return count;
    }

    protected List<ViewAttribute> getViewAttributes() {
        List<ViewAttribute> vas = new ArrayList<ViewAttribute>();
        for (AttributesBean ab: getPolicyWizardBean().getPrivilegeBean().getAttributesBeans()) {
            if (ab.getAttributeType() == getAttributeType()) {
                vas.addAll(ab.getViewAttributes());
            }
        }
        return vas;
    }

    public String getToFormattedString() {
        List<ViewAttribute> vas = getViewAttributes();
        String f = AttributesBean.getToFormattedString(vas);
        return f;
    }

    public String getValue() {
        int count = getCount();
        return Integer.toString(count);
    }

    public boolean isExpandable() {
        int count = getCount();
        return count > 0;
    }

    public String getIcon() {
        return "../image/device.png";
    }

    public int getGotoStep() {
        return PolicyWizardStep.ADVANCED.toInt();
    }
}
