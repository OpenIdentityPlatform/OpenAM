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
 * $Id: EditAuthTypeViewBean.java,v 1.2 2008/06/25 05:42:45 qcheng Exp $
 *
 */

/**
 * Portions Copyright 2014 ForgeRock AS
 */

package com.sun.identity.console.authentication;

import com.iplanet.jato.NavigationException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.authentication.model.AuthPropertiesModel;
import com.sun.identity.console.authentication.model.AuthPropertiesModelImpl;
import com.sun.identity.console.authentication.model.AuthProfileModelImpl;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.sm.DynamicAttributeValidator;
import com.sun.web.ui.view.alert.CCAlert;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class EditAuthTypeViewBean
    extends AMServiceProfileViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL = "/console/authentication/EditAuthType.jsp";
    public static final String SERVICE_TYPE = "authServiceType";

    private static final String DYNAMIC_VALIDATION = "dynamic_validation";
    private static final String ATTRIBUTE_NAME = "attrname";
    private static final String HTML_BREAK = "<br>";

    private AuthPropertiesModel authModel = null;
    private boolean dynamicRequest = false;
    private Map<String, Set<String>> unpersistedValueMap;

    /**
     * Creates a authentication module edit view bean.
     */
    public EditAuthTypeViewBean() {
        super("EditAuthType", DEFAULT_DISPLAY_URL, null);

        String type = (String)getPageSessionAttribute(SERVICE_TYPE);
        if (type != null) {

            /* 
             * The name of the service will return null if the entry no
             * longer exists. This can happen if someone is viewing the
             * properties of a service, and another person deletes the
             * entry. Any request on that page will result in an error
             * without a valid service name. To avoid the error we set the
             * service name to the default core authentication service.
             */
            String name = getServiceName(type);
            if (name == null) {
                name = AMAdminConstants.CORE_AUTH_SERVICE;
                debug.warning("EditAuthTypeViewBean() " +
                    "The auth instance could not be found. The instance name" +
                    " has been reset to " + name);
                
            }  
            initialize(name);
        }
    }

    /**
     * Checks to see if this is a dynamic validator request, if not execution is passed to the parent.
     *
     * @param event Request invocation event.
     */
    public void handleDynLinkRequest(RequestInvocationEvent event) {
        final HttpServletRequest request = event.getRequestContext().getRequest();
        final String attributeName = request.getParameter(ATTRIBUTE_NAME);

        if (Boolean.parseBoolean(request.getParameter(DYNAMIC_VALIDATION))) {
            handleDynamicValidationRequest(attributeName);
        } else {
            super.handleDynLinkRequest(event);
        }
    }

    /**
     * Retrieve the validators specified for the attribute, invoke their validate methods
     * and display the validation messages if any are present.
     *
     * @param attributeName The name of the attribute for which the validation should be done.
     */
    private void handleDynamicValidationRequest(String attributeName) {
        try {
            // Store the current attribute values from the UI to render when beginDisplay is called
            unpersistedValueMap = getUnpersistedValueMap();
            dynamicRequest = true;
            final String instance = (String) getPageSessionAttribute(SERVICE_TYPE);
            final List<DynamicAttributeValidator> validatorList = getAuthModel().
                    getDynamicValidators(instance, attributeName);
            final StringBuilder messageBuilder = new StringBuilder();
            final Encoder encoder = ESAPI.encoder();

            for (DynamicAttributeValidator validator : validatorList) {
                if (!validator.validate(instance, attributeName, unpersistedValueMap)) {
                    final String message = validator.getValidationMessage();
                    if (message != null) {
                        final String[] messageLines = validator.getValidationMessage().split("\n");
                        for (String line : messageLines) {
                            if (line != null && !line.trim().isEmpty()) {
                                messageBuilder.append(encoder.encodeForHTML(line));
                                messageBuilder.append(HTML_BREAK);
                            }
                        }
                    }
                }
            }

            if (messageBuilder.length() > 0) {
                final String message = messageBuilder.substring(0, messageBuilder.length() - HTML_BREAK.length());
                setInlineAlertMessage(CCAlert.TYPE_WARNING, "message.warning", message, false);
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information", "message.validation.success");
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", e.getMessage());
        }
        forwardTo();
    }

    /**
     * Converts the Attribute Value map to a checked map.
     * @return A checked attribute value map.
     */
    private Map<String, Set<String>> getUnpersistedValueMap() {
        final Map<String, Set<String>> checkedValueMap = new HashMap<String, Set<String>>();

        if (propertySheetModel != null) {
            final Map uncheckedValueMap = propertySheetModel.getAttributeValueMap();
            final Iterator<Map.Entry> oldIterator = uncheckedValueMap.entrySet().iterator();

            while (oldIterator.hasNext()) {
                final Map.Entry entry = oldIterator.next();
                final Set<String> valueSet = new HashSet<String>();
                final Object[] objectValues = (Object[]) entry.getValue();
                final String[] stringValues = Arrays.copyOf(objectValues, objectValues.length, String[].class);
                Collections.addAll(valueSet, stringValues);
                checkedValueMap.put((String) entry.getKey(), valueSet);
            }
        }

        return checkedValueMap;
    }

    /**
     * This will populate the property sheet with attribute values for display. If this is called after
     * a dynamic request the values that was present on the UI (which might not have been persisted) will be used.
     * @param event The display event.
     * @throws ModelControlException
     */
    public void beginDisplay(DisplayEvent event) throws ModelControlException {
        super.beginDisplay(event);
        final AuthPropertiesModel model = getAuthModel();
        final String instance = (String) getPageSessionAttribute(SERVICE_TYPE);
        final AMPropertySheet propertySheet = (AMPropertySheet) getChild(PROPERTY_ATTRIBUTE);
        Map valueMap = unpersistedValueMap;

        if (model != null && propertySheet != null) {
            // If this is not a dynamic request the UI is set with persisted values
            if (!dynamicRequest) {
                valueMap = model.getInstanceValues(instance);
            }
            if (valueMap != null) {
                propertySheet.setAttributeValues(valueMap, model);
            }
        }
    }

    public void forwardTo(RequestContext reqContext)
        throws NavigationException {
        String name = getServiceName(
            (String)getPageSessionAttribute(SERVICE_TYPE));
        initialize(name);
        super.forwardTo(reqContext);
    }

    protected String getBackButtonLabel() {
        return getBackButtonLabel("config.auth.label");
    }

    /**
     * Handles save request.
     *
     * @param event Request invocation event.
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        String instance = (String)getPageSessionAttribute(SERVICE_TYPE);
    
        AuthPropertiesModel model = getAuthModel();
        /*
         * The service name will be null if the entry was deleted by
         * another user while the properties were being viewed.
         */
        if (getServiceName(instance) == null) {
            returnToAuthProperties(
                model.getLocalizedString("no.module.instance"));
        } else {
            if (model != null) {
                try {
                    Map values = getValues();
                    model.setInstanceValues(instance, values);
                    setInlineAlertMessage(CCAlert.TYPE_INFO, 
                        "message.information", "message.updated");
                } catch (AMConsoleException e) {
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        e.getMessage());
                }
            }
            forwardTo();
        }
    }

    /**
     * Handles save request.
     * 
     * @param event Request invocation event.
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException
    {  
        String instance = (String)getPageSessionAttribute(SERVICE_TYPE);

        /*                                                               
         * The service name will be null if the entry was deleted by
         * another user while the properties were being viewed.
         */
        if (getServiceName(instance) == null) {
            debug.warning("EditAuthTypeViewBean.handleButton2Request() " +
                "The instance " + instance + " could not be found");
            AuthPropertiesModel model = getAuthModel();
            returnToAuthProperties(
                model.getLocalizedString("no.module.instance"));
        } else {
            super.handleButton2Request(event);
        }
    }  

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        returnToAuthProperties(null);
    }

    private void returnToAuthProperties(String message) {
        backTrail();
        AuthPropertiesViewBean vb = (AuthPropertiesViewBean)
            getViewBean(AuthPropertiesViewBean.class);
        if (message != null) {
            vb.setPageSessionAttribute(
                AuthPropertiesViewBean.INSTANCE_MSG, message);
        }
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected Map getValues()
        throws ModelControlException, AMConsoleException
    {
        Map values = null;
        String instance = (String)getPageSessionAttribute(SERVICE_TYPE);

        AuthPropertiesModel model = getAuthModel();
        if (model != null) {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            values = ps.getAttributeValues(
                model.getInstanceValues(instance), model);
        }

        return values;
    }

    /*
     * Returns the real service name for a given authentication type
     */
    private String getServiceName(String type) {
        AuthPropertiesModel m = getAuthModel();
        return m.getServiceName(type);
    }

    private AuthPropertiesModel getAuthModel() {
        if (authModel == null) {
            RequestContext rc = RequestManager.getRequestContext();
            authModel = new AuthPropertiesModelImpl(
                rc.getRequest(), getPageSessionAttributes());
        }
        return authModel;
    }

    protected String getBreadCrumbDisplayName() {
        String instance = (String)getPageSessionAttribute(SERVICE_TYPE);
        String[] arg = {instance};
        AuthPropertiesModel model = getAuthModel();
        return MessageFormat.format(model.getLocalizedString(
            "breadcrumbs.auth.editInstance"), (Object[])arg);
    }

    protected boolean startPageTrail() {
        return false;
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        try {
            return new AuthProfileModelImpl(
                req, serviceName, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        return null;
    }
}
