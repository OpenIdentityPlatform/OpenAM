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
 * $Id: SessionID.java,v 1.10 2009/10/02 23:45:42 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2011, 2013 ForgeRock, Inc.
 */

package com.iplanet.dpro.session;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.share.SessionEncodeURL;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.CookieUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The <code>SessionID</code> class is used to identify a Session object. It
 * contains a random String and the name of the session server. The random
 * String in the Session ID is unique on a given session server.
 * 
 * @see com.iplanet.dpro.session.Session
 */

public class SessionID implements Serializable {
    private String encryptedString = "";
    private boolean isParsed = false;

    private boolean comingFromAuth = false;

    private String sessionServerProtocol = "";

    private String sessionServer = "";

    private String sessionServerPort = "";

    private String sessionServerURI = "";

    protected String sessionDomain = "";

    private String sessionServerID = "";

    private String tail = null;

    private String extensionPart = null;

    private Map extensions = new HashMap();

    private static String cookieName = null;

    private static Debug debug;

    private Boolean cookieMode = null;

    static {
        cookieName = System.getProperty("com.iplanet.am.cookie.name");
        if (cookieName == null) {
            cookieName = SystemProperties.get("com.iplanet.am.cookie.name");
        }
        debug = Debug.getInstance("amSession");
    }

    // prefix "S" is reserved to be used by session framework-specific
    // extensions for session id format
    public static final String PRIMARY_ID = "S1";

    public static final String STORAGE_KEY = "SK";

    public static final String SITE_ID = "SI";

    /**
     * Constructs a <code>SessionID</code> object based on a
     * <code>HttpServletRequest</code> object. but if cookie is not found it
     * checks the URL for session ID.
     * 
     * @param request <code>HttpServletRequest</code> object which contains
     *        the encrypted session string.
     */
    public SessionID(HttpServletRequest request) {
        String cookieValue = null;

        if (cookieName == null) {
            cookieName = SystemProperties.get("com.iplanet.am.cookie.name");
        }

        if (cookieName != null) {
            // check if this is a forward from authentication service case.
            // if yes, find Session ID in the request URL first, otherwise
            // find Session ID in the cookie first
            String isForward = (String) 
                request.getAttribute(Constants.FORWARD_PARAM);
            if (debug.messageEnabled()) {
                debug.message("SessionID(HttpServletRequest) : is forward = "
                    + isForward); 
            } 
            if ((isForward != null) &&
                isForward.equals(Constants.FORWARD_YES_VALUE)) {
                String realReqSid = SessionEncodeURL.getSidFromURL(request);
                if (realReqSid != null) {
                    encryptedString = realReqSid;
                } else {
                    cookieValue = CookieUtils
                        .getCookieValueFromReq(request, cookieName);
                    if (cookieValue != null) {
                        encryptedString = cookieValue;
                        cookieMode = Boolean.TRUE;
                    }
                }
            } else {
                cookieValue = CookieUtils
                    .getCookieValueFromReq(request, cookieName);

                // if no cookie found in the request then check if
                // the URL has it.
                if (cookieValue == null) {
                    String realReqSid = SessionEncodeURL.getSidFromURL(request);
                    if (realReqSid != null) {
                        encryptedString = realReqSid;
                    }
                    cookieMode = Boolean.FALSE;
                } else {
                    cookieMode = Boolean.TRUE;
                    encryptedString = cookieValue;
                }
            }
        }
    }

    /**
     * Creates a default instance of SessionID with a null Session ID.
     * Note: This function is needed for deserialisation.
     */
    public SessionID() {
    }

    /**
     * Constructs a <code>SessionID</code> object based on a Session ID.
     * 
     * @param sid The session ID String in an encrypted format.
     */
    public SessionID(String sid) {
        encryptedString = sid;
        // toString() returns a String that is identical to 'sid'
    }

    /**
     * Checks if encrypted string is null or empty
     * 
     * @return true if encrypted string is null or empty.
     */
    public boolean isNull() {
        return isNull(encryptedString);
    }

    /**
     * Utility method to check if argument is null or empty string
     * 
     * @param s string to check
     * @return true if <code>s</code> is null or empty.
     */
    private static boolean isNull(String s) {
        return s == null || s.length() == 0;

    }

    /**
     * Returns the session server URI in this object.
     * 
     * @return The session server URI in this object.
     */
    public String getSessionServerURI() {
        if (isNull(sessionServerURI)) {
            parseSessionString();
        }
        return sessionServerURI;
    }
    
    /**
      * This method returns the boolean representing if this session id
      * is a regular auth token, generated via AuthContext API
      * and not a restricted one.
      *
      * @return The boolean representing if this session id
      * is that of a regular auth token, generated via AuthContext API
      */
     public boolean getComingFromAuth() {
         if (debug.messageEnabled()) {
             debug.message("SessionID.getComingFromAuth():"
                 +"comingFromAuth:"+comingFromAuth);
         }
         return comingFromAuth;
     }

     /**
      * This method sets the boolean representing if this session id
      * is a regular auth token, generated via AuthContext API
      * @param comingFromAuth boolean representing if the
      * token has been generated by AuthContext and is a regular token,
      * not restricted one.
      */
     public void setComingFromAuth(boolean comingFromAuth) {
         this.comingFromAuth = comingFromAuth;
     }

     /**
     * Returns the session server name in this object.
     * 
     * @return The session server protocol in this object.
     */
    public String getSessionServerProtocol() {
        if (isNull(sessionServerProtocol)) {
            parseSessionString();
        }
        return sessionServerProtocol;
    }

    /**
     * Gets the session server port in this object
     * 
     * @return The session server port in this object.
     */
    public String getSessionServerPort() {
        if (isNull(sessionServerPort)) {
            parseSessionString();
        }
        return sessionServerPort;
    }

    /**
     * Gets the session server name in this object.
     * 
     * @return The session server name in this object.
     */
    public String getSessionServer() {
        if (isNull(sessionServer)) {
            parseSessionString();
        }
        return sessionServer;
    }

    /**
     * Gets the domain where this session belongs to.
     * 
     * @return The session domain name.
     */
    public String getSessionDomain() {
        return sessionDomain;
    }

    /**
     * Gets the session server id in this object.
     * 
     * @return The session server id in this object.
     */
    public String getSessionServerID() {
        if (isNull(sessionServerID)) {
            parseSessionString();
        }
        return sessionServerID;
    }

    /**
     * Returns the encrypted session string.
     * 
     * @return An encrypted session string.
     */
    public String toString() {
        return encryptedString;
    }

    /**
     * Compares this Session ID to the specified object. The result is true if
     * and only if the argument is not null and the random string and server
     * name are the same in both objects.
     * 
     * @param object the object to compare this Session ID against.
     * @return true if the Session ID are equal; false otherwise.
     */
    public boolean equals(Object object) {
        if (object == null || !(object instanceof SessionID)) {
            return false;
        }
        SessionID another = (SessionID) object;
        return encryptedString.equals(another.encryptedString);
    }

    /**
     * Returns a hash code for this object.
     * 
     * @return a hash code value for this object.
     */
    public int hashCode() {
        // Since SessionID is immutable, it's hashCode doesn't change.
        return encryptedString.hashCode();
    }

    /**
     * Extracts the  server, protocol, port, extensions and tail from Session ID
     * 
     */
    private void parseSessionString() {
        // parse only once
        if (isParsed) {
            return;
        }

        /**
         * This check is done because the SessionID object is getting created
         * with empty sid value. This is a temparory fix. The correct fix for
         * this is, throw a SessionException while creating the SessionID
         * object.
         */
        if (isNull()) {
            throw new IllegalArgumentException("sid value is null or empty");
        }
        String serverID = null;
        try {
            String sidString = encryptedString;
            // sidString would have * if it has been c66 encoded
            if (sidString.indexOf("*") != -1) {
                sidString = c66DecodeCookieString(encryptedString);
            }
            int outerIndex = sidString.lastIndexOf("@");
            if (outerIndex == -1) {
                isParsed = true;
                return;
            }

            String outer = sidString.substring(outerIndex + 1);
            int tailIndex = outer.indexOf("#");
            tail = outer.substring(tailIndex + 1);

            if (tailIndex != -1) {

                // TODO implement lazy parsing of the exceptions
                extensionPart = outer.substring(0, tailIndex);

                DataInputStream extensionStr = new DataInputStream(
                        new ByteArrayInputStream(Base64.decode(extensionPart)));

                Map extMap = new HashMap();

                // expected syntax is a sequence of pairs of UTF-encoded strings
                // (name, value)
                while (true) {
                    String extName;
                    try {
                        extName = extensionStr.readUTF();
                    } catch (EOFException e) {
                        break;
                    }
                    String extValue = extensionStr.readUTF();
                    extMap.put(extName, extValue);
                }
                extensions = extMap;
            }

            serverID = (String) extensions.get(SITE_ID);
            if (serverID != null) {
                setServerID(serverID);
            }

        } catch (Exception e) {
            debug.error("Invalid sessionid format:["+serverID+"]", e);
            throw new IllegalArgumentException("Invalid sessionid format:["+serverID+"]" + e);
        }
        isParsed = true;
    }

    /**
     * Sets the server info by making a naming request by passing
     * its id which is in session id and parses it.
     * @param id ServerID
     */
    protected void setServerID(String id) {
        try {
            URL url = new URL(WebtopNaming.getServerFromID(id));
            sessionServerID = id;
            sessionServerProtocol = url.getProtocol();
            sessionServer = url.getHost();
            sessionServerPort = String.valueOf(url.getPort());
            sessionServerURI = url.getPath();

            int idx = sessionServerURI.lastIndexOf('/');
            while (idx > 0) {
                sessionServerURI = sessionServerURI.substring(0, idx);
                idx = sessionServerURI.lastIndexOf('/');
            }
        } catch (Exception e) {
            debug.error("Could not get server info from sessionid: "+id+"]", e);
            throw new IllegalArgumentException(
                    "Invalid server id in session id:["+id+"]" + e);
        }
    }

    /**
     * Returns tail part of session id
     * 
     * @return An opaque tail part of session id
     */
    public String getTail() {
        parseSessionString();
        return tail;
    }

    /**
     * Returns the if the cookies are supported.
     * 
     * @return Boolean object value which is Boolean.<code>TRUE<code> if 
     *         supported <code>FALSE</code> otherwise
     */
    public Boolean getCookieMode() {
        return cookieMode;
    }

    /**
     * Retrieves extension value by name Currently used session id extensions
     * are
     * 
     * <code>SessionService.SITE_ID</code> server id (from platform server list)
     * hosting this session (in failover mode this will be server id of the
     * load balancer)
     * 
     * <code>SessionService.PRIMARY_ID</code>,
     * <code>SessionService.SECONDARY_ID</code> used if internal request
     * routing mode is enabled.
     * 
     * @param name Name of the session ID extension.
     * @return extension.
     */
    public String getExtension(String name) {
        parseSessionString();
        return (String) extensions.get(name);
    }

    /**
     * Generates properly encoded session id string given the encrypted ID,
     * extension map and tail part
     * 
     * @param encryptedID encrypted part of session ID.
     * @param extensions map of session ID extensions.
     * @param tail tail part of session ID (currently used to carry associated
     *        HTTP session ID)
     * @return encoded session id string.
     * @throws SessionException
     */
    public static String makeSessionID(String encryptedID, Map extensions,
            String tail) throws SessionException {
        try {
            StringBuilder buf = new StringBuilder();
            buf.append(encryptedID);
            if (extensions != null || tail != null) {
                buf.append("@");
            }
            if (extensions != null) {
                ByteArrayOutputStream baOut = new ByteArrayOutputStream();
                DataOutputStream dataOut = new DataOutputStream(baOut);
                for (Iterator iter = extensions.entrySet().iterator(); iter
                        .hasNext();) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    dataOut.writeUTF((String) entry.getKey());
                    dataOut.writeUTF((String) entry.getValue());
                }
                dataOut.close();
                buf.append(Base64.encode(baOut.toByteArray()));
                buf.append("#");
            }
            if (tail != null) {
                buf.append(tail);
            }
            String returnValue = buf.toString();
            if (c66EncodeCookie()) {
                returnValue = c66EncodeSidString(returnValue);
            }
            return returnValue;
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    /**
     * Generates encoded session id string which uses the same extensions and
     * tail part as prototype session id, but a different encrypted ID. This
     * method is used to generate session handle and restricted token id for a
     * given master session id. Related session IDs must share extensions and
     * tail information in order for session failover to work properly
     * 
     * @param encryptedID encrypted ID.
     * @param prototype session ID to copy extensions and tail from
     * @return encoded session id
     * @throws SessionException
     */
    public static String makeRelatedSessionID(String encryptedID,
            SessionID prototype) throws SessionException {
        prototype.parseSessionString();
        return makeSessionID(encryptedID, prototype.extensions, prototype.tail);
    }

    /**
     * Checks whether session id needs to be c66 encoded to convert to cookie
     * value. 
     * @return <code>true</code> if session id needs to be c66 encoded to
     * convert to cookie value. Otherwise returns <code>false</code>.
     * c66 encoding is opensso specific url safe char66 encoding.
     *
     * @see #c66EncodeSidString(String)
     * @see #c66DecodeCookieString(String)
     */
    private static boolean c66EncodeCookie() {
        return Boolean.valueOf(
                SystemProperties.get(Constants.C66_ENCODE_AM_COOKIE, 
                "false")).booleanValue();
    }

    /** 
     * Converts native session id  string to opensso specific url safe char66
     * encoded string.
     * This is not a general purpose utility. 
     * This is meant only for internal use
     *
     * @param sidString plain text string
     * @return url safe modifed char66 encoded string
     *
     * @see #c66DecodeCookieString(String)
     * 
     * Sample session id string:
     * AQIC5wM2LY4SfcxPEcjVKCEI7QdmYvlOZvKZpdEErxVPvx8=@AAJTSQACMDE=# 
     *
     * We would replace
     * + with -
     * / with _
     * = with .
     * @ with star
     * # with star
     *
     * while reconstucting the original cookie value first occurence of
     * star would be replaced with @ and the subsequent occurunce star would 
     * be replaced with #
     */
    private static String c66EncodeSidString(String sidString) {
        if (sidString == null || sidString.length() == 0) {
            return sidString;
        }
        int length = sidString.length();
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            char c = sidString.charAt(i);
            if (c == '+') {
                chars[i] = '-';
            } else if (c == '/') {
                chars[i] = '_';
            } else if (c == '=') {
                chars[i] = '.';
            } else if (c == '@') {
                chars[i] = '*';
            } else if (c == '#') {
                chars[i] = '*';
            } else {
                chars[i] = c;
            }
        }
        return new String(chars); 
    }


    /** 
     * Converts opensso specific url safe char66
     * encoded string to native session id  string.
     * This is not a general purpose utility. 
     * This is meant only for internal use
     *
     * @param urlEncodedString  opensso specific url safe char66 encoded string
     * @return native session id string
     *
     * @see #c66EncodeSidString(String)
     *
     * We would replace
     * - with +
     * _ with /
     * . with =
     * first occurence of star with @
     * subsequent occurence of star with #
     */
    private static String c66DecodeCookieString(String urlEncodedString) {
        if (urlEncodedString == null || urlEncodedString.length() == 0) {
            return urlEncodedString;
        }
        int length = urlEncodedString.length();
        char[] chars = new char[length];
        boolean firstStar = true;
        for (int i = 0; i < length; i++) {
            char c = urlEncodedString.charAt(i);
            if (c == '-') {
                chars[i] = '+';
            } else if (c == '_') {
                chars[i] = '/';
            } else if (c == '.') {
                chars[i] = '=';
            } else if (c == '*') {
                if (firstStar) {
                    firstStar = false;
                    chars[i] = '@';
                } else {
                    chars[i] = '#';
                }
            } else {
                chars[i] = c;
            }
        }
        return new String(chars); 
    }

}
