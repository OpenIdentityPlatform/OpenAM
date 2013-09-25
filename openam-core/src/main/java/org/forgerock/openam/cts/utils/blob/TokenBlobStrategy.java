/**
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.cts.utils.blob;

import javax.inject.Inject;
import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.api.tokens.Token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Responsible for selecting the appropriate algorithm for dealing with Token binary objects
 * prior to them being stored in the data store.
 *
 * @author robert.wapshott@forgerock.com
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
     * Perform the strategy on the Token.
     *
     * Note: This operation will modify the Token.
     *
     * @param token Non null Token to perform the strategy on.
     *
     * @throws TokenStrategyFailedException If an error occurred whilst processing the Token.
     */
    public void perfom(Token token) throws TokenStrategyFailedException {
        for (BlobStrategy strategy : strategies) {
            strategy.perform(token);
        }
    }

    /**
     * Performs the reverse strategy on the Token.
     *
     * Note: This operation will modify the Token.
     *
     * @param token Non null Token to perform the reverse strategy on.
     *
     * @throws TokenStrategyFailedException If an error occurred whilst processing the Token.
     */
    public void reverse(Token token) throws TokenStrategyFailedException {
        for (BlobStrategy strategy : reverseStrategies) {
            strategy.reverse(token);
        }
    }
}
