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
 * $Id: Login.java,v 1.5 2008/06/25 05:41:13 qcheng Exp $
 *
 */

package com.sun.identity.samples.authentication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;

public class Login {
    private String loginIndexName;
    private String orgName;
    private String locale;
    
    private Login(String loginIndexName, String orgName) {
        this.loginIndexName = loginIndexName;
        this.orgName = orgName;
    }
    
    private Login(String loginIndexName, String orgName, String locale) {
        this.loginIndexName = loginIndexName;
        this.orgName = orgName;
        this.locale = locale;
    }
    
    protected AuthContext getAuthContext()
        throws AuthLoginException {
        AuthContext lc = new AuthContext(orgName);
        AuthContext.IndexType indexType = AuthContext.IndexType.MODULE_INSTANCE;
        if (locale == null || locale.length() == 0) {
            lc.login(indexType, loginIndexName);
        } else {
            lc.login(indexType, loginIndexName, locale);
        }
        debugMessage(loginIndexName + ": Obtained login context");
        return lc;
    }
    
    private void addLoginCallbackMessage(Callback[] callbacks)
    throws UnsupportedCallbackException {
        int i = 0;
        try {
            for (i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof TextOutputCallback) {
                    handleTextOutputCallback((TextOutputCallback)callbacks[i]);
                } else if (callbacks[i] instanceof NameCallback) {
                    handleNameCallback((NameCallback)callbacks[i]);
                } else if (callbacks[i] instanceof PasswordCallback) {
                    handlePasswordCallback((PasswordCallback)callbacks[i]);
                } else if (callbacks[i] instanceof TextInputCallback) {
                    handleTextInputCallback((TextInputCallback)callbacks[i]);
                } else if (callbacks[i] instanceof ChoiceCallback) {
                    handleChoiceCallback((ChoiceCallback)callbacks[i]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new UnsupportedCallbackException(callbacks[i],e.getMessage());
        }
    }
    
    private void handleTextOutputCallback(TextOutputCallback toc) {
        debugMessage("Got TextOutputCallback");
        // display the message according to the specified type
        
        switch (toc.getMessageType()) {
            case TextOutputCallback.INFORMATION:
                debugMessage(toc.getMessage());
                break;
            case TextOutputCallback.ERROR:
                debugMessage("ERROR: " + toc.getMessage());
                break;
            case TextOutputCallback.WARNING:
                debugMessage("WARNING: " + toc.getMessage());
                break;
            default:
                debugMessage("Unsupported message type: " +
                    toc.getMessageType());
        }
    }
    
    private void handleNameCallback(NameCallback nc)
        throws IOException {
        // prompt the user for a username
        System.out.print(nc.getPrompt());
        System.out.flush();
        nc.setName((new BufferedReader
            (new InputStreamReader(System.in))).readLine());
    }
    
    private void handleTextInputCallback(TextInputCallback tic)
        throws IOException {
        // prompt for text input
        System.out.print(tic.getPrompt());
        System.out.flush();
        tic.setText((new BufferedReader
            (new InputStreamReader(System.in))).readLine());
    }
    
    private void handlePasswordCallback(PasswordCallback pc)
        throws IOException {
        // prompt the user for sensitive information
        System.out.print(pc.getPrompt());
        System.out.flush();
        String passwd = (new BufferedReader(new InputStreamReader(System.in))).
            readLine();
        pc.setPassword(passwd.toCharArray());
    }
    
    private void handleChoiceCallback(ChoiceCallback cc)
        throws IOException {
        // ignore the provided defaultValue
        System.out.print(cc.getPrompt());
        
        String[] strChoices = cc.getChoices();
        for (int j = 0; j < strChoices.length; j++) {
            System.out.print("choice[" + j + "] : " + strChoices[j]);
        }
        System.out.flush();
        cc.setSelectedIndex(Integer.parseInt((new BufferedReader
            (new InputStreamReader(System.in))).readLine()));
    }
    
    protected boolean login(AuthContext lc)
        throws UnsupportedCallbackException {
        boolean succeed = false;
        Callback[] callbacks = null;
        
        // get information requested from module
        while (lc.hasMoreRequirements()) {
            callbacks = lc.getRequirements();
            if (callbacks != null) {
                addLoginCallbackMessage(callbacks);
                lc.submitRequirements(callbacks);
            }
        }
        
        if (lc.getStatus() == AuthContext.Status.SUCCESS) {
            System.out.println("Login succeeded.");
            succeed = true;
        } else if (lc.getStatus() == AuthContext.Status.FAILED) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Unknown status: " + lc.getStatus());
        }
        
        return succeed;
    }
    
    protected void logout(AuthContext lc)
        throws AuthLoginException {
        lc.logout();
        System.out.println("Logged Out!!");
    }
    
    static void debugMessage(String msg) {
        System.out.println(msg);
    }
    
    public static void main(String[] args) {
        try {
            System.out.print("Realm (e.g. /): ");
            String orgName = (new BufferedReader(
                new InputStreamReader(System.in))).readLine();

            System.out.print("Login module name (e.g. DataStore or LDAP): ");
            String moduleName = (new BufferedReader(
                new InputStreamReader(System.in))).readLine();
            
            System.out.print("Login locale (e.g. en_US or fr_FR): ");
            String locale = (new BufferedReader(
                new InputStreamReader(System.in))).readLine();
            
            Login login = new Login(moduleName, orgName, locale);
            AuthContext lc = login.getAuthContext();
            if (login.login(lc)) {
                login.logout(lc);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AuthLoginException e) {
            e.printStackTrace();
        } catch (UnsupportedCallbackException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
