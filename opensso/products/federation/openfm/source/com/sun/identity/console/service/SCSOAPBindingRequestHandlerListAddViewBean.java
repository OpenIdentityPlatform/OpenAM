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
 * $Id: SCSOAPBindingRequestHandlerListAddViewBean.java,v 1.2 2008/06/25 05:49:44 qcheng Exp $
 *
 */
 
package com.sun.identity.console.service;

import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.service.model.SCSOAPBindingModelImpl;
import com.sun.identity.console.service.model.SOAPBindingRequestHandler;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.Map;
import java.util.Set;

public class SCSOAPBindingRequestHandlerListAddViewBean
    extends SCSOAPBindingRequestHandlerListViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/service/SCSOAPBindingRequestHandlerListAdd.jsp";

    public SCSOAPBindingRequestHandlerListAddViewBean(
	String name,
	String defaultURL
    ) {
	super(name, defaultURL);
    }

    public SCSOAPBindingRequestHandlerListAddViewBean() {
	super("SCSOAPBindingRequestHandlerListAdd", DEFAULT_DISPLAY_URL);
    }

    protected void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/twoBtnsPageTitle.xml"));

	ptModel.setPageTitleText(
            "soapBinding.service.requestHandlerList.create.page.title");

	ptModel.setValue("button1", "button.ok");
	ptModel.setValue("button2", "button.cancel");
    }

    protected void handleButton1Request(Map values)
	throws AMConsoleException {
	SCSOAPBindingViewBean vb = (SCSOAPBindingViewBean)getViewBean(
	    SCSOAPBindingViewBean.class);
	Map attrValues = (Map)getPageSessionAttribute(
	    SCSOAPBindingViewBean.PROPERTY_ATTRIBUTE);
	Set handlers = (Set)attrValues.get(
	    SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);

	if ((handlers == null) || handlers.isEmpty()) {
	    handlers = new OrderedSet();
	    attrValues.put(
		SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST,
		(OrderedSet)handlers);
	}

	String val = SOAPBindingRequestHandler.toString(
	    (String)values.get(ATTR_KEY),
	    (String)values.get(ATTR_CLASS),
	    (String)values.get(ATTR_ACTION));

	if (handlers.contains(val)) {
	    throw new AMConsoleException(
		"soapBinding.service.requestHandlerList.already.exist");
	}

	handlers.add(val);
	setPageSessionAttribute(SCSOAPBindingViewBean.PAGE_MODIFIED, "1");
	backTrail();
	unlockPageTrailForSwapping();
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
      return "breadcrumbs.webservices.soapbinding.requesthandlerlist.add";
    }

    protected boolean startPageTrail() {
      return false;
    }
}
