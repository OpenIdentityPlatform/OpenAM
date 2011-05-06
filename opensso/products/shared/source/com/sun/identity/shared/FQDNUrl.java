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
 * $Id: FQDNUrl.java,v 1.4 2008/06/25 05:53:00 qcheng Exp $
 *
 */

package com.sun.identity.shared;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * URL with fully qualified domain name.
 */
public class FQDNUrl {
    private URL url;
    private boolean valid;
    private boolean fqdn;
   
    /**
     * Constructs a fully qualified domain name URL object.
     * 
     * @param strURL String representation of the URL.
     * @throws MalformedURLException if the URL is malformed or its
     *         host name is not fully qualified.
     */
    public FQDNUrl(String strURL) throws MalformedURLException {
        url = new URL(strURL);
        //java.net.URL is ok with http:/test.com - note missing /
        
        valid = strURL.startsWith("http://") || strURL.startsWith("https://");
        String host = url.getHost();
        fqdn = (host.indexOf(".") != -1);
    }

    /**
     * Returns <code>true</code> if URL is valid.
     * 
     * @return <code>true</code> if URL is valid.
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Returns <code>true</code> if host name is fully qualified.
     * 
     * @return <code>true</code> if host name is fully qualified.
     */
    public boolean isFullyQualified() {
        return fqdn;
    }
    
    /**
     * Returns URL object.
     * 
     * @return URL object.
     */
    public URL getURL() {
        return url;
    }
    
    /**
     * Returns deployment descriptor.
     * 
     * @return deployment descriptor.
     */
    public String getURI() {
        String uri = url.getPath();
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        int idx = uri.indexOf('/');
        if (idx != -1) {
            uri = uri.substring(0, idx);
        }
        
        return (uri.length() > 0) ? "/" + uri : uri;
    }

    /**
     * Returns protocol.
     * 
     * @return protocol.
     */
    public String getProtocol() {
        return url.getProtocol();
    }


    /**
     * Returns host.
     * 
     * @return host.
     */
    public String getHost() {
        return url.getHost();
    }
    
    /**
     * Returns port number.
     * 
     * @return port number.
     */
    public String getPort() {
        return Integer.toString(url.getPort());
    }
}
