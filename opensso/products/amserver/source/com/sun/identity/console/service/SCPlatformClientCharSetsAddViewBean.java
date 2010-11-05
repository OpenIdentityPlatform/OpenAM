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
 * $Id: SCPlatformClientCharSetsAddViewBean.java,v 1.4 2008/11/05 22:02:40 asyhuang Exp $
 *
 */

package com.sun.identity.console.service;

import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.service.model.SCPlatformModelImpl;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.Map;
import java.util.Set;

public class SCPlatformClientCharSetsAddViewBean
    extends SCPlatformClientCharSetsViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/SCPlatformClientCharSetsAdd.jsp";

    public SCPlatformClientCharSetsAddViewBean() {
        super("SCPlatformClientCharSetsAdd", DEFAULT_DISPLAY_URL);
    }

    protected String getButtonlLabel() {
        return "button.ok";
    }

    protected String getPageTitleText() {
        return "platform.service.clientCharSets.create.page.title";
    }

    protected void handleButton1Request(Map values) {
        AMViewBeanBase vb = getPlatformViewBean();
        
        Map attrValues = (Map)getPageSessionAttribute(
            SCPlatformViewBean.PROPERTY_ATTRIBUTE);
        Set clientCharSets = (Set)attrValues.get(
            SCPlatformModelImpl.ATTRIBUTE_NAME_CLIENT_CHAR_SETS);

        if ((clientCharSets == null) || clientCharSets.isEmpty()) {
            clientCharSets = new OrderedSet();
            attrValues.put(SCPlatformModelImpl.ATTRIBUTE_NAME_CLIENT_CHAR_SETS,
                (OrderedSet)clientCharSets);
        }

        String val = (String)values.get(ATTR_CLIENT_TYPE) + "|" +
            (String)values.get(ATTR_CHARSET);
        if (clientCharSets.contains(val)){
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "platform.service.clientCharSets.create.page.existing");
            forwardTo();
        } else {
            clientCharSets.add(val);
            setPageSessionAttribute(SCPlatformViewBean.PAGE_MODIFIED, "1");
            backTrail();
            unlockPageTrailForSwapping();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        }
    }

    protected String getBreadCrumbDisplayName() {
      return "breadcrumbs.services.platform.client.charsets.add";
    }

    protected boolean startPageTrail() {
      return false;
    }
}
