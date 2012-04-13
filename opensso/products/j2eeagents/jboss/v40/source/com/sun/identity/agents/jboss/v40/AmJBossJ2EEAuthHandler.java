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
 * $Id: AmJBossJ2EEAuthHandler.java,v 1.1 2009/01/10 08:10:48 naghaon Exp $
 *
 */

package com.sun.identity.agents.jboss.v40;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jboss.web.tomcat.security.login.WebAuthentication;
import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.filter.AmFilterManager;
import com.sun.identity.agents.filter.IJ2EEAuthenticationHandler;

/**
 * The generic implementation of <code>IJ2EEAuthenticationHandler</code> interface.
 */

public class AmJBossJ2EEAuthHandler implements IJ2EEAuthenticationHandler {
   
    /**
     * Authenticates a user 
     *
     * @param userName the name of the user
     * @param password the password of the user
     * @param request the HttpServletRequest object of the user request
     * @param response the HttpServletResponse object to the user request
     * @param extraData the extra data used for authenticating the user
     *
     * @return true if the authentication is successful
     *
     */

    public boolean authenticate(String userName, String password,
            HttpServletRequest request,
            HttpServletResponse response,
            Object extraData) {
         boolean result  = false;
         HttpSession       session = request.getSession(true);
         WebAuthentication pwl = new WebAuthentication();
             if((userName != null) && (password != null)) {
             IModuleAccess modAccess = AmFilterManager.getModuleAccess();
             result=pwl.login(userName, password);
                 
             if( !result) {
                 if (modAccess.isLogWarningEnabled()) {
                     modAccess.logMessage("AmJBossJ2EEAuthHandler: "
                         + "Programmatic Login Failed, Invalidating Session");
                 }
                 session.invalidate();
             }  else {
                // Show some success message
                 if (modAccess.isLogMessageEnabled()) {
                     modAccess.logMessage("AmJBossJ2EEAuthHandler: "
                                + "Programmatic Login was successful");
                 }
                }
             }
         return result;  
    }
}