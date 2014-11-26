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
 * Copyright © 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.operation;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.forgerock.openam.sts.XmlMarshaller;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.rest.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.token.AMTokenParser;
import org.forgerock.openam.sts.token.AMTokenParserImpl;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCacheImpl;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.sts.token.UrlConstituentCatenatorImpl;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdTokenMarshaller;
import org.forgerock.openam.sts.token.provider.AuthnContextMapper;
import org.forgerock.openam.sts.token.provider.AuthnContextMapperImpl;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumer;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumerImpl;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.PrincipalFromSessionImpl;
import org.forgerock.openam.sts.token.validator.wss.AuthenticationHandler;
import org.forgerock.openam.sts.token.validator.wss.AuthenticationHandlerImpl;
import org.forgerock.openam.sts.token.validator.wss.UsernameTokenValidator;
import org.forgerock.openam.sts.token.validator.wss.disp.CertificateAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.wss.disp.OpenIdConnectAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.wss.disp.TokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.wss.url.AuthenticationUrlProvider;
import org.forgerock.openam.sts.token.validator.wss.url.AuthenticationUrlProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Named;

import java.security.cert.X509Certificate;

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

            bind(AuthenticationUrlProvider.class)
                    .to(AuthenticationUrlProviderImpl.class);
            bind(AMTokenParser.class).to(AMTokenParserImpl.class);
            bind(new TypeLiteral<XmlMarshaller<OpenIdConnectIdToken>>(){}).to(OpenIdConnectIdTokenMarshaller.class);
            bind(TokenGenerationServiceConsumer.class).to(TokenGenerationServiceConsumerImpl.class);
            bind(XMLUtilities.class).to(XMLUtilitiesImpl.class);
            bind(AuthnContextMapper.class).to(AuthnContextMapperImpl.class);
        }

        @Provides
        UsernameTokenValidator getUsernameTokenValidator() {
            return null;
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
            return "/token-gen/issue?_action=issue";
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
