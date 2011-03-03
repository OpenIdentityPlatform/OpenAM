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
 * $Id: RoleMembersViewBean.java,v 1.2 2008/06/25 05:42:55 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.dm;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.dm.model.RoleModel;
import com.sun.identity.console.dm.model.RoleModelImpl;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCActionTableModelInterface;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.List;

public class RoleMembersViewBean
    extends RolePropertiesBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/RoleMembers.jsp";

    protected static final String TF_FILTER = "tfFilter";
    protected static final String BTN_SEARCH = "btnSearch";
    protected static final String BTN_SHOW = "btnShowMenu";
    protected static final String PAGE_TITLE = "pgtitle";

    protected static final String TBL_SEARCH = "tblSearch";
    protected static final String TBL_COL_NAME = "tblColName";
    protected static final String TBL_DATA_NAME = "tblDataName";

    protected static final String TBL_COL_PATH = "tblColPath";
    protected static final String TBL_DATA_PATH = "tblDataPath";

    protected static final String ACTION_MENU = "actionMenu";
    protected CCActionTableModel tblModel = null;

    private static final String ADD_USERS = "role.add.user"; 
    private static final String REMOVE_MEMBERS = "role.remove.member";
    private static final String SELECT_ACTION = "role.members.action";

    RoleModel model = null;

    /**
     * Creates a authentication domains view bean.
     */
    public RoleMembersViewBean() {
	super("RoleMembers");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createTableModel();
	registerChildren();
    }

    protected void registerChildren() {
	super.registerChildren();
	registerChild(PAGE_TITLE, CCPageTitle.class);
	registerChild(TBL_SEARCH, CCActionTable.class);
        registerChild(TF_FILTER, CCTextField.class);
        registerChild(BTN_SEARCH, CCButton.class);
        registerChild(BTN_SHOW, CCButton.class);
	ptModel.registerChildren(this);
	tblModel.registerChildren(this);
    }

    protected View createChild(String name) {
	View view = null;

	if (name.equals(TBL_SEARCH)) {
	    SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
	    populateTableModel((List)szCache.getSerializedObj());
	    view = new CCActionTable(this, tblModel, name);
	} else if (name.equals(PAGE_TITLE)) {
	    view = new CCPageTitle(this, ptModel, name);
	} else if (tblModel.isChildSupported(name)) {
	    view = tblModel.createChild(this, name);
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
	populateTableModel(getEntries());
        setMemberActions();
        setPageTitle("page.title.role.members");
    }

    protected AMModel getModelInternal() { 
        if (model == null) {
            RequestContext rc = RequestManager.getRequestContext();
            model = new RoleModelImpl(
                rc.getRequest(), getPageSessionAttributes());
        }
        return model;
    }

    /**
     * If this is a static role, we need to create the action menu to add
     * and remove users.
     * Dynamic roles do not have this as membership is defined by the 
     * role filter.
     */
    protected void setMemberActions() {
        String roleName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);

        model = (RoleModel)getModel();
        if (!model.isFilteredRole(roleName)) {
            OptionList permList = new OptionList();
            permList.add(model.getLocalizedString(SELECT_ACTION), "");
            permList.add(model.getLocalizedString(ADD_USERS), ADD_USERS);
            permList.add(
		model.getLocalizedString(REMOVE_MEMBERS), REMOVE_MEMBERS);
            CCDropDownMenu cb = (CCDropDownMenu)getChild(ACTION_MENU);
            cb.setOptions(permList);
	}
    }

    /**
     * Returns all the members of this role that match the filter. The filter
     * defaults to "*" if its not defined.
     */
    protected Set getEntries() {
        Set members = Collections.EMPTY_SET;
        try {
            CCTextField tf = (CCTextField)getChild(TF_FILTER);
            String filter = (String)tf.getValue();
            if ((filter == null) || (filter.length() == 0)) {
                filter = "*";
            }
            String role =  (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_PROFILE);

            model = (RoleModel)getModel();
            members = model.getMembers(role, filter);
        } catch (AMConsoleException a) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                a.getMessage());
        }
        return members;
    }

    /**
     * Handles "Back to..." button request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) {
        forwardToRoleView(event);
    }

    protected void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/oneBtnPageTitle.xml"));
        ptModel.setValue("button1", getBackButtonLabel());
    }

    private  void createTableModel() {
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
	    "com/sun/identity/console/tblRoleMembers.xml"));

        model = (RoleModel)getModel();
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_COL_NAME, model.getNameColumnLabel());
        tblModel.setActionValue(TBL_COL_PATH, "table.dm.path.column.name");
    }
       
    protected void populateTableModel(Collection members) {
        tblModel.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        model = (RoleModel)getModel();
        tblModel.setMaxRows(model.getPageSize());
        String roleName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);

        if (model.isFilteredRole(roleName)) {
            tblModel.setSelectionType(CCActionTableModelInterface.NONE);
        }
        if ((members != null) && !members.isEmpty()) {
            List cache = new ArrayList(members.size()*2);
            boolean firstEntry = true;
            int rowIdx = 0;
            for (Iterator iter = members.iterator(); iter.hasNext(); ) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblModel.appendRow();
                }
                String name = (String)iter.next();
                tblModel.setValue(TBL_DATA_NAME, model.getUserDisplayValue(name));
                tblModel.setValue(TBL_DATA_PATH, model.getDisplayPath(name));

                cache.add(name);
            }
            szCache.setValue((Serializable)cache);
        } else {
            szCache.setValue(null);
        }
    }

    /**
     * Forwards request to creation view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleBtnShowMenuRequest(RequestInvocationEvent event) 
	throws ModelControlException
    {
       String action = (String)getDisplayFieldValue(ACTION_MENU);
       if (action.equals(ADD_USERS)) {
            setPageSessionAttribute(AMAdminConstants.SAVE_VB_NAME, 
                getClass().getName());        
            SearchUsersViewBean vb = 
                (SearchUsersViewBean)getViewBean(SearchUsersViewBean.class);
	    unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
       } else if (action.equals(REMOVE_MEMBERS)) {
           removeMembers();
       } else {
           forwardTo();
       }
    }

    private void removeMembers() {
        model = (RoleModel)getModel();

        try {
            CCActionTable table = (CCActionTable)getChild(TBL_SEARCH);
            table.restoreStateData();
        } catch (ModelControlException m) {
            debug.error("RoleMembersViewBean.removeMembers ", m); 
        }
        Integer[] selected = tblModel.getSelectedRows();
        
        if (selected.length < 1) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                model.getLocalizedString("no.entries.selected"));
        } else {
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            List cache = (List)szCache.getSerializedObj();
            
            Set names = new HashSet(selected.length * 2);
            for (int i = 0; i < selected.length; i++) {
                names.add(cache.get(selected[i].intValue()));
            }

            try {
                String role = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_PROFILE);
                model.removeUsers(role, names); 
                String message = "message.delete.entries";
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    model.getLocalizedString(message));
            } catch (AMConsoleException a) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    a.getMessage());
            }
        }
    	forwardTo();
    }

    /**
     * Handles search request.
     *
     * @param event Request Invocation Event.
     */
    public void handleBtnSearchRequest(RequestInvocationEvent event) {
        forwardTo();
    }

    public boolean beginActionMenuDisplay(ChildDisplayEvent event) {
        model = (RoleModel)getModel();
        String roleName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);
        return !model.isFilteredRole(roleName);
    }
}
