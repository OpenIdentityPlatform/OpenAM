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
 * $Id: ServerCloneViewBean.java,v 1.2 2008/06/25 05:43:16 qcheng Exp $
 *
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
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.text.MessageFormat;
import javax.servlet.http.HttpServletRequest;

/**
 * Creates a clone server.
 */
public class ServerCloneViewBean
    extends AMPrimaryMastHeadViewBean
{
    static final String PG_ATTR_SERVER_NAME = "pgAttrServerName";

    private static final String DEFAULT_DISPLAY_URL =
        "/console/service/ServerClone.jsp";
    private static final String TF_NAME = "tfName";
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;

    /**
     * Creates a clone server view bean.
     */
    public ServerCloneViewBean() {
        super("ServerClone");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
        ptModel.registerChildren(this);
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        propertySheetModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE_TWO_BTNS)) {
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
     * Sets the title of the page.
     *
     * @param event Display event.
     * @throws ModelControlException if JATO model fails to initiate.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        ServerSiteModel model = (ServerSiteModel)getModel();
        String serverName = (String)getPageSessionAttribute(
            PG_ATTR_SERVER_NAME);
        Object[] param = {serverName};
        ptModel.setPageTitleText(MessageFormat.format(
            model.getLocalizedString("page.title.server.clone"), param));
        
       setDisplayFieldValue("helpName", MessageFormat.format(
           model.getLocalizedString("serverconfig.server.clone.help.name"), 
           param));
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.cancel");
    }

    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertyServerClone.xml"));
        propertySheetModel.clear();
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        backTrail();
        ServerSiteViewBean vb = (ServerSiteViewBean)getViewBean(
            ServerSiteViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles clone server request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        ServerSiteModel model = (ServerSiteModel)getModel();
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        String serverName = (String)getPageSessionAttribute(
            PG_ATTR_SERVER_NAME);
        String name = (String)getDisplayFieldValue(TF_NAME);
        name = name.trim();

        if (name.length() > 0) {
            try {
                model.cloneServer(serverName, name);
                backTrail();
                ServerSiteViewBean vb = (ServerSiteViewBean)getViewBean(
                    ServerSiteViewBean.class);
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                forwardTo();
            }
        } else {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "serverconfig.clone.server.missing.atributes");
            forwardTo();
        }
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.clone.server";
    }

    protected boolean startPageTrail() {
        return false;
    }
}
