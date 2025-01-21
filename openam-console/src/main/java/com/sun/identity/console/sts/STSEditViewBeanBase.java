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
 * Copyright 2014-2016 ForgeRock AS. All rights reserved.
 */

/*
 * Portions Copyrighted 2015-2016 Nomura Research Institute, Ltd.
 * Portions Copyrighted 2025 3A Systems LLC.
 */
package com.sun.identity.console.sts;

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
import com.sun.identity.console.sts.model.RestSTSInstanceModel;
import com.sun.identity.console.sts.model.STSInstanceModel;
import com.sun.identity.console.sts.model.STSInstanceModelResponse;
import com.sun.identity.console.sts.model.SoapSTSInstanceModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import org.forgerock.openam.shared.sts.SharedSTSConstants;
import org.forgerock.openam.utils.CollectionUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

import static com.sun.identity.console.sts.model.STSInstanceModel.STSType;

/**
 * Base class ViewBean invoked to edit an existing STS instance. The AMServiceProfileViewBeanBase class is not extended here
 * as for the RestSTSAddViewBean because the generic propertySheet constitution logic in the AMServiceProfileViewBeanBase
 * class does not work for SubConfig state, which is where STS instance state is stored.
 */
class STSEditViewBeanBase extends AMPrimaryMastHeadViewBean {

    private static final String REST_DEFAULT_DISPLAY_URL = "/console/sts/RestSTSEdit.jsp";
    private static final String SOAP_DEFAULT_DISPLAY_URL = "/console/sts/SoapSTSEdit.jsp";

    private static final String REST_PROPERTY_MODEL_DEFINITION_FILE = "com/sun/identity/console/propertyRestSecurityTokenService.xml";
    private static final String SOAP_PROPERTY_MODEL_DEFINITION_FILE = "com/sun/identity/console/propertySoapSecurityTokenService.xml";

    private static final String PAGE_MODIFIED = "pageModified";
    private static final String PGTITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    protected CCPageTitleModel ptModel;
    protected boolean submitCycle;
    protected AMPropertySheetModel propertySheetModel;
    protected STSInstanceModel.STSType stsType;

    protected STSEditViewBeanBase(String viewBeanName, STSType stsType) {
        super(viewBeanName);
        this.stsType = stsType;
        if (stsType.isRestSTS()) {
            setDefaultDisplayURL(REST_DEFAULT_DISPLAY_URL);
        } else {
            setDefaultDisplayURL(SOAP_DEFAULT_DISPLAY_URL);
        }
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
        ptModel = new CCPageTitleModel(getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", "button.back");
    }

    protected void createPropertyModel() {
        String xmlFileName;
        if (stsType.isRestSTS()) {
            xmlFileName = REST_PROPERTY_MODEL_DEFINITION_FILE;
        } else {
            xmlFileName = SOAP_PROPERTY_MODEL_DEFINITION_FILE;
        }
        String xml = AMAdminUtils.getStringFromInputStream(
                getClass().getClassLoader().getResourceAsStream(xmlFileName));

        propertySheetModel = new AMPropertySheetModel(xml);
        propertySheetModel.clear();
    }

    public void beginDisplay(DisplayEvent event) throws ModelControlException {
        super.beginDisplay(event);
        final String instanceName = (String)getPageSessionAttribute(STSHomeViewBean.INSTANCE_NAME);
        final String currentRealm = (String)getPageSessionAttribute(AMAdminConstants.CURRENT_REALM);
        if (!submitCycle) {
            STSInstanceModel model = (STSInstanceModel)getModel();
            Map map;
            try {
                map = model.getInstanceState(stsType, currentRealm, instanceName);
            } catch (AMConsoleException e) {
                throw new ModelControlException(e);
            }
            if (!map.isEmpty()) {
                AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
                propertySheetModel.clear();
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
            if (stsType.isRestSTS()) {
                return new RestSTSInstanceModel(req, getPageSessionAttributes());
            } else {
                return new SoapSTSInstanceModel(req, getPageSessionAttributes());
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", e.getMessage());
            throw new IllegalStateException("Exception getting model in STSEditViewBeanBase: " + e.getMessage(), e);
        }
    }

    /**
     * Handles save button request. Validates the rest sts configuration state, and invokes the model to publish a
     * rest sts instance corresponding to this state.
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) throws ModelControlException {
        submitCycle = true;
        final String currentRealm = (String)getPageSessionAttribute(AMAdminConstants.CURRENT_REALM);
        final String instanceName = (String)getPageSessionAttribute(STSHomeViewBean.INSTANCE_NAME);
        try {
            Map<String, Set<String>> configurationState = getUpdatedConfigurationState(currentRealm, instanceName);
            if (configurationState.isEmpty()) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information", getModel().getLocalizedString("rest.sts.view.no.updates"));
            } else {
                if (instanceNameUpdated(currentRealm, instanceName)) {
                    setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                            MessageFormat.format(getModel().getLocalizedString("rest.sts.view.no.edit.deployment.url"), instanceName));
                } else {
                    STSInstanceModel model = (STSInstanceModel) getModel();
                    STSInstanceModelResponse validationResponse = model.validateConfigurationState(stsType, configurationState);
                    if (validationResponse.isSuccessful()) {
                        try {
                            STSInstanceModelResponse creationResponse = model.updateInstance(stsType, configurationState, currentRealm, instanceName);
                            if (creationResponse.isSuccessful()) {
                                forwardToSTSHomeViewBean();
                                return;
                            } else {
                                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", creationResponse.getMessage());
                            }
                        } catch (AMConsoleException e) {
                            throw new ModelControlException(e);
                        }
                    } else {
                        setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", validationResponse.getMessage());
                    }
                }
            }
        } catch (AMConsoleException e) {
            //getUpdatedConfigurationState will throw this exception if passwords are mis-matched.
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", e.getMessage());
        }
        forwardTo();
    }

    private void forwardToSTSHomeViewBean() throws AMConsoleException {
        removePageSessionAttribute(PAGE_MODIFIED);
        AMViewBeanBase vb = getPreviousPage();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles reset button request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) throws ModelControlException {
        propertySheetModel.clear();
        forwardTo();
    }

    /**
     * Handles back button request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) throws ModelControlException, AMConsoleException {
        removePageSessionAttribute(PAGE_MODIFIED);
        AMViewBeanBase vb = getPreviousPage();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected void disableSaveButton() {
        disableButton("button1", true);
    }

    /**
     * Called to harvest the full set of updated configuration properties.
     * @param realm
     * @param instanceName
     * @return The set up updated properties. An empty-set will be returned if no properties updated.
     * @throws ModelControlException thrown by AMPropertySheet#getAttributeValues if model for property-sheet cannot be
     * obtained
     * @throws AMConsoleException thrown by AMPropertySheet#getAttributeValues if passwords are mis-matched.
     */
    private Map<String, Set<String>> getUpdatedConfigurationState(String realm, String instanceName) throws ModelControlException, AMConsoleException {
        STSInstanceModel model = (STSInstanceModel)getModel();
        Map<String, Set<String>> currentPersistedInstanceState;
        try {
            currentPersistedInstanceState = model.getInstanceState(stsType, realm, instanceName);
        } catch (AMConsoleException e) {
            throw new ModelControlException(e.getMessage(), e);
        }
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        Map<String, Set<String>> updatedValues = ps.getAttributeValues(currentPersistedInstanceState, model);
        if (updatedValues.isEmpty()) {
            return updatedValues;
        } else {
            currentPersistedInstanceState.putAll(updatedValues);
            return currentPersistedInstanceState;
        }
    }

    /*
    The deploymentUrl of an existing sts instance cannot be edited, as it constitutes (along with the realm) the dn of the
    rest-sts instance state. This method returns true if the changes include the deployment url.
     */
    private boolean instanceNameUpdated(String realm, String instanceName) throws ModelControlException, AMConsoleException {
        STSInstanceModel model = (STSInstanceModel)getModel();
        Map<String, Set<String>> currentPersistedInstanceState;
        try {
            currentPersistedInstanceState = model.getInstanceState(stsType, realm, instanceName);
        } catch (AMConsoleException e) {
            throw new ModelControlException(e.getMessage(), e);
        }
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        Map<String, Set<String>> updatedValues = ps.getAttributeValues(currentPersistedInstanceState, model);
        return !CollectionUtils.isEmpty(updatedValues.get(SharedSTSConstants.DEPLOYMENT_URL_ELEMENT));
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
