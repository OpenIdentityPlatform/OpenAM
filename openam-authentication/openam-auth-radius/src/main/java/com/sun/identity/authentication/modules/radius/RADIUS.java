/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AccessAccept.java,v 1.2 2008/06/25 05:42:00 qcheng Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions Copyrighted [2015] [Intellectual Reserve, Inc (IRI)]
 */
package com.sun.identity.authentication.modules.radius;

import com.sun.identity.authentication.modules.radius.client.ChallengeException;
import com.sun.identity.authentication.modules.radius.client.RadiusConn;
import com.sun.identity.authentication.modules.radius.client.RejectException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.io.IOException;
import java.net.SocketException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * An authentication module that defers to remote radius servers to validate username and password. It includes admin
 * console configuration pages for identifying the remote servers and various parameters.
 */
public class RADIUS extends AMLoginModule {
    private static final String DEFAULT_TIMEOUT = "5";
    private static final String DEFAULT_SERVER_PORT = "1645";
    private static final String DEFAULT_INTERVAL = "5";
    private static final String AM_AUTH_RADIUS = "amAuthRadius";
    // initial state
    private Map sharedState;
    private String userTokenId = null;
    private String challengeID;

    // the authentication status
    private boolean succeeded = false;
    private RADIUSPrincipal userPrincipal = null;
    private String username;
    private static Debug debug = Debug.getInstance(AM_AUTH_RADIUS);

    private Set<RADIUSServer> primaryServers;
    private Set<RADIUSServer> secondaryServers;
    private String sharedSecret;
    private int iServerPort = 1645;
    private int iTimeOut = 5;
    private int healthCheckInterval = 5;
    private RadiusConn radiusConn = null;
    private boolean getCredentialsFromSharedState;
    private ChallengeException cException = null;

    /**
     * Initializes this <code>LoginModule</code>.
     *
     * @param subject
     *            the <code>Subject</code> to be authenticated.
     * @param sharedState
     *            shared <code>LoginModule</code> state.
     * @param options
     *            options specified in the login. <code>Configuration</code> for this particular
     *            <code>LoginModule</code>.
     */
    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        try {
            if (debug.messageEnabled()) {
                debug.message("Radius resbundle locale=" + getLoginLocale());
            }

            this.sharedState = sharedState;

            if (options != null) {
                try {
                    String serverPort = CollectionHelper.getMapAttr(options, "iplanet-am-auth-radius-server-port",
                            DEFAULT_SERVER_PORT);
                    iServerPort = Integer.parseInt(serverPort);

                    primaryServers = new LinkedHashSet<RADIUSServer>();
                    Set<String> tmp;
                    tmp = CollectionHelper.getServerMapAttrs(options, "iplanet-am-auth-radius-server1");
                    if (tmp.isEmpty()) {
                        primaryServers.add(new RADIUSServer("localhost", iServerPort));
                        debug.error("Error: primary server attribute " + "misconfigured using localhost");
                    }
                    for (String server : tmp) {
                        int idx = server.indexOf(':');
                        if (idx == -1) {
                            primaryServers.add(new RADIUSServer(server, iServerPort));
                        } else {
                            primaryServers.add(new RADIUSServer(server.substring(0, idx), Integer.parseInt(server
                                    .substring(idx + 1))));
                        }
                    }

                    secondaryServers = new LinkedHashSet<RADIUSServer>();
                    tmp = CollectionHelper.getServerMapAttrs(options, "iplanet-am-auth-radius-server2");

                    if (tmp == null) {
                        secondaryServers.add(new RADIUSServer("localhost", iServerPort));
                        debug.error("Error: primary server attribute " + "misconfigured using localhost");
                    }
                    for (String server : tmp) {
                        int idx = server.indexOf(':');
                        if (server.indexOf(':') == -1) {
                            secondaryServers.add(new RADIUSServer(server, iServerPort));
                        } else {
                            secondaryServers.add(new RADIUSServer(server.substring(0, idx), Integer.parseInt(server
                                    .substring(idx + 1))));
                        }
                    }

                    sharedSecret = CollectionHelper.getMapAttr(options, "iplanet-am-auth-radius-secret");

                    String timeOut = CollectionHelper.getMapAttr(options, "iplanet-am-auth-radius-timeout",
                            DEFAULT_TIMEOUT);
                    iTimeOut = Integer.parseInt(timeOut);
                    String authLevel = CollectionHelper.getMapAttr(options, "iplanet-am-auth-radius-auth-level");

                    String interval = CollectionHelper.getMapAttr(options, "openam-auth-radius-healthcheck-interval",
                            DEFAULT_INTERVAL);
                    healthCheckInterval = Integer.parseInt(interval);

                    if (authLevel != null) {
                        try {
                            setAuthLevel(Integer.parseInt(authLevel));
                        } catch (Exception e) {
                            debug.error("Unable to set auth level " + authLevel);
                        }
                    }

                    if (debug.messageEnabled()) {
                        debug.message("server1: " + primaryServers + " server2: " + secondaryServers + " serverPort: "
                                + serverPort + " timeOut: " + timeOut + " authLevel: " + authLevel);
                    }

                    if ((sharedSecret == null) || (sharedSecret.length() == 0)) {
                        debug.error("RADIUS initialization failure; no Shared Secret");
                    }
                } catch (Exception ex) {
                    debug.error("RADIUS parameters initialization failure", ex);
                }
            } else {
                debug.error("options not initialized");
            }

        } catch (Exception e) {
            debug.error("RADIUS init Error....", e);
        }
    }

    private void setDynamicText(int state) throws AuthLoginException {
        // Callbacks may not be initialized or may contain stale data, we need to re-read them.
        setForceCallbacksRead(true);
        forceCallbacksInit();
        Callback[] callbacks = getCallback(state);
        String prompt = ((PasswordCallback) callbacks[0]).getPrompt();
        boolean echo = ((PasswordCallback) callbacks[0]).isEchoOn();

        if (challengeID != null) {
            prompt += "[" + challengeID + "]: ";
        }

        callbacks[0] = new PasswordCallback(prompt, echo);
        replaceCallback(state, 0, callbacks[0]);
    }

    /**
     * Takes an array of submitted <code>Callback</code>, process them and decide the order of next state to go. Return
     * STATE_SUCCEED if the login is successful, return STATE_FAILED if the LoginModule should be ignored.
     *
     * @param callbacks
     *            an array of <code>Callback</code> for this Login state
     * @param state
     *            order of state. State order starts with 1.
     * @return int order of next state. Return STATE_SUCCEED if authentication is successful, return STATE_FAILED if the
     *         LoginModule should be ignored.
     * @throws AuthLoginException if the user fails authentication or some anomalous condition occurs
     */
    @Override
    public int process(Callback[] callbacks, int state) throws AuthLoginException {
        String tmpPasswd = null;
        String sState;

        switch (state) {
        case ISAuthConstants.LOGIN_START:
            try {
                radiusConn = new RadiusConn(primaryServers, secondaryServers, sharedSecret, iTimeOut,
                        healthCheckInterval);
            } catch (SocketException se) {
                debug.error("RADIUS login failure; Socket Exception se == ", se);
                shutdown();
                throw new AuthLoginException(AM_AUTH_RADIUS, "RadiusNoServer", null);
            } catch (Exception e) {
                debug.error("RADIUS login failure; Can't connect to RADIUS server", e);
                shutdown();
                throw new AuthLoginException(AM_AUTH_RADIUS, "RadiusNoServer", null);
            }

            if (callbacks != null && callbacks.length == 0) {
                username = (String) sharedState.get(getUserKey());
                tmpPasswd = (String) sharedState.get(getPwdKey());

                if (username == null || tmpPasswd == null) {
                    return ISAuthConstants.LOGIN_START;
                }

                getCredentialsFromSharedState = true;
            } else {
                username = ((NameCallback) callbacks[0]).getName();
                tmpPasswd = charToString(((PasswordCallback) callbacks[1]).getPassword(), callbacks[1]);

                if (debug.messageEnabled()) {
                    debug.message("username: " + username);
                }
            }

            storeUsernamePasswd(username, tmpPasswd);

            try {
                succeeded = false;
                radiusConn.authenticate(username, tmpPasswd);
            } catch (RejectException re) {
                if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                    getCredentialsFromSharedState = false;
                    return ISAuthConstants.LOGIN_START;
                }

                if (debug.messageEnabled()) {
                    debug.message("Radius login request rejected", re);
                }

                shutdown();
                setFailureID(username);
                throw new InvalidPasswordException(AM_AUTH_RADIUS, "RadiusLoginFailed", null, username, re);
            } catch (IOException ioe) {
                if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                    getCredentialsFromSharedState = false;
                    return ISAuthConstants.LOGIN_START;
                }

                debug.error("Radius request IOException", ioe);
                shutdown();
                setFailureID(username);
                throw new AuthLoginException(AM_AUTH_RADIUS, "RadiusLoginFailed", null);
            } catch (java.security.NoSuchAlgorithmException ne) {
                if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                    getCredentialsFromSharedState = false;
                    return ISAuthConstants.LOGIN_START;
                }

                debug.error("Radius No Such Algorithm Exception", ne);
                shutdown();
                setFailureID(username);
                throw new AuthLoginException(AM_AUTH_RADIUS, "RadiusLoginFailed", null);
            } catch (ChallengeException ce) {
                if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                    getCredentialsFromSharedState = false;
                    return ISAuthConstants.LOGIN_START;
                }

                cException = ce;
                sState = ce.getState();

                if (sState == null) {
                    debug.error("Radius failure - no state returned in challenge");
                    shutdown();
                    setFailureID(username);
                    throw new AuthLoginException(AM_AUTH_RADIUS, "RadiusAuth", null);
                }

                challengeID = ce.getReplyMessage();

                if (debug.messageEnabled()) {
                    debug.message("Server challenge with " + "challengeID: " + challengeID);
                }

                setDynamicText(2);

                return ISAuthConstants.LOGIN_CHALLENGE;
            } catch (Exception e) {
                if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                    getCredentialsFromSharedState = false;
                    return ISAuthConstants.LOGIN_START;
                }

                shutdown();
                setFailureID(username);
                throw new AuthLoginException(AM_AUTH_RADIUS, "RadiusLoginFailed", null, e);
            }

            succeeded = true;
            break;

        case ISAuthConstants.LOGIN_CHALLENGE:
            String passwd = getChallengePassword(callbacks);

            if (debug.messageEnabled()) {
                debug.message("reply to challenge--username: " + username);
            }

            try {
                succeeded = false;
                radiusConn.replyChallenge(username, passwd, cException);
            } catch (ChallengeException ce) {
                sState = ce.getState();

                if (sState == null) {
                    debug.error("handle Challenge failure - no state returned");
                    shutdown();
                    setFailureID(username);
                    throw new AuthLoginException(AM_AUTH_RADIUS, "RadiusLoginFailed", null);
                }

                resetCallback(2, 0);
                challengeID = ce.getReplyMessage();

                if (debug.messageEnabled()) {
                    debug.message("Server challenge again with challengeID: " + challengeID);
                }

                cException = ce; // save it for next replyChallenge
                setDynamicText(2);
                // note that cException is reused

                return ISAuthConstants.LOGIN_CHALLENGE;
            } catch (RejectException ex) {
                debug.error("Radius challenge response rejected", ex);
                shutdown();
                setFailureID(username);
                throw new InvalidPasswordException(AM_AUTH_RADIUS, "RadiusLoginFailed", null, username, ex);
            } catch (IOException ioe) {
                debug.error("Radius challenge IOException", ioe);
                shutdown();
                setFailureID(username);
                throw new AuthLoginException(AM_AUTH_RADIUS, "RadiusLoginFailed", null);
            } catch (java.security.NoSuchAlgorithmException ex) {
                debug.error("Radius No Such Algorithm Exception", ex);
                shutdown();
                setFailureID(username);
                throw new AuthLoginException(AM_AUTH_RADIUS, "RadiusLoginFailed", null);
            } catch (Exception e) {
                debug.error("RADIUS challenge Authentication Failed ", e);
                shutdown();
                setFailureID(username);
                throw new AuthLoginException(AM_AUTH_RADIUS, "RadiusLoginFailed", null);
            }
            succeeded = true;
            break;

        default:
            debug.error("RADIUS Authentication Failed - invalid state" + state);
            shutdown();
            succeeded = false;
            setFailureID(username);
            throw new AuthLoginException(AM_AUTH_RADIUS, "RadiusLoginFailed", null);
        }

        if (succeeded) {
            if (debug.messageEnabled()) {
                debug.message("RADIUS authentication successful");
            }

            if (username != null) {
                StringTokenizer usernameToken = new StringTokenizer(username, ",");
                userTokenId = usernameToken.nextToken();
            }

            if (debug.messageEnabled()) {
                debug.message("userTokenID: " + userTokenId);
            }

            shutdown();
            return ISAuthConstants.LOGIN_SUCCEED;
        } else {
            if (debug.messageEnabled()) {
                debug.message("RADIUS authentication to be ignored");
            }
            return ISAuthConstants.LOGIN_IGNORE;
        }
    }

    /**
     * Returns <code>java.security.Principal</code>.
     *
     * @return <code>java.security.Principal</code>
     */
    @Override
    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        } else if (userTokenId != null) {
            userPrincipal = new RADIUSPrincipal(userTokenId);
            return userPrincipal;
        } else {
            return null;
        }
    }

    /**
     * Destroy the module state.
     */
    @Override
    public void destroyModuleState() {
        userTokenId = null;
        userPrincipal = null;
    }

    /**
     * Set all the used variables to null.
     */
    @Override
    public void nullifyUsedVars() {
        sharedState = null;
        challengeID = null;
        primaryServers = null;
        secondaryServers = null;
        sharedSecret = null;
    }

    private String getChallengePassword(Callback[] callbacks) throws AuthLoginException {
        // callback[0] is for password(also display challenge text)
        char[] tmpPassword = ((PasswordCallback) callbacks[0]).getPassword();

        if (tmpPassword == null) {
            // treat a NULL password as an empty password
            tmpPassword = new char[0];
        }

        char[] pwd = new char[tmpPassword.length];
        System.arraycopy(tmpPassword, 0, pwd, 0, tmpPassword.length);
        ((PasswordCallback) callbacks[0]).clearPassword();

        return (new String(pwd));
    }

    private String charToString(char[] tmpPassword, Callback cbk) {
        if (tmpPassword == null) {
            // treat a NULL password as an empty password
            tmpPassword = new char[0];
        }

        char[] pwd = new char[tmpPassword.length];
        System.arraycopy(tmpPassword, 0, pwd, 0, tmpPassword.length);
        ((PasswordCallback) cbk).clearPassword();

        return new String(pwd);
    }

    /**
     * Shutdown the RADIUS connection.
     */
    public void shutdown() {
        try {
            radiusConn.disconnect();
        } catch (IOException e) {
            // ignore since we are disconnecting
        }

        radiusConn = null;
    }

}
