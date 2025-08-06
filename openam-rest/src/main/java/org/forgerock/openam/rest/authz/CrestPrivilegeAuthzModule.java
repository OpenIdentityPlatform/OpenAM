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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.rest.authz;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.authz.PrivilegeAuthzModule;
import org.forgerock.openam.authz.PrivilegeDefinition;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationPermissionFactory;

/**
 * A CREST authorization module for performing privilege checking, {@see PrivilegeAuthzModule} for more detail.
 *
 * @since 14.0.0
 */
public class CrestPrivilegeAuthzModule extends PrivilegeAuthzModule implements CrestAuthorizationModule {

    /**
     * Create a new instance of {@link CrestPrivilegeAuthzModule}.
     *
     * @param evaluator The Delegation Evaluator.
     * @param actionToDefinition The action to definition map.
     * @param permissionFactory The Delegation Permission Factory.
     * @param coreWrapper The Core Wrapper.
     * @param ssoTokenManager The SSOToken manager.
     */
    @Inject
    public CrestPrivilegeAuthzModule(DelegationEvaluator evaluator,
            @Named("CrestPrivilegeDefinitions") Map<String, PrivilegeDefinition> actionToDefinition,
            DelegationPermissionFactory permissionFactory, CoreWrapper coreWrapper, SSOTokenManager ssoTokenManager) {
        super(evaluator, actionToDefinition, permissionFactory, coreWrapper, ssoTokenManager);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeRead(
            Context serverContext, ReadRequest readRequest) {
        return evaluateAsPromise(serverContext, READ);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeQuery(
            Context serverContext, QueryRequest queryRequest) {
        return evaluateAsPromise(serverContext, READ);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeCreate(
            Context serverContext, CreateRequest createRequest) {
        return evaluateAsPromise(serverContext, MODIFY);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeUpdate(
            Context serverContext, UpdateRequest updateRequest) {
        return evaluateAsPromise(serverContext, MODIFY);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeDelete(
            Context serverContext, DeleteRequest deleteRequest) {
        return evaluateAsPromise(serverContext, MODIFY);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizePatch(
            Context serverContext, PatchRequest patchRequest) {
        return evaluateAsPromise(serverContext, MODIFY);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(
            Context serverContext, ActionRequest actionRequest) {

        // Get the privilege definition for the CREST action.
        final String crestAction = actionRequest.getAction();
        final PrivilegeDefinition definition = actionToDefinition.get(crestAction);

        if (definition == null) {
            return Promises.newResultPromise(
                    AuthorizationResult.accessDenied("No privilege mapping for requested action " + crestAction));
        }

        return evaluateAsPromise(serverContext, definition);
    }

    Promise<AuthorizationResult, ResourceException> evaluateAsPromise(Context context, PrivilegeDefinition definition) {
        try {
            return Promises.newResultPromise(evaluate(context, definition));
        } catch (InternalServerErrorException e) {
            return e.asPromise();
        }
    }

}
