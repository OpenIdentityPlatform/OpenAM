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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap.token.config;

import com.google.inject.Inject;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.operation.TokenCancelOperation;
import org.apache.cxf.sts.token.canceller.TokenCanceller;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.operation.CancelOperation;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;

import javax.inject.Named;
import javax.inject.Provider;
import javax.xml.ws.WebServiceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Provider for the CancelOperation exposed by the soap-sts instance
 */
public class TokenCancelOperationProvider implements Provider<CancelOperation> {
    /*
    This class exists to wrap top-level STS operations with a finally block to clear the thread-local containing
    the OpenAM session cached as part of any token validation operations. Note that the actual TokenCancel implementations
    will not set the ThreadLocalAMTokenCache with OpenAM session tokens resulting from token validation (as none is performed),
    but SecurityPolicy binding traversal will result in ThreadLocalAMTokenCache population with OpenAM Session token state,
    which must be cleared in all cases.
     */
    static class TokenCancelOperationWrapper implements CancelOperation {
        private final TokenCancelOperation cancelDelegate;
        private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
        TokenCancelOperationWrapper(TokenCancelOperation cancelDelegate, ThreadLocalAMTokenCache threadLocalAMTokenCache) {
            this.cancelDelegate = cancelDelegate;
            this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        }

        @Override
        public RequestSecurityTokenResponseType cancel(RequestSecurityTokenType request, WebServiceContext context) {
            try {
                return cancelDelegate.cancel(request, context);
            } finally {
                threadLocalAMTokenCache.clearCachedSessions();
            }
        }
    }
    /*
    Class returned if the sts is configured to not persist issued tokens in the STS, which is necessary to implement
    the CancelOperation. Will simply throw an exception indicating that the operation is unsupported in the current
    configuration.
     */
    static class UnsupportedCancelOperation implements CancelOperation {
        @Override
        public RequestSecurityTokenResponseType cancel(RequestSecurityTokenType request, WebServiceContext context) {
            throw new STSException("Soap STS instance not configured to persist issued tokens in the CoreTokenService, and " +
                    "thus the CancelOperation cannot be implemented. If the cancellation of issued tokens is desired, " +
                    "republish the soap-sts instance, with configuration which indicates that issued tokens should be " +
                    "persisted in the CoreTokenService.");
        }
    }

    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final STSPropertiesMBean stsPropertiesMBean;
    private final TokenStore tokenStore;
    private final Set<TokenType> validatedTokens;
    private final TokenOperationFactory operationFactory;
    private final boolean issuedTokensPersistedInCTS;

    @Inject
    TokenCancelOperationProvider(
            ThreadLocalAMTokenCache threadLocalAMTokenCache,
            STSPropertiesMBean stsPropertiesMBean,
            TokenStore tokenStore,
            @Named(AMSTSConstants.ISSUED_TOKEN_TYPES) Set<TokenType> validatedTokens,
            TokenOperationFactory operationFactory,
            @Named (AMSTSConstants.ISSUED_TOKENS_PERSISTED_IN_CTS) boolean issuedTokensPersistedInCTS) {
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.stsPropertiesMBean = stsPropertiesMBean;
        this.tokenStore = tokenStore;
        this.validatedTokens = validatedTokens;
        this.operationFactory = operationFactory;
        this.issuedTokensPersistedInCTS = issuedTokensPersistedInCTS;
    }

    public CancelOperation get() {
        if (issuedTokensPersistedInCTS) {
            return getFunctionalCancelOperation();
        } else {
            return getUnsupportedCancelOperation();
        }
    }

    private CancelOperation getFunctionalCancelOperation() {
        TokenCancelOperation tokenCancelOperation = new TokenCancelOperation();
        tokenCancelOperation.setStsProperties(stsPropertiesMBean);
        tokenCancelOperation.setTokenStore(tokenStore);
        try {
            List<TokenCanceller> tokenCancellers = new ArrayList<>();
            for (TokenType tokentype : validatedTokens) {
                tokenCancellers.add(operationFactory.getTokenCanceller(tokentype));
            }
            tokenCancelOperation.setTokenCancellers(tokenCancellers);
        } catch (STSInitializationException e) {
            throw new RuntimeException(e);
        }
        return new TokenCancelOperationWrapper(tokenCancelOperation, threadLocalAMTokenCache);
    }

    private CancelOperation getUnsupportedCancelOperation() {
        return new UnsupportedCancelOperation();
    }
}
