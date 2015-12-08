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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.rest.authz;

import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.forgerock.util.promise.Promises.*;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.RestConstants;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;

import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationPermissionFactory;

/**
 * An authorization module that permits a privileged user from the requested realm or a parent read and write access,
 * and permits any other privileged user read-only access.
 *
 * @since 13.0.0
 */
public class PrivilegeWriteAndAnyPrivilegeReadOnlyAuthzModule extends PrivilegeAuthzModule {

    private static final Set<String> READ_ONLY_ACTIONS = asSet(RestConstants.SCHEMA, RestConstants.TEMPLATE);

    private final AnyPrivilegeAuthzModule anyPrivilegeAuthzModule;
    @Inject
    public PrivilegeWriteAndAnyPrivilegeReadOnlyAuthzModule(DelegationEvaluator evaluator,
            Map<String, PrivilegeDefinition> actionToDefinition, DelegationPermissionFactory permissionFactory,
            SessionCache sessionCache, CoreWrapper coreWrapper, AnyPrivilegeAuthzModule anyPrivilegeAuthzModule) {
        super(evaluator, actionToDefinition, permissionFactory, sessionCache, coreWrapper);
        this.anyPrivilegeAuthzModule = anyPrivilegeAuthzModule;
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(Context context, ActionRequest request) {
        if (READ_ONLY_ACTIONS.contains(request.getAction())) {
            return evaluateReadOnly(context);
        } else {
            return super.authorizeAction(context, request);
        }
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeRead(Context context, ReadRequest request) {
        return evaluateReadOnly(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeQuery(Context context, QueryRequest request) {
        return evaluateReadOnly(context);
    }

    private Promise<AuthorizationResult, ResourceException> evaluateReadOnly(Context context) {
        return super.evaluate(context, READ).thenAsync(new PermitAnyPrivilegedRead(anyPrivilegeAuthzModule, context));
    }

    private static class PermitAnyPrivilegedRead
            implements AsyncFunction<AuthorizationResult, AuthorizationResult, ResourceException> {

        private final AnyPrivilegeAuthzModule anyPrivilegeAuthzModule;
        private final Context context;

        private PermitAnyPrivilegedRead(AnyPrivilegeAuthzModule anyPrivilegeAuthzModule, Context context) {
            this.anyPrivilegeAuthzModule = anyPrivilegeAuthzModule;
            this.context = context;
        }

        @Override
        public Promise<? extends AuthorizationResult, ? extends ResourceException> apply(
                AuthorizationResult result) throws ResourceException {
            Promise<AuthorizationResult, ResourceException> resultPromise = newResultPromise(result);
            return result.isAuthorized() ? resultPromise : anyPrivilegeAuthzModule.evaluate(context, READ);
        }
    }

}
