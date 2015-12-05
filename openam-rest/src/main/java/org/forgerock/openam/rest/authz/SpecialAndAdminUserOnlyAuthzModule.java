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

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ResourceException;
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
 * This AuthzModule checks for the presence of the user corresponding to the token returned from
 * AccessController.doPrivileged(AdminTokenAction.getInstance()), or other special user, as evaluated by
 * AuthD#isSpecialUser. If that authZ fails, it delegates to the AdminOnlyAuthzModule. This class is subclassed by
 * the STSTokenGenerationServiceAuthzModule, which needs to provide access to special users (for rest-sts consumption),
 * to soap-sts-agent users (for soap-sts consumption), and to Admins (so that tokens can be queried and cancelled).
 */
public class SpecialAndAdminUserOnlyAuthzModule extends AdminOnlyAuthzModule {
    public static final String NAME = "SpecialUserOnlyFilter";

    protected SpecialUserIdentity specialUserIdentity;

    @Inject
    public SpecialAndAdminUserOnlyAuthzModule(Config<SessionService> sessionService, SpecialUserIdentity specialUserIdentity, @Named("frRest") Debug debug) {
        super(sessionService, debug);
        this.specialUserIdentity = specialUserIdentity;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected Promise<AuthorizationResult, ResourceException> validateToken(Context context, SSOToken token) throws SSOException, ResourceException {
        String userId = token.getPrincipal().getName();
        if (specialUserIdentity.isSpecialUser(token)) {
            if (debug.messageEnabled()) {
                debug.message("{} :: User, {} accepted as Special user", moduleName, userId);
            }
            return Promises.newResultPromise(AuthorizationResult.accessPermitted());
        } else {
            return super.validateToken(context, token);
        }
    }

}
