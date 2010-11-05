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
 * $Id: PeopleContainerGeneralViewBean.java,v 1.2 2008/06/25 05:42:55 qcheng Exp $
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
import com.sun.identity.console.dm.model.PeopleContainerModel;
import com.sun.identity.console.dm.model.PeopleContainerModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.text.MessageFormat;

public class PeopleContainerGeneralViewBean
    extends AMPrimaryMastHeadViewBean 
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/PeopleContainerGeneral.jsp";

    public static final String PAGE_TITLE = "pgtitle";
    public CCPageTitleModel ptModel = null;
    private PeopleContainerModel model = null;

    /**
     * Creates a realm creation view bean.
     */
    public PeopleContainerGeneralViewBean() {
	super("PeopleContainerGeneral");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
	createPageTitleModel();
	registerChildren();
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException
    {
	super.beginDisplay(event);

        model = (PeopleContainerModel)getModel();
	if (!model.hasDisplayProperties()) {
	    setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                model.getLocalizedString("no.properties"));
        }

        setPageTitle("page.title.people.container.properties");
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

    protected AMModel getModelInternal() {
        if (model == null) {
            RequestContext rc = RequestManager.getRequestContext();
            model = new PeopleContainerModelImpl(
                rc.getRequest(), getPageSessionAttributes());
        } 
        return model;
    }        

    protected void setPageTitle(String title) {
        AMModel model = getModel();
        String[] tmp = {getDisplayName()} ;
        ptModel.setPageTitleText(MessageFormat.format(
            model.getLocalizedString(title),
            (Object[])tmp));
    }

    private void createPageTitleModel() {
        model = (PeopleContainerModel)getModel();
	if (!model.hasDisplayProperties()) {
            ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/oneBtnPageTitle.xml"));
            ptModel.setValue("button1", getBackButtonLabel(
                "breadcrumbs.directorymanager.peoplecontainer"));
        } else {
            ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
            ptModel.setValue("button1", "button.save");
            ptModel.setValue("button2", "button.reset");
            ptModel.setValue("button3", getBackButtonLabel(
                "breadcrumbs.directorymanager.peoplecontainer"));
        }
    }

    private String getDisplayName() {
        String name =  (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);
        AMModel model = getModel();
        if (name == null || name.length() == 0) {
            name = model.getStartDSDN();
        }
	return AMFormatUtils.DNToName(model, name);
    }

    protected String getBreadCrumbDisplayName() {
	AMModel model = getModel();
	String[] arg = {getDisplayName()};
	return MessageFormat.format(model.getLocalizedString(
	    "breadcrumbs.directorymanager.peoplecontainer.edit"), (Object[])arg);
    }

    protected boolean startPageTrail() {
	return false;
    }   
    
    /**
     * Handles create request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) 
        throws ModelControlException
    {
        model = (PeopleContainerModel)getModel();
        if (model.hasDisplayProperties()) {
            forwardTo();
        } else {
            forwardToPeopleContainerView(event);
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
        forwardToPeopleContainerView(event);
    }

    private void forwardToPeopleContainerView(RequestInvocationEvent event) {   
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
        PeopleContainerViewBean vb = (PeopleContainerViewBean)getViewBean(
            PeopleContainerViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
}
