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
package org.forgerock.openam.radius.server;

import org.forgerock.openam.radius.common.PacketType;

/**
 * This class returns the result of processing a radius request so that it may be logged.
 */
public class RadiusAuthResult {

    private final RadiusAuthResultStatus authStatus;
    private volatile PacketType finalPacketType = null;

    /**
     * Constructor
     *
     * @param authStatus
     */
    public RadiusAuthResult(RadiusAuthResultStatus authStatus) {
        this.authStatus = authStatus;
    }

    /**
     * @return
     */
    public RadiusAuthResultStatus getRequestResult() {
        return authStatus;
    }

    /**
     * @param finalPacketTypeSent
     */
    public void setFinalPacketType(PacketType finalPacketTypeSent) {
        this.finalPacketType = finalPacketTypeSent;
    }

    /**
     * Get the final packet type that was sent in response to the auth request.
     *
     * @return a <code>PacketType</code> enum denoting the type of packet last sent in response to the auth request.
     */
    public PacketType getFinalPacketType() {
        return this.finalPacketType;
    }
}
