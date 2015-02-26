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
 * $Id: WebServiceRequestInputStream.java,v 1.2 2008/06/25 05:51:49 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ServletInputStream;

public class WebServiceRequestInputStream extends ServletInputStream {

    public WebServiceRequestInputStream(String body, String encoding) 
    throws Exception {
        setInputStream(new ByteArrayInputStream(body.getBytes(encoding)));
    }

    public int read() throws IOException {
        return getInputStream().read();
    }

    public int available() throws IOException {
        return getInputStream().available();
    }

    private ByteArrayInputStream getInputStream() {
        return _inputStream;
    }

    private void setInputStream(ByteArrayInputStream istream) {
        _inputStream = istream;
    }

    private ByteArrayInputStream _inputStream;
}
