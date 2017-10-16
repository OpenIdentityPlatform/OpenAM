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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.upgrade.steps;

import static com.sun.identity.authentication.util.ISAuthConstants.AUTHCONFIG_SERVICE_NAME;
import static com.sun.identity.authentication.util.ISAuthConstants.AUTH_SERVICE_NAME;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;
import static org.forgerock.openam.upgrade.UpgradeUtils.ATTR_AUTH_POST_CLASS;

import javax.inject.Inject;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.google.common.collect.ImmutableMap;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.annotations.VisibleForTesting;

/**
 * An upgrade step to update the classes of Post Authentication Plugins in authentication settings
 * and chains which have been changed.
 */
@UpgradeStepInfo
public class PostAuthenticationPluginUpgradeStep extends AbstractUpgradeStep {

    private static final String CHAIN_REPORT_TAG = "%CHAIN%";
    private static final String FROM_REPORT_TAG = "%FROM%";
    private static final String TO_REPORT_TAG = "%TO%";
    private static final String REPORT_SHORT_DESCRIPTION_KEY = "upgrade.postauthenticationplugins.short";
    private static final String REPORT_FULL_DESCRIPTION_KEY = "upgrade.postauthenticationplugins.report";
    private static final String REPORT_FULL_AUTH_SETTINGS_DESCRIPTION_KEY =
            "upgrade.postauthenticationplugins.report.auth.settings";
    private static final String REPORT_FULL_AUTH_CHAINS_HEADING_KEY =
            "upgrade.postauthenticationplugins.report.auth.chains.heading";
    private static final String REPORT_FULL_AUTH_CHAINS_DESCRIPTION_KEY =
            "upgrade.postauthenticationplugins.report.auth.chains.entry";
    private static final String REPORT_REALM_TEXT_KEY = "upgrade.realm";

    private static final String ORIGINAL_ADAPTIVE_PAP_CLASS_NAME =
            "org.forgerock.openam.authentication.modules.adaptive.Adaptive";
    private static final String NEW_ADAPTIVE_PAP_CLASS_NAME =
            "org.forgerock.openam.authentication.modules.adaptive.AdaptivePostAuthenticationPlugin";
    private static final String ORIGINAL_PERSISTENT_COOKIE_PAP_CLASS_NAME =
            "org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieAuthModule";
    private static final String NEW_PERSISTENT_COOKIE_PAP_CLASS_NAME =
            "org.forgerock.openam.authentication.modules.persistentcookie."
                    + "PersistentCookieAuthModulePostAuthenticationPlugin";

    private static final String AUTH_CHAIN_SUB_CONFIG_NAME = "Configurations";

    private final Map<String, String> postAuthPluginClassMapping = ImmutableMap.of(ORIGINAL_ADAPTIVE_PAP_CLASS_NAME,
            NEW_ADAPTIVE_PAP_CLASS_NAME, ORIGINAL_PERSISTENT_COOKIE_PAP_CLASS_NAME,
            NEW_PERSISTENT_COOKIE_PAP_CLASS_NAME);
    private final Map<String, String> realmLevelPapsToUpdate = new HashMap<>();
    private final Map<String, Set<String>> chainLevelPapsToUpdate = new HashMap<>();
    private final Map<String, Map<String, Set<String>>> chainLevelPapsUpdated = new HashMap<>();

    @Inject
    public PostAuthenticationPluginUpgradeStep(PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            for (String realm : getRealmNames()) {
                ServiceConfig organizationConfig = getAuthSettingsServiceConfig(realm);
                Set<String> postAuthPluginClassName = getPostAuthPluginClasses(organizationConfig);
                if (postAuthPluginClassName != null
                        && !Collections.disjoint(postAuthPluginClassMapping.keySet(), postAuthPluginClassName)) {
                    realmLevelPapsToUpdate.put(realm, CollectionUtils.getFirstItem(postAuthPluginClassName));
                }
            }

            for (String realm : getRealmNames()) {
                ServiceConfig organizationConfig = getAuthChainServiceConfig(realm)
                        .getSubConfig(AUTH_CHAIN_SUB_CONFIG_NAME);
                Set<String> chainsToUpdate = chainLevelPapsToUpdate.get(realm);
                for (String name : organizationConfig.getSubConfigNames()) {
                    Set<String> postAuthPluginClassNames =
                            getPostAuthPluginClasses(organizationConfig.getSubConfig(name));
                    if (postAuthPluginClassNames != null
                            && !Collections.disjoint(postAuthPluginClassMapping.keySet(), postAuthPluginClassNames)) {
                        if (chainsToUpdate == null) {
                            chainsToUpdate = new HashSet<>();
                            chainLevelPapsToUpdate.put(realm, chainsToUpdate);
                        }
                        chainsToUpdate.add(name);
                    }
                }
            }
        } catch (SMSException | SSOException e) {
            throw new UpgradeException(e);
        }
    }

    @Override
    public boolean isApplicable() {
        return !realmLevelPapsToUpdate.isEmpty() || !chainLevelPapsToUpdate.isEmpty();
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            for (String realm : realmLevelPapsToUpdate.keySet()) {
                ServiceConfig organizationConfig = getAuthSettingsServiceConfig(realm);
                Set<String> attr = getPostAuthPluginClasses(organizationConfig);
                organizationConfig.setAttributes(Collections.singletonMap(ATTR_AUTH_POST_CLASS,
                        mapPostAuthPluginClasses(attr)));
            }

            for (Map.Entry<String, Set<String>> entry : chainLevelPapsToUpdate.entrySet()) {
                ServiceConfig organizationConfig = getAuthChainServiceConfig(entry.getKey())
                        .getSubConfig(AUTH_CHAIN_SUB_CONFIG_NAME);

                Map<String, Set<String>> realmChainPapsUpdated = chainLevelPapsUpdated.get(entry.getKey());
                if (realmChainPapsUpdated == null) {
                    realmChainPapsUpdated = new HashMap<>();
                    chainLevelPapsUpdated.put(entry.getKey(), realmChainPapsUpdated);
                }
                for (String chainName : entry.getValue()) {
                    ServiceConfig subConfig = organizationConfig.getSubConfig(chainName);
                    Set<String> attrs = getPostAuthPluginClasses(subConfig);
                    subConfig.setAttributes(Collections.singletonMap(ATTR_AUTH_POST_CLASS,
                            mapPostAuthPluginClasses(attrs)));
                    realmChainPapsUpdated.put(chainName, attrs);
                }
            }
        } catch (SMSException | SSOException e) {
            throw new UpgradeException(e);
        }
    }

    @VisibleForTesting
    ServiceConfig getAuthSettingsServiceConfig(String realm) throws SSOException, SMSException {
        return new ServiceConfigManager(AUTH_SERVICE_NAME, getAdminToken()).getOrganizationConfig(realm, null);
    }

    @VisibleForTesting
    ServiceConfig getAuthChainServiceConfig(String realm) throws SSOException, SMSException {
        return new ServiceConfigManager(AUTHCONFIG_SERVICE_NAME, getAdminToken()).getOrganizationConfig(realm, null);
    }

    private Set<String> getPostAuthPluginClasses(ServiceConfig config) {
        return (Set<String>) config.getAttributesWithoutDefaults().get(ATTR_AUTH_POST_CLASS);
    }

    private Set<String> mapPostAuthPluginClasses(Set<String> originalPapClasses) {
        if (originalPapClasses.isEmpty()) {
            return originalPapClasses;
        }
        Set<String> upgradedPapClasses = new HashSet<>();
        for (String papClass : originalPapClasses) {
            String upgradedPapClass = postAuthPluginClassMapping.get(papClass);
            if (upgradedPapClass != null) {
                upgradedPapClasses.add(upgradedPapClass);
            } else {
                upgradedPapClasses.add(papClass);
            }
        }
        return upgradedPapClasses;
    }

    @Override
    public String getShortReport(String delimiter) {
        int postAuthPluginsChangeCount = realmLevelPapsToUpdate.size();
        for (Map.Entry<String, Set<String>> entry : chainLevelPapsToUpdate.entrySet()) {
            postAuthPluginsChangeCount += entry.getValue().size();
        }
        return BUNDLE.getString(REPORT_SHORT_DESCRIPTION_KEY) + " (" + postAuthPluginsChangeCount + ")" + delimiter;
    }

    @Override
    public String getDetailedReport(String delimiter) {
        StringBuilder sb = new StringBuilder();
        Map<String, String> tags = new HashMap<>();
        tags.put(LF, delimiter);
        return sb.append(tagSwapReport(tags, REPORT_FULL_DESCRIPTION_KEY))
                .append(authSettingsReport(delimiter))
                .append(authChainReport(delimiter))
                .append(delimiter).toString();
    }

    private String authSettingsReport(String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : realmLevelPapsToUpdate.entrySet()) {
            sb.append(BUNDLE.getString(REPORT_REALM_TEXT_KEY)).append(": ").append(entry.getKey()).append(delimiter);
            Map<String, String> tags = new HashMap<>();
            tags.put(LF, delimiter);
            tags.put(FROM_REPORT_TAG, entry.getValue());
            tags.put(TO_REPORT_TAG, postAuthPluginClassMapping.get(entry.getValue()));
            sb.append(INDENT).append(tagSwapReport(tags, REPORT_FULL_AUTH_SETTINGS_DESCRIPTION_KEY))
                    .append(delimiter);
        }
        return sb.toString();
    }

    private String authChainReport(String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, Set<String>>> realmEntry : chainLevelPapsUpdated.entrySet()) {
            sb.append(BUNDLE.getString(REPORT_REALM_TEXT_KEY)).append(": ").append(realmEntry.getKey())
                    .append(delimiter);

            for (Map.Entry<String, Set<String>> papEntry : realmEntry.getValue().entrySet()) {
                Map<String, String> tags = new HashMap<>();
                tags.put(LF, delimiter);
                tags.put(CHAIN_REPORT_TAG, papEntry.getKey());
                sb.append(INDENT)
                        .append(tagSwapReport(tags, REPORT_FULL_AUTH_CHAINS_HEADING_KEY))
                        .append(delimiter);
                for (String className : papEntry.getValue()) {
                    sb.append(authChainEntryReport(delimiter, className));
                }
            }
            sb.append(delimiter);
        }
        return sb.toString();
    }

    private String authChainEntryReport(String delimiter, String className) {
        StringBuilder sb = new StringBuilder();
        Map<String, String> tags = new HashMap<>();
        tags.put(LF, delimiter);
        tags.put(FROM_REPORT_TAG, className);
        tags.put(TO_REPORT_TAG, postAuthPluginClassMapping.get(className));
        sb.append(INDENT).append(tagSwapReport(tags, REPORT_FULL_AUTH_CHAINS_DESCRIPTION_KEY));
        return sb.toString();
    }
}
