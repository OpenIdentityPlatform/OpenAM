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
 * $Id: SCPolicyResourceComparatorAddViewBean.java,v 1.3 2008/06/25 05:43:16 qcheng Exp $
 *
 */

package com.sun.identity.console.service;

import com.sun.identity.console.service.model.PolicyResourceComparator;
import com.sun.identity.console.service.model.SCPolicyModel;
import com.sun.identity.shared.datastruct.OrderedSet;
import java.util.Map;
import java.util.Set;

public class SCPolicyResourceComparatorAddViewBean
    extends SCPolicyResourceComparatorViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/SCPolicyResourceComparatorAdd.jsp";

    public SCPolicyResourceComparatorAddViewBean() {
        super("SCPolicyResourceComparatorAdd", DEFAULT_DISPLAY_URL);
    }

    protected String getButtonlLabel() {
        return "button.ok";
    }

    protected String getPageTitleText() {
        return "policy.service.resource.comparator.create.page.title";
    }

    protected void handleButton1Request(PolicyResourceComparator rc) {
        SCPolicyViewBean vb = (SCPolicyViewBean)getViewBean(
            SCPolicyViewBean.class);
        Map attrValues = (Map)getPageSessionAttribute(
            SCPolicyViewBean.PROPERTY_ATTRIBUTE);
        Set resourceComparator = (Set)attrValues.get(
            SCPolicyModel.ATTRIBUTE_NAME_RESOURCE_COMPARATOR);

        if ((resourceComparator == null) || resourceComparator.isEmpty()) {
            resourceComparator = new OrderedSet();
            attrValues.put(SCPolicyModel.ATTRIBUTE_NAME_RESOURCE_COMPARATOR,
                (OrderedSet)resourceComparator);
        }

        resourceComparator.add(rc.toString());
        setPageSessionAttribute(SCPolicyViewBean.PAGE_MODIFIED, "1");
        backTrail();
        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.services.policy.resource.comparator.add";
    }

    protected boolean startPageTrail() {
        return false;
    }
}
