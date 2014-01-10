/*
 * Copyright 2014 ForgeRock, AS.
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

package org.forgerock.openam.cts.impl.query;

import org.forgerock.openam.cts.CTSOperation;
import org.forgerock.openam.cts.exceptions.QueryFailedException;
import org.forgerock.openam.cts.impl.LDAPConfig;
import org.forgerock.openam.cts.monitoring.CTSOperationsMonitoringStore;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;

import javax.inject.Inject;
import java.util.Collection;

/**
 * Decorator for {@link LDAPSearchHandler} that adds hooks to the CTS monitoring store to track query statistics.
 *
 * @since 12.0.0
 * @author neil.madden@forgerock.com
 */
public class MonitoredLDAPSearchHandler extends LDAPSearchHandler {
    private final CTSOperationsMonitoringStore monitoringStore;

    @Inject
    public MonitoredLDAPSearchHandler(ConnectionFactory connectionFactory, LDAPConfig constants,
                                      CTSOperationsMonitoringStore monitoringStore) {
        super(connectionFactory, constants);
        this.monitoringStore = monitoringStore;
    }

    @Override
    public Result performSearch(SearchRequest request, Collection<Entry> entries) throws QueryFailedException {
        boolean success = false;
        try {
            final Result result = super.performSearch(request, entries);
            success = true;
            return result;
        } finally {
            monitoringStore.addTokenOperation(null, CTSOperation.LIST, success);
        }
    }
}