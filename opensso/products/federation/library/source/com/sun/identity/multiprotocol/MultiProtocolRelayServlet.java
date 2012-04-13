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
 * $Id: MultiProtocolRelayServlet.java,v 1.4 2008/06/25 05:47:25 qcheng Exp $
 *
 */

package com.sun.identity.multiprotocol;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.federation.common.IFSConstants;

/**
 * The <code>MultiProtocolRelayServlet</code> class is a servlet used as
 * returned <code>RelayState</code> for HTTP based protocol to continue on
 * next federation protocol.
 *
 */
public class MultiProtocolRelayServlet extends HttpServlet {
    
    /** Processes requests for both HTTP <code>GET</code> and 
     * <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, 
        HttpServletResponse response)
        throws ServletException, IOException {
        String uri = request.getRequestURI();
        int index = uri.lastIndexOf("/");
        String status = request.getParameter(SingleLogoutManager.STATUS_PARAM);
        int currentStatus = SingleLogoutManager.LOGOUT_FAILED_STATUS;
        // TODO : handle all possible logout status from different protocols
        // check logout success status for IDFF 
        // ws-federation does not have logout status, assume success
        if ((status == null) || status.equals(IFSConstants.LOGOUT_SUCCESS)) {
            currentStatus = SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS;
        }
        String handler = uri.substring(index + 1);
        SingleLogoutManager manager = SingleLogoutManager.getInstance();
        if (SingleLogoutManager.debug.messageEnabled()) {
            SingleLogoutManager.debug.message("MultiProtocolRelayServlet." +
                "processRequest: handler=" + handler + 
                ", status string =" + status +  
                ", status int value =" + currentStatus);
        }
        try {
           
            int retStatus = manager.doIDPSingleLogout(null, null, request, 
                response, false, true, null, null, null, null, handler, 
                null, null, currentStatus);
            if (retStatus != SingleLogoutManager.LOGOUT_REDIRECTED_STATUS) {
                SingleLogoutManager.getInstance().sendLogoutResponse(
                    request, response, handler);
            }
        } catch (Exception ex) {
            SingleLogoutManager.debug.error(
                "MultiProtocolRelayServlet.processRequest: doSLO", ex);
            throw new ServletException(ex.getMessage());
        }
        return;
    }
   
    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, 
        HttpServletResponse response)
        throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, 
        HttpServletResponse response)
        throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Relay servlet for multi-federation protocol feature";
    }
}
