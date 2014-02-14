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

package org.forgerock.openam.sts.rest.token.provider;

import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;


import org.slf4j.Logger;

/**
 * This encapsulates logic to both create a SAML token, and to invalidate the interim OpenAM session object
 * generated from the preceeding TokenValidation operation if the TokenTransform has been configured to invalidate
 * the interim OpenAM sessions generated from token validation. Note that thus the AMSessionInvalidator can be null
 * TODO: really would like to use something like @Nullable everywhere where nulls are expected, and then use something like
 * Guava's Preconditions.checkNotNull on every reference without a @Nullable. Update with Reject from forgerock commons.
 * TODO: proper exception
 */
public class AMSAMLTokenProvider implements TokenProvider {
    private final SAMLTokenProvider delegate;
    private final AMSessionInvalidator amSessionInvalidator;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final Logger logger;

    /**
     *
     * @param delegate The SAMLTokenProvider which will ultimately issue the SAML token. This will be replaced by OpenAM
     *                 SAML assertion generation classes.
     * @param amSessionInvalidator Possibly null. Will be called to invalidate the OpenAM session generated via token
     *                             validation if the TokenTransform was configured to invalidate interim OpenAM sessions.
     * @param threadLocalAMTokenCache Will be used to pull the OpenAM session.
     * @param logger
     */
    public AMSAMLTokenProvider(SAMLTokenProvider delegate, AMSessionInvalidator amSessionInvalidator,
                               ThreadLocalAMTokenCache threadLocalAMTokenCache, Logger logger) {
        this.delegate = delegate;
        this.amSessionInvalidator = amSessionInvalidator;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.logger = logger;
    }

    @Override
    public boolean canHandleToken(String tokenType) {
        return delegate.canHandleToken(tokenType);
    }

    @Override
    public boolean canHandleToken(String tokenType, String realm) {
        return delegate.canHandleToken(tokenType, realm);
    }

    @Override
    public TokenProviderResponse createToken(TokenProviderParameters tokenParameters) {
        TokenProviderResponse tokenProviderResponse = delegate.createToken(tokenParameters);
        if (amSessionInvalidator != null) {
            try {
                amSessionInvalidator.invalidateAMSession(threadLocalAMTokenCache.getAMToken());
            } catch (Exception e) {
                String message = "Exception caught invalidating interim AMSession: " + e;
                logger.warn(message, e);
                /*
                I cannot throw an exception, as the TokenProvider interface does not permit it.
                I could mark the token status in the TokenProviderResponse as invalid, but the fact that
                the interim OpenAM session was not invalidated should not prevent a token from being issued, so
                I will neither throw a RuntimeException either nor mark the token status as invalid.
                 */
            }
        }
        return tokenProviderResponse;
    }
}
