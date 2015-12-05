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
package org.forgerock.openam.radius.server;

import org.forgerock.guava.common.base.Preconditions;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.server.config.RadiusServerConstants;

import com.sun.identity.shared.debug.Debug;

/**
 * Holds the Radius packet to be returned to the requester, along with other information for audit logging/accounting
 * purposes.
 */
public class RadiusResponse {

    private static final Debug LOG = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    private Packet responsePacket;

    /**
     * The realm (if applicable) for which the response was formed.
     */
    private String realm;

    /**
     * The time it too to service the request for which the response is being made.
     */
    private long timeToServiceRequestInMilliSeconds;

    private String universalId;


    /**
     * Constructor.
     */
    public RadiusResponse() {
        LOG.message("Constructing RadiusResponse.RadiusResponse()");
    }

    /**
     * Set the response packet to be sent the the requester.
     *
     * @param responsePacket the packet to be sent to the requester. Must not be null.
     */
    public void setResponsePacket(Packet responsePacket) {
        Preconditions.checkNotNull(responsePacket, "Argument supplied to responsePacket param was null.");
        this.responsePacket = responsePacket;
    }

    /**
     * Get the response packet to be sent to the requester.
     *
     * @return the response packet to be sent to the requester.
     */
    public Packet getResponsePacket() {
        return responsePacket;
    }

    /**
     * Return the realm that the authentication was made against.
     *
     * @return the realm
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Sets the realm against which hte authentication was made.
     *
     * @param realm the realm to set
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

    /**
     * Set the time to service the request for which the response is being made.
     *
     * @param timeToServeRequestInMS time to service the request for which the response is being made.
     */
    public void setTimeToServiceRequestInMilliSeconds(long timeToServeRequestInMS) {
        this.timeToServiceRequestInMilliSeconds = timeToServeRequestInMS;
    }

    /**
     * Set the time to service the request for which the response is being made.
     *
     * @return the time to service the request for which the response is being made.
     */
    public long getTimeToServiceRequestInMilliSeconds() {
        return timeToServiceRequestInMilliSeconds;
    }

    /**
     * Set the Universal ID of the principal of the AuthContext.
     *
     * @param uid the principal of the response.
     */
    public void setUniversalId(String uid) {
        this.universalId = uid;
    }

    /**
     * Get the universal ID of the principal of the AuthContext, or null if none has been set.
     *
     * @return the universal ID of the principal of the AuthContext, or null if none has been set.
     */
    public String getUniversalId() {
        return this.universalId;
    }
}
