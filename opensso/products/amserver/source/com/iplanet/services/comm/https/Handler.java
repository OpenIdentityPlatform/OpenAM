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
 * $Id: Handler.java,v 1.2 2008/06/25 05:41:34 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.comm.https;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import com.iplanet.am.util.JSSInit;

/**
 * This is the URL protocol handler class for HTTPS.  It's
 * HTTP with the Secure Sockets Layer (SSL) between HTTP and
 * TCP protcols.
 *
 */
public class Handler extends sun.net.www.protocol.http.Handler
{
    private String nickName = null;

    static{
        JSSInit.initialize();
    }

    /**
     * Default constructor, this one must be there in order for setting 
     * java.protocol.handler.pkgs=com.iplanet.services.comm
     * to work on standalone program
     */
    public Handler () {
        super();
    }

    /**
     * Returns default port number for protocol https 
     */
    protected int getDefaultPort() {
        return 443;
    }

    /**
     * Constructor
     * @param clientCertNickName  nick name for client certificate used in the
     *     https connection if client auth is required
     */
    public Handler (String clientCertNickName) {
        super();
        nickName = clientCertNickName;
    }
    
    protected URLConnection openConnection (URL u)
        throws IOException {
        HttpsURLConnection conn = new HttpsURLConnection (u, this);
        // set client certificate used for SSL connection
        if (nickName != null) {
            conn.setClientCertificate(nickName);
        }
        return conn;
    }
}
