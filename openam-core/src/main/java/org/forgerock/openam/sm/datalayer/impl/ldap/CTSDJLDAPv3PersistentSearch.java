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

import java.util.HashSet;
import java.util.Set;

import com.iplanet.services.ldap.event.LDAPv3PersistentSearch;

import org.forgerock.openam.cts.continuous.ContinuousQuery;
import org.forgerock.openam.cts.continuous.ContinuousQueryListener;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.controls.PersistentSearchChangeType;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.util.annotations.VisibleForTesting;

/**
 * This class will execute persistent search request against the configured datastore. When a result is received, the
 * listeners will be notified about the changes, so the messages can be propagated. This implementation acts to
 * translate between the CTS's {@link ContinuousQueryListener}s and the OpenDJ LDAP supported implementation of the
 * datastore.
 */
public class CTSDJLDAPv3PersistentSearch extends LDAPv3PersistentSearch<ContinuousQueryListener, Set<Void>>
        implements ContinuousQuery {

    private final SearchResultEntryHandler resultEntryHandler = new PSearchResultEntryHandler();

    /**
     * Generate a new CTSDJLDAPv3PersistentSearch, providing the connection factory to use to produce
     * connections.
     *
     * TODO: Configure the parameters set appropriate to the configuration of the system itself.
     *
     * @param factory Used to produce connections down to the CTS.
     */
    public CTSDJLDAPv3PersistentSearch(DN searchBaseDN, Filter searchFilter,
                                       SearchScope searchScope, ConnectionFactory factory) {
        super(3000, searchBaseDN, searchFilter, searchScope, factory);
    }

    @Override
    protected void clearCaches() {
        for (ContinuousQueryListener listener : getListeners().keySet()) {
            listener.connectionLost();
        }
    }

    @Override
    public void stopQuery() {
        for (ContinuousQueryListener listener : getListeners().keySet()) {
            removeContinuousQueryListener(listener);
        }

        super.stopSearch();
    }

    @Override
    protected SearchResultEntryHandler getSearchResultEntryHandler() {
        return resultEntryHandler;
    }

    @Override
    public void addListener(ContinuousQueryListener listener, Set<Void> idTypes) {
        throw new UnsupportedOperationException("Please use the addContinuousQueryListener method definition.");
    }

    @Override
    public void removeListener(ContinuousQueryListener listener) {
        throw new UnsupportedOperationException("Please use the removeContinuousQueryListener method definition.");
    }

    @Override
    public ContinuousQuery addContinuousQueryListener(ContinuousQueryListener listener) {
        super.addListener(listener, new HashSet<Void>());
        return this;
    }

    @Override
    public ContinuousQuery removeContinuousQueryListener(ContinuousQueryListener listener) {
        super.removeListener(listener);
        return this;
    }

    private final class PSearchResultEntryHandler implements LDAPv3PersistentSearch.SearchResultEntryHandler {

        @Override
        public boolean handle(SearchResultEntry entry, String dn, DN previousDn, PersistentSearchChangeType type) {
            if (type != null) {
                for (ContinuousQueryListener listener : getListeners().keySet()) {
                    listener.objectChanged(entry.getName().toString());
                }
            }
            return true;
        }
    }

    @VisibleForTesting
    protected boolean isShutdown() {
        return super.isShutdown();
    }
}
