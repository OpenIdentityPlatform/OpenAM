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
 * $Id: IDRepoOpViewBeanBase.java,v 1.2 2008/06/25 05:43:11 qcheng Exp $
 *
 */

package com.sun.identity.console.realm;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.realm.model.IDRepoModel;
import com.sun.identity.console.realm.model.IDRepoModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public abstract class IDRepoOpViewBeanBase
    extends AMPrimaryMastHeadViewBean
{
    protected static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    protected static final String IDREPO_NAME = "tfIdRepoName";
    protected static final String IDREPO_TYPE = "idRepoTypeName";
    protected static final String IDREPO_TYPE_NAME = "tfIdRepoTypeName";

    protected CCPageTitleModel ptModel;
    protected boolean submitCycle;
    public AMPropertySheetModel propertySheetModel;

    public IDRepoOpViewBeanBase(String name, String defaultDisplayURL) {
        super(name);
        setDefaultDisplayURL(defaultDisplayURL);
    }

    protected void initialize() {
        if (!initialized) {
            initialized = createPropertyModel();

            if (initialized) {
                super.initialize();
                createPageTitleModel();
                registerChildren();
            }
        }
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new IDRepoModelImpl(req, getPageSessionAttributes());
    }

    protected boolean createPropertyModel() {
        boolean created = false;
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        String type = (String)getPageSessionAttribute(IDREPO_TYPE);
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);

        if ((type != null) && (type.trim().length() > 0)) {
            IDRepoModel model = (IDRepoModel)getModel();
            try {
                propertySheetModel = new AMPropertySheetModel(
                    model.getPropertyXMLString(realmName, getClass().getName(),
                        type, isCreateViewBean()));
                propertySheetModel.clear();
                created = true;
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
        }

        return created;
    }

    protected void registerChildren() {
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
        super.registerChildren();
    }

    protected View createChild(String name) {
        View view = null;

        if ((ptModel != null) && ptModel.isChildSupported(name)) {
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

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        String idRepoName = (String)getPageSessionAttribute(
            IDREPO_NAME);

        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        IDRepoModel model = (IDRepoModel)getModel();
        try {
            Map localizedMap = model.getIDRepoTypesMap();
            String idRepoType = (String)getPageSessionAttribute(IDREPO_TYPE);
            String i18nName = (String)localizedMap.get(idRepoType);

            if (!submitCycle) {
                setDefaultValues(idRepoType);
            }

            propertySheetModel.setValue(IDREPO_TYPE, idRepoType);
            propertySheetModel.setValue(IDREPO_TYPE_NAME, i18nName);
            propertySheetModel.setValue(IDREPO_NAME, idRepoName);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    protected void forwardToIDRepoViewBean() {
        IDRepoViewBean vb = (IDRepoViewBean)getViewBean(IDRepoViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        forwardToIDRepoViewBean();
    }

    protected abstract void createPageTitleModel();
    protected abstract void setDefaultValues(String type);
    protected abstract boolean isCreateViewBean();
}
