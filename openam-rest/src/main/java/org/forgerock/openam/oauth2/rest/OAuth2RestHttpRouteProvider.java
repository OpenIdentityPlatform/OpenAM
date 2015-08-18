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

package org.forgerock.openam.oauth2.rest;

import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.openam.audit.AuditConstants.Component.OAUTH2;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.Set;

import org.forgerock.openam.http.HttpRoute;
import org.forgerock.openam.http.HttpRouteProvider;
import org.forgerock.openam.rest.RestRouter;
import org.forgerock.openam.rest.authz.AdminOnlyAuthzModule;

public class OAuth2RestHttpRouteProvider implements HttpRouteProvider {

    private RestRouter rootRouter;

    @Inject
    public void setRouters(RestRouter router) {
        this.rootRouter = router;
    }

    @Override
    public Set<HttpRoute> get() {

        rootRouter.route("token")
                .auditAs(OAUTH2)
                .toCollection(TokenResource.class);

        rootRouter.route("client")
                .auditAs(OAUTH2)
                .authorizeWith(AdminOnlyAuthzModule.class)
                .toCollection(ClientResource.class);

        return Collections.singleton(HttpRoute.newHttpRoute(STARTS_WITH, "frrest/oauth2", rootRouter.getRouter()));
    }
}
