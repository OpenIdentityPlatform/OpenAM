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
 * $Id: SOAPRequestHandler.java,v 1.47 2010/01/15 18:54:34 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wss.security.handler;

import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPHeaderElement;
import javax.security.auth.Subject;
import java.security.Key;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.cert.X509Certificate;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ResourceBundle;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.Constants;
import com.iplanet.am.util.SystemProperties;

import com.sun.identity.liberty.ws.authnsvc.AuthnSvcClient;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcException;
import com.sun.identity.liberty.ws.authnsvc.protocol.SASLRequest;
import com.sun.identity.liberty.ws.authnsvc.protocol.SASLResponse;
import com.sun.identity.liberty.ws.disco.ResourceOffering;
import com.sun.identity.liberty.ws.security.SecurityAssertion;
import com.sun.identity.liberty.ws.soapbinding.SOAPBindingException;
import com.sun.identity.liberty.ws.soapbinding.SOAPBindingConstants;

import com.sun.identity.wss.security.SecurityException;
import com.sun.identity.wss.security.SecurityToken;
import com.sun.identity.wss.security.AssertionTokenSpec;
import com.sun.identity.wss.security.SAML2TokenSpec;
import com.sun.identity.wss.security.SecurityMechanism;
import com.sun.identity.wss.security.SecurityTokenFactory;
import com.sun.identity.wss.security.BinarySecurityToken;
import com.sun.identity.wss.security.X509TokenSpec;
import com.sun.identity.wss.security.KerberosTokenSpec;
import com.sun.identity.wss.security.UserNameTokenSpec;
import com.sun.identity.wss.security.WSSUtils;
import com.sun.identity.wss.security.PasswordCredential;
import com.sun.identity.wss.security.WSSConstants;
import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.provider.TrustAuthorityConfig;
import com.sun.identity.wss.provider.STSConfig;
import com.sun.identity.wss.provider.ProviderException;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.wss.sts.TrustAuthorityClient;
import com.sun.identity.wss.sts.FAMSTSException;
import com.sun.identity.wss.sts.config.STSRemoteConfig;
import com.sun.identity.wss.security.AssertionToken;
import com.sun.identity.wss.security.SAML2Token;
import com.sun.identity.wss.sts.STSConstants;
import com.sun.identity.wss.logging.LogUtil;
import com.iplanet.services.util.Crypt;
import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.xmlsig.KeyProvider;

/* iPlanet-PUBLIC-CLASS */

/**
 * This class <code>SOAPRequestHandler</code> is to process and secure the 
 * in-bound or out-bound  <code>SOAPMessage</code>s of the web service clients
 *  and web service providers. 
 *
 * <p> This class processes the <code>SOAPMessage</code>s for the
 * web services security according to the processing rules defined in
 * OASIS web services security specification and as well as the Liberty
 * Identity Web services security framework.
 *
 */
public class SOAPRequestHandler implements SOAPRequestHandlerInterface {

    private String providerName = null;
    private String PROVIDER_NAME = "providername";
    private static Debug debug = WSSUtils.debug;
    private static ResourceBundle bundle = WSSUtils.bundle;

    private static String BACK_SLASH = "\\";
    private static String FORWARD_SLASH = "/";
    private static MessageAuthenticator authenticator = null;
    private static MessageAuthorizer authorizer = null;
        
    /**
     * Property for web services authenticator.
     */
    private static final String WSS_AUTHENTICATOR =
            "com.sun.identity.wss.security.authenticator";

    private static final String WSS_AUTHORIZER =
            "com.sun.identity.wss.security.authorizer";
    
    /**
     * Property string for liberty authentication service url.
     */
    private static final String LIBERTY_AUTHN_URL =
            "com.sun.identity.liberty.authnsvc.url";
    
    private static final String MECHANISM_SSOTOKEN = "SSOTOKEN";

    /**
     * Property for the SAML issuer name.
     */
    private static final String ASSERTION_ISSUER = 
                   "com.sun.identity.wss.security.samlassertion.issuer";
    
    private static final String CLIENT_CERT = "AuthnSubjectCertificate";

    private static final String CLIENT_CERT_ALIAS = "AuthnClientCertAlias";

    /**
     * Initializes the handler with the given configuration. 
     *
     * @param config the configuration map to initializate the provider.
     *
     * @exception SecurityException if the initialization fails.
     */
    public void init(Map config) throws SecurityException {
        if(debug.messageEnabled()) {
            debug.message("SOAPRequestHandler.Init map:");
        }
        providerName = (String)config.get(PROVIDER_NAME);
    }

    /**
     * Authenticates the <code>SOAPMessage</code> from a remote client. 
     *
     * @param soapRequest SOAPMessage that needs to be validated.
     *
     * @param subject the subject that may be used by the callers
     *        to store Principals and credentials validated in the request.
     *
     * @param sharedState that may be used to store any shared state 
     *        information between <code>validateRequest and <secureResponse>
     *
     * @param request the <code>HttpServletRequest</code> associated with 
     *        this SOAP Message request.
     *
     * @param response the <code>HttpServletResponse</code> associated with
     *        this SOAP Message response. 
     *
     * @return Object the authenticated token.
     *
     * @exception SecurityException if any error occured during validation.
     */
    public Object validateRequest(SOAPMessage soapRequest,
            Subject subject,
            Map sharedState,
            HttpServletRequest request,
            HttpServletResponse response)
            throws SecurityException {

        ProviderConfig config = null;
        STSRemoteConfig stsConfig = null;
        if(debug.messageEnabled()) {
           debug.message("SOAPRequestHandler.validateRequest: " +
                   "Received SOAP message Before validation: "
                   + WSSUtils.print(soapRequest.getSOAPPart())); 
        }

        try {
            if (soapRequest.getSOAPPart().getEnvelope().getBody().hasFault()) {
                SOAPFault fault =
                    soapRequest.getSOAPPart().getEnvelope().getBody().getFault();
                String code = fault.getFaultCode();
                String errorString = fault.getFaultString();
                if(debug.messageEnabled()) {
                    debug.message("SOAPRequestHandler.validateRequest - " +
                        "SOAPFault code : " + code);
                    debug.message("SOAPRequestHandler.validateRequest - " +
                        "SOAPFault errorString : " + errorString);
                }
                throw new SecurityException(
                    bundle.getString("notAuthorizedByServer"));
            }
        } catch (SOAPException se) {
            throw new SecurityException(se.getMessage());
        }

        if ( sharedState == null || sharedState.isEmpty() ) {
            sharedState = new HashMap();
        }

        if (LogUtil.isLogEnabled()) {
            String[] data = {WSSUtils.print(soapRequest.getSOAPPart())};
            LogUtil.access(Level.FINE,
                LogUtil.REQUEST_TO_BE_VALIDATED,
                data,
                null);
        
        }
        Boolean isTrustMessage = (Boolean) sharedState.get("IS_TRUST_MSG");
        boolean isSTS = 
            (isTrustMessage != null) ? isTrustMessage.booleanValue(): false;
        
        if (isSTS) {
            debug.message("ValidateRequest: This is WS-Trust Request");
            stsConfig = new STSRemoteConfig();
            config = getSTSProviderConfig(stsConfig);
        } else {
            config = getWSPConfig();
        }

        if(isLibertyMessage(soapRequest)) {
            if(debug.messageEnabled()) {
                debug.message("SOAPRequestHandler.validateRequest:: Incoming " +
                        "SOAPMessage is of liberty message type.");
            }
            MessageProcessor processor = new MessageProcessor(config);
            try {
                processor.validateRequest(soapRequest, subject,
                        sharedState, request);
                removeValidatedHeaders(config, soapRequest);
                return subject;
            } catch (SOAPBindingException sbe) {
                debug.error("SOAPRequestHandler.validateRequest:: SOAP" +
                        "BindingException:: ", sbe);
                throw new SecurityException(sbe.getMessage());
            }
        }

        SecureSOAPMessage secureMsg =
                new SecureSOAPMessage(soapRequest, false);
        SecurityContext securityContext = new SecurityContext();
        securityContext.setDecryptionAlias(config.getKeyAlias());
        String dnsClaim = secureMsg.getClientDnsClaim();
        ProviderConfig remoteProvider = null;
        if(dnsClaim != null) {
           remoteProvider = WSSUtils.getConfigByDnsClaim(
                            dnsClaim, ProviderConfig.WSC);
           if(remoteProvider != null) {
              String pubKeyAlias = remoteProvider.getKeyAlias();
              securityContext.setVerificationCertAlias(pubKeyAlias);
              sharedState.put(CLIENT_CERT_ALIAS, pubKeyAlias);
           }
        } else {           
           securityContext.setVerificationCertAlias(config.getPublicKeyAlias());
        }
        secureMsg.setSecurityContext(securityContext);

        if ( ((config != null) && ((config.isRequestEncryptEnabled()) ||
                (config.isRequestHeaderEncryptEnabled()))) ) {
            secureMsg.decrypt(config.getKeyAlias(),
                    (config.isRequestEncryptEnabled()),
                    (config.isRequestHeaderEncryptEnabled()));                    
            soapRequest = secureMsg.getSOAPMessage();
            secureMsg = new SecureSOAPMessage(soapRequest, false);
            secureMsg.setSecurityContext(securityContext);
        }
        
        secureMsg.parseSecurityHeader(
                    (Node)secureMsg.getSecurityHeaderElement());
        
        String msgID = secureMsg.getMessageID();
        if(msgID != null && config.isMessageReplayDetectionEnabled()) {
           if(checkForReplay(msgID, config.getProviderName())) {
              throw new SecurityException(
                      bundle.getString("replayAttackDetected")); 
           }  
        }
        
        SecurityMechanism securityMechanism =
                secureMsg.getSecurityMechanism();
        String uri = securityMechanism.getURI();
        if(debug.messageEnabled()) {
           debug.message("SOAPRequestHandler.validateRequest: " +
                   "soap message security mechanism: " + uri);
        }
        
        if ( ((config != null) && (config.isRequestSignEnabled())) ){
            // delay the signature verification after authentication
            // for kerberos token.
            if(!uri.equals(SecurityMechanism.WSS_NULL_KERBEROS_TOKEN_URI)) {               
               if (!secureMsg.verifySignature()) {
                   if(debug.warningEnabled()) {
                      debug.warning("SOAPRequestHandler.validateRequest:: " +
                              "Signature verification failed.");
                   }
                   throw new SecurityException(
                        bundle.getString("signatureValidationFailed"));
               } else {
                 if(debug.messageEnabled()) {
                    debug.message("SOAPRequestHandler.validateRequest: " +
                            "Signature verification successful"); 
                 }  
               }
            }
        }

        List list = null;
        if (config != null) {
            list = config.getSecurityMechanisms();
        }

        if(debug.messageEnabled()) {
            debug.message("SOAPRequestHandler.validateRequest: " +
                    "list of accepted SecurityMechanisms : " + list);            
        }

        if(!list.contains(uri)) {
           if( (!list.contains(
                    SecurityMechanism.WSS_NULL_ANONYMOUS_URI)) &&
                    (!list.contains(
                SecurityMechanism.WSS_TLS_ANONYMOUS_URI))&&
                    (!list.contains(
                    SecurityMechanism.WSS_CLIENT_TLS_ANONYMOUS_URI))) {
                if(debug.warningEnabled()) {
                   debug.warning("SOAPRequestHandler.validateRequest: "
                        + "unsupported security mechanism");
                }
                throw new SecurityException(
                        bundle.getString("unsupportedSecurityMechanism"));
            } else {
              if(debug.messageEnabled()) {
                    debug.message("SOAPRequestHandler.validateRequest:: " +
                            "provider is not configured for the incoming message " +
                            " level type but allows anonymous");
                }
                return subject;
            }
        }

        if(SecurityMechanism.WSS_NULL_ANONYMOUS_URI.equals(uri) ||
                (SecurityMechanism.WSS_TLS_ANONYMOUS_URI.equals(uri)) ||
                (SecurityMechanism.WSS_CLIENT_TLS_ANONYMOUS_URI.equals(uri))) {
            return subject;
        }
        
        subject = (Subject) getAuthenticator().authenticate(subject,
                secureMsg.getSecurityMechanism(),
                secureMsg.getSecurityToken(),
                config, secureMsg, false);
        
        if(msgID == null && config.isMessageReplayDetectionEnabled()) {
           if(checkForReplay(subject, secureMsg.getMessageTimestamp(),
                   config.getProviderName())) {
              throw new SecurityException(
                      bundle.getString("replayAttackDetected")); 
           }
        }
        
        if(uri.equals(SecurityMechanism.WSS_NULL_KERBEROS_TOKEN_URI) &&
                (config.isValidateKerberosSignature())){
                
           java.security.Key secretKey = null;
           Iterator iter = subject.getPublicCredentials().iterator();
           while(iter.hasNext()) {
               Object obj = iter.next();
               if (obj instanceof java.security.Key) {
                   secretKey = (java.security.Key)obj;
                   break;
               }
           }                      
           
           if (!secureMsg.verifyKerberosTokenSignature(secretKey)) {
               debug.error("SOAPRequestHandler.validateRequest::Signature"
                           + "verification failed.");
               throw new SecurityException(
                     bundle.getString("signatureValidationFailed"));
           }                         
        }
        
        removeValidatedHeaders(config, soapRequest);
        if(!isSTS) {
            ThreadLocalService.setSubject(subject);
        }
        if(debug.messageEnabled()) {
            debug.message("SOAPRequestHandler.validateRequest:** SOAP message" +
                    " at the end of Validate request **"); 
            debug.message(WSSUtils.print(soapRequest.getSOAPPart()));
        }
        
        if (LogUtil.isLogEnabled()) {
            String[] data2 = {providerName,uri};
            LogUtil.access(Level.INFO,
                LogUtil.SUCCESS_VALIDATE_REQUEST,
                data2,
                null);
        }

        if(!getAuthorizer().authorize(subject,
                secureMsg,
                secureMsg.getSecurityMechanism(),
                secureMsg.getSecurityToken(),
                config, false)) {
           if(debug.messageEnabled()) {
              debug.message("SOAPRequestHandler.validateRequest: "  +
                      " Unauthorized. "); 
           }
           throw new SecurityException(bundle.getString("notAuthorized"));
        }

        updateSharedState(subject, sharedState);
            return subject;    
     }
    /**
     * Secures the SOAP Message response to the client.
     *
     * @param soapMessage SOAP Message that needs to be secured.
     *
     * @param sharedState a map for the callers to store any state information
     *        between <code>validateRequest</code> and 
     *        <code>secureResponse</code>.
     *
     * @exception SecurityException if any error occurs during securing. 
     */
    public SOAPMessage secureResponse (SOAPMessage soapMessage, 
            Map sharedState) throws SecurityException {

        if(debug.messageEnabled()) {            
            debug.message("SOAPRequestHandler.secureResponse - " + 
                "Input SOAP message before securing : " +
                WSSUtils.print(soapMessage.getSOAPPart()));
        }

        try {
            if (soapMessage.getSOAPPart().getEnvelope().getBody().hasFault()) {
                SOAPFault fault =
                    soapMessage.getSOAPPart().getEnvelope().getBody().getFault();
                String code = fault.getFaultCode();
                String errorString = fault.getFaultString();
                if(debug.messageEnabled()) {
                    debug.message("SOAPRequestHandler.secureResponse - " +
                        "SOAPFault code : " + code);
                    debug.message("SOAPRequestHandler.secureResponse - " +
                        "SOAPFault errorString : " + errorString);
                }
                throw new SecurityException(
                    bundle.getString("notAuthorizedByServer"));
            }
        } catch (SOAPException se) {
            throw new SecurityException(se.getMessage());
        }

        if ( sharedState == null || sharedState.isEmpty() ) {
            sharedState = new HashMap();
        }
        
        if (LogUtil.isLogEnabled()) {
            String[] data = {WSSUtils.print(soapMessage.getSOAPPart())};
            LogUtil.access(Level.FINE,
                   LogUtil.RESPONSE_TO_BE_SECURED,
                    data,
                    null);
        }
        ProviderConfig config = null;
        STSRemoteConfig stsConfig = null;
        
        Boolean isTrustMessage = (Boolean) sharedState.get("IS_TRUST_MSG");
        boolean isSTS = 
            (isTrustMessage != null) ? isTrustMessage.booleanValue(): false;
        
        if (isSTS) {
            debug.message("SecureResponse: This is WS-Trust Response");
            stsConfig = new STSRemoteConfig();
            config = getSTSProviderConfig(stsConfig);
        } else {
            ThreadLocalService.removeSubject();
            config = getWSPConfig();
        }

        Object req = sharedState.get(SOAPBindingConstants.LIBERTY_REQUEST);
        if(req != null) {
            if(debug.messageEnabled()) {
               debug.message("SOAPRequestHandler.secureResponse: liberty req:"); 
            }
            MessageProcessor processor = new MessageProcessor(config);
            try {
                return processor.secureResponse(soapMessage, sharedState);
            } catch (SOAPBindingException sbe) {
                debug.error("SOAPRequestHandler.secureResponse:: SOAP" +
                        "BindingException.", sbe);
                throw new SecurityException(sbe.getMessage());
            }
        }
        SecurityContext securityContext = new SecurityContext();
        SecureSOAPMessage secureMessage =   new SecureSOAPMessage(soapMessage, 
                true, config.getSignedElements());
        try {
            secureMessage.getSOAPMessage().saveChanges();
        } catch (SOAPException se) {
            throw new SecurityException(se.getMessage());
        }

        if ( ((config != null) && 
            (!config.isResponseSignEnabled() && 
            !config.isResponseEncryptEnabled())) ){
            return soapMessage;
        }

        SSOToken token = WSSUtils.getAdminToken();
        SecurityTokenFactory factory = SecurityTokenFactory.getInstance(token);

        String keyAlias = SystemConfigurationUtil.getProperty(
                Constants.SAML_XMLSIG_CERT_ALIAS);
        String publicKeyAlias = null;
        if (config != null) {
            keyAlias = config.getKeyAlias();
            publicKeyAlias =config.getPublicKeyAlias();
        } else if (stsConfig != null) {
            keyAlias = stsConfig.getPrivateKeyAlias();
            publicKeyAlias = stsConfig.getPublicKeyAlias();
        }
        
        securityContext.setSigningCertAlias(keyAlias);
        securityContext.setSigningRef(config.getSigningRefType());
        String[] certAlias = {keyAlias};
        X509TokenSpec tokenSpec = new X509TokenSpec(certAlias,
                BinarySecurityToken.X509V3,
                BinarySecurityToken.BASE64BINARY);

        SecurityToken securityToken =
                factory.getSecurityToken(tokenSpec);

        secureMessage.setSecurityContext(securityContext);
        secureMessage.setSecurityToken(securityToken);
        secureMessage.setSecurityMechanism(
                SecurityMechanism.WSS_NULL_X509_TOKEN);
        secureMessage.setSignedElements(config.getSignedElements());

        if ( (config != null && config.isResponseSignEnabled()) ){            
            secureMessage.sign();
        }

        if ( (config != null && config.isResponseEncryptEnabled()) ) {                     
            KeyProvider keyProvider = 
                     WSSUtils.getXMLSignatureManager().getKeyProvider();
            X509Certificate cert = 
                    (X509Certificate)sharedState.get(CLIENT_CERT);
            String encryptAlias = publicKeyAlias;
            if(cert != null) {
               encryptAlias =  keyProvider.getCertificateAlias(cert);
            } else {
               String clientCertAlias = (String)sharedState.get(
                                         CLIENT_CERT_ALIAS);
               if(clientCertAlias != null) {
                  encryptAlias = clientCertAlias;
               }
            }
                    
            secureMessage.encrypt(encryptAlias, 
                    config.getEncryptionAlgorithm(),
                    config.getEncryptionStrength(),true,false);
        }

        soapMessage = secureMessage.getSOAPMessage();
        
        if (LogUtil.isLogEnabled()) {
            String[] data2 = {providerName};
            LogUtil.access(Level.INFO,
                    LogUtil.SUCCESS_SECURE_RESPONSE,
                    data2,
                    null);
        }
        if(debug.messageEnabled()) {            
            debug.message("SOAPRequestHandler.secureResponse - " + 
                "Secured SOAP response : " + 
                WSSUtils.print(soapMessage.getSOAPPart()));
        }
        return soapMessage;

    }

    /**
     * Secures the <code>SOAPMessage</code> request by adding necessary
     * credential information.
     *
     * @param soapMessage the <code>SOAPMessage</code> that needs to be secured.
     *
     * @param subject  the <code>Subject<code> of the authenticating entity.
     *
     * @param sharedState Any shared state information that may be used between
     *        the <code>secureRequest</code> and <code>validateResponse</code>. 
     *
     * @exception SecurityException if any failure for securing the request.
     */
    public SOAPMessage secureRequest (
            SOAPMessage soapMessage,
            Subject subject,
            Map sharedState) throws SecurityException {
       
        if (LogUtil.isLogEnabled()) {
            String[] data = {WSSUtils.print(soapMessage.getSOAPPart())};
            LogUtil.access(Level.FINE,
                    LogUtil.REQUEST_TO_BE_SECURED,
                    data,
                    null);
        }
        ProviderConfig config = getProviderConfig(sharedState);
        if(config == null) {
           if(WSSUtils.debug.messageEnabled()) {
              WSSUtils.debug.message("SOAPRequestHandler.secureRequest: "+
                        "Provider configuration from shared map is null");
            }
            config = getWSCConfig();
        }

        SecurityToken securityToken = null;
        List secMechs = config.getSecurityMechanisms();
        if(secMechs == null || secMechs.isEmpty()) {
            throw new SecurityException(
                    bundle.getString("securityMechNotConfigured"));
        }

        String sechMech = (String)secMechs.iterator().next();
        SecurityMechanism securityMechanism =
                SecurityMechanism.getSecurityMechanism(sechMech);
        
        SecurityContext securityContext = new SecurityContext();
        String keyAlias = config.getKeyAlias();
        if (keyAlias == null) {
            keyAlias = SystemConfigurationUtil.getProperty(
                Constants.SAML_XMLSIG_CERT_ALIAS);
        }
        
        if(keyAlias != null) {
           securityContext.setSigningCertAlias(keyAlias);
           securityContext.setKeyType(SecurityContext.ASYMMETRIC_KEY);
        }
        securityContext.setEncryptionKeyAlias(config.getPublicKeyAlias());
        securityContext.setSigningRef(config.getSigningRefType());
        SecureSOAPMessage secureMessage = null;
        String uri = securityMechanism.getURI();
        if (((SecurityMechanism.WSS_NULL_ANONYMOUS_URI.equals(uri)) ||
                (SecurityMechanism.WSS_TLS_ANONYMOUS_URI.equals(uri)) ||
                (SecurityMechanism.WSS_CLIENT_TLS_ANONYMOUS_URI.equals(uri)))) {
            secureMessage = new SecureSOAPMessage(soapMessage, true,
                    config.getSignedElements());
        } else {
            if (securityMechanism.isTALookupRequired()) {
                if(debug.messageEnabled()) {
                   debug.message("SOAPRequestHandler.secureRequest :"
                           + "using STS for security tokens"); 
                }
                if(config.usePassThroughSecurityToken()) {   
                   Subject authnSubj = getAuthenticatedSubject();
                   if(authnSubj != null) {
                      if(debug.messageEnabled()) {
                         debug.message("SOAPRequestHandler.secureRequest :" +
                              " using the authenticated subject"); 
                      }
                      subject = authnSubj;
                   }
                }
                SubjectSecurity subjectSecurity = getSubjectSecurity(subject);
                SSOToken ssoToken = subjectSecurity.ssoToken;
                if(ssoToken == null) {
                   if(debug.messageEnabled()) {
                      debug.message("SOAPRequestHandler.secureRequest:: " +
                              "using thread local for SSOToken");
                   }
                   ssoToken = (SSOToken)ThreadLocalService.getSSOToken(); 
                }
                if(debug.messageEnabled() && ssoToken != null) {
                   debug.message("SOAPequestHandler.secureRequest: ssoToken " +
                           "is available. ");   
                }
                if(securityMechanism.getURI().equals
                     (SecurityMechanism.LIBERTY_DS_SECURITY_URI)) {
                   if(ssoToken == null) {
                      throw new SecurityException(
                                bundle.getString("invalidSSOToken"));
                   }
                   if(debug.messageEnabled()) {
                      debug.message("SOAPRequestHandler.secureRequest: "
                              + " using liberty security"); 
                   }
                   return getSecureMessageFromLiberty(ssoToken, subject,
                            soapMessage, sharedState, config);
                } else {
                   try {
                        TrustAuthorityClient client = new TrustAuthorityClient();
                        TrustAuthorityConfig taconfig =
                                config.getTrustAuthorityConfig();
                        String taName = taconfig.getName();
                        if(taName != null) {
                            ThreadLocalService.setServiceName(taName);
                        }
                        Object customToken = getCustomCredential(subject);
                        if(customToken == null) {
                           if(debug.messageEnabled()) {
                              debug.message("SOAPRequestHandler.secureRequest:"
                                      + " using sso token as OBOToken"); 
                           }
                           securityToken = client.getSecurityToken(config,
                                        ssoToken);
                        } else {
                           if(debug.messageEnabled()) {
                              debug.message("SOAPRequestHandler.secureRequest:"
                                      + " using custom token as OBOToken"); 
                           }
                           securityToken = client.getSecurityToken(config,
                                        customToken); 
                        }
                        Key signingKey = client.getSecretKey();
                        if(signingKey != null) {
                           securityContext.setSigningKey(client.getSecretKey());
                           securityContext.setKeyType(
                                   SecurityContext.SYMMETRIC_KEY);
                        }
                    } catch (FAMSTSException stsEx) {
                        debug.error("SOAPRequestHandler.secureRequest: exception" +
                                "in obtaining STS Token", stsEx);
                        throw new SecurityException(stsEx.getMessage());
                    }
                }
            
            } else {
                if(debug.messageEnabled()) {
                   debug.message("SOAPRequestHandler.secureRequest: " +
                           " Generate security tokens locally"); 
                }
                securityToken = getSecurityToken(
                        securityMechanism, config, subject);
            }
            
            secureMessage =  new SecureSOAPMessage(soapMessage, true,
                    config.getSignedElements());
            String dnsClaim = config.getDNSClaim();
            if(dnsClaim != null) {
               secureMessage.setSenderIdentity(dnsClaim);
            }
            String refType = config.getSigningRefType();
            if(!(securityMechanism.getURI().equals(
                    SecurityMechanism.WSS_NULL_X509_TOKEN_URI)) ||
                    (refType == null) ||
                    WSSConstants.DIRECT_REFERENCE.equals(refType)) {
               secureMessage.setSecurityToken(securityToken);
            }
        }

        secureMessage.setSecurityMechanism(securityMechanism);        
        secureMessage.setSecurityContext(securityContext);
         
        if(config.isRequestSignEnabled()) {            
           secureMessage.sign();
        }

        if((config.isRequestEncryptEnabled()) || 
                (config.isRequestHeaderEncryptEnabled())) {
            secureMessage.encrypt(config.getPublicKeyAlias(),
                    config.getEncryptionAlgorithm(),
                    config.getEncryptionStrength(),
                    (config.isRequestEncryptEnabled()),
                    (config.isRequestHeaderEncryptEnabled()));
        }

        soapMessage = secureMessage.getSOAPMessage();

        if (LogUtil.isLogEnabled()) {
            String[] data2 = {providerName,uri};
            LogUtil.access(Level.INFO,
                    LogUtil.SUCCESS_SECURE_REQUEST,
                    data2,
                    null);
        }
        if(debug.messageEnabled()) {
           debug.message("SOAPRequestHandler.secureRequest:  SOAP message" +
              " after securing: " + WSSUtils.print(soapMessage.getSOAPPart())); 
        }
        return soapMessage;
    }

    /**
     * Validates the SOAP Response from the service provider. 
     *
     * @param soapMessage the <code>SOAPMessage</code> that needs to be 
     *        validated.
     *
     * @param sharedState Any shared data that may be used between the
     *        <code>secureRequest</code> and <code>validateResponse</code>.
     *
     * @exception SecurityException if any failure occured for validating the
     *            response.
     */
    public void validateResponse (SOAPMessage soapMessage, 
            Map sharedState) throws SecurityException {

        if(debug.messageEnabled()) {      
            debug.message("SOAPRequestHandler.validateResponse - " + 
                "Input SOAP message : " + 
                WSSUtils.print(soapMessage.getSOAPPart()));
        }

        try {
            if (soapMessage.getSOAPPart().getEnvelope().getBody().hasFault()) {
                SOAPFault fault =
                    soapMessage.getSOAPPart().getEnvelope().getBody().getFault();
                String code = fault.getFaultCode();
                String errorString = fault.getFaultString();
                if(debug.messageEnabled()) {
                    debug.message("SOAPRequestHandler.validateResponse - " +
                        "SOAPFault code : " + code);
                    debug.message("SOAPRequestHandler.validateResponse - " +
                        "SOAPFault errorString : " + errorString);
                }
                throw new SecurityException(
                    bundle.getString("notAuthorizedByServer"));
            }
        } catch (SOAPException se) {
            throw new SecurityException(se.getMessage());
        }
        
        if (LogUtil.isLogEnabled()) {
            String[] data = {WSSUtils.print(soapMessage.getSOAPPart())};
            LogUtil.access(Level.FINE,
                    LogUtil.RESPONSE_TO_BE_VALIDATED,
                    data,
                    null);
        }
        
        ProviderConfig config = getProviderConfig(sharedState);
        if(config == null) {
           if(WSSUtils.debug.messageEnabled()) {
              WSSUtils.debug.message("SOAPRequestHandler.validateResponse: "+
                        "Provider configuration from shared map is null");
            }
            config = getWSCConfig();
        }
        if(isLibertyMessage(soapMessage)) {
            MessageProcessor processor = new MessageProcessor(config);
            try {
                processor.validateResponse(soapMessage, sharedState);
                removeValidatedHeaders(config, soapMessage);
                return;
            } catch (SOAPBindingException sbe) {
                debug.error("SOAPRequestHandler.validateResponse:: SOAP" +
                        "BindingException. ", sbe);
                throw new SecurityException(sbe.getMessage());
            }
        }

        if (config.isResponseEncryptEnabled() 
            || config.isResponseSignEnabled()) {
            SecureSOAPMessage secureMessage =
                new SecureSOAPMessage(soapMessage, false);
            SecurityContext securityContext = new SecurityContext();
            securityContext.setDecryptionAlias(config.getKeyAlias());
            securityContext.setVerificationCertAlias(
                    config.getPublicKeyAlias());
            if(config.isResponseEncryptEnabled()) {
                secureMessage.decrypt(config.getKeyAlias(),
                        config.isResponseEncryptEnabled(), false);
                soapMessage = secureMessage.getSOAPMessage();
            }

            secureMessage.parseSecurityHeader(
                (Node)(secureMessage.getSecurityHeaderElement()));
            secureMessage.setSecurityContext(securityContext);
            
            if(config.isResponseSignEnabled()) {           
                if(!secureMessage.verifySignature()) {
                debug.error("SOAPRequestHandler.validateResponse:: Signature" +
                        " Verification failed");
                throw new SecurityException(
                        bundle.getString("signatureValidationFailed"));
                }
            }
        }
        removeValidatedHeaders(config, soapMessage);
        
        if (LogUtil.isLogEnabled()) {
            String[] data2 = {providerName};
            LogUtil.access(Level.INFO,
                    LogUtil.SUCCESS_VALIDATE_RESPONSE,
                    data2,
                    null);
        }
        if(debug.messageEnabled()) {      
            debug.message("SOAPRequestHandler.validateResponse - " + 
                "SOAP message after validation : " + 
                WSSUtils.print(soapMessage.getSOAPPart()));
        }
    }

    /**
     * Initialize the system properties before the SAML module is invoked.
     */
    private void initializeSystemProperties(ProviderConfig config)
            throws IOException {

        String keyStoreFile = config.getKeyStoreFile();
        String ksPasswd = config.getKeyStoreEncryptedPasswd();
        String keyPasswd = config.getKeyEncryptedPassword();
        String certAlias = config.getKeyAlias();

        if(keyStoreFile == null || ksPasswd == null) {
           if(debug.messageEnabled()) {
                debug.message("SOAPRequestHandler.initSystemProperties:: " +
                "Provider config does not have keystore information. Will " +
                        "fallback to the default configuration in AMConfig.");
            }
            return;
        }

        if(keyStoreFile.indexOf(BACK_SLASH) != -1) {
            keyStoreFile.replaceAll(BACK_SLASH, FORWARD_SLASH);
        }

        int index = keyStoreFile.lastIndexOf(FORWARD_SLASH);
        String storePassFile =
                keyStoreFile.substring(0, index) + "/.storepassfile";
        String keyPassFile = keyStoreFile.substring(0, index) + "/.keypassfile";

        if(debug.messageEnabled()) {
            debug.message("SOAPRequestHandler.initSystemProperties:: " +
           "\n" +  "KeyStoreFile: " + keyStoreFile + "\n" +
           "Encrypted keystore password: " + ksPasswd + "\n" +
           "Encrypted key password: " + keyPasswd + "\n" +
           "Location of the store encrypted password: " + storePassFile + "\n"+
                    "Location of the key encrypted password: " + keyPassFile);
        }

        if(keyPasswd == null) {
            keyPasswd = ksPasswd;
        }
        FileOutputStream out = new FileOutputStream(new File(keyPassFile));
        out.write(keyPasswd.getBytes());
        out.flush();
        FileOutputStream out1 = new FileOutputStream(new File(storePassFile));
        out1.write(ksPasswd.getBytes());
        out1.flush();

        SystemProperties.initializeProperties(
                Constants.SAML_XMLSIG_KEYSTORE, keyStoreFile);
        SystemProperties.initializeProperties(
                Constants.SAML_XMLSIG_STORE_PASS, storePassFile);
        SystemProperties.initializeProperties(
                Constants.SAML_XMLSIG_KEYPASS, keyPassFile);
        SystemProperties.initializeProperties(
                Constants.SAML_XMLSIG_CERT_ALIAS, certAlias);
    }

    private ProviderConfig getWSPConfig() throws SecurityException {

        ProviderConfig config = null;
        if( (providerName == null) || (providerName.length() == 0) ) {
            providerName = SystemConfigurationUtil.getProperty(
                    "com.sun.identity.wss.provider.defaultWSP", "wsp");
            if(debug.messageEnabled()) {
               debug.message("SOAPRequestHandler.getWSPConfig: " +
                       "default provider name:" + providerName);
            }
        }
        try {
            if (!ProviderConfig.isProviderExists(
                    providerName, ProviderConfig.WSP)) {
                config = ProviderConfig.getProviderByEndpoint(
                        providerName, ProviderConfig.WSP);
                if (!ProviderConfig.isProviderExists(
                    providerName, ProviderConfig.WSP, true)) {
                    providerName = SystemConfigurationUtil.getProperty(
                        "com.sun.identity.wss.provider.defaultWSP", "wsp");
                    config = ProviderConfig.getProvider(providerName,
                        ProviderConfig.WSP);
                }
            } else {
                config = ProviderConfig.getProvider(providerName,
                    ProviderConfig.WSP);
            }
            if(!config.useDefaultKeyStore()) {
                initializeSystemProperties(config);
            }

        } catch (ProviderException pe) {
            debug.error("SOAPRequestHandler.getWSPConfig:: Provider" +
                    " configuration read failure", pe);
            throw new SecurityException(
                    bundle.getString("cannotInitializeProvider"));

        } catch (IOException ie) {
            debug.error("SOAPRequestHandler.getWSPConfig:: Provider" +
                    " configuration read failure", ie);
            throw new SecurityException(
                    bundle.getString("cannotInitializeProvider"));
        }
        return config;
    }

    private ProviderConfig getWSCConfig() throws SecurityException {

        ProviderConfig config = null;
        try {
            if (!ProviderConfig.isProviderExists(
                    providerName, ProviderConfig.WSC)) {
                providerName = SystemConfigurationUtil.getProperty(
                    "com.sun.identity.wss.provider.defaultWSC", "wsc");
                config = ProviderConfig.getProvider(providerName,
                    ProviderConfig.WSC);
            } else {
                config = ProviderConfig.getProvider(providerName,
                    ProviderConfig.WSC);
            }        
            if(!config.useDefaultKeyStore()) {
                initializeSystemProperties(config);
            }

        } catch (ProviderException pe) {
            debug.error("SOAPRequestHandler.getWSCConfig:: Provider" +
                    " configuration read failure", pe);
            throw new SecurityException(
                    bundle.getString("cannotInitializeProvider"));

        } catch (IOException ie) {
            debug.error("SOAPRequestHandler.getWSCConfig:: Provider" +
                    " configuration read failure", ie);
            throw new SecurityException(
                    bundle.getString("cannotInitializeProvider"));
        }
        return config;
    }

    /**
     * Returns the security token for the configured security mechanism.
     */
    private SecurityToken getSecurityToken(
            SecurityMechanism secMech,
            ProviderConfig config,
            Subject subject) throws SecurityException {

        String uri = secMech.getURI();
        String certAlias = SystemConfigurationUtil.getProperty(
                Constants.SAML_XMLSIG_CERT_ALIAS);
        if(!config.useDefaultKeyStore()) {
            certAlias = config.getKeyAlias();
        }
        SecurityToken securityToken = null;

        if(debug.messageEnabled()) {
            debug.message("getSecurityToken: SecurityMechanism URI : " + uri);
        }

        SSOToken token = WSSUtils.getAdminToken();

        SecurityTokenFactory factory = SecurityTokenFactory.getInstance(token);
        //remove
        //uri = SecurityMechanism.WSS_NULL_KERBEROS_TOKEN_URI;
        if(SecurityMechanism.WSS_NULL_X509_TOKEN_URI.equals(uri) ||
           SecurityMechanism.WSS_TLS_X509_TOKEN_URI.equals(uri) ||
           SecurityMechanism.WSS_CLIENT_TLS_X509_TOKEN_URI.equals(uri)) {

           if(debug.messageEnabled()) {
                debug.message("SOAPRequestHandler.getSecurityToken:: creating " +
                        "X509 token");
            }
            String[] aliases = {certAlias};
            X509TokenSpec tokenSpec = new X509TokenSpec(
                    aliases, BinarySecurityToken.X509V3,
                    BinarySecurityToken.BASE64BINARY);
           securityToken = factory.getSecurityToken(tokenSpec);

        } else if(
           (SecurityMechanism.WSS_NULL_SAML_HK_URI.equals(uri)) ||
                (SecurityMechanism.WSS_TLS_SAML_HK_URI.equals(uri)) ||
                (SecurityMechanism.WSS_CLIENT_TLS_SAML_HK_URI.equals(uri)) ||
                (SecurityMechanism.WSS_NULL_SAML_SV_URI.equals(uri)) ||
                (SecurityMechanism.WSS_TLS_SAML_SV_URI.equals(uri)) ||
           (SecurityMechanism.WSS_CLIENT_TLS_SAML_SV_URI.equals(uri)) ) {

           if(debug.messageEnabled()) {
                debug.message("SOAPRequestHandler.getSecurityToken:: creating " +
                        "SAML token");
            }
            if(config.usePassThroughSecurityToken()) {
               securityToken = getSecurityTokenFromSubject(subject);
            }
            if(securityToken != null) {
                if(debug.messageEnabled()) {                  
                  debug.message("SOAPRequestHandler.getSecurityToken::" +
                       "security token from subject is not null"); 
               }
               return securityToken;
            }
            String issuer = SystemConfigurationUtil.getProperty(
                            ASSERTION_ISSUER);
            NameIdentifier ni = null;
            Map samlAttributes = new HashMap();
            try {
                SubjectSecurity subjectSecurity = getSubjectSecurity(subject);
                SSOToken userToken = subjectSecurity.ssoToken;
                if(userToken == null) {
                   if(debug.messageEnabled()) {
                      debug.message("SOAPRequestHandler.getSecurityToken:: " +
                              "using thread local for SSOToken");
                   }
                   userToken = (SSOToken)ThreadLocalService.getSSOToken();
                }
                if(userToken != null) {
                   String subjectName = userToken.getPrincipal().getName();
                   String nameIDImpl = config.getNameIDMapper();
                   if(nameIDImpl == null || nameIDImpl.length() == 0) {
                      ni = new NameIdentifier(subjectName); 
                   } else {
                      ni = new NameIdentifier(
                           WSSUtils.getUserPseduoName(subjectName, nameIDImpl));
                   }                                               
                   Map attributes = WSSUtils.getSAMLAttributes(
                        subjectName, config.getSAMLAttributeMapping(),
                        config.getSAMLAttributeNamespace(), userToken);
                
                   if(attributes != null) {
                      samlAttributes.putAll(attributes); 
                   }
                
                   if(config.shouldIncludeMemberships()) {
                      Map memberships = WSSUtils.getMembershipAttributes(
                          subjectName, config.getSAMLAttributeNamespace());
                      if(memberships != null) {
                          samlAttributes.putAll(memberships);
                      }
                    }
                } else {
                    if(issuer != null && issuer.length() != 0) {
                       ni = new NameIdentifier(issuer); 
                    } else {
                       ni = new NameIdentifier(
                            SystemConfigurationUtil.getProperty(
                            Constants.AM_SERVER_HOST));                        
                    }
                }
                
            } catch (Exception ex) {
                debug.error("SOAPRequestHandler.getSecurityToken: " +
                        "Failed in creating SAML tokens", ex);
                throw new SecurityException(ex.getMessage());
            }
                        
            AssertionTokenSpec tokenSpec = new AssertionTokenSpec(ni,
                    secMech, certAlias);
           
            if(issuer != null && issuer.length() !=0 ) {
               tokenSpec.setIssuer(issuer);
            }
            if(!samlAttributes.isEmpty()) {
               tokenSpec.setClaimedAttributes(samlAttributes); 
            }
            tokenSpec.setSigningAlias(certAlias);
            String appliesTo = config.getWSPEndpoint();
            if(appliesTo != null) {
               tokenSpec.setAppliesTo(appliesTo);
            }
            tokenSpec.setAssertionID(SAMLUtilsCommon.generateID());
            securityToken = factory.getSecurityToken(tokenSpec); 

        } else if(
            (SecurityMechanism.WSS_NULL_USERNAME_TOKEN_URI.equals(uri)) ||
                (SecurityMechanism.WSS_TLS_USERNAME_TOKEN_URI.equals(uri)) ||
            (SecurityMechanism.WSS_CLIENT_TLS_USERNAME_TOKEN_URI.equals(uri))
            || (SecurityMechanism.WSS_NULL_USERNAME_TOKEN_PLAIN_URI.equals(uri)) 
            || (SecurityMechanism.WSS_TLS_USERNAME_TOKEN_PLAIN_URI.equals(uri)) 
            || (SecurityMechanism.WSS_CLIENT_TLS_USERNAME_TOKEN_PLAIN_URI.
                equals(uri))){

            if(debug.messageEnabled()) {
                debug.message("SOAPRequestHandler.getSecurityToken:: creating " +
                        "UserName token");
            }
            List creds = null;
            
            try {
                SubjectSecurity subjectSecurity = getSubjectSecurity(subject);
                SSOToken ssoToken = subjectSecurity.ssoToken;
                creds = getUserCredentialsFromSSOToken(ssoToken);
                if(creds == null || creds.isEmpty()) {
                   creds = subjectSecurity.userCredentials;                   
                }                
            } catch (Exception ex) {
                if(debug.messageEnabled()) {
                    debug.message("SOAPRequestHandler.getSecurityToken:: " +
                                  "getSubjectSecurity error :" 
                                  + ex.getMessage());
                }
            }

            if(creds == null || creds.isEmpty()) {
                creds = config.getUsers();
            }
            if(creds == null || creds.isEmpty()) {
                debug.error("SOAPRequestHandler.getSecurityToken:: No users " +
                        " are configured.");
                throw new SecurityException(
                        bundle.getString("nousers"));
            }
            PasswordCredential credential =
                   (PasswordCredential)creds.iterator().next();
            UserNameTokenSpec tokenSpec = new UserNameTokenSpec();
            if((SecurityMechanism.WSS_NULL_USERNAME_TOKEN_PLAIN_URI.equals(uri)) 
               || (SecurityMechanism.WSS_TLS_USERNAME_TOKEN_PLAIN_URI.equals(uri)) 
               || (SecurityMechanism.WSS_CLIENT_TLS_USERNAME_TOKEN_PLAIN_URI.
                   equals(uri))) {
                tokenSpec.setPasswordType(WSSConstants.PASSWORD_PLAIN_TYPE);
            } else {
                tokenSpec.setNonce(true);
                tokenSpec.setPasswordType(WSSConstants.PASSWORD_DIGEST_TYPE);
            }
            tokenSpec.setCreateTimeStamp(true);
            tokenSpec.setUserName(credential.getUserName());
            tokenSpec.setPassword(credential.getPassword());
            securityToken = factory.getSecurityToken(tokenSpec);

        } else if(
           (SecurityMechanism.WSS_NULL_SAML2_HK_URI.equals(uri)) ||
                (SecurityMechanism.WSS_TLS_SAML2_HK_URI.equals(uri)) ||
                (SecurityMechanism.WSS_CLIENT_TLS_SAML2_HK_URI.equals(uri)) ||
                (SecurityMechanism.WSS_NULL_SAML2_SV_URI.equals(uri)) ||
                (SecurityMechanism.WSS_TLS_SAML2_SV_URI.equals(uri)) ||
           (SecurityMechanism.WSS_CLIENT_TLS_SAML2_SV_URI.equals(uri)) ) {

           if(debug.messageEnabled()) {
                debug.message("SOAPRequestHandler.getSecurityToken:: creating " +
                        "SAML2 token");
            }
            if(config.usePassThroughSecurityToken()) {
               securityToken = getSecurityTokenFromSubject(subject);
            }
            if(securityToken != null) {
               if(debug.messageEnabled()) {                  
                  debug.message("SOAPRequestHandler.getSecurityToken::" +
                       "security token from subject is not null"); 
               }
               return securityToken;
            }
            String issuer = SystemConfigurationUtil.getProperty(
                            ASSERTION_ISSUER);
            NameID ni = null;
            Map samlAttributes = new HashMap();
            try {
                AssertionFactory assertionFactory =
                        AssertionFactory.getInstance();
                ni = assertionFactory.createNameID();
                SubjectSecurity subjectSecurity = getSubjectSecurity(subject);
                SSOToken userToken = subjectSecurity.ssoToken;
                if(userToken == null) {
                   if(debug.messageEnabled()) {
                      debug.message("SOAPRequestHandler.getSecurityToken:: " +
                              "using thread local for SSOToken");
                   }
                   userToken = (SSOToken)ThreadLocalService.getSSOToken();
                }
                if(userToken != null) {
                   String subjectName = userToken.getPrincipal().getName();                  
                   String nameIDMapper = config.getNameIDMapper();
                   if(nameIDMapper == null) {
                      ni.setValue(subjectName); 
                   } else {
                      ni.setValue(
                        WSSUtils.getUserPseduoName(subjectName, nameIDMapper));
                   }
                   Map attributes = WSSUtils.getSAMLAttributes(subjectName, 
                          config.getSAMLAttributeMapping(), 
                          config.getSAMLAttributeNamespace(),
                          userToken);
                   if(attributes != null) {
                      samlAttributes.putAll(attributes); 
                   }
                   Map memberships = WSSUtils.getMembershipAttributes(
                           subjectName, config.getSAMLAttributeNamespace());
                   if(memberships != null) {
                      samlAttributes.putAll(memberships); 
                   }
                } else {
                   if(issuer != null && issuer.length() !=0 ) {
                      ni.setValue(issuer);
                   } else {
                      ni.setValue(SystemConfigurationUtil.getProperty(
                              Constants.AM_SERVER_HOST)); 
                   }
                }

            } catch (Exception ex) {
                throw new SecurityException(ex.getMessage());
            }

            SAML2TokenSpec tokenSpec = new SAML2TokenSpec(ni,
                    secMech, certAlias);


            if(issuer != null && issuer.length() !=0 ) {
               tokenSpec.setIssuer(issuer);
            }

            if(!samlAttributes.isEmpty()) {
               tokenSpec.setClaimedAttributes(samlAttributes); 
            }
            String appliesTo = config.getWSPEndpoint();
            if(appliesTo != null) {
               tokenSpec.setAppliesTo(appliesTo);
            }
            tokenSpec.setSigningAlias(certAlias);
            tokenSpec.setAssertionID(SAMLUtilsCommon.generateID());
            securityToken = factory.getSecurityToken(tokenSpec);                                 

        } else if(SecurityMechanism.WSS_NULL_KERBEROS_TOKEN_URI.equals(uri) ||
           SecurityMechanism.WSS_TLS_KERBEROS_TOKEN_URI.equals(uri) ||
           SecurityMechanism.WSS_CLIENT_TLS_KERBEROS_TOKEN_URI.equals(uri)) {

           if(debug.messageEnabled()) {
                debug.message("SOAPRequestHandler.getSecurityToken:: creating " +
                        "Kerberos token");
            }            
            KerberosTokenSpec tokenSpec = new KerberosTokenSpec();
            tokenSpec.setKDCDomain(config.getKDCDomain());
            tokenSpec.setKDCServer(config.getKDCServer());
            tokenSpec.setServicePrincipal(config.getKerberosServicePrincipal());
            tokenSpec.setTicketCacheDir(config.getKerberosTicketCacheDir());
            securityToken = factory.getSecurityToken(tokenSpec);   
        } else {
            throw new SecurityException(
                    bundle.getString("unsupportedSecurityMechanism"));
        }
        return securityToken;
    }

    /**
     * Place holder class for the subject credential objects.
     */
    private class SubjectSecurity {
        SSOToken ssoToken = null;
        ResourceOffering discoRO = null;
        List discoCredentials = null;
        List userCredentials = null;
    }

    /**
     * Returns the security credentials if exists in the subject.
     */
    private SubjectSecurity getSubjectSecurity(Subject subject) {

        final SubjectSecurity subjectSecurity = new SubjectSecurity();
        final Subject sub = subject;
        AccessController.doPrivileged(
                new PrivilegedAction() {
                    public Object run() {
                        Set creds = sub.getPrivateCredentials();
                     if(creds == null || creds.isEmpty()) {
                            return null;
                        }
                     Iterator iter =  creds.iterator();
                     while(iter.hasNext()) {
                            Object credObj = iter.next();
                         if(credObj instanceof SSOToken) {
                            subjectSecurity.ssoToken = (SSOToken)credObj;
                         } else if(credObj instanceof ResourceOffering) {
                            subjectSecurity.discoRO = (ResourceOffering)credObj;
                         } else if(credObj instanceof List) {
                            List list = (List)credObj;
                            if(list != null && list.size() > 0) {
                                    if (list.get(0) instanceof SecurityAssertion) {
                                        subjectSecurity.discoCredentials = list;
                                } else if (
                                  list.get(0) instanceof PasswordCredential) {
                                        subjectSecurity.userCredentials = list;
                                    }
                                }
                            }
                        }
                        return null;
                    }
                });
        return subjectSecurity;
    }

    /**
     * Returns the configured message authenticator.
     */
    public static MessageAuthenticator getAuthenticator()
            throws SecurityException {

        if(authenticator != null) {
            return authenticator;
        }
        String classImpl = SystemConfigurationUtil.getProperty(
                WSS_AUTHENTICATOR,
                "com.sun.identity.wss.security.handler.DefaultAuthenticator");
        try {
            Class authnClass = Class.forName(classImpl);
            authenticator = (MessageAuthenticator)authnClass.newInstance();
        } catch (Exception ex) {
            debug.error("SOAPRequestHandler.getAuthenticator:: Unable to " +
                    "get the authenticator", ex);
            throw new SecurityException(
                    bundle.getString("authenticatorNotFound"));
        }
        return authenticator;
    }

    /**
     * Returns the configured message authenticator.
     */
    public static MessageAuthorizer getAuthorizer()
            throws SecurityException {
        if(authorizer != null) {
           return authorizer;
        }
        
        String classImpl = SystemConfigurationUtil.getProperty(
                WSS_AUTHORIZER,
                "com.sun.identity.wss.security.handler.DefaultAuthorizer");
        try {
            Class authnClass = Class.forName(classImpl);
            authorizer = (MessageAuthorizer)authnClass.newInstance();
        } catch (Exception ex) {
            debug.error("SOAPRequestHandler.getAuthenticator:: Unable to " +
                    "get the authorizer", ex);
            throw new SecurityException(
                    bundle.getString("authorizerInitFailed"));
        }
        return authorizer;
    }

    /**
     * Returns the secured <code>SOAPMessage</code> by using liberty
     * protocols.
     *
     * @param ssoToken Single sign-on token of the user.
     *
     * @param subject the subject.
     *
     * @param soapMessage the SOAPMessage that needs to be secured.
     *
     * @param sharedData any shared data map between request and the response.
     *
     * @param providerConfig the provider configuration.
     *
     * @return SecurityException if there is any error occured.
     */
    private SOAPMessage getSecureMessageFromLiberty(
            SSOToken ssoToken,
            Subject subject,
            SOAPMessage soapMessage,
            Map sharedData,
            ProviderConfig providerConfig)
            throws SecurityException {

        try {
            SSOTokenManager.getInstance().validateToken(ssoToken);
            ResourceOffering discoRO = getDiscoveryResourceOffering(
                    subject, ssoToken);
           if(debug.messageEnabled()) {
              debug.message("SOAPRequestHandler.getSecureMessageFromLiberty:: "+
              "Discovery service resource offering. " + discoRO.toString());
            }
            List credentials = getDiscoveryCredentials(subject);
            MessageProcessor processor = new MessageProcessor(providerConfig);
            return processor.secureRequest(discoRO, credentials,
                    providerConfig.getServiceType(), soapMessage, sharedData);

        } catch (SSOException se) {
            debug.error("SOAPRequestHandler.getSecureMessageFromLiberty:: " +
                    "Invalid sso token", se);
            throw new SecurityException(
                    bundle.getString("invalidSSOToken"));
        } catch (SOAPBindingException sbe) {
            debug.error("SOAPRequestHandler.getSecureMessageFromLiberty:: " +
                    " SOAPBinding exception", sbe);
            throw new SecurityException(sbe.getMessage());
        }
    }

    /**
     * Returns the discovery service resource offering.
     */
    private ResourceOffering getDiscoveryResourceOffering(
            Subject subject, SSOToken ssoToken) throws SecurityException {

        SubjectSecurity subjectSecurity = getSubjectSecurity(subject);
        if(subjectSecurity.discoRO != null) {
           if(debug.messageEnabled()) {
              debug.message("SOAPRequestHandler.getDiscoveryResourceOffering::"
              + " subject contains resource offering.");
            }
            return subjectSecurity.discoRO;
        }

// If the creds not present, authenticate to the IDP via AuthnService.
        SASLResponse saslResponse =  getSASLResponse(ssoToken);
        if(saslResponse == null) {
            debug.error("SOAPRequestHandler.getDiscoveryResourceOffering:: " +
                    "SASL Response is null");
            throw new SecurityException(
                    bundle.getString("SASLFailure"));
        }

        final ResourceOffering discoRO = saslResponse.getResourceOffering();
        if(discoRO == null) {
            throw new SecurityException(
                    bundle.getString("resourceOfferingMissing"));
        }
        final List credentials = saslResponse.getCredentials();
        final Subject sub = subject;
        if(discoRO != null) {
            AccessController.doPrivileged(
                    new PrivilegedAction() {
                        public Object run() {
                            sub.getPrivateCredentials().add(discoRO);
                            List assertions = new ArrayList();
                            if(credentials != null && !credentials.isEmpty()) {
                               for(Iterator iter = credentials.iterator();
                                        iter.hasNext();) {
                                   Element elem = (Element)iter.next();
                                   try {
                                       SecurityAssertion secAssertion = 
                                           new SecurityAssertion(elem);
                                       assertions.add(secAssertion);
                                   } catch (Exception ex) {
                                       if(debug.warningEnabled()) {
                                          debug.warning("SOAPRequestHandler." +
                                          "getDiscoveryResourceOffering: ", ex);
                                       }
                                   }
                                   if(assertions != null && 
                                              !assertions.isEmpty()) {
                                      sub.getPrivateCredentials().
                                          add(assertions);
                                   }
                                   
                               }
                            }
                            return null;
                        }
               }
           );
        } 
        return discoRO;
    }

    /**
     * Returns the credentials for the discovery service.
     */
    private List getDiscoveryCredentials(Subject subject) {
        SubjectSecurity subjectSecurity = getSubjectSecurity(subject);
        return subjectSecurity.discoCredentials;
    }

    /**
     * Returns the <code>SASLResponse</code> using user's SSOToken.
     */
    private SASLResponse getSASLResponse(SSOToken ssoToken)
            throws SecurityException {
        SASLRequest saslReq = new SASLRequest(MECHANISM_SSOTOKEN);
        try {
            String authURL = SystemConfigurationUtil.getProperty(LIBERTY_AUTHN_URL);
            if(authURL == null) {
                debug.error("SOAPRequestHandler.getSASLResponse:: AuthnURL " +
                        " not present in the configuration.");
                throw new SecurityException(
                        bundle.getString("authnURLMissing"));
            }

            SASLResponse saslResp = AuthnSvcClient.sendRequest(
                    saslReq, authURL);
            if(!saslResp.getStatusCode().equals(SASLResponse.CONTINUE)) {
                debug.error("SOAPRequestHandler.getSASLResponse:: ABORT");
                throw new SecurityException(
                        bundle.getString("SASLFailure"));
            }

            String serverMechanism = saslResp.getServerMechanism();
            saslReq = new SASLRequest(serverMechanism);
            saslReq.setData(ssoToken.getTokenID().toString().getBytes("UTF-8"));
            saslReq.setRefToMessageID(saslResp.getMessageID());
            saslResp = AuthnSvcClient.sendRequest(saslReq, authURL);
            if(!saslResp.getStatusCode().equals(SASLResponse.OK)) {
                debug.error("SOAPRequestHandler.getSASLResponse:: SASL Failure");
                throw new SecurityException(
                        bundle.getString("SASLFailure"));
            }
            return saslResp;

        } catch (AuthnSvcException ae) {
            debug.error("SOAPRequestHandler.getSASLResponse:: Exception", ae);
            throw new SecurityException(
                    bundle.getString("SASLFailure"));
        } catch (UnsupportedEncodingException uae) {
            debug.error("SOAPRequestHandler.getSASLResponse:: Exception", uae);
            throw new SecurityException(
                    bundle.getString("SASLFailure"));
        }
    }

    /**
     * Checks if the received SOAP Message is a liberty request.
     */
    private boolean isLibertyMessage(SOAPMessage soapMessage)
            throws SecurityException {
        try {
            SOAPHeader soapHeader = soapMessage.getSOAPPart().
                    getEnvelope().getHeader();
            if(soapHeader == null) {
                return false;
            }
            NodeList headerChildNodes = soapHeader.getChildNodes();
            if((headerChildNodes == null) ||
                    (headerChildNodes.getLength() == 0)) {
                return false;
            }
            for(int i=0; i < headerChildNodes.getLength(); i++) {
                Node currentNode = headerChildNodes.item(i);
                if(currentNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                if((SOAPBindingConstants.TAG_CORRELATION.equals(
                        currentNode.getLocalName())) &&
                        (SOAPBindingConstants.NS_SOAP_BINDING.equals(
                        currentNode.getNamespaceURI()))) {
                    return true;
                }
            }
            return false;
        } catch (SOAPException se) {
            debug.error("SOAPRequest.isLibertyRequest:: SOAPException", se);
            throw new SecurityException(se.getMessage());
        }
    }

    /**
     * Returns the provider config from the shared state.
     */
    private ProviderConfig getProviderConfig(Map sharedMap) {
        if((sharedMap == null) || (sharedMap.isEmpty())) {
            return null;
        }

        try {
            String serviceName = ThreadLocalService.getServiceName();
            if(serviceName == null) {
                serviceName = getServiceName(sharedMap);
                if(debug.messageEnabled()) {
                    debug.message("SOAPRequestHandler.getServiceName: " +
                        "Service Name from javax.xml.ws.wsdl.service : " 
                        + serviceName);
                }
            } else {
                // If we find the service name, that is of TA service name
                // So, create provider config from TA.
                if(debug.messageEnabled()) {
                    debug.message("SOAPRequestHandler.getProviderConfig: " +
                            "Service Name found in thread local" + serviceName);
                }
                ThreadLocalService.removeServiceName(serviceName);
                STSConfig stsConfig = 
                    (STSConfig) TrustAuthorityConfig.getConfig(serviceName, 
                        TrustAuthorityConfig.STS_TRUST_AUTHORITY);
                ProviderConfig pc =
                    ProviderConfig.getProvider(serviceName, ProviderConfig.WSC);
                pc.setSecurityMechanisms(stsConfig.getSecurityMech());
                pc.setRequestSignEnabled(stsConfig.isRequestSignEnabled());
                pc.setRequestEncryptEnabled(stsConfig.isRequestEncryptEnabled());
                pc.setDefaultKeyStore(true);
                pc.setUsers(stsConfig.getUsers());
                pc.setWSPEndpoint(stsConfig.getEndpoint());
                pc.setKDCDomain(stsConfig.getKDCDomain());
                pc.setKDCServer(stsConfig.getKDCServer());
                pc.setKerberosServicePrincipal(pc.getKerberosServicePrincipal());
                pc.setKerberosTicketCacheDir(
                        stsConfig.getKerberosTicketCacheDir());
                pc.setEncryptionAlgorithm(stsConfig.getEncryptionAlgorithm());
                pc.setEncryptionStrength(stsConfig.getEncryptionStrength());
                pc.setSigningRefType(stsConfig.getSigningRefType());
                pc.setSAMLAttributeMapping(stsConfig.getSAMLAttributeMapping());
                pc.setSAMLAttributeNamespace(
                        stsConfig.getSAMLAttributeNamespace());
                pc.setIncludeMemberships(stsConfig.shouldIncludeMemberships());
                pc.setNameIDMapper(stsConfig.getNameIDMapper());
                pc.setDNSClaim(stsConfig.getDNSClaim());
                pc.setSignedElements(stsConfig.getSignedElements());

                return pc;
            }
            if(!ProviderConfig.isProviderExists(serviceName,  
                    ProviderConfig.WSC)) {
                return null;
            }

            ProviderConfig config =
                ProviderConfig.getProvider(serviceName, ProviderConfig.WSC);
            if(!config.useDefaultKeyStore()) {
                initializeSystemProperties(config);
            }
            return config;
        } catch (ProviderException pe) {
            WSSUtils.debug.error("SOAPRequestHandler.getProviderConfig: from" +
                    "shared map: Exception", pe);
            return null;
        } catch (IOException ie) {
            WSSUtils.debug.error("SOAPRequestHandler.getProviderConfig: from" +
                    "shared map: IOException", ie);
            return null;
        }

    }

    private String getServiceName(Map sharedMap) {

        // Check first if thread local has the service name.

        if((sharedMap == null) || (sharedMap.isEmpty())) {
            return null;
        }
        QName service = (QName)sharedMap.get("javax.xml.ws.wsdl.service");
        if(service == null) {
            return null;
        }
        return service.getLocalPart();
    }

    /**
     * Prints a Node tree recursively.
     *
     * @param node A DOM tree Node
     *
     * @return An xml String representation of the DOM tree.
     */
    public String print(Node node) {
        return WSSUtils.print(node);
    }

// Removes the validated headers.
    private void removeValidatedHeaders(ProviderConfig config,
            SOAPMessage soapMessage) {

        SOAPHeader header = null;
        try {
            header = soapMessage.getSOAPPart().getEnvelope().getHeader();
        } catch (SOAPException se) {
            WSSUtils.debug.error("SOAPRequestHandler.removeValidateHeaders: " +
                    "Failed to read the SOAP Header.");
        }
        if(header != null) {
            Iterator iter = header.examineAllHeaderElements();
            while(iter.hasNext()) {
              SOAPHeaderElement headerElement = (SOAPHeaderElement)iter.next();
              if ((config == null) || (!config.preserveSecurityHeader())) {
                 if("Security".equalsIgnoreCase(
                            headerElement.getElementName().getLocalName())) {
                        headerElement.detachNode();
                    }
                }
              if ("Correlation".equalsIgnoreCase(
                        headerElement.getElementName().getLocalName())) {
                    headerElement.detachNode();
                }
            }
        }
    }
    
    private ProviderConfig getSTSProviderConfig(STSRemoteConfig stsConfig) 
            throws SecurityException {
        
        if(stsConfig == null) {
           return null;
        }      
        try {
             ProviderConfig pc = ProviderConfig.getProvider(
                     stsConfig.getIssuer(), ProviderConfig.WSP, false);
             pc.setKDCDomain(stsConfig.getKDCDomain());
             pc.setKDCServer(stsConfig.getKDCServer());
             pc.setKerberosServicePrincipal(
                     stsConfig.getKerberosServicePrincipal());
             pc.setKeyTabFile(stsConfig.getKeyTabFile());
             pc.setValidateKerberosSignature(
                     stsConfig.isValidateKerberosSignature());
             pc.setSecurityMechanisms(stsConfig.getSecurityMechanisms());
             pc.setUsers(stsConfig.getUsers());
             pc.setRequestEncryptEnabled(stsConfig.isRequestEncryptEnabled());
             pc.setRequestHeaderEncryptEnabled(stsConfig.isRequestHeaderEncryptEnabled());
             pc.setRequestSignEnabled(stsConfig.isRequestSignEnabled());
             pc.setResponseEncryptEnabled(stsConfig.isResponseEncryptEnabled());
             pc.setResponseSignEnabled(stsConfig.isResponseSignEnabled());
             pc.setPreserveSecurityHeader(false);
             pc.setPublicKeyAlias(stsConfig.getPublicKeyAlias());
             pc.setKeyAlias(stsConfig.getPrivateKeyAlias());
             pc.setEncryptionAlgorithm(stsConfig.getEncryptionAlgorithm());
             pc.setEncryptionStrength(stsConfig.getEncryptionStrength());
             pc.setSigningRefType(stsConfig.getSigningRefType());
             pc.setAuthenticationChain(stsConfig.getAuthenticationChain());
             pc.setDetectUserTokenReplay(
                     stsConfig.isUserTokenDetectReplayEnabled());
             pc.setMessageReplayDetection(
                     stsConfig.isMessageReplayDetectionEnabled());
             pc.setDNSClaim(stsConfig.getIssuer());
             pc.setSignedElements(stsConfig.getSignedElements());
             return pc;
             
        } catch (ProviderException pe) {
            throw new SecurityException(pe.getMessage());
        }        
    }
    
    private SecurityToken getSecurityTokenFromSubject(Subject subj)
          throws SecurityException {
        
        SecurityToken securityToken = null;
        Subject subject = subj;
        if(subject.getPublicCredentials().isEmpty()) {
           Object obj = ThreadLocalService.getSubject();
           if(obj != null) {
              subject = (Subject)obj;              
           }           
        }
        Iterator iter = subject.getPublicCredentials().iterator();
        while(iter.hasNext()) {
            Object obj = iter.next();
            if(!(obj instanceof Element)) {
               continue; 
            }
            Element tokenE = (Element)obj;
            if(!tokenE.getLocalName().equals("Assertion")) {
               continue;
            }
            String ns = tokenE.getNamespaceURI();
            if(ns == null) {
               continue;
            }
            try {
                if(ns.equals(STSConstants.SAML10_ASSERTION)) {               
                   securityToken = new AssertionToken(tokenE);                
                } else if (ns.equals(STSConstants.SAML20_ASSERTION)) {
                   securityToken = new SAML2Token(tokenE); 
                }
            } catch (Exception ex) {
                WSSUtils.debug.error("SOAPRequestHandler.getSecurityToken" +
                        "FromSubject:: exception", ex); 
                throw new SecurityException(ex.getMessage());
            }
        }
        return securityToken;
    }
    
    private Subject getAuthenticatedSubject() {
        Subject subject = (Subject)ThreadLocalService.getSubject();
        if(subject == null) {
           return null;
        }
        
        if(!subject.getPrivateCredentials().isEmpty()) {
           return subject;
        }
        return null;
    }
    
    /**
     * Retrieves the custom credential from the Subject.
     * The custom subject is used as on behalf of token element to the STS.    
     * @param subject the authenticated JAAS subject where the custom token
     *                is set.
     * @return the custom token object.
     */
    private Object getCustomCredential(Subject subject) {
        Set creds = subject.getPublicCredentials();
        if(creds == null || creds.isEmpty()) {
           return null; 
        }
        
        Iterator iter = creds.iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            if(!(obj instanceof Map)) {
               continue;
            }
            Map map = (Map)obj;
            if(map.containsKey(WSSConstants.CUSTOM_TOKEN)) {
               return map.get(WSSConstants.CUSTOM_TOKEN); 
            }
        }
        return null;
    }
    
    private List getUserCredentialsFromSSOToken(SSOToken ssoToken) {
        
        if(ssoToken == null) {
           if(debug.messageEnabled()) {
              debug.message("SOAPRequestHandler.getUserCredentialsFrom" +
                      "SSOToken. ssotoken is null"); 
           }
           return null; 
        }        
        try {
            if(SSOTokenManager.getInstance().isValidToken(ssoToken)) {
               String password = null;
               boolean useHashedPassword = Boolean.valueOf(
                    SystemConfigurationUtil.getProperty(
                    "com.sun.identity.wss.security.useHashedPassword", "true"));
               if(useHashedPassword) {
                  password = ssoToken.getProperty(
                          WSSConstants.HASHED_USER_PASSWORD); 
               } else {
                  String encryptedPassword = ssoToken.getProperty(
                       WSSConstants.ENCRYPTED_USER_PASSWORD);
                  if(encryptedPassword == null) {
                     if(debug.messageEnabled()) {
                        debug.message("SOAPRequestHandler.getUserCredentials" +
                                "FromSSOToken. encrypted password is null"); 
                     }
                     return null;          
                  }
                  password = Crypt.decrypt(encryptedPassword);
               }
                            
               if(password == null) {
                  if(debug.messageEnabled()) {
                        debug.message("SOAPRequestHandler.getUserCredentials" +
                                "FromSSOToken. password is null"); 
                  }
                  return null;
               }
               String userId = ssoToken.getProperty("UserId");
               List list = new ArrayList();
               PasswordCredential pc = new PasswordCredential(userId, password);
               list.add(pc);
               return list;
            }
        } catch (SSOException se) {
            if(debug.warningEnabled()) {
               debug.warning("SOAPRequestHandler.getUserCredentials" +
                                "FromSSOToken. ssoexception", se); 
            }
            return null;
        }        
        return null;
    }
     
    private boolean checkForReplay(String msgID, String wsp) {
        
        if(msgID == null) {
           return false; 
        }
        Map messageIDMap = WSSCache.messageIDMap;
        long stale_limit = WSSCache.cacheTimeoutInterval * 1000;
        WSSCacheRepository cacheRepo = WSSUtils.getWSSCacheRepository();
        Long prevMsgIDTime = (Long)messageIDMap.get(msgID);
        if(prevMsgIDTime == null && cacheRepo != null) {
           prevMsgIDTime = cacheRepo.retrieveMessageTimestamp(msgID, wsp);
        }
        long currentTime = System.currentTimeMillis();
        if((prevMsgIDTime != null) &&
                ((currentTime - prevMsgIDTime.longValue()) < stale_limit)) {
            if(WSSUtils.debug.warningEnabled()) {
               WSSUtils.debug.warning("SOAPRequestHandler.checkForReplay: " +
                       "replay attack detected");
            }
            return true;
        } else {
           messageIDMap.put(msgID, new Long(currentTime));
           if(cacheRepo != null) {
              cacheRepo.saveMessageTimestamp(msgID, new Long(currentTime), wsp);
           }
        }
        
        return false;        
    }
    
    private boolean checkForReplay(Subject subject, long msgTimestamp, 
            String wsp) {
                        
        Iterator iter = subject.getPrincipals().iterator();
        Principal principal = (Principal)iter.next();
        String replayIndexStr = principal.getName() +
                                new Long(msgTimestamp).toString();
        return checkForReplay(replayIndexStr, wsp);
       
    }
    
    private void updateSharedState(Subject subject, Map sharedState) {
        
        Set creds = subject.getPublicCredentials();        
        if(creds != null && !creds.isEmpty()) {
           Iterator iter = creds.iterator();
           Object  object = iter.next();
           if(object instanceof X509Certificate) {
              X509Certificate cert = (X509Certificate)object;              
              sharedState.put(CLIENT_CERT, cert);
                      
           }
        }

        if(sharedState.get(CLIENT_CERT) == null) {
           //Check if thread local has it.
           X509Certificate clientCert =
                   (X509Certificate)ThreadLocalService.getClientCertificate();
           if(clientCert != null) {
              sharedState.put(CLIENT_CERT, clientCert);
              ThreadLocalService.removeClientCertificate();
           }
        }
    }    
}
