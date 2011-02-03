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
 * $Id: NewRoleViewBean.java,v 1.2 2008/06/25 05:42:54 qcheng Exp $
 *
 */

package com.sun.identity.console.dm;

import com.iplanet.am.sdk.AMObject;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.dm.model.RoleModel;
import com.sun.identity.console.dm.model.RoleModelImpl;
import com.sun.identity.console.realm.RMRealmOpViewBeanBase;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;

public class NewRoleViewBean
    extends RMRealmOpViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/NewRole.jsp";

    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String ROLE_TYPE_CB = "iplanet-am-role-type";
    private static final String PERMISSION_LIST = "iplanet-am-role-aci-list";

    private int roleType = -1;

    private AMPropertySheetModel propertySheetModel;
    private RoleModel model = null;

    public NewRoleViewBean() {
	super("NewRole");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {
            String role = (String)getPageSessionAttribute(RoleModel.ROLE_TYPE);

            if (role != null) {
		super.initialize();
		roleType = Integer.parseInt(role);

	        createPageTitleModel();
	        createPropertyModel();
	        registerChildren();
                initialized = true;
            }
        }
    }

    protected void registerChildren() {
	super.registerChildren();
	registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
	propertySheetModel.registerChildren(this);
    }

    protected View createChild(String name) {
	View view = null;

	if (name.equals(PGTITLE_TWO_BTNS)) {
	    view = new CCPageTitle(this, ptModel, name);
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

	setAccessPermissions();
        setRoleTypes();
	if (roleType == AMObject.ROLE) {
	    ptModel.setPageTitleText(
		model.getLocalizedString("page.title.create.role"));
        } else {
	    ptModel.setPageTitleText(
		model.getLocalizedString("page.title.create.filtered.role"));
	}
    }

    private void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/twoBtnsPageTitle.xml"));
	ptModel.setValue("button1", "button.ok");
	ptModel.setValue("button2", "button.cancel");
    }

    /**
     * Populate the values for the access permission combobox.
     */
    protected void setAccessPermissions() {
        Set aciList = model.getDefaultPermissions();
        OptionList permList = new OptionList();

        if ((aciList != null) && (aciList.size() > 0)) {
            for (Iterator it = aciList.iterator(); it.hasNext(); ) {
                String type = (String)it.next();
                permList.add(model.getOptionString(type),type);
            }
            CCDropDownMenu cb = (CCDropDownMenu)getChild(PERMISSION_LIST);
            cb.setOptions(permList);
        }
    }

    /**
     * Populate the values for the role types combobox.   Current types
     * are Administrative or Service.
     */
    protected void setRoleTypes() {
        Map roleTypes = model.getDefaultTypes();
        if (roleTypes != null) {
            OptionList typeList = new OptionList();
            String defaultValue = null;

            
            for (Iterator it = roleTypes.keySet().iterator(); it.hasNext(); ) {
                String type = (String)it.next();
                String value = (String)roleTypes.get(type);
                if (defaultValue == null) {
                    defaultValue = value;
                }
                typeList.add(type, value);
            }

            CCDropDownMenu cb = (CCDropDownMenu)getChild(ROLE_TYPE_CB);
            cb.setOptions(typeList);

            if (defaultValue != null ) {
                setDisplayFieldValue(ROLE_TYPE_CB, defaultValue);
            }
        }
    }

    private void createPropertyModel() {
	model = (RoleModel)getModel();
	propertySheetModel = new AMPropertySheetModel(
	    model.getRoleCreateXML(roleType));
	propertySheetModel.clear();
    }
    
    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
	model = new RoleModelImpl(rc.getRequest(), getPageSessionAttributes());
        return model;
    }

    /**
     * Handles create realm request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
	throws ModelControlException
    {
	RoleModel model = (RoleModel)getModel();
	String location = (String)getPageSessionAttribute(
	    AMAdminConstants.CURRENT_ORG);
        if (location == null || location.length() == 0) {
            location = model.getStartDSDN();
	}

	try {
            AMPropertySheet ps = 
	        (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);

	    Map values = ps.getAttributeValues(
		model.getDataMap(roleType), false, model);
            if (values == null) {
                values = new HashMap();
            }
            // set the type of role being created. 
            values.put(RoleModel.ROLE_TYPE, 
                (String)getPageSessionAttribute(RoleModel.ROLE_TYPE));
            model.createRole(location, values);
            forwardToRoleView(event);
        } catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
	        e.getMessage());
	    forwardTo();
	}
        setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information","TBD");
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardToRoleView(event);
    }

    private void forwardToRoleView(RequestInvocationEvent event){
        RoleViewBean vb = (RoleViewBean)getViewBean(RoleViewBean.class);
	backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
	return "breadcrumbs.directorymanager.role.add";
    }

    protected boolean startPageTrail() {
	return false;
    }
}
