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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.operation;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.DefaultHttpURLConnectionFactory;
import org.forgerock.openam.sts.HttpURLConnectionFactory;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.config.user.CustomTokenOperation;
import org.forgerock.openam.sts.rest.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.rest.operation.translate.TokenTransform;
import org.forgerock.openam.sts.rest.operation.translate.TokenTransformFactory;
import org.forgerock.openam.sts.rest.operation.translate.TokenTransformFactoryImpl;
import org.forgerock.openam.sts.rest.operation.validate.IssuedTokenValidatorFactory;
import org.forgerock.openam.sts.rest.operation.validate.IssuedTokenValidatorFactoryImpl;
import org.forgerock.openam.sts.rest.token.provider.oidc.DefaultOpenIdConnectTokenAuthMethodReferencesMapper;
import org.forgerock.openam.sts.rest.token.provider.oidc.DefaultOpenIdConnectTokenAuthnContextMapper;
import org.forgerock.openam.sts.rest.token.provider.oidc.OpenIdConnectTokenAuthMethodReferencesMapper;
import org.forgerock.openam.sts.rest.token.provider.oidc.OpenIdConnectTokenAuthnContextMapper;
import org.forgerock.openam.sts.rest.token.provider.saml.Saml2JsonTokenAuthnContextMapperImpl;
import org.forgerock.openam.sts.rest.token.validator.disp.RestUsernameTokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.AMTokenParser;
import org.forgerock.openam.sts.token.AMTokenParserImpl;
import org.forgerock.openam.sts.token.CTSTokenIdGenerator;
import org.forgerock.openam.sts.token.CTSTokenIdGeneratorImpl;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCacheImpl;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.sts.token.UrlConstituentCatenatorImpl;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.openam.sts.rest.token.provider.saml.Saml2JsonTokenAuthnContextMapper;
import org.forgerock.openam.sts.token.model.RestUsernameToken;
import org.forgerock.openam.sts.token.provider.TokenServiceConsumer;
import org.forgerock.openam.sts.token.provider.TokenServiceConsumerImpl;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.PrincipalFromSessionImpl;
import org.forgerock.openam.sts.token.validator.AuthenticationHandler;
import org.forgerock.openam.sts.token.validator.AuthenticationHandlerImpl;
import org.forgerock.openam.sts.token.validator.disp.CertificateAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.rest.token.validator.disp.OpenIdConnectAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.disp.TokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.url.AuthenticationUrlProvider;
import org.forgerock.openam.sts.token.validator.url.AuthenticationUrlProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Named;
import javax.inject.Singleton;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Set;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class TokenTransformFactoryImplTest {
    static final boolean INVALIDATE_INTERIM_AM_SESSIONS = true;
    TokenTransformFactory transformFactory;
    static class MyModule extends AbstractModule {
        @Override
        protected void configure() {
            bindConstant().annotatedWith(Names.named(AMSTSConstants.AM_DEPLOYMENT_URL)).to("http://host.com/");
            bindConstant().annotatedWith(Names.named(AMSTSConstants.REST_LOGOUT_URI_ELEMENT)).to("json/session");
            bindConstant().annotatedWith(Names.named(AMSTSConstants.REST_ID_FROM_SESSION_URI_ELEMENT)).to("faux_id_from_session_url");
            bindConstant().annotatedWith(Names.named(AMSTSConstants.AM_SESSION_COOKIE_NAME)).to("faux_cookie_name");
            bind(ThreadLocalAMTokenCache.class).to(ThreadLocalAMTokenCacheImpl.class);
            bind(UrlConstituentCatenator.class).to(UrlConstituentCatenatorImpl.class);
            bind(IssuedTokenValidatorFactory.class).to(IssuedTokenValidatorFactoryImpl.class);
            bind(TokenTransformFactory.class).to(TokenTransformFactoryImpl.class);
            bind(PrincipalFromSession.class).to(PrincipalFromSessionImpl.class);
            bind(new TypeLiteral<TokenAuthenticationRequestDispatcher<OpenIdConnectIdToken>>(){})
                    .to(OpenIdConnectAuthenticationRequestDispatcher.class);
            bind(new TypeLiteral<AuthenticationHandler<OpenIdConnectIdToken>>(){})
                    .to(new TypeLiteral<AuthenticationHandlerImpl<OpenIdConnectIdToken>>() {
                    });
            bind(new TypeLiteral<TokenAuthenticationRequestDispatcher<X509Certificate[]>>(){})
                    .to(CertificateAuthenticationRequestDispatcher.class);
            bind(new TypeLiteral<AuthenticationHandler<X509Certificate[]>>(){})
                    .to(new TypeLiteral<AuthenticationHandlerImpl<X509Certificate[]>>() {
                    });

            bind(new TypeLiteral<TokenAuthenticationRequestDispatcher<RestUsernameToken>>(){})
                    .to(RestUsernameTokenAuthenticationRequestDispatcher.class);
            bind(new TypeLiteral<AuthenticationHandler<RestUsernameToken>>(){})
                    .to(new TypeLiteral<AuthenticationHandlerImpl<RestUsernameToken>>() {
                    });
            bind(AuthenticationUrlProvider.class)
                    .to(AuthenticationUrlProviderImpl.class);
            bind(AMTokenParser.class).to(AMTokenParserImpl.class);
            bind(TokenServiceConsumer.class).to(TokenServiceConsumerImpl.class);
            bind(XMLUtilities.class).to(XMLUtilitiesImpl.class);
            bind(Saml2JsonTokenAuthnContextMapper.class).to(Saml2JsonTokenAuthnContextMapperImpl.class);
            bind(HttpURLConnectionFactory.class).to(DefaultHttpURLConnectionFactory.class);
            bind(OpenIdConnectTokenAuthnContextMapper.class).to(DefaultOpenIdConnectTokenAuthnContextMapper.class);
            bind(OpenIdConnectTokenAuthMethodReferencesMapper.class).to(DefaultOpenIdConnectTokenAuthMethodReferencesMapper.class);
            bind(CTSTokenIdGenerator.class).to(CTSTokenIdGeneratorImpl.class).in(Scopes.SINGLETON);
        }

        @Provides
        @Named(AMSTSConstants.REALM)
        String realm() {
            return "bobo";
        }

        @Provides
        @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT)
        String getJsonRoot() {
            return "json";
        }

        @Provides
        Logger getSlf4jLogger() {
            return LoggerFactory.getLogger(AMSTSConstants.REST_STS_DEBUG_ID);
        }

        @Provides
        @Named (AMSTSConstants.REST_AUTHN_URI_ELEMENT)
        String restAuthnUriElement() {
            return "cho_mama";
        }

        @Provides
        AuthTargetMapping authTargetMapping() {
            return AuthTargetMapping.builder().addMapping(TokenType.USERNAME, "index_type", "index_value").build();
        }

        @Provides
        @Named(AMSTSConstants.STS_INSTANCE_ID)
        String getInstanceId() {
            return "cornholio";
        }

        @Provides
        @Named(AMSTSConstants.REST_TOKEN_GENERATION_SERVICE_URI_ELEMENT)
        String getTokenGenServiceUriElement() {
            return "/token-gen/issue?_action=create";
        }

        @Provides
        @Named(AMSTSConstants.OFFLOADED_TWO_WAY_TLS_HEADER_KEY)
        String getOffloadedTwoWayTLSHeaderKey() {
            return "client_cert";
        }

        @Provides
        @Named(AMSTSConstants.CREST_VERSION_AUTHN_SERVICE)
        String getCrestVersionAuthNService() {
            return "protocol=1.0, resource=1.0";
        }

        @Provides
        @Named(AMSTSConstants.CREST_VERSION_SESSION_SERVICE)
        String getCrestVersionSessionService() {
            return "protocol=1.0, resource=1.0";
        }

        @Provides
        @Named(AMSTSConstants.CREST_VERSION_USERS_SERVICE)
        String getCrestVersionUsersService() {
            return "protocol=1.0, resource=1.0";
        }

        @Provides
        @Named(AMSTSConstants.CREST_VERSION_TOKEN_GEN_SERVICE)
        String getCrestVersionTokenGenService() {
            return "protocol=1.0, resource=1.0";
        }

        @Provides
        @Singleton
        AMSTSConstants.STSType getSTSType() {
            return AMSTSConstants.STSType.REST;
        }

        @Provides
        @Named(AMSTSConstants.REST_CUSTOM_TOKEN_VALIDATORS)
        Set<CustomTokenOperation> getCustomTokenValidators() {
            return Collections.emptySet();
        }

        @Provides
        @Named(AMSTSConstants.REST_CUSTOM_TOKEN_PROVIDERS)
        Set<CustomTokenOperation> getCustomTokenProviders() {
            return Collections.emptySet();
        }

        @Provides
        @Named(AMSTSConstants.REST_CUSTOM_TOKEN_TRANSLATIONS)
        Set<TokenTransformConfig> getCustomTokenTransforms() {
            return Collections.emptySet();
        }
    }

    @BeforeTest
    public void initialize() {
         transformFactory = Guice.createInjector(new MyModule()).getInstance(TokenTransformFactory.class);
    }

    @Test
    public void testTransformCreation() throws STSInitializationException {
        TokenTransformConfig ttc = new TokenTransformConfig(TokenType.USERNAME, TokenType.SAML2, INVALIDATE_INTERIM_AM_SESSIONS);
        TokenTransform transform = transformFactory.buildTokenTransform(ttc);
        assertTrue(transform.isTransformSupported(TokenType.USERNAME, TokenType.SAML2));
        assertFalse(transform.isTransformSupported(TokenType.OPENAM, TokenType.SAML2));

        ttc = new TokenTransformConfig(TokenType.OPENAM, TokenType.SAML2, INVALIDATE_INTERIM_AM_SESSIONS);
        transform = transformFactory.buildTokenTransform(ttc);
        assertTrue(transform.isTransformSupported(TokenType.OPENAM, TokenType.SAML2));
        assertFalse(transform.isTransformSupported(TokenType.USERNAME, TokenType.SAML2));

        ttc = new TokenTransformConfig(TokenType.OPENIDCONNECT, TokenType.SAML2, INVALIDATE_INTERIM_AM_SESSIONS);
        transform = transformFactory.buildTokenTransform(ttc);
        assertTrue(transform.isTransformSupported(TokenType.OPENIDCONNECT, TokenType.SAML2));
        assertFalse(transform.isTransformSupported(TokenType.USERNAME, TokenType.SAML2));
    }
}
