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
 * $Id: DSAMECallbackHandler.java,v 1.7 2008/08/19 19:08:54 veiming Exp $
 *
 */


package com.sun.identity.authentication.service;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.authentication.spi.PagePropertiesCallback;

/**
 * This class is OpenSSO implementation for Java 
 * <code>CallbackHandler</code> and it can be passed to underlying 
 * auth services so that it may interact with other components to retrieve 
 * specific authentication data, such as usernames and passwords, or to display 
 * certain information, such as error and warning messages.
*/
public class DSAMECallbackHandler implements CallbackHandler {

    // this should be read by AuthContext in order to receive
    // what callbacks the module needs

    static Debug debug = Debug.getInstance("amCallback");
    AMLoginContext am;
    LoginState loginState;

    // this will be sent by AuthContext for module to read.
    Callback[] submitRequiredInfo = null;
    static AuthThreadManager authThreadManager ;
    String sid = null;

    /**
     * Creates <code>DSAMECallbackHandler</code> object and it associates 
     * login thread and login state with callback hndler 
     * @param am <code>AMLoginContext</code> for this callback
     */
    public DSAMECallbackHandler(AMLoginContext am) {
        this.am = am;
        this.authThreadManager= am.authThread;
        this.loginState = am.getLoginState();
    }
        
    private void setPageTimeout(Callback[] callbacks) {
        long pageTimeOut = getTimeOut(callbacks);
        loginState.setPageTimeOut(pageTimeOut);
        long lastCallbackSent = System.currentTimeMillis();
        loginState.setLastCallbackSent(lastCallbackSent);
    }
        
    /**
     * <p> Retrieves or displays the information requested in the
     * provided Callbacks.
     *
     * <p> This method implementation checks the
     * instance(s) of the <code>Callback</code> object(s) passed in
     * to retrieve or display the requested information.
     * @param callbacks an array of <code>Callback</code> objects provided
     *        by an underlying security service which contains
     *        the information requested to be retrieved or displayed.
     *
     * @exception java.io.IOException if an input or output error occurs.
     * @exception UnsupportedCallbackException if the implementation of this
     *            method does not support one or more of the Callbacks
     *            specified in the <code>callbacks</code> parameter.
     */
    public void handle(Callback[] callbacks)
        throws IOException, UnsupportedCallbackException {

        if (debug.messageEnabled()) {
            debug.message("callback handler method");
        }

        if ((callbacks.length == 1) &&
            (callbacks[0] instanceof LoginStateCallback)) {
            debug.message("LoginState Callbacks");
            ((LoginStateCallback) callbacks[0]).setLoginState(loginState);
        } else {
            if (am.isPureJAAS()) {
                setPageTimeout(callbacks);
                loginState.setSubmittedCallback(null,am) ;
                    loginState.setReceivedCallback(callbacks,am) ;
                Thread thread = Thread.currentThread();

                if (debug.messageEnabled()) {
                        debug.message("waiting for submitted info " + thread);
                }

                callbacks = am.submitCallbackInfo();
                // check if the thread had timedout 
                if (authThreadManager.isTimedOut(thread)) {
                    loginState.setTimedOut(true);
                    loginState.setReceivedCallback(null,am) ;
                    authThreadManager.removeFromHash(thread,"timedOutHash");
                    throw new IOException(AMAuthErrorCode.AUTH_TIMEOUT);
                }
                    // check if there is a timeout 
                checkLoginTimeout();

            } else {
                if (loginState.getSubmittedInfo() != null) {
                    debug.message("DSAMEHandler: found submitted callbacks !");
                        // check if there is a timeout 
                    checkLoginTimeout();
                    Callback[] callbacks2 = loginState.getSubmittedInfo();
                    copyCallbacks(callbacks, callbacks2);
                    loginState.setReceivedCallback_NoThread(null);
                } else {
                    setPageTimeout(callbacks);
                    loginState.setReceivedCallback_NoThread(callbacks) ;
                    debug.message("Set callbacks, throwing java.lang.Error.");
                    throw new java.lang.Error("return from DSAMECallback");
                }
             }

            if (debug.messageEnabled()) {
                debug.message("DSAMECAllbackhandler..."+ callbacks);
            }
            if (am.isPureJAAS()) {
                loginState.setReceivedCallback(null,am) ;
            }
        }
    } 

    /**
     * Clones callbacks from cb2 to cb1.
     * @param cb1 new callbacks will be cloned from.
     * @param cb2 original callbacks will be cloned to.
     */
    private void copyCallbacks(Callback[] cb1, Callback[] cb2) {
        int len1 = cb1.length;
        int len2 = cb2.length;

        if (len1 == len2) {
            for (int m = 0; m < len1; m++) {
                if (cb1[m] != cb2[m]) {
                    cb1[m] = cb2[m];
                }
            }
        } else {
            int indx1 = 0;
            int indx2 = 0;
            while (indx1 < len1 && cb1[indx1] instanceof PagePropertiesCallback) {
                indx1 ++;
            }

            while (indx2 < len2 && cb2[indx2] instanceof PagePropertiesCallback) {
                indx2 ++;
            }

            int n = len1 - indx1;
            if (n > len2 - indx2) {
                n = len2 - indx2;
            }
            for (int m = 0; m < n; m++ ) {
                if (cb1[indx1] != cb2[indx2]) {
                    cb1[indx1] = cb2[indx2];
                }
                indx1 ++;
                indx2 ++;
            }
        }
    }


    /**
     *  Returns timeout value , 60 secs assumed if no timeout value found 
     *  @param callbacks checked for timeout. 
     *  @return  timeout value for callbacks.
     */
    long getTimeOut(Callback[] callbacks) {

        long pageTimeOut = 60;

        if (callbacks != null && 
            callbacks[0] instanceof PagePropertiesCallback) {
            PagePropertiesCallback pagePropertyCallback
                = (PagePropertiesCallback) callbacks[0];
            pageTimeOut =
                new Integer(pagePropertyCallback.getTimeOutValue()).longValue();
        } 
        return pageTimeOut;
    }

    /**
      * Check login time out. LoginTimeoutException will be thrown if the login
      * time out has been reached.
      * @exception IOException must be thrown if the login timeout
      * has been reached.
      */
    void checkLoginTimeout() throws IOException {

        long lastCallbackSent = loginState.getLastCallbackSent();
        long pageTimeOut = loginState.getPageTimeOut();
        long now = System.currentTimeMillis();
        if ((lastCallbackSent + ((pageTimeOut-3)*1000)) < now) {
            debug.message("Page Timeout");
            loginState.setTimedOut(true);
            loginState.setReceivedCallback(null,am) ;
            throw new IOException(AMAuthErrorCode.AUTH_TIMEOUT);
        }
    }

}
