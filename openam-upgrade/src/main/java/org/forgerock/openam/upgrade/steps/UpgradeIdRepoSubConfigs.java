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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.upgrade.steps;

import static org.forgerock.openam.upgrade.UpgradeServices.*;
import static org.forgerock.openam.utils.CollectionUtils.*;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.opendj.ldap.Filter;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * This upgrade steps if there is any data store configured to use the Netscape LDAPv3Repo implementation, and if there
 * is, it will modify those configurations to use the new DJLDAPv3Repo instead.
 * This upgrade step will also upgrade the psearch filter setting to exclude the CTS suffix as well.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class UpgradeIdRepoSubConfigs extends AbstractUpgradeStep {

    private static final String REPO_DATA = "%REPO_DATA%";
    private static final String OLD_IDREPO_CLASS = "com.sun.identity.idm.plugins.ldapv3.LDAPv3Repo";
    private static final String NEW_IDREPO_CLASS = "org.forgerock.openam.idrepo.ldap.DJLDAPv3Repo";
    private static final String PSEARCH_FILTER = "sun-idrepo-ldapv3-config-psearch-filter";
    private static final String OLD_CONNECTION_MODE = "sun-idrepo-ldapv3-config-ssl-enabled";
    private static final String NEW_CONNECTION_MODE = "sun-idrepo-ldapv3-config-connection-mode";
    private final Map<String, Set<String>> repos = new HashMap<String, Set<String>>();
    private final Map<String, Map<String, String>> oldConnectionModeRepos = new HashMap<String, Map<String, String>>();

    @Inject
    public UpgradeIdRepoSubConfigs(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    @Override
    public boolean isApplicable() {
        return !repos.isEmpty();
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            for (String realm : getRealmNames()) {
                ServiceConfigManager scm = new ServiceConfigManager(IdConstants.REPO_SERVICE, getAdminToken());
                ServiceConfig sc = scm.getOrganizationConfig(realm, null);
                if (sc != null) {
                    Set<String> subConfigNames = sc.getSubConfigNames("*", "LDAPv3*");
                    if (subConfigNames != null) {
                        if (DEBUG.messageEnabled()) {
                            DEBUG.message("IdRepo configurations found under realm: " + realm + " : "
                                    + subConfigNames);
                        }
                        for (String subConfig : subConfigNames) {
                            ServiceConfig repoConfig = sc.getSubConfig(subConfig);
                            if (repoConfig != null) {
                                Map<String, Set<String>> attributes = repoConfig.getAttributes();
                                String className = CollectionHelper.getMapAttr(attributes, IdConstants.ID_REPO);
                                String sslMode = getModifiedConnectionMode(attributes);
                                if (OLD_IDREPO_CLASS.equals(className) || getModifiedFilter(attributes) != null ||
                                        sslMode != null) {
                                    if (DEBUG.messageEnabled()) {
                                        DEBUG.message("Discovered IdRepo: " + subConfig + " in realm: " + realm);
                                    }
                                    Set<String> values = repos.get(realm);
                                    if (values == null) {
                                        values = new HashSet<String>();
                                    }
                                    values.add(subConfig);
                                    repos.put(realm, values);
                                    
                                    if (sslMode != null) {
                                        Map<String, String> sslModes = oldConnectionModeRepos.get(realm);
                                        if (sslModes == null){ 
                                            sslModes = new HashMap<String, String>();
                                        }
                                        if (sslModes.get(subConfig) == null) {
                                            sslModes.put(subConfig, sslMode);
                                            oldConnectionModeRepos.put(realm, sslModes);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            DEBUG.error("Unable to identify old datastore configurations", ex);
            throw new UpgradeException("An error occured while trying to identify old datastore configurations");
        }
    }

    @Override
    public void perform() throws UpgradeException {
        for (Map.Entry<String, Set<String>> entry : repos.entrySet()) {
            try {
                String realm = entry.getKey();
                OrganizationConfigManager ocm = new OrganizationConfigManager(getAdminToken(), realm);
                for (String subConfig : entry.getValue()) {
                    UpgradeProgress.reportStart("upgrade.data.store.start", subConfig);
                    Map<String, Set<String>> newValues = new HashMap<String, Set<String>>(2);
                    ServiceConfig sc = ocm.getServiceConfig(IdConstants.REPO_SERVICE);
                    ServiceConfig repoConfig = sc.getSubConfig(subConfig);
                    Map<String, Set<String>> attributes = repoConfig.getAttributes();
                    
                    String className = CollectionHelper.getMapAttr(attributes, IdConstants.ID_REPO);
                    if (OLD_IDREPO_CLASS.equals(className)) {
                        newValues.put(IdConstants.ID_REPO, asSet(NEW_IDREPO_CLASS));
                    }
                    
                    String newFilter = getModifiedFilter(attributes);
                    if (newFilter != null) {
                        if (DEBUG.messageEnabled()) {
                            DEBUG.message("Upgrading psearch filter for datastore: " + subConfig
                                    + " to: " + newFilter);
                        }
                        newValues.put(PSEARCH_FILTER, asSet(newFilter));
                    }
                    
                    Map<String, String> sslModes = oldConnectionModeRepos.get(realm);
                    String sslMode = (sslModes == null) ? null : sslModes.get(subConfig);
                    if (sslMode != null) {
                        if (DEBUG.messageEnabled()) {
                            DEBUG.message("Upgrading connection mode for datastore: " + subConfig
                                    + " to: " + sslMode);
                        }
                        newValues.put(NEW_CONNECTION_MODE, asSet(sslMode));
                        repoConfig.removeAttribute(OLD_CONNECTION_MODE);
                    }

                    repoConfig.setAttributes(newValues);
                    UpgradeProgress.reportEnd("upgrade.success");
                }
            } catch (Exception ex) {
                UpgradeProgress.reportEnd("upgrade.failed");
                DEBUG.error("An error occurred while upgrading service config ", ex);
                throw new UpgradeException("Unable to upgrade IdRepo configuration");
            }
        }
    }

    private String getModifiedFilter(Map<String, Set<String>> attrs) {
        String pFilter = CollectionHelper.getMapAttr(attrs, PSEARCH_FILTER, "");
        if (pFilter.contains("(!(ou:dn:=services))") && !pFilter.contains("(!(ou:dn:=tokens))")) {
            return Filter.and(Filter.valueOf(pFilter), Filter.not(Filter.extensible(null, "ou", "tokens", true)))
                    .toString();
        }
        return null;
    }

    private String getModifiedConnectionMode(Map<String, Set<String>> attrs) {
        String sslMode = CollectionHelper.getMapAttr(attrs, OLD_CONNECTION_MODE);
        if (sslMode != null) {
            if (Boolean.parseBoolean(sslMode)) {
                return "LDAPS";
            } else {
                return "LDAP";
            }
        }
        return null;
    }

    @Override
    public String getShortReport(String delimiter) {
        int count = 0;
        for (Set<String> changes : repos.values()) {
            count += changes.size();
        }
        return new StringBuilder(BUNDLE.getString("upgrade.data.store")).append(" (").append(count).append(')')
                .append(delimiter).toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : repos.entrySet()) {
            sb.append(BUNDLE.getString("upgrade.realm")).append(": ").append(entry.getKey()).append(delimiter);
            for (String subConfig : entry.getValue()) {
                sb.append(INDENT).append(subConfig.substring(subConfig.lastIndexOf('/') + 1)).append(delimiter);
            }
            sb.append(delimiter);
        }
        tags.put(REPO_DATA, sb.toString());
        return tagSwapReport(tags, "upgrade.idreporeport");
    }
}
