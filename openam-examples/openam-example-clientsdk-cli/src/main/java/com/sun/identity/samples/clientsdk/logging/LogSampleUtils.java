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
 * $Id: LogSampleUtils.java,v 1.3 2008/07/29 20:34:57 bigfatrat Exp $
 *
 */

package com.sun.identity.samples.clientsdk.logging;


import java.io.*;
import java.lang.Integer;
import java.util.*;
import javax.security.auth.callback.*;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.share.AuthXMLTags;
import com.sun.identity.log.Logger;
import com.sun.identity.log.LogRecord;


/**
 * This class 
 *
 *
 * @author 
 */
public class LogSampleUtils {

    public LogSampleUtils() {
    }

    public SSOToken realmLogin (String userid, String password, AuthContext ac)
        throws SSOException, AuthLoginException, Exception
    {
        String adminDN;
        String adminPassword;
        SSOToken ssoToken = null;
        String userID = null;

        try {
            ac.login();
        } catch (AuthLoginException le) {
            System.err.println("LogSampleUtils: Failed to start login " +
                "for default auth module.");
            throw le;
        }

        userID = userid;
        Callback[]  callbacks = null;
        Hashtable values = new Hashtable();
        values.put(AuthXMLTags.NAME_CALLBACK, userid);
        values.put(AuthXMLTags.PASSWORD_CALLBACK, password);

        while (ac.hasMoreRequirements()) {
            callbacks = ac.getRequirements();
            try {
                fillCallbacks(callbacks, values);
                ac.submitRequirements(callbacks);
            } catch (Exception e) {
                System.err.println( "Failed to submit callbacks!"); 
                e.printStackTrace();
                return null;
            }
        }

        AuthContext.Status istat = ac.getStatus();
        if (istat == AuthContext.Status.SUCCESS) {
            System.out.println("==>Authentication SUCCESSFUL for user " +
                userid);
        } else if (istat == AuthContext.Status.COMPLETED) {
            System.out.println("==>Authentication Status for user " +
                userid+ " = " + istat);
            return null;
        }

        try {
            ssoToken = ac.getSSOToken();
        } catch (Exception e) {
            System.err.println( "Failed to get SSO token!"); 
            throw e;
        }

        return ssoToken;
    }

    protected void fillCallbacks(Callback[] callbacks, Hashtable values) 
        throws Exception
    {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback nc = (NameCallback) callbacks[i];
                nc.setName((String)values.get(AuthXMLTags.NAME_CALLBACK));
            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback pc = (PasswordCallback) callbacks[i];
                pc.setPassword(((String)values.get(
                    AuthXMLTags.PASSWORD_CALLBACK)).toCharArray());
            } else if (callbacks[i] instanceof TextInputCallback) {
                TextInputCallback tic = (TextInputCallback) callbacks[i];
                tic.setText((String)values.get(
                    AuthXMLTags.TEXT_INPUT_CALLBACK));
            } else if (callbacks[i] instanceof ChoiceCallback) {
                ChoiceCallback cc = (ChoiceCallback) callbacks[i];
                cc.setSelectedIndex(Integer.parseInt((String)values.get(
                    AuthXMLTags.CHOICE_CALLBACK)));
            }
        }
    }

    public String getLine() {
        StringBuffer buf = new StringBuffer(80);
        int c;

        try {
            while ((c = System.in.read()) != -1) {
                char ch = (char)c;
                if (ch == '\r') {
                    continue;
                }
                if (ch == '\n') {
                    break;
                }
                buf.append(ch);
            }
        } catch (IOException e) {
            System.err.println ("getLine: " + e.getMessage());
        }
        return (buf.toString());
    }

    public String getLine (String prompt) {
        System.out.print (prompt);
        return (getLine());
    }

    public String getLine (String prompt, String defaultVal) {
        System.out.print (prompt + " [" + defaultVal + "]: ");
        String tmp = getLine();
        if (tmp.length() == 0) {
            tmp = defaultVal;
        }
        return (tmp);
    }

    /*
     *  return integer value of String sVal; -1 if error
     */
    public int getIntValue (String sVal) {
        int i = -1;
        try {
            i = Integer.parseInt (sVal);
        } catch (NumberFormatException e) {
            System.err.println ("'" + sVal +
                "' does not appear to be an integer.");
        }
        return i;
    }

}


