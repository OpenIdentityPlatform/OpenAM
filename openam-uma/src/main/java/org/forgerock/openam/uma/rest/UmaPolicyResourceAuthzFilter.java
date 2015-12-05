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

package org.forgerock.openam.uma.rest;

import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import javax.inject.Named;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.services.context.Context;
import org.forgerock.openam.rest.authz.ResourceOwnerOrSuperUserAuthzModule;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * <p>A sub-type of {@link ResourceOwnerOrSuperUserAuthzModule} that prevents an admin user from
 * creating policies on behalf of other users.</p>
 *
 * <p>This limitation is needed as the backend policy engine sets the "createdBy" attribute of
 * a policy based on the universal id of the current logged in user and cannot be told on whos
 * behalf the operation is on.</p>
 *
 * @since 13.0.0
 */
public class UmaPolicyResourceAuthzFilter extends ResourceOwnerOrSuperUserAuthzModule {

    /** Name used by the LoggingAuthzModule. */
    public static final String NAME = "ResourceOwnerOrSuperUserAuthzModule";

    @Inject
    public UmaPolicyResourceAuthzFilter(Config<SessionService> sessionService, @Named("frRest") Debug debug) {
        super(sessionService, debug);
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeCreate(Context context, CreateRequest request) {
        try {
            SSOTokenContext ssoTokenContext = context.asContext(SSOTokenContext.class);
            if (!getUserId(ssoTokenContext.getCallerSSOToken()).equalsIgnoreCase(getUserIdFromUri(context))) {
                return Promises.newResultPromise(
                        AuthorizationResult.accessDenied("Only resource owner of resource set can create UMA "
                                + "policies for it."));
            } else {
                return authorize(context);
            }
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (SSOException e) {
            return newResultPromise(AuthorizationResult.accessDenied("Not authorized."));
        }
    }
}
