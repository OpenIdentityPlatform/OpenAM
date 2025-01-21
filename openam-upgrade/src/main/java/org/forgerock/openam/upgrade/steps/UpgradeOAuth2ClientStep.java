/*
 * Copyright 2013-2016 ForgeRock AS.
 *
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
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.upgrade.steps;

import static org.forgerock.openam.oauth2.OAuth2Constants.OAuth2Client.*;
import static org.forgerock.openam.upgrade.UpgradeServices.*;
import static org.forgerock.openam.upgrade.steps.UpgradeOAuth2ProviderStep.*;

import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.VersionUtils;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * This upgrade step discovers first OAuth2 Client profiles available in OpenAM (across the different realms), and then
 * checks if the agent profile is using pre 10.2.0 format for storing the configuration. Upgradable OAuth2
 * configurations will be modified to use the new configuration format (i.e. [0]=value). This will ensure that the
 * configuration export functionality will work correctly, as well as the OAuth2 functionality will have its
 * settings in the expected format.
 *
 * @author Peter Major
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class UpgradeOAuth2ClientStep extends AbstractUpgradeStep {

    private static final String OAUTH2_DATA = "%OAUTH2_DATA%";
    public static final List<String> CHANGED_PROPERTIES = Arrays.asList(
            REDIRECT_URI, SCOPES, DEFAULT_SCOPES, NAME, DESCRIPTION, POST_LOGOUT_URI, CLIENT_NAME);
    public static final List<String> ADDED_LIFETIME_PROPERTIES = Arrays.asList(
            AUTHORIZATION_CODE_LIFE_TIME, ACCESS_TOKEN_LIFE_TIME, REFRESH_TOKEN_LIFE_TIME, JWT_TOKEN_LIFE_TIME);
    private static final Pattern pattern = Pattern.compile("\\[\\d+\\]=.*");
    private final Map<String, Map<AgentType, Map<String, Set<String>>>> upgradableConfigs =
            new HashMap<String, Map<AgentType, Map<String, Set<String>>>>();
    private static final int AM_13 = 1300;
    
    @Inject
    public UpgradeOAuth2ClientStep(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory factory) {
        super(adminTokenAction, factory);
    }

    @Override
    public boolean isApplicable() {
        return !upgradableConfigs.isEmpty();
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            ServiceConfigManager scm = new ServiceConfigManager(IdConstants.AGENT_SERVICE, getAdminToken());
            for (String realm : getRealmNames()) {
                findUpgradableConfigs(realm, scm, AgentType.AGENT);
                findUpgradableConfigs(realm, scm, AgentType.GROUP);
            }
        } catch (Exception ex) {
            DEBUG.error("An error occurred while trying to look for upgradable OAuth2 client profiles", ex);
            throw new UpgradeException("Unable to retrieve modified OAuth2 clients");
        }
    }

    private void findUpgradableConfigs(String realm, ServiceConfigManager scm, AgentType type)
            throws SMSException, SSOException {
        ServiceConfig serviceConfig = null;
        try {
            serviceConfig = scm.getOrganizationConfig(realm, type.instanceName);
        } catch (SMSException smse) {
            if ("sms-no-such-instance".equals(smse.getErrorCode()) && AgentType.GROUP.equals(type)) {
                //this case may be possible in scenarios where agentgroup isn't setup in the configuration
                DEBUG.message("Unable to find agentgroup in the configuration: " + smse.getMessage());
                return;
            } else {
                //this is not expected, let's rethrow it
                throw smse;
            }
        }
        Set<String> subConfigNames = serviceConfig.getSubConfigNames("*", AgentConfiguration.AGENT_TYPE_OAUTH2);
        Map<AgentType, Map<String, Set<String>>> map = upgradableConfigs.get(realm);
        if (map == null) {
            map = new EnumMap<AgentType, Map<String, Set<String>>>(AgentType.class);
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("OAuth2 " + type.name() + " configurations found under realm: " + realm + " : "
                    + subConfigNames);
        }
        for (String subConfig : subConfigNames) {
            ServiceConfig oauth2Config = serviceConfig.getSubConfig(subConfig);
            Map<String, Set<String>> attrs = oauth2Config.getAttributesWithoutDefaults();
            for (Map.Entry<String, Set<String>> entry : attrs.entrySet()) {
                final String attrName = entry.getKey();
                if (CHANGED_PROPERTIES.contains(attrName)) {
                    
                    // Check if single string scopes are included in the Scope(s) or Default Scope(s).
                    Set<String> scopes = attrs.get(attrName);
                    if (VersionUtils.isCurrentVersionLessThan(AM_13, true) && (SCOPES.equals(attrName) || DEFAULT_SCOPES.equals(attrName))) {
                        for (String scope : scopes) {
                            if (!scope.contains("|")) {
                                addAttributeToMap(map, type, subConfig, attrName, realm);
                                break;
                            }
                        }
                    }
                    
                    String value = CollectionHelper.getMapAttr(attrs, attrName);
                    if (value == null) {
                        //this doesn't prove anything, let's advance to the next property
                        continue;
                    } else {
                        if (!pattern.matcher(value).matches()) {
                            if (DEBUG.messageEnabled()) {
                                DEBUG.message("Discovered OAuth2 " + type.name() + ": " + subConfig + " in realm: "
                                        + realm);
                            }
                            addAttributeToMap(map, type, subConfig, attrName, realm);
                        }
                    }
                } else if (IDTOKEN_SIGNED_RESPONSE_ALG.equals(attrName)) {
                    final String value = CollectionHelper.getMapAttr(attrs, attrName);
                    if (ALGORITHM_NAMES.containsKey(value)) {
                        addAttributeToMap(map, type, subConfig, attrName, realm);
                    }
                }
            }
            attrs = oauth2Config.getAttributes();
            for (String addedLifetimeProps : ADDED_LIFETIME_PROPERTIES) {
                if (!attrs.containsKey(addedLifetimeProps)) {
                    addAttributeToMap(map, type, subConfig, addedLifetimeProps, realm);
                }
            }
            if (!attrs.containsKey(SUBJECT_TYPE)) {
                addAttributeToMap(map, type, subConfig, SUBJECT_TYPE, realm);
            }
        }
    }

    private void addAttributeToMap(Map<AgentType, Map<String, Set<String>>> map, AgentType type, String subConfig,
                                   String attrName, String realm) {

        Map<String, Set<String>> configurations = map.get(type);
        if (configurations == null) {
            configurations = new HashMap<String, Set<String>>();
            configurations.put(subConfig, new HashSet<String>());
            map.put(type, configurations);
        } else if (!configurations.containsKey(subConfig)) {
            configurations.put(subConfig, new HashSet<String>());
        }
        configurations.get(subConfig).add(attrName);
        upgradableConfigs.put(realm, map);
    }

    @Override
    public void perform() throws UpgradeException {
        for (Map.Entry<String, Map<AgentType, Map<String, Set<String>>>> entry : upgradableConfigs.entrySet()) {
            String realm = entry.getKey();
            try {
                ServiceConfigManager scm = new ServiceConfigManager(IdConstants.AGENT_SERVICE, getAdminToken());
                for (Map.Entry<AgentType, Map<String, Set<String>>> changes : entry.getValue().entrySet()) {
                    AgentType type = changes.getKey();
                    ServiceConfig sc = scm.getOrganizationConfig(realm, type.instanceName);
                    for (Map.Entry<String, Set<String>> subConfig : changes.getValue().entrySet()) {
                        UpgradeProgress.reportStart("upgrade.oauth2.start", subConfig.getKey());
                        ServiceConfig oauth2Config = sc.getSubConfig(subConfig.getKey());
                        Map<String, Set<String>> attrs = oauth2Config.getAttributesWithoutDefaults();
                        for (String attrName : subConfig.getValue()) {
                            if (CHANGED_PROPERTIES.contains(attrName)) {
                                Set<String> values = attrs.get(attrName);
                                
                                // If single string scopes are included in the Scope(s) or Default Scope(s), then apend a pipe.
                                if (VersionUtils.isCurrentVersionLessThan(AM_13, true) && (SCOPES.equals(attrName) || DEFAULT_SCOPES.equals(attrName))) {
                                    addScopesWithPipe(attrs, attrName, values);
                                }
                                
                                String value = CollectionHelper.getMapAttr(attrs, attrName);
                                if (value != null) {
                                    if (!pattern.matcher(value).matches()) {
                                        if (values != null) {
                                            attrs.put(attrName, convertValues(values));
                                        }
                                    }
                                }
                            } else if (IDTOKEN_SIGNED_RESPONSE_ALG.equals(attrName)) {
                                String value = CollectionHelper.getMapAttr(attrs, attrName);
                                if (ALGORITHM_NAMES.containsKey(value)) {
                                    attrs.put(attrName, Collections.singleton(ALGORITHM_NAMES.get(value)));
                                }
                            } else if (ADDED_LIFETIME_PROPERTIES.contains(attrName)) {
                                attrs.put(attrName, Collections.singleton("0"));
                            } else if (SUBJECT_TYPE.contains(attrName)) {
                                attrs.put(attrName, Collections.singleton("Public"));
                            }
                        }
                        oauth2Config.setAttributes(attrs);
                        UpgradeProgress.reportEnd("upgrade.success");
                    }
                }
            } catch (Exception ex) {
                UpgradeProgress.reportEnd("upgrade.failed");
                DEBUG.error("An error occurred while trying to upgrade an OAuth2 client", ex);
                throw new UpgradeException("Unable to upgrade OAuth2 client");
            }
        }
    }

    private void addScopesWithPipe(Map<String, Set<String>> attrs, String attrName, Set<String> scopes) {
        boolean isChanged = false;
        Set<String> replacedScopes = new HashSet<String>();
        for (String scope : scopes) {
            if (!scope.contains("|")) {
                scope = scope + "|";
                isChanged = true;
            }
            replacedScopes.add(scope);
        }
        if (isChanged) {
            attrs.put(attrName, replacedScopes);
        }

    }

    private Set<String> convertValues(Set<String> values) {
        int counter = 0;
        Set<String> newValues = new HashSet<String>(values.size());
        for (String value : values) {
            newValues.add("[" + counter++ + "]=" + value);
        }
        return newValues;
    }

    @Override
    public String getShortReport(String delimiter) {
        int agentCount = 0;
        int groupCount = 0;
        for (Map<AgentType, Map<String, Set<String>>> entry : upgradableConfigs.values()) {
            Map<String, Set<String>> tmp = entry.get(AgentType.AGENT);
            if (tmp != null) {
                agentCount += tmp.size();
            }
            tmp = entry.get(AgentType.GROUP);
            if (tmp != null) {
                groupCount += tmp.size();
            }
        }
        StringBuilder sb = new StringBuilder();
        if (agentCount != 0) {
            sb.append(BUNDLE.getString("upgrade.oauth2.clients")).append(" (").append(agentCount).append(')')
                    .append(delimiter);
        }
        if (groupCount != 0) {
            sb.append(BUNDLE.getString("upgrade.oauth2.groups")).append(" (").append(groupCount).append(')')
                    .append(delimiter);

        }
        return sb.toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<AgentType, Map<String, Set<String>>>> entry : upgradableConfigs.entrySet()) {
            sb.append(BUNDLE.getString("upgrade.realm")).append(": ").append(entry.getKey()).append(delimiter);
            for (Map.Entry<AgentType, Map<String, Set<String>>> changes : entry.getValue().entrySet()) {
                sb.append(INDENT).append(changes.getKey()).append(delimiter);
                for (String subConfig : changes.getValue().keySet()) {
                    sb.append(INDENT).append(INDENT).append(subConfig).append(delimiter);
                }
            }
        }
        tags.put(OAUTH2_DATA, sb.toString());
        return tagSwapReport(tags, "upgrade.oauth2report");
    }

    private enum AgentType {

        AGENT("upgrade.client", null),
        GROUP("upgrade.group", "agentgroup");
        private String i18nKey;
        private String instanceName;

        private AgentType(String i18nKey, String instanceName) {
            this.i18nKey = i18nKey;
            this.instanceName = instanceName;
        }

        @Override
        public String toString() {
            return BUNDLE.getString(i18nKey);
        }
    }
}
