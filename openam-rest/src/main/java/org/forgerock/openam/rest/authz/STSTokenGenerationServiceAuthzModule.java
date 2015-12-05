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

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.utils.AgentIdentity;
import org.forgerock.openam.forgerockrest.utils.SpecialUserIdentity;
import org.forgerock.openam.utils.Config;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;

/**
 * This CrestAuthorizationModule protects the token generation service. It limits consumption to action invocations
 * made only by SSOTokens corresponding to the 'special' user (to authZ rest-sts consumption), and corresponding to
 * Soap STS agents (to authZ soap-sts consumption), and to Admins. Used to protect the STS' token service.
 */
public class STSTokenGenerationServiceAuthzModule extends SpecialAndAdminUserOnlyAuthzModule  {
    public static final String NAME = "STSTokenGenerationServiceAuthzModule";

    private final AgentIdentity agentIdentity;

    @Inject
    public STSTokenGenerationServiceAuthzModule(Config<SessionService> sessionService, AgentIdentity agentIdentity,
                                                SpecialUserIdentity specialUserIdentity, @Named("frRest") Debug debug) {
        super(sessionService, specialUserIdentity, debug);
        this.agentIdentity = agentIdentity;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeUpdate(Context context, UpdateRequest request) {
        return rejectConsumption();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizePatch(Context context, PatchRequest request) {
        return rejectConsumption();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(Context context, ActionRequest request) {
        return rejectConsumption();
    }

    private Promise<AuthorizationResult, ResourceException> rejectConsumption() {
        return Promises.newResultPromise(AuthorizationResult.accessDenied("TokenGenerationServiceAuthzModule: " +
                "invoked functionality is not authorized for any user."));
    }

    @Override
    protected Promise<AuthorizationResult, ResourceException> validateToken(Context context, SSOToken token)
            throws SSOException, ResourceException {
        String userId = token.getPrincipal().getName();
        if (agentIdentity.isSoapSTSAgent(token)) {
            if (debug.messageEnabled()) {
                debug.message("{} :: User, {} accepted as Soap STS Agent", moduleName, userId);
            }
            return Promises.newResultPromise(AuthorizationResult.accessPermitted());
        } else {
            return super.validateToken(context, token);
        }
    }

}