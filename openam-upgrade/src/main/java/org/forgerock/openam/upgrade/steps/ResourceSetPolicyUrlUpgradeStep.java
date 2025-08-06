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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.upgrade.steps;

import static org.forgerock.openam.tokens.CoreTokenField.STRING_ONE;
import static org.forgerock.openam.tokens.CoreTokenField.STRING_THREE;
import static org.forgerock.openam.tokens.CoreTokenField.TOKEN_ID;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.opendj.ldap.Filter.and;
import static org.forgerock.opendj.ldap.Filter.equality;
import static org.forgerock.opendj.ldap.Filter.substrings;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.openam.oauth2.ResourceSetDescription;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.uma.ResourceSetAcceptAllFilter;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.EntryNotFoundException;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.responses.SearchResultReference;
import org.forgerock.opendj.ldif.ConnectionEntryReader;

import com.iplanet.sso.SSOToken;

/**
 * An upgrade step to fix the policy URLs from Resource Set Registration.
 */
@UpgradeStepInfo
public class ResourceSetPolicyUrlUpgradeStep extends AbstractUpgradeStep {

    private static final String REALM = "%REALM%";
    private static final String COUNT = "%COUNT%";
    private static final String REPORT_DATA = "%REPORT_DATA%";

    private final Map<String, Set<String>> affected = new HashMap<>();
    private final ConnectionFactory<Connection> rsConnectionFactory;
    private final ResourceSetStoreFactory rsStoreFactory;
    private final DN rootDn;
    private long affectedCount = 0;

    @Inject
    public ResourceSetPolicyUrlUpgradeStep(PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) ConnectionFactory connectionFactory,
            @DataLayer(ConnectionType.RESOURCE_SETS) ConnectionFactory rsConnectionFactory,
            @DataLayer(ConnectionType.RESOURCE_SETS) LdapDataLayerConfiguration dataLayerConfiguration,
            ResourceSetStoreFactory rsStoreFactory) {
        super(adminTokenAction, connectionFactory);
        this.rsConnectionFactory = rsConnectionFactory;
        this.rootDn = dataLayerConfiguration.getTokenStoreRootSuffix();
        this.rsStoreFactory = rsStoreFactory;
    }

    @Override
    public void initialize() throws UpgradeException {
        try (Connection connection = rsConnectionFactory.create()) {
            connection.readEntry(rootDn);
        } catch (EntryNotFoundException e) {
            // The resource set container does not exist, so there are no resource sets
            return;
        } catch (DataLayerException | LdapException e) {
            throw new UpgradeException("Could not connect to LDAP resource set store", e);
        }
        for (String realm : getRealmNames()) {
            Set<String> resourceSetIds = new HashSet<>();
            try (Connection connection = rsConnectionFactory.create()) {
                ConnectionEntryReader ids = connection.search(
                        Requests.newSearchRequest(rootDn, SearchScope.SINGLE_LEVEL,
                                and(substrings(STRING_ONE.toString(), null, asSet("oauth2/XUI"), null),
                                        equality(STRING_THREE.toString(), realm)),
                                TOKEN_ID.toString()));
                while (ids.hasNext()) {
                    if (ids.isEntry()) {
                        resourceSetIds.add(ids.readEntry().getAttribute(TOKEN_ID.toString()).firstValueAsString());
                    } else {
                        SearchResultReference ref = ids.readReference();
                        DEBUG.warning("Got an LDAP reference when expecting entries for resource sets: {}",
                                ref.getURIs());
                    }
                }
            } catch (DataLayerException | LdapException e) {
                throw new UpgradeException("Could not load resource sets for realm " + realm, e);
            } catch (SearchResultReferenceIOException e) {
                throw new UpgradeException("Unexpected reference when already checked for entry", e);
            }
            if (!resourceSetIds.isEmpty()) {
                affected.put(realm, resourceSetIds);
                affectedCount += resourceSetIds.size();
            }
        }
    }

    @Override
    public boolean isApplicable() {
        return affectedCount > 0;
    }

    @Override
    public void perform() throws UpgradeException {
        for (Map.Entry<String, Set<String>> realm : affected.entrySet()) {
            ResourceSetStore factory = rsStoreFactory.create(realm.getKey());
            for (String rsId : realm.getValue()) {
                try {
                    ResourceSetDescription description = factory.read(rsId, ResourceSetAcceptAllFilter.INSTANCE);
                    description.setPolicyUri(description.getPolicyUri().replace("oauth2/XUI", "XUI"));
                    factory.update(description);
                } catch (NotFoundException | ServerException e) {
                    throw new UpgradeException("Could not load resource set " + rsId + " for realm " + realm, e);
                }
            }
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        StringBuilder sb = new StringBuilder();
        if (affectedCount != 0) {
            sb.append(BUNDLE.getString("upgrade.resourcesets.short")).append(" (").append(affectedCount).append(')')
                    .append(delimiter);
        }
        return sb.toString();
    }

    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<>();
        tags.put(LF, delimiter);
        tags.put(REPORT_DATA, getRealmReport(delimiter, affected.keySet()));
        return tagSwapReport(tags, "upgrade.resourcesets.report");
    }

    private String getRealmReport(String delimiter, Set<String> realms) {
        StringBuilder sb = new StringBuilder();
        for (String realm : realms) {
            Map<String, String> tags = new HashMap<>();
            tags.put(LF, delimiter);
            tags.put(REALM, realm);
            tags.put(COUNT, String.valueOf(affected.get(realm).size()));
            sb.append(tagSwapReport(tags, "upgrade.resourcesets.realmreport"));
        }
        return sb.toString();
    }
}
