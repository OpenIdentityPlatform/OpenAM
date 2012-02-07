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
 * $Id: AmAgentServletRequest.java,v 1.2 2008/06/25 05:51:42 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.sun.identity.agents.common.IHttpServletRequestHelper;

/**
 * An agent wrapper class of HttpServletRequest
 */
public class AmAgentServletRequest extends HttpServletRequestWrapper {
    
    public AmAgentServletRequest(HttpServletRequest request, 
            IHttpServletRequestHelper helper) 
    {
        super(request);
        setHelper(helper);
    }
    
    public ServletInputStream getInputStream() throws IOException {
        return getHelper().getInputStream(super.getInputStream());
    }
    
    public Enumeration getHeaders(String name) {
        return getHelper().getHeaders(name, super.getHeaders(name));
    }
    
    public Enumeration getHeaderNames() {
        return getHelper().getHeaderNames(super.getHeaderNames());
    }
    
    public String getHeader(String name) {
        return getHelper().getHeader(name, super.getHeader(name));
    }
    
    public long getDateHeader(String name) {
        return getHelper().getDateHeader(name, super.getDateHeader(name));
    }
    
    public int getIntHeader(String name) {
        return getHelper().getIntHeader(name, super.getIntHeader(name));
    }
    
    private IHttpServletRequestHelper getHelper() {
        return _helper;
    }
    
    private void setHelper(IHttpServletRequestHelper helper) {
        _helper = helper;
    }

    private IHttpServletRequestHelper _helper;

}
