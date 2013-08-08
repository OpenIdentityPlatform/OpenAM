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

import com.sun.identity.idm.IdConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import static org.forgerock.openam.utils.CollectionUtils.*;
import org.forgerock.opendj.ldap.Filter;

/**
 * This upgrade steps if there is any data store configured to use the Netscape LDAPv3Repo implementation, and if there
 * is, it will modify those configurations to use the new DJLDAPv3Repo instead.
 * This upgrade step will also upgrade the psearch filter setting to exclude the CTS suffix as well.
 *
 * @author Peter Major
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class UpgradeIdRepoSubConfigs extends AbstractUpgradeStep {

    private static final String REPO_DATA = "%REPO_DATA%";
    private static final String OLD_IDREPO_CLASS = "com.sun.identity.idm.plugins.ldapv3.LDAPv3Repo";
    private static final String NEW_IDREPO_CLASS = "org.forgerock.openam.idrepo.ldap.DJLDAPv3Repo";
    private static final String PSEARCH_FILTER = "sun-idrepo-ldapv3-config-psearch-filter";
    private final Map<String, Set<String>> repos = new HashMap<String, Set<String>>();

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
                                if (OLD_IDREPO_CLASS.equals(className) || getModifiedFilter(attributes) != null) {
                                    if (DEBUG.messageEnabled()) {
                                        DEBUG.message("Discovered IdRepo: " + subConfig + " in realm: " + realm);
                                    }
                                    Set<String> values = repos.get(realm);
                                    if (values == null) {
                                        values = new HashSet<String>();
                                    }
                                    values.add(subConfig);
                                    repos.put(realm, values);
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
                    newValues.put(IdConstants.ID_REPO, asSet(NEW_IDREPO_CLASS));
                    ServiceConfig sc = ocm.getServiceConfig(IdConstants.REPO_SERVICE);
                    ServiceConfig repoConfig = sc.getSubConfig(subConfig);
                    String newFilter = getModifiedFilter(repoConfig.getAttributes());
                    if (newFilter != null) {
                        if (DEBUG.messageEnabled()) {
                            DEBUG.message("Upgrading psearch filter for datastore: " + subConfig
                                    + " to: " + newFilter);
                        }
                        newValues.put(PSEARCH_FILTER, asSet(newFilter));
                    }
                    //There is no need to remove the obsolete attributes here, because once we set an attribute, the SMS
                    //framework will automagically remove those that do not adhere to the schema.
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
