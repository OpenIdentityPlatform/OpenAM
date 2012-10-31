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
 * $Id: NewAuthInstanceViewBean.java,v 1.2 2008/06/25 05:42:45 qcheng Exp $
 *
 */

package com.sun.identity.console.authentication;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.authentication.model.AuthPropertiesModel;
import com.sun.identity.console.authentication.model.AuthPropertiesModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCRadioButton;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Map;


public class NewAuthInstanceViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/authentication/NewAuthInstance.jsp";

    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String INSTANCE_TYPE = "instanceType";
    private static final String INSTANCE_NAME = "instanceName";

    private AMPropertySheetModel propertySheetModel;
    private CCPageTitleModel ptModel;

    /**
     * Creates a realm creation view bean.
     */
    public NewAuthInstanceViewBean() {
        super("NewAuthInstance");
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
            if (name.equals(INSTANCE_TYPE)) {
                //set the available options for the radio button
                CCRadioButton radio = (CCRadioButton)view;
                radio.setOptions(createAuthTypeOptionList());
            }
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
    }

    /**
     * Handles create request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        String name = (String)getDisplayFieldValue(INSTANCE_NAME);
        String type = (String)getDisplayFieldValue(INSTANCE_TYPE);

        if (name == null || name.length() < 1) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR,
                "message.error", "authentication.missing.instance.name");
            forwardTo();
        } else if (type == null || type.length() < 1) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR,
                "message.error", "authentication.missing.instance.type");
            forwardTo();
        } else {
            AuthPropertiesModel model = (AuthPropertiesModel)getModel();
            try {
                model.createAuthInstance(name, type);
                forwardToProperties(event);
            } catch (AMConsoleException ae) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, 
                    "message.error", ae.getMessage());
                forwardTo();
            }
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
        forwardToProperties(event);
    }
    
    private void forwardToProperties(RequestInvocationEvent event) 
        throws ModelControlException
    {
        AuthPropertiesViewBean vb = (AuthPropertiesViewBean)
            getViewBean(AuthPropertiesViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    private OptionList createAuthTypeOptionList() {
           AuthPropertiesModel model = (AuthPropertiesModel)getModel();
           Map types = model.getAuthTypes();
           return createOptionList(types);
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.cancel");
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        return new AuthPropertiesModelImpl(
            rc.getRequest(), getPageSessionAttributes());
    }

    private void createPropertyModel() {
        AuthPropertiesModel model = (AuthPropertiesModel)getModel();
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/propertyNewAuthInstance.xml"));
        propertySheetModel.clear();
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.auth.newInstance";
    }

    protected boolean startPageTrail() {
        return false;
    }
}
