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
 * $Id: ServicesEditViewBean.java,v 1.3 2008/06/25 05:42:59 qcheng Exp $
 *
 */

package com.sun.identity.console.idm;

import com.iplanet.jato.NavigationException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.idm.model.EntitiesModel;
import com.sun.identity.console.idm.model.EntitiesModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.text.MessageFormat;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class ServicesEditViewBean
    extends ServiceViewBeanBase {
    public static final String DEFAULT_DISPLAY_URL =
        "/console/idm/ServicesEdit.jsp";

    /**
     * Creates a service profile view bean.
     */
    public ServicesEditViewBean() {
        super("ServicesEdit", DEFAULT_DISPLAY_URL, null);
        String lserviceName = (String)getPageSessionAttribute(SERVICE_NAME);
        if (lserviceName != null) {
            initialize(lserviceName);
        }
    }

    public void forwardTo(RequestContext reqContext)
        throws NavigationException {
        String serviceName = (String)getPageSessionAttribute(SERVICE_NAME);
        EntitiesModel model = (EntitiesModel)getModel();

        // Check if the selected service has attributes to display.
        if (model.hasDisplayableAttributes(serviceName)){
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
        return new EntitiesModelImpl(req, getPageSessionAttributes());
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", "button.back");
    }

    /**
     * Handles add service request.
     *
     * @param event Request invocation event.
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        submitCycle = true;
        EntitiesModel model = (EntitiesModel)getModel();

        try {
            Map values = getValues();
            String universalId = (String)getPageSessionAttribute( 
                EntityEditViewBean.UNIVERSAL_ID);
            String serviceName = (String)getPageSessionAttribute(SERVICE_NAME);
            model.setServiceAttributeValues(universalId, serviceName, values);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "message.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        forwardTo();
    }

    /**
     * Handles back to previous view request.
     *
     * @param event Request invocation event.
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        EntityServicesViewBean vb = (EntityServicesViewBean)getViewBean(
            com.sun.identity.console.idm.EntityServicesViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected String getPageTitle() {
        EntitiesModel model = (EntitiesModel)getModel();
        String lserviceName = model.getLocalizedServiceName(
            (String)getPageSessionAttribute(SERVICE_NAME));
        String[] param = {lserviceName};
        return MessageFormat.format(
            model.getLocalizedString("page.title.entities.editservice"), (Object[])param);
    }

    protected boolean isCreateViewBean() {
        return false;
    }

    protected String getBreadCrumbDisplayName() {
        EntitiesModel model = (EntitiesModel)getModel();
        String lserviceName = model.getLocalizedServiceName(
            (String)getPageSessionAttribute(SERVICE_NAME));
        String[] param = {lserviceName};
        return MessageFormat.format(model.getLocalizedString(
            "breadcrumbs.editentities.editservice"), (Object[])param);
    }

    protected boolean startPageTrail() {
        return false;
    }
}
