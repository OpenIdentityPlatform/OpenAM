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
 * $Id: I18n.java,v 1.3 2008/06/25 05:41:41 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.util;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * The <code>I18n</code> class provides methods for applications and services
 * to internationalize their messages.
 * <p>
 * In order for <code>I18n</code> to internationalize messages, it needs to
 * determine the resource bundle name, i.e., properties file name.
 * <code>I18n</code> supports two techniques by which applications and
 * services can specify their I18N resource bundle name. The recommendation is
 * to specify them during service (or application) registration via <b> SMS</b>
 * using a XML file (see <code>com.iplanet.services.ServiceManager
 * </code> and
 * service registration DTD). The XML file could specify the resource bundle
 * name (the attribute defined in the DTD is <code>i18nFileName</code>) and
 * optionally URL of the jar file which contains the property file (the
 * attribute defined in the DTD is <code>resourceBundleURL</code>). If URL
 * for the jar file is not specified it is assumed that the resource bundle is
 * in the <code>
 * CLASSPATH</code>. Using this technique it is possible to
 * customize resource bundle name and URL of the jar file by using SMS APIs,
 * commands (CLI) or GUI. The solution makes internationalization of messages
 * very dynamic and highly customizable.
 * <p>
 * <code>I18n</code> class be instantiated by calling the static
 * <code>getInstance(String serviceName)</code> method. The parameter
 * <code>serviceName</code> specifies the name of the service as mentioned in
 * the XML file at the time of service registration.
 * <p>
 * Alternatively services and application can instantiate <code>I18n</code>
 * object by specifying the resource bundle name (i.e., properties file name).
 * Using this technique it is not possible to customize at runtime either the
 * resource bundle name or the URL of the jar file that contains the properties
 * file name. It is assumed that the properties file is present in
 * <code>CLASSPATH</code>
 *
 * @supported.api
 */
public class I18n {

    /* ASCII ISO */
    public static final String ASCII_CHARSET = "ISO-8859-1";

    /* Static varibale that holds all the I18n objects */
    private static Map i18nMap = new HashMap();

    // private static SSOToken userSSOToken = null;

    /* Instance variable */
    private boolean initialized = false;

    private String serviceName = null;

    private String i18nFile = null;

    private ClassLoader ucl = null;

    private Map resourceBundles = new HashMap();

    /**
     * This constructor takes the name of the component as an argument and it
     * should match with name of the resource bundle
     */
    protected I18n(String serviceName) {
        this.serviceName = serviceName;
    }

    private void initialize() {
        if (initialized)
            return;

        // %%% Hack to get around cyclic dependency on I18n
        // and other components that call I18n
        i18nFile = serviceName;
        initialized = true;
    }

    /**
     * Method to get an instance of I18n object that has been either previously
     * created or to obtain a new instance if it does'nt exist
     * 
     * @param serviceName
     *            name of the service for which messages must be
     *            internationalized
     * @return I18n object
     * @supported.api
     */
    public static I18n getInstance(String serviceName) {
        if (serviceName == null)
            return (null);

        I18n i18nobj = null;
        synchronized (i18nMap) {
            if ((i18nobj = (I18n) i18nMap.get(serviceName)) == null) {
                i18nobj = new I18n(serviceName);
                i18nMap.put(serviceName, i18nobj);
            }
        }
        return (i18nobj);
    }

    /**
     * Method to obtain Locale object given its string representation
     * 
     * @param stringformat
     *            Locale in a string format
     * @return Locale object
     */
    public static java.util.Locale getLocale(String stringformat) {
        if (stringformat == null)
            return java.util.Locale.getDefault();

        StringTokenizer tk = new StringTokenizer(stringformat, "_");
        String lang = "";
        String country = "";
        String variant = "";
        if (tk.hasMoreTokens())
            lang = tk.nextToken();
        if (tk.hasMoreTokens())
            country = tk.nextToken();
        if (tk.hasMoreTokens())
            variant = tk.nextToken();
        return (new java.util.Locale(lang, country, variant));
    }

    /**
     * Returns the resource file name associated with the service
     * 
     * @return Returns the the ResourceBundle name associated with the service
     */
    public String getResBundleName() {
        initialize();
        return i18nFile;
    }

    /* Gets the resource bundle */
    private synchronized ResourceBundle getResourceBundle(String stringformat) {
        ResourceBundle bundle = (ResourceBundle) resourceBundles
                .get(stringformat);
        if (bundle == null) {
            if (ucl != null) {
                bundle = ResourceBundle.getBundle(i18nFile,
                        getLocale(stringformat), ucl);
            } else {
                bundle = ResourceBundle.getBundle(i18nFile,
                        getLocale(stringformat));
            }
            if (initialized)
                resourceBundles.put(stringformat, bundle);
        }
        return (bundle);
    }

    /* Get the default locale stored in config */
    private static String getDefaultLocale() {
        String loc = "en_US";

        /* %%% Get the default locale stored in config - to be implemented */
        return (loc);
    }

    /**
     * Method to obtain internationalized message from the
     * resource bundle given the key and locale.
     * 
     * @param key
     *            key string in the properties file
     * @param locale
     *            locale in a string format
     * @return returns internationalized message for the specified key
     * @supported.api
     */
    public String getString(String key, String locale) {
        initialize();
        if (key == null) {
            return null;
        }
        ResourceBundle bundle = getResourceBundle(locale);
        return (bundle.getString(key));
    }

    /**
     * Method to obtain internationalized message from the
     * resource bundle given the key.
     * 
     * @param key
     *            Key string in the properties file
     * @return Returns value to the specified key
     * @supported.api
     */
    public String getString(String key) {
        initialize();
        if (key == null) {
            return null;
        }
        ResourceBundle bundle = getResourceBundle(getDefaultLocale());
        return (bundle.getString(key));
    }

    /**
     * Method to obtain internationalized message from the
     * resource bundle given the key, locale and parameters.
     * 
     * @param key
     *            key string in the properties file
     * @param locale
     *            locale in a string format
     * @param params
     *            parameters to be applied to the message
     * @return returns internationalized message for the specified key
     * @supported.api
     */
    public String getString(String key, String locale, Object[] params) {
        initialize();
        if (key == null)
            return (null);
        return (MessageFormat.format(getString(key, locale), params));
    }

    /**
     * Method to obtain internationalized message from the
     * resource bundle given the key and parameters.
     * 
     * @param key
     *            Key string in the properties file
     * @param params
     *            parameters to be applied to the message
     * @return Returns value to the specified key
     * @supported.api
     */
    public String getString(String key, Object[] params) {
        initialize();
        if (key == null)
            return (null);
        return (MessageFormat.format(getString(key), params));
    }

    /**
     * Decodes the string into specified charset
     * 
     * @param s
     *            string to be decoded
     * @param charset
     *            character set in which the string to be decoded
     * @return Returns the decoded string
     */
    public static String decodeCharset(String s, String charset) {
        if (s == null) {
            return null;
        }

        try {
            byte buf[] = s.getBytes(ASCII_CHARSET);
            return (new String(buf, 0, buf.length, charset));
        } catch (UnsupportedEncodingException uee) {
            return s;
        }
    }

    /**
     * Checks whether the string is ascii or not
     * 
     * @param s
     *            string to be checked
     * @return true if the string is ascii, otherwise false
     */
    public static boolean isAscii(String s) {
        if (s == null) {
            return true;
        }

        try {
            if (!s.equals(new String(s.getBytes(ASCII_CHARSET), ASCII_CHARSET)))
            {
                return false;
            }
        } catch (java.io.UnsupportedEncodingException uee) {
            return false;
        }
        return true;
    }

    private static String format(MessageFormat mf, Object o) {
        String msg = mf.format(new Object[] { o }, new StringBuffer(), null)
                .toString();
        return msg;
    }

    /**
     * Formats the objects into specified message format.
     * 
     * @param pattern
     *            pattern for which the message to be formatted
     * @param j
     *            Object to be formatted & substituted
     * @param l
     *            locale in a string format
     * @return Returns the formatted message
     */
    public static String format(String pattern, Long j, String l) {
        MessageFormat mf = new MessageFormat("");
        mf.setLocale(getLocale(l));
        mf.applyPattern(pattern);
        String msg = format(mf, j);

        return msg;
    }

    /**
     * Formats the objects into specified message format.
     * 
     * @param pattern
     *            pattern for which the message to be formatted
     * @param i
     *            Integer to be formatted & substituted
     * @param l
     *            locale in a string format
     * @return Returns the formatted message
     */
    public static String format(String pattern, Integer i, String l) {
        MessageFormat mf = new MessageFormat("");
        mf.setLocale(getLocale(l));
        mf.applyPattern(pattern);
        String msg = format(mf, i);

        return msg;
    }

    /**
     * Formats the objects into specified message format
     * 
     * @param pattern
     *            pattern for which the message to be formatted
     * @param d
     *            date
     * @param tz
     *            Timezone
     * @param l
     *            locale in a string format
     * @return Returns the formatted message
     */
    public static String format(String pattern, Date d, TimeZone tz, String l) {

        MessageFormat mf = new MessageFormat("");
        mf.setLocale(getLocale(l));
        mf.applyPattern(pattern);
        ((DateFormat) mf.getFormats()[0]).setTimeZone(tz);

        DateFormat df1 = ((DateFormat) mf.getFormats()[0]);
        if (df1 != null) {
            df1.setTimeZone(tz);
        }

        DateFormat df2 = ((DateFormat) mf.getFormats()[1]);
        if (df2 != null) {
            df2.setTimeZone(tz);
        }

        return format(mf, d);
    }
}
