/* The contents of this file are subject to the terms
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
 * $Id: FAMX509Validator.java,v 1.1 2008/03/27 17:09:56 mrudul_uchil Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
 
package com.sun.identity.wss.sts.spi;

import com.sun.xml.wss.SubjectAccessor;
import com.sun.xml.wss.impl.callback.CertificateValidationCallback;

import java.security.cert.CertPathBuilder;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

import java.util.*;
import javax.security.auth.Subject;
import java.security.Principal;
import javax.security.auth.callback.Callback;
import java.security.PrivilegedAction;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.security.AccessController;
import java.security.cert.X509Certificate;
import com.sun.identity.authentication.spi.X509CertificateCallback;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.debug.Debug;

import com.sun.identity.wss.security.SecurityException;
import com.sun.identity.wss.security.SecurityPrincipal;
import com.sun.identity.wss.security.WSSConstants;
import com.sun.identity.wss.security.WSSUtils;
import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.provider.ProviderException;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;

public class FAMX509Validator implements 
CertificateValidationCallback.CertificateValidator {

    private static Debug debug = 
        Debug.getInstance("famX509Validator");


    public boolean validate(X509Certificate certificate)
            throws CertificateValidationCallback.CertificateValidationException {
        debug.message("FAMX509Validator: In validate cert");
        debug.message("certificate : " + certificate);        
        
        if(certificate == null) {
            debug.error("FAMX509Validator.validate: X509 auth " +
            "could not find the message certificate.");
        }

        XMLSignatureManager sigManager = WSSUtils.getXMLSignatureManager();
        String certAlias = sigManager.getKeyProvider().getCertificateAlias(certificate);
            
        try {
            certificate.checkValidity();
        } catch (CertificateExpiredException e) {
            debug.error("FAMX509Validator:X509Certificate Expired", e );
            throw new CertificateValidationCallback.CertificateValidationException("X509Certificate Expired", e);
        } catch (CertificateNotYetValidException e) {
            debug.error("FAMX509Validator:X509Certificate not yet valid", e );
            throw new CertificateValidationCallback.CertificateValidationException("X509Certificate not yet valid", e);
        }
        
        Subject subject = SubjectAccessor.getRequesterSubject();
        if (subject == null){
            subject = new Subject();
            SubjectAccessor.setRequesterSubject(subject);
        }

        try {
            ProviderConfig config = getWSPConfig("wsp");
            String authChain = null;
        
            if (config != null) {
                authChain = config.getAuthenticationChain();
            }
            
            if(!authenticateCert(certAlias, authChain, certificate, subject)) {
                debug.error("FAMX509Validator: Authentication failed : ");
            }

        } catch (Exception ex) {
            debug.error("FAMX509Validator.ERROR setting Subject: "
                        ,ex);
        }

	debug.message("FAMX509Validator::final Subject is : " 
                      + subject.toString());
        debug.message("FAMX509Validator::final Subject principals is : " 
                      + subject.getPrincipals());
        debug.message("FAMX509Validator::final Subject public cred is : " 
                      + subject.getPublicCredentials());

        return true;
    }  

    // Returns STS configuration.
    private static ProviderConfig getWSPConfig(String providerName) {

        ProviderConfig config = null;
        try {
            config = ProviderConfig.getProvider(providerName, 
                          ProviderConfig.WSP);
            if(config == null) {
               debug.error("FAMX509Validator.getWSPConfig::" + 
                           " Provider configuration is null");
            }

        } catch (ProviderException pe) {
            debug.error("FAMX509Validator.getWSPConfig:: Provider"+
               " configuration read failure", pe);
        } catch (Exception e) {
            debug.error("FAMX509Validator.getWSPConfig:: Provider"+
               " configuration read failure", e);
        }
        return config;
    }
    
    /**
     * Authenticates the client certificate, present in the X509 token
     * against configured Certificate authentication chain on AM server.
     */
    private boolean authenticateCert(String certAlias, String authChain,
           X509Certificate cert, Subject subject) throws SecurityException {

        if( (certAlias == null) || (certAlias.length() == 0) ) {  
            return false;
        }
      
        if (debug.messageEnabled()) {
            debug.message("authenticateCert: certAlias : " + certAlias);
            debug.message("authenticateCert: authChain : " + authChain);
        }

        // Autheticate to LDAP server using Authentication client API
        AuthContext ac = null;
        AuthContext.IndexType indexType = AuthContext.IndexType.SERVICE;
        String indexName = authChain;
        try {
            ac = new AuthContext("/", certAlias);
	    debug.message("authenticateCert: Obtained AuthContext");
            ac.login(indexType, indexName);
        } catch (AuthLoginException le) {
            debug.error("authenticateCert: Login error : " + le.getMessage());
            return false;
        }

	Callback[] callbacks = null;
        while (ac.hasMoreRequirements()) {
	    callbacks = ac.getRequirements();

	    if (callbacks != null) {
		try {
		    addLoginCallbackMessage(callbacks,null,null,cert);
		    ac.submitRequirements(callbacks);
		} catch (Exception e) {
		    debug.error("authenticateCert: Submit error : " 
                                + e.getMessage());
                    return false;
		}
	    }
	}

        SSOToken ssotoken = null;
	if (ac.getStatus() == AuthContext.Status.SUCCESS) {
	    debug.message("authenticateCert: Login success!!");
            try {
                ssotoken = ac.getSSOToken();
                debug.message("authenticateCert: got SSOToken successfully");
            } catch(Exception ex){
                if(debug.messageEnabled()) {
                    debug.message("authenticateCert: SSOToken error : " 
                              + ex.getMessage());
                }
            }

        } else if (ac.getStatus() == AuthContext.Status.FAILED) {
	    debug.error("authenticateCert: Login Failed.");
            return false;
	} else {
            debug.error("authenticateCert: Unknown status : " 
                        + ac.getStatus());
            return false;
	}

        addSSOToken(ssotoken, subject);
        return true;
    }
    
    /**
     * Adds SSOToken Id as private credential of the Subject.
     * @param ssoToken
     *
     * @exception SecurityException
     */
    private void addSSOToken(final SSOToken ssoToken, final Subject subj)
                   throws SecurityException {
        if (ssoToken != null) {
            try {                
                AccessController.doPrivileged(new PrivilegedAction() {
                    public java.lang.Object run() {
                        Set creds = subj.getPrivateCredentials();
                        /*if(creds != null) {                        
                             Iterator iter =  creds.iterator();
                             while(iter.hasNext()) {
                                 Object credObj = iter.next();
                                 if(credObj instanceof SSOToken) {
                                     creds.remove(credObj);
                                 } 
                             }
                        }*/
                        creds.add(ssoToken);
                        return null;
                    }
                });
                debug.message("Set SSOToken in Subject successfully");
            } catch (Exception e) {
                debug.message("Can not set SSOToken in Subject");
                throw new SecurityException(e.getMessage());
            }
        }
    }

    /**
     * Adds SecurityPrincipal to the Subject.
     */
    private Subject addPrincipal(String principalName, Subject subject) {
        Principal principal = new SecurityPrincipal(principalName); 
        subject.getPrincipals().add(principal);
        return subject;
    }

    // get user's inputs and set them to callback array.
    static void addLoginCallbackMessage(Callback[] callbacks,String userName, 
                                        String password, X509Certificate cert) 
        throws UnsupportedCallbackException {
        int i = 0;
        try {
            for (i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    NameCallback nc = (NameCallback) callbacks[i];
                    nc.setName(userName.trim());
                } else if (callbacks[i] instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) callbacks[i];
                    pc.setPassword(password.toCharArray());
                } else if (callbacks[i] instanceof X509CertificateCallback) {
                    X509CertificateCallback certCB = 
                        (X509CertificateCallback)callbacks[i];
                    try {
                        certCB.setReqSignature(false);
                        certCB.setCertificate(cert);  
                    } catch (Exception e) {
                        if(debug.messageEnabled()) {
                            debug.message("createX509CertificateCallback : " 
                                         + e.toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new UnsupportedCallbackException(callbacks[i], 
                                                   "Callback exception: " + e);
        }
    }
    
}
