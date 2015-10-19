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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.rest;

import static org.apache.commons.lang.StringUtils.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.util.promise.Promises.*;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.entitlement.rest.model.json.PolicyRequest;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

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
    private final ExceptionMappingHandler<EntitlementException, ResourceException> resourceErrorHandler;

    private final PolicyEvaluatorFactory factory;
    private final PolicyRequestFactory requestFactory;

    @Inject
    public PolicyResource(final PolicyEvaluatorFactory factory,
                          final PolicyRequestFactory requestFactory,
                          final PolicyParser parser,
                          final PolicyStoreProvider provider,
                          final ExceptionMappingHandler<EntitlementException, ResourceException> handler) {
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
    public Promise<ActionResponse, ResourceException> actionCollection(Context context,
            ActionRequest actionRequest) {
        final String actionString = actionRequest.getAction();
        final PolicyAction action = PolicyAction.getAction(actionString);

        if (!PolicyAction.isEvaluateAction(action)) {
            final String errorMsg = "Action '" + actionString + "' not implemented for this resource";
            final NotSupportedException nsE = new NotSupportedException(errorMsg);
            DEBUG.error(errorMsg, nsE);
            return nsE.asPromise();
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
            return newResultPromise(newActionResponse(policyParser.printEntitlements(entitlements)));

        } catch (final EntitlementException eE) {
            DEBUG.error("Error evaluating policy request", eE);
            return resourceErrorHandler.handleError(context, actionRequest, eE).asPromise();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
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
            return newResultPromise(policyResource(policy));
        } catch (EntitlementException ex) {
            DEBUG.error("PolicyResource :: CREATE : Error performing create for policy, " + providedName, ex);
            return resourceErrorHandler.handleError(context, request, ex).asPromise();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
            DeleteRequest request) {
        try {
            PolicyStore store = policyStoreProvider.getPolicyStore(context);
            store.delete(resourceId);
            if (DEBUG.messageEnabled()) {
                DEBUG.message("PolicyResource :: DELETE : Deleted policy with ID, " + resourceId);
            }
            // Return an empty resource to indicate success?
            return newResultPromise(newResourceResponse(resourceId, "0", json(object())));
        } catch (EntitlementException ex) {
            String debug = "PolicyResource :: DELETE : Error performing delete for policy, " + resourceId;
            return resourceErrorHandler.handleError(context, debug, request, ex).asPromise();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        try {
            List<ResourceResponse> results = new ArrayList<>();
            for (Privilege policy: policyStoreProvider.getPolicyStore(context).query(request)) {
                results.add(policyResource(policy));
            }

            QueryResponsePresentation.enableDeprecatedRemainingQueryResponse(request);
            return QueryResponsePresentation.perform(handler, request, results);
        } catch (EntitlementException ex) {
            DEBUG.error("PolicyResource :: QUERY : Error querying policy collection.", ex);
            return resourceErrorHandler.handleError(context, request, ex).asPromise();
        } catch (IllegalArgumentException ex) {
            DEBUG.error("PolicyResource :: QUERY : Error querying policy collection due to bad request.", ex);
            return new BadRequestException(ex.getMessage()).asPromise();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {
        try {
            Privilege policy = policyStoreProvider.getPolicyStore(context).read(resourceId);
            return newResultPromise(policyResource(policy));
        } catch (EntitlementException ex) {
            DEBUG.error("PolicyResource :: READ : Error reading policy, " + resourceId + ".", ex);
            return resourceErrorHandler.handleError(context, request, ex).asPromise();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {
        try {
            Privilege policy = policyParser.parsePolicy(resourceId, request.getContent());
            ResourceResponse result = policyResource(policyStoreProvider.getPolicyStore(context).update(resourceId, policy));
            return newResultPromise(result);
        } catch (EntitlementException ex) {
            DEBUG.error("PolicyResource :: UPDATE : Error updating policy, " + resourceId + ".", ex);
            return resourceErrorHandler.handleError(context, request, ex).asPromise();
        }
    }

    /**
     * Returns a resource based on the given policy.
     *
     * @param policy the policy to return a resource representation of.
     * @return the policy as a resource.
     * @throws EntitlementException if the policy cannot be serialised into JSON.
     */
    private ResourceResponse policyResource(Privilege policy) throws EntitlementException {
        return newResourceResponse(policy.getName(), policyRevision(policy), policyParser.printPolicy(policy));
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
