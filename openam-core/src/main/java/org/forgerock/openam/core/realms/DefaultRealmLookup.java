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
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.realms;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.OrganizationConfigManagerFactory;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceManager;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link RealmLookup} which determines how to lookup a given realm identifier based
 * on its given format and validates that the realm identifier maps to a single valid realm.
 *
 * @since 14.0.0
 */
@Singleton
class DefaultRealmLookup implements RealmLookup {

    private static Logger logger = LoggerFactory.getLogger("amIdm");

    private final Provider<SSOToken> adminTokenProvider;
    private final OrganizationConfigManagerFactory organizationConfigManagerFactory;

    @Inject
    DefaultRealmLookup(@Named("AdminToken") Provider<SSOToken> adminTokenProvider,
            OrganizationConfigManagerFactory organizationConfigManagerFactory) {
        this.adminTokenProvider = adminTokenProvider;
        this.organizationConfigManagerFactory = organizationConfigManagerFactory;
    }

    @Override
    public final Realm lookup(String realmName) throws RealmLookupException {
        String id = null;
        logger.trace("DefaultRealms:lookup Input orgname: {}", realmName);
        if (StringUtils.isEmpty(realmName) || realmName.equals("/")) {
            // Return base DN
            id = Realm.root().asDN();
        } else if (realmName.startsWith("/")) {
            // If realmName is in "/" format covert to DN and return
            id = Realm.convertRealmPathToDN(realmName);
            try {
                organizationConfigManagerFactory.create(adminTokenProvider.get(), realmName);
            } catch (SMSException e) {
                logger.trace("DefaultRealms:lookup Exception in getting org name from SMS", e);
                throw new NoRealmFoundException(realmName);
            }
        } else if (LDAPUtils.isDN(realmName)) {
            id = realmName;
            try {
                // Search for realms with realmName name
                organizationConfigManagerFactory.create(adminTokenProvider.get(), realmName);
            } catch (SMSException e) {
                logger.trace("DefaultRealms:lookup Exception in getting org name from SMS", e);
                throw new NoRealmFoundException(realmName);
            }
        } else if (isCoexistenceMode()) {
            // Return the org DN as determined by AMStoreConnection
            logger.trace("DefaultRealms:lookup: getting from AMSDK");
            try {
                AMStoreConnection amsc = new AMStoreConnection(adminTokenProvider.get());
                id = amsc.getOrganizationDN(realmName, null);
            } catch (AMException e) {
                logger.trace("DefaultRealms:lookup Exception in getting org name from AMSDK", e);
                throw convertAMException(e);
            } catch (SSOException e) {
                throw new RealmLookupException(e);
            }
        } else {
            // Get the realm name from SMS
            logger.trace("DefaultRealms:lookup: getting from SMS realms");
            try {
                boolean foundOrg = false;
                Set<String> subOrgNames = getSubOrganisations(adminTokenProvider.get(), realmName);
                if (CollectionUtils.isNotEmpty(subOrgNames)) {
                    if (subOrgNames.size() == 1) {
                        id = Realm.convertRealmPathToDN(subOrgNames.iterator().next());
                        foundOrg = true;
                    } else {
                        for (String subRealmName : subOrgNames) {
                            // check for realmName
                            StringTokenizer st = new StringTokenizer(subRealmName, "/");
                            // Need to handle the scenario where multiple sub-realm
                            // with the same name should not be allowed
                            while (st.hasMoreTokens()) {
                                if (st.nextToken().equalsIgnoreCase(realmName)) {
                                    if (!foundOrg) {
                                        id = Realm.convertRealmPathToDN(subRealmName);
                                        foundOrg = true;
                                    } else {
                                        throw new MultipleRealmsFoundException(realmName);
                                    }
                                }
                            }
                        }
                    }
                }

                // Check if organization name has been determined
                logger.trace("DefaultRealms:lookup: getting from SMS realms aliases");
                // perform organization alias search
                Set orgAliases = getRealmAliases(adminTokenProvider.get(), realmName);
                if (!foundOrg && CollectionUtils.isEmpty(orgAliases)) {
                    logger.warn("DefaultRealms:lookup Unable to find Org name for: {}", realmName);
                    throw new NoRealmFoundException(realmName);
                } else if (CollectionUtils.isNotEmpty(orgAliases) && foundOrg) {
                    // Check to see if there is one alias, which points to the same realm as the one previously found
                    boolean sameRealm = false;
                    if (orgAliases.size() == 1) {
                        String aliasId = Realm.convertRealmPathToDN((String)orgAliases.iterator().next());
                        if (StringUtils.isEqualTo(aliasId, id)) {
                            // The realm has an alias that is equivalent to the realm name
                            sameRealm = true;
                        }
                    }
                    if (!sameRealm) {
                        // Multiple realms should not have the same alias
                        logger.warn("DefaultRealms:lookup Multiple  matching Orgs found for: {}", realmName);
                        throw new MultipleRealmsFoundException(realmName);
                    }
                } else if (CollectionUtils.isNotEmpty(orgAliases) && (orgAliases.size() > 1)) {
                    // Multiple realms should not have the same alias
                    logger.warn("DefaultRealms:lookup Multiple  matching Orgs found for: {}", realmName);
                    throw new MultipleRealmsFoundException(realmName);
                }
                if (!foundOrg) {
                    String tmpS = (String) orgAliases.iterator().next();
                    id = Realm.convertRealmPathToDN(tmpS);
                }
            } catch (SMSException | SSOException e) {
                logger.trace("DefaultRealms:lookup Exception in getting org name from SMS", e);
                throw new NoRealmFoundException(realmName);
            }
        }
        logger.trace("DefaultRealms:lookup Search for OrgIdentifier:{} returning realm DN: {}", realmName, id);
        return new Realm(id);
    }

    @Override
    public final boolean isActive(Realm org) throws RealmLookupException {
        boolean isActive = true;
        // Need to initialize ServiceManager by creating the constructor
        if (!isCoexistenceMode()) {
            // Pick it up from the realms tree.
            try {
                OrganizationConfigManager ocm = organizationConfigManagerFactory.create(adminTokenProvider.get(),
                        org.asDN());
                if (ocm == null) {
                    throw new NoRealmFoundException(org.asPath());
                }
                Map attributes = ocm.getAttributes(IdConstants.REPO_SERVICE);
                Set<String> vals = (Set<String>) attributes.get(IdConstants.ORGANIZATION_STATUS_ATTR);
                if (vals == null || vals.isEmpty()) {
                    isActive = true;
                } else {
                    String stringActive = vals.iterator().next();
                    isActive = stringActive.equalsIgnoreCase("Active");
                }
            } catch (SMSException e) {
                throw new NoRealmFoundException(org.asPath());
            }
        } else if (isAMSDKEnabled()) {
            // Return the org DN as determined by AMStoreConnection.
            try {
                AMStoreConnection amsc = new AMStoreConnection(adminTokenProvider.get());
                AMOrganization orgObj = amsc.getOrganization(org.asDN());
                isActive = orgObj.isActivated();
            } catch (AMException ame) {
                throw convertAMException(ame);
            } catch (SSOException e) {
                throw new RealmLookupException(e);
            }
        }
        return isActive;
    }

    private static RealmLookupException convertAMException(AMException ame) {
        Object[] args = ame.getMessageArgs();
        String eCode = ame.getErrorCode();
        IdRepoException ide;
        if (args == null) {
            ide = new IdRepoException("amProfile", eCode, null);
        } else {
            ide = new IdRepoException("amProfile", ame.getErrorCode(), args);
        }
        ide.setLDAPErrorCode(ame.getLDAPErrorCode());
        return new RealmLookupException(ide);
    }

    @VisibleForTesting
    Set<String> getSubOrganisations(SSOToken token, String realm) throws SMSException, SSOException {
        ServiceManager sm = new ServiceManager(token);
        // First search for realms with orgIdentifier name
        OrganizationConfigManager ocm = sm.getOrganizationConfigManager("/");
        return ocm.getSubOrganizationNames(realm, true);
    }

    @VisibleForTesting
    Set<String> getRealmAliases(SSOToken token, String realmAlias) throws RealmLookupException {
        try {
            ServiceManager serviceManager = new ServiceManager(token);
            return serviceManager.searchOrganizationNames(
                    IdConstants.REPO_SERVICE,
                    IdConstants.ORGANIZATION_ALIAS_ATTR, Collections.singleton(realmAlias));
        } catch (SSOException | SMSException e) {
            throw new RealmLookupException(e);
        }
    }

    @VisibleForTesting
    boolean isCoexistenceMode() {
        return ServiceManager.isCoexistenceMode();
    }

    @VisibleForTesting
    boolean isAMSDKEnabled() {
        return ServiceManager.isAMSDKEnabled();
    }
}
