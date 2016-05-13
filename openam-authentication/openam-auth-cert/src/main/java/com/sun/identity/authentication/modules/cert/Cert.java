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
 * $Id: Cert.java,v 1.14 2009/03/13 20:54:42 beomsuk Exp $
 *
 * Portions Copyrighted 2013-2016 ForgeRock AS.
 */

package com.sun.identity.authentication.modules.cert;

import com.iplanet.security.x509.CertUtils;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.X509CertificateCallback;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.security.cert.AMCRLStore;
import com.sun.identity.security.cert.AMCertPath;
import com.sun.identity.security.cert.AMCertStore;
import com.sun.identity.security.cert.AMLDAPCertStoreParameters;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.encode.Base64;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.GeneralNames;
import sun.security.x509.OtherName;
import sun.security.x509.RFC822Name;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.opendj.ldap.LDAPUrl;

public class Cert extends AMLoginModule {

    private static java.util.Locale locale = null;
    private ResourceBundle bundle = null;

    private String userTokenId = null;
    private X509Certificate thecert = null;

    // from profile server.
    // default: MUST HAVE where is the ldap server.
    private String amAuthCert_serverHost;  
    // default: values stored in auth.certificate.ldap.server.context; 
    // think ok to be nil.
    private String amAuthCert_startSearchLoc;  
    // none, simple or CRAM-MD5 (default to NONE)
    private String amAuthCert_securityType; 
    // ldap user name [if missing default to amAuthCert_securityType to none.]
    private String amAuthCert_principleUser;  
    // ldap user's passwd  
    // [if missing default to amAuthCert_securityType to none.]
    private String amAuthCert_principlePasswd;  
    // use ssl to talk to ldap. default is false.
    private String amAuthCert_useSSL;        
    // Field in Cert to user to access user profile.  default to DN
    private String amAuthCert_userProfileMapper;    
    // Alternate Field in Cert to userid to access user profile 
    // if above is "other"
    private String amAuthCert_altUserProfileMapper;
    // SubjectAltNameExtension Value Type OID 
    // This OID type of value is retrieved and used to access user profile
    private String amAuthCert_subjectAltExtMapper;
    // check user cert against revoke list in LDAP.
    private String amAuthCert_chkCRL;        
    // check CA cert against revoke list in LDAP.
    private String amAuthCert_validateCA;        
    // attr to use in search for user cert in CRL in LDAP
    private String amAuthCert_chkAttrCRL = null;
    // attributes to use in searchfilter to find crlDistributionPoint entry in LDAP
    // content of searchfilter is described in AMCRLStore to avoid duplication
    private String[] amAuthCert_chkAttributesCRL = null;
    // params to use in accessing CRL DP
    private String amAuthCert_uriParamsCRL = null;
    // check user cert with cert in LDAP.
    private String amAuthCert_chkCertInLDAP; 
    // attr to use in search for user cert in LDAP
    private String amAuthCert_chkAttrCertInLDAP = null;
    // this is what appears in the user selectable choice field.
    private String amAuthCert_emailAddrTag; 
    private int amAuthCert_serverPort =389;
    private boolean portal_gw_cert_auth_enabled = false;
    private boolean portal_gw_cert_preferred = false;
    private Set portalGateways = null;
    // HTTP Header name to have clien certificate in servlet request.
    private String certParamName = null;
    private boolean ocspEnabled = false;
    private boolean crlEnabled = false;
    private AMLDAPCertStoreParameters ldapParam = null;

    // configurations
    private Map options;
    private CertAuthPrincipal userPrincipal;
    private CallbackHandler callbackHandler;
    static final int ldap_version = 3;

    private static final String amAuthCert = "amAuthCert";
    
    private static com.sun.identity.shared.debug.Debug debug = null;

    static String UPNOID = "1.3.6.1.4.1.311.20.2.3";

    private String amAuthCert_cacheCRL;
    private boolean doCRLCaching = true;
    
    //attribute and flag to check whether CRLs should be updated from CRL distribution point
    private String amAuthCert_updateCRL;
    private boolean doCRLUpdate = true;
    

    /**
     * Default module constructor does nothing
     */
    public Cert() {
    }

    /**
     * Initialize module 
     * @param subject for auth
     * @param sharedState with auth framework
     * @param options for auth
     */
    public void init(Subject subject, Map sharedState, Map options) {
        if (debug == null) {
            debug = com.sun.identity.shared.debug.Debug.getInstance(amAuthCert);
        }
        java.util.Locale locale = getLoginLocale();
        bundle = amCache.getResBundle(amAuthCert, locale);

        this.callbackHandler = getCallbackHandler();
        this.options = options;
        if (debug.messageEnabled()) {
            debug.message("Cert Auth resbundle locale="+locale);
            debug.message("Cert auth init() done");
        }
    } 

    private void initAuthConfig() throws AuthLoginException {
        if (options != null) {
            debug.message("Certificate: getting attributes."); 
            // init auth level
            String authLevel = CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-cert-auth-level");
            if (authLevel != null) {
                try {
                    int tmp = Integer.parseInt(authLevel);
                    setAuthLevel(tmp);
                } catch (Exception e) {
                    // invalid auth level
                    debug.error("Invalid auth level " + authLevel, e);
                }
            } 
            // will need access control to ldap server; passwd and user name
            // will also need to yank out the user profile based on cn or dn
            //  out of "profile server"
            amAuthCert_securityType = CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-cert-security-type");
            amAuthCert_principleUser = CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-cert-principal-user");
               amAuthCert_principlePasswd = CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-cert-principal-passwd");
            amAuthCert_useSSL = CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-cert-use-ssl");
            amAuthCert_userProfileMapper = CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-cert-user-profile-mapper"); 
            amAuthCert_altUserProfileMapper = CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-cert-user-profile-mapper-other");
            amAuthCert_subjectAltExtMapper = CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-cert-user-profile-mapper-ext");
            amAuthCert_chkCRL = CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-cert-check-crl"); 
            if (amAuthCert_chkCRL.equalsIgnoreCase("true")) {
                amAuthCert_chkAttrCRL = CollectionHelper.getMapAttr(
                    options, "iplanet-am-auth-cert-attr-check-crl");
                if (amAuthCert_chkAttrCRL == null || 
                    amAuthCert_chkAttrCRL.equals("")) {
                    throw new AuthLoginException(amAuthCert, "noCRLAttr", null);
                } else {
                    amAuthCert_chkAttributesCRL = trimItems(amAuthCert_chkAttrCRL.split(","));
                }
                amAuthCert_cacheCRL = CollectionHelper.getMapAttr(
                        options, "openam-am-auth-cert-attr-cache-crl","true");
                if (amAuthCert_cacheCRL.equalsIgnoreCase("false")) {
                    doCRLCaching = false;
                }
                amAuthCert_updateCRL = CollectionHelper.getMapAttr(
                        options, "openam-am-auth-cert-update-crl", "true");
                if (amAuthCert_updateCRL.equalsIgnoreCase("false")) {
                    doCRLUpdate = false;
                }               
                
                crlEnabled = true;
            }
            amAuthCert_validateCA = CollectionHelper.getMapAttr(
                options, "sunAMValidateCACert"); 

            amAuthCert_uriParamsCRL = CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-cert-param-get-crl");
            amAuthCert_chkCertInLDAP = CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-cert-check-cert-in-ldap"); 
            if (amAuthCert_chkCertInLDAP.equalsIgnoreCase("true")) {
                amAuthCert_chkAttrCertInLDAP = CollectionHelper.getMapAttr(
                    options, "iplanet-am-auth-cert-attr-check-ldap");
                if (amAuthCert_chkAttrCertInLDAP == null ||
                    amAuthCert_chkAttrCertInLDAP.equals("")) {
                    throw new AuthLoginException(
                        amAuthCert, "noLDAPAttr", null);
                }
            }
            String ocspChk = CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-cert-check-ocsp"); 
            ocspEnabled = (ocspChk != null && ocspChk.equalsIgnoreCase("true"));

             //
            //  portal-style gateway cert auth enabled if
            //  explicitly specified in cert service template.
            //  "none", empty list, or null means disabled;
            //  "any" or non-empty list means enabled.  also check
            //  non-empty list for remote client's addr.
            //
            String gwCertAuth = CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-cert-gw-cert-auth-enabled");
            certParamName = CollectionHelper.getMapAttr(
                options,"sunAMHttpParamName");

            String client = getLoginState("process").getClient();
            portal_gw_cert_auth_enabled = false;
            if (gwCertAuth == null || gwCertAuth.equals("") 
                                || gwCertAuth.equalsIgnoreCase("none")) {
                if (debug.messageEnabled()) {
                    debug.message("iplanet-am-auth-cert-gw-cert-auth-enabled = "
                        + gwCertAuth);
                }
            } else if (gwCertAuth.equalsIgnoreCase("any")) {
                portal_gw_cert_auth_enabled = true;
            } else {
                portalGateways = 
                  (Set)options.get("iplanet-am-auth-cert-gw-cert-auth-enabled");
                if ((client !=null) && (portalGateways.contains(client))) {
                    portal_gw_cert_auth_enabled = true;
                } else {
                    if (debug.messageEnabled()) {
                        debug.message("gateway list does not contain client");
                        Iterator clientIter = portalGateways.iterator();
                        while (clientIter.hasNext()) {
                            String clientStr = (String)clientIter.next();
                            debug.message("client list entry = " + clientStr);
                        }
                    }
                 }
            }

            // Switch to decide if provided certs in portal gateway (header)
            // is preferred over certs provided from servlet request
            portal_gw_cert_preferred = Boolean.valueOf(CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-cert-gw-cert-preferred"));

            amAuthCert_emailAddrTag = bundle.getString("emailAddrTag");

            amAuthCert_serverHost = CollectionHelper.getServerMapAttr(
                options, "iplanet-am-auth-cert-ldap-provider-url");
            if (amAuthCert_serverHost == null 
                && (amAuthCert_chkCertInLDAP.equalsIgnoreCase("true") || 
                    amAuthCert_chkCRL.equalsIgnoreCase("true"))) {
                debug.error("Fatal error: LDAP Server and Port misconfigured");
                throw new AuthLoginException(amAuthCert, 
                                "wrongLDAPServer", null);
            }

            if (amAuthCert_serverHost != null) {
                // set LDAP Parameters
                try {
                    LDAPUrl ldapUrl = LDAPUrl.valueOf("ldap://"+amAuthCert_serverHost);
                    amAuthCert_serverPort = ldapUrl.getPort();
                    amAuthCert_serverHost = ldapUrl.getHost();
                } catch (Exception e) {
                    throw new AuthLoginException(amAuthCert, "wrongLDAPServer",
                        null);
                }
            }

            amAuthCert_startSearchLoc = CollectionHelper.getServerMapAttr(
                options, "iplanet-am-auth-cert-start-search-loc");
            if (amAuthCert_startSearchLoc == null 
                && (amAuthCert_chkCertInLDAP.equalsIgnoreCase("true") || 
                    amAuthCert_chkCRL.equalsIgnoreCase("true"))) {
                debug.error("Fatal error: LDAP Start Search " +
                                "DN is not configured");
                throw new AuthLoginException(amAuthCert, "wrongStartDN", null);
            }

            if (amAuthCert_startSearchLoc != null) {
                if (!LDAPUtils.isDN(amAuthCert_startSearchLoc)) {
                    throw new AuthLoginException(amAuthCert, "wrongStartDN", null);
                }
            }

            if (debug.messageEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("\nldapProviderUrl=").append(amAuthCert_serverHost)
                    .append("\n\tamAuthCert_serverPort=").append(amAuthCert_serverPort)
                    .append("\n\tstartSearchLoc=").append(amAuthCert_startSearchLoc)
                    .append("\n\tsecurityType=").append(amAuthCert_securityType)
                    .append("\n\tprincipleUser=").append(amAuthCert_principleUser)
                    .append("\n\tauthLevel=").append(authLevel)
                    .append("\n\tuseSSL=").append(amAuthCert_useSSL)
                    .append("\n\tocspEnable=").append(ocspEnabled)
                    .append("\n\tuserProfileMapper=").append(amAuthCert_userProfileMapper)
                    .append("\n\tsubjectAltExtMapper=")
                        .append(amAuthCert_subjectAltExtMapper)
                    .append("\n\taltUserProfileMapper=")
                        .append(amAuthCert_altUserProfileMapper)
                    .append("\n\tchkCRL=").append(amAuthCert_chkCRL)
                    .append("\n\tchkAttrCRL=").append(amAuthCert_chkAttrCRL)
                    .append("\n\tchkAttributesCRL=").append(Arrays.toString(amAuthCert_chkAttributesCRL))
                    .append("\n\tcacheCRL=").append(doCRLCaching)
                    .append("\n\tupdateCRLs=").append(doCRLUpdate)
                    .append("\n\tchkCertInLDAP=").append(amAuthCert_chkCertInLDAP)
                    .append("\n\tchkAttrCertInLDAP=").append(amAuthCert_chkAttrCertInLDAP)
                    .append("\n\temailAddr=").append(amAuthCert_emailAddrTag)
                    .append("\n\tgw-cert-auth-enabled=").append(portal_gw_cert_auth_enabled)
                    .append("\n\tgw-cert-ParamName=").append(certParamName)
                    .append("\n\tgw_cert_preferred=").append(portal_gw_cert_preferred)
                    .append("\n\tclient=").append(client);
                debug.message(sb.toString());
            }
        } else {
            debug.error("options is null");
            throw new AuthLoginException(amAuthCert, "CERTex", null);
        }
    }
    
    /**
     * Process Certificate based auth request
     * @param callbacks for auth
     * @param state with auth framework
     * @return proper jaas state for auth framework
     * @throws AuthLoginException if auth fails
     */
    public int process (Callback[] callbacks, int state) 
        throws AuthLoginException {
        initAuthConfig();
        X509Certificate[] allCerts = null;
        try {
            HttpServletRequest servletRequest = getHttpServletRequest();
            if (servletRequest != null) { 
                allCerts = (X509Certificate[]) servletRequest.
                   getAttribute("javax.servlet.request.X509Certificate"); 
                if (allCerts == null || allCerts.length == 0) {
                    debug.message(
                          "Certificate: checking for cert passed in the URL.");
                    if (!portal_gw_cert_auth_enabled) {
                        debug.error ("Certificate: cert passed " +
                                     "in URL not enabled for this client");
                        throw new AuthLoginException(amAuthCert, 
                            "noURLCertAuth", null);
                    }
                    thecert = getPortalStyleCert(servletRequest);
                    allCerts = new X509Certificate[] { thecert };
                } else {
                    if (portal_gw_cert_auth_enabled && portal_gw_cert_preferred) {
                        thecert = getPortalStyleCert(servletRequest);
                        allCerts = new X509Certificate[] { thecert };
                    } else {
                       if (debug.messageEnabled()) {
                           debug.message("Certificate: got all certs from " +
                               "HttpServletRequest = {}", allCerts.length);
                       }
                       thecert = allCerts[0];
                    }
                }
            } else {
                thecert = sendCallback();
            }

            if (thecert == null) {
                debug.message("Certificate: no cert passed in.");
                throw new AuthLoginException(amAuthCert, "noCert", null);
            }

            // moved this call from the bottom to here so that url redirection
            // can work.
            getTokenFromCert(thecert);
            storeUsernamePasswd(userTokenId, null);
            if(debug.messageEnabled()){
                debug.message("in Certificate. userTokenId=" + 
                    userTokenId + " from getTokenFromCert");
            }
        } catch (AuthLoginException e) {
            setFailureID(userTokenId);
            debug.error("Certificate:  exiting validate with exception", e);
            throw new AuthLoginException(amAuthCert, "noCert", null);
        }

        /* debug statements added for cgi. */
        if (debug.messageEnabled()) {
            debug.message("Got client cert =\n" + thecert.toString());
        }

        if (amAuthCert_chkCertInLDAP.equalsIgnoreCase("false") && 
                amAuthCert_chkCRL.equalsIgnoreCase("false") &&
                                !ocspEnabled) {
                return ISAuthConstants.LOGIN_SUCCEED;
        }

        /*
        * Based on the certificates presented, find the registered
        * (representation) of the certificate. If no certificates
        * match in the LDAP certificate directory return a failure
        * status.
        */
        if (ldapParam == null) {
            setLdapStoreParam();
        }
        
        if (amAuthCert_chkCertInLDAP.equalsIgnoreCase("true")) { 
            X509Certificate ldapcert = 
                AMCertStore.getRegisteredCertificate(
                    ldapParam, thecert, amAuthCert_chkAttrCertInLDAP);
            if (ldapcert == null) {
                debug.error("X509Certificate: getRegCertificate is null");
                setFailureID(userTokenId);
                throw new AuthLoginException(amAuthCert, "CertNoReg", null);
            }
        }

        int ret = doJCERevocationValidation(allCerts);

        if (ret != ISAuthConstants.LOGIN_SUCCEED) {
            debug.error("X509Certificate:CRL / OCSP verify failed.");
    	    setFailureID(userTokenId);
            throw new AuthLoginException(amAuthCert, "CertVerifyFailed", null);
        }
        
        return ISAuthConstants.LOGIN_SUCCEED;
    }

    private int doJCERevocationValidation(X509Certificate[] allCerts)
        throws AuthLoginException {
    	int ret = ISAuthConstants.LOGIN_IGNORE;
		
    	try {
            Vector crls = new Vector();
            for (X509Certificate cert : allCerts) {
                X509CRL crl = AMCRLStore.getCRL(ldapParam, cert, amAuthCert_chkAttributesCRL);
                if (crl != null) {
                    crls.add(crl);
                }
            }
            if (debug.messageEnabled()) {
                debug.message("Cert.doRevocationValidation: crls size = " +
                          crls.size());
                if (crls.size() > 0) {
                    debug.message("CRL = " + crls.toString());
                }
            }

            AMCertPath certpath = new AMCertPath(crls);
            if (!certpath.verify(allCerts, crlEnabled, ocspEnabled)) {
                debug.error("CertPath:verify failed.");
                return ret;
            } else {
                if (debug.messageEnabled()) {
                    debug.message("CertPath:verify success.");
                }
            }
            ret = ISAuthConstants.LOGIN_SUCCEED;
    	}catch (Exception e) {
            debug.error("Cert.doRevocationValidation: verify failed.", e);
    	}
        
        return ret;
    }

    private void setLdapStoreParam() throws AuthLoginException {
    /*
     * Setup the LDAP certificate directory service context for
     * use in verification of the users certificates.
     */
        try {
            ldapParam = AMCertStore.setLdapStoreParam(amAuthCert_serverHost,
                       amAuthCert_serverPort,
                       amAuthCert_principleUser,
                       amAuthCert_principlePasswd,
                       amAuthCert_startSearchLoc,
                       amAuthCert_uriParamsCRL,
                       amAuthCert_useSSL.equalsIgnoreCase("true"));

            ldapParam.setDoCRLCaching(doCRLCaching);
            ldapParam.setDoCRLUpdate(doCRLUpdate);
            
        } catch (Exception e) {
            debug.error("validate.SSLSocketFactory", e);
            setFailureID(userTokenId);
            throw new AuthLoginException(amAuthCert,"sslSokFactoryFail", null);
        }
            
        return;
    }

    private void getTokenFromCert(X509Certificate cert)
        throws AuthLoginException {
	if (!amAuthCert_subjectAltExtMapper.equalsIgnoreCase("none")) {
	    getTokenFromSubjectAltExt(cert);
	}

	if (!amAuthCert_userProfileMapper.equalsIgnoreCase("none") && 
	    (userTokenId == null)) {
	    getTokenFromSubjectDN(cert);
	}
    }

    private void getTokenFromSubjectAltExt(X509Certificate cert)
        throws AuthLoginException {
        try {
            X509CertImpl certImpl = 
                new X509CertImpl(cert.getEncoded());
            X509CertInfo cinfo = 
                new X509CertInfo(certImpl.getTBSCertificate());
            CertificateExtensions exts = (CertificateExtensions) 
                            cinfo.get(X509CertInfo.EXTENSIONS);
            SubjectAlternativeNameExtension altNameExt = 
                (SubjectAlternativeNameExtension)
                    exts.get(SubjectAlternativeNameExtension.NAME);

            if (altNameExt != null) {
                GeneralNames names = (GeneralNames) altNameExt.get
                    (SubjectAlternativeNameExtension.SUBJECT_NAME);
        
                GeneralName generalname = null;  
                ObjectIdentifier upnoid = new ObjectIdentifier(UPNOID); 
                Iterator itr = (Iterator) names.iterator();
                while ((userTokenId == null) && itr.hasNext()) {
                    generalname = (GeneralName) itr.next(); 
                    if (generalname != null) {
                        if (amAuthCert_subjectAltExtMapper.
                        	equalsIgnoreCase("UPN") && 
                        	(generalname.getType() == 
                	        GeneralNameInterface.NAME_ANY)) {
                            OtherName othername = 
                                (OtherName)generalname.getName(); 
                            if (upnoid.equals((Object)(othername.getOID()))) {
                                byte[] nval = othername.getNameValue(); 
                                DerValue derValue = new DerValue(nval); 
                                userTokenId = 
                                    derValue.getData().getUTF8String(); 
                            } 
                        }else if (amAuthCert_subjectAltExtMapper.
                            equalsIgnoreCase("RFC822Name") && 
                            (generalname.getType() == 
                	        GeneralNameInterface.NAME_RFC822)) {
                            RFC822Name email = 
                                (RFC822Name) generalname.getName(); 
                            userTokenId = email.getName(); 
                        }
                    }
                }
            }
        } catch (Exception e) {
            debug.error("Certificate - " +
                    "Error in getTokenFromSubjectAltExt = " , e);
            throw new AuthLoginException(amAuthCert, "CertNoReg", null);
        }
            
    }

    private void getTokenFromSubjectDN(X509Certificate cert)
        throws AuthLoginException {
    /*
     * The certificate has passed the authentication steps
     * so return the part of the certificate as specified 
     * in the profile server.
     */
        try {
        /*
         * Get the Attribute value of the input certificate
         */
            X500Principal subjectPrincipal = cert.getSubjectX500Principal();
            if (debug.messageEnabled()) {
                debug.message("getTokenFromCert: Subject DN : " + CertUtils.getSubjectName(cert));
            }

            if (amAuthCert_userProfileMapper.equalsIgnoreCase("subject DN")) {
                userTokenId = CertUtils.getSubjectName(cert);
            } else if (amAuthCert_userProfileMapper.equalsIgnoreCase("subject UID")) {
                userTokenId = CertUtils.getAttributeValue(subjectPrincipal, CertUtils.UID);
            } else if (amAuthCert_userProfileMapper.equalsIgnoreCase("subject CN")) {
                userTokenId = CertUtils.getAttributeValue(subjectPrincipal, CertUtils.COMMON_NAME);
            } else if (amAuthCert_userProfileMapper.equalsIgnoreCase(amAuthCert_emailAddrTag)) {
                userTokenId = CertUtils.getAttributeValue(subjectPrincipal, CertUtils.EMAIL_ADDRESS);
                if (userTokenId == null) {
                    userTokenId = CertUtils.getAttributeValue(subjectPrincipal, CertUtils.MAIL);
                }
            } else if (amAuthCert_userProfileMapper.equalsIgnoreCase("DER Certificate")) {
                userTokenId = String.valueOf(cert.getTBSCertificate());
            } else if (amAuthCert_userProfileMapper.equals("other")) {
                //  "other" has been selected, so use attribute specified in the
                //  iplanet-am-auth-cert-user-profile-mapper-other attribute,
                //  which is in amAuthCert_altUserProfileMapper.
                userTokenId =  CertUtils.getAttributeValue(subjectPrincipal, amAuthCert_altUserProfileMapper);
            }

            if (debug.messageEnabled()) {
                debug.message("getTokenFromCert: " + amAuthCert_userProfileMapper + " " + userTokenId);
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("Certificate - Error in getTokenFromSubjectDN = " , e);
            }
            throw new AuthLoginException(amAuthCert, "CertNoReg", null);
        }
    }

    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        } else if (userTokenId != null) {
            userPrincipal = new CertAuthPrincipal(userTokenId);
            return userPrincipal;
        } else {
            return null;
        }
    }

    /**
     * Return value of Certificate 
     * @return X509Certificate for auth
     */
    public X509Certificate getCertificate() {
       return thecert;
    }

    /**
     * Return value of Attribute Name for CRL checking 
     * @return value for attribute name to search crl from ldap store
     */
    public String getChkAttrCRL() {
       return amAuthCert_chkAttrCRL;
    }
        
    /**
     * Return value of Debug object for this module 
     *
     * @return debug 
     */
    public com.sun.identity.shared.debug.Debug getDebug() {
       return debug;
    }

    /**
     * Return value of URI parameter for getting CRL 
     *
     * @return value of URI parameter for getting CRL 
     */
    public String getUriParamsCRL() {
       return amAuthCert_uriParamsCRL;
    }

    /**
     * Return value of LDAP Search loc for directory server 
     *
     * @return value of LDAP Search loc for directory server  
     */
    public String getStartSearchLoc() {
       return amAuthCert_startSearchLoc;
    }
                        
    private X509Certificate sendCallback() throws AuthLoginException {
        if (callbackHandler == null) {
            throw new AuthLoginException(amAuthCert, "NoCallbackHandler", null);        }
        X509Certificate cert = null;
        try {
            Callback[] callbacks = new Callback[1];
            callbacks[0] = 
                new X509CertificateCallback (bundle.getString("certificate"));
            callbackHandler.handle(callbacks);
            X509CertificateCallback xcb = (X509CertificateCallback)callbacks[0];
            /*
             * Allow Cert auth module accepts personal certificate only for
             * following 3 cases :
             * 1. portal_gw_cert_auth_enabled == true :
             *    Case of getting cert from trusted host like sra,
             *    distAuth, trusted LB
             * 2. xcb.getReqSignature() == false :
             *    Case of getting cert through ssl client auth enabled port
             * 3. (xcb.getReqSignature() == true) && (signature != null) :
             *    Case of getting cert together with signature from sdk client              */
            byte[] signature = xcb.getSignature();
            if (portal_gw_cert_auth_enabled ||
                !xcb.getReqSignature() ||
                (xcb.getReqSignature() && (signature != null))) {
                cert = xcb.getCertificate();
            }
            return cert;
        } catch (IllegalArgumentException ill) {
            debug.message("message type missing");
            throw new AuthLoginException(amAuthCert, "IllegalArgs", null);
        } catch (java.io.IOException ioe) {
            throw new AuthLoginException(ioe);
        } catch (UnsupportedCallbackException uce) {
            throw new AuthLoginException(amAuthCert, "NoCallbackHandler", null);
        }    
    }

    private X509Certificate getPortalStyleCert (HttpServletRequest request) 
       throws AuthLoginException {
       String certParam = null;

       if ((certParamName != null) && (certParamName.length() > 0)) {
           debug.message ("getPortalStyleCert: checking cert in HTTP header");
           StringTokenizer tok = new StringTokenizer(certParamName, ",");
           while (tok.hasMoreTokens()) {
               String key = tok.nextToken();
                certParam = request.getHeader(key);
                if (certParam == null) {
                    continue;
                }
                certParam = certParam.trim();
                String begincert = "-----BEGIN CERTIFICATE-----";
                String endcert = "-----END CERTIFICATE-----";
                int idx = certParam.indexOf(endcert);
                if (idx != -1) {
                    certParam = certParam.substring(begincert.length(), idx);
                    certParam = certParam.trim();
                }
           }
       } else {
           debug.message("getPortalStyleCert: checking cert in userCert param");
           Hashtable requestHash = 
               getLoginState("getPortalStyleCert()").getRequestParamHash(); 
           if (requestHash != null) {
               certParam = (String) requestHash.get("IDToken0"); 
               if (certParam == null) {
                   certParam = (String) requestHash.get("Login.Token0"); 
               }
           }
       }

       if (debug.messageEnabled()) {
           debug.message ("in Certificate. validate certParam: " + certParam);
       }
       if (certParam == null || certParam.equals("")) {
           debug.message("Certificate: no cert from HttpServletRequest header");
           throw new AuthLoginException(amAuthCert, "noCert", null);
       }

       byte[] decoded = Base64.decode(certParam);
       if (decoded == null) {
           debug.error("CertificateFromParameter(decode): failed, possibly invalid Base64 input");
           throw new AuthLoginException(amAuthCert, "CERTex", null);
       }
       InputStream carray = new ByteArrayInputStream(decoded);

       debug.message("Certificate: CertificateFactory.getInstance.");
       CertificateFactory cf = null;
       X509Certificate userCert = null;
       try {
           cf = CertificateFactory.getInstance("X.509");
           userCert = (X509Certificate) cf.generateCertificate(carray);
       } catch (Exception e) {
           debug.error("CertificateFromParameter(X509Cert): exception ", e);
           throw new AuthLoginException(amAuthCert, "CERTex", null);
       }

       if (userCert == null) {
           throw new AuthLoginException(amAuthCert, "CERTex", null);
       }

       if (debug.messageEnabled()) {
           debug.message("X509Certificate: principal is: " + 
               userCert.getSubjectDN().getName() +
               "\nissuer DN:" + userCert.getIssuerDN().getName() +
               "\nserial number:" + String.valueOf(userCert.getSerialNumber()) +
               "\nsubject dn:" + userCert.getSubjectDN().getName());
        }
        return userCert;
    }

    /**
     * Destroy the state of module
     */
    public void destroyModuleState() {
        userPrincipal = null;
        userTokenId = null;
    }

    /**
     * Initialize all member variables as null
     */
    public void nullifyUsedVars() {
        bundle = null;
        thecert = null;
        options = null;
        callbackHandler = null;
        amAuthCert_serverHost = null;
        amAuthCert_startSearchLoc = null;
        amAuthCert_securityType = null;
        amAuthCert_principleUser = null;
        amAuthCert_principlePasswd = null;
        amAuthCert_useSSL = null;
        amAuthCert_userProfileMapper = null;
        amAuthCert_altUserProfileMapper = null;
        amAuthCert_chkCRL = null;
        amAuthCert_chkAttrCRL = null;
        amAuthCert_chkAttributesCRL = null;
        amAuthCert_uriParamsCRL = null;
        amAuthCert_chkCertInLDAP = null;
        amAuthCert_chkAttrCertInLDAP = null;
        amAuthCert_emailAddrTag = null;
        portalGateways = null;
        amAuthCert_updateCRL = null;
    }
    
    private String[] trimItems(String[] items) {
        String[] trimmedItems = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            trimmedItems[i] = items[i].trim();
        }
        return trimmedItems;
    }    
}
