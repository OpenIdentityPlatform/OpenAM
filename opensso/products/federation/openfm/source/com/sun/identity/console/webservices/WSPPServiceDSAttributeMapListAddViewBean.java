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
 * $Id: WSPPServiceDSAttributeMapListAddViewBean.java,v 1.2 2008/06/25 05:49:50 qcheng Exp $
 *
 */

package com.sun.identity.console.webservices;

import com.sun.identity.console.webservices.model.WSPersonalProfileServiceModelImpl;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.Map;
import java.util.Set;

public class WSPPServiceDSAttributeMapListAddViewBean
    extends WSPPServiceDSAttributeMapListViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/webservices/WSPPServiceDSAttributeMapListAdd.jsp";

    public WSPPServiceDSAttributeMapListAddViewBean(
	String pageName,
	String defaultDisplayURL
    ) {
	super(pageName, defaultDisplayURL);
    }

    public WSPPServiceDSAttributeMapListAddViewBean() {
	super("WSPPServiceDSAttributeMapListAdd", DEFAULT_DISPLAY_URL);
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setPageTitleText(getPageTitleText());
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.cancel");
    }

    protected String getPageTitleText() {
	return
	"webservices.personal.profile.dsAttributeMapList.create.page.title";
    }

    protected void handleButton1Request(Map values) {
	WSPersonalProfileServiceViewBean vb = (WSPersonalProfileServiceViewBean)
	    getViewBean(WSPersonalProfileServiceViewBean.class);
	Map attrValues = (Map)getPageSessionAttribute(
	    WSPersonalProfileServiceViewBean.PROPERTY_ATTRIBUTE);
	Set mappings = (Set)attrValues.get(
    WSPersonalProfileServiceModelImpl.ATTRIBUTE_NAME_DS_ATTRIBUTE_MAP_LIST);

	if ((mappings == null) || mappings.isEmpty()) {
	    mappings = new OrderedSet();
	    attrValues.put(
	WSPersonalProfileServiceModelImpl.ATTRIBUTE_NAME_DS_ATTRIBUTE_MAP_LIST,
		(OrderedSet)mappings);
	}

	mappings.add(
	    (String)values.get(ATTR_NAME) + "=" +
	    (String)values.get(ATTR_MAPPING_ATTRIBUTE));
	backTrail();
	unlockPageTrailForSwapping();
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
	return
	    "breadcrumbs.webservices.personalprofile.ds.attributemaplist.add";
    }

    protected boolean startPageTrail() {
	return false;
    }
}
