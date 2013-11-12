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
 * $Id: SessionUtils.java,v 1.10 2009/11/09 18:35:22 beomsuk Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */
package com.sun.identity.session.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.services.util.Crypt;
import com.iplanet.services.util.I18n;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.EncodeAction;

/**
 * This class Implements utility methods for handling HTTP Session.
 * <p>
 */

public class SessionUtils {

    /** The QUERY encoding scheme*/
     public static final short QUERY = 0;

    /** The SLASH encoding scheme*/
     public static final short SLASH = 1;

    /** The SEMICOLON encoding scheme*/
     public static final short SEMICOLON = 2;

    static Debug debug = Debug.getInstance("amSessionUtils");

    /** Set of trusted Inetaddresses */
     private static Set trustedSources = null;

    /** The HTTPClient IPHeader */
     private static final String httpClientIPHeader = SystemProperties.get(
            Constants.CLIENT_IP_ADDR_HEADER, "proxy-ip");

    /** The SESSION_ENCRYPTION to check if this is encrypted session */
      private static final boolean SESSION_ENCRYPTION = Boolean.valueOf(
            SystemProperties.get(Constants.SESSION_REPOSITORY_ENCRYPTION,
                    "false")).booleanValue();

    /**
     * Returns a SessionID string based on a HttpServletRequest object or null
     * if session id is not present or there was an error.
     * <p>
     * 
     * @param request
     *            The HttpServletRequest object which contains the session
     *            string.
     * @return an encodeURL with sessionID or the url if session was not present
     *         or there was an error.
     */
    public static String getSessionId(HttpServletRequest request) {
        String sidString = (new SessionID(request)).toString();
        if (sidString.length() == 0) {
            sidString = null;
        }
        return sidString;
    }

    /**
     * Returns URL encoded with the cookie Value (SSOToken ID) if cookies are
     * not support. Throws an SSOException in case of an error.
     * 
     * <p>
     * The cookie Value is written in the URL based on the encodingScheme
     * specified. The Cookie Value could be written as path info separated by
     * either a "/" OR ";" or as a query string.
     * 
     * <p>
     * If the encoding scheme is SLASH then the cookie value would be written in
     * the URL as extra path info in the following format:
     * <pre>
     * protocol://server:port/servletpath/&lt;cookieName>=&lt;cookieValue>?
     *     queryString
     * </pre>
     * <p>
     * Note that this format works only if the path is a servlet, if a a jsp
     * file is specified then webcontainers return with "File Not found" error.
     * To rewrite links which are JSP files with cookie value use the SEMICOLON
     * OR QUERY encoding scheme.
     * 
     * <p>
     * If the encoding scheme is SEMICOLON then the cookie value would be
     * written in the URL as extra path info in the following format:
     * <pre>
     * protocol://server:port/path;&lt;cookieName=cookieValue>?queryString
     * </pre>
     * Note that this is not supported in the servlet specification and some web
     * containers do not support this.
     * 
     * <p>
     * If the encoding scheme is QUERY then the cookie value would be written in
     * the URL in the following format:
     * <pre>
     * protocol://server:port/path?&lt;cookieName>=&lt;cookieValue>
     * protocol://server:port/path?queryString&amp;
     *       &lt;cookieName>=&lt;cookieValue>
     * </pre>
     * <p>
     * This is the default and OpenSSO always encodes in this format
     * unless otherwise specified. If the URL passed in has query parameter then
     * entity escaping of ampersand will be done before appending the cookie if
     * the escape is true.Only the ampersand before appending cookie parameter
     * will be entity escaped.
     * <p>
     * 
     * @param ssoToken Single Sign Token which contains the session string.
     * @param url the URL to be encoded
     * @param encodingScheme possible values are <code>QUERY</code>,
     *        <code>SLASH</code>, <code>SEMICOLON</code>.
     * @param escape <code>true</code> to escape ampersand when appending the
     *        Single Sign On Token ID to request query string.
     * @return encoded URL with cookie value (session ID) based on the encoding
     *         scheme.
     * @exception SSOException if URL cannot be encoded.
     */
    public static String encodeURL(SSOToken ssoToken, String url,
            short encodingScheme, boolean escape) throws SSOException {
        String encodedURL = url;
        try {
            SSOTokenID ssoTokenId = ssoToken.getTokenID();
            SessionID sessionID = new SessionID(ssoTokenId.toString());
            Session session = Session.getSession(sessionID);
            encodedURL = session.encodeURL(url, encodingScheme, escape);
        } catch (Exception e) {
            debug.message("Exception encoding URL ", e);
            throw new SSOException(e);
        }
        return encodedURL;
    }

    /**
     * Returns URL encoded with the cookie Value (SSOToken ID) if cookies are
     * not supported.
     * 
     * This method assumes default encoding scheme which is QUERY. The cookie
     * value would be written in the URL in the following format:
     * <pre>
     * protocol://server:port/path?&lt;cookieName>=&lt;cookieValue>
     * protocol://server:port/path?queryString&amp;
     *        &lt;cookieName>=&lt;cookieValue>
     * </pre>
     * <p>
     * 
     * This is the default and OpenSSO always encodes in this format
     * unless otherwise specified. If the URL passed in has query parameter then
     * entity escaping of ampersand will be done before appending the cookie if
     * the escape is true.Only the ampersand before appending cookie parameter
     * will be entity escaped.
     * <p>
     * 
     * @param ssoToken Single Sign Token which contains the session string.
     * @param url the URL to be encoded.
     * @param escape <code>true</code> to escape ampersand when appending the
     *        Single Sign On Token ID to request query string.
     * @return URL encoded with cookie Value in the query string.
     * @exception SSOException if URL cannot be encoded.
     */
    public static String encodeURL(
        SSOToken ssoToken,
        String url,
        boolean escape
    ) throws SSOException 
    {
        String encodedURL = url;
        try {
            encodedURL = encodeURL(ssoToken, url, QUERY, escape);
        } catch (Exception e) {
            debug.message("Exception encoding url", e);
            throw new SSOException(e);
        }
        return encodedURL;
    }

   /**
    * Returns the remote IP address of the client
    * 
    * @param servletRequest The HttpServletRequest object which contains the
    *        session string.
    * @return InetAddress the client address
    * @exception Exception
    */
   public static InetAddress getClientAddress(
            HttpServletRequest servletRequest) throws Exception 
    {

        InetAddress remoteClient = InetAddress.getByName(servletRequest
                .getRemoteAddr());

        if (isTrustedSource(remoteClient)) {
            String proxyHeader = servletRequest.getHeader(httpClientIPHeader);
            if (proxyHeader != null) {
                remoteClient = InetAddress.getByName(proxyHeader);
            }
        }
        return remoteClient;
    }

    /* build the trust source set*/
    private static Set getTrustedSourceList() throws SessionException {
        Set result = new HashSet();
        try {
            String rawList = SystemProperties
                    .get(Constants.TRUSTED_SOURCE_LIST);
            if (rawList != null) {
                StringTokenizer stk = new StringTokenizer(rawList, ",");
                while (stk.hasMoreTokens()) {
                    result.add(InetAddress.getByName(stk.nextToken()));
                }
            } else {
                // use platform server list as a default fallback
                Vector psl = WebtopNaming.getPlatformServerList();
                if (psl == null) {
                    throw new SessionException(SessionBundle.rbName,
                            "emptyTrustedSourceList", null);
                }
                for (Enumeration e = psl.elements(); e.hasMoreElements();) {
                    try {
                        URL url = new URL((String) e.nextElement());
                        result.add(InetAddress.getByName(url.getHost()));
                    } catch (Exception ex) {
                        debug.error("SessionUtils.getTrustedSourceList : " + 
                                    "Validating Host exception", ex);
                    }
                }
            }
        } catch (Exception e) {
            throw new SessionException(e);
        }
        return result;
    }

   /**
    * Returns the remote IP address of the client is a trusted source
    * 
    * @param source the InetAddress of the remote client
    * @return a <code>true </code> if is a trusted source.<code>false> otherwise
    * @exception Exception
    */
   public static boolean isTrustedSource(InetAddress source)
            throws SessionException {
        if (trustedSources == null) {
            trustedSources = getTrustedSourceList();
        }
        return trustedSources.contains(source);
    }

    /**
     * Helper method to serialize and encrypt objects saved in the repository
     * 
     * @param obj
     *            object to be serialized and encrypted
     * @return encrypted byte array containing serialized objects
     * @throws Exception
     *             if anything goes wrong
     */
    public static byte[] encode(Object obj) throws Exception {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream objOutStream = new ObjectOutputStream(byteOut);

        // convert object to byte using streams
        objOutStream.writeObject(obj);
        objOutStream.close();

        final byte[] blob = byteOut.toByteArray();

        if (SESSION_ENCRYPTION) {
            return (byte[]) AccessController
                    .doPrivileged(new PrivilegedExceptionAction() {
                        public Object run() throws Exception {
                            return Crypt.getEncryptor().encrypt(blob);
                        }
                    });
        }
        return blob;

    }

    /**
     * Deserializes and decrypts objects retrieved from the repository.
     * 
     * @param blob Byte array containing serialized and encrypted object value.
     * @return retrieved object.
     * @throws Exception if anything goes wrong.
     */
    public static Object decode(final byte blob[]) throws Exception {
        byte[] decryptedBlob;
        if (SESSION_ENCRYPTION) {
            decryptedBlob = (byte[]) AccessController
                    .doPrivileged(new PrivilegedExceptionAction() {
                        public Object run() throws Exception {
                            return Crypt.getEncryptor().decrypt(blob);
                        }
                    });
        } else {
            decryptedBlob = blob;
        }
        ByteArrayInputStream byteIn = new ByteArrayInputStream(decryptedBlob);
        ObjectInputStream objInStream = new ObjectInputStream(byteIn);
        return objInStream.readObject();

    }

    /**
     * Helper method to get the encrypted session storage key
     * 
     * @param sessionID
     *            SessionID
     * @return encrypted session storage key
     * @throws Exception
     *             if anything goes wrong
     */
    public static String getEncryptedStorageKey(SessionID sessionID)
            throws Exception {

        String sKey = sessionID.getExtension(SessionID.STORAGE_KEY);
        if (SESSION_ENCRYPTION) {
            String strEncrypted = (String) AccessController
                    .doPrivileged(new EncodeAction(sKey, Crypt
                            .getHardcodedKeyEncryptor()));
            return strEncrypted;
        }
        return sKey;
    }


    /**
     * Helper method to get admin token. This is not amadmin user
     * but the user configured in serverconfig.xml as super user.
     *
     * @return SSOToken of super admin.
     */
    public static SSOToken getAdminToken() throws SSOException{
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        if (adminToken == null) {
            I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);
            String rbName = i18n.getResBundleName();
            throw new SSOException(rbName, IUMSConstants.NULL_TOKEN, null);
        }
        return (adminToken);
    }

    /**
     * Helper method to compare if the user token passed is same as admin
     * token. It does not check if user token or admin token is valid.
     *
     * @param admToken Admin Single Sign-On token.
     * @param usrToken User Single Sign-On token to compare against admin
     *        Single Sign-On token.
     * @return <code>true</code> if they both are same.
     */
    public static boolean isAdmin(SSOToken admToken, SSOToken usrToken) {
        if (usrToken == null) {
            debug.error("SessionUtils.isAdmin(): user token is null");
            return false;
        }

       if (admToken == null) {
            debug.error("SessionUtils.isAdmin(): admin token is null");
            return false;
        }

        boolean result = false;

        String usrName = null;
        String admName = null;

        try {
            usrName = usrToken.getPrincipal().getName();
        } catch (SSOException ssoEx) {
            debug.error("SessionUtils.isAdmin(): user token fails"
                        + "to get principal");
            return false;
        }

        try {
            admName = admToken.getPrincipal().getName();
        } catch (SSOException ssoEx) {
            debug.error("SessionUtils.isAdmin(): admin token fails "
                        + "to get principal");
            return false;
        }

        if (usrName.equalsIgnoreCase(admName)) {
            result = true;
        }

        if (debug.messageEnabled()) {
            debug.message("SessionUtils.isAdmin(): returns " + result +
               " for user principal: " + usrName +
               " against admin principal: " + admName);
        }

        return result;
    }


    /**
     * Helper method to check if client has taken permission to
     * set value to it. If
     * @param clientToken Token of the client setting protected property.
     * @param key Property key
     * @param value Property value.
     * @throws SessionException if the key is protected property.
     */
    public static void checkPermissionToSetProperty(SSOToken clientToken,
        String key, String value) throws SessionException {
        if (InternalSession.isProtectedProperty(key)) {
            if (clientToken == null) {
                // Throw Ex. Client should identify itself.
                if (SessionService.sessionDebug.warningEnabled()) {
                    SessionService.sessionDebug.warning(
                        "SessionUtils.checkPermissionToSetProperty(): "
                        + "Attempt to set protected property without client "
                        + "token [" + key + "=" + value + "]");
                }
                throw new SessionException(
                    SessionBundle.getString("protectedPropertyNoClientToken")
                    + " " + key);
            }

            SSOTokenManager ssoTokenManager = null;
            try {
                ssoTokenManager = SSOTokenManager.getInstance();
            } catch (SSOException ssoEx) {
                // Throw Ex. Not able to get SSOTokenManager instance.
                SessionService.sessionDebug.error(
                    "SessionUtils.checkPermissionToSetProperty(): "
                    + "Cannot get instance of SSOTokenManager.");
                throw new SessionException(
                    SessionBundle.getString(
                        "protectedPropertyNoSSOTokenMgrInstance")+ " " + key);
            }

            if (!ssoTokenManager.isValidToken(clientToken)) {
                // Throw Ex. Client should identify itself.
                if (SessionService.sessionDebug.warningEnabled()) {
                    SessionService.sessionDebug.warning(
                        "SessionUtils.checkPermissionToSetProperty(): "
                        + "Attempt to set protected property with invalid client"
                        + " token [" + key + "=" + value + "]");
                }
                throw new SessionException(
                    SessionBundle.getString(
                    "protectedPropertyInvalidClientToken") + " " + key);
            }

            SSOToken admToken = null;
            try {
                admToken = SessionUtils.getAdminToken();
            } catch (SSOException ssoEx) {
                // Throw Ex. Server not able to get Admin Token.
                SessionService.sessionDebug.error(
                    "SessionUtils.checkPermissionToSetProperty(): "
                    + "Cannot get Admin Token for validation to set protected "
                    + "property [" + key + "=" + value + "]");
                throw new SessionException(
                    SessionBundle.getString("protectedPropertyNoAdminToken")
                    + " " + key);
            }
            if (!SessionUtils.isAdmin(admToken, clientToken)) {
                // Throw Ex. Client not authorized to set this property.
                SessionService.sessionDebug.error(
                    "SessionUtils.checkPermissionToSetProperty(): "
                    + "Client does not have permission to set protected "
                    + "property" + key + "=" + value + "]");
                throw new SessionException(
                    SessionBundle.getString("protectedPropertyNoPermission")
                    + " " + key);
            }
        }
    }
}
