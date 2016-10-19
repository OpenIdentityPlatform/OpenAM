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

package org.forgerock.openam.notifications;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.iplanet.sso.SSOToken;
import org.apache.commons.lang.StringUtils;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.authentication.service.AuthUtilsWrapper;
import org.forgerock.openam.forgerockrest.utils.AgentIdentity;
import org.forgerock.openam.rest.SSOTokenFactory;

/**
 * Provides authentication for the WebSocket notifications endpoint.
 *
 * <p>This is required because the Servlet WebSocket API (JSR 356) does not allow us
 * to perform this authentication directly when the connection is established so we
 * are forced to use a Servlet filter instead.</p>
 *
 * @since 14.0.0
 */
public final class NotificationsWebSocketFilter implements Filter {
    private SSOTokenFactory tokenFactory;
    private AuthUtilsWrapper authUtilsWrapper;
    private AgentIdentity agentIdentity;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        tokenFactory = InjectorHolder.getInstance(SSOTokenFactory.class);
        authUtilsWrapper = InjectorHolder.getInstance(AuthUtilsWrapper.class);
        agentIdentity = InjectorHolder.getInstance(AgentIdentity.class);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String tokenId = request.getHeader(authUtilsWrapper.getCookieName());
        if (StringUtils.isNotEmpty(tokenId)) {
            SSOToken ssoToken = tokenFactory.getTokenFromId(tokenId);
            if (ssoToken != null) {
                if (agentIdentity.isAgent(ssoToken)) {
                    filterChain.doFilter(request, response);
                    return;
                } else {
                    response.setStatus(SC_FORBIDDEN);
                    return;
                }
            }
        }
        response.setStatus(SC_UNAUTHORIZED);
    }

    @Override
    public void destroy() {

    }
}
