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
 * $Id: AMCallbackHandler.java,v 1.2 2008/06/25 05:52:58 qcheng Exp $
 *
 */

package com.sun.identity.security.keystore;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.security.AccessController;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.Locale;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.security.SecurityDebug;
import com.sun.identity.security.DecodeAction;


public class AMCallbackHandler implements CallbackHandler {
    static final String bundleName = "amSecurity";
    static ResourceBundle bundle = null;
    static AMResourceBundleCache amCache = AMResourceBundleCache.getInstance(); 
    static String passwdPrompt = null;
    
    static {
        bundle = amCache.getResBundle(bundleName, Locale.getDefault());
        passwdPrompt = bundle.getString("KeyStorePrompt");
    }
    
    public AMCallbackHandler() {
        this(passwdPrompt);
    }
    
    public AMCallbackHandler(String prompt) {
        super();
        String passWDFile = System.getProperty(
                       "com.sun.identity.security.keyStorePasswordFile", null);
        String keystorePW = System.getProperty(
                       "javax.net.ssl.keyStorePassword", null);
            
        if (prompt == null) {
            prompt = passwdPrompt;
        }
        
        if (passWDFile != null) {
            try {
                FileInputStream fis = new FileInputStream(passWDFile);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader br = new BufferedReader(isr);
                keystorePW = (String) AccessController.doPrivileged(
                    new DecodeAction(br.readLine()));
                fis.close(); 
            } catch (Exception ex) {
                ex.printStackTrace();
                SecurityDebug.debug.error("AMCallbackHandler: Unable to " +
                        "read keystore password file " + passWDFile);
            }
        }

        if (keystorePW != null) {
            password = keystorePW.toCharArray();
        }
    }
    
    public void handle(Callback[] callbacks)
        throws UnsupportedCallbackException {
        int i = 0;
        try {
            for (i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof PasswordCallback) {
                // prompt the user for sensitive information
                    if (SecurityDebug.debug.messageEnabled()) {
                        SecurityDebug.debug.message(
                            "AMCallbackHandler() :  PasswordCallback()");
                    }
                    PasswordCallback pc = (PasswordCallback) callbacks[i];
                    
                    if (password == null) {
                        if (SecurityDebug.debug.messageEnabled()) {
                            SecurityDebug.debug.message(
                                "AMCallbackHandler() :  Prompt Password ");
                        }

                        if (passwdPrompt != null) {
                            System.out.print(passwdPrompt);
                        } else {
                            System.out.print(pc.getPrompt());
                        }

                        System.out.flush();
                        pc.setPassword(readPassword(System.in));
                    } else {
                        pc.setPassword(password);
                    }
                } else {
                    SecurityDebug.debug.error("Got UnknownCallback");
                    break;
                }
            }
        } catch (Exception e) {
            SecurityDebug.debug.error("Exception in Callback : "+e);
            e.printStackTrace();
            throw new UnsupportedCallbackException(
                callbacks[i], "Callback exception: " + e);
        }
    }

    // Reads user password from given input stream.
    private char[] readPassword(InputStream in) throws IOException {
        char[] lineBuffer;
        char[] buf;
        int i;

        buf = lineBuffer = new char[128];

        int room = buf.length;
        int offset = 0;
        int c;

        loop:
            while (true) {
                switch (c = in.read()) {
                    case -1:
                    case '\n':
                        break loop;

                    case '\r':
                        int c2 = in.read();
                        if ((c2 != '\n') && (c2 != -1)) {
                            if (!(in instanceof PushbackInputStream)) {
                                in = new PushbackInputStream(in);
                            }
                            ((PushbackInputStream)in).unread(c2);
                        } else
                            break loop;

                    default:
                        if (--room < 0) {
                            buf = new char[offset + 128];
                            room = buf.length - offset - 1;
                            System.arraycopy(lineBuffer, 0, buf, 0, offset);
                            Arrays.fill(lineBuffer, ' ');
                            lineBuffer = buf;
                        }
                        buf[offset++] = (char) c;
                        break;
                }
            }

            if (offset == 0) {
                return null;
            }

            password = new char[offset];
            System.arraycopy(buf, 0, password, 0, offset);
            Arrays.fill(buf, ' ');
            cleared = false;
            
            return password;
    }

    /**
     * Clears the password so that sensitive data is no longer present
     * in memory. This should be called as soon as the password is no
     * longer needed.
     */
    public synchronized void clear() {
        int i;
        int len = password.length;

        for(i=0; i < len; i++) {
            password[i] = 0;
        }
        cleared = true;
    }
    
    /**
     * The finalizer clears the sensitive information before releasing
     * it to the garbage collector, but it should have been cleared manually
     * before this point anyway.
     */
    protected void finalize() throws Throwable {
        clear();
    }
    
    /**
     * Set password for key store 
     * @param pw Value of string to be set 
     */
    public void setPassword(String pw) {
        password = pw.toCharArray();
        cleared = false;
    }

    // The password, stored as a char[] so we can clear it.  Passwords
    // should never be stored in Strings because Strings can't be cleared.
    char[] password = null;

    // true if the char[] has been cleared of sensitive information
    boolean cleared;
}
        
