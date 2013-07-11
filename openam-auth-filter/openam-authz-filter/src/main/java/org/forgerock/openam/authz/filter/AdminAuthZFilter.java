/**
 * Copyright 2013 ForgeRock, Inc.
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
import org.forgerock.openam.auth.shared.AuthnRequestUtils;
import org.forgerock.openam.auth.shared.SSOTokenFactory;
import org.forgerock.openam.guice.InjectorHolder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Responsible for validating that the user performing the request is the Admin user.
 * If this is not the case then the request is not allowed to proceed.
 *
 * @author robert.wapshott@forgerock.com
 */
public class AdminAuthZFilter implements Filter {
    // Overridable for testing as required.
    private SSOTokenFactory factory;
    private AuthnRequestUtils requestUtils;
    private SessionService service;

    public AdminAuthZFilter() {
    }

    public AdminAuthZFilter(SSOTokenFactory factory, AuthnRequestUtils requestUtils, SessionService service) {
        this.factory = factory;
        this.requestUtils = requestUtils;
        this.service = service;
    }

    /**
     * No operation to perform.
     */
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    private boolean isInitialised() {
        return getFactory() != null;
    }

    private void initDependencies() {
        factory = InjectorHolder.getInstance(SSOTokenFactory.class);
        requestUtils = InjectorHolder.getInstance(AuthnRequestUtils.class);
        service = InjectorHolder.getInstance(SessionService.class);
    }

    /**
     * No operation to perform.
     */
    public void destroy() {
    }

    /**
     * Filter the request by examining the SSOToken UUID against the Admin user SSOToken.
     * If this comparison fails, then set the Response status to 403.
     *
     * @param servletRequest Non null request which contains details from the caller.
     * @param servletResponse Non null reqponse which will be updated if the validation fails.
     * @param filterChain {@inheritDoc}
     * @throws IOException Not thrown for this implementation.
     * @throws ServletException Not thrown for this implementation.
     */
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (!isInitialised()) {
            initDependencies();
        }

        // Request must contain the TokenID of the user.
        String tokenId = getRequestUtils().getTokenId((HttpServletRequest) servletRequest);
        if (tokenId == null) {
            failResponse(servletResponse);
            return;
        }

        // Must generate a valid SSOToken from this TokenID.
        SSOToken token = getFactory().getTokenFromId(tokenId);
        if (token == null) {
            failResponse(servletResponse);
            return;
        }

        // Verify that the SSOToken is the super user.
        String userId;
        try {
            userId = token.getProperty(Constants.UNIVERSAL_IDENTIFIER);
        } catch (SSOException e) {
            throw new IllegalStateException(e);
        }

        if (!getSessionService().isSuperUser(userId)) {
            failResponse(servletResponse);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    /**
     *@param response The ServletResponse to fail.
     */
    private void failResponse(ServletResponse response) {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * @return The SessionService, Guice initialised if it has not been assigned.
     */
    public SessionService getSessionService() {
        return service;
    }

    /**
     * @return The SSOTokenFactory, Guice initialised if it has not been assigned.
     */
    public SSOTokenFactory getFactory() {
        return factory;
    }

    /**
     * @return The AuthRequestUtils, Guice initialised if it has not been assigned.
     */
    public AuthnRequestUtils getRequestUtils() {
        return requestUtils;
    }
}
