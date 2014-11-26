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

package org.forgerock.openam.sts.rest.operation;

import com.google.inject.Provider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.validator.TokenValidator;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XmlMarshaller;
import org.forgerock.openam.sts.rest.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.rest.token.provider.AMSessionInvalidator;
import org.forgerock.openam.sts.rest.token.validator.RestCertificateTokenValidator;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.rest.token.provider.AMSAMLTokenProvider;
import org.forgerock.openam.sts.rest.token.provider.AMSessionInvalidatorImpl;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.openam.sts.token.provider.AuthnContextMapper;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumer;
import org.forgerock.openam.sts.token.validator.AMTokenValidator;
import org.forgerock.openam.sts.token.validator.OpenIdConnectIdTokenValidator;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.wss.AuthenticationHandler;
import org.forgerock.openam.sts.token.validator.wss.UsernameTokenValidator;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.MalformedURLException;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;

/**
 * @see org.forgerock.openam.sts.rest.operation.TokenTransformFactory
 */
public class TokenTransformFactoryImpl implements TokenTransformFactory {
    private static final AMSessionInvalidator NULL_AM_SESSION_INVALIDATOR = null;

    private final String amDeploymentUrl;
    private final String jsonRestRoot;
    private final String restLogoutUriElement;
    private final String amSessionCookieName;
    private final String realm;
    private final String stsInstanceId;
    private final Provider<UsernameTokenValidator> wssUsernameTokenValidatorProvider;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final PrincipalFromSession principalFromSession;
    private final AuthenticationHandler<OpenIdConnectIdToken> openIdConnectIdTokenAuthenticationHandler;
    private final AuthenticationHandler<X509Certificate[]> x509TokenAuthenticationHandler;
    private final UrlConstituentCatenator urlConstituentCatenator;
    private final XmlMarshaller<OpenIdConnectIdToken> idTokenXmlMarshaller;
    private final XMLUtilities xmlUtilities;
    private final TokenGenerationServiceConsumer tokenGenerationServiceConsumer;
    private final AuthnContextMapper authnContextMapper;
    private final HttpURLConnectionWrapperFactory connectionWrapperFactory;
    private final String crestVersionSessionService;
    private final Logger logger;

    @Inject
    TokenTransformFactoryImpl(
            @Named(AMSTSConstants.AM_DEPLOYMENT_URL) String amDeploymentUrl,
            @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT) String jsonRestRoot,
            @Named(AMSTSConstants.REST_LOGOUT_URI_ELEMENT) String restLogoutUriElement,
            @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME) String amSessionCookieName,
            @Named (AMSTSConstants.REALM) String realm,
            @Named(AMSTSConstants.STS_INSTANCE_ID) String stsInstanceId,
            Provider<UsernameTokenValidator> wssUsernameTokenValidatorProvider,
            ThreadLocalAMTokenCache threadLocalAMTokenCache,
            PrincipalFromSession principalFromSession,
            AuthenticationHandler<OpenIdConnectIdToken> openIdConnectIdTokenAuthenticationHandler,
            AuthenticationHandler<X509Certificate[]> x509TokenAuthenticationHandler,
            UrlConstituentCatenator urlConstituentCatenator,
            XmlMarshaller<OpenIdConnectIdToken> idTokenXmlMarshaller,
            XMLUtilities xmlUtilities,
            TokenGenerationServiceConsumer tokenGenerationServiceConsumer,
            AuthnContextMapper authnContextMapper,
            HttpURLConnectionWrapperFactory connectionWrapperFactory,
            @Named(AMSTSConstants.CREST_VERSION_SESSION_SERVICE) String crestVersionSessionService,
            Logger logger) {

        this.amDeploymentUrl = amDeploymentUrl;
        this.jsonRestRoot = jsonRestRoot;
        this.restLogoutUriElement = restLogoutUriElement;
        this.amSessionCookieName = amSessionCookieName;
        this.realm = realm;
        this.stsInstanceId = stsInstanceId;
        this.wssUsernameTokenValidatorProvider = wssUsernameTokenValidatorProvider;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.principalFromSession = principalFromSession;
        this.openIdConnectIdTokenAuthenticationHandler = openIdConnectIdTokenAuthenticationHandler;
        this.x509TokenAuthenticationHandler = x509TokenAuthenticationHandler;
        this.urlConstituentCatenator = urlConstituentCatenator;
        this.idTokenXmlMarshaller = idTokenXmlMarshaller;
        this.xmlUtilities = xmlUtilities;
        this.tokenGenerationServiceConsumer = tokenGenerationServiceConsumer;
        this.authnContextMapper = authnContextMapper;
        this.connectionWrapperFactory = connectionWrapperFactory;
        this.crestVersionSessionService = crestVersionSessionService;
        this.logger = logger;
    }

    @Override
    public TokenTransform buildTokenTransform(TokenTransformConfig tokenTransformConfig) throws STSInitializationException {
        TokenType inputTokenType = tokenTransformConfig.getInputTokenType();
        TokenType outputTokenType = tokenTransformConfig.getOutputTokenType();
        TokenValidator tokenValidator;
        if (TokenType.USERNAME.equals(inputTokenType)) {
            tokenValidator = buildUsernameTokenValidator();
        } else if (TokenType.OPENAM.equals(inputTokenType)) {
            tokenValidator = buildOpenAMTokenValidator();
        } else if (TokenType.OPENIDCONNECT.equals(inputTokenType)) {
            tokenValidator = buildOpenIdConnectValidator();
        } else if (TokenType.X509.equals(inputTokenType)) {
            tokenValidator = buildX509TokenValidator();
        }
        else {
            String message = "Unexpected input token type of: " + inputTokenType;
            logger.error(message);
            throw new STSInitializationException(ResourceException.INTERNAL_ERROR, message);
        }

        TokenProvider tokenProvider;
        if (TokenType.SAML2.equals(outputTokenType)) {
            tokenProvider = buildOpenSAMLTokenProvider(tokenTransformConfig.isInvalidateInterimOpenAMSession());
        } else {
            String message = "Unexpected output token type of: " + outputTokenType;
            logger.error(message);
            throw new STSInitializationException(ResourceException.INTERNAL_ERROR, message);
        }
        return new TokenTransformImpl(tokenValidator, tokenProvider, inputTokenType, outputTokenType, logger);
    }

    private TokenValidator buildUsernameTokenValidator() {
        org.apache.cxf.sts.token.validator.UsernameTokenValidator validator =
                new org.apache.cxf.sts.token.validator.UsernameTokenValidator();
        validator.setValidator(wssUsernameTokenValidatorProvider.get());
        return validator;
    }

    private TokenValidator buildOpenIdConnectValidator() {
        return new OpenIdConnectIdTokenValidator(openIdConnectIdTokenAuthenticationHandler, idTokenXmlMarshaller,
                threadLocalAMTokenCache, principalFromSession, logger);
    }

    private TokenValidator buildX509TokenValidator() {
        return new RestCertificateTokenValidator(x509TokenAuthenticationHandler, threadLocalAMTokenCache, principalFromSession);
    }

    private TokenValidator buildOpenAMTokenValidator() {
        return new AMTokenValidator(threadLocalAMTokenCache, principalFromSession, logger);
    }

    private TokenProvider buildOpenSAMLTokenProvider(boolean invalidateInterimAMSession) throws STSInitializationException {
        if (invalidateInterimAMSession) {
            try {
                final AMSessionInvalidator sessionInvalidator =
                        new AMSessionInvalidatorImpl(amDeploymentUrl, jsonRestRoot, realm, restLogoutUriElement,
                                amSessionCookieName, urlConstituentCatenator, crestVersionSessionService, connectionWrapperFactory, logger);
                return new AMSAMLTokenProvider(tokenGenerationServiceConsumer, sessionInvalidator,
                        threadLocalAMTokenCache, stsInstanceId, realm, xmlUtilities, authnContextMapper, logger);
            } catch (MalformedURLException e) {
                throw new STSInitializationException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
            }
        } else {
            return new AMSAMLTokenProvider(tokenGenerationServiceConsumer, NULL_AM_SESSION_INVALIDATOR,
                    threadLocalAMTokenCache, stsInstanceId, realm, xmlUtilities, authnContextMapper, logger);
        }
    }
}
