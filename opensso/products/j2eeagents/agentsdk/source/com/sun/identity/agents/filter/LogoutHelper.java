/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package com.sun.identity.agents.filter;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.common.ICookieResetHelper;
import java.util.Hashtable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Logout helper class, this class is intended to make it possible to use the same
 * logout logic in multiple taskhandlers.
 *
 * @see NotenforcedIPTaskHandler
 * @see NotenforcedListTaskHandler
 * @see ApplicationLogoutHandler
 * 
 * @author Peter Major
 */
public class LogoutHelper {

    private Hashtable<String, IJ2EELogoutHandler> _localLogoutHandlers = new Hashtable<String, IJ2EELogoutHandler>();
    private AmFilterTaskHandler parent = null;

    public LogoutHelper(AmFilterTaskHandler parentTask) {
        parent = parentTask;
    }

    protected void doLogout(AmFilterRequestContext ctx) throws AgentException {
        // Call the container specific logout handler in ALL or J2EE_POLICY
        // mode
        if (ctx.getFilterMode().equals(AmFilterMode.MODE_J2EE_POLICY)
                || ctx.getFilterMode().equals(AmFilterMode.MODE_ALL)) {
            invokeApplicationLogoutHandler(ctx);
        }

        // remove the SSO Token from the local caches
        removeSSOToken(ctx);

        // Even if logout handler fails, we will Destroy local session and
        // redirect to AM
        if (parent.isLogMessageEnabled()) {
            parent.logMessage(
                    "LogoutHelper : Invalidating HTTP Session.");
        }

        HttpSession session = ctx.getHttpServletRequest().getSession(false);
        if (session != null) {
            session.invalidate();
        }

        doCookiesReset(ctx);
    }

    /**
     * @param ctx  the <code>AmFilterRequestContext</code> that carries
     *            information about the incoming request and response objects.
     */
    private boolean invokeApplicationLogoutHandler(AmFilterRequestContext ctx)
            throws AgentException {
        boolean result = false;

        try {
            String appName = parent.getApplicationName(ctx.getHttpServletRequest());

            IJ2EELogoutHandler localAuthHandler =
                    getApplicationLogoutHandler(appName);

            if (localAuthHandler != null) {
                if (parent.isLogMessageEnabled()) {
                    parent.logMessage(
                            "LogoutHelper : "
                            + "Invoking Local Logout handler");
                }

                localAuthHandler.logout(ctx.getHttpServletRequest(), ctx.getHttpServletResponse(), null);
                result = true;
            }
        } catch (Exception ex) {
            throw new AgentException(
                    "LogoutHelper.invokeApplicationLogoutHandler()"
                    + " failed to invoke Local Logout with exception", ex);
        }

        return result;
    }

    /**
     * Method getApplicationLogoutHandler
     *
     * @param appName Application Name
     *  @ return IJ2EELogoutHandler Mapped Local Logout Handler
     *
     * @see Returns the Application Logout Handler for the context URI. If the
     *      application does not have an entry in the configuration.
     *
     */
    private IJ2EELogoutHandler getApplicationLogoutHandler(String appName)
            throws AgentException {
        IJ2EELogoutHandler localLogoutHandlerClass = null;

        if ((appName != null) && (appName.length() > 0)) {
            localLogoutHandlerClass = getApplicationLogoutHandlers().get(appName);

            if (localLogoutHandlerClass == null) {
                String localLogoutHandlerClassName =
                        parent.getManager().getApplicationConfigurationString(
                        AmFilterTaskHandler.CONFIG_LOGOUT_APPLICATION_HANDLER_MAP, appName);

                if ((localLogoutHandlerClassName != null)
                        && (localLogoutHandlerClassName.length() > 0)) {
                    try {
                        localLogoutHandlerClass = Class.forName(localLogoutHandlerClassName).
                                asSubclass(IJ2EELogoutHandler.class).newInstance();

                        getApplicationLogoutHandlers().put(appName, localLogoutHandlerClass);
                        if (parent.isLogMessageEnabled()) {
                            parent.logMessage(
                                    "LogoutHelper: Application Name = "
                                    + appName
                                    + " registering"
                                    + " Local Logout Handler = "
                                    + localLogoutHandlerClass);
                        }
                    } catch (Exception ex) {
                        throw new AgentException(
                                "Failed to load Local Logout Handler "
                                + "for Application = " + appName
                                + " with exception :", ex);
                    }
                }
            }
        }

        return localLogoutHandlerClass;
    }

    /**
     * Remove SSO Token from local cache during logout.
     * If notification is enabled this code is probably a noop, but if the
     * browser was faster then the logout notification this method will work
     * as a safety net.
     *
     * @param ctx RequestContext
     */
    private void removeSSOToken(AmFilterRequestContext ctx) {
        HttpServletRequest request = ctx.getHttpServletRequest();
        String rawToken = parent.getSSOTokenValidator().getSSOTokenValue(request);
        Session.removeSID(new SessionID(rawToken));
    }

    private void doCookiesReset(AmFilterRequestContext ctx) {
        HttpServletRequest request = ctx.getHttpServletRequest();
        HttpServletResponse response = ctx.getHttpServletResponse();
        ICookieResetHelper cookieResetHelper =
                parent.getSSOContext().getCookieResetHelper();
        if (cookieResetHelper != null && cookieResetHelper.isActive()) {
            cookieResetHelper.doCookiesReset(request, response);
        }
    }

    private Hashtable<String, IJ2EELogoutHandler> getApplicationLogoutHandlers() {
        return _localLogoutHandlers;
    }
}
