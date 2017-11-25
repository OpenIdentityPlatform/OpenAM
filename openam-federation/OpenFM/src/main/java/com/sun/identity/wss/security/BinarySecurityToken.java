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
 * $Id: BinarySecurityToken.java,v 1.9 2009/07/24 21:51:06 mallas Exp $
 *
 */

package com.sun.identity.wss.security;

import java.util.ResourceBundle;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Iterator;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLUtilsCommon;
//import com.iplanet.am.util.Locale;
import com.sun.identity.shared.locale.Locale;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertPath;
import java.security.Key;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.Oid;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import javax.security.auth.Subject;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.kerberos.KerberosTicket;
import sun.security.krb5.EncryptionKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.AccessController;

/**
 * This class <code>BinarySecurityToken</code> represents an X509
 * token that can be inserted into web services security header
 * for message level security.
 *
 * <p>This class implements <code>SecurityToken</code> and can be
 * created through security token factory. 
 */
public class BinarySecurityToken implements SecurityToken {

    private String[] certAlias = null;
    private String valueType = null;
    private String encodingType = null;
    private String id = null;
    private String xmlString = null;
    private String value = null;

    private static final String BINARY_SECURITY_TOKEN = "BinarySecurityToken";
    private static final String ENCODING_TYPE = "EncodingType";
    private static final String VALUE_TYPE = "ValueType";
    private static final String ID = "Id";
    private static Debug debug = WSSUtils.debug;
    private static ResourceBundle bundle = WSSUtils.bundle;
    private String tokenType = SecurityToken.WSS_X509_TOKEN;
    private String kerberosToken = null;
    private Key secretKey = null;    
    private KerberosTokenSpec kbSpec = null;

    /**
     * Default constructor
     */
    private BinarySecurityToken () {}

    /**
     * Constructor
     * @param tokenSpec the <code>X509TokenSpec</code> for generating
     *        binary security token.
     */
    public BinarySecurityToken(X509TokenSpec tokenSpec)
               throws SecurityException {

        if(tokenSpec == null) {
           throw new SecurityException(
                 bundle.getString("invalidTokenSpec"));
        }
       
        this.valueType = tokenSpec.getValueType();
        this.encodingType = tokenSpec.getEncodingType();
        this.certAlias = tokenSpec.getSubjectCertAlias();

        if(valueType == null || encodingType == null ||
                 certAlias == null || certAlias.length == 0) {
           debug.error("BinarySecurityToken.constructor: invalid token spec");
           throw new SecurityException(
                 bundle.getString("invalidTokenSpec"));
        }

        byte[] data = null;

        try {
            if(PKIPATH.equals(valueType)) {
               List certs = AMTokenProvider.getX509Certificates(certAlias);

               CertificateFactory factory = 
                         CertificateFactory.getInstance("X.509");
               CertPath path = factory.generateCertPath(certs);
               data = path.getEncoded();

            } else if(X509V3.equals(valueType)) {
               X509Certificate certificate = 
                     AMTokenProvider.getX509Certificate(certAlias[0]); 
               data = certificate.getEncoded();
            } else {
               debug.error("BinarySecurityToken.constructor: unsupported" +
               "value type. " + valueType);
               throw new SecurityException(
                        bundle.getString("invalidTokenSpec"));
            }
            this.value = Base64.encode(data);

        } catch (CertificateEncodingException cee) {
            debug.error("BinarySecurityToken.constructor:: Certificate " +
            "Encoding Exception", cee); 
            throw new SecurityException(
                   bundle.getString("invalidCertificate"));

        } catch (CertificateException ce) {
            debug.error("BinarySecurityToken.constructor:: Certificate " +
            "Exception", ce); 
            throw new SecurityException(
                   bundle.getString("invalidCertificate"));
        }

        this.id = SAMLUtils.generateID();
    }

    public BinarySecurityToken(X509Certificate cert, 
            String valueType, String encodingType) throws SecurityException {

        byte data[];
        try {
            data = cert.getEncoded();
        } catch (CertificateEncodingException ce) {
            debug.error("BinarySecurityToken. Invalid Certifcate", ce);
            throw new SecurityException(
                  bundle.getString("invalidCertificate"));
        }
        value = Base64.encode(data);
        this.valueType = valueType;
        this.encodingType = encodingType; 
             
    }

    /**
     * Constructor to create Kerberos Token
     * @param kbSpec The Kerberos Token Specification
     * @throws com.sun.identity.wss.security.SecurityException
     */
    public BinarySecurityToken(KerberosTokenSpec kbSpec) 
                      throws SecurityException {
         this.kbSpec = kbSpec;
         getKerberosToken();
         this.value = kerberosToken;
         this.valueType = kbSpec.getValueType();
         this.encodingType = kbSpec.getEncodingType();
         this.tokenType = SecurityToken.WSS_KERBEROS_TOKEN;
         this.id = SAMLUtils.generateID();         
                 
    }
    /**
     * Constructor
     * @param token Binary Security Token Element
     * @exception SecurityException if token Element is not a valid binary 
     *     security token 
     */
    public BinarySecurityToken(Element token) 
        throws SecurityException {

        if (token == null) {
            debug.error("BinarySecurityToken: null input token");
            throw new IllegalArgumentException(
                    bundle.getString("nullInputParameter")) ;
        }

        String elementName = token.getLocalName();
        if (elementName == null)  {
            debug.error("BinarySecurityToken: local name missing");
            throw new SecurityException(bundle.getString("nullInput")) ;
        }
	if (!(elementName.equals(BINARY_SECURITY_TOKEN)))  {
            debug.error("BinarySecurityToken: invalid binary token");
	    throw new SecurityException(bundle.getString("invalidElement") + 
                ":" + elementName) ;   
	}
        NamedNodeMap nm = token.getAttributes();
        if (nm == null) {
            debug.error("BinarySecurityToken: missing token attrs in element");
            throw new SecurityException(bundle.getString("missingAttribute"));
        }

        int len = nm.getLength();
        for (int i = 0; i < len; i++) {
            Attr attr = (Attr) nm.item(i);
            String localName = attr.getLocalName();
            if (localName == null) {
                continue;
            }

            // check Id/EncodingType/ValueType attribute
            if (localName.equals(ID)) {
                this.id = attr.getValue();
            } else if (localName.equals(ENCODING_TYPE)) {
                // no namespace match done here
                encodingType =  trimPrefix(attr.getValue());
            } else if (localName.equals(VALUE_TYPE)) {
                // no namespace match done here
                valueType = trimPrefix(attr.getValue());
            }
        }
 
	if (id == null || id.length() == 0) {
            debug.error("BinarySecurityToken: ID missing");
	    throw new SecurityException(
                  bundle.getString("missingAttribute") + " : " + ID);
	}

        if (encodingType == null) {
            debug.error("BinarySecurityToken: encoding type missing");
            throw new SecurityException(
                  bundle.getString("missingAttribute") + " : " + ENCODING_TYPE);
        }
        
        if (valueType == null) {
            debug.error("BinarySecurityToken: valueType missing");
            throw new SecurityException(
                bundle.getString("missingAttribute") + " : " + VALUE_TYPE);
        }
        
        if(valueType.equals(WSSConstants.KERBEROS_VALUE_TYPE)) {
           tokenType = SecurityToken.WSS_KERBEROS_TOKEN;
        }

        try {            
            NodeList nodelist = token.getChildNodes();
            for (int i= 0; i < nodelist.getLength(); i++) {
                Node childNode = nodelist.item(i);
                if(childNode.getNodeType() == Node.ELEMENT_NODE) {
                   continue; 
                } else if (childNode.getNodeType() == Node.TEXT_NODE) {                  
                  this.value = SAMLUtils.removeNewLineChars(
                          childNode.getNodeValue().trim());        
                }
                
            }
         //   Node node = token.getFirstChild();
            
            
            
        } catch (Exception e) {
            debug.error("BinarySecurityToken: unable to get value", e);
            this.value = null;
        }

        if (value == null || value.length() == 0) {
            debug.error("BinarySecurityToken: value missing");
            throw new SecurityException(bundle.getString("missingValue"));
        }
               
        // save the original string for toString()
        xmlString = XMLUtils.print(token);

    }
    
    /**
     * Returns Kerberos Token
     * @throws com.sun.identity.wss.security.SecurityException
     */
    private void getKerberosToken() throws SecurityException {
        Subject clientSubject = getKerberosSubject();
        final String serviceName = kbSpec.getServicePrincipal();
        try {
            Subject.doAs(clientSubject, new PrivilegedExceptionAction(){                
                public Object run() throws Exception {
                   
                    final GSSManager manager = GSSManager.getInstance();
                    final Oid krb5Oid = new Oid("1.2.840.113554.1.2.2");                    
                    GSSName serverName = manager.createName(serviceName, null);                                        
                    GSSContext context = manager.createContext(serverName,
                                         krb5Oid,
                                         null,
                                         GSSContext.DEFAULT_LIFETIME);
                    byte[] token = new byte[0];
                    token = context.initSecContext(token, 0, token.length);            
                    String encodeToken =  Base64.encode(token);
                    kerberosToken = encodeToken;            
                    return null;
                }
            });
        } catch (Exception ge) {
            debug.error("BinarySecurityToken.getKerberosToken: GSS Error", ge);
            throw new SecurityException(ge.getMessage());
        }
        
        // Obtain the session key to sign using kerberos ticket.
        Set creds = clientSubject.getPrivateCredentials();
        Iterator<Object> iter2 = creds.iterator();
        while(iter2.hasNext()){
                Object privObject = iter2.next();
                if(privObject instanceof KerberosTicket){
                    KerberosTicket kerbTicket = (KerberosTicket)privObject;
                    if(!kerbTicket.getServer().getName().equals(serviceName)){
                       continue;
                    }
                    secretKey = kerbTicket.getSessionKey();                                        
                    break;
                }
        }
       
    }
    
    private Subject getKerberosSubject() throws SecurityException {
        
        String kdcRealm = kbSpec.getKDCDomain();
        String kdcServer = kbSpec.getKDCServer();
               
        System.setProperty("java.security.krb5.realm", kdcRealm);
        System.setProperty("java.security.krb5.kdc", kdcServer);        
        Configuration config = Configuration.getConfiguration();
        KerberosConfiguration kc = null;
        if (config instanceof KerberosConfiguration) {
            kc = (KerberosConfiguration) config;
            kc.setRefreshConfig("true");
            kc.setPrincipalName(kbSpec.getServicePrincipal());
            kc.setTicketCacheDir(kbSpec.getTicketCacheDir());
        } else {
            kc = new KerberosConfiguration(config);
            kc.setRefreshConfig("true");
            kc.setPrincipalName(kbSpec.getServicePrincipal());
            kc.setTicketCacheDir(kbSpec.getTicketCacheDir());
            
        }
        Configuration.setConfiguration(kc);

        // perform service authentication using JDK Kerberos module
        try {
            LoginContext lc = new LoginContext(
                KerberosConfiguration.WSC_CONFIGURATION);
            lc.login();
            return lc.getSubject();
        } catch (LoginException ex) {
            throw new SecurityException(ex.getMessage());
        }        
    }    

    /**
     * trim prefix and get the value, e.g, for wsse:X509v3 will return X509v3 
     */
    private String trimPrefix(String val) {
        if(val.indexOf("wsse") == -1) {
           return val; 
        }
        int pos = val.indexOf(":");
        if (pos == -1) {
            return val;
        } else if (pos == val.length()) {
            return "";
        } else {
            return val.substring(pos+1);
        } 
    }

    /**
     * Gets encoding type for the token.
     *
     * @return encoding type for the token. 
     */
    public String getEncodingType() {
        return encodingType;
    }

    /**
     * Gets value type for the token.
     *
     * @return value type for the token. 
     */
    public String getValueType() {
        return valueType;
    }

    /**
     * Gets id attribute for the tokens.
     *
     * @return id attribute for the token.
     */
    public java.lang.String getId() {
        return id;
    }

    /**
     * Gets value of the token.
     *
     * @return value of the token.
     */
    public java.lang.String getTokenValue() { 
        return value;
    }
    
    /**
     * Returns the secret key for kerberos token.
     * @return the secret key
     */
    public Key getSecretKey() {
        return secretKey;
    }
       
    /**
     * Returns a String representation of the token 
     * @return A string containing the valid XML for this element
     */
    public java.lang.String toString() {
        if (xmlString == null) {
            StringBuffer sb = new StringBuffer(300);
            sb.append("<").append(WSSConstants.WSSE_TAG).append(":")
              .append(BINARY_SECURITY_TOKEN).append(" ")
              .append(WSSConstants.TAG_XML_WSSE).append("=\"")
              .append(WSSConstants.WSSE_NS).append("\" ") 
              .append(WSSConstants.TAG_XML_WSU).append("=\"")
              .append(WSSConstants.WSU_NS).append("\" ")
              .append(WSSConstants.WSU_ID).append("=\"").append(id)
              .append("\" ").append(VALUE_TYPE).append("=\"")
              .append(valueType).append("\" ")
              .append(ENCODING_TYPE).append("=\"")
              .append(encodingType).append("\">\n")
              .append(value.toString()).append("\n").append("</")
              .append(WSSConstants.WSSE_TAG).append(":")
              .append(BINARY_SECURITY_TOKEN).append(">\n");

              xmlString = sb.toString();
        }
        return xmlString;
    }

    /**
     * Returns the token type.
     * @return String the token type.
     */
    public String getTokenType() {
        return tokenType;        
    }

    /**
     * Returns the array of certificate aliases defined in this spec.
     *
     * @return String[] the array of subject certificate aliases.
     */ 
    public String[] getSubjectCertAlias() {
        return this.certAlias;
    }
    
    /**
     * Returns the signing id for binary security token.
     * @return the signing id for the binary security token.
     */
    public String getSigningId() {
        return id;
    }

    /**
     * Returns the <code>DOM</code> Element of the binary security
     * token.
     * @return Element the DOM document element of binary security token.
     * @exception SecurityException if the document element can not be
     *            created.
     */
    public Element toDocumentElement() throws SecurityException {
        Document document = XMLUtils.toDOMDocument(
                toString(), WSSUtils.debug);
        if(document == null) {
           throw new SecurityException(
                 WSSUtils.bundle.getString("cannotConvertToDocument"));
        }
        return document.getDocumentElement();
    }
            
    /**
     * The <code>X509V3</code> value type indicates that
     * the value name given corresponds to a X509 Certificate
     */
    public static final String X509V3 = WSSConstants.WSSE_X509_NS + "#X509v3";

    /**
     * The <code>PKCS7</code> value type indicates
     * that the value name given corresponds to a
     * PKCS7 object
     */
    public static final String PKCS7 = WSSConstants.WSSE_X509_NS + "#PKCS7";

    /**
     * The <code>PKIPATH</code> value type indicates
     * that the value name given corresponds to a
     * PKI Path object
     */
    public static final String PKIPATH = WSSConstants.WSSE_X509_NS + "#PKIPath";

    /** 
     * The <code>BASE64BINARY</code> encoding type indicates that
     * the encoding name given corresponds to base64 encoding of a binary value 
     */
    public static final String BASE64BINARY =  
            WSSConstants.WSSE_MSG_SEC + "#Base64Binary";
        
    /**
     * The <code>HEXBINARY</code> encoding type indicates that
     * the encoding name given corresponds to Hex encoding of
     * a binary value 
     */
    public static final String HEXBINARY =  
         WSSConstants.WSSE_MSG_SEC + "#HexBinary";
        
}
