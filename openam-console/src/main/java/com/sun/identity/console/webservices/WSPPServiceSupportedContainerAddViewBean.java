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
 * $Id: WSPPServiceSupportedContainerAddViewBean.java,v 1.2 2008/06/25 05:49:50 qcheng Exp $
 *
 */

package com.sun.identity.console.webservices;

import com.sun.identity.console.webservices.model.WSPersonalProfileServiceModelImpl;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.Map;
import java.util.Set;

public class WSPPServiceSupportedContainerAddViewBean
    extends WSPPServiceSupportedContainerViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/webservices/WSPPServiceSupportedContainerAdd.jsp";

    public WSPPServiceSupportedContainerAddViewBean(
	String pageName,
	String defaultDisplayURL
    ) {
	super(pageName, defaultDisplayURL);
    }

    public WSPPServiceSupportedContainerAddViewBean() {
	super("WSPPServiceSupportedContainerAdd", DEFAULT_DISPLAY_URL);
    }

    protected void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/twoBtnsPageTitle.xml"));

	ptModel.setPageTitleText(
            "webservices.personal.profile.supportedContainer.create.page.title");

	ptModel.setValue("button1", "button.ok");
	ptModel.setValue("button2", "button.cancel");
    }


    protected void handleButton1Request(Map values) {
	WSPersonalProfileServiceViewBean vb = (WSPersonalProfileServiceViewBean)
	    getViewBean(WSPersonalProfileServiceViewBean.class);
	Map attrValues = (Map)getPageSessionAttribute(
	    WSPersonalProfileServiceViewBean.PROPERTY_ATTRIBUTE);
	Set containers = (Set)attrValues.get(
            WSPersonalProfileServiceModelImpl.ATTRIBUTE_NAME_SUPPPORTED_CONTAINERS);

	if ((containers == null) || containers.isEmpty()) {
	    containers = new OrderedSet();
	    attrValues.put(
        	WSPersonalProfileServiceModelImpl.ATTRIBUTE_NAME_SUPPPORTED_CONTAINERS,
        	(OrderedSet)containers);
	}

	containers.add(mapToString(values));
	backTrail();
	unlockPageTrailForSwapping();
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
	return "breadcrumbs.webservices.personalprofile.supportedcontainer.add";
    }

    protected boolean startPageTrail() {
	return false;
    }
}
