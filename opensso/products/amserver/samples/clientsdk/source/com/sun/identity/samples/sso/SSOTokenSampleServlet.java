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
 * $Id: SSOTokenSampleServlet.java,v 1.4 2008/10/29 03:16:04 veiming Exp $
 *
 */

package com.sun.identity.samples.sso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenListener;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This Sample serves as a basis for using SSO API. It demonstrates creating
 * a SSO Token, calling various methods from the token, setting up event 
 * listeners and getting called on event listeners.
 *
 * @see com.iplanet.sso.SSOToken
 * @see com.iplanet.sso.SSOTokenID
 * @see com.iplanet.sso.SSOTokenManager
 * @see com.iplanet.sso.SSOTokenEvent
 * @see com.iplanet.sso.SSOTokenListener
 */
public class SSOTokenSampleServlet extends HttpServlet {

    public void init() throws ServletException {
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response){
        ServletOutputStream out = null;
        
        try {
            try {
                response.setContentType("text/html");
                out = response.getOutputStream();

                // create the sso token from http request 
                SSOTokenManager manager = SSOTokenManager.getInstance();
                SSOToken token = manager.createSSOToken(request);

                if (manager.isValidToken(token)) {
                    //print some of the values from the token.
                    String host = token.getHostName();
                    java.security.Principal principal = token.getPrincipal();
                    String authType = token.getAuthType();
                    int level = token.getAuthLevel();
                    InetAddress ipAddress = token.getIPAddress();

                    out.println("SSOToken host name: " + host);
                    out.println("<br />");
                    out.println("SSOToken Principal name: " +
                        principal.getName());
                    out.println("<br />");
                    out.println("Authentication type used: " + authType);
                    out.println("<br />");
                    out.println("IPAddress of the host: " +
                        ipAddress.getHostAddress());
                    out.println("<br />");
                }

                /* Validate the token again, with another method.
                 * if token is invalid, this method throws exception
                 */
                manager.validateToken(token);
                out.println("SSO Token validation test succeeded");
                out.println("<br />");

                // Get the SSOTokenID associated with the token and print it.
                SSOTokenID tokenId = token.getTokenID();
                out.println("The token id is " + tokenId.toString());
                out.println("<br />");

                // Set and get some properties in the token.
                token.setProperty("Company", "Sun Microsystems");
                token.setProperty("Country", "USA");
                String name = token.getProperty("Company");
                String country = token.getProperty("Country");
                out.println("Property: Company: " + name);
                out.println("<br />");
                out.println("Property: Country: " + country);
                out.println("<br />");

                // Retrieve user profile and print them
                AMIdentity userIdentity = IdUtils.getIdentity(token);
                Map attrs = userIdentity.getAttributes();
                out.println("User Attributes: " + attrs);

                /* let us add a listener to the SSOToken. Whenever a token
                 * event arrives, ssoTokenChanged method of the listener will
                 * get called.
                 */
                SSOTokenListener myListener = new SampleTokenListener();
                token.addSSOTokenListener(myListener);
            } catch (SSOException e) {
                out.println("SSO Exception: " + e);
                out.println("<p>Authenticate to OpenSSO server before visiting this page.</p>");
                e.printStackTrace();
            } catch (IdRepoException e) {
                out.println("IdRepo Exception: " + e);
                e.printStackTrace();
            } catch (IOException e) {
                out.println("IO Exception: " + e);
                e.printStackTrace();
            } finally {
                out.flush();
            }
        } catch (IOException e) {
            // ignored
        }
    }    
}
