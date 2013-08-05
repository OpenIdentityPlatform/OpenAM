/**
 * Copyright 2013 ForgeRock, AS.
 *
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
 */
package org.forgerock.openam.authz.filter;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
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
 * Responsible for validating that the user performing the request is the Admin user.
 * If this is not the case then the request is not allowed to proceed.
 *
 * @author robert.wapshott@forgerock.com
 * @author Phill Cunnington
 * @since 10.2.0
 */
@Singleton
public class AdminAuthorizationFilter implements AuthorizationFilter {

    private final SSOTokenFactory ssoTokenFactory;
    private final AuthnRequestUtils requestUtils;
    private final SessionService sessionService;

    private DebugLogger debugLogger;

    /**
     * Constructs a new instance of the AdminAuthorizationFilter.
     *
     * @param ssoTokenFactory An instance of the SSOTokenFactory.
     * @param requestUtils An instance of the AuthnRequestUtils.
     * @param sessionService An instance of the SessionService.
     */
    @Inject
    public AdminAuthorizationFilter(SSOTokenFactory ssoTokenFactory, AuthnRequestUtils requestUtils,
            SessionService sessionService) {
        this.ssoTokenFactory = ssoTokenFactory;
        this.requestUtils = requestUtils;
        this.sessionService = sessionService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialise(LoggingConfigurator configuration, AuditLogger auditLogger, DebugLogger debugLogger) {
        this.debugLogger = debugLogger;
    }

    /**
     * Filter the request by examining the SSOToken UUID against the Admin user SSOToken.
     *
     * @param servletRequest {@inheritDoc}
     * @param servletResponse {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean authorize(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {

        // Request must contain the TokenID of the user.
        String tokenId = requestUtils.getTokenId(servletRequest);
        if (tokenId == null) {
            return false;
        }

        // Must generate a valid SSOToken from this TokenID.
        SSOToken token = ssoTokenFactory.getTokenFromId(tokenId);
        if (token == null) {
            return false;
        }

        // Verify that the SSOToken is the super user.
        String userId;
        try {
            userId = token.getProperty(Constants.UNIVERSAL_IDENTIFIER);
        } catch (SSOException e) {
            debugLogger.error("Failed to get userId", e);
            throw new IllegalStateException(e);
        }

        return sessionService.isSuperUser(userId);
    }
}
