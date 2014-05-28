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

import java.util.List;

/**
 * A policy request that takes a single resource and is used to render policy decisions
 * for that resource and for all policies that match sub-resources of that resource.
 *
 * @since 12.0.0
 */
public final class TreePolicyRequest extends PolicyRequest {

    private final static String RESOURCE = "resource";

    private final String resource;

    private TreePolicyRequest(final TreePolicyRequestBuilder builder) {
        super(builder);
        resource = builder.resource;
    }

    public String getResource() {
        return resource;
    }

    @Override
    public List<Entitlement> dispatch(final PolicyEvaluator evaluator) throws EntitlementException {
        return evaluator.evaluateTree(this);
    }

    /**
     * Builder used to build tree policy requests.
     */
    private static final class TreePolicyRequestBuilder extends PolicyRequestBuilder<TreePolicyRequest> {

        private final String resource;

        private TreePolicyRequestBuilder(final ServerContext context,
                                         final ActionRequest request) throws EntitlementException {
            super(context, request);
            final JsonValue jsonValue = request.getContent();
            Reject.ifNull(jsonValue);
            resource = getResource(jsonValue);
        }

        private String getResource(final JsonValue value) throws EntitlementException {
            final String resource = value.get(RESOURCE).asString();

            if (resource == null || resource.isEmpty()) {
                // Protected resource is required.
                throw new EntitlementException(EntitlementException.INVALID_VALUE, new Object[]{RESOURCE});
            }

            return resource;
        }

        @Override
        TreePolicyRequest build() {
            return new TreePolicyRequest(this);
        }

    }

    /**
     * Gets a tree policy request based on the context and action request.
     *
     * @param context
     *         the context
     * @param request
     *         the request
     *
     * @return a tree policy request
     *
     * @throws EntitlementException
     *         should creating a tree policy request fail
     */
    public static TreePolicyRequest getTreePolicyRequest(final ServerContext context,
                                                         final ActionRequest request) throws EntitlementException {
        return new TreePolicyRequestBuilder(context, request).build();
    }


}
