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
 */

package org.forgerock.openam.rest.authz;

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
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.utils.SoapSTSAgentIdentity;
import org.forgerock.openam.forgerockrest.utils.SpecialUserIdentity;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.HttpURLConnection;

/**
 * This CrestAuthorizationModule protects the token generation service. It limits consumption to action invocations
 * made only by SSOTokens corresponding to the 'special' user (to authZ rest-sts consumption), and corresponding to
 * Soap STS agents (to authZ soap-sts consumption).
 */
public class STSTokenGenerationServiceAuthzModule extends SpecialUserOnlyAuthzModule  {
    public static final String NAME = "STSTokenGenerationServiceAuthzModule";

    private final SoapSTSAgentIdentity agentIdentity;

    @Inject
    public STSTokenGenerationServiceAuthzModule(SoapSTSAgentIdentity agentIdentity, SpecialUserIdentity specialUserIdentity,
                                                @Named("frRest") Debug debug) {
        super(specialUserIdentity, debug);
        this.agentIdentity = agentIdentity;
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeCreate(ServerContext context, CreateRequest request) {
        return rejectConsumption();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeRead(ServerContext context, ReadRequest request) {
        return rejectConsumption();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeUpdate(ServerContext context, UpdateRequest request) {
        return rejectConsumption();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeDelete(ServerContext context, DeleteRequest request) {
        return rejectConsumption();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizePatch(ServerContext context, PatchRequest request) {
        return rejectConsumption();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(ServerContext context, ActionRequest request) {
        return authorize(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeQuery(ServerContext context, QueryRequest request) {
        return rejectConsumption();
    }

    private Promise<AuthorizationResult, ResourceException> rejectConsumption() {
        return Promises.newSuccessfulPromise(AuthorizationResult.accessDenied("TokenGenerationServiceAuthzModule: " +
                "invoked functionality is not authorized for any user."));
    }

    Promise<AuthorizationResult, ResourceException> authorize(ServerContext context) {
        SSOTokenContext tokenContext = context.asContext(SSOTokenContext.class);
        String userId;
        SSOToken token;
        try {
            token = tokenContext.getCallerSSOToken();
            userId = token.getPrincipal().getName();
        } catch (SSOException e) {
            if (debug.messageEnabled()) {
                debug.message("TokenGenerationServiceAuthzModule :: Unable to obtain SSOToken or principal", e);
            }
            return Promises.newFailedPromise(ResourceException
                    .getException(HttpURLConnection.HTTP_UNAUTHORIZED, e.getMessage(), e));
        }

        if (agentIdentity.isSoapSTSAgent(token)) {
            if (debug.messageEnabled()) {
                debug.message("TokenGenerationServiceAuthzModule :: User, " + userId + " accepted as Soap STS Agent.");
            }
            return Promises.newSuccessfulPromise(AuthorizationResult.accessPermitted());
        } else {
            return super.authorize(context);
        }
    }
}