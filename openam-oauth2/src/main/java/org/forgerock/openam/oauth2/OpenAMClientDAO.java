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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import static org.forgerock.oauth2.core.OAuth2Constants.OAuth2Client.*;

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
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openam.identity.idm.AMIdentityRepositoryFactory;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openidconnect.Client;
import org.forgerock.openidconnect.ClientBuilder;
import org.forgerock.openidconnect.ClientDAO;
import org.forgerock.openidconnect.exceptions.InvalidClientMetadata;

/**
 * Implements OAuth2 client storage for OpenAM
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMClientDAO implements ClientDAO {

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
    public OpenAMClientDAO(AMIdentityRepositoryFactory idRepoFactory) {
        this.idRepoFactory = idRepoFactory;
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public void update(Client client, OAuth2Request request) throws InvalidClientMetadata, UnauthorizedClientException {
        delete(client.getClientID(), request);
        create(client, request);
    }

    /**
     * {@inheritDoc}
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
        Set<String> temp;

        if (client.getClientSecret() != null) {
            clientAttributeMap.put(USERPASSWORD, CollectionUtils.asSet(client.getClientSecret()));
        }

        if (client.getAccessToken() != null) {
            temp = new HashSet<String>();
            temp.add(client.getAccessToken());
            clientAttributeMap.put(ACCESS_TOKEN, CollectionUtils.asSet(client.getAccessToken()));
        }

        if (client.getAllowedGrantScopes() != null) {
            clientAttributeMap.put(SCOPES, formatSet(client.getAllowedGrantScopes()));
        }

        if (client.getClientName() != null) {
            temp = new HashSet<String>();
            temp.add(client.getClientName());
            clientAttributeMap.put(CLIENT_NAME, CollectionUtils.asSet(client.getClientName()));
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
            temp = new HashSet<String>();
            temp.add(client.getJwksUri());
            clientAttributeMap.put(JWKS_URI, CollectionUtils.asSet(client.getJwksUri()));
        }

        if (client.getX509() != null) {
            temp = new HashSet<String>();
            temp.add(client.getX509());
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

        if (client.getClientName() != null) {
            clientAttributeMap.put(CLIENT_NAME, CollectionUtils.asSet(client.getClientName()));
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
            temp = new HashSet<String>();
            temp.addAll(client.getPostLogoutRedirectionURIs());
            clientAttributeMap.put(POST_LOGOUT_URI, temp);
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
        clientBuilder.setAllowedGrantScopes(new ArrayList<String>(getSetAttribute(clientAttributeMap, SCOPES)));
        clientBuilder.setClientName(getSingleAttribute(clientAttributeMap, CLIENT_NAME));
        clientBuilder.setClientSessionURI(getSingleAttribute(clientAttributeMap, CLIENT_SESSION_URI));
        clientBuilder.setClientType(getSingleAttribute(clientAttributeMap, CLIENT_TYPE));
        clientBuilder.setDefaultGrantScopes(new ArrayList<String>(getSetAttribute(clientAttributeMap, DEFAULT_SCOPES)));
        clientBuilder.setDisplayDescription(new ArrayList<String>(getSetAttribute(clientAttributeMap, DESCRIPTION)));
        clientBuilder.setDisplayName(new ArrayList<String>(getSetAttribute(clientAttributeMap, NAME)));
        clientBuilder.setIdTokenSignedResponseAlgorithm(getSingleAttribute(clientAttributeMap, IDTOKEN_SIGNED_RESPONSE_ALG));
        clientBuilder.setRedirectionURIs(new ArrayList<String>(getSetAttribute(clientAttributeMap, REDIRECT_URI)));
        clientBuilder.setPostLogoutRedirectionURIs(new ArrayList<String>(getSetAttribute(clientAttributeMap, POST_LOGOUT_URI)));
        clientBuilder.setResponseTypes(new ArrayList<String>(getSetAttribute(clientAttributeMap, RESPONSE_TYPES)));
        clientBuilder.setDefaultMaxAge(Long.valueOf(getSingleAttribute(clientAttributeMap, DEFAULT_MAX_AGE)));
        clientBuilder.setDefaultMaxAgeEnabled(Boolean.valueOf(getSingleAttribute(clientAttributeMap, DEFAULT_MAX_AGE_ENABLED)));
        clientBuilder.setTokenEndpointAuthMethod(getSingleAttribute(clientAttributeMap, TOKEN_ENDPOINT_AUTH_METHOD));
        clientBuilder.setSubjectType(getSingleAttribute(clientAttributeMap, SUBJECT_TYPE));
        clientBuilder.setApplicationType(APPLICATION_TYPE_DEFAULT);
        clientBuilder.setJwks(getSingleAttribute(clientAttributeMap, JWKS));
        clientBuilder.setJwksUri(getSingleAttribute(clientAttributeMap, JWKS_URI));
        clientBuilder.setX509(getSingleAttribute(clientAttributeMap, CLIENT_JWT_PUBLIC_KEY));
        clientBuilder.setPublicKeySelector(getSingleAttribute(clientAttributeMap, PUBLIC_KEY_SELECTOR));

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
