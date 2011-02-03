/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LoanApplication.java,v 1.1 2008/07/12 18:33:43 mallas Exp $
 *
 */
package com.sun.samples.loanrequestorclient;

import java.io.*;
import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.ws.WebServiceRef;


public class LoanApplication extends HttpServlet {
    @WebServiceRef(wsdlLocation = "http://localhost:8080/LoanRequestor/LoanRequestorService?wsdl")
    private LoanRequestorService service;
   
    /** 
    * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
    * @param request servlet request
    * @param response servlet response
    */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String statusmessage = "Unable to process";
        try { // Call Web Service Operation
            com.sun.samples.loanrequestorclient.LoanRequestorPortType port = service.getLoanRequestorPort();
            // TODO initialize WS operation arguments here
            com.sun.samples.loanrequestorclient.ProcessApplicationType requestLoanMessage = new com.sun.samples.loanrequestorclient.ProcessApplicationType();
            String applicantname = request.getParameter("applicantname");
            String ssn = request.getParameter("ssn");
            String gender = request.getParameter("gdender");
            String email = request.getParameter("email");
            String age = request.getParameter("age");
            String address = request.getParameter("address");
            String salary = request.getParameter("salary");
            String loanamount = request.getParameter("loanamount");
            requestLoanMessage.setApplicantName(applicantname);
            requestLoanMessage.setAmountRequested(new Double(loanamount));
            requestLoanMessage.setAnnualSalary(new Double(salary));
            requestLoanMessage.setApplicantAddress(address);
            requestLoanMessage.setApplicantAge(new Integer(age));
            requestLoanMessage.setApplicantEmailAddress(email);
            requestLoanMessage.setApplicantGender(gender);
            requestLoanMessage.setSocialSecurityNumber(ssn);                        
            com.sun.samples.loanrequestorclient.ProcessApplicationResponseType result = port.loanRequestorOperation(requestLoanMessage);
            statusmessage = result.getReturn();
            
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO handle custom exceptions here
        }

        
        
        try {
        
            out.println("<html>");
            out.println("<head>");
            out.println("<title>LoanApplication Status</title>");  
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>LoanApplication Status: </h1>");
            out.println("Status:  " + statusmessage);
            out.println("<p>");            
            out.write("<a href=\"loan.jsp\">Try again</a>");
            out.println("</body>");
            out.println("</html>");
            
        } finally { 
            out.close();
        }
    } 

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
    * Handles the HTTP <code>GET</code> method.
    * @param request servlet request
    * @param response servlet response
    */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    } 

    /** 
    * Handles the HTTP <code>POST</code> method.
    * @param request servlet request
    * @param response servlet response
    */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
    * Returns a short description of the servlet.
    */
    public String getServletInfo() {
        return "Short description";
    }
    // </editor-fold>
}
