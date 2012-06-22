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
 * $Id: FAMHttpServletRequest.java,v 1.2 2008/06/25 05:54:48 qcheng Exp $
 *
 */

package com.sun.identity.wssagents.weblogic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.ServletInputStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * A helper class used to manage the servlet request content. 
 */
public class FAMHttpServletRequest extends HttpServletRequestWrapper
{
    private static int BUFFER_SIZE = 1024;
    private byte[] bytes;

    public FAMHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    public ServletInputStream getInputStream() {
        return new FAMServletInputStream(new ByteArrayInputStream(bytes));
    }

    /**
     * Returns the contents of the request
     */
    public String getContents() throws java.io.IOException {
        InputStream istream = super.getInputStream();
        StringBuffer buffer = new StringBuffer();
        byte[] strBytes = new byte[BUFFER_SIZE];
        int length = 0;
        while ((length = istream.read(strBytes, 0, BUFFER_SIZE)) != -1) {
            buffer.append(new String(strBytes, 0, length));
        }
        if (istream != null) {
            istream.close();
        }
        bytes = buffer.toString().getBytes();
        return buffer.toString();
    }
    
    /**
     * Sets the contents of the request
     */
    public void setContents(String contents) throws java.io.IOException {
        bytes = contents.getBytes();
    }
}
