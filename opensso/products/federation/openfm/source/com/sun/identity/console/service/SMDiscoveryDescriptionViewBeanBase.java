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
 * $Id: SMDiscoveryDescriptionViewBeanBase.java,v 1.3 2008/10/20 23:40:01 babysunil Exp $
 *
 */
 
package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.service.model.SMDescriptionData;
import com.sun.identity.console.service.model.SMDiscoveryServiceModelImpl;
import com.sun.identity.console.service.model.SMDiscoEntryData;
import com.sun.identity.liberty.ws.disco.common.DiscoServiceManager;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.web.ui.model.CCAddRemoveModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.addremove.CCAddRemove;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public abstract class SMDiscoveryDescriptionViewBeanBase
    extends AMPrimaryMastHeadViewBean
{
    private static final String PGTITLE = "pgtitle";

    private static final String TF_DESCRIPTION_ID = "tfDescriptionID";
    static final String ATTR_NAME_SECURITY_MECH_ID = "securityMechID";
    private static final String ATTR_NAME_SOAP_HTTP_END_POINT =
	"soapHttpEndPoint";
    private static final String ATTR_NAME_SOAP_HTTP_ACTION = "soapHttpAction";

    public static final String PG_SESSION_RETURN_VIEW_BEAN_CLASSNAME =
	"discoveryDescReturnViewBean";
    public static final String PG_SESSION_DISCO_ENTRY_DATA =
	"discoEntryData";
    protected static final String PROPERTY_ATTRIBUTE =
	"discoveryDescPropertyAttributes";

    protected CCPageTitleModel ptModel;
    protected AMPropertySheetModel propertySheetModel;
    private boolean populateMechID = false;

    public SMDiscoveryDescriptionViewBeanBase(
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
            ptModel.setPageTitleText(getPageTitleText());
            
            createPropertyModel();
            registerChildren();
        }
    }

    protected void registerChildren() {
	registerChild(PGTITLE, CCPageTitle.class);
	ptModel.registerChildren(this);
	registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
	propertySheetModel.registerChildren(this);
	registerChild(TF_DESCRIPTION_ID, CCTextField.class);
	super.registerChildren();
    }

    protected View createChild(String name) {
	View view = null;

	if (name.equals(ATTR_NAME_SECURITY_MECH_ID) && !populateMechID) {
	    populateMechID();
	}

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

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.cancel");
    }

    protected void create3ButtonPageTitle() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/threeBtnsPageTitle.xml"));

	ptModel.setValue("button1", "button.save");
	ptModel.setValue("button2", "button.reset");
	ptModel.setValue("button3", setBackButton());
    }

    /**
     * Creates the label used to take the user back to the previous page
     * they were viewing. The label is defined in the property file as
     * "Back to {0}" which lets us control the format of the label in
     * different locales. 
     */
    protected String setBackButton() {
        return getBackButtonLabel("");
    }

    protected void createPropertyModel() {
	propertySheetModel = new AMPropertySheetModel(
	    getClass().getClassLoader().getResourceAsStream(
	    "com/sun/identity/console/propertySMDiscoveryDescription.xml"));
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

    void populateMechID() {
	populateMechID = true;
	CCAddRemoveModel model = new CCAddRemoveModel();
	model.setOrientation(CCAddRemoveModel.VERTICAL);
	model.setListboxHeight(CCAddRemoveModel.DEFAULT_LISTBOX_HEIGHT);
	setMechID(model);
	propertySheetModel.setModel(ATTR_NAME_SECURITY_MECH_ID, model);
    }

    protected void repopulateMechID() {
	CCAddRemove child = (CCAddRemove)getChild(ATTR_NAME_SECURITY_MECH_ID);
	child.resetStateData();
	populateMechID();
    }

    private void setMechID(CCAddRemoveModel model) {
	SMDiscoEntryData data = (SMDiscoEntryData)getPageSessionAttribute(
	    PG_SESSION_DISCO_ENTRY_DATA);
	Set availableMechIds = getSecurityMechanisms();
	Set assignedMechIds = data.getAllAssignedMechIDs();
	availableMechIds.removeAll(assignedMechIds);
	model.setAvailableOptionList(createOptionList(availableMechIds));
	SMDescriptionData descData = getCurrentData();

	if (descData != null) {
	    model.setSelectedOptionList(
		createOptionList(descData.securityMechId));
	}
    }

    private Set getSecurityMechanisms() {
	Set set = new HashSet();
	set.addAll(DiscoServiceManager.getSupportedAuthenticationMechanisms());
	return set;
    }

    /**
     * Handles cancel request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
	AMViewBeanBase vb = getReturnToViewBean();
	backTrail();
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    /**
     * Handles the "Back to" page request. Essentially this is just canceling 
     * the page and returning to the previous page. Same as a cancel request
     * from the create page. 
     *
     * @param event Request Invocation Event.
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        AMViewBeanBase vb = getReturnToViewBean();
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected AMViewBeanBase getReturnToViewBean() {
	String viewBeanClassName = (String)getPageSessionAttribute(
	    PG_SESSION_RETURN_VIEW_BEAN_CLASSNAME);
	try {
	    return (AMViewBeanBase)getViewBean(
		Class.forName(viewBeanClassName));
	} catch (ClassNotFoundException e) {
	    return (SMDiscoveryBootstrapRefOffViewBeanBase)getViewBean(
		SMDiscoveryBootstrapRefOffAddViewBean.class);
	}
    }

    protected void setValues(SMDescriptionData smData) {
	propertySheetModel.clear();
	propertySheetModel.setValue(ATTR_NAME_SOAP_HTTP_END_POINT,
	    smData.endPointUrl);
	propertySheetModel.setValue(ATTR_NAME_SOAP_HTTP_ACTION,
	    smData.soapAction);
    }

    private SMDescriptionData getValues()
	throws AMConsoleException
    {
	SMDescriptionData smData = new SMDescriptionData();
	smData.descriptionID = (String)getDisplayFieldValue(TF_DESCRIPTION_ID);
	if (smData.descriptionID.length() == 0) {
	    smData.descriptionID = SAMLUtils.generateID();
	}

	CCAddRemove mechIdChild = (CCAddRemove)getChild(
	    ATTR_NAME_SECURITY_MECH_ID);
	mechIdChild.restoreStateData();
	CCAddRemoveModel mechIdModel = (CCAddRemoveModel)
	    propertySheetModel.getModel(ATTR_NAME_SECURITY_MECH_ID);
	smData.securityMechId = getList(mechIdModel.getSelectedOptionList());

	smData.endPointUrl = ((String)propertySheetModel.getValue(
	    ATTR_NAME_SOAP_HTTP_END_POINT)).trim();
	smData.soapAction = ((String)propertySheetModel.getValue(
	    ATTR_NAME_SOAP_HTTP_ACTION)).trim();

	if (smData.securityMechId.isEmpty()) {
	    throw new AMConsoleException(
	    "discovery.service.description.missing.securityMechId.message");
	}

	if (smData.endPointUrl.length() == 0) {
	    throw new AMConsoleException(
		"discovery.service.description.missing.endPointUrl.message");
	}

	return smData;
    }

    public void handleButton1Request(RequestInvocationEvent event)
	throws ModelControlException
    {
	try {
	    SMDescriptionData smData = getValues();
	    handleButton1Request(smData);
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(
		CCAlert.TYPE_ERROR, "message.error", e.getMessage());
	    forwardTo();
	}
    }

    protected abstract SMDescriptionData getCurrentData();
    protected abstract String getPageTitleText();
    protected abstract void handleButton1Request(SMDescriptionData smData)
	throws AMConsoleException;
}
