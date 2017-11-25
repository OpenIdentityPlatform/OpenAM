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
 * $Id: SAML2Token.java,v 1.12 2010/01/23 00:20:26 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wss.security;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import java.security.cert.X509Certificate;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.RSAPublicKey;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import javax.xml.namespace.QName;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.encode.Base64;

import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.assertion.AuthnStatement;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.SubjectConfirmation;
import com.sun.identity.saml2.assertion.SubjectConfirmationData;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.assertion.AudienceRestriction;
import com.sun.identity.saml2.assertion.Conditions;
import com.sun.identity.saml2.assertion.AttributeStatement;
import com.sun.identity.saml2.assertion.Attribute;

/**
 * This class <code>SAML2Token</code> represents a SAML2
 * token that can be inserted into web services security header
 * for message level security.
 *
 * <p>This class implements <code>SecurityToken</code> and can be
 * created through security token factory. 
 */
public class SAML2Token implements SecurityToken {
    
    private SAML2TokenSpec spec = null;
    private String authType = "";
    private String authTime = "";
    private Assertion assertion;
    private String certAlias = "";
    private String signingAlias = "";
    private Element assertionE = null;    
    private static final String KEY_INFO_TYPE =
         "com.sun.identity.liberty.ws.security.keyinfotype";
    private static String keyInfoType = SystemConfigurationUtil.getProperty(
                                          KEY_INFO_TYPE);
    private static AssertionFactory factory = AssertionFactory.getInstance();
        
    /**
     * Constructor that initializes the SAML2Token.
     */ 
     public SAML2Token(SAML2TokenSpec spec, 
                  SSOToken ssoToken) throws SecurityException {
 
         if(spec == null) {
            WSSUtils.debug.error("SAML2Token: constructor: SAML2" +
                  " Token specification is null");
            throw new SecurityException(
                   WSSUtils.bundle.getString("tokenSpecNotSpecified"));
         }

         validateSSOToken(ssoToken);
         this.spec = spec;
         createAssertion();
     }
     
     public SAML2Token(Element element) 
                   throws SAML2Exception {
         assertionE = element;
         assertion = factory.createAssertion(element);         
     }
     
     /**
       * Validates the SSOtoken and extract the required properties.
       */
      private void validateSSOToken(SSOToken ssoToken) 
                      throws SecurityException {
         try {
             SSOTokenManager.getInstance().validateToken(ssoToken);
             authType = ssoToken.getAuthType();
             authTime =  ssoToken.getProperty("authInstant");

         } catch (SSOException se) {
             WSSUtils.debug.error("AssertionToken.validateSSOToken: " +
               "SSOException", se);
             throw new SecurityException(
                   WSSUtils.bundle.getString("invalidSSOToken"));
         }
      }
      
      /** 
       * Returns the security token type.
       * @return String SAMLToken type.
       */
      public String getTokenType() {
          return SecurityToken.WSS_SAML2_TOKEN;
      }

      /**
       * Create Assertion using SAML2 token specification
       */
      private void createAssertion() throws SecurityException {
          assertion = factory.createAssertion();          
          SecurityMechanism securityMechanism = spec.getSecurityMechanism();
          NameID nameIdentifier = spec.getSenderIdentity();
          certAlias = spec.getSubjectCertAlias();

          if(nameIdentifier == null) {
             throw new SecurityException(
                   WSSUtils.bundle.getString("invalidSAML2TokenSpec"));
          }

          
          String confirmationMethod = spec.getConfirmationMethod();
          if(confirmationMethod == null) {
             confirmationMethod = 
                     getConfirmationMethod(securityMechanism.getURI());
          }

          // TODO: Read the issuer from the trustd authority configuration
          // when the STS is ready.
          try {
              assertion.setVersion("2.0");
              String assertionID = spec.getAssertionID();
              if(assertionID == null) {
                 assertionID =  SAML2SDKUtils.generateID();
              }
              assertion.setID(assertionID);
              Issuer issuer = factory.createIssuer();
              String issuerName = spec.getIssuer();
              if(issuerName == null) {
                 issuerName =  SystemConfigurationUtil.getProperty(
                               Constants.AM_SERVER_HOST);
              }
              issuer.setValue(issuerName);          
              assertion.setIssuer(issuer);
          
              Date issueInstant = new Date();
              Date expiryTime = null;
              long assertionInterval = spec.getAssertionInterval();
              if(assertionInterval != 0) {
                 expiryTime = new Date(issueInstant.getTime() 
                         + assertionInterval);
              }
              assertion.setIssueInstant(issueInstant);
              assertion.setSubject(
                    createSubject(nameIdentifier, confirmationMethod));
              List authnStatements = new ArrayList();          
              authnStatements.add(createAuthnStatement());
              assertion.setAuthnStatements(authnStatements);
              
              Map attributes = spec.getClaimedAttributes();
              if(attributes != null && !attributes.isEmpty()) {
                 AttributeStatement attrStatement = 
                             createAttributeStatement();
                 if(attrStatement != null) {
                    assertion.getAttributeStatements().add(attrStatement);
                 }
              }
          
              if(WSSUtils.debug.messageEnabled()) {
                 WSSUtils.debug.message("SAML2Token.createAssertion: " +
                 "Assertion constructs:\n" +
                 "Confirmation method: " + confirmationMethod + "\n" +
                 "Issuer: " + issuer + "\n");
               }
                                        
               Conditions conds = factory.createConditions();
               conds.setNotBefore(issueInstant);
               if(expiryTime != null) {
                  conds.setNotOnOrAfter(expiryTime); 
               }
               String appliesTo = spec.getAppliesTo();
               if(appliesTo != null) {
                  AudienceRestriction arc = factory.createAudienceRestriction();
                  List auds = new ArrayList();
                  auds.add(appliesTo);
                  arc.setAudience(auds);
                  List list = new ArrayList();
                  list.add(arc);                  
                  conds.setAudienceRestrictions(list);                  
               }
               assertion.setConditions(conds);
               
          } catch (SAML2Exception se) {
              WSSUtils.debug.error("SAML2Token.createAssertion:", se);
              throw new SecurityException(WSSUtils.bundle.getString(
                      "unableToGenerateAssertion"));
          }
      }
            
      /**
       * Creates a subject
       */
      private Subject createSubject (
            NameID nameIdentifier,
            String confirmationMethod) throws SecurityException {

          try {
              Subject subject = factory.createSubject();
              subject.setNameID(nameIdentifier);              
              if(confirmationMethod == null) {
                 throw new SecurityException(
                       WSSUtils.bundle.getString("nullConfirmationMethod"));
              }

              SubjectConfirmation subConfirmation = 
                      factory.createSubjectConfirmation();              
              SubjectConfirmationData confirmationData =
                      factory.createSubjectConfirmationData();
              
              if(confirmationMethod.equals(                      
                   SAML2Constants.SUBJECT_CONFIRMATION_METHOD_HOLDER_OF_KEY)) {
                 subConfirmation.setMethod(confirmationMethod);
                 /** Websphere does not like the xsi type.
                  **/
                  //confirmationData.setContentType(
                   //     WSSConstants.KEY_INFO_DATA_TYPE);
                 confirmationData.getContent().add(createKeyInfo()); 
                 subConfirmation.setSubjectConfirmationData(confirmationData);
 
              } else if(confirmationMethod.equals(
                   SAML2Constants.SUBJECT_CONFIRMATION_METHOD_SENDER_VOUCHES)) {
                 subConfirmation.setMethod(confirmationMethod);
                 
              } else if(confirmationMethod.equals(
                   SAML2Constants.SUBJECT_CONFIRMATION_METHOD_BEARER)) {
                 subConfirmation.setMethod(confirmationMethod);

              } else {
                 throw new SecurityException(
                       WSSUtils.bundle.getString("invalidConfirmationMethod"));
              }
              List list = new ArrayList();
              list.add(subConfirmation);
              subject.setSubjectConfirmation(list);
              return subject;
          } catch (SAML2Exception se) {
              WSSUtils.debug.error("AssertionToken.getAuthenticationStatement:"+
              "Failed to generate the authentication statement.", se);
              throw new SecurityException(
                       WSSUtils.bundle.getString("unabletoGenerateAssertion"));
          }


      }
      
      /**
       * creates an authentication statement.
       */
      private AuthnStatement createAuthnStatement() throws SecurityException {
          try {
              AuthnStatement authnStatement = factory.createAuthnStatement();
              authnStatement.setAuthnInstant(new Date());
              AuthnContext authnContext = factory.createAuthnContext();
              String authnCtxClassRef = spec.getAuthnContextClassRef();
              if(authnCtxClassRef == null) {
                 authnCtxClassRef =  
                         WSSConstants.CLASSREF_AUTHN_CONTEXT_SOFTWARE_PKI;
              }
              authnContext.setAuthnContextClassRef(authnCtxClassRef);                  
              authnStatement.setAuthnContext(authnContext);                            
              return authnStatement;
          } catch (SAML2Exception se) {
              WSSUtils.debug.error("SAML2Token.createAuthnStatement: SAML2" +
                      "Exception ", se);
              throw new SecurityException(
                      WSSUtils.bundle.getString("unableToGenerateAssertion"));
          }

      }
      
      public  Assertion getAssertion() {
          return assertion;
      }
      
      /**
       * Returns the confirmation method for the given security mech.
       */
      private String getConfirmationMethod(String securityURI) 
               throws SecurityException {

          if(securityURI == null) {
             throw new SecurityException(
                  WSSUtils.bundle.getString("nullSecurityMechanism"));
          }

          if(securityURI.equals(SecurityMechanism.WSS_NULL_SAML2_HK_URI)||
             securityURI.equals(SecurityMechanism.WSS_TLS_SAML2_HK_URI) ||
             securityURI.equals(SecurityMechanism.WSS_CLIENT_TLS_SAML2_HK_URI)){
             return SAML2Constants.SUBJECT_CONFIRMATION_METHOD_HOLDER_OF_KEY;

          } else if(
             securityURI.equals(SecurityMechanism.WSS_NULL_SAML2_SV_URI)||
             securityURI.equals(SecurityMechanism.WSS_TLS_SAML2_SV_URI) ||
             securityURI.equals(SecurityMechanism.WSS_CLIENT_TLS_SAML2_SV_URI)){
             return SAML2Constants.SUBJECT_CONFIRMATION_METHOD_SENDER_VOUCHES;

          } else {
             throw new SecurityException(
                   WSSUtils.bundle.getString("invalidConfirmationMethod"));
          }
      }
      
      /**
       * creates key info
       */
      private Element createKeyInfo() throws SecurityException {
          Element keyInfo = spec.getKeyInfo();
          if(keyInfo != null) {
             return keyInfo; 
          }
          X509Certificate cert = getX509Certificate();
          Document doc = null;
          try {
              doc = XMLUtils.newDocument();
          } catch (Exception e) {
              throw new SecurityException(e.getMessage());
          }

          String keyNameTextString = null;
          String base64CertString = null;

          PublicKey pk = null;
          try {
              pk = cert.getPublicKey();
              keyNameTextString = cert.getSubjectDN().getName();
              base64CertString = Base64.encode(cert.getEncoded());
          } catch (Exception e) {
              WSSUtils.debug.error("SAML2Token.createKeyInfo: ", e);
              throw new SecurityException(e.getMessage());
          }

          keyInfo = doc.createElementNS(
                            SAML2Constants.NS_XMLSIG,
                            WSSConstants.TAG_KEYINFO);
          keyInfo.setPrefix("ds");
          //keyInfo.setAttribute("xmlns", SAML2Constants.NS_XMLSIG);

          if ( (keyInfoType!=null) && 
               (keyInfoType.equalsIgnoreCase("certificate")) ) {
                //put Certificate in KeyInfo
                Element x509Data = doc.createElementNS(
                                SAML2Constants.NS_XMLSIG,
                                WSSConstants.TAG_X509DATA);
                x509Data.setPrefix("ds");
                Element x509Certificate = doc.createElementNS(
                                SAML2Constants.NS_XMLSIG,
                                WSSConstants.TAG_X509CERTIFICATE);
                x509Certificate.setPrefix("ds");
                Text certText = doc.createTextNode(base64CertString);
            x509Certificate.appendChild(certText);
            keyInfo.appendChild(x509Data).appendChild(x509Certificate);
        } else {
            //put public key in keyinfo
            Element keyName = doc.createElementNS(
                            SAML2Constants.NS_XMLSIG,
                            WSSConstants.TAG_KEYNAME);
            keyName.setPrefix("ds");
            Text keyNameText = doc.createTextNode(keyNameTextString);

            Element keyvalue = doc.createElementNS(
                            SAML2Constants.NS_XMLSIG,
                            WSSConstants.TAG_KEYVALUE);
            keyvalue.setPrefix("ds");

            if (pk.getAlgorithm().equals("DSA")) {
                DSAPublicKey dsakey = (DSAPublicKey) pk;
                DSAParams dsaParams = dsakey.getParams();
                BigInteger _p = dsaParams.getP();
                BigInteger _q = dsaParams.getQ();
                BigInteger _g = dsaParams.getG();
                BigInteger _y = dsakey.getY();
                Element DSAKeyValue = doc.createElementNS(
                            SAML2Constants.NS_XMLSIG
                            , "DSAKeyValue");
                DSAKeyValue.setPrefix("ds");
                Element p = doc.createElementNS(
                                SAML2Constants.NS_XMLSIG, "P");
                p.setPrefix("ds");
                Text value_p =
                        doc.createTextNode(Base64.encode(_p.toByteArray()));
                p.appendChild(value_p);
                DSAKeyValue.appendChild(p);

                Element q = doc.createElementNS(
                                SAML2Constants.NS_XMLSIG, "Q");
                q.setPrefix("ds");
                Text value_q =
                        doc.createTextNode(Base64.encode(_q.toByteArray()));
                q.appendChild(value_q);
                DSAKeyValue.appendChild(q);

                Element g = doc.createElementNS(
                                SAML2Constants.NS_XMLSIG, "G");
                g.setPrefix("ds");
                Text value_g =
                        doc.createTextNode(Base64.encode(_g.toByteArray()));
                g.appendChild(value_g);
                DSAKeyValue.appendChild(g);

                Element y = doc.createElementNS(
                                SAML2Constants.NS_XMLSIG, "Y");
                y.setPrefix("ds");
                Text value_y =
                        doc.createTextNode(Base64.encode(_y.toByteArray()));
                y.appendChild(value_y);
                DSAKeyValue.appendChild(y);
                keyvalue.appendChild(DSAKeyValue);

            } else {
                // It is RSA
                RSAPublicKey rsakey = (RSAPublicKey) pk;
                BigInteger exponent = rsakey.getPublicExponent();
                BigInteger modulus  = rsakey.getModulus();
                Element RSAKeyValue = doc.createElementNS(
                                        SAML2Constants.NS_XMLSIG
                                        , "RSAKeyValue");
                RSAKeyValue.setPrefix("ds");
                Element modulusNode = doc.createElementNS(
                                        SAML2Constants.NS_XMLSIG
                                        , "Modulus");
                modulusNode.setPrefix("ds");
                Element exponentNode = doc.createElementNS(
                                        SAML2Constants.NS_XMLSIG
                                        , "Exponent");
                exponentNode.setPrefix("ds");
                RSAKeyValue.appendChild(modulusNode);
                RSAKeyValue.appendChild(exponentNode);
                Text modulusValue =
                    doc.createTextNode(Base64.encode(modulus.toByteArray()));
                modulusNode.appendChild(modulusValue);
                Text exponentValue =
                    doc.createTextNode(Base64.encode(exponent.toByteArray()));
                exponentNode.appendChild(exponentValue);
                keyvalue.appendChild(RSAKeyValue);
            }

            keyInfo.appendChild(keyName).appendChild(keyNameText);
            keyInfo.appendChild(keyvalue);
        }
        return keyInfo;
    }
      
      /**
       * Returns X509 certificate of the authenticated subject.
       */
      public X509Certificate getX509Certificate() throws SecurityException {
          X509Certificate cert = 
                AMTokenProvider.getKeyProvider().getX509Certificate(certAlias);
          if(cert == null) {
             WSSUtils.debug.error("SAML2Token.getX509Certificate: " +
             "Could not get certificate for alias : " + certAlias);
             throw new SecurityException(
                   WSSUtils.bundle.getString("noCertificate"));
          }
          return cert;
      }
      
      /**
       * Returns DOM element for the SAML2 token
       * @return the DOM <code>Element</code> element
       * @exception SecurityException if there is a failure.
       */
      public Element toDocumentElement() throws SecurityException {
          if(assertionE != null) {
              return WSSUtils.getCanonicalElement(assertionE);
          }
          Document document = null;
          try {
              document = XMLUtils.toDOMDocument(
                   assertion.toXMLString(true, true), WSSUtils.debug);
          } catch (SAML2Exception se) {
              WSSUtils.debug.error("SAML2Token.toDocumentElement: failed", se);
              throw new SecurityException(
                 WSSUtils.bundle.getString("cannotConvertToDocument"));
          }
          
          if(document == null) {
             throw new SecurityException(
                 WSSUtils.bundle.getString("cannotConvertToDocument"));
          }
          return WSSUtils.getCanonicalElement(document.getDocumentElement());
      }
      
     /**
      * Signs the SAML2 Token.
      *
      * @exception SecurityException if unable to sign the assertion.
      */
     public void sign(String alias) throws SecurityException {
         try {             
             X509Certificate x509Cert = 
                 AMTokenProvider.getKeyProvider().getX509Certificate(
                 spec.getSigningAlias());
             PrivateKey privateKey = 
                     AMTokenProvider.getKeyProvider().getPrivateKey(alias);
             assertion.sign(privateKey, x509Cert);            
         } catch (SAML2Exception se) {
            WSSUtils.debug.error("AssertionToken.sign: exception", se);
            throw new SecurityException(
                      WSSUtils.bundle.getString("unabletoSign"));
         }
     }
     
     /**
      * Returns true if the SAML2 token is of type sender vouches
      */
     public boolean isSenderVouches() {
         
         Subject subject = assertion.getSubject();
         List list = 
                 subject.getSubjectConfirmation();
         if(list == null || list.isEmpty()) {
            return false;
         }         
         SubjectConfirmation subjConfirmation = 
                 (SubjectConfirmation)list.get(0);
         String confirmationMethod = subjConfirmation.getMethod();
         if (SAML2Constants.SUBJECT_CONFIRMATION_METHOD_SENDER_VOUCHES.
                 equals(confirmationMethod)) {
             return true;
         }
         return false;
     }
     
     private AttributeStatement createAttributeStatement() 
                throws SAML2Exception {
         Map<QName, List<String>> attributes = spec.getClaimedAttributes();         
         if(attributes == null) {
            return null;
         }         
         List samlAttributes = new ArrayList();                  
         Iterator iter = attributes.keySet().iterator();
         while(iter.hasNext()) {
             QName qName = (QName)iter.next();
             String attrName = qName.getLocalPart();
             if("NameID".equals(qName.getLocalPart())) {
                continue; 
             }             
             Attribute attr = factory.createAttribute();
             attr.setName(attrName);
             List values = attributes.get(qName);            
             List elementValues = new ArrayList();
                 for (Iterator iter1=values.iterator(); iter1.hasNext();) {
                     String value = (String)iter1.next();
                     String attrValue = "<saml:AttributeValue>" + value +
                              "</saml:AttributeValue>";
                     elementValues.add(attrValue);
                 }
             attr.setAttributeValue(elementValues);
             samlAttributes.add(attr);            
         }
         if(samlAttributes.isEmpty()) {
            return null;
         }                         
         AttributeStatement attrStatement = factory.createAttributeStatement();
         attrStatement.setAttribute(samlAttributes);
         return attrStatement;
     }
}
