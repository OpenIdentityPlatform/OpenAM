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
* Copyright 2014-2016 ForgeRock AS.
* Portions copyright 2025 3A Systems LLC.
*/

package org.forgerock.openam.core.rest.cts;

import jakarta.inject.Inject;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.rest.authz.AdminOnlyAuthzModule;
import org.forgerock.openam.utils.Config;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.shared.debug.Debug;

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
    private final CoreTokenConfig coreTokenConfig;

    @Inject
    public CoreTokenResourceAuthzModule(Config<SessionService> sessionService, Debug debug, CoreTokenConfig coreTokenConfig) {
        super(sessionService, debug);
        this.coreTokenConfig = coreTokenConfig;
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Prevents access to {@link CoreTokenResource} unless this
     * REST endpoint has been explicitly enabled. If the endpoint has been explicitly enabled, it defers to
     * {@link org.forgerock.openam.rest.authz.AdminOnlyAuthzModule} to ensure that the SSO Token belongs to
     * a user with Administrator-level access.
     */
    @Override
    protected Promise<AuthorizationResult, ResourceException> authorize(Context context) {

        if (!coreTokenConfig.isCoreTokenResourceEnabled()) {
            if (debug.messageEnabled()) {
                debug.message("CoreTokenResourceAuthzModule :: Restricted access to CoreTokenResource");
            }
            return Promises.newResultPromise(AuthorizationResult.accessDenied("CoreTokenResource not enabled."));
        }

        if (debug.messageEnabled()) {
            debug.message("CoreTokenResourceAuthzModule :: Request forwarded to AdminOnlyAuthzModule.");
        }

        return super.authorize(context);
    }

}
