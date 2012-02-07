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
 * $Id: WSPPServiceSupportedContainerViewBeanBase.java,v 1.2 2008/06/25 05:49:50 qcheng Exp $
 *
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
import com.sun.identity.console.webservices.model.WSPersonalProfileServiceModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;

public abstract class WSPPServiceSupportedContainerViewBeanBase
    extends AMPrimaryMastHeadViewBean
{
    private static final String PGTITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    static final String ATTR_CONTAINER_NAME = "containerName";
    static final String ATTR_PLUGIN = "plugin";

    protected CCPageTitleModel ptModel;
    protected AMPropertySheetModel propertySheetModel;

    public WSPPServiceSupportedContainerViewBeanBase(
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
"com/sun/identity/console/propertyWSPersonalProfileSupportedContainers.xml"));
	propertySheetModel.clear();
    }

    protected AMModel getModelInternal() {
	AMModel model = null;
	HttpServletRequest req =
	    RequestManager.getRequestContext().getRequest();

	try {
	    model = new WSPersonalProfileServiceModelImpl(
		req, getPageSessionAttributes());
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
	}

	return model;
    }

    /**
     * Handles cancel request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
	WSPersonalProfileServiceViewBean vb = 
	    (WSPersonalProfileServiceViewBean)getViewBean(
		WSPersonalProfileServiceViewBean.class);
	backTrail();
	unlockPageTrailForSwapping();
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    protected void setValues(String value) {
	StringTokenizer st = new StringTokenizer(value, "|");

	while (st.hasMoreTokens()) {
	    String tok = st.nextToken();
	    int idx = tok.indexOf('=');

	    if (idx != -1) {
		String id = tok.substring(0, idx);
		String val = tok.substring(idx+1);

		if (id.equals(WSPersonalProfileServiceModelImpl.
		    SUPPORTED_CONTAINER_CONTAINER_PREFIX)
		) {
		    propertySheetModel.setValue(ATTR_CONTAINER_NAME, val);
		} else if (id.equals(WSPersonalProfileServiceModelImpl.
		    SUPPORTED_CONTAINER_PLUGIN_PREFIX)
		) {
		    propertySheetModel.setValue(ATTR_PLUGIN, val);
		}
	    }
	}
    }

    private String getValues(Map map) {
	String name = (String)propertySheetModel.getValue(ATTR_CONTAINER_NAME);
	name = name.trim();
	String plugin = (String)propertySheetModel.getValue(ATTR_PLUGIN);
	plugin = plugin.trim();

	map.put(WSPersonalProfileServiceModelImpl.
	    SUPPORTED_CONTAINER_CONTAINER_PREFIX, name);

	if (plugin.length() > 0) {
	    map.put(WSPersonalProfileServiceModelImpl.
		SUPPORTED_CONTAINER_PLUGIN_PREFIX, plugin);
	}

	String errorMsg = null;
	if (name.length() == 0) {
	    errorMsg =
	  "webservices.personal.profile.missing.supportedContainerName.message";
	}
	return errorMsg;
    }

    protected String mapToString(Map map) {
	boolean first = true;
	StringBuffer buff = new StringBuffer();
	for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
	    String key = (String)i.next();
	    if (first) {
		first = false;
	    } else {
		buff.append("|");
	    }
	    buff.append(key)
		.append("=")
		.append((String)map.get(key));
	}
	return buff.toString();
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

    protected abstract void createPageTitleModel();
    protected abstract void handleButton1Request(Map values);
}
