/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DNUtils.java,v 1.6 2009/11/20 23:52:54 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */

package com.sun.identity.common;

import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.LDAPDN;

public class DNUtils {
    /**
     * Returns the normalized DN string.
     * 
     * @param dn
     *            string needs to be normalized.
     * @return returns the normalized DN string if the passed in string is in DN
     *         format otherwise returns null.
     */
    public static String normalizeDN(String dn) {
        return normalizeDN(new DN(dn));
    }

    public static String normalizeDN(DN dn) {
        String newDN = null;
        if (dn != null) {
            newDN = dn.toRFCString().toLowerCase();
            // in case dn is not a DN, the return value will be "".
            if (newDN.length() == 0) {
                newDN = null;
            }
        }
        return newDN;
    }

    /**
     * Converts a DN string to the token value of the naming attribute.
     * 
     * @param dn
     *            The passed in DN string
     * @return returns the token value of the naming attribute in the passed in
     *         DN string. If the dn string is not in DN format, returns itself.
     */
    public static String DNtoName(String dn) {
        // String dn is guaranteed type of DN
        return DNtoName(dn, true);
    }

    /**
     * Converts a DN string to the token value of the naming attribute.
     *
     * @param dn
     *            The passed in DN string
     * @param noTypes
     *            if true, returns only the values of the components and 
     *            not the names(such as "cn")
     * @return returns the token value of the naming attribute in the passed in
     *         DN string. If the dn string is not in DN format, returns itself.
     */
    public static String DNtoName(String dn,  boolean noTypes) {
        // String dn is guaranteed type of DN
        String id = dn;
        try {
            id = LDAPDN.explodeDN(dn, noTypes)[0];
        } catch (Exception e) {
        }
        return id;
    }


}
