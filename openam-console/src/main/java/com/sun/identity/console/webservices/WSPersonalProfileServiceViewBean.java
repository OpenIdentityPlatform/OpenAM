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
 * $Id: WSPersonalProfileServiceViewBean.java,v 1.2 2008/06/25 05:49:50 qcheng Exp $
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
import com.sun.identity.console.webservices.model.WSPersonalProfileServiceModelImpl;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.table.CCActionTable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;

public class WSPersonalProfileServiceViewBean
    extends AMServiceProfileViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/webservices/WSPersonalProfileService.jsp";
    private static final String TBL_SUPPORTED_CONTAINER_COL_NAME =
	"tblSupportedContainerColName";
    private static final String TBL_SUPPORTED_CONTAINER_DATA_NAME =
	"tblSupportedContainerDataName";
    private static final String TBL_SUPPORTED_CONTAINER_DATA_TYPE =
	"tblSupportedContainerDataType";
    private static final String TBL_SUPPORTED_CONTAINER_HREF_ACTION =
	"tblSupportedContainerHrefAction";
    private static final String TBL_SUPPORTED_CONTAINER_LABEL_ACTION =
	"tblSupportedContainerLabelAction";
    private static final String TBL_SUPPORTED_CONTAINER_ADD_BTN =
	"tblSupportedContainerButtonAdd";
    private static final String TBL_SUPPORTED_CONTAINER_DELETE_BTN =
	"tblSupportedContainerButtonDelete";

    private static final String TBL_DS_ATTRIBUTE_MAP_LIST_COL_NAME =
	"tblDSAttributeMapListColName";
    private static final String TBL_DS_ATTRIBUTE_MAP_LIST_COL_MAP =
	"tblDSAttributeMapListColMap";
    private static final String TBL_DS_ATTRIBUTE_MAP_LIST_DATA_NAME =
	"tblDSAttributeMapListDataName";
    private static final String TBL_DS_ATTRIBUTE_MAP_LIST_DATA_MAP =
	"tblDSAttributeMapListDataMap";
    private static final String TBL_DS_ATTRIBUTE_MAP_LIST_HREF_ACTION =
	"tblDSAttributeMapListHrefAction";
    private static final String TBL_DS_ATTRIBUTE_MAP_LIST_LABEL_ACTION =
	"tblDSAttributeMapListLabelAction";
    private static final String TBL_DS_ATTRIBUTE_MAP_LIST_ADD_BTN =
	"tblDSAttributeMapListButtonAdd";
    private static final String TBL_DS_ATTRIBUTE_MAP_LIST_DELETE_BTN =
	"tblDSAttributeMapListButtonDelete";

    private boolean tablePopulated = false;

    /**
     * Creates a personal profile service profile view bean.
     */
    public WSPersonalProfileServiceViewBean() {
	super("WSPersonalProfileService", DEFAULT_DISPLAY_URL,
	    "sunIdentityServerLibertyPPService");
    }

    protected View createChild(String name) {
	if (!tablePopulated) {
	    prePopulateTable();
	}
	return super.createChild(name);
    }

    protected void createPropertyModel() {
	super.createPropertyModel();
	createSupportedContainerTableModel();
	createDSAttributeMapListTableModel();
    }
    
    private void createSupportedContainerTableModel() {
	CCActionTableModel tblModel = new CCActionTableModel(
	    getClass().getClassLoader().getResourceAsStream(
       "com/sun/identity/console/tblWSPersonalProfileSupportedContainers.xml"));
	tblModel.setTitleLabel("label.items");
	tblModel.setActionValue(TBL_SUPPORTED_CONTAINER_COL_NAME,
	    "webservices.personal.profile.table.supportedContainers.name");
	tblModel.setActionValue(TBL_SUPPORTED_CONTAINER_ADD_BTN,
	"webservices.personal.profile.table.supportedContainers.add.button");
	tblModel.setActionValue(TBL_SUPPORTED_CONTAINER_DELETE_BTN,
	"webservices.personal.profile.table.supportedContainers.delete.button");
	propertySheetModel.setModel(
	WSPersonalProfileServiceModelImpl.ATTRIBUTE_NAME_SUPPPORTED_CONTAINERS,
	    tblModel);
    }

    private void createDSAttributeMapListTableModel() {
	CCActionTableModel tblModel = new CCActionTableModel(
	    getClass().getClassLoader().getResourceAsStream(
	"com/sun/identity/console/tblWSPersonalProfileDSAttributeMapList.xml"));
	tblModel.setTitleLabel("label.items");
	tblModel.setActionValue(TBL_DS_ATTRIBUTE_MAP_LIST_COL_NAME,
	    "webservices.personal.profile.table.dsAttributeMapList.name");
	tblModel.setActionValue(TBL_DS_ATTRIBUTE_MAP_LIST_COL_MAP,
	    "webservices.personal.profile.table.dsAttributeMapList.map");
	tblModel.setActionValue(TBL_DS_ATTRIBUTE_MAP_LIST_ADD_BTN,
	"webservices.personal.profile.table.dsAttributeMapList.add.button");
	tblModel.setActionValue(TBL_DS_ATTRIBUTE_MAP_LIST_DELETE_BTN,
	"webservices.personal.profile.table.dsAttributeMapList.delete.button");
	propertySheetModel.setModel(
	WSPersonalProfileServiceModelImpl.ATTRIBUTE_NAME_DS_ATTRIBUTE_MAP_LIST,
	    tblModel);
    }

    protected AMModel getModelInternal() {
	HttpServletRequest req =
	    RequestManager.getRequestContext().getRequest();
	try {
	    return new WSPersonalProfileServiceModelImpl(
		req, serviceName, getPageSessionAttributes());
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
	}
	return null;
    }

    protected void createPageTitleModel() {
	createTwoButtonPageTitleModel();
    }

    private void prePopulateTable() {
	Map attributeValues = (Map)removePageSessionAttribute(
	    PROPERTY_ATTRIBUTE);
	if (attributeValues != null) {
	    AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
	    ps.setAttributeValues(attributeValues, getModel());
	}
	prePopulateSupportContainersTable(attributeValues);
	prePopulateDSAttributeMapListTable(attributeValues);
    }

    private void prePopulateSupportContainersTable(Map attributeValues) {
	Set supportedContainers = null;

	if (attributeValues != null) {
	    supportedContainers = (Set)attributeValues.get(
		WSPersonalProfileServiceModelImpl.
		ATTRIBUTE_NAME_SUPPPORTED_CONTAINERS);
	    if (supportedContainers == null) {
		supportedContainers = Collections.EMPTY_SET;
	    }
	} else {
	    supportedContainers = (Set)removePageSessionAttribute(
		WSPersonalProfileServiceModelImpl.
		ATTRIBUTE_NAME_SUPPPORTED_CONTAINERS);
	}

	if (supportedContainers != null) {
	    populateSupportedContainersTable(supportedContainers);
	}
    }

    private void prePopulateDSAttributeMapListTable(Map attributeValues) {
	Set mappingList = null;

	if (attributeValues != null) {
	    mappingList = (Set)attributeValues.get(
		WSPersonalProfileServiceModelImpl.
		ATTRIBUTE_NAME_DS_ATTRIBUTE_MAP_LIST);
	    if (mappingList == null) {
		mappingList = Collections.EMPTY_SET;
	    }
	} else {
	    mappingList = (Set)removePageSessionAttribute(
		WSPersonalProfileServiceModelImpl.
		ATTRIBUTE_NAME_DS_ATTRIBUTE_MAP_LIST);
	}

	if (mappingList != null) {
	    populateDSAttributeMapListTable(mappingList);
	}
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException {
	super.beginDisplay(event);

	if (!tablePopulated) {
	    if (!isSubmitCycle()) {
		AMServiceProfileModel model = (AMServiceProfileModel)getModel();

		if (model != null) {
		    Set supportedContainers = new OrderedSet();
		    supportedContainers.addAll(model.getAttributeValues(
			WSPersonalProfileServiceModelImpl.
			ATTRIBUTE_NAME_SUPPPORTED_CONTAINERS));
		    populateSupportedContainersTable(supportedContainers);

		    Set dsAttributeMapList = new OrderedSet();
		    dsAttributeMapList.addAll(model.getAttributeValues(
			WSPersonalProfileServiceModelImpl.
			ATTRIBUTE_NAME_DS_ATTRIBUTE_MAP_LIST));
		    populateDSAttributeMapListTable(dsAttributeMapList);
		}
	    }
	}
    }

    private void populateSupportedContainersTable(Set containers) {
	tablePopulated = true;
	CCActionTableModel tblModel =
	    (CCActionTableModel)propertySheetModel.getModel(
	WSPersonalProfileServiceModelImpl.ATTRIBUTE_NAME_SUPPPORTED_CONTAINERS);
	tblModel.clearAll();
	boolean firstEntry = true;
	int counter = 0;

	for (Iterator iter = containers.iterator(); iter.hasNext(); ) {
	    if (!firstEntry) {
		tblModel.appendRow();
	    } else {
		firstEntry = false;
	    }

	    String c = (String)iter.next();
	    StringTokenizer st = new StringTokenizer(c, "|");

	    while (st.hasMoreTokens()) {
		String tok = st.nextToken();
		int idx = tok.indexOf('=');
		if (idx != -1) {
		    String id = tok.substring(0, idx);
		    String val = tok.substring(idx+1);

		    if (id.equals(WSPersonalProfileServiceModelImpl.
			SUPPORTED_CONTAINER_CONTAINER_PREFIX)) {
			tblModel.setValue(
			    TBL_SUPPORTED_CONTAINER_DATA_NAME, val);
			tblModel.setValue(TBL_SUPPORTED_CONTAINER_HREF_ACTION,
			    Integer.toString(counter));
		    }
		}
	    }

	    counter++;
	}
	setPageSessionAttribute(
	WSPersonalProfileServiceModelImpl.ATTRIBUTE_NAME_SUPPPORTED_CONTAINERS,
	    (OrderedSet)containers);
    }

    private void populateDSAttributeMapListTable(Set attributeMap) {
	tablePopulated = true;
	CCActionTableModel tblModel =
	    (CCActionTableModel)propertySheetModel.getModel(
	WSPersonalProfileServiceModelImpl.ATTRIBUTE_NAME_DS_ATTRIBUTE_MAP_LIST);
	tblModel.clearAll();
	boolean firstEntry = true;
	int counter = 0;

	for (Iterator iter = attributeMap.iterator(); iter.hasNext(); ) {
	    if (!firstEntry) {
		tblModel.appendRow();
	    } else {
		firstEntry = false;
	    }

	    String mapping = (String)iter.next();
	    int idx = mapping.indexOf('=');

	    if (idx != -1) {
		String name = mapping.substring(0, idx);
		String attr = mapping.substring(idx+1);

		tblModel.setValue(TBL_DS_ATTRIBUTE_MAP_LIST_DATA_NAME, name);
		tblModel.setValue(TBL_DS_ATTRIBUTE_MAP_LIST_DATA_MAP, attr);
		tblModel.setValue(TBL_DS_ATTRIBUTE_MAP_LIST_HREF_ACTION,
		    Integer.toString(counter));
		tblModel.setValue(TBL_DS_ATTRIBUTE_MAP_LIST_LABEL_ACTION,
    "webservices.personal.profile.table.dsAttributeMapList.action.edit.label");
	    }
	    counter++;
	}
	setPageSessionAttribute(
	WSPersonalProfileServiceModelImpl.ATTRIBUTE_NAME_DS_ATTRIBUTE_MAP_LIST,
	    (OrderedSet)attributeMap);
    }

    protected boolean onBeforeSaveProfile(Map attrValues) {
	Set supportedContainers = (Set)getPageSessionAttribute(
	WSPersonalProfileServiceModelImpl.ATTRIBUTE_NAME_SUPPPORTED_CONTAINERS);

	if ((supportedContainers != null) && !supportedContainers.isEmpty()) {
	    attrValues.put(
	 WSPersonalProfileServiceModelImpl.ATTRIBUTE_NAME_SUPPPORTED_CONTAINERS,
		supportedContainers);
	}

	Set attributeMap = (Set)getPageSessionAttribute(
	WSPersonalProfileServiceModelImpl.ATTRIBUTE_NAME_DS_ATTRIBUTE_MAP_LIST);

	if ((attributeMap != null) && !attributeMap.isEmpty()) {
	    attrValues.put(
	 WSPersonalProfileServiceModelImpl.ATTRIBUTE_NAME_DS_ATTRIBUTE_MAP_LIST,
		attributeMap);
	}

	return true;
    }

    protected void onBeforeResetProfile() {
	removePageSessionAttribute(WSPersonalProfileServiceModelImpl.
	    ATTRIBUTE_NAME_SUPPPORTED_CONTAINERS);
	removePageSessionAttribute(WSPersonalProfileServiceModelImpl.
	    ATTRIBUTE_NAME_DS_ATTRIBUTE_MAP_LIST);
	tablePopulated = false;
    }

    /**
     * Handles remove supported container request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblSupportedContainerButtonDeleteRequest(
	RequestInvocationEvent event
    ) throws ModelControlException {
	setSubmitCycle(true);
	CCActionTable table = (CCActionTable)getChild(
	    WSPersonalProfileServiceModelImpl.
	    ATTRIBUTE_NAME_SUPPPORTED_CONTAINERS);
	table.restoreStateData();
	CCActionTableModel tblModel =
	    (CCActionTableModel)propertySheetModel.getModel(
	WSPersonalProfileServiceModelImpl.ATTRIBUTE_NAME_SUPPPORTED_CONTAINERS);
	Integer[] selected = tblModel.getSelectedRows();

	if ((selected != null) && (selected.length > 0)) {
	    OrderedSet tblValues = (OrderedSet)getPageSessionAttribute(
		WSPersonalProfileServiceModelImpl.
		ATTRIBUTE_NAME_SUPPPORTED_CONTAINERS);
	    tblValues.removeAll(selected);
	    setPageSessionAttribute(
		WSPersonalProfileServiceModelImpl.
		ATTRIBUTE_NAME_SUPPPORTED_CONTAINERS, tblValues);
	    populateSupportedContainersTable(tblValues);
	}

	resetButtonState(TBL_SUPPORTED_CONTAINER_DELETE_BTN);
	forwardTo();
    }

    /**
     * Handles add supported container request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblSupportedContainerButtonAddRequest(
	RequestInvocationEvent event
    ) throws ModelControlException {
	try {
	    Map values = getValues();
	    onBeforeSaveProfile(values);
	    setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
	    WSPPServiceSupportedContainerAddViewBean vb =
		(WSPPServiceSupportedContainerAddViewBean)getViewBean(
		    WSPPServiceSupportedContainerAddViewBean.class);
	    unlockPageTrail();
	    passPgSessionMap(vb);
	    vb.forwardTo(getRequestContext());
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
	    forwardTo();
	}
    }

    /**
     * Handles edit supported container request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblSupportedContainerHrefActionRequest(
	RequestInvocationEvent event
    ) throws ModelControlException {
	try {
	    Map values = getValues();
	    onBeforeSaveProfile(values);
	    setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
	    WSPPServiceSupportedContainerEditViewBean vb = 
		(WSPPServiceSupportedContainerEditViewBean)getViewBean(
		    WSPPServiceSupportedContainerEditViewBean.class);
	    unlockPageTrail();
	    passPgSessionMap(vb);
	    vb.populateValues((String)getDisplayFieldValue(
		"tblSupportedContainerHrefAction"));
	    vb.forwardTo(getRequestContext());
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
	    forwardTo();
	}
    }

    /**
     * Handles remove LDAP Attribute Mapping request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblDSAttributeMapListButtonDeleteRequest(
	RequestInvocationEvent event
    ) throws ModelControlException {
	setSubmitCycle(true);
	CCActionTable table = (CCActionTable)getChild(
	    WSPersonalProfileServiceModelImpl.
	    ATTRIBUTE_NAME_DS_ATTRIBUTE_MAP_LIST);
	table.restoreStateData();
	CCActionTableModel tblModel =
	    (CCActionTableModel)propertySheetModel.getModel(
	WSPersonalProfileServiceModelImpl.ATTRIBUTE_NAME_DS_ATTRIBUTE_MAP_LIST);
	Integer[] selected = tblModel.getSelectedRows();

	if ((selected != null) && (selected.length > 0)) {
	    OrderedSet tblValues = (OrderedSet)getPageSessionAttribute(
		WSPersonalProfileServiceModelImpl.
		ATTRIBUTE_NAME_DS_ATTRIBUTE_MAP_LIST);
	    tblValues.removeAll(selected);
	    setPageSessionAttribute(
		WSPersonalProfileServiceModelImpl.
		ATTRIBUTE_NAME_DS_ATTRIBUTE_MAP_LIST, tblValues);
	    populateDSAttributeMapListTable(tblValues);
	}

	resetButtonState(TBL_DS_ATTRIBUTE_MAP_LIST_DELETE_BTN);
	forwardTo();
    }

    /**
     * Handles add LDAP Attribute Mapping request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblDSAttributeMapListButtonAddRequest(
	RequestInvocationEvent event
    ) throws ModelControlException {
	try {
	    Map values = getValues();
	    onBeforeSaveProfile(values);
	    setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
	    WSPPServiceDSAttributeMapListAddViewBean vb =
		(WSPPServiceDSAttributeMapListAddViewBean)getViewBean(
		    WSPPServiceDSAttributeMapListAddViewBean.class);
	    unlockPageTrail();
	    passPgSessionMap(vb);
	    vb.forwardTo(getRequestContext());
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
	    forwardTo();
	}
    }

    /**
     * Handles edit LDAP Attribute Mapping request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblDSAttributeMapListHrefActionRequest(
	RequestInvocationEvent event
    ) throws ModelControlException {
	try {
	    Map values = getValues();
	    onBeforeSaveProfile(values);
	    setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
	    WSPPServiceDSAttributeMapListEditViewBean vb = 
		(WSPPServiceDSAttributeMapListEditViewBean)getViewBean(
		    WSPPServiceDSAttributeMapListEditViewBean.class);
	    unlockPageTrail();
	    passPgSessionMap(vb);
	    vb.populateValues((String)getDisplayFieldValue(
		"tblDSAttributeMapListHrefAction"));
	    vb.forwardTo(getRequestContext());
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
	    forwardTo();
	}
    }

    protected String getBreadCrumbDisplayName() {
	return "breadcrumbs.webservices.personalprofile";
    }
}
