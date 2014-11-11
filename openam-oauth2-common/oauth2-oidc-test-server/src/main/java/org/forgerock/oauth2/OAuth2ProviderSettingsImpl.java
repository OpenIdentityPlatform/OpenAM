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

package org.forgerock.oauth2;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.jose.jwk.JWKSet;
import org.forgerock.json.jose.utils.KeystoreManager;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AuthenticationMethod;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.ResponseTypeHandler;
import org.forgerock.oauth2.core.ScopeValidator;
import org.forgerock.oauth2.core.Token;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.forgerock.server.ConfigurationResource;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.forgerock.oauth2.core.Utils.isEmpty;
import static org.forgerock.oauth2.core.Utils.joinScope;

/**
 * @since 12.0.0
 */
public class OAuth2ProviderSettingsImpl implements OAuth2ProviderSettings {

    private final String deploymentUrl;
    private final ConfigurationResource configurationResource;
    private final Map<String, String> savedConsents = new HashMap<String, String>();

    private ScopeValidator scopeValidator;

    public OAuth2ProviderSettingsImpl(final String deploymentUrl, final ConfigurationResource configurationResource) {
        this.deploymentUrl = deploymentUrl;
        this.configurationResource = configurationResource;
    }

    public Map<String, ResponseTypeHandler> getAllowedResponseTypes() throws UnsupportedResponseTypeException, ServerException {
        try {
            final Iterator<JsonValue> iter = configurationResource.getConfiguration().get("allowedResponseTypes").iterator();

            final HashMap<String, ResponseTypeHandler> responseTypeHandlers = new HashMap<String, ResponseTypeHandler>();
            while (iter.hasNext()) {
                final JsonValue responseTypeHandler = iter.next();
                    responseTypeHandlers.put(responseTypeHandler.get("responseType").asString(),
                            InjectorHolder.getInstance(Class.forName(responseTypeHandler.get("handler").asString())
                                    .asSubclass(ResponseTypeHandler.class)));
            }
            return responseTypeHandlers;

        } catch (ClassNotFoundException e) {
            throw new ServerException(e);
        }
    }

    public boolean isConsentSaved(ResourceOwner resourceOwner, String clientId, Set<String> scope) {

        final String consent = savedConsents.get(resourceOwner.getId());
        if (consent == null) {
            return false;
        }

        final String[] split = consent.split(" ");
        if (!split[0].equals(clientId)) {
            return false;
        }

        return split[1].equals(joinScope(scope));
    }

    private synchronized ScopeValidator getScopeValidator() throws ServerException {
        if (scopeValidator == null) {
            try {
                final String scopeValidatorClassName = configurationResource.getConfiguration().get("scopeValidator")
                        .asString();
                if (isEmpty(scopeValidatorClassName)) {
                    throw new ServerException("Scope Validator class not set.");
                }

                final Class<?> scopeValidatorClass = Class.forName(scopeValidatorClassName);

                scopeValidator = InjectorHolder.getInstance(scopeValidatorClass.asSubclass(ScopeValidator.class));

            } catch (ClassNotFoundException e) {
                throw new ServerException(e);
            }
        }
        return scopeValidator;
    }

    public Set<String> validateAuthorizationScope(ClientRegistration clientRegistration, Set<String> scope, OAuth2Request request) throws ServerException, InvalidScopeException {
        return getScopeValidator().validateAuthorizationScope(clientRegistration, scope, request);
    }

    public Set<String> validateAccessTokenScope(ClientRegistration clientRegistration, Set<String> scope, OAuth2Request request) throws ServerException, InvalidScopeException {
        return getScopeValidator().validateAccessTokenScope(clientRegistration, scope, request);
    }

    public Set<String> validateRefreshTokenScope(ClientRegistration clientRegistration, Set<String> requestedScope, Set<String> tokenScope, OAuth2Request request) throws ServerException, InvalidScopeException {
        return getScopeValidator().validateRefreshTokenScope(clientRegistration, requestedScope, tokenScope, request);
    }

    public Map<String, Object> getUserInfo(AccessToken token, OAuth2Request request) throws ServerException, UnauthorizedClientException {
        return getScopeValidator().getUserInfo(token, request);
    }

    public Map<String, Object> evaluateScope(AccessToken accessToken) throws ServerException {
        return getScopeValidator().evaluateScope(accessToken);
    }

    public Map<String, String> additionalDataToReturnFromAuthorizeEndpoint(Map<String, Token> tokens, OAuth2Request request) throws ServerException {
        return getScopeValidator().additionalDataToReturnFromAuthorizeEndpoint(tokens, request);
    }

    public void additionalDataToReturnFromTokenEndpoint(AccessToken accessToken, OAuth2Request request) throws ServerException, InvalidClientException {
        getScopeValidator().additionalDataToReturnFromTokenEndpoint(accessToken, request);
    }

    public void saveConsent(ResourceOwner resourceOwner, String clientId, Set<String> scope) {
        savedConsents.put(resourceOwner.getId(), clientId + " " + joinScope(scope));
    }

    public boolean issueRefreshTokens() throws ServerException {
        return configurationResource.getConfiguration().get("issueRefreshTokens").asBoolean();
    }

    public boolean issueRefreshTokensOnRefreshingToken() throws ServerException {
        return configurationResource.getConfiguration().get("issueRefreshTokensOnRefreshingToken").asBoolean();
    }

    public long getAuthorizationCodeLifetime() throws ServerException {
        return configurationResource.getConfiguration().get("authorizationCodeLifetime").asLong();
    }

    public long getAccessTokenLifetime() throws ServerException {
        return configurationResource.getConfiguration().get("accessTokenLifetime").asLong();
    }

    public long getOpenIdTokenLifetime() throws ServerException {
        return configurationResource.getConfiguration().get("openIdTokenLifetime").asLong();
    }

    public long getRefreshTokenLifetime() throws ServerException {
        return configurationResource.getConfiguration().get("refreshTokenLifetime").asLong();
    }

    public KeyPair getServerKeyPair() throws ServerException {
        final JsonValue keystore = configurationResource.getConfiguration().get("keystore");

        final String keystorePath = keystore.get("keystorePath").asString();
        final String keystoreType = keystore.get("keystoreType").asString();
        final String keystorePassword = keystore.get("keystorePassword").asString();
        final String keyAlias = keystore.get("keyAlias").asString();
        final String keyPassword = keystore.get("keyPassword").asString();

        final KeystoreManager keystoreManager = new KeystoreManager(keystoreType, keystorePath, keystorePassword);
        final PublicKey publicKey = keystoreManager.getPublicKey(keyAlias);
        final PrivateKey privateKey = keystoreManager.getPrivateKey(keyAlias, keyPassword);
        return new KeyPair(publicKey, privateKey);
    }

    public Set<String> getResourceOwnerAuthenticatedAttributes() throws ServerException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<String> getSupportedClaims() throws ServerException {
        return new HashSet<String>(configurationResource.getConfiguration().get("supportedClaims")
                .asList(String.class));
    }

    public Set<String> getSupportedIDTokenSigningAlgorithms() throws ServerException {
        return new HashSet<String>(configurationResource.getConfiguration().get("supportedOpenIdTokenSigningAlgorithms")
                .asList(String.class));
    }

    public String getOpenIDConnectVersion() {
        return "3.0";
    }

    public String getOpenIDConnectIssuer() {
        return deploymentUrl;
    }

    public String getAuthorizationEndpoint() {
        return deploymentUrl + "/oauth2/authorize";
    }

    public String getTokenEndpoint() {
        return deploymentUrl + "/oauth2/access_token";
    }

    public String getUserInfoEndpoint() {
        return deploymentUrl + "/oauth2/userinfo";
    }

    public String getCheckSessionEndpoint() {
        return deploymentUrl + "/oauth2/connect/checkSession";
    }

    public String getEndSessionEndpoint() {
        return deploymentUrl + "/oauth2/connect/endSession";
    }

    public String getJWKSUri() throws ServerException {
        return configurationResource.getConfiguration().get("jwksUri").asString();
    }

    @Override
    public JsonValue getJWKSet() throws ServerException {
        return new JsonValue(Collections.singletonMap("keys", new JWKSet().getJWKsAsList()));
    }

    public String getCreatedTimestampAttributeName() throws ServerException {
        return configurationResource.getConfiguration().get(OAuth2Constants.OAuth2ProviderService.CREATED_TIMESTAMP_ATTRIBUTE_NAME).asString();
    }

    public String getModifiedTimestampAttributeName() throws ServerException {
        return configurationResource.getConfiguration().get(OAuth2Constants.OAuth2ProviderService.MODIFIED_TIMESTAMP_ATTRIBUTE_NAME).asString();
    }

    public String getClientRegistrationEndpoint() {
        return deploymentUrl + "/oauth2/connect/register";
    }

    public Set<String> getSupportedSubjectTypes() throws ServerException {
        return new HashSet<String>(configurationResource.getConfiguration().get("supportedSubjectTypes")
                .asList(String.class));
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
        return Collections.emptyMap();
    }

    @Override
    public String getDefaultAcrValues() throws ServerException {
        return null;
    }

    @Override
    public Map<String, String> getAMRAuthModuleMappings() throws ServerException {
        return Collections.emptyMap();
    }

}
