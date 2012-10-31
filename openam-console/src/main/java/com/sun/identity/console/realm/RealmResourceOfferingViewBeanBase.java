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
 * $Id: RealmResourceOfferingViewBeanBase.java,v 1.2 2008/06/25 05:49:43 qcheng Exp $
 *
 */

package com.sun.identity.console.realm;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.service.SMDiscoveryBootstrapRefOffViewBeanBase;
import com.sun.identity.console.service.model.SMDiscoEntryData;
import com.sun.identity.console.realm.model.RealmResourceOfferingModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;

public abstract class RealmResourceOfferingViewBeanBase
    extends SMDiscoveryBootstrapRefOffViewBeanBase
{
    private static final String ATTR_RESOURCE_ID = "resourceId";
    private static final String ATTR_RESOURCE_ID_VALUE = "resourceIdValue";

    public RealmResourceOfferingViewBeanBase(
	String pageName,
	String defaultDisplayURL
    ) {
	super(pageName, defaultDisplayURL);
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

    protected void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/twoBtnsPageTitle.xml"));
	ptModel.setPageTitleText(getPageTitleText());
	ptModel.setValue("button1", getButtonlLabel());
	ptModel.setValue("button2", "button.cancel");
    }

    protected abstract String getButtonlLabel();
    protected abstract String getPageTitleText();

    protected void createPropertyModel() {
	InputStream is = getClass().getClassLoader().getResourceAsStream(
	    "com/sun/identity/console/propertyRealmResOffering.xml");
	String xml = AMAdminUtils.getStringFromInputStream(is);
	propertySheetModel = new AMPropertySheetModel(xml);
	propertySheetModel.clear();
	createSecurityMechIDTable();
    }

    protected AMModel getModelInternal() {
	HttpServletRequest req =RequestManager.getRequestContext().getRequest();
	return new RealmResourceOfferingModelImpl(
	    req, getPageSessionAttributes());
    }

    /**
     * Handles cancel request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
	RealmResourceOfferingViewBean vb = (RealmResourceOfferingViewBean)
	    getViewBean(RealmResourceOfferingViewBean.class);
	removePageSessionAttribute(ATTR_SECURITY_MECH_ID);
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    protected void setValues(SMDiscoEntryData smDisco, AMModel model) {
	propertySheetModel.setValue(ATTR_RESOURCE_ID,
	    smDisco.resourceIdAttribute);
	propertySheetModel.setValue(ATTR_RESOURCE_ID_VALUE,
	    smDisco.resourceIdValue);
	super.setValues(smDisco, model);
    }

    public void handleTblSecurityMechIDButtonAddRequest(
	RequestInvocationEvent event
    ) throws ModelControlException {
	try {
            SMDiscoEntryData smData = getValues(false);
            setPageSessionAttribute(RealmDiscoveryDescriptionViewBeanBase.
		PG_SESSION_DISCO_ENTRY_DATA, smData);
	    setPageSessionAttribute(RealmDiscoveryDescriptionViewBeanBase.
		PG_SESSION_RETURN_VIEW_BEAN_CLASSNAME, getClass().getName());
	    RealmDiscoveryDescriptionAddViewBean vb =
		(RealmDiscoveryDescriptionAddViewBean)getViewBean(
		    RealmDiscoveryDescriptionAddViewBean.class);
	    passPgSessionMap(vb);
	    vb.forwardTo(getRequestContext());
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(
		CCAlert.TYPE_ERROR, "message.error", e.getMessage());
	    forwardTo();
	}
    }

    /**
     * Handles edit security mechanism ID request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblSecurityMechIDHrefActionRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        SMDiscoEntryData smData = (SMDiscoEntryData)getPageSessionAttribute(
            PROPERTY_ATTRIBUTE);
        setPageSessionAttribute(RealmDiscoveryDescriptionViewBeanBase.
	    PG_SESSION_DISCO_ENTRY_DATA, smData);
	setPageSessionAttribute(RealmDiscoveryDescriptionViewBeanBase.
	    PG_SESSION_RETURN_VIEW_BEAN_CLASSNAME, getClass().getName());

	RealmDiscoveryDescriptionEditViewBean vb =
	    (RealmDiscoveryDescriptionEditViewBean)getViewBean(
	    RealmDiscoveryDescriptionEditViewBean.class);
	passPgSessionMap(vb);
	vb.populateValues((String)getDisplayFieldValue(
	    TBL_SECURITY_MECH_ID_HREF_ACTION));
	vb.forwardTo(getRequestContext());
    }
}
