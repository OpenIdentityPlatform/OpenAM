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
/**
 *
 */
package org.forgerock.openam.radius.server.monitoring;

/**
 * Interface through which implementations that monitor the state of the Radius Server can update the state by notifying
 * the implementation of events.
 */
public interface RadiusServerEventRegistrator {

    /**
     * Report to the Event Registrar that a packet has been received.
     *
     * @return the total number of packets received (including the one being reported).
     */
    long packetReceived();

    /**
     * Notify the Event Registrar that a packet has been understood and hence 'accepted'.
     *
     * @return the total number of packets accepted (including the one being reported).
     */
    long packetAccepted();

    /**
     * Notify the event Registrar that a packet has been processed. The authentication may have succeeded, or failed,
     * but it has been processed to completion.
     *
     * @return the total number of packets that have been processed.
     */
    long packetProcessed();

    /**
     * Notify the event Registrar that an auth request has been accepted. That is an AccessAccept packet has been sent
     * to the client in response to the AccessRequest.
     *
     * @return the total number of authentication requests that have been accepted.
     */
    long authRequestAccepted();

    /**
     * Notify the event Registrar that an auth request has been rejected. That is, an AccessReject packet has been sent
     * to the client in response to the AccessRequest.
     *
     * @return the total number of authentication requests that have been rejected.
     */
    long authRequestRejected();
}
