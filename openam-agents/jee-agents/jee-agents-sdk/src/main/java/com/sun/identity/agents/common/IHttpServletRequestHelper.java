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
 * $Id: IHttpServletRequestHelper.java,v 1.2 2008/06/25 05:51:39 qcheng Exp $
 *
 */

package com.sun.identity.agents.common;

import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletInputStream;

/**
 * The interface for HttpServletRequestHelper
 */
public interface IHttpServletRequestHelper {
    public abstract void initialize(String dateFormatString, Map attributes, 
            ServletInputStream inputStream);
    
    public abstract ServletInputStream getInputStream(
            ServletInputStream inputStream);

    public abstract Enumeration getHeaders(
            String name, Enumeration innerHeaders);

    public abstract Enumeration getHeaderNames(Enumeration innerHeaderNames);

    public abstract String getHeader(String name, String innerValue);

    public abstract long getDateHeader(String name, long innerValue);

    public abstract int getIntHeader(String name, int innerValue);
    
    public abstract Map getUserAttributes();
    
    public abstract void addUserAttributes(Map newAttributes);
}
