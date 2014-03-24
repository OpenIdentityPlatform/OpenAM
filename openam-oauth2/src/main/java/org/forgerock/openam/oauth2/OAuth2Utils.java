/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 ForgeRock AS All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/**
 * Portions copyright 2012-2013 ForgeRock Inc
 */

package org.forgerock.openam.oauth2;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.*;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;
import com.sun.identity.log.messageid.LogMessageProvider;
import com.sun.identity.log.messageid.MessageProviderFactory;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.json.jose.utils.KeystoreManager;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.openam.oauth2.provider.OAuth2ProviderSettings;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.provider.impl.OAuth2ProviderSettingsImpl;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * Utilities related to OAuth2.
 */
public class OAuth2Utils {

    public static Debug DEBUG;

    private static LogMessageProvider msgProvider;
    private static Logger accessLogger;
    private static Logger errorLogger;
    public static boolean logStatus = false;
    private static final Map<String, OAuth2ProviderSettings> settingsProviderMap =
            new HashMap<String, OAuth2ProviderSettings>();


    private final static String DEFAULT_KEYSTORE_FILE_PROP =
            "com.sun.identity.saml.xmlsig.keystore";
    private final static String DEFAULT_KEYSTORE_PASS_FILE_PROP =
            "com.sun.identity.saml.xmlsig.storepass";
    private final static String DEFAULT_KEYSTORE_TYPE_PROP =
            "com.sun.identity.saml.xmlsig.storetype";
    private final static String DEFAULT_PRIVATE_KEY_PASS_FILE_PROP  =
            "com.sun.identity.saml.xmlsig.keypass";

    static {
        DEBUG = Debug.getInstance("OAuth2Provider");

        String status = SystemProperties.get(Constants.AM_LOGSTATUS);
        logStatus = ((status != null) && status.equalsIgnoreCase("ACTIVE"));

        if (logStatus) {
            accessLogger = (Logger)Logger.getLogger(OAuth2Constants.ACCESS_LOG_NAME);
            errorLogger = (Logger)Logger.getLogger(OAuth2Constants.ERROR_LOG_NAME);
        }
    }

    public static boolean isBlank(final String s) {
        return org.forgerock.openam.oauth2.utils.OAuth2Utils.isBlank(s);
    }

    public static Set<String> split(final String s, final String delimiter) {
        return org.forgerock.openam.oauth2.utils.OAuth2Utils.split(s, delimiter);
    }

    public static String getDeploymentURL(Request request) {
        return org.forgerock.openam.oauth2.utils.OAuth2Utils.getDeploymentURL(request);
    }

    public static String getDeploymentURL(HttpServletRequest request) {
        return org.forgerock.openam.oauth2.utils.OAuth2Utils.getDeploymentURL(request);
    }

    public static String getModuleName(Request request) {
        return org.forgerock.openam.oauth2.utils.OAuth2Utils.getModuleName(request);
    }

    public static String getServiceName(Request request) {
        return org.forgerock.openam.oauth2.utils.OAuth2Utils.getServiceName(request);
    }

    public static String getLocale(Request request) {
        return org.forgerock.openam.oauth2.utils.OAuth2Utils.getLocale(request);
    }

    /**
     * Logs an access message
     * @param msgIdName name of message id
     * @param data array of data to be logged
     * @param token session token of the user who did the operation
     * that triggered this logging
     */
    public static void logAccessMessage(
            String msgIdName,
            String data[],
            SSOToken token
    ) {
        try {
            if (msgProvider == null) {
                msgProvider = MessageProviderFactory.getProvider("OAuth2Provider");
            }
        } catch (IOException e) {
            DEBUG.error("OAuth2Utils.logAccessMessage()", e);
            DEBUG.error("OAuth2Utils.logAccessMessage():"
                    + "disabling logging");
            logStatus = false;
        }
        if ((accessLogger != null) && (msgProvider != null)) {
            LogRecord lr = msgProvider.createLogRecord(msgIdName, data, token);
            if (lr != null) {
                SSOToken ssoToken = (SSOToken)AccessController.doPrivileged(
                        AdminTokenAction.getInstance());
                    accessLogger.log(lr, ssoToken);
            }
        }
    }

    /**
     * Logs an error message
     * @param msgIdName name of message id
     * @param data array of data to be logged
     * @param token session token of the user who did the operation
     * that triggered this logging
     */
    public static void logErrorMessage(
            String msgIdName,
            String data[],
            SSOToken token
    ) {
        try {
            if (msgProvider == null) {
                msgProvider = MessageProviderFactory.getProvider("OAuth2Provider");
            }
        } catch (IOException e) {
            DEBUG.error("OAuth2Utils.logErrorMessage()", e);
            DEBUG.error("OAuth2Utils.logAccessMessage():"
                    + "disabling logging");
            logStatus = false;
        }
        if ((errorLogger != null) && (msgProvider != null)) {
            LogRecord lr = msgProvider.createLogRecord(msgIdName, data, token);
            if (lr != null) {
                SSOToken ssoToken = (SSOToken)AccessController.doPrivileged(
                        AdminTokenAction.getInstance());
                errorLogger.log(lr, ssoToken);
            }
        }
    }

    public static SSOToken getSSOToken(Request request){
        try {
            HttpServletRequest req = ServletUtils.getRequest(request);
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            return mgr.createSSOToken(req);
        } catch (Exception e){
            if (OAuth2Utils.DEBUG.messageEnabled()){
                OAuth2Utils.DEBUG.message("OAuth2Utils::Unable to get sso token: ", e);
            }
        }
        return null;
    }

    /**
     * Sets the value of the "access_token_path" parameter.
     *
     * @param value
     *            The value of the "access_token_path" parameter
     * @param context
     *            The context where to set the parameter.
     */
    public static void setAccessTokenPath(String value, Context context) {
        context.getParameters().set(org.forgerock.openam.oauth2.utils.OAuth2Utils.ACCESS_TOKEN_PATH, value);
    }

    /**
     * Sets the value of the "authorize_path" parameter.
     *
     * @param value
     *            The value of the "authorize_path" parameter
     * @param context
     *            The context where to set the parameter.
     */
    public static void setAuthorizePath(String value, Context context) {
        context.getParameters().set(org.forgerock.openam.oauth2.utils.OAuth2Utils.AUTHORIZE_PATH, value);
    }

    /**
     * Sets the value of the "tokeninfo_path" parameter.
     *
     * @param value
     *            The value of the "tokeninfo_path" parameter
     * @param context
     *            The context where to set the parameter.
     */
    public static void setTokenInfoPath(String value, Context context) {
        context.getParameters().set(org.forgerock.openam.oauth2.utils.OAuth2Utils.TOKENINFO_PATH, value);
    }

    /**
     * Constructor.
     */
    private OAuth2Utils() {
    }

    public static <T> T getOAuth2ProviderSetting(String setting, Class<T> clazz, Request request){
        SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
        ServiceConfig scm = null;
        ServiceConfigManager mgr = null;
        if (setting == null || setting.isEmpty()){
            return null;
        }
        try {
            mgr = new ServiceConfigManager(token, OAuth2Constants.OAuth2ProviderService.NAME, OAuth2Constants.OAuth2ProviderService.VERSION);
            scm = mgr.getOrganizationConfig(OAuth2Utils.getRealm(request), null);
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get provider setting: ", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to get provider setting");
        }
        Map<String, Set<String>> attrs = scm.getAttributes();
        Set<String> attr = attrs.get(setting);
        if (attr != null && attr.size() > 0){
            if (clazz.isAssignableFrom(String.class)){
                return clazz.cast(attr.iterator().next());
            } else if (clazz.isAssignableFrom(Set.class)){
                return clazz.cast(attr);
            } else {
                OAuth2Utils.DEBUG.error("OAuth2Utils::Provided an unsupported class type.");
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Provided unsupported class type");
            }
        } else {
            return null;
        }
    }
    public static String getRealm(Request request) {
        return org.forgerock.openam.oauth2.utils.OAuth2Utils.getRealm(request);
    }

    public static String getRealm(HttpServletRequest request) {
        return org.forgerock.openam.oauth2.utils.OAuth2Utils.getRealm(request);
    }

    public static AMIdentity getClientIdentity(String uName, String realm) throws OAuthProblemException {
        SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
        AMIdentity theID = null;

        try {
            AMIdentityRepository amIdRepo = new AMIdentityRepository(token, realm);

            IdSearchControl idsc = new IdSearchControl();
            idsc.setRecursive(true);
            idsc.setAllReturnAttributes(true);
            // search for the identity
            Set<AMIdentity> results = Collections.EMPTY_SET;
            idsc.setMaxResults(0);
            IdSearchResults searchResults =
                    amIdRepo.searchIdentities(IdType.AGENTONLY, uName, idsc);
                results = searchResults.getSearchResults();

            if (results == null || results.size() != 1) {
                OAuth2Utils.DEBUG.error("OAuth2Utils.getClientIdentity()::No client profile or more than one profile found.");
                throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(null,
                        "Not able to get client from OpenAM");
            }

            theID = results.iterator().next();

            //if the client is deactivated return null
            if (theID.isActive()){
                return theID;
            } else {
                return null;
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("OAuth2Utils::Unable to get client AMIdentity: ", e);
            throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(null, "Not able to get client from OpenAM");
        }
    }

    public static <T> T getRequestParameter(Request request, String s, Class<T> type) {
        return org.forgerock.openam.oauth2.utils.OAuth2Utils.getRequestParameter(request, s, type);
    }

    public static AMIdentity getIdentity(String uName, String realm) throws OAuthProblemException {
        SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
        AMIdentity theID = null;

        try {
            AMIdentityRepository amIdRepo = new AMIdentityRepository(token, realm);

            IdSearchControl idsc = new IdSearchControl();
            idsc.setRecursive(true);
            idsc.setAllReturnAttributes(true);
            // search for the identity
            Set<AMIdentity> results = Collections.EMPTY_SET;
            idsc.setMaxResults(0);
            IdSearchResults searchResults =
                    amIdRepo.searchIdentities(IdType.USER, uName, idsc);
            if (searchResults != null && !searchResults.getResultAttributes().isEmpty()) {
                results = searchResults.getSearchResults();
            } else {
                OAuth2ProviderSettings settings = OAuth2Utils.getSettingsProvider(Request.getCurrent());
                Map<String, Set<String>> avPairs = toAvPairMap(settings.getListOfAttributesTheResourceOwnerIsAuthenticatedOn(),
                        uName);
                idsc.setSearchModifiers(IdSearchOpModifier.OR, avPairs);
                searchResults =
                        amIdRepo.searchIdentities(IdType.USER, "*", idsc);
                if (searchResults != null) {
                    results = searchResults.getSearchResults();
                }
            }

            if (results == null || results.size() != 1) {
                OAuth2Utils.DEBUG.error("ScopeImpl.getIdentity()::No user profile or more than one profile found.");
                throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(null,
                        "Not able to get user from OpenAM");
            }

            theID = results.iterator().next();

            //if the client is deactivated return null
            if (theID.isActive()){
                return theID;
            } else {
                return null;
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("ClientVerifierImpl::Unable to get client AMIdentity: ", e);
            throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(null, "Not able to get client from OpenAM");
        }
    }

    private static Map toAvPairMap(Set names, String token) {
        if (token == null) {
            return Collections.EMPTY_MAP;
        }
        Map map = new HashMap();
        Set set = new HashSet();
        set.add(token);
        if (names == null || names.isEmpty()) {
            return map;
        }
        Iterator it = names.iterator();
        while (it.hasNext()) {
            map.put((String) it.next(), set);
        }
        return map;
    }

    public static String decodePassword(String password)  {
        String decodedPassword = AccessController.doPrivileged(new DecodeAction(password));

        return decodedPassword == null ? password : decodedPassword;
    }

    public static KeyPair getServerKeyPair(org.restlet.Request request){
        HttpServletRequest httpRequest = ServletUtils.getRequest(request);
        return getServerKeyPair(httpRequest);
    }

    public static KeyPair getServerKeyPair(javax.servlet.http.HttpServletRequest request){
        OAuth2ProviderSettings settings = getSettingsProvider(request);
        String alias = settings.getKeyStoreAlias();

        //get keystore password from file
        String kspfile = SystemPropertiesManager.get(DEFAULT_KEYSTORE_PASS_FILE_PROP);
        String keystorePass = null;
        if (kspfile != null) {
            try {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(new FileInputStream(kspfile)));
                    keystorePass = decodePassword(br.readLine());
                } finally {
                    if (br != null) {
                        br.close();
                    }
                }
            } catch (IOException e) {
                OAuth2Utils.DEBUG.error("OAuth2Utils.getServerKeyPair():: Unable to read keystore password file " + kspfile, e);
            }
        } else {
            OAuth2Utils.DEBUG.error("OAuth2Utils.getServerKeyPair():: keystore password is null");
        }

        String keypassfile = SystemPropertiesManager.get(DEFAULT_PRIVATE_KEY_PASS_FILE_PROP);
        String keypass = null;
        if (keypassfile != null) {
            try {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(new FileInputStream(keypassfile)));
                    keypass = decodePassword(br.readLine());
                } finally {
                    if (br != null) {
                        br.close();
                    }
                }
            } catch (IOException e) {
                OAuth2Utils.DEBUG.error("OAuth2Utils.getServerKeyPair():: Unable to read key password file " + keypassfile, e);
            }
        } else {
            OAuth2Utils.DEBUG.error("OAuth2Utils.getServerKeyPair():: key password is null");
        }

        //get private key password

        KeystoreManager keystoreManager = new KeystoreManager(
                SystemPropertiesManager.get(DEFAULT_KEYSTORE_TYPE_PROP, "JKS"),
                SystemPropertiesManager.get(DEFAULT_KEYSTORE_FILE_PROP),
                keystorePass);

        PrivateKey privateKey = keystoreManager.getPrivateKey(alias, keypass);
        PublicKey publicKey = keystoreManager.getPublicKey(alias);
        return new KeyPair(publicKey, privateKey);
    }

    /*
     * This method is called from multiple threads, and must initialize a new OAuth2ProviderSettings instance atomically.
     */
    public static OAuth2ProviderSettings getSettingsProvider(org.restlet.Request request){
        HttpServletRequest httpRequest = ServletUtils.getRequest(request);
        return getSettingsProvider(httpRequest);
    }

    /*
     * This method is called from multiple threads, and must initialize a new OAuth2ProviderSettings instance atomically.
     */
    public static OAuth2ProviderSettings getSettingsProvider(javax.servlet.http.HttpServletRequest request){
        synchronized (settingsProviderMap) {
            String realm = OAuth2Utils.getRealm(request);
            OAuth2ProviderSettings setting = settingsProviderMap.get(realm);
            if (setting != null){
                return setting;
            } else {
                setting = new OAuth2ProviderSettingsImpl(request);
                settingsProviderMap.put(realm, setting);
                return setting;
            }
        }
    }

    /**
     * Returns the OAuth2ProviderSettings instance from the local cache for the given realm. If the realm does not
     * have a OAuth2ProviderSettings instance in the cache a OAuthProblemException.
     *
     * @param realm The realm.
     * @return The OAuth2ProviderSettings instance.
     * @throws OAuthProblemException If a OAuth2ProviderSettings has not been configured for the realm.
     */
    public static OAuth2ProviderSettings getSettingsProvider(final String realm) {
        synchronized (settingsProviderMap) {
            OAuth2ProviderSettings setting = settingsProviderMap.get(realm);
            if (setting != null) {
                return setting;
            }
            throw OAuthProblemException.
                    handleOAuthProblemException("OAuth2ProviderSettings not configured for realm, " + realm);
        }
    }
}
