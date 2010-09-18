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
 * $Id: GroupViewBean.java,v 1.2 2008/06/25 05:42:54 qcheng Exp $
 *
 */

package com.sun.identity.console.dm;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.dm.model.GroupModel;
import com.sun.identity.console.dm.model.GroupModelImpl;
import com.sun.web.ui.model.CCActionTableModel;
import java.util.Set;

public class GroupViewBean
    extends DMTypeBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/Group.jsp";

    GroupModel model = null;
    protected static final String TBL_ADD_STATIC = "tblButtonAddStatic";
    protected static final String TBL_ADD_DYNAMIC = "tblButtonAddDynamic";

    /**
     * Creates a authentication domains view bean.
     */
    public GroupViewBean() {
	super("Group");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    /**
     * disable the add/new button is you cant create an organization
     * at the current location.
     */
    protected void setAddButtonState(String location) {
        if (!model.createGroup(location)) {
            disableButton(TBL_ADD_STATIC, true);
            disableButton(TBL_ADD_DYNAMIC, true);
        }
    }  

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        return new GroupModelImpl(
            rc.getRequest(), getPageSessionAttributes());
    }

    protected Set getEntries() {
        String filter = getFilter();
	String location = (String)getPageSessionAttribute(
	    AMAdminConstants.CURRENT_ORG);

    	model = (GroupModel)getModel();
        if (location == null || location.length() == 0) {
            location = model.getStartDSDN();
        }
        return model.getGroups(location, filter);
    }

    protected void createTableModel() {
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblDMGroups.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_ADD_STATIC, "table.dm.button.new.static");
        tblModel.setActionValue(TBL_ADD_DYNAMIC, "table.dm.button.new.dynamic");
        tblModel.setActionValue(TBL_BUTTON_DELETE, "table.dm.button.delete");
        tblModel.setActionValue(TBL_COL_NAME, "table.dm.name.column.name");
        tblModel.setActionValue(TBL_COL_PATH, "table.dm.path.column.name");
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
    public void handleTblButtonAddStaticRequest(RequestInvocationEvent event) {
        NewStaticGroupViewBean vb = (NewStaticGroupViewBean)
            getViewBean(NewStaticGroupViewBean.class);
        unlockPageTrail();
	passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Forwards request to creation view bean.
     * 
     * @param event Request Invocation Event.
     */
    public void handleTblButtonAddDynamicRequest(RequestInvocationEvent event) {
        NewDynamicGroupViewBean vb = (NewDynamicGroupViewBean)
            getViewBean(NewDynamicGroupViewBean.class);
        unlockPageTrail();
	passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }


    /**
     * Handles the edit object properties request.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblDataActionHrefRequest(RequestInvocationEvent event)
	throws ModelControlException
    {
        // TBD LOG VIEW PROPERTY EVENT
        String group = (String)getDisplayFieldValue(TBL_DATA_ACTION_HREF);
        setPageSessionAttribute(AMAdminConstants.CURRENT_PROFILE, group);
        setPageSessionAttribute("AMAdminConstants.GROUP_NAME", group);

        // store the current selected tab in the page session
        String tmp = (String)getPageSessionAttribute(getTrackingTabIDName());
        setPageSessionAttribute(AMAdminConstants.PREVIOUS_TAB_ID, tmp);

        GroupMembersViewBean vb = (GroupMembersViewBean)
            getViewBean(GroupMembersViewBean.class);
        unlockPageTrail();
	passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles the drill down navigation request.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblDataHrefRequest(RequestInvocationEvent event)
	throws ModelControlException {
        // TBD LOG NAVIGATION EVENT
        String tmp = (String)getDisplayFieldValue(TBL_DATA_HREF);
	setPageSessionAttribute(AMAdminConstants.CURRENT_ORG, tmp);
	forwardTo();
    }

    protected String getBreadCrumbDisplayName() {
	return "breadcrumbs.directorymanager.group";
    }
}
