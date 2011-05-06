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
 * $Id: EncryptionAlgorithm.java,v 1.2 2009/07/23 20:46:54 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import java.util.HashMap;
import java.util.Map;

public enum EncryptionAlgorithm {

    TRIPLEDES_0(0),
    TRIPLEDES_112(1),
    TRIPLEDES_168(2),
    AES_128(3),
    AES_192(4),
    AES_256(5);
    
    private final int intValue;
    private static final Map<Integer, EncryptionAlgorithm> intValues =
            new HashMap<Integer, EncryptionAlgorithm>() {

                {
                    put(TRIPLEDES_0.toInt(), TRIPLEDES_0);
                    put(TRIPLEDES_112.toInt(), TRIPLEDES_112);
                    put(TRIPLEDES_168.toInt(), TRIPLEDES_168);
                    put(AES_128.toInt(), AES_128);
                    put(AES_192.toInt(), AES_192);
                    put(AES_256.toInt(), AES_256);
                }
            };
    private static final Map<Integer, String> localeKeys =
            new HashMap<Integer, String>() {
                {
                    put(TRIPLEDES_0.toInt(), "3des_0");
                    put(TRIPLEDES_112.toInt(), "3des_112");
                    put(TRIPLEDES_168.toInt(), "3des_168");
                    put(AES_128.toInt(), "aes_128");
                    put(AES_192.toInt(), "aes_192");
                    put(AES_256.toInt(), "aes_256");
                }
            };

    EncryptionAlgorithm(int intValue) {
        this.intValue = intValue;
    }

    public int toInt() {
        return intValue;
    }

    public String toLocaleString() {
        Resources r = new Resources();
        return r.getString(this, localeKeys.get(Integer.valueOf(intValue)));
    }

    public static EncryptionAlgorithm valueOf(int i) {
        return intValues.get(Integer.valueOf(i));
    }
}
