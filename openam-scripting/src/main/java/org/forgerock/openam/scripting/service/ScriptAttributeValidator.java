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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */
package org.forgerock.openam.scripting.service;

import static org.forgerock.openam.scripting.ScriptConstants.SCRIPT_ERROR_DETAIL;
import static org.forgerock.openam.scripting.ScriptConstants.SCRIPT_ERROR_MESSAGE;

import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.sm.DynamicAttributeValidator;
import org.forgerock.openam.scripting.ScriptConstants;
import org.forgerock.openam.scripting.ScriptError;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.ScriptingLanguage;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.utils.CollectionUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * The <code>ScriptAttributeValidator</code> is tasked with validating a script. It will use the validator
 * supplied by the predefined scripting languages from {@link SupportedScriptingLanguage}, based on the scripting
 * language selected by the user for the module instance.
 *
 * @since 12.0.0
 */
public abstract class ScriptAttributeValidator implements DynamicAttributeValidator {

    private ResourceBundle resourceBundle = null;
    private List<ScriptError> scriptErrorList;
    private ScriptException scriptException;

    /**
     * Get the name of the attribute that holds the language that the script is written in.
     * @return The Scripting language.
     */
    public abstract String getLanguageAttributeName();

    /**
     * This implementation will look up the language specified by the user and create a {@link ScriptObject} instance
     * to validate the script with.
     *
     * {@inheritDoc}
     */
    public boolean validate(String instanceName, String attributeName, Map<String, Set<String>> attributeMap) {
        try {
            ScriptingLanguage language = ScriptConstants.getLanguageFromString(CollectionHelper.getMapAttr
                    (attributeMap, getLanguageAttributeName()));
            String scriptText = CollectionHelper.getMapAttr(attributeMap, attributeName);
            scriptErrorList = new ScriptObject(instanceName, scriptText, language, null).validate();
        } catch (ScriptException e) {
            scriptException = e;
        }
        return CollectionUtils.isEmpty(scriptErrorList);
    }

    @Override
    public String getValidationMessage(Locale locale) {

        if (CollectionUtils.isNotEmpty(scriptErrorList)) {
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(getResourceBundle(locale).getString(SCRIPT_ERROR_MESSAGE));
            for (ScriptError error : scriptErrorList) {
                Object[] parameters = {error.getLineNumber(), error.getColumnNumber(), error.getMessage()};
                messageBuilder.append(
                        MessageFormat.format(getResourceBundle(locale).getString(SCRIPT_ERROR_DETAIL), parameters));
            }
            return messageBuilder.substring(0, messageBuilder.length() - 1);

        } else if (scriptException != null) {
            return scriptException.getL10NMessage(locale);
        }

        return ScriptConstants.EMPTY;
    }

    private ResourceBundle getResourceBundle(Locale locale) {
        if (resourceBundle == null) {
            resourceBundle = AMResourceBundleCache.getInstance().getResBundle(ScriptConstants.RESOURCE_BUNDLE, locale);
        }
        return resourceBundle;
    }
}
