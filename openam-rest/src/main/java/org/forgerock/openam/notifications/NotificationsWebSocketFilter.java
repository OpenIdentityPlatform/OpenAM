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

package org.forgerock.openam.notifications;

import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.authentication.service.AuthUtilsWrapper;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.SSOTokenFactory;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

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
    private CoreWrapper coreWrapper;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        tokenFactory = InjectorHolder.getInstance(SSOTokenFactory.class);
        authUtilsWrapper = InjectorHolder.getInstance(AuthUtilsWrapper.class);
        coreWrapper = InjectorHolder.getInstance(CoreWrapper.class);
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
                try {
                    AMIdentity identity = coreWrapper.getIdentity(ssoToken);
                    if (isJ2eeAgent(identity) || (isWebAgent(identity))) {
                        filterChain.doFilter(request, response);
                    } else {
                        response.setStatus(SC_FORBIDDEN);
                    }
                    return;
                } catch (SSOException | IdRepoException e) {
                    response.setStatus(SC_UNAUTHORIZED);
                    return;
                }
            }
        }
        response.setStatus(SC_UNAUTHORIZED);
    }

    @Override
    public void destroy() {

    }

    private boolean isJ2eeAgent(AMIdentity identity) throws IdRepoException, SSOException {
        return AgentConfiguration.AGENT_TYPE_J2EE.equalsIgnoreCase(AgentConfiguration.getAgentType(identity));
    }

    private boolean isWebAgent(AMIdentity identity) throws IdRepoException, SSOException {
        return AgentConfiguration.AGENT_TYPE_WEB.equalsIgnoreCase(AgentConfiguration.getAgentType(identity));
    }
}
