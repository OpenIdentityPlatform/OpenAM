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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.upgrade.steps;

import static org.forgerock.openam.oauth2.OAuth2Constants.OAuth2ProviderService.*;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import jakarta.inject.Inject;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSUtils;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceNotFoundException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.utils.MappingUtils;

/**
 * This upgrade step will find all the OAuth2 Providers that was created with a subset of the available attributes.
 * It will then add the missing attributes with their default values and persist the provider's attributes.
 * This needs to be done before changes can be made to the OAuth2 Provider Schema to ensure that the existing
 * providers does not automatically inherit the changes made to the schema.
 *
 * @since 12.0.0
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class UpgradeOAuth2ProviderStep extends AbstractUpgradeStep {

    public static final Map<String, String> ALGORITHM_NAMES = new HashMap<String, String>();
    public static final Set<String> DEFAULT_CLAIMS = new HashSet<String>();
    private static final Map<String, String> RESPONSE_TYPE_PLUGINS_UPGRADE_MAPPINGS = new HashMap<>();
    private static final String OLD_SCOPE_PLUGIN = "org.forgerock.openam.oauth2.provider.impl.ScopeImpl";
    private static final String NEW_SCOPE_PLUGIN = "org.forgerock.openam.oauth2.OpenAMScopeValidator";

    static {
        ALGORITHM_NAMES.put(JwsAlgorithm.HS256.getAlgorithm(), JwsAlgorithm.HS256.name());
        ALGORITHM_NAMES.put(JwsAlgorithm.HS384.getAlgorithm(), JwsAlgorithm.HS384.name());
        ALGORITHM_NAMES.put(JwsAlgorithm.HS512.getAlgorithm(), JwsAlgorithm.HS512.name());

        DEFAULT_CLAIMS.add("email");
        DEFAULT_CLAIMS.add("address");
        DEFAULT_CLAIMS.add("phone_number");
        DEFAULT_CLAIMS.add("given_name");
        DEFAULT_CLAIMS.add("zoneinfo");
        DEFAULT_CLAIMS.add("family_name");
        DEFAULT_CLAIMS.add("locale");
        DEFAULT_CLAIMS.add("name");

        RESPONSE_TYPE_PLUGINS_UPGRADE_MAPPINGS.put(
                "org.forgerock.restlet.ext.oauth2.flow.responseTypes.TokenResponseType",
                "org.forgerock.oauth2.core.TokenResponseTypeHandler");
        RESPONSE_TYPE_PLUGINS_UPGRADE_MAPPINGS.put(
                "org.forgerock.restlet.ext.oauth2.flow.responseTypes.CodeResponseType",
                "org.forgerock.oauth2.core.AuthorizationCodeResponseTypeHandler");
        RESPONSE_TYPE_PLUGINS_UPGRADE_MAPPINGS.put(
                "org.forgerock.restlet.ext.oauth2.flow.responseTypes.IDTokenResponseType",
                "org.forgerock.openidconnect.IdTokenResponseTypeHandler");
    }

    private static final String REPORT_DATA = "%REPORT_DATA%";
    private static final String OAUTH2_PROVIDER = "OAuth2Provider";

    private final Map<String, Map<String, Set<String>>> attributesToUpdate =
            new HashMap<String, Map<String, Set<String>>>();
    private ServiceConfigManager scm;
    private ServiceSchemaManager ssm;

    @Inject
    public UpgradeOAuth2ProviderStep(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory factory) {
        super(adminTokenAction, factory);
    }

    @Override
    public boolean isApplicable() {
        return !attributesToUpdate.isEmpty();
    }

    @Override
    public void initialize() throws UpgradeException {
        final SSOToken token = getAdminToken();
        try {
            scm = new ServiceConfigManager(OAUTH2_PROVIDER, token);
            ssm = new ServiceSchemaManager(OAUTH2_PROVIDER, token);
            findUpgradableProviders();
        } catch (ServiceNotFoundException e) {
            //When upgrading from 10.0.x and before there is no OAuth2 service, so we expect this exception in this case
            DEBUG.message("OAuth2Provider service not found. Nothing to upgrade", e);
        } catch (Exception e) {
            DEBUG.error("An error occurred while trying to create Service Config and Schema Managers.", e);
            throw new UpgradeException("Unable to create Service Config and Schema Managers.", e);
        }
    }

    private void findUpgradableProviders() throws UpgradeException {
        try {
            final ServiceSchema serviceSchema = ssm.getOrganizationSchema();
            for (String realm : getRealmNames()) {
                final ServiceConfig serviceConfig = scm.getOrganizationConfig(realm, null);
                if (!serviceConfig.exists()) {
                    continue;
                }
                final Map<String, Set<String>> withDefaults = serviceConfig.getAttributesForRead();
                final Map<String, Set<String>> withoutDefaults = serviceConfig.getAttributesWithoutDefaultsForRead();
                final Map<String, Set<String>> withoutValidators = SMSUtils.removeValidators(withDefaults,
                        serviceSchema);

                if (isProviderRelyingOnDefaults(withoutDefaults, withoutValidators)) {
                    attributesToUpdate.put(realm, withoutValidators);
                } else if (shouldUpgradeClaims(withDefaults)) {
                    attributesToUpdate.put(realm, withoutValidators);
                } else if (shouldUpgradeAlgorithmName(withoutDefaults)) {
                    attributesToUpdate.put(realm, null);
                } else if (shouldUpgradeResponseTypePlugins(withoutDefaults)) {
                    attributesToUpdate.put(realm, null);
                } else if (shouldUpgradeScopePlugin(withoutDefaults)) {
                    attributesToUpdate.put(realm, null);
                }
            }
        } catch (Exception e) {
            DEBUG.error("An error occurred while trying to look for upgradable OAuth2 Providers.", e);
            throw new UpgradeException("Unable to retrieve OAuth2 Providers.", e);
        }
    }

    private boolean shouldUpgradeClaims(Map<String, Set<String>> attributes) {
        final Set<String> scopes = attributes.get(SUPPORTED_SCOPES);

        return scopes == null || scopes.isEmpty();
    }

    private boolean isProviderRelyingOnDefaults(Map<String, Set<String>> withoutDefaults,
                                                Map<String, Set<String>> withoutValidators) throws UpgradeException {
        return !withoutDefaults.isEmpty() && withoutDefaults.size() < withoutValidators.size();
    }

    private boolean shouldUpgradeAlgorithmName(Map<String, Set<String>> attributes) {
        final Set<String> algorithms = attributes.get(ID_TOKEN_SIGNING_ALGORITHMS);
        if (algorithms != null) {
            for (String algorithm : algorithms) {
                if (ALGORITHM_NAMES.containsKey(algorithm)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldUpgradeResponseTypePlugins(Map<String, Set<String>> attributes) {
        final Set<String> responseTypePlugins = attributes.get(RESPONSE_TYPE_LIST);
        if (responseTypePlugins != null) {
            for (String responseTypePlugin : responseTypePlugins) {
                String pluginClass = responseTypePlugin.split("\\|")[1];
                if (RESPONSE_TYPE_PLUGINS_UPGRADE_MAPPINGS.containsKey(pluginClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldUpgradeScopePlugin(Map<String, Set<String>> attributes) {
        return attributes.get(SCOPE_PLUGIN_CLASS).contains(OLD_SCOPE_PLUGIN);
    }

    @Override
    public void perform() throws UpgradeException {
        persistDefaultsForProviders();
    }

    private void persistDefaultsForProviders() throws UpgradeException {
        try {
            for (Map.Entry<String, Map<String, Set<String>>> entry : attributesToUpdate.entrySet()) {
                final String realm = entry.getKey();
                UpgradeProgress.reportStart("upgrade.oauth2.provider.start", realm);
                final ServiceConfig serviceConfig = scm.getOrganizationConfig(realm, null);
                Map<String, Set<String>> attributes = entry.getValue();
                if (attributes == null) {
                    attributes = serviceConfig.getAttributesWithoutDefaults();
                }
                migrateResponseTypePlugins(attributes);
                renameAlgorithms(attributes);
                sortScopes(attributes);
                migrateScopeValidatorPlugin(attributes);
                serviceConfig.setAttributes(attributes);
                UpgradeProgress.reportEnd("upgrade.success");
            }
        } catch (Exception e) {
            UpgradeProgress.reportEnd("upgrade.failed");
            DEBUG.error("An error occurred while trying to upgrade an OAuth2 Provider", e);
            throw new UpgradeException("Unable to upgrade OAuth2 Providers.", e);
        }

    }

    private void sortScopes(Map<String, Set<String>> attributes) {

        final Set<String> scopes = attributes.get(SUPPORTED_SCOPES);
        final Set<String> claims = attributes.get(SUPPORTED_CLAIMS);

        if (scopes == null || scopes.isEmpty()) {
            attributes.put(SUPPORTED_SCOPES, claims);
            attributes.put(SUPPORTED_CLAIMS, DEFAULT_CLAIMS);
        }

    }

    private void renameAlgorithms(Map<String, Set<String>> attributes) {
        final Set<String> algorithms = attributes.get(ID_TOKEN_SIGNING_ALGORITHMS);
        if (algorithms != null) {
            final Set<String> newAlgorithms = new HashSet<String>();
            for (String algorithm : algorithms) {
                if (ALGORITHM_NAMES.containsKey(algorithm)) {
                    newAlgorithms.add(ALGORITHM_NAMES.get(algorithm));
                } else {
                    newAlgorithms.add(algorithm);
                }
            }
            attributes.put(ID_TOKEN_SIGNING_ALGORITHMS, newAlgorithms);
        }
    }

    private void migrateScopeValidatorPlugin(Map<String, Set<String>> attributes) {
        if (attributes.get(SCOPE_PLUGIN_CLASS).contains(OLD_SCOPE_PLUGIN)) {
            attributes.put(SCOPE_PLUGIN_CLASS, asSet(NEW_SCOPE_PLUGIN));
        }
    }

    private void migrateResponseTypePlugins(Map<String, Set<String>> attributes) {
        final Set<String> responseTypePlugins = attributes.get(RESPONSE_TYPE_LIST);
        if (responseTypePlugins != null) {
            final Set<String> newResponseTypePlugins = new HashSet<>();
            for (String responseTypePlugin : responseTypePlugins) {
                String pluginClass = responseTypePlugin.split("\\|")[1];
                if (RESPONSE_TYPE_PLUGINS_UPGRADE_MAPPINGS.containsKey(pluginClass)) {
                    newResponseTypePlugins.add(responseTypePlugin.replace(pluginClass,
                            RESPONSE_TYPE_PLUGINS_UPGRADE_MAPPINGS.get(pluginClass)));
                } else {
                    newResponseTypePlugins.add(responseTypePlugin);
                }
            }
            attributes.put(RESPONSE_TYPE_LIST, newResponseTypePlugins);
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        return attributesToUpdate.size() == 0 ? "" : BUNDLE.getString("upgrade.oauth2.provider.persisted.short") +
                " (" + attributesToUpdate.size() + ')' + delimiter;
    }

    @Override
    public String getDetailedReport(String delimiter) {
        final Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);
        StringBuilder sb = new StringBuilder();
        sb.append(BUNDLE.getString("upgrade.oauth2.provider.persisted.detail")).append(": ").append(delimiter);
        for (Map.Entry<String, Map<String, Set<String>>> entry : attributesToUpdate.entrySet()) {
            sb.append(INDENT).append(entry.getKey()).append(delimiter);
        }
        tags.put(REPORT_DATA, sb.toString());
        return tagSwapReport(tags, "upgrade.oauth2.provider.report");
    }
}
