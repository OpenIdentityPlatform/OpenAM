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
import com.google.inject.TypeLiteral;
import com.sun.identity.idm.AMIdentity;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.oauth2.core.ClientAuthenticator;
import org.forgerock.oauth2.core.ClientCredentialsGrantTypeHandler;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.GrantType;
import org.forgerock.oauth2.core.GrantTypeHandler;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.PasswordGrantTypeHandler;
import org.forgerock.oauth2.core.ResourceOwnerAuthenticator;
import org.forgerock.oauth2.core.ScopeValidator;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.reslet.ClientCredentialsExtractor;
import org.forgerock.oauth2.reslet.ResourceOwnerCredentialsExtractor;
import org.forgerock.openam.noauth2.wrappers.ClientAuthenticatorImpl;
import org.forgerock.openam.noauth2.wrappers.OpenAMClientRegistrationStore;
import org.forgerock.openam.noauth2.wrappers.ResourceOwnerAuthenticatorImpl;
import org.forgerock.openam.noauth2.wrappers.ScopeValidatorImpl;
import org.forgerock.openam.noauth2.wrappers.TokenStoreImpl;
import org.forgerock.openam.noauth2.wrappers.restlet.OpenAMClientCredentialsExtractor;
import org.forgerock.openam.noauth2.wrappers.restlet.OpenAMResourceOwnerCredentialsExtractor;
import org.forgerock.openam.oauth2.ext.cts.repo.DefaultOAuthTokenStoreImpl;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.openam.oauth2.provider.Scope;
import org.forgerock.openam.oauth2.provider.impl.OAuth2ProviderSettingsImpl;
import org.forgerock.openam.oauth2.provider.impl.ScopeImpl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

import static org.forgerock.oauth2.core.AccessTokenService.GRANT_TYPE_HANDLERS_INJECT_KEY;

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
        bind(ResourceOwnerAuthenticator.class).to(ResourceOwnerAuthenticatorImpl.class).in(Singleton.class);
        bind(ScopeValidator.class).to(ScopeValidatorImpl.class).in(Singleton.class);
        bind(TokenStore.class).to(TokenStoreImpl.class).in(Singleton.class);
        bind(ClientCredentialsExtractor.class).to(OpenAMClientCredentialsExtractor.class).in(Singleton.class);
        bind(ResourceOwnerCredentialsExtractor.class).to(OpenAMResourceOwnerCredentialsExtractor.class).
                in(Singleton.class);
        bind(new TypeLiteral<ClientRegistrationStore<AMIdentity>>() {
        }).to(OpenAMClientRegistrationStore.class)
                .in(Singleton.class);
        bind(OAuth2TokenStore.class).to(DefaultOAuthTokenStoreImpl.class).in(Singleton.class);
        bind(Scope.class).to(ScopeImpl.class).in(Singleton.class); //TODO this should be behind a provider as can be configured at runtime!
        bind(OAuth2ProviderSettings.class).to(OAuth2ProviderSettingsImpl.class).in(Singleton.class);
    }

    @Inject
    @Provides
    @Singleton
    @Named(GRANT_TYPE_HANDLERS_INJECT_KEY)
    Map<? extends GrantType, ? extends GrantTypeHandler> getGrantTypeHandlers(
            final ClientCredentialsGrantTypeHandler clientCredentialsGrantTypeHandler,
            final PasswordGrantTypeHandler passwordGrantTypeHandler) {
        final Map<GrantType, GrantTypeHandler> grantTypeHandlers = new HashMap<GrantType, GrantTypeHandler>();

        grantTypeHandlers.put(GrantType.DefaultGrantType.CLIENT_CREDENTIALS, clientCredentialsGrantTypeHandler);
        grantTypeHandlers.put(GrantType.DefaultGrantType.PASSWORD, passwordGrantTypeHandler);

        return grantTypeHandlers;
    }
}
