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
 * $Id: DefaultAuthenticator.java,v 1.22 2009/07/24 21:51:06 mallas Exp $
 *
 */

package com.sun.identity.wss.security.handler;

import org.w3c.dom.Element;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.net.URL;
import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.security.AccessController;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdRepoException;

import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.security.PasswordCredential;
import com.sun.identity.wss.security.SecurityMechanism;
import com.sun.identity.wss.security.SecurityToken;
import com.sun.identity.wss.security.SecurityException;
import com.sun.identity.wss.security.SecurityPrincipal;
import com.sun.identity.wss.security.UserNameToken;
import com.sun.identity.wss.security.AssertionToken;
import com.sun.identity.wss.security.WSSConstants;
import com.sun.identity.wss.security.WSSUtils;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.assertion.AttributeStatement;
import com.sun.identity.saml.assertion.AuthenticationStatement;
import com.sun.identity.saml.assertion.Statement;
import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.liberty.ws.soapbinding.Message;
import com.sun.identity.liberty.ws.security.SecurityAssertion;
import com.sun.identity.wss.security.SAML2Token;
import com.sun.identity.wss.security.SAML2TokenUtils;
import com.sun.identity.wss.security.BinarySecurityToken;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.X509CertificateCallback;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.wss.security.KerberosConfiguration;
import com.sun.identity.wss.sts.TrustAuthorityClient;
import com.sun.identity.wss.sts.FAMSTSException;
import com.sun.identity.wss.security.FAMSecurityToken;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.services.naming.URLNotFoundException;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.assertion.SubjectConfirmation;
import com.sun.identity.shared.Constants;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.Oid;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import sun.security.krb5.EncryptionKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.wss.sts.STSConstants;
import com.iplanet.security.x509.CertUtils;
import com.sun.identity.wss.logging.LogUtil;
import com.sun.identity.shared.DateUtils;
import java.text.ParseException;
import java.util.Date;

/**
 * This class provides a default implementation for authenticating the
 * webservices clients using various security mechanisms.
 */ 
public class DefaultAuthenticator implements MessageAuthenticator {
        
    private ProviderConfig config = null;
    //private Subject subject = null;
    private static ResourceBundle bundle = WSSUtils.bundle;
    private static Debug debug = WSSUtils.debug;
    public static final String WSS_CACHE_PLUGIN =
                               "com.sun.identity.wss.security.cache.plugin";
    private String kerberosPrincipal = null;
    private static Class cacheClass;
            
    /**
     * Authenticates the web services client.
     * @param subject the JAAS subject that may be used during authentication.
     * @param securityMechanism the security mechanism that will be used to
     *        authenticate the web services client.
     * @param securityToken the security token that is used.
     * @param config the provider configuration.
     * @param secureMessage the secure SOAPMessage.
     *      If the message security is provided by the WS-I profies, the
     *      secureMessage object is of type 
     *     <code>com.sun.identity.wss.security.handler.SecureSOAPMessage</code>.
     *     If the message security is provided by the Liberty ID-WSF
     *     profiles, the secure message is of type 
     *     <code>com.sun.identity.liberty.ws.soapbinding.Message</code>.
     * @param isLiberty boolean variable to indicate that the message
     *        security is provided by the liberty security profiles.
     * @exception SecurityException if there is a failure in authentication.
     */
    public Object authenticate(
             Subject subject,
             SecurityMechanism securityMechanism,
             SecurityToken securityToken,
             ProviderConfig config,
             Object secureMessage,
             boolean isLiberty) throws SecurityException {

        debug.message("DefaultAuthenticator.authenticate: start");
        this.config = config;        
        Map secureAttrs = new HashMap();
        String authChain = null;
        if (config != null) {
            authChain = config.getAuthenticationChain();
        }

        if(isLiberty) {
           return authenticateLibertyMessage(secureMessage, subject,secureAttrs);
        }
        if(securityMechanism == null) {
           throw new SecurityException(
                 bundle.getString("nullInputParameter"));
        }

        String uri = securityMechanism.getURI();

        if((SecurityMechanism.WSS_NULL_USERNAME_TOKEN_URI.equals(uri)) ||
            (SecurityMechanism.WSS_TLS_USERNAME_TOKEN_URI.equals(uri)) ||
            (SecurityMechanism.WSS_CLIENT_TLS_USERNAME_TOKEN_URI.equals(uri)) ||
            (SecurityMechanism.WSS_NULL_USERNAME_TOKEN_PLAIN_URI.equals(uri)) ||
            (SecurityMechanism.WSS_TLS_USERNAME_TOKEN_PLAIN_URI.equals(uri)) ||
            (SecurityMechanism.WSS_CLIENT_TLS_USERNAME_TOKEN_PLAIN_URI.
             equals(uri))){

            if(debug.messageEnabled()) {
               debug.message("DefaultAuthenticator.authenticate:: username" +
               " token authentication");
               debug.message("authenticate: authChain : " + authChain);
            }

            UserNameToken usertoken = (UserNameToken)securityToken;
            
            if ((authChain == null) || (authChain.length() == 0) ||
                (authChain.equals("none"))) {
                if((config != null) && (!validateUser(usertoken, subject)) ){
                    if(debug.warningEnabled()) {
                       debug.warning("DefaultAuthenticator. authentication " +                                
                               "failed."); 
                    }
                    throw new SecurityException(
                        bundle.getString("authenticationFailed"));
                }
            } else {
                if(!authenticateUser(usertoken, subject, authChain)) {
                    throw new SecurityException(
                        bundle.getString("authenticationFailed"));
                }
            }
        } else if(
            (SecurityMechanism.WSS_NULL_X509_TOKEN_URI.equals(uri)) ||
            (SecurityMechanism.WSS_TLS_X509_TOKEN_URI.equals(uri)) ||
            (SecurityMechanism.WSS_CLIENT_TLS_X509_TOKEN_URI.equals(uri))) {

            if(debug.messageEnabled()) {
               debug.message("DefaultAuthenticator.authenticate:: x509" +
               " token authentication");
               debug.message("authenticate: authChain : " + authChain);
            }

            SecureSOAPMessage securedMessage = 
                              (SecureSOAPMessage)secureMessage;
            X509Certificate cert = securedMessage.getMessageCertificate();
            
            if(cert == null) {
               debug.error("DefaultAuthenticator.authenticate:: X509 auth " +
               "could not find the message certificate.");
               throw new SecurityException(
                     bundle.getString("authenticationFailed"));
            }

            XMLSignatureManager sigManager = WSSUtils.getXMLSignatureManager();
            String certAlias = sigManager.getKeyProvider().
                getCertificateAlias(cert);

            if(debug.messageEnabled()) {
                debug.message("DefaultAuthenticator.authenticate: cert : " 
                              + cert);
                debug.message("DefaultAuthenticator.authenticate: certAlias : " 
                              + certAlias);
            }
            
            if ((authChain != null) && (authChain.length() != 0) &&
                (!authChain.equals("none"))) {
                if(!authenticateCert(certAlias,authChain,cert, subject)) {
                    throw new SecurityException(
                        bundle.getString("authenticationFailed"));
                }
            }
            
            String subjectDN = CertUtils.getSubjectName(cert);
            subject = addPrincipal(subjectDN, subject);
            subject.getPublicCredentials().add(cert);
            WSSUtils.setRoles(subject, subjectDN);
        } else if(
            (SecurityMechanism.WSS_NULL_SAML_HK_URI.equals(uri)) ||
            (SecurityMechanism.WSS_TLS_SAML_HK_URI.equals(uri)) ||
            (SecurityMechanism.WSS_CLIENT_TLS_SAML_HK_URI.equals(uri)) ||
            (SecurityMechanism.WSS_NULL_SAML_SV_URI.equals(uri)) ||
            (SecurityMechanism.WSS_TLS_SAML_SV_URI.equals(uri)) ||
            (SecurityMechanism.WSS_CLIENT_TLS_SAML_SV_URI.equals(uri))) {

            if(debug.messageEnabled()) {
               debug.message("DefaultAuthenticator.authenticate:: saml" +
               " token authentication");
            }
            AssertionToken assertionToken = (AssertionToken)securityToken;
            if ((authChain != null) && (authChain.length() != 0) &&
                (!authChain.equals("none"))) {
                if(!authenticateAssertion(assertionToken.toDocumentElement(),
                        config, subject)) {
                   throw new SecurityException(
                     bundle.getString("authenticationFailed"));
                }
            } else if(!validateAssertion(
                    assertionToken.getAssertion(), subject, secureAttrs)) {
               throw new SecurityException(
                     bundle.getString("authenticationFailed"));
            }
        }  else if(
            (SecurityMechanism.WSS_NULL_SAML2_HK_URI.equals(uri)) ||
            (SecurityMechanism.WSS_TLS_SAML2_HK_URI.equals(uri)) ||
            (SecurityMechanism.WSS_CLIENT_TLS_SAML2_HK_URI.equals(uri)) ||
            (SecurityMechanism.WSS_NULL_SAML2_SV_URI.equals(uri)) ||
            (SecurityMechanism.WSS_TLS_SAML2_SV_URI.equals(uri)) ||
            (SecurityMechanism.WSS_CLIENT_TLS_SAML2_SV_URI.equals(uri))) {
            
            if(debug.messageEnabled()) {
               debug.message("DefaultAuthenticator.authenticate:: saml2" +
               " token authentication");
            }
            SAML2Token saml2Token = (SAML2Token)securityToken;
            if ((authChain != null) && (authChain.length() != 0) &&
                (!authChain.equals("none"))) {
                if(!authenticateAssertion(saml2Token.toDocumentElement(),
                        config, subject)) {
                   throw new SecurityException(
                     bundle.getString("authenticationFailed"));
                }
            } else 
            if(!SAML2TokenUtils.validateAssertion(saml2Token.getAssertion(),
                    subject, secureAttrs)) {
               throw new SecurityException(
                     bundle.getString("authenticationFailed"));
            }
        } else if(
            (SecurityMechanism.WSS_NULL_KERBEROS_TOKEN_URI.equals(uri)) ||
            (SecurityMechanism.WSS_TLS_KERBEROS_TOKEN_URI.equals(uri)) ||
            (SecurityMechanism.WSS_CLIENT_TLS_KERBEROS_TOKEN_URI.equals(uri))) {
            
            if(debug.messageEnabled()) {
               debug.message("DefaultAuthenticator.authenticate:: kerberos" +
                       " token authentication");               
            }
            
            BinarySecurityToken bst = (BinarySecurityToken)securityToken;
            validateKerberosToken(Base64.decode(bst.getTokenValue()), subject);            
        } else {
            debug.error("DefaultAuthenticator.authenticate:: Invalid " +
            "security mechanism");
            String[] data = {uri};
            LogUtil.error(Level.INFO,
                        LogUtil.AUTHENTICATION_FAILED,
                        data,
                        null);
            throw new SecurityException(
                     bundle.getString("authenticationFailed"));
        }
        //add security token to the subject for the application consumption.
        if(securityToken != null) {
           subject.getPublicCredentials().add(
                   securityToken.toDocumentElement());
        }
        
        if(securityMechanism != null) {
           secureAttrs.put(WSSConstants.AUTH_METHOD, uri);
           subject.getPublicCredentials().add(secureAttrs);
        }
        return subject;
    }

    /**
     * Validates the user present in the username token.
     */
    private boolean validateUser(UserNameToken usernameToken, Subject subject) 
        throws SecurityException {

        String user = usernameToken.getUserName();
        String password = usernameToken.getPassword();
        if( (user == null) || (password == null) ) {  
            return false;
        }
      
        List users = config.getUsers();
        if(users == null || users.isEmpty()) {
           debug.error("DefaultAuthenticator.validateUser:: users are not " +
           " configured in the providers.");
           return false;
        }

        Iterator iter = users.iterator();
        String configuredUser = null, configuredPassword = null;
        while(iter.hasNext()) {
            PasswordCredential cred = (PasswordCredential)iter.next();
            configuredUser = cred.getUserName();
            if(configuredUser.equals(user)) {
               configuredPassword = cred.getPassword();
               break;
            }
        } 

        if(configuredUser == null || configuredPassword == null) {
           debug.error("DefaultAuthenticator.validateUser:: configured user " +
           " does not have the password.");
            return false;
        }

        String passwordType = usernameToken.getPasswordType(); 
        if((passwordType != null) && 
           (passwordType.equals(WSSConstants.PASSWORD_DIGEST_TYPE)) ) {
           String nonce = usernameToken.getNonce();
           String created = usernameToken.getCreated();
           if(!validateUserTokenTime(created)) {
              return false; 
           }
           String digest = UserNameToken.getPasswordDigest(
                 configuredPassword, nonce, created);
           if(!(digest.equals(password)) || !(configuredUser.equals(user))) {
               debug.error("DefaultAuthenticator.validateUser:: Password " +
               "does not match");
               return false;
           }
           
           if(config.isUserTokenDetectReplayEnabled()) {
              cacheNonce(created, nonce);
           }
        } else if(!(configuredPassword.equals(password)) || 
                   !(configuredUser.equals(user))) { 
           
          debug.error("DefaultAuthenticator.validateUser:: Password " +
          "does not match");
           return false;
        }
        
        subject = addPrincipal(user, subject);
        WSSUtils.setRoles(subject, user);
        return true;
    }

    /**
     * Authenticates the user, present in the username token
     * against configured LDAP server.
     */
    private boolean authenticateUser(UserNameToken usernameToken, 
            Subject subject, String authChain) throws SecurityException {

        String user = usernameToken.getUserName();
        String password = usernameToken.getPassword();
        if( (user == null) || (password == null) ) {  
            return false;
        }
        String nonce = usernameToken.getNonce();
        String created = usernameToken.getCreated();
        if(!validateUserTokenTime(created)) {
           return false; 
        }
        String passwordType = usernameToken.getPasswordType().trim();                
        if(WSSConstants.PASSWORD_DIGEST_TYPE.equals(passwordType)) {
           password = "PasswordDigest=" + password + ";" +
                      "Nonce=" + nonce+ ";" +
                      "Timestamp=" + created;
        }
      
        // Autheticate to LDAP server using Authentication client API
        AuthContext ac = null;
        AuthContext.IndexType indexType = AuthContext.IndexType.SERVICE;
        String indexName = authChain;
	try {
            ac = new AuthContext("/");
	    debug.message("authenticateUser: Obtained AuthContext");
            ac.login(indexType, indexName);
        } catch (AuthLoginException le) {
            debug.error("authenticateUser: Login error : " + le.getMessage());
            return false;
        }

	Callback[] callbacks = null;
        while (ac.hasMoreRequirements()) {
	    callbacks = ac.getRequirements();

	    if (callbacks != null) {
		try {
		    addLoginCallbackMessage(callbacks,user,password,null);
		    ac.submitRequirements(callbacks);
		} catch (Exception e) {
		    debug.error("authenticateUser: Submit error : " 
                                + e.getMessage());
                    return false;
		}
	    }
	}

        SSOToken ssotoken = null;
	if (ac.getStatus() == AuthContext.Status.SUCCESS) {
	    debug.message("authenticateUser: Login success!!");
            try {
                ssotoken = ac.getSSOToken();
                debug.message("authenticateUser: got SSOToken successfully");
            } catch(Exception ex){
                if(debug.messageEnabled()) {
                    debug.message("authenticateUser: SSOToken error : " 
                              + ex.getMessage());
                }
            }

        } else if (ac.getStatus() == AuthContext.Status.FAILED) {
	    debug.error("authenticateUser: Login Failed.");
            return false;
	} else {
            debug.error("authenticateUser: Unknown status : " 
                        + ac.getStatus());
            return false;
	}
        
        if(config.isUserTokenDetectReplayEnabled()) {
           cacheNonce(created, nonce);
        }
        subject = addPrincipal(user, subject);
        WSSUtils.setRoles(subject, user);
        addSSOToken(ssotoken, subject);
        return true;
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
     * Validates the security assertion token.
     */
    private boolean validateAssertion(Assertion assertion, Subject subject, 
            Map secureAttrs) throws SecurityException {

        if((assertion.getConditions() != null) &&
                  !(assertion.getConditions().checkDateValidity(
                    System.currentTimeMillis() + WSSUtils.getTimeSkew())) ) {
           if(debug.messageEnabled()) {
              debug.message("DefaultAuthenticator.validateAssertionToken:: " +
              " assertion time is not valid");
           }
           return false;
        }

        com.sun.identity.saml.assertion.Subject sub = null;
        Iterator iter = assertion.getStatement().iterator();
        while(iter.hasNext()) {
            Statement st = (Statement)iter.next();
            if(Statement.AUTHENTICATION_STATEMENT == st.getStatementType()) {
               AuthenticationStatement authStatement = 
                                       (AuthenticationStatement)st;
               sub = authStatement.getSubject();
               
               Element keyInfo = sub.getSubjectConfirmation().getKeyInfo();
               if(keyInfo != null) {
                  X509Certificate cert = WSSUtils.getCertificate(keyInfo);
                  subject.getPublicCredentials().add(cert);
               }
               break;
            } else if(Statement.ATTRIBUTE_STATEMENT == st.getStatementType()) {
               AttributeStatement attribStatement = (AttributeStatement)st;
               sub = attribStatement.getSubject();
               SubjectConfirmation subConfirmation = null;
               Element keyInfo = null;
               if(subConfirmation != null) {
                  keyInfo = subConfirmation.getKeyInfo(); 
               }
               
               if(keyInfo != null) {
                  X509Certificate cert = WSSUtils.getCertificate(keyInfo);
                  subject.getPublicCredentials().add(cert);
               }
               List attributes = attribStatement.getAttribute();
               if(attributes.isEmpty()) {
                  break; 
               }
               for (Iterator iter1 = attributes.iterator(); iter1.hasNext();) {
                   Attribute attr = (Attribute)iter1.next();
                   try {
                       secureAttrs.put(attr.getAttributeName(), 
                                               attr.getAttributeValue());
                   } catch (Exception se) {
                      throw new SecurityException(se.getMessage());
                   }
               }               
               break;
            }
        }

        if(sub == null) {
           if(debug.messageEnabled()) {
              debug.message("DefaultAuthenticator.validateAssertionToken:: " +
              "Assertion does not have subject");
           }
           return false;
        }

        NameIdentifier ni = sub.getNameIdentifier(); 
        if(ni == null) {
           return false;
        }

        subject = addPrincipal(ni.getName(), subject);
        WSSUtils.setRoles(subject, ni.getName());
        return true;
    }

    /**
     * Authenticates SOAPMessages using Liberty ID-WSF profiles.
     */
    private Object authenticateLibertyMessage(Object message, Subject subject, 
            Map secureAttrs) throws SecurityException  {

        if(message == null || subject == null) {
           throw new IllegalArgumentException(
                 bundle.getString("nullInput"));
        }
        Message requestMsg = (Message)message;
        SecurityAssertion assertion = requestMsg.getAssertion();
        if(assertion != null) {
           if(!validateAssertion(assertion, subject, secureAttrs)) {
              throw new SecurityException(
                 bundle.getString("authenticationFailed"));
           } else {
              return subject;
           }
        }
        X509Certificate messageCert = requestMsg.getMessageCertificate();
        if(messageCert == null) {
           throw new SecurityException(
                 bundle.getString("authenticationFailed"));
        }
        String subjectDN = messageCert.getSubjectDN().getName();
        Principal principal = new SecurityPrincipal(subjectDN);
        subject.getPrincipals().add(principal);
        return subject;   
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
                        subj.getPrivateCredentials().add(ssoToken);
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
    
     private boolean validateKerberosToken(final byte[] token, Subject subject) 
                        throws SecurityException {         
         Subject serverSubject = getServerSubject();         
         try {
             Subject.doAs(serverSubject, new PrivilegedExceptionAction(){                
                public Object run() throws Exception {                    
                    final GSSManager manager = GSSManager.getInstance();
                    final Oid krb5Oid = new Oid("1.2.840.113554.1.2.2");
                    // Use custom GSS XWSS Provider to get the secret key
                    // This works only with JDK6 for now.
                    AccessController.doPrivileged(
                           new java.security.PrivilegedAction() {                    
                        public Object run() {
                           try {
                               manager.addProviderAtFront(
                               new com.sun.xml.ws.security.jgss.XWSSProvider(),
                               krb5Oid);
                           } catch(GSSException gsse){
                               WSSUtils.debug.error("BinarySecurityToken." +
                                       "validateKerberosToken", gsse);
                           }
                           return null;
                       }
                    });                                                           
                    
                    GSSContext context = manager.createContext(
                            (GSSCredential)null);                                        
                    byte[] outToken = context.acceptSecContext(token, 0,
                            token.length);
                    kerberosPrincipal = context.getSrcName().toString();
                    return null;
                 }
             });
         } catch (Exception ge) {
            debug.error("BinarySecurityToken.getKerberosToken: GSS Error", ge);
            throw new SecurityException(ge.getMessage());
        }
        
        // Retrieve the session key
        Set<Object> creds =  serverSubject.getPrivateCredentials();
        Iterator<Object> iter2 = creds.iterator();
        while(iter2.hasNext()){
                Object privObject = iter2.next();
                if(privObject instanceof EncryptionKey){
                    EncryptionKey encKey = (EncryptionKey)privObject;                                         
                    byte[] keyBytes = encKey.getBytes();
                    Key secretKey = new SecretKeySpec(keyBytes, "DES");                            
                    subject.getPublicCredentials().add(secretKey);
                    break;                    
                }
        }
        addPrincipal(kerberosPrincipal, subject);
        return true;
    }
    
    private Subject getServerSubject() throws SecurityException {
        String kdcRealm =config.getKDCDomain();
        String kdcServer = config.getKDCServer();
                
        System.setProperty("java.security.krb5.realm", kdcRealm);
        System.setProperty("java.security.krb5.kdc", kdcServer);
        System.setProperty("java.security.auth.login.config", "/dev/null");
        Configuration kbConfig = Configuration.getConfiguration();
        KerberosConfiguration kc = null;
        if (kbConfig instanceof KerberosConfiguration) {
            kc = (KerberosConfiguration) kbConfig;
            kc.setRefreshConfig("true");
            kc.setPrincipalName(config.getKerberosServicePrincipal());
            kc.setKeyTab(config.getKeyTabFile());
        } else {
            kc = new KerberosConfiguration(kbConfig);
            kc.setPrincipalName(config.getKerberosServicePrincipal());
            kc.setKeyTab(config.getKeyTabFile());
        }
        Configuration.setConfiguration(kc);

        // perform service authentication using JDK Kerberos module
        try {
            LoginContext lc = new LoginContext(
                KerberosConfiguration.WSP_CONFIGURATION);
            lc.login();
            return lc.getSubject();
        } catch (LoginException ex) {
            throw new SecurityException(ex.getMessage());
        }
    }
    
    /**
     * Authenticates assertion using WS-Trust
     * @param assertionE Assertion to be validated.
     * @param config provider config for the wsp
     * @return
     */
    private boolean authenticateAssertion(Element assertionE, 
            ProviderConfig config, Subject subject) throws SecurityException {
        
        try {
            String protocol = SystemConfigurationUtil.getProperty(
                    Constants.AM_SERVER_PROTOCOL);
            String host = SystemConfigurationUtil.getProperty(
                    Constants.AM_SERVER_HOST);
            String port = SystemConfigurationUtil.getProperty(
                    Constants.AM_SERVER_PORT);
            String deployURI = SystemConfigurationUtil.getProperty(
                    Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);           
            
            URL stsURL = WebtopNaming.getServiceURL("sts", protocol,
                    host, port, deployURI);
            URL stsMexURL = WebtopNaming.getServiceURL("sts-mex", protocol,
                    host, port, deployURI);
            TrustAuthorityClient taClient = new TrustAuthorityClient();
            String tokenConversionType = config.getTokenConversionType();
            String tokenType = tokenConversionType;
            if(tokenConversionType != null) {
               if(tokenConversionType.equals(SecurityToken.WSS_SAML_TOKEN)) {
                  tokenType =  STSConstants.SAML11_ASSERTION_TOKEN_TYPE;
               } else if(tokenConversionType.equals(
                       SecurityToken.WSS_SAML2_TOKEN)) {
                   tokenType =  STSConstants.SAML20_ASSERTION_TOKEN_TYPE;
               }
            }
            
            SecurityToken token = taClient.getSecurityToken(
                    config.getWSPEndpoint(),
                    stsURL.toString(), stsMexURL.toString(), assertionE, 
                    SecurityMechanism.STS_SECURITY_URI, 
                    tokenType,null);
            
            if(token.getTokenType().equals(SecurityToken.WSS_FAM_SSO_TOKEN)) {
               FAMSecurityToken famToken = (FAMSecurityToken)token;
               SSOToken ssoToken = SSOTokenManager.getInstance().
                       createSSOToken(famToken.getTokenID());
               addSSOToken(ssoToken, subject);
               subject = addPrincipal(ssoToken.getPrincipal().getName(), subject);
            } else  {
               subject.getPublicCredentials().add(token.toDocumentElement());
            }
            return true;
        } catch (FAMSTSException fae) {
            throw new SecurityException(fae.getMessage());
        } catch (SSOException se) {
            throw new SecurityException(se.getMessage());
        } catch (URLNotFoundException ure) {
            throw new SecurityException(ure.getMessage());
        }       
        
    }
    
    private boolean validateUserTokenTime(String created) {
        long tokentime = 0;
        try {
            tokentime = DateUtils.stringToDate(created).getTime();
        } catch (java.text.ParseException pe) {
            WSSUtils.debug.error("DefaultAuthenticator.validateUserToken"
                    + "Time: parse error", pe);
            return false;
        }
        //5sec for time skew
        long now = (new Date()).getTime() + WSSUtils.getTimeSkew();       
        if(now - tokentime >= 0) {
            return true; 
        }
        return false;
    }
    
    /**
     * Cache the nonce to avoid the replay attacks.
     * @param timestamp the timestamp of the user name token
     * @param nonce nonce of the user name token.
     * @throws com.sun.identity.wss.security.SecurityException
     */
    private void cacheNonce (String timeStamp, String nonce) 
             throws SecurityException {
        
        Set nonces = (Set)WSSCache.nonceCache.get(timeStamp);        
        WSSCacheRepository cacheRepo = WSSUtils.getWSSCacheRepository();
        if(nonces == null || nonces.isEmpty()) {
           if(cacheRepo != null) {
              nonces = cacheRepo.retrieveUserTokenNonce(timeStamp, 
                      config.getProviderName());
           }
        }       
        
        if(nonces == null || nonces.isEmpty()) {
              nonces = new HashSet();
              nonces.add(nonce);
              WSSCache.nonceCache.put(timeStamp, nonces);
              if(cacheRepo != null) { 
                 cacheRepo.saveUserTokenNonce(timeStamp, nonces, 
                         config.getProviderName());
              }
        } else {
              if(nonces.contains(nonce)) {
                 throw new SecurityException(WSSUtils.bundle.getString(
                         "replayAttackDetected")); 
              }
                          
              nonces.add(nonce);
              WSSCache.nonceCache.put(timeStamp, nonces);
              if(cacheRepo != null) {
                 cacheRepo.saveUserTokenNonce(timeStamp, nonces, 
                         config.getProviderName());
              }
        }
        
    }
}
