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
 * $Id: CoreAttributesViewBean.java,v 1.2 2008/06/25 05:42:45 qcheng Exp $
 *
 */

package com.sun.identity.console.authentication;

import com.iplanet.jato.NavigationException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.ViewBean;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.authentication.model.CoreAttributesModel;
import com.sun.identity.console.authentication.model.CoreAttributesModelImpl;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class CoreAttributesViewBean
    extends AMServiceProfileViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/authentication/CoreAttributes.jsp";

    /**
     * Creates a authentication module edit view bean.
     */
    public CoreAttributesViewBean() {
        super("CoreAttributes", DEFAULT_DISPLAY_URL, 
            AMAdminConstants.CORE_AUTH_SERVICE);
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
    }

    public void forwardTo(RequestContext reqContext)
        throws NavigationException {
        super.forwardTo(reqContext);
    }

    protected void initialize() {
        initialize(AMAdminConstants.CORE_AUTH_SERVICE);
    }

    protected void initialize(String serviceName) {
        String loc = (String)getPageSessionAttribute(
            AMAdminConstants.CONSOLE_LOCATION_DN);

        if (!initialized && loc != null) {
            super.initialize();
            getModel().setLocationDN(loc);
            initialized = true;
            this.serviceName = serviceName;
            createPageTitleModel();
            createPropertyModel();
            registerChildren();
        }
    }

    protected String getBackButtonLabel() {
        return getBackButtonLabel("config.auth.label");
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event.
     */
    public void handleButton1Request(RequestInvocationEvent event) 
        throws ModelControlException
    {
        try {
            CoreAttributesModel model = (CoreAttributesModel)getModel();
            Map original = model.getAttributeValues();
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            model.setAttributeValues(ps.getAttributeValues(
                original, true, model));
            setInlineAlertMessage(CCAlert.TYPE_INFO,
                "message.information", "message.updated");
            forwardTo();
        } catch (AMConsoleException a) {
            setInlineAlertMessage(
                CCAlert.TYPE_ERROR, "message.error", a.getMessage());
            forwardTo();
        }
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event.
     */
    public void handleButton3Request(RequestInvocationEvent event) 
        throws ModelControlException
    {
        backTrail();
        ViewBean vb = (AuthPropertiesViewBean)getViewBean(
           AuthPropertiesViewBean.class);

        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        try {
            return new CoreAttributesModelImpl(
                req, "iPlanetAMAuthService", getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        return null;
    }

    protected Map getValues()
        throws ModelControlException, AMConsoleException
    {
        Map values = null;
        CoreAttributesModel model = (CoreAttributesModel)getModelInternal();

        if (model != null) {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            values = ps.getAttributeValues(model.getAttributeValues(), model);
        }

        return values;
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.auth.advanced.properties";
    }

    protected boolean startPageTrail() {
        return false;
    }

}
