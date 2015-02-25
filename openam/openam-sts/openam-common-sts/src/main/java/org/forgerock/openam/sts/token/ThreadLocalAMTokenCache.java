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

import org.forgerock.openam.sts.TokenCreationException;

/**
 * This interface defines the contract that allows token validators (either SecurityPolicy- or STS-related) to persist
 * the OpenSSO session id resulting from a successful token verification. This session id will be the input for all
 * token transformation operations. The bottom line is that state has to be communicated from token validators
 * to token issuers (in the case of the STS validate token transformation actions), within the STS token validation
 * framework. Storing and clearing this state in a ThreadLocal is one approach - another might be to store this token
 * state in a actual STS token store. Because the STS token store is bound by guice, it might make sense to leverage this first -
 * relieves me of the headache of always clearing the thread-local, and worries about propagating ThreadLocal state
 * to newly created threads. But the problem with the TokenStore is that tokens can only be looked-up by key - and the
 * key itself is the session id, so the question is how to communicate this identifier other than via a ThreadLocal, or
 * a defined attribute in the ServletRequest? The two viable approaches are storing the session_id in a ThreadLocal, or
 * in the ServletRequest, as implemented in the AMTokenCacheImpl. See comments in the AMTokenCacheImpl for reasons why the
 * ThreadLocal approach is currently in favor.
 */
public interface ThreadLocalAMTokenCache {
    /**
     * Caches the String corresponding to an OpenAM session id in a thread-local.
     * @param tokenId The OpenAM session id.
     */
    void cacheAMToken(String tokenId);

    /**
     *
     * @return the OpenAM session id stored in the thread-local. A TokenCreationException is thrown if no value is set
     * in the thread-local.
     */
    String getAMToken() throws TokenCreationException;

    /**
     * Clear the thread-local. Must be called in a finally block in the outermost layer of an STS deployment.
     */
    void clearAMToken();
}
