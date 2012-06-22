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
 * $Id: MAPDuplicationDeviceViewBean.java,v 1.2 2008/06/25 05:43:15 qcheng Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.service.model.MAPCreateDeviceModel;
import com.sun.identity.console.service.model.MAPCreateDeviceModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.masthead.CCSecondaryMasthead;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import javax.servlet.http.HttpServletRequest;

public class MAPDuplicationDeviceViewBean
    extends AMViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/MAPDuplicationDevice.jsp";

    private static final String SEC_MH_COMMON = "secMhCommon";
    private static final String PGTITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    static final String TF_ORIG_CLIENT_TYPE = "tfOrigClientType";

    String clientType;
    String deviceName;
    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;

    public MAPDuplicationDeviceViewBean() {
        super("MAPDuplicationDevice");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
    }

    /**
     * Registers user interface components used by this view bean.
     */
    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE, CCPageTitle.class);
        registerChild(SEC_MH_COMMON, CCSecondaryMasthead.class);
        registerChild(TF_ORIG_CLIENT_TYPE, CCTextField.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
    }

    /**
     * Creates user interface components used by this view bean.
     *
     * @param name of component
     * @return child component
     */
    protected View createChild(String name) {
        View view = null;

        if (name.equals(SEC_MH_COMMON)) {
            view = new CCSecondaryMasthead(this, name);
        } else if (name.equals(PGTITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.duplicate");
        ptModel.setValue("button2", "button.cancel");
    }

    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertyMAPDuplicationDevice.xml"));
        propertySheetModel.clear();
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        if (clientType != null) {
            propertySheetModel.setValue("tfClientType", clientType);
        }

        if (deviceName != null) {
            propertySheetModel.setValue("tfDeviceUserAgent", deviceName);
        }
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new MAPCreateDeviceModelImpl(req, getPageSessionAttributes());
    }

    /**
     * Handles cancel request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardToClientManagerView();
    }

    /**
     * Handles create device request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton1Request(RequestInvocationEvent event) {
        String origClientType = (String)getDisplayFieldValue(
            TF_ORIG_CLIENT_TYPE);
        String clientType = (String)propertySheetModel.getValue(
            "tfClientType");
        String deviceName = (String)propertySheetModel.getValue(
            "tfDeviceUserAgent");

        if (clientType.trim().length() == 0) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "mapMissingClientType.message");
            forwardTo();
        } else if (deviceName.trim().length() == 0) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "mapMissingDeviceUserAgent.message");
            forwardTo();
        } else {
            try {
                MAPCreateDeviceModel model = (MAPCreateDeviceModel)getModel();
                model.cloneDevice(origClientType, clientType, deviceName);
                forwardToClientManagerView();
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                forwardTo();
            }
        }
    }

    private void forwardToClientManagerView() {
        MAPClientManagerViewBean vb = (MAPClientManagerViewBean)getViewBean(
            MAPClientManagerViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
}
