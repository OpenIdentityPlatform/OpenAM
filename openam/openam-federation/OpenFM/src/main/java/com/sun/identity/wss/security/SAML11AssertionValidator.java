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
 * $Id: SAML11AssertionValidator.java,v 1.8 2010/01/15 18:54:34 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wss.security;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.Statement;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.assertion.AuthenticationStatement;
import com.sun.identity.saml.assertion.AttributeStatement;
import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.SubjectConfirmation;
import com.sun.identity.saml.assertion.SubjectLocality;
import com.sun.identity.shared.StringUtils;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.wss.sts.STSConstants;
import java.security.cert.X509Certificate;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.wss.sts.config.FAMSTSConfiguration;

/**
 * This class provides validation functionality for SAML1.x assertions.
 */
public class SAML11AssertionValidator {
    
  
    private static Debug debug = WSSUtils.debug;
    private Map<String, String> attributeMap = new HashMap<String, String>();
    private String subjectName = null;      
    private FAMSTSConfiguration stsConfig = null;
    private X509Certificate cert = null;
          
    public SAML11AssertionValidator(Element assertionE,
        FAMSTSConfiguration stsConfig) throws SecurityException {
        
        debug.message("SAML11AssertionValidator.constructor..");
        this.stsConfig = stsConfig;
        if(stsConfig == null) {
           throw new SecurityException(
                 WSSUtils.bundle.getString("nullConfig"));
        }
        
        try {
            Assertion saml11Assertion = new Assertion(assertionE);
            if(!saml11Assertion.isSigned()) { 
               throw new SecurityException(
                       WSSUtils.bundle.getString("assertionNotSigned"));
            }

            String issuer = saml11Assertion.getIssuer();
            if(issuer == null) {
               throw new SecurityException(
                       WSSUtils.bundle.getString("nullIssuer"));
            }

            Set trustedIssuers = stsConfig.getTrustedIssuers();
            String issuerAlias = null;
            boolean issuerTrusted = false;
            if (trustedIssuers != null && !trustedIssuers.isEmpty()) {
               Iterator iterator = trustedIssuers.iterator();
               while (iterator.hasNext()){
                    String aliasIssuer = (String)iterator.next();
                    if (aliasIssuer.length() > 0) {
                    int index = aliasIssuer.indexOf(":");
                    if (index == -1) {
                        throw new SecurityException(
                            WSSUtils.bundle.getString("issuerOrAliasNull"));
                    } else {
                        issuerAlias = aliasIssuer.substring(0, index).trim();
                        if (issuerAlias.length() > 0) {
                            String configIssuer =
                                    aliasIssuer.substring(index + 1).trim();
                            if (configIssuer.length() > 0) {
                                if (issuer.equals(configIssuer)) {
                                    issuerTrusted = true;
                                    break;
                                }
                            }
                        }
                    }
                    }
                }
            }

            if(!issuerTrusted) {
                throw new SecurityException(
                    WSSUtils.bundle.getString("issuerNotTrusted"));
            }

            //Verify signature on Assertion
            try {
                XMLSignatureManager sigManager =
                    WSSUtils.getXMLSignatureManager();
                Document document = XMLUtils.newDocument();
                document.appendChild(document.importNode(assertionE, true));
                if(WSSUtils.debug.messageEnabled()) {
                    WSSUtils.debug.message("SAML11AssertionValidator:"+
                        " Assertion to be verified" + XMLUtils.print(assertionE));
                }
                if(!sigManager.verifyXMLSignature(document, issuerAlias)){
                    if(WSSUtils.debug.messageEnabled()) {
                        WSSUtils.debug.message("SAML11AssertionValidator:"
                            + " Signature verification for the assertion failed");
                    }
                    throw new SecurityException(
                        WSSUtils.bundle.getString("assertionSigNotVerified"));
                } else {
                    if(WSSUtils.debug.messageEnabled()) {
                        WSSUtils.debug.message("SAML11AssertionValidator: "
                            + "Signature verification successful for the Assertion");
                    }
                }
            } catch (Exception ex) {
                WSSUtils.debug.error("SAML11AssertionValidator:Signature" +
                   " validation on Assertion failed", ex);
                throw new SecurityException(
                    WSSUtils.bundle.getString("signatureValidationFailed"));
            }
                       
            if(!saml11Assertion.isTimeValid()) {               
               throw new SecurityException(WSSUtils.bundle.getString(
                       "assertionTimeNotValid"));               
            }
            
            Set statements = saml11Assertion.getStatement();
            if(statements == null || statements.isEmpty()) {
               throw new SecurityException(
                       WSSUtils.bundle.getString("nullStatments"));
            }
            
            for (Iterator iter = statements.iterator(); iter.hasNext();) {
                 Statement statement = (Statement)iter.next();
                 int type = statement.getStatementType();
                 if(type == Statement.AUTHENTICATION_STATEMENT) {
                   AuthenticationStatement authnStatement = 
                           (AuthenticationStatement)statement;
                   
                   validateAuthnStatement(authnStatement);  
                } else if (type == com.sun.identity.saml.assertion.Statement.
                        ATTRIBUTE_STATEMENT) {
                    AttributeStatement attributeStatement = 
                            (AttributeStatement)statement;
                   validateAttributeStatement(attributeStatement);
                }
            }                       
            
        } catch (SAMLException se) {
            throw new SecurityException(se.getMessage());  
        }                
    }
    
    private void validateAuthnStatement(AuthenticationStatement authnStatement)
            throws SecurityException {
        
        Subject subject = authnStatement.getSubject();
        if(subject == null) {
           throw new SecurityException(
                   WSSUtils.bundle.getString("nullSubject"));
        }        
        subjectName = subject.getNameIdentifier().getName();
        SubjectLocality subjectLocality = authnStatement.getSubjectLocality();
        String ipAddress = null;
        if(subjectLocality != null) {
           ipAddress = subjectLocality.getIPAddress();
        }
        if(ipAddress != null) {
            /* TODO check for the valid IP addresses.
           if(!stsConfig.getTrustedIPAddresses().contains(ipAddress)) {
              throw new SecurityException(
                    WSSUtils.bundle.getString("invalidIPAddress"));
           }*/
        }
       
        SubjectConfirmation sc = 
            authnStatement.getSubject().getSubjectConfirmation(); 
        if (sc != null) {
            Element keyInfo = sc.getKeyInfo();
            if(keyInfo != null) {
               cert = WSSUtils.getCertificate(keyInfo); 
            }
        }               
    }
    
    private void validateAttributeStatement(
            AttributeStatement attributeStatement) throws SecurityException {
        try {
            Subject subject = attributeStatement.getSubject();
            if(subject == null) {
               throw new SecurityException(
                       WSSUtils.bundle.getString("nullSubject"));
            }
            subjectName = subject.getNameIdentifier().getName();
            SubjectConfirmation sc = subject.getSubjectConfirmation(); 
            if (sc != null) {
                Element keyInfo = sc.getKeyInfo();
                if(keyInfo != null) {
                   cert = WSSUtils.getCertificate(keyInfo); 
                }
            }               
            
            List<Attribute> attributes = attributeStatement.getAttribute();
            for (Iterator iter = attributes.iterator(); iter.hasNext();) {
                Attribute attribute = (Attribute)iter.next();
                String attrName = attribute.getAttributeName();
                List <org.w3c.dom.Element>  values = 
                        attribute.getAttributeValue();
                if(values != null && !values.isEmpty()) {
                   StringBuilder sb = new StringBuilder();
                   for (int i = 0; i < values.size(); i++) {
                       if (i != 0) {
                           sb.append(StringUtils.PROPERTY_VALUE_DELIMITER);
                       }
                       sb.append(StringUtils.getEscapedValue(XMLUtils.getElementValue(
                                 (Element)values.get(i))));
                   }
                   attributeMap.put(attrName, sb.toString());
                }            
            }
        } catch (SAMLException se) {
            throw new SecurityException(se.getMessage());
        }
        
    }
    
    public Map getAttributes() {
        return attributeMap;
    }
    
    public String getSubjectName() {
        return subjectName;
    }
    
    public X509Certificate getKeyInfoCert() {
        return cert;
    }

}
