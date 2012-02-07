/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PrivilegeXmlServlet.java,v 1.5 2009/06/04 11:49:11 veiming Exp $
 */

package com.sun.identity.admin;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import java.io.IOException;
import java.io.PrintWriter;
import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PrivilegeXmlServlet extends HttpServlet {

    private SSOToken getSSOToken(HttpServletRequest httpRequest) throws ServletException {
        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken ssoToken = manager.createSSOToken(httpRequest);

            return ssoToken;
        } catch (SSOException ssoe) {
            throw new ServletException(ssoe);
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/xml;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String[] names = request.getParameterValues("name");
        if (names == null || names.length == 0) {
            throw new ServletException("no names specified");
        }
        String realm = request.getParameter("realm");
        if (realm == null || realm.length() == 0) {
            throw new ServletException("no realm specified");
        }

        SSOToken t = getSSOToken(request);
        Subject s = SubjectUtils.createSubject(t);

        try {
            PrivilegeManager pm = PrivilegeManager.getInstance(realm, s);
            StringBuffer xml = new StringBuffer();

            // TODO: fetch single policy set
            for (String name: names) {
                xml.append(pm.getPrivilegeXML(name));
            }
            
            out.print(xml);
        } catch (EntitlementException ee) {
            throw new ServletException(ee);
        } finally { 
            out.close();
        }
    } 

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    } 

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
}
