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
 * $Id: WSAuthNServicesViewBean.java,v 1.2 2008/06/25 05:49:50 qcheng Exp $
 *
 */

package com.sun.identity.console.webservices;

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
import com.sun.identity.console.webservices.model.WSAuthHandlerEntry;
import com.sun.identity.console.webservices.model.WSAuthNServicesModelImpl;
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

public class WSAuthNServicesViewBean
    extends AMServiceProfileViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/webservices/WSAuthNServices.jsp";
    public static final String PAGE_MODIFIED = "pageModified";

    private static final String TBL_HANDLERS_COL_KEY = "tblHandlersColKey";
    private static final String TBL_HANDLERS_COL_CLASS = "tblHandlersColClass";
    private static final String TBL_HANDLERS_DATA_KEY = "tblHandlersDataKey";
    private static final String TBL_HANDLERS_DATA_CLASS =
	"tblHandlersDataClass";
    private static final String TBL_HANDLERS_HREF_ACTION =
	"tblHandlersHrefAction";
    private static final String TBL_HANDLERS_ADD_BTN =
	"tblHandlersButtonAdd";
    private static final String TBL_HANDLERS_DELETE_BTN =
	"tblHandlersButtonDelete";

    private boolean tablePopulated = false;

    /**
     * Creates a personal profile service profile view bean.
     */
    public WSAuthNServicesViewBean() {
	super("WSAuthNServices", DEFAULT_DISPLAY_URL,
	    "sunIdentityServerAuthnService");
    }

    protected View createChild(String name) {
	if (!tablePopulated) {
	    prePopulateTable();
	}
	return super.createChild(name);
    }

    protected void createPropertyModel() {
	super.createPropertyModel();
	createHandlersTableModel();
    }

    protected void createPageTitleModel() {
	createTwoButtonPageTitleModel();
    }

    private void createHandlersTableModel() {
	CCActionTableModel tblModel = new CCActionTableModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/tblWSAuthentication.xml"));
	tblModel.setTitleLabel("label.items");
	tblModel.setActionValue(TBL_HANDLERS_COL_KEY,
	    "webservices.authentication.service.table.handlers.key");
	tblModel.setActionValue(TBL_HANDLERS_COL_CLASS,
	    "webservices.authentication.service.table.handlers.class");
	tblModel.setActionValue(TBL_HANDLERS_ADD_BTN,
	    "webservices.authentication.service.table.handlers.add.button");
	tblModel.setActionValue(TBL_HANDLERS_DELETE_BTN,
	"webservices.authentication.service.table.handlers.delete.button");
	propertySheetModel.setModel(
	    WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS, tblModel);
    }

    protected AMModel getModelInternal() {
	HttpServletRequest req =
	    RequestManager.getRequestContext().getRequest();
	try {
	    return new WSAuthNServicesModelImpl(
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
	prePopulateHandlersTable(attributeValues);
    }

    private void prePopulateHandlersTable(Map attributeValues) {
	Set handlers = null;

	if (attributeValues != null) {
	    handlers = (Set)attributeValues.get(
		WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS);
	    if (handlers == null) {
		handlers = Collections.EMPTY_SET;
	    }
	} else {
	    handlers = (Set)removePageSessionAttribute(
		WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS);
	}

	if (handlers != null) {
	    populateHandlersTable(handlers);
	}
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException {
	super.beginDisplay(event);

	resetButtonState(TBL_HANDLERS_DELETE_BTN);

	if (!tablePopulated) {
	    if (!isSubmitCycle()) {
		AMServiceProfileModel model = (AMServiceProfileModel)getModel();

		if (model != null) {
		    Set handlers = new OrderedSet();
		    handlers.addAll(model.getAttributeValues(
			WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS));
		    populateHandlersTable(handlers);
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


    private void populateHandlersTable(Set handlers) {
	tablePopulated = true;
	CCActionTableModel tblModel =
	    (CCActionTableModel)propertySheetModel.getModel(
		WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS);
	tblModel.clearAll();
	boolean firstEntry = true;
	int counter = 0;

	for (Iterator iter = handlers.iterator(); iter.hasNext(); ) {
	    String c = (String)iter.next();
	    WSAuthHandlerEntry entry = new WSAuthHandlerEntry(c);

	    if (entry.isValid()) {
		if (!firstEntry) {
		    tblModel.appendRow();
		} else {
		    firstEntry = false;
		}

		tblModel.setValue(TBL_HANDLERS_DATA_KEY, entry.strKey);
		tblModel.setValue(TBL_HANDLERS_DATA_CLASS, entry.strClass);
		tblModel.setValue(TBL_HANDLERS_HREF_ACTION,
		    Integer.toString(counter));
	    }
	    counter++;
	}

	setPageSessionAttribute(
	    WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS,
		(OrderedSet)handlers);
    }

    protected boolean onBeforeSaveProfile(Map attrValues) {
	Set handlers = (Set)getPageSessionAttribute(
	    WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS);
	if ((handlers != null) && !handlers.isEmpty()) {
	    attrValues.put(WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS,
		handlers);
	}
	return true;
    }

    protected void onBeforeResetProfile() {
	removePageSessionAttribute(
	    WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS);
	tablePopulated = false;
    }

    /**
     * Handles remove handler request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblHandlersButtonDeleteRequest(
	RequestInvocationEvent event
    ) throws ModelControlException {
	setSubmitCycle(true);
	CCActionTable table = (CCActionTable)getChild(
	    WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS);
	table.restoreStateData();
	CCActionTableModel tblModel =
	    (CCActionTableModel)propertySheetModel.getModel(
		WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS);
	Integer[] selected = tblModel.getSelectedRows();

	if ((selected != null) && (selected.length > 0)) {
	    OrderedSet tblValues = (OrderedSet)getPageSessionAttribute(
		WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS);
	    tblValues.removeAll(selected);
	    setPageSessionAttribute(
		WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS, tblValues);
	    populateHandlersTable(tblValues);
	}

	forwardTo();
    }

    /**
     * Handles add handler request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblHandlersButtonAddRequest(
	RequestInvocationEvent event
    ) throws ModelControlException {
	try {
	    Map values = getValues();
	    onBeforeSaveProfile(values);
	    setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
	    WSAuthNServicesHandlersAddViewBean vb =
		(WSAuthNServicesHandlersAddViewBean)
		getViewBean(WSAuthNServicesHandlersAddViewBean.class);
	    unlockPageTrail();
	    passPgSessionMap(vb);
	    vb.forwardTo(getRequestContext());
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
	}
    }

    /**
     * Handles edit handler request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblHandlersHrefActionRequest(
	RequestInvocationEvent event
    ) throws ModelControlException {
	try {
	    Map values = getValues();
	    onBeforeSaveProfile(values);
	    setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
	    WSAuthNServicesHandlersEditViewBean vb =
		(WSAuthNServicesHandlersEditViewBean)getViewBean(
		    WSAuthNServicesHandlersEditViewBean.class);
	    unlockPageTrail();
	    passPgSessionMap(vb);
	    vb.populateValues((String)getDisplayFieldValue(
		"tblHandlersHrefAction"));
	    vb.forwardTo(getRequestContext());
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
	}
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

    protected String getBreadCrumbDisplayName() {
	return "breadcrumbs.webservices.authentication";
    }

}
