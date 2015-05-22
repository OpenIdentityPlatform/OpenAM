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
package org.forgerock.openam.scripting.service;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.ChoiceValues;
import com.sun.identity.sm.SMSEntry;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.scripting.ScriptConstants;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.query.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to retrieve the script names and IDs from the scripting service for display
 * in a drop down UI component. Implementing classes has to specify the context on which to filter the scripts.
 *
 * @since 13.0.0
 */
public abstract class ScriptChoiceValues extends ChoiceValues {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ScriptConstants.LOGGER_NAME);

    @Override
    public Map getChoiceValues() {
        return getChoiceValues(Collections.EMPTY_MAP);
    }

    @Override
    public Map getChoiceValues(Map envParams) {
        String realm = null;

        if (envParams != null) {
            realm = (String)envParams.get(Constants.ORGANIZATION_NAME);
        }
        if (StringUtils.isBlank(realm)) {
            realm = SMSEntry.getRootSuffix();
        }
        ScriptingService<ScriptConfiguration> scriptingService = getScriptingService(realm);
        Map<String, String> choiceValues = new LinkedHashMap<>();
        try {
            Set<ScriptConfiguration> scriptConfigs = scriptingService.get(
                    QueryFilter.equalTo(ScriptConstants.SCRIPT_CONTEXT, getContextName()));
            for (ScriptConfiguration config : scriptConfigs) {
                choiceValues.put(config.getId(), config.getName());
            }
        } catch (ScriptException e) {
            LOGGER.error("Failed to retrieve scripts for " + getContextName(), e);
        }
        choiceValues.put("[Default]", "label.default");
        return choiceValues;
    }

    /**
     * Override this method to specify the context on which to filter the scripts.
     * @return The name of the script context to filter on.
     */
    protected abstract String getContextName();

    private ScriptingService<ScriptConfiguration> getScriptingService(String realm) {
        ScriptingServiceFactory<ScriptConfiguration> scriptingServiceFactory =
                InjectorHolder.getInstance(Key.get(new TypeLiteral<ScriptingServiceFactory<ScriptConfiguration>>() {}));
        return scriptingServiceFactory.create(SubjectUtils.createSuperAdminSubject(), realm);
    }

}
