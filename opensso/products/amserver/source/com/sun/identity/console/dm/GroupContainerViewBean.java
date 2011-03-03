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
 * $Id: GroupContainerViewBean.java,v 1.2 2008/06/25 05:42:54 qcheng Exp $
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
import com.sun.identity.console.dm.model.GroupContainerModel;
import com.sun.identity.console.dm.model.GroupContainerModelImpl;
import java.util.Set;

public class GroupContainerViewBean
    extends DMTypeBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/GroupContainer.jsp";

    GroupContainerModel model = null;

    /**
     * Creates a authentication domains view bean.
     */
    public GroupContainerViewBean() {
	super("GroupContainer");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    /**
     * disable the add/new button is you cant create an organization
     * at the current location.
     */
    protected void setAddButtonState(String location) {
        if (!model.createGroupContainer(location)) {
            disableButton(TBL_BUTTON_ADD, true);
        }
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        return new GroupContainerModelImpl(
            rc.getRequest(), getPageSessionAttributes());
    }

    protected Set getEntries() {
        String filter = getFilter();
	String location = (String)getPageSessionAttribute(
	    AMAdminConstants.CURRENT_ORG);

    	model = (GroupContainerModel)getModel();
        if (location == null || location.length() == 0) {
            location = model.getStartDSDN();
        }
        return model.getGroupContainers(location, filter);
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
        String tmp = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_ORG);
        NewGroupContainerViewBean vb = (NewGroupContainerViewBean)getViewBean(
	    NewGroupContainerViewBean.class);
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
        String tmp = (String)getDisplayFieldValue(TBL_DATA_ACTION_HREF);
        setPageSessionAttribute(AMAdminConstants.CURRENT_PROFILE, tmp);

        // store the current selected tab in the page session
        tmp = (String)getPageSessionAttribute(getTrackingTabIDName());
        setPageSessionAttribute(AMAdminConstants.PREVIOUS_TAB_ID, tmp);

        GroupContainerGeneralViewBean vb = (GroupContainerGeneralViewBean)
            getViewBean(GroupContainerGeneralViewBean.class);
        unlockPageTrail();
	passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
	return "breadcrumbs.directorymanager.groupcontainer";
    }
}
