/*
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
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.security.cert;

import static org.forgerock.opendj.ldap.LDAPConnectionFactory.AUTHN_BIND_REQUEST;
import static org.forgerock.opendj.ldap.LDAPConnectionFactory.SSL_CONTEXT;

import com.iplanet.security.x509.CertUtils;
import com.sun.identity.security.SecurityDebug;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.SSLContextBuilder;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.SimpleBindRequest;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldap.responses.SearchResultReference;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.util.Options;

/**
* The class is used to manage certificate store in LDAP server
* This class does get certificate with specified attr name and 
* value. This class should be used in order to manage certificate
* store in LDAP
**/

public class AMCertStore {
    public static final String USERCERTIFICATE = "usercertificate";
    public static final String USERCERTIFICATE_BINARY = "usercertificate;binary";
    public static final String CACERTIFICATE = "cacertificate";
    public static final String CACERTIFICATE_BINARY = "cacertificate;binary";
    protected AMLDAPCertStoreParameters storeParam = null;
    protected ConnectionFactory ldapconn = null;
    protected X509Certificate certificate = null;
    protected static CertificateFactory cf = null;

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
     * Return ldap connection for ldap certificate store, or null if an error occured when connecting.
     */
    synchronized Connection getConnection() {
        if (ldapconn == null) {
            /*
             * Setup the LDAP certificate directory service context for
             * use in verification of the users certificates.
             */
            String serverName = storeParam.getServerName();
            int port = storeParam.getPort();
            LDAPConnectionFactory factory;

            // Regardless of SSL on connection, we will use authentication
            SimpleBindRequest authenticatedRequest = LDAPRequests.newSimpleBindRequest(
                    storeParam.getUser(), storeParam.getPassword().toCharArray());
            Options options = Options.defaultOptions()
                    .set(AUTHN_BIND_REQUEST, authenticatedRequest);

            if (storeParam.isSecure()) {
                debug.message("AMCertStore.getConnection: initial connection factory using ssl.");
                try {
                    options = options.set(SSL_CONTEXT, new SSLContextBuilder().getSSLContext());
                    ldapconn = new LDAPConnectionFactory(serverName, port, options);
                    debug.message("AMCertStore.getConnection: SSLSocketFactory called");
                } catch (GeneralSecurityException e) {
                    debug.error("AMCertStore.getConnection: Error getting SSL Context", e);
                    return null;
                }
            } else { // non-ssl
                ldapconn = new LDAPConnectionFactory(serverName, port, options);
            }
        }

        try {
            return ldapconn.getConnection();
        } catch (LdapException e) {
            debug.error("AMCertStore.getConnection: Exception in connection to LDAP server", e);
            return null;
        }
    }

    /**
     * Return matched ldap result from ldap certificate store, or null if either no results or an error occured.
     *
     * @param ldc The ldap connection
     */
    ConnectionEntryReader getSearchResults(Connection ldc, String... attributes) {
        /*
         * Retrieve the DN of the signer of the certificate and
         * extract the CN information so we can search the LDAP
         * certficate directory.
         */
        ConnectionEntryReader results = null;

        try {
            results = ldc.search(LDAPRequests.newSearchRequest(storeParam.getStartLoc(), SearchScope.SUBORDINATES,
                    storeParam.getSearchFilter(), attributes));
                                                 
            /*
             * The search based on the cn yielded no results
             * so return a status of verfication was false.
             */
                                                 
            if (!results.hasNext()) {
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
     * @param ldc The connection.
     */
    SearchResultEntry getLdapEntry(Connection ldc, String... attributes) {
        /*
         * Retrieve the DN of the signer of the certificate and
         * extract the CN information so we can search the LDAP
         * certficate directory.
         */
        try {
            return ldc.searchSingleEntry(LDAPRequests.newSingleEntrySearchRequest(storeParam.getStartLoc(),
                    SearchScope.SUBORDINATES, storeParam.getSearchFilter(), attributes));
        } catch (Exception e) {
            debug.error("AMCertStore.getLdapEntry : Error in getting Cached CRL");
            return null;
        }
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
        try (Connection ldc = getConnection()) {
            if (ldc == null) {
                return null;
            }
            ConnectionEntryReader results = getSearchResults(ldc, USERCERTIFICATE, USERCERTIFICATE_BINARY,
                    CACERTIFICATE, CACERTIFICATE_BINARY);

            while (results != null && results.hasNext()) {
                // "Found search results for: " + cn , 2);
                if (results.isEntry()) {
                    SearchResultEntry entry = results.readEntry();

                    /*
                     * Retrieve the certificate from the store
                     */

                    Attribute certAttribute = entry.getAttribute(USERCERTIFICATE);
                    if (certAttribute == null) {
                        certAttribute = entry.getAttribute(USERCERTIFICATE_BINARY);
                        if (certAttribute == null) {
                            // an end-entity certificate can be a CA certificate
                            certAttribute = entry.getAttribute(CACERTIFICATE);
                            if (certAttribute == null) {
                                certAttribute = entry.getAttribute(CACERTIFICATE_BINARY);
                            }
                            if (certAttribute == null) {
                                debug.message("AMCertStore.getCertificate: Certificate - get usercertificate is null ");
                                continue;
                            }
                        }
                    }

                    for (ByteString value : certAttribute) {
                        byte[] bytes = value.toByteArray();
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
                } else {
                    SearchResultReference reference = results.readReference();
                    debug.warning("Got an LDAP reference - only expected entries. Ignoring: {}",
                            reference);
                }
            }  // outer while  
        } catch (Exception e) {
            debug.error("AMCertStore.getCertificate : " +
                "Certificate - Error finding registered certificate = ", e);
        }
        
        return null;
    }

    /**
     * Return value of certificate Issuer DN.
     *
     * @param certificate
     * @return The Issuer's DN as String.
     */
    public static String getIssuerDN(X509Certificate certificate) {
        return CertUtils.getIssuerName(certificate);
    }

    /**
     * Return value of certificate subject DN.
     *
     * @param certificate
     * @return The Subject's DN as String.
     */
    public static String getSubjectDN(X509Certificate certificate) throws IOException {
        return CertUtils.getSubjectName(certificate);
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
        String attrValue = CertUtils.getAttributeValue(cert.getIssuerX500Principal(), attrName);

        if (attrValue == null) {
            return null;
        }

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
        String attrValue = CertUtils.getAttributeValue(cert.getSubjectX500Principal(), attrName);
        X509Certificate c = null;
        
        if (attrValue == null) {
            return null;
        }

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
        return CertUtils.getIssuerName(cert).equals(CertUtils.getSubjectName(cert));
    }
}
