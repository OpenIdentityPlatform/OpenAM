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

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.forgerockrest.utils.AgentIdentity;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;

/**
 * Authorization module that only grants access to agents (e.g. web agent, J2EE agent, SOAP STS).
 *
 * @since 13.0.0
 */
public class AgentOnlyAuthzModule extends SSOTokenAuthzModule {

    public static final String NAME = "AgentOnlyFilter";

    protected final AgentIdentity agentIdentity;

    @Inject
    public AgentOnlyAuthzModule(AgentIdentity agentIdentity, @Named("frRest") Debug debug) {
        super(debug);
        this.agentIdentity = agentIdentity;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected Promise<AuthorizationResult, ResourceException> validateToken(Context context, SSOToken token) throws SSOException {
        String userId = token.getPrincipal().getName();
        if (agentIdentity.isAgent(token)) {
            debug.message("AgentOnlyAuthzModule :: User, {} accepted as Agent user", userId);
            return Promises.newResultPromise(AuthorizationResult.accessPermitted());
        } else {
            debug.warning("AgentUserOnlyAuthzModule :: Denied access to {}", userId);
            return Promises.newResultPromise(AuthorizationResult.accessDenied("User is not an Agent."));
        }
    }
}