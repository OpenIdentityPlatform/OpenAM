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
 * $Id: MAPCreateDeviceViewBean.java,v 1.2 2008/06/25 05:43:15 qcheng Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.service.model.MAPCreateDeviceModel;
import com.sun.identity.console.service.model.MAPCreateDeviceModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCRadioButton;
import com.sun.web.ui.view.masthead.CCSecondaryMasthead;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Iterator;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class MAPCreateDeviceViewBean
    extends AMViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/MAPCreateDevice.jsp";

    private static final String SEC_MH_COMMON = "secMhCommon";
    private static final String PGTITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String RADIO_STYLE = "radioStyle";

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;

    public MAPCreateDeviceViewBean() {
        super("MAPCreateDevice");
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
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.back");
        ptModel.setValue("button2", "button.next");
        ptModel.setValue("button3", "button.cancel");
    }

    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertyMAPCreateDevice1.xml"));
        propertySheetModel.clear();
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        disableButton("button1", true);
        MAPCreateDeviceModel model = (MAPCreateDeviceModel)getModel();
        String profileName = (String)getPageSessionAttribute(
            MAPClientManagerViewBean.PAGE_SESSION_PROFILE_NAME);
        Set styles = model.getStyleNames(profileName);

        if ((styles != null) && !styles.isEmpty()) {
            CCRadioButton btn = (CCRadioButton)getChild(RADIO_STYLE);
            OptionList styleList = new OptionList();
            for (Iterator iter = styles.iterator(); iter.hasNext(); ) {
                String name = (String)iter.next();
                styleList.add(name, name);
            }
            btn.setOptions(styleList);

            String value = (String)getDisplayFieldValue(RADIO_STYLE);
            if ((value == null) || (value.trim().length() == 0)) {
                setDisplayFieldValue(RADIO_STYLE,
                    (String)styles.iterator().next());
            }
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
    public void handleButton3Request(RequestInvocationEvent event) {
        MAPClientManagerViewBean vb = (MAPClientManagerViewBean)getViewBean(
            MAPClientManagerViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles create device request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        String clientType = (String)propertySheetModel.getValue(
            "deviceUserAgent");

        if (clientType.trim().length() == 0) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "mapMissingDeviceUserAgent.message");
            forwardTo();
        } else {
            MAPCreateDeviceTwoViewBean vb = (MAPCreateDeviceTwoViewBean)
                getViewBean(MAPCreateDeviceTwoViewBean.class);
            vb.attrParentId = (String)propertySheetModel.getValue("radioStyle");
            vb.attrClientType = clientType;
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        }
    }
}
