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
 * $Id: SiteEditViewBean.java,v 1.2 2008/06/25 05:43:17 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.service.model.ServerSiteModel;
import com.sun.identity.console.service.model.ServerSiteModelImpl;
import com.sun.web.ui.model.CCEditableListModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.editablelist.CCEditableList;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;

/**
 * Creates a new site.
 */
public class SiteEditViewBean
    extends AMPrimaryMastHeadViewBean
{
    static final String PG_ATTR_SITE_NAME = "pgAttrSiteName";
    private static final String DEFAULT_DISPLAY_URL =
        "/console/service/SiteEdit.jsp";
    private static final String TF_URL = "tfURL";
    private static final String EDITABLE_FAILOVER_URLS = "eListFailoverURLs";
    private static final String TF_SERVERS = "tfServers";
    private static final String PGTITLE_THREE_BTNS = "pgtitleThreeBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;
    private boolean submitCycle;

    /**
     * Creates a site creation view bean.
     */
    public SiteEditViewBean() {
        super("SiteEdit");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
        ptModel.registerChildren(this);
        registerChild(PGTITLE_THREE_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        propertySheetModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE_THREE_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new ServerSiteModelImpl(req, getPageSessionAttributes());
    }

    /**
     * Displays the profile of a site.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        String siteName = (String)getPageSessionAttribute(
            PG_ATTR_SITE_NAME);
        ServerSiteModel model = (ServerSiteModel)getModel();
        ptModel.setPageTitleText(model.getEditSitePageTitle(siteName));
        try {
            setDisplayFieldValue(TF_URL, model.getSitePrimaryURL(siteName));
            getFailoverURLs(siteName, model);
            getServers(siteName, model);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

    }
    
    private void getFailoverURLs(String siteName, ServerSiteModel model)
        throws AMConsoleException {
        if (!submitCycle) {
            Set failoverURLs = model.getSiteFailoverURLs(siteName);
            CCEditableList elist = (CCEditableList)getChild(
                EDITABLE_FAILOVER_URLS);
            CCEditableListModel m = (CCEditableListModel)elist.getModel();
            elist.resetStateData();
            m.setOptionList(failoverURLs);
        }
    }

    private void getServers(String siteName, ServerSiteModel model)
        throws AMConsoleException {
        Set assigned = model.getSiteServers(siteName);
        
        if ((assigned != null) && !assigned.isEmpty()) {
            Set set = new TreeSet();
            set.addAll(assigned);
            StringBuilder buff = new StringBuilder();
            for (Iterator i = set.iterator(); i.hasNext(); ) {
                buff.append((String)i.next())
                    .append("<br />");
            }
            setDisplayFieldValue(TF_SERVERS, buff.toString());
        } else {
            setDisplayFieldValue(TF_SERVERS, model.getLocalizedString(
                "serverconfig.site.attribute.no.servers"));
        }
    }
    
    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }

    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/propertySiteEdit.xml"));
        propertySheetModel.clear();
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }

    /**
     * Handles create site request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        submitCycle = true;
        String siteName = (String)getPageSessionAttribute(
            PG_ATTR_SITE_NAME);
        String primaryURL = (String)getDisplayFieldValue(TF_URL);
        
        CCEditableList elist = (CCEditableList)getChild(
            EDITABLE_FAILOVER_URLS);
        elist.restoreStateData();
        Set failoverURLs = getValues(elist.getModel().getOptionList());
        
        ServerSiteModel model = (ServerSiteModel)getModel();
        
        try {
            model.modifySite(siteName, primaryURL, failoverURLs);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "siteconfig.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        forwardTo();
    }

    /**
     * Handles return to home page request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event)
        throws ModelControlException {
        returnToHomePage();
    }
    
    private void returnToHomePage() {
        backTrail();
        ServerSiteViewBean vb = (ServerSiteViewBean)getViewBean(
            ServerSiteViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.addsite";
    }

    protected boolean startPageTrail() {
        return false;
    }
    
    protected String getBackButtonLabel() {
        return getBackButtonLabel("page.title.serversite.config");
    }
}
