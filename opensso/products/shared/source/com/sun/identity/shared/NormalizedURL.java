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
 * $Id: NormalizedURL.java,v 1.1 2008/08/13 17:37:11 veiming Exp $
 */

package com.sun.identity.shared;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Normalize URL.
 */
public class NormalizedURL {
    private NormalizedURL() {
    
    }
    /**
     * Returns a normalized URL object.
     * 
     * @param strURL String representation of the URL.
     */
    public static String normalize(String strURL) {
        URL url = null;
        
        try {
            url = new URL(strURL);
        } catch (MalformedURLException e) {
            return strURL; // cannot be normalized
        }
        
        String protocol = url.getProtocol();
        String host = url.getHost();
        String path = url.getPath();
        int port = url.getPort();
        String sPort;
        
        if (port == -1) {
            sPort = protocol.equals("https") ? "443" : "80";
        } else {
            sPort = Integer.toString(port);
        }
        
        return protocol + "://" + host + ":" + sPort + path;
    }
}
