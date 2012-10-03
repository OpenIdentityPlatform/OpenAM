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
 * $Id: AuthSSOToken.java,v 1.4 2009/01/16 10:49:02 manish_rustagi Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */

package com.sun.identity.authentication.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import javax.security.auth.login.LoginException;

import com.sun.identity.shared.encode.Base64;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenListener;
import com.sun.identity.authentication.util.ISAuthConstants;

public class AuthSSOToken implements SSOToken {

    // %%% this should finally reside in HttpSession
    protected static Map allSSOTokens = Collections
            .synchronizedMap(new HashMap());

    // Variables to generate Keys
    protected static Random random = new Random();

    public static final int INVALID = 0;

    public static final int VALID = 1;

    public static final int DESTROYED = 3;

    protected AuthContext authContext;

    // %% session information should also get into HttpSession
    protected HashMap session = new HashMap();

    protected HashSet callbackObjects = new HashSet();

    protected String key;

    private AuthSSOToken() {
        // cannot be constructed
    }

    protected AuthSSOToken(AuthContext authc)
            throws InvalidAuthContextException {

        if (authc == null)
            throw (new InvalidAuthContextException());

        if (authc.getLoginStatus() != AuthContext.AUTH_SUCCESS)
            throw (new InvalidAuthContextException());

        authContext = authc;

        // Generate key (or tokenID);
        key = getNewKey();
        allSSOTokens.put(key, this);
    }

    protected static String getNewKey() {
        byte[] keyRandom = new byte[12];
        random.nextBytes(keyRandom);
        String key = Base64.encode(keyRandom);
        try {
            InetAddress.getLocalHost().getAddress();
            key += Base64.encode((InetAddress.getLocalHost()).getAddress());
        } catch ( UnknownHostException unknownHostException) {
            // This issue is due to Bug:  	MACOSX_PORT-564
            // @see http://java.net/jira/browse/MACOSX_PORT-564
            if (System.getProperty("os.name").toLowerCase().contains("mac"))
            {
                System.out.println("Unknown Host Exception Encountered, could be related to MACOSX_PORT-564,\n"
                        +unknownHostException.getCause());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        key += Long.toString(System.currentTimeMillis());
        return (Base64.encode(key.getBytes()));
    }

    public Principal getPrincipal() throws SSOException {
        return (authContext.getPrincipal());
    }

    public String getAuthType() throws SSOException {
        return ("ldap");
    }

    public int getAuthLevel() throws SSOException {
        return (1);
    }

    public InetAddress getIPAddress() throws SSOException {
        try {
            String host = getProperty("Host");
            return InetAddress.getByName(host);
        } catch (Exception e) {
            throw (new SSOException(e));
        }
    }

    public String getHostName() throws SSOException {
        try {
            String hostName = getProperty("HostName");
            return hostName;
        } catch (Exception e) {
            throw (new SSOException(e));
        }
    }

    public byte[] getAddress() throws SSOException {
        return (getIPAddress().getAddress());
    }

    public long getTimeLeft() throws SSOException {
        return (0);
    }

    public String encodeURL(String url) {
        return null;
    }

    public long getMaxSessionTime() throws SSOException {
        return (0);
    }

    public long getIdleTime() throws SSOException {
        return (0);
    }

    public long getMaxIdleTime() throws SSOException {
        return (-1);
    }

    public SSOTokenID getTokenID() {
        return (new AuthSSOTokenID(key));
    }

    protected int getState() throws SSOException {
        if ((authContext != null)
                && (authContext.getLoginStatus() == AuthContext.AUTH_SUCCESS))
            return (VALID);
        return (INVALID);
    }

    public void setProperty(String name, String value) throws SSOException {
        session.put(name, value);
    }

    public String getProperty(String name) throws SSOException {
        return ((String) session.get(name));
    }

    public String getProperty(String name, boolean ignoreState)
        throws SSOException {
        return ((String) session.get(name));
    }

    public void addSSOTokenListener(SSOTokenListener listener)
            throws SSOException {
        callbackObjects.add(listener);
    }

    protected boolean isValid() {
        if ((authContext != null)
                && (authContext.getLoginStatus() == AuthContext.AUTH_SUCCESS))
            return (true);
        return (false);
    }

    protected void validate() throws SSOException {
        if ((authContext != null)
                && (authContext.getLoginStatus() == AuthContext.AUTH_SUCCESS))
            return;
        throw new SSOException(ISAuthConstants.AUTH_BUNDLE_NAME,
                "invalidcontext", null);
    }

    protected void invalidate() {
        // Remove itself from allSSOTokens MAP
        allSSOTokens.remove(key);
        try {
            authContext.logout();
        } catch (LoginException le) {
            // do nothing
        }
    }

    public boolean isTokenRestricted() throws SSOException {
        throw new UnsupportedOperationException("This method is not supported");
    }

    public String dereferenceRestrictedTokenID(SSOToken requester, String restrictedId)
    throws SSOException {
        throw new UnsupportedOperationException("This method is not supported");
    }

    protected class AuthSSOTokenID implements SSOTokenID {

        private String tokenID;

        protected AuthSSOTokenID(String t) {
            tokenID = t;
        }

        public String toString() {
            return (tokenID);
        }

        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object == null || getClass() != object.getClass()) {
                return false;
            }
            AuthSSOTokenID token = (AuthSSOTokenID) object;
            return (tokenID.equals(token.tokenID));
        }

        public int hashCode() {
            return authContext.hashCode();
        }

    }
}
