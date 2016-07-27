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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.http.authz;

import javax.inject.Inject;

import org.forgerock.authz.filter.http.api.HttpAuthorizationContext;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * A Filter implementation that injects the required OpenAM contexts into the context hierarchy.
 *
 * @since 14.0.0
 */
public class HttpContextFilter implements Filter {

    private final SSOTokenContext.Factory ssoTokenContextFactory;

    @Inject
    public HttpContextFilter(SSOTokenContext.Factory ssoTokenContextFactory) {
        this.ssoTokenContextFactory = ssoTokenContextFactory;
    }

    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
        return next.handle(addMissingContexts(context), request);
    }

    private Context addMissingContexts(Context context) {
        return addSSOTokenContext(addSecurityContext(context));
    }

    private Context addSecurityContext(Context context) {
        if (!context.containsContext(SecurityContext.class)) {
            HttpAuthorizationContext authorizationContext = HttpAuthorizationContext.forRequest(context);
            String tokenId = authorizationContext.getAttribute("tokenId");
            context = new SecurityContext(context, tokenId, authorizationContext.getAttributes());
        }
        return context;
    }

    private Context addSSOTokenContext(Context context) {
        if (!context.containsContext(SSOTokenContext.class)) {
            context = ssoTokenContextFactory.create(context);
        }
        return context;
    }
}
