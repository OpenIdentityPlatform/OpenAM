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
import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.identity.idm.AMIdentityRepositoryFactory;
import org.forgerock.openam.oauth2.model.Client;
import org.forgerock.openam.oauth2.model.ClientBuilder;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeClass;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.restlet.Request;
import org.testng.TestException;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;


@PrepareForTest({AMIdentityRepository.class, AMIdentity.class, SSOToken.class, AMIdentityRepositoryFactory.class})
public class OpenAMClientDAOTest extends PowerMockTestCase {

    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_TYPE = "confidential";
    private static final List<String> REDIRECTION_URIS = new ArrayList<String>();
    private static final String REDIRECTION_URIS_VALUE = "http://localhost.com";
    private static final List<String> SCOPES = new ArrayList<String>();
    private static final String SCOPES_VALUE = "openid";
    private static final List<String> DEFAULT_SCOPES = new ArrayList<String>();
    private static final String DEFAULT_SCOPES_VALUE = "openid";
    private static final List<String> DISPLAY_NAME = new ArrayList<String>();
    private static final String DISPLAY_NAME_VALUE = "name";
    private static final List<String> DISPLAY_DESCRIPTION = new ArrayList<String>();
    private static final String DISPLAY_DESCRIPTION_VALUE = "description";
    private static final String SUBJECT_TYPE = "public";
    private static final String ID_TOKEN_SIGNED_RESPONSE_ALGORITHM = "HS256";
    private static final String POST_LOGOUT_REDIRECT_URI = "http://localhost.com";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String CLIENT_SESSION_URI = "http://localhost.com";
    private static final String CLIENT_SECRET = "secret";
    private static final String CLIENT_NAME = "clientName";

    private static final String PREFIX = "[0]=";

    private static final int SINGLE_ELEMENT = 1;

    private OpenAMClientDAO openAMClientDAO;

    @BeforeClass
    public void oneTimeSetup() {
        REDIRECTION_URIS.add(REDIRECTION_URIS_VALUE);
        SCOPES.add(SCOPES_VALUE);
        DEFAULT_SCOPES.add(DEFAULT_SCOPES_VALUE);
        DISPLAY_DESCRIPTION.add(DISPLAY_DESCRIPTION_VALUE);
        DISPLAY_NAME.add(DISPLAY_NAME_VALUE);
    }

    @Test
    public void testShouldCreateAClientInTheRepository() {
        //given
        ClientBuilder clientBuilder = new ClientBuilder();
        clientBuilder.setAccessToken(ACCESS_TOKEN)
                .setSubjectType(SUBJECT_TYPE)
                .setPostLogoutRedirectionURI(POST_LOGOUT_REDIRECT_URI)
                .setAllowedGrantScopes(SCOPES)
                .setClientID(CLIENT_ID)
                .setClientName(CLIENT_NAME)
                .setDisplayName(DISPLAY_NAME)
                .setClientType(CLIENT_TYPE)
                .setRedirectionURIs(REDIRECTION_URIS)
                .setDefaultGrantScopes(DEFAULT_SCOPES)
                .setDisplayDescription(DISPLAY_DESCRIPTION)
                .setIdTokenSignedResponseAlgorithm(ID_TOKEN_SIGNED_RESPONSE_ALGORITHM)
                .setClientSessionURI(CLIENT_SESSION_URI)
                .setClientSecret(CLIENT_SECRET);

        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        SSOToken token;

        try {
            AMIdentityRepository amIdentityRepository = PowerMockito.mock(AMIdentityRepository.class);
            AMIdentity amIdentity = PowerMockito.mock(AMIdentity.class);
            token = PowerMockito.mock(SSOToken.class);
            PowerMockito.mockStatic(AMIdentityRepositoryFactory.class);
            when(AMIdentityRepositoryFactory.createAMIdentityRepository(any(SSOToken.class), anyString()))
                    .thenReturn(amIdentityRepository);
            when(amIdentityRepository.createIdentity(any(IdType.class), anyString(), attributeCaptor.capture()))
                    .thenReturn(amIdentity);
        } catch (Exception e) {
            throw new TestException("FAIL", e);
        }

        Request request = PowerMockito.mock(Request.class);
        openAMClientDAO = new OpenAMClientDAO("/", request, token);

        //when
        openAMClientDAO.create(clientBuilder.createClient());

        //then
        Map<String, Set<String>> attributes = attributeCaptor.getValue();

        assert(attributes != null);
        assert(!attributes.isEmpty());

        assert(attributes.get(OAuth2Constants.OAuth2Client.ACCESS_TOKEN) != null);
        assert(attributes.get(OAuth2Constants.OAuth2Client.ACCESS_TOKEN).contains(ACCESS_TOKEN) &&
               attributes.get(OAuth2Constants.OAuth2Client.ACCESS_TOKEN).size() == SINGLE_ELEMENT);

        assert(attributes.get(OAuth2Constants.OAuth2Client.POST_LOGOUT_URI) != null);
        assert(attributes.get(OAuth2Constants.OAuth2Client.POST_LOGOUT_URI).contains(POST_LOGOUT_REDIRECT_URI) &&
                attributes.get(OAuth2Constants.OAuth2Client.POST_LOGOUT_URI).size() == SINGLE_ELEMENT);


        assert(attributes.get(OAuth2Constants.OAuth2Client.SCOPES) != null);
        assert(attributes.get(OAuth2Constants.OAuth2Client.SCOPES).contains(PREFIX + SCOPES_VALUE) &&
                attributes.get(OAuth2Constants.OAuth2Client.SCOPES).size() == SINGLE_ELEMENT);

        assert(attributes.get(OAuth2Constants.OAuth2Client.REDIRECT_URI) != null);
        assert(attributes.get(OAuth2Constants.OAuth2Client.REDIRECT_URI).contains(PREFIX + REDIRECTION_URIS_VALUE) &&
                attributes.get(OAuth2Constants.OAuth2Client.REDIRECT_URI).size() == SINGLE_ELEMENT);

        assert(attributes.get(OAuth2Constants.OAuth2Client.CLIENT_TYPE) != null);
        assert(attributes.get(OAuth2Constants.OAuth2Client.CLIENT_TYPE).size() == SINGLE_ELEMENT &&
                attributes.get(OAuth2Constants.OAuth2Client.CLIENT_TYPE).iterator().next().equalsIgnoreCase(CLIENT_TYPE));

        assert(attributes.get(OAuth2Constants.OAuth2Client.NAME) != null);
        assert(attributes.get(OAuth2Constants.OAuth2Client.NAME).contains(PREFIX + DISPLAY_NAME_VALUE) &&
                attributes.get(OAuth2Constants.OAuth2Client.NAME).size() == SINGLE_ELEMENT);

        assert(attributes.get(OAuth2Constants.OAuth2Client.DEFAULT_SCOPES) != null);
        assert(attributes.get(OAuth2Constants.OAuth2Client.DEFAULT_SCOPES).contains(PREFIX + DEFAULT_SCOPES_VALUE) &&
                attributes.get(OAuth2Constants.OAuth2Client.DEFAULT_SCOPES).size() == SINGLE_ELEMENT);

        assert(attributes.get(OAuth2Constants.OAuth2Client.DESCRIPTION) != null);
        assert(attributes.get(OAuth2Constants.OAuth2Client.DESCRIPTION).contains(PREFIX + DISPLAY_DESCRIPTION_VALUE) &&
                attributes.get(OAuth2Constants.OAuth2Client.DESCRIPTION).size() == SINGLE_ELEMENT);

        assert(attributes.get(OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG) != null);
        assert(attributes.get(OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG).contains(ID_TOKEN_SIGNED_RESPONSE_ALGORITHM) &&
                attributes.get(OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG).size() == SINGLE_ELEMENT);

        assert(attributes.get(OAuth2Constants.OAuth2Client.CLIENT_SESSION_URI) != null);
        assert(attributes.get(OAuth2Constants.OAuth2Client.CLIENT_SESSION_URI).contains(CLIENT_SESSION_URI) &&
                attributes.get(OAuth2Constants.OAuth2Client.CLIENT_SESSION_URI).size() == SINGLE_ELEMENT);

        assert(attributes.get(OAuth2Constants.OAuth2Client.USERPASSWORD) != null);
        assert(attributes.get(OAuth2Constants.OAuth2Client.USERPASSWORD).size() == SINGLE_ELEMENT);
    }
    private Set<String> createSet(String... items) {
        Set<String> set = new HashSet<String>();
        for(String item : items) {
            set.add(item);
        }
        return set;
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

    private Map<String, Set<String>> createAttributeMap() {
        Map<String, Set<String>> attributeMap = new HashMap<String, Set<String>>();
        attributeMap.put(OAuth2Constants.OAuth2Client.ACCESS_TOKEN, createSet(ACCESS_TOKEN));
        attributeMap.put(OAuth2Constants.OAuth2Client.POST_LOGOUT_URI, createSet(POST_LOGOUT_REDIRECT_URI));
        attributeMap.put(OAuth2Constants.OAuth2Client.SCOPES, formatSet(new HashSet<String>(SCOPES)));
        attributeMap.put(OAuth2Constants.OAuth2Client.REDIRECT_URI, formatSet(new HashSet<String>(REDIRECTION_URIS)));
        attributeMap.put(OAuth2Constants.OAuth2Client.CLIENT_TYPE, createSet(CLIENT_TYPE));
        attributeMap.put(OAuth2Constants.OAuth2Client.NAME, formatSet(new HashSet<String>(DISPLAY_NAME)));
        attributeMap.put(OAuth2Constants.OAuth2Client.DEFAULT_SCOPES, formatSet(new HashSet<String>(DEFAULT_SCOPES)));
        attributeMap.put(OAuth2Constants.OAuth2Client.DESCRIPTION, formatSet(new HashSet<String>(DISPLAY_DESCRIPTION)));
        attributeMap.put(OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG, createSet(ID_TOKEN_SIGNED_RESPONSE_ALGORITHM));
        attributeMap.put(OAuth2Constants.OAuth2Client.CLIENT_SESSION_URI, createSet(CLIENT_SESSION_URI));
        attributeMap.put(OAuth2Constants.OAuth2Client.CLIENT_SECRET, createSet(CLIENT_SECRET));
        return attributeMap;
    }

    @Test
    public void testShouldReadAClientFromTheRepository() {

        //given
        Map<String, Set<String>> attributeMap = createAttributeMap();
        SSOToken token;
        try {
            token = PowerMockito.mock(SSOToken.class);
            AMIdentityRepository amIdentityRepository = PowerMockito.mock(AMIdentityRepository.class);
            AMIdentity amIdentity = PowerMockito.mock(AMIdentity.class);
            Set<AMIdentity> resultSet = new HashSet<AMIdentity>();
            resultSet.add(amIdentity);
            IdSearchResults searchResults = PowerMockito.mock(IdSearchResults.class);
            PowerMockito.mockStatic(AMIdentityRepositoryFactory.class);
            when(AMIdentityRepositoryFactory.createAMIdentityRepository(any(SSOToken.class), anyString()))
                    .thenReturn(amIdentityRepository);
            when(amIdentityRepository.searchIdentities(any(IdType.class), anyString(), any(IdSearchControl.class)))
                    .thenReturn(searchResults);
            when(searchResults.getSearchResults()).thenReturn(resultSet);
            when(amIdentity.getAttributes()).thenReturn(attributeMap);
            when(amIdentity.isActive()).thenReturn(true);
        } catch (Exception e) {
            throw new TestException("FAIL", e);
        }

        //when
        Request request = PowerMockito.mock(Request.class);
        openAMClientDAO = new OpenAMClientDAO("/", request, token);

        //when
        Client client = openAMClientDAO.read(CLIENT_ID);

        assert(client != null);

        assert(attributeMap.get(OAuth2Constants.OAuth2Client.ACCESS_TOKEN).contains(client.getAccessToken()));
        assert(attributeMap.get(OAuth2Constants.OAuth2Client.POST_LOGOUT_URI).contains(client.getPostLogoutRedirectionURI()));
        assert(unformattedSet(attributeMap.get(OAuth2Constants.OAuth2Client.SCOPES)).containsAll(client.getAllowedGrantScopes()));
        assert(unformattedSet(attributeMap.get(OAuth2Constants.OAuth2Client.REDIRECT_URI)).containsAll(client.getRedirectionURIsAsString()));
        assert(attributeMap.get(OAuth2Constants.OAuth2Client.CLIENT_TYPE).iterator().next().equalsIgnoreCase(client.getClientType().toString()));
        assert(unformattedSet(attributeMap.get(OAuth2Constants.OAuth2Client.NAME)).containsAll(client.getDisplayName()));
        assert(unformattedSet(attributeMap.get(OAuth2Constants.OAuth2Client.DEFAULT_SCOPES)).containsAll(client.getDefaultGrantScopes()));
        assert( unformattedSet(attributeMap.get(OAuth2Constants.OAuth2Client.DESCRIPTION)).containsAll(client.getDisplayDescription()));
        assert( attributeMap.get(OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG).contains(client.getIdTokenSignedResponseAlgorithm()));
        assert( attributeMap.get(OAuth2Constants.OAuth2Client.CLIENT_SESSION_URI).contains(client.getClientSessionURI()));
        assert( attributeMap.get(OAuth2Constants.OAuth2Client.USERPASSWORD).size() == SINGLE_ELEMENT);

    }

}
