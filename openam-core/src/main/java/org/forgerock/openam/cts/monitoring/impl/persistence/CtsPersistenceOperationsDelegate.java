package org.forgerock.openam.cts.monitoring.impl.persistence;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import javax.inject.Inject;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.QueryFactory;
import org.forgerock.openam.cts.utils.LDAPDataConversion;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.Filter;

/**
 * Used to query the CTS persistence store and return data to the monitoring service
 */
public class CtsPersistenceOperationsDelegate {

    private final LDAPDataConversion dataConversion;
    private final QueryFactory factory;

    @Inject
    public CtsPersistenceOperationsDelegate(LDAPDataConversion dataConversion, QueryFactory factory) {
        this.dataConversion = dataConversion;
        this.factory = factory;
    }

    /**
     * Gathers (just the TOKEN_IDs) from the persistence store the current tokens
     *
     * @param tokenType The type of token for which we are gathering results
     * @return
     * @throws CoreTokenException
     */
    public Collection<Entry> getTokenEntries(TokenType tokenType) throws CoreTokenException {

        //create the filter to restrict by token type
        final Filter filter = factory.createFilter().and().attribute(CoreTokenField.TOKEN_TYPE, tokenType).build();

        return factory.createInstance()
                .returnTheseAttributes(CoreTokenField.TOKEN_ID)
                .withFilter(filter)
                .executeRawResults();
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

        final long currentEpochTime = dataConversion.currentEpochedSeconds();

        final Filter filter = factory.createFilter().and().attribute(CoreTokenField.TOKEN_TYPE, tokenType).build();

        final Collection<Entry> queryResults = factory.createInstance()
                .returnTheseAttributes(CoreTokenField.CREATE_TIMESTAMP)
                .withFilter(filter)
                .executeRawResults();

        for (Entry entry : queryResults) {
            String dateString = entry.getAttribute(CoreTokenField.CREATE_TIMESTAMP.toString()).firstValueAsString();
            Calendar timestamp = dataConversion.fromLDAPDate(dateString);
            results.add(currentEpochTime - dataConversion.toEpochedSeconds(timestamp));
        }

        return results;
    }

}
