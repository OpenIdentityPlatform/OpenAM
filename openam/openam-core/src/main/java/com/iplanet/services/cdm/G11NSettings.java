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
 * $Id: G11NSettings.java,v 1.8 2008/08/27 22:05:40 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.iplanet.services.cdm;

import com.iplanet.am.util.QCharset;
import com.iplanet.services.cdm.clientschema.AMClientCapData;
import com.iplanet.services.cdm.clientschema.AMClientCapException;
import com.iplanet.services.cdm.clientschema.AMClientDataListener;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * G11NSettings service identifies the list of valid charsets to be used for any
 * given locale.It is a helper class to identify suitable charset to be used for
 * a client. Client will provide supported charsets using CcppAccept-Charset
 * attribute and this class has methods such as getCharset() to parse the
 * parameter and locate a charset suitable for a given client for a locale
 */
public class G11NSettings implements ServiceListener, ICDMConstants,
        AMClientDataListener {

    private Map rawServiceData = null;

    private static G11NSettings currInstance = null;

    private static Debug debug = null;

    private ServiceSchemaManager serviceSchemaManager = null;

    // These cache entries are used to store service entries
    private Map localeCharset = new TreeMap();

    private Map charsetAlias = new TreeMap();

    /*
     * This variable is to cache charset to be used for a client charset lookup
     * mechanism is computation intensive and needs to be cached. We will cache
     * the locale->charset mapping table with client type as index
     */
    private Map charsetCache = new HashMap();

    //
    // Instances for Client Schema API
    //
    private static AMClientCapData intCapInstance = null;

    private static AMClientCapData extCapInstance = null;

    static {
        debug = Debug.getInstance("amClientDetection");
        currInstance = new G11NSettings();
    }

    /**
     * @return unique instance of G11NSettings Object
     * 
     * Gets singletion instance of G11NSettings Reads the G11NSettings Service
     * and find out charsets applicable for a given locale.
     */
    public static G11NSettings getInstance() {
        return currInstance;
    }

    /**
     * @return preferred charset to be used for the locale It uses fall back
     *         mechanism by dropping variant, country to find suitable charset.
     */
    public String getDefaultCharsetForLocale(Locale loc) {
        List result = getCharsetForLocale(loc.toString());
        if (result != null && !result.isEmpty()) {
            return result.iterator().next().toString();
        }
        String lang = loc.getLanguage();
        String country = loc.getCountry();

        if (country != null && country.length() > 0) {
            result = getCharsetForLocale(lang + "_" + country);
            if (result != null && !result.isEmpty()) {
                return result.iterator().next().toString();
            }
        }

        if (lang != null && lang.length() > 0) {
            result = getCharsetForLocale(lang);
            if (result != null && !result.isEmpty()) {
                return result.iterator().next().toString();
            }
        }
        return CDM_DEFAULT_CHARSET;
    }

    /**
     * @return list of charsets supported by a locale
     * @param loc
     *            locale such as en, en_US
     */
    public List getCharsetForLocale(String loc) {
        if (debug.messageEnabled()) {
            debug.message("G11NSettings::Getcharsetforlocale" + loc);
            debug.message("returns " + localeCharset.get(loc));
        }
        return (List) localeCharset.get(loc);
    }

    /**
     * @param mimeCharset
     * @return a Map which can provide equivalent charset names Currently,
     *        OpenSSO uses only Java name however application Can
     *        configure to have different names for platform level mapping etc.
     */
    public Map getCharsetAliasTable(String mimeCharset) {
        return (Map) charsetAlias.get(mimeCharset);
    }

    /**
     * @param mimeCharset
     * @return charset name used in Java. If there is no such mapping, same
     *         mimecharset is returned
     */
    public String getJavaCharset(String mimeCharset) {
        Map charMap = (Map) charsetAlias.get(mimeCharset);
        if (charMap == null) {
            return mimeCharset;
        }
        String val = (String) charMap.get(JAVA_CHARSET_NAME);
        if (val == null || val.length() == 0) {
            if (debug.messageEnabled()) {
                debug.message("Unable to find java charset for " + mimeCharset);
            }
            return mimeCharset;
        }
        if (debug.messageEnabled()) {
            debug.message("javacharset(" + mimeCharset + ")=" + val);
        }
        return val;
    }

    /* Callback function to track changes to G11NSettings schema */
    public void schemaChanged(String serviceName, String version) {
        if (debug.messageEnabled()) {
            debug.message("Schema change serviceName = " + serviceName
                    + "\tversion = " + version + "\tinitializing ...");
        }

        if (serviceName.equals(G11N_SETTINGS_SERVICE_NAME)) {
            localeCharset.clear();
            charsetAlias.clear();
            charsetCache.clear();
            initializeService();
        }
        if (debug.messageEnabled()) {
            debug.message("G11NSettings init  complete");
        }

    }

    /* Callback function to track changes to G11NSettings Schema */
    public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int type) {
        // Ignore this event

    }

    /* Callback function to track changes to G11NSettings Schema */
    public void organizationConfigChanged(String serviceName, String version,
            String orgName, String groupName, String serviceComponent, int type)
    {
        // Ignore this event
    }

    /**
     * @return string Charset to be used for the given locale
     * @param clientType
     *            Client type identified by CDM module
     * @param loc
     *            Locale such as en_US This method computes the
     *            characterset to be used for any given clientType It uses
     *            Ccpp-Accept-Charset of the ClientType and picks appropriate
     *            charset for any given locale. CcppAccept-Charset list may have
     *            Q factor to weigh the client preference of the charset
     */
    public String getCharset(String clientType, Locale loc)
            throws ClientException {
        Client client = Client.getInstance(clientType);
        String result = fetchCharsetFromCache(client, loc);
        if (result != null && result.length() > 0) {
            return result;
        }
        result = fetchCharset(client, loc);
        Map cCache = (Map) charsetCache.get(client.getClientType());
        if (cCache == null) {
            cCache = new HashMap(3);
            charsetCache.put(clientType, cCache);
        }
        if (debug.messageEnabled()) {
            debug.message("getCharset() for clientType = " + clientType
                    + "\tlocale=" + loc + "\tcharset = " + result);
        }
        cCache.put(loc, result);
        return result;
    }

    //
    // AMClientDataListener method for client change notification
    //
    public void clientChanged(String clientType, int dbType, int opType) {
        if (debug.messageEnabled()) {
            debug.message("G11nSettings::clientChanged() NOTIFICATION: "
                    + "clientType = " + clientType + " :DB = " + dbType
                    + " : Op = " + opType);
        }

        if (clientType == null || clientType.length() == 0) {
            return;
        }

        synchronized (charsetCache) {

            switch (opType) {
            /*
             * When a client is removed,clientType will be lowercase always
             * hence we can't remove the client from hashmap directly we have to
             * compare each entry with equalIgnoreCase and remove them
             */
            case AMClientCapData.REMOVED:
                Iterator it = charsetCache.keySet().iterator();
                while (it.hasNext()) {
                    String ctype = (String) it.next();
                    if (ctype.equalsIgnoreCase(clientType)) {
                        it.remove();
                        break;
                    }
                }
                return;
            /*
             * Since it is new client, it will not be present in the cache at
             * all and hence we can ignore this event
             */
            case AMClientCapData.ADDED:
                return;

            /* Remove the entry if it is present in the cache */
            case AMClientCapData.MODIFIED:
                charsetCache.remove(clientType);
                return;
            default:
                debug
                        .error("ClientSchema modification unknown opType"
                                + opType);
            }
        }
        return;
    }

    /**
     * Constructor is made private to make it singleton
     */
    private G11NSettings() {
        initializeService();
        if (serviceSchemaManager != null) {
            serviceSchemaManager.addListener(this);
        }

        //TOFIX: Need to check if clientdata service is enabled or not.
        // Check if AMSDK is configured
        if (ServiceManager.isAMSDKEnabled()) {
            try {
                intCapInstance = AMClientCapData.getInternalInstance();
                intCapInstance.addListener(this); // register to internal
                extCapInstance = AMClientCapData.getExternalInstance();
                extCapInstance.addListener(this); // register to external

            } catch (AMClientCapException ce) {
                debug.error("Unable to get an instance Of ClientData in " +
                    "G11Nsettings", ce);
            }
        }
    }

    /**
     * Initialize the service
     */
    private void initializeService() {
        try {
            readServiceConfig();
            Object[] cTypes = ((Set) rawServiceData.get(LOCALE_CHARSET_ATTR))
                    .toArray();
            for (int i = 0; i < cTypes.length; i++) {
                createLocaleEntry((String) cTypes[i]);
            }
            cTypes = ((Set) rawServiceData.get(CHARSET_ALIAS_ATTR)).toArray();
            for (int i = 0; i < cTypes.length; i++) {
                createCharsetAliasEntry((String) cTypes[i]);
            }
        } catch (SSOException ex) {
            debug.error("Unable to get internal SSOToken for locale attribute ",
                            ex);
        } catch (SMSException ex) {
            debug.error("Unable to get  locale attribute value", ex);
        }

    }

    /**
     * read G11NSettings service config data from SMS
     */
    private void readServiceConfig() throws SSOException, SMSException {
        // Get the internal token to read the values
        if (debug.messageEnabled()) {
            debug.message("G11Settings::ReadServiceConfig");
        }
        SSOToken internalToken = (SSOToken) AccessController
                .doPrivileged(AdminTokenAction.getInstance());
        // read iPlanetAMClientDetection service using SMS API
        serviceSchemaManager = new ServiceSchemaManager(
                G11N_SETTINGS_SERVICE_NAME, internalToken);
        ServiceSchema gsc = serviceSchemaManager.getGlobalSchema();
        rawServiceData = gsc.getAttributeDefaults();
    }

    /*
     * Create charset Alias Entry in a HashMap indexed by mimeCharset in the
     * format mimeName=XXX|javaName=YYY|Win32Name=BBBB
     */
    private void createCharsetAliasEntry(String val) {
        StringTokenizer tok1 = new StringTokenizer(val, "|");
        String mimeCharsetName = null;
        HashMap charsetNameList = new HashMap();

        while (tok1.hasMoreElements()) {
            String element = tok1.nextToken();
            // break on "="
            StringTokenizer pairTok = new StringTokenizer(element, "=");
            if (pairTok.countTokens() == 2) {
                String key = pairTok.nextToken();
                key = key.trim();
                String value = pairTok.nextToken().trim();
                if (key.equalsIgnoreCase("mimeName")) {
                    mimeCharsetName = value.toUpperCase();
                } else {
                    charsetNameList.put(key.toLowerCase(), value.toUpperCase());
                }
            }
        }
        if (mimeCharsetName != null) {
            charsetAlias.put(mimeCharsetName, charsetNameList);
            if (debug.messageEnabled()) {
                debug.message("Building alias name for " + mimeCharsetName);
                Set keys = charsetNameList.keySet();
                Iterator it = keys.iterator();
                while (it.hasNext()) {
                    String x = (String) it.next();
                    debug.message("name = " + x + " value = "
                            + charsetNameList.get(x));
                }
            }
        }
    }

    /**
     * load the allClientData Map with the data read from profile service. The
     * charset values are separated by semi-colon format is
     * locale=ja|charset=UTF-8;SJIS;eucJP charset values are stored in
     * LinkedList
     */
    private void createLocaleEntry(String val) {
        StringTokenizer tok1 = new StringTokenizer(val, "|");
        String localeValue = null;
        LinkedList charsetList = null;
        while (tok1.hasMoreElements()) {
            String element = tok1.nextToken();

            // break on "="
            StringTokenizer pairTok = new StringTokenizer(element, "=");
            if (pairTok.countTokens() == 2) {
                String key = pairTok.nextToken();
                key = key.trim();
                String value = pairTok.nextToken();
                if (key.equalsIgnoreCase("locale")) {
                    localeValue = value.toLowerCase();
                }
                int charsetCount = 0;
                if (key.equalsIgnoreCase("charset")) {
                    charsetList = new LinkedList();
                    StringTokenizer charsetTokenizer = new StringTokenizer(
                            value, ";");
                    while (charsetTokenizer.hasMoreElements()) {
                        String charsetVal = charsetTokenizer.nextToken();
                        charsetList.add(charsetVal);
                        charsetCount++;
                    }
                }
            }
        }
        if (charsetList != null && localeValue != null) {
            localeCharset.put(localeValue, charsetList);
            if (debug.messageEnabled()) {
                debug.message("G11NSettings Locale = " + localeValue
                        + "\tAllowed charset are " + charsetList);
            }
        }
    }

    /**
     * Fetches the charset to be used for any given locale from cache
     * 
     * @param com.iplanet.services.cdm.Client
     *            Client object
     * @param java.util.Locale
     *            loc
     * @return String mimeCharset Name to be used
     */
    private String fetchCharsetFromCache(Client client, Locale loc) {
        Map cCache = (Map) charsetCache.get(client.getClientType());
        String val = null;
        if (cCache != null) {
            val = (String) cCache.get(loc);
        }
        return val;
    }

    /**
     * Fetches the charset to be used for a given locale from G11NSettings
     * 
     * @param Client
     *            client object
     * @param java.util.Locale
     *            loc
     * @return String Updates charsetCache with the value it found. If the value
     *         can't be found fills with default ISO8859-1 Locale names are
     *         CASE-INSENSITIVE
     */
    private synchronized String fetchCharset(Client client, 
            java.util.Locale loc) 
    {

        /* Check if the charset is already computed by another thread */
        String result = null;
        result = fetchCharsetFromCache(client, loc);
        if (result != null && result.length() > 0) {
            return result;
        }
        Set charsets = client.getProperties(CDM_ACCEPT_CHARSET);
        if (charsets == null) {
            // Problem in accessing CDM_ACCEPT_CHARSET , may be 6.0
            result = getCharsetLegacy(client, loc);
            if (result != null && result.length() > 0) {
                /*
                 * Able to find charset attribute or charset_<locale> attribute
                 * which confirms that the schema is Access Manager 6.0 based
                 */
                return result;
            }
        }

        Set sortedCharset = new TreeSet(); // Orderd charset with Q factor
        if (charsets != null) {
            Iterator charsetIterator = charsets.iterator();
            while (charsetIterator.hasNext()) {
                String charsetName = (String) charsetIterator.next();
                /*
                 * Find out the syntax of charset which can be of val;q=xx where
                 * 0 < xx < 1.0
                 */
                if (charsetName.indexOf(";") >= 0) {
                    StringTokenizer tok1 = 
                        new StringTokenizer(charsetName, ";");
                    if (tok1.countTokens() != 2) {
                        debug.error("G11NSettings::fetchcharSet () Unable to"
                                + "parse charset entry " + charsetName);
                        break;
                    }
                    String cname = tok1.nextToken();
                    String qval = tok1.nextToken();
                    StringTokenizer tok2 = new StringTokenizer(qval, "=");
                    if (tok2.countTokens() != 2) {
                        debug.error("G11NSettings::fetchcharSet ()Unable to"
                                + " parse qvalue of charset entry " + qval);
                        break;
                    }
                    String tname = tok2.nextToken();
                    if (tname.length() > 1 || (!tname.equalsIgnoreCase("q"))) {
                        debug.error("G11NSettings::Fetchcharset() Unable to"
                                + "parse  charset entry invalid Q " + tname);
                        break;
                    }
                    String tval = tok2.nextToken();
                    try {
                        float fval = Float.parseFloat(tval);
                        sortedCharset.add(new QCharset(cname, fval));
                    } catch (NumberFormatException ex) {
                        debug.error("G11NSettings::fetchcharSet () unable"
                                + "to parse q factor", ex);
                        break;
                    }
                } else {
                    sortedCharset.add(new QCharset(charsetName));
                }
            }
            if (debug.messageEnabled()) {
                Iterator it = sortedCharset.iterator();
                while (it.hasNext()) {
                    debug.message("G11NSettings.fetchCharset()"
                            + "ccpp-accept-charset order = "
                            + it.next().toString());
                }
            }
        }
        String key;
        String lang = loc.getLanguage();
        String country = loc.getCountry();
        String variant = loc.getVariant();

        key = lang.toLowerCase();
        if (country != null && country.length() != 0) {
            key = key + "_" + country.toLowerCase();
        }
        if (variant != null && variant.length() != 0) {
            key = key + "_" + variant.toLowerCase();
        }

        result = locateCharset(key, sortedCharset);

        if (result != null && result.length() > 0) {
            return result;
        }
        // Drop the variant part of locale
        if (country != null && country.length() != 0) {
            result = locateCharset(lang + "_" + country, sortedCharset);
            if (result != null) {
                if (debug.messageEnabled()) {
                    debug
                            .message("charset located for " + lang + "_"
                                    + country);
                }
                return result;
            }
        }
        // Drop the variant and country part
        if (lang != null && lang.length() != 0) {
            result = locateCharset(lang, sortedCharset);
            if (result != null) {
                if (debug.messageEnabled()) {
                    debug.message("charset located for " + lang);
                }
                return result;
            }
        }
        if (debug.messageEnabled()) {
            debug.message("unable to locate charset for " + loc);
        }
        return CDM_DEFAULT_CHARSET;
    }

    private String locateCharset(String key, Set charsets) {
        if (charsets == null) {
            // Serious Error condition . OK to have empty set even
            // CcppAccept-charset == NULL charsets is empty
            return null;
        }
        String res = null;

        List supCharset = getCharsetForLocale(key);
        if (supCharset == null) {
            return null;
        }

        Iterator it = charsets.iterator();
        while (it.hasNext()) {
            QCharset idc = (QCharset) it.next();
            String id = idc.getName();
            /*
             * '*' is a valid charset name to denote any character set name if
             * browser hints that it can accept any character set name we will
             * pass the first character set name listed in g11nsettings for the
             * given locale
             */
            if (id.equals("*")) {
                res = (String) supCharset.get(0); // get first element
                break;
            } else {
                /* charset names are case insensitive */
                Iterator cit = supCharset.iterator();
                while (cit.hasNext()) {
                    String cname = (String) cit.next();
                    if (cname.equalsIgnoreCase(id)) {
                        res = id;
                        break;
                    }
                }
                if (res != null) {
                    break;
                }
            }
        }
        return res;
    }

    /**
     * Original getCharset method used in Access Manager 6.0. This method is
     * deprecated to make room for Mobile Access requirements. For compatiblity
     * reasons, we will use this method also if ccppAccept-Charset is missing
     */
    private String getCharsetLegacy(Client client, java.util.Locale loc) {
        StringBuffer key = new StringBuffer("charset_");
        int locLen = key.length();
        String result;
        String lang = loc.getLanguage();
        String country = loc.getCountry();
        if (country == null || country.length() == 0) {
            key.append(lang);
        } else {
            key.append(lang).append('_').append(country);
        }

        result = client.getProperty(key.toString());
        if (result != null) {
            return result;
        }

        key.setLength(locLen);

        key.append(lang);
        result = client.getProperty(key.toString());
        if (result != null) {
            return result;
        }

        key.setLength(locLen - 1);// remove last underscore
        result = client.getProperty(key.toString());
        /*
         * result will be NULL if there is no such property charset_<locale> or
         * charset is present
         */
        return result;
    }

}
