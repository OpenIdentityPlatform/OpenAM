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
package org.forgerock.openam.cts.monitoring.impl.connections;

import javax.inject.Inject;
import org.forgerock.openam.cts.monitoring.CTSConnectionMonitoringStore;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.ResultHandler;

/**
 * Factory class for throwing out wrapped ResultHandlers for the
 * purposes of Asynchronous connection gets.
 */
public class WrappedHandlerFactory {

    private final CTSConnectionMonitoringStore monitorStore;

    @Inject
    public WrappedHandlerFactory(CTSConnectionMonitoringStore monitorStore) {
        this.monitorStore = monitorStore;
    }

    /**
     * Builds a wrapped version of the supplied ResultHandler which ensures that the monitoring
     * framework is notified of successes or failures.
     *
     * @param resultHandler The custom ResultHandler to use
     * @return A wrapped version of the custom ResultHandler
     */
    public ResultHandler<Connection> build(final ResultHandler<? super Connection> resultHandler) {

        return new ResultHandler<Connection>() {
            @Override
            public void handleResult(final Connection result) {
                monitorStore.addConnection(true);
                resultHandler.handleResult(result);
            }

            @Override
            public void handleErrorResult(final ErrorResultException error) {
                monitorStore.addConnection(false);
                resultHandler.handleErrorResult(error);
            }
        };

    }

}
