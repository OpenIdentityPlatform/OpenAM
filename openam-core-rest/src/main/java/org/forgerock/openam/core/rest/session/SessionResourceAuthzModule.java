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

package org.forgerock.openam.core.rest.session;

import jakarta.inject.Inject;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * Authorization module specifically designed for the Sessions Resource endpoint. This allows anonymous access
 * to the Sessions Endpoint for the ACTIONS of 'logout' and 'validate'. All other endpoint requests are denied.
 */
public class SessionResourceAuthzModule extends TokenOwnerAuthzModule {

    public final static String NAME = "SessionResourceFilter";

    @Inject
    public SessionResourceAuthzModule(SSOTokenManager ssoTokenManager) {
        super("tokenId", ssoTokenManager,
                SessionResource.DELETE_PROPERTY_ACTION_ID, SessionResource.GET_PROPERTY_ACTION_ID,
                SessionResource.GET_PROPERTY_NAMES_ACTION_ID, SessionResource.SET_PROPERTY_ACTION_ID,
                SessionResource.GET_TIME_LEFT_ACTION_ID, SessionResource.GET_MAX_IDLE_ACTION_ID,
                SessionResourceV2.GET_SESSION_INFO_ACTION_ID, SessionResourceV2.GET_SESSION_PROPERTIES_ACTION_ID,
                SessionResourceV2.UPDATE_SESSION_PROPERTIES_ACTION_ID);
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Lets through requests known to {@link SessionResource}.
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(Context context, ActionRequest request) {

        if (actionCanBeInvokedByNonAdmin(request.getAction())) {
            return Promises.newResultPromise(AuthorizationResult.accessPermitted());
        }

        return super.authorizeAction(context, request);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeRead(Context context, ReadRequest request) {
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

    private boolean actionCanBeInvokedByNonAdmin(String actionId) {
        return SessionResource.VALIDATE_ACTION_ID.equalsIgnoreCase(actionId) ||
                SessionResource.LOGOUT_ACTION_ID.equalsIgnoreCase(actionId);
    }
}
