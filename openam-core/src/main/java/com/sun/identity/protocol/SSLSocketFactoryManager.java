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
 * $Id: SSLSocketFactoryManager.java,v 1.3 2008/06/25 05:43:54 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock AS
 */
package com.sun.identity.protocol;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;


import com.sun.identity.security.keystore.AMX509KeyManager;
import com.sun.identity.security.keystore.AMX509KeyManagerFactory;
import com.sun.identity.security.keystore.AMX509TrustManager;
import com.sun.identity.security.SecurityDebug;

/**
 * Generate SSLContext can be used to create an SSL socket connection 
 * to ssl enabled server.
 * It is using the JSSE package. 
 */

public class SSLSocketFactoryManager {

    private static AMX509KeyManager amKeyMgr = null;
    private static SSLContext ctx = null;
	
    static {
        KeyManager[] keyMgr = null;
        String keyStore = System.getProperty("javax.net.ssl.keyStore", null);
    	try {
    	    if (keyStore != null) {
                amKeyMgr = AMX509KeyManagerFactory.createAMX509KeyManager();
        	    keyMgr = new KeyManager[] { amKeyMgr };
    	    } else {
                if (SecurityDebug.debug.messageEnabled()) {
                    SecurityDebug.debug.message("SSLSocketFactoryManager: no keyStore specified, amKeyMgr will be null");
                }
            }
    	    
            TrustManager[] amTrustMgr = new TrustManager[] { new AMX509TrustManager() };
	
    	    ctx = SSLContext.getInstance("SSL");
    	    ctx.init(keyMgr, amTrustMgr, null);
        } catch (Exception e) {
            SecurityDebug.debug.error("Exception in SSLSocketFactoryManager static initializer" + e.getMessage(), e);
        }
    }

    public static SSLSocketFactory getSocketFactory() {

        SSLSocketFactory sf = null;
	    if (ctx != null) {
            sf = ctx.getSocketFactory();
	    }
        return sf;
    }

    public static AMX509KeyManager getKeyStoreMgr() {
        return amKeyMgr;
    }
}
