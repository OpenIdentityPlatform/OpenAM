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
 * $Id: SMDiscoveryBootstrapRefOffAddViewBean.java,v 1.2 2008/06/25 05:49:44 qcheng Exp $
 *
 */
 
package com.sun.identity.console.service;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.service.model.SMDiscoEntryData;
import com.sun.identity.console.service.model.SMDiscoveryServiceData;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.Map;
import java.util.Set;

public class SMDiscoveryBootstrapRefOffAddViewBean
    extends SMDiscoveryBootstrapRefOffViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/service/SMDiscoveryBootstrapRefOffAdd.jsp";

    public SMDiscoveryBootstrapRefOffAddViewBean() {
	super("SMDiscoveryBootstrapRefOffAdd", DEFAULT_DISPLAY_URL);
    }

    protected void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/twoBtnsPageTitle.xml"));
	
        ptModel.setPageTitleText(
            "discovery.service.bootstrapResOff.create.page.title");

	ptModel.setValue("button1", "button.ok");
	ptModel.setValue("button2", "button.cancel");
    }

    protected SMDiscoEntryData getCurrentServiceData() {
	return null;
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException {
	super.beginDisplay(event);
	SMDiscoEntryData data = (SMDiscoEntryData)getPageSessionAttribute(
	    PROPERTY_ATTRIBUTE);
	if (data != null) {
	    populateDirectiveMechIDRefs(data);
	}
    }

    protected void handleButton1Request(SMDiscoEntryData smData) {
	SMDiscoveryServiceViewBean vb = (SMDiscoveryServiceViewBean)
	    getViewBean(SMDiscoveryServiceViewBean.class);
	Map attrValues = (Map)getPageSessionAttribute(
	    SMDiscoveryServiceViewBean.PROPERTY_ATTRIBUTE);
	Set resoff = (Set)attrValues.get(
	    AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF);

	try {
	    SMDiscoveryServiceData smEntry =
		((resoff == null) || resoff.isEmpty())
		? new SMDiscoveryServiceData() :
		SMDiscoveryServiceData.getEntries(resoff);
	    smEntry.addResourceData(smData);
	    attrValues.put(
		AMAdminConstants.DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF,
		    (OrderedSet)smEntry.getDiscoveryEntries());
	    setPageSessionAttribute(
		SMDiscoveryServiceViewBean.PAGE_MODIFIED, "1");
	    backTrail();
	    unlockPageTrailForSwapping();
	    passPgSessionMap(vb);
	    vb.forwardTo(getRequestContext());
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
	    forwardTo();
	}
    }

    protected String getBreadCrumbDisplayName() {
	return "breadcrumbs.webservices.discovery.bootstrap.resoffering.add";
    }

    protected boolean startPageTrail() {
	return false;
    }
}
