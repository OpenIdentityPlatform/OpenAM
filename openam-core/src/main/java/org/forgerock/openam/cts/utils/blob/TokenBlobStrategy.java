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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.utils.blob;

import org.forgerock.openam.cts.CoreTokenConfig;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Responsible for selecting the appropriate algorithm for dealing with Token binary objects
 * prior to them being stored in the data store.
 */
public class TokenBlobStrategy {
    private final Collection<BlobStrategy> strategies;
    private final List<BlobStrategy> reverseStrategies;

    @Inject
    public TokenBlobStrategy(TokenStrategyFactory factory, CoreTokenConfig config) {
        strategies = factory.getStrategies(config);
        // Reverse list.
        reverseStrategies = new ArrayList<BlobStrategy>(strategies);
        Collections.reverse(reverseStrategies);
    }

    /**
     * Perform the strategy on the byte array.
     *
     * @param data A possibly null byte[] to perform the strategy on.
     * @return A modified copy of the byte[] or null if data was null.
     *
     * @throws TokenStrategyFailedException If an error occurred whilst processing the Token.
     */
    public byte[] perform(byte[] data) throws TokenStrategyFailedException {
        return apply(strategies, true, data);
    }

    /**
     * Performs the reverse strategy on the byte array.
     *
     * @param data A possibly null byte[] to perform the reverse strategy on.
     * @return A modified copy of the byte[] or null if data was null.
     *
     * @throws TokenStrategyFailedException If an error occurred whilst processing the Token.
     */
    public byte[] reverse(byte[] data) throws TokenStrategyFailedException {
        return apply(reverseStrategies, false, data);
    }

    /**
     * Applies the change to the byte[].
     *
     * @param strategies Non null strategies to apply.
     * @param perform True indicates perform, false indicates reverse.
     * @param data The data to apply the change to. May be null.
     * @return A copy of the data with the change applied. May be null if data was null.
     *
     * @throws TokenStrategyFailedException If there was a problem performing the operation.
     */
    private byte[] apply(Collection<BlobStrategy> strategies, boolean perform, byte[] data)
            throws TokenStrategyFailedException {
        if (data == null) {
            return null;
        }

        byte[] r = Arrays.copyOf(data, data.length);
        for (BlobStrategy strategy : strategies) {
            if (perform) {
                r = strategy.perform(r);
            } else {
                r = strategy.reverse(r);
            }
        }
        return r;
    }
}
