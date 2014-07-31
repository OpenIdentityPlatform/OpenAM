/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package com.sun.identity.console.reststs;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMServiceProfileModel;
import com.sun.identity.console.reststs.model.RestSTSModel;
import com.sun.identity.console.reststs.model.RestSTSModelImpl;
import com.sun.identity.console.reststs.model.RestSTSModelResponse;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

/**
 * ViewBean invoked to edit an existing Rest STS instance. The AMServiceProfileViewBeanBase class is not extended here
 * as for the RestSTSAddViewBean because the generic propertySheet constitution logic in the AMServiceProfileViewBeanBase
 * class does not work for SubConfig state, which is where Rest STS instance state is stored.
 */
public class RestSTSEditViewBean extends AMPrimaryMastHeadViewBean {
    public static final String DEFAULT_DISPLAY_URL = "/console/reststs/RestSTSEdit.jsp";

    private static final String PAGE_MODIFIED = "pageModified";
    private static final String PGTITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    protected CCPageTitleModel ptModel;
    protected boolean submitCycle;
    protected AMPropertySheetModel propertySheetModel;

    public RestSTSEditViewBean() {
        super("RestSTSEdit");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        super.initialize();
        createPropertyModel();
        createPageTitleModel();
        registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
        ptModel.registerChildren(this);
        registerChild(PGTITLE, CCPageTitle.class);

        if (propertySheetModel != null) {
            registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
            propertySheetModel.registerChildren(this);
        }
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if ((propertySheetModel != null) && propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                        "com/sun/identity/console/twoBtnsPageTitle.xml"));

        ptModel.setPageTitleText("rest.sts.edit.page.title");
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.back");
    }

    protected void createPropertyModel() {
        String xmlFileName = "com/sun/identity/console/propertyRestSecurityTokenService.xml";
        String xml = AMAdminUtils.getStringFromInputStream(
                getClass().getClassLoader().getResourceAsStream(xmlFileName));

        propertySheetModel = new AMPropertySheetModel(xml);
        propertySheetModel.clear();
    }

    public void beginDisplay(DisplayEvent event) throws ModelControlException {
        super.beginDisplay(event);
        final String instanceName = (String)getPageSessionAttribute(RestSTSHomeViewBean.INSTANCE_NAME);
        final String currentRealm = (String)getPageSessionAttribute(AMAdminConstants.CURRENT_REALM);
            if (!submitCycle) {
                RestSTSModel model = (RestSTSModel)getModel();
                Map map;
                try {
                    map = model.getInstanceState(currentRealm, instanceName);
                } catch (AMConsoleException e) {
                    throw new ModelControlException(e);
                }
                if (!map.isEmpty()) {
                    AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
                    ps.setAttributeValues(map, getModel());
                } else {
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                            MessageFormat.format(model.getLocalizedString("rest.sts.view.no.instance.message"), instanceName));
                }
            }
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        try {
            return new RestSTSModelImpl(req, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", e.getMessage());
            throw new IllegalStateException("Exception getting model in RestSTSAddViewBean: " + e.getMessage(), e);
        }
    }

    /**
     * Handles save button request. Validates the rest sts configuration state, and invokes the model to publish a
     * rest sts instance corresponding to this state.
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) throws ModelControlException {
        submitCycle = true;
        Map<String, Set<String>> configurationState = getAttributeSettings();
        RestSTSModel model = (RestSTSModel)getModel();
        RestSTSModelResponse validationResponse = model.validateConfigurationState(configurationState);
        if (validationResponse.isSuccessful()) {
            final String currentRealm = (String)getPageSessionAttribute(AMAdminConstants.CURRENT_REALM);
            final String instanceName = (String)getPageSessionAttribute(RestSTSHomeViewBean.INSTANCE_NAME);
            try {
                RestSTSModelResponse creationResponse = model.updateInstance(configurationState, currentRealm, instanceName);
                if (creationResponse.isSuccessful()) {
                    setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information", creationResponse.getMessage());
                } else {
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", creationResponse.getMessage());
                }
            } catch (AMConsoleException e) {
                throw new ModelControlException(e);
            }
        } else {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", validationResponse.getMessage());
        }
        forwardTo();
    }

    /*
    Returns a map of all settings, including those not changed from the default values in the model.
     */
    private Map<String, Set<String>> getAttributeSettings() throws ModelControlException {
        Map<String, Set<String>> values = null;
        AMServiceProfileModel model = (AMServiceProfileModel)getModel();

        if (model != null) {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            try {
                values = ps.getAttributeValues(model.getAttributeValues(), false, model);
            } catch (AMConsoleException e) {
                throw new ModelControlException(e.getMessage(), e);
            }
        }
        return values;
    }

    public void handleButton2Request(RequestInvocationEvent event)
            throws ModelControlException, AMConsoleException {
        removePageSessionAttribute(PAGE_MODIFIED);
        AMViewBeanBase vb = getPreviousPage();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    private AMViewBeanBase getPreviousPage() throws AMConsoleException {
        if (getPageSessionAttribute(AMAdminConstants.SAVE_VB_NAME) != null) {
            String name = (String) getPageSessionAttribute(AMAdminConstants.SAVE_VB_NAME);
            if (name == null) {
                throw new AMConsoleException("No page session attribute corresponding to " + AMAdminConstants.SAVE_VB_NAME);
            }
            try {
                return (AMViewBeanBase) getViewBean(Class.forName(name));
            } catch (ClassNotFoundException e) {
                throw new AMConsoleException("Could not find class corresponding to class name "
                        + name + ". Exception: " + e);
            }
        } else {
            return (AMViewBeanBase) getViewBean(RestSTSHomeViewBean.class);
        }
    }
}
