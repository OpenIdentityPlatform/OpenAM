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
 * $Id: SCSOAPBindingRequestHandlerListEditViewBean.java,v 1.2 2008/06/25 05:49:44 qcheng Exp $
 *
 */
 
package com.sun.identity.console.service;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.service.model.SCSOAPBindingModelImpl;
import com.sun.identity.console.service.model.SOAPBindingRequestHandler;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.Iterator;
import java.util.Map;

public class SCSOAPBindingRequestHandlerListEditViewBean
    extends SCSOAPBindingRequestHandlerListViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/service/SCSOAPBindingRequestHandlerListEdit.jsp";
    private static final String PGATTR_INDEX = "handlerListTblIndex";
    private boolean populateValues = false;

    public SCSOAPBindingRequestHandlerListEditViewBean() {
	super("SCSOAPBindingRequestHandlerListEdit", DEFAULT_DISPLAY_URL);
    }

    void populateValues(String index) {
	setPageSessionAttribute(PGATTR_INDEX, index);
	populateValues = true;
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException {
	super.beginDisplay(event);

	if (populateValues) {
	    int index = Integer.parseInt((String)
		getPageSessionAttribute(PGATTR_INDEX));

	    Map mapAttrs = (Map)getPageSessionAttribute(
		SCSOAPBindingViewBean.PROPERTY_ATTRIBUTE);
	    OrderedSet set = (OrderedSet)mapAttrs.get(
		SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
	    setValues((String)set.get(index));
	}
    }

    protected void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/threeBtnsPageTitle.xml"));
	
        ptModel.setPageTitleText(
            "soapBinding.service.requestHandlerList.edit.page.title");

	ptModel.setValue("button1", "button.save");
	ptModel.setValue("button2", "button.reset");
	ptModel.setValue("button3", 
            getBackButtonLabel("breadcrumbs.webservices.soapbinding"));
    }

    protected void handleButton1Request(Map values)
	throws AMConsoleException {
	SCSOAPBindingViewBean vb = (SCSOAPBindingViewBean)getViewBean(
	    SCSOAPBindingViewBean.class);

	Map mapAttrs = (Map)getPageSessionAttribute(
	    SCSOAPBindingViewBean.PROPERTY_ATTRIBUTE);
	OrderedSet serverList = (OrderedSet)mapAttrs.get(
	    SCSOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
	int index = Integer.parseInt((String)
	    getPageSessionAttribute(PGATTR_INDEX));
	String val = SOAPBindingRequestHandler.toString(
	    (String)values.get(ATTR_KEY),
	    (String)values.get(ATTR_CLASS),
	    (String)values.get(ATTR_ACTION));
	int count = 0;

	for (Iterator i = serverList.iterator(); i.hasNext(); ) {
	    String v = (String)i.next();
	    if ((count != index) && v.equals(val)) {
		throw new AMConsoleException(
		    "soapBinding.service.requestHandlerList.already.exist");
	    }
	    count++;
	}

	serverList.set(index, val);
	setPageSessionAttribute(SCSOAPBindingViewBean.PAGE_MODIFIED, "1");
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
	populateValues = true;
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
	boolean canModify = dConfig.hasPermission("/", null,
	    AMAdminConstants.PERMISSION_MODIFY,
	    getRequestContext().getRequest(), getClass().getName());

	String xmlFile = (canModify) ?
       "com/sun/identity/console/propertySCSOAPBindingRequestHandlerList.xml":
"com/sun/identity/console/propertySCSOAPBindingRequestHandlerList_Readonly.xml";

	propertySheetModel = new AMPropertySheetModel(
	    getClass().getClassLoader().getResourceAsStream(xmlFile));
	propertySheetModel.clear();
    }

    protected String getBreadCrumbDisplayName() {
	return "breadcrumbs.webservices.soapbinding.requesthandlerlist.edit";
    }

    protected boolean startPageTrail() {
	return false;
    }
}
