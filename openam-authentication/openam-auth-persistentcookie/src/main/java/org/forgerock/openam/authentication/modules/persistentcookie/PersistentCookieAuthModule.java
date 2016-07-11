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
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.forgerock.caf.authentication.framework.AuthenticationFramework;
import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.jaspi.modules.session.jwt.ServletJwtSessionModule;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.openam.authentication.modules.common.JaspiAuthModuleWrapper;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.utils.AMKeyProvider;
import org.forgerock.openam.utils.ClientUtils;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * Authentication logic for persistent cookie authentication in OpenAM. Making use of the JASPI JwtSessionModule
 * to create and verify the persistent cookie.
 */
public class PersistentCookieAuthModule extends JaspiAuthModuleWrapper<ServletJwtSessionModule> {

    private static final String AUTH_RESOURCE_BUNDLE_NAME = "amAuthPersistentCookie";
    private static final Debug DEBUG = Debug.getInstance(AUTH_RESOURCE_BUNDLE_NAME);
    private static final String AUTH_SERVICE_NAME = "iPlanetAMAuthService";
    private static final String AUTH_KEY_ALIAS = "iplanet-am-auth-key-alias";
    private static final String SSO_TOKEN_ORGANIZATION_PROPERTY_KEY = "Organization";
    private static final int MINUTES_IN_HOUR = 60;

    private static final String COOKIE_IDLE_TIMEOUT_SETTING_KEY = "openam-auth-persistent-cookie-idle-time";
    private static final String COOKIE_MAX_LIFE_SETTING_KEY = "openam-auth-persistent-cookie-max-life";
    private static final String ENFORCE_CLIENT_IP_SETTING_KEY = "openam-auth-persistent-cookie-enforce-ip";
    private static final String SECURE_COOKIE_KEY = "openam-auth-persistent-cookie-secure-cookie";
    private static final String HTTP_ONLY_COOKIE_KEY = "openam-auth-persistent-cookie-http-only-cookie";
    private static final String COOKIE_NAME_KEY = "openam-auth-persistent-cookie-name";
    private static final String COOKIE_DOMAINS_KEY = "openam-auth-persistent-cookie-domains";
    private static final String HMAC_KEY = "openam-auth-persistent-cookie-hmac-key";

    private static final String OPENAM_USER_CLAIM_KEY = "openam.usr";
    private static final String OPENAM_AUTH_TYPE_CLAIM_KEY = "openam.aty";
    private static final String OPENAM_SESSION_ID_CLAIM_KEY = "openam.sid";
    private static final String OPENAM_REALM_CLAIM_KEY = "openam.rlm";
    private static final String OPENAM_CLIENT_IP_CLAIM_KEY = "openam.clientip";

    private final AMKeyProvider amKeyProvider;
    private final CoreWrapper coreWrapper;

    private Integer tokenIdleTime;
    private Integer maxTokenLife;
    private boolean enforceClientIP;
    private boolean secureCookie;
    private boolean httpOnlyCookie;
    private String cookieName;
    private Collection<String> cookieDomains;
    private String encryptedHmacKey;

    private Principal principal;

    /**
     * Constructs an instance of the PersistentCookieAuthModule.
     *
     * Used by the PersistentCookie in a server deployment environment.
     */
    public PersistentCookieAuthModule() {
        this(new ServletJwtSessionModule(), new AMKeyProvider(), new CoreWrapper());
    }

    /**
     * Constructs an instance of the PersistentCookieAuthModule.
     *
     * Used in a unit test environment.
     *
     * @param jwtSessionModule An instance of the JwtSessionModule.
     * @param amKeyProvider An instance of the AMKeyProvider.
     * @param coreWrapper An instance of the CoreWrapper.
     */
    public PersistentCookieAuthModule(ServletJwtSessionModule jwtSessionModule, AMKeyProvider amKeyProvider,
            CoreWrapper coreWrapper) {
        super(jwtSessionModule, AUTH_RESOURCE_BUNDLE_NAME);
        this.amKeyProvider = amKeyProvider;
        this.coreWrapper = coreWrapper;
    }

    /**
     * Initialises the JwtSessionModule for use by the AM Login Module.
     *
     * @param subject {@inheritDoc}
     * @param sharedState {@inheritDoc}
     * @param options {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    protected Map<String, Object> initialize(Subject subject, Map sharedState, Map options) {

        String idleTimeString = CollectionHelper.getMapAttr(options, COOKIE_IDLE_TIMEOUT_SETTING_KEY);
        String maxLifeString = CollectionHelper.getMapAttr(options, COOKIE_MAX_LIFE_SETTING_KEY);
        if (StringUtils.isEmpty(idleTimeString)) {
            DEBUG.warning("Cookie Idle Timeout not set. Defaulting to 0");
            idleTimeString = "0";
        }
        if (StringUtils.isEmpty(maxLifeString)) {
            DEBUG.warning("Cookie Max Life not set. Defaulting to 0");
            maxLifeString = "0";
        }
        tokenIdleTime = Integer.parseInt(idleTimeString) * MINUTES_IN_HOUR;
        maxTokenLife = Integer.parseInt(maxLifeString) * MINUTES_IN_HOUR;
        enforceClientIP = CollectionHelper.getBooleanMapAttr(options, ENFORCE_CLIENT_IP_SETTING_KEY, false);
        secureCookie = CollectionHelper.getBooleanMapAttr(options, SECURE_COOKIE_KEY, true);
        httpOnlyCookie = CollectionHelper.getBooleanMapAttr(options, HTTP_ONLY_COOKIE_KEY, true);
        cookieName = CollectionHelper.getMapAttr(options, COOKIE_NAME_KEY);
        cookieDomains = coreWrapper.getCookieDomainsForRequest(getHttpServletRequest());
        String hmacKey = CollectionHelper.getMapAttr(options, HMAC_KEY);

        // As this key will need to be passed via session properties to the post-authentication plugin, we encrypt it
        // here to avoid it being accidentally exposed.
        encryptedHmacKey = AccessController.doPrivileged(new EncodeAction(hmacKey));

        try {
            return initialize(tokenIdleTime.toString(), maxTokenLife.toString(), enforceClientIP, getRequestOrg(),
                    secureCookie, httpOnlyCookie, cookieName, cookieDomains, hmacKey);
        } catch (SMSException e) {
            DEBUG.error("Error initialising Authentication Module", e);
            return null;
        } catch (SSOException e) {
            DEBUG.error("Error initialising Authentication Module", e);
            return null;
        }
    }

    /**
     * Creates a Map of configuration information required to configure the JwtSessionModule.
     *
     * @param tokenIdleTime The number of seconds the JWT can be not used for before becoming invalid.
     * @param maxTokenLife The number of seconds the JWT can be used for before becoming invalid.
     * @param enforceClientIP The enforcement client IP.
     * @param realm The realm for the persistent cookie.
     * @param secureCookie {@code true} if the persistent cookie should be set as secure.
     * @param httpOnlyCookie {@code true} if the persistent cookie should be set as http only.
     * @param hmacKey base64-encoded HMAC signing key.
     * @return A Map containing the configuration information for the JWTSessionModule.
     * @throws SMSException If there is a problem getting the key alias.
     * @throws SSOException If there is a problem getting the key alias.
     */
    private Map<String, Object> initialize(final String tokenIdleTime, final String maxTokenLife,
            final boolean enforceClientIP,  final String realm, boolean secureCookie, boolean httpOnlyCookie,
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
     * Overridden as to call different method on underlying JASPI JwtSessionModule.
     *
     * @param callbacks {@inheritDoc}
     * @param state {@inheritDoc}
     * @return {@inheritDoc}
     * @throws LoginException {@inheritDoc}
     */
    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {

        switch (state) {
        case ISAuthConstants.LOGIN_START: {
            setUserSessionProperty(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY, tokenIdleTime.toString());
            setUserSessionProperty(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY, maxTokenLife.toString());
            setUserSessionProperty(ENFORCE_CLIENT_IP_SETTING_KEY, Boolean.toString(enforceClientIP));
            setUserSessionProperty(SECURE_COOKIE_KEY, Boolean.toString(secureCookie));
            setUserSessionProperty(HTTP_ONLY_COOKIE_KEY, Boolean.toString(httpOnlyCookie));
            if (cookieName != null) {
                setUserSessionProperty(COOKIE_NAME_KEY, cookieName);
            }
            String cookieDomainsString = "";
            for (String cookieDomain : cookieDomains) {
                cookieDomainsString += cookieDomain + ",";
            }
            setUserSessionProperty(COOKIE_DOMAINS_KEY, cookieDomainsString);
            setUserSessionProperty(HMAC_KEY, encryptedHmacKey);
            final Subject clientSubject = new Subject();
            MessageInfo messageInfo = prepareMessageInfo(getHttpServletRequest(), getHttpServletResponse());
            if (process(messageInfo, clientSubject, callbacks)) {
                return ISAuthConstants.LOGIN_SUCCEED;
            }
            throw new AuthLoginException(AUTH_RESOURCE_BUNDLE_NAME, "cookieNotValid", null);
        }
        default: {
            throw new AuthLoginException(AUTH_RESOURCE_BUNDLE_NAME, "incorrectState", null);
        }
        }
    }

    /**
     * If Jwt is invalid then throws LoginException, otherwise Jwt is valid and the realm is check to ensure
     * the user is authenticating in the same realm.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param callbacks {@inheritDoc}
     * @return {@inheritDoc}
     * @throws LoginException {@inheritDoc}
     */
    @Override
    protected boolean process(MessageInfo messageInfo, Subject clientSubject, Callback[] callbacks)
            throws LoginException {

        final Jwt jwt = getServerAuthModule().validateJwtSessionCookie(messageInfo);

        if (jwt == null) {
            //BAD
            throw new AuthLoginException(AUTH_RESOURCE_BUNDLE_NAME, "cookieNotValid", null);
        } else {
            //GOOD

            final Map<String, Object> claimsSetContext =
                    jwt.getClaimsSet().getClaim(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, Map.class);
            if (claimsSetContext == null) {
                throw new AuthLoginException(AUTH_RESOURCE_BUNDLE_NAME, "jaspiContextNotFound", null);
            }

            // Need to check realm
            final String jwtRealm = (String) claimsSetContext.get(OPENAM_REALM_CLAIM_KEY);
            if (!getRequestOrg().equals(jwtRealm)) {
                throw new AuthLoginException(AUTH_RESOURCE_BUNDLE_NAME, "authFailedDiffRealm", null);
            }

            final String storedClientIP = (String) claimsSetContext.get(OPENAM_CLIENT_IP_CLAIM_KEY);
            if (enforceClientIP) {
                enforceClientIP(storedClientIP);
            }

            // Need to get user from jwt to use in Principal
            final String username = (String) claimsSetContext.get(OPENAM_USER_CLAIM_KEY);
            principal = new Principal() {
                public String getName() {
                    return username;
                }
            };

            setUserSessionProperty(JwtSessionModule.JWT_VALIDATED_KEY, Boolean.TRUE.toString());

            return true;
        }
    }

    /**
     * Enforces that the client IP that the request originated from matches the stored client IP that the
     * persistent cookie was issued to.
     *
     * @param storedClientIP The stored client IP.
     * @throws AuthLoginException If the client IP on the request does not match the stored client IP.
     */
    private void enforceClientIP(final String storedClientIP) throws AuthLoginException {
        final String clientIP = ClientUtils.getClientIPAddress(getHttpServletRequest());
        if (storedClientIP == null || storedClientIP.isEmpty()) {
            DEBUG.message("Client IP not stored when persistent cookie was issued.");
            throw new AuthLoginException(AUTH_RESOURCE_BUNDLE_NAME, "authFailedClientIPDifferent", null);
        } else if (clientIP == null || clientIP.isEmpty()) {
            DEBUG.message("Client IP could not be retrieved from request.");
            throw new AuthLoginException(AUTH_RESOURCE_BUNDLE_NAME, "authFailedClientIPDifferent", null);
        } else if (!storedClientIP.equals(clientIP)) {
            DEBUG.message("Client IP not the same, original: " + storedClientIP + ", request: " + clientIP);
            throw new AuthLoginException(AUTH_RESOURCE_BUNDLE_NAME, "authFailedClientIPDifferent", null);
        }
        // client IP is valid
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * Initialises the JwtSessionModule for use by the Post Authentication Process.
     *
     * @param requestParamsMap {@inheritDoc}
     * @param request {@inheritDoc}
     * @param response {@inheritDoc}
     * @param ssoToken {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthenticationException {@inheritDoc}
     */
    @Override
    protected Map<String, Object> initialize(Map requestParamsMap, HttpServletRequest request,
            HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {

        try {
            final String tokenIdleTime = ssoToken.getProperty(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY);
            final String maxTokenLife = ssoToken.getProperty(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY);
            final boolean enforceClientIP = Boolean.parseBoolean(ssoToken.getProperty(ENFORCE_CLIENT_IP_SETTING_KEY));
            final String realm = ssoToken.getProperty(SSO_TOKEN_ORGANIZATION_PROPERTY_KEY);
            boolean secureCookie = Boolean.parseBoolean(ssoToken.getProperty(SECURE_COOKIE_KEY));
            boolean httpOnlyCookie = Boolean.parseBoolean(ssoToken.getProperty(HTTP_ONLY_COOKIE_KEY));
            String cookieName = ssoToken.getProperty(COOKIE_NAME_KEY);
            String cookieDomainsString = ssoToken.getProperty(COOKIE_DOMAINS_KEY);
            Collection<String> cookieDomains;
            if (cookieDomainsString.isEmpty()) {
                cookieDomains = Collections.singleton(null);
            } else {
                cookieDomains = Arrays.asList(cookieDomainsString.split(","));
            }
            final String hmacKey = AccessController.doPrivileged(new DecodeAction(ssoToken.getProperty(HMAC_KEY)));
            ssoToken.setProperty(HMAC_KEY, "");

            return initialize(tokenIdleTime, maxTokenLife, enforceClientIP, realm, secureCookie, httpOnlyCookie,
                    cookieName, cookieDomains, hmacKey);

        } catch (SSOException e) {
            DEBUG.error("Could not initialise the Auth Module", e);
            throw new AuthenticationException(e.getLocalizedMessage());
        } catch (SMSException e) {
            DEBUG.error("Could not initialise the Auth Module", e);
            throw new AuthenticationException(e.getLocalizedMessage());
        }
    }

    /**
     * Sets the required information that needs to be in the jwt.
     *
     * @param messageInfo {@inheritDoc}
     * @param requestParamsMap {@inheritDoc}
     * @param request {@inheritDoc}
     * @param response {@inheritDoc}
     * @param ssoToken {@inheritDoc}
     * @throws AuthenticationException {@inheritDoc}
     */
    @Override
    public void onLoginSuccess(MessageInfo messageInfo, Map requestParamsMap, HttpServletRequest request,
            HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {

        try {
            Map<String, Object> contextMap = getServerAuthModule().getContextMap(messageInfo);

            contextMap.put(OPENAM_USER_CLAIM_KEY, ssoToken.getPrincipal().getName());
            contextMap.put(OPENAM_AUTH_TYPE_CLAIM_KEY, ssoToken.getAuthType());
            contextMap.put(OPENAM_SESSION_ID_CLAIM_KEY, ssoToken.getTokenID().toString());
            contextMap.put(OPENAM_REALM_CLAIM_KEY, ssoToken.getProperty(SSO_TOKEN_ORGANIZATION_PROPERTY_KEY));
            contextMap.put(OPENAM_CLIENT_IP_CLAIM_KEY, ClientUtils.getClientIPAddress(request));

            String jwtString = ssoToken.getProperty(JwtSessionModule.JWT_VALIDATED_KEY);
            if (jwtString != null) {
                messageInfo.getMap().put(JwtSessionModule.JWT_VALIDATED_KEY, Boolean.parseBoolean(jwtString));
            }

        } catch (SSOException e) {
            DEBUG.error("Could not secure response", e);
            throw new AuthenticationException(e.getLocalizedMessage());
        }
    }

    /**
     * Deletes the persistent cookie if authentication fails for some reason.
     *
     * @param requestParamsMap {@inheritDoc}
     * @param request {@inheritDoc}
     * @param response {@inheritDoc}
     */
    @Override
    public void onLoginFailure(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response) {
        //TODO would need to get the initialization config from the JWT? before attempting to delete the cookie
        //getServerAuthModule().deleteSessionJwtCookie(response);
    }

    /**
     * Deletes the persistent cookie on logout.
     *
     * @param request {@inheritDoc}
     * @param response {@inheritDoc}
     * @param ssoToken {@inheritDoc}
     */
    @Override
    public void onLogout(HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken) {
        try {
            Map<String, Object> config = initialize(null, request, response, ssoToken);
            // The HMAC signing key will be null on logout, but this is rejected by the commons auth module, so
            // replace with a dummy value here. It is not used.
            config.put(JwtSessionModule.HMAC_SIGNING_KEY, Base64.encode(new byte[32]));
            getServerAuthModule().initialize(createRequestMessagePolicy(), null, null, config);
            getServerAuthModule().deleteSessionJwtCookie(prepareMessageInfo(null, response));
        } catch (AuthenticationException e) {
            DEBUG.error("Failed to initialise the underlying JASPI Server Auth Module.", e);
        } catch (AuthException e) {
            DEBUG.error("Failed to initialise the underlying JASPI Server Auth Module.", e);
        }
    }

    /**
     * Gets the Key alias for the realm.
     *
     * @param orgName The organisation name for the realm.
     * @return The key alias.
     * @throws SSOException If there is a problem getting the key alias.
     * @throws SMSException If there is a problem getting the key alias.
     */
    protected String getKeyAlias(String orgName) throws SSOException, SMSException {

        SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());

        ServiceConfigManager scm = new ServiceConfigManager(AUTH_SERVICE_NAME, token);

        ServiceConfig orgConfig = scm.getOrganizationConfig(orgName, null);
        String keyAlias = CollectionHelper.getMapAttr(orgConfig.getAttributes(), AUTH_KEY_ALIAS);

        return keyAlias;
    }
}
