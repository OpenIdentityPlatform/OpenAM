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
 * $Id: SMDiscoveryBootstrapRefOffViewBeanBase.java,v 1.2 2008/06/25 05:49:44 qcheng Exp $
 *
 */
 
package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.service.model.SMDescriptionData;
import com.sun.identity.console.service.model.SMDiscoveryServiceModelImpl;
import com.sun.identity.console.service.model.SMDiscoEntryData;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCEditableListModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.editablelist.CCEditableList;
import com.sun.web.ui.view.html.CCCheckBox;
import com.sun.web.ui.view.html.CCSelectableList;
import com.sun.web.ui.view.html.CCStaticTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public abstract class SMDiscoveryBootstrapRefOffViewBeanBase
    extends AMPrimaryMastHeadViewBean
{
    private static final String PGTITLE = "pgtitle";
    public static final String PROPERTY_ATTRIBUTE =
	"bootstrapRefPropertyAttributes";
    public static final String PG_SESSION_MODIFIED = "resOffModified";

    private static final String ATTR_ABSTRACT = "abstract";
    private static final String ATTR_SERVICE_TYPE = "serviceType";
    private static final String ATTR_PROVIDER_ID = "providerID";
    protected static final String ATTR_SECURITY_MECH_ID = "SecurityMechID";
    private static final String ATTR_RESOURCE_OFFERING_OPTIONS_OPTIONS =
	"resourceOfferingOptionsOptions";
    private static final String ATTR_RESOURCE_OFFERING_OPTIONS_LIST =
	"resourceOfferingOptionsList";
    private static final String ATTR_DIRECTIVES_GENERATE_BEARER_TOKEN_REFS =
	"resourceOfferingOptionsDirectivesGenerateBearerTokenRefs";
    private static final String ATTR_DIRECTIVES_AUTHENTICATE_REQUESTER_REFS =
	"resourceOfferingOptionsDirectivesAuthenticateRequesterRefs";
    private static final String ATTR_DIRECTIVES_ENCRYPT_RESOURCE_ID_REFS =
	"resourceOfferingOptionsDirectivesEncryptResourceIDRefs";
    private static final String
	ATTR_DIRECTIVES_AUTHENTICATE_SESSION_CONTEXT_REFS =
	"resourceOfferingOptionsDirectivesAuthenticateSessionContextRefs";
    private static final String ATTR_DIRECTIVES_AUTHORIZE_REQUESTER_REFS =
	"resourceOfferingOptionsDirectivesAuthorizeRequesterRefs";
    private static final String ATTR_DIRECTIVES_GENERATE_BEARER_TOKEN =
	"GenerateBearerToken";
    private static final String ATTR_DIRECTIVES_AUTHENTICATE_REQUESTER =
	"AuthenticateRequester";
    private static final String ATTR_DIRECTIVES_ENCRYPT_RESOURCE_ID =
	"EncryptResourceID";
    private static final String ATTR_DIRECTIVES_AUTHENTICATE_SESSION_CONTEXT =
	"AuthenticateSessionContext";
    private static final String ATTR_DIRECTIVES_AUTHORIZE_REQUESTER =
	"AuthorizeRequester";

    private static List DIRECTIVES_MECHID = new ArrayList(5);
    private static Map MAP_DIRECTIVES_MECHID = new HashMap(10);
    private static List DIRECTIVES_MECHID_LIST = new ArrayList(5);

    static {
	DIRECTIVES_MECHID.add(ATTR_DIRECTIVES_GENERATE_BEARER_TOKEN);
	DIRECTIVES_MECHID.add(ATTR_DIRECTIVES_AUTHENTICATE_REQUESTER);
	DIRECTIVES_MECHID.add(ATTR_DIRECTIVES_ENCRYPT_RESOURCE_ID);
	DIRECTIVES_MECHID.add(ATTR_DIRECTIVES_AUTHENTICATE_SESSION_CONTEXT);
	DIRECTIVES_MECHID.add(ATTR_DIRECTIVES_AUTHORIZE_REQUESTER);

	DIRECTIVES_MECHID_LIST.add(ATTR_DIRECTIVES_GENERATE_BEARER_TOKEN_REFS);
	DIRECTIVES_MECHID_LIST.add(
	    ATTR_DIRECTIVES_AUTHENTICATE_REQUESTER_REFS);
	DIRECTIVES_MECHID_LIST.add(ATTR_DIRECTIVES_ENCRYPT_RESOURCE_ID_REFS);
	DIRECTIVES_MECHID_LIST.add(
	    ATTR_DIRECTIVES_AUTHENTICATE_SESSION_CONTEXT_REFS);
	DIRECTIVES_MECHID_LIST.add(ATTR_DIRECTIVES_AUTHORIZE_REQUESTER_REFS);

	MAP_DIRECTIVES_MECHID.put(ATTR_DIRECTIVES_GENERATE_BEARER_TOKEN,
	    ATTR_DIRECTIVES_GENERATE_BEARER_TOKEN_REFS);
	MAP_DIRECTIVES_MECHID.put(ATTR_DIRECTIVES_AUTHENTICATE_REQUESTER,
	    ATTR_DIRECTIVES_AUTHENTICATE_REQUESTER_REFS);
	MAP_DIRECTIVES_MECHID.put(ATTR_DIRECTIVES_ENCRYPT_RESOURCE_ID,
	    ATTR_DIRECTIVES_ENCRYPT_RESOURCE_ID_REFS);
	MAP_DIRECTIVES_MECHID.put(ATTR_DIRECTIVES_AUTHENTICATE_SESSION_CONTEXT,
	    ATTR_DIRECTIVES_AUTHENTICATE_SESSION_CONTEXT_REFS);
	MAP_DIRECTIVES_MECHID.put(ATTR_DIRECTIVES_AUTHORIZE_REQUESTER,
	    ATTR_DIRECTIVES_AUTHORIZE_REQUESTER_REFS);
    }

    private static final String TBL_SECURITY_MECH_ID = "tblSecurityMechID";
    private static final String TBL_SECURITY_MECH_ID_ADD_BTN =
	"tblSecurityMechIDButtonAdd";
    private static final String TBL_SECURITY_MECH_ID_DELETE_BTN =
	"tblSecurityMechIDButtonDelete";
    private static final String TBL_SECURITY_MECH_ID_COL_ID =
	"tblSecurityMechIDColID";
    private static final String TBL_SECURITY_MECH_ID_COL_ACTION =
	"tblSecurityMechIDColAction";
    private static final String TBL_SECURITY_MECH_ID_DATA_ID =
	"tblSecurityMechIDDataID";
    protected static final String TBL_SECURITY_MECH_ID_HREF_ACTION =
	"tblSecurityMechIDHrefAction";
    private static final String TBL_SECURITY_MECH_ID_LABEL_ACTION =
	"tblSecurityMechIDLabelAction";
    private static final String ATTRIBUTE_NAME_SECURITY_MECH_ID =
	"SecurityMechID";

    protected boolean tablePopulated = false;
    protected CCPageTitleModel ptModel;
    protected AMPropertySheetModel propertySheetModel;
    protected boolean canModify = true;

    public SMDiscoveryBootstrapRefOffViewBeanBase(
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
	if (!tablePopulated) {
	    prePopulateTable();
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

    protected void createPropertyModel() {
	InputStream is = getClass().getClassLoader().getResourceAsStream(
	    "com/sun/identity/console/propertySMDiscoveryBootstrapResOff.xml");
	String xml = AMAdminUtils.getStringFromInputStream(is);
	propertySheetModel = new AMPropertySheetModel(
	    processPropertiesXML(xml));

	propertySheetModel.clear();
	createSecurityMechIDTable();
    }

    protected String processPropertiesXML(String origXML) {
        String baseClass = "SMDiscoveryBootstrapResOff";
	String className = getClass().getName();
	int idx = className.lastIndexOf('.');
	className = className.substring(idx+1);
	idx = className.lastIndexOf("ViewBean");
	className = className.substring(0, idx);
        int len = baseClass.length();
        idx = origXML.indexOf(baseClass);
        while (idx != -1) {
            origXML = origXML.substring(0, idx) + className +
                origXML.substring(idx +len);
            idx = origXML.indexOf(baseClass, idx +len);
        }

        return origXML;
    }

    protected void createSecurityMechIDTable() {
        CCActionTableModel tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblDiscoverySecurityMechID.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_SECURITY_MECH_ID_COL_ID,
            "discovery.service.bootstrapResOff.securityMechID.ID");
        tblModel.setActionValue(TBL_SECURITY_MECH_ID_COL_ACTION,
            "discovery.service.bootstrapResOff.securityMechID.Action");
        tblModel.setActionValue(TBL_SECURITY_MECH_ID_ADD_BTN,
            "discovery.service.bootstrapResOff.securityMechID.add.button");
        tblModel.setActionValue(TBL_SECURITY_MECH_ID_DELETE_BTN,
	    "discovery.service.bootstrapResOff.securityMechID.delete.button");
        propertySheetModel.setModel(ATTRIBUTE_NAME_SECURITY_MECH_ID, tblModel);
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

    private void prePopulateTable() {
        SMDiscoEntryData data = (SMDiscoEntryData)getPageSessionAttribute(
            PROPERTY_ATTRIBUTE);
        if (data != null) {
	    setValues(data, getModel());
        }
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        resetButtonState(TBL_SECURITY_MECH_ID_DELETE_BTN);

    }

    protected void populateDirectiveMechIDRefs(SMDiscoEntryData data) {
	List mechIDs = data.getPossibleDirectivesToMechIDs();
	if (canModify) {
	    OptionList optionList = createOptionList(mechIDs);

	    for (Iterator i = DIRECTIVES_MECHID_LIST.iterator(); i.hasNext();) {
		String childName = (String)i.next();
		CCSelectableList child = (CCSelectableList)getChild(childName);
		child.setOptions(optionList);
	    }
	}

    }

    private void populateDescriptionsTable(List descData) {
        tablePopulated = true;
        CCActionTableModel tblModel =
            (CCActionTableModel)propertySheetModel.getModel(
		ATTRIBUTE_NAME_SECURITY_MECH_ID);
        tblModel.clearAll();

	if (descData != null) {
	    boolean firstEntry = true;
	    int counter = 0;

	    if ((descData != null) && !descData.isEmpty()) {
		for (Iterator iter = descData.iterator(); iter.hasNext(); ) {
		    SMDescriptionData smDesc = (SMDescriptionData)iter.next();

		    if (!firstEntry) {
			tblModel.appendRow();
		    } else {
			firstEntry = false;
		    }
                                                                                
		    tblModel.setValue(TBL_SECURITY_MECH_ID_DATA_ID,
			smDesc.getFirstSecurityMechId());
		    tblModel.setValue(TBL_SECURITY_MECH_ID_HREF_ACTION,
			Integer.toString(counter));
		    tblModel.setValue(TBL_SECURITY_MECH_ID_LABEL_ACTION,
		"discovery.service.table.bootstrapResOff.action.edit.label");
		    counter++;
		}
            }
        }
                                                                                
        setPageSessionAttribute(ATTR_SECURITY_MECH_ID, (ArrayList)descData);
    }


    /**
     * Handles cancel request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
	SMDiscoveryServiceViewBean vb = (SMDiscoveryServiceViewBean)getViewBean(
	    SMDiscoveryServiceViewBean.class);
	removePageSessionAttribute(ATTR_SECURITY_MECH_ID);
	backTrail();
	unlockPageTrailForSwapping();
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    protected void setValues(SMDiscoEntryData smDisco, AMModel model) {
	propertySheetModel.clear();
	propertySheetModel.setValue(ATTR_ABSTRACT, smDisco.abstractValue);
	propertySheetModel.setValue(ATTR_SERVICE_TYPE, smDisco.serviceType);
	propertySheetModel.setValue(ATTR_PROVIDER_ID, smDisco.providerId);
	propertySheetModel.setValue(ATTR_RESOURCE_OFFERING_OPTIONS_OPTIONS,
	    (smDisco.noOption) ? "true" : "false" );
	propertySheetModel.setValues(ATTR_RESOURCE_OFFERING_OPTIONS_LIST,
	    smDisco.options.toArray(), model);

        if (!tablePopulated) {
	    populateDescriptionsTable(smDisco.descData);
        }

	setDirectiveMechIDMapping(smDisco);
	setPageSessionAttribute(PROPERTY_ATTRIBUTE, smDisco);
    }

    private void setDirectiveMechIDMapping(SMDiscoEntryData smDisco) {
	populateDirectiveMechIDRefs(smDisco);

	Map directives = smDisco.directives;
	Set directiveNames = directives.keySet();

	for (Iterator iter = DIRECTIVES_MECHID.iterator(); iter.hasNext(); ) {
	    String directiveName = (String)iter.next();
	    String childName = (String)MAP_DIRECTIVES_MECHID.get(
		directiveName);

	    CCCheckBox cb = null;
	    CCStaticTextField staticText = null;

	    if (canModify) {
		cb = (CCCheckBox)getChild(
		    childName.substring(0, childName.length() -4));
	    } else {
		staticText = (CCStaticTextField)getChild(
		    childName.substring(0, childName.length() -4));
	    }

	    if (directiveNames.contains(directiveName)) {
		List refIds = (List)directives.get(directiveName);

		if (canModify) {
		    cb.setChecked(true);
		    CCSelectableList child = (CCSelectableList)getChild(
			childName);

		    if ((refIds != null) && !refIds.isEmpty()) {
			child.setValues(refIds.toArray());
		    } else {
			child.setValues(null);
		    }
		} else {
		    staticText.setValue("true");
		    CCStaticTextField child = (CCStaticTextField)getChild(
			childName);
		    if ((refIds != null) && !refIds.isEmpty()) {
			child.setValue(AMAdminUtils.getString(
			    refIds, ",", false));
		    } else {
			child.setValue("");
		    }
		}
	    } else {
		if (canModify) {
		    cb.setChecked(false);
		} else {
		    staticText.setValue("false");
		}
	    }
	}
    }

    private Map getDirectiveMechIDMapping() {
	Map map = new HashMap(10);

	for (Iterator i = DIRECTIVES_MECHID.iterator(); i.hasNext(); ) {
	    String directiveName = (String)i.next();
	    String childRefId = (String)MAP_DIRECTIVES_MECHID.get(
		directiveName);
	    String childCheckBoxId = childRefId.substring(
		0, childRefId.length() -4);
	    String checkbox = (String)propertySheetModel.getValue(
		childCheckBoxId);

	    if (checkbox.equals("true")) {
		map.put(directiveName, AMAdminUtils.toList(
		    propertySheetModel.getValues(childRefId)));
	    }
	}

	return map;
    }

    protected SMDiscoEntryData getValues(boolean validated)
	throws AMConsoleException
    {
	SMDiscoEntryData smDisco = new SMDiscoEntryData();
	smDisco.abstractValue = ((String)propertySheetModel.getValue(
	    ATTR_ABSTRACT)).trim();
	smDisco.serviceType = ((String)propertySheetModel.getValue(
	    ATTR_SERVICE_TYPE)).trim();
	smDisco.providerId = ((String)propertySheetModel.getValue(
	    ATTR_PROVIDER_ID)).trim();
	String optionFlag = (String)propertySheetModel.getValue(
	    ATTR_RESOURCE_OFFERING_OPTIONS_OPTIONS);
	smDisco.noOption = optionFlag.equalsIgnoreCase("true");

	CCEditableList eList = (CCEditableList)getChild(
	    ATTR_RESOURCE_OFFERING_OPTIONS_LIST);
	eList.restoreStateData();
	CCEditableListModel eModel = (CCEditableListModel)eList.getModel();
	OptionList options = eModel.getOptionList();
	if (options != null) {
	    smDisco.options = AMAdminUtils.toList(options);
	}

	List descData = (List)removePageSessionAttribute(
	    ATTR_SECURITY_MECH_ID);
	if (descData != null) {
	    smDisco.descData = descData;
	}

	smDisco.directives = getDirectiveMechIDMapping();

	if (validated) {
	    if ((descData == null) || descData.isEmpty()) {
		throw new AMConsoleException(
	      "discovery.service.bootstrapResOff.missing.service.desc.message");
	    } else if (smDisco.serviceType.length() == 0) {
		throw new AMConsoleException(
	       "discovery.service.bootstrapResOff.missing.serviceType.message");
	    } else if (smDisco.providerId.length() == 0) {
		throw new AMConsoleException(
		"discovery.service.bootstrapResOff.missing.providerId.message");
	    }
	}

	return smDisco;
    }

    public void handleButton1Request(RequestInvocationEvent event)
	throws ModelControlException
    {
	try {
	    SMDiscoEntryData smData = getValues(true);
	    handleButton1Request(smData);
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(
		CCAlert.TYPE_ERROR, "message.error", e.getMessage());
	    forwardTo();
	}
    }

    public void handleTblSecurityMechIDButtonAddRequest(
	RequestInvocationEvent event
    ) throws ModelControlException {
	try {
	    SMDiscoEntryData smData = getValues(false);
	    setPageSessionAttribute(
		SMDiscoveryDescriptionViewBeanBase.PG_SESSION_DISCO_ENTRY_DATA,
		smData);
	    setPageSessionAttribute(SMDiscoveryDescriptionViewBeanBase.
		PG_SESSION_RETURN_VIEW_BEAN_CLASSNAME, getClass().getName());
	    SMDiscoveryDescriptionAddViewBean vb =
		(SMDiscoveryDescriptionAddViewBean)getViewBean(
		    SMDiscoveryDescriptionAddViewBean.class);
	    unlockPageTrail();
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
	    PROPERTY_ATTRIBUTE);;
	setPageSessionAttribute(
	    SMDiscoveryDescriptionViewBeanBase.PG_SESSION_DISCO_ENTRY_DATA,
	    smData);
	setPageSessionAttribute(SMDiscoveryDescriptionViewBeanBase.
	    PG_SESSION_RETURN_VIEW_BEAN_CLASSNAME, getClass().getName());

	SMDiscoveryDescriptionEditViewBean vb =
	    (SMDiscoveryDescriptionEditViewBean)getViewBean(
	SMDiscoveryDescriptionEditViewBean.class);
	unlockPageTrail();
	passPgSessionMap(vb);
	vb.populateValues((String)getDisplayFieldValue(
	    TBL_SECURITY_MECH_ID_HREF_ACTION));
	vb.forwardTo(getRequestContext());
    }


    /**
     * Handles remove security mechanism ID request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblSecurityMechIDButtonDeleteRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
	try {
	    SMDiscoEntryData smData = getValues(false);
	    CCActionTable table = (CCActionTable)getChild(
		ATTRIBUTE_NAME_SECURITY_MECH_ID);
	    table.restoreStateData();
	    CCActionTableModel tblModel = (CCActionTableModel)
		propertySheetModel.getModel(ATTRIBUTE_NAME_SECURITY_MECH_ID);
	    Integer[] selected = tblModel.getSelectedRows();

	    if ((selected != null) && (selected.length > 0)) {
		for (int i = selected.length-1; i >= 0; --i) {
		    Integer index = selected[i];
		    smData.descData.remove(index.intValue());
		}
		setValues(smData, getModel());
		populateDescriptionsTable(smData.descData);
	    }
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(
		CCAlert.TYPE_ERROR, "message.error", e.getMessage());
	}
                                                                                
        forwardTo();
    }

    protected abstract void createPageTitleModel();
    protected abstract void handleButton1Request(SMDiscoEntryData smData)
	throws AMConsoleException;

}
