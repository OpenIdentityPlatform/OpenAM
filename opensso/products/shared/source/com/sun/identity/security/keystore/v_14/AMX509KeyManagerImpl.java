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
 * $Id: AMX509KeyManagerImpl.java,v 1.3 2008/08/21 20:11:14 beomsuk Exp $
 *
 */

package com.sun.identity.security.keystore.v_14;

import java.io.FileInputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ResourceBundle;
import java.util.Locale;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;

import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.security.SecurityDebug;
import com.sun.identity.security.keystore.AMX509KeyManager;

/**
 * The <code>AMX509KeyManagerImpl</code> class implements JSSE X509KeyManager
 * interface. This implementation is the same as JSSE default implementation
 * exception it will supply user-specified client certificate alias when
 * client authentication is on.
 */
public class AMX509KeyManagerImpl implements AMX509KeyManager {

    static final String bundleName = "amSecurity";
    static ResourceBundle bundle = null;
    static AMResourceBundleCache amCache = AMResourceBundleCache.getInstance(); 
    public static Debug debug = SecurityDebug.debug;

    static String keyStoreFile = 
                     System.getProperty("javax.net.ssl.keyStore", null);
    static String keyStorePassword = 
                     System.getProperty("javax.net.ssl.keyStorePassword", "");

    String certAlias = null;
    X509KeyManager sunX509KeyManager = null;
    KeyStore keyStore = null;
    KeyManagerFactory kmf = null;
    static String provider = null;
    static String algorithm = null;
    
    static {
        Provider sProviders[] = Security.getProviders();
        for (int i = 0; i < sProviders.length; i++) {
            if (sProviders[i].getName().equalsIgnoreCase("IBMJSSE")) {
                provider = "IBMJSSE";
                algorithm = "IbmX509";
            }
        }

        if (provider == null) {
            provider = "SunJSSE";
            algorithm = "SunX509";
        }
    }

    // create sunX509KeyManager
    //
    // for example:
    //     Create/load a keystore
    //     Get instance of a "SunX509" KeyManagerFactory "kmf"
    //     init the KeyManagerFactory with the keystore
    public AMX509KeyManagerImpl() {
        // initialize KeyStore and get KeyManagerFactory 
        try {
            bundle = amCache.getResBundle(bundleName, Locale.getDefault());

            KeyStore ks = KeyStore.getInstance("JKS");
            FileInputStream fis = new FileInputStream(keyStoreFile);
            ks.load(fis, keyStorePassword.toCharArray());
    		
            kmf = KeyManagerFactory.getInstance(algorithm, provider);
            kmf.init(ks, keyStorePassword.toCharArray());
        } catch (Exception e) {
            debug.error("AMX509KeyManager.AMX509KeyManager:", e);
        }

        // com.sun.net.ssl.internal.ssl.X509KeyManagerImpl
        sunX509KeyManager = (X509KeyManager) kmf.getKeyManagers()[0];
    }

    /**
     * This constructor takes a JSSE default implementation and a
     * user-specified client certificate alias.
     * @param alias certificate alias
     */
    public void setAlias(String alias) {
        certAlias = alias;
    }

   /** 
    * Choose an alias to authenticate the client side of a secure socket given
    * the public key type and the list of certificate issuer authorities
    * recognized by the peer (if any). If the certAlias specified in the
    * constructor is not null, it will be used.
    * @param keyType the key algorithm type name
    * @param issuers the list of acceptable CA issuer subject names
    * @return the alias name for the desired key
    */
    public String chooseClientAlias(String[] keyType, Principal[] issuers, 
           Socket sock) {

        if (debug.messageEnabled()) {
            debug.message("AMX509KeyManagerImpl.chooseClientAlias: " +
                          "certAlias = " + certAlias);
        }

        if (certAlias != null && certAlias.length() > 0) {
            return certAlias;
        }

        return sunX509KeyManager.chooseClientAlias(keyType, issuers, sock);
    }

   /**
    * Choose an alias to authenticate the server side of a secure socket
    * given the public key type and the list of certificate issuer
    * authorities recognized by the peer (if any).
    * @param keyType the key algorithm type name
    * @param issuers the list of acceptable CA issuer subject names
    * @return the alias name for the desired key
    */
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket sock)
    {
        return sunX509KeyManager.chooseServerAlias(keyType, issuers, sock);
    }

   /**
    * Get the matching aliases for authenticating the client side of a secure
    * socket given the public key type and the list of certificate issuer
    * authorities recognized by the peer (if any).
    * @param keyType the key algorithm type name
    * @param issuers the list of acceptable CA issuer subject names
    * @return the matching alias names
    */
    public String[] getClientAliases(String keyType, Principal[] issuers)
    {
        return sunX509KeyManager.getClientAliases(keyType, issuers);
    }

   /**
    * Get the matching aliases for authenticating the server side of a secure
    * socket given the public key type and the list of certificate issuer
    * authorities recognized by the peer (if any).
    * @param keyType the key algorithm type name
    * @param issuers the list of acceptable CA issuer subject names
    * @return the matching alias names
    */
    public String[]
    getServerAliases(String keyType, Principal[] issuers)
    {
        return sunX509KeyManager.getServerAliases(keyType, issuers);
    }

   /**
    * Returns the certificate chain associated with the given alias.
    * @param alias the alias name
    * @return the certificate chain (ordered with the user's certificate first
    *         and the root certificate authority last)
    */
    public X509Certificate[]  getCertificateChain(String alias)
    {
        return sunX509KeyManager.getCertificateChain(alias);
    }

   /**
    * Returns the private key associated with the given alias.
    * @return the private key associated with the given alias
    */
    public PrivateKey getPrivateKey(String alias)
    {
        return sunX509KeyManager.getPrivateKey(alias);
    }
}
