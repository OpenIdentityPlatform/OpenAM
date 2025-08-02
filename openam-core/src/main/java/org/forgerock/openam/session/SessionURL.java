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
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.session;

import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.openam.utils.StringUtils;

import com.google.inject.Singleton;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.share.SessionEncodeURL;
import com.sun.identity.session.util.SessionUtils;

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
     * @deprecated Do not use this method as can result in state being initialised before system
     * properties are set. Use Guice to get the instance instead.
     */
    @Deprecated
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
        return encodeSessionURL(httpEncodeUrl, SessionUtils.QUERY, true, cookieName, session);
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
        return encodeSessionURL(url, SessionUtils.QUERY, escape, cookieName, session);
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
        return encodeSessionURL(url, SessionUtils.QUERY, true, cookieName, session);
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
        return encodeSessionURL(url, encodingScheme, escape, sessionCookies.getCookieName(), session);
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
    public String encodeSessionURL(String url, short encodingScheme, boolean escape, String cookieName, Session session) {
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

    /**
     * Encodes the url by adding the cookiename=sid to it.
     * if cookie support is true returns without encoding
     *
     * <p>
     * The cookie Value is written in the URL based on the encodingScheme
     * specified. The Cookie Value could be written as path info separated
     * by either a "/" OR  ";" or as a query string.
     *
     * <p>
     * If the encoding scheme is SLASH then the  cookie value would be
     * written in the URL as extra path info in the following format:
     * <pre>
     * protocol://server:port/servletpath/&lt;cookieName>=&lt;cookieValue>?
     *       queryString
     * </pre>
     * <p>
     * Note that this format works only if the path is a servlet, if a
     * a jsp file is specified then webcontainers return with
     * "File Not found" error. To rewrite links which are JSP files with
     * cookie value use the SEMICOLON OR QUERY encoding scheme.
     *
     * <p>
     * If the encoding scheme is SEMICOLON then the cookie value would be
     * written in the URL as extra path info in the following format:
     * <pre>
     * protocol://server:port/path;&lt;cookieName=cookieValue>?queryString
     * </pre>
     * Note that this is not supported in the servlet specification and
     * some web containers do not support this.
     *
     * <p>
     * If the encoding scheme is QUERY then the cookie value would be
     * written in the URL in the following format:
     * <pre>
     * protocol://server:port/path?&lt;cookieName>=&lt;cookieValue>
     * protocol://server:port/path?queryString&&lt;cookieName>=&lt;cookieValue>
     * </pre>
     * <p>
     * This is the default and OpenAM always encodes in this format
     * unless otherwise specified. If the URL passed in has query parameter then
     * entity escaping of ampersand will be done before appending the cookie
     * if the escape is true.  Only the ampersand before appending
     * cookie parameter
     * will be entity escaped.
     * <p>
     * @param url the url to be encoded
     * @param encodingScheme possible values are QUERY,SLASH,SEMICOLON
     * @param escape entity escaping of ampersand when appending the
     *        SSOToken ID to request query string.
     * @param cookieName
     * @return encoded URL with cookie value (session id) based
     *         on the encoding scheme or the url itself if there is an error.
     */
    public String encodeInternalSessionURL(String url, short encodingScheme, boolean escape, String cookieName,
                                           InternalSession internalSession) {
        String encodedURL = url;
        String cookieStr = internalSession.getCachedCookieString();

        if (!StringUtils.isBlank(url) && (!internalSession.getCookieSupport())) {
            if (!StringUtils.isBlank(cookieStr) && (sessionCookies.containsCookie(cookieStr, cookieName))) {
                encodedURL = SessionEncodeURL.buildCookieString(url, cookieStr, encodingScheme, escape);
            } else { // cookie str not set so call encodeURL
                if (internalSession.getSessionID() != null) {
                    internalSession.cacheCookieString(SessionEncodeURL.createCookieString(cookieName, internalSession.getSessionID().toString()));
                    encodedURL = SessionEncodeURL.encodeURL(internalSession.getCachedCookieString(), url, encodingScheme, escape);
                }
            }
        }
        return encodedURL;
    }

}
