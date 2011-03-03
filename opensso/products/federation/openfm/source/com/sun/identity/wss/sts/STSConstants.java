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
 * $Id: STSConstants.java,v 1.16 2009/08/29 03:05:58 mallas Exp $
 *
 */

package com.sun.identity.wss.sts;

/**
 * This class defines the Constants related to Security Token
 * Service.
 */
public class STSConstants {

    /** WS-Trust namespace URI */
    public static final String WST10_NAMESPACE = 
        "http://schemas.xmlsoap.org/ws/2005/02/trust";
    public static final String WST13_NAMESPACE =
            "http://docs.oasis-open.org/ws-sx/ws-trust/200512";
    public static final String WST_VERSION_13= "1.3";
    public static final String WST_VERSION_10 = "1.0";
    
    public static final String WST_PREFIX = "wst:";    
    public static final String WST_XMLNS = "xmlns:wst";
    
    public static final String WSP_PREFIX = "wsp:";
    public static final String WSP_XMLNS = "xmlns:wsp";
    public static final String WSP_NS = 
                  "http://schemas.xmlsoap.org/ws/2004/09/policy";
    public static final String WSA_PREFIX = "wsa:";
    public static final String WSA_XMLNS = "xmlns:wsa";
    public static final String WSA_NS = "http://www.w3.org/2005/08/addressing";
                  
   
    /**
     * URI for KeyType     
     */
    public static final String SYMMETRIC_KEY = "SymmetricKey";
    public static final String PUBLIC_KEY = "PublicKey";
    public static final String WST10_PUBLIC_KEY = WST10_NAMESPACE+ "/PublicKey";
    public static final String WST10_SYMMETRIC_KEY =
                               WST10_NAMESPACE + "/SymmetricKey";
    public static final String WST10_BEARER_KEY = 
            "http://schemas.xmlsoap.org/ws/2005/05/identity/NoProofKey";
    
    public static final String WST13_PUBLIC_KEY = WST13_NAMESPACE+ "/PublicKey";
    public static final String WST13_SYMMETRIC_KEY = 
                               WST13_NAMESPACE + "/SymmetricKey";
    public static final String WST13_BEARER_KEY = WST13_NAMESPACE + "/Bearer";
            

    /**
     * URI for TokenType
     */
    public static final String SAML11_ASSERTION_TOKEN_TYPE = 
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";

    public static final String SAML20_ASSERTION_TOKEN_TYPE = 
        "urn:oasis:names:tc:SAML:2.0:assertion";
    
    public static String ASSERTION_ELEMENT = "Assertion";
    public static final String SAML20_NAMESPACE = "xmlns:saml2";
    public static final String SAML10_NAMESPACE = "xmlns:saml";
            
    public static final String SAML10_ASSERTION = 
        "urn:oasis:names:tc:SAML:1.0:assertion";
    public static final String SAML20_ASSERTION = 
        "urn:oasis:names:tc:SAML:2.0:assertion";
            
    public static final String SSO_TOKEN_TYPE = "FAMSSOToken";
    
    public static final String FAM_TOKEN_NS = 
        "http://www.sun.com/identity/famtoken";
        
    public static final String STS_CLIENT_USER_TOKEN_PLUGIN =
        "com.sun.identity.wss.sts.clientusertoken";
    
    public static final String SAML_HOLDER_OF_KEY_1_0 = 
        "urn:oasis:names:tc:SAML:1.0:cm:holder-of-key";
    public static final String SAML_HOLDER_OF_KEY_2_0 = 
        "urn:oasis:names:tc:SAML:2.0:cm:holder-of-key";
    public static final String SAML_BEARER_1_0 = 
        "urn:oasis:names:tc:SAML:1.0:cm:bearer";
    public static final String SAML_BEARER_2_0 = 
        "urn:oasis:names:tc:SAML:2.0:cm:bearer";
    public static final String SAML_SENDER_VOUCHES_1_0 = 
        "urn:oasis:names:tc:SAML:1.0:cm:sender-vouches";
    public static final String SAML_SENDER_VOUCHES_2_0 = 
        "urn:oasis:names:tc:SAML:2.0:cm:sender-vouches";
    
    // Jar files path at OpenSSO client, for OpenSSO
    // classloader to load those jar files. 
    public static final String FAM_CLASSLOADER_DIR_PATH = 
        "com.sun.identity.classloader.client.jarsPath";
    
    public static final String TRUSTED_ISSUERS = "trustedIssuers";
    
    public static final String TRUSTED_IPADDRESSES = "trustedIPAddresses";
    
    public static final String WSIT_VERSION_CLASS = 
            "com.sun.xml.ws.security.trust.impl.ic.ICContractImpl";
    
    public static final String WST_VERSION_ATTR = "WSTrustVersion";
    
    public static final String USER_NAME_TOKEN = "UsernameToken";
    
    public static final String ANONYMOUS_ADDRESS = 
            "http://www.w3.org/2005/08/addressing/anonymous";
        
}

