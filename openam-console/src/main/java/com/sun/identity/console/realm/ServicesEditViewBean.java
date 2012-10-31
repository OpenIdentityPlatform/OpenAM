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
 * $Id: ServicesEditViewBean.java,v 1.2 2008/06/25 05:43:12 qcheng Exp $
 *
 */

package com.sun.identity.console.realm;

import com.iplanet.jato.NavigationException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.realm.model.ServicesProfileModel;
import com.sun.identity.console.realm.model.ServicesProfileModelImpl;
import com.sun.web.ui.view.alert.CCAlert;
import java.text.MessageFormat;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class ServicesEditViewBean
    extends AMServiceProfileViewBeanBase {
    public static final String DEFAULT_DISPLAY_URL =
        "/console/realm/ServicesEdit.jsp";

    public String location = null;

    protected ServicesEditViewBean(String view, String url) {
        super(view, url, null);
        serviceName = (String)getPageSessionAttribute(SERVICE_NAME);
    }

    /**
     * Creates a service profile view bean.
     */
    public ServicesEditViewBean() {
        super("ServicesEdit", DEFAULT_DISPLAY_URL, null);
        serviceName = (String)getPageSessionAttribute(SERVICE_NAME);
    }

    public void forwardTo(RequestContext reqContext)
        throws NavigationException {
        String serviceName = (String)getPageSessionAttribute(SERVICE_NAME);
        ServicesProfileModel model = (ServicesProfileModel)getModel();

        if (model.hasOrganizationAttributes()) {
            initialize(serviceName);
            super.forwardTo(reqContext);
        } else {
            ServicesNoAttributeViewBean vb = (ServicesNoAttributeViewBean)
                getViewBean(ServicesNoAttributeViewBean.class);
            passPgSessionMap(vb);
            vb.forwardTo(reqContext);
        }
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        String serviceName = (String)getPageSessionAttribute(SERVICE_NAME);
        if (location == null) {
            location = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
        }
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
     * Handles add service request.
     *
     * @param event Request invocation event.
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        submitCycle = true;
        ServicesProfileModel model = (ServicesProfileModel)getModel();

        if (model != null) {
            try {
                Map values = getValues();
                model.setAttributes(values);
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "message.updated");
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
        }
        forwardTo();
    }

    // button2 request handled by AMServiceProfileViewBean

    /**
     * Handles request to go back to previous page. This will result in going
     * back to the realm properties view.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        backTrail();
        ServicesViewBean vb = (ServicesViewBean)
            getViewBean(ServicesViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected void forwardToServicesViewBean() {
        AMViewBeanBase vb = getCallingView();
        if (vb != null) {
            backTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } else {
            forwardTo();
        }
    }

    protected Map getValues()
        throws ModelControlException, AMConsoleException {
        Map values = null;
        ServicesProfileModel model = (ServicesProfileModel)getModel();
        if (model != null) {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            values = ps.getAttributeValues(model.getAttributeValues(), model);
        }
        return values;
    }

    protected String getBreadCrumbDisplayName() {
        ServicesProfileModel model = (ServicesProfileModel)getModel();
        String[] arg = {model.getPageTitle()};
        return MessageFormat.format(model.getLocalizedString(
            "breadcrumbs.realm.services.editService"), (Object[])arg);
    }

    protected String getBackButtonLabel() {
        return getBackButtonLabel("page.title.services");
    }

    protected boolean startPageTrail() {
        return false;
    }
}
