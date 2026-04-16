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
 */
package org.forgerock.openam.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.openam.dpro.session.NoOpTokenRestriction;
import org.forgerock.openam.identity.idm.AMIdentityRepositoryFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.DNOrIPAddressListTokenRestriction;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.TokenRestrictionFactory;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;

public class TokenRestrictionResolverTest {

    private final static String REALM = "realm";
    private final static String ORG_NAME = "org name";
    private final static String GOTO_URL = "http://go.to.url";
    private final static String HOST = "host.url";
    private final static String PROVIDER_ID = "http://some.example.url";
    private final static String LDAP_STATUS_ATTR_NAME = "sunIdentityServerDeviceStatus";
    private final static String LDAP_ATTR_NAME = "sunIdentityServerDeviceKeyValue";
    private final static String PROVIDER_ID_ATTR_NAME = "agentRootURL";
    private final static String HOSTNAME_ATTR_NAME = "hostname";
    private final static boolean UNIQUE_SSO_TOKEN_COOKIE = true;
    private final static boolean NOT_UNIQUE_SSO_TOKEN_COOKIE = false;
    private final static String AGENT_DN = "agent dn";;

    private TokenRestrictionResolver tokenRestrictionResolver;
    private AMIdentityRepositoryFactory identityRepositoryFactory;
    private SSOToken adminToken;
    private TokenRestrictionFactory tokenRestrictionFactory;

    @BeforeMethod
    public void setUpTest() {
        adminToken = mock(SSOToken.class);
        tokenRestrictionFactory = mock(TokenRestrictionFactory.class);
        identityRepositoryFactory = mock(AMIdentityRepositoryFactory.class);
        tokenRestrictionResolver = new TokenRestrictionResolver(identityRepositoryFactory, tokenRestrictionFactory);
    }

    @Test(expectedExceptions = SSOException.class)
    public void getTokenRestrictionThrowsExceptionWhenNoAgents() throws Exception {
        setUpIdentityRepositoryWithNoAgents();

        tokenRestrictionResolver.resolve(
                PROVIDER_ID + "?Realm=" + REALM,
                GOTO_URL,
                adminToken,
                UNIQUE_SSO_TOKEN_COOKIE);
    }

    @Test(expectedExceptions = SSOException.class)
    public void getTokenRestrictionThrowsExceptionWhenAgentHasNoAttributes() throws Exception {
        final HashMap attributes = new HashMap();
        AMIdentity agentIdentity = createAgentIdentity(attributes);
        setUpIdentityRepositoryWithAgent(agentIdentity);

        tokenRestrictionResolver.resolve(
                PROVIDER_ID + "?Realm=" + REALM,
                GOTO_URL,
                adminToken,
                UNIQUE_SSO_TOKEN_COOKIE);
    }

    @Test(expectedExceptions = SSOException.class)
    public void getTokenRestrictionThrowsExceptionWhenAgentHasNoStatusAttribute() throws Exception {
        HashMap<String, Set<String>> attributes = new HashMap<>();
        attributes.put("some attribute", new HashSet<>(Collections.singletonList("some attribute value")));
        AMIdentity agentIdentity = createAgentIdentity(attributes);
        setUpIdentityRepositoryWithAgent(agentIdentity);

        tokenRestrictionResolver.resolve(
                PROVIDER_ID + "?Realm=" + REALM,
                GOTO_URL,
                adminToken,
                UNIQUE_SSO_TOKEN_COOKIE);
    }

    @Test(expectedExceptions = SSOException.class)
    public void getTokenRestrictionThrowsExceptionWhenAgentIsNotActive() throws Exception {
        HashMap<String, Set<String>> attributes = new HashMap<>();
        attributes.put(LDAP_STATUS_ATTR_NAME, new HashSet<>(Collections.singletonList("not active")));
        attributes.put(LDAP_ATTR_NAME,
                new HashSet<>(Collections.singletonList(PROVIDER_ID_ATTR_NAME + "=" + GOTO_URL)));
        AMIdentity agentIdentity = createAgentIdentity(attributes);
        setUpIdentityRepositoryWithAgent(agentIdentity);

        tokenRestrictionResolver.resolve(
                PROVIDER_ID + "?Realm=" + REALM,
                GOTO_URL,
                adminToken,
                UNIQUE_SSO_TOKEN_COOKIE);
    }

    @Test(expectedExceptions = SSOException.class)
    public void getTokenRestrictionThrowsExceptionWhenAgentIsActiveButHasNoOtherAttributes() throws Exception {
        HashMap<String, Set<String>> attributes = new HashMap<>();
        attributes.put(LDAP_STATUS_ATTR_NAME, new HashSet<>(Collections.singletonList("Active")));
        AMIdentity agentIdentity = createAgentIdentity(attributes);
        setUpIdentityRepositoryWithAgent(agentIdentity);

        tokenRestrictionResolver.resolve(
                PROVIDER_ID + "?Realm=" + REALM,
                GOTO_URL,
                adminToken,
                UNIQUE_SSO_TOKEN_COOKIE);
    }

    @Test(expectedExceptions = SSOException.class)
    public void getTokenRestrictionThrowsExceptionIfGotoUrlProtocolIsInvalid() throws Exception {
        HashMap<String, Set<String>> attributes = new HashMap<>();
        attributes.put(LDAP_STATUS_ATTR_NAME, new HashSet<>(Collections.singletonList("Active")));
        attributes.put(LDAP_ATTR_NAME,
                new HashSet<>(Collections.singletonList(PROVIDER_ID_ATTR_NAME + "=https://go.to.url")));
        AMIdentity agentIdentity = createAgentIdentity(attributes);
        setUpIdentityRepositoryWithAgent(agentIdentity);

        tokenRestrictionResolver.resolve(
                PROVIDER_ID + "?Realm=" + REALM,
                GOTO_URL,
                adminToken,
                UNIQUE_SSO_TOKEN_COOKIE);
    }

    @Test(expectedExceptions = SSOException.class)
    public void getTokenRestrictionThrowsExceptionIfGotoUrlHostIsInvalid() throws Exception {
        HashMap<String, Set<String>> attributes = new HashMap<>();
        attributes.put(LDAP_STATUS_ATTR_NAME, new HashSet<>(Collections.singletonList("Active")));
        attributes.put(LDAP_ATTR_NAME,
                new HashSet<>(Collections.singletonList(PROVIDER_ID_ATTR_NAME + "=http://go.another.url")));
        AMIdentity agentIdentity = createAgentIdentity(attributes);
        setUpIdentityRepositoryWithAgent(agentIdentity);

        tokenRestrictionResolver.resolve(
                PROVIDER_ID + "?Realm=" + REALM,
                GOTO_URL,
                adminToken,
                UNIQUE_SSO_TOKEN_COOKIE);
    }

    @Test(expectedExceptions = SSOException.class)
    public void getTokenRestrictionThrowsExceptionIfGotoUrlPortIsInvalid() throws Exception {
        HashMap<String, Set<String>> attributes = new HashMap<>();
        attributes.put(LDAP_STATUS_ATTR_NAME, new HashSet<>(Collections.singletonList("Active")));
        attributes.put(LDAP_ATTR_NAME,
                new HashSet<>(Collections.singletonList(PROVIDER_ID_ATTR_NAME + "=http://go.to.url:788")));
        AMIdentity agentIdentity = createAgentIdentity(attributes);
        setUpIdentityRepositoryWithAgent(agentIdentity);

        tokenRestrictionResolver.resolve(
                PROVIDER_ID + "?Realm=" + REALM,
                GOTO_URL,
                adminToken,
                UNIQUE_SSO_TOKEN_COOKIE);
    }

    @Test
    public void getTokenRestrictionReturnsNoOpTokenRestrictionIfNotUniqueSSOTokenCookie() throws Exception {
        HashMap<String, Set<String>> attributes = new HashMap<>();
        attributes.put(LDAP_STATUS_ATTR_NAME, new HashSet<>(Collections.singletonList("Active")));
        attributes.put(LDAP_ATTR_NAME,
                new HashSet<>(Collections.singletonList(PROVIDER_ID_ATTR_NAME + "=" + GOTO_URL)));
        AMIdentity agentIdentity = createAgentIdentity(attributes);
        setUpIdentityRepositoryWithAgent(agentIdentity);
        final NoOpTokenRestriction noOpTokenRestriction = mock(NoOpTokenRestriction.class);
        when(tokenRestrictionFactory.createNoOpTokenRestriction()).thenReturn(noOpTokenRestriction);

        final TokenRestriction tokenRestriction = tokenRestrictionResolver.resolve(
                PROVIDER_ID + "?Realm=" + REALM,
                GOTO_URL,
                adminToken,
                NOT_UNIQUE_SSO_TOKEN_COOKIE);

        assertThat(tokenRestriction).isEqualTo(noOpTokenRestriction);
    }

    @Test
    public void getTokenRestrictionReturnsDNOrIPAddressListTokenRestrictionIfUniqueSSOTokenCookie() throws Exception {
        HashMap<String, Set<String>> attributes = new HashMap<>();
        attributes.put(LDAP_STATUS_ATTR_NAME, new HashSet<>(Collections.singletonList("Active")));
        attributes.put(LDAP_ATTR_NAME,
                new HashSet<>(Arrays.asList(
                        HOSTNAME_ATTR_NAME + "=" + HOST,
                        PROVIDER_ID_ATTR_NAME + "=" + GOTO_URL)));
        AMIdentity agentIdentity = createAgentIdentity(attributes);
        setUpIdentityRepositoryWithAgent(agentIdentity);
        final DNOrIPAddressListTokenRestriction dnOrIPAddressListTokenRestriction
                = mock(DNOrIPAddressListTokenRestriction.class);
        when(tokenRestrictionFactory.createDNOrIPAddressListTokenRestriction(
                AGENT_DN, new HashSet<>(Arrays.asList(HOST, new URL(GOTO_URL).getHost()))))
                .thenReturn(dnOrIPAddressListTokenRestriction);

        final TokenRestriction tokenRestriction = tokenRestrictionResolver.resolve(
                PROVIDER_ID + "?Realm=" + REALM,
                GOTO_URL,
                adminToken,
                UNIQUE_SSO_TOKEN_COOKIE);

        assertThat(tokenRestriction).isEqualTo(dnOrIPAddressListTokenRestriction);
    }

    @Test
    public void getTokenRestrictionReturnsDNOrIPAddressListTokenRestrictionForAllAgents() throws Exception {
        final String ANOTHER_GOTO_URL = "http://another.goto.url";
        HashMap<String, Set<String>> firstAgentAttributes = new HashMap<>();
        firstAgentAttributes.put(LDAP_STATUS_ATTR_NAME, new HashSet<>(Collections.singletonList("Active")));
        firstAgentAttributes.put(LDAP_ATTR_NAME,
                new HashSet<>(Collections.singletonList(PROVIDER_ID_ATTR_NAME + "=" + GOTO_URL)));
        AMIdentity firstAgentIdentity = createAgentIdentity(firstAgentAttributes);
        HashMap<String, Set<String>> secondAgentAttributes = new HashMap<>();
        secondAgentAttributes.put(LDAP_STATUS_ATTR_NAME, new HashSet<>(Collections.singletonList("Active")));
        secondAgentAttributes.put(LDAP_ATTR_NAME,
                new HashSet<>(Collections.singletonList(PROVIDER_ID_ATTR_NAME + "=" + ANOTHER_GOTO_URL)));
        AMIdentity secondAgentIdentity = createAgentIdentity(secondAgentAttributes);
        setUpIdentityRepositoryWithAgents(firstAgentIdentity, secondAgentIdentity);
        final DNOrIPAddressListTokenRestriction dnOrIPAddressListTokenRestriction
                = mock(DNOrIPAddressListTokenRestriction.class);
        when(tokenRestrictionFactory.createDNOrIPAddressListTokenRestriction(
                eq(AGENT_DN + "|" + AGENT_DN),
                eq(new HashSet<>(Arrays.asList(new URL(GOTO_URL).getHost(), new URL(ANOTHER_GOTO_URL).getHost())))))
                .thenReturn(dnOrIPAddressListTokenRestriction);

        final TokenRestriction tokenRestriction = tokenRestrictionResolver.resolve(
                PROVIDER_ID + "?Realm=" + REALM,
                GOTO_URL,
                adminToken,
                UNIQUE_SSO_TOKEN_COOKIE);

        assertThat(tokenRestriction).isEqualTo(dnOrIPAddressListTokenRestriction);
    }

    private AMIdentity createAgentIdentity(HashMap agentAttributes) throws IdRepoException, SSOException {
        AMIdentity identity = mock(AMIdentity.class);
        when(identity.getAttributes()).thenReturn(agentAttributes);
        when(identity.getDN()).thenReturn(AGENT_DN);
        return identity;
    }

    private void setUpIdentityRepositoryWithNoAgents() throws IdRepoException, SSOException {
        AMIdentityRepository identityRepository = mock(AMIdentityRepository.class);
        when(identityRepository.searchIdentities(eq(IdType.AGENT), eq("*"), any(IdSearchControl.class)))
                .thenReturn(new IdSearchResults(IdType.AGENT, ORG_NAME));
        when(identityRepositoryFactory.create(REALM, adminToken)).thenReturn(identityRepository);
    }

    private void setUpIdentityRepositoryWithAgent(AMIdentity agentIdentity) throws IdRepoException, SSOException {
        final IdSearchResults searchResults = new IdSearchResults(IdType.AGENT, ORG_NAME);
        searchResults.addResult(agentIdentity, new HashMap());
        AMIdentityRepository identityRepository = mock(AMIdentityRepository.class);
        when(identityRepository.searchIdentities(eq(IdType.AGENT), eq("*"), any(IdSearchControl.class)))
                .thenReturn(searchResults);
        when(identityRepositoryFactory.create(REALM, adminToken)).thenReturn(identityRepository);
    }

    private void setUpIdentityRepositoryWithAgents(AMIdentity... agentIdentities) throws IdRepoException, SSOException {
        final IdSearchResults searchResults = new IdSearchResults(IdType.AGENT, ORG_NAME);
        for (AMIdentity agentIdentity : agentIdentities) {
            searchResults.addResult(agentIdentity, new HashMap());
        }
        AMIdentityRepository identityRepository = mock(AMIdentityRepository.class);
        when(identityRepository.searchIdentities(eq(IdType.AGENT), eq("*"), any(IdSearchControl.class)))
                .thenReturn(searchResults);
        when(identityRepositoryFactory.create(REALM, adminToken)).thenReturn(identityRepository);
    }
}