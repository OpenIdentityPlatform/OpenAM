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
 * $Id: ServicesDefaultValues.java,v 1.38 2009/01/28 05:35:02 ww203982 Exp $
 *
 * Portions Copyrighted 2013-2016 ForgeRock AS.
 */

package com.sun.identity.setup;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.utils.ValidateIPaddress;
import org.forgerock.opendj.ldap.DN;

import com.iplanet.am.util.SecureRandomManager;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.Crypt;
import com.sun.identity.common.DNUtils;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.Hash;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.SMSSchema;

/**
 * This class holds the default values of service schema.
 */
public class ServicesDefaultValues {
    public static final String RANDOM_SECURE = "@128_BIT_RANDOM_SECURE@";
    public static final String RANDOM_SECURE_256 = "@256_BIT_RANDOM_SECURE@";

    private static ServicesDefaultValues instance = new ServicesDefaultValues();
    private static Set preappendSlash = new HashSet();
    private static Set trimSlash = new HashSet();
    private static SecureRandom secureRandom;

    private Map defValues = new HashMap();

    static {
        preappendSlash.add(SetupConstants.CONFIG_VAR_PRODUCT_NAME);
        preappendSlash.add(SetupConstants.CONFIG_VAR_OLD_CONSOLE_URI);
        preappendSlash.add(SetupConstants.CONFIG_VAR_CONSOLE_URI);
        preappendSlash.add(SetupConstants.CONFIG_VAR_SERVER_URI);
        trimSlash.add(SetupConstants.CONFIG_VAR_CONSOLE_URI);
        trimSlash.add(SetupConstants.CONFIG_VAR_SERVER_URI);
    }

    private ServicesDefaultValues() {
        ResourceBundle bundle = ResourceBundle.getBundle(
                "serviceDefaultValues");
        Enumeration e = bundle.getKeys();
        while (e.hasMoreElements()) {
            String key = (String)e.nextElement();
            defValues.put(key, bundle.getString(key));
        }

        try {
            secureRandom = SecureRandomManager.getSecureRandom();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialise secure random");
        }
    }

    /**
     * This method validates the form fields and populates the
     * map with valid values.
     *
     * @param request is the Servlet Request.
     */
    public static void setServiceConfigValues(
        IHttpServletRequest request
    ) {
        Locale locale = (Locale)request.getLocale();
        Map<String, Object> map = instance.defValues;
        map.putAll(request.getParameterMap());

        String base = (String)map.get(
            SetupConstants.CONFIG_VAR_BASE_DIR);
        base = base.replace('\\', '/');
        map.put(SetupConstants.CONFIG_VAR_BASE_DIR, base);
        map.put(SetupConstants.USER_HOME, System.getProperty("user.home", ""));

        if (!isEncryptionKeyValid()){
            throw new ConfiguratorException("configurator.encryptkey",
                null, locale);
        }

        // this set the encryption password for crypt class.
        // otherwises password in serverconfig.xml will be incorrect
        String ekey = ((String)map.get(
            SetupConstants.CONFIG_VAR_ENCRYPTION_KEY));
        SystemProperties.initializeProperties("am.encryption.pwd", ekey);

        validatePassword(locale);
        if (!isServiceURLValid()) {
            throw new ConfiguratorException("configurator.invalidhostname", null, locale);
        }

        String cookieDomain = (String) map.get(SetupConstants.CONFIG_VAR_COOKIE_DOMAIN);
        if (!isCookieDomainValid(cookieDomain)) {
            throw new ConfiguratorException("configurator.invalidcookiedomain", null, locale);
        }

        setDeployURI(request.getContextPath(), map);

        String hostname = (String)map.get(
            SetupConstants.CONFIG_VAR_SERVER_HOST);
        map.put(SetupConstants.CONFIG_VAR_COOKIE_DOMAIN, getCookieDomain(cookieDomain, hostname));
        setPlatformLocale();

        String dbOption = (String)map.get(SetupConstants.CONFIG_VAR_DATA_STORE);
        boolean embedded = dbOption.equals(SetupConstants.SMS_EMBED_DATASTORE);

        AMSetupDSConfig dsConfig = AMSetupDSConfig.getInstance();
        dsConfig.setDSValues();

        if (!embedded) { //Sun DS as SM datastore
            String sslEnabled = (String) map.get(
                SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_SSL);
            boolean ssl = (sslEnabled != null) && sslEnabled.equals("SSL");
            if (!dsConfig.isDServerUp(ssl)) {
                dsConfig = null;
                throw new ConfiguratorException(
                    "configurator.dsconnnectfailure", null, locale);
            }
            if ((!LDAPUtils.isDN((String) map.get(
                    SetupConstants.CONFIG_VAR_ROOT_SUFFIX))) ||
                (!dsConfig.connectDSwithDN(ssl))) {
                dsConfig = null;
                throw new ConfiguratorException("configurator.invalidsuffix",
                    null, locale);
            }

            map.put(SetupConstants.DIT_LOADED, dsConfig.isDITLoaded(ssl));
        }

        // Enable data store notifications and switch persistent search on for sms.
        // This is now done by default for embedded data stores as well as external
        // to ensure consistent behaviour for refreshing data store cache.
        map.put(SetupConstants.DATASTORE_NOTIFICATION, "true");
        map.put(SetupConstants.DISABLE_PERSISTENT_SEARCH, "aci,um");

        Map userRepo = (Map)map.get(SetupConstants.USER_STORE);
        String umRootSuffix = null;
        boolean bUseExtUMDS = (userRepo != null) && !userRepo.isEmpty();
        if (bUseExtUMDS) {
            map.put(SetupConstants.UM_DS_DIRMGRDN,
                UserIdRepo.getBindDN(userRepo));
            map.put(SetupConstants.UM_DS_DIRMGRPASSWD,
                UserIdRepo.getBindPassword(userRepo));
            map.put(SetupConstants.UM_DIRECTORY_SERVER,
                UserIdRepo.getHost(userRepo));
            map.put(SetupConstants.UM_DIRECTORY_PORT,
                UserIdRepo.getPort(userRepo));
            String s = (String) userRepo.get(SetupConstants.USER_STORE_SSL);
            final String isSecure = ((s != null) && s.equals("SSL")) ? "true" : "false";
            map.put(SetupConstants.UM_SSL, isSecure);
            if (Boolean.parseBoolean(isSecure)) {
                map.put(SetupConstants.LDAP_CONNECTION_MODE_TAG, SetupConstants.LDAP_CONNECTION_MODE_LDAPS);
            } else {
                map.put(SetupConstants.LDAP_CONNECTION_MODE_TAG, SetupConstants.LDAP_CONNECTION_MODE_LDAP);
            }
            umRootSuffix =(String)userRepo.get(
                SetupConstants.USER_STORE_ROOT_SUFFIX);
        } else {
            map.put(SetupConstants.UM_DS_DIRMGRDN,
                map.get(SetupConstants.CONFIG_VAR_DS_MGR_DN));
            map.put(SetupConstants.UM_DS_DIRMGRPASSWD,
                map.get(SetupConstants.CONFIG_VAR_DS_MGR_PWD));
            map.put(SetupConstants.UM_DIRECTORY_SERVER,
                map.get(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_HOST));
            map.put(SetupConstants.UM_DIRECTORY_PORT,
                map.get(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_PORT));
            map.put(SetupConstants.UM_SSL, "false");
            map.put(SetupConstants.LDAP_CONNECTION_MODE_TAG, SetupConstants.LDAP_CONNECTION_MODE_LDAP);
            umRootSuffix = (String)map.get(
                SetupConstants.CONFIG_VAR_ROOT_SUFFIX);
        }
        umRootSuffix = umRootSuffix.trim();
        String normalizedDN = DN.valueOf(umRootSuffix).toString();
        String escapedDN = SMSSchema.escapeSpecialCharacters(normalizedDN);
        map.put(SetupConstants.UM_NORMALIZED_ORGBASE, escapedDN);
    }

    /**
     * Set the platform locale.
     */
    private static void setPlatformLocale() {
        Map map = instance.defValues;
        String locale = (String)map.get(
            SetupConstants.CONFIG_VAR_PLATFORM_LOCALE);
        if (locale == null) {
            map.put(SetupConstants.CONFIG_VAR_PLATFORM_LOCALE,
                SetupConstants.DEFAULT_PLATFORM_LOCALE);
        }
    }

   /**
     * Validates serverURL.
     *
     * @return <code>true</code> if service URL is valid.
     */
    private static boolean isServiceURLValid() {
        String protocol = "http";
        String port = "80";
        String hostName;
        Map map = instance.defValues;
        String hostURL = (String)map.get(SetupConstants.CONFIG_VAR_SERVER_URL);
        boolean valid = (hostURL != null) && (hostURL.length() > 0);
        try {
            if (valid) {
                if ((hostURL.indexOf("http", 0) == -1) &&
                    (hostURL.indexOf("https", 0) == -1)) {
                    int idx = hostURL.lastIndexOf(":");
                    if ((idx != -1)) {
                        port = hostURL.substring(idx + 1);
                        hostName = hostURL.substring(0, idx);
                    } else {
                        hostName = hostURL;
                    }
                    if (port.equals("443")) {
                        protocol = "https";
                    }
                } else {
                    URL serverURL = new URL(hostURL);
                    int intPort = serverURL.getPort();
                    protocol = serverURL.getProtocol();
                    if (intPort < 0) {
                        if (protocol.equalsIgnoreCase("https")) {
                            port = "443";
                        }
                    } else {
                        port = Integer.toString(intPort);
                    }
                    hostName = serverURL.getHost();
                }
                if (StringUtils.isNotEmpty(hostName)) {
                    map.put(SetupConstants.CONFIG_VAR_SERVER_HOST, hostName);
                    map.put(SetupConstants.CONFIG_VAR_SERVER_PROTO, protocol);
                    map.put(SetupConstants.CONFIG_VAR_SERVER_PORT, port);
                    map.put(SetupConstants.CONFIG_VAR_SERVER_URL,
                        protocol + "://" + hostName + ":" + port);
                } else {
                    valid = false;
                }
            }
        } catch (MalformedURLException mue){
           valid = false;
        }
        return valid;
    }

    /**
     * Validates if cookie Domain is syntactically correct.
     *
     * @param cookieDomain is the user specified cookie domain.
     * @return <code>true</code> if syntax for cookie domain is correct.
     */
    public static boolean isCookieDomainValid(String cookieDomain) {
        return StringUtils.isEmpty(cookieDomain) || !cookieDomain.contains(":");
    }

    /**
     * Returns the cookie Domain based on the hostname. In case the hostname has only one component, or if the hostname
     * ends with a '.', or if the hostname is a valid IP address, host only cookies shall be used.
     *
     * @param cookieDomain Is the user specified cookie domain.
     * @param hostname Is the host for which the cookie domain is set.
     * @return CookieDomain containing the valid cookie domain for the specified hostname.
     */
    private static String getCookieDomain(String cookieDomain, String hostname) {
        int idx = hostname.lastIndexOf(".");
        if (idx == -1 || idx == hostname.length() - 1 || ValidateIPaddress.isValidIP(hostname)) {
            cookieDomain = "";
        }

        return cookieDomain;
    }

    /**
     * Validates the encryption key.
     *
     * @return <code>true</code> if ecryption key is valid.
     */
    private static boolean isEncryptionKeyValid() {
        Map map = instance.defValues;
        String ekey = ((String)map.get(
                SetupConstants.CONFIG_VAR_ENCRYPTION_KEY));
        if (ekey == null) {
            ekey = AMSetupUtils.getRandomString().trim();
            map.put(SetupConstants.CONFIG_VAR_ENCRYPTION_KEY, ekey);
        }
        // in future release need to check if length of greater from 10.
        return ((ekey != null) && (ekey.length() > 0)) ? true : false;
    }

    /**
     * Validates Admin passwords.
     */
    private static void validatePassword(Locale locale) {
        Map map = instance.defValues;
        String adminPwd = ((String)map.get(
            SetupConstants.CONFIG_VAR_ADMIN_PWD)).trim();
        String confirmAdminPwd = ((String)map.get(
            SetupConstants.CONFIG_VAR_CONFIRM_ADMIN_PWD)).trim();
        if (isPasswordValid(adminPwd, confirmAdminPwd, locale)) {
            SystemProperties.initializeProperties(
                SetupConstants.ENC_PWD_PROPERTY, (((String) map.get(
                    SetupConstants.CONFIG_VAR_ENCRYPTION_KEY)).trim()));
            Crypt.reinitialize();
            map.put(SetupConstants.HASH_ADMIN_PWD, (String)Hash.hash(adminPwd));
        }

        String urlAccessAgentPwd = (String)map.get(
            SetupConstants.CONFIG_VAR_AMLDAPUSERPASSWD);

        if (urlAccessAgentPwd != null) {
            urlAccessAgentPwd.trim();

            String urlAccessAgentPwdConfirm = ((String)map.get(
                SetupConstants.CONFIG_VAR_AMLDAPUSERPASSWD_CONFIRM)).trim();
            validateURLAccessAgentPassword(adminPwd, urlAccessAgentPwd,
                urlAccessAgentPwdConfirm, locale);
            map.remove(SetupConstants.CONFIG_VAR_AMLDAPUSERPASSWD_CONFIRM);
        }

        String dbOption = (String)map.get(SetupConstants.CONFIG_VAR_DATA_STORE);
        boolean embedded =
              dbOption.equals(SetupConstants.SMS_EMBED_DATASTORE);
        boolean dbSunDS = false;
        boolean dbMsAD  = false;
        if (embedded) {
            dbSunDS = true;
        } else { // Keep old behavior for now.
            dbSunDS = dbOption.equals(SetupConstants.SMS_DS_DATASTORE);
            dbMsAD  = dbOption.equals(SetupConstants.SMS_AD_DATASTORE);
        }

        if (dbSunDS || dbMsAD) {
            String dsMgrPwd = ((String)map.get(
                SetupConstants.CONFIG_VAR_DS_MGR_PWD)).trim();

            if (embedded) {
                if (dsMgrPwd.length() == 0) {
                    map.put(SetupConstants.CONFIG_VAR_DS_MGR_PWD, adminPwd);
                }
            }
        }

        String dsMgrPwd = ((String)map.get(
            SetupConstants.CONFIG_VAR_DS_MGR_PWD));
        map.put(SetupConstants.ENCRYPTED_SM_DS_PWD,
            (String)Crypt.encrypt(dsMgrPwd));

        String ldapUserPwd = (String)map.get(SetupConstants.LDAP_USER_PWD);
        if (ldapUserPwd != null) {
            ldapUserPwd.trim();
            map.put(SetupConstants.ENCRYPTED_LDAP_USER_PWD,
                (String)Crypt.encrypt(ldapUserPwd));
            map.put(SetupConstants.HASH_LDAP_USER_PWD,
                (String)Hash.hash(ldapUserPwd));
        }

        map.put(SetupConstants.SSHA512_LDAP_USERPWD,
            (String)EmbeddedOpenDS.hash(adminPwd));

        String encryptAdminPwd = Crypt.encrypt(adminPwd);
        map.put(SetupConstants.ENCRYPTED_ADMIN_PWD, encryptAdminPwd);
        map.put(SetupConstants.ENCRYPTED_AD_ADMIN_PWD, encryptAdminPwd);
        map.remove(SetupConstants.CONFIG_VAR_CONFIRM_ADMIN_PWD);
    }

    /*
     * valid: password greater than 8 characters
     * valid: password and confirm passwords match
     *
     * @param pwd  is the Admin password.
     * @param cPwd is the confirm Admin password.
     * @param locale Locale of the HTTP Request.
     * @return <code>true</code> if password is valid.
     */
    private static boolean isPasswordValid(
        String pwd,
        String cPwd,
        Locale locale
    ) {
        if ((pwd != null) && (pwd.length() > 7)) {
            if (!pwd.equals(cPwd)) {
                 throw new ConfiguratorException("configurator.nopasswdmatch",
                     null, locale);
            }
        } else {
             throw new ConfiguratorException("configurator.passwdlength",
                 null, locale);
        }
        return true;
    }

    private static boolean validateURLAccessAgentPassword(
        String amadminPwd,
        String pwd,
        String cPwd,
        Locale locale
    ) {
        if ((pwd != null) && (pwd.length() > 7)) {
            if (!pwd.equals(cPwd)) {
                throw new ConfiguratorException(
                    "configurator.urlaccessagent.passwd.nomatch", null, locale);
            }

            if (amadminPwd.equals(pwd)) {
                throw new ConfiguratorException(
                    "configurator.urlaccessagent.passwd.match.amadmin.pwd",
                    null, locale);
            }
        } else {
            throw new ConfiguratorException("configurator.passwdlength",
                null, locale);
        }
        return true;
    }

    /**
     * Returns the map of default attribute name to its value.
     *
     * @return the map of default attribute name to its value.
     */
    public static Map getDefaultValues() {
        return instance.defValues;
    }

    /**
     * Set the deploy URI.
     *
     * @param deployURI Deploy URI.
     * @param map Service attribute values.
     */
    public static void setDeployURI(String deployURI, Map map) {
        map.put(SetupConstants.CONFIG_VAR_PRODUCT_NAME, deployURI);
        map.put(SetupConstants.CONFIG_VAR_OLD_CONSOLE_URI, deployURI);
        map.put(SetupConstants.CONFIG_VAR_CONSOLE_URI, deployURI);
        map.put(SetupConstants.CONFIG_VAR_SERVER_URI, deployURI);
    }

    /**
     * Returns the tag swapped string.
     *
     * @param orig String to be tag swapped.
     * @return the tag swapped string.
     */
    public static String tagSwap(String orig) {
        return tagSwap(orig, false);
    }

    /**
     * Returns the tag swapped string.
     *
     * @param orig String to be tag swapped.
     * @param bXML <code>true</code> if it is an XML file. and value
     *        needs to be escaped.
     * @return the tag swapped string.
     */
    public static String tagSwap(String orig, boolean bXML) {
        Map map = instance.defValues;
        for (Object okey : map.keySet().toArray() ) {
            String key = (String)okey;
            String value = (String)map.get(key);
            if (value != null) {
                value = value.replaceAll("[$]", "\\\\\\$");

                if (preappendSlash.contains(key)) {
                    if (bXML) {
                        value = XMLUtils.escapeSpecialCharacters(value);
                    }
                    orig = orig.replaceAll("/@" + key + "@", value);

                    if (trimSlash.contains(key)) {
                        orig = orig.replaceAll("@" + key + "@",
                            value.substring(1));
                    }
                } else if (key.equals(SetupConstants.CONFIG_VAR_ROOT_SUFFIX)) {
                    String normalized = DNUtils.normalizeDN(value);
                    String tmp = normalized.replaceAll(",", "^");
                    tmp = (bXML) ? XMLUtils.escapeSpecialCharacters(tmp) :
                        tmp;
                    orig = orig.replaceAll(
                            "@" + SetupConstants.SM_ROOT_SUFFIX_HAT + "@", tmp);

                    String rfced = DN.valueOf(value).toString();
                    tmp = (bXML) ? XMLUtils.escapeSpecialCharacters(rfced) :
                            rfced;
                    orig = orig.replaceAll(
                            "@" + SetupConstants.CONFIG_VAR_ROOT_SUFFIX + "@",
                            tmp);
                } else if(key.equals(SetupConstants.SM_ROOT_SUFFIX_HAT_SLASH)) {
                    orig = orig.replace("@" + key + "@", value);
                } else if (
                    key.equals(SetupConstants.SM_ROOT_SUFFIX_HAT) ||
                    key.equals(SetupConstants.NORMALIZED_RS) ||
                    key.equals(SetupConstants.NORMALIZED_ORG_BASE) ||
                    key.equals(SetupConstants.CONFIG_VAR_SESSION_ROOT_SUFFIX) ||
                    key.equals(SetupConstants.CONFIG_VAR_SESSION_STORE_TYPE)
                ) {
                    orig = orig.replaceAll("@" + key + "@", value);
                } else {
                    if (bXML) {
                        value = XMLUtils.escapeSpecialCharacters(value);
                    }
                    orig = orig.replaceAll("@" + key + "@", value);
                }
            }
        }
        orig = replaceRandomSecureTags(orig, RANDOM_SECURE, 128);
        orig = replaceRandomSecureTags(orig, RANDOM_SECURE_256, 256);
        return orig;
    }

    private static String replaceRandomSecureTags(String orig, String tag, int size) {
        // Each Secure Random tag should be a newly generated random.
        while (orig.contains(tag)) {
            byte[] bytes = new byte[size / 8];
            secureRandom.nextBytes(bytes);
            orig = orig.replace(tag, Base64.encode(bytes));
        }
        return orig;
    }
}
