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

package org.forgerock.openam.rest.router;

/**
* Manager for all of the registered rest endpoints.
*
* @since 12.0.0
*/
public interface RestEndpointManager {

    /**
     * The users resource endpoint.
     */
    String USERS = "/users";

    /**
     * The sessions resource endpoint.
     */
    String SESSIONS = "/sessions";

    /**
     * The serverinfo resource endpoint.
     */
    String SERVER_INFO = "/serverinfo";

    /**
     * The authenticate service endpoint.
     */
    String AUTHENTICATE = "/authenticate";

    /**
     * Returns the type of endpoint of the given endpoint.
     *
     * @param endpoint The endpoint.
     * @return The endpoint's type.
     */
    EndpointType getEndpointType(String endpoint);

    /**
     * Given the request path, i.e. /realm1/realm2/endpoint, this method will attempt to find the endpoint
     * from the set of registered endpoints.
     * <br/>
     * Will return <code>null</code> if no endpoint is found.
     *
     * @param request The context path of the request.
     * @return The endpoint or <code>null</code>.
     */
    String findEndpoint(final String request);

    /**
     * An enum for each of the possible Endpoint types.
     *
     * @since 12.0.0
     */
    enum EndpointType {

        /**
         * Resource Endpoint Type.
         */
        RESOURCE,

        /**
         * Service Endpoint Type.
         */
        SERVICE
    }
}
