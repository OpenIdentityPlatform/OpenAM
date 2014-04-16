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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.util.Reject;

import javax.inject.Inject;

import java.util.List;

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * REST endpoint for policy/entitlements management and evaluation.
 *
 * @since 12.0.0
 */
public final class EntitlementsResource implements CollectionResourceProvider {

    /**
     * Parser for converting between JSON and concrete entitlements policy instances.
     */
    private final PolicyParser policyParser;
    /**
     * Data store for entitlements policies.
     */
    private final PolicyStoreProvider policyStoreProvider;
    /**
     * Handler for converting entitlements exceptions into appropriate resource exceptions.
     */
    private final ResourceErrorHandler<EntitlementException> resourceErrorHandler;

    @Inject
    public EntitlementsResource(final PolicyParser parser,
                                final PolicyStoreProvider provider,
                                final ResourceErrorHandler<EntitlementException> handler) {
        Reject.ifNull(parser, provider, handler);
        this.policyParser = parser;
        this.policyStoreProvider = provider;
        this.resourceErrorHandler = handler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
                               ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        try {
            Privilege policy = policyParser.parsePolicy(request.getNewResourceId(), request.getContent());
            policyStoreProvider.getPolicyStore(context).create(policy);
            handler.handleResult(policyResource(policy));
        } catch (EntitlementException ex) {
            handler.handleError(resourceErrorHandler.handleError(request, ex));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                               ResultHandler<Resource> handler) {
        try {
            PolicyStore store = policyStoreProvider.getPolicyStore(context);
            store.delete(resourceId);
            // Return an empty resource to indicate success?
            handler.handleResult(new Resource(resourceId, null, json(object())));
        } catch (EntitlementException ex) {
            handler.handleError(resourceErrorHandler.handleError(request, ex));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
                              ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        try {
            List<Privilege> policies = policyStoreProvider.getPolicyStore(context).query(request);

            if (policies != null) {
                for (Privilege policy : policies) {
                    handler.handleResource(policyResource(policy));
                }
            }

            handler.handleResult(new QueryResult());
        } catch (EntitlementException ex) {
            handler.handleError(resourceErrorHandler.handleError(request, ex));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                             ResultHandler<Resource> handler) {
        try {
            Privilege policy = policyStoreProvider.getPolicyStore(context).read(resourceId);
            handler.handleResult(policyResource(policy));
        } catch (EntitlementException ex) {
            handler.handleError(resourceErrorHandler.handleError(request, ex));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                               ResultHandler<Resource> handler) {
        try {
            Privilege policy = policyParser.parsePolicy(resourceId, request.getContent());
            Resource result = policyResource(policyStoreProvider.getPolicyStore(context).update(policy));
            handler.handleResult(result);
        } catch (EntitlementException ex) {
            handler.handleError(resourceErrorHandler.handleError(request, ex));
        }
    }

    /**
     * Returns a resource based on the given policy.
     *
     * @param policy the policy to return a resource representation of.
     * @return the policy as a resource.
     * @throws EntitlementException if the policy cannot be serialised into JSON.
     */
    private Resource policyResource(Privilege policy) throws EntitlementException {
        return new Resource(policy.getName(), policyRevision(policy), policyParser.printPolicy(policy));
    }

    /**
     * Determines the revision value to use for the eTag attribute in the result. Currently just uses the last modified
     * timestamp. Currently etag values are not validated for requests.
     *
     * @param policy the policy to generate a revision string for.
     * @return the revision string (last modified time stamp).
     */
    private String policyRevision(Privilege policy) {
        return Long.toString(policy.getLastModifiedDate());
    }
}
