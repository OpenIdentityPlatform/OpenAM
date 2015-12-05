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

package org.forgerock.openam.audit.rest;

import static org.forgerock.http.routing.RoutingMode.*;
import static org.forgerock.openam.audit.AuditConstants.Component.*;

import javax.inject.Inject;

import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.audit.AMAuditService;
import org.forgerock.openam.audit.AuditServiceProvider;
import org.forgerock.openam.rest.AbstractRestRouteProvider;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.RealmContextFilter;
import org.forgerock.openam.rest.ResourceRouter;
import org.forgerock.openam.rest.RestRouteProvider;
import org.forgerock.openam.rest.authz.SpecialOrAdminOrAgentAuthzModule;
import org.forgerock.openam.rest.fluent.AuditEndpointAuditFilter;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link RestRouteProvider} that add routes for the audit endpoint.
 *
 * @since 13.0.0
 */
public class AuditRestRouteProvider extends AbstractRestRouteProvider {
    private AuditServiceProvider auditServiceProvider;
    private final Logger logger = LoggerFactory.getLogger("amAudit");

    /**
     * Inject the service provider.
     * @param auditServiceProvider The provider.
     */
    @Inject
    public void setProvider(AuditServiceProvider auditServiceProvider) {
        this.auditServiceProvider = auditServiceProvider;
    }

    @Override
    public void addResourceRoutes(ResourceRouter rootRouter, ResourceRouter realmRouter) {
        rootRouter.route("global-audit")
                .auditAs(AUDIT, AuditEndpointAuditFilter.class)
                .authorizeWith(SpecialOrAdminOrAgentAuthzModule.class)
                .forVersion(1)
                .toRequestHandler(STARTS_WITH, auditServiceProvider.getDefaultAuditService());

        rootRouter.route("realm-audit")
                .auditAs(AUDIT, AuditEndpointAuditFilter.class)
                .authorizeWith(SpecialOrAdminOrAgentAuthzModule.class)
                .forVersion(1)
                .through(RealmContextFilter.class)
                .toRequestHandler(
                        STARTS_WITH, new RequestHandler() {
                            @Override
                            public Promise<ActionResponse, ResourceException> handleAction(Context context,
                                    ActionRequest actionRequest) {
                                return getAuditService(context).handleAction(context, actionRequest);
                            }

                            @Override
                            public Promise<ResourceResponse, ResourceException> handleCreate(Context context,
                                    CreateRequest createRequest) {
                                return getAuditService(context).handleCreate(context, createRequest);
                            }

                            @Override
                            public Promise<ResourceResponse, ResourceException> handleDelete(Context context,
                                    DeleteRequest deleteRequest) {
                                return getAuditService(context).handleDelete(context, deleteRequest);
                            }

                            @Override
                            public Promise<ResourceResponse, ResourceException> handlePatch(Context context,
                                    PatchRequest patchRequest) {
                                return getAuditService(context).handlePatch(context, patchRequest);
                            }

                            @Override
                            public Promise<QueryResponse, ResourceException> handleQuery(Context context,
                                    QueryRequest queryRequest, QueryResourceHandler queryResourceHandler) {
                                return getAuditService(context).handleQuery(
                                        context, queryRequest, queryResourceHandler);
                            }

                            @Override
                            public Promise<ResourceResponse, ResourceException> handleRead(Context context,
                                    ReadRequest readRequest) {
                                return getAuditService(context).handleRead(context, readRequest);
                            }

                            @Override
                            public Promise<ResourceResponse, ResourceException> handleUpdate(Context context,
                                    UpdateRequest updateRequest) {
                                return getAuditService(context).handleUpdate(context, updateRequest);
                            }

                            private AMAuditService getAuditService(Context context) {
                                String realm = context.asContext(RealmContext.class).getResolvedRealm();

                                if (StringUtils.isEmpty(realm)) {
                                    logger.warn("Context contained RealmContext but had an empty resolved realm");
                                    return auditServiceProvider.getDefaultAuditService();
                                }
                                return auditServiceProvider.getAuditService(realm);
                            }
                        });
    }
}
