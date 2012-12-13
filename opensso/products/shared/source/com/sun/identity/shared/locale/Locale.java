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
 * $Id: Locale.java,v 1.7 2009/07/07 17:32:02 bina Exp $
 *
 */

package com.sun.identity.shared.locale;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

/**
 * This class <code>Locale.java</code> is a utility that provides
 * functionality for applications and services to internationalize their
 * messages.
 * @supported.all.api
 */
public class Locale {
    static BitSet dontEncode;

    static final int caseDiff = ('a' - 'A');

    private static final int LOCALE_STRING_MAX_LEN = 5;

    static java.util.Locale defaultLocale;

    static Debug debug;

    protected static final String USER_PROPERTIES = "amUser";

    protected static final String DATE_SYNTAX = "dateSyntax";

    private static final String normalizedDateString = "yyyy/MM/dd HH:mm:ss";

    private static final SimpleDateFormat normalizedDateFormat;

    private static final String UNDERSCORE = "_";

    private static final String HYPHEN = "-";


    /*
     * The list of characters that are not encoded have been determined by
     * referencing O'Reilly's "HTML: The Definitive Guide" (page 164).
     */

    static {
        // Intialize static variables
        debug = Debug.getInstance("amUtil");

        dontEncode = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            dontEncode.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            dontEncode.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            dontEncode.set(i);
        }
        dontEncode.set(' '); /*
                                 * encoding a space to a + is done in the
                                 * encode() method
                                 */
        dontEncode.set('-');
        dontEncode.set('_');
        dontEncode.set('.');
        dontEncode.set('*');

        String loc = SystemPropertiesManager.get(Constants.AM_LOCALE, "en_US");
        defaultLocale = getLocale(loc);
        normalizedDateFormat = new SimpleDateFormat(normalizedDateString);
    }

    public static void main(String[] args) {
        System.out.println(":" + Locale.getLocale(args[0]) + ":");
        System.out.println(":" + Locale.getLocale(args[0]).getCountry() + ":");
    }

    /**
     * Gets the locale object for the specified localized string format.
     * 
     * @param stringformat
     *            String representation of the locale. Examples:
     *            <code>en_US, en_UK, ja_JP</code>.
     * @return the <code>java.util.locale</code> object.
     */
    public static java.util.Locale getLocale(String stringformat) {
        java.util.Locale locale = java.util.Locale.getDefault();
        if (stringformat == null) {
            return locale;
        }

        StringTokenizer tk = null;
        String lang = "";
        String country = "";
        String variant = "";

        if (stringformat.indexOf(HYPHEN) != -1) {
            tk = new StringTokenizer(stringformat,HYPHEN);
        } else {
            tk = new StringTokenizer(stringformat,UNDERSCORE);
        }

        if (tk != null) {
            if (tk.hasMoreTokens()) {
                lang = tk.nextToken();
            }
            if (tk.hasMoreTokens()) {
                country = tk.nextToken();
            }
            if (tk.hasMoreTokens()) {
                variant = tk.nextToken();
            }
            locale = new java.util.Locale(lang, country, variant);
        } 

        return locale;
    }

    /**
     * Returns locale from accept-language header HTTP accept language header
     * can have more than one language in the header, we honor the first
     * language as locale
     * 
     * @param langstr
     *            Value from Accept-Language header of HTTP
     * @return locale string in this format <code>en_US, fr</code>
     */
    public static String getLocaleStringFromAcceptLangHeader(String langstr) {

        if (langstr == null)
            return null;

        char[] lstr = langstr.toCharArray();
        int leadSpace = 0;
        /*
         * Accept Language Syntax Accept-Language = "Accept-Language" ":" 1#(
         * language-range [ ";" "q" "=" qvalue ] ) language-range = ( ( 1*8ALPHA
         * *("-" 1*8ALPHA ) ) | "*" ) For more info Read RFC 2616 Examples:
         * Accept-Language: da, en-gb;q=0.8, en;q=0.7 Accept-Language: en-gb, en
         * Accept-Language: ja Accept-Language: zh-cn Accept-Language: *
         * 
         * We will use first language as locale. We will not process any
         * further.Netscape,IE will give mostly one language as Accept-Language
         * header. Max length of string is 5 lang-> 2chars , country -> two
         * chars and separator is -
         */

        try {
            while (Character.isWhitespace(lstr[leadSpace]))
                leadSpace++;
            int len = lstr.length;
            if (len > leadSpace + LOCALE_STRING_MAX_LEN)
                len = leadSpace + LOCALE_STRING_MAX_LEN;

            boolean isCountry = false;
            for (int i = leadSpace; i < len; i++) {
                char ch = lstr[i];
                if (ch == '*')
                    return null;
                // "*" can be a valid accept-lang but does
                // give idea about locale, return null and force the caller to
                // use
                // default locale
                if (ch == '-') {
                    lstr[i] = '_'; // We will follow Java mechanism en_US
                    isCountry = true;
                } else if (ch == ';' || ch == ',') {// Language separators used
                                                    // by accept-lang
                    return new String(lstr, leadSpace, i - leadSpace);
                } else if (isCountry) {
                    lstr[i] = Character.toUpperCase(ch);
                }
            }
            return new String(lstr, 0, len);
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    /**
     * Gets locale from accept-language header HTTP accept language header can
     * have more than one language in the header, we honor the first language as
     * locale
     * 
     * @param langStr
     *            Value from Accept-Language header of HTTP
     * @return locale string in this format <code>en_US, fr</code>.
     */
    public static java.util.Locale getLocaleObjFromAcceptLangHeader(
            String langStr) {
        String lstr = getLocaleStringFromAcceptLangHeader(langStr);

        if (lstr == null)
            return null;
        String lang = lstr.substring(0, 2);
        String country = "";
        if (lstr.length() == LOCALE_STRING_MAX_LEN)
            country = lstr.substring(3, 5);
        return new java.util.Locale(lang, country);
    }

    /**
     * Gets the resource bundle corresponding to the specified locale and the
     * localized property file name.
     * 
     * @param bundle
     *            Localized property file name.
     * @param stringformat
     *            String representation of the locale.
     * 
     * @return <code>java.util.ResourceBundle</code> object.
     * 
     */
    public static ResourceBundle getResourceBundle(String bundle,
            String stringformat) {
        return ResourceBundle.getBundle(bundle, getLocale(stringformat));
    }

    protected static ResourceBundle getResourceBundle(String bundle) {
        return getInstallResourceBundle(bundle);
    }

    /**
     * Gets the default install resource bundle for the default locale
     * 
     * @param bundle
     *            Localized property file name
     * @return the install resource bundle object
     */
    public static ResourceBundle getInstallResourceBundle(String bundle) {
        String loc = SystemPropertiesManager.get(Constants.AM_LOCALE, "en_US");
        return ResourceBundle.getBundle(bundle, getLocale(loc));
    }

    /**
     * Gets the default locale
     * 
     * @return the default Locale object
     */
    public static java.util.Locale getDefaultLocale() {
        return defaultLocale;
    }

    /**
     * Formats messages using <code>MessageFormat</code> Class.
     * 
     * @param formatStr
     *            string format template.
     * @param obj1
     *            object to be added to the template.
     * @return formatted message.
     */
    public static String formatMessage(String formatStr, Object obj1) {
        Object arr[] = new Object[1];
        arr[0] = obj1;
        return MessageFormat.format(formatStr, arr);
    }

    /**
     * Formats to format messages using <code>MessageFormat</code> Class.
     * given params to format them with
     * 
     * @param formatStr
     *            string format template.
     * @param objs
     *            objects to be added to the template.
     * @return formatted message.
     */
    public static String formatMessage(String formatStr, Object[] objs) {
        return MessageFormat.format(formatStr, objs);
    }

    /**
     * Returns the Date object from the date string in <code>ISO-8601</code>
     * format. OpenSSO stores date in <code>ISO-8601</code> format
     * <code>yyyy/MM/yy hh:mm</code>
     * 
     * @param dateString
     *            in the format <code>2002/12/31 23:59</code>.
     * @return Date object
     */
    public static Date parseNormalizedDateString(String dateString) {
        if (dateString == null)
            return null;

        ParsePosition pos = new ParsePosition(0);
        Date date = normalizedDateFormat.parse(dateString, pos);
        if (date == null) {
            debug.error("Locale.parseNormalizedDateString: "
                    + "Unable to parse date string");
        }
        if (debug.messageEnabled()) {
            debug.message("Locale.parseNormalizedDateString(" + dateString
                    + ")=" + date);
        }
        return date;

    }

    /**
     * Gets Date object from date string with specified locale.
     * 
     * @param dateString
     *            date string
     * @param locale
     *            Locale object
     * @param dateSyntax
     *            syntax of the date string.
     * 
     * @return Date object returned if <code>dateString</code> matches the
     *         <code> dateSyntax</code>. If the syntax or date string is
     *         empty, or the string does not match the syntax, null will be
     *         returned.
     */
    public static Date parseDateString(String dateString,
            java.util.Locale locale, String dateSyntax) {
        if (debug.messageEnabled()) {
            debug.message("Local.parseDateString(date, locale, syntax)");
            debug.message("date string = " + dateString);
            debug.message("date syntax = " + dateSyntax);
            debug.message("locale = " + locale.toString());
        }
        if ((dateString == null) || (dateString.length() < 1)
                || (dateSyntax == null) || (dateSyntax.length() < 1)) {
            return null;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(dateSyntax);
        sdf.setLenient(false);
        ParsePosition pos = new ParsePosition(0);
        Date date = sdf.parse(dateString, pos);
        if (date == null) {
            debug.warning("Locale.parseDateString: unable to parse the date.");
        }

        return date;
    }

    /**
     * Gets Date object from date string with specified locale. Syntax of date
     * string is defined in amUser_&lt;locale> properties file.
     * 
     * @param dateString
     *            date string
     * @param locale
     *            Locale object
     * 
     * @return Date object. null will be returned if error happens
     */
    public static Date parseDateString(String dateString,
            java.util.Locale locale) {
        ResourceBundle rb = AMResourceBundleCache.getInstance().getResBundle(
                USER_PROPERTIES, locale);

        if (rb == null) {
            debug.error("Locale.parseDateString: Unable to get resource "
                    + "bundle. Locale = " + locale);
            return null;
        }

        String dateSyntax = null;
        try {
            dateSyntax = rb.getString(DATE_SYNTAX);
            dateSyntax.trim();
        } catch (Exception ex) {
            debug.error("Locale.parseDateString: Unable to get " + DATE_SYNTAX
                    + ". Locale " + locale);
            return null;
        }
        return parseDateString(dateString, locale, dateSyntax);
    }

    /**
     * Converts the Date object into <code>ISO-8601</code> format
     * <code>yyyy/MM/dd HH:mm</code> like <code>2002/12/23 20:40</code>.
     * 
     * @param date
     *            to be normalized.
     * @return date in <code>ISO8601</code> format
     *         <code>2002/12/31 11:59</code>.
     */
    public static String getNormalizedDateString(Date date) {
        if (date == null)
            return null;
        return normalizedDateFormat.format(date);
    }

    /**
     * Gets date string from date with specified locale.
     * 
     * @param date
     *            Date object
     * @param locale
     *            Locale object
     * 
     * @return date string. null will be returned if error happens
     */
    public static String getDateString(Date date, java.util.Locale locale) {
        if (date == null) {
            return null;
        }

        ResourceBundle rb = AMResourceBundleCache.getInstance().getResBundle(
                USER_PROPERTIES, locale);
        if (rb == null) {
            debug.error("Locale.getDateString: Unable to get resource "
                    + "bundle. Locale = " + locale);
            return null;
        }

        String dateSyntax = null;
        try {
            dateSyntax = rb.getString(DATE_SYNTAX);
        } catch (Exception ex) {
            debug.error("Locale.getDateString: Unable to get " + DATE_SYNTAX
                    + ". Locale " + locale);
            return null;
        }

        if (debug.messageEnabled()) {
            debug.message("Locale.getDateString: dateSyntax = " + dateSyntax);
        }

        SimpleDateFormat sdf = new SimpleDateFormat(dateSyntax);
        return sdf.format(date);
    }

    /**
     * Converts date string from source locale to destination locale
     * 
     * @param srcDateString
     *            source date string
     * @param srcLocale
     *            source Locale object
     * @param dstLocale
     *            destination Locale object
     * 
     * @return converted date string. null will be returned if error happens
     */
    public static String convertDateString(String srcDateString,
            java.util.Locale srcLocale, java.util.Locale dstLocale) {
        Date date = parseDateString(srcDateString, srcLocale);

        return getDateString(date, dstLocale);
    }

    /**
     * Gets the localized string for the specified key formatted as per passed
     * parameters.
     * 
     * @param rb
     *            resource bundle.
     * @param resource
     *            the specified key.
     * @param params
     *            formatting done as per these parameters.
     * 
     * @return the localized string representation formatted as per passed
     *         parameters.
     */
    public static String getString(ResourceBundle rb, String resource,
            Object[] params) {
        try {
            return MessageFormat.format(rb.getString(resource), params);
        } catch (Exception mre) {
            if (debug.messageEnabled()) {
                debug.message("missing resource: " + resource);
            }
        }
        return resource;
    }

    /**
     * Gets the localized string for the specified key from the specified
     * Resource or from the specified default resource formatted as per provided
     * parameters.
     * 
     * @param rb
     *            resource bundle.
     * @param resource
     *            the specified key.
     * @param defaultRb
     *            Default resource bundle.
     * @param params
     *            formatting done as per these parameters.
     * 
     * @return the localized string representation formatted as per passed
     *         parameters.
     * 
     */
    public static String getString(ResourceBundle rb, String resource,
            ResourceBundle defaultRb, Object[] params) {
        try {
            return MessageFormat.format(rb.getString(resource), params);
        } catch (Exception mre) {
            try {
                if (debug.messageEnabled()) {
                    debug.message("missing resource: " + resource);
                    debug.message("fall back to default resource bundle");
                }
                return MessageFormat.format(defaultRb.getString(resource),
                        params);
            } catch (Exception mrde) {
                if (debug.messageEnabled()) {
                    debug.message("missing resource in default resource bundle:"
                                    + resource);
                }
            }
        }
        return resource;
    }

    /**
     * Gets the localized string for the specified key
     * 
     * @param rb
     *            resource bundle.
     * @param resource
     *            the specified key.
     * @param debug
     *            the debug instance to which the debug messages need to be
     *            printed.
     * 
     * @return the localized string representation
     */
    public static String getString(ResourceBundle rb, String resource,
            Debug debug) {
        try {
            return rb.getString(resource);
        } catch (Exception mre) {
            if (debug.messageEnabled()) {
                debug.message("missing resource: " + resource);
            }
        }
        return resource;
    }

    /**
     * Gets the localized string for the specified key from the specified
     * Resource or from the specified default resource
     * 
     * @param rb
     *            resource bundle.
     * @param resource
     *            the specified key.
     * @param debug
     *            the debug instance to which the debug messages need to be
     *            printed.
     * @param defaultRb
     *            Default resource bundle.
     * 
     * @return the localized string representation
     */
    public static String getString(ResourceBundle rb, String resource,
            Debug debug, ResourceBundle defaultRb) {
        try {
            return rb.getString(resource);
        } catch (Exception mre) {
            try {
                if (debug.messageEnabled()) {
                    debug.message("missing resource: " + resource);
                    debug.message("fall back to default resource bundle");
                }
                return defaultRb.getString(resource);
            } catch (Exception mrde) {
                if (debug.messageEnabled()) {
                    debug.message("missing resource in default resource bundle:"
                                    + resource);
                }
            }
        }
        return resource;
    }

    /**
     * Gets the localized string for the specified key.
     * 
     * @param rb
     *            resource bundle.
     * @param resource
     *            the specified key.
     * @return the localized string representation
     */
    public static String getString(ResourceBundle rb, String resource) {
        try {
            return rb.getString(resource);
        } catch (Exception mre) {
            if (debug.messageEnabled()) {
                debug.message("missing resource: " + resource);
            }
        }
        return resource;
    }

    /**
     * Gets the localized string for the specified key from the specified
     * Resource or from the specified default resource.
     * 
     * @param rb
     *            resource bundle.
     * @param resource
     *            the specified key.
     * @param defaultRb
     *            Default resource bundle.
     * @return the localized string representation
     */
    public static String getString(ResourceBundle rb, String resource,
            ResourceBundle defaultRb) {
        try {
            return rb.getString(resource);
        } catch (Exception mre) {
            try {
                if (debug.messageEnabled()) {
                    debug.message("missing resource: " + resource);
                    debug.message("fall back to default resource bundle");
                }
                return defaultRb.getString(resource);
            } catch (Exception mrde) {
                if (debug.messageEnabled()) {
                    debug.message("missing resource in default resource bundle:"
                                    + resource);
                }
            }
        }
        return resource;
    }

    /**
     * This method is replacement function for <code>URLEncoder</code>
     * Function URL encoder function converts input string into
     * <code>URLEncoded</code> byte stream after converting Unicode string
     * into bytes using native encoding. The <code>URLEncoder</code> does not
     * work for OpenSSO if default encoding is not
     * <code>UTF-8</code>, hence this method was written.
     * 
     * @param input
     *            the input string.
     * @param enc
     *            the encoding format.
     * @return the encoded string.
     * @throws UnsupportedEncodingException
     */
    public static String URLEncodeField(String input, String enc)
            throws UnsupportedEncodingException {
        int inputLen = input.length();

        byte[] byteOut = input.getBytes(enc);
        StringBuffer result = new StringBuffer(inputLen * 4); // approx size
        for (int i = 0; i < byteOut.length; i++) {
            int c = byteOut[i] & 0xff;
            if (dontEncode.get(c)) {
                if (c == ' ') {
                    c = '+';
                }
                result.append((char) c);
            } else {
                result.append('%');
                char ch = Character.forDigit((c >> 4) & 0xF, 16);
                if (('a' <= ch) && (ch <= 'f')) {
                    ch -= caseDiff;
                }
                result.append(ch);
                ch = Character.forDigit(c & 0xF, 16);
                if (('a' <= ch) && (ch <= 'f')) {
                    ch -= caseDiff;
                }
                result.append(ch);
            }

        }
        return result.toString();
    }

    /**
     * This method is replacement function for <code>URLEncoder<code> Function
     * URL encoder function converts input string into <code>URLencoded</code>
     * byte stream after converting Unicode string into bytes using native
     * encoding. The <code>URLEncoder</code> does not work for Sun Java System
     * OpenSSO if default encoding is not <code>UTF-8</code>, hence this
     * method was written.
     * 
     * @param input the input string
     * @param enc the encoding format 
     * @param debug the debug instance to which debug messages need to
     * be printed
     *
     * @return the encoded string
     */
    public static String URLEncodeField(String input, String enc, Debug debug) {
        int inputLen = input.length();

        byte[] byteOut;
        try {
            byteOut = input.getBytes(enc);
        } catch (UnsupportedEncodingException ex) {
            if (debug != null) {
                debug.error("Locale.URLEncodeField: Unsupported Encoding "
                        + enc, ex);
            }
            return input;
        }

        StringBuffer result = new StringBuffer(inputLen * 4); // approx size
        for (int i = 0; i < byteOut.length; i++) {
            int c = byteOut[i] & 0xff;
            if (dontEncode.get(c)) {
                if (c == ' ') {
                    c = '+';
                }
                result.append((char) c);
            } else {
                result.append('%');
                char ch = Character.forDigit((c >> 4) & 0xF, 16);
                if (('a' <= ch) && (ch <= 'f')) {
                    ch -= caseDiff;
                }
                result.append(ch);
                ch = Character.forDigit(c & 0xF, 16);
                if (('a' <= ch) && (ch <= 'f')) {
                    ch -= caseDiff;
                }
                result.append(ch);
            }

        }
        return result.toString();
    }

    static public String URLDecodeField(String strIn, Debug debug) {
        return URLDecodeField(strIn, "UTF-8", debug);
    }

    /*
     * Translate the individual field values in the encoding value Do not use
     * getBytes instead convert unicode into bytes by casting. Using getBytes
     * results in conversion into platform encoding. It appears to work file in
     * C locale because default encoding is 8859-1 but fails in japanese locale.
     * 
     * @param strIn the inputString @param charset character encoding of
     * inputString @param debug the debug instance to which debug messages need
     * to be printed.
     * 
     * @return the decoded string
     */
    static public String URLDecodeField(String strIn, String charset,
            Debug debug) {

        if (strIn == null) {
            return strIn;
        }
        String strOut = null;
        try {
            int len = strIn.length();
            byte buf[] = new byte[len];

            int i = 0;
            int offset = 0;
            char[] carr = strIn.toCharArray();
            while (i < len) {
                byte b = (byte) carr[i];
                switch (b) {
                case '%':
                    int val = 0;
                    if (i + 2 < len) {
                        i++;
                        b = (byte) carr[i];
                        if ('a' <= b && b <= 'f') {
                            b -= caseDiff;
                        }
                        if ('A' <= b && b <= 'F') {
                            val = 10 + b - 'A';
                            val = val << 4;
                        } else if ('0' <= b && b <= '9') {
                            val = (b - '0') << 4;
                        } else {
                            throw new IllegalArgumentException(
                                    "invalid hex char");
                        }
                        i++;
                        b = (byte) carr[i];
                        if ('a' <= b && b <= 'f') {
                            b -= caseDiff;
                        }
                        if ('A' <= b && b <= 'F') {
                            val += 10 + b - 'A';
                        } else if ('0' <= b && b <= '9') {
                            val += b - '0';
                        } else {
                            throw new IllegalArgumentException(
                                    "invalid hex char");
                        }
                        buf[offset++] = (byte) val;
                        i++;
                    } else {
                       buf[offset++] = (byte) carr[i++]; 
                    }
                    break;
                default:
                    buf[offset++] = (byte) carr[i++];
                    break;
                }
            }
            if (charset == null || charset.length() == 0) {
                strOut = new String(buf, 0, offset, "UTF-8");
            } else {
                strOut = new String(buf, 0, offset, charset);
            }
        } catch (Exception ex) {
            debug.error("Locale::decodeField", ex);
            strOut = strIn;
        }
        return strOut;
    }
}
