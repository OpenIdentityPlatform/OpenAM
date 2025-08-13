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
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.http.authz;

import static org.forgerock.caf.authentication.framework.AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

import com.sun.identity.shared.debug.Debug;

/**
 * A Filter implementation that injects the required OpenAM contexts into the context hierarchy.
 *
 * @since 14.0.0
 */
public class HttpContextFilter implements Filter {

    private final Debug debug;
    private final SSOTokenContext.Factory ssoTokenContextFactory;

    @Inject
    public HttpContextFilter(@Named("frRest") Debug debug, SSOTokenContext.Factory ssoTokenContextFactory) {
        this.debug = debug;
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
            Map<String, Object> attributes = context.asContext(AttributesContext.class).getAttributes();
            Map<String, Object> contextMap = asMap(attributes.get(ATTRIBUTE_AUTH_CONTEXT));
            String tokenId = (String) contextMap.get("tokenId");
            context = new SecurityContext(context, tokenId, contextMap);
        }
        return context;
    }

    private Context addSSOTokenContext(Context context) {
        if (!context.containsContext(SSOTokenContext.class)) {
            context = ssoTokenContextFactory.create(context);
        }
        return context;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object object) {
        try {
            return (Map) object;
        } catch (ClassCastException e) {
            debug.error("Invalid object found in authorization context attribute", e);
            return new LinkedHashMap<>();
        }
    }
}
