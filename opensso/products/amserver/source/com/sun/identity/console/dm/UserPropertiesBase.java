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
 * $Id: UserPropertiesBase.java,v 1.2 2008/06/25 05:42:55 qcheng Exp $
 *
 */


package com.sun.identity.console.dm;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMSystemConfig;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.dm.model.UserModel;
import com.sun.identity.console.dm.model.UserModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.tabs.CCTabs;
import java.text.MessageFormat;

public abstract class UserPropertiesBase 
    extends GeneralPropertiesBase
{
    private static final String PROFILE_TAB = "users";

    public CCPageTitleModel ptModel = null;
    public UserModel model = null;
    private String userName = null;

    public UserPropertiesBase(String name) {
	super(name);
    }

    protected void registerChildren() {
	registerChild(TAB_COMMON, CCTabs.class);
	super.registerChildren();
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
		PROFILE_TAB, nodeID);
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
            debug.error("UserPropertiesBase.nodeClicked", e);
            forwardTo();
        }
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }

    protected void createTabModel() {
	if (tabModel == null) {
	    AMViewConfig amconfig = AMViewConfig.getInstance();
	    String realmName = (String)getPageSessionAttribute(
		AMAdminConstants.CURRENT_PROFILE);
	    tabModel = amconfig.getTabsModel(PROFILE_TAB, realmName,
		getRequestContext().getRequest());
	}
    }

    protected void forwardToUserView(RequestInvocationEvent event) {   
        // reset the current org to be the org that was being viewed before
        // the user profile page was opened.
	String tmp = 
	    (String)getPageSessionAttribute(AMAdminConstants.CURRENT_ORG);
	setPageSessionAttribute(AMAdminConstants.CURRENT_ORG, tmp);

        // reset the tab selected to the realm view
	tmp = (String)getPageSessionAttribute(
	    AMAdminConstants.PREVIOUS_TAB_ID);
	setPageSessionAttribute(getTrackingTabIDName(), tmp);

        // and now forward on to the realm page...
        UserViewBean vb = (UserViewBean)getViewBean(
            UserViewBean.class);
        backTrail();
	passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    public void setPageTitle(String title) {
        String name =  (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);

        if (name == null || name.length() == 0) {
            name = AMSystemConfig.defaultOrg;
        }
        model = (UserModel)getModel();
        String[] tmp = { AMFormatUtils.DNToName(model, name)  } ;
        ptModel.setPageTitleText(MessageFormat.format(
	    model.getLocalizedString(title), 
	    (Object[])tmp));
    }

    protected String getUserName() {
        if (userName == null) {
            userName = (String)getPageSessionAttribute(
	        AMAdminConstants.CURRENT_PROFILE);
	    if (userName == null) {
	        return "";
	    }
	}
	return userName;
    }

    protected AMModel getModelInternal() {
        if (model == null) {
            RequestContext rc = RequestManager.getRequestContext();
            model = new UserModelImpl(
                rc.getRequest(), getPageSessionAttributes());
        }
        return model;
    }

    protected String getBreadCrumbDisplayName() {
        UserModel lmodel = (UserModel)getModel();
        String name = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);
      String[] arg = {AMFormatUtils.DNToName(lmodel, name)};
      return MessageFormat.format(lmodel.getLocalizedString(
          "breadcrumbs.directorymanager.user.edit"), (Object[])arg);
    }

    /**
     * Handles back to Organization view request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        backTrail();
        UserViewBean vb = (UserViewBean)
            getViewBean(UserViewBean.class);
        String tmp = (String)getPageSessionAttribute(
            AMAdminConstants.PREVIOUS_TAB_ID);
        setPageSessionAttribute(getTrackingTabIDName(), tmp);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Creates the label used to take the user back to the previous page
     * they were viewing. The label is defined in the property file as
     * "Back to {0}" which lets us control the format of the label in
     * different locales.  The name of the current object is substituted.
     */
    protected String getBackButtonLabel() {
        return getBackButtonLabel("breadcrumbs.directorymanager.user");
    }

    protected boolean startPageTrail() {
	return false;
    }
}
