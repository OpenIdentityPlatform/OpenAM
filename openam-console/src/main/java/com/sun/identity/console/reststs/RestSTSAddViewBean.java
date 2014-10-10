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
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
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
import com.sun.web.ui.view.alert.CCAlert;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

/**
 * The ViewBean used to create new Rest STS instances. Extends the AMServiceProfileViewBeanBase class as this class
 * provides for automatic constitution of propertySheet values based on model state.
 */
public class RestSTSAddViewBean extends AMServiceProfileViewBeanBase {

    public static final String DEFAULT_DISPLAY_URL =
            "/console/reststs/RestSTSAdd.jsp";

    public static final String PAGE_MODIFIED = "pageModified";

    public RestSTSAddViewBean() {
        super("RestSTSAdd", DEFAULT_DISPLAY_URL, AMAdminConstants.REST_STS_SERVICE);
    }

    protected void initialize() {
        super.initialize();
        createPropertyModel();
        createPageTitleModel();
        registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
    }

    protected void createPageTitleModel() {
        createThreeButtonPageTitleModel();
    }

    protected void createPropertyModel() {
        String xmlFileName = "com/sun/identity/console/propertyRestSecurityTokenService.xml";
        String xml = AMAdminUtils.getStringFromInputStream(
                getClass().getClassLoader().getResourceAsStream(xmlFileName));

        propertySheetModel = new AMPropertySheetModel(xml);
        propertySheetModel.clear();
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
        try {
            Map<String, Set<String>> configurationState = (Map<String, Set<String>>) getAttributeSettings();
            RestSTSModel model = (RestSTSModel) getModel();
            RestSTSModelResponse validationResponse = model.validateConfigurationState(configurationState);
            if (validationResponse.isSuccessful()) {
                final String currentRealm = (String) getPageSessionAttribute(AMAdminConstants.CURRENT_REALM);
                try {
                    RestSTSModelResponse creationResponse = model.createInstance(configurationState, currentRealm);
                    if (creationResponse.isSuccessful()) {
                        setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information", creationResponse.getMessage());
                        disableSaveAndResetButtons();
                    } else {
                        setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", creationResponse.getMessage());
                    }
                } catch (AMConsoleException e) {
                    throw new ModelControlException(e);
                }
            } else {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", validationResponse.getMessage());
            }
        } catch (AMConsoleException e) {
            //will be entered if getAttributeSettings throws a AMConsoleException because passwords are mis-matched.
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", e.getMessage());
        }
        forwardTo();
    }

    protected void disableSaveAndResetButtons() {
        disableButton("button1", true);
        disableButton("button2", true);
    }

    /*
    Returns a map of all settings, including those not changed from the default values in the model.
    AMConsoleException will be thrown if passwords are mis-matched.
     */
    private Map getAttributeSettings() throws ModelControlException, AMConsoleException {
        Map values = null;
        AMServiceProfileModel model = (AMServiceProfileModel)getModel();

        if (model != null) {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            values = ps.getAttributeValues(model.getAttributeValues(), false, model);
        }
        return values;
    }

    protected String getBackButtonLabel() {
        return "button.back";
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     *
     */
    public void handleButton3Request(RequestInvocationEvent event)
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
