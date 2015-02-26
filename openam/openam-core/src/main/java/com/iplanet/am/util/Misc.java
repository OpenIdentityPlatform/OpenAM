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
 * $Id: Misc.java,v 1.6 2009/01/28 05:34:48 ww203982 Exp $
 *
 */

package com.iplanet.am.util;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.sun.identity.shared.ldap.util.DN;

/**
 * The Misc class contains various misc methods :)
 */
public class Misc {

    static final String localDsameServer = SystemProperties
        .get("com.iplanet.am.server.host");

    // default is false to ensure more stringent (security-wise) check
    private static final boolean isCaseInsensitiveDN = Boolean.valueOf(
        SystemProperties.get(Constants.CASE_INSENSITIVE_DN)).booleanValue();

    /**
     * This method is a convenience to get a single attribute from a Map
     * returned by SMS and the dpro SDK. SDK returns a Map with a tuple of
     * attribute name and a Set for the values. When all you want is a string
     * this method is nice.
     * 
     * @deprecated As of OpenSSO version 8.0
     * {@link com.sun.identity.shared.datastruct.CollectionHelper#getMapAttr(Map, String)}
     */
    public static String getMapAttr(Map m, String name) {

        String retVal = null;
        Set s = (Set) m.get(name);
        if (s != null) {
            Iterator iter = s.iterator();
            if (iter.hasNext()) {
                retVal = (String) iter.next();
                if (retVal != null) {
                    retVal = retVal.trim();
                }
            }
        }
        return retVal;
    }

    /**
     * @deprecated As of OpenSSO version 8.0
     * {@link com.sun.identity.shared.datastruct.CollectionHelper#getMapAttr(Map, String, String)}
     */
    public static String getMapAttr(Map m, String name, String defaultValue) {

        String retVal = defaultValue;
        Set s = (Set) m.get(name);
        if (s != null) {
            Iterator iter = s.iterator();
            if (iter.hasNext()) {
                String tmp = (String) iter.next();
                if (tmp != null) {
                    tmp = tmp.trim();
                    if (tmp.length() > 0) {
                        retVal = tmp;
                    }
                }

            }
        }
        return retVal;
    }

    /**
     * This method is a convenience to get a single int value from a Map
     * returned by SMS and the dpro SDK. This method picks up the first value
     * (String) of the attribute, and returns the int value of the string. If
     * there is no value for the attribute, or the first value could not be
     * parsed as a valid integer, returns the default value as an integer.
     * 
     * @param m
     *            Map, key is attribute name, value is String Set which contains
     *            all the values for the attribute
     * @param name
     *            Attribute name
     * @param defaultValue
     *            default value for the attribute
     * @param debug
     *            Debug
     * @return int value
     * @throws NumberFormatException
     *             when fails to parse the defaultValue argument as a signed
     *             decimal integer for return
     * @deprecated As of OpenSSO version 8.0
     * {@link com.sun.identity.shared.datastruct.CollectionHelper#getIntMapAttr(Map, String, String, Debug)}
     */
    public static int getIntMapAttr(Map m, String name, String defaultValue,
            Debug debug) throws NumberFormatException {
        try {
            return Integer.parseInt(getMapAttr(m, name, defaultValue));
        } catch (Exception e) {
            debug.error("getIntMapAttr : " + name, e);
            return Integer.parseInt(defaultValue);
        }
    }

    /**
     * This method is a convenience to get a single int value from a Map
     * returned by SMS and the dpro SDK. This method picks up the first value
     * (String) of the attribute, and returns the int value of the string. If
     * there is no value for the attribute, or the first value could not be
     * parsed as a valid integer, returns the default value as an integer.
     * 
     * @param m
     *            Map, key is attribute name, value is String Set which contains
     *            all the values for the attribute
     * @param name
     *            Attribute name
     * @param defaultValue
     *            default value for the attribute
     * @param debug
     *            Debug
     * @return int value
     * @deprecated As of OpenSSO version 8.0
     * {@link com.sun.identity.shared.datastruct.CollectionHelper#getIntMapAttr(Map, String, int, Debug)}
     */
    public static int getIntMapAttr(Map m, String name, int defaultValue,
            Debug debug) {
        try {
            return Integer.parseInt(getMapAttr(m, name));
        } catch (Exception e) {
            debug.error("getIntMapAttr : " + name, e);
            return defaultValue;
        }
    }

    /**
     * check if dn1 is descendant of dn2
     * 
     * @param dn1
     *            dn string
     * @param dn2
     *            dn string
     * @return true if dn1 is descendant of dn2 or equals to dn2, false
     *         otherwise
     */
    public static boolean isDescendantOf(String dn1, String dn2) {
        DN temp1 = new DN(dn1);
        DN temp2 = new DN(dn2);
        if (temp1.equals(temp2)) {
            return true;
        }
        return temp1.isDescendantOf(temp2);
    }

    /**
     * This convenience method is for getting server specific attributes from a
     * list attribute. Server specific is determined by prefixing a list
     * attribute value with DSAME local server name followed by the | character.
     * If the list has more than one entry but no matching local server prefixes
     * than null is returned as this is an invalid configuration for these type
     * of attributes. This allows services like authentication to support a
     * geographic directory configuration.
     *
     * @deprecated As of OpenSSO version 8.0
     * {@link com.sun.identity.shared.datastruct.CollectionHelper#getServerMapAttr(Map, String)}
     */
    public static String getServerMapAttr(Map m, String attrName) {

        Set attrValues = (Set) m.get(attrName);
        if (attrValues == null || attrValues.isEmpty()) {
            return null;
        }
        Iterator iter = attrValues.iterator();
        int index = -1;
        if (attrValues.size() == 1) {
            String strServer = (String)iter.next();
            if (strServer != null) {
                strServer = strServer.trim();
            }
            if (strServer.startsWith(localDsameServer)) {
                index = strServer.indexOf("|");
                if (index != -1) {
                    return strServer.substring(index + 1);
                }
            }
            return strServer;
        }
        while (iter.hasNext()) {
            String s = (String)iter.next();
            if (s != null) {
                if (s.startsWith(localDsameServer)) {
                    index = s.indexOf("|");
                    if (index != -1) {
                        return s.substring(index + 1).trim();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns a canonicalized form of the DN String
     * 
     * @param dn
     *            String representing a DN.
     * @return a canonicalized form of the DN String
     */
    public static String canonicalize(String dn) {
        // CAUTION! toLowerCase() canonicalization
        // is technically too agressive as DN might
        // be having attributes with caseExactSyntax
        // LDAP SDK owners convinced us that it is hardly
        // possible (and practical) in all the existing
        // LDAP server/client implementations to
        // implement any non-trivial matching rules
        // (including mixture of attributes
        // with caseInsensitive/caseSensitive syntax)
        // differences (given that it requires schema queries)

        String canonicalizedDN = (new DN(dn)).toRFCString();
        if (isCaseInsensitiveDN) {
            canonicalizedDN = canonicalizedDN.toLowerCase();
        }
        return canonicalizedDN;
    }
}
