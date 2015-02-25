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

import org.forgerock.guice.core.InjectorHolder;

/**
 * Abstracts away the creation of the {@link SessionMonitoringTimingStore} to reduce the need for Guice
 * InjectorHolder to be referenced in the {@link SessionMonitoringStore}.
 */
public class SessionMonitoringTimingStoreFactory {

    /**
     * Simple creation of a SessionMonitoringTimingStore, using Guice.
     *
     * @return a new instance of a SessionMonitoringTimingStore
     */
    public SessionMonitoringTimingStore createSessionMonitoringTimingStore() {
        return InjectorHolder.getInstance(SessionMonitoringTimingStore.class);
    }

}
