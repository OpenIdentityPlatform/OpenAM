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
 * $Id: LDAPCallbacks.java,v 1.3 2008/06/25 05:41:58 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.ldap;

import java.util.ResourceBundle;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

public class LDAPCallbacks {
    private CallbackHandler callbackHandler;
    private static com.sun.identity.shared.debug.Debug debug = null;
    private String passwd;
    private String username;
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
    private ResourceBundle bundle;

    static {
        if (debug == null) {
            debug = com.sun.identity.shared.debug.Debug.getInstance("amAuthLDAP");
        }
    }
    
    public LDAPCallbacks(
        CallbackHandler callbackHandler,
        ResourceBundle bundle) {
        this.callbackHandler = callbackHandler;
        this.bundle = bundle;
    }

    /**
     * Instantiates and passes a callbacks to the <code>invokeCallback</code>
     * method of a <code>CallbackHandler</code> to ask for user name and
     * password to procees Login Page.
     *
     * @throws LoginException
     */
    public void setLoginScreen() throws LoginException  {
        loginCallbacks();
    }

    /**
     * Instantiates and passes a callbacks to  the <code>invokeCallback</code>
     * method of a <code>CallbackHandler</code> to ask for old, new and
     * confirm passwords to procees change password screen.
     *
     * @throws LoginException
     */
    public void setPwdExpiryScreen() throws LoginException {  
        chgPwdCallback();
    }

    /**
     * Instantiates and passes a <code>TextOutputCallback</code> to the
     * <code>invokeCallback</code> method of a <code>CallbackHandler</code> to
     * display information messages, warning messages and error messages.
     *
     * @param type
     * @param msg
     * @throws LoginException
     */
    public void sendMessage(int type, String msg) throws LoginException {
        messageCallback(type,msg);
    }

    protected String getUserName() {
        return username;
    }

    protected String getUserPWD() {
        return passwd;
    }

    protected String getOldPWD() {
        return oldPassword;
    }

    protected String getNewPWD() {
        return newPassword;
    }

    protected String getConfirmPWD() {
        return confirmPassword;
    }
  
    private void loginCallbacks() throws LoginException {
        if (callbackHandler == null) {
            
            throw new LoginException(bundle.getString("NoCallbackHandler"));
                                     
        }
        Callback[] callbacks = new Callback[3];
        callbacks[0] = new TextOutputCallback
            (TextOutputCallback.INFORMATION,"LDAP Authentication");
        callbacks[1] = new NameCallback("Enter Username :");
        callbacks[2] = new PasswordCallback("Enter Password :", false);
        try {
            callbackHandler.handle(callbacks);
            username = ((NameCallback) callbacks[1]).getName();
            passwd = charToString(
                ((PasswordCallback)callbacks[2]).getPassword(),callbacks[2]);
            
        } catch (java.io.IOException ioe) {
            throw new LoginException(bundle.getString("NoCallbackHandler"));
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException();
        }
    }
    
    private void chgPwdCallback() throws LoginException {
        char [] pwd = null;
        if (callbackHandler == null) {
            
            throw new LoginException(bundle.getString("NoCallbackHandler"));
        }
        Callback[] callbacks = new Callback[4];
        callbacks[0] = new TextOutputCallback
            (TextOutputCallback.INFORMATION,"Change Password");
        callbacks[1] = new PasswordCallback("EnterOld Password", false);
        callbacks[2] = new PasswordCallback("Enter New Password", false);
        callbacks[3] = new PasswordCallback("Confirm Password", false);
        try {
            
            callbackHandler.handle(callbacks);
            
            oldPassword = charToString(
                ((PasswordCallback)callbacks[1]).getPassword(),callbacks[1]);
            newPassword = charToString(
                ((PasswordCallback)callbacks[2]).getPassword(), callbacks[2]);
            confirmPassword = charToString(
                ((PasswordCallback)callbacks[3]).getPassword(),callbacks[3]);
        } catch (java.io.IOException ioe) {
            throw new LoginException(ioe.toString());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException(bundle.getString("NoCallbackHandler"));
        }
        
    }

    private String charToString (char [] tmpPassword, Callback cbk ) {
        if (tmpPassword == null) {
            // treat a NULL password as an empty password
            tmpPassword = new char[0];
        }
        char[] pwd = new char[tmpPassword.length];
        System.arraycopy(tmpPassword, 0, pwd, 0, tmpPassword.length);
        ((PasswordCallback)cbk).clearPassword();
        return new String(pwd);
    }
    
    private void  messageCallback(int msgType, String msg)
            throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException(bundle.getString("NoCallbackHandler"));
        }
        try {
            Callback[] callbacks = new Callback[1];
            callbacks[0] = new TextOutputCallback (msgType,msg);
            callbackHandler.handle(callbacks);
        } catch (java.io.IOException ioe) {
            throw new LoginException(ioe.toString());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException(bundle.getString("NoCallbackHandler"));
        } catch (IllegalArgumentException ill) {
            debug.message("message type missing");
            throw new LoginException(bundle.getString("IllegalArgs"));
        }
    }
}
