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

package org.forgerock.openam.oauth2.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.oauth2.core.AccessTokenService;
import org.forgerock.oauth2.core.AccessTokenServiceImpl;
import org.forgerock.oauth2.core.AccessTokenVerifier;
import org.forgerock.oauth2.core.AuthorizationCodeGrantTypeHandler;
import org.forgerock.oauth2.core.AuthorizationCodeRequestValidator;
import org.forgerock.oauth2.core.AuthorizationCodeRequestValidatorImpl;
import org.forgerock.oauth2.core.AuthorizationService;
import org.forgerock.oauth2.core.AuthorizationServiceImpl;
import org.forgerock.oauth2.core.AuthorizeRequestValidator;
import org.forgerock.oauth2.core.AuthorizeRequestValidatorImpl;
import org.forgerock.oauth2.core.ClientAuthenticator;
import org.forgerock.oauth2.core.ClientCredentialsGrantTypeHandler;
import org.forgerock.oauth2.core.ClientCredentialsRequestValidator;
import org.forgerock.oauth2.core.ClientCredentialsRequestValidatorImpl;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.GrantTypeHandler;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.PasswordCredentialsGrantTypeHandler;
import org.forgerock.oauth2.core.PasswordCredentialsRequestValidator;
import org.forgerock.oauth2.core.PasswordCredentialsRequestValidatorImpl;
import org.forgerock.oauth2.core.ResourceOwnerAuthenticator;
import org.forgerock.oauth2.core.ResourceOwnerConsentVerifier;
import org.forgerock.oauth2.core.ResourceOwnerSessionValidator;
import org.forgerock.oauth2.core.TokenInfoService;
import org.forgerock.oauth2.core.TokenInfoServiceImpl;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.restlet.RefreshTokenResource;
import org.forgerock.oauth2.restlet.RestletHeaderAccessTokenVerifier;
import org.forgerock.oauth2.restlet.RestletOAuth2RequestFactory;
import org.forgerock.oauth2.restlet.TokenEndpointResource;
import org.forgerock.openam.oauth2.ClientAuthenticatorImpl;
import org.forgerock.openam.oauth2.OpenAMClientDAO;
import org.forgerock.openam.oauth2.OpenAMClientRegistrationStore;
import org.forgerock.openam.oauth2.OpenAMOAuth2ProviderSettingsFactory;
import org.forgerock.openam.oauth2.OpenAMResourceOwnerAuthenticator;
import org.forgerock.openam.oauth2.OpenAMResourceOwnerSessionValidator;
import org.forgerock.openam.oauth2.OpenAMSettings;
import org.forgerock.openam.oauth2.OpenAMSettingsImpl;
import org.forgerock.openam.oauth2.OpenAMTokenStore;
import org.forgerock.openam.openidconnect.OpenAMOpenIDConnectProvider;
import org.forgerock.openam.openidconnect.OpenAMOpenIdConnectClientRegistrationService;
import org.forgerock.openidconnect.ClientDAO;
import org.forgerock.openidconnect.OpenIDConnectProvider;
import org.forgerock.openidconnect.OpenIdConnectAuthorizeRequestValidator;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationService;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.forgerock.openidconnect.OpenIdConnectTokenStore;
import org.forgerock.openidconnect.OpenIdResourceOwnerConsentVerifier;
import org.forgerock.openidconnect.UserInfoService;
import org.forgerock.openidconnect.UserInfoServiceImpl;
import org.restlet.Request;
import org.restlet.resource.ServerResource;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Guice module for OAuth2/OpenId Connect provider bindings.
 *
 * @since 12.0.0
 */
@GuiceModule
public class OAuth2GuiceModule extends AbstractModule {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        bind(AuthorizationService.class).to(AuthorizationServiceImpl.class);
        bind(new TypeLiteral<OAuth2RequestFactory<Request>>(){}).to(RestletOAuth2RequestFactory.class);
        bind(ResourceOwnerConsentVerifier.class).to(OpenIdResourceOwnerConsentVerifier.class);
        bind(ClientRegistrationStore.class).to(OpenAMClientRegistrationStore.class);
        bind(OpenIdConnectClientRegistrationStore.class).to(OpenAMClientRegistrationStore.class);
        bind(OAuth2ProviderSettingsFactory.class).to(OpenAMOAuth2ProviderSettingsFactory.class);
        bind(ResourceOwnerSessionValidator.class).to(OpenAMResourceOwnerSessionValidator.class);
        bind(ClientAuthenticator.class).to(ClientAuthenticatorImpl.class);
        bind(TokenStore.class).to(OpenAMTokenStore.class);
        bind(OpenIdConnectTokenStore.class).to(OpenAMTokenStore.class);
        bind(AccessTokenService.class).to(AccessTokenServiceImpl.class);
        bind(ResourceOwnerAuthenticator.class).to(OpenAMResourceOwnerAuthenticator.class);
        bind(UserInfoService.class).to(UserInfoServiceImpl.class);
        bind(TokenInfoService.class).to(TokenInfoServiceImpl.class);
        bind(AccessTokenVerifier.class).to(RestletHeaderAccessTokenVerifier.class);
        bind(OpenIDConnectProvider.class).to(OpenAMOpenIDConnectProvider.class);
        bind(ClientDAO.class).to(OpenAMClientDAO.class);
        bind(OpenIdConnectClientRegistrationService.class).to(OpenAMOpenIdConnectClientRegistrationService.class);
        bind(OpenAMSettings.class).toProvider(new Provider<OpenAMSettings>() {
            public OpenAMSettings get() {
                return new OpenAMSettingsImpl(OAuth2Constants.OAuth2ProviderService.NAME,
                        OAuth2Constants.OAuth2ProviderService.VERSION);
            }
        });

        final Multibinder<AuthorizeRequestValidator> authorizeRequestValidators =
                Multibinder.newSetBinder(binder(), AuthorizeRequestValidator.class);

        authorizeRequestValidators.addBinding().to(AuthorizeRequestValidatorImpl.class);
        authorizeRequestValidators.addBinding().to(OpenIdConnectAuthorizeRequestValidator.class);

        final Multibinder<AuthorizationCodeRequestValidator> authorizationCodeRequestValidators =
                Multibinder.newSetBinder(binder(), AuthorizationCodeRequestValidator.class);
        authorizationCodeRequestValidators.addBinding().to(AuthorizationCodeRequestValidatorImpl.class);

        final Multibinder<ClientCredentialsRequestValidator> clientCredentialsRequestValidators =
                Multibinder.newSetBinder(binder(), ClientCredentialsRequestValidator.class);
        clientCredentialsRequestValidators.addBinding().to(ClientCredentialsRequestValidatorImpl.class);

        final Multibinder<PasswordCredentialsRequestValidator> passwordCredentialsRequestValidators =
                Multibinder.newSetBinder(binder(), PasswordCredentialsRequestValidator.class);
        passwordCredentialsRequestValidators.addBinding().to(PasswordCredentialsRequestValidatorImpl.class);


        final MapBinder<String, GrantTypeHandler> grantTypeHandlers = MapBinder.newMapBinder(binder(), String.class, GrantTypeHandler.class);
        grantTypeHandlers.addBinding("client_credentials").to(ClientCredentialsGrantTypeHandler.class);
        grantTypeHandlers.addBinding("password").to(PasswordCredentialsGrantTypeHandler.class);
        grantTypeHandlers.addBinding("authorization_code").to(AuthorizationCodeGrantTypeHandler.class);


        final MapBinder<String, ServerResource> oauth2Endpoint = MapBinder.newMapBinder(binder(), String.class, ServerResource.class);
        oauth2Endpoint.addBinding(OAuth2Constants.TokeEndpoint.AUTHORIZATION_CODE).to(TokenEndpointResource.class);
        oauth2Endpoint.addBinding(OAuth2Constants.TokeEndpoint.REFRESH_TOKEN).to(RefreshTokenResource.class);
        oauth2Endpoint.addBinding(OAuth2Constants.TokeEndpoint.CLIENT_CREDENTIALS).to(TokenEndpointResource.class);
        oauth2Endpoint.addBinding(OAuth2Constants.TokeEndpoint.PASSWORD).to(TokenEndpointResource.class);
    }

    @Inject
    @Provides
    @Singleton
    List<AuthorizeRequestValidator> getAuthorizeRequestValidators(
            final Set<AuthorizeRequestValidator> authorizeRequestValidators) {
        return new ArrayList<AuthorizeRequestValidator>(authorizeRequestValidators);
    }

    @Inject
    @Provides
    @Singleton
    List<AuthorizationCodeRequestValidator> getAuthorizationCodeRequestValidators(
            final Set<AuthorizationCodeRequestValidator> authorizationCodeRequestValidators) {
        return new ArrayList<AuthorizationCodeRequestValidator>(authorizationCodeRequestValidators);
    }

    @Inject
    @Provides
    @Singleton
    List<ClientCredentialsRequestValidator> getClientCredentialsRequestValidators(
            final Set<ClientCredentialsRequestValidator> clientCredentialsRequestValidators) {
        return new ArrayList<ClientCredentialsRequestValidator>(clientCredentialsRequestValidators);
    }

    @Inject
    @Provides
    @Singleton
    List<PasswordCredentialsRequestValidator> getPasswordCredentialsRequestValidators(
            final Set<PasswordCredentialsRequestValidator> passwordCredentialsRequestValidators) {
        return new ArrayList<PasswordCredentialsRequestValidator>(passwordCredentialsRequestValidators);
    }
}
