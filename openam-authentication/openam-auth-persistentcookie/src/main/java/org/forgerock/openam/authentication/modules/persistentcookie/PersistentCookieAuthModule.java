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
 * Portions Copyrighted 2018-2025 3A Systems, LLC.
 */

package org.forgerock.openam.authentication.modules.persistentcookie;

import static org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieModuleWrapper.*;

import java.security.AccessController;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.security.auth.message.MessageInfo;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.caf.authentication.framework.AuthenticationFramework;
import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.openam.authentication.modules.common.JaspiAuthLoginModule;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.utils.ClientUtils;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;

/**
 * Authentication logic for persistent cookie authentication in OpenAM. Making use of the JASPI JwtSessionModule
 * to create and verify the persistent cookie.
 */
public class PersistentCookieAuthModule extends JaspiAuthLoginModule {

    private static final Debug DEBUG = Debug.getInstance(AUTH_RESOURCE_BUNDLE_NAME);
    private static final int MINUTES_IN_HOUR = 60;

    private static final String COOKIE_IDLE_TIMEOUT_SETTING_KEY = "openam-auth-persistent-cookie-idle-time";
    private static final String COOKIE_MAX_LIFE_SETTING_KEY = "openam-auth-persistent-cookie-max-life";

    private final CoreWrapper coreWrapper;

    private Integer tokenIdleTime;
    private Integer maxTokenLife;
    private boolean enforceClientIP;
    private boolean secureCookie;
    private boolean httpOnlyCookie;
    private String cookieName;
    private Collection<String> cookieDomains;
    private String encryptedHmacKey;

    private String UIField;
    private String RepoField;
    private Integer MaxTokens;
    
    private Principal principal;

    private final PersistentCookieModuleWrapper persistentCookieModuleWrapper;

    /**
     * Constructs an instance of the PersistentCookieAuthModule.
     *
     * Used by the PersistentCookie in a server deployment environment.
     */
    public PersistentCookieAuthModule() {
        this(new CoreWrapper(), new PersistentCookieModuleWrapper());
    }

    /**
     * Constructs an instance of the PersistentCookieAuthModule.
     *
     * Used in a unit test environment.
     *
     * @param coreWrapper An instance of the CoreWrapper.
     * @param persistentCookieModuleWrapper An instance of the wrapper for Persistent Cookie.
     */
    public PersistentCookieAuthModule(CoreWrapper coreWrapper, PersistentCookieModuleWrapper persistentCookieModuleWrapper) {
        super(AUTH_RESOURCE_BUNDLE_NAME, persistentCookieModuleWrapper);
        this.coreWrapper = coreWrapper;
        this.persistentCookieModuleWrapper = persistentCookieModuleWrapper;
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
    protected Map<String, Object> generateConfig(Subject subject, Map sharedState, Map options) {
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
//      openam-auth-persistent-cookie-input=The name of the check box, which means that the function is enabled by the user
//      openam-auth-persistent-cookie-field=The name of the field in the repository in which the issued tokens are stored
//      openam-auth-persistent-cookie-field-max=Maximum number of tokens (devices) per user
        UIField=	CollectionHelper.getMapAttr(options, "openam-auth-persistent-cookie-input");
        RepoField=CollectionHelper.getMapAttr(options, "openam-auth-persistent-cookie-field");
        String MaxTokensString = CollectionHelper.getMapAttr(options, "openam-auth-persistent-cookie-field-max");
        if (StringUtils.isEmpty(MaxTokensString)) {
            DEBUG.warning("MaxTokens not set. Defaulting to 5");
            MaxTokensString = "5";
        }
        MaxTokens = Integer.parseInt(MaxTokensString);
        
        String hmacKey = CollectionHelper.getMapAttr(options, HMAC_KEY);
        // As this key will need to be passed via session properties to the post-authentication plugin, we encrypt it
        // here to avoid it being accidentally exposed.
        encryptedHmacKey = AccessController.doPrivileged(new EncodeAction(hmacKey));

        try {
            return persistentCookieModuleWrapper.generateConfig(tokenIdleTime.toString(), maxTokenLife.toString(), enforceClientIP,
                    getRequestOrg(), secureCookie, httpOnlyCookie, cookieName, cookieDomains, hmacKey);
        } catch (SMSException e) {
            DEBUG.error("Error initialising Authentication Module", e);
            return null;
        } catch (SSOException e) {
            DEBUG.error("Error initialising Authentication Module", e);
            return null;
        }

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
	            //repo field for tokens
	            if (StringUtils.isNotBlank(RepoField)) { 
	            		setUserSessionProperty("openam.field.repo", RepoField);
	            		setUserSessionProperty("openam.field.repo.max", MaxTokens==null?"1":MaxTokens.toString());
	            }
	            
	            final Subject clientSubject = new Subject();
	            MessageInfo messageInfo = persistentCookieModuleWrapper.prepareMessageInfo(getHttpServletRequest(),
	                getHttpServletResponse());
	            if (process(messageInfo, clientSubject, callbacks)) {
	                if (principal != null) {
	                    setAuthenticatingUserName(principal.getName());
	                }
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

        final Jwt jwt = persistentCookieModuleWrapper.validateJwtSessionCookie(messageInfo);

        if (jwt == null) {
            //BAD
            //remember check ?
            if (StringUtils.isNotBlank(UIField)) { 
            		setUserSessionProperty("openam.field.ui", UIField);
            		if (StringUtils.equalsIgnoreCase("POST",getHttpServletRequest().getMethod()) && getHttpServletRequest().getParameter(UIField)!=null)
            			setUserSessionProperty("remember.check", "1");
            }
			
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

            //test and remove before session
            if (StringUtils.isNotBlank(RepoField)) 
	            	try{
	            		final SSOToken admintoken=AccessController.doPrivileged(AdminTokenAction.getInstance());
	            		final AMIdentity idm=IdUtils.getIdentity(admintoken,username, getRequestOrg());
	            		final Map<String,Set<String>> attrMap=new HashMap<String,Set<String>>(1);
	            		final Set<String> values=idm.getAttribute(RepoField);
	            		attrMap.put(RepoField, values);
	            		//test current
	            		if (!values.remove(claimsSetContext.get("openam.sid"))) //unknown token
	            			throw new AuthLoginException("Token expired");
	            		idm.setAttributes(attrMap);
        				idm.store();
	            }catch (Exception e) {
	            		throw new AuthLoginException("Token expired");
				}
            else
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
}
