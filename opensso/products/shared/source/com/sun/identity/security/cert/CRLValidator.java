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
 * $Id: CRLValidator.java,v 1.3 2008/06/25 05:52:58 qcheng Exp $
 *
 */

package com.sun.identity.security.cert;

import java.io.IOException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Vector;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.AMPasswordUtil;
import com.iplanet.security.x509.X500Name;
import com.sun.identity.security.SecurityDebug;

/**
 * This interface is for <code>CRLValidator</code> that is representing
 * configued <code>X509CRLValidator</code> 
 */
public class CRLValidator {
    private static Debug debug = SecurityDebug.debug;
    private static AMLDAPCertStoreParameters ldapParams = null;
    // Dir server info for CRL entry
    private static boolean crlCheckEnabled = false;
    private static String dirServerHost = null;
    private static String dirServerPort = null;
    private static String dirUseSSL = null;
    private static String dirPrincipleUser = null;
    private static String dirPrinciplePasswd = null;
    private static String dirStartSearchLoc = null;
    private static String crlSearchAttr = null;

    static {
       /*
         * Setup the LDAP certificate directory service context for
         * use in verification of signing certificates.
         */
        dirServerHost = SystemPropertiesManager.get(
                        Constants.CRL_CACHE_DIR_HOST, null);
        crlCheckEnabled = dirServerHost != null;
        if (debug.messageEnabled()) {
            debug.message("CRLValidator : " + 
                "CRL Check configured : " + crlCheckEnabled);
        }

        if (crlCheckEnabled == true) {
            dirServerHost = SystemPropertiesManager.get(
                        Constants.CRL_CACHE_DIR_HOST, null);
            dirServerPort = SystemPropertiesManager.get(
                        Constants.CRL_CACHE_DIR_PORT, "389");
            dirUseSSL = SystemPropertiesManager.get(
                        Constants.CRL_CACHE_DIR_SSL_ENABLED, "false");
            dirPrincipleUser = SystemPropertiesManager.get(
                        Constants.CRL_CACHE_DIR_USER, null);
            dirPrinciplePasswd = AMPasswordUtil.decrypt(
                        SystemPropertiesManager.get(
                            Constants.CRL_CACHE_DIR_PASSWD, null));
            dirStartSearchLoc = SystemPropertiesManager.get(
                        Constants.CRL_CACHE_DIR_SEARCH_LOC, null);
            crlSearchAttr = SystemPropertiesManager.get(
                        Constants.CRL_CACHE_DIR_SEARCH_ATTR, "CN");

            try {
                ldapParams = AMCertStore.setLdapStoreParam(dirServerHost,
                       Integer.valueOf(dirServerPort).intValue(),
                       dirPrincipleUser,
                       dirPrinciplePasswd,
                       dirStartSearchLoc,
                       null,
                       dirUseSSL.equalsIgnoreCase("true"));
            } catch (Exception e) {
                debug.error("Unable to configure ldap CRL cache " + e);
            }

            if (debug.messageEnabled()) {
                debug.message("CRLValidator : Directory Server Host : " 
                    + dirServerHost);
                debug.message("CRLValidator : Directory Server Port# : " 
                    + dirServerPort);
                debug.message("CRLValidator : SSL Enabled : " + dirUseSSL);
                debug.message("CRLValidator : Principal User : " 
                    + dirPrincipleUser);
                if (dirPrinciplePasswd != null) {
                    debug.message("CRLValidator : User Password : xxxxxx");
                } else {
                    debug.message("CRLValidator : User Password : null");
                }
                debug.message("CRLValidator : Start Search Loc : " 
                    + dirStartSearchLoc);
                debug.message("CRLValidator : CRL Search Attr : " 
                    + crlSearchAttr);
            }
        }
    }
    
    /**
     * Validate certificate against configured crl
     * @param cert cert to be validated 
     * @return true if certificate is not in crl
     */
    static public boolean validateCertificate(X509Certificate cert, 
                                       boolean checkCAStatus) {
        String method = "validateCertificate : ";
        boolean certgood = true;

    	try {
            Vector crls = new Vector();
            X509CRL crl = 
               AMCRLStore.getCRL(ldapParams, cert, crlSearchAttr);

            if (crl != null) {
                crls.add(crl);
            }

            if (debug.messageEnabled()) {
                debug.message(method + " crls size = " + crls.size());
                if (crls.size() > 0) {
                    debug.message(method + "CRL = " + crls.toString());
                } else {
                    debug.message(method + "NO CRL found.");
                }
            }

            AMCertPath certpath = new AMCertPath(crls);
            X509Certificate certs[] = { cert }; 
            if (!certpath.verify(certs, true, false)) {
                debug.error(method + "CertPath:verify failed.");
                return certgood = false;
            }
    	} catch (Exception e) {
            debug.error(method + "verify failed.", e);
            return certgood = false;
    	}

        if ((checkCAStatus == true) && (AMCertStore.isRootCA(cert) == false)) {
            X509Certificate caCert = 
                AMCertStore.getIssuerCertificate(
                    ldapParams, cert, crlSearchAttr);
            certgood = validateCertificate(caCert, checkCAStatus);
        }

        return certgood;
    }
    
    /**
     * Get certificate revocation list from cofigured ldap store
     * @param cert cert to be validated 
     * @return crl if ldap store configured with crl
     */
    static public X509CRL getCRL(X509Certificate cert) {
        X509CRL crl = null;
            /*
         * Get the CN of the input certificate
         */
        String attrValue = null;
        
        try {
            X500Name dn = AMCRLStore.getIssuerDN(cert);
            // Retrieve attribute value of crlSearchAttr
            if (dn != null) {
                attrValue = dn.getAttributeValue(crlSearchAttr);
            }
        } catch (Exception ex) {
            debug.error("attrValue to search crl : " + attrValue, ex); 
            return null;
        }

        if ((attrValue == null) || (ldapParams == null))
            return null;
        
        if (debug.messageEnabled()) {
            debug.message("CRLValidator - " +
                              "attrValue to search crl : " + attrValue); 
        }

        /*
         * Lookup the certificate in the LDAP certificate
         * directory and compare the values.
         */ 
        String searchFilter = 
            AMCRLStore.setSearchFilter(crlSearchAttr, attrValue);
        ldapParams.setSearchFilter(searchFilter);
        try {
            AMCRLStore store = new AMCRLStore(ldapParams);
            crl = store.getCRL(cert);
        } catch (IOException e) {
            debug.error("X509Certificate: verifyCertificate." + e.toString());
        }
                
        return crl; 
    }
    
    static public boolean isCRLCheckEnabled() {
        return crlCheckEnabled;
    }
}
