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
 * $Id: WSX509KeyManager.java,v 1.2 2008/06/25 05:47:24 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.soapbinding; 

import java.net.Socket;

import java.security.cert.X509Certificate;
import java.security.Principal;
import java.security.PrivateKey;

import javax.net.ssl.X509KeyManager;

/**
 * The <code>WSX509KeyManager</code> class implements JSSE X509KeyManager
 * interface. This implementation is the same as JSSE default implementation
 * exception it will supply user-specified client certificate alias when
 * client authentication is on.
 */
public class WSX509KeyManager implements X509KeyManager {

    private X509KeyManager defaultX509KM = null;
    private String  certAlias = null;


    /**
     * Constructor.
     *
     * @param defaultX509KeyManager a JSSE default implementation
     * @param certAlias the client certificate alias
     */
    public WSX509KeyManager(X509KeyManager defaultX509KeyManager,
            String certAlias) {
        defaultX509KM = defaultX509KeyManager;
        this.certAlias = certAlias;
    }

    /**
     * Choose an alias to authenticate the client side of a secure socket given
     * the public key type and the list of certificate issuer authorities
     * recognized by the peer (if any). If the certAlias specified in the
     * constructor is not null, it will be used.
     *
     * @param keyType the key algorithm type name
     * @param issuers the list of acceptable CA issuer subject names
     * @return the alias name for the desired key
     */
    public String chooseClientAlias(String[] keyType,
            Principal[] issuers,Socket socket) {
        if (certAlias != null && certAlias.length() > 0) {
            if (Utils.debug.messageEnabled()) {
                Utils.debug.message("WSX509KeyManager.chooseClientAlias: " +
                        "certAlias = " + certAlias);
            }
            return certAlias;
        }
        
        if (Utils.debug.messageEnabled()) {
            Utils.debug.message("WSX509KeyManager.chooseClientAlias: " +
                    "using default implementation");
        }
        return defaultX509KM.chooseClientAlias(keyType, issuers, socket);
    }

    /**
     * Returns an alias to authenticate the server side of a secure socket
     * given the public key type and the list of certificate issuer
     * authorities recognized by the peer (if any).
     *
     * @param keyType the key algorithm type name
     * @param issuers the list of acceptable CA issuer subject names
     * @return the alias name for the desired key
     */
    public String chooseServerAlias(String keyType,Principal[] issuers,
            Socket socket) {
        return defaultX509KM.chooseServerAlias(keyType, issuers, socket);
    }

    /**
     * Returns the matching aliases for authenticating the client  of a secure
     * socket given the public key type and the list of certificate issuer
     * authorities recognized by the peer (if any).
     *
     * @param keyType the key algorithm type name
     * @param issuers the list of acceptable CA issuer subject names
     * @return the matching alias names
     */
    public String[] getClientAliases(String keyType,Principal[] issuers) {
        return defaultX509KM.getClientAliases(keyType, issuers);
    }

    /**
     * Returns the matching aliases for authenticating the server  of a secure
     * socket given the public key type and the list of certificate issuer
     * authorities recognized by the peer (if any).
     *
     * @param keyType the key algorithm type name
     * @param issuers the list of acceptable CA issuer subject names
     * @return the matching alias names
     */
    public String[] getServerAliases(String keyType,Principal[] issuers) {
        return defaultX509KM.getServerAliases(keyType, issuers);
    }

    /**
     * Returns the certificate chain associated with the given alias.
     *
     * @param alias the alias name
     * @return the certificate chain (ordered with the user's certificate first
     *         and the root certificate authority last)
     */
    public X509Certificate[] getCertificateChain(String alias) {
        return defaultX509KM.getCertificateChain(alias);
    }
    
    /**
     * Returns the private key associated with the given alias.
     *
     * @return the private key associated with the given alias
     */
    public PrivateKey getPrivateKey(String alias) {
        return defaultX509KM.getPrivateKey(alias);
    }
}
