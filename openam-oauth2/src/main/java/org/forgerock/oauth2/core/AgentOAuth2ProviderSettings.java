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
package org.forgerock.oauth2.core;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OpenAMScopeValidator;
import org.forgerock.openidconnect.Client;
import org.forgerock.openidconnect.IdTokenResponseTypeHandler;

import com.sun.identity.shared.debug.Debug;

import freemarker.template.Template;

/**
 * Models all of the OAuth2 provider settings specific to agents.
 */
public class AgentOAuth2ProviderSettings implements OAuth2ProviderSettings {

    private final Debug logger = Debug.getInstance("AgentOAuth2ProviderSettings");

    @Override
    public boolean isStatelessTokensEnabled() throws ServerException {
        return false;
    }

    @Override
    public boolean isIdTokenInfoClientAuthenticationEnabled() throws ServerException {
        return true;
    }

    @Override
    public String getTokenSigningAlgorithm() throws ServerException {
        return "HS256";
    }

    @Override
    public boolean isTokenCompressionEnabled() throws ServerException {
        return false;
    }

    @Override
    public String getTokenHmacSharedSecret() throws ServerException {
        return null;
    }

    @Override
    public Map<String, ResponseTypeHandler> getAllowedResponseTypes()
            throws UnsupportedResponseTypeException, ServerException {
        Map<String, ResponseTypeHandler> responseTypes = new HashMap<>();
        responseTypes.put("id_token", InjectorHolder.getInstance(IdTokenResponseTypeHandler.class));
        return responseTypes;
    }

    @Override
    public boolean isSaveConsentEnabled() {
        return false;
    }

    @Override
    public boolean isConsentSaved(ResourceOwner resourceOwner, String clientId, Set<String> scope) {
        return false;
    }

    @Override
    public Set<String> validateAuthorizationScope(ClientRegistration clientRegistration, Set<String> scope,
                                                  OAuth2Request request) throws ServerException, InvalidScopeException {
        return getScopeValidator()
                .validateAuthorizationScope(clientRegistration, scope, request);
    }

    @Override
    public Set<String> validateAccessTokenScope(ClientRegistration clientRegistration, Set<String> scope,
                                                OAuth2Request request) throws ServerException, InvalidScopeException {
        return getScopeValidator()
                .validateAccessTokenScope(clientRegistration, scope, request);
    }

    @Override
    public Set<String> validateRefreshTokenScope(ClientRegistration clientRegistration, Set<String> requestedScope,
                                                 Set<String> tokenScope, OAuth2Request request)
            throws ServerException, InvalidScopeException {
        return getScopeValidator()
                .validateRefreshTokenScope(clientRegistration, requestedScope, tokenScope, request);
    }

    @Override
    public UserInfoClaims getUserInfo(ClientRegistration clientRegistration, AccessToken token, OAuth2Request request)
            throws ServerException, UnauthorizedClientException, NotFoundException {
        return getScopeValidator().getUserInfo(clientRegistration, token, request);
    }

    @Override
    public Map<String, Object> evaluateScope(AccessToken accessToken) throws ServerException {
        return getScopeValidator().evaluateScope(accessToken);
    }

    @Override
    public Map<String, String> additionalDataToReturnFromAuthorizeEndpoint(Map<String, Token> tokens,
                                                                           OAuth2Request request)
            throws ServerException {
        return getScopeValidator().additionalDataToReturnFromAuthorizeEndpoint(tokens, request);
    }

    @Override
    public void additionalDataToReturnFromTokenEndpoint(AccessToken accessToken, OAuth2Request request)
            throws ServerException, InvalidClientException, NotFoundException {
        getScopeValidator().additionalDataToReturnFromTokenEndpoint(accessToken, request);
    }

    @Override
    public void saveConsent(ResourceOwner resourceOwner, String clientId, Set<String> scope) {
        logger.error("Save consent operation is not supported by AgentOAuth2ProviderSettings.");
    }

    @Override
    public void revokeConsent(String userId, String clientId) {
        logger.error("Revoke consent operation is not supported by AgentOAuth2ProviderSettings.");
    }

    @Override
    public boolean issueRefreshTokens() throws ServerException {
        return false;
    }

    @Override
    public boolean issueRefreshTokensOnRefreshingToken() throws ServerException {
        return false;
    }

    @Override
    public long getAuthorizationCodeLifetime() throws ServerException {
        return OAuth2Constants.AgentOAuth2ProviderService.AUTHORIZATION_CODE_LIFETIME;
    }

    @Override
    public long getAccessTokenLifetime() throws ServerException {
        return OAuth2Constants.AgentOAuth2ProviderService.ACCESS_TOKEN_LIFETIME;
    }

    @Override
    public long getOpenIdTokenLifetime() throws ServerException {
        return OAuth2Constants.AgentOAuth2ProviderService.OPENID_CONNECT_JWT_TOKEN_LIFETIME;
    }

    @Override
    public long getRefreshTokenLifetime() throws ServerException {
        return OAuth2Constants.AgentOAuth2ProviderService.REFRESH_TOKEN_LIFETIME;
    }

    @Override
    public KeyPair getSigningKeyPair(JwsAlgorithm algorithm) throws ServerException {
        return new KeyPair(null, null);
    }

    @Override
    public Set<String> getResourceOwnerAuthenticatedAttributes() throws ServerException {
        return Sets.newHashSet("uid");
    }

    @Override
    public Set<String> getSupportedClaims() throws ServerException {
        return Sets.newHashSet();
    }

    @Override
    public Set<String> getSupportedClaimsWithTranslations() throws ServerException {
        return Sets.newHashSet();
    }

    @Override
    public Set<String> getSupportedScopes() throws ServerException {
        return Sets.newHashSet("openid");
    }

    @Override
    public Set<String> getSupportedScopesWithTranslations() throws ServerException {
        return getSupportedScopes();
    }

    @Override
    public Set<String> getDefaultScopes() throws ServerException {
        return Sets.newHashSet();
    }

    @Override
    public Set<String> getSupportedIDTokenSigningAlgorithms() throws ServerException {
        return Sets.newHashSet("HS256");
    }

    @Override
    public Set<String> getSupportedIDTokenEncryptionAlgorithms() throws ServerException {
        return Sets.newHashSet();
    }

    @Override
    public Set<String> getSupportedIDTokenEncryptionMethods() throws ServerException {
        return Sets.newHashSet();
    }

    @Override
    public String getOpenIDConnectVersion() {
        return OAuth2Constants.OAuth2ProviderService.OPENID_CONNECT_VERSION;
    }

    @Override
    public JsonValue getJWKSet() throws ServerException {
        return null;
    }

    @Override
    public String getCreatedTimestampAttributeName() throws ServerException {
        return null;
    }

    @Override
    public String getModifiedTimestampAttributeName() throws ServerException {
        return null;
    }

    @Override
    public Set<String> getSupportedSubjectTypes() throws ServerException {
        return Sets.newHashSet("public");
    }

    @Override
    public boolean isOpenDynamicClientRegistrationAllowed() throws ServerException {
        return false;
    }

    @Override
    public boolean isRegistrationAccessTokenGenerationEnabled() throws ServerException {
        return false;
    }

    @Override
    public Map<String, AuthenticationMethod> getAcrMapping() throws ServerException {
        return new HashMap<>();
    }

    @Override
    public String getDefaultAcrValues() throws ServerException {
        return null;
    }

    @Override
    public Map<String, String> getAMRAuthModuleMappings() throws ServerException {
        return new HashMap<>();
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public ResourceSetStore getResourceSetStore() {
        return null;
    }

    @Override
    public boolean getClaimsParameterSupported() throws ServerException {
        return false;
    }

    @Override
    public String validateRequestedClaims(String requestedClaims) throws InvalidRequestException, ServerException {
        return null;
    }

    @Override
    public Set<String> getEndpointAuthMethodsSupported() {
        Set<String> supported = new HashSet<String>();

        for (Client.TokenEndpointAuthMethod method : Client.TokenEndpointAuthMethod.values()) {
            supported.add(method.getType());
        }

        return supported;
    }

    @Override
    public boolean isCodeVerifierRequired() throws ServerException {
        return false;
    }

    @Override
    public String getHashSalt() throws ServerException {
        return null;
    }

    @Override
    public boolean isAlwaysAddClaimsToToken() throws ServerException {
        return false;
    }

    @Override
    public String getUserDisplayNameAttribute() throws ServerException {
        return "cn";
    }

    @Override
    public String getJWKSUri() throws ServerException {
        return null;
    }

    @Override
    public Template getCustomLoginUrlTemplate() throws ServerException {
        return null;
    }

    @Override
    public String getVerificationUrl() throws ServerException {
        return null;
    }

    @Override
    public String getCompletionUrl() throws ServerException {
        return null;
    }

    @Override
    public int getDeviceCodeLifetime() throws ServerException {
        return 0;
    }

    @Override
    public int getDeviceCodePollInterval() throws ServerException {
        return 0;
    }

    @Override
    public boolean shouldStoreOpsTokens() throws ServerException {
        return false;
    }

    @Override
    public boolean clientsCanSkipConsent() throws ServerException {
        return true;
    }

    @Override
    public boolean isOpenIDConnectSSOProviderEnabled() throws ServerException {
        return true;
    }

    private OpenAMScopeValidator getScopeValidator() {
        return InjectorHolder.getInstance(OpenAMScopeValidator.class);
    }
}
