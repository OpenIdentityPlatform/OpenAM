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

import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 *
 *
 * @since 13.0.0
 */
public class ResourceOwnerOrSuperUserAuthzModule extends AdminOnlyAuthzModule {

    /** Name used by the LoggingAuthzModule. */
    public static final String NAME = "ResourceOwnerOrSuperUserAuthzModule";

    /**
     * Constructs a new ResourceOwnerOrSuperUserAuthzModule instance.
     *
     * @param sessionService An instance of the SessionService.
     * @param debug An instance of the Rest Debugger.
     */
    @Inject
    public ResourceOwnerOrSuperUserAuthzModule(Config<SessionService> sessionService, @Named("frRest") Debug debug) {
        super(sessionService, debug);
    }

    /**
     * Authorizes caller if they are either a super user or they are making a request to a resource they "own",
     * i.e. demo making a call to /json/users/demo/uma/resourceset.
     *
     * @param context The request context.
     * @return The authorization result.
     */
    @Override
    protected Promise<AuthorizationResult, ResourceException> authorize(ServerContext context) {

        try {
            String loggedInUserId = getUserId(context);
            if (isSuperUser(loggedInUserId)) {
                if (debug.messageEnabled()) {
                    debug.message("ResourceOwnerOrSuperUserAuthzModule :: User, " + loggedInUserId
                            + " accepted as Super user.");
                }
                return Promises.newSuccessfulPromise(AuthorizationResult.accessPermitted());
            } else if (loggedInUserId.equalsIgnoreCase(getUserIdFromUri(context))) {
                if (debug.messageEnabled()) {
                    debug.message("ResourceOwnerOrSuperUserAuthzModule :: User, " + loggedInUserId
                            + " accepted as Resource Owner.");
                }
                return Promises.newSuccessfulPromise(AuthorizationResult.accessPermitted());
            } else {
                if (debug.warningEnabled()) {
                    debug.warning("ResourceOwnerOrSuperUserAuthzModule :: Denied access to " + loggedInUserId);
                }
                return Promises.newSuccessfulPromise(AuthorizationResult.accessDenied("User, " + loggedInUserId
                        + ", not authorized."));
            }
        } catch (ResourceException e) {
            return Promises.newFailedPromise(e);
        }
    }

    protected String getUserIdFromUri(ServerContext context) throws InternalServerErrorException {
        String username = context.asContext(RouterContext.class).getUriTemplateVariables().get("user");
        String realm = context.asContext(RealmContext.class).getResolvedRealm();
        return IdUtils.getIdentity(username, realm).getUniversalId();
    }
}
