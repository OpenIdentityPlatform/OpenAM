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
 * $Id: AuthLoginThread.java,v 1.2 2008/06/25 05:41:53 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */
package com.sun.identity.authentication.internal;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

/**
 * The class <code>AuthLoginThread</code> provides the needed synchronization
 * for JAAS's callback mechanism. This class starts a new thread and waits for
 * user's authentication information to be submitted. Used by a state less
 * protocol like HTTP.
 */
public class AuthLoginThread extends Thread implements CallbackHandler {

    private AuthContext loginContext;

    /**
     * Constructor for this class. Since it is protected, only classes in this
     * package can instantiate this object.
     */
    protected AuthLoginThread(AuthContext ctx) {
        AuthContext.authDebug.message("AuthLoginThread::Constructor");
        loginContext = ctx;
    }

    /**
     * Method that call's JAAS's <code>LoginContext</code>'s
     * <code>login()</code> method.
     */
    public void run() {
        AuthContext.authDebug.message("AuthLoginThread::run()");
        try {
            loginContext.loginContext.login();
            loginContext.setLoginStatus(AuthContext.AUTH_SUCCESS);
            AuthContext.authDebug.message("AuthLoginThread::run() successful login");
        } catch (LoginException le) {
            loginContext.setLoginStatus(AuthContext.AUTH_FAILED);
            loginContext.loginException = le;
            AuthContext.authDebug.message("AuthLoginThread::run() exception during login; " + le);
        }
    }

    /**
     * Method that implements JAAS's <code>CallbackHandler</code> interface.
     * This method receives the authentication information requests from the
     * plug-ins and sends it to <code>AuthContext</code>, and similarly
     * accepts the submited authentication information from <code>
     * AuthContext</code>
     * and sends it to the plug-ins.
     */
    public void handle(Callback[] callback) throws IOException, UnsupportedCallbackException {
        AuthContext.authDebug.message("AuthLoginThread::handle()");

        // Clear the previously submitted information
        loginContext.submittedInformation = null;

        // Set the required information variable
        synchronized (this) {
            loginContext.informationRequired = callback;
            // wake up threads waiting for this variable
            this.notify();
        }
        AuthContext.authDebug.message("AuthLoginThread::handle() sent notify to wake up sleeping threads");

        // check if the requested information is ready
        while (loginContext.submittedInformation == null) {
            // wait for the variable to be set
            try {
                AuthContext.authDebug.message("AuthLoginThread::handle() "
                        + "waiting for Callbacks to be submitted");
                synchronized (this) {
                    if (loginContext.submittedInformation == null)
                        this.wait();
                }
                AuthContext.authDebug.message("AuthLoginThread::handle() "
                        + "woke up from waiting for Callbacks to be submitted");
            } catch (InterruptedException ie) {
                // do nothing
            }
        }

        // Update the shared state and return the requested information
        loginContext.loginContext.updateSharedState(loginContext.submittedInformation);
        callback = loginContext.submittedInformation;
    }
}
