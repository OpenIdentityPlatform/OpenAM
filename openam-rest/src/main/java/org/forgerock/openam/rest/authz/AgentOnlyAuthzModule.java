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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
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
import org.forgerock.openam.forgerockrest.utils.AgentIdentity;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.HttpURLConnection;

/**
 * Authorization module that only grants access to agents (e.g. web agent, J2EE agent, SOAP STS).
 *
 * @since 13.0.0
 */
public class AgentOnlyAuthzModule implements CrestAuthorizationModule {

    public static final String NAME = "AgentOnlyFilter";

    protected final AgentIdentity agentIdentity;
    protected final Debug debug;

    @Inject
    public AgentOnlyAuthzModule(AgentIdentity agentIdentity, @Named("frRest") Debug debug) {
        this.agentIdentity = agentIdentity;
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

    private Promise<AuthorizationResult, ResourceException> authorize(ServerContext context) {
        SSOTokenContext tokenContext = context.asContext(SSOTokenContext.class);
        String userId;
        try {
            SSOToken token = tokenContext.getCallerSSOToken();
            userId = token.getPrincipal().getName();
            if (agentIdentity.isAgent(token)) {
                if (debug.messageEnabled()) {
                    debug.message("AgentOnlyAuthzModule :: User, " + userId + " accepted as Agent user.");
                }
                return Promises.newResultPromise(AuthorizationResult.accessPermitted());
            } else {
                if (debug.warningEnabled()) {
                    debug.warning("AgentUserOnlyAuthzModule :: Denied access to " + userId);
                }
                return Promises.newResultPromise(AuthorizationResult.accessDenied("User is not an Agent."));
            }
        } catch (SSOException e) {
            if (debug.messageEnabled()) {
                debug.message("AgentOnlyAuthzModule :: Unable to authorize as Agent user using SSO Token.", e);
            }
            return Promises.newExceptionPromise(ResourceException
                    .getException(HttpURLConnection.HTTP_UNAUTHORIZED, e.getMessage(), e));
        }
    }
}