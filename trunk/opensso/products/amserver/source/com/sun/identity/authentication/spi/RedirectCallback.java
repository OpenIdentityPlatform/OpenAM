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
 * $Id: RedirectCallback.java,v 1.5 2008/08/19 19:08:55 veiming Exp $
 *
 */

package com.sun.identity.authentication.spi;

import java.util.HashMap;
import java.util.Map;
import javax.security.auth.callback.Callback;

/* iPlanet-PUBLIC-CLASS */
/**
 * This <code>RedirectCallback</code> class implements <code>Callback</code>
 * and is used by the authentication module when redirect to a particulat URL 
 * is required with specific redirect data.
 */

public class RedirectCallback implements Callback, java.io.Serializable {
    
    private String redirectUrl = null;
    private Map redirectData = new HashMap();
    private String method = "get";
    private String status = null;
    private String statusParameter = "AM_AUTH_SUCCESS_PARAM";
    private String redirectBackUrlCookie = "AM_REDIRECT_BACK_SERVER_URL";
    
    /**
     *  The status as the result of redirection, as "SUCCESS".
     */
    public static final String SUCCESS = "SUCCESS";
    
    /**
     *  The status as the result of redirection, as "FAILED".
     */
    public static final String FAILED = "FAILED";
    
    /**
     * Constructs a <code>RedirectCallback</code> object.
     */
    public RedirectCallback() {    
    }

    /**
     * Constructs a <code>RedirectCallback</code> object with
     * redirect URL,redirect data and redirect method.
     *
     * @param redirectUrl URL to be redirected to.
     * @param redirectData the data to be redirected to redirect URL.
     * @param method Method used for redirection, either "GET" or "POST".
     */
    public RedirectCallback(String redirectUrl, Map redirectData, 
        String method) {        
        this.redirectUrl = redirectUrl;
        this.redirectData = redirectData;
        this.method = method;
    }
    
    /**
     * Constructs a <code>RedirectCallback</code> object with
     * redirect URL,redirect data,redirect method,status parameter
     * and redirect back URL Cookie name.
     *
     * @param redirectUrl URL to be redirected to.
     * @param redirectData the data to be redirected to redirect URL.
     * @param method Method used for redirection, either "GET" or "POST".
     * @param statusParameter statusParameter to be checked from 
     * HttpServletRequest object at the result of redirection.
     * @param redirectBackUrlCookie redirectBackUrlCookie name to be set as 
     * OpenSSO server URL when redirecting to external web site.
     */
    public RedirectCallback(String redirectUrl, Map redirectData, 
                            String method, String statusParameter, 
                            String redirectBackUrlCookie) {        
        this.redirectUrl = redirectUrl;
        this.redirectData = redirectData;
        this.method = method;
        this.statusParameter = statusParameter;
        this.redirectBackUrlCookie = redirectBackUrlCookie;
    }
    
    /**
     * Sets the URL to be redirected to.
     *
     * @param redirectUrl URL to be redirected to.
     */
    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
    
    /**
     * Returns the URL to be redirected to.
     * @return the URL to be redirected to.
     */
    public String getRedirectUrl() {
        return redirectUrl;
    }
    
    /**
     * Sets the data to be redirected to redirect URL. This data is in the
     * form of HashMap of name / value pairs.
     *
     * @param redirectData the data to be redirected to redirect URL.
     */
    public void setRedirectData(Map redirectData) {
        this.redirectData = redirectData;
    }
    
    /**
     * Returns the data to be redirected to redirect URL.
     * @return the Map of data to be redirected to redirect URL.
     */
    public Map getRedirectData() {
        return redirectData;
    }
    
    /**
     * Sets the Method used for redirection, either "GET" or "POST".
     *
     * @param method Method used for redirection, either "GET" or "POST".
     */
    public void setMethod(String method) {
        this.method = method;
    }
    
    /**
     * Returns the Method used for redirection, either "GET" or "POST".
     * @return the Method used for redirection, either "GET" or "POST".
     */
    public String getMethod() {
        return method;
    }
    
    /**
     * Sets the status as the result of redirection, 
     * either "SUCCESS" or "FAILED".
     *
     * @param status status as the result of redirection, 
     * either "SUCCESS" or "FAILED".
     */
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Returns the status as the result of redirection, 
     * either "SUCCESS" or "FAILED".
     *
     * @return the status as the result of redirection, 
     * either "SUCCESS" or "FAILED".
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Sets the status parameter to be checked from HTTP Servlet Request object
     * at the result of redirection.
     *
     * @param statusParameter Status parameter.
     */
    public void setStatusParameter(String statusParameter) {
        this.statusParameter = statusParameter;
    }
    
    /**
     * Returns the statusParameter to be checked from HttpServletRequest object 
     * at the result of redirection.
     *
     * @return the statusParameter to be checked from HttpServletRequest object 
     * at the result of redirection.
     */
    public String getStatusParameter() {
        return statusParameter;
    }
    
    /**
     * Sets the redirectBackUrlCookie name to be set as OpenSSO 
     * server URL when redirecting to external web site.
     *
     * @param redirectBackUrlCookie redirectBackUrlCookie name to be set as 
     * OpenSSO server URL when redirecting to external web site.
     */
    public void setRedirectBackUrlCookieName(String redirectBackUrlCookie) {
        this.redirectBackUrlCookie = redirectBackUrlCookie;
    }
    
    /**
     * Returns the redirectBackUrlCookie name to be set as OpenSSO 
     * server URL when redirecting to external web site.
     *
     * @return the redirectBackUrlCookie name to be set as OpenSSO 
     * server URL when redirecting to external web site.
     */
    public String getRedirectBackUrlCookieName() {
        return redirectBackUrlCookie;
    }
   
}

