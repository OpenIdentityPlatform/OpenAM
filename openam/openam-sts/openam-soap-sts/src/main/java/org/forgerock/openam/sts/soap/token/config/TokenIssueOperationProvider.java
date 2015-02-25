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
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.operation.IssueOperation;
import org.apache.cxf.ws.security.sts.provider.operation.IssueSingleOperation;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;


import javax.xml.ws.WebServiceContext;

import org.forgerock.openam.sts.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        public RequestSecurityTokenResponseCollectionType issue(RequestSecurityTokenType request, WebServiceContext context) {
            try {
                return issueDelegate.issue(request, context);
            } finally {
                threadLocalAMTokenCache.clearAMToken();
            }
        }

        @Override
        public RequestSecurityTokenResponseType issueSingle(RequestSecurityTokenType request, WebServiceContext context) {
            try {
                return issueDelegate.issueSingle(request, context);
            } finally {
                threadLocalAMTokenCache.clearAMToken();
            }
        }
    }
    private final STSPropertiesMBean stsPropertiesMBean;
    private final TokenStore tokenStore;
    private final Set<TokenType> tokenTypes;
    private final TokenOperationFactory operationFactory;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final Logger logger;

    @Inject
    TokenIssueOperationProvider(
            STSPropertiesMBean stsPropertiesMBean,
            TokenStore tokenStore,
            @Named(AMSTSConstants.TOKEN_ISSUE_OPERATION) Set<TokenType> tokenTypes,
            TokenOperationFactory operationFactory,
            ThreadLocalAMTokenCache threadLocalAMTokenCache,
            Logger logger) {
        this.stsPropertiesMBean = stsPropertiesMBean;
        this.tokenStore = tokenStore;
        this.tokenTypes = tokenTypes;
        this.operationFactory = operationFactory;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.logger = logger;
    }

    public IssueOperation get() {
        //TODO: migrate to throwing providers
        try {
            TokenIssueOperation tokenIssueOperation = new TokenIssueOperation();
            //TODO: does this need to be configurable?
            tokenIssueOperation.setEncryptIssuedToken(false);
            tokenIssueOperation.setStsProperties(stsPropertiesMBean);
            tokenIssueOperation.setTokenStore(tokenStore);

            List<TokenProvider> tokenProviders = new ArrayList<TokenProvider>();
            for(TokenType tokenType: tokenTypes) {
                tokenProviders.add(operationFactory.getTokenProviderForType(tokenType));
            }
            tokenIssueOperation.setTokenProviders(tokenProviders);
            return new TokenIssueOperationWrapper(tokenIssueOperation, threadLocalAMTokenCache);
        } catch (STSInitializationException e) {
            logger.error("Exception caught initializing a IssueOperation: " + e, e);
            throw new RuntimeException(e);
        }
    }
}
