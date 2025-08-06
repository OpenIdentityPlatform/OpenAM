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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openidconnect;

import static com.sun.identity.shared.datastruct.CollectionHelper.getLongMapAttr;
import static org.forgerock.openam.oauth2.OAuth2Constants.OAuth2Client.*;
import static org.forgerock.openidconnect.Client.MIN_DEFAULT_MAX_AGE;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openam.identity.idm.AMIdentityRepositoryFactory;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openidconnect.exceptions.InvalidClientMetadata;

/**
 * Interface to do basic CRUD operations on a OAuth2Client.
 *
 * @since 12.0.0
 */
@Singleton
public class ClientDAO {

    private static final String OAUTH2_CLIENT = "OAuth2Client";
    private static final String AGENT_TYPE = "AgentType";
    private static final String ACTIVE = "Active";
    private static final String SUN_IDENTITY_SERVER_DEVICE_STATUS = "sunIdentityServerDeviceStatus";

    private static final String ID_TOKEN_SIGNED_RESPONSE_ALG_DEFAULT = "HS256";
    private static final String CLIENT_TYPE_DEFAULT = "Confidential";
    private static final String APPLICATION_TYPE_DEFAULT = "web";

    private final Debug logger = Debug.getInstance("OAuth2Provider");

    private final AMIdentityRepositoryFactory idRepoFactory;

    @Inject
    public ClientDAO(AMIdentityRepositoryFactory idRepoFactory) {
        this.idRepoFactory = idRepoFactory;
    }

    /**
     * Stores a client to a storage system.
     *
     * @param client The client to store
     * @throws InvalidClientMetadata If the client's registration details are invalid.
     */
    public void create(Client client, OAuth2Request request) throws InvalidClientMetadata {
        Map<String, Set<String>> attrs = createClientAttributeMap(client);
        try {
            final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
            final String realm = request.getParameter(OAuth2Constants.Custom.REALM);
            AMIdentityRepository repo = idRepoFactory.create(realm, token);
            repo.createIdentity(IdType.AGENTONLY, client.getClientID(), attrs);
        } catch (Exception e) {
            logger.error("ConnectClientRegistration.Validate(): Unable to create client", e);
            throw new InvalidClientMetadata();
        }
    }

    /**
     * Reads a client from a storage system.
     *
     * @param clientId The client id of the client to retrieve.
     * @return A Client read from the storage system.
     * @throws UnauthorizedClientException If the client's authorization fails.
     */
    public Client read(String clientId, OAuth2Request request) throws UnauthorizedClientException {
        Map<String, Set<String>> clientAttributes = new HashMap<String, Set<String>>();
        try {
            AMIdentity theID = null;
            final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
            final String realm = request.getParameter(OAuth2Constants.Custom.REALM);
            AMIdentityRepository repo = idRepoFactory.create(realm, token);

            IdSearchControl idsc = new IdSearchControl();
            idsc.setRecursive(true);
            idsc.setAllReturnAttributes(true);
            // search for the identity
            Set<AMIdentity> results;
            idsc.setMaxResults(0);
            IdSearchResults searchResults =
                    repo.searchIdentities(IdType.AGENTONLY, clientId, idsc);
            results = searchResults.getSearchResults();

            if (results == null || results.size() != 1) {
                logger.error("OpenAMClientDAO.read(): No client profile or more than one profile found.");
                throw new UnauthorizedClientException("Not able to get client from OpenAM");
            }

            theID = results.iterator().next();

            //if the client is deactivated return null
            if (!theID.isActive()) {
                theID = null;
            } else {
                clientAttributes = theID.getAttributes();
            }
        } catch (UnauthorizedClientException e) {
            logger.error("OpenAMClientDAO.read(): Unable to get client AMIdentity: ", e);
            throw new UnauthorizedClientException("Not able to get client from OpenAM");
        } catch (SSOException e) {
            logger.error("OpenAMClientDAO.read(): Unable to get client AMIdentity: ", e);
            throw new UnauthorizedClientException("Not able to get client from OpenAM");
        } catch (IdRepoException e) {
            logger.error("OpenAMClientDAO.read(): Unable to get client AMIdentity: ", e);
            throw new UnauthorizedClientException("Not able to get client from OpenAM");
        }

        Client client = createClient(clientAttributes);
        client.setClientID(clientId);
        return client;
    }

    /**
     * Updates a client already stored.
     *
     * @param client The updated client to use to update the storage.
     * @throws InvalidClientMetadata If the client's registration details are invalid.
     * @throws UnauthorizedClientException If the client's authorization fails.
     */
    public void update(Client client, OAuth2Request request) throws InvalidClientMetadata, UnauthorizedClientException {
        delete(client.getClientID(), request);
        create(client, request);
    }

    /**
     * Delete a client from the storage system.
     *
     * @param clientId The client id of the client to delete.
     * @throws UnauthorizedClientException If the client's authorization fails.
     */
    public void delete(String clientId, OAuth2Request request) throws UnauthorizedClientException {
        try {
            //get the AMIdentity
            final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
            final String realm = request.getParameter(OAuth2Constants.Custom.REALM);
            AMIdentityRepository repo = idRepoFactory.create(realm, token);

            AMIdentity theID = null;

            IdSearchControl idsc = new IdSearchControl();
            idsc.setRecursive(true);
            idsc.setAllReturnAttributes(true);
            // search for the identity
            Set<AMIdentity> results;
            idsc.setMaxResults(0);
            IdSearchResults searchResults = repo.searchIdentities(IdType.AGENTONLY, clientId, idsc);
            results = searchResults.getSearchResults();

            if (results == null || results.size() != 1) {
                logger.error("OpenAMClientDAO.delete(): No client profile or more than one profile found.");
                throw new UnauthorizedClientException("Not able to get client from OpenAM");
            }

            theID = results.iterator().next();

            //if the client is deactivated return null
            if (!theID.isActive()) {
                theID = null;
            }

            //delete the AMIdentity
            Set<AMIdentity> identities = new HashSet<AMIdentity>();
            identities.add(theID);
            repo.deleteIdentities(identities);
        } catch (SSOException e) {
            logger.error("OpenAMClientDAO.delete(): Unable to delete client", e);
            throw new UnauthorizedClientException();
        } catch (IdRepoException e) {
            logger.error("OpenAMClientDAO.delete(): Unable to delete client", e);
            throw new UnauthorizedClientException();
        }
    }

    private Map<String, Set<String>> createClientAttributeMap(Client client) {
        Map<String, Set<String>> clientAttributeMap = new HashMap<String, Set<String>>();

        if (client.getClientSecret() != null) {
            clientAttributeMap.put(USERPASSWORD, CollectionUtils.asSet(client.getClientSecret()));
        }

        if (client.getAccessToken() != null) {
            clientAttributeMap.put(ACCESS_TOKEN, CollectionUtils.asSet(client.getAccessToken()));
        }

        if (client.getAllowedGrantScopes() != null) {
            clientAttributeMap.put(SCOPES, formatSet(client.getAllowedGrantScopes()));
        }

        if (client.getClientName() != null) {
            clientAttributeMap.put(CLIENT_NAME, formatSet(client.getClientName()));
        }

        if (client.getClientSessionURI() != null) {
            clientAttributeMap.put(CLIENT_SESSION_URI, CollectionUtils.asSet(client.getClientSessionURI()));
        }

        if (client.getClientType() != null) {
            clientAttributeMap.put(CLIENT_TYPE, CollectionUtils.asSet(client.getClientType().toString()));
        } else {
            clientAttributeMap.put(CLIENT_TYPE, CollectionUtils.asSet(CLIENT_TYPE_DEFAULT));
        }

        if (client.getDefaultGrantScopes() != null) {
            clientAttributeMap.put(DEFAULT_SCOPES, formatSet(client.getDefaultGrantScopes()));
        }

        if (client.getDisplayDescription() != null) {
            clientAttributeMap.put(DESCRIPTION, formatSet(client.getDisplayDescription()));
        }

        if (client.getDisplayName() != null) {
            clientAttributeMap.put(NAME, formatSet(client.getDisplayName()));
        }

        if (client.getTokenEndpointAuthMethod() != null) {
            clientAttributeMap.put(TOKEN_ENDPOINT_AUTH_METHOD,
                    CollectionUtils.asSet(client.getTokenEndpointAuthMethod().getType()));
        }

        if (client.getJwks() != null) {
            clientAttributeMap.put(JWKS, CollectionUtils.asSet(client.getJwks()));
        }

        if (client.getJwksUri() != null) {
            clientAttributeMap.put(JWKS_URI, CollectionUtils.asSet(client.getJwksUri()));
        }

        if (client.getX509() != null) {
            clientAttributeMap.put(CLIENT_JWT_PUBLIC_KEY, CollectionUtils.asSet(client.getX509()));
        }

        if (client.getKeySelector() != null) {
            clientAttributeMap.put(PUBLIC_KEY_SELECTOR, CollectionUtils.asSet(client.getKeySelector()));
        }

        if (client.getTokenEndpointAuthMethod() != null) {
            clientAttributeMap.put(TOKEN_ENDPOINT_AUTH_METHOD,
                    CollectionUtils.asSet(client.getTokenEndpointAuthMethod().getType()));
        }

        if (client.getSubjectType() != null) {
            clientAttributeMap.put(SUBJECT_TYPE, CollectionUtils.asSet(client.getSubjectType().getType()));
        }

        if (client.getDefaultMaxAgeEnabled() != null) {
            clientAttributeMap.put(DEFAULT_MAX_AGE_ENABLED,
                    CollectionUtils.asSet(String.valueOf(client.getDefaultMaxAgeEnabled())));
        }

        if (client.getDefaultMaxAge() != null) {
            clientAttributeMap.put(DEFAULT_MAX_AGE, CollectionUtils.asSet(String.valueOf(client.getDefaultMaxAge())));
        }

        if (client.getSectorIdUri() != null) {
            clientAttributeMap.put(SECTOR_IDENTIFIER_URI, CollectionUtils.asSet(client.getSectorIdUri()));
        }

        if (client.getIdTokenSignedResponseAlgorithm() != null) {
            clientAttributeMap.put(IDTOKEN_SIGNED_RESPONSE_ALG,
                    CollectionUtils.asSet(client.getIdTokenSignedResponseAlgorithm()));
        } else {
            clientAttributeMap.put(IDTOKEN_SIGNED_RESPONSE_ALG,
                    CollectionUtils.asSet(ID_TOKEN_SIGNED_RESPONSE_ALG_DEFAULT));
        }

        if (client.getRedirectionURIsAsString() != null) {
            clientAttributeMap.put(REDIRECT_URI, formatSet(client.getRedirectionURIsAsString()));
        }

        if (client.getPostLogoutRedirectionURIs() != null) {
            clientAttributeMap.put(POST_LOGOUT_URI, formatSet(new HashSet<>(client.getPostLogoutRedirectionURIs())));
        }

        if (client.getResponseTypes() != null) {
            clientAttributeMap.put(RESPONSE_TYPES, formatSet(client.getResponseTypes()));
        }

        if (client.getContacts() != null) {
            clientAttributeMap.put(CONTACTS, formatSet(client.getContacts()));
        }

        //add the standard agent stuff
        clientAttributeMap.put(AGENT_TYPE, CollectionUtils.asSet(OAUTH2_CLIENT));

        clientAttributeMap.put(SUN_IDENTITY_SERVER_DEVICE_STATUS, CollectionUtils.asSet(ACTIVE));

        return clientAttributeMap;
    }

    private String getSingleAttribute(Map<String, Set<String>> clientAttributeMap, String attributeName) {
        Set<String> attributeSet = clientAttributeMap.get(attributeName);
        if (attributeSet != null && !attributeSet.isEmpty() ) {
            return attributeSet.iterator().next();
        }
        return null;
    }

    private Set<String> getSetAttribute(Map<String, Set<String>> clientAttributeMap, String attributeName) {
        Set<String> attributeSet = clientAttributeMap.get(attributeName);
        if (attributeSet != null) {
            return unformattedSet(attributeSet);
        }
        return Collections.emptySet();
    }

    private Client createClient(Map<String, Set<String>> clientAttributeMap) throws UnauthorizedClientException {
        if (clientAttributeMap == null || clientAttributeMap.isEmpty()) {
            throw new UnauthorizedClientException("Client has no attributes");
        }

        ClientBuilder clientBuilder = new ClientBuilder();

        clientBuilder.setAccessToken(getSingleAttribute(clientAttributeMap, ACCESS_TOKEN));
        clientBuilder.setAllowedGrantScopes(new ArrayList<>(getSetAttribute(clientAttributeMap, SCOPES)));
        clientBuilder.setClientName(new ArrayList<>(getSetAttribute(clientAttributeMap, CLIENT_NAME)));
        clientBuilder.setClientSecret(getSingleAttribute(clientAttributeMap, USERPASSWORD));
        clientBuilder.setClientSessionURI(getSingleAttribute(clientAttributeMap, CLIENT_SESSION_URI));
        clientBuilder.setClientType(getSingleAttribute(clientAttributeMap, CLIENT_TYPE));
        clientBuilder.setContacts(new ArrayList<>(getSetAttribute(clientAttributeMap, CONTACTS)));
        clientBuilder.setDefaultGrantScopes(new ArrayList<>(getSetAttribute(clientAttributeMap, DEFAULT_SCOPES)));
        clientBuilder.setDisplayDescription(new ArrayList<>(getSetAttribute(clientAttributeMap, DESCRIPTION)));
        clientBuilder.setDisplayName(new ArrayList<>(getSetAttribute(clientAttributeMap, NAME)));
        clientBuilder.setIdTokenSignedResponseAlgorithm(getSingleAttribute(clientAttributeMap, IDTOKEN_SIGNED_RESPONSE_ALG));
        clientBuilder.setRedirectionURIs(new ArrayList<>(getSetAttribute(clientAttributeMap, REDIRECT_URI)));
        clientBuilder.setPostLogoutRedirectionURIs(new ArrayList<>(getSetAttribute(clientAttributeMap, POST_LOGOUT_URI)));
        clientBuilder.setResponseTypes(new ArrayList<>(getSetAttribute(clientAttributeMap, RESPONSE_TYPES)));
        clientBuilder.setDefaultMaxAgeEnabled(Boolean.valueOf(getSingleAttribute(clientAttributeMap, DEFAULT_MAX_AGE_ENABLED)));
        clientBuilder.setTokenEndpointAuthMethod(getSingleAttribute(clientAttributeMap, TOKEN_ENDPOINT_AUTH_METHOD));
        clientBuilder.setSubjectType(getSingleAttribute(clientAttributeMap, SUBJECT_TYPE));
        clientBuilder.setApplicationType(APPLICATION_TYPE_DEFAULT);
        clientBuilder.setJwks(getSingleAttribute(clientAttributeMap, JWKS));
        clientBuilder.setJwksUri(getSingleAttribute(clientAttributeMap, JWKS_URI));
        clientBuilder.setX509(getSingleAttribute(clientAttributeMap, CLIENT_JWT_PUBLIC_KEY));
        clientBuilder.setPublicKeySelector(getSingleAttribute(clientAttributeMap, PUBLIC_KEY_SELECTOR));
        clientBuilder.setDefaultMaxAge(getLongMapAttr(clientAttributeMap, DEFAULT_MAX_AGE, MIN_DEFAULT_MAX_AGE, logger));

        return clientBuilder.createClient();
    }

    private Set<String> formatSet(Set<String> unformattedSet) {
        Set formattedSet = new HashSet<String>();
        Iterator<String> iter = unformattedSet.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            String string = iter.next();
            string = ("[" + i + "]=" + string);
            formattedSet.add(string);
        }
        return formattedSet;
    }

    private Set<String> unformattedSet(Set<String> formattedSet) {
        Set<String> unformattedSet = new HashSet<String>();
        Iterator<String> iter = formattedSet.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            String string = iter.next();
            int start = string.indexOf('=');
            unformattedSet.add((string.substring(start+1, string.length())).trim());
        }
        return unformattedSet;
    }
}
