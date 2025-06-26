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
package org.forgerock.openam.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.RealmOAuth2ProviderSettings;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.agent.AgentConstants;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

public class AgentClientRegistrationTest {

    private static final String AGENT_ROOT_URI = "http://agent.root.uri:8080/";
    private static final String CDSSO_REDIRECT_URI = "/redirect/uri";
    private static final String POST_LOGOUT_URI = "http://post.logout.uri";
    private static final String AGENT_NAME = "agent name";

    private AMIdentity agent;
    private AgentClientRegistration agentClientRegistration;

    @BeforeClass
    public void setUpTest() throws IdRepoException, SSOException {
        agent = mock(AMIdentity.class);
        when(agent.getName()).thenReturn(AGENT_NAME);
        agentClientRegistration = new AgentClientRegistration(agent);
    }

    @Test
    public void canGetIdentity() {
        assertThat(agentClientRegistration.getIdentity()).isEqualTo(agent);
    }

    @Test
    public void canGetRedirectUris() throws IdRepoException, SSOException {

        setUpAgentWithAttribute(AgentConstants.AGENT_ROOT_URL_FOR_CDSSO_ATTRIBUTE_NAME,
                "agentRootURL=" + AGENT_ROOT_URI);
        setUpAgentWithAttribute(AgentConstants.CDSSO_REDIRECT_URI_ATTRIBUTE_NAME, CDSSO_REDIRECT_URI);

        assertThat(agentClientRegistration.getRedirectUris())
                .isEqualTo(new HashSet<>(Collections.singletonList(URI
                        .create("http://agent.root.uri:8080/redirect/uri"))));
    }

    @Test(expectedExceptions = OAuthProblemException.class)
    public void throwExceptionIfCannotGetRedirectUris() throws IdRepoException, SSOException {
        setUpAgentToThrowExceptionForAttribute(AgentConstants.CDSSO_REDIRECT_URI_ATTRIBUTE_NAME);

        agentClientRegistration.getRedirectUris();
    }

    @Test
    public void canGetPostLogoutRedirectUris() throws IdRepoException, SSOException {
        final String logoutUri = AgentConstants.LOGOUT_ENTRY_UTI_ATTRIBUTE_NAME + "=" + POST_LOGOUT_URI;
        setUpAgentWithAttribute(AgentConstants.LOGOUT_ENTRY_UTI_ATTRIBUTE_NAME, logoutUri);

        assertThat(agentClientRegistration.getPostLogoutRedirectUris())
                .isEqualTo(new HashSet<>(Collections.singletonList(URI.create(POST_LOGOUT_URI))));
    }

    @Test(expectedExceptions = OAuthProblemException.class)
    public void throwExceptionIfCannotGetPostLogoutRedirectUris() throws IdRepoException, SSOException {
        setUpAgentToThrowExceptionForAttribute(AgentConstants.LOGOUT_ENTRY_UTI_ATTRIBUTE_NAME);

        agentClientRegistration.getPostLogoutRedirectUris();
    }

    @Test
    public void canGetAllowedResponseTypes() {
        final Set<String> responseTypes = agentClientRegistration.getAllowedResponseTypes();

        assertThat(responseTypes).isEqualTo(new HashSet<>(Collections.singletonList("id_token")));
    }

    @Test
    public void canGetClientId() {
        final String clientId = agentClientRegistration.getClientId();

        assertThat(clientId).isEqualTo(AGENT_NAME);
    }

    @Test
    public void canGetAccessTokenType() {
        final String accessTokenType = agentClientRegistration.getAccessTokenType();

        assertThat(accessTokenType).isEqualTo("Bearer");
    }

    @Test
    public void canGetClientSecret() throws IdRepoException, SSOException {
        final String SECRET = "client secret";
        setUpAgentWithAttribute(AgentConstants.USER_PASSWORD_ATTRIBUTE_NAME, SECRET);

        assertThat(agentClientRegistration.getClientSecret()).isEqualTo(SECRET);
    }

    @Test(expectedExceptions = OAuthProblemException.class)
    public void throwExceptionIfCannotGetClientSecret() throws IdRepoException, SSOException {
        setUpAgentToThrowExceptionForAttribute(AgentConstants.USER_PASSWORD_ATTRIBUTE_NAME);

        agentClientRegistration.getClientSecret();
    }

    @Test
    public void consentIsImplied() {
        assertThat(agentClientRegistration.isConsentImplied()).isEqualTo(true);
    }

    @Test
    public void canGetDefaultScopes() {
        final Set<String> defaultScopes = agentClientRegistration.getDefaultScopes();

        assertThat(defaultScopes).isEqualTo(new HashSet<>(Arrays.asList("openid")));
    }

    @Test
    public void canGetAllowedScopes() {
        final Set<String> allowedScopes = agentClientRegistration.getAllowedScopes();

        assertThat(allowedScopes).isEqualTo(new HashSet<>(Arrays.asList("openid")));
    }

    @Test
    public void clientIsConfidential() {
        assertThat(agentClientRegistration.isConfidential()).isEqualTo(true);
    }

    @Test
    public void canGetSubjectType() {
        assertThat(agentClientRegistration.getSubjectType()).isEqualTo("Public");
    }

    @Test
    public void canGetIDTokenSignedResponseAlgorithm() {
        assertThat(agentClientRegistration.getIDTokenSignedResponseAlgorithm()).isEqualTo("HS256");
    }

    @Test
    public void canGetTokenEndpointAuthMethod() {
        assertThat(agentClientRegistration.getTokenEndpointAuthMethod()).isEqualTo("client_secret_basic");
    }

    @Test
    public void canGetAuthorizationCodeLifeTime() throws ServerException {
        final long AUTHORIZATION_CODE_LIFETIME = 12L;
        OAuth2ProviderSettings providerSettings = mock(RealmOAuth2ProviderSettings.class);
        given(providerSettings.getAuthorizationCodeLifetime()).willReturn(AUTHORIZATION_CODE_LIFETIME);

        assertThat(agentClientRegistration.getAuthorizationCodeLifeTime(providerSettings))
                .isEqualTo(AUTHORIZATION_CODE_LIFETIME);
    }

    @Test
    public void canGetAccessTokenLifeTime() throws ServerException {
        final long ACCESS_TOKEN_LIFETIME = 73L;
        OAuth2ProviderSettings providerSettings = mock(RealmOAuth2ProviderSettings.class);
        given(providerSettings.getAccessTokenLifetime()).willReturn(ACCESS_TOKEN_LIFETIME);

        assertThat(agentClientRegistration.getAccessTokenLifeTime(providerSettings))
                .isEqualTo(ACCESS_TOKEN_LIFETIME);
    }

    @Test
    public void canGetRefreshTokenLifeTime() throws ServerException {
        final long REFRESH_TOKEN_LIFETIME = 9833L;
        OAuth2ProviderSettings providerSettings = mock(RealmOAuth2ProviderSettings.class);
        given(providerSettings.getRefreshTokenLifetime()).willReturn(REFRESH_TOKEN_LIFETIME);

        assertThat(agentClientRegistration.getRefreshTokenLifeTime(providerSettings))
                .isEqualTo(REFRESH_TOKEN_LIFETIME);
    }

    @Test
    public void canGetJwtTokenLifeTime() throws ServerException {
        final long OPENID_TOKEN_LIFETIME = 77L;
        OAuth2ProviderSettings providerSettings = mock(RealmOAuth2ProviderSettings.class);
        given(providerSettings.getOpenIdTokenLifetime()).willReturn(OPENID_TOKEN_LIFETIME);

        assertThat(agentClientRegistration.getJwtTokenLifeTime(providerSettings))
                .isEqualTo(OPENID_TOKEN_LIFETIME);
    }

    private void setUpAgentWithAttribute(String attributeName, String... attributeValues)
            throws IdRepoException, SSOException {
        given(agent.getAttribute(attributeName))
                .willReturn(new HashSet<>(Arrays.asList(attributeValues)));
    }

    private void setUpAgentToThrowExceptionForAttribute(String attributeName) throws IdRepoException, SSOException {
        given(agent.getAttribute(attributeName))
                .willThrow(new SSOException("exception!"));
    }
}