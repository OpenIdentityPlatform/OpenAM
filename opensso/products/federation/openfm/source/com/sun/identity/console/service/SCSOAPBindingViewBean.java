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
 * $Id: SCSOAPBindingViewBean.java,v 1.3 2008/06/25 05:49:44 qcheng Exp $
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
import com.sun.identity.console.service.model.SOAPBindingRequestHandler;
import com.sun.identity.console.service.model.SCSOAPBindingModelImpl;
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

public class SCSOAPBindingViewBean
    extends AMServiceProfileViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/service/SCSOAPBinding.jsp";
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
    public SCSOAPBindingViewBean() {
	super("SCSOAPBinding", DEFAULT_DISPLAY_URL,
	    "sunIdentityServerSOAPBinding");
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
	createTwoButtonPageTitleModel();
    }

    private void createRequestHandlerListTableModel() {
	CCActionTableModel tblModel = new CCActionTableModel(
	    getClass().getClassLoader().getResourceAsStream(
	    "com/sun/identity/console/tblSOAPBindingRequestHandlerList.xml"));
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
	SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST, tblModel);
    }

    protected AMModel getModelInternal() {
	HttpServletRequest req =
	    RequestManager.getRequestContext().getRequest();
	try {
	    return new SCSOAPBindingModelImpl(
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
		SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
	    if ((set != null) && !set.isEmpty()) {
		handlers.addAll(set);
	    }
	} else {
	    handlers = (Set)removePageSessionAttribute(
		SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
	}

	if (handlers != null) {
	    populateRequestHandlerListTable(handlers);
	}
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException {
	super.beginDisplay(event);
	resetButtonState(TBL_REQUEST_HANDLER_LIST_DELETE_BTN);

	if (!tablePopulated) {
	    if (!isSubmitCycle()) {
		AMServiceProfileModel model = (AMServiceProfileModel)getModel();

		if (model != null) {
		    Set handlers = new OrderedSet();
		    handlers.addAll(model.getAttributeValues(
		    SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST
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
		SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
	tblModel.clearAll();
	boolean firstEntry = true;
	int counter = 0;

	for (Iterator iter = handlers.iterator(); iter.hasNext(); ) {
	    String c = (String)iter.next();
	    SOAPBindingRequestHandler entry =
		new SOAPBindingRequestHandler(c);

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
	    SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST,
	    (OrderedSet)handlers);
    }
										
    protected boolean onBeforeSaveProfile(Map attrValues) {
	Set handlers = (Set)getPageSessionAttribute(
	    SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);

	if ((handlers != null) && !handlers.isEmpty()) {
	    attrValues.put(
		SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST,
		handlers);
	} else {
	    attrValues.put(
		SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST,
		Collections.EMPTY_SET);
	}

	return true;
    }

    protected void onBeforeResetProfile() {
	removePageSessionAttribute(
	    SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
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
     * Handles remove request handlers request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblRequestHandlerListButtonDeleteRequest(
	RequestInvocationEvent event
    ) throws ModelControlException {
	setSubmitCycle(true);
	CCActionTable table = (CCActionTable)getChild(
	    SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
	table.restoreStateData();
	CCActionTableModel tblModel =
	    (CCActionTableModel)propertySheetModel.getModel(
		SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
	Integer[] selected = tblModel.getSelectedRows();

	if ((selected != null) && (selected.length > 0)) {
	    OrderedSet tblValues = (OrderedSet)getPageSessionAttribute(
		SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
	    tblValues.removeAll(selected);
	    setPageSessionAttribute(
		SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST,
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
	    SCSOAPBindingRequestHandlerListAddViewBean vb =
		(SCSOAPBindingRequestHandlerListAddViewBean)
		getViewBean(SCSOAPBindingRequestHandlerListAddViewBean.class);
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
	    SCSOAPBindingRequestHandlerListEditViewBean vb =
		(SCSOAPBindingRequestHandlerListEditViewBean)getViewBean(
		    SCSOAPBindingRequestHandlerListEditViewBean.class);
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
	    SCSOAPBindingRequestHandlerListDupViewBean vb =
		(SCSOAPBindingRequestHandlerListDupViewBean)getViewBean(
		    SCSOAPBindingRequestHandlerListDupViewBean.class);
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
