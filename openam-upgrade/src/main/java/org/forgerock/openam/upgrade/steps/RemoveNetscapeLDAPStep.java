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

package org.forgerock.openam.upgrade.steps;

import static org.forgerock.openam.upgrade.UpgradeServices.*;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.inject.Inject;

import org.forgerock.guava.common.base.Joiner;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.VersionUtils;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * A step to migrate old NetscapeLDAPv3 sub schema instances to the equivalent other sub schema type.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class RemoveNetscapeLDAPStep extends AbstractUpgradeStep {

    private static final String REALM_PROGRESS = "upgrade.removenetscapeldap.progress.realm";
    private static final String SCHEMA_PROGRESS = "upgrade.removenetscapeldap.progress.schema";
    private static final String SHORT_REPORT_REALM = "upgrade.removenetscapeldap.short.realm";
    private static final String SHORT_REPORT_SCHEMA = "upgrade.removenetscapeldap.short.schema";
    private static final String DETAIL_REPORT_REALM = "upgrade.removenetscapeldap.detail.realm";
    private static final String DETAIL_REPORT_IDREPO = "upgrade.removenetscapeldap.detail.idrepo";
    private static final String DETAIL_REPORT_CONFIG = "upgrade.removenetscapeldap.detail.config";
    private static final String DETAIL_REPORT_SCHEMA = "upgrade.removenetscapeldap.detail.schema";
    private static final String DETAIL_REPORT = "upgrade.removenetscapeldap.detail";

    private static final List<String> ATTRIBUTES_TO_COPY = Arrays.asList(
            "sunIdRepoAttributeMapping",
            "sunIdRepoSupportedOperations",
            "sun-idrepo-ldapv3-config-ldap-server",
            "sun-idrepo-ldapv3-config-authid",
            "sun-idrepo-ldapv3-config-authpw",
            "sun-idrepo-ldapv3-config-organization_name",
            "sun-idrepo-ldapv3-config-connection_pool_min_size",
            "sun-idrepo-ldapv3-config-connection_pool_max_size",
            "sun-idrepo-ldapv3-config-max-result",
            "sun-idrepo-ldapv3-config-time-limit",
            "sun-idrepo-ldapv3-config-search-scope",
            "sun-idrepo-ldapv3-config-users-search-attribute",
            "sun-idrepo-ldapv3-config-users-search-filter",
            "sun-idrepo-ldapv3-config-user-objectclass",
            "sun-idrepo-ldapv3-config-user-attributes",
            "sun-idrepo-ldapv3-config-createuser-attr-mapping",
            "sun-idrepo-ldapv3-config-isactive",
            "sun-idrepo-ldapv3-config-active",
            "sun-idrepo-ldapv3-config-inactive",
            "sun-idrepo-ldapv3-config-groups-search-attribute",
            "sun-idrepo-ldapv3-config-groups-search-filter",
            "sun-idrepo-ldapv3-config-group-container-name",
            "sun-idrepo-ldapv3-config-group-container-value",
            "sun-idrepo-ldapv3-config-group-objectclass",
            "sun-idrepo-ldapv3-config-group-attributes",
            "sun-idrepo-ldapv3-config-memberof",
            "sun-idrepo-ldapv3-config-uniquemember",
            "sun-idrepo-ldapv3-config-memberurl",
            "sun-idrepo-ldapv3-config-dftgroupmember",
            "sun-idrepo-ldapv3-config-roles-search-attribute",
            "sun-idrepo-ldapv3-config-roles-search-filter",
            "sun-idrepo-ldapv3-config-role-search-scope",
            "sun-idrepo-ldapv3-config-role-objectclass",
            "sun-idrepo-ldapv3-config-filterrole-objectclass",
            "sun-idrepo-ldapv3-config-filterrole-attributes",
            "sun-idrepo-ldapv3-config-nsrole",
            "sun-idrepo-ldapv3-config-nsroledn",
            "sun-idrepo-ldapv3-config-nsrolefilter",
            "sun-idrepo-ldapv3-config-people-container-name",
            "sun-idrepo-ldapv3-config-people-container-value",
            "sun-idrepo-ldapv3-config-auth-naming-attr",
            "sun-idrepo-ldapv3-config-psearchbase",
            "sun-idrepo-ldapv3-config-psearch-filter",
            "sun-idrepo-ldapv3-config-psearch-scope",
            "com.iplanet.am.ldap.connection.delay.between.retries",
            "sun-idrepo-ldapv3-config-service-attributes"
    );

    private static final int AM_13 = 1300;
    public static final String NETSCAPE_LDAP_V3 = "NetscapeLDAPv3";

    private final Map<String, Set<String>> subSchemaIds = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private boolean removeSubSchema = false;

    @Inject
    public RemoveNetscapeLDAPStep(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    @Override
    public boolean isApplicable() {
        return removeSubSchema || !subSchemaIds.isEmpty();
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            if (VersionUtils.isCurrentVersionLessThan(AM_13, true)) {
                for (String realm : getRealmNames()) {
                    ServiceConfigManager scm = new ServiceConfigManager(IdConstants.REPO_SERVICE, getAdminToken());
                    ServiceConfig sc = scm.getOrganizationConfig(realm, null);
                    if (sc != null) {
                        Set<String> subConfigNames = sc.getSubConfigNames("*", NETSCAPE_LDAP_V3);
                        if (!subConfigNames.isEmpty()) {
                            Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                            names.addAll(subConfigNames);
                            subSchemaIds.put(realm, names);
                        }
                    }
                }
                ServiceSchemaManager ssm = new ServiceSchemaManager(IdConstants.REPO_SERVICE, getAdminToken());
                ServiceSchema orgSchema = ssm.getOrganizationSchema();
                if (orgSchema.getSubSchemaNames().contains(NETSCAPE_LDAP_V3)) {
                    removeSubSchema = true;
                }
            }
        } catch (Exception ex) {
            DEBUG.error("Unable to identify old datastore configurations", ex);
            throw new UpgradeException("An error occured while trying to identify old datastore configurations");
        }
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            ServiceConfigManager scm = new ServiceConfigManager(IdConstants.REPO_SERVICE, getAdminToken());
            for (Map.Entry<String, Set<String>> realmConfig : subSchemaIds.entrySet()) {
                ServiceConfig sc = scm.getOrganizationConfig(realmConfig.getKey(), null);
                UpgradeProgress.reportStart(REALM_PROGRESS, realmConfig.getKey());
                for (String configName : realmConfig.getValue()) {
                    Map<String, Set<String>> oldConfig = sc.getSubConfig(configName).getAttributesWithoutDefaultsForRead();
                    Map<String, Set<String>> newConfig = new HashMap<>();

                    // A configured NetscapeLDAPv3 schema will have only one of the following
                    // attributes, which will indicate the type of the ldap connection.
                    copyAttribute(oldConfig, newConfig, "sun-idrepo-ldapv3-ldapv3Generic");
                    copyAttribute(oldConfig, newConfig, "sun-idrepo-ldapv3-ldapv3AMDS");
                    copyAttribute(oldConfig, newConfig, "sun-idrepo-ldapv3-ldapv3OpenDS");
                    copyAttribute(oldConfig, newConfig, "sun-idrepo-ldapv3-ldapv3Tivoli");
                    copyAttribute(oldConfig, newConfig, "sun-idrepo-ldapv3-ldapv3AD");
                    copyAttribute(oldConfig, newConfig, "sun-idrepo-ldapv3-ldapv3ADAM");

                    if (newConfig.size() != 1) {
                        DEBUG.error("ID Repo {} in realm {} has types: ", configName, realmConfig.getKey(), newConfig);
                        throw new UpgradeException("Cannot deduce type of id repo config: " + configName);
                    }

                    String typeString = newConfig.keySet().iterator().next();
                    LdapType type = LdapType.valueOf(typeString.substring("sun-idrepo-ldapv3-ldapv3".length()));

                    for (String attributeName : ATTRIBUTES_TO_COPY) {
                        copyAttribute(oldConfig, newConfig, attributeName);
                    }

                    if (CollectionHelper.getBooleanMapAttr(oldConfig, "sun-idrepo-ldapv3-config-ssl-enabled", false)) {
                        newConfig.put("sun-idrepo-ldapv3-config-connection-mode", asSet("LDAPS"));
                    }

                    sc.removeSubConfig(configName);
                    sc.addSubConfig(configName, type.schemaType, 0, newConfig);
                }
                UpgradeProgress.reportEnd("upgrade.success");
            }
            if (removeSubSchema) {
                UpgradeProgress.reportStart(SCHEMA_PROGRESS);
                ServiceSchemaManager ssm = new ServiceSchemaManager(IdConstants.REPO_SERVICE, getAdminToken());
                ssm.getOrganizationSchema().removeSubSchema(NETSCAPE_LDAP_V3);
                UpgradeProgress.reportEnd("upgrade.success");
            }
        } catch (Exception ex) {
            DEBUG.error("Unable to upgrade old datastore configurations", ex);
            throw new UpgradeException("An error occured while trying to upgrade old datastore configurations");
        }
    }

    private void copyAttribute(Map<String, Set<String>> config, Map<String, Set<String>> newConfig, String attributeName) {
        Set<String> values = config.get(attributeName);
        if (values != null && !values.isEmpty()) {
            newConfig.put(attributeName, values);
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        StringBuilder report = new StringBuilder();
        if (subSchemaIds.size() > 0) {
            int count = 0;
            for (Set<String> entries : subSchemaIds.values()) {
                count += entries.size();
            }
            report.append(MessageFormat.format(BUNDLE.getString(SHORT_REPORT_REALM), count));
            report.append(delimiter);
        }
        if (removeSubSchema) {
            report.append(BUNDLE.getString(SHORT_REPORT_SCHEMA)).append(delimiter);
        }
        return report.toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        StringBuilder content = new StringBuilder();
        if (!subSchemaIds.isEmpty()) {
            content.append(delimiter);
            content.append(BUNDLE.getString(DETAIL_REPORT_CONFIG));
            content.append(delimiter);
        }
        for (Map.Entry<String, Set<String>> realmConfig : subSchemaIds.entrySet()) {
            content.append(MessageFormat.format(BUNDLE.getString(DETAIL_REPORT_REALM), realmConfig.getKey()));
            content.append(delimiter);
            for (String configName : realmConfig.getValue()) {
                content.append(MessageFormat.format(BUNDLE.getString(DETAIL_REPORT_IDREPO), configName));
                content.append(delimiter);
            }
        }
        if (removeSubSchema) {
            content.append(delimiter);
            content.append(BUNDLE.getString(DETAIL_REPORT_SCHEMA));
            content.append(delimiter);
        }
        Map<String, String> tags = new HashMap<>();
        tags.put(LF, delimiter);
        tags.put("%CONTENT%", content.toString());
        return tagSwapReport(tags, DETAIL_REPORT);
    }

    private enum LdapType {
        Generic("LDAPv3"),
        AMDS("LDAPv3ForAMDS"),
        OpenDS("LDAPv3ForOpenDS"),
        Tivoli("LDAPv3ForTivoli"),
        AD("LDAPv3ForAD"),
        ADAM("LDAPv3ForADAM");

        private final String schemaType;

        LdapType(String schemaType) {
            this.schemaType = schemaType;
        }
    }
}
