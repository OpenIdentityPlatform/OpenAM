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
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import javax.inject.Inject;
import javax.inject.Named;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * Filter which ensures that only users with an SSO Token which has Administrator-level access are allowed access
 * to the resources protected.
 */
public class AdminOnlyAuthzModule implements CrestAuthorizationModule {

    public static final String NAME = "AdminOnlyFilter";

    private final Config<SessionService> sessionService;
    protected final Debug debug;

    @Inject
    public AdminOnlyAuthzModule(Config<SessionService> sessionService, @Named("frRest") Debug debug) {
        this.sessionService = sessionService;
        this.debug = debug;
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeCreate(ServerContext context, CreateRequest request) {
        return authorize(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeRead(ServerContext context, ReadRequest request) {
        return authorize(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeUpdate(ServerContext context, UpdateRequest request) {
        return authorize(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeDelete(ServerContext context, DeleteRequest request) {
        return authorize(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizePatch(ServerContext context, PatchRequest request) {
        return authorize(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(ServerContext context, ActionRequest request) {
        return authorize(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeQuery(ServerContext context, QueryRequest request) {
        return authorize(context);
    }

    /**
     * Lets through any request which is coming from a verifiable administrator.
     */
    Promise<AuthorizationResult, ResourceException> authorize(ServerContext context) {

        SSOTokenContext tokenContext = context.asContext(SSOTokenContext.class);

        String userId;
        try {
            SSOToken token = tokenContext.getCallerSSOToken();
            userId = token.getProperty(Constants.UNIVERSAL_IDENTIFIER);
        } catch (SSOException e) {
            if (debug.errorEnabled()) {
                debug.error("AdminOnlyAuthZModule :: Unable to authorize as Admin user using SSO Token.", e);
            }
            return Promises.newFailedPromise(ResourceException
                    .getException(ResourceException.FORBIDDEN, e.getMessage(), e));
        }

        if (sessionService.get().isSuperUser(userId)) {
            if (debug.messageEnabled()) {
                debug.message("AdminOnlyAuthZModule :: User, " + userId + " accepted as Administrator.");
            }
            return Promises.newSuccessfulPromise(AuthorizationResult.success());
        } else {
            if (debug.messageEnabled()) {
                debug.message("AdminOnlyAuthZModule :: Restricted access to " + userId);
            }
            return Promises.newSuccessfulPromise(AuthorizationResult.failure("User is not an administrator."));
        }

    }
}
