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
 * $Id: ISResourceBundle.java,v 1.3 2008/06/25 05:42:26 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * This class provides static utility methods to be used by console and Access
 * Manager admin * CLI for storing, retrieving and deleting locale-specific
 * resource bundles in the directory (data store)
 * <p>
 * This class is a subclass of <code> java.util.ListResourceBundle </code>. The
 * method (shown below) returns an instance of ISResourceBundle, which can be
 * used to obtain the key and values as any other ResourceBundle.
 * 
 * <PRE>
 * 
 * ResourceBundle rb = ISResourceBundle.getBundle(token, "test", "en_US");
 * 
 * </PRE>
 */

public class ISResourceBundle extends ListResourceBundle {

    private static final String LOCALE_SERVICE = "sunIdentityLocaleService";

    private static final String VERSION = "1.0";

    private static final String RB_CONFIG_ID = "ResourceBundleName";

    private static final String LOCALE_CONFIG_ID = "Locale";

    private String[][] rbArray;

    private static Debug debug = Debug.getInstance("amProfile");

    private static final String bundleName = "amCommonUtils";

    public static ResourceBundle _bundle = ResourceBundle.getBundle(bundleName);

    /**
     * Non-default construtor used to create a new ISResourceBundle object This
     * constructor takes a Map of key-value pairs (both Strings) and uses that
     * Map to construct an ISResourceBundle object
     * 
     * @param keyValues
     *            Map of key-value pairs.
     */
    protected ISResourceBundle(Map keyValues) {
        super();
        if (keyValues == null) {
            throw new NullPointerException();
        }
        rbArray = new String[keyValues.size()][2];
        Iterator it = keyValues.keySet().iterator();
        int count = 0;
        while (it.hasNext()) {
            String key = (String) it.next();
            String value;
            Set values = (Set) keyValues.get(key);
            if (!values.isEmpty()) {
                value = (String) values.iterator().next();
                rbArray[count][0] = convertUnicode(key);
                rbArray[count][1] = convertUnicode(value);
                count++;
            }
        }

    }

    /**
     * Returns a ResourceBundle. This method tries to find a ResourceBundle
     * given locale using <code> java.util.ResourceManager </code> first. But if
     * it doesn't find the ResourceBundle in the local file system, then look
     * for it in the directory (data store). In the directory, it follows the
     * same protocol as the default ResourceManager. It looks for the specific
     * locale first (language and country) and if not found than language
     * locale, and if that is not found then the default locale.
     * 
     * @param token
     *            Single sign-on token of the user
     * @param rbName
     *            Name of the ResourceBundle
     * @param locale
     *            Specific locale of ResourceBundle
     * @return ResourceBundle with the key-value pairs.
     * @throws SSOException
     */
    public static ResourceBundle getResourceBundle(SSOToken token,
            String rbName, Locale locale) throws SSOException {
        if (locale == null) {
            return getResourceBundle(token, rbName, (String) null);
        }
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String loc = null;
        if (language != null && language.length() > 0) {
            loc = language;
        }
        if (country != null && country.length() > 0 && loc != null) {
            loc = loc + "_" + country;
        }
        return getResourceBundle(token, rbName, loc);
    }

    /**
     * Returns a ResourceBundle. This method tries to find a ResourceBundle
     * given locale using <code> java.util.ResourceManager </code> first. But if
     * it doesn't find the ResourceBundle in the local file system, then look
     * for it in the directory (data store). In the directory, it follows the
     * same protocol as the default ResourceManager. It looks for the specific
     * locale first (language and country) and if not found than language
     * locale, and if that is not found then the default locale.
     * 
     * @param token
     *            Single sign-on token of the user
     * @param rbName
     *            Name of the ResourceBundle
     * @param locale
     *            String identifying the language and country (for example:
     *            en_US). If this is null, then the default ResourceBundle is
     *            returned.
     * @return ISResourceBundle
     * @throws SSOException
     *             If user does not have read access to read the ResourceBundles
     *             from directory.
     */
    public static ResourceBundle getResourceBundle(SSOToken token,
            String rbName, String locale) throws SSOException {
        try {
            return getResourceBundleFromDisk(rbName, locale);
        } catch (MissingResourceException me) {
            return getResourceBundleFromDirectory(token, rbName, locale);
        }
    }

    /**
     * Deletes the specified ResourceBundle from the directory.
     * 
     * @param token
     *            Single sign-on token of user
     * @param rbName
     *            Name of ResourceBundle
     * @param locale
     *            String defining the locale. If null, then all the locales of
     *            this ResourceBundle, including the default one, are deleted.
     * @throws SMSException
     *             If there is an error trying to modify the datastore
     * @throws SSOException
     *             If this user's token has expired.
     */
    public static void deleteResourceBundle(SSOToken token, String rbName,
            String locale) throws SMSException, SSOException {
        if (rbName == null) {
            return;
        }
        ServiceConfigManager scm = new ServiceConfigManager(token,
                LOCALE_SERVICE, VERSION);
        ServiceConfig globalConfig = scm.getGlobalConfig(null);
        if (locale == null) {
            // Delete the entire ResourceBundle tree
            try {
                globalConfig.removeSubConfig(rbName);
            } catch (SMSException se) {
                throw new MissingResourceException(_bundle
                        .getString("isResourceBundleMsg1")
                        + rbName + ". " + se.getMessage(), _bundle
                        .getString("isResourceBundleMsg2"), _bundle
                        .getString("isResourceBundleMsg3"));
            }
        } else {
            ServiceConfig rbConfig = globalConfig.getSubConfig(rbName);
            if (rbConfig == null) {
                // This will happen only when there is no config for
                // the resource bundle provided.
                throw new MissingResourceException(_bundle
                        .getString("isResourceBundleMsg0")
                        + rbName + ". ", _bundle
                        .getString("isResourceBundleMsg2"), _bundle
                        .getString("isResourceBundleMsg3"));
            } else {
                rbConfig.removeSubConfig(locale);
            }
        }
    }

    /**
     * Stores or creates the ResourceBundle in the directory.
     * 
     * @param token
     *            Single sign-on token of user
     * @param rbName
     *            Name of ResourceBundle
     * @param locale
     *            Locale of ResourceBundle. If null, the default ResourceBundle
     *            is updated.
     * @param attributes
     *            Map of key-value pairs defining the ResourceBundle.
     * @throws SMSException
     *             If there is an error trying to modify the datastore
     * @throws SSOException
     *             If this user's token has expired.
     */
    public static void storeResourceBundle(SSOToken token, String rbName,
            String locale, Map attributes) throws SMSException, SSOException {
        if (rbName == null) {
            return;
        }
        ServiceConfigManager scm = new ServiceConfigManager(token,
                LOCALE_SERVICE, VERSION);
        ServiceConfig globalConfig = scm.getGlobalConfig(null);
        ServiceConfig rbConfig = globalConfig.getSubConfig(rbName);
        if (rbConfig == null) {
            // create a sub config
            globalConfig.addSubConfig(rbName, RB_CONFIG_ID, 0, new HashMap());
            rbConfig = globalConfig.getSubConfig(rbName);
        }
        if (locale == null) {
            // store these as defaults
            rbConfig.setAttributes(attributes);
        } else {
            // get/create locale subconfig and set these attributes
            ServiceConfig localeConfig = rbConfig.getSubConfig(locale);
            if (localeConfig == null) {
                rbConfig.addSubConfig(locale, LOCALE_CONFIG_ID, 0, attributes);
                localeConfig = rbConfig.getSubConfig(locale);
            } else {
                localeConfig.setAttributes(attributes);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.ListResourceBundle#getContents()
     */
    protected Object[][] getContents() {
        return rbArray;
    }

    private static ResourceBundle getResourceBundleFromDisk(String rbName,
            String locale) {
        if ((locale != null) && (locale.trim().length() > 0)) {
            locale = locale.trim();
            StringTokenizer tzer = new StringTokenizer(locale, "_");
            int count = tzer.countTokens();

            if (count <= 2 && count > 0) {
                String language = tzer.nextToken();

                if (tzer.hasMoreTokens()) {
                    return ResourceBundle.getBundle(rbName, new Locale(
                            language, tzer.nextToken()));
                } else {
                    return ResourceBundle.getBundle(rbName, new Locale(
                            language, ""));
                }
            } else {
                return ResourceBundle.getBundle(rbName);
            }
        } else {
            return ResourceBundle.getBundle(rbName);
        }
    }

    private static ResourceBundle getResourceBundleFromDirectory(
            SSOToken token, String rbName, String locale) throws SSOException {
        // Since we did not find the ResourceBundle
        // in the local file system, we look it up in the directory.
        if (debug.messageEnabled()) {
            debug.message("ISResourceBundle.get-> Unable to find RB in"
                    + " local path. looking in the directory: " + rbName);
        }
        String language = null;
        try {
            ServiceConfigManager scm = new ServiceConfigManager(token,
                    LOCALE_SERVICE, VERSION);
            ServiceConfig globalConfig = scm.getGlobalConfig(null);
            if (globalConfig == null) {
                // throw MissingResourceException
                throw new MissingResourceException("Unable to find "
                        + "LocaleService",
                        "com.sun.identity.common.ISResourceBundle", "all");
            }
            // Check to see if rb subconfig exists
            ServiceConfig rbConfig = globalConfig.getSubConfig(rbName);
            if (rbConfig == null) {
                throw new MissingResourceException("Unable to find "
                        + "ResourceBundle: " + rbName,
                        "com.sun.identity.common.ISResourceBundle", "all");
            }
            if (locale == null) {
                // Get the default set of key-values

                Map attrs = rbConfig.getAttributes();
                return new ISResourceBundle(attrs);
            } else {
                StringTokenizer tzer = new StringTokenizer(locale, "_");
                language = tzer.nextToken();
            }

            ServiceConfig localeConfig = rbConfig.getSubConfig(locale);
            if (localeConfig == null) {
                // look for language specific RB
                localeConfig = rbConfig.getSubConfig(language);
            }
            if (localeConfig == null) {
                // look for generic RB
                Map attrs = rbConfig.getAttributes();
                return new ISResourceBundle(attrs);
            } else {
                Map attrs = localeConfig.getAttributes();
                return new ISResourceBundle(attrs);
            }

        } catch (SMSException se) {
            // log a debug exceptipn
            throw new MissingResourceException(_bundle
                    .getString("isResourceBundleMsg0")
                    + rbName, _bundle.getString("isResourceBundleMsg2"),
                    _bundle.getString("isResourceBundleMsg3"));
        }
    }

    /*
     * Converts \\uxxxx to unicode chars
     */
    private String convertUnicode(String inputStr) {
        char c;
        int len = inputStr.length();
        StringBuffer result = new StringBuffer(len);

        for (int x = 0; x < len;) {
            c = inputStr.charAt(x++);
            if (c == '\\' && x < len) {
                c = inputStr.charAt(x++);
                if (c == 'u') {
                    // Read the xxxx
                    int value = 0;
                    if (x + 4 <= len) {
                        for (int i = 0; i < 4; i++) {
                            c = inputStr.charAt(x++);
                            switch (c) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + c - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + c - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + c - 'A';
                                break;
                            default:
                                throw new IllegalArgumentException(
                                        "Malformed \\uxxxx encoding.");
                            }
                        }
                        result.append((char) value);
                    } else {
                        throw new IllegalArgumentException(
                                "Malformed \\uxxxx encoding.\\u should be" +
                                " followed by atleast four hex digits ");
                    }

                } else {
                    // Process the escape character
                    if (c == 't') {
                        c = '\t';
                    } else if (c == 'r') {
                        c = '\r';
                    } else if (c == 'n') {
                        c = '\n';
                    } else if (c == 'f') {
                        c = '\f';
                    } else {
                        // illegal escape character
                        // do not process it.append as it is
                        result.append('\\');
                    }
                    result.append(c);
                }
            } else
                result.append(c);
        }
        return result.toString();
    }
}
