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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.authz.filter;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.auth.common.AuditLogger;
import org.forgerock.auth.common.DebugLogger;
import org.forgerock.auth.common.LoggingConfigurator;
import org.forgerock.authz.AuthorizationFilter;
import org.forgerock.openam.auth.shared.AuthnRequestUtils;
import org.forgerock.openam.auth.shared.SSOTokenFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Responsible for merely logging the request with the requests token id, if present, and not applying any
 * authorization to the request, just letting it pass through to the resource.
 *
 * @author Phill Cunnington
 * @since 10.2.0
 */
@Singleton
public class PassThroughAuthorizationFilter implements AuthorizationFilter {

    private final SSOTokenFactory ssoTokenFactory;
    private final AuthnRequestUtils requestUtils;

    private DebugLogger debugLogger;

    /**
     * Constructs a new instance of the PassThroughAuthorizationFilter.
     *
     * @param ssoTokenFactory An instance of the SSOTokenFactory.
     * @param requestUtils An instance of the AuthnRequestUtils.
     */
    @Inject
    public PassThroughAuthorizationFilter(SSOTokenFactory ssoTokenFactory, AuthnRequestUtils requestUtils) {
        this.ssoTokenFactory = ssoTokenFactory;
        this.requestUtils = requestUtils;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialise(LoggingConfigurator loggingConfigurator, AuditLogger auditLogger, DebugLogger debugLogger) {
        this.debugLogger = debugLogger;
    }

    /**
     * Performs no filtering or application of authorization to the request, just tries to get as much information about
     * the user/client making the request to log the request.
     *
     * @param request {@inheritDoc}
     * @param response {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean authorize(HttpServletRequest request, HttpServletResponse response) {

        // Request MAY contain the TokenID of the user.
        String tokenId = requestUtils.getTokenId(request);
        if (tokenId == null) {
            debugLogger.debug("Request made without TokenID");
        }

        String authorizationDebugMessage = request.getRequestURI() + " Accessed using PassThroughAuthorizationFilter, with "
                + "unauthenticated user.";

        if (tokenId != null) {
            // Must generate a valid SSOToken from this TokenID.
            SSOToken token = ssoTokenFactory.getTokenFromId(tokenId);
            if (token == null) {
                return false;
            }

            try {
                String name = token.getPrincipal().getName();
                String realm = token.getProperty("Organization");
                authorizationDebugMessage = request.getRequestURI() + " Accessed using PassThroughAuthorizationFilter by user, "
                        + name + " on realm, " + realm + " with token, " + token.getTokenID();
            } catch (SSOException e) {
                debugLogger.error("Error getting AMIdentity for token.", e);
                return false;
            }
        }

        debugLogger.debug(authorizationDebugMessage);

        return true;
    }
}
