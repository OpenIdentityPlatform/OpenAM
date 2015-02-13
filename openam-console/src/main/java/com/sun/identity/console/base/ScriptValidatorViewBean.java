/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2015 Nomura Research Institute, Ltd. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2012-2014 ForgeRock AS"
 */

/*
 * Portions Copyrighted 2015 ForgeRock AS.
 */
package com.sun.identity.console.base;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.sm.DynamicAttributeValidator;
import com.sun.web.ui.view.alert.CCAlert;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;

/**
 * This view bean validates a script.
 */
public abstract class ScriptValidatorViewBean extends AMServiceProfileViewBeanBase {

    protected boolean dynamicRequest = false;
    protected Map<String, Set<String>> unpersistedValueMap;

    private static final String DYNAMIC_VALIDATION = "dynamic_validation";
    private static final String ATTRIBUTE_NAME = "attrname";
    private static final String HTML_BREAK = "<br>";
    private static final String SCRIPT_LINE_BREAK = "\n";

    /**
     * Constructs a script validate view bean.
     */
    public ScriptValidatorViewBean(String name, String url, String serviceName) {
        super(name, url, serviceName);
    }

    /**
     * Checks to see if this is a dynamic validator request, if not execution is
     * passed to the parent.
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

    protected abstract void handleDynamicValidationRequest(String attributeName);

    /**
     * Validate a provided script.
     * 
     * @param attributeName The name of the attribute for which the validation should be done.
     * @param instance The name of the authentication instance.
     * @param validatorList Classes to validate a script.
     */
    protected void validateScript(String attributeName, String instance, List<DynamicAttributeValidator> validatorList) {

        final StringBuilder messageBuilder = new StringBuilder();
        final Encoder encoder = ESAPI.encoder();
        
        // Store the current attribute values from the UI to render when beginDisplay is called
        unpersistedValueMap = getConvertedValueMap();
        dynamicRequest = true;

        for (DynamicAttributeValidator validator : validatorList) {
            if (!validator.validate(instance, attributeName, unpersistedValueMap)) {
                final String message = validator.getValidationMessage();
                if (message != null) {
                    final String[] messageLines = validator.getValidationMessage().split(SCRIPT_LINE_BREAK);
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
    }

    /**
     * Get the converted attribute value map.
     * 
     * @return the converted attribute value map
     */
    private Map<String, Set<String>> getConvertedValueMap() {
        final Map<String, Set<String>> convertedValueMap = new HashMap<String, Set<String>>();

        if (propertySheetModel != null) {
            final Map attributeValueMap = propertySheetModel.getAttributeValueMap();
            final Iterator<Map.Entry> oldIterator = attributeValueMap.entrySet().iterator();
            while (oldIterator.hasNext()) {
                final Map.Entry entry = oldIterator.next();
                final Set<String> valueSet = new HashSet<String>();
                final Object[] objectValues = (Object[]) entry.getValue();
                final String[] stringValues = Arrays.copyOf(objectValues, objectValues.length, String[].class);
                Collections.addAll(valueSet, stringValues);
                convertedValueMap.put((String) entry.getKey(), valueSet);
            }
        }
        return convertedValueMap;
    }
}
