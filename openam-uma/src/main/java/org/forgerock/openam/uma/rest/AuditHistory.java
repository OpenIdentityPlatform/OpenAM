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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.uma.rest;

import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ACTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.AUDIT_HISTORY_RESOURCE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_500_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.enums.QueryType;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaAuditEntry;
import org.forgerock.openam.sm.datalayer.store.ServerException;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import com.google.inject.Inject;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;

@CollectionProvider(
        details = @Handler(
                title = AUDIT_HISTORY_RESOURCE + TITLE,
                description = AUDIT_HISTORY_RESOURCE + DESCRIPTION,
                mvccSupported = false,
                parameters = {
                        @Parameter(
                                name = "user",
                                type = "string",
                                description = AUDIT_HISTORY_RESOURCE + PATH_PARAM + "user")},
                resourceSchema = @Schema(fromType = UmaAuditEntry.class)))
public class AuditHistory {

    private final UmaAuditLogger auditLogger;

    @Inject
    public AuditHistory(UmaAuditLogger datastore) {
        this.auditLogger = datastore;
    }


    @Action(operationDescription = @Operation(description = AUDIT_HISTORY_RESOURCE + ACTION + "getHistory.description"),
        response = @Schema(fromType = SetOfAuditEntry.class))
    public Promise<ActionResponse, ResourceException> getHistory(Context context, ActionRequest request) {
        AMIdentity identity = getIdentity(context);
        try {
            return newResultPromise(newActionResponse(new JsonValue(auditLogger.getHistory(identity, null))));
        } catch (ServerException e) {
            return new InternalServerErrorException(e).asPromise();
        }
    }

    @Query(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = AUDIT_HISTORY_RESOURCE + ERROR_500_DESCRIPTION
                    )},
            description = AUDIT_HISTORY_RESOURCE + QUERY_DESCRIPTION),
            type = QueryType.FILTER,
            queryableFields = "*")
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        AMIdentity identity = getIdentity(context);

        Set<UmaAuditEntry> history;
        try {
            if (request.getQueryFilter().toString().equals("true")) {
                history = auditLogger.getEntireHistory(identity);
            } else {
                history = auditLogger.getHistory(identity, request);
            }
        } catch (ServerException e) {
            return new InternalServerErrorException(e).asPromise();
        }

        List<ResourceResponse> results = new ArrayList<>();
        for (UmaAuditEntry entry : history) {
            JsonValue result = entry.asJson();
            results.add(newResourceResponse(entry.getId(), String.valueOf(result.hashCode()), result));
        }

        QueryResponsePresentation.enableDeprecatedRemainingQueryResponse(request);
        return QueryResponsePresentation.perform(handler, request, results);
    }

    private AMIdentity getIdentity(Context context) {
        String realm = context.asContext(RealmContext.class).getRealm().asPath();
        final String user = context.asContext(UriRouterContext.class).getUriTemplateVariables().get("user");
        return IdUtils.getIdentity(user, realm);
    }

    private interface SetOfAuditEntry extends Set<UmaAuditEntry> {}
}
