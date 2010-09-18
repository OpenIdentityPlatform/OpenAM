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
 * $Id: AMServiceProfileViewBeanBase.java,v 1.3 2009/11/10 04:16:17 bhavnab Exp $
 *
 */


package com.sun.identity.console.base;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMServiceProfileModel;
import com.sun.identity.console.base.model.AMServiceProfileModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;


public abstract class AMServiceProfileViewBeanBase
    extends AMPrimaryMastHeadViewBean
{
    private static final String PGTITLE = "pgtitle";
    public static final String SERVICE_NAME = "serviceName";
    public static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    protected CCPageTitleModel ptModel;
    protected boolean initialized;
    protected boolean submitCycle;
    protected AMPropertySheetModel propertySheetModel;
    protected String serviceName;

    /**
     * Creates a service profile view bean.
     *
     * @param name Name of view bean.
     * @param url Default display URL.
     * @param serviceName Name of service.
     */
    public AMServiceProfileViewBeanBase(
        String name,
        String url,
        String serviceName
    ) {
        super(name);
        setDefaultDisplayURL(url);

        if (serviceName != null) {
            initialize(serviceName);
        }
    }

    public void setRequestContext(RequestContext rc) {
        super.setRequestContext(rc);
        if (serviceName != null) {
            initialize(serviceName);
        }
    }

    protected void initialize(String serviceName) {
        if (!initialized) {
            initialized = true;
            this.serviceName = serviceName;
            createPageTitleModel();
            createPropertyModel();
            registerChildren();
        }
    }

    protected void registerChildren() {
        super.registerChildren();
        ptModel.registerChildren(this);
        registerChild(PGTITLE, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        if (propertySheetModel != null) {
            propertySheetModel.registerChildren(this);
        }
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if ((propertySheetModel != null) &&
            propertySheetModel.isChildSupported(name)
        ) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void setSubmitCycle(boolean cycle) {
        submitCycle = cycle;
    }

    protected boolean isSubmitCycle() {
        return submitCycle;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);

        if (propertySheetModel != null) {
            if (!submitCycle) {
                Map map = getAttributeValues();
                if (map != null) {
                    AMPropertySheet ps = (AMPropertySheet)getChild(
                        PROPERTY_ATTRIBUTE);
                    ps.setAttributeValues(map, getModel());
                }
            }
        } else {
            disableButton("button1", true);
            disableButton("button2", true);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "propertysheet.no.attributes.message");
        }
    }

    public boolean beginPropertyDisplay(ChildDisplayEvent event) {
        return (propertySheetModel != null);
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        try {
            return new AMServiceProfileModelImpl(
                req, serviceName, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        return null;
    }

    protected void createPageTitleModel() {
        createThreeButtonPageTitleModel();
    }

    protected void createTwoButtonPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));

        AMServiceProfileModel model = (AMServiceProfileModel)
            getModel();

        if (model != null) {
            ptModel.setPageTitleText(model.getPageTitle());
            ptModel.setValue("button1", "button.save");
            ptModel.setValue("button2", "button.reset");
        }
    }

    protected void createThreeButtonPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));

        AMServiceProfileModel model = (AMServiceProfileModel)
            getModel();

        if (model != null) {
            ptModel.setPageTitleText(model.getPageTitle());
            ptModel.setValue("button1", "button.save");
            ptModel.setValue("button2", "button.reset");
            ptModel.setValue("button3", getBackButtonLabel());
        }
    }

    /*
     * Overridden in the following classes
     *     /authentication/CoreAttributesViewBean
     *     /authentication/EditAuthTypeViewBean
     *     /realm/ServicesEditViewBean
     */
    protected String getBackButtonLabel() {
        return getBackButtonLabel("breadcrumbs.services.config");
    }

    protected void createPropertyModel() {
        AMServiceProfileModel model = (AMServiceProfileModel)getModel();

        if (model != null) {
            try {
                propertySheetModel = new AMPropertySheetModel(
                    getPropertySheetXML(model));
                propertySheetModel.clear();
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
        }
    }

    protected String getPropertySheetXML(AMServiceProfileModel model)
        throws AMConsoleException {
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        return model.getPropertySheetXML(realmName, getClass().getName(),
            serviceName);
    }

    /**
     * Handles save request.
     *
     * @param event Request invocation event.
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        submitCycle = true;
        AMServiceProfileModel model = (AMServiceProfileModel)getModel();

        if (model != null) {
            try {
                Map values = getValues();
                onBeforeSaveProfile(values);
                model.setAttributeValues(values);
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "message.updated");
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
        }
        forwardTo();
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        onBeforeResetProfile();
        propertySheetModel.clear();
        forwardTo();
    }

    protected Map getValues()
        throws ModelControlException, AMConsoleException
    {
        Map values = null;
        AMServiceProfileModel model = (AMServiceProfileModel)getModel();

        if (model != null) {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            values = ps.getAttributeValues(model.getAttributeValues(), model);
        }

        return values;
    }

    /**
     * Dervived class can overwrite this method to alter attribute values
     * before are the saved.
     *
     * @return true to proceed with saving attribute values.
     */
    protected boolean onBeforeSaveProfile(Map attrValues) {
        return true;
    }

    /**
     * Dervived class can overwrite this method to perform some necessary
     * tasks before reseting profile.
     */
    protected void onBeforeResetProfile() {
    }

    /**
     * Handles link type attribute schema request.
     *
     * @param event Request invocation event.
     */
    public void handleDynLinkRequest(RequestInvocationEvent event) {
        submitCycle = true;
        RequestContext requestContext = getRequestContext();
        HttpServletRequest req = requestContext.getRequest();
        String attrName = req.getParameter("attrname");

        AMServiceProfileModel model = (AMServiceProfileModel)getModel();

        if (model != null) {
            try {
                String url = appendPgSession(
                    model.getPropertiesViewBean(attrName));
                requestContext.getResponse().sendRedirect(url);
            } catch (IOException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                forwardTo();
            }
        } else {
            forwardTo();
        }
    }

    protected Map getAttributeValues() {
        AMServiceProfileModel model = (AMServiceProfileModel)getModel();
        return (model != null) ? model.getAttributeValues() :
            Collections.EMPTY_MAP;
    }

    protected String getServiceNameForAccessControl() {
        return serviceName;
    }
}
