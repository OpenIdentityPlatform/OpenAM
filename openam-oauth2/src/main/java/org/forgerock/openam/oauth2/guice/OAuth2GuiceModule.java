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
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.oauth2.guice;

import static com.google.inject.name.Names.named;
import static org.forgerock.oauth2.core.AccessTokenVerifier.FORM_BODY;
import static org.forgerock.oauth2.core.AccessTokenVerifier.HEADER;
import static org.forgerock.oauth2.core.AccessTokenVerifier.QUERY_PARAM;
import static org.forgerock.oauth2.core.AccessTokenVerifier.REALM_AGNOSTIC_FORM_BODY;
import static org.forgerock.oauth2.core.AccessTokenVerifier.REALM_AGNOSTIC_HEADER;
import static org.forgerock.oauth2.core.AccessTokenVerifier.REALM_AGNOSTIC_QUERY_PARAM;
import static org.forgerock.oauth2.core.TokenStore.REALM_AGNOSTIC_TOKEN_STORE;
import static org.forgerock.openam.oauth2.OAuth2Constants.TokenEndpoint.AUTHORIZATION_CODE;
import static org.forgerock.openam.oauth2.OAuth2Constants.TokenEndpoint.CLIENT_CREDENTIALS;
import static org.forgerock.openam.oauth2.OAuth2Constants.TokenEndpoint.DEVICE_CODE;
import static org.forgerock.openam.oauth2.OAuth2Constants.TokenEndpoint.JWT_BEARER;
import static org.forgerock.openam.oauth2.OAuth2Constants.TokenEndpoint.PASSWORD;
import static org.forgerock.openam.rest.service.RestletUtils.wrap;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.forgerock.guice.core.GuiceModule;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverService;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverServiceImpl;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.oauth2.core.AccessTokenVerifier;
import org.forgerock.oauth2.core.AuthorizationCodeGrantTypeHandler;
import org.forgerock.oauth2.core.AuthorizationCodeRequestValidator;
import org.forgerock.oauth2.core.AuthorizationCodeRequestValidatorImpl;
import org.forgerock.oauth2.core.AuthorizeRequestValidator;
import org.forgerock.oauth2.core.AuthorizeRequestValidatorImpl;
import org.forgerock.oauth2.core.ClientCredentialsGrantTypeHandler;
import org.forgerock.oauth2.core.ClientCredentialsRequestValidator;
import org.forgerock.oauth2.core.ClientCredentialsRequestValidatorImpl;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.DeviceCodeGrantTypeHandler;
import org.forgerock.oauth2.core.DuplicateRequestParameterValidator;
import org.forgerock.oauth2.core.GrantTypeHandler;
import org.forgerock.oauth2.core.JwtBearerGrantTypeHandler;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.OAuth2TokenIntrospectionHandler;
import org.forgerock.oauth2.core.PasswordCredentialsGrantTypeHandler;
import org.forgerock.oauth2.core.PasswordCredentialsRequestValidator;
import org.forgerock.oauth2.core.PasswordCredentialsRequestValidatorImpl;
import org.forgerock.oauth2.core.RedirectUriResolver;
import org.forgerock.oauth2.core.ResourceOwnerConsentVerifier;
import org.forgerock.oauth2.core.TokenIntrospectionHandler;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.oauth2.restlet.AuthorizeRequestHook;
import org.forgerock.oauth2.restlet.OpenAMClientAuthenticationFailureFactory;
import org.forgerock.oauth2.restlet.RestletFormBodyAccessTokenVerifier;
import org.forgerock.oauth2.restlet.RestletHeaderAccessTokenVerifier;
import org.forgerock.oauth2.restlet.RestletQueryParameterAccessTokenVerifier;
import org.forgerock.oauth2.restlet.TokenRequestHook;
import org.forgerock.oauth2.restlet.resources.ResourceSetRegistrationExceptionFilter;
import org.forgerock.oauth2.restlet.resources.ResourceSetRegistrationHook;
import org.forgerock.openam.audit.context.AMExecutorServiceFactory;
import org.forgerock.openam.blacklist.Blacklist;
import org.forgerock.openam.blacklist.Blacklistable;
import org.forgerock.openam.blacklist.BloomFilterBlacklist;
import org.forgerock.openam.blacklist.CTSBlacklist;
import org.forgerock.openam.blacklist.CachingBlacklist;
import org.forgerock.openam.blacklist.NoOpBlacklist;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.JavaBeanAdapter;
import org.forgerock.openam.cts.adapters.TokenAdapter;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.tokens.TokenIdGenerator;
import org.forgerock.openam.oauth2.AccessTokenProtectionFilter;
import org.forgerock.openam.oauth2.CookieExtractor;
import org.forgerock.openam.oauth2.OAuth2AuditLogger;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuth2GlobalSettings;
import org.forgerock.openam.oauth2.OAuth2UrisFactory;
import org.forgerock.openam.oauth2.OAuth2Utils;
import org.forgerock.openam.oauth2.OAuthTokenStore;
import org.forgerock.openam.oauth2.OpenAMClientRegistrationStore;
import org.forgerock.openam.oauth2.OpenAMTokenStore;
import org.forgerock.openam.oauth2.ResourceSetDescription;
import org.forgerock.openam.oauth2.StatefulTokenStore;
import org.forgerock.openam.oauth2.StatelessCheck;
import org.forgerock.openam.oauth2.StatelessTokenCtsAdapter;
import org.forgerock.openam.oauth2.StatelessTokenMetadata;
import org.forgerock.openam.oauth2.StatelessTokenStore;
import org.forgerock.openam.oauth2.resources.OpenAMResourceSetStore;
import org.forgerock.openam.oauth2.resources.ResourceSetRegistrationEndpoint;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.oauth2.resources.labels.LabelsGuiceModule;
import org.forgerock.openam.oauth2.validation.ConfirmationKeyValidator;
import org.forgerock.openam.oauth2.validation.OpenIDConnectURLValidator;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openam.scripting.ScriptEngineConfiguration;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.forgerock.openam.sm.datalayer.utils.ThreadSafeTokenIdGenerator;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.utils.OpenAMSettings;
import org.forgerock.openam.utils.OpenAMSettingsImpl;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.openam.utils.RecoveryCodeGenerator;
import org.forgerock.openidconnect.ClaimsParameterValidator;
import org.forgerock.openidconnect.CodeVerifierValidator;
import org.forgerock.openidconnect.OpenIdConnectAuthorizeRequestValidator;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.forgerock.openidconnect.OpenIdConnectTokenStore;
import org.forgerock.openidconnect.OpenIdResourceOwnerConsentVerifier;
import org.forgerock.openidconnect.SubjectTypeValidator;
import org.forgerock.openidconnect.restlet.LoginHintHook;
import org.forgerock.openidconnect.ssoprovider.OpenIdConnectSSOProvider;
import org.forgerock.util.thread.ExecutorServiceFactory;
import org.restlet.Restlet;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.iplanet.services.naming.WebtopNamingQuery;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.debug.Debug;

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
        bind(RedirectUriResolver.class);
        bind(ResourceOwnerConsentVerifier.class).to(OpenIdResourceOwnerConsentVerifier.class);
        bind(ClientRegistrationStore.class).to(OpenAMClientRegistrationStore.class);
        bind(OpenIdConnectClientRegistrationStore.class).to(OpenAMClientRegistrationStore.class);
        bind(TokenStore.class).to(OpenAMTokenStore.class);
        bind(OpenIdConnectTokenStore.class).to(OpenAMTokenStore.class);
        bind(ClientAuthenticationFailureFactory.class).to(OpenAMClientAuthenticationFailureFactory.class);
        bind(AccessTokenVerifier.class).to(RestletHeaderAccessTokenVerifier.class);
        bind(AccessTokenVerifier.class).annotatedWith(named(HEADER)).to(RestletHeaderAccessTokenVerifier.class);
        bind(AccessTokenVerifier.class).annotatedWith(named(FORM_BODY)).to(RestletFormBodyAccessTokenVerifier.class);
        bind(AccessTokenVerifier.class).annotatedWith(named(QUERY_PARAM)).to(RestletQueryParameterAccessTokenVerifier.class);
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
        authorizeRequestValidators.addBinding().to(ClaimsParameterValidator.class);
        authorizeRequestValidators.addBinding().to(SubjectTypeValidator.class);
        authorizeRequestValidators.addBinding().to(CodeVerifierValidator.class);
        authorizeRequestValidators.addBinding().to(DuplicateRequestParameterValidator.class);
        authorizeRequestValidators.addBinding().to(ConfirmationKeyValidator.class);

        final Multibinder<AuthorizationCodeRequestValidator> authorizationCodeRequestValidators =
                Multibinder.newSetBinder(binder(), AuthorizationCodeRequestValidator.class);
        authorizationCodeRequestValidators.addBinding().to(AuthorizationCodeRequestValidatorImpl.class);
        authorizationCodeRequestValidators.addBinding().to(ConfirmationKeyValidator.class);

        final Multibinder<ClientCredentialsRequestValidator> clientCredentialsRequestValidators =
                Multibinder.newSetBinder(binder(), ClientCredentialsRequestValidator.class);
        clientCredentialsRequestValidators.addBinding().to(ClientCredentialsRequestValidatorImpl.class);
        clientCredentialsRequestValidators.addBinding().to(ConfirmationKeyValidator.class);

        final Multibinder<PasswordCredentialsRequestValidator> passwordCredentialsRequestValidators =
                Multibinder.newSetBinder(binder(), PasswordCredentialsRequestValidator.class);
        passwordCredentialsRequestValidators.addBinding().to(PasswordCredentialsRequestValidatorImpl.class);
        passwordCredentialsRequestValidators.addBinding().to(ConfirmationKeyValidator.class);

        final MapBinder<String, GrantTypeHandler> grantTypeHandlers =
                MapBinder.newMapBinder(binder(), String.class, GrantTypeHandler.class);
        grantTypeHandlers.addBinding(CLIENT_CREDENTIALS).to(ClientCredentialsGrantTypeHandler.class);
        grantTypeHandlers.addBinding(PASSWORD).to(PasswordCredentialsGrantTypeHandler.class);
        grantTypeHandlers.addBinding(AUTHORIZATION_CODE).to(AuthorizationCodeGrantTypeHandler.class);
        grantTypeHandlers.addBinding(DEVICE_CODE).to(DeviceCodeGrantTypeHandler.class);
        grantTypeHandlers.addBinding(JWT_BEARER).to(JwtBearerGrantTypeHandler.class);

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

        Multibinder.newSetBinder(binder(), ResourceSetRegistrationHook.class);

        bind(OpenIDConnectURLValidator.class).toInstance(OpenIDConnectURLValidator.getInstance());
        install(new LabelsGuiceModule());

        bind(new TypeLiteral<StatelessCheck<Boolean>>() {}).to(DefaultStatelessCheck.class);
        bind(new TypeLiteral<TokenAdapter<StatelessTokenMetadata>>(){}).to(StatelessTokenCtsAdapter.class);

        bind(OpenIdConnectSSOProvider.class);
    }

    public static class DefaultStatelessCheck implements StatelessCheck<Boolean> {
        private final OAuth2ProviderSettingsFactory providerSettings;

        @Inject
        public DefaultStatelessCheck(OAuth2ProviderSettingsFactory oAuth2ProviderSettings) {
            this.providerSettings = oAuth2ProviderSettings;
        }

        @Override
        public Boolean byToken(String tokenId) {
            try {
                new JwtReconstruction().reconstructJwt(tokenId, SignedJwt.class);
                return true;
            } catch (InvalidJwtException e) {
                return false;
            }
        }

        @Override
        public Boolean byRealm(String realm) {
            try {
                return providerSettings.getRealmProviderSettings(realm).isStatelessTokensEnabled();
            } catch (ServerException | NotFoundException e) {
                return false;
            }
        }

        @Override
        public Boolean byRequest(OAuth2Request request) {
            try {
                OAuth2ProviderSettings oAuth2ProviderSettings = providerSettings.get(request);
                return oAuth2ProviderSettings.isStatelessTokensEnabled();
            } catch (ServerException | NotFoundException e) {
                return false;
            }
        }
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
            OAuth2ProviderSettingsFactory providerSettingsFactory, OAuth2UrisFactory oauth2UrisFactory,
            OpenIdConnectClientRegistrationStore clientRegistrationStore, RealmNormaliser realmNormaliser,
            SSOTokenManager ssoTokenManager, CookieExtractor cookieExtractor, OAuth2AuditLogger auditLogger,
            @Named(OAuth2Constants.DEBUG_LOG_NAME) Debug debug, SecureRandom secureRandom,
            ClientAuthenticationFailureFactory failureFactory, JwtBuilderFactory jwtBuilder,
            Blacklist<Blacklistable> tokenBlacklist, CTSPersistentStore cts,
            TokenAdapter<StatelessTokenMetadata> tokenAdapter, RecoveryCodeGenerator recoveryCodeGenerator,
            OAuth2Utils utils) {
        StatefulTokenStore realmAgnosticStatefulTokenStore = new RealmAgnosticStatefulTokenStore(oauthTokenStore,
                providerSettingsFactory, oauth2UrisFactory, clientRegistrationStore, realmNormaliser, ssoTokenManager,
                cookieExtractor, auditLogger, debug, secureRandom, failureFactory, recoveryCodeGenerator, utils);
        StatelessTokenStore realmAgnosticStatelessTokenStore = new RealmAgnosticStatelessTokenStore(
                realmAgnosticStatefulTokenStore, jwtBuilder, providerSettingsFactory, debug, clientRegistrationStore,
                realmNormaliser, oauth2UrisFactory, tokenBlacklist, cts, tokenAdapter, utils);
        return new OpenAMTokenStore(realmAgnosticStatefulTokenStore,
                realmAgnosticStatelessTokenStore, new DefaultStatelessCheck(providerSettingsFactory));
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
    public Restlet createResourceSetRegistrationEndpoint(TokenStore store, OAuth2RequestFactory reqFactory,
            JacksonRepresentationFactory jacksonRepresentationFactory) {
        return new ResourceSetRegistrationExceptionFilter(
                new AccessTokenProtectionFilter(null, store, reqFactory, wrap(ResourceSetRegistrationEndpoint.class)),
                jacksonRepresentationFactory);
    }

    public static class RealmAgnosticStatefulTokenStore extends StatefulTokenStore {

        public RealmAgnosticStatefulTokenStore(OAuthTokenStore tokenStore,
                OAuth2ProviderSettingsFactory providerSettingsFactory, OAuth2UrisFactory oauth2UrisFactory,
                OpenIdConnectClientRegistrationStore clientRegistrationStore, RealmNormaliser realmNormaliser,
                SSOTokenManager ssoTokenManager, CookieExtractor cookieExtractor, OAuth2AuditLogger auditLogger,
                Debug debug, SecureRandom secureRandom, ClientAuthenticationFailureFactory failureFactory,
                RecoveryCodeGenerator recoveryCodeGenerator, OAuth2Utils utils) {
            super(tokenStore, providerSettingsFactory, oauth2UrisFactory, clientRegistrationStore,
                    realmNormaliser, ssoTokenManager, cookieExtractor, auditLogger, debug, secureRandom,
                    failureFactory, recoveryCodeGenerator, utils);
        }

        @Override
        protected void validateTokenRealm(String tokenRealm, OAuth2Request request) throws InvalidGrantException {
            //No need to validate the realm for the provided token.
        }
    }

    public static class RealmAgnosticStatelessTokenStore extends StatelessTokenStore {

        public RealmAgnosticStatelessTokenStore(StatefulTokenStore statefulTokenStore, JwtBuilderFactory jwtBuilder,
                OAuth2ProviderSettingsFactory providerSettingsFactory, Debug logger,
                OpenIdConnectClientRegistrationStore clientRegistrationStore, RealmNormaliser realmNormaliser,
                OAuth2UrisFactory oAuth2UrisFactory, Blacklist<Blacklistable> tokenBlacklist,
                CTSPersistentStore cts, TokenAdapter<StatelessTokenMetadata> tokenAdapter, OAuth2Utils utils) {
            super(statefulTokenStore, jwtBuilder, providerSettingsFactory, logger, clientRegistrationStore,
                    realmNormaliser, oAuth2UrisFactory, tokenBlacklist, cts, tokenAdapter, utils);
        }

        @Override
        protected void validateTokenRealm(String tokenRealm, OAuth2Request request) throws InvalidGrantException {
            //No need to validate the realm for the provided token.
        }
    }

    @Provides
    public CTSBlacklist<Blacklistable> getCtsStatelessTokenBlacklist(CTSPersistentStore cts, AMExecutorServiceFactory esf,
            ThreadMonitor threadMonitor, WebtopNamingQuery webtopNamingQuery,OAuth2GlobalSettings globalSettings) {
        ScheduledExecutorService scheduledExecutorService = esf.createScheduledService(1, "OAuthTokenBlacklisting");
        long purgeDelayMs = globalSettings.getBlacklistPurgeDelay(TimeUnit.MILLISECONDS);
        long pollIntervalMs = globalSettings.getBlacklistPollInterval(TimeUnit.MILLISECONDS);
        return new CTSBlacklist<>(cts, TokenType.OAUTH_BLACKLIST, scheduledExecutorService, threadMonitor,
                webtopNamingQuery, purgeDelayMs, pollIntervalMs);
    }

    @Provides @Singleton @Inject
    public static Blacklist<Blacklistable> getStatelessTokenBlacklist(CTSBlacklist<Blacklistable> ctsBlacklist,
            OAuth2GlobalSettings globalSettings) {

        if (!globalSettings.isSessionBlacklistingEnabled()) {
            return new NoOpBlacklist<>();
        }

        long purgeDelayMs = globalSettings.getBlacklistPurgeDelay(TimeUnit.MILLISECONDS);
        int cacheSize = globalSettings.getBlacklistCacheSize();
        long pollIntervalMs = globalSettings.getBlacklistPollInterval(TimeUnit.MILLISECONDS);

        Blacklist<Blacklistable> blacklist = ctsBlacklist;
        if (cacheSize > 0) {
            blacklist = new CachingBlacklist<>(blacklist, cacheSize, purgeDelayMs);
        }

        if (pollIntervalMs > 0) {
            blacklist = new BloomFilterBlacklist<>(blacklist, purgeDelayMs);
        }

        return blacklist;
    }
}
