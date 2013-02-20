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
 * $Id: ClientFilter.java,v 1.3 2009/11/04 04:55:42 kamna Exp $
 *
 */

package com.sun.identity.wssagents.jaxws.client;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.security.auth.Subject;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.security.handler.ThreadLocalService;

public class ClientFilter implements Filter {

     public void init(FilterConfig config) {
     }

     public void doFilter(ServletRequest request, ServletResponse response,
                   FilterChain chain) throws ServletException {
         HttpServletRequest httpRequest =
                (HttpServletRequest)request;
         HttpServletResponse httpResponse = (HttpServletResponse)response;
         SSOToken ssoToken = null;       
           
         ThreadLocalService.setSubject(null);     
        
         if (shouldAuthenticate()) {
             try {
                 SSOTokenManager manager = SSOTokenManager.getInstance();
                 ssoToken = manager.createSSOToken(httpRequest);
                 if (manager.isValidToken(ssoToken)) {
                     Subject subject = new Subject();
                     subject.getPrivateCredentials().add(ssoToken);
                     ThreadLocalService.setSubject(subject);
                  }else{
                     httpResponse.sendRedirect(getLoginURL(httpRequest));
                     return;
                 }
             } catch (Exception e) {
                 //Invalid SSOToken, hence redirect to Login URL
                 try {
                     if (shouldAuthenticate()) {
                         httpResponse.sendRedirect(getLoginURL(httpRequest));
                         return;
                     }
                 } catch (IOException ie) {
                     ie.printStackTrace();
                 // continue
                 }
             }
         }
         try {
             chain.doFilter(request, response);
         } catch (IOException ie) {
             ie.printStackTrace();
             throw new ServletException(ie.getMessage());
         }

         return;
     }

    /**
     * Returns Login URL for client to be redirected.
     * @param request the <code>HttpServletRequest</code>.
     *
     * @return String Login URL
     */
     public String getLoginURL(HttpServletRequest request) {
         String loginURL =
             SystemProperties.get(Constants.LOGIN_URL);
         StringBuffer requestURL = request.getRequestURL();

         // This is useful for SAML2 integrations.
         String gotoparam = SystemProperties.get(
                "com.sun.identity.loginurl.goto", "goto");
         loginURL = loginURL + "?" + gotoparam + "=" + requestURL.toString();
         String query = request.getQueryString();
         if(query != null) {
             loginURL = loginURL + "&" + query;
         }
         return loginURL;

     }

    /**
     * Checks whether end user authentication is required for WSC configuration
     * or not.
     *
     * @return boolean result
     */
     public boolean shouldAuthenticate() {
         String providername = SystemProperties.get(
             "com.sun.identity.wss.wsc.providername");
         if((providername != null) && (providername.length() != 0)) {
             try {
                 ProviderConfig pc = ProviderConfig.getProvider(
                     providername,ProviderConfig.WSC);
                 if((pc != null) && (!pc.forceUserAuthentication())) {
                     return false;
                 }
             } catch (Exception e) {
                 // continue
             }
         }
         return true;
     }
     
     public void destroy() {
     }   
}
