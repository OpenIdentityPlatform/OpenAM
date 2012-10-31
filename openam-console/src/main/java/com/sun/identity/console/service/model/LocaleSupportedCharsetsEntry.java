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
 * $Id: LocaleSupportedCharsetsEntry.java,v 1.2 2008/06/25 05:43:18 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

/* - NEED NOT LOG - */

public class LocaleSupportedCharsetsEntry {
    public String strLocale;
    public String strCharsets;

    public LocaleSupportedCharsetsEntry(String formatedStr) {
        StringTokenizer st = new StringTokenizer(formatedStr, "|");

        if (st.countTokens() == 2) {
            boolean valid = true;

            while (st.hasMoreTokens() && valid) {
                String token = st.nextToken();
                if (token.startsWith(SMG11NModelImpl.LOCALE_PREFIX)) {
                    strLocale = token.substring(
                        SMG11NModelImpl.LOCALE_PREFIX.length());
                } else if (token.startsWith(SMG11NModelImpl.CHARSETS_PREFIX)) {
                    strCharsets = token.substring(
                        SMG11NModelImpl.CHARSETS_PREFIX.length());
                } else {
                    valid = false;
                }
            }
        }
    }

    public static String toString(String locale, String charsets) {
        return SMG11NModelImpl.LOCALE_PREFIX + locale + "|" +
            SMG11NModelImpl.CHARSETS_PREFIX + charsets;
    }

    public boolean isValid() {
        return (strLocale != null) && (strCharsets != null);
    }
    
    /*
     * Here are we checking if the allCharSets all ready contains the locale
     * in charsets
     */
    public static void validate(Set allCharSets, String strLocale) 
        throws AMConsoleException {
            if ((allCharSets != null) && !allCharSets.isEmpty()) {
                for (Iterator i = allCharSets.iterator(); i.hasNext(); ) {
                    LocaleSupportedCharsetsEntry entry = new
                        LocaleSupportedCharsetsEntry((String)i.next());
                    if (entry.strLocale.equals(strLocale)) {
                        throw new AMConsoleException(
                            "globalization.service.locale.already.exists");
                    }
                }
            }
    }
}
