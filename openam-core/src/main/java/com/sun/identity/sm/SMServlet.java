/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SMServlet.java,v 1.4 2008/06/25 05:44:05 qcheng Exp $
 *
 */

package com.sun.identity.sm;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;

/**
 * The <code>SMServlet</code> provides`simple http interface to obtain service
 * configuration
 */
public class SMServlet extends HttpServlet {

    static final Debug debug = SMSEntry.debug;

    // Supported operations
    static final String METHOD = "method";
    static final String VERSION = "version";

    static final String IS_REALM_ENABLED = "isRealmEnabled";

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        doPost(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String method = request.getParameter(METHOD);
        String answer = "no data";
        if (method != null && method.equalsIgnoreCase(IS_REALM_ENABLED)) {
            if (ServiceManager.isRealmEnabled()) {
                answer = "true";
            } else {
                answer = "false";
            }
        } else if (method != null && method.equalsIgnoreCase(VERSION)) {
            answer = SystemProperties.get(Constants.AM_VERSION);
        }

        // Send the response
        if (debug.messageEnabled()) {
            debug.message("SMServlet::doPost request=" + method + " response="
                    + answer);
        }
        response.setContentType("text/html");
        response.setHeader("Pragma", "no-cache");
        PrintWriter out = response.getWriter();
        out.println(answer);
        out.flush();
        out.close();
    }
}
