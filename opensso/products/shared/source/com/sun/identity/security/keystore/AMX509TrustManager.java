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
 * $Id: AMX509TrustManager.java,v 1.3 2008/08/21 20:11:13 beomsuk Exp $
 *
 */

package com.sun.identity.security.keystore;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.Provider;
import java.security.Security;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.security.SecurityDebug;

/**
 * The <code>AMX509TrustManager</code> class implements JSSE X509TrustManager 
 * interface. This implementation is the same as JSSE default implementation
 * except it can manage user specified truststore.
 */
public class AMX509TrustManager implements X509TrustManager {
    static final String bundleName = "amSecurity";
    static final String javahome = System.getProperty("java.home");
    static final String seperator = System.getProperty("file.separator", "/");
    static StringBuffer defTrustStore = null; 
    static X509TrustManager sunX509TrustManager;
    static TrustManagerFactory tmf = null;
    static String trustStore = null;
    static String trustStoreType = null;
    static KeyStore trustKeyStore = null;
    static boolean trustAllServerCerts = false;
    
    static {
        try {
            // Construct dir name for default trust store 
            // javahome + seperator + "lib" + seperator + "security" + 
            // seperator + "cacerts";
            defTrustStore = new StringBuffer();
            defTrustStore.append(javahome);
            defTrustStore.append(seperator);
            defTrustStore.append("lib");
            defTrustStore.append(seperator);
            defTrustStore.append("security");
            defTrustStore.append(seperator);
            defTrustStore.append("cacerts");
 
            trustStoreType = System.getProperty("javax.net.ssl.trustStoreType",
                KeyStore.getDefaultType());
            trustStore = System.getProperty("javax.net.ssl.trustStore",
                defTrustStore.toString());
            trustAllServerCerts = Boolean.valueOf(SystemPropertiesManager.get(
                "com.iplanet.am.jssproxy.trustAllServerCerts", "false"))
                    .booleanValue();

            trustKeyStore = KeyStore.getInstance(trustStoreType);
            FileInputStream fis = new FileInputStream(trustStore);
            trustKeyStore.load(fis, null);
                    
            Provider sProviders[] = Security.getProviders();
            String provider = null;
            String algorithm = null;
            for (int i = 0; i < sProviders.length; i++) {
                if (sProviders[i].getName().equalsIgnoreCase("IBMJSSE2")) {
                    provider = "IBMJSSE2";
                    algorithm = "IbmX509";
                }
            }

            if (provider == null) {
                provider = "SunJSSE";
                algorithm = "SunX509";
            }
            
            tmf = TrustManagerFactory.getInstance(algorithm,  provider);
        
            tmf.init(trustKeyStore);
            sunX509TrustManager =
                 (X509TrustManager)tmf.getTrustManagers()[0];
        } catch (Exception e) {
            SecurityDebug.debug.error(e.toString());
        }
    }
    

    /** create sunX509KeyManager
     * 
     * for example:
     *           Create/load a truststore
     *           Get instance of a "SunX509" TrustManagerFactory "tmf"
     *           init the TrustManagerFactory with the truststore
     */
    public AMX509TrustManager() {
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) 
                                                 throws CertificateException {
        if (trustAllServerCerts) {
            return;
        }

               sunX509TrustManager.checkServerTrusted(chain, authType);
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) 
                                                 throws CertificateException {
               sunX509TrustManager.checkClientTrusted(chain, authType);
    }

    public X509Certificate[] getAcceptedIssuers() {
            X509Certificate[] certs = null;
            
            if (sunX509TrustManager != null) {
                certs = sunX509TrustManager.getAcceptedIssuers();
            }
            
            return certs;
    }    

    public KeyStore getKeyStore() {
        return trustKeyStore;
    }    
}
        
