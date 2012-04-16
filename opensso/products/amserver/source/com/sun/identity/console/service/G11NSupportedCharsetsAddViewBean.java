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
 * $Id: G11NSupportedCharsetsAddViewBean.java,v 1.3 2008/06/25 05:43:14 qcheng Exp $
 *
 */

package com.sun.identity.console.service;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.service.model.LocaleSupportedCharsetsEntry;
import com.sun.identity.console.service.model.SMG11NModelImpl;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.Map;
import java.util.Set;

public class G11NSupportedCharsetsAddViewBean
    extends G11NSupportedCharsetsViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/G11NSupportedCharsetsAdd.jsp";

    public G11NSupportedCharsetsAddViewBean() {
        super("G11NSupportedCharsetsAdd", DEFAULT_DISPLAY_URL);
    }

    protected String getButtonlLabel() {
        return "button.ok";
    }

    protected String getPageTitleText() {
        return
            "globalization.service.SupportedCharsets.create.page.title";
    }

    protected void handleButton1Request(Map values) {
        boolean error = false;
        SMG11NViewBean vb = (SMG11NViewBean)getViewBean(SMG11NViewBean.class);
        Map attrValues = (Map)getPageSessionAttribute(
            SMG11NViewBean.PROPERTY_ATTRIBUTE);
        Set charsets = (Set)attrValues.get(
            SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS);
        String strEntry = LocaleSupportedCharsetsEntry.toString(
            (String)values.get(ATTR_LOCALE),(String)values.get(ATTR_CHARSETS));

        if ((charsets == null) || charsets.isEmpty()) {
            charsets = new OrderedSet();
            attrValues.put(SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS,
                (OrderedSet)charsets);
            charsets.add(strEntry);
        } else {
            try {
                LocaleSupportedCharsetsEntry.validate(charsets, 
                    (String)values.get(ATTR_LOCALE));
                charsets.add(strEntry);
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                error = true;
            }
        }
        
        if (error ) {
            forwardTo();
        } else {
            setPageSessionAttribute(SMG11NViewBean.PAGE_MODIFIED, "1");
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        }
    }
}
