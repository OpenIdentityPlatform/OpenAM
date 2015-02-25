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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.entitlements.model.json;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.forgerockrest.entitlements.PolicyEvaluator;
import org.forgerock.util.Reject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A policy request that takes a batch of resources and is used to render a policy decision for each.
 *
 * @since 12.0.0
 */
public final class BatchPolicyRequest extends PolicyRequest {

    private final static String RESOURCES = "resources";

    private final Set<String> resources;

    private BatchPolicyRequest(final BatchPolicyRequestBuilder builder) {
        super(builder);
        resources = builder.resources;
    }

    public Set<String> getResources() {
        return resources;
    }

    @Override
    public List<Entitlement> dispatch(final PolicyEvaluator evaluator) throws EntitlementException {
        return evaluator.evaluateBatch(this);
    }

    /**
     * Builder used to build batch policy requests.
     */
    private static final class BatchPolicyRequestBuilder extends PolicyRequestBuilder<BatchPolicyRequest> {

        private final Set<String> resources;

        private BatchPolicyRequestBuilder(final ServerContext context,
                                          final ActionRequest request) throws EntitlementException {
            super(context, request);
            final JsonValue jsonValue = request.getContent();
            Reject.ifNull(jsonValue);
            resources = getResources(jsonValue);
        }

        private Set<String> getResources(final JsonValue value) throws EntitlementException {
            final List<String> resources = value.get(RESOURCES).asList(String.class);

            if (resources == null || resources.isEmpty()) {
                // Protected resources are required.
                throw new EntitlementException(EntitlementException.INVALID_VALUE, new Object[]{RESOURCES});
            }

            return new HashSet<String>(resources);
        }

        @Override
        BatchPolicyRequest build() {
            return new BatchPolicyRequest(this);
        }

    }

    /**
     * Gets a batch policy request based on the context and action request.
     *
     * @param context
     *         the context
     * @param request
     *         the request
     *
     * @return a batch policy request
     *
     * @throws EntitlementException
     *         should creating a batch policy request fail
     */
    public static BatchPolicyRequest getBatchPolicyRequest(final ServerContext context,
                                                           final ActionRequest request) throws EntitlementException {
        return new BatchPolicyRequestBuilder(context, request).build();
    }

}
