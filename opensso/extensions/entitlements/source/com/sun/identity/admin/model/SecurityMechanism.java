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
 * $Id: SecurityMechanism.java,v 1.2 2009/07/23 20:46:53 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import java.util.HashMap;
import java.util.Map;

public enum SecurityMechanism {

    ANONYMOUS(0),
    SAML_HOK(1),
    SAML_SV(2),
    SAML2_HOK(3),
    SAML2_SV(4),
    USERNAME_TOKEN(5),
    USERNAME_TOKEN_PLAIN(6),
    X509_TOKEN(7),
    KERBEROS_TOKEN(8),
    STS_SECURITY(9);
    
    private final int intValue;
    private static final Map<Integer, SecurityMechanism> intValues =
            new HashMap<Integer, SecurityMechanism>() {

                {
                    put(ANONYMOUS.toInt(), ANONYMOUS);
                    put(SAML_HOK.toInt(), SAML_HOK);
                    put(SAML_SV.toInt(), SAML_SV);
                    put(SAML2_HOK.toInt(), SAML2_HOK);
                    put(SAML2_SV.toInt(), SAML2_SV);
                    put(USERNAME_TOKEN.toInt(), USERNAME_TOKEN);
                    put(USERNAME_TOKEN_PLAIN.toInt(), USERNAME_TOKEN_PLAIN);
                    put(X509_TOKEN.toInt(), X509_TOKEN);
                    put(KERBEROS_TOKEN.toInt(), KERBEROS_TOKEN);
                    put(STS_SECURITY.toInt(), STS_SECURITY);
                }
            };
    private static final Map<Integer, String> localeKeys =
            new HashMap<Integer, String>() {
                {
                    put(ANONYMOUS.toInt(), "Anonymous");
                    put(SAML_HOK.toInt(), "SAML-HolderOfKey");
                    put(SAML_SV.toInt(), "SAML-SenderVouches");
                    put(SAML2_HOK.toInt(), "SAML2-HolderOfKey");
                    put(SAML2_SV.toInt(), "SAML2-SenderVouches");
                    put(USERNAME_TOKEN.toInt(), "UserNameToken");
                    put(USERNAME_TOKEN_PLAIN.toInt(), "UserNameToken-Plain");
                    put(X509_TOKEN.toInt(), "X509Token");
                    put(KERBEROS_TOKEN.toInt(), "KerberosToken");
                    put(STS_SECURITY.toInt(), "STSSecurity");
                }
            };
    private static final Map<Integer, String> configValues =
            new HashMap<Integer, String>() {
                {
                    put(ANONYMOUS.toInt(), "urn:sun:wss:security:null:Anonymous");
                    put(SAML_HOK.toInt(), "urn:sun:wss:security:null:SAMLToken-HK");
                    put(SAML_SV.toInt(), "urn:sun:wss:security:null:SAMLToken-SV");
                    put(SAML2_HOK.toInt(), "urn:sun:wss:security:null:SAML2Token-HK");
                    put(SAML2_SV.toInt(), "urn:sun:wss:security:null:SAML2Token-SV");
                    put(USERNAME_TOKEN.toInt(), "urn:sun:wss:security:null:UserNameToken");
                    put(USERNAME_TOKEN_PLAIN.toInt(), "urn:sun:wss:security:null:UserNameToken-Plain");
                    put(X509_TOKEN.toInt(), "urn:sun:wss:security:null:X509Token");
                    put(KERBEROS_TOKEN.toInt(), "urn:sun:wss:security:null:KerberosToken");
                    put(STS_SECURITY.toInt(), "urn:sun:wss:sts:security");
                }
        };

    SecurityMechanism(int intValue) {
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

    public static SecurityMechanism valueOf(int i) {
        return intValues.get(Integer.valueOf(i));
    }
}
