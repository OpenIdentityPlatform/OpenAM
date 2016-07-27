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
package org.forgerock.openam.http.authz;

import static org.forgerock.authz.filter.api.AuthorizationResult.accessDenied;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.authz.filter.api.AuthorizationContext;
import org.forgerock.authz.filter.api.AuthorizationException;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.http.api.HttpAuthorizationModule;
import org.forgerock.http.protocol.Request;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.authz.PrivilegeAuthzModule;
import org.forgerock.openam.authz.PrivilegeDefinition;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationPermissionFactory;

/**
 * An HTTP authorization module for performing privilege checking, {@see PrivilegeAuthzModule} for more detail.
 *
 * @since 14.0.0
 */
public class HttpPrivilegeAuthzModule extends PrivilegeAuthzModule implements HttpAuthorizationModule {

    /**
     * Create a new instance of {@link HttpAuthorizationModule}.
     *
     * @param evaluator The Delegation Evaluator.
     * @param actionToDefinition The action to definition map.
     * @param permissionFactory The Delegation Permission Factory.
     * @param coreWrapper The Core Wrapper.
     * @param ssoTokenManager The SSOToken manager.
     */
    @Inject
    public HttpPrivilegeAuthzModule(DelegationEvaluator evaluator,
            @Named("HttpPrivilegeDefinitions") Map<String, PrivilegeDefinition> actionToDefinition,
            DelegationPermissionFactory permissionFactory, CoreWrapper coreWrapper, SSOTokenManager ssoTokenManager) {
        super(evaluator, actionToDefinition, permissionFactory, coreWrapper, ssoTokenManager);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Promise<AuthorizationResult, AuthorizationException> authorize(Context context, Request request,
            AuthorizationContext authorizationContext) {
        String method = request.getMethod();
        PrivilegeDefinition definition = actionToDefinition.get(method);
        if (definition == null) {
            return newResultPromise(accessDenied("No privilege mapping for requested method " + method));
        }

        try {
            return newResultPromise(evaluate(context, definition));
        } catch (InternalServerErrorException e) {
            return Promises.newExceptionPromise(new AuthorizationException(e.getMessage(), e));
        }
    }
}
