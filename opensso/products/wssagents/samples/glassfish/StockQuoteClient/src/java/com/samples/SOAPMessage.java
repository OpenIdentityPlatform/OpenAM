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
 * $Id: SOAPMessage.java,v 1.3 2008/12/20 01:30:46 mallas Exp $
 *
 */
package com.samples;

import java.io.*;
import java.net.URL;

import javax.servlet.*;
import javax.servlet.http.*;

public class SOAPMessage extends HttpServlet {

     private static String requrl = 
              "http://localhost:8080/StockService/request";
     private ServletContext servletContext;

     public void init(ServletConfig config) {
         servletContext = config.getServletContext();
     }
    
    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/xml;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String file = request.getParameter("dir");
        if (file == null || file.length() == 0) {
            file = "request";
        }
        InputStream is = null;
        if(!file.equals("request")) {
           String fileName = servletContext.getRealPath("/") + "/" + file;
            // Open the file and return the message
           is = new FileInputStream(fileName);
        } else {
           try {
               URL url = new URL(requrl);
               is = url.openConnection().getInputStream(); 
           } catch (Exception ex) {
               ex.printStackTrace();
               return;
           }
        }
        BufferedReader br = new BufferedReader(
            new InputStreamReader(is));
        out.println("<?xml version='1.0' encoding=\"ISO-8859-1\"?>\n\n");
        String line;
        while ((line = br.readLine()) != null) {
            out.println(line);
        }
        br.close();
        out.close();
    }
    
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Short description";
    }
    // </editor-fold>
}
