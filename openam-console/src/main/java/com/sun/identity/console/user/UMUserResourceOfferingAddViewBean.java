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
 * $Id: UMUserResourceOfferingAddViewBean.java,v 1.3 2008/10/01 16:19:05 babysunil Exp $
 *
 */

package com.sun.identity.console.user;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.idm.EntityEditViewBean;
import com.sun.identity.console.service.model.SMDiscoveryServiceData;
import com.sun.identity.console.service.model.SMDiscoEntryData;
import com.sun.identity.console.user.model.UMUserResourceOfferingModel;
import com.sun.identity.console.service.model.DiscoveryDataCache;
import com.sun.web.ui.view.alert.CCAlert;

public class UMUserResourceOfferingAddViewBean
    extends UMUserResourceOfferingViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/user/UMUserResourceOfferingAdd.jsp";

    public UMUserResourceOfferingAddViewBean() {
	super("UMUserResourceOfferingAdd", DEFAULT_DISPLAY_URL);
    }

    protected String getButtonlLabel() {
	return "button.save";
    }

    protected String getPageTitleText() {
	return "discovery.service.bootstrapResOff.create.page.title";
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
	UMUserResourceOfferingViewBean vb = (UMUserResourceOfferingViewBean)
	    getViewBean(UMUserResourceOfferingViewBean.class);
	UMUserResourceOfferingModel model =
	    (UMUserResourceOfferingModel)getModel();
	DiscoveryDataCache cache = DiscoveryDataCache.getInstance();
	String cacheID = (String)getPageSessionAttribute(
	    UMUserResourceOfferingViewBean.DATA_ID);

	SMDiscoveryServiceData data = (SMDiscoveryServiceData)cache.getData(
	    model.getUserSSOToken(), cacheID);
	data.addResourceData(smData);

	String userId = (String)getPageSessionAttribute(
	    EntityEditViewBean.UNIVERSAL_ID);

	try {
	    model.setUserDiscoEntry(userId, data);
	    removePageSessionAttribute(UMUserResourceOfferingViewBean.DATA_ID);
	    passPgSessionMap(vb);
	    vb.forwardTo(getRequestContext());
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
	    forwardTo();
	}
    }
}
