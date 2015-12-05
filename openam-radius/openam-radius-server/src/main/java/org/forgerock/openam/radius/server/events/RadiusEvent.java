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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.radius.server.events;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Base class for events submitted to the RadiusServer's event bus.
 */
public class RadiusEvent {

    /**
     * The time at which the event occurred.
     */
    private final DateTime timeOfEvent;

    /**
     * Constructor.
     */
    public RadiusEvent() {
        timeOfEvent = DateTime.now();
    }

    /**
     * Get the number of milliseconds after the Java epoch at which the event occurred.
     *
     * @return the number of milliseconds after the Java epoch at which the event occurred.
     */
    public long getTimeOfEvent() {
        return timeOfEvent.getMillis();
    }

    /**
     * Get the time at which the event occurred in ISO format.
     *
     * @return a String containing an ISO date format representation of the time at which the event occurred.
     */
    public String getTimeOfEventAsString() {
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        return fmt.print(timeOfEvent);
    }
}