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
 * $Id: UserGeneralViewBean.java,v 1.2 2008/06/25 05:42:55 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.dm;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.dm.model.UserModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Map;

public class UserGeneralViewBean
    extends UserPropertiesBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/UserGeneral.jsp";

    private static final String PAGE_TITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTES = "propertyAttributes";
    private AMPropertySheetModel psModel = null;
    private boolean submitCycle;

    /**
     * Creates a realm creation view bean.
     */
    public UserGeneralViewBean() {
	super("UserGeneral");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
	if (!initialized) {
	    String name = (String)getPageSessionAttribute(
		AMAdminConstants.CURRENT_PROFILE);

	    if (name != null) {
		super.initialize();
		initialized = true;
		createPageTitleModel();
		createPropertyModel(name);
		registerChildren();
	    }
	}
    }

    protected void registerChildren() {
	registerChild(PROPERTY_ATTRIBUTES, AMPropertySheet.class);
	psModel.registerChildren(this);
	ptModel.registerChildren(this);
	super.registerChildren();
    }

    protected View createChild(String name) {
	View view = null;

	if (name.equals(PAGE_TITLE)) {
	    view = new CCPageTitle(this, ptModel, name);
	} else if (name.equals(PROPERTY_ATTRIBUTES)) {
	     view = new AMPropertySheet(this, psModel, name);
	} else if (psModel.isChildSupported(name)) {
	    view = psModel.createChild(this, name);
	} else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name); 
	} else {
	    view = super.createChild(name);
	}

	return view;
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException
    {
	super.beginDisplay(event);

	if (!submitCycle) {
	    model = (UserModel)getModel();
	    if (model != null) {
		try {
		    AMPropertySheet ps = 
			(AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
		    String name = (String)getPageSessionAttribute(
			AMAdminConstants.CURRENT_PROFILE);
		    psModel.clear();
		    ps.setAttributeValues(model.getValues(name), model);
		} catch (AMConsoleException e) {
		    setInlineAlertMessage(
			CCAlert.TYPE_WARNING, "message.warning",
			"noproperties.message");
		}
	    }
        }

        setPageTitle("page.title.user.properties");
    }


    /**
     * Handles create realm request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
	throws ModelControlException
    {
	submitCycle = true;
	UserModel model = (UserModel)getModel();
        String name = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);
        try {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
            Map values = ps.getAttributeValues(
		model.getValues(name), false, model);
            model.updateUser(name, values);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                model.getLocalizedString("profile.updated"));
            forwardTo();
         } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }

    private void createPropertyModel(String userName) {
	UserModel model = (UserModel)getModel();
	psModel = new AMPropertySheetModel(
	    model.getUserProfileXML(userName, getClass().getName()));
	psModel.clear();
    }
}
