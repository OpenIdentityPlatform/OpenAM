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
 * $Id: HttpsURLConnection.java,v 1.2 2008/06/25 05:41:34 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.comm.https;

import java.net.URL;
import java.io.IOException;

import sun.net.www.http.*;
import sun.net.www.protocol.http.*;

import org.mozilla.jss.crypto.X509Certificate;

/**
 * HTTPS URL connection support.  These URL connection classes are
 * not always made public, but this one needs to be public in order
 * to expose SSL-related information which must needs to be presented
 * to users.
 *
 */
public
class HttpsURLConnection extends HttpURLConnection
{
    private String nickName;

    protected void setNewClient (URL url)
    throws IOException {
        http = getNewClient (url);
    }
 
    /**
     * Returns a connection to an HTTPS server.
     *
     * @param url identifies the HTTPS server to connect with.
     */
    protected HttpClient getNewClient (URL url)
    throws IOException {
        HttpsClient client = new HttpsClient (url);
        if (nickName != null) {
            client.setClientCertificate(nickName);
        }
        return client;
    }

    /**
     * We don't support HTTPS through proxies, since that compromises
     * the expectation that SSL traffic is normally confidential.
     *
     * @param url ignored
     * @param proxyHost ignored
     * @param proxyPort ignored
     * @throws IOException always
     */

    protected HttpClient
    getProxiedClient (URL url, String proxyHost, int proxyPort)
    throws IOException {
        throw new IOException ("HTTPS proxying not supported");
    }


    /*
     * Initialize an HTTPS URLConnection ... could check that the URL
     * is an "https" URL, and that the handler is also an HTTPS one,
     * but that's established by other code in this package.
     */
    HttpsURLConnection (URL url, Handler handler) throws IOException {
        super (url, handler);
    }

    /**
     * Implements the HTTP protocol handler's "connect" method,
     * establishing an SSL connection to the server as necessary.
     */
    public void connect () throws IOException {
        if (connected) {
            return;
        }
        if ("https".equals(url.getProtocol())) {
            http = HttpsClient.New(url);
            if (nickName != null) {
                ((HttpsClient) http).setClientCertificate(nickName);
            }
            connected = true;
        } else {
            super.connect ();
        }
    }

    /**
     * Returns the cipher suite in use on this connection.
     */
    public String getCipherSuite () {
        return ((HttpsClient)http).getCipherSuite ();
    }
    /**
     * Returns the server's X.509 certificate chain, or null if
     * the server did not authenticate.
     */
    public X509Certificate [] getServerCertificateChain () {
        return ((HttpsClient)http).getServerCertificateChain ();
    }

    /**
     * Set client certificate nick name used for SSL handshake
     * @param name cert nick name
     */
    public void setClientCertificate(String name) {
        nickName = name;
    }
}
