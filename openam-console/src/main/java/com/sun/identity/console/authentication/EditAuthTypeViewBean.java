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
 * $Id: EditAuthTypeViewBean.java,v 1.2 2008/06/25 05:42:45 qcheng Exp $
 *
 */

package com.sun.identity.console.authentication;

import com.iplanet.jato.NavigationException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.authentication.model.AuthPropertiesModel;
import com.sun.identity.console.authentication.model.AuthPropertiesModelImpl;
import com.sun.identity.console.authentication.model.AuthProfileModelImpl;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.text.MessageFormat;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class EditAuthTypeViewBean
    extends AMServiceProfileViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/authentication/EditAuthType.jsp";

    public static final String SERVICE_TYPE = "authServiceType";

    private AuthPropertiesModel authModel = null;

    /**
     * Creates a authentication module edit view bean.
     */
    public EditAuthTypeViewBean() {
        super("EditAuthType", DEFAULT_DISPLAY_URL, null);

        String type = (String)getPageSessionAttribute(SERVICE_TYPE);
        if (type != null) {

            /* 
             * The name of the service will return null if the entry no
             * longer exists. This can happen if someone is viewing the
             * properties of a service, and another person deletes the
             * entry. Any request on that page will result in an error
             * without a valid service name. To avoid the error we set the
             * service name to the default core authentication service.
             */
            String name = getServiceName(type);
            if (name == null) {
                name = AMAdminConstants.CORE_AUTH_SERVICE;
                debug.warning("EditAuthTypeViewBean() " +
                    "The auth instance could not be found. The instance name" +
                    " has been reset to " + name);
                
            }  
            initialize(name);
        }
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        String instance = (String)getPageSessionAttribute(SERVICE_TYPE);

        AuthPropertiesModel model = getAuthModel();
        if (model != null) {
            AMPropertySheet ps = (AMPropertySheet)getChild(
                PROPERTY_ATTRIBUTE);
            Map map = model.getInstanceValues(instance);
            ps.setAttributeValues(map, model);
        }
    }

    public void forwardTo(RequestContext reqContext)
        throws NavigationException {
        String name = getServiceName(
            (String)getPageSessionAttribute(SERVICE_TYPE));
        initialize(name);
        super.forwardTo(reqContext);
    }

    protected String getBackButtonLabel() {
        return getBackButtonLabel("config.auth.label");
    }

    /**
     * Handles save request.
     *
     * @param event Request invocation event.
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        String instance = (String)getPageSessionAttribute(SERVICE_TYPE);
    
        AuthPropertiesModel model = getAuthModel();
        /*
         * The service name will be null if the entry was deleted by
         * another user while the properties were being viewed.
         */
        if (getServiceName(instance) == null) {
            returnToAuthProperties(
                model.getLocalizedString("no.module.instance"));
        } else {
            if (model != null) {
                try {
                    Map values = getValues();
                    model.setInstanceValues(instance, values);
                    setInlineAlertMessage(CCAlert.TYPE_INFO, 
                        "message.information", "message.updated");
                } catch (AMConsoleException e) {
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        e.getMessage());
                }
            }
            forwardTo();
        }
    }

    /**
     * Handles save request.
     * 
     * @param event Request invocation event.
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException
    {  
        String instance = (String)getPageSessionAttribute(SERVICE_TYPE);

        /*                                                               
         * The service name will be null if the entry was deleted by
         * another user while the properties were being viewed.
         */
        if (getServiceName(instance) == null) {
            debug.warning("EditAuthTypeViewBean.handleButton2Request() " +
                "The instance " + instance + " could not be found");
            AuthPropertiesModel model = getAuthModel();
            returnToAuthProperties(
                model.getLocalizedString("no.module.instance"));
        } else {
            super.handleButton2Request(event);
        }
    }  

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        returnToAuthProperties(null);
    }

    private void returnToAuthProperties(String message) {
        backTrail();
        AuthPropertiesViewBean vb = (AuthPropertiesViewBean)
            getViewBean(AuthPropertiesViewBean.class);
        if (message != null) {
            vb.setPageSessionAttribute(
                AuthPropertiesViewBean.INSTANCE_MSG, message);
        }
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected Map getValues()
        throws ModelControlException, AMConsoleException
    {
        Map values = null;
        String instance = (String)getPageSessionAttribute(SERVICE_TYPE);

        AuthPropertiesModel model = getAuthModel();
        if (model != null) {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            values = ps.getAttributeValues(
                model.getInstanceValues(instance), model);
        }

        return values;
    }

    /*
     * Returns the real service name for a given authentication type
     */
    private String getServiceName(String type) {
        AuthPropertiesModel m = getAuthModel();
        return m.getServiceName(type);
    }

    private AuthPropertiesModel getAuthModel() {
        if (authModel == null) {
            RequestContext rc = RequestManager.getRequestContext();
            authModel = new AuthPropertiesModelImpl(
                rc.getRequest(), getPageSessionAttributes());
        }
        return authModel;
    }

    protected String getBreadCrumbDisplayName() {
        String instance = (String)getPageSessionAttribute(SERVICE_TYPE);
        String[] arg = {instance};
        AuthPropertiesModel model = getAuthModel();
        return MessageFormat.format(model.getLocalizedString(
            "breadcrumbs.auth.editInstance"), (Object[])arg);
    }

    protected boolean startPageTrail() {
        return false;
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        try {
            return new AuthProfileModelImpl(
                req, serviceName, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        return null;
    }
}
