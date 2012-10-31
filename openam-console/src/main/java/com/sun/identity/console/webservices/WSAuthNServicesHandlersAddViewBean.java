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
 * $Id: WSAuthNServicesHandlersAddViewBean.java,v 1.3 2008/11/26 18:21:43 farble1670 Exp $
 *
 */

package com.sun.identity.console.webservices;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.identity.console.webservices.model.WSAuthNServicesModelImpl;
import com.sun.identity.console.webservices.model.WSAuthHandlerEntry;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.Map;
import java.util.Set;

public class WSAuthNServicesHandlersAddViewBean
    extends WSAuthNServicesHandlersViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/webservices/WSAuthNServicesHandlersAdd.jsp";

    public WSAuthNServicesHandlersAddViewBean() {
	super("WSAuthNServicesHandlersAdd", DEFAULT_DISPLAY_URL);
    }

    protected void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/twoBtnsPageTitle.xml"));
	
        ptModel.setPageTitleText(
            "webservices.authentication.service.handlers.create.page.title");

	ptModel.setValue("button1", "button.ok");
	ptModel.setValue("button2", "button.cancel");
    }

    protected void handleButton1Request(Map values) {
	WSAuthNServicesViewBean vb = (WSAuthNServicesViewBean)getViewBean(
	    WSAuthNServicesViewBean.class);
	Map attrValues = (Map)getPageSessionAttribute(
	    WSAuthNServicesViewBean.PROPERTY_ATTRIBUTE);
	Set handlers = (Set)attrValues.get(
	    WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS);

	if ((handlers == null) || handlers.isEmpty()) {
	    handlers = new OrderedSet();
	    attrValues.put(WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS,
		(OrderedSet)handlers);
	}

        String key = (String)values.get(ATTR_KEY);
        String val = (String)values.get(ATTR_CLASS);
        WSAuthHandlerEntry e = new WSAuthHandlerEntry(key, val);
        
        if (handlerExists(handlers, e)) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", "webservices.authentication.service.handlers.duplicates");
            forwardTo(getRequestContext());
            return;
        }
        
	handlers.add(e.toString());
	setPageSessionAttribute(WSAuthNServicesViewBean.PAGE_MODIFIED, "1");
	backTrail();
	unlockPageTrailForSwapping();
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }
    
    /**
     * Handles cancel request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardToAuthServicesView(event);
    }

    protected String getBreadCrumbDisplayName() {
	return "breadcrumbs.webservices.authentication.mechanism.handler.add";
    }

    protected boolean startPageTrail() {
	return false;
    }

}
