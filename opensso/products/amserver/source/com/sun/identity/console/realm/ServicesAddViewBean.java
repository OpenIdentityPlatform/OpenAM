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
 * $Id: ServicesAddViewBean.java,v 1.2 2008/06/25 05:43:12 qcheng Exp $
 *
 */

package com.sun.identity.console.realm;

import com.iplanet.jato.NavigationException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.realm.model.ServicesProfileModel;
import com.sun.identity.console.realm.model.ServicesProfileModelImpl;
import com.sun.web.ui.view.alert.CCAlert;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class ServicesAddViewBean
    extends AMServiceProfileViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/realm/ServicesAdd.jsp";

    /**
     * Creates a service profile view bean.
     */
    public ServicesAddViewBean() {
        super("ServicesAdd", DEFAULT_DISPLAY_URL, null);
        String serviceName = (String)getPageSessionAttribute(SERVICE_NAME);
        if (serviceName != null) {
            initialize(serviceName);
        }
    }

    public void forwardTo(RequestContext reqContext)
        throws NavigationException {
        String serviceName = (String)getPageSessionAttribute(SERVICE_NAME);
        ServicesProfileModel model = (ServicesProfileModel)getModel();

        if (model.hasOrganizationAttributes()) {
            initialize(serviceName);
            super.forwardTo(reqContext);
        } else {
            try {
                model.assignService(Collections.EMPTY_MAP);
                forwardToServicesViewBean();
            } catch (AMConsoleException e) {
                ServicesCannotAssignServiceViewBean vb =
                    (ServicesCannotAssignServiceViewBean)getViewBean(
                        ServicesCannotAssignServiceViewBean.class);
                passPgSessionMap(vb);
                vb.message = e.getMessage();
                vb.forwardTo(reqContext);
            } 
        }
    }

    protected void createPageTitleModel() {
        super.createThreeButtonPageTitleModel();
        ServicesProfileModel model = (ServicesProfileModel)getModel();
        String[] arg = {model.getLocalizedServiceName(serviceName)};
        ptModel.setPageTitleText(MessageFormat.format(model.getLocalizedString(
            "page.title.services.add"), (Object[])arg));
        ptModel.setValue("button1", "button.back");
        ptModel.setValue("button2", "button.finish");
        ptModel.setValue("button3", "button.cancel");
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        String serviceName = (String)getPageSessionAttribute(SERVICE_NAME);
        String location = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);

        try {
            return new ServicesProfileModelImpl(
                req, serviceName, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        return null;
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event.
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        forwardToServicesViewBean();
    }

    public void handleButton1Request(RequestInvocationEvent event) {
        ServicesSelectViewBean vb = (ServicesSelectViewBean)getViewBean(
            ServicesSelectViewBean.class);
        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles add service request.
     *
     * @param event Request invocation event.
     */
    public void handleButton2Request(RequestInvocationEvent event) 
        throws ModelControlException {
        submitCycle = true;
        ServicesProfileModel model = (ServicesProfileModel)getModel();

        if (model != null) {
            try {
                Map values = getValues();
                model.assignService(values);
                forwardToServicesViewBean();
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                forwardTo();
            }
        } else {
            forwardTo();
        }
    }

    private void forwardToServicesViewBean() {
        ServicesViewBean vb = (ServicesViewBean)getViewBean(
            ServicesViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        if (!submitCycle) {
            ServicesProfileModel model = (ServicesProfileModel)getModel();
            Map map = model.getDefaultAttributeValues();
                                                                                
            if (map != null) {
                AMPropertySheet ps = (AMPropertySheet)getChild(
                    PROPERTY_ATTRIBUTE);
                ps.setAttributeValues(map, model);
            }
        }
    }

    protected Map getValues()
        throws ModelControlException, AMConsoleException {
        Map values = null;
        ServicesProfileModel model = (ServicesProfileModel)getModel();
        if (model != null) {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            /*
             * 20050426 Dennis
             * false means returns every attribute values and not the
             * modified ones
             */
            values = ps.getAttributeValues(
                model.getDefaultAttributeValues(), false, model);
        }
        return values;
    }

    protected Map getAttributeValues() {
        ServicesProfileModel model = (ServicesProfileModel)getModel();
        Map values = model.getDefaultAttributeValues();
        return (values != null) ? values : Collections.EMPTY_MAP;
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.realm.services.addService";
    }

    protected boolean startPageTrail() {
        return false;
    }
}
