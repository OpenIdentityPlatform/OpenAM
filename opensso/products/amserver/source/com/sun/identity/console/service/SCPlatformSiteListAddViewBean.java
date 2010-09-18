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
 * $Id: SCPlatformSiteListAddViewBean.java,v 1.3 2008/06/25 05:43:15 qcheng Exp $
 *
 */

package com.sun.identity.console.service;

import com.sun.identity.console.service.model.SCPlatformModelImpl;
import com.sun.identity.shared.datastruct.OrderedSet;
import java.util.Map;
import java.util.Set;

public class SCPlatformSiteListAddViewBean
    extends SCPlatformSiteListViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/SCPlatformSiteListAdd.jsp";

    public SCPlatformSiteListAddViewBean() {
        super("SCPlatformSiteListAdd", DEFAULT_DISPLAY_URL);
    }

    protected String getButtonlLabel() {
        return "button.ok";
    }

    protected String getPageTitleText() {
        return "platform.service.siteList.create.page.title";
    }

    protected void handleButton1Request(Map values) {
        SCPlatformViewBean vb = (SCPlatformViewBean)getViewBean(
            SCPlatformViewBean.class);
        Map attrValues = (Map)getPageSessionAttribute(
            SCPlatformViewBean.PROPERTY_ATTRIBUTE);
        Set siteList = (Set)attrValues.get(
            SCPlatformModelImpl.ATTRIBUTE_NAME_SITE_LIST);

        if ((siteList == null) || siteList.isEmpty()) {
            siteList = new OrderedSet();
            attrValues.put(SCPlatformModelImpl.ATTRIBUTE_NAME_SITE_LIST,
                (OrderedSet)siteList);
        }

        String val = (String)values.get(ATTR_SERVER) + "|" +
            (String)values.get(ATTR_NAME);
        siteList.add(val);
        setPageSessionAttribute(SCPlatformViewBean.PAGE_MODIFIED, "1");
        backTrail();
        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.services.platform.site.add";
    }

    protected boolean startPageTrail() {
        return false;
    }
}
