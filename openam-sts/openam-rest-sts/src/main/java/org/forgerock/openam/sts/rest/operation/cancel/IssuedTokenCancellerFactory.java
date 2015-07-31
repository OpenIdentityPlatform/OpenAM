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

package org.forgerock.openam.sts.rest.operation.cancel;

import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.rest.token.canceller.RestIssuedTokenCanceller;

/**
 * Responsible for created RestIssuedTokenCanceller instances for a specific token type. Consumed by the IssuedTokenCancelOperationImpl,
 * to constitute the set of IssuedTokenCanceller instances corresponding to tokens issued by this particular sts instance
 * (if the sts instance is configured to persist issued tokens in the CTS).
 */
public interface IssuedTokenCancellerFactory {
    /**
     *
     * @param tokenType The TokenTypeId for which a RestIssuedTokenCanceller instance should be obtained
     * @return the RestIssuedTokenCanceller instance which can cancel the specified TokenType
     * @throws STSInitializationException if a RestIssuedTokenCanceller cannot be created - usually because the TokenTypeId is
     * unsupported - i.e. does not correspond to a rest-sts-issued token.
     */
    RestIssuedTokenCanceller getTokenCanceller(TokenTypeId tokenType) throws STSInitializationException;

}
