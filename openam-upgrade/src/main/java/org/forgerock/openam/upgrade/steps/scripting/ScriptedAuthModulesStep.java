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

import static com.sun.identity.shared.datastruct.CollectionHelper.getBooleanMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static org.forgerock.openam.scripting.ScriptConstants.EMPTY;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext.AUTHENTICATION_CLIENT_SIDE;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext.AUTHENTICATION_SERVER_SIDE;
import static org.forgerock.openam.scripting.ScriptConstants.getLanguageFromString;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.sm.SMSException;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.scripting.service.ScriptingService;
import org.forgerock.openam.scripting.service.ScriptingServiceFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.steps.AbstractUpgradeStep;

import javax.inject.Inject;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This upgrade step moves scripts from the Scripted Auth Modules to the Scripting Service.
 *
 * @since 13.0.0
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class ScriptedAuthModulesStep extends AbstractUpgradeStep {

    private static final String REPORT_DATA = "%REPORT_DATA%";
    private static final String CLIENT_SCRIPT = "iplanet-am-auth-scripted-client-script";
    private static final String SCRIPT_TYPE = "iplanet-am-auth-scripted-script-type";
    private static final String SERVER_SCRIPT = "iplanet-am-auth-scripted-server-script";
    private static final String CLIENT_SCRIPT_ENABLED = "iplanet-am-auth-scripted-client-script-enabled";

    private class ScriptData {
        String moduleName;
        ScriptConfiguration clientSideScript;
        ScriptConfiguration serverSideScript;
    }
    private final Map<String, Set<ScriptData>> scriptsToMove = new HashMap<>();
    private final ScriptingServiceFactory<ScriptConfiguration> serviceFactory;
    private int moduleCount = 0;

    @Inject
    public ScriptedAuthModulesStep(final PrivilegedAction<SSOToken> adminTokenAction,
                                   @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory factory,
                                   ScriptingServiceFactory<ScriptConfiguration> scriptingServiceFactory) {
        super(adminTokenAction, factory);
        this.serviceFactory = scriptingServiceFactory;
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            for (String realm : getRealmNames()) {
                captureScriptedModuleData(realm);
            }
        } catch (AMConfigurationException | ScriptException e) {
            DEBUG.error("An error occurred while trying to look for upgradable Scripted auth modules", e);
            throw new UpgradeException("Unable to retrieve Scripted auth modules", e);
        }
    }

    private void captureScriptedModuleData(String realm) throws AMConfigurationException, ScriptException {
        Set<ScriptData> scriptDataSet = new HashSet<>();
        AMAuthenticationManager authManager = new AMAuthenticationManager(getAdminToken(), realm);
        Set<AMAuthenticationInstance> instances = authManager.getAuthenticationInstances();
        for (AMAuthenticationInstance instance : instances) {
            if ("Scripted".equalsIgnoreCase(instance.getType())) {
                DEBUG.message("Found Scripted Module called {}, in realm {}", instance.getName(), realm);
                @SuppressWarnings("unchecked")
                Map<String, Set<String>> attributes = instance.getAttributeValues();
                if (attributes.containsKey(SCRIPT_TYPE)) {
                    scriptDataSet.add(getScriptData(instance.getName(), attributes));
                    moduleCount++;
                }
            }
        }
        if (!scriptDataSet.isEmpty()) {
            scriptsToMove.put(realm, scriptDataSet);
        }
    }

    private ScriptData getScriptData(String moduleName, Map<String, Set<String>> attributes) throws ScriptException {
        ScriptData scriptData = new ScriptData();
        scriptData.moduleName = moduleName;
        String serverScript = getMapAttr(attributes, SERVER_SCRIPT);
        scriptData.serverSideScript = ScriptConfiguration.builder().generateId()
                .setName("Server-Side " + moduleName)
                .setDescription("Server-Side script for Scripted Module: " + moduleName)
                .setContext(AUTHENTICATION_SERVER_SIDE)
                .setLanguage(getLanguageFromString(getMapAttr(attributes, SCRIPT_TYPE)))
                .setScript(serverScript == null ? EMPTY : serverScript).build();
        DEBUG.message("Captured server script for {}", moduleName);

        if (getBooleanMapAttr(attributes, CLIENT_SCRIPT_ENABLED, false)) {
            String clientScript = getMapAttr(attributes, CLIENT_SCRIPT);
            scriptData.clientSideScript = ScriptConfiguration.builder().generateId()
                    .setName("Client-Side " + moduleName)
                    .setDescription("Client-Side script for Scripted Module: " + moduleName)
                    .setContext(AUTHENTICATION_CLIENT_SIDE)
                    .setLanguage(SupportedScriptingLanguage.JAVASCRIPT)
                    .setScript(clientScript == null ? EMPTY : clientScript).build();
            DEBUG.message("Captured client script for {}", moduleName);
        }

        return scriptData;
    }

    @Override
    public boolean isApplicable() {
        return !scriptsToMove.isEmpty();
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            for (Map.Entry<String, Set<ScriptData>> entry : scriptsToMove.entrySet()) {
                upgradeScriptedAuthModules(entry.getKey(), entry.getValue());
            }
        } catch (AMConfigurationException | SMSException | SSOException | ScriptException e) {
            UpgradeProgress.reportEnd("upgrade.failed");
            DEBUG.error("An error occurred while trying to update Scripted auth modules", e);
            throw new UpgradeException("Unable to update Scripted auth modules", e);
        }
    }

    private void upgradeScriptedAuthModules(String realm, Set<ScriptData> scriptDataSet)
            throws AMConfigurationException, ScriptException, SSOException, SMSException {

        ScriptingService<ScriptConfiguration> service = serviceFactory.create(getAdminSubject(), realm);
        AMAuthenticationManager authManager = new AMAuthenticationManager(getAdminToken(), realm);
        for (ScriptData scriptData : scriptDataSet) {
            Map<String, Set<String>> attributes = new HashMap<>();

            UpgradeProgress.reportStart("upgrade.scripted.auth.server.script.start",
                    scriptData.serverSideScript.getName(), realm);
            service.create(scriptData.serverSideScript);
            attributes.put(SERVER_SCRIPT, Collections.singleton(scriptData.serverSideScript.getId()));
            UpgradeProgress.reportEnd("upgrade.success");

            if (scriptData.clientSideScript != null) {
                UpgradeProgress.reportStart("upgrade.scripted.auth.client.script.start",
                        scriptData.serverSideScript.getName(), realm);
                service.create(scriptData.clientSideScript);
                attributes.put(CLIENT_SCRIPT, Collections.singleton(scriptData.clientSideScript.getId()));
                UpgradeProgress.reportEnd("upgrade.success");
            } else {
                attributes.put(CLIENT_SCRIPT, Collections.singleton("[Default]"));
            }

            UpgradeProgress.reportStart("upgrade.scripted.auth.module.script.start", scriptData.moduleName, realm);
            AMAuthenticationInstance instance = authManager.getAuthenticationInstance(scriptData.moduleName);
            if (instance != null) {
                instance.setAttributeValues(attributes);
                UpgradeProgress.reportEnd("upgrade.success");
            } else {
                DEBUG.error("Scripted module {} in realm {} could not be found", scriptData.moduleName, realm);
                UpgradeProgress.reportEnd("upgrade.failed");
            }
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        StringBuilder sb = new StringBuilder();
        if (moduleCount > 0) {
            sb.append(MessageFormat.format(BUNDLE.getString("upgrade.scripted.auth.modules"), moduleCount));
            sb.append(delimiter);
        }
        return sb.toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<>();
        tags.put(LF, delimiter);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Set<ScriptData>> entry : scriptsToMove.entrySet()) {
            sb.append(MessageFormat.format(BUNDLE.getString("upgrade.scripted.auth.realm"), entry.getKey()));
            for (ScriptData scriptData : entry.getValue()) {
                sb.append(delimiter).append(INDENT);
                sb.append(MessageFormat.format(BUNDLE.getString("upgrade.scripted.auth.server.script"),
                        scriptData.serverSideScript.getName(), scriptData.moduleName));
                if (scriptData.clientSideScript != null) {
                    sb.append(delimiter).append(INDENT);
                    sb.append(MessageFormat.format(BUNDLE.getString("upgrade.scripted.auth.client.script"),
                            scriptData.clientSideScript.getName(), scriptData.moduleName));
                }
            }
            sb.append(delimiter);
        }
        tags.put(REPORT_DATA, sb.toString());
        return tagSwapReport(tags, "upgrade.scripted.auth.report");
    }
}
