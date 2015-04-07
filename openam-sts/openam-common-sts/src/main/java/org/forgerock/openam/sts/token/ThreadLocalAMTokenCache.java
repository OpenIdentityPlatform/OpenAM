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
 * Copyright 2013-2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token;

import org.forgerock.openam.sts.TokenCreationException;

import java.util.Set;

/**
 * This interface defines the contract that allows token validators (either SecurityPolicy- or STS-related) to persist
 * the OpenAM session id resulting from a successful token verification. STS operations, like token transformation,
 * involve a loosely-coupled deployment of token operations, like TokenValidators and TokenProviders. An OpenAM session
 * id is the fundamental, internal token which must be communicated from TokenValidators to TokenProviders, as it is
 * the basis for the subject asserted by tokens created by the TokenGenerationService, and the basis for subject
 * attributes included as claims in these tokens.
 * Configuration state in published sts instances will determine whether the OpenAM session resulting from token validation
 * should be invalidated following output token generation. Note that these scenarios can be somewhat complicated: in a
 * soap-sts ISSUE invocation protected by SecurityPolicy bindings, and containing a delegated token (ActAs/OnBehalfOf)
 * in the RequestSecurityToken, two TokenValidators will be invoked: the first to validate the SupportingToken specified
 * in the SecurityPolicy bindings protecting the ISSUE operation, and the second to validate the delegated token. Both
 * sessions must potentially be invalidated, depending upon configuration state associated with the published soap-sts
 * instance.
 * The ValidationInvocationContext enum will allow TokenValidators to know the context of their invocation, and thus
 * where to store the resulting OpenAM session in the ThreadLocal. They will also be created with the state which will
 * tell them whether the OpenAM session should be invalidated, which will also allow them to update the ThreadLocal
 * accordingly. The AMSessionInvalidatorImpl class will consult this cache to obtain the set of OpenAM session ids which
 * must be invalidated following output token creation.
 */
public interface ThreadLocalAMTokenCache {
    void cacheAMSessionId(String sessionId, boolean invalidateAfterTokenCreation);

    String getAMSessionId() throws TokenCreationException;

    void cacheDelegatedAMSessionId(String sessionId, boolean invalidateAfterTokenCreation);

    String getDelegatedAMSessionId() throws TokenCreationException;

    Set<String> getToBeInvalidatedAMSessionIds();

    /**
     * Clear the thread-local. Must be called in a finally block in the outermost layer of an STS deployment.
     */
    void clearCachedSessions();
}
