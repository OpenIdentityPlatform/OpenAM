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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.soap.token.config;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.renewer.SAMLTokenRenewer;
import org.apache.ws.security.message.token.UsernameToken;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.XmlMarshaller;
import org.forgerock.openam.sts.token.AMTokenParser;
import org.forgerock.openam.sts.token.AMTokenParserImpl;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCacheImpl;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.sts.token.UrlConstituentCatenatorImpl;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.model.OpenAMSessionTokenMarshaller;
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
import org.slf4j.Logger;

import static org.testng.Assert.assertTrue;

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
        }

        @Provides
        @Named(AMSTSConstants.AM_DEPLOYMENT_URL)
        public String getAMDeploymentUrl() {
            return "am_deployment_url";
        }

        @Provides
        @Named(AMSTSConstants.REST_LOGOUT_URI_ELEMENT)
        public String getAMRestLogoutUriElement() {
            return "am_rest_logout";
        }

        @Provides
        @Named(AMSTSConstants.REST_ID_FROM_SESSION_URI_ELEMENT)
        public String getIdFromSessionUriElement() {
            return "id_from_session";
        }

        @Provides
        @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME)
        public String getAMSessionCookieName() {
            return "cornholio";
        }

        @Provides
        @Named (AMSTSConstants.REST_AUTHN_URI_ELEMENT)
        String restAuthnUriElement() {
            return "rest_authn";
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
        @Named(AMSTSConstants.CREST_VERSION_AUTHN_SERVICE)
        String getCrestAuthNVersion() {
            return "protocol=1.0, resource=1.0";
        }

        @Provides
        @Named(AMSTSConstants.CREST_VERSION_USERS_SERVICE)
        String getCrestUsersServiceVersion() {
            return "protocol=1.0, resource=1.0";
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

    @Test(expectedExceptions = STSInitializationException.class)
    public void testNonExistentTransform() throws STSInitializationException {
        operationFactory.getTokenProviderForTransformOperation(TokenType.OPENAM, TokenType.USERNAME);
    }

    @Test
    public void testProvider() throws STSInitializationException {
        assertTrue(operationFactory.getTokenProviderForType(TokenType.SAML2) instanceof SAMLTokenProvider);
    }

    @Test(expectedExceptions = STSInitializationException.class)
    public void testNonExistentProvider() throws STSInitializationException {
        operationFactory.getTokenProviderForType(TokenType.USERNAME);
    }
}
