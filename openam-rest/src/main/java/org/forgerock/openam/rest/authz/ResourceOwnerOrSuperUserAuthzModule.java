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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.rest.authz;

import static org.forgerock.util.promise.Promises.newSuccessfulPromise;

import javax.inject.Inject;
import javax.inject.Named;

import java.security.AccessController;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.promise.Promise;

/**
 * Authorization module which will only allow the logged in user to see their own resources
 * or an administrator to see any users resources.
 */
public class ResourceOwnerOrSuperUserAuthzModule extends AdminOnlyAuthzModule {

    public static final String NAME = "ResourceOwnerOrSuperUserAuthzModule";

    private final SSOToken adminToken;

    @Inject
    public ResourceOwnerOrSuperUserAuthzModule(Config<SessionService> sessionService, @Named("frRest") Debug debug) {
        super(sessionService, debug);
        adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
    }
    
    @Override
    protected Promise<AuthorizationResult, ResourceException> validateToken(Context context, SSOToken token)
            throws SSOException, ResourceException {
        String loggedInUserId = getUserId(token);
        boolean ignoreCase = SystemProperties.getAsBoolean(Constants.CASE_SENSITIVE_UUID);
        if (isSuperUser(loggedInUserId)) {
            debug.message("{} :: User, {} accepted as Super user", NAME, loggedInUserId);
            return newSuccessfulPromise(AuthorizationResult.accessPermitted());
        } else if (ignoreCase ? loggedInUserId.equalsIgnoreCase(getUserIdFromUri(context))
                : loggedInUserId.equalsIgnoreCase(getUserIdFromUri(context))) {
            debug.message("{} :: User, {} accepted as Resource Owner", NAME, loggedInUserId);
            return newSuccessfulPromise(AuthorizationResult.accessPermitted());
        } else {
            debug.warning("{} :: Denied access to {}", NAME, loggedInUserId);
            return newSuccessfulPromise(AuthorizationResult.accessDenied("User, " + loggedInUserId
                    + ", not authorized."));
        }
    }

    protected String getUserIdFromUri(Context context) throws InternalServerErrorException {
        String username = context.asContext(RouterContext.class).getUriTemplateVariables().get("user");
        String realm = context.asContext(RealmContext.class).getResolvedRealm();
        return new AMIdentity(adminToken, username, IdType.USER, realm, null).getUniversalId();
    }
}
