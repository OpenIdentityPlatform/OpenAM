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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 * Portions Copyrighted 2025 3A Systems LLC.
 */
package com.sun.identity.console.sts;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMServiceProfileModel;
import com.sun.identity.console.sts.model.RestSTSInstanceModel;
import com.sun.identity.console.sts.model.STSInstanceModel;
import com.sun.identity.console.sts.model.STSInstanceModelResponse;
import com.sun.identity.console.sts.model.SoapSTSInstanceModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

import static com.sun.identity.console.sts.model.STSInstanceModel.STSType;


/**
 * The ViewBean used to create new Rest STS instances. Extends the AMServiceProfileViewBeanBase class as this class
 * provides for automatic constitution of propertySheet values based on model state.
 */
public class STSAddViewBeanBase extends AMServiceProfileViewBeanBase {
    public static final String PAGE_MODIFIED = "pageModified";
    private final STSType stsType;

    public STSAddViewBeanBase(String viewBeanName, String defaultDisplayUrl, String serviceName,
                              STSType stsType) {
        super(viewBeanName, defaultDisplayUrl, serviceName);
        this.stsType = stsType;
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
        ptModel = new CCPageTitleModel(getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.create");
        ptModel.setValue("button2", "button.cancel");
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        try {
            if (stsType.isRestSTS()) {
                return new RestSTSInstanceModel(req, getPageSessionAttributes());
            } else {
                return new SoapSTSInstanceModel(req, getPageSessionAttributes());
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", e.getMessage());
            throw new IllegalStateException("Exception getting model in STSAddViewBeanBase: " + e.getMessage(), e);
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
            STSInstanceModel model = (STSInstanceModel) getModel();

            STSInstanceModelResponse validationResponse = model.validateConfigurationState(stsType, configurationState);
            if (validationResponse.isSuccessful()) {
                final String currentRealm = (String) getPageSessionAttribute(AMAdminConstants.CURRENT_REALM);
                try {
                    STSInstanceModelResponse creationResponse = model.createInstance(stsType, configurationState, currentRealm);
                    if (creationResponse.isSuccessful()) {
                        setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information", creationResponse.getMessage());
                        forwardToAMViewBean();
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

    /**
     * Handles cancel button request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) throws ModelControlException {
        try {
            forwardToAMViewBean();
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", e.getMessage());
        }
    }

    private void forwardToAMViewBean() throws AMConsoleException {
        removePageSessionAttribute(PAGE_MODIFIED);
        AMViewBeanBase vb = getPreviousPage();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
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
            return (AMViewBeanBase) getViewBean(STSHomeViewBean.class);
        }
    }
}
