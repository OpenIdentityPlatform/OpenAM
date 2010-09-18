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
 * $Id: NewUserViewBean.java,v 1.2 2008/06/25 05:42:54 qcheng Exp $
 *
 */

package com.sun.identity.console.dm;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.dm.model.UserModel;
import com.sun.identity.console.dm.model.UserModelImpl;
import com.sun.identity.console.realm.RMRealmOpViewBeanBase;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NewUserViewBean
    extends RMRealmOpViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/NewUser.jsp";

    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String NAME = "tfName";

    private AMPropertySheetModel propertySheetModel;
    private UserModel model = null;

    /**
     * Creates a realm creation view bean.
     */
    public NewUserViewBean() {
	super("NewUser");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {
            String location = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_ORG);
            if (location != null) {
                super.initialize();
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
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        ps.init();
	super.beginDisplay(event);
	setPeopleContainerList();
    }

    private void setPeopleContainerList() {
        String location = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_ORG);
        UserModel model = (UserModel)getModel();

	Set peopleContainers = model.getPeopleContainers(location);
        if ((peopleContainers != null) && (peopleContainers.size() > 1) &&
	    !model.showPeopleContainers() ) 
	{
            OptionList containers = new OptionList();
	    for (Iterator i = peopleContainers.iterator(); i.hasNext();) {
		String entry = (String)i.next();
                containers.add(entry, entry);
	    }
            CCDropDownMenu cb = (CCDropDownMenu)getChild(
                UserModel.PEOPLE_CONTAINER);
            cb.setOptions(containers);
        }
    }

    private void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/twoBtnsPageTitle.xml"));
	ptModel.setValue("button1", "button.ok");
	ptModel.setValue("button2", "button.cancel");
    }

    private void createPropertyModel() {
        UserModel model = (UserModel)getModel();
        
        String location = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_ORG);
        
        propertySheetModel = new AMPropertySheetModel(
            model.getCreateUserPropertyXML(location));
        propertySheetModel.clear();
    }

    protected AMModel getModelInternal() {
        if (model == null) {
            RequestContext rc = RequestManager.getRequestContext();
            model = new UserModelImpl(
                rc.getRequest(), getPageSessionAttributes());
        }
        return model;
    }

    /**
     * Handles create user request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
	throws ModelControlException
    {
        UserModel model = (UserModel)getModel();

        /*
         * get the location where the user is being created. If the admin
         * is currently in the group members page, the profile attribute
         * will be set and we can use that value as the location. If the admin
         * is not in a group, the profile will set to the users starting dn 
	 * and we need to then take the location attribute as the value to use.
         */
        String location = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);

	String startDN = model.getStartDN();
	if (location.equalsIgnoreCase(startDN)) {
            location = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_ORG);
        }

        try {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            Map values = ps.getAttributeValues(model.getDataMap(), false,model);
            model.createUser(location, values);
            forwardToUserView(event);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardToUserView(event);
    }

    private void forwardToUserView(RequestInvocationEvent event){
        String returnVB = (String)removePageSessionAttribute(
            AMAdminConstants.SAVE_VB_NAME);

        if (returnVB == null) {
	    debug.warning("viewbean not set returning to user view");
            UserViewBean vb = (UserViewBean)getViewBean(
                UserViewBean.class);
            backTrail();
	    passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } else {
            try {
                String tmp = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_ORG);
                Class clazz = Class.forName(returnVB);
                AMViewBeanBase vb = (AMViewBeanBase)getViewBean(clazz);
                backTrail();
		passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            } catch (ClassNotFoundException cnfe) {
                forwardTo(getRequestContext());
            }
        }
    }

    protected String getBreadCrumbDisplayName() {
	return "breadcrumbs.directorymanager.user.add";
    }

    protected boolean startPageTrail() {
	return false;
    }
}

