/*
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
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 */
package com.sun.identity.shared.datastruct;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This class contains various Collection manipulation methods.
 */
public class CollectionHelper {

    private static final String localDsameServer = SystemPropertiesManager.get(Constants.AM_SERVER_HOST);

    private static final Logger logger = LoggerFactory.getLogger(CollectionHelper.class);
    private static final String SEPARATOR = "|";

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
     * Return String from a map of strings to set of strings.
     * @param map
     * @param key
     * @return the String value from a map of strings to set of strings.
     * @throws ValueNotFoundException if no value is found for the key.
     *
     */
    public static String getMapAttrThrows(Map map, String key) throws ValueNotFoundException {
        String str = getMapAttr(map, key);
        if (StringUtils.isBlank(str)){
            throw new ValueNotFoundException("No value found for key '" + key + "'.");
        }
        return str;
    }

    /**
     * Gets the set based on the passed key.
     *
     * @param map
     *         the map
     * @param key
     *         key to lookup
     *
     * @return associated set
     *
     * @throws ValueNotFoundException
     *         should the key not exist
     */
    public static Set<String> getMapSetThrows(Map<String, Set<String>> map, String key) throws ValueNotFoundException {
        if (!map.containsKey(key)) {
            throw new ValueNotFoundException("No value found for key " + key);
        }

        return map.get(key);
    }

    /*
     * The key we are given must refer to an entry in the Map which is a set of lines of the form:<br>
     *     en_GB|Here is some text in English<br>
     *     fr_FR|Voici un texte en français<br>
     * All the text must fit onto one line.  If it does not, we are unlikely to be able to cope since lines will be
     * retrieved from the {@link Set} in an almost random order.  Also if you specify the same locale on two or more
     * lines, only one can be added to the map (a random one due to the random order - although you will get a warning.
     * Caveat administrator.
     *
     * @param map The map of strings (keys) to sets of strings
     * @param key The key to use to access the map
     * @return A map of locales to localized text.
     * @throws ValueNotFoundException if the set of values we need are not present
     */
    public static Map<Locale, String> getLocaleMapAttrThrows(Map<String, Set<String>> map, String key)
            throws ValueNotFoundException {

        Set<String> values = map.get(key);

        if (values == null || values.isEmpty()) {
            throw new ValueNotFoundException("No value found for key '" + key + "'.");
        }

        Map<Locale, String> result = new HashMap<>();

        for (String s : values) {
            // ignore blank or empty lines
            if (org.forgerock.openam.utils.StringUtils.isBlank(s)) {
                continue;
            }
            String[] parts = s.split(Pattern.quote(SEPARATOR));
            if (parts.length != 2) {
                logger.warn("Config key "
                        + key
                        + " has value in invalid format: "
                        + s);
                continue;
            }
            Locale locale = com.sun.identity.shared.locale.Locale.getLocale(parts[0]);
            if (result.containsKey(locale)) {
                logger.warn("Config key "
                        + key
                        + " has multiple entries for locale "
                        + locale.toString());
            }
            result.put(locale, parts[1]);
        }
        return result;
    }

    /**
     * Gets a boolean attribute from a {@code Map<String, Set<String>>}, defaulting to the given default value if
     * the attribute is not present.
     *
     * @param map the attribute map.
     * @param name the name of the attribute to retrieve.
     * @param defaultValue the value to use if the attribute is not present.
     * @return the boolean value using {@link Boolean#parseBoolean(String)}.
     */
    public static boolean getBooleanMapAttr(Map map, String name, boolean defaultValue) {
        String value = getMapAttr(map, name, Boolean.toString(defaultValue));
        return Boolean.parseBoolean(value);
    }

    /**
     * Gets a boolean attribute from a {@code Map<String, Set<String>>}, throwing an exception if no boolean value (case
     * insensitive comparisons against "true" or "false") is found for the given key.
     *
     * @param map
     *            the attribute map.
     * @param name
     *            the name of the attribute to retrieve.
     * @return the boolean value using {@link Boolean#parseBoolean(String)}.
     * @throws ValueNotFoundException
     *             if no boolean value is found for the given key.
     */
    public static boolean getBooleanMapAttrThrows(Map map, String name)
            throws ValueNotFoundException {
        String value = getMapAttrThrows(map, name);
        Boolean boolValue = Boolean.parseBoolean(value);
        return boolValue;
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
     * Returns integer value from a Map of String of Set of String.
     *
     * @param map
     *            Map of String of Set of String.
     * @param name
     *            Key of the map entry.
     * @return integer value from a Map of String of Set of String.
     * @throws ValueNotFoundException
     *             if there is no value for the key provided.
     */
    public static int getIntMapAttrThrows(Map map, String name)
            throws ValueNotFoundException {
        try {
            return Integer.parseInt(getMapAttr(map, name));
        } catch (NumberFormatException nfe) {
            throw new ValueNotFoundException("No value found for key '" + name + "'.");
        }
    }

    /**
     * Returns a long value from the given configuration map.
     *
     * @param config the map of attribute values.
     * @param name the attribute name to get.
     * @param defaultValue the default value to use if the attribute is not set or is not a long.
     * @param debug the debug object to report format errors to.
     * @return the long value of the attribute or the defaultValue if not set/not a long.
     */
    public static long getLongMapAttr(Map<String, Set<String>> config, String name, long defaultValue, Debug debug) {
        String valueString = null;
        try {
            valueString = getMapAttr(config, name);
            return Long.parseLong(valueString);
        } catch (NumberFormatException nfe) {
            debug.error("Unable to parse " + name + "=" + valueString, nfe);
            return defaultValue;
        }
    }

    /**
     * Given the map attempts to return the named value as a long.
     *
     * @param map
     *         the map
     * @param name
     *         the named value
     *
     * @return the corresponding long value
     *
     * @throws ValueNotFoundException
     *         should the value fail to parse
     */
    public static long getLongMapAttrThrows(Map<String, Set<String>> map, String name) throws ValueNotFoundException {
        try {
            return Long.parseLong(getMapAttr(map, name));
        } catch (NumberFormatException nfe) {
            throw new ValueNotFoundException("No value found for key '" + name + "'.");
        }
    }

    /**
     * Returns the first attribute value for the corresponding name in the config map and parses it to a long.
     *
     * @param config The map where the attribute should be retrieved from.
     * @param name The name of the attribute that should be retrieved from the map.
     * @return The attribute from the map corresponding to the provided attribute name, parsed to a long.
     * If the attribute does not exist the current date time will be returned.
     */
    public static long getMapAttrAsDateLong(Map<String, Set<String>> config, String name, Logger logger) {
        String valueString = null;
        try {
            valueString = getMapAttr(config, name);
            return Long.parseLong(valueString);
        } catch (NumberFormatException nfe) {
            logger.error("Unable to parse " + name + "=" + valueString, nfe);
            return newDate().getTime();
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
     * The priority order of the attributes as follows.
     * 1- LDAP Servers belong to current OpenAM Server, localDsameServer is prefixed with the attribute
     * 2- LDAP Servers belong to no OpenAM Server, no server prefix
     * 3- All other servers - LDAP Servers prefixed with other OpenAM Servers
     * This allows services like authentication to support a geographic directory configuration.
     *
     * @param map Map of String of Set of String.
     * @param attrName Key of the map entry of interest.
     * @return attributes based on the prioritization.
     */
    public static Set<String> getServerMapAttrs(Map<String, Set<?>> map, String attrName) {
        Set<String> ret = new LinkedHashSet<String>();
        Set<String> attrValues = (Set<String>) map.get(attrName);
        Set<String> currentServerDefined = new LinkedHashSet<String>();
        Set<String> otherServerDefined = new LinkedHashSet<String>();
        Set<String> nonMatchingServers = new LinkedHashSet<String>();
        for (String attr : attrValues) {
            if (attr != null) {
                attr = attr.trim();
                int index = attr.indexOf("|");
                if (index == -1) {
                    nonMatchingServers.add(attr);
                } else {
                    String currentPrefix = attr.substring(0, index);
                    if (currentPrefix.equalsIgnoreCase(localDsameServer)) {
                        attr = attr.substring(index + 1);
                        currentServerDefined.add(attr);
                    } else {
                        attr = attr.substring(index + 1);
                        otherServerDefined.add(attr);
                    }
                }
            }
        }
        ret.addAll(currentServerDefined);
        ret.addAll(nonMatchingServers);
        ret.addAll(otherServerDefined);
        return ret;
    }
}
