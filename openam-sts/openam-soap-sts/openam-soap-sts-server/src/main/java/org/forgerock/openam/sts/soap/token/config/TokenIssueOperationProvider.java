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

import javax.inject.Inject;
import com.google.inject.Provider;
import javax.inject.Named;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.operation.TokenIssueOperation;
import org.apache.cxf.sts.token.delegation.TokenDelegationHandler;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.operation.IssueOperation;
import org.apache.cxf.ws.security.sts.provider.operation.IssueSingleOperation;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.soap.config.user.TokenValidationConfig;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;


import javax.xml.ws.WebServiceContext;

import org.forgerock.openam.sts.TokenType;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.slf4j.Logger;

/**
 * This class provides instances of the TokenIssueOperation. The configuration information necessary to construct
 * an appropriately-configured TokenIssueOperation instance will be injected.
 *
 * If, for example, we eventually support the set of configuration options for the SAMLTokenProvider, then an additional
 * configuration object will be injected into this Provider to support these configurations. And it may well be that
 * what is injected is itself a Provider, which has the state necessary to provide the various interface instances necessary
 * to configure a SAMLTokenProvider
 *
 */
public class TokenIssueOperationProvider implements Provider<IssueOperation> {
    /*
    This class exists to wrap top-level STS operations with a finally block to clear the thread-local containing
    the OpenAM session cached as part of any token validation operations.
     */
    static class TokenIssueOperationWrapper implements IssueOperation, IssueSingleOperation {
        private final TokenIssueOperation issueDelegate;
        private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
        TokenIssueOperationWrapper(TokenIssueOperation issueDelegate, ThreadLocalAMTokenCache threadLocalAMTokenCache) {
            this.issueDelegate = issueDelegate;
            this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        }

        @Override
        public RequestSecurityTokenResponseCollectionType issue(RequestSecurityTokenType request,
                                                                Principal principal,
                                                                Map<String, Object> messageContext) {
            try {
                return issueDelegate.issue(request, principal, messageContext);
            } finally {
                threadLocalAMTokenCache.clearCachedSessions();
            }
        }

        @Override
        public RequestSecurityTokenResponseType issueSingle(RequestSecurityTokenType request, Principal principal, Map<String, Object> messageContext) {
            try {
                return issueDelegate.issueSingle(request, principal, messageContext);
            } finally {
                threadLocalAMTokenCache.clearCachedSessions();
            }
        }
    }
    private final STSPropertiesMBean stsPropertiesMBean;
    private final TokenStore tokenStore;
    private final Set<TokenType> issueTokenTypes;
    private final Set<TokenValidationConfig> delegatedTokenValidationConfig;
    private final List<TokenDelegationHandler> tokenDelegationHandlers;
    private final TokenOperationFactory operationFactory;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final Logger logger;

    @Inject
    TokenIssueOperationProvider(
            STSPropertiesMBean stsPropertiesMBean,
            TokenStore tokenStore,
            @Named(AMSTSConstants.ISSUED_TOKEN_TYPES) Set<TokenType> issueTokenTypes,
            @Named(AMSTSConstants.DELEGATED_TOKEN_VALIDATORS) Set<TokenValidationConfig> delegatedTokenValidationConfig,
            List<TokenDelegationHandler> tokenDelegationHandlers,
            TokenOperationFactory operationFactory,
            ThreadLocalAMTokenCache threadLocalAMTokenCache,
            Logger logger) {
        this.stsPropertiesMBean = stsPropertiesMBean;
        this.tokenStore = tokenStore;
        this.issueTokenTypes = issueTokenTypes;
        this.delegatedTokenValidationConfig = delegatedTokenValidationConfig;
        this.tokenDelegationHandlers = tokenDelegationHandlers;
        this.operationFactory = operationFactory;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.logger = logger;
    }

    public IssueOperation get() {
        //TODO: migrate to throwing providers
        try {
            TokenIssueOperation tokenIssueOperation = new TokenIssueOperation();
            /*
            The STS will not encrypt the issued tokens - the TokenGenerationService already offers functionality to
            encrypt issued SAML assertions.
             */
            tokenIssueOperation.setEncryptIssuedToken(false);
            tokenIssueOperation.setStsProperties(stsPropertiesMBean);
            tokenIssueOperation.setTokenStore(tokenStore);
            /*
            Set the tokenValidators which will be called to validate the tokens presented as ActAs or OnBehalfOf
            elements
             */
            tokenIssueOperation.setTokenValidators(getDelegationTokenValidators());

            /*
            Set the TokenDelegationHandlers (either empty if this sts instance will not process ActAs or OnBehalfOf elements,
            or with the DefaultTokenDelegationHandler, or with user-specified custom handlers.
             */
            tokenIssueOperation.setDelegationHandlers(tokenDelegationHandlers);

            List<TokenProvider> tokenProviders = new ArrayList<TokenProvider>();
            for(TokenType tokenType: issueTokenTypes) {
                tokenProviders.add(operationFactory.getTokenProvider(tokenType));
            }
            tokenIssueOperation.setTokenProviders(tokenProviders);
            return new TokenIssueOperationWrapper(tokenIssueOperation, threadLocalAMTokenCache);
        } catch (STSInitializationException e) {
            logger.error("Exception caught initializing a IssueOperation: " + e, e);
            throw new RuntimeException(e);
        }
    }

    private List<TokenValidator> getDelegationTokenValidators() throws STSInitializationException {
        List<TokenValidator> tokenValidators = new ArrayList<TokenValidator>();
        for (TokenValidationConfig tokenValidationConfig : delegatedTokenValidationConfig) {
            tokenValidators.add(operationFactory.getTokenValidator(
                    tokenValidationConfig.getValidatedTokenType(),
                    ValidationInvocationContext.SOAP_TOKEN_DELEGATION,
                    tokenValidationConfig.invalidateInterimOpenAMSession()));
        }
        return tokenValidators;
    }
}
