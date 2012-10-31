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
 * $Id: SCPolicyResourceComparatorViewBeanBase.java,v 1.2 2008/06/25 05:43:16 qcheng Exp $
 *
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
import com.sun.identity.console.service.model.PolicyResourceComparator;
import com.sun.identity.console.service.model.SCPolicyModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import javax.servlet.http.HttpServletRequest;

public abstract class SCPolicyResourceComparatorViewBeanBase
    extends AMPrimaryMastHeadViewBean
{
    private static final String PGTITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    static final String ATTR_SERVICE_TYPE = "serviceType";
    static final String ATTR_CLASS = "clazz";
    static final String ATTR_WILDCARD = "wildcard";
    static final String ATTR_DELIMITER = "delimiter";
    static final String ATTR_CASE_SENSITIVE = "caseSensitive";
    static final String ATTR_ONE_LEVEL_WILDCARD = "oneLevelWildcard";

    protected CCPageTitleModel ptModel;
    protected AMPropertySheetModel propertySheetModel;

    public SCPolicyResourceComparatorViewBeanBase(
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
            "com/sun/identity/console/propertySCPolicyResourceComparator.xml"));
        propertySheetModel.clear();
    }

    protected AMModel getModelInternal() {
        HttpServletRequest r = RequestManager.getRequestContext().getRequest();
        return new SCPolicyModelImpl(r, getPageSessionAttributes());
    }

    /**
     * Handles cancel request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        SCPolicyViewBean vb = (SCPolicyViewBean)getViewBean(
            SCPolicyViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected void setValues(String value) {
        PolicyResourceComparator rc = new PolicyResourceComparator(value);
        propertySheetModel.setValue(ATTR_SERVICE_TYPE, rc.getServiceType());
        propertySheetModel.setValue(ATTR_CLASS, rc.getClazz());
        propertySheetModel.setValue(ATTR_WILDCARD, rc.getWildcard());
        propertySheetModel.setValue(ATTR_DELIMITER, rc.getDelimiter());
        propertySheetModel.setValue(
            ATTR_ONE_LEVEL_WILDCARD, rc.getOneLevelWildcard());
        propertySheetModel.setValue(ATTR_CASE_SENSITIVE, rc.getCaseSensitive());
    }

    private PolicyResourceComparator getValues()
        throws AMConsoleException {
        String serviceType = (String)propertySheetModel.getValue(
            ATTR_SERVICE_TYPE);
        String clazz = (String)propertySheetModel.getValue(ATTR_CLASS);
        String delimiter = (String)propertySheetModel.getValue(ATTR_DELIMITER);
        String wildcard = (String)propertySheetModel.getValue(ATTR_WILDCARD);
        String oneLevelWildcard = (String)propertySheetModel.getValue(
            ATTR_ONE_LEVEL_WILDCARD);
        String caseSensitive = (String)propertySheetModel.getValue(
            ATTR_CASE_SENSITIVE);

        if (serviceType.trim().length() == 0) {
            throw new AMConsoleException(
            "policy.service.resource.comparator.missing.service.type.message");
        }

        PolicyResourceComparator c = new PolicyResourceComparator();
        c.setServiceType(serviceType);
        c.setClazz(clazz);
        c.setDelimiter(delimiter);
        c.setWildcard(wildcard);
        c.setOneLevelWildcard(oneLevelWildcard);
        c.setCaseSensitive(caseSensitive);
        return c;
    }

    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        try {
            PolicyResourceComparator rc = getValues();
            handleButton1Request(rc);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }

    protected abstract void handleButton1Request(PolicyResourceComparator rc);
}
