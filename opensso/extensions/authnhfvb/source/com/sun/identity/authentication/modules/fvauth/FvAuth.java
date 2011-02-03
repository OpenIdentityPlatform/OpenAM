/* The contents of this file are subject to the terms
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
 * FvAuth.java
 *
 * Created on 2007/09/20, 21:11 
 * @author yasushi.iwakata@sun.com
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.authentication.modules.fvauth;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Finger Vein Authentication Login Module.
 */
public class FvAuth extends AMLoginModule {

    private String userTokenId = null;
    private Principal userPrincipal = null;
    private FvChallengeBean cbean = null;
    private FvValidationProxy vproxy;
    private static final String AM_FV_AUTH = "amFvAuth";
    private static Debug debug;

    static {
        if (debug == null) {
            debug = com.sun.identity.shared.debug.Debug.getInstance(AM_FV_AUTH);
        }
    }

    /**
     * Creates <code>FvAuth</code> object.
     *
     * @throws LoginException if class cannot be instantiated.
     */
    public FvAuth() throws LoginException {
        debug.message("FvAuth Login Module loaded.");
    }

    /**
     * Initializes the module.
     *
     * @param subject
     * @param sharedState
     * @param options
     */
    public void init(Subject subject, Map sharedState, Map options) {
        debug.message("FvAuth Login Module Initialization Started.");
    }

    /**
     * Processes the callback requests.
     *
     * @param callbacks Array of callback object.
     * @param state Status of authentication process.
     * @return the status of the request.
     * @throws AuthLoginException if there are errors in processing the request.
     */
    public int process(Callback[] callbacks, int state)
            throws AuthLoginException {
        int returnState = -1;
        switch (state) {
            case 1:
                returnState = processState1(callbacks);
                break;
            case 2:
                returnState = processState2(callbacks);
                break;
            case 3:
                returnState = processState3(callbacks);
                break;
            default:
                debug.error("Invalid state:" + state +
                        "in processing callbacks");
                throw new AuthLoginException(AM_FV_AUTH, "Invalid State", null);
        }
        return returnState;
    }

    /**
     * Processes the callback requests when the status is 1.
     *
     * @param callbacks Array of callback object.
     * @return the status of the request.
     * @throws AuthLoginException if there are errors in processing the request.
     */
    private int processState1(Callback[] callbacks) throws AuthLoginException {
        try {
            HttpServletRequest req = getHttpServletRequest();
            HttpSession session = req.getSession(true);
            vproxy = FvValidationProxy.getInstance();
            debug.message("getInstance of FvProxy scceeded");
            cbean = vproxy.getChallenge();
            debug.message("getChallenge succeeded");
            session.setAttribute("Challenge",
                    String.valueOf(cbean.getFvChallenge()));
            // debug
            debug.message("Challenge: " +
                    String.valueOf(cbean.getFvChallenge()));
            debug.message("ChallengeId: " +
                    String.valueOf(cbean.getFvChallengeId()));
        } catch (IOException ex) {
            debug.error("IO error in prosessState1" + ex.getMessage());
            throw new AuthLoginException(AM_FV_AUTH,
                    "IO Error in processState1", null);
        }
        return 2;
    }

    /**
     * Processes the callback requests when the status is 2.
     *
     * @param callbacks Array of callback object.
     * @return the status of the request.
     * @throws AuthLoginException if there are errors in processing the request.
     */
    private int processState2(Callback[] callbacks) throws AuthLoginException {
        int len = callbacks.length;
        int authResult;
        int rtnval = -1;
        String userName = null;
        String authdata_txt = null;

        for (int i = 0; i < len; i++) {
            if (callbacks[i] instanceof NameCallback) {
                userName = ((NameCallback) callbacks[i]).getName();
                debug.message("userName:" + userName);
            } else if (callbacks[i] instanceof PasswordCallback) {
                debug.error("PasswordCallback was called");
            }
        }
        HttpServletRequest req = getHttpServletRequest();
        if (req != null) {
            authdata_txt = req.getParameter("txtAuthData");
        }
        if (authdata_txt == null || userName == null) {
            debug.error("Fail to get requred Fv Auth info");
            throw new AuthLoginException(AM_FV_AUTH,
                    "Failed to get requred auth info", null);
        }
        debug.message("userName = " + userName +
                "," + "authdata_txt = " + authdata_txt);
        authResult = vproxy.verify(userName, cbean.getFvChallengeId(),
                authdata_txt, 0);
        debug.error("authResult:" + authResult);
        switch (authResult) {
            case 0: // success
                userTokenId = userName;
                rtnval = -1;
                break;
            case 1: // auth failed
                debug.message("Authentication Failed for the user:" + userName);
                rtnval = 3;
                break;
            case -1: // Server error
                debug.message("Server Error Occurred");
                throw new AuthLoginException(AM_FV_AUTH,
                        "Server Error Occurred", null);
            default:
                debug.message("Unknown return value from ther server:" +
                        authResult);
                throw new AuthLoginException(AM_FV_AUTH,
                        "Unknown return value from ther server:" +
                        authResult, null);
        }
        return rtnval; // return -1 for login successful
    }

    /**
     * Processes the callback requests when the status is 3.
     *
     * @param callbacks Array of callback object.
     * @return the status of the request.
     * @throws AuthLoginException if there are errors in processing the request.
     */
    private int processState3(Callback[] callbacks) {
        debug.message("Process Auth Error started");
        return 1;
    }

    /**
     * get the Principal of the user who is logging in.
     *
     */
    public Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        }
        if (userTokenId != null) {
            userPrincipal = new FvPrincipal(userTokenId);
            return userPrincipal;
        }
        return null;
    }
}
