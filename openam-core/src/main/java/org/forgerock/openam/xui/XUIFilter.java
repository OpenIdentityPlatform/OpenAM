/*
 * Copyright 2013 ForgeRock AS.
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

package org.forgerock.openam.xui;

import java.io.IOException;
import java.security.AccessController;
import java.util.Map;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;

import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchemaManager;
import org.forgerock.guava.common.annotations.VisibleForTesting;
import org.forgerock.guice.core.InjectorHolder;

/**
 * XUIFilter class is a servlet Filter for filtering incoming requests to OpenAM and redirecting them
 * to XUI or classic UI by inspecting the attribute openam-xui-interface-enabled in the iPlanetAMAuthService
 * service.
 *
 * @author Travis
 */
public class XUIFilter implements Filter {

    private String xuiLoginPath;
    private String xuiLogoutPath;
    private String profilePage;
    protected volatile boolean initialized;
    private ServiceSchemaManager scm = null;
    private XUIState xuiState;

    public XUIFilter() {}

    @VisibleForTesting XUIFilter(XUIState xuiState) {
        this.xuiState = xuiState;
    }

    /**
     * {@inheritDoc}
     */
    public void init(FilterConfig filterConfig) {
        if (xuiState == null) {
            xuiState = InjectorHolder.getInstance(XUIState.class);
        }
        ServletContext ctx = filterConfig.getServletContext();
        xuiLoginPath = ctx.getContextPath() + "/XUI/#login/";
        xuiLogoutPath = ctx.getContextPath() + "/XUI/#logout/";
        profilePage = ctx.getContextPath() + "/XUI/#profile/";
    }

    /**
     * {@inheritDoc}
     */
    public void doFilter(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain chain)
            throws IOException, ServletException {

        if (!(servletResponse instanceof HttpServletResponse) || !(servletRequest instanceof HttpServletRequest)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpServletResponse response = (HttpServletResponse) servletResponse;
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        if (xuiState.isXUIEnabled() && request.getRequestURI() != null) {
            String query = request.getQueryString();

            // prepare query
            if (query != null) {
                if (!query.startsWith("&")) {
                    query = "&" + query;
                }
            } else {
                query = "";
            }

            // redirect to correct location
            if (request.getRequestURI().contains("UI/Logout")) {
                response.sendRedirect(xuiLogoutPath + query);
            } else if (request.getRequestURI().contains("idm/EndUser")) {
                response.sendRedirect(profilePage + query);
            } else {
                response.sendRedirect(xuiLoginPath + query);
            }
        } else {
            chain.doFilter(servletRequest, servletResponse);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
        xuiState.destroy();
    }

}
