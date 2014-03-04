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
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.validator.TokenValidator;

import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.rest.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.rest.token.provider.AMSessionInvalidator;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.rest.token.provider.AMSAMLTokenProvider;
import org.forgerock.openam.sts.rest.token.provider.AMSessionInvalidatorImpl;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.sts.token.provider.AMTokenProvider;
import org.forgerock.openam.sts.token.validator.AMTokenValidator;
import org.forgerock.openam.sts.token.validator.wss.UsernameTokenValidator;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;

/**
 * TODO: Need to think about the fact if a configured STS instance is published, and ultimately the REST Resource which
 * services these invocations will create an injector in the ctor, I don't want a new guice object graph to be created
 * when the framework creates the REST Resource instance with each invocation. Thus I want my REST Resources to be
 * singletons. Which means any stateful access to instance variables in objects handing off of this REST Resource will
 * cause problems. I can enforce this behavior in my code, but it is less certain that this is the case in CXF-STS classes.
 * I need to investigate the spring 'no scope' instantiations of these objects, and how spring creates the object graph
 * for the CXF-STS objects. I believe it is similar - this object graph is created, and then adheres to the traditional
 * servlet programming model in which every class is re-entrant. This certainly seems the case from the code I've examined.
 * But I need to confirm this, and if it is not the case,
 * inject Providers for the CXF-STS classes I leverage, so that a new instance can be obtained with every invocation. Certainly
 * the CXF-STS seems to be put together with a 'static' set of Token{Issue/Validate/Renew/Cancel}Operation instances, each with
 * what appears to be a static set of TokenProvider/TokenValidator instances, but I need to confirm this unequivocally.
 */
public class TokenTransformFactoryImpl implements TokenTransformFactory {
    private static final AMSessionInvalidator NULL_AM_SESSION_INVALIDATOR = null;

    private final String amDeploymentUrl;
    private final String jsonRestRoot;
    private final String restLogoutUriElement;
    private final String idFromSessionUriElement;
    private final String amSessionCookieName;
    private final String realm;
    private final Provider<UsernameTokenValidator> wssUsernameTokenValidatorProvider;
    private final Provider<AMTokenProvider> amTokenProviderProvider;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final UrlConstituentCatenator urlConstituentCatenator;
    private final Logger logger;

    @Inject
    TokenTransformFactoryImpl(
            @Named(AMSTSConstants.AM_DEPLOYMENT_URL) String amDeploymentUrl,
            @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT) String jsonRestRoot,
            @Named(AMSTSConstants.REST_LOGOUT_URI_ELEMENT) String restLogoutUriElement,
            @Named(AMSTSConstants.REST_ID_FROM_SESSION_URI_ELEMENT) String idFromSessionUriElement,
            @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME) String amSessionCookieName,
            @Named (AMSTSConstants.REALM) String realm,
            Provider<UsernameTokenValidator> wssUsernameTokenValidatorProvider,
            Provider<AMTokenProvider> amTokenProviderProvider,
            ThreadLocalAMTokenCache threadLocalAMTokenCache,
            UrlConstituentCatenator urlConstituentCatenator,
            Logger logger) {

        this.amDeploymentUrl = amDeploymentUrl;
        this.jsonRestRoot = jsonRestRoot;
        this.restLogoutUriElement = restLogoutUriElement;
        this.idFromSessionUriElement = idFromSessionUriElement;
        this.amSessionCookieName = amSessionCookieName;
        this.realm = realm;
        this.wssUsernameTokenValidatorProvider = wssUsernameTokenValidatorProvider;
        this.amTokenProviderProvider = amTokenProviderProvider;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.urlConstituentCatenator = urlConstituentCatenator;
        this.logger = logger;
    }

    @Override
    public TokenTransform buildTokenTransform(TokenTransformConfig tokenTransformConfig) throws STSInitializationException {
        TokenType inputTokenType = tokenTransformConfig.getInputTokenType();
        TokenType outputTokenType = tokenTransformConfig.getOutputTokenType();
        TokenValidator tokenValidator = null;
        if (TokenType.USERNAME.equals(inputTokenType)) {
            tokenValidator = buildUsernameTokenValidator();
        } else if (TokenType.OPENAM.equals(inputTokenType)) {
            tokenValidator = buildOpenAMTokenValidator();
        } else {
            String message = "Unexpected input token type of: " + inputTokenType;
            logger.error(message);
            throw new STSInitializationException(message);
        }

        TokenProvider tokenProvider = null;
        if (TokenType.OPENAM.equals(outputTokenType)) {
            tokenProvider = buildOpenAMTokenProvider();
        } else if (TokenType.SAML2.equals(outputTokenType)) {
            tokenProvider = buildOpenSAMLTokenProvider(tokenTransformConfig.isInvalidateInterimOpenAMSession());
        } else {
            String message = "Unexpected output token type of: " + outputTokenType;
            logger.error(message);
            throw new STSInitializationException(message);
        }
        return new TokenTransformImpl(tokenValidator, tokenProvider, inputTokenType, outputTokenType, logger);
    }

    private TokenValidator buildUsernameTokenValidator() {
        org.apache.cxf.sts.token.validator.UsernameTokenValidator validator = new org.apache.cxf.sts.token.validator.UsernameTokenValidator();
        validator.setValidator(wssUsernameTokenValidatorProvider.get());
        return validator;
    }

    /*
    The AMTokenProvider does not need the state from the TokenTransformConfig on whether the interimOpenAMSessionToken
    should be invalidated - if it is issuing an OpenAM token, then obviously this token should not be invalidated.
     */
    private TokenProvider buildOpenAMTokenProvider() {
        return amTokenProviderProvider.get();
    }

    private TokenValidator buildOpenAMTokenValidator() {
        return new AMTokenValidator(
                                amDeploymentUrl,
                                jsonRestRoot,
                                realm,
                                idFromSessionUriElement,
                                amSessionCookieName,
                                threadLocalAMTokenCache,
                                urlConstituentCatenator,
                                logger);
    }

    private TokenProvider buildOpenSAMLTokenProvider(boolean invalidateInterimAMSession) throws STSInitializationException {
        if (invalidateInterimAMSession) {
            try {
                return new AMSAMLTokenProvider(new SAMLTokenProvider(),
                        new AMSessionInvalidatorImpl(amDeploymentUrl,
                                jsonRestRoot, realm, restLogoutUriElement, amSessionCookieName, logger), threadLocalAMTokenCache, logger);
            } catch (URISyntaxException e) {
                throw new STSInitializationException(e.getMessage(), e);
            }
        } else {
            return new AMSAMLTokenProvider(new SAMLTokenProvider(), NULL_AM_SESSION_INVALIDATOR, threadLocalAMTokenCache, logger);
        }
    }
}
