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
package org.forgerock.openam.upgrade.steps.scripting;

import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static org.forgerock.openam.scripting.ScriptConstants.*;
import static org.forgerock.openam.scripting.ScriptConstants.GlobalScript.*;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext.AUTHENTICATION_CLIENT_SIDE;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext.AUTHENTICATION_SERVER_SIDE;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;
import static org.forgerock.openam.utils.Time.*;

import com.google.inject.Inject;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceNotFoundException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
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
import java.util.UUID;

/**
 * This upgrade step moves global script engine configurations from the Scripted Auth global settings
 * to the Scripting Service global settings.
 *
 * @since 13.0.0
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class ScriptingSchemaStep extends AbstractUpgradeStep {

    private static final String AUTH_MODULE_SERVICE_NAME = "iPlanetAMAuthScriptedService";
    private static final String DEVICE_ID_SERVICE_NAME = "iPlanetAMAuthDeviceIdMatchService";
    private static final String SCRIPTING_SERVICE_NAME = "ScriptingService";
    private static final String CLIENT_SIDE_SCRIPT = "iplanet-am-auth-scripted-client-script";
    private static final String SERVER_SIDE_SCRIPT = "iplanet-am-auth-scripted-server-script";
    private static final String SERVER_SCRIPT_TYPE = "iplanet-am-auth-scripted-script-type";

    private final Map<String, String> globalSchemaKeys = new HashMap<>();
    private final Map<String, Set<String>> contextEngineConfigurations = new HashMap<>();
    private final Map<GlobalScript, Map<String, Set<String>>> globalScriptConfigurations = new HashMap<>();

    @Inject
    public ScriptingSchemaStep(PrivilegedAction<SSOToken> adminTokenAction,
                               @DataLayer(ConnectionType.DATA_LAYER) ConnectionFactory connectionFactory) {

        super(adminTokenAction, connectionFactory);
        globalSchemaKeys.put("iplanet-am-auth-scripted-server-timeout", SCRIPT_TIMEOUT);
        globalSchemaKeys.put("iplanet-am-auth-scripted-core-threads", THREAD_POOL_CORE_SIZE);
        globalSchemaKeys.put("iplanet-am-auth-scripted-max-threads", THREAD_POOL_MAX_SIZE);
        globalSchemaKeys.put("iplanet-am-auth-scripted-queue-size", THREAD_POOL_QUEUE_SIZE);
        globalSchemaKeys.put("iplanet-am-auth-scripted-idle-timeout", THREAD_POOL_IDLE_TIMEOUT);
        globalSchemaKeys.put("iplanet-am-auth-scripted-white-list", WHITE_LIST);
        globalSchemaKeys.put("iplanet-am-auth-scripted-black-list", BLACK_LIST);
        globalSchemaKeys.put("iplanet-am-auth-scripted-use-security-manager", USE_SECURITY_MANAGER);
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            captureScriptedAuthModuleConfiguration();
            captureDeviceIdMatchConfiguration();
        } catch (ServiceNotFoundException e) {
            DEBUG.message("Scripted auth modules not found. Nothing to upgrade", e);
        }catch (SMSException | SSOException e) {
            DEBUG.error("An error occurred while trying to look for upgradable global Scripting settings", e);
            throw new UpgradeException("Unable to retrieve global Scripting settings", e);
        }
    }

    private void captureScriptedAuthModuleConfiguration() throws SSOException, SMSException {
        ServiceSchemaManager schemaManager = new ServiceSchemaManager(AUTH_MODULE_SERVICE_NAME, getAdminToken());
        ServiceSchema globalSchema = schemaManager.getGlobalSchema();
        if (globalSchema == null || globalSchema.getAttributeDefaults().isEmpty()) {
            DEBUG.message("No upgrade required for {}; no global schema found.", AUTH_MODULE_SERVICE_NAME);
            return;
        }
        captureEngineConfiguration(globalSchema);
        captureServerScriptConfiguration(schemaManager.getOrganizationSchema(),
                AUTH_MODULE_SERVER_SIDE, AUTHENTICATION_SERVER_SIDE, AUTH_MODULE_SERVICE_NAME);
        captureClientScriptConfiguration(schemaManager.getOrganizationSchema(),
                AUTH_MODULE_CLIENT_SIDE, AUTHENTICATION_CLIENT_SIDE, AUTH_MODULE_SERVICE_NAME);
    }

    private void captureDeviceIdMatchConfiguration() throws SSOException, SMSException {
        ServiceSchemaManager schemaManager = new ServiceSchemaManager(DEVICE_ID_SERVICE_NAME, getAdminToken());
        ServiceSchema orgSchema = schemaManager.getOrganizationSchema();
        if (orgSchema == null || orgSchema.getAttributeSchema(SERVER_SCRIPT_TYPE) == null) {
            DEBUG.message("No upgrade required for {}; no script type found.", DEVICE_ID_SERVICE_NAME);
            return;
        }
        captureServerScriptConfiguration(schemaManager.getOrganizationSchema(),
                DEVICE_ID_MATCH_SERVER_SIDE, AUTHENTICATION_SERVER_SIDE, DEVICE_ID_SERVICE_NAME);
        captureClientScriptConfiguration(schemaManager.getOrganizationSchema(),
                DEVICE_ID_MATCH_CLIENT_SIDE, AUTHENTICATION_CLIENT_SIDE, DEVICE_ID_SERVICE_NAME);
    }

    private void captureEngineConfiguration(ServiceSchema globalSchema) {
        DEBUG.message("Capture global schema attributes for {}", AUTH_MODULE_SERVICE_NAME);
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> schemaAttributes = globalSchema.getAttributeDefaults();
        for (Map.Entry<String, String> attributeEntry : globalSchemaKeys.entrySet()) {
            contextEngineConfigurations.put(attributeEntry.getValue(), schemaAttributes.get(attributeEntry.getKey()));
        }
    }

    private void captureServerScriptConfiguration(ServiceSchema orgSchema, GlobalScript globalScript,
                                                  ScriptContext context, String service) {
        DEBUG.message("Capture default server script attributes for {}", service);
        Map<String, Set<String>> attributes = new HashMap<>();
        attributes.put(SCRIPT_NAME, Collections.singleton(globalScript.getDisplayName()));
        attributes.put(SCRIPT_CONTEXT, Collections.singleton(context.name()));

        @SuppressWarnings("unchecked")
        Map<String, Set<String>> orgAttributes = orgSchema.getAttributeDefaults();
        String serverScript = getMapAttr(orgAttributes, SERVER_SIDE_SCRIPT);
        if (StringUtils.isNotEmpty(serverScript)) {
            DEBUG.message("Found default server side script for {}", service);
            attributes.put(SCRIPT_TEXT, Collections.singleton(serverScript));
        } else {
            DEBUG.message("No default server side script found for {}", service);
            attributes.put(SCRIPT_TEXT, Collections.singleton(""));
        }

        String scriptLanguage = getMapAttr(orgAttributes, SERVER_SCRIPT_TYPE);
        if (StringUtils.isBlank(scriptLanguage)) {
            scriptLanguage = SupportedScriptingLanguage.JAVASCRIPT.name();
        }
        attributes.put(SCRIPT_LANGUAGE, Collections.singleton(scriptLanguage.toUpperCase()));
        globalScriptConfigurations.put(globalScript, attributes);
    }

    private void captureClientScriptConfiguration(ServiceSchema orgSchema, GlobalScript globalScript,
                                                  ScriptContext context, String service) {
        DEBUG.message("Capture default client script attributes for {}", service);
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> orgAttributes = orgSchema.getAttributeDefaults();
        String clientScript = getMapAttr(orgAttributes, CLIENT_SIDE_SCRIPT);
        if (StringUtils.isEmpty(clientScript) && AUTH_MODULE_CLIENT_SIDE.equals(globalScript)) {
            return;
        }

        Map<String, Set<String>> attributes = new HashMap<>();
        attributes.put(SCRIPT_NAME, Collections.singleton(globalScript.getDisplayName()));
        attributes.put(SCRIPT_CONTEXT, Collections.singleton(context.name()));
        if (StringUtils.isNotEmpty(clientScript)) {
            DEBUG.message("Found default client side script for {}", service);
            attributes.put(SCRIPT_TEXT, Collections.singleton(clientScript));
        } else {
            DEBUG.message("No default client side script found for {}", service);
            attributes.put(SCRIPT_TEXT, Collections.singleton(""));
        }

        attributes.put(SCRIPT_LANGUAGE, Collections.singleton(SupportedScriptingLanguage.JAVASCRIPT.name()));
        globalScriptConfigurations.put(globalScript, attributes);
    }

    @Override
    public boolean isApplicable() {
        return !contextEngineConfigurations.isEmpty() || !globalScriptConfigurations.isEmpty();
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
        replaceObsoleteWhiteListEntries();
        String contextName = AUTHENTICATION_SERVER_SIDE.name();
        DEBUG.message("Upgrading engine configuration for script context: {}", contextName);
        UpgradeProgress.reportStart("upgrade.scripting.global.engine.start", contextName);
        ServiceConfig contextConfig = globalConfig.getSubConfig(contextName);
        ServiceConfig engineConfig = contextConfig.getSubConfig("engineConfiguration");
        engineConfig.setAttributes(contextEngineConfigurations);
        DEBUG.message("Saved engine configuration: {}", contextEngineConfigurations.toString());
        UpgradeProgress.reportEnd("upgrade.success");
    }

    private void replaceObsoleteWhiteListEntries() {
        Set<String> whiteList = contextEngineConfigurations.get(WHITE_LIST);
        if (whiteList != null) {
            if (whiteList.remove("org.forgerock.openam.authentication.modules.scripted.http.*")) {
                whiteList.add("org.forgerock.openam.scripting.api.http.GroovyHttpClient");
                whiteList.add("org.forgerock.openam.scripting.api.http.JavaScriptHttpClient");
            }
            if (whiteList.contains("org.forgerock.openam.authentication.modules.scripted.*")) {
                whiteList.add("org.forgerock.openam.scripting.api.ScriptedIdentity");
                whiteList.add("org.forgerock.openam.scripting.api.ScriptedSession");
            }
        }
    }

    private void upgradeScriptConfiguration(ServiceConfig globalConfig) throws SMSException, SSOException {
        for (Map.Entry<GlobalScript, Map<String, Set<String>>> entry : globalScriptConfigurations.entrySet()) {
            Map<String, Set<String>> attributes = entry.getValue();
            updateMetaData(attributes);
            String scriptName = getMapAttr(attributes, SCRIPT_NAME);
            DEBUG.message("Upgrading default script to global script: {}", scriptName);
            UpgradeProgress.reportStart("upgrade.scripting.global.script.start", scriptName);
            GlobalScript globalScript = entry.getKey();
            ServiceConfig scriptConfigs = globalConfig.getSubConfig("globalScripts");
            ServiceConfig globalScriptConfig = scriptConfigs.getSubConfig(globalScript.getId());
            if (AUTH_MODULE_CLIENT_SIDE.equals(globalScript)) {
                attributes.put(SCRIPT_DESCRIPTION,
                        Collections.singleton("Default global script created during upgrade."));
                scriptConfigs.addSubConfig(UUID.randomUUID().toString(), "globalScript", 0, attributes);
                DEBUG.message("Created script configuration: {}", attributes.toString());
            } else if (globalScriptConfig == null) {
                attributes.put(SCRIPT_DESCRIPTION,
                        Collections.singleton("Default global script created during upgrade."));
                scriptConfigs.addSubConfig(globalScript.getId(), "globalScript", 0, attributes);
                DEBUG.message("Created script configuration: {}", attributes.toString());
            } else {
                globalScriptConfig.setAttributes(attributes);
                DEBUG.message("Upgraded script configuration: {}", attributes.toString());
            }
            UpgradeProgress.reportEnd("upgrade.success");
        }
    }

    private void updateMetaData(Map<String, Set<String>> attributes) {
        long now = currentTimeMillis();
        String principalName = SubjectUtils.getPrincipalId(getAdminSubject());

        if (!attributes.containsKey(SCRIPT_CREATED_BY)) {
            attributes.put(SCRIPT_CREATED_BY, Collections.singleton(principalName));
            attributes.put(SCRIPT_CREATION_DATE, Collections.singleton(String.valueOf(now)));
        }
        attributes.put(SCRIPT_LAST_MODIFIED_BY, Collections.singleton(principalName));
        attributes.put(SCRIPT_LAST_MODIFIED_DATE, Collections.singleton(String.valueOf(now)));
    }

    @Override
    public String getShortReport(String delimiter) {
        StringBuilder sb = new StringBuilder();
        if (globalScriptConfigurations.size() > 0) {
            sb.append(MessageFormat.format(BUNDLE.getString("upgrade.scripting.global.settings"),
                    globalScriptConfigurations.size()));
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
        sb.append(INDENT);
        sb.append(MessageFormat.format(BUNDLE.getString("upgrade.scripting.global.context"),
                DEVICE_ID_SERVICE_NAME, SCRIPTING_SERVICE_NAME));
        sb.append(delimiter);
        tags.put("%REPORT_DATA%", sb.toString());
        return tagSwapReport(tags, "upgrade.scripting.global.report");
    }
}
