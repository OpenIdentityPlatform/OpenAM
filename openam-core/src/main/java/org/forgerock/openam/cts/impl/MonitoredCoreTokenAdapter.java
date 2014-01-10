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

package org.forgerock.openam.cts.impl;

import com.google.inject.name.Named;
import com.sun.identity.common.configuration.ConfigurationObserver;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.CTSOperation;
import org.forgerock.openam.cts.CoreTokenConfigListener;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.CreateFailedException;
import org.forgerock.openam.cts.exceptions.DeleteFailedException;
import org.forgerock.openam.cts.impl.query.QueryFactory;
import org.forgerock.openam.cts.monitoring.CTSOperationsMonitoringStore;
import org.forgerock.opendj.ldap.ConnectionFactory;

import javax.inject.Inject;

/**
 * Decorator sub-class for the {@link CoreTokenAdapter} that adds the ability to monitor CTS operation success/failure
 * rates via the OpenAM monitoring framework.
 *
 * @since 12.0.0
 * @author neil.madden@forgerock.com
 */
public class MonitoredCoreTokenAdapter extends CoreTokenAdapter {
    private final CTSOperationsMonitoringStore monitoringStore;

    /**
     * Create a new instance of the CoreTokenAdapter with dependencies.
     *
     * @param connectionFactory Required for connections to LDAP.
     * @param queryFactory      Required for query instances.
     * @param ldapAdapter       Required for all LDAP operations.
     */
    @Inject
    public MonitoredCoreTokenAdapter(ConnectionFactory connectionFactory, QueryFactory queryFactory,
                                     LDAPAdapter ldapAdapter, ConfigurationObserver observer,
                                     CoreTokenConfigListener listener, @Named(CoreTokenConstants.CTS_DEBUG) Debug debug,
                                     CTSOperationsMonitoringStore monitoringStore) {
        super(connectionFactory, queryFactory, ldapAdapter, observer, listener, debug);

        this.monitoringStore = monitoringStore;
    }

    @Override
    public void create(Token token) throws CoreTokenException {
        boolean success = false;
        try {
            super.create(token);
            success = true;
        } finally {
            monitoringStore.addTokenOperation(token, CTSOperation.CREATE, success);
        }
    }

    @Override
    public Token read(String tokenId) throws CoreTokenException {
        boolean success = false;
        Token result = null;
        try {
            result = super.read(tokenId);
            success = true;
            return result;
        } finally {
            monitoringStore.addTokenOperation(result, CTSOperation.READ, success);
        }
    }

    @Override
    public boolean updateOrCreate(Token token) throws CoreTokenException {
        boolean success = false;
        boolean created = false;
        try {
            created = super.updateOrCreate(token);
            success = true;
            return created;
        } catch (CreateFailedException ex) {
            created = true;
            throw ex;
        } finally {
            // Assume that this is an update operation unless we know that a create was attempted
            monitoringStore.addTokenOperation(token, created ? CTSOperation.CREATE : CTSOperation.UPDATE, success);
        }
    }

    @Override
    public void delete(String tokenId) throws DeleteFailedException {
        boolean success = false;
        try {
            super.delete(tokenId);
            success = true;
        } finally {
            monitoringStore.addTokenOperation(null, CTSOperation.DELETE, success);
        }
    }
}
