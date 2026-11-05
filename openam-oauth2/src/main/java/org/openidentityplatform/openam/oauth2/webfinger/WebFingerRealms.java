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
 * Copyright 2026 3A Systems, LLC.
 */

package org.openidentityplatform.openam.oauth2.webfinger;

import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.rest.RealmContext;

/**
 * The realm steps of the WebFinger route, with the semantics of the former Restlet {@code RestletRealmRouter}: the
 * realm of the host name, one sub-realm per path segment before {@code webfinger}, then the {@code realm} query
 * parameter, which wins. A realm that does not exist answers 404 in the OAuth2 error shape.
 */
final class WebFingerRealms {

    /** The URI template variable of one sub-realm path segment. */
    static final String SUB_REALM = "subrealm";

    private WebFingerRealms() {
    }

    /**
     * Starts from the realm of the host name, unless a {@code realms/...} path has already chosen one.
     *
     * @return The filter.
     */
    static Filter hostRealmFilter() {
        return (context, request, next) -> {
            if (context.containsContext(RealmContext.class)) {
                return next.handle(context, request);
            }
            try {
                return next.handle(new RealmContext(context, Realm.of(request.getUri().getHost())), request);
            } catch (RealmLookupException e) {
                return newResultPromise(realmNotFound(e.getRealm()));
            }
        };
    }

    /**
     * Resolves the matched path segment as a sub-realm of the current realm, then routes the rest of the path to
     * {@code next}.
     *
     * @param next The router of the remaining path.
     * @return The handler.
     */
    static Handler subRealmHandler(Handler next) {
        return (context, request) -> {
            Realm current = context.asContext(RealmContext.class).getRealm();
            String segment = context.asContext(UriRouterContext.class).getUriTemplateVariables().get(SUB_REALM);
            if (segment.contains("/")) {
                // A decoded %2F: Realm.of rejects it with an IllegalArgumentException; master answered 404.
                return newResultPromise(realmNotFound(segment));
            }
            try {
                return next.handle(new RealmContext(context, Realm.of(current, segment)), request);
            } catch (RealmLookupException e) {
                return newResultPromise(realmNotFound(e.getRealm()));
            }
        };
    }

    /**
     * Lets the first {@code realm} query parameter replace the realm, as the Restlet router did at the last hop.
     *
     * @return The filter.
     */
    static Filter realmParameterFilter() {
        return (context, request, next) -> {
            String realm = new Form().fromRequestQuery(request).getFirst("realm");
            if (realm == null) {
                return next.handle(context, request);
            }
            try {
                return next.handle(new RealmContext(context, Realm.of(realm)), request);
            } catch (RealmLookupException e) {
                return newResultPromise(realmNotFound(e.getRealm()));
            }
        };
    }

    /**
     * The answer of the Restlet {@code OAuth2StatusService} to a realm that does not exist.
     *
     * @param realm The realm as the request named it.
     * @return 404 with the OAuth2 error body.
     */
    static Response realmNotFound(String realm) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", Status.NOT_FOUND.getReasonPhrase());
        body.put("error_description", "Realm \"" + realm + "\" not found");
        return new Response(Status.NOT_FOUND).setEntity(body);
    }
}
