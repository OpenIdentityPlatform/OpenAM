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
 * $Id: EvaluatePolicyServlet.java,v 1.2 2008/06/25 05:40:25 qcheng Exp $
 *
 */
package com.sun.identity.demo.server;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.Set;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.shared.debug.Debug;
//import com.iplanet.am.util.Debug;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.am.util.XMLUtils;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

import com.sun.identity.shared.Constants;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.policy.PolicyDecision;

public class EvaluatePolicyServlet extends HttpServlet {
    
    // the debug file
    private static Debug debug = Debug.getInstance("amEvaluatePolicyServlet");
    static final String WEB_AGENT_SERVICE = "iPlanetAMWebAgentService";
    public static final String NEWLINE = 
         System.getProperty("line.separator", "\n");
    static String serverUrl = SystemProperties.get(Constants.AM_SERVER_PROTOCOL)
        + "://" + SystemProperties.get(Constants.AM_SERVER_HOST)
        + ":" + SystemProperties.get(Constants.AM_SERVER_PORT) 
        + SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR) 
        + "/UI/Login";
    
    
    ServletConfig config = null;
    
    /**
     * Initializes the servlet.
     * @param config servlet config
     * @throws ServletException if it fails to get servlet context.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.config = config;
    }
    
    /** 
     * Destroys the servlet.
     */
    public void destroy() {
        
    }
    
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request.
     * @param response servlet response.
     * @throws ServletException
     * @throws java.io.IOException
     */
    protected void doGet(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, java.io.IOException {
        processRequest(request, response);
    }
    
    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException
     * @throws java.io.IOException
     */
    protected void doPost(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, java.io.IOException {
        processRequest(request, response);
    }
    
    /**
     * Reads the resource which the user needs to access from the servlet
     * request parameter <code>resource</code>.
     * if the user's session is invalid, the user gets redirected to the 
     * amserver login page to log in first.
     * Once the session is valid, the access permissions for the requested 
     * resource  is computed and sent back in the servlet response.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException
     * @throws java.io.IOException
     */
    protected void processRequest(
        HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, java.io.IOException 
    {        
        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            SSOToken ssoToken = mgr.createSSOToken(request);
            if (mgr.isValidToken(ssoToken)) {
                if (ssoToken.getProperty(Constants.UNIVERSAL_IDENTIFIER) 
                    != null) 
                {
                    debug.message("UNIV ID in ssoToken:"
                        +ssoToken.getProperty(Constants.UNIVERSAL_IDENTIFIER));
                } else {
                    debug.message("univ id is null");
                    if (debug.messageEnabled()) {
                        debug.message("principal:"
                            +ssoToken.getPrincipal().getName());
                    }
                }
                    String resource = request.getParameter("resource");
                PolicyEvaluator pe = new PolicyEvaluator(WEB_AGENT_SERVICE);
                    Set actions = new HashSet();
                actions.add("GET");
                PolicyDecision pd = pe.getPolicyDecision(ssoToken, resource, 
                    actions, null);
                boolean allowed = pe.isAllowed(ssoToken, resource, "GET", null);
                StringBuffer message = new StringBuffer("<pre>");
                message.append("isAllowed() for ").append(resource).
                append(" action:GET is:   ");
                message = message.append(allowed);
                message.append(NEWLINE);
                message.append(NEWLINE);
                message.append("getPolicyDecision() for ").append(resource).
                    append(" action:GET is:");
                message.append(NEWLINE);
                message.append(XMLUtils.escapeSpecialCharacters(pd.toXML()));
                message.append("</pre>");
                sendResponse(response, message.toString());
            }
        } catch (Exception ire) {
            debug.error("processRequest::exception:",ire);
            String requestUrl = request.getRequestURL().toString();
            String redirectUrl = serverUrl + "?goto=" + requestUrl;
            response.sendRedirect(redirectUrl);            
            return;
        }
    }
    
    private void sendResponse(HttpServletResponse response, String message)
        throws IOException {
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        
        writer.println("<html><head><title>Policy Evaluation Result</title>"
            +"</head>");
        writer.println("<body bgcolor=\"#FFFFFF\" text=\"#000000\">");
        writer.println("<p><p><b>" + message + "</b>");
        writer.println("</body></html>");
        writer.flush();
        writer.close();
    }
}
