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

import static org.forgerock.http.routing.RouteMatchers.resourceApiVersionMatcher;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.services.context.Context;
import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.routing.ResourceApiVersionBehaviourManager;
import org.forgerock.http.routing.Version;
import org.forgerock.services.routing.RouteMatch;
import org.forgerock.services.routing.RouteMatcher;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.ext.servlet.ServletUtils;

public final class ResourceApiVersionRestlet extends Restlet {

    private final RestletRouter router = new RestletRouter();
    private final ResourceApiVersionRoutingFilter versionFilter;

    public ResourceApiVersionRestlet(ResourceApiVersionBehaviourManager behaviourManager) {
        versionFilter = new ResourceApiVersionRoutingFilter(behaviourManager);
    }

    public void attach(Version version, Restlet restlet) {
        router.addRoute(new RequestApiVersionRouteMatcher(resourceApiVersionMatcher(version)), restlet);
    }

    @Override
    public void handle(Request request, Response response) {
        versionFilter.handle(request, response, router);
    }

    public static Version parseResourceApiVersion(Request request) {
        HttpServletRequest httpRequest = ServletUtils.getRequest(request);
        String versionHeader = httpRequest.getHeader(AcceptApiVersionHeader.NAME);
        AcceptApiVersionHeader apiVersionHeader = AcceptApiVersionHeader.valueOf(versionHeader);
        return apiVersionHeader.getResourceVersion();
    }

    /**
     * A Restlet specific {@code RouteMatcher} which extracts the resource API
     * version from a {@code Request} and passes it to the common
     * {@code Version} route matcher.
     */
    private static final class RequestApiVersionRouteMatcher extends RouteMatcher<Request> {

        private final RouteMatcher<Version> delegate;

        private RequestApiVersionRouteMatcher(RouteMatcher<Version> delegate) {
            this.delegate = delegate;
        }

        @Override
        public RouteMatch evaluate(Context context, Request request) {
            return delegate.evaluate(context, parseResourceApiVersion(request));
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RequestApiVersionRouteMatcher)) {
                return false;
            }
            RequestApiVersionRouteMatcher that = (RequestApiVersionRouteMatcher) o;
            return delegate.equals(that.delegate);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }
}
