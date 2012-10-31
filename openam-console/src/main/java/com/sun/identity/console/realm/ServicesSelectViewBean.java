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
 * $Id: ServicesSelectViewBean.java,v 1.3 2008/07/07 20:39:20 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.realm;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.realm.model.ServicesModel;
import com.sun.identity.console.realm.model.ServicesModelImpl;
import com.sun.identity.console.service.model.SCUtils;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCRadioButton;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServicesSelectViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/realm/ServicesSelect.jsp";
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String ATTR_SERVICE_LIST = "cbServiceList";

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;

    /**
     * Creates a view to prompt user for services to be added to realm.
     */
    public ServicesSelectViewBean() {
        super("ServicesSelect");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
    }

    protected void registerChildren() {
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        super.registerChildren();
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

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.back");
        ptModel.setValue("button2", "button.next");
        ptModel.setValue("button3", "button.cancel");
    }

    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertyRMServicesSelect.xml"));
        propertySheetModel.clear();
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new ServicesModelImpl(req, getPageSessionAttributes());
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        disableButton("button1", true);

        try {
            ServicesModel model = (ServicesModel)getModel();
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            Map assignables = model.getAssignableServiceNames(curRealm);
            CCRadioButton rb = (CCRadioButton)getChild(ATTR_SERVICE_LIST);
            OptionList optList = AMFormatUtils.getSortedOptionList(
                assignables, model.getUserLocale());
            rb.setOptions(optList);

            String val = (String)rb.getValue();
            if ((val == null) || (val.length() == 0)) {
                rb.setValue(optList.getValue(0));
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        ServicesViewBean vb = (ServicesViewBean)getViewBean(
            ServicesViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles next button request.
     *
     * @param event Request invocation event.
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException {
        ServicesModel model = (ServicesModel)getModel();
        String serviceName = (String)getDisplayFieldValue(ATTR_SERVICE_LIST);
        serviceName = serviceName.trim();

        if (serviceName.length() > 0) {
            SCUtils utils = new SCUtils(serviceName, model);
            String propertiesViewBeanURL = utils.getServiceDisplayURL();

            if ((propertiesViewBeanURL != null) &&
                (propertiesViewBeanURL.trim().length() > 0)
            ) {
                String curRealm = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_REALM);
                if (curRealm == null) {
                    curRealm = AMModelBase.getStartDN(
                        getRequestContext().getRequest());
                }
                try {
                    String pageTrailID = (String)getPageSessionAttribute(
                        PG_SESSION_PAGE_TRAIL_ID);
                    propertiesViewBeanURL += "?ServiceName=" + serviceName +
                        "&Location=" +
                        stringToHex(curRealm) +
                        "&Template=true&Op=" + AMAdminConstants.OPERATION_ADD +
                        "&" + PG_SESSION_PAGE_TRAIL_ID + "=" + pageTrailID;
                    HttpServletResponse response =
                        getRequestContext().getResponse();
                    backTrail();
                    response.sendRedirect(propertiesViewBeanURL);
                } catch (UnsupportedEncodingException e) {
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        e.getMessage());
                    forwardTo();
                } catch (IOException e) {
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        e.getMessage());
                    forwardTo();
                }
            } else {
                ServicesAddViewBean vb = (ServicesAddViewBean)getViewBean(
                    ServicesAddViewBean.class);
                setPageSessionAttribute(ServicesAddViewBean.SERVICE_NAME,
                    serviceName);
                unlockPageTrailForSwapping();
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            }
        } else {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "services.missing.servicename");
            forwardTo();
        }
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.realm.services.selectService";
    }

    protected boolean startPageTrail() {
        return false;
    }
}
