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
 * Copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.openam.oauth2.guice;

import static com.google.inject.name.Names.named;
import static org.forgerock.oauth2.core.AccessTokenVerifier.*;
import static org.forgerock.oauth2.core.OAuth2Constants.TokenEndpoint.*;
import static org.forgerock.oauth2.core.TokenStore.REALM_AGNOSTIC_TOKEN_STORE;
import static org.forgerock.openam.audit.AuditConstants.OAUTH2_AUDIT_CONTEXT_PROVIDERS;
import static org.forgerock.openam.rest.service.RestletUtils.wrap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverService;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverServiceImpl;
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
import org.forgerock.oauth2.core.DeviceCodeGrantTypeHandler;
import org.forgerock.oauth2.core.GrantTypeHandler;
import org.forgerock.oauth2.core.JwtBearerGrantTypeHandler;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.OAuth2TokenIntrospectionHandler;
import org.forgerock.oauth2.core.OAuth2UrisFactory;
import org.forgerock.oauth2.core.PasswordCredentialsGrantTypeHandler;
import org.forgerock.oauth2.core.PasswordCredentialsRequestValidator;
import org.forgerock.oauth2.core.PasswordCredentialsRequestValidatorImpl;
import org.forgerock.oauth2.core.ResourceOwnerAuthenticator;
import org.forgerock.oauth2.core.ResourceOwnerConsentVerifier;
import org.forgerock.oauth2.core.ResourceOwnerSessionValidator;
import org.forgerock.oauth2.core.TokenInfoService;
import org.forgerock.oauth2.core.TokenInfoServiceImpl;
import org.forgerock.oauth2.core.TokenIntrospectionHandler;
import org.forgerock.oauth2.core.TokenIntrospectionService;
import org.forgerock.oauth2.core.TokenIntrospectionServiceImpl;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.oauth2.restlet.AuthorizeRequestHook;
import org.forgerock.oauth2.restlet.OpenAMClientAuthenticationFailureFactory;
import org.forgerock.oauth2.restlet.RestletFormBodyAccessTokenVerifier;
import org.forgerock.oauth2.restlet.RestletHeaderAccessTokenVerifier;
import org.forgerock.oauth2.restlet.RestletOAuth2RequestFactory;
import org.forgerock.oauth2.restlet.RestletQueryParameterAccessTokenVerifier;
import org.forgerock.oauth2.restlet.TokenRequestHook;
import org.forgerock.oauth2.restlet.resources.ResourceSetRegistrationExceptionFilter;
import org.forgerock.oauth2.restlet.resources.ResourceSetRegistrationHook;
import org.forgerock.openam.core.RealmInfo;
import org.forgerock.openam.cts.adapters.JavaBeanAdapter;
import org.forgerock.openam.cts.api.tokens.TokenIdGenerator;
import org.forgerock.openam.oauth2.AccessTokenProtectionFilter;
import org.forgerock.openam.oauth2.ClientAuthenticatorImpl;
import org.forgerock.openam.oauth2.CookieExtractor;
import org.forgerock.openam.oauth2.OAuth2AuditLogger;
import org.forgerock.openam.oauth2.OAuthTokenStore;
import org.forgerock.openam.oauth2.OpenAMClientDAO;
import org.forgerock.openam.oauth2.OpenAMClientRegistrationStore;
import org.forgerock.openam.oauth2.OpenAMOAuth2ProviderSettingsFactory;
import org.forgerock.openam.oauth2.OpenAMOAuth2UrisFactory;
import org.forgerock.openam.oauth2.OpenAMResourceOwnerAuthenticator;
import org.forgerock.openam.oauth2.OpenAMResourceOwnerSessionValidator;
import org.forgerock.openam.oauth2.OpenAMTokenStore;
import org.forgerock.openam.oauth2.resources.OpenAMResourceSetStore;
import org.forgerock.openam.oauth2.resources.ResourceSetRegistrationEndpoint;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.oauth2.resources.labels.LabelsGuiceModule;
import org.forgerock.openam.oauth2.saml2.core.Saml2GrantTypeHandler;
import org.forgerock.openam.oauth2.validation.OpenIDConnectURLValidator;
import org.forgerock.openam.openidconnect.OpenAMIdTokenResponseTypeHandler;
import org.forgerock.openam.openidconnect.OpenAMOpenIDConnectProvider;
import org.forgerock.openam.openidconnect.OpenAMOpenIdConnectClientRegistrationService;
import org.forgerock.openam.openidconnect.OpenAMOpenIdTokenIssuer;
import org.forgerock.openam.rest.audit.OAuth2AuditAccessTokenContextProvider;
import org.forgerock.openam.rest.audit.OAuth2AuditContextProvider;
import org.forgerock.openam.rest.audit.OAuth2AuditRefreshTokenContextProvider;
import org.forgerock.openam.rest.audit.OAuth2AuditSSOTokenContextProvider;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openam.scripting.ScriptEngineConfiguration;
import org.forgerock.openam.sm.datalayer.utils.ThreadSafeTokenIdGenerator;
import org.forgerock.openam.utils.OpenAMSettings;
import org.forgerock.openam.utils.OpenAMSettingsImpl;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.openidconnect.ClaimsParameterValidator;
import org.forgerock.openidconnect.ClientDAO;
import org.forgerock.openidconnect.CodeVerifierValidator;
import org.forgerock.openidconnect.IdTokenResponseTypeHandler;
import org.forgerock.openidconnect.OpenIDConnectProvider;
import org.forgerock.openidconnect.OpenIDTokenIssuer;
import org.forgerock.openidconnect.OpenIdConnectAuthorizeRequestValidator;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationService;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.forgerock.openidconnect.OpenIdConnectTokenStore;
import org.forgerock.openidconnect.OpenIdResourceOwnerConsentVerifier;
import org.forgerock.openidconnect.SubjectTypeValidator;
import org.forgerock.openidconnect.UserInfoService;
import org.forgerock.openidconnect.UserInfoServiceImpl;
import org.forgerock.openidconnect.restlet.LoginHintHook;
import org.restlet.Request;
import org.restlet.Restlet;

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
        bind(new TypeLiteral<OAuth2RequestFactory<?, Request>>() { }).to(RestletOAuth2RequestFactory.class);
        bind(ResourceOwnerConsentVerifier.class).to(OpenIdResourceOwnerConsentVerifier.class);
        bind(ClientRegistrationStore.class).to(OpenAMClientRegistrationStore.class);
        bind(OpenIdConnectClientRegistrationStore.class).to(OpenAMClientRegistrationStore.class);
        bind(OAuth2ProviderSettingsFactory.class).to(OpenAMOAuth2ProviderSettingsFactory.class);
        bind(OAuth2ProviderSettingsFactory.class).to(OpenAMOAuth2ProviderSettingsFactory.class);
        bind(ResourceOwnerSessionValidator.class).to(OpenAMResourceOwnerSessionValidator.class);
        bind(ClientAuthenticator.class).to(ClientAuthenticatorImpl.class);
        bind(TokenStore.class).to(OpenAMTokenStore.class);
        bind(OpenIdConnectTokenStore.class).to(OpenAMTokenStore.class);
        bind(AccessTokenService.class).to(AccessTokenServiceImpl.class);
        bind(ResourceOwnerAuthenticator.class).to(OpenAMResourceOwnerAuthenticator.class);
        bind(IdTokenResponseTypeHandler.class).to(OpenAMIdTokenResponseTypeHandler.class);
        bind(UserInfoService.class).to(UserInfoServiceImpl.class);
        bind(TokenInfoService.class).to(TokenInfoServiceImpl.class);
        bind(ClientAuthenticationFailureFactory.class).to(OpenAMClientAuthenticationFailureFactory.class);
        bind(AccessTokenVerifier.class).to(RestletHeaderAccessTokenVerifier.class);
        bind(AccessTokenVerifier.class).annotatedWith(named(HEADER)).to(RestletHeaderAccessTokenVerifier.class);
        bind(AccessTokenVerifier.class).annotatedWith(named(FORM_BODY)).to(RestletFormBodyAccessTokenVerifier.class);
        bind(AccessTokenVerifier.class).annotatedWith(named(QUERY_PARAM)).to(RestletQueryParameterAccessTokenVerifier.class);
        bind(OpenIDConnectProvider.class).to(OpenAMOpenIDConnectProvider.class);
        bind(ClientDAO.class).to(OpenAMClientDAO.class);
        bind(OpenIdConnectClientRegistrationService.class).to(OpenAMOpenIdConnectClientRegistrationService.class);
        bind(OpenAMSettings.class).toProvider(new Provider<OpenAMSettings>() {
            public OpenAMSettings get() {
                return new OpenAMSettingsImpl(OAuth2Constants.OAuth2ProviderService.NAME,
                        OAuth2Constants.OAuth2ProviderService.VERSION);
            }
        });
        bind(OpenIDTokenIssuer.class).to(OpenAMOpenIdTokenIssuer.class);

        final Multibinder<AuthorizeRequestValidator> authorizeRequestValidators =
                Multibinder.newSetBinder(binder(), AuthorizeRequestValidator.class);

        authorizeRequestValidators.addBinding().to(AuthorizeRequestValidatorImpl.class);
        authorizeRequestValidators.addBinding().to(OpenIdConnectAuthorizeRequestValidator.class);
        authorizeRequestValidators.addBinding().to(ClaimsParameterValidator.class);
        authorizeRequestValidators.addBinding().to(SubjectTypeValidator.class);
        authorizeRequestValidators.addBinding().to(CodeVerifierValidator.class);

        final Multibinder<AuthorizationCodeRequestValidator> authorizationCodeRequestValidators =
                Multibinder.newSetBinder(binder(), AuthorizationCodeRequestValidator.class);
        authorizationCodeRequestValidators.addBinding().to(AuthorizationCodeRequestValidatorImpl.class);

        final Multibinder<ClientCredentialsRequestValidator> clientCredentialsRequestValidators =
                Multibinder.newSetBinder(binder(), ClientCredentialsRequestValidator.class);
        clientCredentialsRequestValidators.addBinding().to(ClientCredentialsRequestValidatorImpl.class);

        final Multibinder<PasswordCredentialsRequestValidator> passwordCredentialsRequestValidators =
                Multibinder.newSetBinder(binder(), PasswordCredentialsRequestValidator.class);
        passwordCredentialsRequestValidators.addBinding().to(PasswordCredentialsRequestValidatorImpl.class);

        final MapBinder<String, GrantTypeHandler> grantTypeHandlers =
                MapBinder.newMapBinder(binder(), String.class, GrantTypeHandler.class);
        grantTypeHandlers.addBinding(CLIENT_CREDENTIALS).to(ClientCredentialsGrantTypeHandler.class);
        grantTypeHandlers.addBinding(PASSWORD).to(PasswordCredentialsGrantTypeHandler.class);
        grantTypeHandlers.addBinding(AUTHORIZATION_CODE).to(AuthorizationCodeGrantTypeHandler.class);
        grantTypeHandlers.addBinding(DEVICE_CODE).to(DeviceCodeGrantTypeHandler.class);
        grantTypeHandlers.addBinding(JWT_BEARER).to(JwtBearerGrantTypeHandler.class);
        grantTypeHandlers.addBinding(OAuth2Constants.TokenEndpoint.SAML2_BEARER).to(Saml2GrantTypeHandler.class);

        final Multibinder<AuthorizeRequestHook> authorizeRequestHooks = Multibinder.newSetBinder(
                binder(), AuthorizeRequestHook.class);
        authorizeRequestHooks.addBinding().to(LoginHintHook.class);

        final Multibinder<TokenRequestHook> tokenRequestHooks = Multibinder.newSetBinder(
                binder(), TokenRequestHook.class);
        tokenRequestHooks.addBinding().to(LoginHintHook.class);

        install(new FactoryModuleBuilder()
                .implement(ResourceSetStore.class, OpenAMResourceSetStore.class)
                .build(ResourceSetStoreFactory.class));

        bind(TokenIdGenerator.class).to(ThreadSafeTokenIdGenerator.class);

        Multibinder.newSetBinder(binder(), TokenIntrospectionHandler.class)
                .addBinding().to(OAuth2TokenIntrospectionHandler.class);
        bind(TokenIntrospectionService.class).to(TokenIntrospectionServiceImpl.class);

        Multibinder.newSetBinder(binder(), ResourceSetRegistrationHook.class);

        bind(OpenIDConnectURLValidator.class).toInstance(OpenIDConnectURLValidator.getInstance());
        install(new LabelsGuiceModule());

        bind(OAuth2UrisFactory.class).to(OpenAMOAuth2UrisFactory.class);
        bind(new TypeLiteral<OAuth2UrisFactory<RealmInfo>>() {}).to(OpenAMOAuth2UrisFactory.class);
    }

    @Provides
    @Singleton
    @Named(OAuth2Constants.Custom.JWK_RESOLVER)
    OpenIdResolverService getOpenIdResolverService() {
        return new OpenIdResolverServiceImpl(3000, 3000);
    }

    private BlockingQueue<Runnable> getThreadPoolQueue(final int size) {
        return size == ScriptEngineConfiguration.UNBOUNDED_QUEUE_SIZE
                ? new LinkedBlockingQueue<Runnable>()
                : new LinkedBlockingQueue<Runnable>(size);
    }

    @Provides
    @Inject
    public JavaBeanAdapter<ResourceSetDescription> getResourceSetDescriptionAdapter(TokenIdGenerator idFactory) {
        return new JavaBeanAdapter<ResourceSetDescription>(ResourceSetDescription.class, idFactory);
    }

    @Inject
    @Provides
    @Named(REALM_AGNOSTIC_HEADER)
    @Singleton
    AccessTokenVerifier getRealmAgnosticHeaderAccessTokenVerifier(
            @Named(REALM_AGNOSTIC_TOKEN_STORE) TokenStore tokenStore) {
        return new RestletHeaderAccessTokenVerifier(tokenStore);
    }

    @Inject
    @Provides
    @Named(REALM_AGNOSTIC_FORM_BODY)
    @Singleton
    AccessTokenVerifier getRealmAgnosticFormBodyAccessTokenVerifier(
            @Named(REALM_AGNOSTIC_TOKEN_STORE) TokenStore tokenStore) {
        return new RestletFormBodyAccessTokenVerifier(tokenStore);
    }

    @Inject
    @Provides
    @Named(REALM_AGNOSTIC_QUERY_PARAM)
    @Singleton
    AccessTokenVerifier getRealmAgnosticQueryParamAccessTokenVerifier(
            @Named(REALM_AGNOSTIC_TOKEN_STORE) TokenStore tokenStore) {
        return new RestletQueryParameterAccessTokenVerifier(tokenStore);
    }

    @Inject
    @Provides
    @Named(REALM_AGNOSTIC_TOKEN_STORE)
    @Singleton
    TokenStore getRealmAgnosticTokenStore(OAuthTokenStore oauthTokenStore,
            OAuth2ProviderSettingsFactory providerSettingsFactory, OAuth2UrisFactory<RealmInfo> oauth2UrisFactory,
            OpenIdConnectClientRegistrationStore clientRegistrationStore, RealmNormaliser realmNormaliser,
            SSOTokenManager ssoTokenManager, CookieExtractor cookieExtractor, OAuth2AuditLogger auditLogger,
            @Named(OAuth2Constants.DEBUG_LOG_NAME) Debug debug, SecureRandom secureRandom,
            ClientAuthenticationFailureFactory failureFactory) {
        return new RealmAgnosticTokenStore(oauthTokenStore, providerSettingsFactory, oauth2UrisFactory,
                clientRegistrationStore, realmNormaliser, ssoTokenManager, cookieExtractor, auditLogger, debug,
                secureRandom, failureFactory);
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

    @Inject
    @Provides
    @Singleton
    @Named(OAuth2Constants.Custom.RSR_ENDPOINT)
    public Restlet createResourceSetRegistrationEndpoint(TokenStore store, OAuth2RequestFactory<?, Request> reqFactory,
            JacksonRepresentationFactory jacksonRepresentationFactory) {
        return new ResourceSetRegistrationExceptionFilter(
                new AccessTokenProtectionFilter(null, store, reqFactory, wrap(ResourceSetRegistrationEndpoint.class)),
                jacksonRepresentationFactory);
    }

    public static class RealmAgnosticTokenStore extends OpenAMTokenStore {

        public RealmAgnosticTokenStore(OAuthTokenStore tokenStore,
                OAuth2ProviderSettingsFactory providerSettingsFactory, OAuth2UrisFactory<RealmInfo> oauth2UrisFactory,
                OpenIdConnectClientRegistrationStore clientRegistrationStore, RealmNormaliser realmNormaliser,
                SSOTokenManager ssoTokenManager, CookieExtractor cookieExtractor, OAuth2AuditLogger auditLogger,
                Debug debug, SecureRandom secureRandom, ClientAuthenticationFailureFactory failureFactory) {
            super(tokenStore, providerSettingsFactory, oauth2UrisFactory, clientRegistrationStore, realmNormaliser,
                    ssoTokenManager, cookieExtractor, auditLogger, debug, secureRandom, failureFactory);
        }

        @Override
        protected void validateTokenRealm(String tokenRealm, OAuth2Request request) throws InvalidGrantException {
            //No need to validate the realm for the provided token.
        }
    }

    @Inject
    @Provides
    @Singleton
    @Named(OAUTH2_AUDIT_CONTEXT_PROVIDERS)
    Set<OAuth2AuditContextProvider> getOAuth2AuditContextProviders(TokenStore tokenStore,
            OAuth2RequestFactory<?, Request> requestFactory) {
        Set<OAuth2AuditContextProvider> set = new HashSet<>();

        set.add(new OAuth2AuditAccessTokenContextProvider(tokenStore, requestFactory));
        set.add(new OAuth2AuditRefreshTokenContextProvider(tokenStore, requestFactory));
        set.add(new OAuth2AuditSSOTokenContextProvider());

        return set;
    }
}
