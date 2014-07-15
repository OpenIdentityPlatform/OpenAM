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
package org.forgerock.openam.cts.monitoring.impl.persistence;

import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.exceptions.CoreTokenException;

import javax.inject.Inject;
import java.util.Collection;

/**
 * This acts to control the interaction between the exposed monitoring endpoints and the CTS persistence
 * store for queries based on a {@link TokenType}
 */
public class CtsPersistenceOperationsMonitor {

    //for gathering results from the store
    private final CtsPersistenceOperationsDelegate delegate;

    /**
     * CtsPersistenceOperationsMonitor allows us to quickly query the CTS for information that we expose across
     * our monitoring services.
     *
     */
    @Inject
    public CtsPersistenceOperationsMonitor(CtsPersistenceOperationsDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns the total number of current tokens in the CTS.
     *
     * @return a positive long which is the total number of tokens
     * @throws CoreTokenException if there are problems communicating with the persistence store
     */
    public Long getTotalCount() throws CoreTokenException {
        return (long) delegate.countAllTokens();
    }

    /**
     * Returns the total number of current valid tokens of the supplier type
     *
     * @param tokenType the token type we're looking up
     * @return a positive long which is the total number of tokens of the supplied type
     * @throws CoreTokenException if there are problems communicating with the persistence store
     */
    public Long getTotalCount(TokenType tokenType) throws CoreTokenException {

        if (tokenType == null) {
            throw new NullPointerException("TokenType cannot be null.");
        }

        return (long) delegate.countTokenEntries(tokenType);
    }

    /**
     * Returns the average duration of the existing tokens of the supplied type
     *
     * @param tokenType the token type we're looking up
     * @return a positive long which is the mean duration tokens of the supplied type have been stored
     * @throws CoreTokenException if there are problems communicating with the persistence store
     */
    public Long getAverageDuration(TokenType tokenType) throws CoreTokenException {
        long result = 0l;

        if (tokenType == null) {
            throw new NullPointerException("TokenType cannot be null.");
        }

        final Collection<Long> queryResults = delegate.listDurationOfTokens(tokenType);

        if (queryResults.size() > 0) {

            for (Long entry : queryResults) {
                result += entry;
            }

            result /= queryResults.size();
        }

        return result;
    }



}
