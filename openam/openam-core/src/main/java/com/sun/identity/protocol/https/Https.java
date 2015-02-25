/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Https.java,v 1.3 2008/06/25 05:43:54 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock AS
 */
package com.sun.identity.protocol.https;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;


import com.sun.identity.security.keystore.AMX509KeyManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.protocol.SSLSocketFactoryManager;

public class Https {

    private static Debug debug = Debug.getInstance("amJSSE");
	
    static {
        try {
            SSLSocketFactory sf = SSLSocketFactoryManager.getSocketFactory();
            HttpsURLConnection.setDefaultSSLSocketFactory(sf);
            HttpsURLConnection.setDefaultHostnameVerifier(new AMHostnameVerifier());
        } catch (Exception e) {
            debug.error("Exception in Https static initializer " + e.getMessage(), e);
        }
    }

    public static void init() {
	    init(null);
    }
	
    public static void init(String alias) {

        if (alias != null && !alias.trim().isEmpty()) {
            AMX509KeyManager manager = SSLSocketFactoryManager.getKeyStoreMgr();
            if (manager != null) {
                manager.setAlias(alias);
            } else {
                if (debug.messageEnabled()) {
                    debug.message("Https.init: AMX509KeyManager was null when trying to set alias: " + alias);
                }
            }
        } else {
            if (debug.messageEnabled()) {
                debug.message("Https.init: Alias was null or empty");
            }
        }
    }
}

