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
 * $Id: HttpServletRequestWrapper.java,v 1.4 2009/05/02 23:06:30 kevinserwin Exp $
 *
 */
/**
 * Portions Copyrighted 2013 ForgeRock AS
 */
package com.sun.identity.setup;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class HttpServletRequestWrapper
    implements IHttpServletRequest {
    private HttpServletRequest req;
    private Map parameterMap = new HashMap();

    public HttpServletRequestWrapper(HttpServletRequest req) {
        this.req = req;
        for (Enumeration e = req.getParameterNames(); e.hasMoreElements(); ) {
            String k = (String)e.nextElement();
            parameterMap.put(k, req.getParameter(k));
        }
    }

    public HttpServletRequestWrapper(Map data) {
        this.req = null;
        parameterMap.putAll(data);
    }

    /**
     * Returns the locale of the request.
     *
     * @return the locale of the request.
     */
    public Locale getLocale() {
        return (req != null) ? req.getLocale() : Locale.getDefault();
    }

    /**
     * Returns all parameters values.
     *
     * @return all parameters values
     */
    public Map getParameterMap() {
        return parameterMap;
    }

    /**
     * Adds parmeter.
     *
     * @param parameterName Name of Parameter. 
     * @param parameterValue Value of Parameter. 
     */
    public void addParameter(String parameterName, Object parameterValue) {
        parameterMap.put(parameterName, parameterValue);
    }
    
    /**
     * Returns values of a parameter.
     *
     * @param parameterName Name of Parameter. 
     * @return values of a parameter.
     */
    public Object getParameter(String parameterName) {
        return parameterMap.get(parameterName);
    }

    /**
     * Returns the context path.
     *
     * @return the context path.
     */
    public String getContextPath() {
        return (req != null) ? req.getContextPath() : (String) parameterMap.get("DEPLOYMENT_URI");
    }

    /**
     * Returns header.
     *
     * @param key Key to header value.
     * @return header.
     */
    public String getHeader(String key) {
        return (req != null) ? req.getHeader(key) : null;
    }
}
