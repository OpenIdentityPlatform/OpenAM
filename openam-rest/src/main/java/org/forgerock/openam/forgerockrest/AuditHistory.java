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

package org.forgerock.openam.forgerockrest;

import static org.forgerock.json.resource.ResourceException.newNotSupportedException;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import com.google.inject.Inject;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import org.forgerock.http.routing.RouterContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.promise.Promise;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.http.context.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryResourceHandlerBuilder;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.sm.datalayer.store.ServerException;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaAuditEntry;
import org.forgerock.openam.uma.audit.UmaAuditLogger;

import java.util.Set;

public class AuditHistory implements CollectionResourceProvider {

    private final UmaAuditLogger auditLogger;

    @Inject
    public AuditHistory(UmaAuditLogger datastore) {
        this.auditLogger = datastore;
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(ServerContext context, ActionRequest request) {
        AMIdentity identity = getIdentity(context);
        try {
            return newResultPromise(newActionResponse(new JsonValue(auditLogger.getHistory(identity, null))));
        } catch (ServerException e) {
            return newExceptionPromise((ResourceException) new InternalServerErrorException(e));
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(ServerContext context, QueryRequest request,
            QueryResourceHandler handler) {
        AMIdentity identity = getIdentity(context);
        try {
            Set<UmaAuditEntry> history;

            if (request.getQueryFilter().toString().equals("true")) {
                history = auditLogger.getEntireHistory(identity);
            } else {
                history = auditLogger.getHistory(identity, request);
            }

            final QueryResourceHandler resultHandler = QueryResourceHandlerBuilder.withPagingAndSorting(handler, request);

            for (UmaAuditEntry entry : history) {
                resultHandler.handleResource(newResourceResponse(entry.getId(), null, entry.asJson()));
            }

            return newResultPromise(newQueryResponse());
        } catch (ServerException e) {
            return newExceptionPromise((ResourceException) new InternalServerErrorException(e));
        }
    }

    private AMIdentity getIdentity(ServerContext context) {
        String realm = context.asContext(RealmContext.class).getResolvedRealm();
        final String user = context.asContext(RouterContext.class).getUriTemplateVariables().get("user");
        return IdUtils.getIdentity(user, realm);
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(ServerContext context, String resourceId,
            ActionRequest request) {
        return newExceptionPromise(newNotSupportedException("Not supported."));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(ServerContext context, CreateRequest request) {
        return newExceptionPromise(newNotSupportedException("Not supported."));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(ServerContext context, String resourceId,
            DeleteRequest request) {
        return newExceptionPromise(newNotSupportedException("Not supported."));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(ServerContext context, String resourceId,
            PatchRequest request) {
        return newExceptionPromise(newNotSupportedException("Not supported."));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(ServerContext context, String resourceId,
            ReadRequest request) {
        return newExceptionPromise(newNotSupportedException("Not supported."));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(ServerContext context, String resourceId,
            UpdateRequest request) {
        return newExceptionPromise(newNotSupportedException("Not supported."));
    }
}
