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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.console;

import com.iplanet.jato.CompleteRequestException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.ViewBeanBase;
import com.sun.identity.console.base.model.AMAdminConstants;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.xui.XUIState;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Helper for redirecting back to the XUI.
 *
 * @since 13.0.0
 */
public final class XuiRedirectHelper {

    public static final String SERVER_DEFAULT_LOCATION = "configure/server-defaults/general";
    public static final String DEPLOYMENT_SERVERS = "deployment/servers";
    public static final String GLOBAL_SERVICES = "configure/global-services";
    public static final String TOP_LEVEL_REALM_SESSIONS = "realms/%2F/sessions";

    private static final String XUI_CONSOLE_BASE_PAGE = "{0}/XUI?realm={1}#{2}";
    private static final String DEFAULT_REALM = "/";

    private XuiRedirectHelper() {
    }

    /**
     * Redirects to the XUI to the specified realm and hash.
     *
     * @param request Used to determine the OpenAM deployment URI.
     * @param administeredRealm The realm which is being administered.
     * @param authenticationRealm The realm to which the user is authenticated.
     * @param xuiHash The XUI location hash.
     */
    public static void redirectToXui(HttpServletRequest request, String administeredRealm, String authenticationRealm,
                                     String xuiHash) {
        String deploymentUri = InjectorHolder.getInstance(BaseURLProviderFactory.class).get(administeredRealm)
                .getContextPath();

        String redirect = MessageFormat.format(XUI_CONSOLE_BASE_PAGE, deploymentUri, authenticationRealm, xuiHash);
        RequestContext rc = RequestManager.getRequestContext();
        try {
            rc.getResponse().sendRedirect(redirect);
            throw new CompleteRequestException();
        } catch (IOException e) {
            //never thrown, empty catch
        }
    }

    /**
     * Redirects to the XUI to the specified hash.
     *
     * @param request Used to determine the OpenAM deployment URI.
     * @param xuiHash The XUI location hash.
     * @param authenticationRealm The realm to which the user is authenticated.
     */
    public static void redirectToXui(HttpServletRequest request, String xuiHash, String authenticationRealm) {
        String deploymentUri = InjectorHolder.getInstance(BaseURLProviderFactory.class).get(DEFAULT_REALM)
                .getRootURL(request);

        String redirect = MessageFormat.format(XUI_CONSOLE_BASE_PAGE, deploymentUri, authenticationRealm, xuiHash);
        RequestContext rc = RequestManager.getRequestContext();
        try {
            rc.getResponse().sendRedirect(redirect);
            throw new CompleteRequestException();
        } catch (IOException e) {
            //never thrown, empty catch
        }
    }

    /**
     * Gets the administered realm to redirect to from the JATO page session.
     *
     * @param viewBean The view bean.
     * @return The administered realm.
     */
    public static String getAdministeredRealm(ViewBeanBase viewBean) {
        String redirectRealm = (String) viewBean.getPageSessionAttribute(AMAdminConstants.CURRENT_REALM);
        if (redirectRealm == null) {
            redirectRealm = (String) viewBean.getPageSessionAttribute(AMAdminConstants.CURRENT_PROFILE);
        }
        return redirectRealm;
    }

    /**
     * Gets the authentication realm to redirect to from the JATO page session.
     *
     * @param viewBean The view bean.
     * @return The authentication realm.
     */
    public static String getAuthenticationRealm(ViewBeanBase viewBean) {
        String authenticationRealm = (String) viewBean.getPageSessionAttribute(AMAdminConstants.CURRENT_PROFILE);
        if (authenticationRealm == "") {
            authenticationRealm = DEFAULT_REALM;
        }
        return authenticationRealm;
    }

    /**
     * Determines if request is from XUI to get a JATO page session.
     *
     * @param request The request.
     * @return {@code true} if the request is from the XUI, {@code false} otherwise.
     */
    public static boolean isJatoSessionRequestFromXUI(HttpServletRequest request) {
        return "XUI".equals(request.getParameter("requester"));
    }

    /**
     * Determines if the XUI admin console is enabled.
     *
     * @return {@code true} if the XUI admin console is enabled, {@code false} otherwise.
     */
    public static boolean isXuiAdminConsoleEnabled() {
        return InjectorHolder.getInstance(XUIState.class).isXUIAdminEnabled();
    }
}
