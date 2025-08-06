/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.session;

import static org.forgerock.openam.session.SessionConstants.*;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.dpro.session.InvalidSessionIdException;
import org.forgerock.openam.session.service.ServicesClusterMonitorHandler;
import org.forgerock.openam.utils.StringUtils;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

/**
 * Responsible for providing functionality around Session cookie management.
 *
 * ClientSDK: This class is ClientSDK aware and will appropriately use Server side code
 * only when run in Server mode.
 */
@Singleton
public class SessionCookies {

    private final SessionCache sessionCache;

    private static Debug sessionDebug = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    /**
     * Name of the cooke as read from System Properties.
     */
    private String cookieName = SystemProperties.get("com.iplanet.am.cookie.name");

    /**
     * Name of the loadbalancer cookie as read from System Properties.
     */
    private String lbCookieName = SystemProperties.get(Constants.AM_LB_COOKIE_NAME, "amlbcookie");

    private static SessionCookies instance;

    /**
     * ClientSDK: Static initialisation required for non-Guice usage.
     *
     * @return A singleton SessionCookies instance.
     * @deprecated Do not use this method as can result in state being initialised before system
     * properties are set. Use Guice to get the instance instead.
     */
    @Deprecated
    public static synchronized SessionCookies getInstance() {
        if (instance == null) {
            instance = new SessionCookies(SessionCache.getInstance());
        }
        return instance;
    }

    // Hidden to enforce singleton.
    @Inject
    private SessionCookies(SessionCache sessionCache) {
        this.sessionCache = sessionCache;
    }

    /**
     * Returns cookie name.
     *
     * @return cookie name.
     */
    public String getCookieName() {
        return cookieName;
    }

    /**
     * Returns loadbalancer cookie name.
     *
     * @return cookie name.
     */
    public String getLBCookieName() {
        return lbCookieName;
    }

    /**
     * Returns load balancer cookie value for the Session.
     *
     * @param sid Session string for load balancer cookie.
     * @return load balancer cookie value.
     * @throws com.iplanet.dpro.session.SessionException if session is invalid.
     */
    public String getLBCookie(String sid) throws SessionException {
        return getLBCookie(new SessionID(sid));
    }

    /**
     * Returns load balancer cookie value for the Session.
     * @param  sid Session ID for load balancer cookie.
     * @return load balancer cookie value.
     * @throws SessionException if session is invalid.
     */
    public String getLBCookie(SessionID sid) throws SessionException {
        String cookieValue = null;

        lbCookieName = SystemProperties.get(Constants.AM_LB_COOKIE_NAME, "amlbcookie");

        if (sessionDebug.messageEnabled()){
            sessionDebug.message("Session.getLBCookie()" + "lbCookieName is:" + lbCookieName);
        }

        if (sid == null || StringUtils.isBlank(sid.toString())) {
            throw new InvalidSessionIdException();
        }

        if (SystemProperties.isServerMode()) {
            SessionServerConfig sessionServerConfig = InjectorHolder.getInstance(SessionServerConfig.class);
            if (!sessionServerConfig.isSiteEnabled()) {
                cookieValue = WebtopNaming.getLBCookieValue(sid.getSessionServerID());
                return lbCookieName + "=" + cookieValue;
            }
        }

        if (RESET_LB_COOKIE_NAME) {
            if (SystemProperties.isServerMode()) {
                SessionServerConfig sessionServerConfig = InjectorHolder.getInstance(SessionServerConfig.class);
                ServicesClusterMonitorHandler servicesClusterMonitorHandler = InjectorHolder.getInstance(ServicesClusterMonitorHandler.class);
                if (sessionServerConfig.isLocalSite(sid)) {
                    cookieValue = WebtopNaming.getLBCookieValue(servicesClusterMonitorHandler.getCurrentHostServer(sid));
                }
            } else {
                Session sess = sessionCache.readSession(sid);
                if (sess != null) {
                    cookieValue = sess.getProperty(lbCookieName);
                }
            }
        }

        if (StringUtils.isBlank(cookieValue)) {
            cookieValue = WebtopNaming.getLBCookieValue(sid.getExtension().getPrimaryID());
        }

        return lbCookieName + "=" + cookieValue;
    }

    /**
     * Checks if the cookie name is in the cookie string.
     *
     * @param cookieStr cookie string (<code>cookieName=cookieValue</code>).
     * @param cookieName name of the cookie.
     * @return true if <code>cookieName</code> is in the <code>cookieStr</code>.
     */
    public boolean containsCookie(String cookieStr, String cookieName) {
        boolean foundCookieName = false;
        String cookieNameInStr = null;

        if (sessionDebug.messageEnabled()) {
            sessionDebug.message("CookieNameStr is :" + cookieNameInStr);
            sessionDebug.message("cookieName is :" + cookieName);
        }

        if (!StringUtils.isBlank(cookieStr)) {
            cookieNameInStr = cookieStr.substring(0, cookieStr.indexOf("="));
        }

        if ((cookieNameInStr != null) && (cookieNameInStr.equals(cookieName))) {
            foundCookieName = true;
        }

        return foundCookieName;
    }

}
