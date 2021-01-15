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
 * Portions Copyrighted 2013-2015 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */
package com.sun.identity.session.util;

import static org.forgerock.openam.session.SessionConstants.*;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.services.util.Crypt;
import com.iplanet.services.util.I18n;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.utils.ClientUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.URL;
import java.security.AccessController;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

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

    private static Debug debug = Debug.getInstance("amSessionUtils");

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
     * Returns the remote IP address of the client
     *
     * @param servletRequest The HttpServletRequest object which contains the
     *        session string.
     * @return InetAddress the client address
     * @exception Exception
     */
    public static InetAddress getClientAddress(HttpServletRequest servletRequest) throws Exception {
        return InetAddress.getByName(ClientUtils.getClientIPAddress(servletRequest));
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
                Set<String> psl = WebtopNaming.getPlatformServerList();
                if (psl == null) {
                    throw new SessionException(SessionBundle.rbName,
                            "emptyTrustedSourceList", null);
                }
                for (String e : psl) {
                    try {
                        URL url = new URL(e);
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
    public static boolean isTrustedSource(InetAddress source) throws SessionException {
        if (trustedSources == null) {
            trustedSources = getTrustedSourceList();
        }
        return trustedSources.contains(source);
    }

    final static Cache<String, String> key2encrypt=CacheBuilder.newBuilder()
    		.expireAfterAccess(Duration.ofMinutes(15))
    		.maximumSize(64000)
    		.build();
    final static Cache<String, String> encrypt2key=CacheBuilder.newBuilder()
    		.expireAfterAccess(Duration.ofMinutes(15))
    		.maximumSize(64000)
    		.build();
    
    public static String getEncryptedStorageKey(SessionID clear) throws Exception {
    	if (clear == null){
            throw new SessionException("SessionUtils.getEncryptedStorageKey: StorageKey is null");
        }
    	return  getEncrypted(clear.getExtension().getStorageKey());
    }
    
    public static String getEncrypted(String clear)  {
    	if (clear==null) {
    		return clear;
    	}
        if (SESSION_ENCRYPTION) {
        	try {
				return key2encrypt.get(clear, new Callable<String>() {
					@Override
					public String call() throws Exception {
						final String encrypted=new EncodeAction(clear, Crypt.getEncryptor()).run();
						encrypt2key.put(encrypted, clear);
						return encrypted;
					}
				});
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
        }
        return clear;
    }

    public static String getDecrypted(String encrypted)  {
    	if (encrypted==null) {
    		return encrypted;
    	}
    	if (SESSION_ENCRYPTION) {
    		try {
				return encrypt2key.get(encrypted, new Callable<String>() {
					@Override
					public String call() throws Exception {
						final String clear=new DecodeAction(encrypted, Crypt.getEncryptor()).run();
						key2encrypt.put(encrypted, clear);
						return clear;
					}
				});
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
    	}
		return encrypted;
	}


    /**
     * Helper method to get admin token. This is not amadmin user
     * but the user configured in serverconfig.xml as super user.
     *
     * @return SSOToken of super admin.
     */
    public static SSOToken getAdminToken() throws SSOException {
        SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
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

        String usrName;
        String admName;

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
    public static void checkPermissionToSetProperty(SSOToken clientToken, String key, String value)
            throws SessionException {

        Debug sessionDebug = InjectorHolder.getInstance(Key.get(Debug.class, Names.named(SESSION_DEBUG)));

        if (InternalSession.isProtectedProperty(key)) {
            if (clientToken == null) {
                // Throw Ex. Client should identify itself.
                if (sessionDebug.warningEnabled()) {
                    sessionDebug.warning(
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
                sessionDebug.error(
                    "SessionUtils.checkPermissionToSetProperty(): "
                    + "Cannot get instance of SSOTokenManager.");
                throw new SessionException(
                    SessionBundle.getString(
                        "protectedPropertyNoSSOTokenMgrInstance")+ " " + key);
            }

            if (!ssoTokenManager.isValidToken(clientToken)) {
                // Throw Ex. Client should identify itself.
                if (sessionDebug.warningEnabled()) {
                    sessionDebug.warning(
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
                sessionDebug.error(
                    "SessionUtils.checkPermissionToSetProperty(): "
                    + "Cannot get Admin Token for validation to set protected "
                    + "property [" + key + "=" + value + "]");
                throw new SessionException(
                    SessionBundle.getString("protectedPropertyNoAdminToken")
                    + " " + key);
            }
            if (!SessionUtils.isAdmin(admToken, clientToken)) {
                // Throw Ex. Client not authorized to set this property.
                sessionDebug.error(
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
