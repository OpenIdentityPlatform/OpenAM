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
 * $Id: UserProfileServlet.java,v 1.3 2008/06/25 05:41:09 qcheng Exp $
 *
 */

package com.sun.identity.samples.clientsdk;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Sample program that show user profile.
 */
public class UserProfileServlet extends SampleBase {
    public void doPost(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, IOException {
        doGet(request, response);
    }

    public void doGet(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, IOException {
        // Get query parameters
        String orgname = request.getParameter("orgname");
        if (orgname == null || orgname.length() == 0) {
            orgname = "/";
        }
        
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        
        response.setContentType("text/html");

        // Get the output stream
        PrintWriter out = response.getWriter();
        out.println(SampleConstants.HTML_HEADER);
        if (username == null || password == null) {
            out.println("Value for user name and password are required.");
            out.println("</body></html>");
            return;
        }
        
        out.println("<br><h3>Username:</h3> " + username);
        
        try {
            // Authenticate the user and obtain SSO Token
            AuthContext lc = authenticate(orgname, username, password, out);
            
            if (lc != null) {
                // Obtain the SSO Token
                SSOToken token = lc.getSSOToken();
                out.println("<br><h3>SSOToken:</h3> " + token.getTokenID());
                out.println("<br><h3>User DN:</h3> " +
                    token.getPrincipal().getName());
                out.println("<p>");
                
                AMIdentity amid = IdUtils.getIdentity(token);
                Map attrs = amid.getAttributes();
                out.println("User Attributes: ");

                for (Iterator i = attrs.keySet().iterator(); i.hasNext(); ) {
                    String attrName = (String)i.next();
                    Set values = (Set)attrs.get(attrName);
                    out.println(attrName + "=" + values);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(out);
            out.println("</body></html>");
        }
    }
}

