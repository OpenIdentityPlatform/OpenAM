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
package org.forgerock.openam.upgrade.steps.scripting;

import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static org.forgerock.openam.scripting.ScriptConstants.*;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;

import com.google.inject.Inject;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceNotFoundException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.steps.AbstractUpgradeStep;
import org.forgerock.openam.utils.StringUtils;

import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This upgrade step moves global script engine configurations from the Scripted Auth global settings
 * to the Scripting Service global settings.
 *
 * @since 13.0.0
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class ScriptingSchemaStep extends AbstractUpgradeStep {

    private static final String AUTH_MODULE_SERVICE_NAME = "iPlanetAMAuthScriptedService";
    private static final String SCRIPTING_SERVICE_NAME = "ScriptingService";
    private static final String CLIENT_SIDE_SCRIPT = "iplanet-am-auth-scripted-client-script";
    private static final String SERVER_SIDE_SCRIPT = "iplanet-am-auth-scripted-server-script";
    private static final String SERVER_SCRIPT_TYPE = "iplanet-am-auth-scripted-script-type";

    private final Map<ScriptContext, Map<String, String>> globalSchemaKeys = new HashMap<>();
    private final Map<ScriptContext, Map<String, Set<String>>> contextEngineConfigurations = new HashMap<>();
    private final Map<ScriptContext, Map<String, Set<String>>> contextScriptConfigurations = new HashMap<>();

    @Inject
    public ScriptingSchemaStep(PrivilegedAction<SSOToken> adminTokenAction,
                               @DataLayer(ConnectionType.DATA_LAYER) ConnectionFactory connectionFactory) {

        super(adminTokenAction, connectionFactory);
        Map<String, String> attributeKeys = new HashMap<>();
        attributeKeys.put("iplanet-am-auth-scripted-server-timeout", SCRIPT_TIMEOUT);
        attributeKeys.put("iplanet-am-auth-scripted-core-threads", THREAD_POOL_CORE_SIZE);
        attributeKeys.put("iplanet-am-auth-scripted-max-threads", THREAD_POOL_MAX_SIZE);
        attributeKeys.put("iplanet-am-auth-scripted-queue-size", THREAD_POOL_QUEUE_SIZE);
        attributeKeys.put("iplanet-am-auth-scripted-idle-timeout", THREAD_POOL_IDLE_TIMEOUT);
        attributeKeys.put("iplanet-am-auth-scripted-white-list", WHITE_LIST);
        attributeKeys.put("iplanet-am-auth-scripted-black-list", BLACK_LIST);
        attributeKeys.put("iplanet-am-auth-scripted-use-security-manager", USE_SECURITY_MANAGER);
        globalSchemaKeys.put(ScriptContext.AUTHENTICATION_SERVER_SIDE, attributeKeys);
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            ServiceSchemaManager schemaManager = new ServiceSchemaManager(AUTH_MODULE_SERVICE_NAME, getAdminToken());
            ServiceSchema globalSchema = schemaManager.getGlobalSchema();
            if (globalSchema == null || globalSchema.getAttributeDefaults().isEmpty()) {
                DEBUG.message("No upgrade required for {}; no global schema found.", AUTH_MODULE_SERVICE_NAME);
                return;
            }
            captureEngineConfiguration(globalSchema);
            captureScriptConfiguration(schemaManager.getOrganizationSchema());
        } catch (ServiceNotFoundException e) {
            DEBUG.message("Scripted auth modules not found. Nothing to upgrade", e);
        }catch (SMSException | SSOException e) {
            DEBUG.error("An error occurred while trying to look for upgradable Scripted auth global settings", e);
            throw new UpgradeException("Unable to retrieve Scripted auth global settings", e);
        }
    }

    private void captureEngineConfiguration(ServiceSchema globalSchema) {
        DEBUG.message("Capture global schema attributes for {}", AUTH_MODULE_SERVICE_NAME);
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> schemaAttributes = globalSchema.getAttributeDefaults();
        for (Map.Entry<ScriptContext, Map<String, String>> contextEntry : globalSchemaKeys.entrySet()) {
            Map<String, Set<String>> engineAttributes = new HashMap<>();
            for (Map.Entry<String, String> attributeEntry : contextEntry.getValue().entrySet()) {
                engineAttributes.put(attributeEntry.getValue(), schemaAttributes.get(attributeEntry.getKey()));
            }
            contextEngineConfigurations.put(contextEntry.getKey(), engineAttributes);
        }
    }

    private void captureScriptConfiguration(ServiceSchema organisationSchema) {
        DEBUG.message("Capture org schema attributes for {}", AUTH_MODULE_SERVICE_NAME);
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> orgAttributes = organisationSchema.getAttributeDefaults();

        Map<String, Set<String>> scriptAttributes = new HashMap<>();
        String clientScript = getMapAttr(orgAttributes, CLIENT_SIDE_SCRIPT);
        if (StringUtils.isNotEmpty(clientScript)) {
            DEBUG.message("Found default client side script for {}", AUTH_MODULE_SERVICE_NAME);
            scriptAttributes.put(DEFAULT_SCRIPT, Collections.singleton(clientScript));
            contextScriptConfigurations.put(ScriptContext.AUTHENTICATION_CLIENT_SIDE, scriptAttributes);
        }

        scriptAttributes = new HashMap<>();
        String serverScript = getMapAttr(orgAttributes, SERVER_SIDE_SCRIPT);
        if (StringUtils.isNotEmpty(serverScript)) {
            DEBUG.message("Found default server side script for {}", AUTH_MODULE_SERVICE_NAME);
            scriptAttributes.put(DEFAULT_SCRIPT, Collections.singleton(serverScript));
        }
        String scriptLanguage = getMapAttr(orgAttributes, SERVER_SCRIPT_TYPE);
        if (StringUtils.isBlank(scriptLanguage)) {
            scriptLanguage = "JAVASCRIPT"; //set default to JS if no default provided as per scripting.xml schema
        }
        scriptAttributes.put(DEFAULT_LANGUAGE, Collections.singleton(scriptLanguage.toUpperCase()));
        contextScriptConfigurations.put(ScriptContext.AUTHENTICATION_SERVER_SIDE, scriptAttributes);
    }

    @Override
    public boolean isApplicable() {
        return !contextEngineConfigurations.isEmpty() || !contextScriptConfigurations.isEmpty();
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            ServiceConfigManager configManager = new ServiceConfigManager(SCRIPTING_SERVICE_NAME, getAdminToken());
            ServiceConfig globalConfig = configManager.getGlobalConfig(null);
            upgradeEngineConfiguration(globalConfig);
            upgradeScriptConfiguration(globalConfig);
        } catch (SMSException | SSOException e) {
            UpgradeProgress.reportEnd("upgrade.failed");
            DEBUG.error("An error occurred while trying to upgrade the Scripting global settings", e);
            throw new UpgradeException("Unable to upgrade Scripting global settings", e);
        }
    }

    private void upgradeEngineConfiguration(ServiceConfig globalConfig) throws SMSException, SSOException {
        for (Map.Entry<ScriptContext, Map<String, Set<String>>> entry : contextEngineConfigurations.entrySet()) {
            String contextName = entry.getKey().name();
            DEBUG.message("Upgrading engine configuration for script context: {}", contextName);
            UpgradeProgress.reportStart("upgrade.scripting.global.engine.start", contextName);
            ServiceConfig contextConfig = globalConfig.getSubConfig(contextName);
            ServiceConfig engineConfig = contextConfig.getSubConfig("engineConfiguration");
            engineConfig.setAttributes(entry.getValue());
            DEBUG.message("Saved engine configuration: {}", entry.getValue().toString());
            UpgradeProgress.reportEnd("upgrade.success");
        }
    }

    private void upgradeScriptConfiguration(ServiceConfig globalConfig) throws SMSException, SSOException {
        for (Map.Entry<ScriptContext, Map<String, Set<String>>> entry : contextScriptConfigurations.entrySet()) {
            String contextName = entry.getKey().name();
            DEBUG.message("Upgrading script configuration for script context: {}", contextName);
            UpgradeProgress.reportStart("upgrade.scripting.global.script.start", contextName);
            ServiceConfig contextConfig = globalConfig.getSubConfig(contextName);
            contextConfig.setAttributes(entry.getValue());
            DEBUG.message("Saved script configuration: {}", entry.getValue().toString());
            UpgradeProgress.reportEnd("upgrade.success");
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        StringBuilder sb = new StringBuilder();
        if (contextEngineConfigurations.size() > 0) {
            sb.append(MessageFormat.format(BUNDLE.getString("upgrade.scripting.global.settings"),
                    contextEngineConfigurations.size()));
            sb.append(delimiter);
        }
        return sb.toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<>();
        tags.put(LF, delimiter);

        StringBuilder sb = new StringBuilder();
        sb.append(INDENT);
        sb.append(MessageFormat.format(BUNDLE.getString("upgrade.scripting.global.context"),
                AUTH_MODULE_SERVICE_NAME, SCRIPTING_SERVICE_NAME));
        sb.append(delimiter);
        tags.put("%REPORT_DATA%", sb.toString());
        return tagSwapReport(tags, "upgrade.scripting.global.report");
    }
}
