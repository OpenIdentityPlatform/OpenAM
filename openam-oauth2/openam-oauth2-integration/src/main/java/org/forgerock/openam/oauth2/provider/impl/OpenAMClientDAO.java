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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.oauth2.provider.impl;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.identity.idm.AMIdentityRepositoryFactory;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.Client;
import org.forgerock.openam.oauth2.model.ClientBuilder;
import org.forgerock.openam.oauth2.provider.ClientDAO;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.Request;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Implements OAuth2 client storage for OpenAM
 */
public class OpenAMClientDAO implements ClientDAO {

    private String realm;
    private Request request;
    private SSOToken token;

    private static final String OAUTH2_CLIENT = "OAuth2Client";
    private static final String AGENT_TYPE = "AgentType";
    private static final String ACTIVE = "Active";
    private static final String SUN_IDENTITY_SERVER_DEVICE_STATUS = "sunIdentityServerDeviceStatus";

    private static final String SUBJECT_TYPE_DEFAULT = "Public";
    private static final String ID_TOKEN_SIGNED_RESPONSE_ALG_DEFAULT = "HS256";
    private static final String CLIENT_TYPE_DEFAULT = "Confidential";
    private static final String APPLICATION_TYPE_DEFAULT = "web";

    /**
     * Creates an OpenAMClientDAO
     */
    public OpenAMClientDAO() {
        this(null, null, (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance()));
    }

    /**
     * Creates an OpenAMClientDAO
     * @param request The request to use in the OpenAMClientDAO.
     */
    public OpenAMClientDAO(Request request) {
        this(null, request, (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance()));
    }

    /**
     * Create and OpenAMClientDAO
     * @param realm The realm the OpenAMClientDAO will use.
     * @param request The request to use in the OpenAMClientDAO.
     * @param token The SSOToken to use then dealing with the OpenAM instance.
     */
    public OpenAMClientDAO(String realm, Request request, SSOToken token) {
        this.realm = realm;
        this.request = request;
        this.token = token;
    }

    private Map<String, Set<String>> createClientAttributeMap(Client client) {
        Map<String, Set<String>> clientAttributeMap = new HashMap<String, Set<String>>();
        Set<String> temp;

        if (client.getClientSecret() != null) {
            temp = new HashSet<String>();
            temp.add(client.getClientSecret());
            clientAttributeMap.put(OAuth2Constants.OAuth2Client.USERPASSWORD, temp);
        }

        if (client.getAccessToken() != null) {
            temp = new HashSet<String>();
            temp.add(client.getAccessToken());
            clientAttributeMap.put(OAuth2Constants.OAuth2Client.ACCESS_TOKEN, temp);
        }

        if (client.getAllowedGrantScopes() != null) {
            clientAttributeMap.put(OAuth2Constants.OAuth2Client.SCOPES, formatSet(client.getAllowedGrantScopes()));
        }

        if (client.getClientName() != null) {
            temp = new HashSet<String>();
            temp.add(client.getClientName());
            clientAttributeMap.put(OAuth2Constants.OAuth2Client.CLIENT_NAME, temp);
        }

        if (client.getClientSessionURI() != null) {
            temp = new HashSet<String>();
            temp.add(client.getClientSessionURI());
            clientAttributeMap.put(OAuth2Constants.OAuth2Client.CLIENT_SESSION_URI, temp);
        }

        if (client.getClientType() != null) {
            temp = new HashSet<String>();
            temp.add(client.getClientType().toString());
            clientAttributeMap.put(OAuth2Constants.OAuth2Client.CLIENT_TYPE, temp);
        } else {
            temp = new HashSet<String>();
            temp.add(CLIENT_TYPE_DEFAULT);
            clientAttributeMap.put(OAuth2Constants.OAuth2Client.CLIENT_TYPE, temp);
        }

        if (client.getDefaultGrantScopes() != null) {
            clientAttributeMap.put(OAuth2Constants.OAuth2Client.DEFAULT_SCOPES, formatSet(client.getDefaultGrantScopes()));
        }

        if (client.getDisplayDescription() != null) {
            clientAttributeMap.put(OAuth2Constants.OAuth2Client.DESCRIPTION, formatSet(client.getDisplayDescription()));
        }

        if (client.getDisplayName() != null) {
            clientAttributeMap.put(OAuth2Constants.OAuth2Client.NAME, formatSet(client.getDisplayName()));
        }

        if (client.getClientName() != null) {
            temp = new HashSet<String>();
            temp.add(client.getClientName());
            clientAttributeMap.put(OAuth2Constants.OAuth2Client.CLIENT_NAME, temp);
        }

        if (client.getIdTokenSignedResponseAlgorithm() != null) {
            temp = new HashSet<String>();
            temp.add(client.getIdTokenSignedResponseAlgorithm());
            clientAttributeMap.put(OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG, temp);
        } else {
            temp = new HashSet<String>();
            temp.add(ID_TOKEN_SIGNED_RESPONSE_ALG_DEFAULT);
            clientAttributeMap.put(OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG, temp);
        }

        if (client.getRedirectionURIsAsString() != null) {
            clientAttributeMap.put(OAuth2Constants.OAuth2Client.REDIRECT_URI, formatSet(client.getRedirectionURIsAsString()));
        }

        if (client.getPostLogoutRedirectionURI() != null) {
            temp = new HashSet<String>();
            temp.add(client.getPostLogoutRedirectionURI());
            clientAttributeMap.put(OAuth2Constants.OAuth2Client.POST_LOGOUT_URI, temp);
        }

        if (client.getResponseTypes() != null) {
            clientAttributeMap.put(OAuth2Constants.OAuth2Client.RESPONSE_TYPES, formatSet(client.getResponseTypes()));
        }

        //add the standard agent stuff
        temp = new HashSet<String>();
        temp.add(OAUTH2_CLIENT);
        clientAttributeMap.put(AGENT_TYPE, temp);

        temp = new HashSet<String>();
        temp.add(ACTIVE);
        clientAttributeMap.put(SUN_IDENTITY_SERVER_DEVICE_STATUS, temp);

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
        return Collections.EMPTY_SET;
    }

    private Client createClient(Map<String, Set<String>> clientAttributeMap) {
        if (clientAttributeMap == null || clientAttributeMap.isEmpty()) {
            throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(null,
                    "Client has no attributes");
        }


        ClientBuilder clientBuilder = new ClientBuilder();

        clientBuilder.setAccessToken(getSingleAttribute(clientAttributeMap, OAuth2Constants.OAuth2Client.ACCESS_TOKEN));
        clientBuilder.setAllowedGrantScopes(new ArrayList<String>(getSetAttribute(clientAttributeMap, OAuth2Constants.OAuth2Client.SCOPES)));
        clientBuilder.setClientName(getSingleAttribute(clientAttributeMap, OAuth2Constants.OAuth2Client.CLIENT_NAME));
        clientBuilder.setClientSessionURI(getSingleAttribute(clientAttributeMap, OAuth2Constants.OAuth2Client.CLIENT_SESSION_URI));
        clientBuilder.setClientType(getSingleAttribute(clientAttributeMap, OAuth2Constants.OAuth2Client.CLIENT_TYPE));
        clientBuilder.setDefaultGrantScopes(new ArrayList<String>(getSetAttribute(clientAttributeMap, OAuth2Constants.OAuth2Client.DEFAULT_SCOPES)));
        clientBuilder.setDisplayDescription(new ArrayList<String>(getSetAttribute(clientAttributeMap, OAuth2Constants.OAuth2Client.DESCRIPTION)));
        clientBuilder.setDisplayName(new ArrayList<String>(getSetAttribute(clientAttributeMap, OAuth2Constants.OAuth2Client.NAME)));
        clientBuilder.setIdTokenSignedResponseAlgorithm(getSingleAttribute(clientAttributeMap, OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG));
        clientBuilder.setRedirectionURIs(new ArrayList<String>(getSetAttribute(clientAttributeMap, OAuth2Constants.OAuth2Client.REDIRECT_URI)));
        clientBuilder.setPostLogoutRedirectionURI(getSingleAttribute(clientAttributeMap, OAuth2Constants.OAuth2Client.POST_LOGOUT_URI));
        clientBuilder.setResponseTypes(new ArrayList<String>(getSetAttribute(clientAttributeMap, OAuth2Constants.OAuth2Client.RESPONSE_TYPES)));
        clientBuilder.setSubjectType(SUBJECT_TYPE_DEFAULT);
        clientBuilder.setApplicationType(APPLICATION_TYPE_DEFAULT);

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

    /**
     * {@inheritDoc}
     */
    public void create(Client client) throws OAuthProblemException {
        Map<String, Set<String>> attrs = createClientAttributeMap(client);
        try {
            AMIdentityRepository repo = AMIdentityRepositoryFactory.createAMIdentityRepository(token, realm);
            repo.createIdentity(IdType.AGENTONLY, client.getClientID(), attrs);
        } catch (Exception e) {
            OAuth2Utils.DEBUG.error("ConnectClientRegistration.Validate(): Unable to create client", e);
            throw OAuthProblemException.OAuthError.INVALID_CLIENT_METADATA.handle(request);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Client read(String clientId) {
        Map<String, Set<String>> clientAttributes = new HashMap<String, Set<String>>();
        try {
            AMIdentity theID = null;
            AMIdentityRepository repo = AMIdentityRepositoryFactory.createAMIdentityRepository(token, realm);

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
                OAuth2Utils.DEBUG.error("OpenAMClientDAO.read(): No client profile or more than one profile found.");
                throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(null,
                        "Not able to get client from OpenAM");
            }

            theID = results.iterator().next();

            //if the client is deactivated return null
            if (!theID.isActive()) {
                theID = null;
            } else {
                clientAttributes = theID.getAttributes();
            }
        } catch (Exception e) {
            OAuth2Utils.DEBUG.error("OpenAMClientDAO.read(): Unable to get client AMIdentity: ", e);
            throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(null, "Not able to get client from OpenAM");
        }

        Client client = createClient(clientAttributes);
        client.setClientID(clientId);
        return client;
    }

    /**
     * {@inheritDoc}
     */
    public void update(Client client) {
        delete(client);
        create(client);
    }

    /**
     * {@inheritDoc}
     */
    public void delete(Client client) {
        delete(client.getClientID());
    }

    /**
     * {@inheritDoc}
     */
    public void delete(String clientId) {
        try {
            //get the AMIdentity
            AMIdentityRepository repo = AMIdentityRepositoryFactory.createAMIdentityRepository(token, realm);

            AMIdentity theID = null;

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
                OAuth2Utils.DEBUG.error("OpenAMClientDAO.delete(): No client profile or more than one profile found.");
                throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(null,
                        "Not able to get client from OpenAM");
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
        } catch (Exception e) {
            OAuth2Utils.DEBUG.error("OpenAMClientDAO.delete(): Unable to delete client", e);
            throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(request);
        }
    }
}
