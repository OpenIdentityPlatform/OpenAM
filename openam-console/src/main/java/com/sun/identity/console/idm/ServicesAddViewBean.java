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
 * $Id: ServicesAddViewBean.java,v 1.2 2008/06/25 05:42:59 qcheng Exp $
 *
 */

package com.sun.identity.console.idm;

import com.iplanet.jato.NavigationException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.idm.model.EntitiesModel;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

public class ServicesAddViewBean
    extends ServiceViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/idm/ServicesAdd.jsp";

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

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.back");
        ptModel.setValue("button2", "button.finish");
        ptModel.setValue("button3", "button.cancel");
    }

    public void forwardTo(RequestContext reqContext)
        throws NavigationException {
        String lserviceName = (String)getPageSessionAttribute(SERVICE_NAME);
        initialize(lserviceName);
        super.forwardTo(reqContext);
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
        EntitiesModel model = (EntitiesModel)getModel();

        try {
            Map values = getValues();
            String universalId = (String)getPageSessionAttribute(
                EntityEditViewBean.UNIVERSAL_ID);
            String serviceName = (String)getPageSessionAttribute(SERVICE_NAME);
            model.assignService(universalId, serviceName, values);
            forwardToServicesViewBean();
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }

    protected Map getAttributeValues()
        throws ModelControlException, AMConsoleException {
        EntitiesModel model = (EntitiesModel)getModel();
        String serviceName = (String)getPageSessionAttribute(SERVICE_NAME);
        String universalId = (String)getPageSessionAttribute(
            EntityEditViewBean.UNIVERSAL_ID);

        try {
            AMIdentity amid = IdUtils.getIdentity(
                model.getUserSSOToken(), universalId);
            return model.getDefaultValues(
                amid.getType().getName(), serviceName);
        } catch (IdRepoException e) {
            return Collections.EMPTY_MAP;
        }
    }

    protected String getPageTitle() {
        EntitiesModel model = (EntitiesModel)getModel();
        String lserviceName = model.getLocalizedServiceName(
            (String)getPageSessionAttribute(SERVICE_NAME));
        String[] param = {lserviceName};
        return MessageFormat.format(
            model.getLocalizedString("page.title.entities.addservice"), (Object[])param);
    }

    protected boolean isCreateViewBean() {
        return true;
    }

    protected Map getValues() 
        throws ModelControlException, AMConsoleException
    {        
        Map values = null;
        EntitiesModel model = (EntitiesModel)getModel();
        String lserviceName = (String)getPageSessionAttribute(SERVICE_NAME);
        String universalId = (String)getPageSessionAttribute(
            EntityEditViewBean.UNIVERSAL_ID);
        Map defaultValues = null;

        try {
            defaultValues = model.getServiceAttributeValues(
                universalId, lserviceName);
            if (defaultValues.isEmpty()) {
                defaultValues = getDefaultValuesForIdentity(universalId, model);
            }
        } catch (AMConsoleException e) {    
            defaultValues = getDefaultValuesForIdentity(universalId, model);
        }

        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        return ps.getAttributeValues(defaultValues, model);
    }

    private Map getDefaultValuesForIdentity(
        String universalId,
        EntitiesModel model
    ) {
        Map defaultValues = null;
        try {
            AMIdentity amid = IdUtils.getIdentity(
                model.getUserSSOToken(), universalId);
            defaultValues = model.getDefaultValues(
                amid.getType().getName(), serviceName);
            AMAdminUtils.makeMapValuesEmpty(defaultValues);
        } catch (AMConsoleException e) {
            defaultValues = Collections.EMPTY_MAP;
        } catch (IdRepoException e) {
            defaultValues = Collections.EMPTY_MAP;
        }
        return defaultValues;
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.editentities.addservice";
    }

    protected boolean startPageTrail() {
        return false;
    }
}
