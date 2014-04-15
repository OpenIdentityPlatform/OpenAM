/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SecurityUtils.java,v 1.5 2009/06/08 23:42:33 madan_ranganath Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */


package com.sun.identity.liberty.ws.security;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.shared.encode.Base64;

import com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken;
import com.sun.identity.liberty.ws.common.wsse.WSSEConstants;
import com.sun.identity.liberty.ws.soapbinding.Message;

import com.sun.identity.saml.assertion.AuthenticationStatement;
import com.sun.identity.saml.assertion.Statement;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.SubjectConfirmation;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLUtils;

import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml.xmlsig.XMLSignatureException;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.content.keyvalues.DSAKeyValue;
import org.apache.xml.security.keys.content.keyvalues.RSAKeyValue;
import org.apache.xml.security.utils.Constants;

import java.io.ByteArrayInputStream;

import java.math.BigInteger;

import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.PublicKey;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class has common utility methods .
 */
public class SecurityUtils {
    
    private static SecurityUtils securityManager = null;
    private static XMLSignatureManager sm = null;
    private static Debug debug = null;
    private static String PROP_TRUSTED_CA_CERT_ALIASES =
            "com.sun.identity.liberty.ws.trustedca.certaliases";
    private static Set trustedCACertAliases = new HashSet();
    private static Map issuerTrustedCACertAliases = new HashMap();
    private static KeyProvider keystore = null;
    
    static {
        debug = Debug.getInstance("libIDWSF");
        String tmpStr = SystemPropertiesManager.get(
            PROP_TRUSTED_CA_CERT_ALIASES);
        if (debug.messageEnabled()) {
            debug.message("SecurityUtils.static: trusted ca certaliases = " +
                    tmpStr);
        }
        if (tmpStr != null) {
            StringTokenizer stz = new StringTokenizer(tmpStr, "|");
            while(stz.hasMoreTokens()) {
                String aliasIssuer = stz.nextToken().trim();
                if (aliasIssuer.length() > 0) {
                    int index = aliasIssuer.indexOf(":");
                    if (index == -1) {
                        trustedCACertAliases.add(aliasIssuer);
                        if (debug.messageEnabled()) {
                            debug.message("SecurityUtils.static: add " +
                                    aliasIssuer + " to trustedCACertAliases");
                        }
                    } else {
                        String alias = aliasIssuer.substring(0, index).trim();
                        if (alias.length() > 0) {
                            trustedCACertAliases.add(alias);
                            if (debug.messageEnabled()) {
                                debug.message("SecurityUtils.static: add " +
                                        alias +" to trustedCACertAliases");
                            }
                            String issuer =
                                    aliasIssuer.substring(index + 1).trim();
                            if (issuer.length() > 0) {
                                issuerTrustedCACertAliases.put(issuer, alias);
                                if (debug.messageEnabled()) {
                                    debug.message("SecurityUtils.static: add "+
                                            "[" + issuer + ", " + alias +
                                            "] to issuerTrustedCACertAliases");
                                }
                            }
                        }
                    }
                }
            }
        }
        
        sm = XMLSignatureManager.getInstance();
	if (sm != null) {
            keystore = sm.getKeyProvider();
        }
    }
    
    /*
     * Sign part of the Message object based on the Security Token
     * profile embedded in the object.
     *
     * @param m Message object
     * @return Signature of Security Token Profile
     */
    public static Element signMessage(Message m) {
        try {
            Document doc = m.toDocument(true);
            int securityType = m.getSecurityProfileType();
            Certificate cert = null;
            List ids = m.getSigningIds();
            
            if (debug.messageEnabled()) {
                debug.message("Security Type = " + securityType);
            }
            if (securityType==m.X509_TOKEN) {
                cert = m.getMessageCertificate();
	        return sm.signWithWSSX509TokenProfile(doc, cert, "", ids, 
                       m.getWSFVersion());
            } else if (securityType==m.SAML_TOKEN) {
                SecurityAssertion assertion = m.getAssertion();
                cert = m.getMessageCertificate();
                String assertionID = assertion.getAssertionID();
	        return sm.signWithWSSSAMLTokenProfile(doc, cert, assertionID,
			"", ids, m.getWSFVersion());
            } else if (securityType==m.ANONYMOUS) {
                // Should be transportation layer encryption.
            }
        } catch (Exception e) {
            debug.error("Unable to sign Soap message!",e);
        }
        return null;
    }
    
    /**
     * Verify all the signatures of the of Message object passed
     * from Soap Binding.
     *
     * @param m Message object whose signature to be verified
     * @return true if the signature is verified.
     */
    public static boolean verifyMessage(Message m){
        try {
            Document doc = m.toDocument(false);
            Certificate clientCert = (Certificate) m.getPeerCertificate();
            Certificate messageCert = (Certificate) m.getMessageCertificate();
            
            int securityProfileType = m.getSecurityProfileType();
            if (securityProfileType == Message.SAML_TOKEN ||
                    securityProfileType == Message.BEARER_TOKEN) {
                
                SecurityAssertion assertion = m.getAssertion();
                String certAlias = null;
                Certificate signingCert = getAssertionSigningCert(assertion);
                if (signingCert == null) {
                    certAlias = (String)issuerTrustedCACertAliases
                            .get(assertion.getIssuer());
                    if (certAlias == null) {
                        debug.error("SecurityUtils.verifyMessage: " +
                                "assertion doesn't have keyInfo and " +
                                "issuer is not in " +
                                "com.sun.identity.liberty.ws.trustedca.certalias" +
                                " in AMConfig");
                        return false;
                    }
                } else {
                    certAlias = keystore.getCertificateAlias(signingCert);
                    if (certAlias == null) {
                        debug.error("SecurityUtils.verifyMessage: " +
                                "assertion is signed with a certificate that " +
                                " is not in the keystore");
                        return false;
                    } else if (!trustedCACertAliases.contains(certAlias)) {
                        debug.error("SecurityUtils.verifyMessage: " +
                                "assertion is signed with a certificate that " +
                                " is in the keystore but not in " +
                                "com.sun.identity.liberty.ws.trustedca.certalias" +
                                " in AMConfig");
                        return false;
                    }
                }
                assertion.setVerifyingCertAlias(certAlias);
                if (!assertion.isSignatureValid()) {
                    debug.error("SecurityUtils.verifyMessage: assertion " +
                            "signature invalid");
                    return false;
                }
                
                if (debug.messageEnabled()) {
                    debug.message("SecurityUtils.verifyMessage: Assertion " +
                            " signing cert alias = " + certAlias);
                }
            }
            
            if ((clientCert!=null)&&(!clientCert.equals(messageCert))) {
                debug.error("Client authentication certificate is not " +
                        "the same as the certificate inside the " +
                        "soap message");
                return false;
            }
            
            if (messageCert != null) {
                String messageCertAlias =
                        keystore.getCertificateAlias(messageCert);
                return sm.verifyXMLSignature(m.getWSFVersion(), 
                    messageCertAlias, doc);
            }
            
            return true;
        } catch (Exception e) {
            debug.error("Unable to verify Soap Message!", e);
        }
        return false;
    }
    
    /**
     * Get Certificate from X509 Security Token Profile document.
     *
     * @param binarySecurityToken the Security Token.
     * @return X509 Certiticate object.
     */
    public static java.security.cert.Certificate getCertificate(
            BinarySecurityToken binarySecurityToken) {
        
        java.security.cert.Certificate cert = null;
        
        try {
            String certString = binarySecurityToken.getTokenValue();
            
            StringBuffer xml = new StringBuffer(100);
            xml.append(WSSEConstants.BEGIN_CERT);
            xml.append(certString);
            xml.append(WSSEConstants.END_CERT);
            
            byte[] barr = null;
            barr = (xml.toString()).getBytes();
            
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bais = new ByteArrayInputStream(barr);
            
            QName valueType = binarySecurityToken.getValueType();
            if (valueType.equals(BinarySecurityToken.PKCS7)) { // PKCS7 format
                Collection c = cf.generateCertificates(bais);
                Iterator i = c.iterator();
                while (i.hasNext()) {
                    cert = (java.security.cert.Certificate) i.next();
                }
            } else { //X509:v3 format
                while (bais.available() > 0) {
                    cert = cf.generateCertificate(bais);
                }
            }
        } catch (Exception e) {
            // Certificate encoding error!
            debug.error("WSSecurityManager:getX509Certificate", e);
        }
        return cert;
    }
    
    /**
     * Gets the  Certificate from the <code>Assertion</code>.
     *
     * @param assertion the SAML <code>Assertion</code>.
     * @return <code>X509Certificate</code> object.
     */
    public static java.security.cert.Certificate getCertificate(
            SecurityAssertion assertion) {
        
        
        if (debug.messageEnabled()) {
            debug.message("SecurityAssertion = " + assertion.toString());
        }
        try {
            Set statements = assertion.getStatement();
            if (statements !=null && !(statements.isEmpty())) {
                Iterator iterator =  statements.iterator();
                while (iterator.hasNext()) {
                    Statement statement =(Statement)iterator.next();
                    int stype = statement.getStatementType();
                    Subject subject = null;
                    if (stype == Statement.AUTHENTICATION_STATEMENT) {
                        subject =
                            ((AuthenticationStatement)statement).getSubject();
                    } else if (stype ==
                        ResourceAccessStatement.RESOURCEACCESS_STATEMENT) {
                        ResourceAccessStatement raStatement = 
                            (ResourceAccessStatement)statement;
                        subject = raStatement.getProxySubject();
                        if (subject == null) {
                            subject = raStatement.getSubject();
                        }
                    } else if (stype == 
                        SessionContextStatement.SESSIONCONTEXT_STATEMENT) {
                        SessionContextStatement scStatement = 
                            (SessionContextStatement)statement;
                        subject = scStatement.getProxySubject();
                        if (subject == null) {
                            subject = scStatement.getSubject();
                        }
                    }

                    if (subject != null) {
                        SubjectConfirmation subConfirm =
                            subject.getSubjectConfirmation();
                        if (subConfirm.getConfirmationMethod().contains(
                            SAMLConstants.CONFIRMATION_METHOD_HOLDEROFKEY)) {

                            Element keyinfo = subConfirm.getKeyInfo();
                            return getCertificate(keyinfo);
                        }
                    }
                }
            } else {
                debug.error("Assertion does not contain any Statement.");
            }
        } catch (Exception e) {
            debug.error("getCertificate Exception: ", e);
        }
        return null;
    }
    
    /**
     * Returns the <code>X509Certificate</code> object.
     *
     * @param keyinfo the <code>KeyInfo</code> Document Element.
     * @return the <code>X509Certificate</code> object.
     */
    private static X509Certificate getCertificate(Element keyinfo) {
        
        X509Certificate cert = null;
        
        if (debug.messageEnabled()) {
            debug.message("KeyInfo = " + XMLUtils.print(keyinfo));
        }
        
        Element x509 = (Element) keyinfo.getElementsByTagNameNS(
                Constants.SignatureSpecNS,
                SAMLConstants.TAG_X509CERTIFICATE).item(0);
        
        if (x509 == null) { // no cert found. try DSA/RSA key
            try {
                PublicKey pk = getPublicKey(keyinfo);
                cert = (X509Certificate) keystore.getCertificate(pk);
            } catch (Exception e) {
                debug.error("getCertificate Exception: ", e);
            }
            
        } else {
            String certString = x509.getChildNodes().item(0).getNodeValue();
            cert = getCertificate(certString, null);
        }
        
        return cert;
    }
    
    /**
     * Returns the <code>PublicKey</code>.
     */
    private static PublicKey getPublicKey(Element reference)
    throws XMLSignatureException {
        
        PublicKey pubKey = null;
        Document doc = reference.getOwnerDocument();
        Element dsaKey = (Element) reference.getElementsByTagNameNS(
                Constants.SignatureSpecNS,
                SAMLConstants.TAG_DSAKEYVALUE).item(0);
        if (dsaKey != null) { // It's DSAKey
            NodeList nodes = dsaKey.getChildNodes();
            int nodeCount = nodes.getLength();
            if (nodeCount > 0) {
                BigInteger p=null, q=null, g=null, y=null;
                for (int i = 0; i < nodeCount; i++) {
                    Node currentNode = nodes.item(i);
                    if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                        String tagName = currentNode.getLocalName();
                        Node sub = currentNode.getChildNodes().item(0);
                        String value = sub.getNodeValue();
                        value = SAMLUtils.removeNewLineChars(value);
                        BigInteger v = new BigInteger(Base64.decode(value));
                        if (tagName.equals("P")) {
                            p = v;
                        } else if (tagName.equals("Q")) {
                            q = v;
                        } else if (tagName.equals("G")) {
                            g = v;
                        } else if (tagName.equals("Y")) {
                            y = v;
                        } else {
                            SAMLUtils.debug.error("Wrong tag name in DSA key.");
                            throw new XMLSignatureException(
                                    SAMLUtils.bundle.getString("errorObtainPK"));
                        }
                    }
                }
                DSAKeyValue dsaKeyValue = new DSAKeyValue(doc, p, q, g, y);
                try {
                    pubKey = dsaKeyValue.getPublicKey();
                } catch (XMLSecurityException xse) {
                    SAMLUtils.debug.error("Could not get Public Key from" +
                            " DSA key value.");
                    throw new XMLSignatureException(
                            SAMLUtils.bundle.getString("errorObtainPK"));
                }
            }
        } else {
            Element rsaKey =
                    (Element) reference.getElementsByTagNameNS(
                    Constants.SignatureSpecNS,
                    SAMLConstants.TAG_RSAKEYVALUE).item(0);
            if (rsaKey != null) { // It's RSAKey
                NodeList nodes = rsaKey.getChildNodes();
                int nodeCount = nodes.getLength();
                BigInteger m=null, e=null;
                if (nodeCount > 0) {
                    for (int i = 0; i < nodeCount; i++) {
                        Node currentNode = nodes.item(i);
                        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                            String tagName = currentNode.getLocalName();
                            Node sub = currentNode.getChildNodes().item(0);
                            String value = sub.getNodeValue();
                            value = SAMLUtils.removeNewLineChars(value);
                            BigInteger v =new BigInteger(Base64.decode(value));
                            if (tagName.equals("Exponent")) {
                                e = v;
                            } else if (tagName.equals("Modulus")){
                                m = v;
                            } else {
                                SAMLUtils.debug.error("Wrong tag name from " +
                                        "RSA key element.");
                                throw new XMLSignatureException(
                                        SAMLUtils.bundle.getString("errorObtainPK"));
                            }
                        }
                    }
                }
                RSAKeyValue rsaKeyValue =
                        new RSAKeyValue(doc,m, e);
                try {
                    pubKey = rsaKeyValue.getPublicKey();
                } catch (XMLSecurityException ex) {
                    SAMLUtils.debug.error("Could not get Public Key from" +
                            " RSA key value.");
                    throw new XMLSignatureException(
                            SAMLUtils.bundle.getString("errorObtainPK"));
                }
            }
        }
        return pubKey;
    }
    
    /**
     * Returns the <code>X509Certificate</code> object.
     *
     * @param certString the Certificate String.
     * @param format the Certificate's format.
     * @return the <code>X509Certificate</code> object.
     */
    private static X509Certificate getCertificate(String certString,
            String format) {
        X509Certificate cert = null;
        
        try {
            
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("getCertificate(Assertion) : " +
                        certString);
            }
            
            StringBuffer xml = new StringBuffer(100);
            xml.append(SAMLConstants.BEGIN_CERT);
            xml.append(certString);
            xml.append(SAMLConstants.END_CERT);
            
            byte[] barr = null;
            barr = (xml.toString()).getBytes();
            
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bais = new ByteArrayInputStream(barr);
            
            if ((format !=null) &&
                    format.equals(SAMLConstants.TAG_PKCS7)) { // PKCS7 format
                Collection c = cf.generateCertificates(bais);
                Iterator i = c.iterator();
                while (i.hasNext()) {
                    cert = (java.security.cert.X509Certificate) i.next();
                }
            } else { //X509:v3 format
                while (bais.available() > 0) {
                    cert = (java.security.cert.X509Certificate)
                    cf.generateCertificate(bais);
                }
            }
        } catch (Exception e) {
            SAMLUtils.debug.error("getCertificate Exception: ", e);
        }
        
        return cert;
    }
    
    /**
     * Returns the <code>X509Certificate</code> in the <code>Assertion</code>.
     *
     * @param assertion the SAML <code>Assertion</code>
     * @return the <code>X509Certificate</code> object.
     */
    private static X509Certificate getAssertionSigningCert(
            SecurityAssertion assertion) {
        X509Certificate cert = null;
        Element signature = assertion.getSignature();
        Element keyInfo = (Element) signature.getElementsByTagNameNS(
                Constants.SignatureSpecNS,
                SAMLConstants.TAG_KEYINFO).item(0);
        if (keyInfo != null) {
            cert = (X509Certificate) getCertificate(keyInfo);
        }
        return cert;
    }

    /**
     * Returns XML Signature instance.
     */
    public static XMLSignatureManager getSignatureManager() {
        return sm;
    }
}
