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
 * $Id: EntityResourceOfferingEditViewBean.java,v 1.2 2008/06/25 05:49:41 qcheng Exp $
 *
 */

package com.sun.identity.console.idm;

import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.idm.model.EntityResourceOfferingModel;
import com.sun.identity.console.service.model.SMDiscoEntryData;
import com.sun.identity.console.service.model.SMDiscoveryServiceData;
import com.sun.identity.console.service.model.DiscoveryDataCache;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.io.InputStream;
import java.util.List;

public class EntityResourceOfferingEditViewBean
    extends EntityResourceOfferingViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/idm/EntityResourceOfferingEdit.jsp";
    private static final String PGATTR_INDEX = "resoffTblIndex";
    private boolean populateValues = false;

    public EntityResourceOfferingEditViewBean() {
	super("EntityResourceOfferingEdit", DEFAULT_DISPLAY_URL);
    }

    void populateValues(String index) {
	setPageSessionAttribute(PGATTR_INDEX, index);
	populateValues = true;
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException {
	super.beginDisplay(event);

	if (populateValues) {
	    SMDiscoEntryData data = getCurrentServiceData();
	    setValues(data);
	} else {
	    SMDiscoEntryData data = (SMDiscoEntryData)getPageSessionAttribute(
		PROPERTY_ATTRIBUTE);
	    if (data != null) {
		populateDirectiveMechIDRefs(data);
	    }
	}
    }

    protected SMDiscoEntryData getCurrentServiceData() {
	int currentIdx = Integer.parseInt((String)
	    getPageSessionAttribute(PGATTR_INDEX));

	DiscoveryDataCache cache = DiscoveryDataCache.getInstance();
	String cacheID = (String)getPageSessionAttribute(
	    EntityResourceOfferingViewBean.DATA_ID);
	SMDiscoveryServiceData smEntry = cache.getData(
	    getModel().getUserSSOToken(), cacheID);
	List resourceData = smEntry.getResourceData();
	return (SMDiscoEntryData)resourceData.get(currentIdx);
    }

    protected void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/threeBtnsPageTitle.xml"));
	ptModel.setPageTitleText(getPageTitleText());
	ptModel.setValue("button1", "button.ok");
	ptModel.setValue("button2", "button.reset");
	ptModel.setValue("button3", "button.cancel");
    }

    protected String getButtonlLabel() {
	return "button.ok";
    }

    protected String getPageTitleText() {
	return "discovery.service.bootstrapResOff.edit.page.title";
    }

    protected void handleButton1Request(SMDiscoEntryData smData)
	throws AMConsoleException
    {
	EntityResourceOfferingModel model =
	    (EntityResourceOfferingModel)getModel();
	EntityResourceOfferingViewBean vb = (EntityResourceOfferingViewBean)
	    getViewBean(EntityResourceOfferingViewBean.class);
	DiscoveryDataCache cache = DiscoveryDataCache.getInstance();
	String cacheID = (String)getPageSessionAttribute(
	    EntityResourceOfferingViewBean.DATA_ID);
	SMDiscoveryServiceData smEntry = cache.getData(
	    model.getUserSSOToken(), cacheID);

	int index = Integer.parseInt((String)
	    getPageSessionAttribute(PGATTR_INDEX));
	smEntry.replaceResourceData(index, smData);

	String univId = (String)getPageSessionAttribute(
	    EntityEditViewBean.UNIVERSAL_ID);

        try {
            model.setEntityDiscoEntry(univId, smEntry);
	    removePageSessionAttribute(EntityResourceOfferingViewBean.DATA_ID);
	    removePageSessionAttribute(PGATTR_INDEX);
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }

    public void handleButton3Request(RequestInvocationEvent event) {
	EntityResourceOfferingViewBean vb = (EntityResourceOfferingViewBean)
	    getViewBean(EntityResourceOfferingViewBean.class);
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

    protected void createPropertyModel() {
	DelegationConfig dConfig = DelegationConfig.getInstance();
	canModify = dConfig.hasPermission("/", null,
	    AMAdminConstants.PERMISSION_MODIFY,
	    getRequestContext().getRequest(), getClass().getName());

	String xmlFile = (canModify) ?
	    "com/sun/identity/console/propertyEntityResOffering.xml":
	    "com/sun/identity/console/propertyEntityResOffering_Readonly.xml";

	InputStream is = getClass().getClassLoader().getResourceAsStream(
	    xmlFile);
	String xml = AMAdminUtils.getStringFromInputStream(is);
	propertySheetModel = new AMPropertySheetModel(
	    processPropertiesXML(xml));

	propertySheetModel.clear();
	createSecurityMechIDTable();
    }
}
