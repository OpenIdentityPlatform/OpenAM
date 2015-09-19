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

package org.forgerock.openam.rest.service;

import static org.forgerock.http.routing.Version.version;
import static org.forgerock.openam.rest.service.ResourceApiVersionRestlet.parseResourceApiVersion;

import org.forgerock.services.context.RootContext;
import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.header.ContentApiVersionHeader;
import org.forgerock.http.routing.ApiVersionRouterContext;
import org.forgerock.http.routing.ResourceApiVersionBehaviourManager;
import org.forgerock.http.routing.Version;
import org.forgerock.json.resource.AdviceWarning;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.ext.servlet.ServletUtils;

final class ResourceApiVersionRoutingFilter extends org.forgerock.http.routing.ResourceApiVersionRoutingFilter {

    private static final Version enforcedProtocolVersion = version(1);
    private static final String AGENT_NAME = "OpenAM REST";
    private final ResourceApiVersionBehaviourManager behaviourManager;

    /**
     * Constructs a new {@code ResourceApiVersionRoutingFilter} instance.
     *
     * @param behaviourManager The {@link ResourceApiVersionBehaviourManager} instance
     *                         that manages the API Version routing settings.
     */
    ResourceApiVersionRoutingFilter(ResourceApiVersionBehaviourManager behaviourManager) {
        super(behaviourManager);
        this.behaviourManager = behaviourManager;
    }

    void handle(Request request, Response response, RestletRouter next) {
        ApiVersionRouterContext apiVersionRouterContext = createApiVersionRouterContext(new RootContext());
        next.handle(apiVersionRouterContext, request, response);

        if (apiVersionRouterContext.getResourceVersion() != null) {
            addContentAPIVersion(response, enforcedProtocolVersion, apiVersionRouterContext.getResourceVersion());

            // If warnings are wanted, and we haven't thrown an exception by this point let's see what we can do...
            if (behaviourManager.isWarningEnabled()) {
                Version requestedVersion = parseResourceApiVersion(request);
                if (requestedVersion == null) {
                    // If no version specified at all, we can warn about that
                    AdviceWarning warningHeader = AdviceWarning.newAdviceWarning(AGENT_NAME,
                            "No " + AcceptApiVersionHeader.NAME + " specified");
                    ServletUtils.getResponse(response).addHeader("Warning", warningHeader.toString());
                }
            }
        }
    }

    private void addContentAPIVersion(Response response, Version protocolVersion, Version resourceVersion) {
        ServletUtils.getResponse(response).addHeader(ContentApiVersionHeader.NAME,
                new ContentApiVersionHeader(protocolVersion, resourceVersion).toString());
    }
}
