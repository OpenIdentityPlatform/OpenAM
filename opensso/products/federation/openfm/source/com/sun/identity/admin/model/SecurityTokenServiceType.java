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
 * $Id: SecurityTokenServiceType.java,v 1.1 2009/08/21 21:07:35 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import java.util.HashMap;
import java.util.Map;

public enum SecurityTokenServiceType {

    OPENSSO(0),
    OTHER(1),
    NONE(2);
    
    private final int intValue;
    private static final Map<Integer, SecurityTokenServiceType> intValues =
            new HashMap<Integer, SecurityTokenServiceType>() {

                {
                    put(OPENSSO.toInt(), OPENSSO);
                    put(OTHER.toInt(), OTHER);
                    put(NONE.toInt(), NONE);
                }
            };
    private static final Map<Integer, String> localeKeys =
            new HashMap<Integer, String>() {
                {
                    put(OPENSSO.toInt(), "OpenSSO");
                    put(OTHER.toInt(), "Other");
                    put(NONE.toInt(), "None");
                }
            };

    SecurityTokenServiceType(int intValue) {
        this.intValue = intValue;
    }

    public int toInt() {
        return intValue;
    }

    public String toLocaleString() {
        Resources r = new Resources();
        return r.getString(this, localeKeys.get(Integer.valueOf(intValue)));
    }

    public static SecurityTokenServiceType valueOf(int i) {
        return intValues.get(Integer.valueOf(i));
    }
}
