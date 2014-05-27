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

import org.forgerock.openam.shared.monitoring.TimingEntry;

/**
 * An implementation of {@link TimingEntry} for the Session monitoring component.
 *
 * This simple implementation simply stores and then provides the duration again.
 */
public class SessionTimingEntry implements TimingEntry {

    private final long duration;

    /**
     * Constructor, generating a new SessionTimingEntry with the provided duration
     *
     * @param duration length of time the operation took (in ms)
     */
    public SessionTimingEntry(long duration) {
        this.duration = duration;
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public long getDuration() {
        return duration;
    }
}
