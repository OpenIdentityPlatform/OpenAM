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
 * $Id: AmASJ2EEAuthHandler.java,v 1.2 2008/06/25 05:52:11 qcheng Exp $
 *
 */

package com.sun.identity.agents.appserver.v81;



import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.sun.appserv.security.ProgrammaticLogin;
import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.filter.AmFilterManager;
import com.sun.identity.agents.filter.IJ2EEAuthenticationHandler;


/**
 *  The J2EE Authentication handler class for Sun Appserver 8.1 Agent
 */
public class AmASJ2EEAuthHandler implements IJ2EEAuthenticationHandler {

    /**
     * Authenticate a user based on username and password
     *
     * @param userName The user name
     * @param password The user's password
     * @param request The HttpServeltRequest object
     * @param response The HttpServletResponse object
     * @param extraData Some extra data for the authentication
     *
     * @return true if authentication is successful
     *
     */
    public boolean authenticate(String userName, String password,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                Object extraData) {

        boolean           result  = false;
        HttpSession       session = request.getSession(true);
        ProgrammaticLogin pLogin  = new ProgrammaticLogin();

        if((userName != null) && (password != null)) {
            IModuleAccess modAccess = AmFilterManager.getModuleAccess();
            result = pLogin.login(userName, password, request,
                                  response).booleanValue();

            if( !result) {
                if (modAccess.isLogWarningEnabled()) {
                    modAccess.logMessage("AmAS81J2EEAuthHandler: "
                        + "Programmatic Login Failed, Invalidating Session");
                }
                session.invalidate();
            } else {
                // Show some success message
                if (modAccess.isLogMessageEnabled()) {
                    modAccess.logMessage("AmAS81J2EEAuthHandler: "
                                + "Programmatic Login was successful");
                }
            }
        }

        return result;
    }
}

