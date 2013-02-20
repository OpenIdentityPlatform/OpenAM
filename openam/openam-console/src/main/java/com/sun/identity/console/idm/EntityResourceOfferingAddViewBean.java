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
 * $Id: EntityResourceOfferingAddViewBean.java,v 1.2 2008/06/25 05:49:41 qcheng Exp $
 *
 */

package com.sun.identity.console.idm;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.idm.model.EntityResourceOfferingModel;
import com.sun.identity.console.service.model.SMDiscoveryServiceData;
import com.sun.identity.console.service.model.SMDiscoEntryData;
import com.sun.identity.console.service.model.DiscoveryDataCache;
import com.sun.web.ui.view.alert.CCAlert;

public class EntityResourceOfferingAddViewBean
    extends EntityResourceOfferingViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/idm/EntityResourceOfferingAdd.jsp";

    public EntityResourceOfferingAddViewBean() {
	super("EntityResourceOfferingAdd", DEFAULT_DISPLAY_URL);
    }

    protected String getButtonlLabel() {
	return "button.ok";
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
	EntityResourceOfferingViewBean vb = (EntityResourceOfferingViewBean)
	    getViewBean(EntityResourceOfferingViewBean.class);
	EntityResourceOfferingModel model =
	    (EntityResourceOfferingModel)getModel();
	DiscoveryDataCache cache = DiscoveryDataCache.getInstance();
	String cacheID = (String)getPageSessionAttribute(
	    EntityResourceOfferingViewBean.DATA_ID);

	SMDiscoveryServiceData data = (SMDiscoveryServiceData)cache.getData(
	    model.getUserSSOToken(), cacheID);
	data.addResourceData(smData);
	String univId = (String)getPageSessionAttribute(
	    EntityEditViewBean.UNIVERSAL_ID);

	try {
	    model.setEntityDiscoEntry(univId, data);
	    removePageSessionAttribute(EntityResourceOfferingViewBean.DATA_ID);
	    passPgSessionMap(vb);
	    vb.forwardTo(getRequestContext());
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
	    forwardTo();
	}
    }
}
