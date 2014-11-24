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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.upgrade.steps;

import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.opendj.ldap.ConnectionFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.forgerock.openam.upgrade.UpgradeServices.*;
import static org.forgerock.openam.utils.CollectionUtils.*;

/**
 * This upgrade step looks in OAuth2 auth module config for uses of DefaultAccountMapper and DefaultAttributeMapper,
 * which have been refactored to different classes.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class UpgradeOAuth2AuthModulesStep extends AbstractUpgradeStep {

    public static final String ACCOUNT_MAPPER_PROPERTY = "org-forgerock-auth-oauth-account-mapper";
    public static final String ATTRIBUTE_MAPPER_PROPERTY = "org-forgerock-auth-oauth-attribute-mapper";

    private static final String REPORT_DATA = "%REPORT_DATA%";
    private static final String SERVICE_NAME = "sunAMAuthOAuthService";
    private static final String JSON_MAPPER =
            "org.forgerock.openam.authentication.modules.common.mapping.JsonAttributeMapper";
    private static final String DEFAULT_ACCOUNT_MAPPER =
            "org.forgerock.openam.authentication.modules.oauth2.DefaultAccountMapper";
    private static final String DEFAULT_ATTRIBUTE_MAPPER =
            "org.forgerock.openam.authentication.modules.oauth2.DefaultAttributeMapper";
    private Map<String, Set<String>> affectedRealms = new HashMap<String, Set<String>>();
    private Map<String, Set<String>> customisedRealms = new HashMap<String, Set<String>>();
    private int moduleCount = 0;

    @Inject
    public UpgradeOAuth2AuthModulesStep(final PrivilegedAction<SSOToken> adminTokenAction,
            @Named(DataLayerConstants.DATA_LAYER_BINDING) final ConnectionFactory factory) {
        super(adminTokenAction, factory);
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            ServiceConfigManager scm = new ServiceConfigManager(SERVICE_NAME, getAdminToken());
            for (String realm : getRealmNames()) {
                ServiceConfig realmConfig = scm.getOrganizationConfig(realm, null);
                for (String moduleName : (Set<String>) realmConfig.getSubConfigNames()) {
                    ServiceConfig moduleConfig = realmConfig.getSubConfig(moduleName);
                    Map<String, Set<?>> attributes = getAttributes(moduleConfig);
                    check(attributes, ACCOUNT_MAPPER_PROPERTY, DEFAULT_ACCOUNT_MAPPER, realm, moduleName);
                    check(attributes, ATTRIBUTE_MAPPER_PROPERTY, DEFAULT_ATTRIBUTE_MAPPER, realm, moduleName);
                }
            }
        } catch (Exception ex) {
            DEBUG.error("An error occurred while trying to look for upgradable OAuth2 auth modules", ex);
            throw new UpgradeException("Unable to retrieve OAuth2 modules", ex);
        }
    }

    private Map<String, Set<?>> getAttributes(ServiceConfig moduleConfig) {
        return moduleConfig.getAttributes();
    }

    private void check(Map<String, Set<?>> attributes, String property, String value, String realm, String moduleName) {
        if (attributes.get(property).contains(value)) {
            flagModule(affectedRealms, realm, moduleName);
        } else {
            flagModule(customisedRealms, realm, moduleName);
        }
    }

    private void flagModule(Map<String, Set<String>> flags, String realm, String moduleName) {
        if (flags.containsKey(realm)) {
            flags.get(realm).add(moduleName);
        } else {
            flags.put(realm, asSet(moduleName));
        }
    }

    @Override
    public boolean isApplicable() {
        return !affectedRealms.isEmpty() || !customisedRealms.isEmpty();
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            ServiceConfigManager scm = new ServiceConfigManager(SERVICE_NAME, getAdminToken());
            for (Map.Entry<String, Set<String>> realm : affectedRealms.entrySet()) {
                ServiceConfig realmConfig = scm.getOrganizationConfig(realm.getKey(), null);
                for (String moduleName : realm.getValue()) {
                    ServiceConfig moduleConfig = realmConfig.getSubConfig(moduleName);
                    Map<String, Set<?>> attributes = getAttributes(moduleConfig);
                    if (attributes.get(ACCOUNT_MAPPER_PROPERTY).contains(DEFAULT_ACCOUNT_MAPPER)) {
                        moduleConfig.replaceAttributeValues(ACCOUNT_MAPPER_PROPERTY, asSet(DEFAULT_ACCOUNT_MAPPER),
                                asSet(JSON_MAPPER));
                    }
                    if (attributes.get(ATTRIBUTE_MAPPER_PROPERTY).contains(DEFAULT_ATTRIBUTE_MAPPER)) {
                        moduleConfig.replaceAttributeValues(ATTRIBUTE_MAPPER_PROPERTY, asSet(DEFAULT_ATTRIBUTE_MAPPER),
                                asSet(JSON_MAPPER));
                    }
                    moduleCount++;
                }
            }
        } catch (Exception ex) {
            DEBUG.error("An error occurred while trying to update OAuth2 auth modules", ex);
            throw new UpgradeException("Unable to update OAuth2 modules", ex);
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        StringBuilder sb = new StringBuilder();
        if (moduleCount != 0) {
            sb.append(BUNDLE.getString("upgrade.oauth2.modules")).append(" (").append(moduleCount).append(')')
                    .append(delimiter);
        }
        return sb.toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);
        StringBuilder sb = new StringBuilder();
        sb.append(getModulesReport("upgrade.oauth2modulesreport.updated", delimiter, affectedRealms));
        sb.append(getModulesReport("upgrade.oauth2modulesreport.customised", delimiter, customisedRealms));
        tags.put(REPORT_DATA, sb.toString());
        return tagSwapReport(tags, "upgrade.oauth2modulesreport");
    }

    private String getModulesReport(String messageKey, String delimiter, Map<String, Set<String>> flags) {
        if (flags.isEmpty()) {
            return "";
        }
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : flags.entrySet()) {
            sb.append(BUNDLE.getString("upgrade.realm")).append(": ").append(entry.getKey()).append(delimiter);
            for (String module : entry.getValue()) {
                sb.append(INDENT).append(module).append(delimiter);
            }
        }
        tags.put(REPORT_DATA, sb.toString());
        return tagSwapReport(tags, messageKey);
    }

}
