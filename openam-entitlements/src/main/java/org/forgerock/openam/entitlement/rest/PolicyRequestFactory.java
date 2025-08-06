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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.entitlement.rest;

import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.services.context.Context;
import org.forgerock.openam.entitlement.rest.model.json.BatchPolicyRequest;
import org.forgerock.openam.entitlement.rest.model.json.PolicyRequest;
import org.forgerock.openam.entitlement.rest.model.json.TreePolicyRequest;
import org.forgerock.util.Reject;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

/**
 * Factory used for creating policy requests.
 *
 * @since 12.0.0
 */
public class PolicyRequestFactory {

    private final Map<PolicyAction, RequestBuilder<?>> builders;

    @Inject
    public PolicyRequestFactory(final SSOTokenManager tokenManager) {
        builders = new HashMap<PolicyAction, RequestBuilder<?>>();
        // Add batch policy request builder.
        builders.put(PolicyAction.EVALUATE, new RequestBuilder<BatchPolicyRequest>() {

            @Override
            public BatchPolicyRequest buildRequest(final Context context,
                                                   final ActionRequest request) throws EntitlementException {
                return BatchPolicyRequest.getBatchPolicyRequest(context, request, tokenManager);
            }

        });
        // Add tree policy request builder.
        builders.put(PolicyAction.TREE_EVALUATE, new RequestBuilder<TreePolicyRequest>() {

            @Override
            public TreePolicyRequest buildRequest(final Context context,
                                                  final ActionRequest request) throws EntitlementException {
                return TreePolicyRequest.getTreePolicyRequest(context, request, tokenManager);
            }

        });
    }

    /**
     * Builds a request for the given action type.
     *
     * @param action
     *         the non-null valid action type
     * @param context
     *         the context
     * @param request
     *         the request
     *
     * @return a new policy request instance
     *
     * @throws EntitlementException
     *         should building the request fail
     */
    public PolicyRequest buildRequest(final PolicyAction action, final Context context,
                                      final ActionRequest request) throws EntitlementException {
        Reject.ifNull(action);

        if (!builders.containsKey(action)) {
            throw new EntitlementException(EntitlementException.UNSUPPORTED_OPERATION);
        }

        return builders.get(action).buildRequest(context, request);
    }

    /**
     * Responsible for building policy requests.
     *
     * @param <T>
     *         the policy request type
     */
    private static interface RequestBuilder<T extends PolicyRequest> {

        /**
         * Build the policy request.
         *
         * @param context
         *         the context
         * @param request
         *         the action request
         *
         * @return a new policy request instance
         *
         * @throws EntitlementException
         *         should building the request fail
         */
        public T buildRequest(final Context context,
                              final ActionRequest request) throws EntitlementException;

    }

}
