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
 * $Id: SMDiscoveryProviderResourceIdMapperEditViewBean.java,v 1.2 2008/06/25 05:49:45 qcheng Exp $
 *
 */
 
package com.sun.identity.console.service;

import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.web.ui.model.CCPageTitleModel;
import java.text.MessageFormat;
import java.util.Map;

public class SMDiscoveryProviderResourceIdMapperEditViewBean
    extends SMDiscoveryProviderResourceIdMapperViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/service/SMDiscoveryProviderResourceIdMapperEdit.jsp";
    private static final String PGATTR_INDEX =
	"providerResourceIdMapperTblIndex";
    private boolean populateValues = false;

    public SMDiscoveryProviderResourceIdMapperEditViewBean() {
	super("SMDiscoveryProviderResourceIdMapperEdit", DEFAULT_DISPLAY_URL);
    }

    void populateValues(String index) {
	setPageSessionAttribute(PGATTR_INDEX, index);
	populateValues = true;
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException {
	super.beginDisplay(event);
	if (populateValues) {
	    setValues(getCurrentServiceData());
	}
    }

    protected String getCurrentServiceData() {
	int currentIdx = Integer.parseInt((String)
	    getPageSessionAttribute(PGATTR_INDEX));
	Map mapAttrs = (Map)getPageSessionAttribute(
	    SMDiscoveryServiceViewBean.PROPERTY_ATTRIBUTE);
	OrderedSet set = (OrderedSet)mapAttrs.get(
	    AMAdminConstants.DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER);
	return (String)set.get(currentIdx);
    }

    protected void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/threeBtnsPageTitle.xml"));

	ptModel.setPageTitleText(
            "discovery.service.providerResourceIdMapper.edit.page.title");

	ptModel.setValue("button1", "button.save");
	ptModel.setValue("button2", "button.reset");
	ptModel.setValue("button3", getBackButtonLabel());
    }

    /**
     * Creates the label used to take the user back to the previous page
     * they were viewing. The label is defined in the property file as
     * "Back to {0}" which lets us control the format of the label in
     * different locales. 
     */
    protected String getBackButtonLabel() {
        String[] arg = { getModel().getLocalizedString(
            "breadcrumbs.webservices.discovery") };

        return MessageFormat.format(
            getModel().getLocalizedString("back.button"), arg);
    }

    protected void handleButton1Request(Map data)
	throws AMConsoleException
    {
	SMDiscoveryServiceViewBean vb = (SMDiscoveryServiceViewBean)
	    getViewBean(SMDiscoveryServiceViewBean.class);

	Map mapAttrs = (Map)getPageSessionAttribute(
	    SMDiscoveryServiceViewBean.PROPERTY_ATTRIBUTE);
	OrderedSet mapper = (OrderedSet)mapAttrs.get(
	    AMAdminConstants.DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER);
	int index = Integer.parseInt((String)
	    getPageSessionAttribute(PGATTR_INDEX));

	mapper.set(index,
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

    /**
     * Handles reset request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
	removePageSessionAttribute(PROPERTY_ATTRIBUTE);
	populateValues = true;
        forwardTo();
    }

    /**
     * Handles "Back to" previous page request.
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
            "com/sun/identity/console/propertySMDiscoveryProviderResourceIdMapper.xml" :
            "com/sun/identity/console/propertySMDiscoveryProviderResourceIdMapper_Readonly.xml";

	propertySheetModel = new AMPropertySheetModel(
	    getClass().getClassLoader().getResourceAsStream(xmlFile));
	propertySheetModel.clear();
    }

    protected String getBreadCrumbDisplayName() {
	return "breadcrumbs.webservices.discovery.resourceid.mapper.edit";
    }

    protected boolean startPageTrail() {
	return false;
    }
}
