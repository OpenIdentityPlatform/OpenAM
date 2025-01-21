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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.rest.authz;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.utils.AgentIdentity;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.net.HttpURLConnection;

/**
 *  This is an authz module specific for the STS publish service. It will allow admins and soap sts agents to
 *  read state corresponding to published sts instances, and allow admins to create, delete, and update new sts instances.
 */
public class STSPublishServiceAuthzModule extends AdminOnlyAuthzModule {
    public static final String NAME = "STSPublishServiceAuthzModule";
    private final AgentIdentity agentIdentity;

    @Inject
    public STSPublishServiceAuthzModule(Config<SessionService> sessionService, AgentIdentity agentIdentity,
                                        @Named("frRest") Debug debug) {
        super(sessionService, debug);
        this.agentIdentity = agentIdentity;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeCreate(Context context, CreateRequest request) {
        return authorizeAdmin(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeRead(Context context, ReadRequest request) {
        return authorizeSoapSTSAgentOrAdmin(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeUpdate(Context context, UpdateRequest request) {
        return authorizeAdmin(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeDelete(Context context, DeleteRequest request) {
        return authorizeAdmin(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizePatch(Context context, PatchRequest request) {
        return rejectConsumption();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(Context context, ActionRequest request) {
        return rejectConsumption();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeQuery(Context context, QueryRequest request) {
        return authorizeSoapSTSAgentOrAdmin(context);
    }

    private Promise<AuthorizationResult, ResourceException> rejectConsumption() {
        return Promises.newResultPromise(AuthorizationResult.accessDenied("STSPublishServiceAuthzModule: " +
                "invoked functionality is not authorized for any user."));
    }

    Promise<AuthorizationResult, ResourceException> authorizeSoapSTSAgentOrAdmin(Context context) {
        try {
            if (isSoapSTSAgent(context)) {
                return Promises.newResultPromise(AuthorizationResult.accessPermitted());
            } else {
                return authorizeAdmin(context);
            }
        } catch (ResourceException e) {
            return ResourceException.getException(HttpURLConnection.HTTP_UNAUTHORIZED, e.getMessage(), e).asPromise();
        }
    }

    Promise<AuthorizationResult, ResourceException> authorizeAdmin(Context context) {
        return super.authorize(context);
    }

    private boolean isSoapSTSAgent(Context context) throws ResourceException{
        SSOTokenContext tokenContext = context.asContext(SSOTokenContext.class);
        String userId;
        SSOToken token;
        try {
            token = tokenContext.getCallerSSOToken();
            userId = token.getPrincipal().getName();
        } catch (SSOException e) {
            if (debug.messageEnabled()) {
                debug.message("STSPublishServiceAuthzModule :: Unable to obtain SSOToken or principal", e);
            }
            throw ResourceException.getException(HttpURLConnection.HTTP_UNAUTHORIZED, e.getMessage());
        }

        if (agentIdentity.isSoapSTSAgent(token)) {
            if (debug.messageEnabled()) {
                debug.message("STSPublishServiceAuthzModule :: User " + userId + " accepted as Soap STS Agent.");
            }
            return true;
        } else {
            if (debug.messageEnabled()) {
                debug.message("STSPublishServiceAuthzModule :: User " + userId + " is not a Soap STS Agent.");
            }
            return false;
        }
    }
}
