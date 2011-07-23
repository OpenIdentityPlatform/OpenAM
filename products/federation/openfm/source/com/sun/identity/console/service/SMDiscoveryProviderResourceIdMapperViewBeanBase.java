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
 * $Id: SMDiscoveryProviderResourceIdMapperViewBeanBase.java,v 1.2 2008/06/25 05:49:45 qcheng Exp $
 *
 */
 
package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.service.model.SMDiscoveryServiceModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public abstract class SMDiscoveryProviderResourceIdMapperViewBeanBase
    extends AMPrimaryMastHeadViewBean
{
    private static final String PGTITLE = "pgtitle";
    protected static final String PROPERTY_ATTRIBUTE =
	"providerResourceIdMapperPropertyAttributes";

    protected static final String ATTR_PROVIDERID = "providerid";
    protected static final String ATTR_IDMAPPER = "idmapper";

    protected CCPageTitleModel ptModel;
    protected AMPropertySheetModel propertySheetModel;

    public SMDiscoveryProviderResourceIdMapperViewBeanBase(
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
	registerChild(PGTITLE, CCPageTitle.class);
	ptModel.registerChildren(this);
	registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
	propertySheetModel.registerChildren(this);
	super.registerChildren();
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
   "com/sun/identity/console/propertySMDiscoveryProviderResourceIdMapper.xml"));
	propertySheetModel.clear();
    }

    protected AMModel getModelInternal() {
	AMModel model = null;
	HttpServletRequest req =
	    RequestManager.getRequestContext().getRequest();

	try {
	    model = new SMDiscoveryServiceModelImpl(
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
	SMDiscoveryServiceViewBean vb = (SMDiscoveryServiceViewBean)getViewBean(
	    SMDiscoveryServiceViewBean.class);
	backTrail();
	unlockPageTrailForSwapping();
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    private Map getValues()
	throws AMConsoleException {
	Map values = new HashMap(4);

	String providerId = ((String)propertySheetModel.getValue(
	    ATTR_PROVIDERID)).trim();	
	String idMapper = ((String)propertySheetModel.getValue(
	    ATTR_IDMAPPER)).trim();	

        if (providerId.length() == 0) {
            throw new AMConsoleException(
                "discovery.service.providerResourceIdMapper.missing.providerId.message");
        } else if (idMapper.length() == 0) {
            throw new AMConsoleException(
                "discovery.service.providerResourceIdMapper.missing.idmapper.message");
        }

	values.put(ATTR_PROVIDERID, providerId);
	values.put(ATTR_IDMAPPER, idMapper);
        return values;
    }

    protected void setValues(String value) {
	Map map = AMAdminUtils.getValuesFromDelimitedString(value, "|");
	propertySheetModel.setValue(ATTR_PROVIDERID,
	    (String)map.get(AMAdminConstants.
	    DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER_PROVIDER_ID));
	propertySheetModel.setValue(ATTR_IDMAPPER,
	    (String)map.get(AMAdminConstants.
	    DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER_ID_MAPPER));
    }

    public void handleButton1Request(RequestInvocationEvent event)
	throws ModelControlException
    {
	try {
	    Map data = getValues();
	    handleButton1Request(data);
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(
		CCAlert.TYPE_ERROR, "message.error", e.getMessage());
	    forwardTo();
	}
    }

    protected abstract void createPageTitleModel();
    protected abstract void handleButton1Request(Map map)
	throws AMConsoleException;
}
