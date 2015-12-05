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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.forgerock.openam.radius.common.AccessAccept;
import org.forgerock.openam.radius.common.AccessChallenge;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.Attribute;
import org.forgerock.openam.radius.common.AttributeSet;
import org.forgerock.openam.radius.common.Authenticator;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.ResponseAuthenticator;
import org.forgerock.openam.radius.server.config.ClientConfig;
import org.forgerock.openam.radius.server.config.RadiusServerConstants;

import com.sun.identity.shared.debug.Debug;

/**
 * Holds context information about a received radius request being processed and provides the means for a client's
 * handler to send a response.
 */
public class RadiusRequestContext {

    private static final Debug LOG = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    /**
     * The configuration object delineating the client that sent the request.
     */
    private final ClientConfig clientConfig;

    /**
     * The channel through which the request was received.
     */
    private final DatagramChannel channel;

    /**
     * The originating host and port of the request.
     */
    private final InetSocketAddress source;

    /**
     * After the call to a client's declared AccessRequestHandler class this variable will indicate if the handler
     * called RadiusResponseHandler's send method or not.
     */
    private volatile boolean sendWasCalled;

    /**
     * The authenticator from the request for use in producing the response's authenticator field at send time.
     */
    private Authenticator requestAuthenticator;

    /**
     * The packet id from the request for embedding in the response.
     */
    private short requestId;

    /**
     * Constructs the reponse handler.
     *
     * @param clientConfig
     *            the configuration of the registered client
     * @param channel
     *            the datagram channel object for the received request
     * @param source
     *            the source address of the UDP packet
     */
    public RadiusRequestContext(ClientConfig clientConfig, DatagramChannel channel, InetSocketAddress source) {
        this.channel = channel;
        this.source = source;
        this.clientConfig = clientConfig;
    }

    /**
     * Log packet's attributes in raw hex and read-able chars (where possible).
     *
     * @param pkt
     *            the packet to be logged
     * @param preamble
     *            text that should be written to log on the line preceding the packet's contents
     */
    public void logPacketContent(Packet pkt, String preamble) {
        LOG.warning(preamble + "\n" + getPacketRepresentation(pkt));
    }

    /**
     * Formats a textual representation of the contents of a packet.
     *
     * @param pkt
     *            the packet whose content is to be logged in human read-able form.
     * @return The packet's contents in human read-able form.
     */
    public static String getPacketRepresentation(Packet pkt) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        String packetType = null;
        final Class clazz = pkt.getClass();
        if (clazz == AccessRequest.class) {
            packetType = "ACCESS_REQUEST";
        } else if (clazz == AccessReject.class) {
            packetType = "ACCESS_REJECT";
        } else if (clazz == AccessAccept.class) {
            packetType = "ACCESS_ACCEPT";
        } else if (clazz == AccessChallenge.class) {
            packetType = "ACCESS_CHALLENGE";
        } else {
            packetType = pkt.getClass().getSimpleName();
        }
        pw.println("  " + packetType + " [" + pkt.getIdentifier() + "]");
        final AttributeSet atts = pkt.getAttributeSet();
        for (int i = 0; i < atts.size(); i++) {
            final Attribute a = atts.getAttributeAt(i);
            pw.println("    - " + a);
        }
        pw.flush();
        return sw.toString();
    }

    /**
     * Takes the passed-in packet, injects the ID of the request and a response authenticator and sends it to the source
     * of the request.
     *
     * @param response The packet to be sent to the client.
     * @throws RadiusProcessingException - if the request can not be sent due to network issues etc.
     */
    public void send(Packet response) throws RadiusProcessingException {
        if (sendWasCalled) {
            LOG.warning("Handler class '" + clientConfig.getAccessRequestHandlerClass().getSimpleName()
                    + "' declared for client "
                    + clientConfig.getName() + " called send more than once.");
            return;
        }
        sendWasCalled = true;

        if (response == null) {
            LOG.error("Handler class '" + clientConfig.getAccessRequestHandlerClass().getSimpleName()
                    + "' declared for client "
                    + clientConfig.getName() + " attempted to send a null response. Rejecting access.");
            send(new AccessReject());
            return;
        }

        // inject the id and authenticator
        response.setIdentifier(requestId);
        injectResponseAuthenticator(response);

        if (clientConfig.isLogPackets()) {
            logPacketContent(response, "\nPacket to " + clientConfig.getName() + ":");
        }
        final ByteBuffer reqBuf = ByteBuffer.wrap(response.getOctets());

        try {
            LOG.message("Sending response of type " + response.getType() + " to " + clientConfig.getName());
            channel.send(reqBuf, source);
        } catch (final IOException e) {
            LOG.error("Unable to send response to " + clientConfig.getName() + ".", e);
        }
    }

    /**
     * Crafts the response authenticator as per the Response Authenticator paragraph of section 3 of rfc 2865 and
     * injects into the response packet thus defining the authenticity and integrity of this response relative to its
     * originating request.
     *
     * @param response
     *            the response packet to be validated
     * @throws RadiusProcessingException
     *             if the response authentication can not be added to the response.
     */
    private void injectResponseAuthenticator(Packet response) throws RadiusProcessingException {
        response.setAuthenticator(requestAuthenticator);
        final byte[] onTheWireFormat = response.getOctets();

        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            md5.update(onTheWireFormat);
            md5.update(clientConfig.getSecret().getBytes("UTF-8"));
            final byte[] hash = md5.digest();

            final ResponseAuthenticator ra = new ResponseAuthenticator(hash);

            // now replace the req authenticator fields used for generating the response authenticator fields
            response.setAuthenticator(ra);
        } catch (final NoSuchAlgorithmException | UnsupportedEncodingException e) {
            final IOException ioException = new IOException(
                    "Failed to add response authentication to RADIUS response.", e);
            throw new RadiusProcessingException(RadiusProcessingExceptionNature.CATASTROPHIC,
                    "Failed to send Radius Response.", ioException);
        }
    }

    /**
     * Indicates if a response has already been sent for the request represented by this context object.
     *
     * @return true if a request was already sent, false otherwise.
     */
    public boolean isSendWasCalled() {
        return this.sendWasCalled;
    }

    /**
     * Marks this context as having already sent a response to its associated request object or not based upon the
     * passed-in boolean value.
     *
     * @param sendWasCalled
     *            the value to be set.
     */
    public void setSendWasCalled(boolean sendWasCalled) {
        this.sendWasCalled = sendWasCalled;
    }

    /**
     * Returns the authenticator for the request associated with this context object.
     *
     * @return the requestAuthenticator
     */
    public Authenticator getRequestAuthenticator() {
        return this.requestAuthenticator;
    }

    /**
     * Sets the authenticator for the request associated with this context so that we can use it when crafting the
     * authenticator of the response.
     *
     * @param requestAuthenticator
     *            the requestAuthenticator to set
     */
    public void setRequestAuthenticator(Authenticator requestAuthenticator) {
        this.requestAuthenticator = requestAuthenticator;
    }

    /**
     * Returns the id of the request associated with this context.
     *
     * @return the request identifier
     */
    public String getRequestId() {
        return Short.toString(this.requestId);
    }

    /**
     * Sets the identifier of the request associated with this context.
     *
     * @param requestId
     *            the requestId to set
     */
    public void setRequestId(short requestId) {
        this.requestId = requestId;
    }

    /**
     * Returns the configuration of the pre-registered client associated with the IP address of the incoming request UDP
     * packet.
     *
     * @return the clientConfig
     */
    public ClientConfig getClientConfig() {
        return this.clientConfig;
    }

    /**
     * Returns the datagram channel associated with the UDP request of this context.
     *
     * @return the channel
     */
    public DatagramChannel getChannel() {
        return this.channel;
    }

    /**
     * Returns the ip address of the UDP datagram of the request associated with this context.
     *
     * @return the source
     */
    public InetSocketAddress getSource() {
        return this.source;
    }

    /**
     * Get the name of the client from which the request was made.
     *
     * @return the name of the client from which this request was made.
     */
    public String getClientName() {
        return getClientConfig().getName();
    }

    /**
     * Get the client secret.
     *
     * @return the client secret for the client from which the request was made.
     */
    public String getClientSecret() {
        return getClientConfig().getSecret();
    }

}
