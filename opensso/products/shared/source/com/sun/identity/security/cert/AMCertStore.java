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
 * $Id: AMCertStore.java,v 1.5 2009/01/28 05:35:12 ww203982 Exp $
 *
 */

/*
 * Created on Sep 11, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.sun.identity.security.cert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import javax.security.auth.x500.X500Principal;

import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPAttributeSet;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPEntry;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPSearchResults;

import com.iplanet.security.x509.X500Name;
import com.sun.identity.security.SecurityDebug;

/**
* The class is used to manage certificate store in LDAP server
* This class does get certificate with specified attr name and 
* value. This class should be used in order to manage certificate
* store in LDAP
**/

public class AMCertStore {
    protected AMLDAPCertStoreParameters storeParam = null;
    protected LDAPConnection ldapconn = null;
    protected X509Certificate certificate = null;
    protected static CertificateFactory cf = null;

    static final String amSecurity = "amSecurity";
    static com.sun.identity.shared.debug.Debug debug = SecurityDebug.debug;

    static {
        try {
           cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
           debug.error("AMCertStore : ", e);
        }
    }
    /**
     * Class AMCertStore is special cased Certificate store for LDAP.
     * A AMCertStore instance has to have all the information for ldap.
     *
     * @param param
     */
    public AMCertStore(AMLDAPCertStoreParameters param) {
            storeParam = param;
    }
    
    /**
     * Return ldap connection for ldap certificate store 
     *
     * @param NONE
     */
    synchronized LDAPConnection getConnection() {
        if (ldapconn != null) {
            return ldapconn;
        }
            
        /*
         * Setup the LDAP certificate directory service context for
         * use in verification of the users certificates.
         */
        if (storeParam.isSecure()) {
        	if (debug.messageEnabled()) {
                debug.message("AMCertStore.getConnection: " +
                    "initial ldc  using ssl.");
        	}
        	
            try {
                ldapconn = new LDAPConnection(
                    storeParam.getSecureSocketFactory());
            	if (debug.messageEnabled()) {
                    debug.message("AMCertStore.getConnection: " + 
                        "SSLSocketFactory called");
            	}
            } catch (Exception e) {
                debug.error("AMCertStore.getConnection: " + 
                	"JSSSocketFactory", e);
            }
        } else { // non-ssl
            ldapconn = new LDAPConnection();
        }

        try {
            ldapconn.connect(storeParam.getServerName(), storeParam.getPort());
            ldapconn.authenticate(storeParam.ldap_version, storeParam.getUser(),
                         storeParam.getPassword());
        } catch (LDAPException e) {
            debug.error("AMCertStore.getConnection: " + 
                "Exception in connection to LDAP server", e);
        }
        
        return ldapconn;
    }

    /**
     * Return matched ldap result from ldap certificate store 
     *
     * @param LDAPConnection ldc
     */
    LDAPSearchResults getSearchResults(LDAPConnection ldc) { 
        /*
         * Retrieve the DN of the signer of the certificate and
         * extract the CN information so we can search the LDAP
         * certficate directory.
         */
        LDAPSearchResults results = null;

        try {
            results = ldc.search(
                storeParam.getStartLoc(), LDAPConnection.SCOPE_SUB,
                storeParam.getSearchFilter(), null, false);
                                                 
            /*
             * The search based on the cn yielded no results
             * so return a status of verfication was false.
             */
                                                 
            if (results == null || results.hasMoreElements() == false) {
                debug.error("No ldap Entry found !");
                return null;
            }
        } catch (Exception e) {
            debug.error("AMCertStore.getSearchResults : " +
            	"Error in ldap search for " + storeParam.getSearchFilter());
            debug.error("AMCertStore.getSearchResults : ", e);
            return null;
        }

        return results;
    }

    /**
     * Return matched ldap entry from ldap certificate store 
     *
     * @param LDAPConnection ldc
     */
    LDAPEntry getLdapEntry(LDAPConnection ldc) { 
        /*
         * Retrieve the DN of the signer of the certificate and
         * extract the CN information so we can search the LDAP
         * certficate directory.
         */
        LDAPEntry ldapEntry = null;
                                                                 
        try {
            LDAPSearchResults results = getSearchResults(ldc);
            ldapEntry = results.next();
        }catch (Exception e) {
            debug.error("AMCertStore.getLdapEntry : " + 
                "Error in getting Cached CRL");
            return null;
        }
                
        return ldapEntry;
    }
    
    /**
     * Return matched certificate from ldap certificate store 
     *
     * @param cert
     */
    public X509Certificate getCertificate (X509Certificate cert) {
        X509Certificate c = getCertificate();
        if ((c != null) && c.equals(cert)) {
            return c;
        }
        
        return null;
    }
    
    /**
     * Return matched certificate from ldap certificate store 
     */
    public X509Certificate getCertificate () {
        /*
         * Lookup the certificate in the LDAP certificate
         * directory and compare the values.
         */ 
        LDAPConnection ldc = getConnection();
        
        try {
            LDAPSearchResults results = getSearchResults(ldc);

            while (results != null && results.hasMoreElements()) {
                // "Found search results for: " + cn , 2);
                LDAPEntry entry = results.next();
                LDAPAttributeSet attrSet = entry.getAttributeSet();
                
                /* 
                 * Retrieve the certificate from the store
                 */

                LDAPAttribute certAttribute = 
                                       attrSet.getAttribute("usercertificate");
                if (certAttribute == null) {
                    certAttribute = 
                                attrSet.getAttribute("usercertificate;binary");
                    if (certAttribute == null ) {
                        debug.message("AMCertStore.getCertificate : " +
                            "Certificate - get usercertificate is null ");
                        continue;
                    }
                }

                Enumeration allCert = certAttribute.getByteValues();
                while (allCert.hasMoreElements()) {
                    byte [] bytes = (byte []) allCert.nextElement();
                    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                    X509Certificate c = null;
                    try {
                        c = (X509Certificate) cf.generateCertificate(bis);
                    } catch (CertificateParsingException e) {
                        debug.error("AMCertStore.getCertificate : " +
                            "Error in Certificate parsing : ", e);
                    }
                    
                    if (c != null) {
                        return c;
                    }
                }  // inner while
            }  // outer while  
        } catch (Exception e) {
            debug.error("AMCertStore.getCertificate : " +
                "Certificate - Error finding registered certificate = ", e);
        } finally {
            try {
                ldc.disconnect();
            } catch (LDAPException e) {}
        }
        
        return null;
    }

    /**
     * Return value of certificate Issuer DN 
     *
     * @param certificate
     */
    static public X500Name getIssuerDN(X509Certificate certificate) 
                                                    throws IOException {
        X500Name dn = null;

        try {
            X500Principal issuerPrincipal =
                 certificate.getIssuerX500Principal();
            dn = new X500Name(issuerPrincipal.getEncoded());
        } catch (IOException e) {
            debug.error("AMCertStore.getIssuerDN : " +
                "Error in getting issuer DN : ", e);
        }
            
        return dn;
    }

    /**
     * Return value of certificate subject DN 
     *
     * @param certificate
     */
    static public X500Name getSubjectDN(X509Certificate certificate) 
                                                     throws IOException {
        X500Name dn = null;

        try {
            X500Principal subjectPrincipal =
                 certificate.getSubjectX500Principal();
            dn = new X500Name(subjectPrincipal.getEncoded());
        } catch (Exception e) {
            debug.error("AMCertStore.getSubjectDN : " +
                "Error in getting subject DN : " + e.toString());
        }
        return dn;
    }    
    
    /**
     * Return value of certificate subject DN 
     *
     * @param attrName
     * @param attrValue
     * @return searchFilter
     */
    public static String setSearchFilter(String attrName, String attrValue) {
        String searchFilter = new StringBuffer(128).append("(")
                    .append(attrName).append("=")
                    .append(attrValue).append(")").toString();
 
        if (debug.messageEnabled()) {
            debug.message("AMCertStore.setSearchFilter : " +
                "ldc.search: using this filter: " + searchFilter);
        }
                
        return searchFilter;
    }

    /**
     * Return ldapParam object has all config params 
     *
     * @param serverHost 
     * @param serverPort 
     * @param principleUser 
     * @param principlePasswd 
     * @param startSearchLoc 
     * @param uriParamsCRL 
     * @param isSSL 
     */
    public static AMLDAPCertStoreParameters setLdapStoreParam(
                  String serverHost, int serverPort,
                  String principleUser, String principlePasswd,
                  String startSearchLoc, String uriParamsCRL,
                  boolean isSSL) throws Exception {
        /*
         * Setup the LDAP certificate directory service context for
         * use in verification of the users certificates.
        */
        AMLDAPCertStoreParameters ldapParam = new AMLDAPCertStoreParameters
                            (serverHost, serverPort);
        AMLDAPCertStoreParameters.setLdapStoreParam(ldapParam,
                                    principleUser,
                                    principlePasswd,
                                    startSearchLoc,
                                    uriParamsCRL, 
                                    isSSL);
            
        return ldapParam;
    }

    /**
     * Return Issuer Certificate if the ldap entry has one
     *
     * @param ldapParam 
     * @param cert 
     * @param attrName 
     */
    public static X509Certificate getIssuerCertificate (
        AMLDAPCertStoreParameters ldapParam, 
        X509Certificate cert, String attrName) {
        String attrValue = null;
        
        try {
            // Retrieve attribute value of amAuthCert_chkAttrCRL
            X500Name dn = getIssuerDN(cert);
	    if (dn != null) {
                attrValue = dn.getAttributeValue(attrName);
	    }
        }
        catch (Exception ex) {
            if (debug.messageEnabled()) {
                debug.message("getIssuerCertificate - cn substring: " + ex); 
            }
            return null;
        }

        if (attrValue == null)
            return null;
        
        return getCertificate(ldapParam, attrName, attrValue);
    }
            
    /**
     * Return X509 Certificate if the ldap entry has the same one
     *
     * @param ldapParam 
     * @param cert 
     * @param attrName 
     */
    public static X509Certificate getRegisteredCertificate (
            AMLDAPCertStoreParameters ldapParam, 
            X509Certificate cert, String attrName) {
        String attrValue = null;
        X509Certificate c = null;
        
        try {
            // Retrieve attribute value of amAuthCert_chkAttrCertInLDAP
            X500Name dn = getSubjectDN(cert);
            if (dn != null) {
                attrValue = dn.getAttributeValue(attrName);
            }
        }
        catch (Exception ex) {
            if (debug.messageEnabled()) {
                debug.message("Certificate - cn substring: " + ex); 
            }
            return null;
        }

        if (attrValue == null)
            return null;
        
        if (debug.messageEnabled()) {
            debug.message("Certificate - cn substring: " + attrValue); 
        }

        c = getCertificate(ldapParam, attrName, attrValue);
        if ((c != null) && c.equals(cert)) {
            return c;
        } else {
            return null;
        }
    }
    
    /**
     * Return X509 Certificate if the ldap entry has one  
     *
     * @param ldapParam 
     * @param attrName 
     * @param attrValue 
     */
    public static  X509Certificate getCertificate (
        AMLDAPCertStoreParameters ldapParam, 
        String attrName, String attrValue) {
        X509Certificate ldapcert = null;
        if (attrValue == null)
            return null;
        
        /*
         * Lookup the certificate in the LDAP certificate
         * directory and compare the values.
         */ 

        try {
            String searchFilter = 
                    AMCertStore.setSearchFilter(attrName, attrValue);

            ldapParam.setSearchFilter(searchFilter);
            AMCertStore store = new AMCertStore(ldapParam);
            ldapcert = store.getCertificate();
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("Certificate - " +
                                "Error finding registered certificate = " , e);
            }
        }

        return ldapcert;
    }
    
    /**
     * Return true if it is self signed ROOT CA  
     *
     * @param cert 
     */
    public static boolean isRootCA(X509Certificate cert) {
        X500Name subjectDN = null;
        X500Name issuerDN = null;
        
        try {
            subjectDN = getIssuerDN(cert);
            issuerDN = getSubjectDN(cert);
        } catch (IOException e) {
            ;
        }
        return issuerDN.equals(subjectDN);
    }
}
