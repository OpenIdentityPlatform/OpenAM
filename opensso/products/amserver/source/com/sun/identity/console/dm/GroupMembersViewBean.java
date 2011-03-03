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
 * $Id: GroupMembersViewBean.java,v 1.2 2008/06/25 05:42:54 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.dm;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.dm.model.GroupModel;
import com.sun.identity.console.dm.model.GroupModelImpl;
import com.sun.web.ui.model.CCActionTableModel;
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
import java.util.List;
import java.util.Set;


public class GroupMembersViewBean
    extends GroupPropertiesBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/GroupMembers.jsp";

    protected static final String TF_FILTER = "tfFilter";
    protected static final String BTN_SEARCH = "btnSearch";
    protected static final String BTN_SHOW = "btnShowMenu";
    protected static final String PAGE_TITLE = "pgtitle";

    protected static final String TBL_SEARCH = "tblSearch";
    protected static final String TBL_COL_NAME = "tblColName";
    protected static final String TBL_TYPE_IMG = "tblTypeImg";
    protected static final String TBL_DATA_NAME = "tblDataName";

    protected static final String TBL_DATA_PATH = "tblDataPath";
    protected static final String TBL_COL_PATH = "tblColPath";

    protected static final String ACTION_MENU = "actionMenu";
    protected CCActionTableModel tblModel = null;

    // action view menu 
    private static final String SELECT_ACTION = "group.members.action.menu";
    private static final String SPACER = "group.members.action.spacer";
    private static final String ADD_USERS = "group.members.add.user"; 
    private static final String ADD_GROUPS = "group.members.add.group"; 
    private static final String CREATE_USER = "group.members.create.user"; 
    private static final String REMOVE_MEMBERS = "group.members.remove.member";
    private static final String DELETE_MEMBERS = "group.members.delete.member";

    private static final String GROUP_IMG = 
        "<img width=16 height=16 src=\"../console/images/LrlGroup.gif\" />";
    private static final String USER_IMG = 
        "<img width=16 height=16 src=\"../console/images/LrlUser.gif\" />";

    /**
     * Creates a authentication domains view bean.
     */
    public GroupMembersViewBean() {
	super("GroupMembers");
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
        setPageTitle("page.title.group.members");
    }

    protected AMModel getModelInternal() {                            
	RequestContext rc = RequestManager.getRequestContext();
	return new GroupModelImpl(rc.getRequest(), getPageSessionAttributes());
    }

    protected void setMemberActions() {
        GroupModel model = (GroupModel)getModel();
        OptionList actions = new OptionList();        
        
        String groupName =  (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);
        
        actions.add(model.getLocalizedString(SELECT_ACTION), "");
        
        // don't display add or create user option for dynamic groups
        if (!model.isDynamicGroup(groupName)) {
            actions.add(model.getLocalizedString(CREATE_USER), CREATE_USER);
            actions.add(model.getLocalizedString(SPACER), "");
            actions.add(model.getLocalizedString(ADD_USERS), ADD_USERS);
        }
        actions.add(model.getLocalizedString(ADD_GROUPS), ADD_GROUPS);
        actions.add(model.getLocalizedString(SPACER), "");
        actions.add(model.getLocalizedString(REMOVE_MEMBERS), REMOVE_MEMBERS);
        actions.add(model.getLocalizedString(DELETE_MEMBERS), DELETE_MEMBERS);
        
        CCDropDownMenu cb = (CCDropDownMenu)getChild(ACTION_MENU);
        cb.setOptions(actions);
    }

    /**
     * Returns all the members of this group that match the filter. The filter
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
            String group = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_PROFILE);

            GroupModel model = (GroupModel)getModel();
            members = model.getMembers(filter, group);
        } catch (AMConsoleException a) {
            debug.error("problem getting the members", a);
        }
        return members;
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
                "com/sun/identity/console/tblGroupMembers.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_COL_NAME, "name.column");
        tblModel.setActionValue(TBL_COL_PATH, "table.dm.path.column.name");
    }

    protected void populateTableModel(Collection members) {
        tblModel.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        
        GroupModel model = (GroupModel)getModel();
        tblModel.setMaxRows(model.getPageSize());
        if ((members != null) && !members.isEmpty()) {
	    List cache = new ArrayList(members.size());
            boolean firstEntry = true;
            
            // needed to determine whether checkbox is shown for user entries
            String groupName = (String)getPageSessionAttribute(
		AMAdminConstants.CURRENT_PROFILE);
            boolean dynamicGroupProfile = model.isDynamicGroup(groupName);
            int rowIdx = 0;
            for (Iterator iter = members.iterator(); iter.hasNext(); ) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblModel.appendRow();
                }
                
                String entry = (String)iter.next();
                if (model.isUser(entry)) {
                    if (dynamicGroupProfile) {
                        tblModel.setSelectionVisible(rowIdx, false);
                    }
                    tblModel.setValue(TBL_TYPE_IMG, USER_IMG); 
                    tblModel.setValue(TBL_DATA_NAME,
                        model.getUserDisplayValue(entry));
                } else {
                    tblModel.setValue(TBL_TYPE_IMG, GROUP_IMG);
                    tblModel.setValue(TBL_DATA_NAME,model.DNToName(entry,true));
                }
                
                tblModel.setValue(TBL_DATA_PATH, model.getDisplayPath(entry));
                cache.add(entry);
                rowIdx++;
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
    public void handleBtnShowMenuRequest(RequestInvocationEvent event) {
        String action = (String)getDisplayFieldValue(ACTION_MENU);
        setPageSessionAttribute(AMAdminConstants.SAVE_VB_NAME, 
            getClass().getName());        
        if (action.equals(ADD_USERS)) {
            SearchUsersViewBean vb = 
                (SearchUsersViewBean)getViewBean(SearchUsersViewBean.class);
            unlockPageTrail();
	    passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } else if (action.equals(CREATE_USER)) {
            NewUserViewBean vb = 
                (NewUserViewBean)getViewBean(NewUserViewBean.class);
            unlockPageTrail();
	    passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } else if (action.equals(REMOVE_MEMBERS)) {
            removeMembers(true);
        } else if (action.equals(DELETE_MEMBERS)) {
            removeMembers(false);
        } else if (action.equals(ADD_GROUPS)) {
            SearchGroupsViewBean vb = 
                (SearchGroupsViewBean)getViewBean(SearchGroupsViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } else {
            forwardTo();
        }
    }

    private void removeMembers(boolean remove) {
	GroupModel model = (GroupModel)getModel();
	try {
            CCActionTable table = (CCActionTable)getChild(TBL_SEARCH);
            table.restoreStateData();
        } catch (ModelControlException m) {
	    debug.error("GroupMembersViewBean.removeMembers ",m);
	}
        Integer[] selected = tblModel.getSelectedRows();
        
	if (selected.length < 1) {
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                model.getLocalizedString("no.entries.selected")); 
	} else {
	    SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
	    List cache = (List)szCache.getSerializedObj();

            Set names = new HashSet(selected.length * 2);
            for (int i = 0; i < selected.length; i++) {
		names.add(cache.get(selected[i].intValue()));
            }
                                                         
            try {
                String group = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_PROFILE);
    
		if (remove) {
                    model.removeMembers(group, names); 
		} else {
		    model.deleteObject(names);
		}
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

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) {
        forwardToGroupView(event);
    }

    /**
     * Handles search request.
     *
     * @param event Request Invocation Event.
     */
    public void handleBtnSearchRequest(RequestInvocationEvent event) {
        forwardTo();
    }

}
