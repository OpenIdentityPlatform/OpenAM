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
 * $Id: SMDiscoveryProviderResourceIdMapperAddViewBean.java,v 1.2 2008/06/25 05:49:45 qcheng Exp $
 *
 */
 
package com.sun.identity.console.service;

import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.Map;
import java.util.Set;

public class SMDiscoveryProviderResourceIdMapperAddViewBean
    extends SMDiscoveryProviderResourceIdMapperViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/service/SMDiscoveryProviderResourceIdMapperAdd.jsp";

    public SMDiscoveryProviderResourceIdMapperAddViewBean() {
	super("SMDiscoveryProviderResourceIdMapperAdd", DEFAULT_DISPLAY_URL);
    }

    protected void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/twoBtnsPageTitle.xml"));
	ptModel.setPageTitleText(
            "discovery.service.providerResourceIdMapper.create.page.title");
	
        ptModel.setValue("button1", "button.ok");
	ptModel.setValue("button2", "button.cancel");
    }

    protected void handleButton1Request(Map data) {
	SMDiscoveryServiceViewBean vb = (SMDiscoveryServiceViewBean)
	    getViewBean(SMDiscoveryServiceViewBean.class);
	Map attrValues = (Map)getPageSessionAttribute(
	    SMDiscoveryServiceViewBean.PROPERTY_ATTRIBUTE);
	Set mapper = (Set)attrValues.get(
	    AMAdminConstants.DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER);
	if (mapper == null) {
	    mapper = new OrderedSet();
	    attrValues.put(
		AMAdminConstants.DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER,
		mapper);
	}
	mapper.add(
	    AMAdminConstants.
		DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER_PROVIDER_ID +
	    "=" + (String)data.get(ATTR_PROVIDERID) + "|" +
	    AMAdminConstants.
		DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER_ID_MAPPER +
	    "=" + (String)data.get(ATTR_IDMAPPER));

	setPageSessionAttribute(SMDiscoveryServiceViewBean.PAGE_MODIFIED, "1");
	backTrail();
	unlockPageTrailForSwapping();
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
	return "breadcrumbs.webservices.discovery.resourceid.mapper.add";
    }

    protected boolean startPageTrail() {
	return false;
    }
}
