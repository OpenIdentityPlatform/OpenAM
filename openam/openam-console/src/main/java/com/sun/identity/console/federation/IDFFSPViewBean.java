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
 * $Id: IDFFSPViewBean.java,v 1.12 2008/10/24 00:11:59 asyhuang Exp $
 *
 */
package com.sun.identity.console.federation;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.ContainerView;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.View;

import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMTableTiledView;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;

import com.sun.identity.console.federation.model.IDFFModel;
import com.sun.identity.console.federation.model.IDFFModelImpl;

import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.table.CCActionTable;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class IDFFSPViewBean
        extends IDFFViewBeanBase {

    public static final String DEFAULT_DISPLAY_URL =
            "/console/federation/IDFFSP.jsp";
    public static final String CHILD_AUTH_CONTEXT_TILED_VIEW = "tableTiledView";
    public static final String TBL_AUTHENTICATION_CONTEXTS =
            "tblAuthenticationContext";
    public static final String TBL_COL_SUPPORTED = "tblColSupported";
    public static final String TBL_COL_CONTEXT_REFERENCE =
            "tblColContextReference";
    public static final String TBL_COL_LEVEL = "tblColLevel";
    public static final String TBL_DATA_CONTEXT_REFERENCE =
            "tblDataContextReference";
    public static final String TBL_DATA_SUPPORTED = "tblDataSupported";
    public static final String TBL_DATA_LABEL = "tblDataLabel";
    public static final String TBL_DATA_LEVEL = "tblDataLevel";
    protected CCActionTableModel tblAuthContextsModel;

    public IDFFSPViewBean() {
        super("IDFFSP");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void registerChildren() {
        super.registerChildren();
        if (isHosted()) {
            registerChild(CHILD_AUTH_CONTEXT_TILED_VIEW,
                    AMTableTiledView.class);
        }
    }

    protected View createChild(String name) {
        View view = null;
        if (isHosted() && (name.equals(CHILD_AUTH_CONTEXT_TILED_VIEW))) {
            view = new AMTableTiledView(this, tblAuthContextsModel, name);
        } else if (isHosted() && (name.equals(TBL_AUTHENTICATION_CONTEXTS))) {
            CCActionTable child = new CCActionTable(
                    this, tblAuthContextsModel, name);
            child.setTiledView((ContainerView) getChild(
                    CHILD_AUTH_CONTEXT_TILED_VIEW));
            view = child;
        } else {
            view = super.createChild(name);
        }
        return view;
    }

    public void beginDisplay(DisplayEvent event)
            throws ModelControlException {
        super.beginDisplay(event);
        IDFFModel model =
                (IDFFModel) getModelInternal();
        psModel.setValue(IDFFModel.ATTR_PROVIDER_TYPE,
                (String) getPageSessionAttribute(ENTITY_LOCATION));
        populateValue(realm, entityName);
        if (isHosted()) {
            IDFFAuthContexts authContexts = null;
            try {
                authContexts = model.getSPAuthenticationContexts(
                        realm,
                        entityName);
            } catch (AMConsoleException e) {
                debug.warning("IDFFSPViewBean", e);
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        e.getMessage());
            }
            populateAuthenticationContext(authContexts);
        }
    }

    private void populateAuthenticationContext(IDFFAuthContexts authContexts) {
        List names = AUTH_CONTEXT_REF_NAMES;
        // We know that names from model contains 10 elements
        int sz = names.size();
        tblAuthContextsModel.clear();
        for (int i = 0; i < sz; i++) {
            String name = (String) names.get(i);
            populateAuthenticationContext(name, authContexts, i);
        }
    }

    private void populateAuthenticationContext(
            String name,
            IDFFAuthContexts authContexts,
            int index) {
        if (index != 0) {
            tblAuthContextsModel.appendRow();
        }

        IDFFModel model =
                (IDFFModel) getModelInternal();
        tblAuthContextsModel.setValue(TBL_DATA_CONTEXT_REFERENCE, name);
        tblAuthContextsModel.setValue(TBL_DATA_LABEL,
                model.getLocalizedString(getAuthContextI18nKey(name)));

        IDFFAuthContexts.IDFFAuthContext c = null;
        if (authContexts != null) {
            c = authContexts.get(name);
        }

        if (c == null) {
            tblAuthContextsModel.setValue(TBL_DATA_SUPPORTED, "");
            tblAuthContextsModel.setValue(TBL_DATA_LEVEL, "0");
        } else {
            tblAuthContextsModel.setValue(TBL_DATA_SUPPORTED, c.supported);
            tblAuthContextsModel.setValue(TBL_DATA_LEVEL, c.level);
        }
    }

    private String getAuthContextI18nKey(String name) {
        int idx = name.lastIndexOf("/");
        String key = (idx != -1) ? name.substring(idx + 1) : name;
        return "idff.authenticationContext." + key + ".label";
    }

    private void populateValue(String realm, String name) {
        try {
            IDFFModel model =
                    (IDFFModel) getModelInternal();
            Map values = model.getEntitySPDescriptor(realm, name);
            values.putAll(model.getSPEntityConfig(realm, name,
                    location));
            AMPropertySheet ps = (AMPropertySheet) getChild(PROPERTY_ATTRIBUTES);
            ps.setAttributeValues(values, model);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new IDFFModelImpl(req, getPageSessionAttributes());
    }

    protected void createPropertyModel() {
        retrieveCommonProperties();
        if (isHosted()) {
            psModel = new AMPropertySheetModel(
                    getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/propertyIDFFSPHosted.xml"));
            createAuthContextsModel();
            psModel.setModel(TBL_AUTHENTICATION_CONTEXTS,
                    tblAuthContextsModel);
        } else {
            psModel = new AMPropertySheetModel(
                    getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/propertyIDFFSPRemote.xml"));
        }

        psModel.clear();
    }

    private void createAuthContextsModel() {
        tblAuthContextsModel = new CCActionTableModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblIDFFSPAuthenticationContext.xml"));
        tblAuthContextsModel.setTitleLabel("label.items");
        tblAuthContextsModel.setActionValue(TBL_COL_SUPPORTED,
                "idff.sp.authenticationContext.table.name.supported.name");
        tblAuthContextsModel.setActionValue(TBL_COL_CONTEXT_REFERENCE,
                "idff.sp.authenticationContext.table.name.contextReference.name");
        tblAuthContextsModel.setActionValue(TBL_COL_LEVEL,
                "idff.sp.authenticationContext.table.name.level.name");
    }

    private IDFFAuthContexts getAuthenticationContexts()
            throws ModelControlException {
        CCActionTable tbl = (CCActionTable) getChild(
                TBL_AUTHENTICATION_CONTEXTS);
        tbl.restoreStateData();

        int size = 10;
        IDFFAuthContexts authContexts = new IDFFAuthContexts();

        for (int i = 0; i < size; i++) {
            tblAuthContextsModel.setLocation(i);
            String name = (String) tblAuthContextsModel.getValue(
                    TBL_DATA_CONTEXT_REFERENCE);
            String supported = (String) tblAuthContextsModel.getValue(
                    TBL_DATA_SUPPORTED);
            String level = (String) tblAuthContextsModel.getValue(
                    TBL_DATA_LEVEL);
            authContexts.put(name, supported, level);
        }
        return authContexts;
    }

    /**
     * Handles save
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
            throws ModelControlException {
        retrieveCommonProperties();
        try {
            IDFFModel model =
                    (IDFFModel) getModel();
            AMPropertySheet ps =
                    (AMPropertySheet) getChild(PROPERTY_ATTRIBUTES);

            // update standard metadata and extended metadata           
            Map stdValues = ps.getAttributeValues(
                    model.getEntitySPDescriptor(realm, entityName),
                    false,
                    model);
            Map extValues = ps.getAttributeValues(
                    model.getAllSPExtendedMetaMap(),
                    false,
                    model);
            Map spAllExtValues = model.getSPEntityConfig(
                    realm, 
                    entityName, 
                    ENTITY_LOCATION);
            spAllExtValues.putAll(extValues);
            model.updateEntitySPDescriptor(
                    realm,
                    entityName,
                    stdValues,
                    spAllExtValues,
                    isHosted());
            model.updateSPEntityConfig(
                    realm,
                    entityName,
                    spAllExtValues);

            if (isHosted()) {
                // update Authentication Contexts
                model.updateSPAuthenticationContexts(
                        realm,
                        entityName,
                        getAuthenticationContexts());
            }

            setInlineAlertMessage(CCAlert.TYPE_INFO,
                    "message.information",
                    "idff.entityDescriptor.provider.sp.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            debug.warning("IDFFSPViewBean -> handleButton1Request", e);
        }
        forwardTo();
    }   
}
