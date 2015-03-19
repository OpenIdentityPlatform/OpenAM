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
 */

/*
 * Portions Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sso.providers.stateless;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionListener;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenListener;
import com.iplanet.sso.providers.dpro.SSOPrincipal;
import com.iplanet.sso.providers.dpro.SSOProviderBundle;
import com.iplanet.sso.providers.dpro.SSOSessionListener;
import com.iplanet.sso.providers.dpro.SSOTokenIDImpl;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.session.SessionURL;
import org.forgerock.openam.utils.StringUtils;

import java.net.InetAddress;
import java.security.Principal;

/**
 * {@link SSOToken} implementation for stateless (client-side) sessions. The state of the session is encoded into a
 * signed (and possibly encrypted) JWT (JSON Web Token) and stored directly in the client cookie. No state is
 * maintained on the server, unless session blacklisting (for logout) is enabled.
 *
 * @since 13.0.0
 */
final class StatelessSSOToken implements SSOToken {
    private static final Debug DEBUG = Debug.getInstance("amSSOProvider");
    private final StatelessSession session;

    StatelessSSOToken(StatelessSession session) {
        this.session = session;
    }

    /**
     * Confirms that the SSOToken is valid by checking its required JWT values.
     */
    public boolean isValid() {
        try {
            return !session.isTimedOut();
        } catch (SessionException e) {
            return false;
        }
    }

    @Override
    public Principal getPrincipal() throws SSOException {
        try {
            String name = session.getProperty(ISAuthConstants.PRINCIPAL);
            return new SSOPrincipal(name);
        } catch (Exception e) {
            DEBUG.message("Can't get token principal name", e);
            throw new SSOException(e);
        }
    }

    @Override
    public String getAuthType() throws SSOException {
        try {
            // auth type may be a list of auth types separated by "|". This can
            // happen because of session upgrade. The list is assumed to have
            // a format like "Ldap|Cert|Radius" with no space between separator.
            // this method simply returns the first auth method in that list.
            String types = session.getProperty(ISAuthConstants.AUTH_TYPE);
            int index = types.indexOf("|");
            if (index != -1) {
                return types.substring(0, index);
            } else {
                return types;
            }
        } catch (Exception e) {
            DEBUG.error("Can't get token authentication type", e);
            throw new SSOException(e);
        }
    }

    @Override
    public int getAuthLevel() throws SSOException {
        try {
            // The property AuthLevel may contain realm information, e.g. "/:10". If so, strip this out.
            String authLevelFull = session.getProperty(ISAuthConstants.AUTH_LEVEL);
            int indexOfStartOfIntegerPart = authLevelFull.lastIndexOf(':') + 1;
            String authLevelInteger = authLevelFull.substring(indexOfStartOfIntegerPart);
            return Integer.parseInt(authLevelInteger);
        } catch (Exception e) {
            DEBUG.error("Can't get token authentication level", e);
            throw new SSOException(e);
        }
    }

    @Override
    public InetAddress getIPAddress() throws SSOException {
        try {
            String host = session.getProperty(ISAuthConstants.HOST);
            if (StringUtils.isBlank(host)) {
                throw new SSOException(SSOProviderBundle.rbName, "ipaddressnull", null);
            }
            return InetAddress.getByName(host);
        } catch (Exception e) {
            DEBUG.error("Can't get client's IPAddress", e);
            throw new SSOException(e);
        }
    }

    @Override
    public String getHostName() throws SSOException {
        try {
            String hostName = session.getProperty(ISAuthConstants.HOST_NAME);
            if (StringUtils.isBlank(hostName)) {
                throw new SSOException(SSOProviderBundle.rbName, "hostnull", null);
            }
            return hostName;
        } catch (Exception e) {
            DEBUG.error("Can't get client's token Host name", e);
            throw new SSOException(e);
        }
    }

    @Override
    public long getTimeLeft() throws SSOException {
        try {
            return session.getTimeLeft();
        } catch (SessionException e) {
            throw new SSOException(e);
        }
    }

    @Override
    public long getMaxSessionTime() throws SSOException {
        return session.getMaxSessionTime();
    }

    @Override
    public long getIdleTime() throws SSOException {
        try {
            return session.getIdleTime();
        } catch (SessionException e) {
            throw new SSOException(e);
        }
    }

    @Override
    public long getMaxIdleTime() throws SSOException {
        return session.getMaxIdleTime();
    }

    @Override
    public SSOTokenID getTokenID() {
        return new SSOTokenIDImpl(session.getID());
    }

    @Override
    public void setProperty(String name, String value) throws SSOException {
        try {
            session.setProperty(name, value);
        } catch (SessionException e) {
            throw new SSOException(e);
        }
    }

    @Override
    public String getProperty(String name) throws SSOException {
        try {
            return session.getProperty(name);
        } catch (SessionException e) {
            throw new SSOException(e);
        }
    }

    @Override
    public String getProperty(String name, boolean ignoreState) throws SSOException {
        try {
            // Always try the normal way first to ensure state is refreshed
            return getProperty(name);
        } catch (SSOException e) {
            if (!ignoreState) {
                throw e;
            }
            DEBUG.message("Getting session property {} failed: {}. Falling back to getting without validation", name,
                    e);
            return session.getPropertyWithoutValidation(name);
        }
    }

    @Override
    public void addSSOTokenListener(SSOTokenListener listener) throws SSOException {
        try {
            SessionListener ssoListener = new SSOSessionListener(listener);
            session.addSessionListener(ssoListener);
        } catch (Exception e) {
            DEBUG.error("Couldn't add listener to the token {}", session.getID(), e);
            throw new SSOException(e);
        }
    }

    @Override
    public String encodeURL(String url) throws SSOException {
        return SessionURL.getInstance().encodeURL(url, session);
    }

    @Override
    public boolean isTokenRestricted() throws SSOException {
        // Stateless sessions do not support restricted tokens
        return false;
    }

    @Override
    public String dereferenceRestrictedTokenID(SSOToken requester, String restrictedId) throws SSOException {
        DEBUG.warning("Unsupported request to dereference restricted token for StatelessSSOToken");
        throw new UnsupportedOperationException(StatelessSession.RESTRICTED_TOKENS_UNSUPPORTED);
    }

    @Override
    public String toString() {
        return session.getID().toString();
    }

}
