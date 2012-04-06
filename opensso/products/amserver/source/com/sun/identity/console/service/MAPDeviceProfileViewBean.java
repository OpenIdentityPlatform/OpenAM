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
 * $Id: MAPDeviceProfileViewBean.java,v 1.2 2008/06/25 05:43:15 qcheng Exp $
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
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.service.model.MAPDeviceProfileModel;
import com.sun.identity.console.service.model.MAPDeviceProfileModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.masthead.CCSecondaryMasthead;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class MAPDeviceProfileViewBean
    extends AMViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/MAPDeviceProfile.jsp";

    private static final String SEC_MH_COMMON = "secMhCommon";
    private static final String PGTITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String SINGLECHOICE_CLASSIFICATION =
        "singleChoiceClassification";
    private static final String BTN_CLASSIFICATION = "btnClassification";
    private static final String TF_CLASSIFICATION = "tfClassification";
    static final String TF_DEVICE_NAME = "tfDeviceName";

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;
    private boolean submitCycle = false;
    String deviceName;

    public MAPDeviceProfileViewBean() {
        super("MAPDeviceProfile");
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
        registerChild(SEC_MH_COMMON, CCSecondaryMasthead.class);
        registerChild(TF_DEVICE_NAME, CCTextField.class);
        registerChild(TF_CLASSIFICATION, CCTextField.class);
        registerChild(SINGLECHOICE_CLASSIFICATION, CCDropDownMenu.class);
        registerChild(BTN_CLASSIFICATION, CCButton.class);
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
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", "button.cancel");
    }

    private boolean createPropertyModel() {
        HttpServletRequest req =RequestManager.getRequestContext().getRequest();

        if ((deviceName == null) || (deviceName.trim().length() == 0)) {
            deviceName = req.getParameter("MAPDeviceProfile." + TF_DEVICE_NAME);
        }

        boolean init = (deviceName != null) && (deviceName.trim().length() > 0);

        if (init) {
            String classification = req.getParameter(
                "MAPDeviceProfile." + TF_CLASSIFICATION);
            if ((classification == null) ||
                (classification.trim().length() == 0)
            ) {
                classification = MAPDeviceProfileModel.DEFAULT_CLASSIFICATION;
            }

            createPropertyModel(deviceName, classification);
        }

        return init;
    }

    private void createPropertyModel(String deviceName, String classification) {
        MAPDeviceProfileModel model = (MAPDeviceProfileModel)getModel();
        try {
            propertySheetModel = new AMPropertySheetModel(
                model.getProfilePropertyXML(deviceName, classification));
            propertySheetModel.clear();
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        setDisplayFieldValue(TF_DEVICE_NAME, deviceName);
        ptModel.setPageTitleText(deviceName);
        initializedClassificationDropDownList();
        MAPDeviceProfileModel model = (MAPDeviceProfileModel)getModel();
        Map attributeValues = model.getAttributeValues(deviceName,
            (String)getDisplayFieldValue(TF_CLASSIFICATION));
        Set attributeNames = (!submitCycle) ? attributeValues.keySet() :
            model.getReadOnlyAttributeNames(
                deviceName, attributeValues.keySet());

        for (Iterator iter = attributeNames.iterator(); iter.hasNext();){
            String name = (String)iter.next();
            propertySheetModel.setValues(name,
                ((Set)attributeValues.get(name)).toArray(), model);
        }
    }

    private void initializedClassificationDropDownList() {
        MAPDeviceProfileModel model = (MAPDeviceProfileModel)getModel();
        String[] classification = model.getAttributeClassification(deviceName);

        if (classification != null) {
            Map localizedLabels = model.getLocalizedClassificationLabels(
                classification);
            OptionList optList = new OptionList();

            for (int i = 0; i < classification.length; i++) {
                String c = classification[i];
                String label = (String)localizedLabels.get(c);

                if ((label == null) || (label.trim().length() == 0)) {
                    label = c;
                }

                optList.add(label, c);
            }

            CCDropDownMenu cb = (CCDropDownMenu)getChild(
                SINGLECHOICE_CLASSIFICATION);
            cb.setOptions(optList);

            String value = (String)cb.getValue();
            if ((value == null) || (value.length() == 0)) {
                cb.setValue(classification[0]);
                setDisplayFieldValue(TF_CLASSIFICATION, classification[0]);
            } else {
                setDisplayFieldValue(TF_CLASSIFICATION, value);
            }
        }
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new MAPDeviceProfileModelImpl(req, getPageSessionAttributes());
    }

    /**
     * Handles reset request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        propertySheetModel.clear();
        forwardTo();
    }

    /**
     * Handles cancel request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        returnToMainPage();
    }

    private void returnToMainPage() {
        MAPClientManagerViewBean vb = (MAPClientManagerViewBean)getViewBean(
            MAPClientManagerViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles change classification request.
     *
     * @param event Request Invocation Event.
     */
    public void handleBtnClassificationRequest(RequestInvocationEvent event) {
        setDisplayFieldValue(SINGLECHOICE_CLASSIFICATION,
            getDisplayFieldValue(TF_CLASSIFICATION));
        forwardTo();
    }

    /**
     * Handles create device request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        submitCycle = true;
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        MAPDeviceProfileModel model = (MAPDeviceProfileModel)getModel();
        deviceName = (String)getDisplayFieldValue(TF_DEVICE_NAME);
        Map orig = model.getAttributeValues(deviceName,
            (String)getDisplayFieldValue(TF_CLASSIFICATION));

        try {
            Map values = ps.getAttributeValues(orig, true, model);
            model.modifyProfile(deviceName, values);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "message.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        forwardTo();
    }
}
