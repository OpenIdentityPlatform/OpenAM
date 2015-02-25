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

import javax.inject.Inject;
import org.forgerock.openam.shared.monitoring.AbstractTimingStore;

/**
 * An implementation of a timing store specific to the needs of the session monitoring component.
 * Added values are wrapped in a {@link SessionTimingEntry}.
 */
public class SessionMonitoringTimingStore extends AbstractTimingStore {

    @Inject
    public SessionMonitoringTimingStore(SessionMonitoringService monitoringService) {
        this(monitoringService.getSessionWindowSize());
    }

    /**
     * Constructs a SessionMonitoringTimingStore with the provided maximum number of entries in its sample window.
     *
     * @param maxEntries the maximum number of samples to consider when performing oeprations on the store
     */
    public SessionMonitoringTimingStore(int maxEntries) {
        super(maxEntries);
    }

    /**
     * Adds an entry to the timing store, having wrapped the supplied long duration in a
     * {@link SessionTimingEntry}.
     *
     * @param duration the length of time the operation took
     */
    public void addTimingEntry(long duration) {
        durationStore.add(new SessionTimingEntry(duration));
    }

}
