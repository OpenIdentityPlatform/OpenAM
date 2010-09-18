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
 * $Id: OrganizationPropertiesBase.java,v 1.2 2008/06/25 05:42:55 qcheng Exp $
 *
 */

package com.sun.identity.console.dm;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.dm.model.UMOrganizationModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.tabs.CCTabs;
import java.text.MessageFormat;

public  abstract class OrganizationPropertiesBase 
    extends GeneralPropertiesBase
{
    private static final String ORG_TABS = "organization";

    public CCPageTitleModel ptModel = null;
    public UMOrganizationModel model = null;

    public OrganizationPropertiesBase(String name) {
	super(name);
    }

    protected void registerChildren() {
	super.registerChildren();
	registerChild(TAB_COMMON, CCTabs.class);
    }
 
    /**
     * Handles tab selected event. 
     *
     * @param event Request Invocation Event.
     * @param nodeID Selected Node ID.
     */
    public void nodeClicked(RequestInvocationEvent event, int nodeID) {
        try {
	    AMViewBeanBase vb = getTabNodeAssociatedViewBean(ORG_TABS, nodeID);

	    String tmp = (String)getPageSessionAttribute(
		AMAdminConstants.CURRENT_ORG);
	    vb.setPageSessionAttribute(AMAdminConstants.CURRENT_ORG, tmp);
	    
            tmp = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_PROFILE);
	    vb.setPageSessionAttribute(
                AMAdminConstants.CURRENT_PROFILE, tmp);

	    tmp = (String)getPageSessionAttribute(
		AMAdminConstants.PREVIOUS_TAB_ID);
	    vb.setPageSessionAttribute(AMAdminConstants.PREVIOUS_TAB_ID, tmp);
	    unlockPageTrailForSwapping();
	    passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            debug.error("OrganizationPropertiesBase.nodeClicked", e);
            forwardTo();
        }
    }

    protected void createTabModel() {
	if (tabModel == null) {
	    AMViewConfig amconfig = AMViewConfig.getInstance();
	    String realmName = (String)getPageSessionAttribute(
		AMAdminConstants.CURRENT_PROFILE);
	    tabModel = amconfig.getTabsModel(ORG_TABS, realmName,
		getRequestContext().getRequest());
	}
    }

    protected void forwardToOrganizationView(RequestInvocationEvent event) {   
        // reset the current org to be the org that was being viewed before
        // the profile page was opened.
	String tmp = 
	    (String)getPageSessionAttribute(AMAdminConstants.CURRENT_ORG);
	setPageSessionAttribute(AMAdminConstants.CURRENT_PROFILE, tmp);

        // reset the tab selected to the realm view
	tmp = (String)getPageSessionAttribute(
	    AMAdminConstants.PREVIOUS_TAB_ID);
	setPageSessionAttribute(getTrackingTabIDName(), tmp);

        // and now forward on to the realm page...
        OrganizationViewBean vb = (OrganizationViewBean)getViewBean(
            OrganizationViewBean.class);
	backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    public void setPageTitle(String title) {
        String name = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);

        model = (UMOrganizationModel)getModel();
        if (name == null || name.length() == 0) {
            name = model.getStartDSDN();
        }
        String[] tmp = { AMFormatUtils.DNToName(model, name)  } ;
        ptModel.setPageTitleText(MessageFormat.format(
	    model.getLocalizedString(title), (Object[])tmp));
    }

    protected String getBreadCrumbDisplayName() {
	UMOrganizationModel model = (UMOrganizationModel)getModel();
        String name = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);
	String[] arg = {AMFormatUtils.DNToName(model, name)};
	return MessageFormat.format(model.getLocalizedString(
	    "breadcrumbs.directorymanager.organization.edit"), (Object[])arg);
    }

    protected boolean startPageTrail() {
	return false;
    }
}
