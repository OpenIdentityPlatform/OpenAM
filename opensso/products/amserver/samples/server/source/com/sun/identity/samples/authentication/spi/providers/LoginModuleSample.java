/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LoginModuleSample.java,v 1.2 2008/06/25 05:41:12 qcheng Exp $
 *
 */

package com.sun.identity.samples.authentication.spi.providers;

import java.security.Principal;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;

/**
 * Sample Login Module.
 */ 
public class LoginModuleSample extends AMLoginModule {
    private static final String LOGIN_USER = "anonymous";

    private String userTokenId;
    private String userName;
    private String lastName;
    private java.security.Principal userPrincipal = null;

    /**
     * Creates an instance of this class.
     *
     * @throws LoginException if class cannot be instantiated.
     */
    public LoginModuleSample() throws LoginException{
        System.out.println("LoginModuleSample()");
    }
 
    /**
     * Initializes the module.
     *
     * @param subject
     * @param sharedState
     * @param options
     */
    public void init(Subject subject, Map sharedState, Map options) {
        System.out.println("LoginModuleSample initialization");
    }

    /**
     * Processes the callback requests.
     *
     * @param callbacks Array of callback object.
     * @param state
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
        case 4:
            returnState = processState4(callbacks);
            break;
        default:
            throw new AuthLoginException("Invalid state : " + state);
        }

        return returnState;
    }

    private int processState1(Callback[] callbacks)
        throws AuthLoginException
    {
        userName = ((NameCallback)callbacks[0]).getName();
        lastName = ((NameCallback)callbacks[1]).getName();
        if ((userName.length() == 0) || (lastName.length() == 0)) {
            throw new AuthLoginException("names must not be empty");
        }
        return 2;
    }

    private int processState2(Callback[] callbacks)
        throws AuthLoginException
    {
        System.out.println("Replace Text first: " + userName +
            " last: " + lastName);

        // set #REPLACE# text in next state
        Callback[] callbacks2 = getCallback(3);
        String msg = ((NameCallback)callbacks2[0]).getPrompt();
        int i = msg.indexOf("#REPLACE#");
        String newMsg = msg.substring(0, i) + userName +
            msg.substring(i +9);
        replaceCallback(3, 0, new NameCallback(newMsg));

        // set #REPLACE# in password callback
        msg = ((PasswordCallback)callbacks2[1]).getPrompt();
        i = msg.indexOf("#REPLACE#");
        newMsg = msg.substring(0, i) + lastName + msg.substring(i+9);
        replaceCallback(3, 1, new PasswordCallback(newMsg, false));

        return 3;
    }

    private int processState3(Callback[] callbacks) {
        int len = callbacks.length;
        for (int i = 0; i<len; i++) {
            if (callbacks[i] instanceof NameCallback) {
                System.out.println("Callback Prompt-> " +
                    ((NameCallback)callbacks[i]).getPrompt());
            } else if (callbacks[i] instanceof PasswordCallback) {
                System.out.println("Callback Prompt-> " +
                    ((PasswordCallback)callbacks[i]).getPrompt());
            }
        }
        return 4;
    }

    private int processState4(Callback[] callbacks) {
        int len = callbacks.length;
        for (int i = 0; i < len; i++) {
            if (callbacks[i] instanceof NameCallback) {
                System.out.println("Callback Value-> " +
                    ((NameCallback)callbacks[i]).getName());
            } else if (callbacks[i] instanceof PasswordCallback) {
                System.out.println("Callback Value-> " +
                    ((PasswordCallback)callbacks[i]).getPassword());
            }
        }
        userTokenId = LOGIN_USER;
        return -1; // return -1 for login successful
    }
 
    public Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        }

        if (userTokenId != null) {
            userPrincipal = new SamplePrincipal(userTokenId);
            return userPrincipal;
        }

        return null;
    }
}
