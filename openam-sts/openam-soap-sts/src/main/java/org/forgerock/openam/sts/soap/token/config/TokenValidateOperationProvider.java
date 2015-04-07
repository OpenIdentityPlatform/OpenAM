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

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.operation.TokenValidateOperation;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.operation.ValidateOperation;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.soap.config.user.TokenValidationConfig;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;

import javax.xml.ws.WebServiceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.slf4j.Logger;

/**
 * This class provides instances of the TokenValidateOperation. The configuration information necessary to construct
 * an appropriately-configured TokenValidateOperation instance will be injected.
 *
 * If, for example, we eventually support the set of configuration options for the SAMLTokenValidator, then an additional
 * configuration object will be injected into this Provider to support these configurations. And it may well be that
 * what is injected is itself a Provider, which has the state necessary to provide the various interface instances necessary
 * to configure a SAMLTokenValidator
 *
 */
public class TokenValidateOperationProvider implements Provider<ValidateOperation> {
    /*
    This class exists to wrap top-level STS operations with a finally block to clear the thread-local containing
    the OpenAM session cached as part of any token validation operations.
     */
    static class TokenValidateOperationWrapper implements ValidateOperation {
        private final TokenValidateOperation validateDelegate;
        private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
        TokenValidateOperationWrapper(TokenValidateOperation validateDelegate, ThreadLocalAMTokenCache threadLocalAMTokenCache) {
            this.validateDelegate = validateDelegate;
            this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        }

        @Override
        public RequestSecurityTokenResponseType validate(RequestSecurityTokenType request, WebServiceContext context) {
            try {
                return validateDelegate.validate(request, context);
            } finally {
                threadLocalAMTokenCache.clearCachedSessions();
            }
        }
    }

    private final STSPropertiesMBean stsPropertiesMBean;
    private final TokenStore tokenStore;
    private final Set<TokenValidationConfig> validatedTokenConfig;
    private final TokenOperationFactory operationFactory;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final Logger logger;

    @Inject
    TokenValidateOperationProvider(
            STSPropertiesMBean stsPropertiesMBean,
            TokenStore tokenStore,
            Set<TokenValidationConfig> validatedTokenConfig,
            TokenOperationFactory operationFactory,
            ThreadLocalAMTokenCache threadLocalAMTokenCache,
            Logger logger) {
        this.stsPropertiesMBean = stsPropertiesMBean;
        this.tokenStore = tokenStore;
        this.validatedTokenConfig = validatedTokenConfig;
        this.operationFactory = operationFactory;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.logger = logger;
    }

    public ValidateOperation get() {
// TODO: migrate to ThrowingProviders

        try {
            TokenValidateOperation tokenValidateOperation = new TokenValidateOperation();
            tokenValidateOperation.setStsProperties(stsPropertiesMBean);
            tokenValidateOperation.setTokenStore(tokenStore);

            List<TokenValidator> tokenValidators = new ArrayList<TokenValidator>();
            for (TokenValidationConfig tokenValidationConfig : validatedTokenConfig) {
                tokenValidators.add(operationFactory.getTokenValidator(tokenValidationConfig.getValidatedTokenType(),
                        ValidationInvocationContext.SOAP_TOKEN_VALIDATION, tokenValidationConfig.invalidateInterimOpenAMSession()));
            }
            tokenValidateOperation.setTokenValidators(tokenValidators);
            return new TokenValidateOperationWrapper(tokenValidateOperation, threadLocalAMTokenCache);
        } catch (STSInitializationException e) {
            logger.error("Exception caught initializing a Validate operation: " + e, e);
            throw new RuntimeException(e);
        }
    }
}
