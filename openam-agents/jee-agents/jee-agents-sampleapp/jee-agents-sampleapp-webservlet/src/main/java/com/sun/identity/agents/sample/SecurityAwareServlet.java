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
 * $Id: SecurityAwareServlet.java,v 1.3 2008/06/25 05:52:10 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock AS.
*/

package com.sun.identity.agents.sample;

import java.io.IOException;
import java.security.Principal;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

public class SecurityAwareServlet extends SampleServletBase {

    public void processRequest(HttpServletRequest request, 
        HttpServletResponse response) throws ServletException, IOException
    {
        request.setAttribute("RESULT", "OK");
        request.setAttribute("DETAILS", getSecurityDetails(request));
        response.setContentType("text/html");
        RequestDispatcher dispatcher = 
		request.getRequestDispatcher("/jsp/securityawareservletresult.jsp");
        dispatcher.forward(request, response);
    }

    private String getSecurityDetails(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        String user = (principal != null)?principal.toString():"Anonymous";
        boolean isManager = request.isUserInRole("MANAGER_ROLE");
        boolean isEmployee = request.isUserInRole("EMPLOYEE_ROLE");

        StringBuffer buff = new StringBuffer();
        buff.append("The User \"").append(user).append("\" is ");
        if (isManager) {
             if (isEmployee) {
                 buff.append("a manager and also an employee.");
             } else {
                 buff.append("a manager but not an employee.");
             }
        } else {
             if (isEmployee) {
                 buff.append("not a manager but is an employee.");
             } else {
                 buff.append("neither a manager nor an employee.");
             }
        }
        return buff.toString();
    }
}
