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

package org.forgerock.openam.noauth2.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.oauth2.core.AuthorizationCodeGrantTypeHandler;
import org.forgerock.oauth2.core.ClientAuthenticator;
import org.forgerock.oauth2.core.ClientCredentialsGrantTypeHandler;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.ContextHandler;
import org.forgerock.oauth2.core.GrantType;
import org.forgerock.oauth2.core.GrantTypeHandler;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.PasswordGrantTypeHandler;
import org.forgerock.oauth2.core.Scope;
import org.forgerock.oauth2.core.ScopeFactory;
import org.forgerock.oauth2.core.ScopeValidator;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.reslet.ResourceOwnerAuthorizationCodeCredentialsExtractor;
import org.forgerock.oauth2.reslet.ResourceOwnerPasswordCredentialsExtractor;
import org.forgerock.openam.noauth2.wrappers.ClientAuthenticatorImpl;
import org.forgerock.openam.noauth2.wrappers.OpenAMClientRegistrationStore;
import org.forgerock.openam.noauth2.wrappers.OpenAMOAuth2ContextHandler;
import org.forgerock.openam.noauth2.wrappers.OpenAMOAuth2ProviderSettingsFactory;
import org.forgerock.openam.noauth2.wrappers.OpenAMResourceOwnerAuthorizationCodeCredentialsExtractor;
import org.forgerock.openam.noauth2.wrappers.OpenAMResourceOwnerPasswordCredentialsExtractor;
import org.forgerock.openam.noauth2.wrappers.ScopeValidatorImpl;
import org.forgerock.openam.noauth2.wrappers.TokenStoreImpl;
import org.forgerock.openam.oauth2.OAuth2Application;
import org.forgerock.openam.oauth2.ext.cts.repo.DefaultOAuthTokenStoreImpl;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.openam.oauth2.provider.impl.ScopeImpl;
import org.forgerock.restlet.ext.oauth2.flow.AbstractFlow;
import org.forgerock.restlet.ext.oauth2.flow.AuthorizationCodeServerResource;
import org.forgerock.restlet.ext.oauth2.flow.ClientCredentialsServerResource;
import org.forgerock.restlet.ext.oauth2.flow.PasswordServerResource;
import org.forgerock.restlet.ext.oauth2.flow.RefreshTokenServerResource;
import org.forgerock.openam.oauth2.saml2.restlet.Saml2BearerServerResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

import static org.forgerock.oauth2.core.AccessTokenService.GRANT_TYPE_HANDLERS_INJECT_KEY;
import static org.forgerock.openam.oauth2.OAuth2Application.OAUTH2_RESTLET_ENDPOINTS_INJECT_KEY;

/**
 * Guice Module that defines the bindings for the OAuth2 implementation.
 *
 * @since 12.0.0
 */
@GuiceModule
public class OAuth2GuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ClientAuthenticator.class).to(ClientAuthenticatorImpl.class).in(Singleton.class);
        bind(ScopeValidator.class).to(ScopeValidatorImpl.class);
        bind(TokenStore.class).to(TokenStoreImpl.class).in(Singleton.class);
        bind(ContextHandler.class).to(OpenAMOAuth2ContextHandler.class).in(Singleton.class);
        bind(ResourceOwnerPasswordCredentialsExtractor.class).to(OpenAMResourceOwnerPasswordCredentialsExtractor.class).
                in(Singleton.class);
        bind(ResourceOwnerAuthorizationCodeCredentialsExtractor.class)
                .to(OpenAMResourceOwnerAuthorizationCodeCredentialsExtractor.class).in(Singleton.class);
        bind(ClientRegistrationStore.class).to(OpenAMClientRegistrationStore.class).in(Singleton.class);
        bind(OAuth2TokenStore.class).to(DefaultOAuthTokenStoreImpl.class).in(Singleton.class);
        bind(OAuth2ProviderSettingsFactory.class).to(OpenAMOAuth2ProviderSettingsFactory.class).in(Singleton.class);

        install(new FactoryModuleBuilder().implement(Scope.class, ScopeImpl.class).build(ScopeFactory.class));
    }

    @Inject
    @Provides
    @Singleton
    @Named(GRANT_TYPE_HANDLERS_INJECT_KEY)
    Map<? extends GrantType, ? extends GrantTypeHandler> getGrantTypeHandlers(
            final ClientCredentialsGrantTypeHandler clientCredentialsGrantTypeHandler,
            final PasswordGrantTypeHandler passwordGrantTypeHandler,
            final AuthorizationCodeGrantTypeHandler authorizationCodeGrantTypeHandler) {
        final Map<GrantType, GrantTypeHandler> grantTypeHandlers = new HashMap<GrantType, GrantTypeHandler>();  //TODO need to use Map Binder so can be extended?...

        grantTypeHandlers.put(GrantType.DefaultGrantType.CLIENT_CREDENTIALS, clientCredentialsGrantTypeHandler);
        grantTypeHandlers.put(GrantType.DefaultGrantType.PASSWORD, passwordGrantTypeHandler);
        grantTypeHandlers.put(GrantType.DefaultGrantType.AUTHORIZATION_CODE, authorizationCodeGrantTypeHandler);

        return grantTypeHandlers;
    }

    @Provides
    @Singleton
    @Named(OAUTH2_RESTLET_ENDPOINTS_INJECT_KEY)
    Map<String, Class<? extends AbstractFlow>> getOAuth2EndpointClasses() {
        final Map<String, Class<? extends AbstractFlow>> endpointClasses =
                new HashMap<String, Class<? extends AbstractFlow>>();  //TODO need to use Map Binder so can be extended?...

        endpointClasses.put(OAuth2Constants.TokeEndpoint.AUTHORIZATION_CODE, AuthorizationCodeServerResource.class);
        endpointClasses.put(OAuth2Constants.TokeEndpoint.REFRESH_TOKEN, RefreshTokenServerResource.class);
        endpointClasses.put(OAuth2Constants.TokeEndpoint.CLIENT_CREDENTIALS, ClientCredentialsServerResource.class);
        endpointClasses.put(OAuth2Constants.TokeEndpoint.PASSWORD, PasswordServerResource.class);
        endpointClasses.put(OAuth2Constants.TokeEndpoint.SAML2_BEARER, Saml2BearerServerResource.class);                   //TODO does the contents of this map need to be configurable?...

        return endpointClasses;
    }
}
