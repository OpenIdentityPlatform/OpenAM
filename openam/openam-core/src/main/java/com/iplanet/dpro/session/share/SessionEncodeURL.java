/**
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
 * $Id: SessionEncodeURL.java,v 1.7 2008/08/19 19:08:41 veiming Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.dpro.session.share;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.session.util.SessionUtils;
import java.util.Hashtable;
import javax.servlet.http.HttpServletRequest;

/** 
 * <code>SessionEncodeURL</code> class encodes the </code>URL</code> 
 * with the cookie value as a query string
 * or extra path info based on the encoding scheme.
 * <p>
 * The cookie Value is written in the URL based on the encoding scheme
 * specified. The Cookie Value could be written as path info separated
 * by either a "/" OR  ";" or as a query string.
 *
 * <p>
 * If the encoding scheme is SLASH then the  cookie value would be
 * written in the URL as extra path info in the following format:
 * <code>protocol://server:port/servletpath/&lt;cookieName>=&lt;cookieValue>?
 * queryString</code>
 * <p>
 * Note that this format works only if the path is a servlet, if a
 * a JSP file is specified then web containers return with
 * "File Not found" error. To rewrite links which are JSP files with
 * cookie value use the <code>SEMICOLON</code> or <code>QUERY</code> encoding
 * scheme.
 *
 * <p>
 * If the encoding scheme is SEMICOLON then the cookie value would be
 * written in the URL as extra path info in the following format:
 * <code>protocol://server:port/path;&lt;cookieName=cookieValue>
 * ?queryString</code>
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
 * This is the default and OpenSSO always encodes in this format
 * unless otherwise specified. If the URL passed in has query parameter then
 * entity escaping of ampersand will be done before appending the cookie
 * if the escape is true.Only the ampersand before appending cookie parameter
 * will be entity escaped. 
 */

public class SessionEncodeURL {

    public static Debug debug;

    public final String delimiter = "_";

    public static Hashtable sidHash = new Hashtable();

    public static final String SESS_DELIMITER = ";"; // semicolon - &#059;

    public static final String SLASH_SESS_DELIMITER = "/";

    public static final String QUERY = "?";

    public static final String AMPERSAND = "&";

    public static final String AMPERSAND_ESC = "&amp;";

    public static final String EQUAL = "=";

    private static SessionEncodeURL se = new SessionEncodeURL();

    static boolean cookieEncoding = (SystemProperties
            .get(Constants.AM_COOKIE_ENCODE) != null && SystemProperties.get(
            Constants.AM_COOKIE_ENCODE).equalsIgnoreCase("true"));

    static {
        debug = Debug.getInstance("amSessionEncodeURL");
    }

   /** 
    * Constructs <code> SessionEncodeURL </Code>
    *
    */
   public SessionEncodeURL() {

    }

    /**
     * Encodes the URL with the cookie value as a query string or extra path
     * info based on the encoding scheme. if encoding scheme is QUERY , encoded
     * URL format will be :
     * <code>protocol://server:port/path?cookieName=cookieValue</code>
     * if escape is false then no entity escaping of ampersand.
     * <pre>
     * protocol://server:port/path?queryString&amp;
     *         cookieName=cookieValue
     * </pre>
     * if escape is true then entity escaping of ampersand
     * <pre>
     * protocol://server:port/path?queryString&amp;cookieName=cookieValue
     * </pre>
     * <p>
     * if encoding scheme is <code>SLASH</code>, <code>encodedURL</code>
     * format will be :
     * <code>protocol://server:port/path/cookieName=cookieValue</code>
     * <p>
     * if encoding scheme is <code>SEMICOLON</code>, <code>encodedURL</code>
     * format will be:
     * <code>protocol://server:port/path;cookieName=cookieValue</code>
     * <p>
     * 
     * @param sidString Session ID.
     * @param url the URL to be encoded.
     * @param encodingScheme how the cookie will be encoded in as query string
     *        or as extra path info (<code>SLASH</code> or
     *        <code>SEMICOLON</code>)
     * @param escape <code>true</code> to escape ampersand.
     * @return the URL encoded with the session ID.
     */
    public static String encodeURL(String sidString, String url,
            short encodingScheme, boolean escape) {

        String encodedURL = se.encodeSidURL(url, sidString, encodingScheme,
                escape);
        if (debug.messageEnabled()) {
            debug.message("URL recd.. " + url);
            debug.message("encodeURL .. " + encodedURL);
            debug.message("encodingScheme is.. " + encodingScheme);
            debug.message("escape is.. " + escape);
        }
        return encodedURL;
    }

    private static String getCookieName() {
        String name = System.getProperty("com.iplanet.am.cookie.name");
        if (name == null) {
            name = SystemProperties.get("com.iplanet.am.cookie.name");
        }
        return name;
    }

    /**
     * Checks whether the encoded URL has session id or not. And then extracts
     * the Session Id from it.
     * 
     * @param request ,
     *            HTTPServletRequestObject
     * @return the extracted SessionID
     */
    public static String getSidFromURL(HttpServletRequest request) {
        return getSidFromURL(request, getCookieName());
    }

    /**
     * Checks whether the encoded URL has session id or not. And then extracts
     * the Session Id from it.
     * 
     * @param request HTTP Servlet Request.
     * @param cookieName Cookie name.
     * @return the extracted Session ID
     */
    public static String getSidFromURL(HttpServletRequest request,
            String cookieName) {
        String sidString = "";
        if (request != null) {
            String url = request.getRequestURI();
            if (url != null) {
                if ((url.indexOf(SLASH_SESS_DELIMITER + cookieName) != -1)
                        || (url.indexOf(SESS_DELIMITER + cookieName) != -1)) {
                    sidString = getSidFromURI(url, cookieName);
                } else {
                    sidString = getSidFromQuery(request, cookieName);
                }
            }
        }

        if (debug.messageEnabled()) {
            debug.message("before decoding getSidFromURL:sidString="
                    + sidString);
        }
        if (cookieEncoding) {
            if (sidString != null) {
                sidString = URLEncDec.decode(sidString);
            }
        }

        if (debug.messageEnabled()) {
            debug.message("after decoding: getSidFromURL:sidString="
                    + sidString);
        }
        return sidString;
    }

    /* retrieves the session ID from the request URI */
    private static String getSidFromURI(String url, String cookieName) {

        String sid = "";
        if (url != null) {
            if (debug.messageEnabled()) {
                debug.message("getSidFromURI: url=" + url);
            }
            if ((url != null) && (url.length() > 0)) {

                int start = url.indexOf(cookieName);

                if (start != -1) {
                    start = start + cookieName.length() + 1;

                    int end = url.indexOf(QUERY, start);

                    if (end != -1) {
                        sid = url.substring(start, end - 1);
                    } else {
                        sid = url.substring(start);
                    }
                }
            }
        }

        if (debug.messageEnabled()) {
            debug.message("getSidFromURL: sid =" + sid);
        }
        return sid;
    }

    /* extracts the sessionId from the request Query */
    private static String getSidFromQuery(HttpServletRequest request,
            String cookieName) {

        String sid = "";
        if (request != null) {
            sid = request.getParameter(cookieName);
        }
        if (debug.messageEnabled()) {
            debug.message("getSidFromQuery: request =" + request);
            debug.message("getSidFromQuery: sid =" + sid);
        }
        return sid;
    }

    /**
     * Encodes the specified URL by including the session ID in it. Similar to
     * <code>javax.servlet.http.HttpServletResponse.encodeURL()</code>. session
     * ID will be only be included if the client needs encoding.
     * 
     * @param url URL to be rewritten with the session ID.
     * @param cookieStr Cookie value which will be append to the URL.
     * @param encodingScheme how the cookie will be encoded in as query string
     *        or as extra path info (<code>SLASH</code> or
     *        <code>SEMICOLON</code>)
     * @param escape <code>true</code> to escape ampersand.
     * @return the encoded URL.
     */
    private String encodeSidURL(String url, String cookieStr,
            short encodingScheme, boolean escape) {

        // separate URI and Query String
        String uri = url;
        String qString = null;
        int index = url.indexOf(QUERY);
        if (index != -1) {
            uri = url.substring(0, index);
            qString = url.substring(index + 1);
        }

        String encodedURL = url;
        if (encodingScheme == SessionUtils.QUERY) {
            encodedURL = encodeSidInQueryString(uri, qString, 
                    cookieStr, escape);
        } else {
            encodedURL = encodeSidInURLPath(uri, qString, cookieStr,
                    encodingScheme);
        }

        if (debug.messageEnabled()) {
            debug.message("encodeSidURL :: URI  :" + uri);
            debug.message("encodeSidURL :: qString :" + qString);
            debug.message("encodeSidURL :: cookieStr:" + cookieStr);
            debug.message("encodeSidURL :: URL  :" + encodedURL);
        }

        return encodedURL;
    }

    /**
     * Constructs the cookie string based on the URL and cookie String passed.
     *
     * @param url URL which needs to be rewritten.
     * @param cookieStr Cookie name and cookie value.
     * @param encodingScheme how the cookie will be encoded in as query string
     *        or as extra path info (<code>SLASH</code> or
     *        <code>SEMICOLON</code>).
     * @param escape <code>true</code> to escape ampersand.
     * @return encoded URL.
     */
    public static String buildCookieString(
        String url,
        String cookieStr,
        short encodingScheme,
        boolean escape
    ) {
        return (encodingScheme == SessionUtils.QUERY) ?
            writeUrlInQuery(url, cookieStr, escape) :
            writeUrlInPath(url, cookieStr, encodingScheme);
    }

    /**
     * write the cookie string in the URL as extra path info
     */
    private static String writeUrlInPath(String url, String cookieStr,
            short encodingScheme) {
        String sessionDelimiter = SLASH_SESS_DELIMITER;
        if (encodingScheme == SessionUtils.SEMICOLON) {
            sessionDelimiter = SESS_DELIMITER;
        }

        StringBuilder encodedURLBuf = new StringBuilder();
        if (url.indexOf(cookieStr) == -1) {
            int i = url.indexOf(QUERY);
            if (i != -1) {
                String uri = url.substring(0, i);
                String query = url.substring(i, url.length());
                encodedURLBuf.append(uri).append(sessionDelimiter).append(
                        cookieStr).append(query);
            } else {
                encodedURLBuf.append(url).append(sessionDelimiter).append(
                        cookieStr);
            }
        } else {
            encodedURLBuf.append(url);
        }

        String encodedURL = encodedURLBuf.toString();
        if (debug.messageEnabled()) {
            debug.message("writeUrlInPath : encoded URL : " + encodedURL);
        }
        return encodedURL;
    }

    /**
     * write the cookie string in the URL as a query string
     */
    private static String writeUrlInQuery(String url, String cookieStr,
            boolean escape) {
        StringBuffer encodedURLBuf = new StringBuffer().append(url);
        if (cookieStr != null && url.indexOf(cookieStr) == -1) {
            int i = url.indexOf(QUERY);
            if (i != -1) {
                if (escape) {
                    encodedURLBuf.append(AMPERSAND_ESC).append(cookieStr);
                } else {
                    encodedURLBuf.append(AMPERSAND).append(cookieStr);
                }
            } else {
                encodedURLBuf.append(QUERY).append(cookieStr);
            }
        }
        String encodedURL = encodedURLBuf.toString();
        if (debug.messageEnabled()) {
            debug.message("writeUrlInQuery : encoded URL : " + encodedURL);
        }
        return encodedURL;
    }

    /*
     * Construct the cookie String. The cookie string is in the format
     * cookieName = cookieValue. @param sessionID which the is the cookie Value
     * @return the cookie String.
     */

    public static String createCookieString(String cookieName, String sessionID)
    {
        StringBuilder cookieStrBuf = new StringBuilder();
        cookieStrBuf.append(URLEncDec.encode(cookieName)).append(EQUAL);
        if (sessionID != null) {
            cookieStrBuf.append(URLEncDec.encode(sessionID));
        }
        return cookieStrBuf.toString();
    }

    /* Rewrites the URL with cookieString in the URI */

    private static String encodeSidInURLPath(String uri, String qString,
            String cookieStr, short encodingScheme) {
        // remove any old sid's from the url
        String sessionDelimiter = SLASH_SESS_DELIMITER;
        if (encodingScheme == SessionUtils.SEMICOLON) {
            sessionDelimiter = SESS_DELIMITER;
        }
        int index = uri.indexOf(sessionDelimiter + getCookieName());
        if (index != -1) {
            uri = uri.substring(0, index);
        }
        StringBuffer urlBuf = new StringBuffer();
        if (cookieStr != null) {
            urlBuf = urlBuf.append(sessionDelimiter).append(cookieStr);
        }
        if ((qString != null) && (qString.length() > 0)) {
            urlBuf.append(QUERY).append(qString);
        }
        if (uri != null) {
            urlBuf.insert(0, uri);
        }

        return urlBuf.toString();
    }

    /*
     * Rewrites the URL with cookieString in the Query Entity Escaping of
     * ampersand is done before appending query string if escape parameter
     * passed to this method is true
     */

    private static String encodeSidInQueryString(String uri, String qString,
            String cookieStr, boolean escape) {
        // remove any old sid's from the url
        String escapeChar = AMPERSAND_ESC;
        if (!escape) {
            escapeChar = AMPERSAND;
        }
        StringBuilder urlBuf = new StringBuilder();
        urlBuf.append(QUERY);
        if (qString != null) {
            int index = qString.indexOf(getCookieName());
            String remQueryString = null;
            if (index != -1) {
                if (index > 0) {
                    remQueryString = qString.substring(0, index - 1);
                }
                int ampIndex = qString.indexOf(AMPERSAND, index);
                if (ampIndex != -1) {
                    String remString = qString.substring(ampIndex + 1, qString
                            .length());
                    if (remQueryString != null && remQueryString.length() > 0) {
                        remQueryString = remQueryString + escapeChar
                                + remString;
                    } else {
                        remQueryString = remString;
                    }
                }
            } else {
                remQueryString = qString;
            }
            if (debug.messageEnabled()) {
                debug.message("After extracting cookie from query: "
                        + remQueryString);
            }
            if (remQueryString != null) {
                urlBuf.append(remQueryString).append(escapeChar);
            }
        }
        if ((cookieStr != null) && (cookieStr.length() > 0)) {
            urlBuf.append(cookieStr);
        }
        if (uri != null) {
            urlBuf.insert(0, uri);
        }

        return urlBuf.toString();
    }
}
