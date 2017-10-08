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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.rest.router;

import org.forgerock.http.ApiProducer;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Request;
import org.forgerock.services.context.Context;
import org.forgerock.services.routing.IncomparableRouteMatchException;
import org.forgerock.services.routing.RouteMatch;
import org.forgerock.services.routing.RouteMatcher;

/**
 * Matches routes for API related URLs.
 *
 * @since 14.0.0
 */
public class ApiRouteMatcher extends RouteMatcher<Request> {

    private static String API = "_api";
    private static String CREST_API = "_crestapi";

    @Override
    public RouteMatch evaluate(Context context, Request request) {
        Form form = request.getForm();
        if (form.containsKey(CREST_API) || form.containsKey(API)) {
            return new RouteMatch() {
                @Override
                public boolean isBetterMatchThan(RouteMatch result) throws IncomparableRouteMatchException {
                    return true;
                }

                @Override
                public Context decorateContext(Context context) {
                    return context;
                }
            };
        }
        return null;
    }

    @Override
    public String toString() {
        return CREST_API + " || " + API;
    }

    @Override
    public String idFragment() {
        return "";
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public <D> D transformApi(D descriptor, ApiProducer<D> producer) {
        return descriptor;
    }
}
