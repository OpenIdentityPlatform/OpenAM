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

package org.forgerock.openam.authz.filter.session;

import com.iplanet.dpro.session.service.SessionService;
import org.forgerock.openam.auth.shared.AuthnRequestUtils;
import org.forgerock.openam.auth.shared.SSOTokenFactory;
import org.forgerock.openam.authz.filter.AdminAuthorizationFilter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Extends the Admin Only Authorization Filter to provide an exception case to the ?_action=logout on the
 * SessionResource. As otherwise only admins could logout.
 *
 * @author Phill Cunnington
 */
@Singleton
public class SessionResourceAuthzFilter extends AdminAuthorizationFilter {

    /**
     * Constructs a new instance of the SessionResourceAuthzFilter.
     *
     * @param ssoTokenFactory An instance of the SSOTokenFactory.
     * @param requestUtils An instance of the AuthnRequestUtils.
     * @param sessionService An instance of the SessionService.
     */
    @Inject
    public SessionResourceAuthzFilter(SSOTokenFactory ssoTokenFactory, AuthnRequestUtils requestUtils,
            SessionService sessionService) {
        super(ssoTokenFactory, requestUtils, sessionService);
    }

    /**
     * Overridden to allow access through to the logout action to any user.
     *
     * @param servletRequest {@inheritDoc}
     * @param servletResponse {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean authorize(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {

        Map<String, String> parameterMap = servletRequest.getParameterMap();
        if (parameterMap.containsKey("_action") &&
                "logout".equalsIgnoreCase(parameterMap.get("_action"))) {
            return true;
        }

        return super.authorize(servletRequest, servletResponse);
    }
}
