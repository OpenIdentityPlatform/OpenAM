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
 * Portions Copyrighted 2025 3A Systems LLC.
 */
package org.forgerock.openam.audit.rest;

import static org.forgerock.api.models.ApiDescription.apiDescription;
import static org.forgerock.api.models.Paths.paths;
import static org.forgerock.api.models.Resource.AnnotatedTypeVariant.REQUEST_HANDLER;
import static org.forgerock.api.models.Resource.fromAnnotatedType;
import static org.forgerock.api.models.VersionedPath.*;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.*;

import jakarta.inject.Inject;

import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.ApiProducer;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.audit.AMAuditService;
import org.forgerock.openam.audit.AuditServiceProvider;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link RequestHandler} that handles realm based audit event requests.
 *
 * @since 14.0.0
 */
@org.forgerock.api.annotations.RequestHandler(value = @Handler(
        title = AUDIT_SERVICE + "realm." + TITLE,
        description = AUDIT_SERVICE + "realm." + DESCRIPTION,
        mvccSupported = false,
        resourceSchema = @Schema(schemaResource = "/org/forgerock/openam/audit/AuditEvent.resource.schema.json"),
        parameters = @Parameter(
                name = "topic",
                description = AUDIT_SERVICE + PATH_PARAM + DESCRIPTION,
                type = "string",
                enumValues = {"access", "activity", "authentication", "config"},
                enumTitles = {"Access event", "Activity event", "Authentication event", "Config event"}
        )))
public class RealmAuditRequestHandler implements RequestHandler, Describable<ApiDescription, Request> {
    private static final String FAKE_ID = "fake:id";
    private static final String FAKE_VERSION = "fake";

    private final Logger logger = LoggerFactory.getLogger("amAudit");
    private final ApiDescription descriptor;

    private AuditServiceProvider auditServiceProvider;

    /**
     * Create a new instance of {@link RealmAuditRequestHandler}.
     *
     * @param auditServiceProvider The {@link AuditServiceProvider} that will handle the requests.
     */
    @Inject
    public RealmAuditRequestHandler(AuditServiceProvider auditServiceProvider) {
        this.auditServiceProvider = auditServiceProvider;
        this.descriptor = apiDescription()
                .id(FAKE_ID).version(FAKE_VERSION)
                .paths(paths().put("{topic}",
                        versionedPath().put(UNVERSIONED, fromAnnotatedType(this.getClass(), REQUEST_HANDLER,
                                apiDescription().id(FAKE_ID).version(FAKE_VERSION).build()))
                                .build())
                        .build())
                .build();
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context,
            ActionRequest actionRequest) {
        return getAuditService(context).handleAction(context, actionRequest);
    }

    @Create(operationDescription = @Operation(description = AUDIT_SERVICE + CREATE_DESCRIPTION))
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
        String realm = context.asContext(RealmContext.class).getRealm().asPath();

        if (StringUtils.isEmpty(realm)) {
            logger.warn("Context contained RealmContext but had an empty resolved realm");
            return auditServiceProvider.getDefaultAuditService();
        }
        return auditServiceProvider.getAuditService(realm);
    }

    @Override
    public ApiDescription api(ApiProducer<ApiDescription> apiProducer) {
        return descriptor;
    }

    @Override
    public ApiDescription handleApiRequest(Context context, Request request) {
        return descriptor;
    }

    @Override
    public void addDescriptorListener(Listener listener) {
        // Doesn't change so no need to support listeners.
    }

    @Override
    public void removeDescriptorListener(Listener listener) {
        // Doesn't change so no need to support listeners.
    }
}
