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
import org.codehaus.jackson.map.JsonMappingException;
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
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.forgerockrest.entitlements.model.json.PolicyRequest;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryResultHandlerBuilder;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import java.util.List;

import static org.apache.commons.lang.StringUtils.*;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * REST endpoint for policy/entitlements management and evaluation.
 *
 * @since 12.0.0
 */
public final class PolicyResource implements CollectionResourceProvider {

    private static final Debug DEBUG = Debug.getInstance("amPolicy");

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
    private final PolicyRequestFactory requestFactory;

    @Inject
    public PolicyResource(final PolicyEvaluatorFactory factory,
                          final PolicyRequestFactory requestFactory,
                          final PolicyParser parser,
                          final PolicyStoreProvider provider,
                          final ResourceErrorHandler<EntitlementException> handler) {
        Reject.ifNull(factory, requestFactory, parser, provider, handler);
        this.factory = factory;
        this.requestFactory = requestFactory;
        this.policyParser = parser;
        this.policyStoreProvider = provider;
        this.resourceErrorHandler = handler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(ServerContext context, ActionRequest actionRequest, ResultHandler<JsonValue> handler) {
        final String actionString = actionRequest.getAction();
        final PolicyAction action = PolicyAction.getAction(actionString);

        if (!PolicyAction.isEvaluateAction(action)) {
            final String errorMsg = "Action '" + actionString + "' not implemented for this resource";
            final NotSupportedException nsE = new NotSupportedException(errorMsg);
            DEBUG.error(errorMsg, nsE);
            handler.handleError(nsE);
            return;
        }

        try {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Rendering policy request for action " + actionString);
            }

            final PolicyRequest request = requestFactory.buildRequest(action, context, actionRequest);
            final PolicyEvaluator evaluator = factory.getEvaluator(request.getRestSubject(), request.getApplication());

            if (DEBUG.messageEnabled()) {
                final StringBuilder builder = new StringBuilder();
                builder.append("Evaluating policy request for action ");
                builder.append(actionString);
                builder.append(" under realm ");
                builder.append(request.getRealm());
                builder.append(" within the application context ");
                builder.append(request.getApplication());
                DEBUG.message(builder.toString());
            }

            final List<Entitlement> entitlements = evaluator.routePolicyRequest(request);
            handler.handleResult(policyParser.printEntitlements(entitlements));

        } catch (final EntitlementException eE) {
            DEBUG.error("Error evaluating policy request", eE);
            handler.handleError(resourceErrorHandler.handleError(actionRequest, eE));
            return;
        }
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
        String providedName = null;
        try {
            providedName = request.getNewResourceId();

            Privilege policy = policyParser.parsePolicy(providedName, request.getContent());

            if (isNotBlank(providedName) && !providedName.equals(policy.getName())) {
                DEBUG.error("PolicyResource :: CREATE : Resource name and JSON body name do not match.");
                throw new EntitlementException(EntitlementException.POLICY_NAME_MISMATCH);
            }

            if (isBlank(providedName)) {
                providedName = policy.getName();
            }

            // OPENAM-5031
            // This is a bad solution and should be rewritten when we have time.  This code rejects anything in the
            // name that when encoded differs from the original.  So, for instance "+" becomes "\+".
            // What we should do is to encode the name for storage purposes, and decode it before presentation to the
            // user.
            if (!providedName.equals(DN.escapeAttributeValue(providedName))) {
                throw new EntitlementException(EntitlementException.INVALID_VALUE,
                        new Object[]{"policy name \"" + providedName + "\""});
            }

            policyStoreProvider.getPolicyStore(context).create(policy);
            handler.handleResult(policyResource(policy));
        } catch (EntitlementException ex) {
            DEBUG.error("PolicyResource :: CREATE : Error performing create for policy, " + providedName, ex);
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
            if (DEBUG.messageEnabled()) {
                DEBUG.message("PolicyResource :: DELETE : Deleted policy with ID, " + resourceId);
            }
            // Return an empty resource to indicate success?
            handler.handleResult(new Resource(resourceId, "0", json(object())));
        } catch (EntitlementException ex) {
            DEBUG.error("PolicyResource :: DELETE : Error performing delete for policy, " + resourceId, ex);
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
            DEBUG.error("PolicyResource :: QUERY : Error querying policy collection.", ex);
            handler.handleError(resourceErrorHandler.handleError(request, ex));
        } catch (IllegalArgumentException ex) {
            DEBUG.error("PolicyResource :: QUERY : Error querying policy collection due to bad request.", ex);
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
            DEBUG.error("PolicyResource :: READ : Error reading policy, " + resourceId + ".", ex);
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
            Resource result = policyResource(policyStoreProvider.getPolicyStore(context).update(resourceId, policy));
            handler.handleResult(result);
        } catch (EntitlementException ex) {
            DEBUG.error("PolicyResource :: UPDATE : Error updating policy, " + resourceId + ".", ex);
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
