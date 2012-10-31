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
 * $Id: WebServiceUserCredEditViewBean.java,v 1.3 2008/11/24 21:36:49 farble1670 Exp $
 *
 */

package com.sun.identity.console.agentconfig;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.HiddenField;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.idm.model.EntitiesModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.model.CCPropertySheetModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * View Bean to edit user credential entry.
 */
public class WebServiceUserCredEditViewBean
    extends AMPrimaryMastHeadViewBean
{
    static final String DEFAULT_DISPLAY_URL =
        "/console/agentconfig/WebServiceUserCredEdit.jsp";

    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    static final String HIDDEN_TOKEN = "hiddenToken";
    protected CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;

    /**
     * Creates edit user credential view bean.
     */
    public WebServiceUserCredEditViewBean() {
        super("WebServiceUserCredEdit");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();

        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertyWebServiceAddUserCred.xml"));
        propertySheetModel.clear();
        registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
        ptModel.registerChildren(this);
        registerChild(HIDDEN_TOKEN, HiddenField.class);
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        propertySheetModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;
        
        if (name.equals(PGTITLE_TWO_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(HIDDEN_TOKEN)) {
            view = new HiddenField(this, name, "");
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

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.cancel");
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        backTrail();
        WebServiceProviderEditViewBean vb = (WebServiceProviderEditViewBean)
            getViewBean(WebServiceProviderEditViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
    /**
     * Handles add user token request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        CCPropertySheetModel model = (CCPropertySheetModel)ps.getModel();
        String username = (String)model.getValue("username");
        String password = (String)model.getValue("password");
        
        if ((username == null) || (username.trim().length() == 0) ||
            (password == null) || (password.trim().length() == 0)) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
              "breadcrumbs.add-web-service-usercred-mandate-username-password");
            forwardTo();
        } else {
            try {
                String original = (String)getDisplayFieldValue(HIDDEN_TOKEN);
                String[] orig = WebServiceEditViewBean.splitUserCredToken(
                    original);

                Map map = (Map)getPageSessionAttribute(
                    WebServiceEditViewBean.TRACKER_ATTR);
                if (username.equals(orig[0])) {
                    WebServiceEditViewBean.replaceUserCredTokenAttr(
                        username, password, map);
                } else {
                    WebServiceEditViewBean.addToUserCredTokenAttr(
                        username, password, map, getModel());
                    WebServiceEditViewBean.removeUserCredTokenAttr(
                        username, map);
                }
                setPageSessionAttribute(WebServiceEditViewBean.TRACKER_ATTR,
                    (Serializable)map);
                setPageSessionAttribute(AgentProfileViewBean.MODIFIED_PROFILE, "true");                
                backTrail();
                WebServiceProviderEditViewBean vb =
                    (WebServiceProviderEditViewBean)
                    getViewBean(WebServiceProviderEditViewBean.class);
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
        }
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.edit-web-service-usercred";
    }

    protected boolean startPageTrail() {
        return false;
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new AMModelBase(req, getPageSessionAttributes());
    }

}
