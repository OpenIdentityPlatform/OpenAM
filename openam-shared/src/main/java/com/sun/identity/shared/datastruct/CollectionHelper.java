/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CollectionHelper.java,v 1.6 2010/01/06 22:31:55 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */
package com.sun.identity.shared.datastruct;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.sun.identity.shared.Constants;
import java.util.LinkedHashSet;

/**
 * This class contains various Collection manipulation methods.
 */
public class CollectionHelper {
    private static final String localDsameServer = SystemPropertiesManager.get(
        Constants.AM_SERVER_HOST);

    /**
     * Returns String from a map of string of set of string.
     *
     * @param map Map of string of set of string.
     * @param name Key of the map entry.
     * @return String from a map of string of set of string
     */
    public static String getMapAttr(Map map, String name) {
        Set s = (Set)map.get(name);
        String retVal = ((s == null) || s.isEmpty()) ? null :
            ((String)s.iterator().next());
        return (retVal != null) ? retVal.trim() : null;
    }

    /**
     * Returns String from a map of string of set of string.
     *
     * @param map Map of string of set of string.
     * @param name Key of the map entry.
     * @param defaultValue Default value if the string is not found.
     * @return String from a map of string of set of string
     */
    public static String getMapAttr(Map map, String name, String defaultValue) {
        String str = getMapAttr(map, name);
        return ((str != null) && (str.length() > 0)) ? str : defaultValue;
    }

    /**
     * Returns integer value from a Map of String of Set of String.
     * 
     * @param map Map of String of Set of String.
     * @param name Kye of the map entry.
     * @param defaultValue Default value if the integer value is not found.
     * @param debug Debug object.
     * @return integer value from a Map of String of Set of String.
     */
    public static int getIntMapAttr(
        Map map, 
        String name,
        String defaultValue,
        Debug debug
    ) {
        try {
            return Integer.parseInt(getMapAttr(map, name, defaultValue));
        } catch (NumberFormatException nfe) {
            debug.error("CollectionHelper.getIntMapAttr", nfe);
            return Integer.parseInt(defaultValue);
        }
    }

    /**
     * Returns integer value from a Map of String of Set of String.
     * 
     * @param map Map of String of Set of String.
     * @param name Key of the map entry.
     * @param defaultValue Default value if the integer value is not found.
     * @param debug Debug object.
     * @return integer value from a Map of String of Set of String.
     */
    public static int getIntMapAttr(
        Map map,
        String name,
        int defaultValue,
        Debug debug
    ) {
        try {
            return Integer.parseInt(getMapAttr(map, name));
        } catch (NumberFormatException nfe) {
            debug.error("CollectionHelper.getIntMapAttr", nfe);
            return defaultValue;
        }
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
     * @param map Map of String of Set of String.
     * @param attrName Key of the map entry of interest.
     * @return the server name.
     */
    public static String getServerMapAttr(Map map, String attrName) {
        String result = null;
        Set attrValues = (Set)map.get(attrName);

        if (attrValues.size() == 1) {
            Iterator iter = attrValues.iterator();
            String strServer = (String)iter.next();
            if (strServer != null) {
                strServer = strServer.trim();
            }
            return strServer;
        }
        if ((attrValues != null) && !attrValues.isEmpty()) {
            for (Iterator i = attrValues.iterator();
                i.hasNext() && (result == null);
            ) {
                result = (String)i.next();
                if (result != null) {
                    result = result.trim();
                    if (result.startsWith(localDsameServer)) {
                        int index = result.indexOf("|");
                        if (index != -1) {
                            result = result.substring(index + 1);
                        } else {
                            result = null;
                        }
                    } else {
                        result = null;
                    }
                }
            }
        }
        return result;
    }

    /**
     * This convenience method is for getting server specific attributes from a
     * list attribute. Server specific is determined by prefixing a list
     * attribute value with DSAME local server name followed by the | character.
     * If the list has more than one entry but no matching local server prefixes
     * than an empty Set is returned as this is an invalid configuration for 
     * these type of attributes. This allows services like authentication to
     * support a geographic directory configuration.
     *
     * @param map Map of String of Set of String.
     * @param attrName Key of the map entry of interest.
     * @return attributes belonging to this server, or if there is only one
     * attribute, then that
     */
    public static Set<String> getServerMapAttrs(Map<String, Set<?>> map, String attrName) {
        Set<String> ret = new LinkedHashSet<String>();
        Set<String> attrValues = (Set<String>) map.get(attrName);

        if (attrValues.size() == 1) {
            Iterator<String> iter = attrValues.iterator();
            String strServer = iter.next();
            if (strServer != null) {
                strServer = strServer.trim();
            }
            ret.add(strServer);
            return ret;
        }
        for (String attr : attrValues) {
            if (attr != null) {
                attr = attr.trim();
                if (attr.startsWith(localDsameServer)) {
                    int index = attr.indexOf("|");
                    if (index != -1) {
                        attr = attr.substring(index + 1);
                        ret.add(attr);
                    }
                }
            }
        }
        return ret;
    }
}
