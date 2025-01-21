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
 * $Id: SSOProviderImpl.java,v 1.9 2009/02/19 05:04:01 bhavnab Exp $
 *
 */

/**
 * Portions copyright 2013-2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */
package com.iplanet.sso.providers.dpro;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.utils.ClientUtils;
import org.forgerock.util.annotations.VisibleForTesting;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOProvider;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.sun.identity.common.SearchResults;
import com.sun.identity.shared.debug.Debug;

/**
 * This <code>final</code> class <code>SSOProviderImpl</code> implements
 * <code>SSOProvider</code> interface and provides implementation of the methods
 * to create , destroy , check the validity of a single sign on token.
 *
 * 
 *
 * Note: Used by ClientSDK, therefore must not use Guice for initialisation.
 */
public final class SSOProviderImpl implements SSOProvider {

    /**
     * Debug SSOProvider
     */
    public static Debug debug = null;

    /**
     * Check to see if the clientIPCheck is enabled
     */
    private static boolean checkIP = Boolean.valueOf(
            SystemProperties.get("com.iplanet.am.clientIPCheckEnabled"))
            .booleanValue();

    private final SessionCache sessionCache;

    // Initialize debug instance;
    static {
        debug = Debug.getInstance("amSSOProvider");
    }

    /**
     * Constructs a instance of <code>SSOProviderImpl</code>
     *
     * @throws SSOException
     * 
     */
    public SSOProviderImpl() throws SSOException {
        this(SessionCache.getInstance());
    }

    @VisibleForTesting
    SSOProviderImpl(SessionCache sessionCache) {
        this.sessionCache = sessionCache;
    }

    /**
     * Creates a single sign on token for the <code>HttpRequest</code>
     *
     * @param request <code>HttpServletRequest</code>
     * @return single sign on token for the request
     * @throws SSOException if the single sign on token cannot be created.
     */
    public SSOToken createSSOToken(HttpServletRequest request)
            throws SSOException {
        try {
            SessionID sid = new SessionID(request);
            Session session = sessionCache.getSession(sid);
            if (sid != null) {
                Boolean cookieMode = sid.getCookieMode();
                if (debug.messageEnabled()) {
                    debug.message("cookieMode is :" + cookieMode);
                }
                if (cookieMode != null) {
                    session.setCookieMode(cookieMode);
                }
            }
            if (checkIP && !isIPValid(session, ClientUtils.getClientIPAddress(request))) {
                throw new Exception(SSOProviderBundle.getString("invalidIP"));
            }
            SSOToken ssoToken = new SSOTokenImpl(session);
            return ssoToken;
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("could not create SSOToken from HttpRequest ("
                        + e.getMessage()
                        + ")");
            }
            throw new SSOException(e);
        }
    }

    /**
     * Creates a single sign on token with user or service as the entity
     *
     * @param user     Principal representing a user or service
     * @param password password string.
     * @return single sign on token
     * @throws SSOException                  if the single sign on token cannot be created.
     * @throws UnsupportedOperationException Thrown to indicate that the
     *                                       requested operation is not supported.
     * @deprecated This method has been deprecated. Please use the
     * regular LDAP authentication mechanism instead. More information
     * on how to use the authentication programming interfaces as well as the
     * code samples can be obtained from the "Authenticating Using
     * OpenAM Java SDK" chapter of the OpenAM Developer's Guide.
     */
    public SSOToken createSSOToken(java.security.Principal user, String password)
            throws SSOException, UnsupportedOperationException {
        try {
            SSOTokenImpl ssoToken = new SSOTokenImpl(user, password);
            if (debug.messageEnabled()) {
                debug.message("SSO token ldap auth successful for "
                        + user.toString());
            }
            return ssoToken;
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("could not create SSOToken for user \""
                        + user.getName()
                        + "\"", e);
            }
            throw new SSOException(e);
        }
    }

    /**
     * Creates a single sign on token. Note: this method should remain private
     * and get called only by the AuthContext API. Note also: this method may reset
     * the idle time of the session.
     *
     * @param tokenId       single sign on token ID.
     * @param invokedByAuth boolean flag indicating that this method has
     *                      been invoked by the AuthContext.getSSOToken() API.
     * @return single sign on token.
     * @throws SSOException                  if the single sign on token cannot be created.
     * @throws UnsupportedOperationException Thrown to indicate that the
     *                                       requested operation is not supported.
     */
    public SSOToken createSSOToken(String tokenId, boolean invokedByAuth)
            throws SSOException, UnsupportedOperationException {
        return createSSOToken(tokenId, invokedByAuth, true);
    }

    /**
     * Creates a single sign on token.
     *
     * @param tokenId       single sign on token ID.
     * @param invokedByAuth boolean flag indicating that this method has been invoked by the AuthContext.getSSOToken()
     * API.
     * @param possiblyResetIdleTime If true, the idle time of the token/session may be reset to zero.  If false, the
     * idle time will never be reset.
     * @return single sign on token.
     * @throws SSOException if the single sign on token cannot be created for any reason.
     * @throws java.lang.UnsupportedOperationException only here to satisfy the interface, this is never thrown.
     */
    public SSOToken createSSOToken(String tokenId, boolean invokedByAuth, boolean possiblyResetIdleTime)
            throws SSOException, UnsupportedOperationException {

        try {
            SessionID sessionId = new SessionID(tokenId);
            sessionId.setComingFromAuth(invokedByAuth);
            Session session = sessionCache.getSession(sessionId, false, possiblyResetIdleTime);
            SSOToken ssoToken = new SSOTokenImpl(session);
            return ssoToken;
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("SSOProviderImpl.createSSOToken(tokenId, "
                        + invokedByAuth
                        + ", "
                        + possiblyResetIdleTime
                        + ") could not create SSOToken for token ID \""
                        + tokenId
                        + "\" ("
                        + e.getMessage()
                        + ")");
            }
            throw new SSOException(e);
        }
    }

    /**
     * Creates a single sign on token.
     *
     * @param tokenId single sign on token ID.
     * @return single sign on token.
     * @throws SSOException                  if the single sign on token cannot be created.
     * @throws UnsupportedOperationException
     * @deprecated Use #createSSOToken(String, String)
     */
    public SSOToken createSSOToken(String tokenId)
            throws SSOException,
            UnsupportedOperationException {
        return createSSOToken(tokenId, false);
    }

    /**
     * Creates a single sign on token.
     *
     * @param tokenId  single sign on token ID.
     * @param clientIP client IP address
     * @return single sign on token.
     * @throws SSOException                  if the single sign on token cannot be created.
     * @throws UnsupportedOperationException Thrown to indicate that the
     *                                       requested operation is not supported.
     * @deprecated Use #createSSOToken(String, String)
     */
    public SSOToken createSSOToken(String tokenId, String clientIP)
            throws SSOException, UnsupportedOperationException {
        try {
            SessionID sessionId = new SessionID(tokenId);
            Session session = sessionCache.getSession(sessionId);
            if (checkIP && !isIPValid(session, clientIP)) {
                throw new Exception(SSOProviderBundle.getString("invalidIP"));
            }
            SSOToken ssoToken = new SSOTokenImpl(session);
            return ssoToken;
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("could not create SSOToken for token ID \""
                        + tokenId
                        + "\" ("
                        + e.getMessage()
                        + ")");
            }
            throw new SSOException(e);
        }
    }

    /**
     * Checks the validity of the single sign on token
     *
     * @param token The single sign on token object to be validated
     * @return Returns true if the <code>SSOToken</code> is valid
     */
    @Override
    public boolean isValidToken(SSOToken token) {
        return isValidToken(token, true);
    }

    /**
     * Checks the validity of the single sign on token
     *
     * @param token   The single sign on token object to be validated
     * @param refresh Flag indicating whether refreshing the token is allowed
     * @return Returns true if the <code>SSOToken</code> is valid, false otherwise
     */
    @Override
    public boolean isValidToken(SSOToken token, boolean refresh) {
        /*
         * If the token was created from createSSOToken(Principal, password)
         * there is no association with session. Use this temp solution for now.
         * If this method is going to go away, we can remove that method, otherwise
         * a better mechanism has to be implemented.
         */
        SSOTokenImpl tokenImpl = (SSOTokenImpl) token;
        return (tokenImpl.isValid(refresh));
    }

    /**
     * Checks if the single sign on token is valid.
     *
     * @param token single sign on token.
     * @throws SSOException if the single sign on token is not valid.
     */
    public void validateToken(SSOToken token) throws SSOException {
        try {
            /*
             * if the token was created from createSSOToken(Principal, password)
             * there is no association with session. Use this temp solution now.
             * if this method is going to go away, we can remove that method.
             * otherwise a better mechanism has to be implemented.
             */
            SSOTokenImpl tokenImpl = (SSOTokenImpl) token;
            tokenImpl.validate();
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("validateToken: ", e);
            }
            throw new SSOException(SSOProviderBundle.rbName, "invalidtoken", null);
        }
    }

    /**
     * Destroys a single sign on token
     *
     * @param token The single sign on token object to be destroyed
     * @throws SSOException if the given token cannot be destroyed
     */
    public void destroyToken(SSOToken token) throws SSOException {
        try {
            SSOTokenImpl tokenImpl = (SSOTokenImpl) token;
            if (tokenImpl.isLdapConnection() == true) {
                tokenImpl.setStatus(false);
                return;
            }
            SSOTokenID tokenid = token.getTokenID();
            String id = tokenid.toString();
            SessionID sessid = new SessionID(id);
            Session session = sessionCache.getSession(sessid);
            session.destroySession(session);
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("DestroyToken: ", e);
            }
            throw new SSOException(e);
        }
    }

    @Override
    public void logout(final SSOToken token) throws SSOException {
        try {
            Session session = sessionCache.getSession(new SessionID(token.getTokenID().toString()));
            session.logout();
        } catch (SessionException e) {
            if (debug.messageEnabled()) {
                debug.message("Logout: ", e);
            }
            throw new SSOException(e);
        }
    }

    /**
     * Validate the IP address of the client with the IP stored in Session.
     *
     * @param sess     Session object associated with the token
     * @param clientIP IP address of the current client who made
     *                 <code>HttpRequest</code>.
     * @return Returns true if the IP is valid else false.
     * @throws SSOException if IP cannot be validated for the given session
     */
    public boolean isIPValid(Session sess, String clientIP) throws SSOException {
        boolean check = false;
        try {
            InetAddress sessIPAddress = InetAddress.getByName(sess
                    .getProperty("Host"));
            InetAddress clientIPAddress = InetAddress.getByName(clientIP);
            if (sessIPAddress.equals(clientIPAddress)) {
                check = true;
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("IP address check of Token Failed", e);
            }
        }
        return check;
    }

    /**
     * Refresh the Session corresponding to the single sign on token from the
     * Session Server.
     *
     * @param token single sign on token for which session need to be refreshed
     * @throws SSOException if the session cannot be refreshed
     */
    @Override
    public void refreshSession(SSOToken token) throws SSOException {
        refreshSession(token, true);
    }

    /**
     * Refresh the Session corresponding to the single sign on token from the
     * Session Server.
     *
     * @param token single sign on token for which session need to be refreshed.
     * @param possiblyResetIdleTime if true, the idle time may be reset, if false it will never be.
     * @throws SSOException if the session cannot be refreshed.
     */
    @Override
    public void refreshSession(SSOToken token, boolean possiblyResetIdleTime) throws SSOException {
        try {
            SSOTokenID tokenId = token.getTokenID();
            SessionID sid = new SessionID(tokenId.toString());
            Session session = sessionCache.getSession(sid, false, false); //Get session without refreshing idle time
            session.refresh(possiblyResetIdleTime);
        } catch (Exception e) {
            debug.error("Error in refreshing the session from sessions server");
            throw new SSOException(e);
        }
    }

    /**
     * Destroys a single sign on token.
     *
     * @param destroyer
     *            The single sign on token object used to authorize the
     *            operation
     * @param destroyed
     *            The single sign on token object to be destroyed.
     * @throws SSOException
     *             if the there was an error during communication with session
     *             service.
     *
     * 
     */
    public void destroyToken(SSOToken destroyer, SSOToken destroyed)
            throws SSOException {
        try {
            Session requester = ((SSOTokenImpl) destroyer).getSession();
            Session target = ((SSOTokenImpl) destroyed).getSession();
            requester.destroySession(target);
        } catch (SessionException e) {
            throw new SSOException(e);
        }
    }

    /**
     * Returns a list of single sign on token objects
     * which correspond to valid Sessions accessible to requester. single sign
     * on token objects returned are restricted: they can only be used to
     * retrieve properties and destroy sessions they represent.
     *
     * @param requester
     *            The single sign on token object used to authorize the
     *            operation
     * @param server
     *            The server for which the valid sessions are to be retrieved
     * @return Set of Valid Sessions
     * @throws SSOException
     *             if the there was an error during communication with session
     *             service.
     *
     * 
     */
    public Set<SSOToken> getValidSessions(SSOToken requester, String server)
            throws SSOException {
        Set<SSOToken> results = new HashSet<>();
        try {
            SearchResults<Session> result = ((SSOTokenImpl) requester).getSession().getValidSessions(server, null);

            for (Session s : result.getSearchResults()) {
                if (s != null) {
                    results.add(new SSOTokenImpl(s));
                }
            }
        } catch (SessionException e) {
            throw new SSOException(e);
        }
        return results;
    }
}
