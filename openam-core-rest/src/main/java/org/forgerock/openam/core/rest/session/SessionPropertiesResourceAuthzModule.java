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

package org.forgerock.openam.core.rest.session;

import javax.inject.Inject;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * Authorization module specifically designed for the Sessions Properties Resource endpoint.
 * This allows access to the Sessions Endpoint for GET,PATCH and UPDATE.
 * All other endpoint requests are denied.
 */
public class SessionPropertiesResourceAuthzModule extends TokenOwnerAuthzModule {

    public final static String NAME = "SessionPropertiesResourceFilter";

    @Inject
    public SessionPropertiesResourceAuthzModule(SSOTokenManager ssoTokenManager) {
        super("tokenId", ssoTokenManager);
    }

    @Override
    public String getName() {
        return NAME;
    }


    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(Context context, ActionRequest request) {
        return new ForbiddenException().asPromise();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeRead(Context context, ReadRequest request) {
        return authorizeRequest(context, request);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizePatch(Context context, PatchRequest request) {
        return authorizeRequest(context, request);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeUpdate(Context context, UpdateRequest request) {
        return authorizeRequest(context, request);
    }

    /**
     * Authorizes the incoming request by checking if the
     * session being accessed belongs to the authenticated user
     *
     * @param context The context.
     * @param request The request.
     * @return The Authiorization result promise.
     */
    private Promise<AuthorizationResult, ResourceException> authorizeRequest(Context context, Request request) {
        try {
            if (isTokenOwner(context, request)) {
                return Promises.newResultPromise(AuthorizationResult.accessPermitted());
            }
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (SSOException e) {
            return new ForbiddenException().asPromise();
        }
        return new ForbiddenException().asPromise();
    }
}