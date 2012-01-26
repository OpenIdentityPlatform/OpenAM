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
 * $Id: FileLookup.java,v 1.4 2008/06/25 05:53:05 qcheng Exp $
 *
 */

package com.sun.identity.shared.search;

import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * This class provides a generic file path generation utility for
 * data file types, be it template, JSP, or property.
 * 
 * FileLookup should follow the same look up pattern of the Java
 * Resource Bundle Lookup.  Failure to do so results in messages in mixed
 * languages.  With the Java lookup mechanism, if the platform locale is
 * ja_JP and the Resource Bundle locale is zh_CN, the lookup
 * sequence is rb_zh_CN, rb_zh, rb_JA_JP, rb_ja, rb.  We can not fall
 * back to rb if the zh resource bundle is not found.  Hence, we have
 * added more steps to file lookup
 */
public class FileLookup {

    private static Debug debug = Debug.getInstance("amFileLookup");

    private static final String UNDERSCORE = "_";

    private static final String NULL_LOCALE = "nullLocale";

    private static int numberOfPlatformLocales = 0;

    private static String[] platformLocales;

    static String platformLocale = null;
    static {
        platformLocale = java.util.Locale.getDefault().toString();
        numberOfPlatformLocales++; // one for the entire localename
        String frontOfStr = platformLocale;
        int idx = 0;
        while (idx != -1) {
            idx = frontOfStr.lastIndexOf(UNDERSCORE);
            if (idx != -1) {
                frontOfStr = frontOfStr.substring(0, idx);
                numberOfPlatformLocales++; // and one for each variant
            }
        }
        frontOfStr = platformLocale;
        platformLocales = new String[numberOfPlatformLocales];
        platformLocales[0] = platformLocale;
        idx = 0;
        int i = 1;

        while (idx != -1) {
            idx = frontOfStr.lastIndexOf(UNDERSCORE);
            if (idx != -1) {
                String thisPartOfStr = frontOfStr.substring(0, idx);
                platformLocales[i++] = thisPartOfStr;
                frontOfStr = thisPartOfStr;
            }
        }

    }

    /**
     * Returns all possible paths for the specified file in an ordered array.
     * If all parameters specified are non-null, then the order of the
     * filepaths is generated in the following manner:
     *   <type>_<locale>/<component>/<clientPath>/<filename>
     *   <type>_<locale>/<component>/<filename>
     *   <type>_<locale>/<component>/<orgPath>/<clientPath>/<filename>
     *   <type>_<locale>/<component>/<orgPath>/<filename>
     *   <type>_<locale>/<clientPath>/<filename>
     *   <type>_<locale>/<filename>
     *     Repeat pattern above, taking off the ending "_xx" portion
     *     of the locale.  For example, if the original <locale> value
     *     was "jp_JP_WIN", then the above would be <type>_jp_JP_WIN.
     *     The next groups would be <type>_jp_JP and <type>_jp.
     *   
     *     If the platform locale is different from the specified locale,
     *     then the pattern for <locale> is repeated with the platform
     *     locale, with the addition of using no "_<locale>" (i.e.,
     *     the last set consists of "<type>/...").
     *
     *     Additionally, "default" is used as "<type>", and the above
     *     patterns are repeated.
     *
     * 
     * @param type The base filepath to begin the search. If null, "default"
     *        is the start of the filepath.
     * @param locale The locale for the file of interest.
     * @param component The component part of the filepath, if any.
     * @param orgPath The organization part of the filepath, if any.
     * @param clientPath The client type for that part of the filepath
     * @param filename The filename of interest
     * @throws FileLookupException
     * @return <code>File[]</code> of ordered search paths.
     */
    public static File[] getOrderedPaths(
        String type, 
        String locale,
        String component,
        String orgPath,
        String clientPath, 
        String filename
     ) throws FileLookupException {
        if (filename == null) {
            debug.error("FileLookup.getOrderedPaths():Filename was null");
            throw new FileLookupException("filename is null");
        }

        if (debug.messageEnabled()) {
            debug.message("getting ordered paths for =" + type + "|" + locale
                    + "|" + component + "|" + orgPath + "|" + clientPath + "|"
                    + filename);
        }

        String types[] = null;
        int numberOfTypes = 1;
        boolean haveType = false;
        int orderedPathLength;

        /*
         * probably would be easier to add type = "default" automatically, then
         * we wouldn't have to process it separately later... you get "type" (if
         * it's not null or == "default") and "default"
         */
        if (type != null && !type.equals("default")) {
            haveType = true;
            numberOfTypes++;
        }

        types = new String[numberOfTypes];
        if (haveType) {
            types[0] = type;
            types[1] = "default";
            orderedPathLength = 2;
        } else {
            types[0] = "default";
            orderedPathLength = 1;
        }

        /*
         * see how many parts to the locale. one part: e.g., "fr"; or two part:
         * e.g., "fr_FR" three part: e.g., "jp_JP_WIN"
         */
        int numberOfLocales = numberOfPlatformLocales;
        int indexOfLastUS = 0;
        String locales[] = null;
        int i = 1;

        if (debug.messageEnabled()) {
            debug.message("platformLocale is : " + platformLocale);
            debug.message("locale is : " + locale);
        }
        if (locale != null && locale.length() != 0
                && !locale.equals(platformLocale)) {
            // locale specified
            numberOfLocales++; // one for the entire localename
            String frontOfStr = locale;
            while (indexOfLastUS != -1) {
                indexOfLastUS = frontOfStr.lastIndexOf(UNDERSCORE);
                if (indexOfLastUS != -1) {
                    frontOfStr = frontOfStr.substring(0, indexOfLastUS);
                    numberOfLocales++; // and one for each variant
                }
            }

            frontOfStr = locale;

            // add one for dummy null locale
            locales = new String[numberOfLocales + 1];
            locales[0] = locale;
            indexOfLastUS = 0;

            while (indexOfLastUS != -1) {
                indexOfLastUS = frontOfStr.lastIndexOf(UNDERSCORE);
                if (indexOfLastUS != -1) {
                    String thisPartOfStr = frontOfStr.substring(0,
                            indexOfLastUS);
                    locales[i++] = thisPartOfStr;
                    frontOfStr = thisPartOfStr;
                }
            }
        } else {
            i = 0;
            locales = new String[numberOfLocales + 1];
        }

        // add platform locale to the locale array
        for (int j = 0; j < numberOfPlatformLocales; j++) {
            locales[i++] = platformLocales[j];
        }
        locales[i++] = NULL_LOCALE;
        numberOfLocales++;

        if (numberOfLocales > 1) {
            orderedPathLength *= numberOfLocales;
        }

        boolean haveClientPath = false;
        String[] clientkeys = null;

        if ((clientPath != null) && (clientPath.length() > 0)) {
            clientkeys = getClientPathKeys(clientPath);
            orderedPathLength = orderedPathLength * (clientkeys.length + 1);
            haveClientPath = true;
        }

        boolean haveComponent = false;

        if (component != null && component.length() != 0) {
            haveComponent = true;
            orderedPathLength *= 2;
        }

        boolean haveOrgPath = false;
        String[] orgPathKeys = null;

        if ((orgPath != null) && (orgPath.length() > 0)) {
            orgPathKeys = getOrgPathKeys(orgPath);
            orderedPathLength = orderedPathLength * (orgPathKeys.length + 1);
            haveOrgPath = true;
        }

        File[] orderedPaths = new File[orderedPathLength];

        int numberOfOrderedPaths = 0;
        for (i = 0; i < numberOfTypes; i++) {
            String currentFilePath = types[i];
            for (int j = 0; j < numberOfLocales; j++) {
                String thisFilePath = null;
                if (locales != null) {
                    if (locales[j].equals(NULL_LOCALE)) {
                        thisFilePath = currentFilePath;
                    } else {
                        thisFilePath = currentFilePath
                                + (UNDERSCORE + locales[j]);
                    }
                }

                if (haveComponent) {
                    thisFilePath += (Constants.FILE_SEPARATOR + component);

                    if (haveClientPath) {
                        int len = clientkeys.length;
                        while (len > 0) {
                            String filePath = thisFilePath
                                    + buildClientPath(clientkeys, len);

                            orderedPaths[numberOfOrderedPaths++] = new File(
                                    filePath, filename);
                            len--;
                        }
                    }
                    /*
                     * no clientPath specified thisFilePath =
                     * path_locale/component
                     */
                    orderedPaths[numberOfOrderedPaths++] = new File(
                            thisFilePath, filename);
                }

                /*
                 * if orgPath is not null do the following this is used by auth
                 */
                String filePath = null;
                String oPath = null;
                if (haveOrgPath) {
                    int orgKeys = orgPathKeys.length;
                    int z = 0;
                    while (orgKeys > 0) {
                        String iPath = orgPathKeys[z++];
                        oPath = thisFilePath + Constants.FILE_SEPARATOR + iPath;

                        if (haveClientPath) {
                            int len = clientkeys.length;
                            while (len > 0) {
                                filePath = getClientFilePath(oPath, clientkeys,
                                        len);

                                orderedPaths[numberOfOrderedPaths++] = new File(
                                        filePath, filename);
                                len--;
                            }
                        }
                        orderedPaths[numberOfOrderedPaths++] = new File(oPath,
                                filename);

                        orgKeys--;
                    }
                }

                /*
                 * have a component, but don't include it. use the clientPath,
                 * if specified
                 */
                if (haveClientPath) {
                    int len = clientkeys.length;
                    while (len > 0) {
                        if (locales[j].equals(NULL_LOCALE)) {
                            thisFilePath = currentFilePath;
                        } else {
                            thisFilePath = currentFilePath + UNDERSCORE
                                    + locales[j];
                        }
                        filePath = thisFilePath
                                + buildClientPath(clientkeys, len);
                        orderedPaths[numberOfOrderedPaths++] = new File(
                                filePath, filename);
                        len--;
                    }
                }

                // no component, no orgPath ,no clientkeys
                if (!locales[j].equals(NULL_LOCALE)) {
                    thisFilePath = currentFilePath + UNDERSCORE + locales[j];
                } else {
                    thisFilePath = currentFilePath;
                }

                orderedPaths[numberOfOrderedPaths++] = new File(thisFilePath,
                        filename);

            } // end for (int j = 0; j < numberOfLocales; j++)
        } // end for (int i = 0; i < numberOfTypes; i++)

        return orderedPaths;
    }

    private static Map fileTable = new HashMap();

    /**
     * Return the first existing file in the ordered search paths.
     * 
     * @param type An arbitrary profile-stored string
     * @param locale The locale for the file of interest.
     * @param component The component part of the filepath, if any.
     * @param clientPath The client type for that part of the filepath
     * @param filename The filename of interest
     * @param templateDir
     * @param enableCache
     * @return first existing file in the ordered search paths.
     * @throws FileLookupException
     */
    public static File getFirstExisting(
        String type,
        String locale,
        String component,
        String clientPath,
        String filename,
        String templateDir,
        boolean enableCache
    ) throws FileLookupException {
        return getFirstExisting(type, locale, component, null, clientPath,
            filename, templateDir, enableCache);
    }

    /**
     * Returns the first existing file in the ordered search paths.
     * 
     * @param type The base filepath to begin the search.
     * @param locale The locale for the file of interest.
     * @param component The component part of the filepath, if any.
     * @param clientPath The client type for that part of the filepath.
     * @param orgFilePath The organization part of the filepath.
     * @param filename The filename of interest
     * @param templateDir The base template filepath to prepend.
     * @param enableCache True if the File object is to be cached.
     * @return first existing file in the ordered search paths.
     * @throws FileLookupException
     */
    public static File getFirstExisting(
        String type,
        String locale,
        String component,
        String orgFilePath,
        String clientPath,
        String filename,
        String templateDir,
        boolean enableCache
    ) throws FileLookupException {
        String tempFile = type + ":" + locale + ":" + component + ":"
                + orgFilePath + ":" + clientPath + ":" + filename + ":"
                + templateDir;
        if (debug.messageEnabled()) {
            debug.message("Check file=" + tempFile);
        }
        if (enableCache) {
            File temp0 = (File) fileTable.get(tempFile);
            if (temp0 != null) {
                if (debug.messageEnabled()) {
                    debug.message("Found existing file=" + tempFile);
                }
                return temp0;
            }
        }
        File[] orderedPaths = getOrderedPaths(type, locale, component,
                orgFilePath, clientPath, filename);
        if (debug.messageEnabled()) {
            debug.message("getFirstExisting:orderedPaths.length = "
                    + orderedPaths.length);
        }

        for (int i = 0; i < orderedPaths.length; i++) {
            if (debug.messageEnabled()) {
                debug.message("orderedPath[" + i + "]="
                        + orderedPaths[i].toString());
            }
            File file = new File(templateDir, orderedPaths[i].toString());
            if (file.exists()) {
                if (debug.messageEnabled()) {
                    debug.message("returning file=" + file.toString());
                }
                if (enableCache) {
                    synchronized (fileTable) {
                        File temp1 = (File)fileTable.get(tempFile);
                        if (temp1 == null) {
                            fileTable.put(tempFile, file);
                        } else {
                            file = temp1;
                        }
                    }
                }
                return file;
            }
        }
        return null;
    }

    /**
     * Returns the first existing url found on the remote server. The search is
     * done based on the users locale. If no document is found in the users
     * locale the default locale will be searched.
     * 
     * @param server Server where document is located
     * @param type The base filepath to begin the search.
     * @param locale The locale for the file of interest.
     * @param filename The filename of interest.
     * @return url of document, or null if no document was found.
     * @throws FileLookupException
     */
    public static String getFirstExistingRemote(
        String server,
        String type,
        String locale,
        String filename
    ) throws FileLookupException {
        debug.message("FileLookup:getFirstExistingRemote");

        File[] orderedPaths = getOrderedPaths(type, locale, null, null, null,
                filename);

        for (int i = 0; i < orderedPaths.length; i++) {
            String tmp = orderedPaths[i].toString();
            // make sure the path starts with a '/'
            if (tmp.charAt(0) != '/') {
                tmp = "/" + tmp;
            }
            String document = server + tmp;
            if (debug.messageEnabled()) {
                debug.message("path " + i + ") " + document);
            }
            int responseCode = -1;
            try {
                URL handle = new URL(document);
                HttpURLConnection connection = 
                    HttpURLConnectionManager.getConnection(handle);
                responseCode = connection.getResponseCode();
                connection.disconnect();
            } catch (MalformedURLException m) {
                debug.error("malformed url excption", m);
            } catch (IOException ioerror) {
                debug.error("ioexcecption opening the connection", ioerror);
            }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                if (debug.messageEnabled()) {
                    debug.message("returning remote file = " + document);
                }
                return document;
            }
        }
        if (debug.messageEnabled()) {
            debug.message("could not locate " + filename + " on " + server
                    + "; returning null");
        }
        return null;
    }

    /*
     * Build client path based on the client keys.
     */
    private static String buildClientPath(String[] keys, int len) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < len; i++) {
            sb.append(Constants.FILE_SEPARATOR).append(keys[i]);
        }
        return sb.toString();
    }

    /*
     * Tokenize clientPath on "/".
     */
    private static String[] getClientPathKeys(String cp) {
        StringTokenizer st = new StringTokenizer(cp, Constants.FILE_SEPARATOR);
        int numTokens = st.countTokens();
        String[] keyArray = new String[numTokens];
        for (int i = 0; i < numTokens; i++) {
            keyArray[i] = st.nextToken();
        }
        return keyArray;
    }

    private static String[] getOrgPathKeys(String orgPath) {
        StringTokenizer st = new StringTokenizer(
            orgPath, Constants.FILE_SEPARATOR);
        int numTokens = st.countTokens();
        String[] keyArray = new String[numTokens];
        for (int i = 0; i < numTokens; i++) {
            keyArray[i] = orgPath;
            int k = orgPath.lastIndexOf(Constants.FILE_SEPARATOR);
            if (k != -1) {
                orgPath = orgPath.substring(0, k);
            }
        }
        return keyArray;
    }

    static String getClientFilePath(String currentFilePath,
            String[] clientkeys, int len) {
        String filePath = currentFilePath + buildClientPath(clientkeys, len);
        return filePath;
    }
}
