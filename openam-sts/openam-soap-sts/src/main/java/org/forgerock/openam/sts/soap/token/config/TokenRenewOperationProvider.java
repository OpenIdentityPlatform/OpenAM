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
import org.apache.cxf.sts.operation.TokenRenewOperation;
import org.apache.cxf.sts.operation.TokenValidateOperation;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.renewer.SAMLTokenRenewer;
import org.apache.cxf.sts.token.renewer.TokenRenewer;
import org.apache.cxf.sts.token.validator.SAMLTokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.operation.RenewOperation;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.validator.AMTokenValidator;


import javax.xml.ws.WebServiceContext;

import org.forgerock.openam.sts.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

/**
 * This class provides instances of the TokenRenewOperation. The configuration information necessary to construct
 * an appropriately-configured TokenValidateOperation instance will be injected.
 *
 * If, for example, we eventually support the set of configuration options for the SAMLTokenValidator, then an additional
 * configuration object will be injected into this Provider to support these configurations. And it may well be that
 * what is injected is itself a Provider, which has the state necessary to provide the various interface instances necessary
 * to configure a SAMLTokenValidator
 *
 */
public class TokenRenewOperationProvider implements Provider<RenewOperation> {
    /*
    This class exists to wrap top-level STS operations with a finally block to clear the thread-local containing
    the OpenAM session cached as part of any token validation operations.
     */
    class TokenRenewOperationWrapper implements RenewOperation {
        private final TokenRenewOperation renewDelegate;
        private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
        TokenRenewOperationWrapper(TokenRenewOperation renewDelegate, ThreadLocalAMTokenCache threadLocalAMTokenCache) {
            this.renewDelegate = renewDelegate;
            this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        }

        @Override
        public RequestSecurityTokenResponseType renew(RequestSecurityTokenType request, WebServiceContext context) {
            try {
                return renewDelegate.renew(request, context);
            } finally {
                threadLocalAMTokenCache.clearAMToken();
            }
        }
    }
    private final STSPropertiesMBean stsPropertiesMBean;
    private final TokenStore tokenStore;
    private final Set<TokenType> renewTokenTypes;
    private final TokenOperationFactory operationFactory;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final Logger logger;

    @Inject
    TokenRenewOperationProvider(
            STSPropertiesMBean stsPropertiesMBean,
            TokenStore tokenStore,
            @Named(AMSTSConstants.TOKEN_RENEW_OPERATION) Set<TokenType> renewTokenTypes,
            TokenOperationFactory operationFactory,
            ThreadLocalAMTokenCache threadLocalAMTokenCache,
            Logger logger) {
        this.stsPropertiesMBean = stsPropertiesMBean;
        this.tokenStore = tokenStore;
        this.renewTokenTypes = renewTokenTypes;
        this.operationFactory = operationFactory;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.logger = logger;
    }

    public RenewOperation get() {
        // TODO: migrate to ThrowingProviders
        try {
            TokenRenewOperation tokenRenewOperation = new TokenRenewOperation();
            tokenRenewOperation.setStsProperties(stsPropertiesMBean);
            tokenRenewOperation.setTokenStore(tokenStore);

            /*
            Token validators must be provided to the token renewal operation, as renewal first involves
            validation. The set of renewable tokens should define the set of validated tokens, so I can use
            the TOKEN_RENEW_OPERATION as the list for both validators and renewers.
             */
            List<TokenValidator> tokenValidators = new ArrayList<TokenValidator>();
            for(TokenType tokenType: renewTokenTypes) {
                tokenValidators.add(operationFactory.getTokenValidatorForRenewal(tokenType));
            }
            tokenRenewOperation.setTokenValidators(tokenValidators);

            List<TokenRenewer> tokenRenewers = new ArrayList<TokenRenewer>();
            for(TokenType tokenType: renewTokenTypes) {
                tokenRenewers.add(operationFactory.getTokenRenewerForType(tokenType));
            }
            tokenRenewOperation.setTokenRenewers(tokenRenewers);

            return new TokenRenewOperationWrapper(tokenRenewOperation, threadLocalAMTokenCache);
        } catch (STSInitializationException e) {
            logger.error("Exception caught initializing a RenewOperation: " + e, e);
            throw new RuntimeException(e);
        }
    }
}
