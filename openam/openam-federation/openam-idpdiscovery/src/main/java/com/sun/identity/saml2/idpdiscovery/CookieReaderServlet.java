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
 * $Id: CookieReaderServlet.java,v 1.4 2009/03/26 19:41:29 madan_ranganath Exp $
 *
 */

package com.sun.identity.saml2.idpdiscovery;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletConfig;

/**
 * The Reader Service is used by the service provider. The service provider
 * redirects the principal to this URL in order to find the preferred identity 
 * provider. Once found, the principal is redirected to the identity provider
 * for single sign-on. The URL is defined as the value for the Reader Service
 * URL attribute when an authentication domain is created. It is formatted as 
 * http://common-domain-host:port/deployment-uri/saml2reader where 
 * common-domain-host:port refers to the machine on which the Common Domain 
 * Services are installed and deployment-uri tells the web container where to 
 * look for information specific to the application (such as classes or JARs).
 * The default URI is amcommon.
 */
public class CookieReaderServlet extends HttpServlet {  
      
    private String preferred_cookie_name = null;  
        
    /**
     * Gets handle to CookieUtils.debug.
     * @param config the ServletConfig object that contains configutation
     *               information for this servlet.
     * @exception ServletException if an exception occurs that interrupts
     *               the servlet's normal operation.
     */
    public void init(ServletConfig config)
        throws ServletException {
        super.init(config);
        if (CookieUtils.debug.messageEnabled()) {
            CookieUtils.debug.message("CookieReaderServlet: Initializing...");
        }
    }
    
    /**
     * Handles the HTTP GET request.
     * @param request an HttpServletRequest object that contains the request
     *                the client has made of the servlet.
     * @param response an HttpServletResponse object that contains the response
     *                 the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the GET request
     * @exception IOException if the request for the GET could not be handled
     */
    public void doGet(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException
    {
        doGetPost(request, response);
    }
    
    /**
     * Handles the HTTP POST request.
     * @param request an HttpServletRequest object that contains the request
     *                the client has made of the servlet.
     * @param response an HttpServletResponse object that contains the response
     *                 the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the GET request
     * @exception IOException if the request for the GET could not be handled
     */
    public void doPost(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException
    {
        doGetPost(request, response);
    }
    
    /**
     * Description :  The QueryString will contain LRURL=URL to redirect with
     * the _saml_idp cookie value
     *
     * @param request an HttpServletRequest object that contains the request
     *                the client has made of the servlet.
     * @param response an HttpServletResponse object that contains the response
     *                 the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the GET request
     * @exception IOException if the request for the GET could not be handled
     */
    private void doGetPost(
        HttpServletRequest  request,
        HttpServletResponse response)
        throws ServletException, IOException
    {
        String classMethod = "CookieReaderServlet.doGetPost: ";
        preferred_cookie_name = CookieUtils.getPreferCookieName(
                request.getRequestURI());
        if (preferred_cookie_name == null) {
            CookieUtils.debug.error( classMethod + 
                "The request uri is null.");
            CookieUtils.sendError(request, response,
                response.SC_INTERNAL_SERVER_ERROR, "nullRequestUri", 
                CookieUtils.bundle.getString("nullRequestUri"));
            return;
        } else if (preferred_cookie_name.equals("")) { 
            CookieUtils.debug.error( classMethod + 
                "Cannot match the cookie name from " +
                "the request uri.");
            CookieUtils.sendError(request, response,
                response.SC_INTERNAL_SERVER_ERROR, "invalidRequestUri",
                CookieUtils.bundle.getString("invalidRequestUri"));
            return;
        }
        
        try{            
            boolean isValidReturn = false;
            String returnURL =
                request.getParameter(IDPDiscoveryConstants.LRURL);
            if (returnURL == null ||
                returnURL.trim().length() <= 0) {
                // Redirect URL not specified. Do nothing
                // Do not throw any error page to user as this operation is
                // done behind the screens.                
                CookieUtils.debug.error(
                    classMethod +
                    "Redirect URL not specified. "+
                    "Cannot send preferred idp in query string"
                );
                isValidReturn = false;
            } else {
                isValidReturn = true;
            }
            if (isValidReturn) {
                String cookieValue =
                    getPreferredIdpCookie(request);
                if (cookieValue != null) {
                    returnURL = appendCookieToReturnURL(
                        returnURL,
                        cookieValue);
                    if (CookieUtils.debug.messageEnabled()) {
                        CookieUtils.debug.message(
                        classMethod + "preferred idp:" +
                        cookieValue); 
                    }
                     
                }
                response.sendRedirect(returnURL);
                return;
            } else {
                CookieUtils.sendError(request, response,
                    response.SC_INTERNAL_SERVER_ERROR, "noRedirectionURL",
                    CookieUtils.bundle.getString("noRedirectionURL"));
                return;
            }
        } catch(Exception e){
            CookieUtils.debug.error(classMethod, e);
            CookieUtils.sendError(request, response,
                    response.SC_INTERNAL_SERVER_ERROR, "readerServiceFailed",
                    e.getMessage());
            return;
        }
    }    
    
    /**
     *
     * This function is used to get the preferred IDP cookie from the request
     * object
     * @param request HTTP request
     * @return string containing the space seperated base64 encoded preferred
     * IDP cookie value
     */
    private String getPreferredIdpCookie(
        HttpServletRequest request) {
        return CookieUtils.getCookieValueFromReq(
            request, 
            preferred_cookie_name
        );
    }
    
    /**
     * This function is used to append the preferred IDP cookie value to the
     * redirect (LRURL) URL
     * @param returnURL URL to redirect to (LRURL)
     * @param cookieValue the _saml_idp cookie value
     * @return String containing the redirect URL with the _saml_idp
     * cookie
     * name/value appended
     */
    private String appendCookieToReturnURL(
        String returnURL,
        String cookieValue) {
        String classMethod =
            "CookieReaderServlet.appendCookieToReturnURL: ";
        if (cookieValue == null ||
            cookieValue.trim().length() <= 0) {
            // Preferred IDP cookie not found
            // Do not throw any error page to user as this operation is done
            // behind the screens.
            CookieUtils.debug.error(
                classMethod +
                "Preferred IDPCookie not found"
            );
        }
        // If the original returnURL already has some params, use
        // AMPERSAND as a delimiter; else use a QUESTION_MARK.
        char delimiter;
        StringBuffer returnBuffer = null;
        if(returnURL.indexOf(IDPDiscoveryConstants.QUESTION_MARK) < 0) {
            delimiter = IDPDiscoveryConstants.QUESTION_MARK;
        } else {
            delimiter = IDPDiscoveryConstants.AMPERSAND;
        }
        returnBuffer = new StringBuffer(100);
        returnBuffer.append(returnURL);
        returnBuffer.append(delimiter);
        returnBuffer.append(preferred_cookie_name);
        returnBuffer.append(IDPDiscoveryConstants.EQUAL_TO);
       
        try { 
            returnBuffer.append(
                java.net.URLEncoder.encode(cookieValue, "UTF-8"));
        } catch (Exception e) { 
            CookieUtils.debug.error("CookieReaderServlet:"
                + "appendCookieToReturnURL" + e.getMessage()); 
        }
        if (CookieUtils.debug.messageEnabled()) {
            CookieUtils.debug.message(
                classMethod +
                "Return URL = " +
                returnBuffer.toString()
            );
        }
        return returnBuffer.toString();
    }
}
