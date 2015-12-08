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

package org.forgerock.openam.core.rest;

import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.openam.audit.AuditConstants.Component.CONFIG;
import static org.forgerock.openam.audit.AuditConstants.Component.REALMS;

import javax.inject.Inject;

import com.sun.identity.sm.SchemaType;
import org.forgerock.openam.core.rest.sms.SmsRequestHandlerFactory;
import org.forgerock.openam.core.rest.sms.SmsServerPropertiesResource;
import org.forgerock.openam.rest.AbstractRestRouteProvider;
import org.forgerock.openam.rest.RealmContextFilter;
import org.forgerock.openam.rest.ResourceRouter;
import org.forgerock.openam.rest.RestRouteProvider;
import org.forgerock.openam.rest.authz.PrivilegeAuthzModule;

/**
 * A {@link RestRouteProvider} that add routes for all the SMS endpoints.
 *
 * @since 13.0.0
 */
public class SmsRestRouteProvider extends AbstractRestRouteProvider {

    private SmsRequestHandlerFactory smsRequestHandlerFactory;

    @Override
    public void addResourceRoutes(ResourceRouter rootRouter, ResourceRouter realmRouter) {

        realmRouter.route("realms")
                .auditAs(REALMS)
                .authorizeWith(PrivilegeAuthzModule.class)
                .toCollection(RealmResource.class);

        realmRouter.route("realm-config")
                .auditAs(CONFIG)
                .authorizeWith(PrivilegeAuthzModule.class)
                .toRequestHandler(STARTS_WITH, smsRequestHandlerFactory.create(SchemaType.ORGANIZATION));

        rootRouter.route("global-config")
                .auditAs(CONFIG)
                .through(RealmContextFilter.class)
                .toRequestHandler(STARTS_WITH, smsRequestHandlerFactory.create(SchemaType.GLOBAL));

        rootRouter.route("global-config/servers/{serverName}/properties/{tab}")
                .auditAs(CONFIG)
                .authorizeWith(PrivilegeAuthzModule.class)
                .toSingleton(SmsServerPropertiesResource.class);
    }

    @Inject
    public void setSmsRequestHandlerFactory(SmsRequestHandlerFactory smsRequestHandlerFactory) {
        this.smsRequestHandlerFactory = smsRequestHandlerFactory;
    }
}
