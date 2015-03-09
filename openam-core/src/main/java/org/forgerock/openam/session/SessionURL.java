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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.session;

import com.google.inject.Singleton;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.share.SessionEncodeURL;
import com.sun.identity.session.util.SessionUtils;
import org.forgerock.openam.utils.StringUtils;

import javax.servlet.http.HttpServletResponse;

/**
 * Ex-Sun class, pulled out from Session.java.
 *
 * todo: attribution
 */
@Singleton
public class SessionURL {

    private final SessionCookies sessionCookies;

    private static SessionURL instance;

    /**
     * ClientSDK: Static initialisation required for non-Guice usage.
     *
     * @return A singleton SessionURL instance.
     */
    public static synchronized SessionURL getInstance() {
        if (instance == null) {
            instance = new SessionURL(SessionCookies.getInstance());
        }
        return instance;
    }

    // Private to enforce singleton.
    private SessionURL(SessionCookies cookies) {
        sessionCookies = cookies;
    }

    /**
     * Returns the encoded URL, rewritten to include the session id. cookie will
     * be rewritten in the URL as a query string with entity escaping of
     * ampersand before appending session ID if other query parameters exists in
     * the URL.
     * <p>
     *
     * @param res HTTP Servlet Response.
     * @param url the URL to be encoded.
     * @return the encoded URL if cookies are not supported and URL if cookies
     *         are supported
     */
    public String encodeURL(HttpServletResponse res, String url, Session session) {
        return encodeURL(res, url, sessionCookies.getCookieName(), session);
    }

    /**
     * Returns the encoded URL, rewritten to include the session id. cookie will
     * be rewritten in the URL as a query string with entity escaping of
     * ampersand before appending session id if other query parameters exists in
     * the URL.
     * <p>
     *
     * @param res HTTP Servlet Response.
     * @param url  the URL to be encoded.
     * @param cookieName AM cookie name.
     * @param session session to use.
     * @return the encoded URL if cookies are not supported and URL if cookies
     *         are supported
     */
    public String encodeURL(HttpServletResponse res, String url, String cookieName, Session session) {
        String httpEncodeUrl = res.encodeURL(url);
        return encodeURL(httpEncodeUrl, SessionUtils.QUERY, true, cookieName, session);
    }

    /**
     * Returns the encoded URL, rewritten to include the session id. Cookie
     * will be written to the URL in as a query string.
     *
     * @param url the URL to be encoded.
     * @param escape true if ampersand entity escaping needs to done
     *        else false. This parameter is valid only when encoding scheme
     *        is <code>SessionUtils.QUERY</code>.
     * @param session session to use.
     * @return the encoded URL if cookies are not supported or the URL if
     *         cookies are supported.
     */
    public String encodeURL(String url, boolean escape, Session session) {
        return encodeURL(url, escape, sessionCookies.getCookieName(), session);
    }

    /**
     * Returns the encoded URL, rewritten to include the session id. Cookie
     * will be written to the URL in as a query string.
     *
     * @param url the URL to be encoded
     * @param escape true if ampersand entity escaping needs to
     *        done else false.This parameter is valid only when encoding
     *        scheme is <code>SessionUtils.QUERY</code>.
     * @param cookieName cookie name.
     * @param session session to use.
     * @return the encoded URL if cookies are not supported or the URL if
     *         cookies are supported.
     */
    public String encodeURL(String url, boolean escape, String cookieName, Session session) {
        return encodeURL(url, SessionUtils.QUERY, escape, cookieName, session);
    }

    /**
     * Returns the encoded URL, rewritten to include the session id in the
     * query string with entity escaping
     *
     * @param url the URL to be encoded
     * @param session session to use.
     * @return the encoded URL if cookies are not supported or the URL if
     *         cookies are supported.
     */
    public String encodeURL(String url, Session session) {
        return encodeURL(url, sessionCookies.getCookieName(), session);
    }

    /**
     * Returns the encoded URL, rewritten to include the session id in the
     * query string with entity escaping
     *
     * @param url the URL to be encoded.
     * @param cookieName the cookie name.
     * @return the encoded URL if cookies are not supported or the URL if
     *         cookies are supported.
     */
    public String encodeURL(String url, String cookieName, Session session) {
        return encodeURL(url, SessionUtils.QUERY, true, cookieName, session);
    }

    /**
     * Returns the encoded URL, rewritten to include the session id.
     *
     * @param url the URL to be encoded.
     * @param encodingScheme the scheme to rewrite the cookie value in URL as
     *        a Query String or Path Info (Slash or Semicolon separated.
     *        Allowed values are <code>SessionUtils.QUERY</code>,
     *        <code>SessionUtils.SLASH</code> and
     *        <code>SessionUtils.SEMICOLON</code>
     * @param escape true if ampersand entity escaping needs to done
     *        else false. This parameter is valid only when encoding scheme
     *        is <code>SessionUtils.QUERY</code>.
     * @param session session to use.
     * @return the encoded URL if cookies are not supported or the URL if
     *         cookies are supported.
     */
    public String encodeURL(String url, short encodingScheme, boolean escape, Session session) {
        return encodeURL(url, encodingScheme, escape, sessionCookies.getCookieName(), session);
    }

    /**
     * Returns the encoded URL, rewritten to include the session id.
     *
     * @param url the URL to be encoded.
     * @param encodingScheme the scheme to rewrite the cookie value in URL as
     *        a Query String or Path Info (Slash or Semicolon separated. Allowed
     *        values are <code>SessionUtils.QUERY</code>,
     *        <code>SessionUtils.SLASH</code> and
     *        <code>SessionUtils.SEMICOLON</code>.
     * @param escape true if ampersand entity escaping needs to done
     *        else false. This parameter is valid only when encoding scheme
     *        is <code>SessionUtils.QUERY</code>.
     * @param cookieName name of the cookie.
     * @param session session to use.
     * @return the encoded URL if cookies are not supported or the URL if
     *         cookies are supported.
     */
    public String encodeURL(String url, short encodingScheme, boolean escape, String cookieName, Session session) {
        String encodedURL = url;
        String cookieStr = session.getCookieStr();

        if (!StringUtils.isBlank(url) && (!session.getCookieSupport())) {
            if (!StringUtils.isBlank(cookieStr) && (sessionCookies.containsCookie(cookieStr, cookieName))) {
                encodedURL = SessionEncodeURL.buildCookieString(url, cookieStr, encodingScheme, escape);
            } else { // cookie str not set so call encodeURL
                if (session.getSessionID() != null) {
                    session.setCookieStr(SessionEncodeURL.createCookieString(cookieName, session.getSessionID().toString()));
                    encodedURL = SessionEncodeURL.encodeURL(session.getCookieStr(), url, encodingScheme, escape);
                }
            }
        }

        return encodedURL;
    }

}
