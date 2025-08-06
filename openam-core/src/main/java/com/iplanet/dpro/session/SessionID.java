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
 * $Id: SessionID.java,v 1.10 2009/10/02 23:45:42 qcheng Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.iplanet.dpro.session;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.security.AccessController;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.openam.utils.PerThreadCache;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.share.SessionEncodeURL;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.services.util.Crypt;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.CookieUtils;

/**
 * The <code>SessionID</code> class is used to identify a Session object. It
 * contains a random String and the name of the session server. The random
 * String in the Session ID is unique on a given session server.
 *
 * @see com.iplanet.dpro.session.Session
 */

public class SessionID implements Serializable {

    public static final String SHANDLE_SCHEME_PREFIX = "shandle:";

    private String encryptedString = "";
    private boolean comingFromAuth = false;
    private String sessionServerProtocol = "";
    private String sessionServer = "";
    private String sessionServerPort = "";
    private String sessionServerURI = "";
    protected String sessionDomain = "";
    private String sessionServerID = "";
    private static String cookieName = null;
    private static Debug debug;
    private Boolean cookieMode = null;

    private transient boolean isParsed = false; // Should not be persisted to enable parsing
    private transient String tail = null; // Instantiated by SessionID#parseSessionString
    private transient SessionIDExtensions extensions; // Instantiated by SessionID#parseSessionString

    static {
        cookieName = System.getProperty("com.iplanet.am.cookie.name");
        if (cookieName == null) {
            cookieName = SystemProperties.get("com.iplanet.am.cookie.name");
        }
        debug = Debug.getInstance("amSession");
    }

    private static final PerThreadCache<SecureRandom, RuntimeException> secureRandom =
            new PerThreadCache<SecureRandom, RuntimeException>(Integer.MAX_VALUE) {
                @Override
                protected SecureRandom initialValue() {
                    try {
                        try {
                            return SecureRandom.getInstance("SHA1PRNG", "SUN");
                        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                            return SecureRandom.getInstance("SHA1PRNG");
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("Need SHA1PRNG algorithm to continue");
                    }
                }
            };

    /**
     * Constructs a <code>SessionID</code> object based on a
     * <code>HttpServletRequest</code> object. but if cookie is not found it
     * checks the URL for session ID.
     *
     * @param request <code>HttpServletRequest</code> object which contains
     *        the encrypted session string.
     */
    public SessionID(HttpServletRequest request) {
        String cookieValue;

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
     * Returns the session server path in this object.
     *
     * @return The session server path in this object.
     */
    public String getSessionServerURI() {
        if (isNull(sessionServerURI)) {
            parseSessionString();
        }
        return sessionServerURI;
    }

    /**
     * Returns the session server URL in this object.
     *
     * @return The session server URL in this object.
     */
    public String getSessionServerURL() {
        parseSessionString();
        return sessionServerProtocol + "://" + sessionServer + ":" + sessionServerPort + sessionServerURI;
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
                     + "comingFromAuth:" + comingFromAuth);
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
     * Returns the encrypted session string. By doing so it also makes it possible to use this string representation
     * for serializing/deserializing SessionID objects for session failover.
     *
     * @return An encrypted session string.
     * @see org.forgerock.openam.cts.utils.JSONSerialisation
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

        /*
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
            if (isC66Encoded()) {
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
                String extensionPart = outer.substring(0, tailIndex);
                extensions = new DynamicSessionIDExtensions(new LegacySessionIDExtensions(extensionPart));
            } else {
                extensions = new LegacySessionIDExtensions();
            }

            /*
             * Assigning the SITE_ID to ServerID is counter-intuitive. See {@link SessionID#validate()}
             * for JavaDoc detailing this arrangement.
             */
            serverID = extensions.getSiteID();
            if (serverID != null) {
                setServerID(serverID);
            }

        } catch (Exception e) {
            debug.error("Invalid sessionid format:[" + encryptedString + "]", e);
            throw new IllegalArgumentException("Invalid sessionid format:[" + encryptedString + "]" + e);
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
     * @return extension.
     */
    public SessionIDExtensions getExtension() {
        parseSessionString();
        return extensions;
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
    static String makeSessionID(String encryptedID, SessionIDExtensions extensions,
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
                for (Map.Entry<String, String> entry : extensions.asMap().entrySet()) {
                    dataOut.writeUTF(entry.getKey());
                    dataOut.writeUTF(entry.getValue());
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
        return makeSessionID(encryptedID, prototype.getExtension(), prototype.tail);
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


    /**
     * Determines whether the session ID is c66 encoded or not.
     *
     * @return {@code true} if the session ID is non-null and c66-encoded.
     * @see Constants#C66_ENCODE_AM_COOKIE
     */
    public boolean isC66Encoded() {
        return encryptedString != null && encryptedString.contains("*");
    }

    public SessionID generateRelatedSessionID(SessionServerConfig serverConfig) throws SessionException {
        return new SessionID(SessionID.makeRelatedSessionID(generateEncryptedID(serverConfig), this));
    }

    /**
     * @return true if this SessionID actually represents a session handle.
     */
    public boolean isSessionHandle() {
        return toString().startsWith(SHANDLE_SCHEME_PREFIX);
    }

    public String generateSessionHandle(SessionServerConfig serverConfig) throws SessionException {
        return SHANDLE_SCHEME_PREFIX + SessionID.makeRelatedSessionID(generateEncryptedID(serverConfig), this);
    }

    public static String generateAmCtxID(SessionServerConfig serverConfig) {
        return Long.toHexString(
                secureRandom.getInstanceForCurrentThread().nextLong()) +
                serverConfig.getLocalServerID();
    }

    /**
     * Generates new encrypted ID string to be used as part of session id
     *
     * @return emcrypted ID string
     */
    private static String generateEncryptedID(SessionServerConfig serverConfig) {
        String r = Long.toHexString(secureRandom.getInstanceForCurrentThread().nextLong());
        // TODO note that this encryptedID string is kept only
        // to make this compatible with older Java SDK clients
        // which knew too much about the structure of the session id
        // newer clients will mostly treat session id as opaque
        //
        return AccessController.doPrivileged(new EncodeAction(r + "@"
                + serverConfig.getPrimaryServerID(), Crypt.getHardcodedKeyEncryptor()));
    }

    /**
     * Generates new SessionID
     *
     * @param serverConfig Required server configuration
     * @param domain session domain
     *
     * @return newly generated session id
     * @throws SessionException
     */
    public static SessionID generateSessionID(SessionServerConfig serverConfig, String domain) throws SessionException {

        String encryptedID = generateEncryptedID(serverConfig);

        String siteID = serverConfig.getPrimaryServerID();
        String primaryID = getPrimaryId(serverConfig);
        // AME-129, always set a Storage Key regardless of persisting or not.
        String storageKey = String.valueOf(secureRandom.getInstanceForCurrentThread().nextLong());
        LegacySessionIDExtensions ext = new LegacySessionIDExtensions(primaryID, siteID, storageKey);

        String sessionID = SessionID.makeSessionID(encryptedID, ext, null);

        return new SessionID(sessionID, serverConfig.getLocalServerID(), domain);
    }

    /**
     * Generates a new stateless session ID.
     *
     * @param serverConfig Required server configuration.
     * @param domain session domain.
     * @param jwt the stateless session JWT.
     * @return the stateless session ID.
     * @throws SessionException if an error occurs encoding the session ID.
     */
    public static SessionID generateStatelessSessionID(SessionServerConfig serverConfig, String domain, String jwt)
            throws SessionException {
        Reject.ifNull(jwt);

        String siteID = serverConfig.getPrimaryServerID();
        String primaryID = getPrimaryId(serverConfig);
        LegacySessionIDExtensions ext = new LegacySessionIDExtensions(primaryID, siteID, null);

        final String sessionId = makeSessionID("", ext, jwt);

        return new SessionID(sessionId, serverConfig.getLocalServerID(), domain);
    }


    private static String getPrimaryId(SessionServerConfig serverConfig) {
        String primaryID = "";
        // AME-129 Required for Automatic Session Failover Persistence
        if (serverConfig.isSiteEnabled() &&
                serverConfig.getLocalServerID() != null &&
                !serverConfig.getLocalServerID().isEmpty()) {

            primaryID = serverConfig.getLocalServerID();
        }
        return primaryID;
    }


    private SessionID(String sid, String serverID, String domain) {
        this(sid);
        setServerID(serverID);
        sessionDomain = domain;
    }

    /**
     * This method validates that the received session ID points to an existing server ID, and the site ID also
     * corresponds to the server ID found in the session. Within this method two "extensions" are of interest: SITE_ID
     * and PRIMARY_ID. The PRIMARY_ID extension contains the hosting server's ID, but only if the given server belongs
     * to a site. The SITE_ID extension contains either the primary site's ID (if the hosting server belongs to a site)
     * or the hosting server's ID. This method will look at the extensions and make sure that they match up with the
     * naming table of this environment. If there is a problem with the session ID (e.g. the server ID actually points
     * to a primary or secondary site, or if the server ID doesn't actually correlate with the site ID), then a
     * SessionException is thrown in order to prevent forwarding of the received session request. A possible scenario
     * for having such an incorrect session ID would be having multiple OpenAM environments using the same cookie
     * domain and cookie name settings.
     *
     * @throws SessionException If the validation failed, possibly because the provided session ID was malformed or not
     * created within this OpenAM deployment.
     */
    public void validate() throws SessionException {
        String siteID = getExtension().getSiteID();
        String primaryID = getExtension().getPrimaryID();
        String errorMessage = null;

        if (StringUtils.isEmpty(siteID)) {
            errorMessage = "Invalid session ID, Site ID is null or empty";
        } else if (primaryID == null) {
            //In this case by definition the server is not assigned to a site, so we want to ensure that the
            //SITE_ID points to a server
            if (!WebtopNaming.isServer(siteID)) {
                errorMessage = "Invalid session ID, Site ID \"" + siteID + "\" either points to a non-existent server,"
                        + " or to a site";
            }
            String realSiteID = WebtopNaming.getSiteID(siteID);
            if (errorMessage == null && realSiteID != null && !realSiteID.equals(siteID)) {
                errorMessage = "Invalid session ID, Site ID \"" + siteID + "\" points to a server, but its "
                        + "corresponding site ID is not present in the session ID";
            }
        } else {
            //PRIMARY_ID is not null, hence this session belongs to a site, we need to verify that the PRIMARY_ID
            //and the SITE_ID are both correct, and they actually correspond to each other
            if (!WebtopNaming.isServer(primaryID)) {
                errorMessage = "Invalid session ID, Primary ID \"" + primaryID + "\" either points to a non-existent "
                        + "server, or to a site";
            }
            String realSiteID = WebtopNaming.getSiteID(primaryID);
            if (errorMessage == null) {
                if (realSiteID == null || realSiteID.equals(primaryID)) {
                    //The server from the session doesn't actually belong to a site
                    errorMessage = "Invalid session ID, Primary ID \"" + primaryID + "\" server isn't member of Site "
                            + "ID \"" + siteID + "\"";
                } else if (!realSiteID.equals(siteID)) {
                    //The server from the session actually belongs to a different site
                    errorMessage = "Invalid session ID, Primary ID \"" + primaryID + "\" server doesn't belong"
                            + " to Site ID \"" + siteID + "\"";
                }
            }
        }

        if (errorMessage != null) {
            if (debug.warningEnabled()) {
                debug.warning(errorMessage);
            }
            throw new SessionException(errorMessage);
        }
    }
}
