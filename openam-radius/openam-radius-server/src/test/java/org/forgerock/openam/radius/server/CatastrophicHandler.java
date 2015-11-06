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

import java.util.Properties;

import org.forgerock.openam.radius.server.spi.AccessRequestHandler;

/**
 * Handler that throws a RadiusProcesingException. Useful for functional testing.
 */
public class CatastrophicHandler implements AccessRequestHandler {

    /* (non-Javadoc)
     * @see org.forgerock.openam.radius.server.spi.AccessRequestHandler#init(java.util.Properties)
     */
    @Override
    public void init(Properties config) {

    }

    /*
     * (non-Javadoc)
     * @see org.forgerock.openam.radius.server.spi.AccessRequestHandler#handle
     * (org.forgerock.openam.radius.common.AccessRequest, org.forgerock.openam.radius.server.RadiusResponseHandler)
     */
    @Override
    public void handle(RadiusRequest request, RadiusResponse response, RadiusRequestContext context)
            throws RadiusProcessingException {
        throw new RadiusProcessingException(RadiusProcessingExceptionNature.CATASTROPHIC, "Test catestrophic.");
    }


}
