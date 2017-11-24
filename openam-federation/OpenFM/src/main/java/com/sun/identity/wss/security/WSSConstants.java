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
 * $Id: WSSConstants.java,v 1.15 2009/11/16 21:52:58 mallas Exp $
 *
 */

package com.sun.identity.wss.security;

/**
 * This class defines the Constants related to Web services security token
 * profiles.
 */
public class WSSConstants {

     public static final String WSSE_SECURITY_LNAME = "Security";

     public static final String BODY_LNAME = "Body";

     public static final String WSSE_NS = "http://docs.oasis-open.org/wss" +
                 "/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"; 
     
     public static final String WSSE11_NS = 
           "http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"; 

     public static final String WSSE_TAG = "wsse";

     public static final String NS_XML =  "http://www.w3.org/2000/xmlns/";

     public static final String TAG_XML_WSSE = "xmlns:wsse";
     
     public static final String TAG_XML_WSSE11 = "xmlns:wsse11";

     public static final String WSU_NS = "http://docs.oasis-open.org/wss" +
                 "/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";

     public static final String WSU_TAG = "wsu";

     public static final String TAG_XML_WSU = "xmlns:wsu";

     public static final String WSU_ID = "wsu:Id";

     public static final String TAG_BINARY_SECURITY_TOKEN = 
                         "BinarySecurityToken";

     public static final String WSSE_MSG_SEC = "http://docs.oasis-open.org/" +
             "wss/2004/01/oasis-200401-wss-soap-message-security-1.0";
 
     public static final String WSSE_X509_NS = "http://docs.oasis-open.org/" +
             "wss/2004/01/oasis-200401-wss-x509-token-profile-1.0";

     public static final String TIME_STAMP = "Timestamp";

     public static final String TAG_KEYIDENTIFIER = "KeyIdentifier";

     public static final String TAG_REFERENCE = "Reference";

     public static final String TAG_SECURITYTOKEN_REFERENCE = 
                         "SecurityTokenReference";

     public static final String TAG_X509DATA = "X509Data";

     public static final String ASSERTION_VALUE_TYPE = 
                   "http://docs.oasis-open.org/" +
                   "wss/oasis-wss-saml-token-profile-1.0#SAMLAssertionID";
     
     public static final String SAML2_ASSERTION_VALUE_TYPE = 
                   "http://docs.oasis-open.org/" +
                   "wss/oasis-wss-saml-token-profile-1.1#SAMLID";

     public static final String XMLSIG_NAMESPACE_URI =   
                   "http://www.w3.org/2000/09/xmldsig#";

     public static final String PASSWORD_DIGEST_TYPE =
                   "http://docs.oasis-open.org/" +
                   "wss/2004/01/oasis-200401-wss-" +
                   "username-token-profile-1.0#PasswordDigest";

     public static final String PASSWORD_PLAIN_TYPE =
                   "http://docs.oasis-open.org/" +
                   "wss/2004/01/oasis-200401-wss-" +
                   "username-token-profile-1.0#PasswordText";

     public static final String TAG_USERNAME_TOKEN = "UsernameToken";
   
     public static final String MUST_UNDERSTAND = "mustUnderstand";

     public static final String CREATED = "Created";

     public static final String EXPIRES = "Expires";

     public static final long INTERVAL = 300;

     public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----\n";
 
     public static final String END_CERT   = "\n-----END CERTIFICATE-----";

     public static final String TRANSFORMATION_PARAMETERS = 
                                "TransformationParameters";

     public static final String TAG_URI = "URI";

     public static final String TAG_VALUETYPE = "ValueType";
     
     public static final String SAML_VALUETYPE = "http://docs.oasis-open.org/" +
         "wss/oasis-wss-saml-token-profile-1.0#SAMLAssertionID";
     
     public static final String TAG_X509CERTIFICATE = "X509Certificate";
     
     public static final String TAG_KEYNAME = "KeyName";
     
     public static final String TAG_KEYVALUE = "KeyValue";
     
     public static final String TAG_KEYINFO = "KeyInfo";
     
     public static final String CLASSREF_AUTHN_CONTEXT_SOFTWARE_PKI = 
             "urn:oasis:names:tc:SAML:2.0:ac:classes:SoftwarePKI";
     
     public static final String KEY_INFO_DATA_TYPE = 
                         "saml:KeyInfoConfirmationDataType";
     
     public static final String TOKEN_TYPE = "wsse11:TokenType";
     
     public static final String SAML2_TOKEN_TYPE = 
     "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
     
     public static final String SAML11_TOKEN_TYPE = 
     "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";

     public static final String TAG_USERNAME_VALUE_TYPE = 
             "http://docs.oasis-open.org/wss/2004/01/" + 
             "oasis-200401-wss-username-token-profile-1.0#UsernameToken";
     
     public static final String KERBEROS_VALUE_TYPE = 
         "http://docs.oasis-open.org/wss/oasis-wss-kerberos-token-profile-1.1"
         + "#Kerberosv5_AP_REQ";
     
    public static final String KEYIDENTIFIER_REFERENCE = "KeyIdentifierRef";

    public static final String DIRECT_REFERENCE = "DirectReference";

    public static final String X509DATA_REFERENCE = "X509IssuerSerialRef";
    
    public static final String KEY_IDENTIFIER_VALUE_TYPE = 
                 WSSE_X509_NS + "#X509SubjectKeyIdentifier";
    
    public static final String TAG_ENCODING_TYPE = "EncodingType";
    
    public static final String TAG_X509_ISSUERSERIAL = "X509IssuerSerial";
    
    public static final String TAG_X509_SERIALNUMBER = "X509SerialNumber";
    
    public static final String TAG_X509_ISSUERNAME = "X509IssuerName";
    
    public static final String CUSTOM_TOKEN = "CustomToken";
    
    public static final String ENCRYPTED_USER_PASSWORD = 
                        "EncryptedUserPassword";
    
    public static final String KERBEROS_AUTH_CTX_CLASS_REF = 
            "urn:oasis:names:tc:SAML:2:0:ac:classes:Kerberos";
    
    public static final String PASSWORD_AUTH_CTX_CLASS_REF = 
            "urn:oasis:names:tc:SAML:2.0:ac:classes:Password";
    
    public static final String PASSWORD_PROTECTED_AUTH_CTX_CLASS_REF =
            "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport";
    
    public static final String PUBLIC_KEY_AUTH_CTX_CLASS_REF =
            "urn:oasis:names:tc:SAML:2.0:ac:classes:X509";
    
    public static final String SOFTWARE_PKI_AUTH_CTX_CLASS_REF = 
            "urn:oasis:names:tc:SAML:2.0:ac:classes:Software-PKI";
    
    public static final String AUTH_METHOD = "AuthMethod";
    
    public static final String CACHE_TIMEOUT_INTERVAL = 
                  "com.sun.identity.wss.security.cache.timeout.interval";
    
    public static final String CACHE_CLEANUP_INTERVAL = 
                  "com.sun.identity.wss.security.cache.cleanup.interval";
    
    public static final String wsaNS = "http://www.w3.org/2005/08/addressing";
    
    public static final String wsaMessageID = "MessageID";
    
    public static final String HASHED_USER_PASSWORD = "HashedUserPassword";

    public static final String WSID_NS =
            "http://schemas.xmlsoap.org/ws/2006/02/addressingidentity";

    public static final String TAG_XML_WSID = "xmlns:wsid";

    public static final String TAG_IDENTITY = "wsid:Identity";

    public static final String TAG_DNSCLAIM = "wsid:DnsClaim";

    public static final String IDENTITY = "Identity";

    public static final String DNS_CLAIM = "DnsClaim";

    public static final String SECURITY_TOKEN = "SecurityToken";

    public static final String TO = "To";

    public static final String FROM = "From";

    public static final String REPLY_TO = "ReplyTo";
    
        
}

