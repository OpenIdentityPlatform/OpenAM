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
 * $Id: AMPipeDelimitAttrTokenizer.java,v 1.2 2008/06/25 05:42:47 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.base;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

public class AMPipeDelimitAttrTokenizer {
    private static AMPipeDelimitAttrTokenizer instance =
        new AMPipeDelimitAttrTokenizer();

    private AMPipeDelimitAttrTokenizer() {
    }

    public static AMPipeDelimitAttrTokenizer getInstance() {
        return instance;
    }

    public Map tokenizes(String token) {
        Map map = new HashMap();
        StringTokenizer st = new StringTokenizer(token, "|");

        while (st.hasMoreTokens()) {
            tokenizes(st.nextToken(), map);
        }

        return map;
    }

    private void tokenizes(String token, Map map) {
        int idx = token.indexOf('=');

        if (idx != -1) {
            String name = token.substring(0, idx);
            String val = token.substring(idx+1);
            map.put(name, val);
        } 
    }

    public String deTokenizes(Map map) {
        StringBuilder buff = new StringBuilder(200);
        boolean firstElement = true;

        if ((map != null) && !map.isEmpty()) {
            for (Iterator iter = map.keySet().iterator(); iter.hasNext(); ) {
                if (!firstElement) {
                    buff.append("|");
                }
                firstElement = false;

                String name = (String)iter.next();
                String value = (String)map.get(name);
                buff.append(name).append("=").append(value);
            }
        }

        return buff.toString();
    }
}
