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
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.operation.ValidateOperation;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;

import javax.inject.Named;
import javax.xml.ws.WebServiceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class provides instances of the TokenValidateOperation. The configuration information necessary to construct
 * an appropriately-configured TokenValidateOperation instance will be injected.
 *
 * In the 13 release, token validation will simply consult the CTS (via the TokenService) to see whether the specified
 * token has been issued. Note that each sts instance will be published with configuration state which indicates
 * whether tokens issued by this sts will be stored in the CTS. This support is necessary to implement a functional
 * ValidateOperation. Thus this Provider will implement logic to either deploy a ValidateOperation with a set of
 * TokenValidator implementations which will consume the TokenService to validate sts-issued tokens, or with a UnsupportedValidateOperation,
 * which will simply throw an exception when invoked, as CTS persistence is required for the validation if issued tokens.
 */
public class TokenValidateOperationProvider implements Provider<ValidateOperation> {
    /*
    This class exists to wrap top-level STS operations with a finally block to clear the thread-local containing
    the OpenAM session cached as part of any token validation operations. Note that the actual TokenValidator implementations
    will not set the ThreadLocalAMTokenCache with OpenAM session tokens resulting from token validation, but SecurityPolicy
    binding traversal will result in ThreadLocalAMTokenCache population with OpenAM Session token state, which must be
    cleared in all cases.
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

    /*
    Class returned if the sts is configured to not persist issued tokens in the STS, which is necessary to implement
    the ValidateOperation. Will simply throw an exception indicating that the operation is unsupported in the current
    configuration.
     */
    static class UnsupportedValidateOperation implements ValidateOperation {
        @Override
        public RequestSecurityTokenResponseType validate(RequestSecurityTokenType request, WebServiceContext context) {
            throw new STSException("Soap STS instance not configured to persist issued tokens in the CoreTokenService, and " +
                    "thus the ValidateOperation cannot be implemented. If the validation of issued tokens is desired, " +
                    "republish the soap-sts instance, with configuration which indicates that issued tokens should be " +
                    "persisted in the CoreTokenService.");
        }
    }

    private final STSPropertiesMBean stsPropertiesMBean;
    private final TokenStore tokenStore;
    private final Set<TokenType> validatedTokens;
    private final TokenOperationFactory operationFactory;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final boolean issuedTokensPersistedInCTS;

    @Inject
    TokenValidateOperationProvider(
            STSPropertiesMBean stsPropertiesMBean,
            TokenStore tokenStore,
            @Named(AMSTSConstants.ISSUED_TOKEN_TYPES) Set<TokenType> validatedTokens,
            TokenOperationFactory operationFactory,
            ThreadLocalAMTokenCache threadLocalAMTokenCache,
            @Named (AMSTSConstants.ISSUED_TOKENS_PERSISTED_IN_CTS) boolean issuedTokensPersistedInCTS) {
        this.stsPropertiesMBean = stsPropertiesMBean;
        this.tokenStore = tokenStore;
        this.validatedTokens = validatedTokens;
        this.operationFactory = operationFactory;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.issuedTokensPersistedInCTS = issuedTokensPersistedInCTS;
    }

    public ValidateOperation get() {
        if (issuedTokensPersistedInCTS) {
            return getFunctionalValidateOperation();
        } else {
            return getUnsupportedValidateOperation();
        }
    }

    private ValidateOperation getFunctionalValidateOperation() {
        TokenValidateOperation tokenValidateOperation = new TokenValidateOperation();
        tokenValidateOperation.setStsProperties(stsPropertiesMBean);
        tokenValidateOperation.setTokenStore(tokenStore);
        try {
            List<TokenValidator> tokenValidators = new ArrayList<>();
            for (TokenType tokentype : validatedTokens) {
                tokenValidators.add(operationFactory.getSimpleTokenValidator(tokentype));
            }
            tokenValidateOperation.setTokenValidators(tokenValidators);
        } catch (STSInitializationException e) {
            throw new RuntimeException(e);
        }
        return new TokenValidateOperationWrapper(tokenValidateOperation, threadLocalAMTokenCache);
    }

    private ValidateOperation getUnsupportedValidateOperation() {
        return new UnsupportedValidateOperation();
    }
}
