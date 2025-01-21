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
 * $Id: G11NSupportedCharsetsViewBeanBase.java,v 1.2 2008/06/25 05:43:14 qcheng Exp $
 *
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.service.model.LocaleSupportedCharsetsEntry;
import com.sun.identity.console.service.model.SMG11NModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

public abstract class G11NSupportedCharsetsViewBeanBase
    extends AMPrimaryMastHeadViewBean
{
    private static final String PGTITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    static final String ATTR_LOCALE = "locale";
    static final String ATTR_CHARSETS = "charsets";

    protected CCPageTitleModel ptModel;
    protected AMPropertySheetModel propertySheetModel;

    public G11NSupportedCharsetsViewBeanBase(
        String pageName,
        String defaultDisplayURL
    ) {
        super(pageName);
        setDefaultDisplayURL(defaultDisplayURL);
    }

    protected void initialize() {
        if (!initialized) {
            super.initialize();
            initialized = true;
            createPageTitleModel();
            createPropertyModel();
            registerChildren();
        }
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE, CCPageTitle.class);
        ptModel.registerChildren(this);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        propertySheetModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setPageTitleText(getPageTitleText());
        ptModel.setValue("button1", getButtonlLabel());
        ptModel.setValue("button2", "button.cancel");
    }

    protected abstract String getButtonlLabel();
    protected abstract String getPageTitleText();

    protected void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertyG11NSupportedCharsets.xml"));
        propertySheetModel.clear();
    }

    protected AMModel getModelInternal() {
        AMModel model = null;
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();

        try {
            model = new SMG11NModelImpl(req, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        return model;
    }

    /**
     * Handles cancel request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        SMG11NViewBean vb = (SMG11NViewBean)getViewBean(
            SMG11NViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected void setValues(String value) {
        LocaleSupportedCharsetsEntry entry = new
            LocaleSupportedCharsetsEntry(value);
        propertySheetModel.setValue(ATTR_LOCALE, entry.strLocale);
        propertySheetModel.setValue(ATTR_CHARSETS, entry.strCharsets);
    }

    private String getValues(Map map) {
        String locale = (String)propertySheetModel.getValue(ATTR_LOCALE);
        String charsets = (String)propertySheetModel.getValue(ATTR_CHARSETS);
        locale = locale.trim();
        charsets = charsets.trim();
        map.put(ATTR_LOCALE, locale);
        map.put(ATTR_CHARSETS, charsets);

        String errorMsg = null;

        if (locale.length() == 0) {
            errorMsg =
            "globalization.service.SupportedCharsets.missing.locale.message";
        } else if (charsets.length() == 0) {
            errorMsg =
            "globalization.service.SupportedCharsets.missing.charsets.message";
        }

        return errorMsg;
    }

    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        Map values = new HashMap(6);
        String errorMsg = getValues(values);

        if (errorMsg != null) {
            setInlineAlertMessage(
                CCAlert.TYPE_ERROR, "message.error", errorMsg);
            forwardTo();
        } else {
            handleButton1Request(values);
        }
    }

    protected abstract void handleButton1Request(Map values);
}
