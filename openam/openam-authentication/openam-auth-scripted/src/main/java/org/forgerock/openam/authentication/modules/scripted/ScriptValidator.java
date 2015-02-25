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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.scripted;

import com.sun.identity.sm.DynamicAttributeValidator;
import org.forgerock.openam.scripting.ScriptError;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.ScriptingLanguage;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;

import java.util.ResourceBundle;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.MissingResourceException;

/**
 * The <code>ScriptValidator</code> is tasked with validating an Authentication script. It will use the validator
 * supplied by the predefined scripting languages from {@link SupportedScriptingLanguage}, based on the scripting
 * language selected by the user for the module instance.
 *
 * @since 12.0.0
 */
public class ScriptValidator implements DynamicAttributeValidator {

    private String validationMessage = null;
    private ResourceBundle resourceBundle = null;
    private static final String RESOURCE_BUNDLE_NAME = "amAuthScripted";
    private static final String LANGUAGE_NOT_SUPPORTED = "language-not-supported";
    private static final String SERVER_SIDE_SCRIPT = "a104";
    private static final String MODULE_DESCRIPTION = "iplanet-am-auth-scripted-service-description";
    private static final Map<String, ScriptingLanguage> SUPPORTED_LANGUAGES =
            new HashMap<String, ScriptingLanguage>() {{
        put(Scripted.JAVA_SCRIPT_LABEL, SupportedScriptingLanguage.JAVASCRIPT);
        put(Scripted.GROOVY_LABEL, SupportedScriptingLanguage.GROOVY);
    }};

    /**
     * This implementation will look up the language specified by the user and create a {@link ScriptObject} instance
     * to validate the script with.
     *
     * {@inheritDoc}
     */
    public boolean validate(String instanceName, String attributeName, Map<String, Set<String>> attributeMap) {
        final String languageName = getAttributeValueAsString(Scripted.SCRIPT_TYPE_ATTR_NAME, attributeMap);
        final ScriptingLanguage language = SUPPORTED_LANGUAGES.get(languageName);
        boolean validScript;

        if (language != null) {
            final String scriptText = getAttributeValueAsString(attributeName, attributeMap);
            final ScriptObject script = new ScriptObject(instanceName, scriptText, language, null);
            final List<ScriptError> scriptErrorList = script.validate();
            final StringBuilder messageBuilder = new StringBuilder();

            if (!scriptErrorList.isEmpty()) {
                messageBuilder.append("Error in ");
                messageBuilder.append(getMessage(SERVER_SIDE_SCRIPT));
                messageBuilder.append(" for ");
                messageBuilder.append(getMessage(MODULE_DESCRIPTION));
                messageBuilder.append(" ");
                messageBuilder.append(instanceName);
                messageBuilder.append(":\n");
            }

            for (ScriptError error : scriptErrorList) {
                messageBuilder.append("Line ");
                messageBuilder.append(error.getLineNumber());
                messageBuilder.append(", column ");
                messageBuilder.append(error.getColumnNumber());
                messageBuilder.append(": ");
                messageBuilder.append(error.getMessage());
                messageBuilder.append("\n");
            }

            if (messageBuilder.length() > 0) {
                validationMessage = messageBuilder.substring(0, messageBuilder.length() - 1);
                validScript = false;
            } else {
                validScript = true;
            }
        } else {
            validScript = false;
            validationMessage = getMessage(LANGUAGE_NOT_SUPPORTED) + ": " + languageName;
        }
        return validScript;
    }

    /**
     * {@inheritDoc}
     */
    public String getValidationMessage() {
        return validationMessage;
    }

    private String getAttributeValueAsString(String attributeName, Map<String, Set<String>> attributeMap) {
        String value = null;
        final Set<String> valueSet = attributeMap.get(attributeName);

        if (valueSet != null && !valueSet.isEmpty()) {
            value = valueSet.iterator().next();
        }
        return value;
    }

    private String getMessage(String key) {
        return getResourceBundle().getString(key);
    }

    private ResourceBundle getResourceBundle() {
        if (resourceBundle == null) {
            try {
                resourceBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);
            } catch (MissingResourceException mre) {
                resourceBundle = new EmptyResourceBundle();
            }
        }
        return resourceBundle;
    }

    /**
     * An empty resource bundle for when the bundle can not be found on the class path.
     */
    private class EmptyResourceBundle extends ResourceBundle {
        @Override
        protected Object handleGetObject(String s) {
            return s;
        }

        @Override
        public Enumeration<String> getKeys() {
            return new Enumeration<String>() {
                public boolean hasMoreElements() {
                    return false;
                }

                public String nextElement() {
                    return null;
                }
            };
        }
    }
}
