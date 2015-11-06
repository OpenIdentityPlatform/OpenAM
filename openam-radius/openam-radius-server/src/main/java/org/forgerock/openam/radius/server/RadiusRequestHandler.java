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

import org.forgerock.guava.common.eventbus.EventBus;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.PacketFactory;
import org.forgerock.openam.radius.common.PacketType;
import org.forgerock.openam.radius.server.config.RadiusServerConstants;
import org.forgerock.openam.radius.server.events.AuthRequestAcceptedEvent;
import org.forgerock.openam.radius.server.events.AuthRequestChallengedEvent;
import org.forgerock.openam.radius.server.events.AuthRequestRejectedEvent;
import org.forgerock.openam.radius.server.spi.AccessRequestHandler;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.ResultHandler;
import org.joda.time.DateTime;

import com.sun.identity.shared.debug.Debug;

/**
 * Handles valid (ie: from approved clients) incoming radius access-request packets passing responsibility for
 * generating a response to the client's declared handler class. The handler results are returned to the request thread
 * via a promise. This allows a catastrophic failure in one off the handler threads to affect a shutdown of the listener
 * thread and the executors. It also allows for retrying of requests that fail for temporary reasons (e.g. network
 * connection issues) although this is not yet implemented.
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
    private final RadiusRequestContext requestContext;

    /**
     * If an exception occurs while handling this request then an exception will be thrown which will be made available
     * to the calling thread via this field.
     */
    private volatile RadiusProcessingException exception;

    /**
     * The success handler of the promise that may be used to pass results back to the RadiusRequestListener thread.
     */
    private final ResultHandler<RadiusResponse> resultHandler;

    /**
     * The exception handler of the promise that is used to pass exceptions back to the RadiusRequestListener thread.
     */
    private final ExceptionHandler<RadiusProcessingException> errorHandler;

    /**
     * The event bus is used by handlers and by this class to notify listeners of events occurring with the lifetime of
     * a radius request.
     */
    private final EventBus eventBus;

    /**
     * factory that will attempt to construct the access request handler class.
     */
    private AccessRequestHandlerFactory accessRequestHandlerFactory;

    /**
     * Constructs a request handler.
     *
     * @param accessRequestHandlerFactory - a factory object that will construct access request handlers used to handle
     *            the radius requests.
     * @param reqCtx a <code>RadiusRequestContext</code> object. Must be non-null.
     * @param buffer an {@code ByteBuffer} containing the bytes received by a radius handler.
     * @param resultHandler - a promise handler that this class can use to notify calling threads of the results of
     *            processing.
     * @param errorHandler used to notify the calling thread if an exception occurs during processing.
     * @param eventBus used to notify interested parties of events occurring during the processing of radius requests.
     */
    public RadiusRequestHandler(AccessRequestHandlerFactory accessRequestHandlerFactory,
            final RadiusRequestContext reqCtx, final ByteBuffer buffer,
            final ResultHandler<RadiusResponse> resultHandler,
            final ExceptionHandler<RadiusProcessingException> errorHandler,
            final EventBus eventBus) {
        LOG.message("Entering RadiusRequestHandler.RadiusRequestHandler()");
        this.requestContext = reqCtx;
        this.buffer = buffer;
        this.resultHandler = resultHandler;
        this.errorHandler = errorHandler;
        this.eventBus = eventBus;
        this.accessRequestHandlerFactory = accessRequestHandlerFactory;
        LOG.message("Leaving RadiusRequestHandler.RadiusRequestHandler()");
    }

    /**
     * Returns the name of the client from which the packet was received.
     *
     * @return the name of the client from which the packet was received.
     */
    public String getClientName() {
        return requestContext.getClientName();
    }

    @Override
    public void run() {
        try {
            LOG.message("Entering RadiusRequestHandler.run();");
            final Packet requestPacket = getValidPacket(buffer);
            if (requestPacket == null) {
                LOG.message("Leaving RadiusRequestHandler.run(); no requestPacket");
                return;
            }

            // grab the items from the request that we'll need in the RadiusResponseHandler at send time
            requestContext.setRequestId(requestPacket.getIdentifier());
            requestContext.setRequestAuthenticator(requestPacket.getAuthenticator());

            final AccessRequest accessRequest = createAccessRequest(requestPacket);
            if (accessRequest == null) {
                LOG.message("Leaving RadiusRequestHandler.run(); Packet received was not an AccessRequest packet.");
                return;
            }

            // Instantiate an instance of the AccessRequestHandler class specified in the configuration for this
            // client.
            final AccessRequestHandler accessRequestHandler = accessRequestHandlerFactory
                    .getAccessRequestHandler(requestContext);
            if (accessRequestHandler == null) {
                LOG.message("Leaving RadiusRequestHandler.run(); Could not obtain Access Request Handler.");
                return;
            }

            final RadiusRequest request = new RadiusRequest(accessRequest);
            final RadiusResponse response = new RadiusResponse();

            try {
                // The handler will form the response.
                accessRequestHandler.handle(request, response, requestContext);
                postHandledEvent(request, response, requestContext);
                // Send the response to the client.
                Packet responsePacket = response.getResponsePacket();
                requestContext.send(responsePacket);

                resultHandler.handleResult(response);
            } catch (final RadiusProcessingException rre) {
                // So the processing of the request failed. Is the error recoverable or does the RADIUS server
                // need to shutdown?
                handleResponseException(rre, requestContext);
            }

        } catch (final Exception t) {
            final StringBuilder sb = new StringBuilder(
                    "Exception occured while handling radius request for RADIUS client '").append(getClientName())
                    .append("'. Rejecting access.");
            LOG.error(sb.toString(), t);

            this.sendAccessReject(requestContext);
            return;
        }
    }

    private void postHandledEvent(RadiusRequest request, RadiusResponse response, RadiusRequestContext requestContext) {
        LOG.message("Entering RadiusRequestHandler.postHandledEvent()");

        // Calculate and set the time to service the response.
        response.setTimeToServiceRequestInMilliSeconds(
                DateTime.now().getMillis() - request.getStartTimestampInMillis());

        Packet responsePacket = response.getResponsePacket();
        if (responsePacket != null) {
            switch (responsePacket.getType()) {
            case ACCESS_ACCEPT:
                eventBus.post(new AuthRequestAcceptedEvent(request, response, requestContext));
                break;
            case ACCESS_CHALLENGE:
                eventBus.post(new AuthRequestChallengedEvent(request, response, requestContext));
                break;
            case ACCESS_REJECT:
                eventBus.post(new AuthRequestRejectedEvent(request, response, requestContext));
                break;
            case ACCOUNTING_RESPONSE:
                break;
            default:
                LOG.warning("Unexpected type of responsePacket;", responsePacket.getType().toString());
                break;
            }
        }
        LOG.message("Leaving RadiusRequestHandler.postHandledEvent()");
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
                requestContext.send(new AccessReject());
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
        LOG.message("Entering RadiusRequestHandler.getValidPacket()");
        // parse into a packet object
        Packet requestPacket = null;

        try {
            requestPacket = PacketFactory.toPacket(buffer2);

            // log packet if client handlerConfig indicates
            if (requestContext.getClientConfig().isLogPackets()) {
                requestContext.logPacketContent(requestPacket, "\nPacket from " + getClientName() + ":");
            }

            // verify packet type
            if (requestPacket.getType() != PacketType.ACCESS_REQUEST) {
                LOG.error("Received non Access-Request packet from RADIUS client '" + getClientName() + "'. Dropping.");
            }
        } catch (final Exception e) {
            LOG.error("Unable to parse packet received from RADIUS client '" + getClientName() + "'. Dropping.", e);
        }
        LOG.message("Leaving RadiusRequestHandler.getValidPacket()");
        return requestPacket;
    }

    /**
     * Sets the handler's exception. If the exception is only temporary we can have a go at sending an access reject
     * response, if the exception is only a temporary failure then this method will try to send
     *
     * @param rre
     */
    private void handleResponseException(RadiusProcessingException rre, RadiusRequestContext reqCtx) {
        final StringBuilder sb = new StringBuilder("Failed to process a radius request for RADIUS client '");
        LOG.error(sb.toString());
        if (rre.getNature() == RadiusProcessingExceptionNature.TEMPORARY_FAILURE) {
            sendAccessReject(reqCtx);
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
}
