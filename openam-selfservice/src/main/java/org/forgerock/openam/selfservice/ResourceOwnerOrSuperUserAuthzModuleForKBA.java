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
 * Portions Copyrighted 2018 Open Source Solution Technology Corporation
 */

package org.forgerock.openam.selfservice;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.rest.authz.AdminOnlyAuthzModule;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.utils.Config;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.debug.Debug;

/**
 * Special filter for KBA(Knowledge based authentication) which ensures that
 * only users with an SSO Token which has Administrator-level access or the
 * owner of the resource are allowed to access the resources protected.
 */
public class ResourceOwnerOrSuperUserAuthzModuleForKBA extends AdminOnlyAuthzModule {

    /** Name used by the LoggingAuthzModule. */
    public static final String NAME = "ResourceOwnerOrSuperUserAuthzModuleForKBA";

    /**
     * Constructs a new ResourceOwnerOrSuperUserAuthzModuleForKBA instance.
     *
     * @param sessionService An instance of the SessionService.
     * @param debug An instance of the Rest Debugger.
     */
    @Inject
    public ResourceOwnerOrSuperUserAuthzModuleForKBA(Config<SessionService> sessionService, @Named("frRest") Debug debug) {
        super(sessionService, debug);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected Promise<AuthorizationResult, ResourceException> validateToken(Context context, SSOToken token)
            throws SSOException, ResourceException {
        String loggedInUserId = getUserId(token);
        if (isSuperUser(loggedInUserId)) {
            debug.message("{} :: User, {} accepted as Super user", moduleName, loggedInUserId);
            return Promises.newResultPromise(AuthorizationResult.accessPermitted());
        } else if (loggedInUserId.equalsIgnoreCase(getUserIdFromUri(context))) {
            debug.message("{} :: User, {} accepted as Resource Owner", moduleName, loggedInUserId);
            return Promises.newResultPromise(AuthorizationResult.accessPermitted());
        } else {
            debug.warning("{} :: Denied access to {}", moduleName, loggedInUserId);
            return Promises.newResultPromise(AuthorizationResult.accessDenied("User, " + loggedInUserId
                    + ", not authorized."));
        }
    }

    protected String getUserIdFromUri(Context context) throws InternalServerErrorException {
        String username = context.asContext(UriRouterContext.class).getRemainingUri();
        String realm = context.asContext(RealmContext.class).getResolvedRealm();
        if (username != null && realm != null) {
            AMIdentity theId = IdUtils.getIdentity(username, realm);
            if (theId != null) {
                return theId.getUniversalId();
            }
        }
        return null;
    }
}
