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
 * $Id: SMDiscoveryServiceViewBean.java,v 1.8 2008/12/17 07:30:34 veiming Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPostViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.idm.EntityEditViewBean;
import com.sun.identity.console.idm.EntityResourceOfferingViewBean;
import com.sun.identity.console.idm.ServicesNoAttributeViewBean;
import com.sun.identity.console.idm.model.EntityResourceOfferingModel;
import com.sun.identity.console.idm.model.EntityResourceOfferingModelImpl;
import com.sun.identity.console.realm.RealmResourceOfferingViewBean;
import com.sun.identity.console.realm.model.RealmResourceOfferingModel;
import com.sun.identity.console.realm.model.RealmResourceOfferingModelImpl;
import com.sun.identity.console.service.model.SMDiscoEntryData;
import com.sun.identity.console.service.model.SMDiscoveryServiceData;
import com.sun.identity.console.service.model.SMDiscoveryServiceModel;
import com.sun.identity.console.service.model.SMDiscoveryServiceModelImpl;
import com.sun.identity.console.user.UMUserResourceOfferingViewBean;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.table.CCActionTable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class SMDiscoveryServiceViewBean
    extends AMServiceProfileViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/SMDiscoveryService.jsp";
    public static final String PAGE_MODIFIED = "pageModified";

    private static final String TBL_PROVIDER_RESOURCEID_MAPPER_COL_PROVIDERID =
        "tblProviderResourceIdMapperColProviderId";
    private static final String TBL_PROVIDER_RESOURCEID_MAPPER_COL_ID_MAPPER =
        "tblProviderResourceIdMapperColIdMapper";
    private static final String TBL_PROVIDER_RESOURCEID_MAPPER_DATA_PROVIDERID =
        "tblProviderResourceIdMapperDataProviderId";
    private static final String TBL_PROVIDER_RESOURCEID_MAPPER_DATA_ID_MAPPER =
        "tblProviderResourceIdMapperDataIdMapper";
    private static final String TBL_PROVIDER_RESOURCEID_MAPPER_HREF_ACTION =
        "tblProviderResourceIdMapperHrefAction";
    private static final String TBL_PROVIDER_RESOURCEID_MAPPER_ADD_BTN =
        "tblProviderResourceIdMapperButtonAdd";
    private static final String TBL_PROVIDER_RESOURCEID_MAPPER_DELETE_BTN =
        "tblProviderResourceIdMapperButtonDelete";

    private static final String TBL_BOOTSTRAP_RES_OFF_COL_SERVICE_TYPE =
        "tblBootstrapResOffColServiceType";
    private static final String TBL_BOOTSTRAP_RES_OFF_COL_ABSTRACT =
        "tblBootstrapResOffColAbstract";
    private static final String TBL_BOOTSTRAP_RES_OFF_DATA_SERVICE_TYPE =
        "tblBootstrapResOffDataServiceType";
    private static final String TBL_BOOTSTRAP_RES_OFF_DATA_ABSTRACT =
        "tblBootstrapResOffDataAbstract";
    private static final String TBL_BOOTSTRAP_RES_OFF_HREF_ACTION =
        "tblBootstrapResOffHrefAction";
    private static final String TBL_BOOTSTRAP_RES_OFF_ADD_BTN =
        "tblBootstrapResOffButtonAdd";
    private static final String TBL_BOOTSTRAP_RES_OFF_DELETE_BTN =
        "tblBootstrapResOffButtonDelete";

    private boolean tablePopulated = false;

    /**
     * Creates a personal profile service profile view bean.
     */
    public SMDiscoveryServiceViewBean() {
        super("SMDiscoveryService", DEFAULT_DISPLAY_URL,
            AMAdminConstants.DISCOVERY_SERVICE);
    }

    /**
     * Forwards to user service view bean if user parameter exists.
     *
     * @param rc Request Context.
     */
    public void forwardTo(RequestContext rc) {
        HttpServletRequest req = rc.getRequest();
        String location = req.getParameter("Location");

        if ((location != null) && (location.trim().length() > 0)) {
            handleRealmOperationRequest(hexToString(location), rc);
        } else {
            String user = req.getParameter("User");

            if ((user != null) && (user.trim().length() > 0)) {
                handleUserOperationRequest(location, hexToString(user), rc);
            } else {
                super.forwardTo(rc);
            }
        }
    }

    private void handleEntityOperationRequest(
        String realm,
        String univId,
        RequestContext rc
    ) {
        HttpServletRequest req = rc.getRequest();
        String op = req.getParameter("Op");
        if (op.equals(AMAdminConstants.OPERATION_EDIT)) {
            setPageSessionAttribute(PG_SESSION_PAGE_TRAIL_ID,
                req.getParameter(PG_SESSION_PAGE_TRAIL_ID));
            setPageSessionAttribute(EntityEditViewBean.UNIVERSAL_ID, univId);
            setPageSessionAttribute(AMAdminConstants.CURRENT_REALM, realm);
            unlockPageTrail();
            EntityResourceOfferingViewBean vb =
                (EntityResourceOfferingViewBean)getViewBean(
                EntityResourceOfferingViewBean.class);
            passPgSessionMap(vb);
            vb.forwardTo(rc);
        } else {
            setPageSessionAttribute(PG_SESSION_PAGE_TRAIL_ID,
                req.getParameter(PG_SESSION_PAGE_TRAIL_ID));
            setPageSessionAttribute(EntityEditViewBean.UNIVERSAL_ID, univId);
            setPageSessionAttribute(AMAdminConstants.CURRENT_REALM, realm);
            setPageSessionAttribute(EntityEditViewBean.ENTITY_TYPE,
                rc.getRequest().getParameter("type"));
            EntityResourceOfferingModel entityModel = new
                EntityResourceOfferingModelImpl(
                    req, getPageSessionAttributes());
            try {
                entityModel.assignService(univId);
            } catch (AMConsoleException e) {
                debug.error(
                   "SMDiscoveryServiceViewBean.handleEntityOperationRequest",e);
            }

            backTrail();
            AMPostViewBean vb = (AMPostViewBean)getViewBean(
                AMPostViewBean.class);
            passPgSessionMap(vb);
            vb.setTargetViewBeanURL("../idm/EntityServices");
            vb.forwardTo(rc);
        }
    }

    private void handleRealmOperationRequest(String realm, RequestContext rc) {
        HttpServletRequest req = rc.getRequest();
        String op = req.getParameter("Op");
        if (op.equals(AMAdminConstants.OPERATION_EDIT)) {
            setPageSessionAttribute(PG_SESSION_PAGE_TRAIL_ID,
                req.getParameter(PG_SESSION_PAGE_TRAIL_ID));
            setPageSessionAttribute(AMAdminConstants.CURRENT_REALM, realm);
            unlockPageTrail();
            RealmResourceOfferingViewBean vb =
                (RealmResourceOfferingViewBean)getViewBean(
                RealmResourceOfferingViewBean.class);
            passPgSessionMap(vb);
            vb.forwardTo(rc);
        } else {
            setPageSessionAttribute(PG_SESSION_PAGE_TRAIL_ID,
                req.getParameter(PG_SESSION_PAGE_TRAIL_ID));
            setPageSessionAttribute(AMAdminConstants.CURRENT_REALM, realm);

            RealmResourceOfferingModel realmModel = new
                RealmResourceOfferingModelImpl(
                    req, getPageSessionAttributes());
            try {
                realmModel.assignService(realm);
            } catch (AMConsoleException e) {
                debug.error(
                   "SMDiscoveryServiceViewBean.handleRealmOperationRequest", e);
            }

            backTrail();
            AMPostViewBean vb = (AMPostViewBean)getViewBean(
                AMPostViewBean.class);
            passPgSessionMap(vb);
            vb.setTargetViewBeanURL("../realm/Services");
            vb.forwardTo(rc);
        }
    }
    
    private void handleUserOperationRequest(
        String realm,
        String univId,
        RequestContext rc
    ) {
        HttpServletRequest req = rc.getRequest();
        SMDiscoveryServiceModel model = (SMDiscoveryServiceModel)getModel();
        try {
            AMIdentity amid = IdUtils.getIdentity(
                model.getUserSSOToken(), univId);
            if (amid.getType().getName().equalsIgnoreCase("user")) {
                UMUserResourceOfferingViewBean vb =
                    (UMUserResourceOfferingViewBean)
                    getViewBean(UMUserResourceOfferingViewBean.class);
                vb.unlockPageTrail();
                vb.setPageSessionAttribute(EntityEditViewBean.UNIVERSAL_ID,
                    univId);
                vb.setPageSessionAttribute(EntityEditViewBean.ENTITY_TYPE, 
                    "user");
                vb.setPageSessionAttribute(
                    UMUserResourceOfferingViewBean.SERVICE_NAME, 
                    AMAdminConstants.DISCOVERY_SERVICE);
                vb.setPageSessionAttribute(AMAdminConstants.CURRENT_REALM,
                    req.getParameter("realm"));
                vb.setPageSessionAttribute(PG_SESSION_PAGE_TRAIL_ID,
                    req.getParameter(PG_SESSION_PAGE_TRAIL_ID));
                vb.forwardTo(rc);
            } else {
                handleEntityOperationRequest(req.getParameter("realm"), 
                    univId, rc);
            }
        } catch (IdRepoException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                model.getErrorString(e));
            super.forwardTo(rc);
        }
    }

    protected View createChild(String name) {
        if (!tablePopulated) {
            prePopulateTable();
        }
        return super.createChild(name);
    }

    protected void createPropertyModel() {
        super.createPropertyModel();
        createProviderResourceIdMapperTableModel();
        createBootstrapResOffTableModel();
    }

    protected void createPageTitleModel() {
        createTwoButtonPageTitleModel();
    }

    private void createProviderResourceIdMapperTableModel() {
        CCActionTableModel tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblProviderResourceIdMapper.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_PROVIDER_RESOURCEID_MAPPER_COL_PROVIDERID,
            "discovery.service.table.providerResourceIdMapper.providerId");
        tblModel.setActionValue(TBL_PROVIDER_RESOURCEID_MAPPER_COL_ID_MAPPER,
            "discovery.service.table.providerResourceIdMapper.idMapper");
        tblModel.setActionValue(TBL_PROVIDER_RESOURCEID_MAPPER_ADD_BTN,
            "discovery.service.table.providerResourceIdMapper.add.button");
        tblModel.setActionValue(TBL_PROVIDER_RESOURCEID_MAPPER_DELETE_BTN,
            "discovery.service.table.providerResourceIdMapper.delete.button");
        propertySheetModel.setModel(
            AMAdminConstants.DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER,
            tblModel);
    }

    private void createBootstrapResOffTableModel() {
        CCActionTableModel tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblDiscoveryBootstrapResOff.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_BOOTSTRAP_RES_OFF_COL_SERVICE_TYPE,
            "discovery.service.table.bootstrapResOff.serviceType");
        tblModel.setActionValue(TBL_BOOTSTRAP_RES_OFF_COL_ABSTRACT,
            "discovery.service.table.bootstrapResOff.abstract");
        tblModel.setActionValue(TBL_BOOTSTRAP_RES_OFF_ADD_BTN,
            "discovery.service.table.bootstrapResOff.add.button");
        tblModel.setActionValue(TBL_BOOTSTRAP_RES_OFF_DELETE_BTN,
            "discovery.service.table.bootstrapResOff.delete.button");
        propertySheetModel.setModel(
            AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF,
            tblModel);
    }

    private void prePopulateTable() {
        Map attributeValues = (Map)removePageSessionAttribute(
            PROPERTY_ATTRIBUTE);
        if (attributeValues != null) {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            ps.setAttributeValues(attributeValues, getModel());
        }
        prePopulateProviderResourceIdTable(attributeValues);
        prePopulateBootstrapResOffTable(attributeValues);
    }

    private void prePopulateProviderResourceIdTable(Map attributeValues) {
        if (attributeValues != null) {
            Set mapper = (Set)attributeValues.get(
                AMAdminConstants.DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER);
            populateProviderResourceIdMapperTable(mapper);
        } else {
            Set mapper = (Set)removePageSessionAttribute(
                AMAdminConstants.DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER);
            if ((mapper != null) && !mapper.isEmpty()) {
                populateProviderResourceIdMapperTable(mapper);
            }
        }
    }

    private void prePopulateBootstrapResOffTable(Map attributeValues) {
        Set resoff = null;

        if (attributeValues != null) {
            resoff = (Set)attributeValues.get(
                AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF);
            populateBootstrapResOffTable(resoff);
        } else {
            resoff = (Set)removePageSessionAttribute(
                AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF);
            if ((resoff != null) && !resoff.isEmpty()) {
                populateBootstrapResOffTable(resoff);
            }
        }
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        resetButtonState(TBL_BOOTSTRAP_RES_OFF_DELETE_BTN);
        resetButtonState(TBL_PROVIDER_RESOURCEID_MAPPER_DELETE_BTN);

        if (!tablePopulated) {
            if (!isSubmitCycle()) {
                SMDiscoveryServiceModel model = (SMDiscoveryServiceModel)
                    getModel();

                if (model != null) {
                    populateProviderResourceIdMapperTable(
                        model.getProviderResourceIdMapper());
                    Set resoff = new OrderedSet();
                    resoff.addAll(model.getDiscoEntry(false));
                    populateBootstrapResOffTable(resoff);
                }
            }
        }

        if (!isInlineAlertMessageSet()) {
            String flag = (String)getPageSessionAttribute(PAGE_MODIFIED);
            if ((flag != null) && flag.equals("1")) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "message.profile.modified");
            }
        }
    }

    private void populateProviderResourceIdMapperTable(Collection mapper) {
        tablePopulated = true;
        OrderedSet cache = new OrderedSet();

        if (mapper != null) {
            CCActionTableModel tblModel =
                (CCActionTableModel)propertySheetModel.getModel(
            AMAdminConstants.DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER);
            tblModel.clearAll();
            boolean firstEntry = true;
            int counter = 0;

            for (Iterator i = mapper.iterator(); i.hasNext(); ) {
                String val = (String)i.next();
                if (!firstEntry) {
                    tblModel.appendRow();
                } else {
                    firstEntry = false;
                }

                Map map = AMAdminUtils.getValuesFromDelimitedString(val, "|");
                String providerId = (String)map.get(AMAdminConstants.
                    DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER_PROVIDER_ID);
                String idMapper = (String)map.get(AMAdminConstants.
                    DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER_ID_MAPPER);

                if ((providerId != null) && (idMapper != null)) {
                    tblModel.setValue(
                        TBL_PROVIDER_RESOURCEID_MAPPER_DATA_PROVIDERID,
                        providerId);
                    tblModel.setValue(
                        TBL_PROVIDER_RESOURCEID_MAPPER_DATA_ID_MAPPER,
                        idMapper);
                    tblModel.setValue(
                        TBL_PROVIDER_RESOURCEID_MAPPER_HREF_ACTION,
                            Integer.toString(counter));
                    counter++;
                    cache.add(val);
                }
            }
        }
        setPageSessionAttribute(AMAdminConstants.
            DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER, cache);
    }

    private void populateBootstrapResOffTable(Set resoff) {
        tablePopulated = true;

        if (resoff != null) {
            CCActionTableModel tblModel =
                (CCActionTableModel)propertySheetModel.getModel(
                AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF);
            tblModel.clearAll();
            boolean firstEntry = true;
            int counter = 0;

            try {
                SMDiscoveryServiceData smEntry =
                    SMDiscoveryServiceData.getEntries(resoff);

                List resourceList = smEntry.getResourceData();
                if ((resourceList != null) && !resourceList.isEmpty()) {
                    for (Iterator i = resourceList.iterator(); i.hasNext(); ) {
                        SMDiscoEntryData smDisco = (SMDiscoEntryData)i.next();

                        if (!firstEntry) {
                            tblModel.appendRow();
                        } else {
                            firstEntry = false;
                        }

                        tblModel.setValue(
                            TBL_BOOTSTRAP_RES_OFF_DATA_SERVICE_TYPE,
                            smDisco.serviceType);
                        tblModel.setValue(TBL_BOOTSTRAP_RES_OFF_DATA_ABSTRACT,
                            smDisco.abstractValue);
                        tblModel.setValue(TBL_BOOTSTRAP_RES_OFF_HREF_ACTION,
                            Integer.toString(counter));
                        counter++;
                    }
                    disableButton(TBL_BOOTSTRAP_RES_OFF_ADD_BTN, true);
                } else {
                    disableButton(TBL_BOOTSTRAP_RES_OFF_ADD_BTN, false);
                }
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
        }

        setPageSessionAttribute(
            AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF,
            (OrderedSet)resoff);
    }


    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        try {
            return new SMDiscoveryServiceModelImpl(
                req, serviceName, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        return null;
    }


    protected boolean onBeforeSaveProfile(Map attrValues) {
        Set mapper = (Set)getPageSessionAttribute(
            AMAdminConstants.DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER);
        if ((mapper != null) && !mapper.isEmpty()) {
            attrValues.put(
                AMAdminConstants.DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER,
                    mapper);
        }
        Set resoff = (Set)getPageSessionAttribute(
            AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF);
        if ((resoff != null) && !resoff.isEmpty()) {
            attrValues.put(
                AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF,
                resoff);
        }

        return true;
    }

    protected void onBeforeResetProfile() {
        removePageSessionAttribute(
            AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF);
        removePageSessionAttribute(
            AMAdminConstants.DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER);
        tablePopulated = false;
    }

    /**
     * Handles save request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        removePageSessionAttribute(PAGE_MODIFIED);
        super.handleButton1Request(event);
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        removePageSessionAttribute(PAGE_MODIFIED);
        super.handleButton2Request(event);
    }

    /**
     * Handles remove resource offerings request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblBootstrapResOffButtonDeleteRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        setSubmitCycle(true);
        CCActionTable table = (CCActionTable)getChild(
            AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF);
        table.restoreStateData();
        CCActionTableModel tblModel =
            (CCActionTableModel)propertySheetModel.getModel(
                AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF);

        Integer[] selected = tblModel.getSelectedRows();

        if ((selected != null) && (selected.length > 0)) {
            try {
                OrderedSet tblValues = (OrderedSet)getPageSessionAttribute(
                    AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF);
                SMDiscoveryServiceData smEntry =
                    SMDiscoveryServiceData.getEntries(tblValues);
                smEntry.deleteDiscoEntries(selected);
                tblValues = (OrderedSet)smEntry.getDiscoveryEntries();
                setPageSessionAttribute(
                    AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF,
                    tblValues);
                populateBootstrapResOffTable(tblValues);
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
        }

        forwardTo();
    }

    /**
     * Handles remove provider resource id mapper request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblProviderResourceIdMapperButtonDeleteRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        setSubmitCycle(true);
        CCActionTable table = (CCActionTable)getChild(
            AMAdminConstants.DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER);
        table.restoreStateData();
        CCActionTableModel tblModel =
            (CCActionTableModel)propertySheetModel.getModel(
                AMAdminConstants.DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER);

        Integer[] selected = tblModel.getSelectedRows();

        if ((selected != null) && (selected.length > 0)) {
            OrderedSet tblValues = (OrderedSet)getPageSessionAttribute(
                AMAdminConstants.DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER);
            tblValues.removeAll(selected);
            populateProviderResourceIdMapperTable(tblValues);
        }
        forwardTo();
    } 

    /**
     * Handles add bootstrap resource offering sets request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblBootstrapResOffButtonAddRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        try {
            Map values = getValues();
            onBeforeSaveProfile(values);
            setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
            SMDiscoveryBootstrapRefOffAddViewBean vb =
                (SMDiscoveryBootstrapRefOffAddViewBean)
                getViewBean(SMDiscoveryBootstrapRefOffAddViewBean.class);
            removePageSessionAttribute(
                SMDiscoveryBootstrapRefOffAddViewBean.PROPERTY_ATTRIBUTE);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    /**
     * Handles add provider resource id mapper request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblProviderResourceIdMapperButtonAddRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        try {
            Map values = getValues();
            onBeforeSaveProfile(values);
            setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
            SMDiscoveryProviderResourceIdMapperAddViewBean vb =
                (SMDiscoveryProviderResourceIdMapperAddViewBean)getViewBean(
                SMDiscoveryProviderResourceIdMapperAddViewBean.class);
            removePageSessionAttribute(
            SMDiscoveryProviderResourceIdMapperAddViewBean.PROPERTY_ATTRIBUTE);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

                                                                                
    /**
     * Handles edit bootstrap resource offering request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblBootstrapResOffHrefActionRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        try {
            Map values = getValues();
            onBeforeSaveProfile(values);
            setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
            SMDiscoveryBootstrapRefOffEditViewBean vb =
                (SMDiscoveryBootstrapRefOffEditViewBean)getViewBean(
                    SMDiscoveryBootstrapRefOffEditViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.populateValues((String)getDisplayFieldValue(
                TBL_BOOTSTRAP_RES_OFF_HREF_ACTION));
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    /**
     * Handles edit provider resource id mapping request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblProviderResourceIdMapperHrefActionRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        try {
            Map values = getValues();
            onBeforeSaveProfile(values);
            setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
            SMDiscoveryProviderResourceIdMapperEditViewBean vb =
                (SMDiscoveryProviderResourceIdMapperEditViewBean)getViewBean(
                    SMDiscoveryProviderResourceIdMapperEditViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.populateValues((String)getDisplayFieldValue(
                TBL_PROVIDER_RESOURCEID_MAPPER_HREF_ACTION));
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.webservices.discovery";
    }
}
