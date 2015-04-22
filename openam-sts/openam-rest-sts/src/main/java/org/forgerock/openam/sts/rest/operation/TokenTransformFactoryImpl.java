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

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.rest.token.provider.JsonTokenAuthnContextMapper;
import org.forgerock.openam.sts.rest.token.provider.Saml2TokenCreationState;
import org.forgerock.openam.sts.rest.token.validator.RestAMTokenValidator;
import org.forgerock.openam.sts.rest.token.validator.RestTokenValidator;
import org.forgerock.openam.sts.rest.token.validator.RestUsernameTokenValidator;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.model.RestUsernameToken;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidator;
import org.forgerock.openam.sts.rest.token.validator.RestCertificateTokenValidator;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.rest.token.provider.RestSamlTokenProvider;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidatorImpl;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.openam.sts.rest.token.provider.RestTokenProvider;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumer;
import org.forgerock.openam.sts.rest.token.validator.OpenIdConnectIdTokenValidator;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.forgerock.openam.sts.token.validator.AuthenticationHandler;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.MalformedURLException;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;

/**
 * @see org.forgerock.openam.sts.rest.operation.TokenTransformFactory
 */
public class TokenTransformFactoryImpl implements TokenTransformFactory {
    private final String amDeploymentUrl;
    private final String jsonRestRoot;
    private final String restLogoutUriElement;
    private final String amSessionCookieName;
    private final String realm;
    private final String stsInstanceId;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final PrincipalFromSession principalFromSession;
    private final AuthenticationHandler<OpenIdConnectIdToken> openIdConnectIdTokenAuthenticationHandler;
    private final AuthenticationHandler<X509Certificate[]> x509TokenAuthenticationHandler;
    private final AuthenticationHandler<RestUsernameToken> usernameTokenAuthenticationHandler;
    private final UrlConstituentCatenator urlConstituentCatenator;
    private final TokenGenerationServiceConsumer tokenGenerationServiceConsumer;
    private final JsonTokenAuthnContextMapper jsonTokenAuthnContextMapper;
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
            ThreadLocalAMTokenCache threadLocalAMTokenCache,
            PrincipalFromSession principalFromSession,
            AuthenticationHandler<OpenIdConnectIdToken> openIdConnectIdTokenAuthenticationHandler,
            AuthenticationHandler<X509Certificate[]> x509TokenAuthenticationHandler,
            AuthenticationHandler<RestUsernameToken> usernameTokenAuthenticationHandler,
            UrlConstituentCatenator urlConstituentCatenator,
            TokenGenerationServiceConsumer tokenGenerationServiceConsumer,
            JsonTokenAuthnContextMapper jsonTokenAuthnContextMapper,
            HttpURLConnectionWrapperFactory connectionWrapperFactory,
            @Named(AMSTSConstants.CREST_VERSION_SESSION_SERVICE) String crestVersionSessionService,
            Logger logger) {

        this.amDeploymentUrl = amDeploymentUrl;
        this.jsonRestRoot = jsonRestRoot;
        this.restLogoutUriElement = restLogoutUriElement;
        this.amSessionCookieName = amSessionCookieName;
        this.realm = realm;
        this.stsInstanceId = stsInstanceId;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.principalFromSession = principalFromSession;
        this.openIdConnectIdTokenAuthenticationHandler = openIdConnectIdTokenAuthenticationHandler;
        this.x509TokenAuthenticationHandler = x509TokenAuthenticationHandler;
        this.usernameTokenAuthenticationHandler = usernameTokenAuthenticationHandler;
        this.urlConstituentCatenator = urlConstituentCatenator;
        this.tokenGenerationServiceConsumer = tokenGenerationServiceConsumer;
        this.jsonTokenAuthnContextMapper = jsonTokenAuthnContextMapper;
        this.connectionWrapperFactory = connectionWrapperFactory;
        this.crestVersionSessionService = crestVersionSessionService;
        this.logger = logger;
    }

    public TokenTransform<?, ? extends TokenTypeId> buildTokenTransform(TokenTransformConfig tokenTransformConfig) throws STSInitializationException {
        TokenType inputTokenType = tokenTransformConfig.getInputTokenType();
        TokenType outputTokenType = tokenTransformConfig.getOutputTokenType();
        RestTokenValidator<?> tokenValidator;
        if (TokenType.USERNAME.equals(inputTokenType)) {
            tokenValidator = buildUsernameTokenValidator(tokenTransformConfig.invalidateInterimOpenAMSession());
        } else if (TokenType.OPENAM.equals(inputTokenType)) {
            tokenValidator = buildOpenAMTokenValidator(tokenTransformConfig.invalidateInterimOpenAMSession());
        } else if (TokenType.OPENIDCONNECT.equals(inputTokenType)) {
            tokenValidator = buildOpenIdConnectValidator(tokenTransformConfig.invalidateInterimOpenAMSession());
        } else if (TokenType.X509.equals(inputTokenType)) {
            tokenValidator = buildX509TokenValidator(tokenTransformConfig.invalidateInterimOpenAMSession());
        }
        else {
            String message = "Unexpected input token type of: " + inputTokenType;
            logger.error(message);
            throw new STSInitializationException(ResourceException.INTERNAL_ERROR, message);
        }

        RestTokenProvider<? extends TokenTypeId> tokenProvider;
        if (TokenType.SAML2.equals(outputTokenType)) {
            tokenProvider = buildOpenSAMLTokenProvider();
        } else {
            String message = "Unexpected output token type of: " + outputTokenType;
            logger.error(message);
            throw new STSInitializationException(ResourceException.INTERNAL_ERROR, message);
        }
        return new TokenTransformImpl(tokenValidator, tokenProvider, inputTokenType, outputTokenType);
    }

    private RestTokenValidator<RestUsernameToken> buildUsernameTokenValidator(boolean invalidateAMSession) {
        return new RestUsernameTokenValidator(usernameTokenAuthenticationHandler, threadLocalAMTokenCache,
                principalFromSession, ValidationInvocationContext.REST_TOKEN_TRANSFORMATION, invalidateAMSession);
    }

    private RestTokenValidator<OpenIdConnectIdToken> buildOpenIdConnectValidator(boolean invalidateAMSession) {
        return new OpenIdConnectIdTokenValidator(openIdConnectIdTokenAuthenticationHandler,
                threadLocalAMTokenCache, principalFromSession, ValidationInvocationContext.REST_TOKEN_TRANSFORMATION,
                invalidateAMSession);
    }

    private RestTokenValidator<X509Certificate[]> buildX509TokenValidator(boolean invalidateAMSession) {
        return new RestCertificateTokenValidator(x509TokenAuthenticationHandler, threadLocalAMTokenCache,
                principalFromSession, ValidationInvocationContext.REST_TOKEN_TRANSFORMATION, invalidateAMSession);
    }

    private RestTokenValidator<OpenAMSessionToken> buildOpenAMTokenValidator(boolean invalidateAMSession) {
        return new RestAMTokenValidator(principalFromSession, threadLocalAMTokenCache,
                ValidationInvocationContext.REST_TOKEN_TRANSFORMATION, invalidateAMSession);
    }

    private RestTokenProvider<Saml2TokenCreationState> buildOpenSAMLTokenProvider() throws STSInitializationException {
        try {
            final AMSessionInvalidator sessionInvalidator =
                    new AMSessionInvalidatorImpl(amDeploymentUrl, jsonRestRoot, realm, restLogoutUriElement,
                            amSessionCookieName, urlConstituentCatenator, crestVersionSessionService, connectionWrapperFactory, logger);
            return new RestSamlTokenProvider(tokenGenerationServiceConsumer, sessionInvalidator,
                    threadLocalAMTokenCache, stsInstanceId, realm, jsonTokenAuthnContextMapper,
                    ValidationInvocationContext.REST_TOKEN_TRANSFORMATION, logger);
        } catch (MalformedURLException e) {
            throw new STSInitializationException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
        }
    }
}
