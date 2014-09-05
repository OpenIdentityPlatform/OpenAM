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
import javax.inject.Inject;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter which ensures that only users with an SSO Token which has Administrator-level access are allowed access
 * to the resources protected.
 */
public class AdminOnlyAuthzModule implements CrestAuthorizationModule {

    private final Logger logger = LoggerFactory.getLogger(AdminOnlyAuthzModule.class);

    private final Config<SessionService> sessionService;

    @Inject
    public AdminOnlyAuthzModule(Config<SessionService> sessionService) {
        this.sessionService = sessionService;
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

    Promise<AuthorizationResult, ResourceException> authorize(ServerContext context) {

        SSOTokenContext tokenContext = context.asContext(SSOTokenContext.class);

        String userId;
        try {
            SSOToken token = tokenContext.getCallerSSOToken();
            userId = token.getProperty(Constants.UNIVERSAL_IDENTIFIER);
        } catch (SSOException e) {
            logger.error("Unable to retrieve userId information from the SSOToken context.");
            return Promises.newFailedPromise(ResourceException
                    .getException(ResourceException.INTERNAL_ERROR, e.getMessage(), e));
        }

        if (sessionService.get().isSuperUser(userId)) {
            return Promises.newSuccessfulPromise(AuthorizationResult.success());
        } else {
            logger.warn("User " + userId + " attempted access but was restricted by AdminOnlyAuthzModule.");
            return Promises.newSuccessfulPromise(AuthorizationResult.failure("User is not an administrator."));
        }

    }
}
