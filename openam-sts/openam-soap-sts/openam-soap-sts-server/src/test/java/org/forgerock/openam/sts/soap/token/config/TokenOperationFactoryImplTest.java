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
 * Portions Copyrighted 2025 3A-Systems LLC.
 */

package org.forgerock.openam.sts.soap.token.config;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.DefaultHttpURLConnectionFactory;
import org.forgerock.openam.sts.HttpURLConnectionFactory;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAccessTokenProvider;
import org.forgerock.openam.sts.soap.config.user.SoapDeploymentConfig;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;
import org.forgerock.openam.sts.soap.token.provider.oidc.DefaultSoapOpenIdConnectTokenAuthnContextMapper;
import org.forgerock.openam.sts.soap.token.provider.oidc.DefaultSoapOpenIdConnectTokenAuthnMethodsReferencesMapper;
import org.forgerock.openam.sts.soap.token.provider.oidc.SoapOpenIdConnectTokenAuthnContextMapper;
import org.forgerock.openam.sts.soap.token.provider.oidc.SoapOpenIdConnectTokenAuthnMethodsReferencesMapper;
import org.forgerock.openam.sts.soap.token.provider.saml2.DefaultSaml2XmlTokenAuthnContextMapper;
import org.forgerock.openam.sts.soap.token.provider.saml2.Saml2XmlTokenAuthnContextMapper;
import org.forgerock.openam.sts.soap.token.provider.saml2.SoapSamlTokenProvider;
import org.forgerock.openam.sts.soap.token.validator.SimpleOpenIdConnectTokenValidator;
import org.forgerock.openam.sts.soap.token.validator.SimpleSAML2TokenValidator;
import org.forgerock.openam.sts.token.AMTokenParser;
import org.forgerock.openam.sts.token.AMTokenParserImpl;
import org.forgerock.openam.sts.token.CTSTokenIdGenerator;
import org.forgerock.openam.sts.token.CTSTokenIdGeneratorImpl;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCacheImpl;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.sts.token.UrlConstituentCatenatorImpl;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidator;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidatorImpl;
import org.forgerock.openam.sts.token.provider.TokenServiceConsumer;
import org.forgerock.openam.sts.token.provider.TokenServiceConsumerImpl;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.PrincipalFromSessionImpl;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.forgerock.openam.sts.token.validator.AuthenticationHandler;
import org.forgerock.openam.sts.token.validator.AuthenticationHandlerImpl;
import org.forgerock.openam.sts.soap.token.validator.wss.OpenAMWSSUsernameTokenValidator;
import org.forgerock.openam.sts.token.validator.disp.TokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.soap.token.validator.disp.SoapUsernameTokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.url.AuthenticationUrlProvider;
import org.forgerock.openam.sts.token.validator.url.AuthenticationUrlProviderImpl;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class TokenOperationFactoryImplTest {
    TokenOperationFactory operationFactory;
    static class MyModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(ThreadLocalAMTokenCache.class).to(ThreadLocalAMTokenCacheImpl.class);
            bind(AuthenticationUrlProvider.class)
                    .to(AuthenticationUrlProviderImpl.class);

            bind(new TypeLiteral<TokenAuthenticationRequestDispatcher<UsernameToken>>(){})
                    .to(SoapUsernameTokenAuthenticationRequestDispatcher.class);

            bind(new TypeLiteral<AuthenticationHandler<UsernameToken>>(){})
                    .to(new TypeLiteral<AuthenticationHandlerImpl<UsernameToken>>() {
                    });
            bind(AMTokenParser.class).to(AMTokenParserImpl.class);
            bind(UrlConstituentCatenator.class).to(UrlConstituentCatenatorImpl.class);
            bind(TokenOperationFactory.class).to(TokenOperationFactoryImpl.class);
            bind(PrincipalFromSession.class).to(PrincipalFromSessionImpl.class);
            bind(UrlConstituentCatenator.class).to(UrlConstituentCatenatorImpl.class);
            bind(TokenServiceConsumer.class).to(TokenServiceConsumerImpl.class);
            bind(XMLUtilities.class).to(XMLUtilitiesImpl.class);
            bind(Saml2XmlTokenAuthnContextMapper.class).to(DefaultSaml2XmlTokenAuthnContextMapper.class);
            bind(SoapSTSAccessTokenProvider.class).toInstance(mock(SoapSTSAccessTokenProvider.class));
            bind(HttpURLConnectionFactory.class).to(DefaultHttpURLConnectionFactory.class);
            bind(HttpURLConnectionWrapperFactory.class);
            bind(SoapOpenIdConnectTokenAuthnContextMapper.class).to(DefaultSoapOpenIdConnectTokenAuthnContextMapper.class);
            bind(SoapOpenIdConnectTokenAuthnMethodsReferencesMapper.class).to(DefaultSoapOpenIdConnectTokenAuthnMethodsReferencesMapper.class);
            bind(CTSTokenIdGenerator.class).to(CTSTokenIdGeneratorImpl.class);
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
        OpenAMWSSUsernameTokenValidator getWssUsernameTokenValidator(
                AuthenticationHandler<UsernameToken> authenticationHandler,
                ThreadLocalAMTokenCache threadLocalAMTokenCache,
                Logger logger) {
            return new OpenAMWSSUsernameTokenValidator(authenticationHandler, threadLocalAMTokenCache,
                    ValidationInvocationContext.SOAP_SECURITY_POLICY, true, logger);
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
            return "/sts-tokengen/issue?_action=create";
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

        @Provides
        @Singleton
        SoapSTSInstanceConfig getStsInstanceConfig() {
            SoapSTSInstanceConfig mockInstanceConfig = mock(SoapSTSInstanceConfig.class);
            SoapDeploymentConfig mockDeploymentConfig = mock(SoapDeploymentConfig.class);
            when(mockInstanceConfig.getDeploymentConfig()).thenReturn(mockDeploymentConfig);
            when(mockDeploymentConfig.getWsdlLocation()).thenReturn("am_ut.wsdl");
            return mockInstanceConfig;
        }
    }

    @BeforeTest
    public void getTokenOperationFactory() {
        operationFactory = Guice.createInjector(new MyModule()).getInstance(TokenOperationFactory.class);
    }

    @Test
    public void testGetUsernameTokenValidator() throws STSInitializationException {
        assertTrue(operationFactory.getTokenValidator(TokenType.USERNAME, ValidationInvocationContext.SOAP_SECURITY_POLICY, true)
                instanceof org.apache.cxf.sts.token.validator.UsernameTokenValidator);
    }

    @Test
    public void testProvider() throws STSInitializationException {
        assertTrue(operationFactory.getTokenProvider(TokenType.SAML2) instanceof SoapSamlTokenProvider);
    }

    @Test
    public void testSAML2SimpleValidator() throws STSInitializationException {
        assertTrue(operationFactory.getSimpleTokenValidator(TokenType.SAML2) instanceof SimpleSAML2TokenValidator);
    }

    @Test
    public void testOIDCSimpleValidator() throws STSInitializationException {
        assertTrue(operationFactory.getSimpleTokenValidator(TokenType.OPENIDCONNECT) instanceof SimpleOpenIdConnectTokenValidator);
    }

    @Test(expectedExceptions = STSInitializationException.class)
    public void testNonExistentProvider() throws STSInitializationException {
        operationFactory.getTokenProvider(TokenType.USERNAME);
    }
}
