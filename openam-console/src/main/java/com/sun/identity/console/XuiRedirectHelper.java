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

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Helper for redirecting back to the XUI.
 *
 * @since 13.0.0
 */
public final class XuiRedirectHelper {

    private static final String XUI_CONSOLE_BASE_PAGE = "{0}/XUI/#{1}";

    private XuiRedirectHelper() {
    }

    /**
     * Redirects to the XUI to the specified realm and hash.
     *
     * @param request Used to determine the OpenAM deployment URI.
     * @param redirectRealm The realm.
     * @param xuiHash The XUI location hash.
     */
    public static void redirectToXui(HttpServletRequest request, String redirectRealm, String xuiHash) {
        String deploymentUri = InjectorHolder.getInstance(BaseURLProviderFactory.class).get(redirectRealm)
                .getRootURL(request);
        String redirect = MessageFormat.format(XUI_CONSOLE_BASE_PAGE, deploymentUri, xuiHash);
        RequestContext rc = RequestManager.getRequestContext();
        try {
            rc.getResponse().sendRedirect(redirect);
            throw new CompleteRequestException();
        } catch (IOException e) {
            //never thrown, empty catch
        }
    }

    /**
     * Gets the realm to redirect to from the JATO page session.
     *
     * @param viewBean The view bean.
     * @return The redirect realm.
     */
    public static String getRedirectRealm(ViewBeanBase viewBean) {
        String redirectRealm = (String) viewBean.getPageSessionAttribute(AMAdminConstants.CURRENT_REALM);
        if (redirectRealm == null) {
            redirectRealm = (String) viewBean.getPageSessionAttribute(AMAdminConstants.CURRENT_PROFILE);
        }
        return redirectRealm;
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
