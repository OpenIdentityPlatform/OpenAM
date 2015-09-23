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
/*
 * Portions copyright 2015 ForgeRock AS
 */
package org.forgerock.openam.radius.server;

import java.nio.ByteBuffer;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.PacketFactory;
import org.forgerock.openam.radius.common.PacketType;
import org.forgerock.openam.radius.server.config.RadiusServerConstants;
import org.forgerock.openam.radius.server.spi.AccessRequestHandler;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.ResultHandler;

import com.sun.identity.shared.debug.Debug;

/**
 * Handles valid (ie: from approved clients) incoming radius access-request packets passing responsibility for
 * generating a response to the client's declared handler class.
 */
public class RadiusRequestHandler implements Runnable {

    private static final Debug LOG = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    /**
     * Buffer containing the on-the-wire bytes of the request prior to parsing.
     */
    private final ByteBuffer buffer;

    /**
     * The ResponseContext object providing access to client handlerConfig, receiving channel, and remote user identity.
     */
    private final RadiusRequestContext reqCtx;

    /**
     * Name of the client from which the packet was received.
     */
    private final String clientName;

    /**
     * Name of the access request handler class that is to handle this request.
     */
    private final String accessRequestHandlerClassName;

    /**
     * If an exception occurs while handling this request then an exception will be thrown which will be made available
     * to the calling thread via this field.
     */
    private volatile RadiusProcessingException exception;

    private final ResultHandler<RadiusAuthResult> resultHandler;

    private final ExceptionHandler<RadiusProcessingException> errorHandler;


    /**
     * Constructs a request handler.
     *
     * @param reqCtx
     *            a <code>RadiusRequestContext</code> object. Must be non-null.
     * @param buffer
     *            an {@code ByteBuffer} containing the bytes received by a radius handler.
     */
    public RadiusRequestHandler(final RadiusRequestContext reqCtx, final ByteBuffer buffer,
            final ResultHandler<RadiusAuthResult> resultHandler,
            ExceptionHandler<RadiusProcessingException> errorHandler) {
        this.reqCtx = reqCtx;
        this.clientName = reqCtx.getClientConfig().getName();
        this.accessRequestHandlerClassName = reqCtx.getClientConfig().getAccessRequestHandlerClass().getName();
        this.buffer = buffer;
        this.resultHandler = resultHandler;
        this.errorHandler = errorHandler;
    }

    /**
     * Returns the name of the client from which the packet was received.
     *
     * @return the name of the client from which the packet was received.
     */
    public String getClientName() {
        return clientName;
    }

    private String getAccessRequestHandlerClassName() {
        return this.accessRequestHandlerClassName;
    }

    @Override
    public void run() {
        try {

            final Packet requestPacket = getValidPacket(buffer);
            if (requestPacket == null) {
                return;
            }

            // grab the items from the request that we'll need in the RadiusResponseHandler at send time
            reqCtx.setRequestId(requestPacket.getIdentifier());
            reqCtx.setRequestAuthenticator(requestPacket.getAuthenticator());

            final AccessRequest accessRequest = createAccessRequest(requestPacket);
            if (accessRequest == null) {
                LOG.message("Packet received was not an AccessRequest packet.");
                return;
            }

            // Instantiate an instance of the AccessRequestHandler class specified in the configuration for this
            // client.
            final AccessRequestHandler accessRequestHandler = getAccessRequestHandler(reqCtx);
            if (accessRequestHandler == null) {
                return;
            }

            // Lets create the RadiusResponseHandler
            final RadiusResponseHandler receiver = new RadiusResponseHandler(reqCtx);
            try {
                final RadiusAuthResult result = accessRequestHandler.handle(accessRequest, receiver);
                result.setFinalPacketType(receiver.getFinalPacketTypeSent());
                resultHandler.handleResult(result);
            } catch (final RadiusProcessingException rre) {
                // So the processing of the request failed. Is the error recoverable or does the RADIUS server
                // need to shutdown?
                handleResponseException(rre, reqCtx, accessRequest, receiver);
            }

        } catch (final Exception t) {
            final StringBuilder sb = new StringBuilder("Exception occured in handle() method of handler class '")
                    .append(getAccessRequestHandlerClassName()).append("' for RADIUS client '").append(getClientName())
                    .append("'. Rejecting access.");
            LOG.error(sb.toString(), t);

            this.sendAccessReject(reqCtx);
            return;
        }
    }

    /**
     * Factory that creates and returns and instance of the AccessRequestHandler class defined in the config for a
     * specific radius client. If the class could not be created then a log message is made.
     *
     * @return an instance of an <code>AccessRequestHandler</code> object or null if it could not be created.
     */
    private AccessRequestHandler getAccessRequestHandler(RadiusRequestContext reqCtx) {
        AccessRequestHandler accessRequestHandler = null;
        try {
            final Class accessRequestHandlerClass = reqCtx.getClientConfig().getAccessRequestHandlerClass();
            accessRequestHandler = (AccessRequestHandler) InjectorHolder.getInstance(accessRequestHandlerClass);
        } catch (final Exception e) {
            final StringBuilder sb = new StringBuilder("Unable to instantiate declared handler class '")
                    .append(getAccessRequestHandlerClassName()).append("' for RADIUS client '")
                    .append("'. Rejecting access.");
            LOG.error(sb.toString(), e);
            sendAccessReject(reqCtx);
        }

        try {
            accessRequestHandler.init(reqCtx.getClientConfig().getHandlerConfig());
        } catch (final Exception e) {
            final StringBuilder sb = new StringBuilder("Unable to initialize declared handler class '")
                    .append(getAccessRequestHandlerClassName()).append("' for RADIUS client '")
                    .append("'. Rejecting access.");
            LOG.error(sb.toString(), e);
            accessRequestHandler = null;
            sendAccessReject(reqCtx);
        }

        return accessRequestHandler;
    }

    /**
     * Cast the request packet into an access request packet. If this is not possible a log entry is made and null is
     * returned.
     *
     * @param requestPacket
     *            - the request packet received from the client.
     * @return the <code>AccessRequest</code> object, or null if one could not be derived from requestPacket.
     */
    private AccessRequest createAccessRequest(Packet requestPacket) {
        AccessRequest accessRequest = null;
        try {
            accessRequest = (AccessRequest) requestPacket;
        } catch (final ClassCastException c) {
            // should never happen
            final StringBuilder sb = new StringBuilder("Received packet of type ACCESS_REQUEST from RADIUS client '")
                    .append(getClientName()).append("' but unable to cast to AccessRequest. Rejecting access.");
            LOG.error(sb.toString(), c);
            try {
                reqCtx.send(new AccessReject());
            } catch (final RadiusProcessingException e) {
                LOG.warning("Failed to send AccessReject() response to client.");
            }
        }
        return accessRequest;
    }

    /**
     * Returns a <code>Packet</code> object that represents the incoming radius request.
     *
     * @param buffer2
     *            - buffer containing the bytes to create the packet from.
     * @return the radius request packet, or null if the packet could not be created.
     */
    private Packet getValidPacket(ByteBuffer buffer2) {
        // parse into a packet object
        Packet requestPacket = null;

        try {
            requestPacket = PacketFactory.toPacket(buffer2);

            // log packet if client handlerConfig indicates
            if (reqCtx.getClientConfig().isLogPackets()) {
                reqCtx.logPacketContent(requestPacket, "\nPacket from " + getClientName() + ":");
            }

            // verify packet type
            if (requestPacket.getType() != PacketType.ACCESS_REQUEST) {
                LOG.error("Received non Access-Request packet from RADIUS client '" + getClientName() + "'. Dropping.");
            }
        } catch (final Exception e) {
            LOG.error("Unable to parse packet received from RADIUS client '" + getClientName() + "'. Dropping.", e);
        }
        return requestPacket;
    }

    /**
     * When a <code>RadiusProcessingException</code> is thrown during the processing of a radius request the volatile
     *
     * @param accessRequest
     * @param receiver
     * @param rre
     */
    private void handleResponseException(RadiusProcessingException rre, RadiusRequestContext reqCtx,
            AccessRequest accessRequest, RadiusResponseHandler receiver) {
        final StringBuilder sb = new StringBuilder("Failed to process a radius request using Access Handler '")
                .append(getAccessRequestHandlerClassName()).append("' for RADIUS client '").append("'");
        switch (rre.getNature()) {
        case TEMPORARY_FAILURE:
            sendAccessReject(reqCtx);
        case CATASTROPHIC:
        case INVALID_RESPONSE:
            setException(rre);
            break;
        default:
            LOG.warning("Unrecognised RadiusResponseException nature.");
            break;
        }
        // Propagate the exception back to the Request Listener.
        errorHandler.handleException(rre);
    }

    /**
     * Attempts to send an AccessReject message to the client. Failed attempts will be logged.
     *
     * @param reqCtx
     *            - the RadiusRequestContext that will be used to send the AccessReject packet.
     */
    private void sendAccessReject(RadiusRequestContext reqCtx) {
        try {
            reqCtx.send(new AccessReject());
            LOG.message("Rejected access request.");
        } catch (final RadiusProcessingException e1) {
            LOG.warning("Failed to send AccessReject() response to client.");
        }
    }

    /**
     * @return the exception
     */
    public synchronized RadiusProcessingException getException() {
        return exception;
    }

    /**
     * @param exception
     *            the exception to set
     */
    private synchronized void setException(RadiusProcessingException exception) {
        this.exception = exception;
    }
}
