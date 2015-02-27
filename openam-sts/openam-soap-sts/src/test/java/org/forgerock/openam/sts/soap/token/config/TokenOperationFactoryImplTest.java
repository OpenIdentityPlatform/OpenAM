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

package org.forgerock.openam.sts.soap.token.config;

import static org.testng.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import org.apache.cxf.sts.token.renewer.SAMLTokenRenewer;
import org.apache.ws.security.message.token.UsernameToken;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.DefaultHttpURLConnectionFactory;
import org.forgerock.openam.sts.HttpURLConnectionFactory;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.XmlMarshaller;
import org.forgerock.openam.sts.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAccessTokenProvider;
import org.forgerock.openam.sts.soap.token.provider.SoapSamlTokenProvider;
import org.forgerock.openam.sts.soap.token.provider.XmlTokenAuthnContextMapper;
import org.forgerock.openam.sts.soap.token.provider.XmlTokenAuthnContextMapperImpl;
import org.forgerock.openam.sts.token.AMTokenParser;
import org.forgerock.openam.sts.token.AMTokenParserImpl;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCacheImpl;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.sts.token.UrlConstituentCatenatorImpl;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.model.OpenAMSessionTokenMarshaller;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidator;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidatorImpl;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumer;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumerImpl;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.PrincipalFromSessionImpl;
import org.forgerock.openam.sts.token.validator.wss.AuthenticationHandler;
import org.forgerock.openam.sts.token.validator.wss.AuthenticationHandlerImpl;
import org.forgerock.openam.sts.token.validator.wss.UsernameTokenValidator;
import org.forgerock.openam.sts.token.validator.wss.disp.TokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.wss.disp.UsernameTokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.wss.url.AuthenticationUrlProvider;
import org.forgerock.openam.sts.token.validator.wss.url.AuthenticationUrlProviderImpl;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

public class TokenOperationFactoryImplTest {
    TokenOperationFactory operationFactory;
    static class MyModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(ThreadLocalAMTokenCache.class).to(ThreadLocalAMTokenCacheImpl.class);
            bind(new TypeLiteral<XmlMarshaller<OpenAMSessionToken>>(){}).to(OpenAMSessionTokenMarshaller.class);

            bind(AuthenticationUrlProvider.class)
                    .to(AuthenticationUrlProviderImpl.class);

            bind(new TypeLiteral<TokenAuthenticationRequestDispatcher<UsernameToken>>(){})
                    .to(UsernameTokenAuthenticationRequestDispatcher.class);

            bind(new TypeLiteral<AuthenticationHandler<UsernameToken>>(){})
                    .to(new TypeLiteral<AuthenticationHandlerImpl<UsernameToken>>() {
                    });
            bind(AMTokenParser.class).to(AMTokenParserImpl.class);
            bind(UrlConstituentCatenator.class).to(UrlConstituentCatenatorImpl.class);
            bind(TokenOperationFactory.class).to(TokenOperationFactoryImpl.class);
            bind(PrincipalFromSession.class).to(PrincipalFromSessionImpl.class);
            bind(UrlConstituentCatenator.class).to(UrlConstituentCatenatorImpl.class);
            bind(TokenGenerationServiceConsumer.class).to(TokenGenerationServiceConsumerImpl.class);
            bind(XMLUtilities.class).to(XMLUtilitiesImpl.class);
            bind(XmlTokenAuthnContextMapper.class).to(XmlTokenAuthnContextMapperImpl.class);
            bind(new TypeLiteral<XmlMarshaller<OpenAMSessionToken>>(){}).to(OpenAMSessionTokenMarshaller.class);
            bind(SoapSTSAccessTokenProvider.class).toInstance(mock(SoapSTSAccessTokenProvider.class));
            bind(HttpURLConnectionFactory.class).to(DefaultHttpURLConnectionFactory.class);
            bind(HttpURLConnectionWrapperFactory.class);
        }

        @Provides
        @Named(AMSTSConstants.AM_DEPLOYMENT_URL)
        public String getAMDeploymentUrl() {
            return "http://host.com:8080/openam";
        }

        @Provides
        @Named(AMSTSConstants.REST_LOGOUT_URI_ELEMENT)
        public String getAMRestLogoutUriElement() {
            return "/sessions/?_action=logout";
        }

        @Provides
        @Named(AMSTSConstants.REST_ID_FROM_SESSION_URI_ELEMENT)
        public String getIdFromSessionUriElement() {
            return "/users/?_action=idFromSession";
        }

        @Provides
        @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME)
        public String getAMSessionCookieName() {
            return "iPlanetDirectoryPro";
        }

        @Provides
        @Named (AMSTSConstants.REST_AUTHN_URI_ELEMENT)
        String restAuthnUriElement() {
            return "/authenticate";
        }

        @Provides
        AuthTargetMapping authTargetMapping() {
            return AuthTargetMapping.builder()
                    .addMapping(TokenType.USERNAME, "index_type", "index_value")
                    .build();
        }

        @Provides
        @Inject
        UsernameTokenValidator getWssUsernameTokenValidator(
                AuthenticationHandler<UsernameToken> authenticationHandler,
                Logger logger) {
            return new UsernameTokenValidator(logger, authenticationHandler);
        }

        @Provides
        @Named (AMSTSConstants.REALM)
        String realm() {
            return "realm";
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
        @javax.inject.Singleton
        @Named(AMSTSConstants.REST_TOKEN_GENERATION_SERVICE_URI_ELEMENT)
        String tokenGenerationServiceUriElement() {
            return "/sts-tokengen/issue?_action=issue";
        }

        @Provides
        @Named(AMSTSConstants.STS_INSTANCE_ID)
        String getSTSInstanceId() {
            return "cho_mama";
        }

        @Provides
        @Inject
        AMSessionInvalidator getSessionInvalidator(
                @Named(AMSTSConstants.AM_DEPLOYMENT_URL) String deploymentUrl,
                @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT) String jsonRestRoot,
                @Named (AMSTSConstants.REALM) String realm,
                @Named(AMSTSConstants.REST_LOGOUT_URI_ELEMENT) String logoutUriElement,
                @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME) String sessionCookieName,
                HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory,
                Logger logger) throws URISyntaxException {
            try {
                return new AMSessionInvalidatorImpl(deploymentUrl, jsonRestRoot, realm, logoutUriElement,
                        sessionCookieName, new UrlConstituentCatenatorImpl(), "crest_session_version", httpURLConnectionWrapperFactory, logger);
            } catch (MalformedURLException e) { return null;}
        }

        @Provides
        Set<TokenTransformConfig> getValidateTransformations() {
            Set<TokenTransformConfig> transformConfigs = new HashSet<TokenTransformConfig>();
            transformConfigs.add(new TokenTransformConfig(TokenType.OPENAM, TokenType.SAML2, true));
            return transformConfigs;
        }

        @Provides
        @Singleton
        @Named(AMSTSConstants.CREST_VERSION_SESSION_SERVICE)
        String getSessionServiceVersion() {
            return "protocol=1.0, resource=1.1";
        }

        @Provides
        @Singleton
        @Named(AMSTSConstants.CREST_VERSION_AUTHN_SERVICE)
        String getAuthNServiceVersion() {
            return "protocol=1.0, resource=2.0";
        }

        @Provides
        @Singleton
        @Named(AMSTSConstants.CREST_VERSION_TOKEN_GEN_SERVICE)
        String getTokenGenServiceVersion() {
            return "protocol=1.0, resource=1.0";
        }

        @Provides
        @Singleton
        @Named(AMSTSConstants.CREST_VERSION_USERS_SERVICE)
        String getUsersServiceVersion() {
            return "protocol=1.0, resource=2.0";
        }

        @Provides
        @Singleton
        AMSTSConstants.STSType getSTSType() {
            return AMSTSConstants.STSType.REST;
        }
    }

    @BeforeTest
    public void getTokenOperationFactory() {
        operationFactory = Guice.createInjector(new MyModule()).getInstance(TokenOperationFactory.class);
    }

    @Test
    public void testGetUsernameTokenValidator() throws STSInitializationException {
        assertTrue(operationFactory.getTokenStatusValidatorForType(TokenType.USERNAME) instanceof org.apache.cxf.sts.token.validator.UsernameTokenValidator);
    }

    @Test
    public void testTokenRenewer() throws STSInitializationException {
        assertTrue(operationFactory.getTokenRenewerForType(TokenType.SAML2) instanceof SAMLTokenRenewer);
    }

    @Test(expectedExceptions = STSInitializationException.class)
    public void testNonExistentTokenRenewer() throws STSInitializationException {
        operationFactory.getTokenRenewerForType(TokenType.USERNAME);
    }

    @Test
    public void testProvider() throws STSInitializationException {
        assertTrue(operationFactory.getTokenProviderForType(TokenType.SAML2) instanceof SoapSamlTokenProvider);
    }

    @Test(expectedExceptions = STSInitializationException.class)
    public void testNonExistentProvider() throws STSInitializationException {
        operationFactory.getTokenProviderForType(TokenType.USERNAME);
    }
}
