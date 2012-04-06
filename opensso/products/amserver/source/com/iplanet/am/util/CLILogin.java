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
 * $Id: CLILogin.java,v 1.2 2008/06/25 05:41:27 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

public class CLILogin {

    String host = null;

    String domain = null;

    String userId = null;

    String passwd = null;

    String gotoUrl = "console";

    String protocol = "http";

    String cookie = null;

    String cookieValue = null;

    SSOToken ssotoken = null;

    // Later more constructors can be added, as per requirements.
    public CLILogin(String host, String domain, String userId, String passwd) {

        this.host = host;
        this.domain = domain;
        this.userId = userId;
        this.passwd = passwd;
    }

    // Retunrs true if logged in, else returns false. It is just a convenience
    // method to check the login status.
    public boolean isLoggedIn() {
        return isSessionValid();
    }

    // Returns the SessionId associated with this Session
    public String getSessionId() {
        return cookieValue;
    }

    // Return the SSOToken associated with this login object
    public SSOToken getToken() {
        return ssotoken;
    }

    // Returns true if the session associated with CLILogin object is valid,
    // else returns false
    public boolean isSessionValid() {
        return validateSession(cookieValue);
    }

    // Returns the output of accessing a URL
    public String getURL(URL url) {
        String str = null;
        try {
            str = urlAccess(url, cookie, true);
        } catch (Exception e) {
            System.out.println("Exception in getURL");
        }
        return str;

    }

    // Returns the cookies associated with the login. Will be implemented later
    public Map getCookies() {
        return null;

    }

    // logout the user
    public void logout() {
        try {
            if (ssotoken != null) {
                com.iplanet.sso.SSOProvider manager =
                    new com.iplanet.sso.providers.dpro.SSOProviderImpl();
                manager.destroyToken(ssotoken);
            }
            cookieValue = null;
            ssotoken = null;
            cookie = null;
        } catch (SSOException e) {
            System.out.println(e.getMessage());
        }
    }

    // Returns true if login is successful else false.
    public boolean login() {
        if (host == null || domain == null || userId == null || passwd == null) 
        {
            return false;
        }

        boolean loggedin = false;

        try {
            // URL url = new URL(protocol + "://"+ host + "/login/LDAP");
            URL url = new URL(protocol + "://" + host + "/login/LDAP?goto="
                    + gotoUrl);
            cookie = getCookie(url);
            int index = cookie.indexOf("=");
            int index1 = cookie.indexOf(";");
            cookieValue = cookie.substring(index + 1, index1);
            if (cookieValue == null)
                return false;

            System.out.println("Cookie = " + cookie);
            System.out.println("CookieVal = " + cookieValue);

            // authenticate user using LDAP module through POST
            url = new URL(protocol + "://" + host + "/login/LDAP");
            authenticate(url, cookie, userId, passwd);

            System.out.println("===== validate session after authenticate ===");
            // check if really logged in or not
            cookieValue = convertCookie(cookieValue);
            loggedin = isSessionValid();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (!loggedin)
            logout();
        return loggedin;

    }

    // Returns true if the session associated with CLILogin object is valid,
    // else returns false
    private boolean validateSession(String cookieValue) {

        if (cookieValue == null)
            return false;
        try {
            com.iplanet.sso.SSOProvider manager = 
                new com.iplanet.sso.providers.dpro.SSOProviderImpl();
            ssotoken = manager.createSSOToken(cookieValue);
            if (manager.isValidToken(ssotoken)) {
                System.out.println("Valid session for "
                        + ssotoken.getProperty("Principal"));
                return true;
            } else {
                System.out.println("Invalid session");
                return false;
            }
        } catch (SSOException e) {
            System.out.println(e.getMessage());
        }
        return false;

    }

    private static String getCookie(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Get the iPlanetDirectoryPro cookie from the header, strip off
        // the cookie name, domain, and path to get the value, it must be
        // included in the next post to the auth server, the cookie anme
        // should be taken from the platform.conf

        String cookie = connection.getHeaderField("Set-cookie");
        if (cookie == null) {
            System.out.println("No cookies in HTTP request, server down ?"
                    + url);
            return null;
        }
        connection.disconnect();
        return cookie;
    }

    private static void authenticate(URL url, String cookie, String userName,
            String passWord) throws IOException, ProtocolException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Cookie", cookie);

        // don't follow redirects since we don't care about them
        // and the auth will do a final redirect after successful
        // authentication. Instead we will just check the session
        // to see if auth suceeded.

        HttpURLConnection.setFollowRedirects(false);
        PrintWriter out = new PrintWriter(connection.getOutputStream());
        out.print("TOKEN0=" + userName + "&TOKEN1=" + passWord);
        out.close();

        // Must get the input stream in order to complete the post even
        // though we don't care about the response.
        new BufferedReader(new InputStreamReader(connection.getInputStream()));
    }

    private static String urlAccess(URL url, String cookie, boolean print)
            throws IOException, ProtocolException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Cookie", cookie);
        String strOutput = null;

        // read output from server
        BufferedReader in = new BufferedReader(new InputStreamReader(connection
                .getInputStream()));

        if (print) {
            StringBuilder in_buf = new StringBuilder();
            int len;
            char[] buf = new char[1024];
            while ((len = in.read(buf, 0, buf.length)) != -1) {
                in_buf.append(buf, 0, len);
            }
            strOutput = in_buf.toString();
            System.out.print(strOutput);
        }
        return strOutput;
    }

    private static String convertCookie(String cookie) {
        // System.out.println("cookie=" + cookie);
        while (true) {
            int temp = cookie.indexOf("%25");
            if (temp == -1) {
                return cookie;
            } else {
                String newCookie = cookie.substring(0, temp) + "%"
                        + cookie.substring(temp + 3);
                // System.out.println("cookie=" + newCookie);
                cookie = newCookie;
            }
        }
    }
}
