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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.sm.datalayer.api;

import java.text.MessageFormat;

/**
 * Indicates that an operation has failed due to an assertion failure.
 *
 * <p>The current revision of the token did not match the provided etag.
 * The caller should perform a read and re-apply any changes before attempting the operation again.</p>
 */
public class OptimisticConcurrencyCheckFailedException extends DataLayerException {

    /**
     * Constructs a new OptimisticConcurrencyCheckFailedException.
     *
     * @param tokenId The token ID.
     * @param etag The expected etag of the token.
     * @param cause The cause.
     */
    public OptimisticConcurrencyCheckFailedException(String tokenId, String etag, Throwable cause) {
        super(MessageFormat.format(
                "Operation failed:\n" +
                        "Token ID: {0}\n" +
                        "ETag: {1}",
                tokenId, etag), cause);
    }
}
