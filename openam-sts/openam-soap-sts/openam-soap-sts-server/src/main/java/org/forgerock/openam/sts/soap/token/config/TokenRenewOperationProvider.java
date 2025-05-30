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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.operation.TokenRenewOperation;
import org.apache.cxf.sts.token.renewer.TokenRenewer;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.operation.RenewOperation;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.TokenType;

import javax.xml.ws.WebServiceContext;

import java.security.Principal;
import java.util.List;
import java.util.Map;
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
 * TODO: this class will be resurrected when AME-5869 is implemented
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
        public RequestSecurityTokenResponseType renew(RequestSecurityTokenType request,
                                                      Principal principal,
                                                      Map<String, Object> messageContext) {
            try {
                return renewDelegate.renew(request, principal, messageContext);
            } finally {
                threadLocalAMTokenCache.clearCachedSessions();
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

            tokenRenewOperation.setTokenValidators(getTokenValidators());

            tokenRenewOperation.setTokenRenewers(getTokenRenewers());

            return new TokenRenewOperationWrapper(tokenRenewOperation, threadLocalAMTokenCache);
        } catch (STSInitializationException e) {
            logger.error("Exception caught initializing a RenewOperation: " + e, e);
            throw new RuntimeException(e);
        }
    }

    private List<TokenValidator> getTokenValidators() throws STSInitializationException {
        return null;
    }

    private List<TokenRenewer> getTokenRenewers() throws STSInitializationException {
        return null;
    }
}
