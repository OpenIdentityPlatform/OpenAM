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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.rest.service;

import org.forgerock.openam.rest.RestEndpoints;
import org.restlet.data.MediaType;

/**
 * A {@code ServiceEndpointApplication} for the /json services.
 */
public class XACMLServiceEndpointApplication extends ServiceEndpointApplication {

    public static final MediaType APPLICATION_XML_XACML3 = MediaType.register(
            "application/xacml+xml; version=3.0", "XACML v3.0 XML");

    /**
     * {@inheritDoc}
     * Sets the default media type as "application/json".
     */
    public XACMLServiceEndpointApplication() {
        super(new XMLRestStatusService());
        getMetadataService().setDefaultMediaType(APPLICATION_XML_XACML3);
    }

    /**
     * Returns the XACML router.
     * @param restEndpoints Registry of routers.
     * @return
     */
    protected ServiceRouter getRouter(RestEndpoints restEndpoints) {
        return restEndpoints.getXACMLServiceRouter();
    }
}
