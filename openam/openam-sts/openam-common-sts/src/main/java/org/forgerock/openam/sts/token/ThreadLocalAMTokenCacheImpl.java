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

package org.forgerock.openam.sts.token;


import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenCreationException;

import javax.inject.Inject;
import org.slf4j.Logger;

/**
 * This class is used to Cache the OpenAM session/token id in a ThreadLocal so it can be obtained in token issue/transformation
 * operations after successful validation in SecurityPolicy or WS-Trust Validate layers.
 *
 * The clearAMToken is called in a finally block in the entry point for all STS operations.
 *
 * Another option would be to put the OpenAM session id in the HTTPServletRequest (and possibly in the HttpServletResponse).
 * Then not exposed to the security-issue of not clearing the ThreadLocal. As long as I can access the HTTPServletRequest in
 * all relevant contexts...
 * Seems to be present in
 * TokenProviderParameters
 * TokenValidatorParameters
 * TokenRenewerParameters
 * TokenCancelerParameters
 * Seems to be possible to pull from RequestData provided to SecurityPolicy validators. Except for a bug in
 * wss4j which I fixed - need to roll to 2.7.9 to get this fix, once (if) 2.7.9 released. See
 * https://issues.apache.org/jira/browse/CXF-5458 for details.
 */
public class ThreadLocalAMTokenCacheImpl implements ThreadLocalAMTokenCache {
    private static final ThreadLocal<String> sessionHolder = new ThreadLocal<String>();
    private final Logger logger;

    @Inject
    ThreadLocalAMTokenCacheImpl(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void cacheAMToken(String tokenId) {
        if (sessionHolder.get() != null) {
            logger.warn("The ThreadLocal contains a session value in call to " +
                    "cacheAMToken. This means that this ThreadLocal was not cleared at end of previous use, or (more likely) " +
                    "that SecurityPolicy validation has already cached an AMToken instance prior to token validation.");
        }
        sessionHolder.set(tokenId);
    }

    @Override
    public String getAMToken() throws TokenCreationException {
        String sessionId = sessionHolder.get();
        if (sessionId ==  null) {
            String message = "No sessionId cached in ThreadLocal. Illegal State!!";
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR, message);
        }
        return sessionId;
    }

    @Override
    public void clearAMToken() {
        boolean entryPresent = (sessionHolder.get() != null);
        sessionHolder.remove();
        logger.debug("When clearing the AMSession thread-local, was a value set?: " + entryPresent);
    }
}
