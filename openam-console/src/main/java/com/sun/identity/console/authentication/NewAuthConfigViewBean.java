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
 * $Id: NewAuthConfigViewBean.java,v 1.2 2008/06/25 05:42:45 qcheng Exp $
 *
 */

package com.sun.identity.console.authentication;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.authentication.model.AuthConfigurationModel;
import com.sun.identity.console.authentication.model.AuthConfigurationModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;

public class NewAuthConfigViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/authentication/NewAuthConfig.jsp";

    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String CONFIG_NAME = "configName";

    private AMPropertySheetModel propertySheetModel;
    private CCPageTitleModel ptModel;
    private AuthConfigurationModel acModel = null;

    /**
     * Creates a realm creation view bean.
     */
    public NewAuthConfigViewBean() {
        super("NewAuthConfig");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        propertySheetModel.registerChildren(this);
        ptModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE_TWO_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);

        acModel = (AuthConfigurationModel)getModel();
    }

    /**
     * Handles create request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) {
        String name = (String)getDisplayFieldValue(CONFIG_NAME);
        acModel = (AuthConfigurationModel)getModel();
        try {
            acModel.createAuthConfiguration(name);
            setInlineAlertMessage(CCAlert.TYPE_INFO,
                "message.information",
                "authentication.configuration.created");

            setPageSessionAttribute(
                AuthConfigurationModelImpl.CONFIG_NAME, name);

            AuthConfigViewBean vb = (AuthConfigViewBean)
                getViewBean(AuthConfigViewBean.class);
            unlockPageTrailForSwapping();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException amc) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR,
                "message.error", amc.getMessage());
            forwardTo();
        }
    }    
        
    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        AuthPropertiesViewBean vb = (AuthPropertiesViewBean)
            getViewBean(AuthPropertiesViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.cancel");
    }

    protected AMModel getModelInternal() {
        if (acModel == null) {
            RequestContext rc = RequestManager.getRequestContext();
            acModel = new AuthConfigurationModelImpl(
                rc.getRequest(), getPageSessionAttributes());
        }
        return acModel;
    }

    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/propertyNewAuthConfig.xml"));
        propertySheetModel.clear();
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.auth.newConfiguration";
    }

    protected boolean startPageTrail() {
        return false;
    }
}
