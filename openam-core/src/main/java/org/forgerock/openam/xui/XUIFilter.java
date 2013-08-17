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

/**
 * XUIFilter class is a servlet Filter for filtering incoming requests to OpenAM and redirecting them
 * to XUI or classic UI by inspecting the attribute openam-xui-interface-enabled in the iPlanetAMAuthService
 * service.
 *
 * @author Travis
 */
public class XUIFilter implements Filter, ServiceListener {

    private String xuiLoginPath;
    private String xuiLogoutPath;
    private String profilePage;
    private boolean xuiState;
    private volatile boolean initialized;
    private static final String XUI_INTERFACE = "openam-xui-interface-enabled";
    private static final String SERVICE_NAME = "iPlanetAMAuthService";
    private static final Debug DEBUG = Debug.getInstance("Configuration");
    private String listenerID;
    private ServiceSchemaManager scm = null;

    public XUIFilter() {
        // no op
    }

    /* constructor for testing */
    public XUIFilter(boolean init, boolean state) {
        this.initialized = init;
        this.xuiState = state;
    }


    /**
     * {@inheritDoc}
     */
    public void init(FilterConfig filterConfig) {
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

        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    detectXUIMode();
                }
            }
        }

        if (xuiState && request.getPathInfo() != null) {
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
            if (request.getPathInfo().contains("UI/Logout")) {
                response.sendRedirect(xuiLogoutPath + query);
            } else if (request.getPathInfo().contains("idm/EndUser")) {
                response.sendRedirect(profilePage + query);
            } else {
                response.sendRedirect(xuiLoginPath + query);
            }
        } else {
            chain.doFilter(servletRequest, servletResponse);
        }
    }

    /**
     * detectXUIMode will detect if XUI is enabled or disabled by inspecting the service
     */
    private void detectXUIMode() {
        try {
            SSOToken dUserToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            scm = new ServiceSchemaManager(SERVICE_NAME, dUserToken);
            ServiceSchema schema = scm.getGlobalSchema();
            Map attrs = schema.getAttributeDefaults();
            xuiState = Boolean.parseBoolean(CollectionHelper.getMapAttr(attrs, XUI_INTERFACE, ""));
            if (listenerID == null) {
                listenerID = scm.addListener(this);
            }
            initialized = true;
        } catch (SMSException smse) {
            DEBUG.error("Could not get iPlanetAMAuthService", smse);
        } catch (SSOException ssoe) {
            DEBUG.error("Could not get iPlanetAMAuthService", ssoe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
        if (listenerID != null && scm != null) {
            scm.removeListener(listenerID);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void organizationConfigChanged(String serviceName, String version,
                                          String orgName, String groupName, String serviceComponent,
                                          int type) {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    public void globalConfigChanged(String serviceName, String version,
                                    String groupName, String serviceComponent, int type) {
        detectXUIMode();
    }

    /**
     * {@inheritDoc}
     */
    public void schemaChanged(String serviceName, String version) {
        detectXUIMode();
    }
}
