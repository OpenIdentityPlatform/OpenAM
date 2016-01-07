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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.scripting.service;

import static org.forgerock.openam.scripting.ScriptConstants.EMPTY_SCRIPT_SELECTION;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;
import static org.forgerock.openam.utils.StringUtils.isBlank;

import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.ChoiceValues;
import com.sun.identity.sm.SMSEntry;
import org.forgerock.openam.scripting.ScriptConstants;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.datastore.ScriptConfigurationDataStore;
import org.forgerock.openam.scripting.datastore.ScriptingDataStore;
import org.forgerock.openam.scripting.datastore.ScriptingDataStoreFactory;
import org.forgerock.util.query.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to retrieve the script names and IDs from the scripting service for display
 * in a drop down UI component.
 *
 * @since 13.0.0
 */
public class ScriptChoiceValues extends ChoiceValues {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ScriptConstants.LOGGER_NAME);

    @Override
    public Map getChoiceValues() {
        return getChoiceValues(Collections.EMPTY_MAP);
    }

    @Override
    public Map getChoiceValues(Map envParams) {
        String realm = null;
        String contextId = null;
        boolean globalOnly = false;
        if (envParams != null) {
            realm = (String)envParams.get(Constants.ORGANIZATION_NAME);
            contextId = (String)envParams.get(Constants.CONFIGURATION_NAME);
        }
        if (isBlank(realm)) {
            realm = SMSEntry.getRootSuffix();
        }
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> keyValues = getConfiguredKeyValues();
        if (isNotEmpty(keyValues)) {

            Set<String> values = keyValues.get("ContextId");
            if (isBlank(contextId) && isNotEmpty(values)) {
                contextId = values.iterator().next();
            }

            values = keyValues.get("GlobalOnly");
            if (isNotEmpty(values)) {
                globalOnly = Boolean.parseBoolean(values.iterator().next());
            }
        }

        Map<String, String> choiceValues = new LinkedHashMap<>();

        if (globalOnly) {
            for (ScriptConstants.GlobalScript globalScript : ScriptConstants.GlobalScript.values()) {
                if (isBlank(contextId) || globalScript.getContext().name().equalsIgnoreCase(contextId)) {
                    choiceValues.put(globalScript.getId(), globalScript.getDisplayName());
                }
            }
        } else {
            ScriptingService scriptingService = getScriptingService(realm);
            try {
                Set<ScriptConfiguration> scriptConfigs;
                if (isBlank(contextId)) {
                    scriptConfigs = scriptingService.getAll();
                } else {
                    scriptConfigs = scriptingService.get(QueryFilter.equalTo(ScriptConstants.SCRIPT_CONTEXT, contextId));
                }
                for (ScriptConfiguration config : scriptConfigs) {
                    choiceValues.put(config.getId(), config.getName());
                }
            } catch (ScriptException e) {
                LOGGER.error("Failed to retrieve scripts for " + contextId, e);
            }
        }

        choiceValues.put(EMPTY_SCRIPT_SELECTION, "label.select.script");
        return choiceValues;
    }

    /**
     * We are not using Guice here as this class is used by the SSOADM, which is not packaged with Guice.
     * @param realm The realm in which to scripting service should be created.
     * @return the scripting service.
     */
    private ScriptingService getScriptingService(String realm) {
        final Subject admin = SubjectUtils.createSuperAdminSubject();
        return new ScriptConfigurationService(LOGGER, admin, realm, new ScriptingDataStoreFactory() {
            @Override
            public ScriptingDataStore create(Subject subject, String realm) {
                return new ScriptConfigurationDataStore(LOGGER, admin, realm);
            }
        });
    }

}
