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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.rest.authz;

import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Authorization module specifically designed for the Core Token Resource endpoint. This prevents access
 * to the Core Token Endpoint unless the System Property "org.forgerock.openam.cts.rest.enabled" has been
 * set and the user has presented an SSO Token which has Administrator-level access.
 *
 * @since 12.0.0
 * @see com.sun.identity.rest.CoreTokenResource
 */
public class CoreTokenResourceAuthzModule extends AdminOnlyAuthzModule {

    public final static String NAME = "CoreTokenResourceFilter";
    private final boolean enabled;

    @Inject
    public CoreTokenResourceAuthzModule(Config<SessionService> sessionService, Debug debug, boolean enabled) {
        super(sessionService, debug);
        this.enabled = enabled;
    }

    /**
     * Prevents access to {@link org.forgerock.openam.forgerockrest.cts.CoreTokenResource} unless this
     * REST endpoint has been explicitly enabled. If the endpoint has been explicitly enabled, it defers to
     * {@link org.forgerock.openam.rest.authz.AdminOnlyAuthzModule} to ensure that the SSO Token belongs to
     * a user with Administrator-level access.
     */
    @Override
    Promise<AuthorizationResult, ResourceException> authorize(ServerContext context) {

        if (!enabled) {
            if (debug.messageEnabled()) {
                debug.message("CoreTokenResourceAuthzModule :: Restricted access to CoreTokenResource");
            }
            return Promises.newSuccessfulPromise(AuthorizationResult.failure("CoreTokenResource not enabled."));
        }

        if (debug.messageEnabled()) {
            debug.message("CoreTokenResourceAuthzModule :: Request forwarded to AdminOnlyAuthzModule.");
        }

        return super.authorize(context);
    }

}
