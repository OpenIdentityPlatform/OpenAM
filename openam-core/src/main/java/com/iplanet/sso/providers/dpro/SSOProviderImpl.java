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
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */
package com.iplanet.sso.providers.dpro;

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
import org.forgerock.openam.utils.ClientUtils;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * This <code>final</code> class <code>SSOProviderImpl</code> implements 
 * <code>SSOProvider</code> interface and provides implementation of the methods
 * to create , destroy , check the validity of a single sign on token.
 *
 * @supported.api
 */
public final class SSOProviderImpl implements SSOProvider {

    /** Debug SSOProvider */
    public static Debug debug = null;

    /** Check to see if the clientIPCheck is enabled */
    private static boolean checkIP = Boolean.valueOf(
            SystemProperties.get("com.iplanet.am.clientIPCheckEnabled"))
            .booleanValue();

    // Initialize debug instance;
    static {
        debug = Debug.getInstance("amSSOProvider");
    }

    /**
     * Constructs a instance of <code>SSOProviderImpl</code>
     * 
     * @throws SSOException
     *
     * @supported.api
     */
    public SSOProviderImpl() throws SSOException {
    }

    /**
     * Creates a single sign on token for the <code>HttpRequest</code>
     *
     * @param request <code>HttpServletRequest</code>
     * @return single sign on token for the request
     * @exception SSOException if the single sign on token cannot be created.
     */
    public SSOToken createSSOToken(HttpServletRequest request)
            throws SSOException {
        try {
            SessionID sid = new SessionID(request);
            Session session = Session.getSession(sid);
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
                debug.message("could not create SSOToken from HttpRequest", e);
            }
            throw new SSOException(e);
        }
    }

    /** Creates a single sign on token with user or service as the entity
     * 
     * @param user Principal representing a user or service
     * @param password password string.
     * @return single sign on token
     * @exception SSOException if the single sign on token cannot be created.
     * @exception UnsupportedOperationException Thrown to indicate that the 
     * requested operation is not supported.
     * @deprecated This method has been deprecated. Please use the
     * regular LDAP authentication mechanism instead. More information 
     * on how to use the authentication programming interfaces as well as the
     *             code samples can be obtained from the "Authentication
     *             Service" chapter of the OpenSSO Developer's Guide.
     */
    public SSOToken createSSOToken(java.security.Principal user,
            String password) throws SSOException, UnsupportedOperationException 
    {
        try {
            SSOTokenImpl ssoToken = new SSOTokenImpl(user, password);
            if (debug.messageEnabled()) {
                debug.message("SSO token ldap auth successful for "
                        + user.toString());
            }
            return ssoToken;
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("could not create SSOTOken for user "
                        + user.getName(), e);
            }
            throw new SSOException(e);
        }
    }

    /**
     * Creates a single sign on token. Note: this method should remain private
     * and get called only by the AuthContext API.
     * 
     * @param tokenId single sign on token ID.
     * @param invokedByAuth boolean flag indicating that this method has
     *        been invoked by the AuthContext.getSSOToken() API.
     * @return single sign on token.
     * @exception SSOException if the single sign on token cannot be created.
     * @exception UnsupportedOperationException Thrown to indicate that the 
     * requested operation is not supported.
     */
    public SSOToken createSSOToken(String tokenId, boolean invokedByAuth) 
            throws SSOException, UnsupportedOperationException 
    {
        try {
            SessionID sessionId = new SessionID(tokenId);
            sessionId.setComingFromAuth(invokedByAuth);
            Session session = Session.getSession(sessionId);
            SSOToken ssoToken = new SSOTokenImpl(session);
            return ssoToken;
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("SSOProviderImpl.createSSOToken(tokenId, boolean)"
                    +"could not create SSOTOken for token ID " + tokenId, e);
            }
            throw new SSOException(e);
        }
    }

     /**
      * Creates a single sign on token.
      *
      * @param tokenId single sign on token ID.
      * @return single sign on token.
      * @exception SSOException if the single sign on token cannot be created.
      * @exception UnsupportedOperationException
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
     * @param tokenId
     *            single sign on token ID.
     * @param clientIP client IP address
     * @return single sign on token.
     * @exception SSOException if the single sign on token cannot be created.
     * @exception UnsupportedOperationException Thrown to indicate that the 
     * requested operation is not supported.
     * @deprecated Use #createSSOToken(String, String)
     */
    public SSOToken createSSOToken(String tokenId, String clientIP)
            throws SSOException, UnsupportedOperationException {
        try {
            SessionID sessionId = new SessionID(tokenId);
            Session session = Session.getSession(sessionId);
            if (checkIP && !isIPValid(session, clientIP)) {
                throw new Exception(SSOProviderBundle.getString("invalidIP"));
            }
            SSOToken ssoToken = new SSOTokenImpl(session);
            return ssoToken;
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("could not create SSOTOken for token ID "
                        + tokenId, e);
            }
            throw new SSOException(e);
        }
    }

   /**
    * Checks the validity of the single sign on token
    * @param token The single sign on token object to be validated
    * @return Returns true if the <code>SSOToken</code> is valid
    */
    public boolean isValidToken(SSOToken token) {
        /*
         * if the token was created from createSSOToken(Principal, password)
         * there is no association with session. Use this temp solution now. if
         * this method is going to go away, we can remove that method. otheriwse
         * a better mechanism has to be implemented.
         */
        SSOTokenImpl tokenImpl = (SSOTokenImpl) token;
        return (tokenImpl.isValid());
    }

    /**
     * Checks if the single sign on token is valid.
     * 
     * @param token
     *            single sign on token.
     * @exception SSOException
     *                if the single sign on token is not valid.
     */
    public void validateToken(SSOToken token) throws SSOException {
        try {
            /*
             * if the token was created from createSSOToken(Principal, password)
             * there is no association with session. Use this temp solution now.
             * if this method is going to go away, we can remove that method.
             * otheriwse a better mechanism has to be implemented.
             */
            SSOTokenImpl tokenImpl = (SSOTokenImpl) token;
            tokenImpl.validate();
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("validateToken: ", e);
            }
            throw new SSOException(SSOProviderBundle.rbName, "invalidtoken",
                    null);
        }
    }

    /** Destroys a single sign on token
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
            Session session = Session.getSession(sessid);
            session.destroySession(session);
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("DestroyToken: ", e);
            }
            throw new SSOException(e);
        }
    }

     /**
      * Validate the IP address of the client with the IP stored in Session.
      * @param sess Session object associated with the token
      * @param clientIP IP address of the current client who made 
      * <code>HttpRequest</code>.
      * @return Returns true if the IP is valid else false.
      * @throws SSOException if IP cannot be validated for the given session
      */
    public boolean isIPValid(Session sess, String clientIP) throws SSOException
    {
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
     * @param token single sign on token for which session need to be refreshed
     * @throws SSOException if the session cannot be refreshed
     * 
     */
    public void refreshSession(SSOToken token) throws SSOException {
        try {
            SSOTokenID tokenId = token.getTokenID();
            SessionID sid = new SessionID(tokenId.toString());
            Session session = Session.getSession(sid);
            session.refresh(true);
        } catch (Exception e) {
            debug.error("Error in refreshing the session from sessions erver");
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
     * @supported.api
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
     * @supported.api
     */
    public Set getValidSessions(SSOToken requester, String server)
            throws SSOException {
        Set results = new HashSet();
        try {

            SearchResults result = ((SSOTokenImpl) requester).getSession()
                    .getValidSessions(server, null);

            for (Iterator iter = result.getResultAttributes().values()
                    .iterator(); iter.hasNext();) {
                Session s = (Session) iter.next();
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
