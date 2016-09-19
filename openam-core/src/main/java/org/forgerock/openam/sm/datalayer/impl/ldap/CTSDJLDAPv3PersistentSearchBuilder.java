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
package org.forgerock.openam.sm.datalayer.impl.ldap;

import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.util.Reject;

/**
 * Constructs new {@link CTSDJLDAPv3PersistentSearch} objects using the provided criteria.
 */
public class CTSDJLDAPv3PersistentSearchBuilder {

    private final ConnectionFactory connectionFactory;

    private DN searchBaseDN;
    private Filter searchFilter;
    private SearchScope searchScope;
    private int retry;
    private String[] attributeNames;

    /**
     * Generates a new builder.
     *
     * @param connectionFactory The {@link ConnectionFactory<org.forgerock.opendj.ldap.Connection>}
     *                          to be passed into generated persistent searches.
     */
    public CTSDJLDAPv3PersistentSearchBuilder(ConnectionFactory connectionFactory) {
        Reject.ifNull(connectionFactory);
        this.connectionFactory = connectionFactory;
    }

    /**
     * Set the search base DN.
     *
     * @param searchBaseDN The {@link DN} to use as the search base for this persistent search.
     * @return this builder.
     */
    public CTSDJLDAPv3PersistentSearchBuilder withSearchBaseDN(DN searchBaseDN) {
        Reject.ifNull(searchBaseDN);
        this.searchBaseDN = searchBaseDN;
        return this;
    }

    /**
     * Set the search filter.
     *
     * @param searchFilter The {@link Filter} to use as the search criteria for this persistent search.
     * @return this builder.
     */
    public CTSDJLDAPv3PersistentSearchBuilder withSearchFilter(Filter searchFilter) {
        Reject.ifNull(searchFilter);
        this.searchFilter = searchFilter;
        return this;
    }

    /**
     * Set the search scope.
     *
     * @param searchScope The {@link SearchScope} to use for this persistent search.
     * @return this builder.
     */
    public CTSDJLDAPv3PersistentSearchBuilder withSearchScope(SearchScope searchScope) {
        Reject.ifNull(searchScope);
        this.searchScope = searchScope;
        return this;
    }

    /**
     * Set how long to wait before attempting to reconnect (in milliseconds) if the connection goes down.
     *
     * @param retry Number of ms to wait before attempting a reconnect. Must be greater than 0.
     * @return this builder.
     */
    public CTSDJLDAPv3PersistentSearchBuilder withRetry(int retry) {
        Reject.ifTrue(retry <= 0);
        this.retry = retry;
        return this;
    }

    /**
     * Set the attribute names to return, if any specific attributes are required to be pulled back from
     * this query in their result entries.
     *
     * @param attributeNames Array of attribute names to return.
     * @return this builder.
     */
    public CTSDJLDAPv3PersistentSearchBuilder returnAttributes(String... attributeNames) {
        this.attributeNames = attributeNames;
        return this;
    }

    /**
     * Constructs a new {@link CTSDJLDAPv3PersistentSearch} from the provided criteria.
     *
     * @return the constructed persistent search object.
     */
    public CTSDJLDAPv3PersistentSearch build() {

        String message = null;

        if (retry < 1) {
            message = "invalid timeout.";
        } else if (searchBaseDN == null) {
           message = "invalid search DN.";
        } else if (searchFilter == null) {
            message = "invalid search filter.";
        } else if (searchScope == null) {
            message = "invalid search scope";
        }

        if (message != null) {
            throw new IllegalStateException("Unable to construct CTSDJLDAPv3PersistentSearch object, " + message);
        }

        return new CTSDJLDAPv3PersistentSearch(retry, searchBaseDN, searchFilter, searchScope, connectionFactory,
                attributeNames);
    }

}
