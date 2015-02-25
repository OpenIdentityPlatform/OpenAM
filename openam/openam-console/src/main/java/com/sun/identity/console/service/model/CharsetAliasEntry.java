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
 * $Id: CharsetAliasEntry.java,v 1.2 2008/06/25 05:43:17 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import java.util.StringTokenizer;

/* - NEED NOT LOG - */

public class CharsetAliasEntry {
    public String strMimeName;
    public String strJavaName;

    public CharsetAliasEntry(String formatedStr) {
        StringTokenizer st = new StringTokenizer(formatedStr, "|");

        if (st.countTokens() == 2) {
            boolean valid = true;

            while (st.hasMoreTokens() && valid) {
                String token = st.nextToken();
                if (token.startsWith(SMG11NModelImpl.MIMENAME_PREFIX)) {
                    strMimeName = token.substring(
                        SMG11NModelImpl.MIMENAME_PREFIX.length());
                } else if (token.startsWith(SMG11NModelImpl.JAVANAME_PREFIX)) {
                    strJavaName = token.substring(
                        SMG11NModelImpl.JAVANAME_PREFIX.length());
                } else {
                    valid = false;
                }
            }
        }
    }

    public static String toString(String mimeName, String javaName) {
        return SMG11NModelImpl.MIMENAME_PREFIX + mimeName + "|" +
            SMG11NModelImpl.JAVANAME_PREFIX + javaName;
    }

    public boolean isValid() {
        return (strMimeName != null) && (strJavaName != null);
    }
}
