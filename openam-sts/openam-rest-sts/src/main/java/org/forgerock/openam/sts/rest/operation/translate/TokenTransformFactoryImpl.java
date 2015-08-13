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

package org.forgerock.openam.sts.rest.operation.translate;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.config.user.CustomTokenOperation;
import org.forgerock.openam.sts.rest.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.rest.token.provider.RestTokenProviderParameters;
import org.forgerock.openam.sts.rest.token.provider.oidc.OpenIdConnectTokenAuthMethodReferencesMapper;
import org.forgerock.openam.sts.rest.token.provider.oidc.OpenIdConnectTokenAuthnContextMapper;
import org.forgerock.openam.sts.rest.token.provider.oidc.RestOpenIdConnectTokenProvider;
import org.forgerock.openam.sts.rest.token.provider.saml.Saml2JsonTokenAuthnContextMapper;
import org.forgerock.openam.sts.rest.token.provider.saml.Saml2TokenCreationState;
import org.forgerock.openam.sts.rest.token.validator.RestAMTokenTransformValidator;
import org.forgerock.openam.sts.rest.token.validator.RestCertificateTokenTransformValidator;
import org.forgerock.openam.sts.rest.token.validator.RestTokenTransformValidator;
import org.forgerock.openam.sts.rest.token.validator.RestTokenTransformValidatorParameters;
import org.forgerock.openam.sts.rest.token.validator.RestTokenTransformValidatorResult;
import org.forgerock.openam.sts.rest.token.validator.RestUsernameTokenTransformValidator;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.model.RestUsernameToken;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidator;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.rest.token.provider.saml.RestSamlTokenProvider;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidatorImpl;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.openam.sts.rest.token.provider.RestTokenProvider;
import org.forgerock.openam.sts.token.provider.TokenServiceConsumer;
import org.forgerock.openam.sts.rest.token.validator.OpenIdConnectIdTokenTransformValidator;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.forgerock.openam.sts.token.validator.AuthenticationHandler;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.MalformedURLException;
import java.security.cert.X509Certificate;
import java.util.Set;

import org.slf4j.Logger;

/**
 * @see TokenTransformFactory
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
    private final TokenServiceConsumer tokenServiceConsumer;
    private final Saml2JsonTokenAuthnContextMapper saml2JsonTokenAuthnContextMapper;
    private final HttpURLConnectionWrapperFactory connectionWrapperFactory;
    private final String crestVersionSessionService;
    private final OpenIdConnectTokenAuthnContextMapper oidcAuthnContextMapper;
    private final OpenIdConnectTokenAuthMethodReferencesMapper oidcAuthModeReferencesMapper;
    private final Set<CustomTokenOperation> customTokenValidators;
    private final Set<CustomTokenOperation> customTokenProviders;

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
            TokenServiceConsumer tokenServiceConsumer,
            Saml2JsonTokenAuthnContextMapper saml2JsonTokenAuthnContextMapper,
            HttpURLConnectionWrapperFactory connectionWrapperFactory,
            @Named(AMSTSConstants.CREST_VERSION_SESSION_SERVICE) String crestVersionSessionService,
            OpenIdConnectTokenAuthnContextMapper oidcAuthnContextMapper,
            OpenIdConnectTokenAuthMethodReferencesMapper oidcAuthModeReferencesMapper,
            @Named(AMSTSConstants.REST_CUSTOM_TOKEN_VALIDATORS) Set<CustomTokenOperation> customTokenValidators,
            @Named(AMSTSConstants.REST_CUSTOM_TOKEN_PROVIDERS) Set<CustomTokenOperation> customTokenProviders,
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
        this.tokenServiceConsumer = tokenServiceConsumer;
        this.saml2JsonTokenAuthnContextMapper = saml2JsonTokenAuthnContextMapper;
        this.connectionWrapperFactory = connectionWrapperFactory;
        this.crestVersionSessionService = crestVersionSessionService;
        this.oidcAuthnContextMapper = oidcAuthnContextMapper;
        this.oidcAuthModeReferencesMapper = oidcAuthModeReferencesMapper;
        this.customTokenValidators = customTokenValidators;
        this.customTokenProviders = customTokenProviders;
        this.logger = logger;
    }

    @SuppressWarnings("unchecked")
    public TokenTransform<?, ? extends TokenTypeId> buildTokenTransform(TokenTransformConfig tokenTransformConfig) throws STSInitializationException {
        TokenTypeId inputTokenType = tokenTransformConfig.getInputTokenType();
        TokenTypeId outputTokenType = tokenTransformConfig.getOutputTokenType();
        RestTokenTransformValidator<?> tokenValidator;
        if (TokenType.USERNAME.getId().equals(inputTokenType.getId())) {
            tokenValidator = buildUsernameTokenValidator(tokenTransformConfig.invalidateInterimOpenAMSession());
        } else if (TokenType.OPENAM.getId().equals(inputTokenType.getId())) {
            tokenValidator = buildOpenAMTokenValidator(tokenTransformConfig.invalidateInterimOpenAMSession());
        } else if (TokenType.OPENIDCONNECT.getId().equals(inputTokenType.getId())) {
            tokenValidator = buildOpenIdConnectValidator(tokenTransformConfig.invalidateInterimOpenAMSession());
        } else if (TokenType.X509.getId().equals(inputTokenType.getId())) {
            tokenValidator = buildX509TokenValidator(tokenTransformConfig.invalidateInterimOpenAMSession());
        } else {
            tokenValidator = buildCustomTokenValidator(inputTokenType, ValidationInvocationContext.REST_TOKEN_TRANSFORMATION,
                    tokenTransformConfig.invalidateInterimOpenAMSession());
        }

        RestTokenProvider<?> tokenProvider;
        if (TokenType.SAML2.getId().equals(outputTokenType.getId())) {
            tokenProvider = buildOpenSAMLTokenProvider();
        } else if (TokenType.OPENIDCONNECT.getId().equals(outputTokenType.getId())) {
            tokenProvider = buildOpenIdConnectTokenProvider();
        } else {
            tokenProvider = buildCustomTokenProvider(outputTokenType);
        }
        return new TokenTransformImpl(tokenValidator, tokenProvider, inputTokenType, outputTokenType);
    }

    private RestTokenTransformValidator<RestUsernameToken> buildUsernameTokenValidator(boolean invalidateAMSession) {
        return new RestUsernameTokenTransformValidator(usernameTokenAuthenticationHandler, threadLocalAMTokenCache,
                principalFromSession, ValidationInvocationContext.REST_TOKEN_TRANSFORMATION, invalidateAMSession);
    }

    private RestTokenTransformValidator<OpenIdConnectIdToken> buildOpenIdConnectValidator(boolean invalidateAMSession) {
        return new OpenIdConnectIdTokenTransformValidator(openIdConnectIdTokenAuthenticationHandler,
                threadLocalAMTokenCache, principalFromSession, ValidationInvocationContext.REST_TOKEN_TRANSFORMATION,
                invalidateAMSession);
    }

    private RestTokenTransformValidator<X509Certificate[]> buildX509TokenValidator(boolean invalidateAMSession) {
        return new RestCertificateTokenTransformValidator(x509TokenAuthenticationHandler, threadLocalAMTokenCache,
                principalFromSession, ValidationInvocationContext.REST_TOKEN_TRANSFORMATION, invalidateAMSession);
    }

    private RestTokenTransformValidator<OpenAMSessionToken> buildOpenAMTokenValidator(boolean invalidateAMSession) {
        return new RestAMTokenTransformValidator(principalFromSession, threadLocalAMTokenCache,
                ValidationInvocationContext.REST_TOKEN_TRANSFORMATION, invalidateAMSession);
    }

    private RestTokenTransformValidator<JsonValue> buildCustomTokenValidator(TokenTypeId inputTokenType,
                                                                    ValidationInvocationContext validationInvocationContext,
                                                                    boolean invalidateInterimAMSession) throws STSInitializationException {
        for (CustomTokenOperation customTokenOperation : customTokenValidators) {
            if(customTokenOperation.getCustomTokenName().equals(inputTokenType.getId())) {
                RestTokenTransformValidator customValidator;
                try {
                    customValidator = Class.forName(customTokenOperation.getCustomOperationClassName()).asSubclass(RestTokenTransformValidator.class).newInstance();
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    throw new STSInitializationException(ResourceException.CONFLICT, "Custom token validator instantiation of class "
                            + customTokenOperation.getCustomOperationClassName() + " failed. Correct class name, " +
                            "or expose in classpath, and republish sts instance. Exception: " + e, e);
                }
                return new CustomTokenTransformValidatorWrapper(customValidator, threadLocalAMTokenCache, validationInvocationContext,
                        invalidateInterimAMSession);
            }
        }
        throw new STSInitializationException(ResourceException.CONFLICT, "No custom token validator found for token type "
                + inputTokenType.getId() + ". Republish rest-sts instance with custom token validator specified for custom token type.");
    }

    private RestTokenProvider<Saml2TokenCreationState> buildOpenSAMLTokenProvider() throws STSInitializationException {
        try {
            final AMSessionInvalidator sessionInvalidator =
                    new AMSessionInvalidatorImpl(amDeploymentUrl, jsonRestRoot, realm, restLogoutUriElement,
                            amSessionCookieName, urlConstituentCatenator, crestVersionSessionService, connectionWrapperFactory, logger);
            return new RestSamlTokenProvider(tokenServiceConsumer, sessionInvalidator,
                    threadLocalAMTokenCache, stsInstanceId, realm, saml2JsonTokenAuthnContextMapper,
                    ValidationInvocationContext.REST_TOKEN_TRANSFORMATION, logger);
        } catch (MalformedURLException e) {
            throw new STSInitializationException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
        }
    }

    private RestOpenIdConnectTokenProvider buildOpenIdConnectTokenProvider() throws STSInitializationException {
        try {
            final AMSessionInvalidator sessionInvalidator =
                    new AMSessionInvalidatorImpl(amDeploymentUrl, jsonRestRoot, realm, restLogoutUriElement,
                            amSessionCookieName, urlConstituentCatenator, crestVersionSessionService, connectionWrapperFactory, logger);
            return new RestOpenIdConnectTokenProvider(tokenServiceConsumer, sessionInvalidator,
                    threadLocalAMTokenCache, stsInstanceId, realm, oidcAuthnContextMapper, oidcAuthModeReferencesMapper,
                    ValidationInvocationContext.REST_TOKEN_TRANSFORMATION, logger);
        } catch (MalformedURLException e) {
            throw new STSInitializationException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
        }
    }

    private RestTokenProvider<JsonValue> buildCustomTokenProvider(TokenTypeId outputTokenType) throws STSInitializationException {
        for (CustomTokenOperation customTokenOperation : customTokenProviders) {
            if (customTokenOperation.getCustomTokenName().equals(outputTokenType.getId())) {
                RestTokenProvider customProvider;
                try {
                    customProvider = Class.forName(customTokenOperation.getCustomOperationClassName()).asSubclass(RestTokenProvider.class).newInstance();
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    throw new STSInitializationException(ResourceException.CONFLICT, "Custom token provider instantiation of class "
                            + customTokenOperation.getCustomOperationClassName() + " failed. Correct class name, " +
                            "or expose in classpath, and republish sts instance. Exception: " + e, e);
                }
                return new CustomTokenProviderWrapper(customProvider);
            }
        }
        throw new STSInitializationException(ResourceException.CONFLICT, "No custom token provider found for token type "
                + outputTokenType.getId() + ". Republish rest-sts instance with custom token provider specified for custom token type.");
    }

    /*
    An instance of this class will wrap any end-user-specified custom token validate operation invoked as part of
    a translate operation. It will serve to cache
    the AM session id resulting from successful validation in the thread-local cache, where it can be referenced as a
    token of the principal who will be asserted by the to-be-created token.
    It also serves to ensure generic type consistency for the RestTokenTransformValidatorParameters<JsonValue> which will always
    be passed to custom token validators.
     */
    private static class CustomTokenTransformValidatorWrapper implements RestTokenTransformValidator<JsonValue> {
        private final RestTokenTransformValidator customDelegate;
        private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
        private final ValidationInvocationContext validationInvocationContext;
        private final boolean invalidateInterimAMSession;

        private CustomTokenTransformValidatorWrapper(RestTokenTransformValidator customDelegate, ThreadLocalAMTokenCache threadLocalAMTokenCache,
                                                     ValidationInvocationContext validationInvocationContext,
                                                     boolean invalidateInterimAMSession) {
            this.customDelegate = customDelegate;
            this.threadLocalAMTokenCache = threadLocalAMTokenCache;
            this.validationInvocationContext = validationInvocationContext;
            this.invalidateInterimAMSession = invalidateInterimAMSession;
        }

        @SuppressWarnings("unchecked")
        @Override
        public RestTokenTransformValidatorResult validateToken(RestTokenTransformValidatorParameters<JsonValue> restTokenTransformValidatorParameters) throws TokenValidationException {
            RestTokenTransformValidatorResult result = customDelegate.validateToken(restTokenTransformValidatorParameters);
            /*
            Only in the case of a token transformation or a token renewal will a new token be issued, and thus the AM session id
            corresponding to the validated principal cached.
             */
            if (ValidationInvocationContext.REST_TOKEN_TRANSFORMATION.equals(validationInvocationContext) ||
                    ValidationInvocationContext.TOKEN_RENEW_OPERATION.equals(validationInvocationContext)) {
                if (result.getAMSessionId() == null) {
                    throw new TokenValidationException(ResourceException.CONFLICT, "The custom rest token validator of class "
                            + customDelegate.getClass().getCanonicalName() + " invoked as part of token transformation, did " +
                            "not set the am session string resulting from successful token validation.");
                } else {
                    threadLocalAMTokenCache.cacheSessionIdForContext(ValidationInvocationContext.REST_TOKEN_TRANSFORMATION,
                            result.getAMSessionId(), invalidateInterimAMSession);
                }
            }
            return result;
        }
    }

    /*
    A class to wrap custom token providers, primarily to provide generic type consistency between the custom token providers
    and the RestTokenProviderParameters<JsonValue> which will always be passed to custom token providers
     */
    @SuppressWarnings("unchecked")
    private static class CustomTokenProviderWrapper implements RestTokenProvider<JsonValue> {
        private final RestTokenProvider customDelegate;

        private CustomTokenProviderWrapper(RestTokenProvider customDelegate) {
            this.customDelegate = customDelegate;
        }

        @Override
        public JsonValue createToken(RestTokenProviderParameters<JsonValue> restTokenProviderParameters) throws TokenCreationException {
            return customDelegate.createToken(restTokenProviderParameters);
        }
    }
}
