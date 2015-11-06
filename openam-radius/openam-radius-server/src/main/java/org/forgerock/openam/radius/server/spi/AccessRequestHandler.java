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
package org.forgerock.openam.radius.server.spi;

import java.util.Properties;

import org.forgerock.openam.radius.server.RadiusProcessingException;
import org.forgerock.openam.radius.server.RadiusRequest;
import org.forgerock.openam.radius.server.RadiusRequestContext;
import org.forgerock.openam.radius.server.RadiusResponse;

/**
 * Defines the interface for handlers of incoming Access-Request packets. The current infrastructure allows for
 * definition of clients in the admin console and declaration on a per-client basis of the handler class to be use for
 * traffic from that client. There are several sample implementations such as the
 * {@link org.forgerock.openam.radius.server.spi.handlers.AcceptAllHandler} which always responds with an
 * {@link com.sun.identity.authentication.modules.radius.client.AccessAccept} packet granting access to everyone and the
 * {@link org.forgerock.openam.radius.server.spi.handlers.RejectAllHandler} that does just the opposite returning an
 * {@link com.sun.identity.authentication.modules.radius.client.AccessReject} packet allowing no access for anyone. The
 * {@link org.forgerock.openam.radius.server.spi.handlers.OpenAMAuthHandler} uses an OpenAM realm and chain to allow
 * users to authenticate via authentication modules that are cognizant of non-http clients. Created by markboyd on
 * 11/21/14.
 */
public interface AccessRequestHandler {

    /**
     * Passes to the handler configuration parameters declared in OpenAM's admin console for the client's declared
     * handler class.
     *
     * @param config
     *            - the handler configuration parameters.
     */
    public void init(Properties config);

    /**
     * Determines how the passed-in request should be handled and calls the send method in the context passing the
     * appropriate response packet of type AccessAccept, AccessReject, or AccessChallenge. Only one call to the
     * context's send method is accepted. Any following calls will be ignored. Once this method returns, the context
     * object is disabled so that any subsequent calls by a launched thread for example will be ignored. If this method
     * returns without invoking the handler's send method then the request packet is essentially dropped silently.
     *
     * @param request
     *            the access request
     * @param response
     *            - the response to be sent to the client.
     * @param context
     *            - provides methods that the handler can use to obtain information about the context in which the
     *            request was made, for example the name and IP address of the client from which the request was
     *            received.
     * @throws RadiusProcessingException
     *             when a response to the request can not be sent.
     */
    public void handle(RadiusRequest request, RadiusResponse response, RadiusRequestContext context)
            throws RadiusProcessingException;
}
