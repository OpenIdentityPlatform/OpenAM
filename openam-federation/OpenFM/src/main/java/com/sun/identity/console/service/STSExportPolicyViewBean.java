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
 * $Id: STSExportPolicyViewBean.java,v 1.1 2009/12/19 00:14:56 asyhuang Exp $
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */
package com.sun.identity.console.service;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.service.model.STSExportPolicyModel;
import com.sun.identity.console.service.model.STSExportPolicyModelImpl;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCStaticTextField;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.owasp.esapi.ESAPI;

/**
 * Export STS Policy
 */
public class STSExportPolicyViewBean extends AMPrimaryMastHeadViewBean {

    private static final String DEFAULT_DISPLAY_URL =
            "/console/service/STSExportPolicy.jsp";
    private static final String PGTITLE_ONE_BTN = "pgtitleOneBtn";
    static final String PG_ATTR_CONFIG_PAGE = "pgAttrConfigPage";
    protected static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private AMPropertySheetModel psModel;
    private CCPageTitleModel ptModel;

    /**
     * Creates a STS policy view bean.
     */
    public STSExportPolicyViewBean() {
        super("STSExportPolicy");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {          
            super.initialize();
            createPropertyModel();
            createPageTitleModel();
            registerChildren();
            initialized = true;
        }
    }

    protected View createChild(String name) {
        View view = null;
        if (name.equals(PGTITLE_ONE_BTN)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, psModel, name);
        } else if ((psModel != null) && psModel.isChildSupported(name)) {
            view = psModel.createChild(this, name, getModel());
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }
        return view;
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/oneBtnPageTitle.xml"));
        ptModel.setValue("button1", getBackButtonLabel());
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new STSExportPolicyModelImpl(req, getPageSessionAttributes());
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        ptModel.registerChildren(this);
        psModel.registerChildren(this);
    }

    protected void createPropertyModel() {
        psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertySTSExportPolicy.xml"));
        psModel.clear();
    }

    public void beginDisplay(DisplayEvent event) throws ModelControlException {
        try {
            super.beginDisplay(event);
            
            AMPropertySheet ps = (AMPropertySheet) getChild(PROPERTY_ATTRIBUTE);
            ps.init();

            STSExportPolicyModel model = (STSExportPolicyModel) getModel();

            Map values = model.getAttributeValues();
            org.owasp.esapi.Encoder enc = ESAPI.encoder();

            CCStaticTextField policyValuesText =
                    (CCStaticTextField) getChild("policyAttributeValues");
            policyValuesText.setValue(enc.encodeForHTML((String) values.get("policyAttributeValues")));

            CCStaticTextField inputPolicyValuesText =
                    (CCStaticTextField) getChild("inputPolicyAttributeValues");
            inputPolicyValuesText.setValue(enc.encodeForHTML((String) values.get("inputPolicyAttributeValues")));

            CCStaticTextField outputPolicyValuesText =
                    (CCStaticTextField) getChild("outputPolicyAttributeValues");
            outputPolicyValuesText.setValue(enc.encodeForHTML((String) values.get("outputPolicyAttributeValues")));

        } catch (AMConsoleException amce) {
            setInlineAlertMessage(
                    CCAlert.TYPE_ERROR, "message.error", amce.getMessage());
        }
    }

    public void handleButton1Request(RequestInvocationEvent event)
            throws ModelControlException {
        try {
            AMViewBeanBase vb =
                    (AMViewBeanBase) getViewBean(
                    Class.forName((String) getPageSessionAttribute(
                    PG_ATTR_CONFIG_PAGE)));
            removePageSessionAttribute(PG_ATTR_CONFIG_PAGE);
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (ClassNotFoundException e) {
            setInlineAlertMessage(
                    CCAlert.TYPE_ERROR, "message.error", e.getMessage());
        }
    }

    protected String getBackButtonLabel() {
        return getBackButtonLabel("breadcrumbs.webservices.security.token.service");
    }
}
