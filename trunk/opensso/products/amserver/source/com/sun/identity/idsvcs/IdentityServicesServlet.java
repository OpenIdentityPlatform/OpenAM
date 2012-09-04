/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IdentityServicesServlet.java,v 1.3 2009/01/05 23:53:38 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.idsvcs;

import com.sun.xml.rpc.server.http.JAXRPCServlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Provides implementation for returning WSDL.
 * Obtains the hostname, port, etc from Naming Service
 * and should be aware of load balance i.e., sites.
 */
public class IdentityServicesServlet extends JAXRPCServlet {

    private String wsdl;
    private ServletContext servletCtx;
    
    /**
     * Obtain a pointer to the servlet context
     */
    public void init(ServletConfig config) throws ServletException {
        servletCtx = config.getServletContext();
        super.init(config);
    }
    
    /** 
    * Checks for "wsdl" query parameter and returns the wsdl
    */
    public void doGet(HttpServletRequest request,
        HttpServletResponse response) throws ServletException {
        String queryParam = request.getQueryString();
        if ((queryParam != null) && (queryParam.equalsIgnoreCase("wsdl"))) {
            try {
                // Check if the wsdl is cached
                if (wsdl == null) {
                    // Read the wsdl from deployment
                    InputStream is = servletCtx.getResourceAsStream(
                        "/WEB-INF/wsdl/IdentityServices.wsdl");
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder(1000);
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    // Replace host, port & protocols
                    wsdl = sb.toString();
                    int start = wsdl.indexOf("REPLACE_WITH_ACTUAL_URL");
                    if (start != -1) {
                        String nwsdl = wsdl.substring(0, start);
                        nwsdl += request.getRequestURL().toString();
                        if (!nwsdl.endsWith("/IdentityServices")) {
                            nwsdl += "/IdentityServices";
                        }
                        wsdl = nwsdl + wsdl.substring(start + 23);
                    }
                }
                response.setContentType("text/xml");
                PrintWriter out = response.getWriter();
                out.write(wsdl);
                out.flush();
                out.close();
            } catch (IOException ioe) {
                // Debug and return null
            }
        } else {
            response.setCharacterEncoding("UTF-8");
            super.doGet(request, response);
        }
    } 
}
