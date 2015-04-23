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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package com.sun.identity.console.base;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.sm.DynamicAttributeValidator;
import com.sun.web.ui.view.alert.CCAlert;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This view bean handles dynamic requests made from the client.
 *
 * @since 13.0.0
 */
public abstract class DynamicRequestViewBean extends AMPrimaryMastHeadViewBean {

    protected boolean dynamicRequest = false;
    protected Map<String, Set<String>> unsavedAttributeValues;
    protected AMPropertySheetModel propertySheetModel;

    /**
     * Creates an instance of {@link DynamicRequestViewBean}.
     *
     * @param name Name of page.
     */
    public DynamicRequestViewBean(String name) {
        super(name);
    }

    /**
     * Checks to see if this is a dynamic validator request, if not execution is passed to the sub class.
     *
     * @param event Request invocation event.
     */
    public void handleDynLinkRequest(RequestInvocationEvent event) {

        HttpServletRequest request = event.getRequestContext().getRequest();
        String attributeName = request.getParameter("attrname");

        if (Boolean.parseBoolean(request.getParameter("dynamic_validation"))) {
            // Store the current attribute values from the UI to render when beginDisplay is called
            unsavedAttributeValues = getAttributeValueMap();
            dynamicRequest = true;
            handleDynamicValidationRequest(attributeName);
        } else {
            handleDynamicLinkRequest(attributeName);
        }
    }

    /**
     * Retrieve the validators specified for the attribute, invoke their
     * validate methods and display the validation messages if any are present.
     *
     * @param attributeName The name of the attribute for which the validation should be done.
     */
    protected abstract void handleDynamicValidationRequest(String attributeName);

    /**
     * Handle the appropriate dynamic link request.
     *
     * @param attributeName The name of the attribute that invoked the request.
     */
    protected abstract void handleDynamicLinkRequest(String attributeName);

    /**
     * Get the current (could be unsaved) attribute values for the view bean.
     *
     * @return the attribute values
     */
    protected abstract Map<String, Set<String>> getAttributeValueMap();

    /**
     * Validate a provided script.
     *
     * @param attributeName The name of the attribute for which the validation should be done.
     * @param instance The name of the script instance.
     * @param validators List of predefined classes that implement {@link DynamicAttributeValidator},
     *                   which should be used to validate the script.
     * @throws IllegalAccessException, InstantiationException if any of the classes could not be instantiated
     */
    protected void performValidation(String attributeName, String instance,
                                     List<Class<DynamicAttributeValidator>> validators)
            throws IllegalAccessException, InstantiationException {

        final StringBuilder messageBuilder = new StringBuilder();
        final Encoder encoder = ESAPI.encoder();

        for (Class<DynamicAttributeValidator> validatorClass : validators) {
            DynamicAttributeValidator validator = validatorClass.newInstance();
            if (!validator.validate(instance, attributeName, unsavedAttributeValues)) {
                final String message = validator.getValidationMessage(getUserLocale());
                if (message != null) {
                    final String[] messageLines = message.split("\n");
                    for (String line : messageLines) {
                        if (line != null && !line.trim().isEmpty()) {
                            messageBuilder.append(encoder.encodeForHTML(line));
                            messageBuilder.append("<br>");
                        }
                    }
                }
            }
        }

        if (messageBuilder.length() > 0) {
            final String message = messageBuilder.substring(0, messageBuilder.length() - 4); // 4 is length of '<br>'
            setInlineAlertMessage(CCAlert.TYPE_WARNING, "message.warning", message, false);
        } else {
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information", "message.validation.success");
        }
    }
}
