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

import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import java.util.concurrent.ExecutionException;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.utils.RestLog;
import org.forgerock.openam.forgerockrest.utils.ServerContextUtils;
import org.forgerock.util.promise.Promise;

/**
 * An Authorization module which performs no authorization itself, but wraps other authz
 * modules so that they write their success/failures out to an audit file.
 *
 * @since 12.0.0
 */
public class LoggingAuthzModule implements CrestAuthorizationModule {

    private final String moduleName;
    private final CrestAuthorizationModule module;
    private final RestLog restLog = InjectorHolder.getInstance(RestLog.class);
    private final Debug debug = Debug.getInstance("frRest");

    public LoggingAuthzModule(CrestAuthorizationModule module, String moduleName) {
        this.module = module;
        this.moduleName = moduleName;
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeCreate(ServerContext serverContext,
                                                                           CreateRequest createRequest) {
        final String resource = ServerContextUtils.getMatchedUri(serverContext);
        final String action = ServerContextUtils.getCreateString(createRequest);

        return log(resource, action, ServerContextUtils.getTokenFromContext(serverContext, debug),
                module.authorizeCreate(serverContext, createRequest), moduleName);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeRead(ServerContext serverContext,
                                                                         ReadRequest readRequest) {
        final String resource = ServerContextUtils.getMatchedUri(serverContext);
        final String action = ServerContextUtils.getReadString(readRequest);

        return log(resource, action, ServerContextUtils.getTokenFromContext(serverContext, debug),
                module.authorizeRead(serverContext, readRequest), moduleName);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeUpdate(ServerContext serverContext,
                                                                           UpdateRequest updateRequest) {
        final String resource = ServerContextUtils.getMatchedUri(serverContext);
        final String action = ServerContextUtils.getUpdateString(updateRequest);

        return log(resource, action, ServerContextUtils.getTokenFromContext(serverContext, debug),
                module.authorizeUpdate(serverContext, updateRequest), moduleName);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeDelete(ServerContext serverContext,
                                                                           DeleteRequest deleteRequest) {
        final String resource = ServerContextUtils.getMatchedUri(serverContext);
        final String action = ServerContextUtils.getDeleteString(deleteRequest);

        return log(resource, action, ServerContextUtils.getTokenFromContext(serverContext, debug),
                module.authorizeDelete(serverContext, deleteRequest), moduleName);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizePatch(ServerContext serverContext,
                                                                          PatchRequest patchRequest) {
        final String resource = ServerContextUtils.getMatchedUri(serverContext);
        final String action = ServerContextUtils.getPatchString(patchRequest);

        return log(resource, action, ServerContextUtils.getTokenFromContext(serverContext, debug),
                module.authorizePatch(serverContext, patchRequest), moduleName);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(ServerContext serverContext,
                                                                           ActionRequest actionRequest) {
        final String resource = ServerContextUtils.getMatchedUri(serverContext);
        final String action = ServerContextUtils.getActionString(actionRequest);

        return log(resource, action, ServerContextUtils.getTokenFromContext(serverContext, debug),
                module.authorizeAction(serverContext, actionRequest), moduleName);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeQuery(ServerContext serverContext,
                                                                          QueryRequest queryRequest) {
        final String resource = ServerContextUtils.getMatchedUri(serverContext);
        final String action = ServerContextUtils.getQueryString(queryRequest);

        return log(resource, action, ServerContextUtils.getTokenFromContext(serverContext, debug),
                module.authorizeQuery(serverContext, queryRequest), moduleName);
    }

    Promise<AuthorizationResult, ResourceException> log(String resource, String action, SSOToken token,
                                                        Promise<AuthorizationResult, ResourceException> result,
                                                        String authZModule) {
        try {
            if (!result.get().isAuthorized()) {
                restLog.auditAccessDenied(resource, action, authZModule, token);
            } else {
                restLog.auditAccessGranted(resource, action, authZModule, token);
            }
        } catch (ExecutionException e) {
            debug.message(e.getMessage());
        } catch (InterruptedException e) {
            debug.message(e.getMessage());
        }

        return result;
    }

}
