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
 * $Id: GroupContainerGeneralViewBean.java,v 1.2 2008/06/25 05:42:54 qcheng Exp $
 *
 */

package com.sun.identity.console.dm;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.dm.model.GroupContainerModel;
import com.sun.identity.console.dm.model.GroupContainerModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.text.MessageFormat;

public class GroupContainerGeneralViewBean
    extends AMPrimaryMastHeadViewBean 
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/GroupContainerGeneral.jsp";

    public CCPageTitleModel ptModel = null;
    private static final String PAGE_TITLE = "pgtitle";
    private GroupContainerModel model = null;

    /**
     * Creates a realm creation view bean.
     */
    public GroupContainerGeneralViewBean() {
	super("GroupContainerGeneral");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
	createPageTitleModel();
	registerChildren();
    }

    protected void registerChildren() {
	ptModel.registerChildren(this);
	super.registerChildren();
    }

    protected View createChild(String name) {
	View view = null;

	if (name.equals(PAGE_TITLE)) {
	    view = new CCPageTitle(this, ptModel, name);
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

        model = (GroupContainerModel)getModel();
        if (!model.hasDisplayProperties()) {
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                model.getLocalizedString("no.properties"));
        }

        setPageTitle("page.title.group.container.properties");
    }

    private void createPageTitleModel() {
        model = (GroupContainerModel)getModel();
        if (!model.hasDisplayProperties()) {
            ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/oneBtnPageTitle.xml"));
            ptModel.setValue("button1", getBackButtonLabel());
        } else {
            ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
            ptModel.setValue("button1", "button.save");
            ptModel.setValue("button2", "button.reset");
            ptModel.setValue("button3", getBackButtonLabel());
        }
    }

    protected AMModel getModelInternal() {
        if (model == null) {
            RequestContext rc = RequestManager.getRequestContext();
            model = new GroupContainerModelImpl(
                rc.getRequest(), getPageSessionAttributes());
        }
        return model;
    }

    private void setPageTitle(String title) {
        GroupContainerModel model = (GroupContainerModel)getModel();
        String[] tmp = {getDisplayName()} ;
        ptModel.setPageTitleText(MessageFormat.format(
            model.getLocalizedString(title), (Object[])tmp));
    }

    private String getDisplayName() {
        String name =  (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_ORG);
        GroupContainerModel model = (GroupContainerModel)getModel();
        if (name == null || name.length() == 0) {
            name = model.getStartDSDN();
        }
        return AMFormatUtils.DNToName(model, name);
    }

    protected String getBreadCrumbDisplayName() {
	GroupContainerModel lmodel = (GroupContainerModel)getModel();
	String[] arg = {getDisplayName()};
	return MessageFormat.format(lmodel.getLocalizedString(
	    "breadcrumbs.directorymanager.groupcontainer.edit"), (Object[])arg);
    }

    protected boolean startPageTrail() {
	return false;
    }

    /**
     * Creates the label used to take the user back to the previous page
     * they were viewing. The label is defined in the property file as
     * "Back to {0}" which lets us control the format of the label in
     * different locales.  The name of the current object is substituted.
     */
    protected String getBackButtonLabel() {
        return getBackButtonLabel(
            "breadcrumbs.directorymanager.groupcontainer");
    }

    /**
     * Handles create request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) 
        throws ModelControlException
    {
        model = (GroupContainerModel)getModel();
        if (model.hasDisplayProperties()) {
            forwardTo();
        } else {
            forwardToGroupContainerView(event);
        }
    }

    /**
     * Handles create realm request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) 
        throws ModelControlException
    {
        forwardTo();
    }

    /**
     * Handles create realm request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) 
        throws ModelControlException
    {
        forwardToGroupContainerView(event);
    }

    private void forwardToGroupContainerView(RequestInvocationEvent event) {   
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
        GroupContainerViewBean vb = (GroupContainerViewBean)getViewBean(
            GroupContainerViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
}
