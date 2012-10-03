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
 * $Id: SCConfigViewBean.java,v 1.4 2008/06/25 05:43:15 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.View;
import com.sun.identity.console.base.AMPostViewBean;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMServiceProfile;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.realm.ServicesNoAttributeViewBean;
import com.sun.identity.console.service.model.SCConfigModel;
import com.sun.identity.console.service.model.SCConfigModelImpl;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.model.CCPropertySheetModel;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.propertysheet.CCPropertySheet;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public abstract class SCConfigViewBean extends AMPrimaryMastHeadViewBean {

    private static final String PAGETITLE = "pgtitle";
    private static final String TBL_COL_NAME = "tblColName";
    private static final String TBL_DATA_NAME_PREFIX = "tblDataName";
    private static final String PROPERTY_SHEET_NAME = "psSections";

    protected static final String TBL_HREF_PREFIX = "tblHref";

    private CCPageTitleModel ptModel;
    protected CCPropertySheetModel psModel;

    public SCConfigViewBean(String name, String url) {
        super(name);
        setDefaultDisplayURL(url);
    }

    protected void initialize() {
        if (!initialized) {
            super.initialize();
            ptModel = new CCPageTitleModel(getClass().getClassLoader().
                getResourceAsStream(
                "com/sun/identity/console/simplePageTitle.xml"));
            createPropertyModel();
            registerChildren();
            initialized = true;
        }
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PAGETITLE, CCPageTitle.class);
        registerChild(PROPERTY_SHEET_NAME, CCPropertySheet.class);
        ptModel.registerChildren(this);
        psModel.registerChildren(this);
    }

    protected View createChild(String name) {
        if (PROPERTY_SHEET_NAME.equals(name)) {
            return new CCPropertySheet(this, psModel, name);
        } else if (PAGETITLE.equals(name)) {
            return new CCPageTitle(this, ptModel, name);
        } else if (psModel.isChildSupported(name)) {
            return psModel.createChild(this, name);
        } else if (ptModel.isChildSupported(name)) {
            return ptModel.createChild(this, name);
        } else {
            return super.createChild(name);
        }
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new SCConfigModelImpl(req, getPageSessionAttributes());
    }

    
    
    protected void populateTableModel(CCActionTableModel tblModel,
        List serviceNames, String section)
    {
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_COL_NAME, "service.name");
        SCConfigModel model = (SCConfigModel)getModel();
        if ((serviceNames != null) && !serviceNames.isEmpty()) {
            int size = serviceNames.size();
            for (int i = 0; i < size; i++) {
                if (i > 0) {
                    tblModel.appendRow();
                }
                String name = (String)serviceNames.get(i);
                tblModel.setValue(TBL_DATA_NAME_PREFIX + section,
                    model.getLocalizedServiceName(name));
                tblModel.setValue(TBL_HREF_PREFIX + section, name);
            }
        }
    }

    protected void forwardToProfile(String serviceName) {
        SCConfigModel model = (SCConfigModel)getModel();
        setPageSessionAttribute(AMServiceProfile.SERVICE_NAME, serviceName);
        unlockPageTrail();
        String url = model.getServicePropertiesViewBeanURL(serviceName);

        if ((url != null) && (url.length() != 0)) {
            AMPostViewBean vb = (AMPostViewBean)getViewBean(
            AMPostViewBean.class);
                passPgSessionMap(vb);
            vb.setTargetViewBeanURL(url);
            vb.forwardTo(getRequestContext());
        } else {
            // check if this service has any attributes to display.
            if (model.hasConfigAttributes(serviceName)) {
                SCServiceProfileViewBean vb = (SCServiceProfileViewBean)
                    getViewBean(SCServiceProfileViewBean.class);
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            } else {                
                setPageSessionAttribute(
                    AMAdminConstants.SAVE_VB_NAME, getClass().getName());
                ServicesNoAttributeViewBean vb = (ServicesNoAttributeViewBean)
                    getViewBean(ServicesNoAttributeViewBean.class);
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            }
        }
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.services.config";
    }
    
    protected abstract void createPropertyModel();
    
    protected abstract void createTableModels();
}
