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
 * $Id: MAPCreateDeviceTwoViewBean.java,v 1.2 2008/06/25 05:43:15 qcheng Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.DisplayField;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMAdminUtils;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class MAPCreateDeviceTwoViewBean
    extends AMViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/MAPCreateDeviceTwo.jsp";

    private static final String SEC_MH_COMMON = "secMhCommon";
    private static final String PGTITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    static final String TF_PARENTID = "tfParentId";
    static final String TF_CLIENT_TYPE = "tfClientType";

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;
    private boolean submitCycle = false;
    String attrParentId;
    String attrClientType;

    public MAPCreateDeviceTwoViewBean() {
        super("MAPCreateDeviceTwo");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {
            initialized = createPropertyModel();
            if (initialized) {
                super.initialize();
                createPageTitleModel();
                registerChildren();
            }
        }
    }

    /**
     * Registers user interface components used by this view bean.
     */
    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE, CCPageTitle.class);
        registerChild(TF_PARENTID, CCTextField.class);
        registerChild(TF_CLIENT_TYPE, CCTextField.class);
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

        if (name.equals(TF_PARENTID) || name.equals(TF_CLIENT_TYPE)) {
            view = new CCTextField(this, name, "");
        } else if (name.equals(SEC_MH_COMMON)) {
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
        ptModel.setValue("button2", "button.finish");
        ptModel.setValue("button3", "button.cancel");
    }

    private boolean createPropertyModel() {
        boolean init = false;
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();

        if ((attrParentId == null) || (attrParentId.trim().length() == 0)) {
            attrParentId = req.getParameter(
                "MAPCreateDeviceTwo." + TF_PARENTID);
        }

        if ((attrClientType == null) || (attrClientType.trim().length() == 0)) {
            attrClientType = req.getParameter(
                "MAPCreateDeviceTwo." + TF_CLIENT_TYPE);
        }

        init = (attrParentId != null) && (attrParentId.trim().length() > 0) &&
            (attrClientType != null) && (attrClientType.trim().length() > 0);

        if (init) {
            MAPCreateDeviceModel model = (MAPCreateDeviceModel)getModel();
            try {
                propertySheetModel = new AMPropertySheetModel(
                    model.getCreateDevicePropertyXML(attrClientType,
                        attrParentId));
                propertySheetModel.clear();
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
        }
        return init;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);

        if (!submitCycle) {
            MAPCreateDeviceModel model = (MAPCreateDeviceModel)getModel();
            Map defaultValues = model.getCreateDeviceDefaultValues();

            for (Iterator i = defaultValues.keySet().iterator(); i.hasNext(); ){
                String attrName = (String)i.next();
                DisplayField f = (DisplayField)getChild(attrName);
                f.setValues(((Set)defaultValues.get(attrName)).toArray());
            }

            setDisplayFieldValue(TF_PARENTID, attrParentId);
            setDisplayFieldValue(TF_CLIENT_TYPE, attrClientType);
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

    public void handleButton1Request(RequestInvocationEvent event) {
        MAPCreateDeviceViewBean vb = (MAPCreateDeviceViewBean)getViewBean(
            MAPCreateDeviceViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles create device request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        submitCycle = true;
        MAPCreateDeviceModel model = (MAPCreateDeviceModel)getModel();
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);

        try {
            Map values = ps.getAttributeValues(
                model.getCreateDeviceAttributeNames());
            Set setParentId = new HashSet(2);
            setParentId.add(attrParentId);
            values.put("parentId", setParentId);

            model.createDevice(values);
            MAPDeviceProfileViewBean vb = (MAPDeviceProfileViewBean)getViewBean(
                MAPDeviceProfileViewBean.class);
            vb.deviceName = (String)AMAdminUtils.getValue((Set)values.get(
                MAPCreateDeviceModel.ATTRIBUTE_NAME_CLIENT_TYPE));
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }
}
