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
 * $Id: UserRolesViewBean.java,v 1.2 2008/06/25 05:42:55 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.dm;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.html.OptionList;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.dm.model.UserModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

import com.sun.web.ui.model.CCAddRemoveModel;
import com.sun.web.ui.view.addremove.CCAddRemove;

public class UserRolesViewBean
    extends UserPropertiesBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/UserRoles.jsp";
    protected static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    protected static final String PAGE_TITLE = "pgtitle";
    protected static final String USER_ROLES = "userRoleSelection";
    private CCAddRemoveModel addRemoveModel;
    private AMPropertySheetModel propertySheetModel;
    private boolean canModify;
    private OptionList selectedOption;

    /**
     * Creates a authentication domains view bean.
     */
    public UserRolesViewBean() {
	super("UserRoles");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
	if (!initialized) {
	    String userName = getUserName();
	    if ((userName != null) && (userName.length() > 0)) {
		super.initialize();
		initialized = true;
		createPageTitleModel();
		createPropertySheetModel(userName);
		registerChildren();
	    }
	}
    }

    protected void registerChildren() {
	super.registerChildren();
	registerChild(PAGE_TITLE, CCPageTitle.class);
	registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
	propertySheetModel.registerChildren(this);
	ptModel.registerChildren(this);
    }

    protected void createPropertySheetModel(String userName) {
	UserModel model = (UserModel)getModel();
	canModify = model.canModify(userName, getClass().getName());
	String xmlFile = (canModify) ?
	    "com/sun/identity/console/propertyUMUserRoles.xml" :
	    "com/sun/identity/console/propertyUMUserRoles_Readonly.xml";

	propertySheetModel = new AMPropertySheetModel(
	    getClass().getClassLoader().getResourceAsStream(xmlFile));

	if (canModify) {
	    addRemoveModel = new CCAddRemoveModel();
	    addRemoveModel.setShowMoveUpDownButtons("false");
	    propertySheetModel.setModel(USER_ROLES, addRemoveModel);
	}
    }

    protected View createChild(String name) {
	View view = null;

        if (name.equals(PAGE_TITLE)) {
	    view = new CCPageTitle(this, ptModel, name);
	} else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name); 
	} else if (name.equals(PROPERTY_ATTRIBUTE)) {
	    view = new AMPropertySheet(this, propertySheetModel, name);
	} else if (propertySheetModel.isChildSupported(name)) {
	    view = propertySheetModel.createChild(this, name);
	} else {
	    view = super.createChild(name);
	}

	return view;
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException
    {
	super.beginDisplay(event);
        setAddRemoveValues();
        setPageTitle("page.title.user.roles");
    }

    /**
     * Handles edit realm request.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblDataActionHrefRequest(RequestInvocationEvent event)
	throws ModelControlException
    {
    	forwardTo();
    }

    private void setAddRemoveValues() {
        UserModel model = (UserModel)getModel();
	Set assigned = model.getAssignedRoles(getUserName());

	if (canModify) {
	    CCAddRemove addRemoveChild = (CCAddRemove)getChild(USER_ROLES);
            addRemoveChild.resetStateData();
	    Set available = null;
	    
	    if (selectedOption != null) {
		available = model.getAvailableRoles(getUserName(),
		    AMAdminUtils.toList(selectedOption));
	    } else {
		available = model.getAvailableRoles(getUserName(), assigned);
	    }

	    if (available != null) {
		addRemoveModel.setAvailableOptionList(
		    createOptionList(createMap(available)));
   
		if (selectedOption != null) {
		    addRemoveModel.setSelectedOptionList(selectedOption);
		} else {
		    if (assigned != null) {
			addRemoveModel.setSelectedOptionList(
			    createOptionList(createMap(assigned)));
		    }
		}
	    } else {
		addRemoveModel.setAvailableOptionList(new OptionList());
	    }
	} else {
	    if ((assigned != null) && !assigned.isEmpty()) {
		propertySheetModel.setValue(USER_ROLES,
		    AMAdminUtils.getString(assigned, ",", false));
	    } else {
		propertySheetModel.setValue(USER_ROLES, "");
	    }
	}
    }    
    
    /**
     * Handles create realm request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
	throws ModelControlException
    {
    	CCAddRemove roles = (CCAddRemove)getChild(USER_ROLES);
	roles.restoreStateData();

	addRemoveModel = (CCAddRemoveModel)roles.getModel();
	selectedOption = addRemoveModel.getSelectedOptionList();
        Set values = getValues(selectedOption);
	
        UserModel model = (UserModel)getModel();
	try {
	    model.updateRoles(getUserName(), values);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information", 
                model.getLocalizedString("profile.updated"));
        } catch (AMConsoleException ame) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", 
	        ame.getMessage());
	}

	forwardTo();
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
    	CCAddRemove roles = (CCAddRemove)getChild(USER_ROLES);
	roles.resetStateData();
        setAddRemoveValues();
        forwardTo();
    }

    private Map createMap(Set roles) {
	Map values = new HashMap(roles.size()*2);
        UserModel model = (UserModel)getModel();

	if ((roles != null) && !roles.isEmpty()) {
	    for (Iterator iter = roles.iterator(); iter.hasNext(); ) {
		String value = (String)iter.next();
		values.put(value, model.DNToName(value, true));
	    }
	}

	return values;
    }

}
