/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: TokenConversionType.java,v 1.1 2009/10/05 21:31:44 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import java.util.HashMap;
import java.util.Map;

public enum TokenConversionType {

    SSO_TOKEN(0),
    SAML_TOKEN(1),
    SAML2_TOKEN(2);
    
    private final int intValue;
    
    private static final Map<Integer, TokenConversionType> intValues =
            new HashMap<Integer, TokenConversionType>() {
                {
                    put(SSO_TOKEN.toInt(), SSO_TOKEN);
                    put(SAML_TOKEN.toInt(), SAML_TOKEN);
                    put(SAML2_TOKEN.toInt(), SAML2_TOKEN);
                }
            };

    private static final Map<Integer, String> localeKeys =
            new HashMap<Integer, String>() {
                {
                    put(SSO_TOKEN.toInt(), "sso_token");
                    put(SAML_TOKEN.toInt(), "saml_token");
                    put(SAML2_TOKEN.toInt(), "saml2_token");
                }
            };

    private static final Map<String, TokenConversionType> configKeys = 
        new HashMap<String, TokenConversionType>() {
            {
                put("urn:sun:wss:ssotoken", TokenConversionType.SSO_TOKEN);
                put("urn:sun:wss:samltoken", TokenConversionType.SAML_TOKEN);
                put("urn:sun:wss:saml2token", TokenConversionType.SAML2_TOKEN);
            }
        };

    private static final Map<Integer, String> configValues =
            new HashMap<Integer, String>() {
                {
                    put(SSO_TOKEN.toInt(), "urn:sun:wss:ssotoken");
                    put(SAML_TOKEN.toInt(), "urn:sun:wss:samltoken");
                    put(SAML2_TOKEN.toInt(), "urn:sun:wss:saml2token");
                }
        };


    TokenConversionType(int intValue) {
        this.intValue = intValue;
    }

    public int toInt() {
        return intValue;
    }

    public String toLocaleString() {
        Resources r = new Resources();
        return r.getString(this, localeKeys.get(Integer.valueOf(intValue)));
    }

    public String toConfigString() {
        return configValues.get(Integer.valueOf(intValue));
    }

    public static TokenConversionType valueOf(int i) {
        return intValues.get(Integer.valueOf(i));
    }
    
    public static TokenConversionType valueOfConfig(String s) {
        return configKeys.get(s);
    }
}
