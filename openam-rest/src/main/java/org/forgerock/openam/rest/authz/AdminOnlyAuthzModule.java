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
* Copyright 2014-2015 ForgeRock AS.
*/

package org.forgerock.openam.rest.authz;

import javax.inject.Inject;
import javax.inject.Named;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.Context;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * Filter which ensures that only users with an SSO Token which has Administrator-level access are allowed access
 * to the resources protected.
 */
public class AdminOnlyAuthzModule extends SSOTokenAuthzModule {

    public static final String NAME = "AdminOnlyFilter";

    private final Config<SessionService> sessionService;

    @Inject
    public AdminOnlyAuthzModule(Config<SessionService> sessionService, @Named("frRest") Debug debug) {
        super(debug);
        this.sessionService = sessionService;
    }

    @Override
    public String getName() {
        return NAME;
    }


    @Override
    protected Promise<AuthorizationResult, ResourceException> validateToken(Context context, SSOToken token) throws SSOException, ResourceException {
        String userId = getUserId(token);
        if (userId != null && isSuperUser(userId)) {
            debug.message("AdminOnlyAuthZModule :: User, {} accepted as Administrator.", userId);
            return Promises.newResultPromise(AuthorizationResult.accessPermitted());
        } else {
            debug.message("AdminOnlyAuthZModule :: Restricted access to {}", userId);
            return Promises.newResultPromise(AuthorizationResult.accessDenied("User is not an administrator."));
        }
    }

    protected static String getUserId(SSOToken token) throws SSOException {
        return token == null ? null : token.getProperty(Constants.UNIVERSAL_IDENTIFIER);
    }

    protected boolean isSuperUser(String userId) {
        return sessionService.get().isSuperUser(userId);
    }
}
