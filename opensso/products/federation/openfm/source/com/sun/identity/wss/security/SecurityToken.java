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
 * $Id: SecurityToken.java,v 1.8 2008/08/27 19:05:52 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wss.security;

import org.w3c.dom.Element;
 
/**
 * This interface represents WS-Security token that can be inserted into
 * or extracted from SOAP messages headers for Web Services Security 
 * transactions. 
 *
 * <p> Each Security Token must need to implement this interface along with
 * <code>SecurityTokenSpec</code> for generating the Security Tokens. 
 * 
 * @supported.all.api
 */
public interface SecurityToken {
      
     /**
      * The <code>URI</code> to identify the WS-Security SAML Security Token.
      */
     public static final String WSS_SAML_TOKEN = "urn:sun:wss:samltoken";
     
     /**
      * The <code>URI</code> to identify the WS-Security SAML2 Security Token.
      */
     public static final String WSS_SAML2_TOKEN = "urn:sun:wss:saml2token";

     /**
      * The <code>URI</code> to identify the WS-Security X509 Security Token
      */
     public static final String WSS_X509_TOKEN = "urn:sun:wss:x509token";

     /**
      * The <code>URI</code> to identify the WS-Security UserName Security Token
      */
     public static final String WSS_USERNAME_TOKEN = 
                                 "urn:sun:wss:usernametoken";
     /**
      * The <code>URI</code> to identify the Liberty SAML Security Token
      */
     public static final String LIBERTY_ASSERTION_TOKEN = 
                                 "urn:sun:wss:libertyassertion";
     
     /**
      * The <code>URI</code> to identify the WS-Security Kerberos Security Token
      */
     public static final String WSS_KERBEROS_TOKEN = "urn:sun:wss:kerberostoken";
     
     /**
      * The <code>URI</code> is to identify the OpenSSO security
      * token. The OpenSSO security token contains SSOToken.
      */
     public static final String WSS_FAM_SSO_TOKEN = "urn:sun:wss:ssotoken";

     /** 
      * Returns the security token type. The possible values are
      *      {@link #WSS_SAML_TOKEN},
      *      {@link #WSS_X509_TOKEN}
      *      {@link #WSS_USERNAME_TOKEN}
      *      {@link #WSS_SAML2_TOKEN}
      */
      public String getTokenType();

      /**
       * Converts the Security Token into DOM Object.
       * 
       * @return the DOM Document Element.
       *
       * @exception SecurityException if any failure is occured.
       */
      public Element toDocumentElement() throws SecurityException;

}
