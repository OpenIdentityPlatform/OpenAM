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
 * $Id: UserViewBean.java,v 1.3 2009/01/28 05:34:57 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.dm;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.dm.model.UserModel;
import com.sun.identity.console.dm.model.UserModelImpl;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.web.ui.model.CCActionTableModel;
import java.util.Iterator;
import java.util.Set;
import java.io.Serializable;
import com.sun.identity.shared.ldap.util.DN;

public class UserViewBean
    extends DMTypeBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/User.jsp";

    UserModel model = null;

    /**
     * Creates a authentication domains view bean.
     */
    public UserViewBean() {
	super("User");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    /**
     * disable the add/new button is you cant create an organization
     * at the current location.
     */
    protected void setAddButtonState(String location) {
        if (!model.createUser(location)) {
            disableButton(TBL_BUTTON_ADD, true);
        }
    } 

    protected AMModel getModelInternal() {
	RequestContext rc = RequestManager.getRequestContext();
	return new UserModelImpl(
	    rc.getRequest(), getPageSessionAttributes());
    }

    protected Set getEntries() {
	String filter = getFilter();
	String location = (String)getPageSessionAttribute(
	    AMAdminConstants.CURRENT_ORG);
    	model = (UserModel)getModel();
	Set values = model.getUsers(location, filter);

	return values;
    }

    protected void populateTableModel(Set realmNames) {
	tblModel.clearAll();
	UserModel model = (UserModel)getModel();
	tblModel.setMaxRows(model.getPageSize());
	SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        DN userDN = new DN(model.getUserDN());

	if ((realmNames != null) && !realmNames.isEmpty()) {
	    boolean firstEntry = true;

	    for (Iterator iter = realmNames.iterator(); iter.hasNext(); ) {
		if (firstEntry) {
		    firstEntry = false;                                  
		} else {
		    tblModel.appendRow();
		}
		String dn = (String)iter.next();

                /* 
                * user name column. the value displayed in the column is
                * defined in the admin service, search return attribute
                */
                tblModel.setValue(TBL_DATA_NAME, model.getUserDisplayValue(dn));
                tblModel.setSelectionVisible(!userDN.equals(new DN(dn)));
                tblModel.setValue(TBL_DATA_ACTION_HREF, dn);

		// path name column
		tblModel.setValue(TBL_DATA_PATH, getPath(model, dn));
		tblModel.setValue(TBL_DATA_HREF, dn);
	    }
	    szCache.setValue((Serializable)realmNames);
	} else {
	    szCache.setValue(null);
	}
    }
    
    protected String getTableXML() {
	return "com/sun/identity/console/tblDMUser.xml";
    }

    /**
     * Handles search request.
     *
     * @param event Request Invocation Event.
     */
    public void handleBtnSearchRequest(RequestInvocationEvent event) {
	forwardTo();
    }

    /**
     * Forwards request to creation view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblButtonAddRequest(RequestInvocationEvent event) {
	NewUserViewBean vb = (NewUserViewBean)getViewBean(
	    NewUserViewBean.class);
	unlockPageTrail();
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    /**
     * Handles edit user request.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblDataActionHrefRequest(RequestInvocationEvent event)
	throws ModelControlException
    {
	String tmp = (String)getDisplayFieldValue(TBL_DATA_ACTION_HREF);
	setPageSessionAttribute(AMAdminConstants.CURRENT_PROFILE, tmp);
	
	// store the current selected tab in the page session
	tmp = (String)getPageSessionAttribute(getTrackingTabIDName());
	setPageSessionAttribute(AMAdminConstants.PREVIOUS_TAB_ID, tmp);

	UserGeneralViewBean vb = (UserGeneralViewBean)
	    getViewBean(UserGeneralViewBean.class);
	unlockPageTrail();
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    protected void createTableModel() {
    	model = (UserModel)getModel();
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(getTableXML()));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_BUTTON_ADD, "table.dm.button.new");
        tblModel.setActionValue(TBL_BUTTON_DELETE, "table.dm.button.delete");
        tblModel.setActionValue(TBL_COL_NAME, model.getNameColumnLabel());
        tblModel.setActionValue(TBL_COL_PATH, "table.dm.path.column.name");
    }

    protected String getBreadCrumbDisplayName() {
	return "breadcrumbs.directorymanager.user";
    }
}
