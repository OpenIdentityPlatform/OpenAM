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
 * $Id: CRLValidator.java,v 1.3 2008/06/25 05:52:58 qcheng Exp $
 *
 * Portions Copyrighted 2014-2015 ForgeRock AS.
 * Portions Copyrighted 2026 3A Systems LLC.
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
import com.iplanet.security.x509.CertUtils;
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
        // Fail closed on a certificate that could not be resolved, which is what the CA-status
        // recursion below passes when the issuer has no usable entry in the CRL directory. The
        // check has to come first: AMCertStore.isRootCA() dereferences the null that
        // CertUtils.getIssuerName() returns for it, and that line is above the try, so the
        // catch-all would not see the NullPointerException — it would leave this method through
        // SAML2Utils.validateCertificate, which does not catch one either.
        if (cert == null) {
            debug.error(method + "no certificate to validate.");
            return false;
        }
        boolean certgood = true;
        boolean isRootCA = AMCertStore.isRootCA(cert);
        // The issuer certificate, used twice: as the chain that lets a fetched CRL be verified
        // and therefore stored, and as the certificate the CA-status check below recurses into.
        // Without a chain, AMCRLStore.findIssuerPublicKeys() sees only the trust store, which for
        // the usual shape — an end-entity under an intermediate that is not itself an anchor —
        // holds no key for the issuer; the CRL is then never stored and is refetched from the
        // distribution point on every single validation.
        //
        // Resolved at most once, and not at all unless something asks for it. A CRL still fresh
        // in the store or the cache is fetched from no distribution point and so needs no chain,
        // which is the common case; this runs on the unauthenticated SAML2 signature path, and
        // one whole-subtree LDAP search per signed message for a value nothing reads is not worth
        // paying. com.sun.identity.saml2.ca.crl.check defaults to false, so the CA-status branch
        // below does not read it either.
        IssuerCertificate caCert = new IssuerCertificate(cert, isRootCA);

    	try {
            if (!new AMCertPath(null).verify(new X509Certificate[]{cert}, false, false)) {
                debug.error(method + "trust chain verify failed, rejecting before CRL fetch.");
                return certgood = false;
            }
            Vector crls = new Vector();
            X509CRL crl = AMCRLStore.getCRLWithLazyChain(ldapParams, cert,
                    () -> chainOf(cert, caCert.get()), crlSearchAttr);

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

        if ((checkCAStatus == true) && (isRootCA == false)) {
            X509Certificate issuer = caCert.get();
            if (issuer != null) {
                certgood = validateCertificate(issuer, checkCAStatus);
            } else if (!crlDirectoryConfigured()) {
                // The misconfiguration, not the missing entry: the CRL directory is the only
                // place this class can resolve an issuer certificate, and nothing configured it.
                // Every non-root certificate fails for the same reason until it is, so say which
                // property is missing rather than name an issuer that was never looked up.
                debug.error(method + "com.sun.identity.saml2.ca.crl.check is on but "
                        + Constants.CRL_CACHE_DIR_HOST + " is not configured, so no issuer "
                        + "certificate can be resolved and no CA's revocation status can be "
                        + "checked.");
                certgood = false;
            } else {
                // Rejected here rather than by recursing into the null check above, which cannot
                // say which issuer went unresolved: it is absent from the CRL directory, or its
                // entry carries no certificate attribute, or the lookup itself failed.
                debug.error(method + "no issuer certificate for "
                        + CertUtils.getIssuerName(cert)
                        + "; the CA's revocation status cannot be checked.");
                certgood = false;
            }
        }

        return certgood;
    }

    /**
     * Whether there is a CRL directory to resolve an issuer certificate in. Both fields are set
     * together, and only when {@code com.sun.identity.crl.cache.directory.host} is configured —
     * which {@code com.sun.identity.saml2.crl.check} and {@code .ca.crl.check} do not imply.
     */
    private static boolean crlDirectoryConfigured() {
        return ldapParams != null && crlSearchAttr != null;
    }

    /**
     * The issuer certificate of the certificate under validation, looked up in the CRL directory
     * on first use and then remembered — including a null answer, which costs one search and not
     * one per reader.
     */
    private static final class IssuerCertificate {

        private final X509Certificate cert;
        private final boolean isRootCA;
        private boolean resolved;
        private X509Certificate issuer;

        IssuerCertificate(X509Certificate cert, boolean isRootCA) {
            this.cert = cert;
            this.isRootCA = isRootCA;
        }

        /** @return null for a root CA, when no CRL directory is configured, or when the lookup
         *      found nothing. */
        X509Certificate get() {
            if (!resolved) {
                resolved = true;
                if (!isRootCA && crlDirectoryConfigured()) {
                    try {
                        issuer = AMCertStore.getIssuerCertificate(ldapParams, cert, crlSearchAttr);
                    } catch (RuntimeException e) {
                        // Read from inside AMCRLStore, which would swallow this, and from the
                        // CA-status branch, which sits outside validateCertificate's own catch —
                        // and SAML2Utils.validateCertificate does not catch one either.
                        debug.error("CRLValidator: cannot resolve the issuer certificate of "
                                + CertUtils.getIssuerName(cert), e);
                    }
                }
            }
            return issuer;
        }
    }

    /**
     * The chain to hand {@code AMCRLStore}: the certificate and, when one could be resolved, the
     * certificate of its issuer.
     *
     * <p>Only the issuer's key is of use there — it is what a CRL for {@code cert} would have
     * been signed with — and the store still traces it back to a configured trust anchor itself
     * before letting it authorise anything into the shared CRL stores.
     */
    private static X509Certificate[] chainOf(X509Certificate cert, X509Certificate caCert) {
        return (caCert == null) ? new X509Certificate[]{cert}
                : new X509Certificate[]{cert, caCert};
    }

    /**
     * Get certificate revocation list from cofigured ldap store
     * @param cert cert to be validated
     * @return crl if ldap store configured with crl
     * @deprecated Has no callers. Use {@link #validateCertificate(X509Certificate, boolean)}
     *             instead; a CRL Distribution Point fetch must never run for a certificate
     *             that has not passed the trust-chain check.
     */
    @Deprecated
    static public X509CRL getCRL(X509Certificate cert) {
        X509CRL crl = null;
        // Same SSRF guard as validateCertificate(): the CRL DP fetch inside
        // AMCRLStore.getCRL() follows a URL taken from the certificate itself.
        try {
            if (!new AMCertPath(null).verify(new X509Certificate[]{cert}, false, false)) {
                debug.error("CRLValidator.getCRL: trust chain verify failed, rejecting before CRL fetch.");
                return null;
            }
        } catch (Exception e) {
            debug.error("CRLValidator.getCRL: trust chain verify failed.", e);
            return null;
        }
        //Get the CN of the input certificate
        String attrValue = CertUtils.getAttributeValue(cert.getIssuerX500Principal(), crlSearchAttr);

        if (attrValue == null || ldapParams == null) {
            return null;
        }

        if (debug.messageEnabled()) {
            debug.message("CRLValidator - attrValue to search crl : " + attrValue);
        }

        //Lookup the certificate in the LDAP certificate directory and compare the values.
        // On a copy: ldapParams is shared by every thread validating a certificate, and a filter
        // written into it would point whichever search reads it next at this entry.
        AMLDAPCertStoreParameters storeParam = new AMLDAPCertStoreParameters(ldapParams);
        storeParam.setSearchFilter(AMCRLStore.setSearchFilter(crlSearchAttr, attrValue));
        try {
            AMCRLStore store = new AMCRLStore(storeParam);
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
