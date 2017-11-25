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
 * $Id: WSSPolicyManager.java,v 1.2 2009/12/19 00:09:41 asyhuang Exp $
 *
 */
package com.sun.identity.wss.policy;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Iterator;
        
import com.sun.identity.wsfederation.jaxb.wspolicy.PolicyElement;
import com.sun.identity.wsfederation.jaxb.wspolicy.ExactlyOneElement;
import com.sun.identity.wsfederation.jaxb.wspolicy.AllElement;
import com.sun.identity.wsfederation.jaxb.wsspolicy.AsymmetricBindingElement;
import com.sun.identity.wsfederation.jaxb.wsspolicy.SymmetricBindingElement;
import com.sun.identity.wsfederation.jaxb.wsspolicy.InitiatorTokenElement;
import com.sun.identity.wsfederation.jaxb.wsspolicy.RecipientTokenElement;
import com.sun.identity.wsfederation.jaxb.wsspolicy.X509TokenElement;
import com.sun.identity.wsfederation.jaxb.wsspolicy.UsernameTokenElement;
import com.sun.identity.wsfederation.jaxb.wsspolicy.SamlTokenElement;
import com.sun.identity.wsfederation.jaxb.wsspolicy.WssX509V3Token10Element;
import com.sun.identity.wsfederation.jaxb.wsspolicy.WssUsernameToken10Element;
import com.sun.identity.wsfederation.jaxb.wsspolicy.WssSamlV20Token11Element;
import com.sun.identity.wsfederation.jaxb.wsspolicy.WssSamlV11Token11Element;
import com.sun.identity.wsfederation.jaxb.wsspolicy.AlgorithmSuiteElement;
import com.sun.identity.wsfederation.jaxb.wsspolicy.Basic128Element;
import com.sun.identity.wsfederation.jaxb.wsspolicy.Basic192Element;
import com.sun.identity.wsfederation.jaxb.wsspolicy.Basic256Element;
import com.sun.identity.wsfederation.jaxb.wsspolicy.TripleDesElement;
import com.sun.identity.wsfederation.jaxb.wsspolicy.LayoutElement;
import com.sun.identity.wsfederation.jaxb.wsspolicy.ProtectionTokenElement;
import com.sun.identity.wsfederation.jaxb.wsspolicy.KerberosTokenElement;
import com.sun.identity.wsfederation.jaxb.wsspolicy.
        WssKerberosV5ApReqToken11Element;
import com.sun.identity.wsfederation.jaxb.wsspolicy.SignedPartsElement;
import com.sun.identity.wsfederation.jaxb.wsspolicy.EncryptedPartsElement;
import com.sun.identity.wsfederation.jaxb.wsspolicy.HeaderType;
import com.sun.identity.wsfederation.jaxb.wsspolicy.IssuedTokenElement;
import com.sun.identity.wsfederation.jaxb.wsaddr.EndpointReferenceElement;
import com.sun.identity.wsfederation.jaxb.wsaddr.AttributedURIType;
import com.sun.identity.wsfederation.jaxb.wsspolicy.
        RequestSecurityTokenTemplateType;
import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.security.SecurityMechanism;
import com.sun.identity.wss.security.WSSConstants;
import com.sun.identity.wss.security.WSSUtils;
import com.sun.identity.wss.sts.config.STSRemoteConfig;


/**
 * The <code>WSSPolicyManager</code> class manages the WS-Security policy
 * configuration and is used to convert from <code>ProviderConfig</code> to
 * WS-Security Policy and vice versa.
 */
public class WSSPolicyManager {
    
    private static final String INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT =
            "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/" +
            "IncludeToken/AlwaysToRecipient";
    private static com.sun.identity.wsfederation.jaxb.wspolicy.ObjectFactory
             wsPolicyFactory = 
             new com.sun.identity.wsfederation.jaxb.wspolicy.ObjectFactory();
    
    private static com.sun.identity.wsfederation.jaxb.wsspolicy.ObjectFactory
             wssPolicyFactory = 
             new com.sun.identity.wsfederation.jaxb.wsspolicy.ObjectFactory();
    
    private static com.sun.identity.wsfederation.jaxb.wsaddr.ObjectFactory
            wsAddressingFactory =
            new com.sun.identity.wsfederation.jaxb.wsaddr.ObjectFactory();
    
    private static WSSPolicyManager wssPolicyManager = 
            new WSSPolicyManager();
    
    private WSSPolicyManager() {
        
    }
    
    public static WSSPolicyManager getInstance() {
        return wssPolicyManager;
    }
    
    /**
     * Returns the web service end point policy based on the provider
     * configuration.
     * @param providerConfig the provider configuration for a given provider
     * configuration.
     * @return the XML String representation of ws-security policy. 
     */
    public String getPolicy(ProviderConfig providerConfig) 
            throws WSSPolicyException { 
           
        try {
            PolicyElement policyElement = wsPolicyFactory.createPolicyElement();           
            ExactlyOneElement exactlyOneElement = 
                    wsPolicyFactory.createExactlyOneElement();
            //TODO - Need to add a config in the WSP config and then create the
            // issued token policy.
            boolean useIssuedTokenPolicy = false;
            List <String> securityMech = providerConfig.getSecurityMechanisms();
            if(securityMech == null || securityMech.isEmpty()) {
               throw new WSSPolicyException(
                       "Security mechanism not configured"); 
            }
            for (Iterator iter = securityMech.iterator(); iter.hasNext();) {
                String secMech = (String)iter.next();
                AllElement allElement = wsPolicyFactory.createAllElement();                
                if(SecurityMechanism.WSS_NULL_KERBEROS_TOKEN_URI.equals(
                        secMech)) {
                   SymmetricBindingElement sbe =
                         wssPolicyFactory.createSymmetricBindingElement();
                   PolicyElement policyElement1 = 
                           wsPolicyFactory.createPolicyElement();
                   sbe.setPolicy(policyElement1);
                   ProtectionTokenElement pte = 
                           createProtectionTokenElement(secMech);
                   policyElement1.getPolicyOrAllOrExactlyOne().add(pte);
                   if(providerConfig.isResponseSignEnabled()) {
                      RecipientTokenElement rte = createRecipientTokenElement();
                      policyElement1.getPolicyOrAllOrExactlyOne().add(rte);
                   }
                   AlgorithmSuiteElement ase = 
                           createAlgorithmSuiteElement(providerConfig);
                   if(ase != null) {
                      policyElement1.getPolicyOrAllOrExactlyOne().add(ase); 
                   }
                   policyElement1.getPolicyOrAllOrExactlyOne().add(
                           createLayoutElement());
                   policyElement1.getPolicyOrAllOrExactlyOne().add(
                        wssPolicyFactory.createIncludeTimestampElement());
                   policyElement1.getPolicyOrAllOrExactlyOne().add(
                       wssPolicyFactory.
                       createOnlySignEntireHeadersAndBodyElement());
                   allElement.getPolicyOrAllOrExactlyOne().add(sbe);
                } else if (useIssuedTokenPolicy) {
                   AsymmetricBindingElement abe =
                      wssPolicyFactory.createAsymmetricBindingElement();
                   PolicyElement policyElement1 = 
                           wsPolicyFactory.createPolicyElement();
                   abe.setPolicy(policyElement1);
                 
                   IssuedTokenElement ite = createIssuedTokenElement();
                   policyElement1.getPolicyOrAllOrExactlyOne().add(ite);
                
                   if(providerConfig.isResponseSignEnabled()) {
                      RecipientTokenElement rte = createRecipientTokenElement();
                      policyElement1.getPolicyOrAllOrExactlyOne().add(rte);
                   }
                   AlgorithmSuiteElement ase = 
                           createAlgorithmSuiteElement(providerConfig);
                   if(ase != null) {
                      policyElement1.getPolicyOrAllOrExactlyOne().add(ase); 
                   }
                   policyElement1.getPolicyOrAllOrExactlyOne().add(
                           createLayoutElement());
                   policyElement1.getPolicyOrAllOrExactlyOne().add(
                        wssPolicyFactory.createIncludeTimestampElement());
                   policyElement1.getPolicyOrAllOrExactlyOne().add(
                       wssPolicyFactory.
                       createOnlySignEntireHeadersAndBodyElement());
                   allElement.getPolicyOrAllOrExactlyOne().add(abe);
                   exactlyOneElement.getPolicyOrAllOrExactlyOne().add(
                           allElement);
                   break;
                } else {
                   AsymmetricBindingElement abe =
                      wssPolicyFactory.createAsymmetricBindingElement();
                   PolicyElement policyElement1 = 
                           wsPolicyFactory.createPolicyElement();
                   abe.setPolicy(policyElement1);
                 
                   InitiatorTokenElement ite = 
                           createInitiatorTokenElement(secMech);
                   policyElement1.getPolicyOrAllOrExactlyOne().add(ite);
                

                   if(providerConfig.isResponseSignEnabled()) {
                      RecipientTokenElement rte = createRecipientTokenElement();
                      policyElement1.getPolicyOrAllOrExactlyOne().add(rte);
                   }
                   AlgorithmSuiteElement ase = 
                           createAlgorithmSuiteElement(providerConfig);
                   if(ase != null) {
                      policyElement1.getPolicyOrAllOrExactlyOne().add(ase); 
                   }
                   policyElement1.getPolicyOrAllOrExactlyOne().add(
                           createLayoutElement());
                   policyElement1.getPolicyOrAllOrExactlyOne().add(
                        wssPolicyFactory.createIncludeTimestampElement());
                   policyElement1.getPolicyOrAllOrExactlyOne().add(
                       wssPolicyFactory.
                       createOnlySignEntireHeadersAndBodyElement());
                   allElement.getPolicyOrAllOrExactlyOne().add(abe);
                }
                exactlyOneElement.getPolicyOrAllOrExactlyOne().add(allElement);
            
            }
            policyElement.getPolicyOrAllOrExactlyOne().add(exactlyOneElement);
            return WSSPolicyUtils.convertJAXBToString(policyElement);
            
        } catch (JAXBException je) {
            WSSUtils.debug.error("WSSPolicyManager.getPolicy:" +
                    " JAXBException", je);
            throw new WSSPolicyException(je.getMessage());
        }        
                
    }
    
    /**
     * Returns the input policy for the given web service provider
     * configuration.
     * @param providerConfig the provider configuration of a 
     *        web service provider.
     * 
     * @return the XML String representation for the web service provider
     *         input policy. 
     * @throws com.sun.identity.wss.policy.WSSPolicyException
     */
    public String getInputPolicy(ProviderConfig providerConfig) 
            throws WSSPolicyException {
        
        try {
            PolicyElement policyElement = wsPolicyFactory.createPolicyElement();           
            ExactlyOneElement exactlyOneElement = 
                    wsPolicyFactory.createExactlyOneElement();
            AllElement allElement = wsPolicyFactory.createAllElement();
            
            policyElement.getPolicyOrAllOrExactlyOne().add(exactlyOneElement);
            
            if(providerConfig.isRequestSignEnabled()) {
               SignedPartsElement signedParts = 
                    wssPolicyFactory.createSignedPartsElement();
               signedParts.setBody(wssPolicyFactory.createEmptyType());
               allElement.getPolicyOrAllOrExactlyOne().add(signedParts);
            }
            
            if(providerConfig.isRequestEncryptEnabled() ||
                    providerConfig.isRequestHeaderEncryptEnabled()) {
               EncryptedPartsElement encryptedParts = 
                       wssPolicyFactory.createEncryptedPartsElement();
               if(providerConfig.isRequestEncryptEnabled()) {
                  encryptedParts.setBody(wssPolicyFactory.createEmptyType());
               }
               if(providerConfig.isRequestHeaderEncryptEnabled()) {
                  HeaderType headerType = 
                                    wssPolicyFactory.createHeaderType();
                  headerType.setName(
                          new QName(WSSConstants.WSSE_SECURITY_LNAME));
                  headerType.setNamespace(WSSConstants.WSSE11_NS);
                  encryptedParts.getHeader().add(headerType);
               }
               allElement.getPolicyOrAllOrExactlyOne().add(encryptedParts);
            }
            exactlyOneElement.getPolicyOrAllOrExactlyOne().add(allElement);
            return WSSPolicyUtils.convertJAXBToString(policyElement);
        } catch (JAXBException je) {
            WSSUtils.debug.error("WSSPolicyManager.getInputPolicy: " +
                    "JAXB Exception ");
            throw new WSSPolicyException(je.getMessage());
        }
        
    }
    
    /**
     * Returns the output policy for the given web service provider 
     * configuration.
     * @param providerConfig the provider configuration of a web service
     *        provider.
     * @return the XML String representation of ws-security policy for the
     *         web service provider.
     * @throws com.sun.identity.wss.policy.WSSPolicyException 
     */
    public String getOutputPolicy(ProviderConfig providerConfig)
            throws WSSPolicyException {
        try {
            PolicyElement policyElement = wsPolicyFactory.createPolicyElement();           
            ExactlyOneElement exactlyOneElement = 
                    wsPolicyFactory.createExactlyOneElement();
            AllElement allElement = wsPolicyFactory.createAllElement();
            
            policyElement.getPolicyOrAllOrExactlyOne().add(exactlyOneElement);
           
            if(providerConfig.isResponseSignEnabled()) {
               SignedPartsElement signedParts = 
                    wssPolicyFactory.createSignedPartsElement();
               signedParts.setBody(wssPolicyFactory.createEmptyType());
               allElement.getPolicyOrAllOrExactlyOne().add(signedParts);
            }
            
            if(providerConfig.isResponseEncryptEnabled()) {
               EncryptedPartsElement encryptedParts = 
                       wssPolicyFactory.createEncryptedPartsElement();
               encryptedParts.setBody(wssPolicyFactory.createEmptyType());
               allElement.getPolicyOrAllOrExactlyOne().add(encryptedParts);
            }
            exactlyOneElement.getPolicyOrAllOrExactlyOne().add(allElement);
            return WSSPolicyUtils.convertJAXBToString(policyElement);
        } catch (JAXBException je) {
            WSSUtils.debug.error("WSSPolicyManager.geOutputPolicy: " +
                    "JAXB Exception ");
            throw new WSSPolicyException(je.getMessage());
        }
    }

    /**
     * Returns the STS end point policy
     * @return the XML String representation of ws-security policy for the
     * STS service.
     * @throws WSSPolicyException
     */
    public String getSTSPolicy()
            throws WSSPolicyException {
         return getPolicy(getSTSConfig());

    }

    /**
     * Returns the input policy for the STS service
     * @return the XML String representation of ws-security policy for the
     *         STS service.
     * @throws com.sun.identity.wss.policy.WSSPolicyException
     */
    public String getSTSInputPolicy() throws WSSPolicyException {
        return getInputPolicy(getSTSConfig());

    }

    /**
     * Returns the output policy for the STS service
     * @return the XML String representation of ws-security policy for the
     *         STS service.
     * @throws com.sun.identity.wss.policy.WSSPolicyException
     */

    public String getSTSOutputPolicy() throws WSSPolicyException {
        return getOutputPolicy(getSTSConfig());
    }

    private InitiatorTokenElement createInitiatorTokenElement(
            String secMech) throws WSSPolicyException {
        
        try {
            InitiatorTokenElement ite = 
                    wssPolicyFactory.createInitiatorTokenElement();
            PolicyElement policyElement1 = 
                    wsPolicyFactory.createPolicyElement();
            ite.setPolicy(policyElement1);
            if(SecurityMechanism.WSS_NULL_X509_TOKEN_URI.equals(secMech)) {
               X509TokenElement x509Token = 
                    wssPolicyFactory.createX509TokenElement();
               x509Token.setIncludeToken(INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT);
               policyElement1.getPolicyOrAllOrExactlyOne().add(x509Token);
               
               PolicyElement policyElement2 = 
                       wsPolicyFactory.createPolicyElement();
               x509Token.getAny().add(policyElement2);
            
               WssX509V3Token10Element wssX509v3TokenElement = 
                    wssPolicyFactory.createWssX509V3Token10Element();
               policyElement2.getPolicyOrAllOrExactlyOne().add(
                       wssX509v3TokenElement);
            }  else if(SecurityMechanism.WSS_NULL_USERNAME_TOKEN_URI.
                    equals(secMech)) {
               UsernameTokenElement userNameTokenElement = 
                       wssPolicyFactory.createUsernameTokenElement();
               userNameTokenElement.setIncludeToken(
                       INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT);
               policyElement1.getPolicyOrAllOrExactlyOne().add(
                       userNameTokenElement);
               
               PolicyElement policyElement2 = 
                       wsPolicyFactory.createPolicyElement();
               userNameTokenElement.getAny().add(policyElement2);
            
               WssUsernameToken10Element wssUserTokenElement = 
                       wssPolicyFactory.createWssUsernameToken10Element();               
               policyElement2.getPolicyOrAllOrExactlyOne().add(
                       wssUserTokenElement);               
            } else if(SecurityMechanism.WSS_NULL_SAML2_HK_URI.equals(secMech)||
                    SecurityMechanism.WSS_NULL_SAML2_SV_URI.equals(secMech)) {
                SamlTokenElement samlTokenElement = 
                        wssPolicyFactory.createSamlTokenElement();
                samlTokenElement.setIncludeToken(
                        INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT);
                policyElement1.getPolicyOrAllOrExactlyOne().add(
                        samlTokenElement);
                
                PolicyElement policyElement2 = 
                        wsPolicyFactory.createPolicyElement();
                samlTokenElement.getAny().add(policyElement2);
               
                WssSamlV20Token11Element wssSaml20TokenElement =
                wssPolicyFactory.createWssSamlV20Token11Element();
                policyElement2.getPolicyOrAllOrExactlyOne().add(
                       wssSaml20TokenElement);
                
            } else if(SecurityMechanism.WSS_NULL_SAML_HK_URI.equals(secMech)||
                    SecurityMechanism.WSS_NULL_SAML_SV_URI.equals(secMech)) {
                SamlTokenElement samlTokenElement = 
                        wssPolicyFactory.createSamlTokenElement();
                samlTokenElement.setIncludeToken(
                        INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT);
                policyElement1.getPolicyOrAllOrExactlyOne().add(
                        samlTokenElement);
                                
                PolicyElement policyElement2 = 
                        wsPolicyFactory.createPolicyElement();
                samlTokenElement.getAny().add(policyElement2);
               
                WssSamlV11Token11Element wssSaml11TokenElement =
                wssPolicyFactory.createWssSamlV11Token11Element();
                policyElement2.getPolicyOrAllOrExactlyOne().add(
                       wssSaml11TokenElement);
                
            }
            
            return ite;
        } catch (JAXBException je) {
            WSSUtils.debug.error("WSSPolicyManager.createInitiateTokenElement: "
                    +  " JAXB Exception ");
            throw new WSSPolicyException (je.getMessage());
        }
        
    }
    
    private RecipientTokenElement createRecipientTokenElement() 
            throws WSSPolicyException {
        
        try {
            RecipientTokenElement rte = 
                    wssPolicyFactory.createRecipientTokenElement();
            PolicyElement policyElement1 = 
                    wsPolicyFactory.createPolicyElement();
            rte.setPolicy(policyElement1);
            X509TokenElement x509Token = 
                    wssPolicyFactory.createX509TokenElement();
            x509Token.setIncludeToken(INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT);
            policyElement1.getPolicyOrAllOrExactlyOne().add(x509Token);
               
            PolicyElement policyElement2 = 
                    wsPolicyFactory.createPolicyElement();
            x509Token.getAny().add(policyElement2);
            
            WssX509V3Token10Element wssX509v3TokenElement = 
                    wssPolicyFactory.createWssX509V3Token10Element();
            policyElement2.getPolicyOrAllOrExactlyOne().add(
                       wssX509v3TokenElement);
            return rte;
        } catch (JAXBException je) {
            WSSUtils.debug.error("WSSPolicyManager.createRecipientTokenElement:"
                    +  " JAXB Exception ");
            throw new WSSPolicyException (je.getMessage());
        }
    }
    
    private AlgorithmSuiteElement createAlgorithmSuiteElement(
            ProviderConfig config) throws WSSPolicyException {
        
        try {
            AlgorithmSuiteElement ase = 
                    wssPolicyFactory.createAlgorithmSuiteElement();
            PolicyElement policyElement1 = 
                    wsPolicyFactory.createPolicyElement();            
            ase.setPolicy(policyElement1);
            String encAlg = config.getEncryptionAlgorithm();
            int keyStrength = config.getEncryptionStrength();
            if("AES".equals(encAlg)) {
               if(keyStrength == 128) {
                  Basic128Element basic128Element = 
                    wssPolicyFactory.createBasic128Element();
                  policyElement1.getPolicyOrAllOrExactlyOne().add(
                          basic128Element);
               } else if (keyStrength == 192) {
                  Basic192Element basic192Element = 
                          wssPolicyFactory.createBasic192Element();
                  policyElement1.getPolicyOrAllOrExactlyOne().add(
                          basic192Element);
               } else if (keyStrength == 256) {
                  Basic256Element basic256Element = 
                          wssPolicyFactory.createBasic256Element();
                  policyElement1.getPolicyOrAllOrExactlyOne().add(
                          basic256Element);                  
               } else {
                  if(WSSUtils.debug.warningEnabled()) {
                     WSSUtils.debug.warning("WSSPolicyManager.create" +
                          "AlgorithmSuite: Invalid key strenghth for AES" +
                          keyStrength); 
                  }
               }
            } else if ("DESede".equals(encAlg)) {
                TripleDesElement tripleDesElement = 
                        wssPolicyFactory.createTripleDesElement();
                policyElement1.getPolicyOrAllOrExactlyOne().add(
                        tripleDesElement);
            } else {
               return null; 
            }
            return ase;
        } catch (JAXBException je) {
            WSSUtils.debug.error("WSSPolicyManager.createAlgorithmSuite: "
                    +  " JAXB Exception ");
            throw new WSSPolicyException (je.getMessage());
        }
    }
    
    private LayoutElement createLayoutElement() throws WSSPolicyException {
        try {
            LayoutElement le = 
                    wssPolicyFactory.createLayoutElement();
            PolicyElement policyElement1 = 
                    wsPolicyFactory.createPolicyElement();            
            le.setPolicy(policyElement1);
            policyElement1.getPolicyOrAllOrExactlyOne().add(
                    wssPolicyFactory.createLaxElement());
            return le;
        } catch (JAXBException je) {
            WSSUtils.debug.error("WSSPolicyManager.createLayout: "
                    +  " JAXB Exception ");
            throw new WSSPolicyException (je.getMessage());
        }
        
    }
       
    
    private ProtectionTokenElement createProtectionTokenElement(
            String secMech) throws WSSPolicyException {
        
        try {
            ProtectionTokenElement protectionElement =
                    wssPolicyFactory.createProtectionTokenElement();
            PolicyElement policyElement1 = 
                    wsPolicyFactory.createPolicyElement();
            protectionElement.setPolicy(policyElement1);
            
            if(SecurityMechanism.WSS_NULL_KERBEROS_TOKEN_URI.equals(secMech)) {
               KerberosTokenElement kerberosTokenElement =
                    wssPolicyFactory.createKerberosTokenElement();
               kerberosTokenElement.setIncludeToken(
                    INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT);
               policyElement1.getPolicyOrAllOrExactlyOne().add(
                    kerberosTokenElement);
            
               PolicyElement policyElement2 = 
                       wsPolicyFactory.createPolicyElement();
               kerberosTokenElement.getAny().add(policyElement2);
            
               WssKerberosV5ApReqToken11Element wssKrbElement = 
                    wssPolicyFactory.createWssKerberosV5ApReqToken11Element();
               policyElement2.getPolicyOrAllOrExactlyOne().add(
                       wssKrbElement);
            }
            
            return protectionElement;
        } catch (JAXBException je) {
            WSSUtils.debug.error("WSSPolicyManager.createProtectionToken: "
                    +  " JAXB Exception ");
            throw new WSSPolicyException (je.getMessage());
        }
    }
    
    private IssuedTokenElement createIssuedTokenElement() 
            throws WSSPolicyException {
        
        try {
            IssuedTokenElement issuedTokenElement =
                    wssPolicyFactory.createIssuedTokenElement();
            issuedTokenElement.setIncludeToken(
                    INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT);
            EndpointReferenceElement epr = 
                    wsAddressingFactory.createEndpointReferenceElement();
            AttributedURIType uriType = 
                    wsAddressingFactory.createAttributedURIType();            
            uriType.setValue("SunSTS");
            epr.setAddress(uriType);
            issuedTokenElement.setIssuer(epr);
            RequestSecurityTokenTemplateType rstTemplate = 
                    wssPolicyFactory.createRequestSecurityTokenTemplateType();
            issuedTokenElement.setRequestSecurityTokenTemplate(rstTemplate);
            return issuedTokenElement;
        } catch (JAXBException je) {
            WSSUtils.debug.error("WSSPolicyManager.createIssuedTokenElement: "
                    +  " JAXB Exception ");
            throw new WSSPolicyException (je.getMessage());
        }
        
    }

    private ProviderConfig getSTSConfig() throws WSSPolicyException {
        try {
            STSRemoteConfig stsConfig = new STSRemoteConfig();
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
            pc.setRequestHeaderEncryptEnabled(
                    stsConfig.isRequestHeaderEncryptEnabled());
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
        } catch (Exception ex) {
            WSSUtils.debug.error("WSSPolicyManager.getSTSConfig: "
                    +  " Exception ", ex);
            throw new WSSPolicyException(ex.getMessage());
        }
    }
}
