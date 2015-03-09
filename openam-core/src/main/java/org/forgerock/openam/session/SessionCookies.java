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
* Copyright 2015 ForgeRock AS.
*/
package org.forgerock.openam.session;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.utils.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.forgerock.openam.session.SessionConstants.RESET_LB_COOKIE_NAME;

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
     */
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
            throw new SessionException(SessionBundle.rbName, "invalidSessionID", null);
        }

        if (SystemProperties.isServerMode()) {
            SessionService sessionService = InjectorHolder.getInstance(SessionService.class);
            if (!sessionService.isSiteEnabled()) {
                cookieValue = WebtopNaming.getLBCookieValue(sid.getSessionServerID());
                return lbCookieName + "=" + cookieValue;
            }
        }

        if (RESET_LB_COOKIE_NAME) {
            if (SystemProperties.isServerMode()) {
                SessionService sessionService = InjectorHolder.getInstance(SessionService.class);
                if (sessionService.isSessionFailoverEnabled() && sessionService.isLocalSite(sid)) {
                    cookieValue = WebtopNaming.getLBCookieValue(sessionService.getCurrentHostServer(sid));
                }
            } else {
                Session sess = sessionCache.readSession(sid);
                if (sess != null) {
                    cookieValue = sess.getProperty(lbCookieName);
                }
            }
        }

        if (StringUtils.isBlank(cookieValue)) {
            cookieValue = WebtopNaming.getLBCookieValue(sid.getExtension(SessionID.PRIMARY_ID));
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
