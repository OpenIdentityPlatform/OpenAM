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
 * $Id: X509SigningRefType.java,v 1.2 2009/07/23 20:46:53 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import java.util.HashMap;
import java.util.Map;

public enum X509SigningRefType {

    DIRECT(0),
    KEY_IDENTIFIER(1),
    ISSUER_SERIAL(2);
    
    private final int intValue;
    private static final Map<Integer, X509SigningRefType> intValues =
            new HashMap<Integer, X509SigningRefType>() {

                {
                    put(DIRECT.toInt(), DIRECT);
                    put(KEY_IDENTIFIER.toInt(), KEY_IDENTIFIER);
                    put(ISSUER_SERIAL.toInt(), ISSUER_SERIAL);
                }
            };
    private static final Map<Integer, String> localeKeys =
            new HashMap<Integer, String>() {
                {
                    put(DIRECT.toInt(), "direct");
                    put(KEY_IDENTIFIER.toInt(), "key_identifier");
                    put(ISSUER_SERIAL.toInt(), "issuer_serial");
                }
            };
    private static final Map<Integer, String> configValues =
            new HashMap<Integer, String>() {
                {
                    put(DIRECT.toInt(), "DirectReference");
                    put(KEY_IDENTIFIER.toInt(), "KeyIdentifierRef");
                    put(ISSUER_SERIAL.toInt(), "X509IssuerSerialRef");
                }
        };


    X509SigningRefType(int intValue) {
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

    public static X509SigningRefType valueOf(int i) {
        return intValues.get(Integer.valueOf(i));
    }
}
