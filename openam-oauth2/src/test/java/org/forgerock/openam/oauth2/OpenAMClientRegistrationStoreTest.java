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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;

import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverServiceImpl;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.PEMDecoder;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.restlet.OpenAMClientAuthenticationFailureFactory;
import org.forgerock.openam.identity.idm.AMIdentityRepositoryFactory;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.mockito.Mock;
import org.restlet.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;

public class OpenAMClientRegistrationStoreTest {

    private static final int TIMEOUT = 1000;
    private static final String REALM = "realm";
    private static final String AGENT_NAME = "agent name";

    @Mock
    PrivilegedAction<SSOToken> adminTokenAction;

    private OpenAMClientRegistrationStore store;
    private SSOToken ssoToken;
    private AMIdentityRepositoryFactory identityRepositoryFactory;

    @BeforeMethod
    public void setUpTest() throws org.forgerock.json.resource.NotFoundException, SSOException, InvalidClientException, IdRepoException {
        initMocks(this);
        ssoToken = mock(SSOToken.class);
        RealmNormaliser realmNormaliser = mock(RealmNormaliser.class);
        given(realmNormaliser.normalise(REALM)).willReturn(REALM);
        given(adminTokenAction.run()).willReturn(ssoToken);
        identityRepositoryFactory = mock(AMIdentityRepositoryFactory.class);
        store = new OpenAMClientRegistrationStore(
                realmNormaliser,
                new PEMDecoder(),
                new OpenIdResolverServiceImpl(TIMEOUT, TIMEOUT),
                mock(OAuth2ProviderSettingsFactory.class),
                new OpenAMClientAuthenticationFailureFactory(realmNormaliser),
                identityRepositoryFactory,
                adminTokenAction);
    }

    @Test
    public void getWithOauth2RequestReturnsAgentRegistrationIfJ2eeAgent()
            throws NotFoundException, InvalidClientException, IdRepoException, SSOException {
        setUpAgent(AgentConfiguration.AGENT_TYPE_J2EE, true, AGENT_NAME);
        OAuth2Request request = createRequest();

        OpenIdConnectClientRegistration registration = store.get(AGENT_NAME, request);

        assertThat(registration).isInstanceOf(AgentClientRegistration.class);
    }

    @Test
    public void getWithOauth2RequestReturnsAgentRegistrationIfWebAgent()
            throws NotFoundException, InvalidClientException, IdRepoException, SSOException {
        setUpAgent(AgentConfiguration.AGENT_TYPE_WEB, true, AGENT_NAME);
        OAuth2Request request = createRequest();

        OpenIdConnectClientRegistration registration = store.get(AGENT_NAME, request);

        assertThat(registration).isInstanceOf(AgentClientRegistration.class);
    }

    @Test
    public void getWithOauth2RequestReturnsAgentRegistrationIfNotJ2eeOrWebAgent()
            throws NotFoundException, InvalidClientException, IdRepoException, SSOException {
        setUpAgent(AgentConfiguration.AGENT_TYPE_OAUTH2, true, AGENT_NAME);
        OAuth2Request request = createRequest();

        OpenIdConnectClientRegistration registration = store.get(AGENT_NAME, request);

        assertThat(registration).isInstanceOf(OpenAMClientRegistration.class);
    }

    @Test
    public void getReturnsAgentRegistrationIfJ2eeAgent()
            throws NotFoundException, InvalidClientException, IdRepoException, SSOException {
        setUpAgent(AgentConfiguration.AGENT_TYPE_J2EE, true, AGENT_NAME);

        OpenIdConnectClientRegistration registration = store.get(AGENT_NAME, REALM, null);

        assertThat(registration).isInstanceOf(AgentClientRegistration.class);
    }

    @Test
    public void getReturnsAgentRegistrationIfWebAgent()
            throws NotFoundException, InvalidClientException, IdRepoException, SSOException {
        setUpAgent(AgentConfiguration.AGENT_TYPE_WEB, true, AGENT_NAME);

        OpenIdConnectClientRegistration registration = store.get(AGENT_NAME, REALM, null);

        assertThat(registration).isInstanceOf(AgentClientRegistration.class);
    }

    @Test
    public void getReturnsAgentRegistrationIfNotJ2eeOrWebAgent()
            throws NotFoundException, InvalidClientException, IdRepoException, SSOException {
        setUpAgent(AgentConfiguration.AGENT_TYPE_OAUTH2, true, AGENT_NAME);

        OpenIdConnectClientRegistration registration = store.get(AGENT_NAME, REALM, null);

        assertThat(registration).isInstanceOf(OpenAMClientRegistration.class);
    }

    @Test(expectedExceptions = InvalidClientException.class)
    public void getWithOauth2RequestThrowsExceptionIfIdentityIsNotActive()
            throws NotFoundException, InvalidClientException, IdRepoException, SSOException {
        setUpAgent(AgentConfiguration.AGENT_TYPE_OAUTH2, false, AGENT_NAME);
        OAuth2Request request = createRequest();

        store.get(AGENT_NAME, request);
    }

    @Test(expectedExceptions = InvalidClientException.class)
    public void getThrowsExceptionIfIdentityIsNotActive()
            throws NotFoundException, InvalidClientException, IdRepoException, SSOException {
        setUpAgent(AgentConfiguration.AGENT_TYPE_OAUTH2, false, AGENT_NAME);

        store.get(AGENT_NAME, REALM, null);
    }
    
    @Test(expectedExceptions = InvalidClientException.class)
    public void getThrowsExceptionIfIdentityNameNotMatch()
            throws NotFoundException, InvalidClientException, IdRepoException, SSOException {
        setUpAgent(AgentConfiguration.AGENT_TYPE_OAUTH2, false, AGENT_NAME.toUpperCase());

        store.get(AGENT_NAME, REALM, null);
    }

    private void setUpAgent(String agentType, boolean isActive, String agentName) throws IdRepoException, SSOException {
        AMIdentity j2eeAgent = mock(AMIdentity.class);
        given(j2eeAgent.getAttribute(IdConstants.AGENT_TYPE))
                .willReturn(new HashSet<>(Collections.singletonList(agentType)));
        given(j2eeAgent.isActive())
                .willReturn(isActive);
        given(j2eeAgent.getName())
        	.willReturn(agentName);
        IdSearchResults searchResults = mock(IdSearchResults.class);
        given(searchResults.getSearchResults())
                .willReturn(new HashSet<>(Collections.singletonList(j2eeAgent)));
        AMIdentityRepository identityRepository = mock(AMIdentityRepository.class);
        given(identityRepository.searchIdentities(eq(IdType.AGENT), eq(AGENT_NAME), any(IdSearchControl.class)))
                .willReturn(searchResults);
        given(identityRepositoryFactory.create(REALM, ssoToken))
                .willReturn(identityRepository);
    }

    private OAuth2Request createRequest() {
        OAuth2Request oAuth2Request = mock(OAuth2Request.class);
        given(oAuth2Request.getParameter(OAuth2Constants.Custom.REALM)).willReturn(REALM);
        Request request = mock(Request.class);
        given(request.getChallengeResponse()).willReturn(null);
        given(oAuth2Request.getRequest()).willReturn(request);

        return oAuth2Request;
    }
}