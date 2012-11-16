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
 * $Id: WebCLI.java,v 1.5 2008/08/19 19:08:57 veiming Exp $
 *
 */

package com.sun.identity.cli;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet responses to OpenSSO commandline request via HTTP or 
 * HTTPS request.
 */
public class WebCLI extends HttpServlet {
    /**
     * Processes requests for both HTTP <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(
        HttpServletRequest request, 
        HttpServletResponse response
    ) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        CommandManager cmdMgr = null;
        BufferOutputWriter outputWriter = new BufferOutputWriter();

        try {
            Map env = new HashMap();
            env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
            env.put(CLIConstants.ARGUMENT_LOCALE, request.getLocale());
            env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
                "com.sun.identity.cli.AccessManager");
            env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "amadm");
            cmdMgr = new CommandManager(env);

            List list = new ArrayList();
            Map map = request.getParameterMap();
            int i = 0;
            while (true) {
                String[] values = (String[])map.get("arg" + i);
                if (values == null) {
                    break;
                } else {
                    list.add(values[0]);
                    i++;
                }
            }

            int sz = list.size();
            String[] args = new String[sz];
            for (int j = 0; j < sz; j++) {
                args[j] = (String)list.get(j);
            }

            CLIRequest req = new CLIRequest(null, args);
            cmdMgr.addToRequestQueue(req);
            cmdMgr.serviceRequestQueue();
            out.println(outputWriter.getBuffer());
            outputWriter.clearBuffer();
        } catch (CLIException e) {
            e.printStackTrace();
            out.println(e);
        }
        out.close();
    }
    
    /** 
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request.
     * @param response servlet response.
     */
    protected void doGet(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request.
     * @param response servlet response.
     */
    protected void doPost(
        HttpServletRequest request, 
        HttpServletResponse response
    ) throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "OpenSSO Web Base CLI";
    }
}
