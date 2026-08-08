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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openam.upgrade.steps.scripting;

import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;

import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.steps.AbstractUpgradeStep;
import org.forgerock.openam.upgrade.steps.UpgradeServiceUtils;
import org.forgerock.openam.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.CreateServiceConfig;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSUtils;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceNotFoundException;

/**
 * This upgrade step ensures that any global sub-configurations defined in scripting.xml (script contexts with
 * their engine configurations, and the default global scripts) that are missing from the configuration data store
 * are added to it. New script contexts are only registered by the SMS when the whole service is new, so instances
 * upgraded from a version that already contained the Scripting Service would otherwise be left without the
 * configuration for contexts introduced in later versions (see OPENAM issue #1103).
 * <p>
 * The step only creates missing sub-configurations; attributes of existing sub-configurations are never
 * reconciled with the service definition, so changes to e.g. an existing engine configuration whitelist do not
 * reach upgraded instances.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class UpgradeScriptingSubConfigsStep extends AbstractUpgradeStep {

    private static final String SCRIPTING_SERVICE_NAME = "ScriptingService";
    private static final String NAME = "name";
    private static final String ID = "id";
    private static final String PRIORITY = "priority";
    private static final String AUDIT_REPORT = "upgrade.scripting.subconfigs.report";
    private static final String AUDIT_NEW_SUB_CONFIGS = "upgrade.scripting.subconfigs";
    private static final String AUDIT_NEW_SUB_CONFIG = "upgrade.scripting.subconfigs.new";
    private static final String AUDIT_NEW_SUB_CONFIG_START = "upgrade.scripting.subconfigs.new.start";

    private final List<MissingSubConfig> missingSubConfigs = new ArrayList<>();
    /**
     * Display names of every sub-configuration that will be created, including descendants of the missing nodes.
     * Only used for reporting; {@link #perform()} creates descendants by recursing into the missing nodes.
     */
    private final List<String> reportedSubConfigs = new ArrayList<>();

    @Inject
    public UpgradeScriptingSubConfigsStep(PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            ServiceConfig globalConfig = getScriptingGlobalConfig();
            if (globalConfig == null) {
                DEBUG.message("No global configuration found for {}. Nothing to upgrade", SCRIPTING_SERVICE_NAME);
                return;
            }
            Node globalConfigNode = getGlobalConfigurationNode(getScriptingServiceXML());
            if (globalConfigNode == null) {
                DEBUG.message("No global configuration defined in the {} service definition", SCRIPTING_SERVICE_NAME);
                return;
            }
            captureMissingSubConfigs(globalConfigNode, globalConfig, new ArrayList<String>());
        } catch (ServiceNotFoundException e) {
            DEBUG.message("Scripting service not found. Nothing to upgrade", e);
        } catch (SMSException | SSOException e) {
            DEBUG.error("An error occurred while looking for missing Scripting Service configurations", e);
            throw new UpgradeException("Unable to detect missing Scripting Service configurations", e);
        }
    }

    private Node getGlobalConfigurationNode(Document scriptingDocument) {
        for (Iterator it = XMLUtils.getChildNodes(scriptingDocument.getDocumentElement(), SMSUtils.SERVICE).iterator();
                it.hasNext();) {
            Node serviceNode = (Node) it.next();
            if (SCRIPTING_SERVICE_NAME.equals(XMLUtils.getNodeAttributeValue(serviceNode, NAME))) {
                Node configurationNode = XMLUtils.getChildNode(serviceNode, SMSUtils.CONFIGURATION);
                return configurationNode == null ? null : XMLUtils.getChildNode(configurationNode,
                        SMSUtils.GLOBAL_CONFIG);
            }
        }
        return null;
    }

    private void captureMissingSubConfigs(Node parentNode, ServiceConfig parentConfig, List<String> parentPath)
            throws SMSException, SSOException {
        for (Iterator it = XMLUtils.getChildNodes(parentNode, SMSUtils.SUB_CONFIG).iterator(); it.hasNext();) {
            Node node = (Node) it.next();
            String name = XMLUtils.getNodeAttributeValue(node, NAME);
            // For an absent entry getSubConfig may return a non-null config wrapping a non-existent SMSEntry
            // when the sub-config name matches a sub-schema name (e.g. engineConfiguration, globalScripts),
            // so presence must be determined via ServiceConfig#exists.
            ServiceConfig existingConfig = parentConfig.getSubConfig(name);
            if (!SMSUtils.serviceExists(existingConfig)) {
                MissingSubConfig missing = new MissingSubConfig(new ArrayList<>(parentPath), name, node);
                missingSubConfigs.add(missing);
                reportSubConfigTree(missing.getDisplayName(), node);
            } else {
                parentPath.add(name);
                captureMissingSubConfigs(node, existingConfig, parentPath);
                parentPath.remove(parentPath.size() - 1);
            }
        }
    }

    private void reportSubConfigTree(String displayName, Node node) {
        reportedSubConfigs.add(displayName);
        for (Iterator it = XMLUtils.getChildNodes(node, SMSUtils.SUB_CONFIG).iterator(); it.hasNext();) {
            Node child = (Node) it.next();
            reportSubConfigTree(displayName + '/' + XMLUtils.getNodeAttributeValue(child, NAME), child);
        }
    }

    @Override
    public boolean isApplicable() {
        return !missingSubConfigs.isEmpty();
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            ServiceConfig globalConfig = getScriptingGlobalConfig();
            for (MissingSubConfig missing : missingSubConfigs) {
                UpgradeProgress.reportStart(AUDIT_NEW_SUB_CONFIG_START, missing.name);
                ServiceConfig parentConfig = globalConfig;
                for (String parentName : missing.parentPath) {
                    parentConfig = parentConfig.getSubConfig(parentName);
                    if (parentConfig == null) {
                        throw new UpgradeException("Missing parent configuration for " + missing.getDisplayName());
                    }
                }
                // The missing list was captured in initialize(); the entry may have been created since.
                if (SMSUtils.serviceExists(parentConfig.getSubConfig(missing.name))) {
                    DEBUG.message("Scripting Service configuration {} already exists, skipping", missing.name);
                } else {
                    addSubConfig(parentConfig, missing.node);
                }
                UpgradeProgress.reportEnd("upgrade.success");
            }
        } catch (UpgradeException e) {
            UpgradeProgress.reportEnd("upgrade.failed");
            throw e;
        } catch (Exception e) {
            UpgradeProgress.reportEnd("upgrade.failed");
            DEBUG.error("An error occurred while adding missing Scripting Service configurations", e);
            throw new UpgradeException("Unable to add missing Scripting Service configurations", e);
        }
    }

    private void addSubConfig(ServiceConfig parentConfig, Node node) throws SMSException, SSOException {
        String name = XMLUtils.getNodeAttributeValue(node, NAME);
        String id = XMLUtils.getNodeAttributeValue(node, ID);
        if (StringUtils.isEmpty(id)) {
            id = name;
        }
        String priority = XMLUtils.getNodeAttributeValue(node, PRIORITY);
        Map<String, Set<String>> attributes = CreateServiceConfig.getAttributeValuePairs(node);
        parentConfig.addSubConfig(name, id, priority == null ? 0 : Integer.parseInt(priority),
                attributes == null ? Collections.<String, Set<String>>emptyMap() : attributes);
        DEBUG.message("Created Scripting Service configuration {} with id {}", name, id);

        Iterator children = XMLUtils.getChildNodes(node, SMSUtils.SUB_CONFIG).iterator();
        if (children.hasNext()) {
            ServiceConfig createdConfig = parentConfig.getSubConfig(name);
            while (children.hasNext()) {
                addSubConfig(createdConfig, (Node) children.next());
            }
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        StringBuilder sb = new StringBuilder();
        if (!reportedSubConfigs.isEmpty()) {
            sb.append(MessageFormat.format(BUNDLE.getString(AUDIT_NEW_SUB_CONFIGS), reportedSubConfigs.size()));
            sb.append(delimiter);
        }
        return sb.toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<>();
        tags.put(LF, delimiter);

        StringBuilder sb = new StringBuilder();
        for (String displayName : reportedSubConfigs) {
            sb.append(INDENT);
            sb.append(MessageFormat.format(BUNDLE.getString(AUDIT_NEW_SUB_CONFIG), displayName));
            sb.append(delimiter);
        }
        tags.put("%REPORT_DATA%", sb.toString());
        return tagSwapReport(tags, AUDIT_REPORT);
    }

    /**
     * Get the Scripting Service definition XML with all tags swapped for their configured values.
     * @return The Scripting Service DOM model.
     * @throws UpgradeException When the service XML cannot be loaded.
     */
    protected Document getScriptingServiceXML() throws UpgradeException {
        Document document = UpgradeServiceUtils.getServiceDefinitions(getAdminToken()).get(SCRIPTING_SERVICE_NAME);
        if (document == null) {
            throw new UpgradeException("Unable to find the Scripting Service definition");
        }
        return document;
    }

    /**
     * Get the global configuration of the Scripting Service from the configuration data store.
     * @return The global Scripting Service configuration.
     * @throws SMSException When the Scripting Service is not available.
     * @throws SSOException When the admin token is not valid.
     */
    protected ServiceConfig getScriptingGlobalConfig() throws SMSException, SSOException {
        return new ServiceConfigManager(SCRIPTING_SERVICE_NAME, getAdminToken()).getGlobalConfig(null);
    }

    private static final class MissingSubConfig {

        private final List<String> parentPath;
        private final String name;
        private final Node node;

        private MissingSubConfig(List<String> parentPath, String name, Node node) {
            this.parentPath = parentPath;
            this.name = name;
            this.node = node;
        }

        private String getDisplayName() {
            StringBuilder sb = new StringBuilder();
            for (String parentName : parentPath) {
                sb.append(parentName).append('/');
            }
            return sb.append(name).toString();
        }
    }
}
