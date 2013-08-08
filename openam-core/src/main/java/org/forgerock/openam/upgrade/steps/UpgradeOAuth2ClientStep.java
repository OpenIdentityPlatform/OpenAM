/*
 * Copyright 2013 ForgeRock AS.
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
 */
package org.forgerock.openam.upgrade.steps;

import com.iplanet.sso.SSOException;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.idm.IdConstants;
import static com.sun.identity.shared.OAuth2Constants.OAuth2Client.*;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import static org.forgerock.openam.upgrade.steps.AbstractUpgradeStep.DEBUG;

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
            REDIRECT_URI, SCOPES, DEFAULT_SCOPES, NAME, DESCRIPTION);
    private static final Pattern pattern = Pattern.compile("\\[\\d+\\]=.*");
    private final Map<String, Map<AgentType, Set<String>>> upgradableConfigs =
            new HashMap<String, Map<AgentType, Set<String>>>();

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
        ServiceConfig serviceConfig = scm.getOrganizationConfig(realm, type.instanceName);
        Set<String> subConfigNames = serviceConfig.getSubConfigNames("*", AgentConfiguration.AGENT_TYPE_OAUTH2);
        Map<AgentType, Set<String>> map = upgradableConfigs.get(realm);
        if (map == null) {
            map = new EnumMap<AgentType, Set<String>>(AgentType.class);
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("OAuth2 " + type.name() + " configurations found under realm: " + realm + " : "
                    + subConfigNames);
        }
        for (String subConfig : subConfigNames) {
            ServiceConfig oauth2Config = serviceConfig.getSubConfig(subConfig);
            Map<String, Set<String>> attrs = oauth2Config.getAttributesWithoutDefaults();
            for (Map.Entry<String, Set<String>> entry : attrs.entrySet()) {
                if (CHANGED_PROPERTIES.contains(entry.getKey())) {
                    String value = CollectionHelper.getMapAttr(attrs, entry.getKey());
                    if (value == null) {
                        //this doesn't prove anything, let's advance to the next property
                        continue;
                    } else {
                        if (!pattern.matcher(value).matches()) {
                            if (DEBUG.messageEnabled()) {
                                DEBUG.message("Discovered OAuth2 " + type.name() + ": " + subConfig + " in realm: "
                                        + realm);
                            }
                            Set<String> values = map.get(type);
                            if (values == null) {
                                values = new HashSet<String>();
                            }
                            values.add(subConfig);
                            map.put(type, values);
                            upgradableConfigs.put(realm, map);
                            //we have detected that this config needs to be upgraded, so let's break this loop
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void perform() throws UpgradeException {
        for (Map.Entry<String, Map<AgentType, Set<String>>> entry : upgradableConfigs.entrySet()) {
            String realm = entry.getKey();
            try {
                ServiceConfigManager scm = new ServiceConfigManager(IdConstants.AGENT_SERVICE, getAdminToken());
                for (Map.Entry<AgentType, Set<String>> changes : entry.getValue().entrySet()) {
                    AgentType type = changes.getKey();
                    ServiceConfig sc = scm.getOrganizationConfig(realm, type.instanceName);
                    for (String subConfig : changes.getValue()) {
                        UpgradeProgress.reportStart("upgrade.oauth2.start", subConfig);
                        ServiceConfig oauth2Config = sc.getSubConfig(subConfig);
                        Map<String, Set<String>> attrs = oauth2Config.getAttributesWithoutDefaults();
                        for (String propertyName : CHANGED_PROPERTIES) {
                            Set<String> values = attrs.get(propertyName);
                            if (values != null) {
                                attrs.put(propertyName, convertValues(values));
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
        for (Map<AgentType, Set<String>> entry : upgradableConfigs.values()) {
            Set<String> tmp = entry.get(AgentType.AGENT);
            if (tmp != null)
                agentCount += tmp.size();
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
        for (Map.Entry<String, Map<AgentType, Set<String>>> entry : upgradableConfigs.entrySet()) {
            sb.append(BUNDLE.getString("upgrade.realm")).append(": ").append(entry.getKey()).append(delimiter);
            for (Map.Entry<AgentType, Set<String>> changes : entry.getValue().entrySet()) {
                sb.append(INDENT).append(changes.getKey()).append(delimiter);
                for (String subConfig : changes.getValue()) {
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
