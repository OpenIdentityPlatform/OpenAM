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
import java.util.Map;
import javax.inject.Inject;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

public class SessionResourceAuthzModule extends AdminOnlyAuthzModule {

    @Inject
    public SessionResourceAuthzModule(Config<SessionService> sessionService) {
        super(sessionService);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeCreate(ServerContext context, CreateRequest request) {
        return authorize(request, context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeRead(ServerContext context, ReadRequest request) {
        return authorize(request, context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeUpdate(ServerContext context, UpdateRequest request) {
        return authorize(request, context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeDelete(ServerContext context, DeleteRequest request) {
        return authorize(request, context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizePatch(ServerContext context, PatchRequest request) {
        return authorize(request, context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(ServerContext context, ActionRequest request) {
        return authorize(request, context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeQuery(ServerContext context, QueryRequest request) {
        return authorize(request, context);
    }

    /**
     * Lets through requests to ?_action=logout and ?_action=validate, otherwise defers to
     * {@link AdminOnlyAuthzModule}.
     */
    Promise<AuthorizationResult, ResourceException> authorize(Request request, ServerContext context) {

        Map<String, String> parameterMap = request.getAdditionalParameters();

        if (parameterMap != null && parameterMap.containsKey("_action")) {
            String value = parameterMap.get("_action");
            if ("logout".equals(value) || "validate".equals(value)) {
                return Promises.newSuccessfulPromise(AuthorizationResult.success());
            }
        }

        return super.authorize(context);
    }
}
