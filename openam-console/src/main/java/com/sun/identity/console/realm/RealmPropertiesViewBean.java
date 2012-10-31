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
 * $Id: RealmPropertiesViewBean.java,v 1.2 2008/06/25 05:43:12 qcheng Exp $
 *
 */

package com.sun.identity.console.realm;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.realm.model.RMRealmModel;
import com.sun.identity.console.realm.model.RMRealmModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class RealmPropertiesViewBean
    extends RealmPropertiesBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/realm/RealmProperties.jsp";

    private AMPropertySheetModel psModel;
    private static final String PAGETITLE = "pgtitle";
    protected static final String REALM_PROPERTIES = "realmProperties";
    private boolean submitCycle;
    private boolean initialized;

    /**
     * Creates a authentication domains view bean.
     */
    public RealmPropertiesViewBean() {
        super("RealmProperties");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            if (realmName != null) {
                initialized = true;
                createPropertyModel(realmName);
                createPageTitleModel();
                registerChildren();
                super.initialize();
            }
        }
        super.registerChildren();
    }

    protected void registerChildren() {
        registerChild(REALM_PROPERTIES, AMPropertySheet.class);
        psModel.registerChildren(this);
        ptModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PAGETITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(REALM_PROPERTIES)) {
            view = new AMPropertySheet(this, psModel, name);
        } else if ((psModel != null) && psModel.isChildSupported(name)) {
            view = psModel.createChild(this, name, getModel());
        } else if ((ptModel != null) && ptModel.isChildSupported(name)) {
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
        RMRealmModel model = (RMRealmModel)getModel();
        if (model != null) {
            if (!submitCycle) {
                String realm = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_REALM);
                AMPropertySheet ps =
                    (AMPropertySheet)getChild(REALM_PROPERTIES);
                psModel.clear();
                try {
                    ps.setAttributeValues(
                        model.getAttributeValues(realm), model);
                } catch (AMConsoleException a) {
                    setInlineAlertMessage(CCAlert.TYPE_ERROR,
                        "message.error", "no.properties");
                }
            }
            setPageTitle(getModel(), "page.title.realms.properties");
        }
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new RMRealmModelImpl(req, getPageSessionAttributes());
    }

    private void createPropertyModel(String realmName) {
        RMRealmModel model = (RMRealmModel)getModel();
        try {
            psModel = new AMPropertySheetModel(
                model.getRealmProfilePropertyXML(realmName,
                getClass().getName()));
            psModel.clear();
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    /**
     * Handles save button request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        submitCycle = true;
        RMRealmModel model = (RMRealmModel)getModel();
        String realm =
            (String)getPageSessionAttribute(AMAdminConstants.CURRENT_REALM);
        AMPropertySheet ps = (AMPropertySheet)getChild(REALM_PROPERTIES);

        try {
            Map orig = model.getAttributeValues(realm);
            Map values = ps.getAttributeValues(orig, true, true, model);
            model.setAttributeValues(realm, values);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "message.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        forwardTo();
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }

}
