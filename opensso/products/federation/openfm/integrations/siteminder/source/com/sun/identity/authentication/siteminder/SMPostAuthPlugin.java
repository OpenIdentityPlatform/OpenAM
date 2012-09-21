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
 * $Id: SMPostAuthPlugin.java,v 1.2 2008/06/25 05:48:57 qcheng Exp $
 *
 */


package com.sun.identity.authentication.siteminder;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;

/**
 * This class <code>SMPostAuthPlugin</code> implements 
 * <code>AMPostAuthProcessInterface</code> and this post auth plug-in
 * will be used for setting the siteminer HTTP headers into SSOToken
 * for attribute exchange.
 */
public class SMPostAuthPlugin implements AMPostAuthProcessInterface {
    /**
     * Post processing on successful authentication.
     *
     * @param requestParamsMap map containing <code>HttpServletRequest</code>
     *        parameters
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object.
     * @param ssoToken authenticated user's single sign token.
     * @exception AuthenticationException if there is an error.
     */
    public void onLoginSuccess(
        Map requestParamsMap,
        HttpServletRequest request,
        HttpServletResponse response,
        SSOToken ssoToken
    ) throws AuthenticationException {
        
        Set configuredHTTPHeaders = (Set)request.getAttribute("SM-HTTPHeaders");
        if(configuredHTTPHeaders == null || configuredHTTPHeaders.isEmpty()) {
           System.out.println("HTTP headers in auth module are not configured");
           return;
        }
        
        for (Iterator iter = configuredHTTPHeaders.iterator(); 
                                    iter.hasNext();) {
             String configHeader = (String)iter.next();
             String headerValue = request.getHeader(configHeader);
             if(headerValue == null) {
                System.out.println("Config Header " + configHeader +
                               " is not present");
                continue;
             }
             try {
                 ssoToken.setProperty(configHeader, headerValue);
             } catch (SSOException se) {
                 throw new AuthenticationException(se.getMessage());
             }
            
        }
    }

    /**
     * Post processing on failed authentication.
     *
     * @param requestParamsMap map containing <code>HttpServletRequest<code>
     *        parameters.
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object.
     * @throws AuthenticationException when there is an error.
     */
    public void onLoginFailure(
        Map requestParamsMap,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws AuthenticationException {
        
    }
 
    /**
     * Post processing on Logout.
     *
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object.
     * @param ssoToken authenticated user's single sign on token.
     * @throws AuthenticationException
     */
    public void onLogout(
        HttpServletRequest request,
        HttpServletResponse response,
        SSOToken ssoToken
    ) throws AuthenticationException {
        
    }
}
