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

package org.forgerock.openam.sts.token.provider;

import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.token.AMTokenCache;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.w3c.dom.Element;

import javax.inject.Inject;
import org.slf4j.Logger;

/**
 * This class will 'issue' and OpenAM token. It will be deployed only in the context of token transformation, so it
 * will expect a OpenAM session id in the ThreadLocalAMTokenCache. Note that final token transformation use-cases will
 * probably not return a OpenAM token, but rather use this token to authenticate the Principal to be asserted in any
 * token returned by the OpenAM REST token-creation API.
 */
public class AMTokenProvider implements TokenProvider {
//    private final AMTokenCache tokenCache;
    private final ThreadLocalAMTokenCache tokenCache;
    private final OpenAMSessionIdElementBuilder sessionIdElementBuilder;
    private  final Logger logger;

    /*
    Right now, this is not inject, but rather created in a Provider defined in the STSInstanceModule.
     */
    public AMTokenProvider(/*AMTokenCache tokenCache,*/ThreadLocalAMTokenCache tokenCache, OpenAMSessionIdElementBuilder sessionIdElementBuilder, Logger logger) {
        this.tokenCache = tokenCache;
        this.sessionIdElementBuilder = sessionIdElementBuilder;
        this.logger = logger;
    }

    @Override
    public boolean canHandleToken(String tokenType) {
        return AMSTSConstants.AM_TOKEN_TYPE.equals(tokenType);
    }

    @Override
    public boolean canHandleToken(String tokenType, String realm) {
        return canHandleToken(tokenType);
    }

    @Override
    public TokenProviderResponse createToken(TokenProviderParameters tokenParameters) {
        TokenProviderResponse response = new TokenProviderResponse();
        String sessionId = null;
        try {
//            sessionId = tokenCache.getAMSessionId(tokenParameters);
            sessionId = tokenCache.getAMToken();
            response.setToken(sessionIdElementBuilder.buildOpenAMSessionIdElement(sessionId));
        } catch (Exception e) {
            logger.error("Exception caught creating a AMToken using cached sessionId: " + e, e);
            //TODO: what exception can I throw? - cannot throw checked - STS TokenProviders throw the STSException - Also tighten the catch block
            // perhaps this is what I should do...
            throw new IllegalStateException("Exception caught creating a AMToken using cached sessionId: " + e);
        }
        response.setTokenId(sessionId);
        return response;
    }
}
