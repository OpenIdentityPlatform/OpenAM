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
 * $Id: SCSAML2SOAPBindingViewBean.java,v 1.4 2008/06/25 05:49:44 qcheng Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMServiceProfileModel;
import com.sun.identity.console.service.model.SAML2SOAPBindingRequestHandler;
import com.sun.identity.console.service.model.SCSAML2SOAPBindingModelImpl;
import com.sun.identity.console.service.SCConfigGlobalViewBean;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.table.CCActionTable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import com.sun.identity.console.base.model.AMAdminConstants; 

public class SCSAML2SOAPBindingViewBean
    extends AMServiceProfileViewBeanBase {
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/SCSAML2SOAPBinding.jsp";
    public static final String PAGE_MODIFIED = "pageModified";
    
    private static final String TBL_REQUEST_HANDLER_LIST_COL_KEY =
        "tblRequestHandlerListColKey";
    private static final String TBL_REQUEST_HANDLER_LIST_COL_CLASS =
        "tblRequestHandlerListColClass";
    private static final String TBL_REQUEST_HANDLER_LIST_COL_ACTION =
        "tblRequestHandlerListColAction";
    private static final String TBL_REQUEST_HANDLER_LIST_DATA_KEY =
        "tblRequestHandlerListDataKey";
    private static final String TBL_REQUEST_HANDLER_LIST_DATA_CLASS =
        "tblRequestHandlerListDataClass";
    private static final String TBL_REQUEST_HANDLER_LIST_HREF_EDIT_ACTION =
        "tblRequestHandlerListHrefEditAction";
    private static final String TBL_REQUEST_HANDLER_LIST_LABEL_EDIT_ACTION =
        "tblRequestHandlerListLabelEditAction";
    private static final String TBL_REQUEST_HANDLER_LIST_HREF_DUP_ACTION =
        "tblRequestHandlerListHrefDupAction";
    private static final String TBL_REQUEST_HANDLER_LIST_LABEL_DUP_ACTION =
        "tblRequestHandlerListLabelDupAction";
    private static final String TBL_REQUEST_HANDLER_LIST_ADD_BTN =
        "tblRequestHandlerListButtonAdd";
    private static final String TBL_REQUEST_HANDLER_LIST_DELETE_BTN =
        "tblRequestHandlerListButtonDelete";
    
    private boolean tablePopulated = false;
    
    /**
     * Creates a personal profile service profile view bean.
     */
    public SCSAML2SOAPBindingViewBean() {
        super("SCSAML2SOAPBinding", DEFAULT_DISPLAY_URL,
            "sunfmSAML2SOAPBindingService");
    }
    
    protected View createChild(String name) {
        if (!tablePopulated) {
            prePopulateTable();
        }
        return super.createChild(name);
    }
    
    protected void createPropertyModel() {
        super.createPropertyModel();
        createRequestHandlerListTableModel();
    }
    
    protected void createPageTitleModel() {
        createThreeButtonPageTitleModel();
    }
    
    private void createRequestHandlerListTableModel() {
        CCActionTableModel tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblSAML2SOAPBindingRequestHandlerList.xml"));
        
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_REQUEST_HANDLER_LIST_COL_KEY,
            "soapBinding.service.table.requestHandlerList.key");
        tblModel.setActionValue(TBL_REQUEST_HANDLER_LIST_COL_CLASS,
            "soapBinding.service.table.requestHandlerList.class");
        tblModel.setActionValue(TBL_REQUEST_HANDLER_LIST_COL_ACTION,
            "soapBinding.service.table.requestHandlerList.action");
        tblModel.setActionValue(TBL_REQUEST_HANDLER_LIST_ADD_BTN,
            "soapBinding.service.table.requestHandlerList.add.button");
        tblModel.setActionValue(TBL_REQUEST_HANDLER_LIST_DELETE_BTN,
            "soapBinding.service.table.requestHandlerList.delete.button");
        propertySheetModel.setModel(
            SCSAML2SOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST, tblModel);
    }
    
    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        try {
            return new SCSAML2SOAPBindingModelImpl(
                req, serviceName, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        return null;
    }
    
    private void prePopulateTable() {
        Map attributeValues = (Map)removePageSessionAttribute(
            PROPERTY_ATTRIBUTE);
        if (attributeValues != null) {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            ps.setAttributeValues(attributeValues, getModel());
        }
        prePopulateRequestHandlerListTable(attributeValues);
    }
    
    private void prePopulateRequestHandlerListTable(Map attributeValues) {
        Set handlers = null;
        if (attributeValues != null) {
            handlers = new OrderedSet();
            Set set = (Set)attributeValues.get(
                SCSAML2SOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
            if ((set != null) && !set.isEmpty()) {
                handlers.addAll(set);
            }
        } else {
            handlers = (Set)removePageSessionAttribute(
                SCSAML2SOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
        }
        
        if (handlers != null) {
            populateRequestHandlerListTable(handlers);
        }
    }
    
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException 
    {
        super.beginDisplay(event);
        resetButtonState(TBL_REQUEST_HANDLER_LIST_DELETE_BTN);
        
        if (!tablePopulated) {
            if (!isSubmitCycle()) {
                AMServiceProfileModel model = (AMServiceProfileModel)getModel();
                if (model != null) {
                    Set handlers = new OrderedSet();
                    handlers.addAll(model.getAttributeValues(
                        SCSAML2SOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST
                        ));
                    populateRequestHandlerListTable(handlers);
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
    
    private void populateRequestHandlerListTable(Set handlers) {
        tablePopulated = true;
        CCActionTableModel tblModel =
            (CCActionTableModel)propertySheetModel.getModel(
            SCSAML2SOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
        
        tblModel.clearAll();
        boolean firstEntry = true;
        int counter = 0;
        
        for (Iterator iter = handlers.iterator(); iter.hasNext(); ) {
            String c = (String)iter.next();
            SAML2SOAPBindingRequestHandler entry =
                new SAML2SOAPBindingRequestHandler(c);
            
            if (entry.isValid()) {
                if (!firstEntry) {
                    tblModel.appendRow();
                } else {
                    firstEntry = false;
                }
                
                tblModel.setValue(TBL_REQUEST_HANDLER_LIST_DATA_KEY,
                    entry.strKey);
                tblModel.setValue(TBL_REQUEST_HANDLER_LIST_DATA_CLASS,
                    entry.strClass);
                tblModel.setValue(TBL_REQUEST_HANDLER_LIST_HREF_EDIT_ACTION,
                    Integer.toString(counter));
                tblModel.setValue(TBL_REQUEST_HANDLER_LIST_LABEL_EDIT_ACTION,
                    "soapBinding.service.table.requestHandlerList.action.edit.label"
                    );
                tblModel.setValue(TBL_REQUEST_HANDLER_LIST_HREF_DUP_ACTION,
                    Integer.toString(counter));
                tblModel.setValue(TBL_REQUEST_HANDLER_LIST_LABEL_DUP_ACTION,
                    "soapBinding.service.table.requestHandlerList.action.dup.label"
                    );
            }
            counter++;
        }
        
        setPageSessionAttribute(
            SCSAML2SOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST,
            (OrderedSet)handlers);
    }
    
    protected boolean onBeforeSaveProfile(Map attrValues) {
        Set handlers = (Set)getPageSessionAttribute(
            SCSAML2SOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
        
        if ((handlers != null) && !handlers.isEmpty()) {
            attrValues.put(
                SCSAML2SOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST,
                handlers);
        } else {
            attrValues.put(
                SCSAML2SOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST,
                Collections.EMPTY_SET);
        }
        
        return true;
    }
    
    protected void onBeforeResetProfile() {
        removePageSessionAttribute(
            SCSAML2SOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
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
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event)
        throws ModelControlException , AMConsoleException 
    {
        removePageSessionAttribute(PAGE_MODIFIED);
        backTrail();
        try {
            String name = (String) getPageSessionAttribute(
                    AMAdminConstants.SAVE_VB_NAME);
            SCConfigViewBean vb = (SCConfigViewBean) getViewBean(
                    Class.forName(name));
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext()); 
        } catch (ClassNotFoundException e) {
            debug.warning(
                    "SCSAML2SOAPBindingViewBean.handleButton3Request:", e);
        }
        
    }
    
    /**
     * Handles remove request handlers request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblRequestHandlerListButtonDeleteRequest(
        RequestInvocationEvent event
        ) throws ModelControlException {
        setSubmitCycle(true);
        CCActionTable table = (CCActionTable)getChild(
            SCSAML2SOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
        table.restoreStateData();
        CCActionTableModel tblModel =
            (CCActionTableModel)propertySheetModel.getModel(
            SCSAML2SOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
        Integer[] selected = tblModel.getSelectedRows();
        
        if ((selected != null) && (selected.length > 0)) {
            OrderedSet tblValues = (OrderedSet)getPageSessionAttribute(
                SCSAML2SOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
            tblValues.removeAll(selected);
            setPageSessionAttribute(
                SCSAML2SOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST,
                tblValues);
            populateRequestHandlerListTable(tblValues);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "message.profile.modified");
            setPageSessionAttribute(PAGE_MODIFIED, "1");
        }
        
        forwardTo();
    }
    
    /**
     * Handles add request handler request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblRequestHandlerListButtonAddRequest(
        RequestInvocationEvent event
        ) throws ModelControlException {
        try {
            Map values = getValues();
            onBeforeSaveProfile(values);
            setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
            SCSAML2SOAPBindingRequestHandlerListAddViewBean vb =
                (SCSAML2SOAPBindingRequestHandlerListAddViewBean)
                getViewBean(SCSAML2SOAPBindingRequestHandlerListAddViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }
    
    /**
     * Handles edit request handler request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblRequestHandlerListHrefEditActionRequest(
        RequestInvocationEvent event
        ) throws ModelControlException {
        try {
            Map values = getValues();
            onBeforeSaveProfile(values);
            setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
            SCSAML2SOAPBindingRequestHandlerListEditViewBean vb =
                (SCSAML2SOAPBindingRequestHandlerListEditViewBean)getViewBean(
                SCSAML2SOAPBindingRequestHandlerListEditViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.populateValues((String)getDisplayFieldValue(
                TBL_REQUEST_HANDLER_LIST_HREF_EDIT_ACTION));
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }    
    
    /**
     * Handles duplicate request handler request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblRequestHandlerListHrefDupActionRequest(
        RequestInvocationEvent event
        ) throws ModelControlException {
        try {
            Map values = getValues();
            onBeforeSaveProfile(values);
            setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
            SCSAML2SOAPBindingRequestHandlerListDupViewBean vb =
                (SCSAML2SOAPBindingRequestHandlerListDupViewBean)getViewBean(
                SCSAML2SOAPBindingRequestHandlerListDupViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            int idx = Integer.parseInt((String)getDisplayFieldValue(
                TBL_REQUEST_HANDLER_LIST_HREF_DUP_ACTION));
            vb.setDupIndex(idx);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }
    
    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.webservices.soapbinding";
    }
}
