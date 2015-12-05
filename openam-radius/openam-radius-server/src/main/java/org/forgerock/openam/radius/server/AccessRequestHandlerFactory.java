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

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.radius.server.config.RadiusServerConstants;
import org.forgerock.openam.radius.server.spi.AccessRequestHandler;

import com.sun.identity.shared.debug.Debug;

/**
 * Responsible for constructing the class specified in the client configuration that will be used to handle
 * Access-Request RADIUS messages.
 */
public class AccessRequestHandlerFactory {

    /**
     * Class allowing debug log entries to be made.
     */
    private static final Debug LOG = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    /**
     * Constructor.
     */
    public AccessRequestHandlerFactory() {
        LOG.message("Constructing AccessRequestHandlerFactory.AccessRequestHandlerFactory()");
    }

    /**
     * Factory that creates and returns and instance of the AccessRequestHandler class defined in the config for a
     * specific radius client. If the class could not be created then a log message is made.
     *
     * @param reqCtx - the request context that holds information about the configuration of the RADIUS server at the
     *            point at which the request was received.
     * @return an instance of an <code>AccessRequestHandler</code> object or null if it could not be created.
     */
    public AccessRequestHandler getAccessRequestHandler(RadiusRequestContext reqCtx) {
        LOG.message("Entering RadiusRequestHandler.getAccessRequestHandler()");
        AccessRequestHandler accessRequestHandler = null;
        final Class<? extends AccessRequestHandler> accessRequestHandlerClass = reqCtx.getClientConfig()
                .getAccessRequestHandlerClass();

        try {
            accessRequestHandler = InjectorHolder.getInstance(accessRequestHandlerClass);
        } catch (final Exception e) {
            final StringBuilder sb = new StringBuilder("Unable to instantiate declared handler class '")
                    .append(accessRequestHandlerClass.getName()).append("' for RADIUS client '")
                    .append("'. Rejecting access.");
            LOG.error(sb.toString(), e);
        }

        try {
            accessRequestHandler.init(reqCtx.getClientConfig().getHandlerConfig());
        } catch (final Exception e) {
            final StringBuilder sb = new StringBuilder("Unable to initialize declared handler class '")
                    .append(accessRequestHandlerClass.getName()).append("' for RADIUS client '")
                    .append("'. Rejecting access.");
            LOG.error(sb.toString(), e);
            accessRequestHandler = null;
        }
        LOG.message("Leaving RadiusRequestHandler.getAccessRequestHandler()");
        return accessRequestHandler;
    }

}
