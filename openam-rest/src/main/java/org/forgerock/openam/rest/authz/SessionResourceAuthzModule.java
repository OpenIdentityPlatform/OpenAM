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
package org.forgerock.openam.rest.authz;

import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.shared.debug.Debug;
import javax.inject.Inject;
import javax.inject.Named;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.forgerockrest.session.SessionResource;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * Authorization module specifically designed for the Sessions Resource endpoint. This allows anonymous access
 * to the Sessions Endpoint for the ACTIONS of 'logout' and 'validate'. All other endpoint requests are
 * pushed up to the {@link AdminOnlyAuthzModule}.
 */
public class SessionResourceAuthzModule extends AdminOnlyAuthzModule {

    public final static String NAME = "SessionResourceFilter";

    @Inject
    public SessionResourceAuthzModule(Config<SessionService> sessionService, @Named("frRest") Debug debug) {
        super(sessionService, debug);
    }

    /**
     * Lets through requests known to {@link SessionResource}.... otherwise it defers to
     * {@link AdminOnlyAuthzModule}.
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(ServerContext context, ActionRequest request) {

        if (actionCanBeInvokedByNonAdmin(request.getAction())) {
            if (debug.messageEnabled()) {
                debug.message("SessionResourceAuthzModule :: " + request.getAction() +
                        " action request authorized by module.");
            }
            return Promises.newSuccessfulPromise(AuthorizationResult.success());
        }

        if (debug.messageEnabled()) {
            debug.message("SessionResourceAuthzModule :: Request forwarded to AdminOnlyAuthzModule.");
        }
        return super.authorize(context);
    }

    private boolean actionCanBeInvokedByNonAdmin(String actionId) {
        return SessionResource.VALIDATE_ACTION_ID.equalsIgnoreCase(actionId) ||
                SessionResource.LOGOUT_ACTION_ID.equalsIgnoreCase(actionId);
    }
}
