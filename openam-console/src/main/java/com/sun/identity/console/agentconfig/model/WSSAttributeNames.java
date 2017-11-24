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
 * $Id: WSSAttributeNames.java,v 1.6 2009/10/20 18:49:49 babysunil Exp $
 *
 */

package com.sun.identity.console.agentconfig.model;

/**
 * Attribute names of WSS Service.
 */
public interface WSSAttributeNames {
    String SECURITY_MECH = "SecurityMech";
    String USE_DEFAULT_KEYSTORE = "useDefaultStore";
    String KEY_STORE_LOCATION = "KeyStoreFile";
    String KEY_STORE_PASSWORD = "KeyStorePassword";
    String KEY_PASSWORD = "KeyPassword";
    String CERT_ALIAS = "privateKeyAlias";
    String USERCREDENTIAL = "UserCredential";
    String USERCREDENTIAL_NAME = "UserName:";
    String USERCREDENTIAL_PWD = "UserPassword:";
    String STS_ENDPOINT = "STSEndpoint";
    String STS_MEX_ENDPOINT = "STSMexEndpoint";
    String AUTH_CHAIN = "authenticationChain";
    String PASSWORD = "userpassword";
    String STS = "STS";
    String DISCOVERY = "Discovery";
    String SAML_ATTR_MAPPING = "SAMLAttributeMapping";
    String TOKEN_CONVERSION_TYPE = "TokenConversionType";
    String KEY_TYPE = "KeyType";
    String NAME_ID_MAPPER = "NameIDMapper";
    String ATTR_NAME_SPACE = "AttributeNamespace";
    String INCLUDE_MEMEBERSHIP = "includeMemberships";
    String REQUSETED_CLAIMS = "RequestedClaims";
}
