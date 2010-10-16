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
 * $Id: AmWLPortalLocalLogoutHandler.java,v 1.2 2008/06/25 05:52:22 qcheng Exp $
 *
 */

package com.sun.identity.agents.weblogic.v10;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.filter.GenericJ2EELogoutHandler;
import com.sun.identity.agents.filter.AmFilterManager;

/**
 * Class AmWLPortalLocalLogoutHandler serves to logout Weblogic Portal 10.
 *
 * @see Local Logout Handler for WL10 portal
 */
public class AmWLPortalLocalLogoutHandler extends GenericJ2EELogoutHandler {
    
    /**
     * Method logout
     *
     * @param request HTTPServletRequest object
     * @param response HttpServletResponse object
     * @param extraData Object
     *
     * @return boolean status if authentication succeeded or failed
     */
    public boolean logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Object extraData) {
        
        boolean result = false;
        IModuleAccess modAccess = AmFilterManager.getModuleAccess();
        
        try {
            Class[] parameterClasses = { HttpServletRequest.class };
            
            Method method = AmWLAgentUtils.getClassMethod(
                    modAccess,
                    "com.bea.p13n.security.Authentication",
                    "logout", 
                    parameterClasses);
            Object[] parameters = { request };
            method.invoke(null, parameters );
            
            result = true;
        } catch (Exception ex) {
            if (modAccess.isLogWarningEnabled()) {
                modAccess.logWarning("AmWLPortalLocalLogoutHandler.logout - "
                        + "Programmatic Logout Failed! " + ex);
            }
        }
        
        return result;
    }
        
}

