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

package org.forgerock.openam.radius.server.monitoring;

/**
 * An interface to expose the state of the RADIUS server to a JMX client such as visualVM or JConsole.
 */
public interface RadiusServerEventMonitorMXBean {
    /**
     * Get a <code>long</code> count of all of the RADIUS packets received on the UDP channel.
     *
     * @return a <code>long</code> count of all of the RADIUS packets received on the UDP channel.
     */
    long getNumberOfPacketsRecieved();

    /**
     * get the number of packets accepted by the radius server. These are packets that were understood and hence not
     * thrown away or ignored by the RADIUS server.
     *
     * @return a <code>long</code> indicating the no. of packets understood and accepted.
     */
    long getNumberOfAcceptedPackets();

    /**
     * get the number of packets processed by the radius server (since started). These packets may have led to auth or
     * been denied auth, but they were processed to completion, and hence not dropped, or failed in some way.
     *
     * @return a <code>long</code> indicating the number of packets processed.
     */
    long getNumberOfPacketsProcessed();

    /**
     * get the number of auth requests (AccessRequest packets) that resulted in AccessAccept packets being returned in
     * response to the request.
     *
     * @return a <code>long</code> indicating the number of requests that resulted in acceptance.
     */
    long getNumberOfAuthRequestsAccepted();

    /**
     * get the number of auth requests (AccessRequest packets) that resulted in AccessReject packets being returned in
     * response to the request.
     *
     * @return a <code>long</code> indicating the number of requests that resulted in rejection.
     */
    long getNumberOfAuthRequestsRejected();
}
