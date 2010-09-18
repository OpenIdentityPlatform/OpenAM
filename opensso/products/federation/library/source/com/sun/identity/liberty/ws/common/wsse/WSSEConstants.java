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
 * $Id: WSSEConstants.java,v 1.3 2008/06/25 05:47:09 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.common.wsse;

/**
 * Constants defined for Web Service Security.
 */
public class WSSEConstants {

    /**
     * Tag name for element SecurityTokenReference.
     */
    public static final String TAG_SECURITYTOKENREFERENCE =
            "SecurityTokenReference";

    /**
     * Tag name for element ResourceAccessStatement.
     */
    public static final String TAG_RESOURCEACCESSSTATEMENT =
            "ResourceAccessStatement";

    /**
     * xmlns string.
     */
    public static final String TAG_XMLNS = "xmlns";

    /**
     * Security namespace with xmlns prefix.
     */
    public static final String TAG_XML_SEC = "xmlns:sec";

    /**
     * Security namespace.
     */
    public static final String TAG_SEC = "sec";

    /**
     * wsse namespace with xmlns prefix.
     */
    public static final String TAG_XML_WSSE = "xmlns:wsse";

    /**
     * wsse namespace.
     */
    public static final String TAG_WSSE = "wsse";

    /**
     * Tag name for Usage.
     */
    public static final String TAG_USAGE = "Usage";

    /**
     * Discovery Service namespace.
     */
    public static final String TAG_DISCO = "disco";

    /**
     * Tag name for element ResourceID.
     */
    public static final String TAG_RESOURCEID = "ResourceID";

    /**
     * Tag name with namespace for element MessageAuthentication.
     */
    public static final String TAG_SEC_MESSAGEAUTHENTICATION =
            "sec:MessageAuthentication";

    /**
     * Tag name with namespace for element BinarySecurityToken.
     */
    public static final String TAG_WSSE_BINARYSECURITYTOKEN =
            "wsse:BinarySecurityToken";

    /**
     * Tag name for element SessionContextStatement.
     */
    public static final String TAG_SESSIONCONTEXTSTATEMENT =
            "SessionContextStatement";

    /**
     * Tag name for element SessionContext.
     */
    public static final String TAG_SESSIONCONTEXT = "SessionContext";

    /**
     * Tag name for ProviderID.
     */
    public static final String TAG_PROVIDERID = "ProviderID";

    /**
     * WSSE namespace definition.
     */
    public static final String NS_WSSE =
            "http://schemas.xmlsoap.org/ws/2003/06/secext";

    /**
     * WSU namespace definition.
     */
    public static final String NS_WSU =
            "http://schemas.xmlsoap.org/ws/2003/06/utility";

    /**
     * Discovery Service namespace definition.
     */
    public static final String NS_DISCO= "urn:liberty:disco:2003-08";

    /**
     * Security namespace definition.
     */
    public static final String NS_SEC = "urn:liberty:sec:2003-08";

    /**
     * ID-FF 1.2 namespace definition.
     */
    public static final String NS_LIB = "urn:liberty:iff:2003-08";

    /**
     * Tag name for BinarySecurityToken.
     */
    public static final String BINARYSECURITYTOKEN = "BinarySecurityToken";

    /**
     * Tag name for id.
     */
    public static final String TAG_ID = "id";

    /**
     * Tag name for Reference.
     */
    public static final String TAG_REFERENCE = "Reference";

    /**
     * Tag name for ProxySubject.
     */
    public static final String TAG_PROXYSUBJECT = "ProxySubject";

    /**
     * Tag name for SessionSubject.
     */
    public static final String TAG_SESSIONSUBJECT = "SessionSubject";

    /**
     * Tag URI.
     */
    public static final String TAG_URI = "URI";

    /**
     * Tag ValueType.
     */
    public static final String TAG_VALUETYPE = "ValueType";

    /**
     * Tag Security.
     */
    public static final String TAG_SECURITYT = "Security";

    /**
     * Tag with namespace prefix for PKCS7.
     */
    public static final String TAG_PKCS7 = "wsse:PKCS7";

    /**
     * First line from a certificate file.
     */
    public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----\n";

    /**
     * Last line from a certificate file.
     */
    public static final String END_CERT   = "\n-----END CERTIFICATE-----";

    /**
     * WSSE 1.1 namespace definition.
     */
    public static final String NS_WSSE_WSF11 =
          "http://docs.oasis-open.org/wss/2004/01/" +
          "oasis-200401-wss-wssecurity-secext-1.0.xsd";

    /**
     * WSU 1.1 namespace definition.
     */
    public static final String NS_WSU_WSF11 =
          "http://docs.oasis-open.org/wss/2004/01/" +
          "oasis-200401-wss-wssecurity-utility-1.0.xsd";

    /**
     * X509 token profile namespace definition.
     */
    public static final String NS_X509 =  "http://docs.oasis-open.org/wss/" +
          "2004/01/oasis-200401-wss-x509-token-profile-1.0";

    /**
     * SOAP message security namespace definition.
     */
    public static final String NS_SMS = "http://docs.oasis-open.org/wss/"  +
          "2004/01/oasis-200401-wss-soap-message-security-1.0";

    /**
     * WSU namespace with xmlns prefix.
     */
    public static final String TAG_XML_WSU = "xmlns:wsu";

    /**
     * Tag with namespace prefix for Id.
     */
    public static final String WSU_ID = "wsu:Id";
}
