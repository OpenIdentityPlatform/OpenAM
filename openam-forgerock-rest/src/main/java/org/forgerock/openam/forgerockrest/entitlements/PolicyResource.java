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

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.forgerockrest.entitlements.model.json.JsonPolicyRequest;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryResultHandlerBuilder;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.openam.forgerockrest.entitlements.model.json.JsonPolicyRequest.Builder;

/**
 * REST endpoint for policy/entitlements management and evaluation.
 *
 * @since 12.0.0
 */
public final class PolicyResource implements CollectionResourceProvider {

    private static final Debug DEBUG = Debug.getInstance("amPolicy");

    private final static String EVAL_ACTION = "evaluate";

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

    private final PolicyEvaluatorFactory factory;

    @Inject
    public PolicyResource(final PolicyEvaluatorFactory factory,
                          final PolicyParser parser,
                          final PolicyStoreProvider provider,
                          final ResourceErrorHandler<EntitlementException> handler) {
        Reject.ifNull(factory, parser, provider, handler);
        this.factory = factory;
        this.policyParser = parser;
        this.policyStoreProvider = provider;
        this.resourceErrorHandler = handler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        final String action = request.getAction();

        if (EVAL_ACTION.equals(action)) {
            try {
                final JsonPolicyRequest policyRequest = new Builder(context, request).build();
                handler.handleResult(evaluateRequest(policyRequest));
                return;
            } catch (EntitlementException eE) {
                DEBUG.error("Error evaluating policy request", eE);
                handler.handleError(resourceErrorHandler.handleError(request, eE));
                return;
            }
        }

        final String errorMsg = "Action '" + action + "' not implemented for this resource";
        final NotSupportedException nsE = new NotSupportedException(errorMsg);
        DEBUG.error(errorMsg, nsE);
        handler.handleError(nsE);
    }

    /**
     * Evaluates the given policy request and returns a json decision.
     *
     * @param request
     *         a non-null policy request
     *
     * @return a json representation of the decision
     *
     * @throws EntitlementException
     *         in the event of a failure during the evaluation process
     */
    private JsonValue evaluateRequest(final JsonPolicyRequest request) throws EntitlementException {
        DEBUG.message("Evaluating policy request");

        final PolicyEvaluator evaluator = factory.getEvaluator(request.getRestSubject(), request.getApplication());

        final List<Entitlement> entitlements = evaluator.evaluate(
                request.getRealm(), request.getPolicySubject(), request.getResources(), request.getEnvironment());

        return policyParser.printEntitlements(entitlements);
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
            Privilege policy = policyParser.parsePolicy(determineNewPolicyName(request), request.getContent());
            policyStoreProvider.getPolicyStore(context).create(policy);
            handler.handleResult(policyResource(policy));
        } catch (EntitlementException ex) {
            handler.handleError(resourceErrorHandler.handleError(request, ex));
        }
    }

    /**
     * Determines the policy name to use for a new policy based on either the name specified in the URL (for PUT
     * requests) or the name specified in the JSON body (for POST requests). If neither is specified then an error is
     * raised as we do not support auto-generating policy names. If both are specified, and they are different, then
     * an error is raised indicating client confusion.
     *
     * @param request the create request for the policy.
     * @return the name to use for the new policy.
     * @throws EntitlementException if the name cannot be determined from the request.
     */
    private String determineNewPolicyName(CreateRequest request) throws EntitlementException {

        String requestPolicyName = request.getNewResourceId();
        String jsonPolicyName = request.getContent().get("name").asString();

        if (isNotBlank(requestPolicyName) && isNotBlank(jsonPolicyName) && !requestPolicyName.equals(jsonPolicyName)) {
            throw new EntitlementException(EntitlementException.POLICY_NAME_MISMATCH);
        }

        String policyName = isNotBlank(requestPolicyName) ? requestPolicyName : jsonPolicyName;

        if (isBlank(policyName)) {
            throw new EntitlementException(EntitlementException.MISSING_PRIVILEGE_NAME);
        }

        return policyName;
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
            handler.handleResult(new Resource(resourceId, "0", json(object())));
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
            handler = QueryResultHandlerBuilder.withPagingAndSorting(handler, request);
            List<Privilege> policies = policyStoreProvider.getPolicyStore(context).query(request);

            int remaining = 0;
            if (policies != null) {
                remaining = policies.size();
                for (Privilege policy : policies) {
                    boolean keepGoing = handler.handleResource(policyResource(policy));
                    remaining--;
                    if (!keepGoing) {
                        break;
                    }
                }
            }

            handler.handleResult(new QueryResult(null, remaining));
        } catch (EntitlementException ex) {
            handler.handleError(resourceErrorHandler.handleError(request, ex));
        } catch (IllegalArgumentException ex) {
            handler.handleError(ResourceException.getException(ResourceException.BAD_REQUEST, ex.getMessage()));
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
