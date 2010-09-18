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
 * $Id: SOAPMessage.java,v 1.3 2009/12/04 00:53:12 kamna Exp $
 *
 */
package com.sun.stockquote;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import javax.servlet.ServletContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SOAPMessage extends HttpServlet {

	private ServletContext servletContext;

	public void init(ServletConfig config) {
		servletContext = config.getServletContext();
	}

	/**
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
	 * methods.
	 * 
	 * @param request Http Servlet Request
	 * @param response Http Servlet Response
	 * 
	 */
	protected void processRequest(HttpServletRequest request,
	    HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/xml;charset=UTF-8");
		PrintWriter out = response.getWriter();
		String file = request.getParameter("dir");
		if (file == null || file.length() == 0) {
			file = "request";
		}
		String fileName = System.getProperty("user.home") + "/opensso/" + file;
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(fileName)));
        String line;
        if(!((line = br.readLine()).startsWith("<?xml"))){
            out.println("<?xml version='1.0' encoding=\"ISO-8859-1\"?>");
        }
        out.println(line);
        while ((line = br.readLine()) != null) {
			out.println(line);
		}
		br.close();
		out.close();
	}

	/**
	 * Handles the HTTP <code>GET</code> method.
	 * 
	 * @param request Http Servlet Request
	 * @param response Http Servlet Response
     *
	 */
	protected void doGet(HttpServletRequest request,
	    HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Handles the HTTP <code>POST</code> method.
	 * 
	 * @param request Http Servlet Request
	 * @param response Http Servlet Response
     *
	 */
	protected void doPost(HttpServletRequest request,
	    HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Returns a short description of the servlet.
	 */
	public String getServletInfo() {
		return "Short description";
	}

}
