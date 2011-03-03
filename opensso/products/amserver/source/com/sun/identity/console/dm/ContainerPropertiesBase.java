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
 * $Id: ContainerPropertiesBase.java,v 1.2 2008/06/25 05:42:53 qcheng Exp $
 *
 */

package com.sun.identity.console.dm;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.dm.model.ContainerModel;
import com.sun.identity.console.dm.model.ContainerModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.tabs.CCTabs;
import java.text.MessageFormat;

public class ContainerPropertiesBase 
    extends GeneralPropertiesBase
{
    private static final String CONTAINER_TABS = "container";
    public static final String PAGE_TITLE = "pgtitle";
    public CCPageTitleModel ptModel = null;
    public ContainerModel model = null;

    public ContainerPropertiesBase(String name) {
	super(name);
    }

    protected void registerChildren() {
        registerChild(TAB_COMMON, CCTabs.class);
        super.registerChildren();
    }
 
    protected void createTabModel() {
	if (tabModel == null) {
	    AMViewConfig amconfig = AMViewConfig.getInstance();
	    String realmName = (String)getPageSessionAttribute(
		AMAdminConstants.CURRENT_PROFILE);
	    tabModel = amconfig.getTabsModel("container", realmName,
		getRequestContext().getRequest());
	}
    }

    protected void forwardToContainerView(RequestInvocationEvent event) {   
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
        ContainerViewBean vb = (ContainerViewBean)getViewBean(
            ContainerViewBean.class);
        backTrail();
	passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        return model = new ContainerModelImpl(
            rc.getRequest(), getPageSessionAttributes());
    }

    /**
     * Handles tab selected event.
     *
     * @param event Request Invocation Event.
     * @param nodeID Selected Node ID.
     */
    public void nodeClicked(RequestInvocationEvent event, int nodeID) {
        try {
            AMViewBeanBase vb = getTabNodeAssociatedViewBean(
	        CONTAINER_TABS, nodeID);

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
            forwardTo();
        }
    }

    protected void setPageTitle(String title) {
        String[] tmp = {getDisplayName()} ;
        ptModel.setPageTitleText(MessageFormat.format(
            model.getLocalizedString(title),
            (Object[])tmp));
    }

    protected String getBreadCrumbDisplayName() {
	AMModel lmodel = getModel();
	String[] larg = {getDisplayName()};
	return MessageFormat.format(lmodel.getLocalizedString(
          "breadcrumbs.directorymanager.container.edit"), (Object[])larg);
    }

    private String getDisplayName() {
        String name =  (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);
        AMModel lmodel = getModel();
        if (name == null || name.length() == 0) {
            name = lmodel.getStartDSDN();
        }
	return AMFormatUtils.DNToName(lmodel, name);
    }

    /**
     * Creates the label used to take the user back to the previous page
     * they were viewing. The label is defined in the property file as
     * "Back to {0}" which lets us control the format of the label in
     * different locales.
     */
    protected String getBackButtonLabel() {
        return getBackButtonLabel("breadcrumbs.directorymanager.container");
    }       
    
    protected boolean startPageTrail() {
      return false;
    }
}
