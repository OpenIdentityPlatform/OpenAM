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

package org.forgerock.openam.sts.rest.token.canceller;

import org.forgerock.openam.sts.TokenCancellationException;
import org.forgerock.openam.sts.TokenTypeId;

/**
 * Interface representing the concerns of token cancellers of rest-sts issued tokens.
 */
public interface RestIssuedTokenCanceller<T> {
    /**
     *
     * @param tokenType the type of the to-be-cancelled token
     * @return true if this RestIssuedTokenCanceller instance can cancel tokens of the specified type
     */
    boolean canCancelToken(TokenTypeId tokenType);

    /**
     *
     * @param cancellerParameters Encapsulation of the state necessary to cancel the token - in this case, simply
     *                            the token itself
     * @throws TokenCancellationException if an exception prevented token cancellation from occurring
     */
    void cancelToken(RestIssuedTokenCancellerParameters<T> cancellerParameters) throws TokenCancellationException;

}
