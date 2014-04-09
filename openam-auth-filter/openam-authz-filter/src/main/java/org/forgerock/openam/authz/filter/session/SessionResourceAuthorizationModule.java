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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.authz.filter.session;

import com.iplanet.dpro.session.service.SessionService;
import org.forgerock.authz.AuthorizationContext;
import org.forgerock.openam.auth.shared.AuthUtilsWrapper;
import org.forgerock.openam.auth.shared.AuthnRequestUtils;
import org.forgerock.openam.auth.shared.SSOTokenFactory;
import org.forgerock.openam.authz.filter.AdminAuthorizationModule;
import org.forgerock.openam.utils.Config;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Extends the Admin Only Authorization Filter to provide an exception case to the ?_action=logout on the
 * SessionResource. As otherwise only admins could logout.
 */
@Singleton
public class SessionResourceAuthorizationModule extends AdminAuthorizationModule {

    /**
     * Constructs a new instance of the SessionResourceAuthorizationModule.
     *
     * @param ssoTokenFactory An instance of the SSOTokenFactory.
     * @param requestUtils An instance of the AuthnRequestUtils.
     * @param sessionService A Future containing an instance of the SessionService.
     * @param authUtilsWrapper An instance of the AuthUtilWrapper.
     */
    @Inject
    public SessionResourceAuthorizationModule(SSOTokenFactory ssoTokenFactory, AuthnRequestUtils requestUtils,
            Config<SessionService> sessionService, AuthUtilsWrapper authUtilsWrapper) {
        super(ssoTokenFactory, requestUtils, sessionService, authUtilsWrapper);
    }

    /**
     * Overridden to allow access through to the logout action to any user.
     *
     * @param servletRequest {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean authorize(HttpServletRequest servletRequest, AuthorizationContext context) {

        if (servletRequest != null) {
            Map<String, String[]> parameterMap = servletRequest.getParameterMap();
            if (parameterMap != null && parameterMap.containsKey("_action")) {
                String[] values = parameterMap.get("_action");
                if (values != null && values.length > 0) {
                    if ("logout".equalsIgnoreCase(values[0])) {
                        return true;
                    }
                    if ("validate".equalsIgnoreCase(values[0])) {
                        return true;
                    }
                }
            }
        }

        return super.authorize(servletRequest, context);
    }
}
