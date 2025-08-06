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
 * $Id: WSAuthNServicesHandlersViewBeanBase.java,v 1.3 2008/11/26 18:21:43 farble1670 Exp $
 *
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.console.webservices;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.webservices.model.WSAuthHandlerEntry;
import com.sun.identity.console.webservices.model.WSAuthNServicesModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;

public abstract class WSAuthNServicesHandlersViewBeanBase
    extends AMPrimaryMastHeadViewBean
{
    private static final String PGTITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    static final String ATTR_KEY = "key";
    static final String ATTR_CLASS = "class";

    protected CCPageTitleModel ptModel;
    protected AMPropertySheetModel propertySheetModel;

    public WSAuthNServicesHandlersViewBeanBase(
	String pageName,
	String defaultDisplayURL
    ) {
	super(pageName);
	setDefaultDisplayURL(defaultDisplayURL);
    }

    protected void initialize() {
        if (!initialized) {
	    super.initialize();
	    initialized = true;
	    createPageTitleModel();
	    createPropertyModel();
	    registerChildren();
        }
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE, CCPageTitle.class);
        ptModel.registerChildren(this);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        propertySheetModel.registerChildren(this);
    }

    protected View createChild(String name) {
	View view = null;

	if (name.equals(PGTITLE)) {
	    view = new CCPageTitle(this, ptModel, name);
	} else if (ptModel.isChildSupported(name)) {
	    view = ptModel.createChild(this, name);
	} else if (name.equals(PROPERTY_ATTRIBUTE)) {
	    view = new AMPropertySheet(this, propertySheetModel, name);
	} else if (propertySheetModel.isChildSupported(name)) {
	    view = propertySheetModel.createChild(this, name, getModel());
	} else {
	    view = super.createChild(name);
	}

	return view;
    }



    protected void createPropertyModel() {
	propertySheetModel = new AMPropertySheetModel(
	    getClass().getClassLoader().getResourceAsStream(
	    "com/sun/identity/console/propertyWSAuthNServicesHandlers.xml"));
	propertySheetModel.clear();
    }

    protected AMModel getModelInternal() {
	AMModel model = null;
	HttpServletRequest req =
	    RequestManager.getRequestContext().getRequest();

	try {
	    model = new WSAuthNServicesModelImpl(
		req, getPageSessionAttributes());
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
	}

	return model;
    }

    protected void forwardToAuthServicesView(RequestInvocationEvent event) {
        WSAuthNServicesViewBean vb = (WSAuthNServicesViewBean)getViewBean(
            WSAuthNServicesViewBean.class);
        backTrail();
        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected void setValues(String value) {
	WSAuthHandlerEntry entry = new WSAuthHandlerEntry(value);
	propertySheetModel.setValue(ATTR_KEY, entry.strKey);
	propertySheetModel.setValue(ATTR_CLASS, entry.strClass);
    }

    private String getValues(Map map) {
	String key = (String)propertySheetModel.getValue(ATTR_KEY);
	String clazz = (String)propertySheetModel.getValue(ATTR_CLASS);
	key = key.trim();
	clazz = clazz.trim();
	map.put(ATTR_KEY, key);
	map.put(ATTR_CLASS, clazz);

	String errorMsg = null;

	if (key.length() == 0) {
	    errorMsg =
	    "webservices.authentication.service.handlers.missing.key.message";
	} else if (clazz.length() == 0) {
	    errorMsg =
	    "webservices.authentication.service.handlers.missing.class.message";
	}

	return errorMsg;
    }

    public void handleButton1Request(RequestInvocationEvent event)
	throws ModelControlException
    {
	Map values = new HashMap(6);
	String errorMsg = getValues(values);

	if (errorMsg != null) {
	    setInlineAlertMessage(
		CCAlert.TYPE_ERROR, "message.error", errorMsg);
	    forwardTo();
	} else {
	    handleButton1Request(values);
	}
    }
    
    protected static boolean handlerExists(Set<String> handlers, WSAuthHandlerEntry entry) {
        // validate that we aren't adding a dup key
        for (String s: handlers) {
            WSAuthHandlerEntry e = new WSAuthHandlerEntry(s);
            if (e.strKey.equals(entry.strKey)) {
                return true;
            }
        }
        
        return false;
    }
    
    protected abstract void createPageTitleModel();
    protected abstract void handleButton1Request(Map values);
}
