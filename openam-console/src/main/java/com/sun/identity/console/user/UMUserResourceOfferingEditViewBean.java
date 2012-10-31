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
 * $Id: UMUserResourceOfferingEditViewBean.java,v 1.2 2008/06/25 05:49:49 qcheng Exp $
 *
 */

package com.sun.identity.console.user;

import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.idm.EntityEditViewBean;
import com.sun.identity.console.service.model.SMDescriptionData;
import com.sun.identity.console.service.model.SMDiscoEntryData;
import com.sun.identity.console.service.model.SMDiscoveryServiceData;
import com.sun.identity.console.user.model.UMUserResourceOfferingModel;
import com.sun.identity.console.service.model.DiscoveryDataCache;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UMUserResourceOfferingEditViewBean
    extends UMUserResourceOfferingViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/user/UMUserResourceOfferingEdit.jsp";
    private static final String PGATTR_INDEX = "resoffTblIndex";
    private static final String PGATTR_ORIG_MECH_ID = "origMechID";
    private boolean populateValues = false;

    public UMUserResourceOfferingEditViewBean() {
	super("UMUserResourceOfferingEdit", DEFAULT_DISPLAY_URL);
    }

    void populateValues(String index) {
	setPageSessionAttribute(PGATTR_INDEX, index);
	removePageSessionAttribute(PGATTR_ORIG_MECH_ID);
	populateValues = true;
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException {
	super.beginDisplay(event);

	if (populateValues) {
	    SMDiscoEntryData data = getCurrentServiceData();
	    setValues(data, getModel());
	} else {
	    SMDiscoEntryData data = (SMDiscoEntryData)getPageSessionAttribute(
		PROPERTY_ATTRIBUTE);
	    if (data != null) {
		populateDirectiveMechIDRefs(data);
	    }
	}

	if (getPageSessionAttribute(PG_SESSION_MODIFIED) != null) {
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
		"message.profile.modified");
	}
    }

    protected SMDiscoEntryData getCurrentServiceData() {
	int currentIdx = Integer.parseInt((String)
	    getPageSessionAttribute(PGATTR_INDEX));

	DiscoveryDataCache cache = DiscoveryDataCache.getInstance();
	String cacheID = (String)getPageSessionAttribute(
	    UMUserResourceOfferingViewBean.DATA_ID);
	SMDiscoveryServiceData smEntry = cache.getData(
	    getModel().getUserSSOToken(), cacheID);
	List resourceData = smEntry.getResourceData();
	SMDiscoEntryData smDisco = (SMDiscoEntryData)resourceData.get(
	    currentIdx);
	if (getPageSessionAttribute(PGATTR_ORIG_MECH_ID) == null) {
	    ArrayList list = new ArrayList();
	    for (Iterator i = smDisco.descData.iterator(); i.hasNext(); ) {
		list.add(new SMDescriptionData((SMDescriptionData)i.next()));
	    }
	    setPageSessionAttribute(PGATTR_ORIG_MECH_ID, list);
	}
	return smDisco;
    }

    private void recoveryMechID() {
	SMDiscoEntryData smDisco = getCurrentServiceData();
	List list = (ArrayList)removePageSessionAttribute(PGATTR_ORIG_MECH_ID);
	if (list != null) {
	    smDisco.descData = list;
	}
    }

    protected void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/threeBtnsPageTitle.xml"));
	ptModel.setPageTitleText(getPageTitleText());
	ptModel.setValue("button1", "button.save");
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
	UMUserResourceOfferingModel model =
	    (UMUserResourceOfferingModel)getModel();
	DiscoveryDataCache cache = DiscoveryDataCache.getInstance();
	String cacheID = (String)getPageSessionAttribute(
	    UMUserResourceOfferingViewBean.DATA_ID);
	SMDiscoveryServiceData smEntry = cache.getData(
	    model.getUserSSOToken(), cacheID);

	int index = Integer.parseInt((String)
	    getPageSessionAttribute(PGATTR_INDEX));
	smEntry.replaceResourceData(index, smData);

	String userId = (String)getPageSessionAttribute(
	    EntityEditViewBean.UNIVERSAL_ID);

        try {
            model.setUserDiscoEntry(userId, smEntry);
	    removePageSessionAttribute(PG_SESSION_MODIFIED);
	    removePageSessionAttribute(PGATTR_ORIG_MECH_ID);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
		"message.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        forwardTo();
    }

    public void handleButton3Request(RequestInvocationEvent event) {
	UMUserResourceOfferingViewBean vb = (UMUserResourceOfferingViewBean)
	    getViewBean(UMUserResourceOfferingViewBean.class);
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
	removePageSessionAttribute(PG_SESSION_MODIFIED);
	recoveryMechID();
	populateValues((String)getPageSessionAttribute(PGATTR_INDEX));
	tablePopulated = false;
        forwardTo();
    }

    protected void createPropertyModel() {
	DelegationConfig dConfig = DelegationConfig.getInstance();
	canModify = dConfig.hasPermission("/", null,
	    AMAdminConstants.PERMISSION_MODIFY,
	    getRequestContext().getRequest(), getClass().getName());

	String xmlFile = (canModify) ?
	    "com/sun/identity/console/propertyUserResOffering.xml":
	    "com/sun/identity/console/propertyUserResOffering_Readonly.xml";

	InputStream is = getClass().getClassLoader().getResourceAsStream(
	    xmlFile);
	String xml = AMAdminUtils.getStringFromInputStream(is);
	propertySheetModel = new AMPropertySheetModel(
	    processPropertiesXML(xml));

	propertySheetModel.clear();
	createSecurityMechIDTable();
    }
}
