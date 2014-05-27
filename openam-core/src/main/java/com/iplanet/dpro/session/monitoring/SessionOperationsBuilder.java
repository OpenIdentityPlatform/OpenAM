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
package com.iplanet.dpro.session.monitoring;

import com.iplanet.dpro.session.operations.SessionOperations;
import com.iplanet.dpro.session.operations.strategies.CTSOperations;
import com.iplanet.dpro.session.operations.strategies.LocalOperations;
import com.iplanet.dpro.session.operations.strategies.RemoteOperations;
import javax.inject.Inject;
import org.forgerock.guice.core.InjectorHolder;

/**
 * Builder to create concrete implementations of the {@link SessionOperations} interface.
 */
public class SessionOperationsBuilder {

    //store is singleton, shared between all monitored operation classes
    private final SessionMonitoringStore store;

    @Inject
    public SessionOperationsBuilder(SessionMonitoringStore store) {
        this.store = store;
    }

    //local

    public SessionOperations createMonitoredLocalOperations() {
        return new MonitoredOperations(createLocalOperations(),
                SessionMonitorType.LOCAL, store);
    }

    public SessionOperations createLocalOperations() {
        return InjectorHolder.getInstance(LocalOperations.class);
    }

    //remote

    public SessionOperations createMonitoredRemoteOperations() {
        return new MonitoredOperations(createRemoteOperations(),
                SessionMonitorType.REMOTE, store);
    }

    public SessionOperations createRemoteOperations() {
        return InjectorHolder.getInstance(RemoteOperations.class);
    }

    //cts

    public SessionOperations createCTSOperations() {
        return InjectorHolder.getInstance(CTSOperations.class);
    }

    public SessionOperations createMonitoredCTSOperations() {
        return new MonitoredOperations(createCTSOperations(),
                SessionMonitorType.CTS, store);
    }

}
