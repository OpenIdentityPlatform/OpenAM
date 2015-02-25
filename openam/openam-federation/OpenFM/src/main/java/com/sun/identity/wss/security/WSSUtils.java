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
 * $Id: WSSUtils.java,v 1.23 2010/01/23 00:20:26 mrudul_uchil Exp $
 *
 *  Portions Copyrighted 2012-2014 ForgeRock AS
 */

package com.sun.identity.wss.security;

import java.io.ByteArrayInputStream;
import java.util.Set;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.math.BigInteger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.security.Principal;
import java.security.AccessController;
import java.security.cert.CertificateFactory;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import javax.xml.soap.SOAPConstants;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.content.keyvalues.DSAKeyValue;
import org.apache.xml.security.keys.content.keyvalues.RSAKeyValue;
import org.apache.xml.security.utils.Constants;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml.xmlsig.XMLSignatureException;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.SubjectConfirmation;
import com.sun.identity.saml.assertion.Statement;
import com.sun.identity.saml.assertion.AuthenticationStatement;
import com.sun.identity.saml.assertion.AttributeStatement;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.xmlenc.XMLEncryptionManager;
import com.sun.identity.shared.encode.Base64;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.MimeHeaders;
import javax.xml.namespace.QName;
import javax.crypto.spec.SecretKeySpec;
import com.sun.identity.wss.xmlsig.WSSSignatureProvider;
import com.sun.identity.wss.xmlenc.WSSEncryptionProvider;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.wss.trust.ClaimType;
import org.apache.xml.security.keys.content.X509Data;
import com.sun.identity.wss.security.handler.WSSCacheRepository;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.wss.sts.spi.NameIdentifierMapper;
import com.sun.identity.xmlenc.EncryptionConstants;
import com.sun.identity.wss.provider.ProviderConfig;
import javax.xml.parsers.DocumentBuilder;

/**
 * This class provides util methods for the web services security. 
 */
public class WSSUtils {

     public static ResourceBundle bundle = null;
     public static Debug debug = Debug.getInstance("WebServicesSecurity");
     private static XMLSignatureManager xmlSigManager = null;
     private static XMLEncryptionManager xmlEncManager = null;
     private static final String AGENT_TYPE_ATTR = "AgentType"; 
     private static final String WSP_ENDPOINT = "WSPEndpoint";
     private static final String WSS_CACHE_REPO_PLUGIN =
                        "com.sun.identity.wss.security.cacherepository.plugin";
     private static WSSCacheRepository cacheRepository = null;
     private static final String MEMBERSHIPS = "Memberships";
     private static Set trustedCACertAliases = new HashSet();
     private static Map issuerTrustedCACertAliases = new HashMap();
     
     static {
        bundle = Locale.getInstallResourceBundle("fmWSSecurity");
        String tmpStr = SystemConfigurationUtil.getProperty(
                         "com.sun.identity.liberty.ws.trustedca.certaliases");
        if (debug.messageEnabled()) {
            debug.message("WSSUtils.static: trusted ca certaliases = " +
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
                    } else {
                        String alias = aliasIssuer.substring(0, index).trim();
                        if (alias.length() > 0) {
                            trustedCACertAliases.add(alias);                            
                            String issuer =
                                    aliasIssuer.substring(index + 1).trim();
                            if (issuer.length() > 0) {
                                issuerTrustedCACertAliases.put(issuer, alias);                                                                
                            }
                        }
                    }
                }
            }
        }
     }

     /**
      * Returns the certificate present in the security token.
      * @param securityToken the security token.
      * @return the certificate.
      */
     public static X509Certificate getCertificate(SecurityToken securityToken)
             throws SecurityException {

         String tokenType = securityToken.getTokenType();

         if(tokenType.equals(SecurityToken.WSS_SAML_TOKEN)) {
            Element keyInfo = null;
            AssertionToken assertionToken = (AssertionToken)securityToken;
            if(!assertionToken.isSenderVouches()) {
               Assertion assertion = assertionToken.getAssertion();
               keyInfo = getKeyInfo(assertion); 
               return getCertificate(keyInfo);
            }

         } else if(tokenType.equals(SecurityToken.WSS_X509_TOKEN)) {
            BinarySecurityToken binaryToken = 
                      (BinarySecurityToken)securityToken;
            String certValue = binaryToken.getTokenValue();
            StringBuffer xml = new StringBuffer(100);
            xml.append(WSSConstants.BEGIN_CERT);
            xml.append(certValue);
            xml.append(WSSConstants.END_CERT);
            byte[] bytevalue = xml.toString().getBytes();
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                ByteArrayInputStream bais = new ByteArrayInputStream(bytevalue);
                return (X509Certificate)cf.generateCertificate(bais);
            } catch (Exception ex) {
                debug.error("WSSUtils.getCertificate:: Unable to retrieve " +
                " certificate from the binary token", ex);
                throw new SecurityException(
                      bundle.getString("cannotRetrieveCert"));
            }
           
         }
         return null;
     }

     private static Element getKeyInfo(Assertion assertion) {
        try {
            AuthenticationStatement authStatement = null;
            AttributeStatement attributeStatement = null;
            Subject subject = null;
            Set statements = assertion.getStatement();
            if (statements == null) {
                debug.error(
                "Assertion does not contain any Statement.");
            }
            if (!(statements.isEmpty())) {
                Iterator iterator =  statements.iterator();
                while (iterator.hasNext()) {
                    Statement statement =(Statement)iterator.next();
                    if (statement.getStatementType()==
                                Statement.AUTHENTICATION_STATEMENT) {
                        authStatement = (AuthenticationStatement) statement;
                        subject = authStatement.getSubject();
                        break;
                    } else if (statement.getStatementType()== 
                            Statement.ATTRIBUTE_STATEMENT) {
                        attributeStatement = (AttributeStatement)statement;
                        subject = attributeStatement.getSubject();
                    }
                }
            }

            SubjectConfirmation subConfirm = subject.getSubjectConfirmation();
            return subConfirm.getKeyInfo();
        } catch (Exception e) {
            debug.error("getCertificate Exception: ", e);
        }
        return null;
     }

     public static X509Certificate getCertificate(Element keyinfo) {

        X509Certificate cert = null;

        if (debug.messageEnabled()) {
            debug.message("KeyInfo = " + XMLUtils.print(keyinfo));
        }
        
        
        Element encryptedKey = (Element)keyinfo.getElementsByTagNameNS(
                EncryptionConstants.ENC_XML_NS, "EncryptedKey").item(0);
        if(encryptedKey != null) {
           return null;
        }
        

        Element x509 = (Element) keyinfo.getElementsByTagNameNS(
                                Constants.SignatureSpecNS,
                                SAMLConstants.TAG_X509CERTIFICATE).item(0);

        if (x509 == null) { 
            //noo cert found. try DSA/RSA key
            try {
                PublicKey pk = getPublicKey(keyinfo);
                cert = (X509Certificate) AMTokenProvider.getKeyProvider().
                          getCertificate(pk);
            } catch (Exception e) {
                debug.error("getCertificate Exception: ", e);
            }

        } else {
            
            String certString = XMLUtils.getElementValue(x509);            
            cert = getCertificate(certString, null);
        }

        return cert;
    }
       
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
                            throw new XMLSignatureException(
                                bundle.getString("invalidReference"));
                        }
                    }
                }
                DSAKeyValue dsaKeyValue = new DSAKeyValue(doc, p, q, g, y);
                try {
                    pubKey = dsaKeyValue.getPublicKey();
                } catch (XMLSecurityException xse) {
                    debug.error("Could not get Public Key from" +
                                        " DSA key value.");
                    throw new XMLSignatureException(
                        bundle.getString("errorObtainPK"));
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
                            }
                            else if (tagName.equals("Modulus")){
                                m = v;
                            } else {
                                throw new XMLSignatureException
                                        ("Invalid reference");
                            }
                        }
                    }
                }
                RSAKeyValue rsaKeyValue =
                    new RSAKeyValue(doc,m, e);
                try {
                    pubKey = rsaKeyValue.getPublicKey();
                } catch (XMLSecurityException ex) {
                    debug.error("Could not get Public Key from" +
                                        " RSA key value.");
                    throw new XMLSignatureException(
                                bundle.getString("errorObtainPK"));
                }
            }
        }
        return pubKey;
    }

    /**
     * Get the X509Certificate from the provided string representation
     * @param certString The certificate in string form
     * @param format The format that the certificate is in, supports PKCS7 and X509:v3
     * @return a X509Certificate
     */
    private static X509Certificate getCertificate(String certString,
                                           String format)
    {
        X509Certificate cert = null;

        try {
            if (debug.messageEnabled()) {
                debug.message("getCertificate(Assertion) : " +
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
            debug.error("getCertificate Exception: ", e);
        }
        return cert;
    }

    public static SOAPMessage toSOAPMessage(Document document) {
        try {
            Element elmDoc = (Element) document.getDocumentElement();
            String nameSpaceURI = elmDoc.getNamespaceURI();
            MessageFactory msgFactory = MessageFactory.newInstance();
            MimeHeaders mimeHeaders = new MimeHeaders();
            if (SAMLConstants.SOAP_URI.equals(nameSpaceURI)) {
                mimeHeaders.addHeader("Content-Type", "text/xml");
            } else if (SAMLConstants.SOAP12_URI.equals(nameSpaceURI)) {
                msgFactory = 
                    MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
                mimeHeaders.addHeader("Content-Type", "application/soap+xml");
            }
            String xmlStr = print(document);
            return msgFactory.createMessage(mimeHeaders,
                   new ByteArrayInputStream(xmlStr.getBytes("UTF-8")));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String print(Node node) {
        return XMLUtils.print(node);
    }

    public static Element prependChildElement(
    Element parent,
    Element child,
    boolean addWhitespace,
    Document doc) {

        Node firstChild = parent.getFirstChild();
        if (firstChild == null) {
            parent.appendChild(child);
        } else {
            parent.insertBefore(child, firstChild);
        }

        if (addWhitespace) {
            Node whitespaceText = doc.createTextNode("\n");
            parent.insertBefore(whitespaceText, child);
        }
        return child;
    }

    public static Node getDirectChild(Node fNode, 
                 String localName,  String namespace) {
        for (Node currentChild = fNode.getFirstChild(); currentChild != null;
                 currentChild = currentChild.getNextSibling()) {
            if (localName.equals(currentChild.getLocalName())
                    && namespace.equals(currentChild.getNamespaceURI())) {
                return currentChild;
            }
        }
        return null;
    }
    
    // Returns WSSEncryptionProvider
    public static XMLEncryptionManager getXMLEncryptionManager() {

        KeyProvider keyprovider = null;
        try {
            String kprovider = SystemConfigurationUtil.getProperty(
                                SAMLConstants.KEY_PROVIDER_IMPL_CLASS,
                                SAMLConstants.JKS_KEY_PROVIDER);
            keyprovider = (KeyProvider)
                          (Thread.currentThread().getContextClassLoader())
                                            .loadClass(kprovider).newInstance();
        } catch (Exception e) {
            debug.error("getXMLEncryptionManager : " +
                        "get keyprovider error", e);
                        throw new RuntimeException(e.getMessage());
        }
        if (xmlEncManager == null) {
	        synchronized (XMLEncryptionManager.class) {
		        if (xmlEncManager == null) {
	                xmlEncManager = XMLEncryptionManager.getInstance(
                                  new WSSEncryptionProvider(),
                                  keyprovider);
		        }
	        }
	    }
        return xmlEncManager;        	
    }

    // Returns WSSSignatureProvider
    public static XMLSignatureManager getXMLSignatureManager() {

    KeyProvider keyprovider = null;
    try {
        String kprovider = SystemConfigurationUtil.getProperty(
                                SAMLConstants.KEY_PROVIDER_IMPL_CLASS,
                                SAMLConstants.JKS_KEY_PROVIDER);
        keyprovider = (KeyProvider)
                      (Thread.currentThread().getContextClassLoader())
                                            .loadClass(kprovider).newInstance();
    } catch (Exception e) {
        debug.error("getXMLSignatureManager : " +
                    "get keystore error", e);
                     throw new RuntimeException(e.getMessage());
    }
    if (xmlSigManager == null) {
	    synchronized (XMLSignatureManager.class) {
		    if (xmlSigManager == null) {
	            xmlSigManager = XMLSignatureManager.getInstance(
                          keyprovider,
                          new WSSSignatureProvider());	    
		   }
	    }
	}
        return xmlSigManager;        	
    }
    
    /**
     * Returns corresponding Authentication method URI to be set in Assertion.
     * @param authModuleName name of the authentication module used to
     *          authenticate the user.
     * @return String corresponding Authentication Method URI to be set in
     *          Assertion.
     */
    public static String getAuthMethodURI(String authModuleName) {
        if (authModuleName == null) {
            return null;
        }

        if (authModuleName.equalsIgnoreCase(SAMLConstants.AUTH_METHOD_CERT)) {
            return SAMLConstants.AUTH_METHOD_CERT_URI;
        }
        if (authModuleName.equalsIgnoreCase(SAMLConstants.AUTH_METHOD_KERBEROS))
        {
            return SAMLConstants.AUTH_METHOD_KERBEROS_URI;
        }
        if (SAMLConstants.passwordAuthMethods.contains(
            authModuleName.toLowerCase()))
        {
            return SAMLConstants.AUTH_METHOD_PASSWORD_URI;
        }
        if (SAMLConstants.tokenAuthMethods.contains(
            authModuleName.toLowerCase()))
        {
            return SAMLConstants.AUTH_METHOD_HARDWARE_TOKEN_URI;
        } else {
            StringBuffer sb = new StringBuffer(100);
            sb.append(SAMLConstants.AUTH_METHOD_URI_PREFIX).
                        append(authModuleName);
            return sb.toString();
        }
    }
    
    /**
     * Sets the memberships for a given user into the JAAS Subject.
     * @param subject the JAAS subject where the role memberships need
     * to be set.
     * @param user the user's universal dn
     */
    public static void setRoles(javax.security.auth.Subject
            subject, String user) {
        List roles = getMemberShips(user);
        if(roles == null || roles.isEmpty()) {
           if(debug.messageEnabled()) {
              debug.message("WSSUtils.setRoles:: " +
                    "There are no memberships for this user");
           }
           return;
        }
        if(debug.messageEnabled()) {
           debug.message("WSSUtils.setRoles:: " + roles);
        }
        Iterator iter = roles.iterator();
        while(iter.hasNext()) {
           String roleName = (String)iter.next();
           Principal principal = new SecurityPrincipal(roleName);
           subject.getPrincipals().add(principal);
        }  
    }
    
    public static List getMemberShips(String pattern) {
        List roles = new ArrayList();
        try {
            SSOToken adminToken = getAdminToken();
            if(adminToken == null) {
               debug.message("WSSUtils.getRoleMemberShips: " +
               "Admin Token is null");
                return roles;
            }
            AMIdentity user = new AMIdentity(adminToken, pattern);
            if(user == null) {
               if(debug.messageEnabled()) {
                  debug.message("WSSUtils.getMemberShips: " +
                  "unable to get the user");
               }
               return roles;
            }

            AMIdentityRepository idRepo =
                  new AMIdentityRepository(adminToken, user.getRealm());
            Set supportedTypes = idRepo.getSupportedIdTypes();

            Set enrolledTypes = new HashSet();
            for (Iterator iter1 = supportedTypes.iterator(); iter1.hasNext();)
            {
                 IdType idType = (IdType)iter1.next();
                 Set canHaveMembers = idType.canHaveMembers();
                 if(!canHaveMembers.isEmpty()) {
                    enrolledTypes.add(idType);
                 }
            }

            if(enrolledTypes.isEmpty()) {
               if(debug.messageEnabled()) {
                  debug.message("WSSUtils.getMemberShips: " +
                  "Can have enrolled types are empty");
               }
               return roles;
            }
            Iterator iter3 = enrolledTypes.iterator();
            while(iter3.hasNext()) {
               IdType idType = (IdType)iter3.next();
               Set roleMemberships = user.getMemberships(idType);
               Iterator roleI = roleMemberships.iterator();
               while(roleI.hasNext()) {
                  AMIdentity role = (AMIdentity)roleI.next();
                  roles.add(role.getUniversalId());
               }
            }
            return roles;

        } catch (SSOException se) {
            debug.message("WSSUtils.getRoleMemberShips: " +
            "SSOException : " + se);
        } catch (IdRepoException ire) {
            debug.message("WSSUtils.getRoleMemberShips: " +
            "IdRepoException : " + ire);
        }
        return roles;
    }    
    
        public static Map getAgentAttributes(
                 String endpoint, Set attrNames, String type) {
        try {
            SSOToken adminToken = WSSUtils.getAdminToken();
            AMIdentityRepository idRepo = 
                     new AMIdentityRepository(adminToken, "/");
            IdSearchControl control = new IdSearchControl();
            control.setAllReturnAttributes(true);
            control.setTimeOut(0);

            Map kvPairMap = new HashMap();
            Set set = new HashSet();
            set.add(type);
            kvPairMap.put(AGENT_TYPE_ATTR, set);

            set = new HashSet();
            set.add(endpoint);
            kvPairMap.put(WSP_ENDPOINT, set);

            control.setSearchModifiers(IdSearchOpModifier.AND, kvPairMap);

            IdSearchResults results = idRepo.searchIdentities(IdType.AGENTONLY,
               "*", control);
            Set agents = results.getSearchResults();
            if (!agents.isEmpty()) {
                Map attrs = (Map) results.getResultAttributes();
                AMIdentity provider = (AMIdentity) agents.iterator().next();
                Map agentConfig = null;
                if(attrNames != null) {
                   agentConfig = provider.getAttributes(attrNames);
                } else {
                   agentConfig = provider.getAttributes();
                }
                return agentConfig;
            }
            return new HashMap();
        } catch (Exception ex) {
            debug.error("STSUtils.getAgentAttributes: Exception", ex); 
            return new HashMap();
        }
    }
    public static SSOToken getAdminToken() {
        SSOToken adminToken = null;
        try {
            adminToken = (SSOToken) AccessController.doPrivileged(
                         AdminTokenAction.getInstance());
            
            if(adminToken != null) {
                if (!SSOTokenManager.getInstance().isValidToken(adminToken)) {
                    if (debug.messageEnabled()) {
                        debug.message("WSSUtils.getAdminToken: "
                            + "AdminTokenAction returned "
                            + "expired or invalid token, trying again...");
                    }
                    adminToken = (SSOToken) AccessController.doPrivileged(
                            AdminTokenAction.getInstance());
                }
            }
        } catch (Exception se) {
            debug.message("WSSUtils.getAdminToken::" +
               "Trying second time ....");
            adminToken = (SSOToken) AccessController.doPrivileged(
               AdminTokenAction.getInstance());
        }
        return adminToken;
    }
    
    /**
     * Returns the message certificate from the security token reference
     * especially for KeyIdentifier and X509IssuerSerial case.
     * @param sigElement the signature element where the security token
     *        ref is present
     * @return the X509Certificate
     */
    public static X509Certificate getMessageCertificate(Element sigElement) {
        if(sigElement == null) {
           return null;
        }
        NodeList nl = sigElement.getElementsByTagNameNS(WSSConstants.WSSE_NS, 
                WSSConstants.TAG_SECURITYTOKEN_REFERENCE);
        if(nl.getLength() == 0) {
           return null;
        }        
        try  {
             SecurityTokenReference secTokenRef = 
                     new SecurityTokenReference((Element)nl.item(0));
             String refType = secTokenRef.getReferenceType();
             if(WSSConstants.DIRECT_REFERENCE.equals(refType)) {
                // This should not come here since the certificate is in
                // message and should have been already resolved.                
                return null; 
             } else if(WSSConstants.KEYIDENTIFIER_REFERENCE.equals(refType)) {
                KeyIdentifier keyIdentifier = secTokenRef.getKeyIdentifier(); 
                if(keyIdentifier != null) {
                   return keyIdentifier.getX509Certificate(); 
                }
             } else if(WSSConstants.X509DATA_REFERENCE.equals(refType)) {                 
                X509Data x509Data = secTokenRef.getX509IssuerSerial();
                return AMTokenProvider.getX509Certificate(x509Data);                
             }
        } catch (SecurityException se) {
             debug.error("WSSUtils.getMessageCertificate: exception", se);   
        }
        return null;
    }
    
     public static WSSCacheRepository getWSSCacheRepository() {
        
         if (cacheRepository == null) {
             synchronized (WSSUtils.class) {
               String adapterName = SystemConfigurationUtil.getProperty(
                                    WSS_CACHE_REPO_PLUGIN);
               if(adapterName == null || adapterName.length() == 0) {
                  return null;
               }
               
               try {
                   Class cacheClass = 
                           (Thread.currentThread().getContextClassLoader())
                                                  .loadClass(adapterName);
                   cacheRepository = 
                           ((WSSCacheRepository) cacheClass.newInstance());
               } catch (Exception ex) {
                   debug.error("WSSUtils.getWSSCacheRepository: " +
                         "Failed in obtaining class", ex);
                   return null;
               }
            }
        }
        return cacheRepository;
    }
    
    /**
     * Returns the SAML Attribute Map<QName, List<String>>. The attribute map
     * is generated from the given SSOToken first and if not found, then it
     * will try to find from the repository.
     * @param subjectName the principal to be used for retrieving the user
     *                    attributes.
     * @param attributeNames set of attribute names for the attribute map
     * @param namespace the name space for the saml attribute name
     * @param ssoToken the user's SSOToken.
     * @return the saml attributes for the SAML Token specification.
     */ 
    public static Map<QName, List<String>> getSAMLAttributes(
            String subjectName,
            Set attributeNames, 
            String namespace,
            SSOToken ssoToken) {

        Map<QName, List<String>> map = new HashMap();
        AMIdentity amId = null;
        try {
            amId = new AMIdentity(getAdminToken(), subjectName);
            if(!amId.isExists()) {
               if(debug.messageEnabled()) {
                  debug.message("WSSUtils.getSAMLAttributes: " +
                  "Subject " + subjectName + " does not exist");
               }
               return map;
            }
        } catch (IdRepoException ex) {
            if(debug.warningEnabled()) {
               debug.warning("WSSUtils.getSAML" +
                    "Attributes: IdRepo exception: ", ex);
            }
            return map;
        } catch (SSOException se) {
            if(debug.warningEnabled()) {
               debug.warning("WSSUtils.getSAML" +
                    "Attributes: SSOException", se);
            }
            return map;
        }

        for (Iterator iter = attributeNames.iterator(); iter.hasNext();) {
             String attribute = (String)iter.next();
             if(attribute.indexOf("=") != -1) {
                StringTokenizer st = new StringTokenizer(attribute, "=");
                if(st.countTokens() != 2) {
                   continue;
                }
                String samlAttribute = st.nextToken();
                String realAttribute = st.nextToken();
                Set values = new HashSet();
                boolean attributeFoundInSSOToken = false;
                if(ssoToken != null) {
                   try {
                       String attributeValue = 
                               ssoToken.getProperty(realAttribute);
                       if(attributeValue != null) {
                          values.add(attributeValue);
                          attributeFoundInSSOToken = true;
                       }
                   } catch (SSOException se) {
                       if(debug.warningEnabled()) {
                          debug.warning("WSSUtils.get"+
                                  "SAMLAttributes: SSOException", se);
                       }                       
                   }
                }
                
                if(!attributeFoundInSSOToken) {
                   try {
                       values = amId.getAttribute(realAttribute);
                   } catch (IdRepoException ex) {
                       if(debug.warningEnabled()) {
                          debug.warning("WSSUtils.getSAML" +
                           "Attributes: IdRepoException", ex);
                       }
                   } catch (SSOException se) {
                       if(debug.warningEnabled()) {
                          debug.warning("WSSUtils.getSAML" +
                           "Attributes: SSOException", se);
                       }
                   }
                }
                
                if(values == null || values.isEmpty()) {
                   if(debug.messageEnabled()) {
                      debug.message("WSSUtils.get"+
                           "SAMLAttributes: attribute value not found for" +
                           realAttribute);
                   }
                   continue;
                }
                List<String> list = new ArrayList();
                list.addAll(values);
                QName qName = new QName(namespace, samlAttribute);
                map.put(qName, list);
             }
        }
        return map; 
    }
    /**
     * Returns the user pseduo name from the given nameid mapper.
     * @param userName the authenticated user name.
     * @param nameIDImpl the nameid mapper implementation class
     * @return the user psueduo name.
     */
    public static String getUserPseduoName(String userName, String nameIDImpl){
   
        if(nameIDImpl == null) {
           return userName;
        }
        try {
            Class nameIDImplClass = 
                (Thread.currentThread().getContextClassLoader()).
                loadClass(nameIDImpl);
            NameIdentifierMapper niMapper = 
                    (NameIdentifierMapper)nameIDImplClass.newInstance(); 
            return niMapper.getUserPsuedoName(userName);
        } catch (Exception ex) {
            debug.error("FAMSTSAttributeProvider.getUserPseduoName: "+
            " Exception", ex);
        }
        return userName;
    }
    
    /**
     * Returns the membership attributes for the given subject.
     * @param subjectName the authenticated subject
     * @param namespace the saml attribute namespace.
     * @return the SAML attributes for the user memberships.
     */
    public static Map<QName, List<String>>  getMembershipAttributes(
               String subjectName, String namespace) {
        
        Map<QName, List<String>> map = new HashMap();
        List<String> roles = getMemberShips(subjectName);
        if(roles == null || roles.isEmpty()) {
           return map;
        }
        QName qName = new QName(namespace, MEMBERSHIPS);
        map.put(qName, roles);
        return map;
    }
    
    public static long getTimeSkew() {
        return Long.parseLong(SystemConfigurationUtil.getProperty(
                "com.sun.identity.wss.security.timeskew", "5000"));                
    }
    
    public static EncryptedKey encryptKey(final Document doc,
            final byte[] encryptedKey, final X509Certificate cert,
            final String keyWrapAlgorithm) {
        
        try {
            final PublicKey pubKey = cert.getPublicKey();
            final XMLCipher cipher;
            if(keyWrapAlgorithm != null){
               cipher = XMLCipher.getInstance(keyWrapAlgorithm);
            } else {
               cipher = XMLCipher.getInstance(XMLCipher.RSA_OAEP);
            }
            cipher.init(XMLCipher.WRAP_MODE, pubKey);

            EncryptedKey encKey = cipher.encryptKey(doc, 
                    new SecretKeySpec(encryptedKey, "AES"));
            final KeyInfo keyinfo = new KeyInfo(doc);        
            encKey.setKeyInfo(keyinfo);        
            return encKey;
        } catch (Exception ex) {
            debug.error("WSSUtils.encryptKey: failed", ex);
            return null;    
        }
    }
    
    /**
     * Returns the secret key from the security token from SAML1 Assertion.
     */
    public static Key getSecretKey(SecurityToken securityToken, 
            String certAlias) throws SecurityException {
        AssertionToken samlToken = (AssertionToken)securityToken;
        if(samlToken.isSenderVouches()) {
           return null;
        }
        Assertion assertion = samlToken.getAssertion();
        Element keyInfo = getKeyInfo(assertion);
        return WSSUtils.getXMLEncryptionManager().decryptKey(
               keyInfo, certAlias);
    }
        
    
    /**
     * Returns the trusted certificate alias from the issuer.
     */
    public static String getCertAlias(String issuer) {
        return (String)issuerTrustedCACertAliases.get(issuer);
    }
    
    /**
     * Returns the list of requested claims for the given subject
     * @param subjectName the subject name
     * @param claimNames the set of requested claims
     * @param ssoToken the user's single sign-on token.
     * @return the hashmap of requested claims.
     */
    public static Map<QName, List<String>> getRequestedClaims(
            String subjectName,
            Set claimNames,            
            SSOToken ssoToken) {
        
        Map<QName, List<String>> map = new HashMap();
        AMIdentity amId = null;
        try {
            amId = new AMIdentity(getAdminToken(), subjectName);
            if(!amId.isExists()) {
               if(debug.messageEnabled()) {
                  debug.message("WSSUtils.getRequestedClaims: " +
                  "Subject " + subjectName + " does not exist");
               }
               return map;
            }
        } catch (IdRepoException ex) {
            if(debug.warningEnabled()) {
               debug.warning("WSSUtils.getRequestedClaims:" +
                    " IdRepo exception: ", ex);
            }
            return map;
        } catch (SSOException se) {
            if(debug.warningEnabled()) {
               debug.warning("WSSUtils.getRequestedClaims:" +
                    "SSOException", se);
            }
            return map;
        }
        Iterator iter = claimNames.iterator();
        while (iter.hasNext()) {
            String attrName = (String)iter.next();            
            Set values = new HashSet();
            boolean attributeFoundInSSOToken = false;
            if(ssoToken != null) {
               try {
                   String attributeValue = ssoToken.getProperty(attrName);
                   if(attributeValue != null) {
                      values.add(attributeValue);
                      attributeFoundInSSOToken = true;
                   }
               } catch (SSOException se) {
                   if(debug.warningEnabled()) {
                      debug.warning("WSSUtils.getRequestedClaims"+
                                  " SSOException", se);
                   }                       
               }
            }                
            if(!attributeFoundInSSOToken) {
               try {
                   values = amId.getAttribute(attrName);
               } catch (IdRepoException ex) {
                       if(debug.warningEnabled()) {
                          debug.warning("WSSUtils.getRequestedClaims: " +
                           " IdRepoException", ex);
                       }
               } catch (SSOException se) {
                       if(debug.warningEnabled()) {
                          debug.warning("WSSUtils.getRequestedClaims: " +
                           " SSOException", se);
                       }
                }
            }                
            if(values == null || values.isEmpty()) {
               if(debug.messageEnabled()) {
                      debug.message("WSSUtils.getRequestedClaims:"+
                           " attribute value not found for" +
                           attrName);
               }
               continue;
             }
             List<String> list = new ArrayList();
             list.addAll(values);
             QName qName = new QName(ClaimType.CLAIMS_NS + "/" + attrName);
             map.put(qName, list);
        }
        return map;
    }

    public static ProviderConfig getConfigByDnsClaim(String dnsClaim,
             String agentType) {
        try {            
            AMIdentityRepository idRepo =
                     new AMIdentityRepository(getAdminToken(), "/");
            IdSearchControl control = new IdSearchControl();
            control.setAllReturnAttributes(true);
            control.setTimeOut(0);
            Map kvPairMap = new HashMap();
            Set set = new HashSet();
            set.add(agentType);
            kvPairMap.put(AGENT_TYPE_ATTR, set);
            set = new HashSet();
            set.add(dnsClaim);
            kvPairMap.put("DnsClaim", set);
            control.setSearchModifiers(IdSearchOpModifier.AND, kvPairMap);
            IdSearchResults results = idRepo.searchIdentities(IdType.AGENTONLY,
               "*", control);
            Set agents = results.getSearchResults();
            if (!agents.isEmpty()) {
                Map attrs = (Map) results.getResultAttributes();
                AMIdentity provider = (AMIdentity) agents.iterator().next();
                String name = provider.getName();
                Set agentTypes = provider.getAttribute("AgentType");
                String type = ProviderConfig.WSC;
                if(agentTypes != null) {
                   type = (String)agentTypes.iterator().next();
                }
                return ProviderConfig.getProvider(name, type);                
            }
            return null;
        } catch (Exception ex) {
            debug.error("WSSUtils.getConfigByEndpoint: Exception", ex);
            return null;
        }
    }

    /**
     * Gets input Node Canonicalized
     *
     * @param node Node
     * @return Canonical element if the operation succeeded.
     *         Otherwise, return null.
     */
    public static Element getCanonicalElement(Node node) {
        try {
            Canonicalizer c14n = Canonicalizer.getInstance(
                "http://www.w3.org/2001/10/xml-exc-c14n#");
            byte outputBytes[] = c14n.canonicalizeSubtree(node);
            DocumentBuilder documentBuilder = XMLUtils.getSafeDocumentBuilder(false);
            Document doc = documentBuilder.parse(
                new ByteArrayInputStream(outputBytes));
            Element result = doc.getDocumentElement();
            return result;
        } catch (Exception e) {
            debug.error("WSSUtils:getCanonicalElement: " +
                "Error while performing canonicalization on " +
                "the input Node." , e);
            return null;
        }
    }
        
 }
