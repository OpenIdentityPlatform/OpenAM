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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.monitoring.impl.persistence;

import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.PartialToken;
import org.forgerock.openam.utils.TimeUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

/**
 * Used to query the CTS persistence store and return data to the monitoring service
 */
public class CtsPersistenceOperationsDelegate {

    private final CTSPersistentStore store;

    @Inject
    public CtsPersistenceOperationsDelegate(CTSPersistentStore store) {
        this.store = store;
    }

    /**
     * Counts the number of tokens in the persistent store that match the requested type.
     *
     * @param tokenType The type of token for which we are gathering results
     * @return Zero or positive integer of the number of tokens in the store.
     * @throws CoreTokenException
     */
    public int countTokenEntries(TokenType tokenType) throws CoreTokenException {

        //create the filter to restrict by token type
        final TokenFilter tokenFilter = new TokenFilterBuilder()
                .returnAttribute(CoreTokenField.TOKEN_ID)
                .and()
                .withAttribute(CoreTokenField.TOKEN_TYPE, tokenType)
                .build();
        return store.attributeQuery(tokenFilter).size();
    }

    /**
     * Counts all Tokens within the CTS Persistent store. All tokens are considered regardless of TokenType.
     *
     * @return Zero or positive integer.
     * @throws CoreTokenException if there are issues talking with the CTS
     */
    public int countAllTokens() throws CoreTokenException {
        TokenFilter filter = new TokenFilterBuilder().returnAttribute(CoreTokenField.TOKEN_ID).build();
        return store.attributeQuery(filter).size();
    }

    /**
     * Gathers list of the durations of tokens in epoch'd seconds
     *
     * @param tokenType The type of token for which we are gathering results
     * @return A collection of longs, each of which represents the duration of a token inside the CTS
     * @throws CoreTokenException
     */
    public Collection<Long> listDurationOfTokens(TokenType tokenType) throws CoreTokenException {

        final Collection<Long> results = new ArrayList<Long>();

        final long unixTime = TimeUtils.currentUnixTime();

        final TokenFilter filter = new TokenFilterBuilder()
                .returnAttribute(CoreTokenField.CREATE_TIMESTAMP)
                .and()
                .withAttribute(CoreTokenField.TOKEN_TYPE, tokenType)
                .build();

        for (PartialToken token : store.attributeQuery(filter)) {
            Calendar timestamp = token.getValue(CoreTokenField.CREATE_TIMESTAMP);
            results.add(unixTime - TimeUtils.toUnixTime(timestamp));
        }

        return results;
    }

}
