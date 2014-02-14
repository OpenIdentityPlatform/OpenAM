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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.operation.TokenIssueOperation;
import org.apache.cxf.sts.operation.TokenValidateOperation;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.validator.SAMLTokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.operation.ValidateOperation;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.validator.AMTokenValidator;


import javax.xml.ws.WebServiceContext;

import org.forgerock.openam.sts.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                threadLocalAMTokenCache.clearAMToken();
            }
        }
    }

    private final STSPropertiesMBean stsPropertiesMBean;
    private final TokenStore tokenStore;
    private final Set<TokenType> statusTokenTypes;
    private final Map<TokenType,TokenType> transformTokenTypes;
    private final TokenOperationFactory operationFactory;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final Logger logger;

    @Inject
    TokenValidateOperationProvider(
            STSPropertiesMBean stsPropertiesMBean,
            TokenStore tokenStore,
            @Named(AMSTSConstants.TOKEN_VALIDATE_OPERATION_STATUS) Set<TokenType> statusTokenTypes,
            Map<TokenType,TokenType> transformTokenTypes,
            TokenOperationFactory operationFactory,
            ThreadLocalAMTokenCache threadLocalAMTokenCache,
            Logger logger) {
        this.stsPropertiesMBean = stsPropertiesMBean;
        this.tokenStore = tokenStore;
        this.statusTokenTypes = statusTokenTypes;
        this.transformTokenTypes = transformTokenTypes;
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
            logger.debug("The status token types: " + statusTokenTypes);
            for(TokenType tokenType: statusTokenTypes) {
                tokenValidators.add(operationFactory.getTokenStatusValidatorForType(tokenType));
            }

            /*
            Plug-in the TokenValidator instances for the input-set in the token transformation operations.
             */
            for (Map.Entry<TokenType, TokenType> entry : transformTokenTypes.entrySet()) {
                tokenValidators.add(operationFactory.getTokenValidatorForTransformOperation(entry.getKey(), entry.getValue()));
            }

            tokenValidateOperation.setTokenValidators(tokenValidators);

            /*
            Now set the providers, using the values in the transformTokenTypes Map. A problem is the fact that the TokenValidateOperation
            just maintains a set of validators and providers, but there need not be a token transformation available for every
            token type for which token status is available. In other words, the TokenValidateOperation does not maintain two
            collections, a Set and a Map, the first corresponding to the set of tokens for which status can be obtained, and the
            second for the Map of available token transformations. So I need to address this semantic impurity somehow. I think
            every TokenIssueOperation associated with the TokenValidateOperation needs to know what the valid transformation are,
            and can pull the initial token passed to the validate operation from the TokenValidatorParameters.getToken method. But
            wait - if I just plug in the wss TokenValidator, I will not have access to this state. So I may need to plug in the actual
            sts.token.Validator class, so I can get at this state. This state is necessary to determine if the desired token transformation
            is allowed, if a transformation is indeed being specified.
             */
            List<TokenProvider> tokenProviders = new ArrayList<TokenProvider>();
            for (Map.Entry<TokenType, TokenType> entry : transformTokenTypes.entrySet()) {
                tokenProviders.add(operationFactory.getTokenProviderForTransformOperation(entry.getKey(), entry.getValue()));
            }
            tokenValidateOperation.setTokenProviders(tokenProviders);

            return new TokenValidateOperationWrapper(tokenValidateOperation, threadLocalAMTokenCache);
        } catch (STSInitializationException e) {
            logger.error("Exception caught initializing a Validate operation: " + e, e);
            throw new RuntimeException(e);
        }

    }
}
