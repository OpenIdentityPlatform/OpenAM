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
 * $Id: SMDiscoveryBootstrapRefOffEditViewBean.java,v 1.2 2008/06/25 05:49:44 qcheng Exp $
 *
 */
 
package com.sun.identity.console.service;

import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.service.model.SMDiscoEntryData;
import com.sun.identity.console.service.model.SMDiscoveryServiceData;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SMDiscoveryBootstrapRefOffEditViewBean
    extends SMDiscoveryBootstrapRefOffViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/service/SMDiscoveryBootstrapRefOffEdit.jsp";
    private static final String PGATTR_INDEX = "resoffTblIndex";
    private boolean populateValues = false;

    public SMDiscoveryBootstrapRefOffEditViewBean() {
	super("SMDiscoveryBootstrapRefOffEdit", DEFAULT_DISPLAY_URL);
    }

    void populateValues(String index) {
	setPageSessionAttribute(PGATTR_INDEX, index);
	populateValues = true;
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException {
	super.beginDisplay(event);

	if (populateValues) {
	    SMDiscoEntryData data = getCurrentServiceData();
	    setValues(data, getModel());
	} else {
	    SMDiscoEntryData data = (SMDiscoEntryData)getPageSessionAttribute(
		PROPERTY_ATTRIBUTE);
	    if (data != null) {
		populateDirectiveMechIDRefs(data);
	    }
	}
    }

    protected SMDiscoEntryData getCurrentServiceData() {
	SMDiscoEntryData curData = null;
	int currentIdx = Integer.parseInt((String)
	    getPageSessionAttribute(PGATTR_INDEX));
	Map mapAttrs = (Map)getPageSessionAttribute(
	    SMDiscoveryServiceViewBean.PROPERTY_ATTRIBUTE);
	OrderedSet set = (OrderedSet)mapAttrs.get(
	    AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF);

	try {
	    SMDiscoveryServiceData smEntry = SMDiscoveryServiceData.getEntries(
		set);
	    List resourceData = smEntry.getResourceData();
	    curData = (SMDiscoEntryData)resourceData.get(currentIdx);
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
	}

	return curData;
    }

    protected void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/threeBtnsPageTitle.xml"));
	
        ptModel.setPageTitleText(
            "discovery.service.bootstrapResOff.edit.page.title");

	ptModel.setValue("button1", "button.save");
	ptModel.setValue("button2", "button.reset");
	ptModel.setValue("button3", 
            getBackButtonLabel("breadcrumbs.webservices.discovery"));
    }

    protected void handleButton1Request(SMDiscoEntryData smData)
	throws AMConsoleException
    {
	SMDiscoveryServiceViewBean vb = (SMDiscoveryServiceViewBean)
	    getViewBean(SMDiscoveryServiceViewBean.class);

	Map mapAttrs = (Map)getPageSessionAttribute(
	    SMDiscoveryServiceViewBean.PROPERTY_ATTRIBUTE);
	OrderedSet resoff = (OrderedSet)mapAttrs.get(
	    AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF);
	SMDiscoveryServiceData smEntry =
	    SMDiscoveryServiceData.getEntries(resoff);
	int index = Integer.parseInt((String)
	    getPageSessionAttribute(PGATTR_INDEX));
	smEntry.replaceResourceData(index, smData);
	mapAttrs.put(
	    AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF,
	    (OrderedSet)smEntry.getDiscoveryEntries());
	setPageSessionAttribute(
	    SMDiscoveryServiceViewBean.PROPERTY_ATTRIBUTE,
	    (HashMap)mapAttrs);
	setPageSessionAttribute(SMDiscoveryServiceViewBean.PAGE_MODIFIED, "1");
	backTrail();
	unlockPageTrailForSwapping();
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    /**
     * Handles reset request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
	removePageSessionAttribute(PROPERTY_ATTRIBUTE);
	populateValues = true;
	tablePopulated = false;
        forwardTo();
    }

    /**
     * Handles "back to " previous page request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        super.handleButton2Request(event);
    }

    protected void createPropertyModel() {
	DelegationConfig dConfig = DelegationConfig.getInstance();
	canModify = dConfig.hasPermission("/", null,
	    AMAdminConstants.PERMISSION_MODIFY,
	    getRequestContext().getRequest(), getClass().getName());

	String xmlFile = (canModify) ?
	    "com/sun/identity/console/propertySMDiscoveryBootstrapResOff.xml":
    "com/sun/identity/console/propertySMDiscoveryBootstrapResOff_Readonly.xml";

	InputStream is = getClass().getClassLoader().getResourceAsStream(
	    xmlFile);
	String xml = AMAdminUtils.getStringFromInputStream(is);
	propertySheetModel = new AMPropertySheetModel(
	    processPropertiesXML(xml));

	propertySheetModel.clear();
	createSecurityMechIDTable();
    }

    protected String getBreadCrumbDisplayName() {
	return "breadcrumbs.webservices.discovery.bootstrap.resoffering.edit";
    }

    protected boolean startPageTrail() {
	return false;
    }
}
