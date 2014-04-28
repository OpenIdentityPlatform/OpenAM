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

package org.forgerock.openam.oauth2;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.json.fluent.JsonValue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Allows a client and resource owner's identity to be retrieved.
 *
 * @since 12.0.0
 */
@Singleton
public class IdentityManager {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;

    /**
     * Constructs a new IdentityManager.
     *
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     */
    @Inject
    public IdentityManager(OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this.providerSettingsFactory = providerSettingsFactory;
    }

    /**
     * Gets a resource owner's identity.
     *
     * @param username The resource owner's username.
     * @param realm The resource owner's realm.
     * @return The resource owner's identity.
     * @throws UnauthorizedClientException If the resource owner's identity cannot be found.
     */
    public AMIdentity getResourceOwnerIdentity(String username, final String realm) throws UnauthorizedClientException {
        final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
        final AMIdentity amIdentity;

        try {
            final AMIdentityRepository amIdRepo = new AMIdentityRepository(token, realm);

            final IdSearchControl idsc = new IdSearchControl();
            idsc.setRecursive(true);
            idsc.setAllReturnAttributes(true);
            // search for the identity
            final Set<AMIdentity> results = new HashSet<AMIdentity>();
            idsc.setMaxResults(0);
            IdSearchResults searchResults = amIdRepo.searchIdentities(IdType.USER, username, idsc);
            if (searchResults != null && !searchResults.getResultAttributes().isEmpty()) {
                results.addAll(searchResults.getSearchResults());
            } else {
                OAuth2ProviderSettings settings = providerSettingsFactory.get(new OAuth2Request() {
                    public <T> T getRequest() {
                        throw new UnsupportedOperationException("Realm parameter only OAuth2Request");
                    }

                    public <T> T getParameter(String name) {
                        if ("realm".equals(name)) {
                            return (T) realm;
                        }
                        throw new UnsupportedOperationException("Realm parameter only OAuth2Request");
                    }

                    public JsonValue getBody() {
                        throw new UnsupportedOperationException("Realm parameter only OAuth2Request");
                    }
                });
                final Map<String, Set<String>> avPairs = toAvPairMap(
                        settings.getResourceOwnerAuthenticatedAttributes(), username);
                idsc.setSearchModifiers(IdSearchOpModifier.OR, avPairs);
                searchResults = amIdRepo.searchIdentities(IdType.USER, "*", idsc);
                if (searchResults != null) {
                    results.addAll(searchResults.getSearchResults());
                }
            }

            if (results.size() != 1) {
                logger.error("No user profile or more than one profile found.");
                throw new UnauthorizedClientException("Not able to get user from OpenAM");
            }

            amIdentity = results.iterator().next();

            //if the client is deactivated return null
            if (amIdentity.isActive()){
                return amIdentity;
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("Unable to get client AMIdentity: ", e);
            throw new UnauthorizedClientException("Not able to get client from OpenAM");
        }
    }

    /**
     * Gets a client's identity.
     *
     * @param clientName The client's name.
     * @param realm The client's realm.
     * @return The Clients identity.
     * @throws UnauthorizedClientException If the client's identity cannot be found.
     */
    public AMIdentity getClientIdentity(String clientName, String realm) throws UnauthorizedClientException {
        final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
        final AMIdentity amIdentity;

        try {
            final AMIdentityRepository amIdRepo = new AMIdentityRepository(token, realm);

            final IdSearchControl idsc = new IdSearchControl();
            idsc.setRecursive(true);
            idsc.setAllReturnAttributes(true);
            // search for the identity
            idsc.setMaxResults(0);
            final IdSearchResults searchResults = amIdRepo.searchIdentities(IdType.AGENTONLY, clientName, idsc);
            final Set<AMIdentity> results = searchResults.getSearchResults();

            if (results == null || results.size() != 1) {
                logger.error("No client profile or more than one profile found.");
                throw new UnauthorizedClientException("Not able to get client from OpenAM");
            }

            amIdentity = results.iterator().next();

            //if the client is deactivated return null
            if (amIdentity.isActive()){
                return amIdentity;
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("Unable to get client AMIdentity: ", e);
            throw new UnauthorizedClientException("Not able to get client from OpenAM");
        }
    }

    private Map<String, Set<String>> toAvPairMap(final Set<String> names, final String token) {
        if (token == null) {
            return Collections.emptyMap();
        }
        final Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        final Set<String> set = new HashSet<String>();
        set.add(token);
        if (names == null || names.isEmpty()) {
            return map;
        }
        final Iterator<String> it = names.iterator();
        while (it.hasNext()) {
            map.put(it.next(), set);
        }
        return map;
    }
}
