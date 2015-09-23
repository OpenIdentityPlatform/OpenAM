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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.server;

import java.io.IOException;

import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.PacketType;
import org.forgerock.openam.radius.common.UserPasswordAttribute;
import org.forgerock.openam.radius.server.config.RadiusServerConstants;

import com.sun.identity.shared.debug.Debug;

/**
 * Provides AccessRequestHandlers the means to send their response to a given request. Created by markboyd on 11/21/14.
 */
public class RadiusResponseHandler {

    private static final Debug logger = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    /**
     * The request context object providing access to the source address of the packet and feedback on the response
     * result.
     */
    private final RadiusRequestContext reqCtx;

    /**
     * The last packet type sent, which will be accept or reject.
     */
    private volatile PacketType finalPacketTypeSent;

    /**
     * Constructs a response handler instance.
     *
     * @param reqCtx
     *            the request context in which the request being handled was made.
     */
    public RadiusResponseHandler(RadiusRequestContext reqCtx) {
        this.reqCtx = reqCtx;
    }

    /**
     * Takes the passed-in packet, injects the ID of the request and a response authenticator and sends it to the source
     * of the request.
     *
     * @param response
     *            the response packet to be sent back to the client.
     * @throws RadiusProcessingException
     *             If the response packet can not be sent.
     */
    public void send(Packet response) throws RadiusProcessingException {
        logger.message("Entering RadiusResponseHandler.send()");
        // delegate to the context object to do the work.
        reqCtx.send(response);
        this.finalPacketTypeSent = response.getType();
        logger.message("Leaving RadiusResposeHandler.send(), finalPacketType is '" + this.finalPacketTypeSent + "'.");
    }

    /**
     * Extracts the password from the provided UserPasswordAttribute object. This is done here since we have the request
     * context holding both the authenticator and secret which are needed to decrypt the attribute's contents.
     *
     * @param credAtt
     *            the attribute containing the user's password, encrypted with the client config secret.
     * @return the password extracted from the userPasswordAttribute.
     * @throws IOException
     *             if the password cannot be decrypted.
     */
    public String extractPassword(UserPasswordAttribute credAtt) throws IOException {
        return credAtt.extractPassword(reqCtx.getRequestAuthenticator(), reqCtx.getClientConfig().getSecret());
    }

    /**
     * Get the final packet type sent, which will tell us if the response to the request was an accept or deny.
     *
     * @return a <code>PacketType</code> enum value denoting the type of packet that was last sent.
     */
    public PacketType getFinalPacketTypeSent() {
        return this.finalPacketTypeSent;
    }
}
