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

package org.forgerock.openam.core.rest.sms;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.rest.RestConstants.NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.RequestHandler;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.enums.QueryType;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.core.rest.sms.tree.SmsRouteTree;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceManager;

/**
 * Endpoint to handle requests for information about specific services available
 * to be generated in OpenAM. Requesting 'getAllTypes' will return the set of all
 * service types on the server, regardless of those currently instantiated. Requesting
 * 'getCreatableTypes' will return the set of all service types which have not been
 * instantiated on the server.
 *
 * @since 14.0.0
 */
@RequestHandler(@Handler(mvccSupported = false, resourceSchema = @Schema(fromType = Object.class)))
public class ServicesRealmSmsHandler {

    private final Debug debug;
    private final SmsConsoleServiceNameFilter consoleNameFilter;
    private SmsRouteTree routeTree;

    @Inject
    ServicesRealmSmsHandler(@Named("frRest") Debug debug, SmsConsoleServiceNameFilter consoleNameFilter) {
        this.debug = debug;
        this.consoleNameFilter = consoleNameFilter;
    }

    @Query(operationDescription = @Operation, type = QueryType.FILTER, queryableFields = "*")
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest queryRequest,
                                                                 QueryResourceHandler queryResourceHandler) {
        String searchForId;
        try {
            searchForId = queryRequest.getQueryFilter().accept(new IdQueryFilterVisitor(), null);
        } catch (UnsupportedOperationException e) {
            return new NotSupportedException("Query not supported: " + queryRequest.getQueryFilter()).asPromise();
        }
        if (queryRequest.getPagedResultsCookie() != null || queryRequest.getPagedResultsOffset() > 0 ||
                queryRequest.getPageSize() > 0) {
            return new NotSupportedException("Query paging not currently supported").asPromise();
        }

        final SSOToken ssoToken = context.asContext(SSOTokenContext.class).getCallerSSOToken();
        final String realm = context.asContext(RealmContext.class).getRealm().asPath();
        final List<ResourceResponse> resourceResponses = new ArrayList<>();

        try {
            final ServiceManager sm = getServiceManager(ssoToken);
            final Set<String> services = sm.getOrganizationConfigManager(realm).getAssignedServices();
            services.addAll(getIdentityServices(realm, ssoToken).getAssignedServices());
            consoleNameFilter.filter(services);

            final Map<String, String> serviceNameMap = consoleNameFilter.mapNameToDisplayName(services);

            for (String instanceName : serviceNameMap.keySet()) {

                if (searchForId == null || searchForId.equals(serviceNameMap.get(instanceName))) {

                    final JsonValue result = json(object(
                            field(ResourceResponse.FIELD_CONTENT_ID, instanceName),
                            field(NAME, serviceNameMap.get(instanceName))));

                    resourceResponses.add(newResourceResponse(instanceName, String.valueOf(result.hashCode()), result));
                }
            }

        } catch (SSOException | SMSException | IdRepoException e) {
            debug.error("ServiceInstanceCollectionHandler:: Unable to query SMS config: ", e);
            return new InternalServerErrorException("Unable to query SMS config.", e).asPromise();
        }

        return QueryResponsePresentation.perform(queryResourceHandler, queryRequest, resourceResponses);
    }

    private ServiceManager getServiceManager(SSOToken token) throws SMSException, SSOException {
        return new ServiceManager(token);
    }

    private AMIdentity getIdentityServices(String realmName, SSOToken userSSOToken)
            throws IdRepoException, SSOException {
        AMIdentityRepository repo = new AMIdentityRepository(realmName, userSSOToken);
        return repo.getRealmIdentity();
    }

    /**
     * The create request for services will be received at this level, but this handler does not know how to create
     * the singleton services. Instead we pass the create method on to the handler for the created resource ID.
     * @param context The request context.
     * @param request The request.
     * @return The result from the downstream handler.
     */
    @Create(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        UriRouterContext ctx = context.asContext(UriRouterContext.class);
        String serviceResourceId = request.getNewResourceId();
        UriRouterContext subRequestCtx = new UriRouterContext(context, "", serviceResourceId,
                ctx.getUriTemplateVariables());
        CreateRequest subRequest = Requests.copyOfCreateRequest(request)
                .setNewResourceId("")
                .setResourcePath(serviceResourceId);
        return routeTree.handleCreate(subRequestCtx, subRequest);
    }

    /**
     * Set the route tree that handles services, so that the create request can be sent on to the service handler for
     * the service that is being created.
     * @param routeTree The SMS route tree.
     */
    void setSmsRouteTree(SmsRouteTree routeTree) {
        this.routeTree = routeTree;
    }

}
