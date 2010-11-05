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
 * $Id: OpenSSOHttpServletResponse.java,v 1.1 2008/10/07 17:36:32 huacui Exp $
 *
 */

package com.sun.identity.agents.filter;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.ServletOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * A helper class used to manage the servlet response content. 
 */
public class OpenSSOHttpServletResponse extends HttpServletResponseWrapper
{
    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    public OpenSSOHttpServletResponse(HttpServletResponse response) {
        super(response);
    }

    public ServletOutputStream getOutputStream() {
        return new OpenSSOServletOutputStream(bos);
    }

    /**
     * Returns the contents of the response
     */
    public String getContents() {
        return bos.toString();
    }
}
