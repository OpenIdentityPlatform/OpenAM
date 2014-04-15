/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FAMSTSTokenProvider.java,v 1.18 2010/01/15 18:54:35 mrudul_uchil Exp $
 *
 *  Portions Copyrighted 2012-2014 ForgeRock AS
 */

package com.sun.identity.wss.sts.spi;

import com.sun.xml.ws.api.security.trust.STSAttributeProvider;
import com.sun.xml.ws.api.security.trust.STSTokenProvider;
import com.sun.xml.ws.api.security.trust.WSTrustException;
import com.sun.xml.ws.security.IssuedTokenContext;
import com.sun.xml.ws.security.trust.GenericToken;
import com.sun.xml.ws.security.trust.WSTrustConstants;
import com.sun.xml.ws.security.trust.util.WSTrustUtil;
import com.sun.xml.ws.security.trust.WSTrustVersion;
import com.sun.xml.ws.security.trust.elements.RequestedAttachedReference;
import com.sun.xml.ws.security.trust.elements.RequestedUnattachedReference;
import com.sun.xml.ws.security.trust.elements.str.SecurityTokenReference;

import com.sun.xml.wss.impl.MessageConstants;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.HashMap;
import java.util.logging.Level;
import com.sun.xml.ws.security.trust.logging.LogStringsMessages;

import java.security.cert.X509Certificate;
import javax.security.auth.Subject;
import java.util.UUID;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.content.X509Data;
import org.apache.xml.security.keys.keyresolver.KeyResolverException;
import com.sun.identity.wss.sts.STSUtils;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.xml.ws.security.trust.WSTrustElementFactory;
import com.sun.identity.wss.sts.STSConstants;
import com.sun.identity.plugin.session.impl.FMSessionProvider;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.plugin.session.SessionException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.wss.sts.FAMSTSException;
import com.sun.identity.wss.sts.STSClientUserToken;
import com.sun.identity.wss.security.SAML11AssertionValidator;
import com.sun.identity.wss.security.SAML2AssertionValidator;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.wss.security.SecurityException;
import com.sun.identity.wss.sts.config.FAMSTSConfiguration;
import com.sun.identity.wss.security.SecurityToken;
import com.sun.identity.wss.security.WSSConstants;
import com.sun.identity.wss.security.UserNameTokenSpec;
import com.iplanet.services.util.Crypt;
import com.sun.identity.wss.security.SecurityTokenFactory;
import com.sun.identity.wss.security.WSSUtils;
import com.sun.identity.wss.security.SecurityMechanism;
import com.sun.identity.wss.security.AssertionTokenSpec;
import com.sun.identity.wss.security.AssertionToken;
import com.sun.identity.wss.security.SAML2TokenSpec;
import com.sun.identity.wss.security.SAML2Token;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.wss.logging.LogUtil;
import com.sun.identity.common.SystemConfigurationUtil;


public class FAMSTSTokenProvider implements STSTokenProvider {
    
    public void generateToken(IssuedTokenContext ctx) throws WSTrustException {
    
        STSUtils.debug.message("FAMSTSTokenProvider.generateToken called.");
           
        String issuer = ctx.getTokenIssuer();
        String appliesTo = ctx.getAppliesTo();
        String tokenType = ctx.getTokenType(); 
        
        //Check
        if(tokenType != null && tokenType.equals(
                          SecurityToken.WSS_FAM_SSO_TOKEN)) {
           generateSSOToken(ctx);
           return;
        }
        
        if(tokenType != null && 
                (tokenType.equals(WSSConstants.PASSWORD_PLAIN_TYPE) ||
                 tokenType.equals(WSSConstants.PASSWORD_DIGEST_TYPE))) {
           generateUserNameToken(ctx, tokenType);
           return;
        }
        String keyType = ctx.getKeyType();
        long tokenLifeSpan = 
            (ctx.getExpirationTime().getTime() - ctx.getCreationTime().
            getTime());
        String confirMethod = 
            (String)ctx.getOtherProperties().get(
            IssuedTokenContext.CONFIRMATION_METHOD);
        // Bug in WSIT
        if("urn:oasis:names:tc:SAML:1.0:cm::sender-vouches".equals(
                confirMethod)) {
           confirMethod = STSConstants.SAML_SENDER_VOUCHES_1_0;
        }
        Map<QName, List<String>> claimedAttrs = 
            (Map<QName, List<String>>) ctx.getOtherProperties().get(
            IssuedTokenContext.CLAIMED_ATTRUBUTES);
        WSTrustVersion wstVer = 
            (WSTrustVersion)ctx.getOtherProperties().get(
            IssuedTokenContext.WS_TRUST_VERSION);
        WSTrustElementFactory eleFac = 
            WSTrustElementFactory.newInstance(wstVer);
        
        final X509Certificate stsCert = 
            (X509Certificate)ctx.getOtherProperties().get(
            IssuedTokenContext.STS_CERTIFICATE);
        String stsCertAlias = WSSUtils.getXMLSignatureManager().
                getKeyProvider().getCertificateAlias(stsCert);
        
        // Create the KeyInfo for SubjectConfirmation
        final KeyInfo keyInfo = createKeyInfo(ctx);
        
        // Create AssertionID
        final String assertionId = "uuid-" + UUID.randomUUID().toString();
        
        if(STSUtils.debug.messageEnabled()) {
            STSUtils.debug.message("FAMSTSTokenProvider.tokenType : " 
                + tokenType);
        }
        
        //Create SAML Assertion
        Element assertionE = null;
        
        if (WSTrustConstants.SAML10_ASSERTION_TOKEN_TYPE.equals(tokenType)||
            WSTrustConstants.SAML11_ASSERTION_TOKEN_TYPE.equals(tokenType)) {
            String authMethod = getAuthnMechanism(ctx);
            try {
                assertionE = createSAML11Assertion(wstVer, tokenLifeSpan,
                             confirMethod, authMethod, issuer, appliesTo, 
                             keyInfo, claimedAttrs,
                             keyType, assertionId, stsCertAlias);                
                if(LogUtil.isLogEnabled()) {
                   String[] data = {assertionId,issuer,appliesTo,confirMethod,
                                 tokenType,keyType};
                   LogUtil.access(Level.INFO,
                           LogUtil.CREATED_SAML11_ASSERTION,
                           data,
                           null);
                }
            } catch (FAMSTSException fse) {
                STSUtils.debug.error("FAMSTSTokenProvider.generateToken: " +
                        "Could not generate SAML11 Assertion", fse);
                throw new WSTrustException(fse.getMessage());
            }
            
        } else if (tokenType == null || 
               WSTrustConstants.SAML20_ASSERTION_TOKEN_TYPE.equals(
               tokenType)){
            String authnCtx = getAuthContextClassRef(ctx);
            try {
                assertionE =
                createSAML20Assertion(wstVer, tokenLifeSpan, confirMethod,
                assertionId, issuer, appliesTo, keyInfo, claimedAttrs, keyType,
                authnCtx, stsCertAlias);
                if(LogUtil.isLogEnabled()) {
                   String[] data = {assertionId,issuer,appliesTo,confirMethod,
                                    tokenType,keyType};
                   LogUtil.access(Level.INFO,
                                  LogUtil.CREATED_SAML20_ASSERTION,
                                  data,
                                  null);
                }
            } catch (FAMSTSException fse) {
                STSUtils.debug.error("FAMSTSTokenProvider.generateToken: " +
                        "Could not generate SAML2 Assertion", fse);
                throw new WSTrustException(fse.getMessage());
            }   
             
        } else {
            // TBD : Need to add code for UserName token creation and 
            // X509 token creation.
            STSUtils.debug.error("FAMSTSTokenProvider.generateToken ERROR : " + 
                "UNSUPPORTED_TOKEN_TYPE");
            String[] data = {tokenType};
            LogUtil.error(Level.INFO,
                        LogUtil.UNSUPPORTED_TOKEN_TYPE,
                        data,
                        null);
            throw new WSTrustException(
                LogStringsMessages.WST_0031_UNSUPPORTED_TOKEN_TYPE(
                tokenType, appliesTo));
        }
                      
        if(STSUtils.debug.messageEnabled()) {
            STSUtils.debug.message("FAMSTSTokenProvider.signedAssertion : " + 
                XMLUtils.print(assertionE));
        }
          
        ctx.setSecurityToken(new GenericToken(assertionE));        
        // Create References
        String valueType = null;
        if (WSTrustConstants.SAML10_ASSERTION_TOKEN_TYPE.equals(tokenType)||
            WSTrustConstants.SAML11_ASSERTION_TOKEN_TYPE.equals(tokenType)){
            valueType = MessageConstants.WSSE_SAML_KEY_IDENTIFIER_VALUE_TYPE;
        } else if (WSTrustConstants.SAML20_ASSERTION_TOKEN_TYPE.equals(
            tokenType)){
            valueType = 
                MessageConstants.WSSE_SAML_v2_0_KEY_IDENTIFIER_VALUE_TYPE;
        }
        final SecurityTokenReference samlReference = 
            WSTrustUtil.createSecurityTokenReference(assertionId, valueType);
        final RequestedAttachedReference raRef =  
            eleFac.createRequestedAttachedReference(samlReference);
        final RequestedUnattachedReference ruRef =  
            eleFac.createRequestedUnattachedReference(samlReference);
        ctx.setAttachedSecurityTokenReference(samlReference);
        ctx.setUnAttachedSecurityTokenReference(samlReference);
    }

    public void isValideToken(IssuedTokenContext ctx) throws WSTrustException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void renewToken(IssuedTokenContext ctx) throws WSTrustException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void invalidateToken(IssuedTokenContext ctx) 
        throws WSTrustException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    protected Element createSAML11Assertion(final WSTrustVersion wstVer,
        long lifeSpan, String confirMethod, final String authMethod,
        final String issuer, final String appliesTo, final KeyInfo keyInfo,
        final Map<QName, List<String>> claimedAttrs, String keyType,
        String assertionId, String stsKey) throws FAMSTSException {       
        try {                              
            SecurityTokenFactory stFactory = SecurityTokenFactory.getInstance(
                    WSSUtils.getAdminToken());
            String subjectName = getSubjectName(claimedAttrs);
            if(subjectName == null) {
               if(STSUtils.debug.warningEnabled()) {
                  STSUtils.debug.warning("FAMSTSTokenProvider.createSAML11" +
                          "Assertion: subject is null"); 
               }
               throw new FAMSTSException(
                       STSUtils.bundle.getString("nullSubject"));
            }
            if (confirMethod == null){
                if (keyType.equals(wstVer.getBearerKeyTypeURI())){
                    confirMethod = STSConstants.SAML_SENDER_VOUCHES_1_0;
                } else {
                    confirMethod = STSConstants.SAML_HOLDER_OF_KEY_1_0;
                }
            }
            AssertionTokenSpec tokenSpec = new AssertionTokenSpec();
            NameIdentifier nameID = new NameIdentifier(subjectName);         
            tokenSpec.setSenderIdentity(nameID);
            tokenSpec.setAppliesTo(appliesTo);
            tokenSpec.setAssertionInterval(lifeSpan);
            tokenSpec.setIssuer(issuer);
            tokenSpec.setConfirmationMethod(confirMethod);
            tokenSpec.setClaimedAttributes(claimedAttrs);            
            X509Certificate cert = keyInfo.getX509Certificate();
            if(cert != null) {
               String clientCert = WSSUtils.getXMLSignatureManager().
                       getKeyProvider().getCertificateAlias(cert);
               tokenSpec.setSubjectCertAlias(clientCert);
            }
            tokenSpec.setAuthenticationMethod(authMethod);
            tokenSpec.setAssertionID(assertionId);
            tokenSpec.setSigningAlias(stsKey);
            if(keyInfo != null) {
               tokenSpec.setKeyInfo(keyInfo.getElement());
            }
            AssertionToken token =
                    (AssertionToken)stFactory.getSecurityToken(tokenSpec);
            return token.toDocumentElement();

        } catch (SecurityException se) {
            STSUtils.debug.error("FAMSTSTokenProvider.createSAML11Assertion:" +
                    " failed in creating SAML11 Token", se);
            throw new FAMSTSException(se.getMessage());
        } catch (SAMLException sle) {
            STSUtils.debug.error("FAMSTSTokenProvider.createSAML11Assertion:" +
                    " failed in creating SAML11 Token", sle);
            throw new FAMSTSException(sle.getMessage());
        } catch (KeyResolverException ke) {
            STSUtils.debug.error("FAMSTSTokenProvider.createSAML11Assertion:" +
                    " failed in creating SAML11 Token", ke);
            throw new FAMSTSException(ke.getMessage());
        }
    }
    
    protected Element createSAML20Assertion(final WSTrustVersion wstVer,
        long lifeSpan, String confirMethod, String assertionId,
        final String issuer, final String appliesTo, final KeyInfo keyInfo,
        final  Map<QName, List<String>> claimedAttrs, String keyType,
        String authnCtx, String stsKey) throws FAMSTSException {

        try {           
            SecurityTokenFactory stFactory = SecurityTokenFactory.getInstance(
                      WSSUtils.getAdminToken());
            String subjectName = getSubjectName(claimedAttrs);
            if(subjectName == null) {
               if(STSUtils.debug.warningEnabled()) {
                  STSUtils.debug.warning("FAMSTSTokenProvider.createSAML2" +
                          "Assertion: subject is null"); 
               }
               throw new FAMSTSException(
                       STSUtils.bundle.getString("nullSubject"));
            }
            if (confirMethod == null) {
                if (keyType.equals(wstVer.getBearerKeyTypeURI())){
                    confirMethod = STSConstants.SAML_SENDER_VOUCHES_2_0;
                } else {
                    confirMethod = STSConstants.SAML_HOLDER_OF_KEY_2_0;                  
                }
            }
            SAML2TokenSpec tokenSpec = new SAML2TokenSpec();
            AssertionFactory assertionFactory = AssertionFactory.getInstance();
            NameID nameID = assertionFactory.createNameID();
            nameID.setValue(subjectName);
            nameID.setNameQualifier(issuer);
            tokenSpec.setAssertionID(assertionId);
            tokenSpec.setSenderIdentity(nameID);
            tokenSpec.setConfirmationMethod(confirMethod);
            X509Certificate cert = keyInfo.getX509Certificate();
            if(cert != null) {
               String clientCert = WSSUtils.getXMLSignatureManager().
                       getKeyProvider().getCertificateAlias(cert);
               tokenSpec.setSubjectCertAlias(clientCert);
            }
            tokenSpec.setAppliesTo(appliesTo);
            tokenSpec.setClaimedAttributes(claimedAttrs);
            tokenSpec.setAssertionInterval(lifeSpan);
            tokenSpec.setAuthnContextClassRef(authnCtx);
            tokenSpec.setIssuer(issuer);
            tokenSpec.setSigningAlias(stsKey);
            if(keyInfo != null) {
               tokenSpec.setKeyInfo(keyInfo.getElement());
            }
            SAML2Token saml2Token =
                    (SAML2Token)stFactory.getSecurityToken(tokenSpec);
            return saml2Token.toDocumentElement();
        } catch (SecurityException se) {
            STSUtils.debug.error("FAMSTSTokenProvider.createSAML2Assertion: " +
                    " failed in creating SAML20 Token", se);
            throw new FAMSTSException(se.getMessage());
        } catch (SAML2Exception s2e) {
             STSUtils.debug.error("FAMSTSTokenProvider.createSAML2Assertion: " +
                    " failed in creating SAML20 Token", s2e);
            throw new FAMSTSException(s2e.getMessage());
        } catch (KeyResolverException ke) {
             STSUtils.debug.error("FAMSTSTokenProvider.createSAML2Assertion: " +
                    " failed in creating SAML20 Token", ke);
            throw new FAMSTSException(ke.getMessage());
        }
    }
        
    private String getSubjectName(Map claimedAttrs) {
        Set<Map.Entry<QName, List<String>>> entries = claimedAttrs.entrySet();
        for(Map.Entry<QName, List<String>> entry : entries){
            QName attrKey = entry.getKey();
            List<String> values = entry.getValue();
            if (values != null && values.size() > 0){
                if (STSAttributeProvider.NAME_IDENTIFIER.equals(
                            attrKey.getLocalPart())){
                    return values.get(0);                                                
                 }
            }
        }
        return null;
    }
     
    private KeyInfo createKeyInfo(final IssuedTokenContext ctx) throws 
        WSTrustException {
        
        Element kiEle = 
            (Element)ctx.getOtherProperties().get("ConfirmationKeyInfo");
        if (kiEle != null){
            try {
                return new KeyInfo(kiEle, null);
            } catch(XMLSecurityException ex){
                STSUtils.debug.error("FAMSTSTokenProvider.createKeyInfo : " + 
                "UNABLE_GET_CLIENT_CERT : ", ex);
                throw new WSTrustException(
                    LogStringsMessages.WST_0034_UNABLE_GET_CLIENT_CERT(), ex);
            }
        }
        Document doc = null;
        try{
            doc = XMLUtils.getSafeDocumentBuilder(false).newDocument();
        }catch(ParserConfigurationException ex){
            STSUtils.debug.error("FAMSTSTokenProvider.createKeyInfo : " + 
                "ERROR_CREATING_DOCFACTORY : ", ex);
            throw new WSTrustException(
                LogStringsMessages.WST_0039_ERROR_CREATING_DOCFACTORY(), ex);
        }
        
        final String appliesTo = ctx.getAppliesTo();
        final KeyInfo keyInfo = new KeyInfo(doc);
        String keyType = ctx.getKeyType();
        WSTrustVersion wstVer = 
            (WSTrustVersion)ctx.getOtherProperties().get(
            IssuedTokenContext.WS_TRUST_VERSION);
        if (wstVer.getSymmetricKeyTypeURI().equals(keyType)){
            final byte[] key = ctx.getProofKey();
            try {
                final X509Certificate cert =
                    (X509Certificate)ctx.getOtherProperties().get(IssuedTokenContext.TARGET_SERVICE_CERTIFICATE);
                final EncryptedKey encKey = WSSUtils.encryptKey(doc, key, cert, null);
                keyInfo.add(encKey);
            } catch (Exception ex) {
                 STSUtils.debug.error("FAMSTSTokenProvider.createKeyInfo : " + 
                "ERROR_ENCRYPT_PROOFKEY : ", ex);
                 throw new WSTrustException(
                     LogStringsMessages.WST_0040_ERROR_ENCRYPT_PROOFKEY(
                     appliesTo), ex);
            }
        } else if(wstVer.getPublicKeyTypeURI().equals(keyType)){
            final X509Data x509data = new X509Data(doc);
            try {
                x509data.addCertificate(ctx.getRequestorCertificate());
            } catch(XMLSecurityException ex) {
                STSUtils.debug.error("FAMSTSTokenProvider.createKeyInfo : " + 
                "UNABLE_GET_CLIENT_CERT : ", ex);
                throw new WSTrustException(
                    LogStringsMessages.WST_0034_UNABLE_GET_CLIENT_CERT(), ex);
            }
            keyInfo.add(x509data);
        }
        
        return keyInfo;
    }
    
    /**
     * Generates OpenSSO SSOToken by consuming SAML Assertion.
     * @param ctx Issued Token Context from WS-Trust Request
     * @throws com.sun.xml.ws.api.security.trust.WSTrustException
     */
    private void generateSSOToken(IssuedTokenContext ctx) 
             throws WSTrustException {
        
        javax.security.auth.Subject subject = ctx.getRequestorSubject();
        if(subject == null) {
           throw new WSTrustException(STSUtils.bundle.getString("nullSubject")); 
        }        
        
        String subjectName = null;
        Map attributeMap = null;
        FAMSTSConfiguration stsConfig = new FAMSTSConfiguration();
        
        Iterator iter = subject.getPublicCredentials().iterator();
        while(iter.hasNext()) {
            Object object = iter.next();
            if(object instanceof Element) {
               Element famToken = (Element)object;
               if(!famToken.getLocalName().equals("FAMToken")) {
                  continue; 
               }
               Element assertionE = null;
               try {
                   STSClientUserToken oboToken = 
                           new STSClientUserToken(famToken);
                   String tokenID = oboToken.getTokenId();
                   assertionE = XMLUtils.toDOMDocument(
                           tokenID, STSUtils.debug).getDocumentElement();
               } catch (FAMSTSException se) {
                   throw new WSTrustException(se.getMessage());
               }
               if(assertionE == null) {
                  throw new WSTrustException(
                          STSUtils.bundle.getString("nullAssertion"));
               }
               if(assertionE.getLocalName().equals("Assertion")) {                   
                  String namespace = assertionE.getNamespaceURI();
                  try {
                      if(SAMLConstants.assertionSAMLNameSpaceURI.equals(
                              namespace)) {                
                         SAML11AssertionValidator validator = 
                             new SAML11AssertionValidator(assertionE, stsConfig);
                         subjectName = validator.getSubjectName();
                         attributeMap = validator.getAttributes();                      
                      } else if (SAML2Constants.ASSERTION_NAMESPACE_URI.equals(
                              namespace)) {
                         SAML2AssertionValidator validator =
                             new SAML2AssertionValidator(assertionE, stsConfig);
                         subjectName = validator.getSubjectName();
                         attributeMap = validator.getAttributes();
                      }
                  } catch (SecurityException se) {                      
                      throw new WSTrustException(se.getMessage());
                  }
               }
            }
        }
        if(subjectName == null) {
           throw new WSTrustException(
                   STSUtils.bundle.getString("assertion subject is null")); 
        }
        Map info = new HashMap();
        info.put(SessionProvider.REALM, "/");
        info.put(SessionProvider.PRINCIPAL_NAME, subjectName);
        info.put(SessionProvider.AUTH_LEVEL, "0");
        FMSessionProvider sessionProvider = new FMSessionProvider();
        try {
            SSOToken ssoToken = (SSOToken)sessionProvider.createSession(
                    info, null, null, null);
            if(attributeMap != null && !attributeMap.isEmpty()) {
               for(Iterator attrIter =  attributeMap.keySet().iterator();
                            attrIter.hasNext();) {
                   String attrName = (String)attrIter.next();
                   String attrValue = (String)attributeMap.get(attrName);
                   ssoToken.setProperty(attrName, attrValue);
                   if(STSUtils.debug.messageEnabled()) {
                       STSUtils.debug.message(
                           "FAMSTSTokenProvider.generateSSOToken: " +
                           "setting session property " + attrName + "=" +
                           attrValue);
                   }
               }
            }
            STSClientUserToken wscToken = new STSClientUserToken();
            wscToken.init(ssoToken);
            ctx.setSecurityToken(wscToken);            
        } catch (SessionException se) {
            STSUtils.debug.error("FAMSTSTokenProvider.generateSSOToken: " +
                    "session exception ", se);
            throw new WSTrustException(se.getMessage());                    
        } catch (FAMSTSException fe) {
            STSUtils.debug.error("FAMSTSTokenProvider.generateSSOToken: " +
                    "FAMSTSException ", fe);
            throw new WSTrustException(fe.getMessage());                    
        } catch (SSOException ssoe) {
            STSUtils.debug.error("FAMSTSTokenProvider.generateSSOToken: " +
                    "SSOException ", ssoe);
            throw new WSTrustException(ssoe.getMessage());                    
        }
    }
    
    private void generateUserNameToken(IssuedTokenContext ctx, 
            String tokenType) throws WSTrustException {
        
        javax.security.auth.Subject subject = ctx.getRequestorSubject();
        if(subject == null) {
           return;
        }                        
        Iterator iter = subject.getPublicCredentials().iterator();
        while(iter.hasNext()) {
            Object object = iter.next();
            if(object instanceof Element) {
               Element credential = (Element)object;
               if(credential.getLocalName().equals("FAMToken")) {
                  try {
                      STSClientUserToken userToken =
                          new STSClientUserToken(credential);
                      String tokenID = userToken.getTokenId();
                      if(!userToken.getType().equals(
                              SecurityToken.WSS_FAM_SSO_TOKEN)) {
                         continue;
                      }                      
                      SSOToken ssoToken = 
                          SSOTokenManager.getInstance().createSSOToken(tokenID);
                      String userid = ssoToken.getProperty("UserId");
                      String password = null;
                      boolean useHashedPassword = Boolean.valueOf(
                              SystemConfigurationUtil.getProperty(
                              "com.sun.identity.wss.security.useHashedPassword",
                              "true"));
                      if(!useHashedPassword) {
                         String encryptedPassword = ssoToken.getProperty(
                              WSSConstants.ENCRYPTED_USER_PASSWORD);
                         if(encryptedPassword == null || 
                                 encryptedPassword.length() ==0) {
                            throw new WSTrustException("noEncryptedPassword");
                         }
                          password = Crypt.decrypt(encryptedPassword);
                      
                      } else {
                         password = ssoToken.getProperty(
                                 WSSConstants.HASHED_USER_PASSWORD); 
                      }
                      
                      if(password == null || password.length() ==0) {
                         throw new WSTrustException("noUserPassword");
                      }
                     
                      UserNameTokenSpec tokenSpec = new UserNameTokenSpec();
                      tokenSpec.setCreateTimeStamp(true);
                      tokenSpec.setNonce(true);
                      tokenSpec.setPassword(password);
                      tokenSpec.setUserName(userid);
                      tokenSpec.setPasswordType(tokenType);                              
                      SecurityTokenFactory tokenFactory = 
                              SecurityTokenFactory.getInstance(
                              WSSUtils.getAdminToken());
                      SecurityToken securityToken = 
                              tokenFactory.getSecurityToken(tokenSpec);
                      ctx.setSecurityToken(
                          new GenericToken(securityToken.toDocumentElement()));
                  } catch (FAMSTSException fe) {
                     STSUtils.debug.error(
                         "FAMSTSTokenProvider.generateUserNameToken: " +                         
                          "FAMSTSException ", fe);
                     throw new WSTrustException(fe.getMessage()); 
                  } catch (SSOException se) {
                     STSUtils.debug.error(
                         "FAMSTSTokenProvider.generateUserNameToken: " +                         
                          "SSOException ", se);
                     throw new WSTrustException(se.getMessage()); 
                  } catch (SecurityException see) {
                     STSUtils.debug.error(
                         "FAMSTSTokenProvider.generateUserNameToken: " +                         
                          "SSOException ", see);
                     throw new WSTrustException(see.getMessage());                       
                  }
               }
            }
        }
        
    }
    
    private String getAuthnMechanism(IssuedTokenContext ctx) {
        
        Subject subject = ctx.getRequestorSubject();
        if(subject == null) {
           return null; 
        }         
        Set creds = subject.getPublicCredentials();
        if(creds == null || creds.isEmpty()) {
           return null; 
        }
        for (Iterator iter=creds.iterator();iter.hasNext();) {
           Object obj = iter.next();
           if(obj instanceof Map) {
              Map secureAttrs = (Map)obj;
              return (String)secureAttrs.get(WSSConstants.AUTH_METHOD);              
           }           
        }
        return null;
    } 
    
    private String getAuthContextClassRef(IssuedTokenContext ctx) {
        String authMech = getAuthnMechanism(ctx);
        if(SecurityMechanism.WSS_NULL_KERBEROS_TOKEN_URI.equals(authMech)) {
           return WSSConstants.KERBEROS_AUTH_CTX_CLASS_REF; 
        } else if(
           SecurityMechanism.WSS_NULL_USERNAME_TOKEN_PLAIN_URI.equals(authMech)) {
           return WSSConstants.PASSWORD_AUTH_CTX_CLASS_REF; 
        } else if(
           SecurityMechanism.WSS_NULL_USERNAME_TOKEN_URI.equals(authMech)) {
           return WSSConstants.PASSWORD_PROTECTED_AUTH_CTX_CLASS_REF; 
        } else if(SecurityMechanism.WSS_NULL_X509_TOKEN_URI.equals(authMech)) {
           return WSSConstants.PUBLIC_KEY_AUTH_CTX_CLASS_REF;
        } else {
           return WSSConstants.SOFTWARE_PKI_AUTH_CTX_CLASS_REF; 
        }
        
    }    
    
 
}
