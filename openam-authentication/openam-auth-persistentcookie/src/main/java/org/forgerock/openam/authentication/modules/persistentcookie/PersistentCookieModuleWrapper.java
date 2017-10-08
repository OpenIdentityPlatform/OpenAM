/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.persistentcookie;

import java.security.AccessController;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.message.MessageInfo;

import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.jaspi.modules.session.jwt.ServletJwtSessionModule;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.openam.authentication.modules.common.JaspiAuthModuleWrapper;
import org.forgerock.openam.utils.AMKeyProvider;
import org.forgerock.util.annotations.VisibleForTesting;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * Wrapper class for providing Persistent Cookie functionality. Uses the Jaspi ServletJwtSessionModule for this.
 * Provides wrappers over the modules functionality, and hides the underlying code from external users.
 */
public class PersistentCookieModuleWrapper extends JaspiAuthModuleWrapper<ServletJwtSessionModule> {
    private final AMKeyProvider amKeyProvider;

    public static final String AUTH_RESOURCE_BUNDLE_NAME = "amAuthPersistentCookie";
    public static final String SSO_TOKEN_ORGANIZATION_PROPERTY_KEY = "Organization";
    public static final String ENFORCE_CLIENT_IP_SETTING_KEY = "openam-auth-persistent-cookie-enforce-ip";
    public static final String SECURE_COOKIE_KEY = "openam-auth-persistent-cookie-secure-cookie";
    public static final String HTTP_ONLY_COOKIE_KEY = "openam-auth-persistent-cookie-http-only-cookie";
    public static final String COOKIE_NAME_KEY = "openam-auth-persistent-cookie-name";
    public static final String COOKIE_DOMAINS_KEY = "openam-auth-persistent-cookie-domains";

    private static final String AUTH_SERVICE_NAME = "iPlanetAMAuthService";

    public static final String HMAC_KEY = "openam-auth-persistent-cookie-hmac-key";

    private static final String AUTH_KEY_ALIAS = "iplanet-am-auth-key-alias";

    public static final String OPENAM_USER_CLAIM_KEY = "openam.usr";
    public static final String OPENAM_AUTH_TYPE_CLAIM_KEY = "openam.aty";
    public static final String OPENAM_SESSION_ID_CLAIM_KEY = "openam.sid";
    public static final String OPENAM_REALM_CLAIM_KEY = "openam.rlm";
    public static final String OPENAM_CLIENT_IP_CLAIM_KEY = "openam.clientip";

    /**
     * Creates the wrapper with default values for the dependencies.
     */
    public PersistentCookieModuleWrapper() {
        this(new ServletJwtSessionModule(), new AMKeyProvider());
    }

    /**
     * Creates the wrapper with selectable dependencies for testing.
     * @param module The underlying module.
     * @param amKeyProvider The AM Key Provider.
     */
    @VisibleForTesting
    protected PersistentCookieModuleWrapper(ServletJwtSessionModule module, AMKeyProvider amKeyProvider) {
        super(module);
        this.amKeyProvider = amKeyProvider;
    }

    /**
     * Creates a Map of configuration information required to configure the JwtSessionModule.
     *
     * @param tokenIdleTime   The number of seconds the JWT can be not used for before becoming invalid.
     * @param maxTokenLife    The number of seconds the JWT can be used for before becoming invalid.
     * @param enforceClientIP The enforcement client IP.
     * @param realm           The realm for the persistent cookie.
     * @param secureCookie    {@code true} if the persistent cookie should be set as secure.
     * @param httpOnlyCookie  {@code true} if the persistent cookie should be set as http only.
     * @param hmacKey         base64-encoded HMAC signing key.
     * @return A Map containing the configuration information for the JWTSessionModule.
     * @throws SMSException If there is a problem getting the key alias.
     * @throws SSOException If there is a problem getting the key alias.
     */
    public Map<String, Object> generateConfig(final String tokenIdleTime, final String maxTokenLife,
                                              final boolean enforceClientIP, final String realm, boolean secureCookie, boolean httpOnlyCookie,
                                              String cookieName, Collection<String> cookieDomains, String hmacKey) throws SMSException, SSOException {

        Map<String, Object> config = new HashMap<String, Object>();
        config.put(JwtSessionModule.KEY_ALIAS_KEY, getKeyAlias(realm));
        config.put(JwtSessionModule.PRIVATE_KEY_PASSWORD_KEY, amKeyProvider.getPrivateKeyPass());
        config.put(JwtSessionModule.KEYSTORE_TYPE_KEY, amKeyProvider.getKeystoreType());
        config.put(JwtSessionModule.KEYSTORE_FILE_KEY, amKeyProvider.getKeystoreFilePath());
        config.put(JwtSessionModule.KEYSTORE_PASSWORD_KEY, new String(amKeyProvider.getKeystorePass()));
        config.put(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY, tokenIdleTime);
        config.put(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY, maxTokenLife);
        config.put(JwtSessionModule.SECURE_COOKIE_KEY, secureCookie);
        config.put(JwtSessionModule.HTTP_ONLY_COOKIE_KEY, httpOnlyCookie);
        config.put(ENFORCE_CLIENT_IP_SETTING_KEY, enforceClientIP);
        config.put(JwtSessionModule.SESSION_COOKIE_NAME_KEY, cookieName);
        config.put(JwtSessionModule.COOKIE_DOMAINS_KEY, cookieDomains);
        config.put(JwtSessionModule.HMAC_SIGNING_KEY, hmacKey);

        return config;
    }

    /**
     * Gets the Key alias for the realm.
     *
     * @param orgName The organisation name for the realm.
     * @return The key alias.
     * @throws SSOException If there is a problem getting the key alias.
     * @throws SMSException If there is a problem getting the key alias.
     */
    private String getKeyAlias(String orgName) throws SSOException, SMSException {
        ServiceConfig orgConfig = getServiceConfigManager().getOrganizationConfig(orgName, null);
        return CollectionHelper.getMapAttr(orgConfig.getAttributes(), AUTH_KEY_ALIAS);
    }

    /**
     * Returns the config manager for the auth service.
     * @return The authentication service config manager.
     * @throws SSOException If admin authentication fails.
     * @throws SMSException If something goes wrong with the operation.
     */
    @VisibleForTesting
    protected ServiceConfigManager getServiceConfigManager() throws SSOException, SMSException {
        SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
        return new ServiceConfigManager(AUTH_SERVICE_NAME, token);
    }


    /**
     * Validates if the Jwt Session Cookie is valid and the idle timeout or max life has expired. Calls into the
     * underlying Jaspi module.
     *
     * @param messageInfo The MessageInfo instance.
     * @return The Jwt if successfully validated otherwise null.
     */
    public Jwt validateJwtSessionCookie(MessageInfo messageInfo) {
        return getServerAuthModule().validateJwtSessionCookie(messageInfo);
    }

    /**
     * Ensures the context map exists within the messageInfo object, and then returns the context map to be used.
     * Calls into the underlying Jaspi module.
     *
     * @param messageInfo The MessageInfo instance.
     * @return The context map internal to the messageInfo's map.
     */
    public Map<String, Object> getContextMap(MessageInfo messageInfo) {
        return getServerAuthModule().getContextMap(messageInfo);
    }

    /**
     * Provides a way to delete the Jwt Session Cookie, by setting a new cookie with the same name, null value and
     * max age 0.
     * Calls into the underlying Jaspi module.
     *
     * @param messageInfo The {@code MessageInfo} which contains the Response with the Jwt Session Cookie.
     */
    public void deleteSessionJwtCookie(MessageInfo messageInfo) {
        getServerAuthModule().deleteSessionJwtCookie(messageInfo);
    }
}
