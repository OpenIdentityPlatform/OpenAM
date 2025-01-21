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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 *
 */

package org.forgerock.openam.authentication.modules.persistentcookie;

import static org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieModuleWrapper.*;

import java.security.AccessController;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessageInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.openam.authentication.modules.common.JaspiAuthLoginModulePostAuthenticationPlugin;
import org.forgerock.openam.utils.ClientUtils;
import org.forgerock.util.annotations.VisibleForTesting;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.sm.SMSException;

/**
 * Post authentication plugin for setting the Persistent Cookie on successful authentication.
 */
public class PersistentCookieAuthModulePostAuthenticationPlugin extends JaspiAuthLoginModulePostAuthenticationPlugin {

    private final PersistentCookieModuleWrapper persistentCookieModuleWrapper;

    /**
     * Zero args constructor required by AMPostAuthProcessInterface.
     */
    public PersistentCookieAuthModulePostAuthenticationPlugin() {
        this(new PersistentCookieModuleWrapper());
    }

    @VisibleForTesting
    protected PersistentCookieAuthModulePostAuthenticationPlugin(PersistentCookieModuleWrapper persistentCookieModuleWrapper) {
        super(AUTH_RESOURCE_BUNDLE_NAME, persistentCookieModuleWrapper);
        this.persistentCookieModuleWrapper = persistentCookieModuleWrapper;
    }

    /**
     * Initialises the JwtSessionModule for use by the Post Authentication Process.
     *
     * @param request {@inheritDoc}
     * @param response {@inheritDoc}
     * @param ssoToken {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthenticationException {@inheritDoc}
     */
    @Override
    protected Map<String, Object> generateConfig(HttpServletRequest request, HttpServletResponse response,
                                                 SSOToken ssoToken) throws AuthenticationException {
    		if (request==null || ssoToken==null)
    			return Collections.EMPTY_MAP;

        try {
            final String tokenIdleTime = ssoToken.getProperty(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY,true);
            final String maxTokenLife = ssoToken.getProperty(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY,true);
            final boolean enforceClientIP = Boolean.parseBoolean(ssoToken.getProperty(ENFORCE_CLIENT_IP_SETTING_KEY,true));
            final String realm = ssoToken.getProperty(SSO_TOKEN_ORGANIZATION_PROPERTY_KEY,true);
            boolean secureCookie = Boolean.parseBoolean(ssoToken.getProperty(SECURE_COOKIE_KEY,true));
            boolean httpOnlyCookie = Boolean.parseBoolean(ssoToken.getProperty(HTTP_ONLY_COOKIE_KEY,true));
            String cookieName = ssoToken.getProperty(COOKIE_NAME_KEY,true);
            String cookieDomainsString = ssoToken.getProperty(COOKIE_DOMAINS_KEY,true);
            Collection<String> cookieDomains;
            if (StringUtils.isBlank(cookieDomainsString)) {
                cookieDomains = Collections.singleton(null);
            } else {
                cookieDomains = Arrays.asList(cookieDomainsString.split(","));
            }
            final String hmacKey = AccessController.doPrivileged(new DecodeAction(ssoToken.getProperty(HMAC_KEY,true)));
            ssoToken.setProperty(HMAC_KEY, "");

            return persistentCookieModuleWrapper.generateConfig(tokenIdleTime, maxTokenLife, enforceClientIP, realm, secureCookie,httpOnlyCookie, cookieName, cookieDomains, hmacKey);

        } catch (SSOException | SMSException e) {
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
    @SuppressWarnings("unchecked")
	@Override
    public void onLoginSuccess(MessageInfo messageInfo, Map requestParamsMap, HttpServletRequest request,
                               HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {
		if (request==null || ssoToken==null)
			return;
        try {
	        //check remember UI checkbox      
        		final String FieldUI=ssoToken.getProperty("openam.field.ui",true);
            if (StringUtils.isNotBlank(FieldUI)
            		&& !(StringUtils.isNotBlank(ssoToken.getProperty("remember.check",true)) 
            				|| StringUtils.equalsIgnoreCase("POST",request.getMethod()) && request.getParameter(FieldUI)!=null
            			)
            		) 
        			messageInfo.getMap().put("skipSession",true);
            else
            {
            		final Map<String, Object> contextMap = persistentCookieModuleWrapper.getContextMap(messageInfo);
            		final String uid=ssoToken.getProperty("Principal",true);
            		final String realm=ssoToken.getProperty(SSO_TOKEN_ORGANIZATION_PROPERTY_KEY,true);
	            contextMap.put(OPENAM_USER_CLAIM_KEY, uid);
	            contextMap.put(OPENAM_AUTH_TYPE_CLAIM_KEY, ssoToken.getProperty("AuthType",true));
	            contextMap.put(OPENAM_SESSION_ID_CLAIM_KEY, ssoToken.getTokenID().toString());
	            contextMap.put(OPENAM_REALM_CLAIM_KEY, realm);
	            contextMap.put(OPENAM_CLIENT_IP_CLAIM_KEY, ClientUtils.getClientIPAddress(request));
            
		        String jwtString = ssoToken.getProperty(JwtSessionModule.JWT_VALIDATED_KEY,true);
		        if (jwtString != null) {
		            messageInfo.getMap().put(JwtSessionModule.JWT_VALIDATED_KEY, Boolean.parseBoolean(jwtString));
		        }
		        //save last token for next usage
		        final String repoField=ssoToken.getProperty("openam.field.repo",true);
		        if (StringUtils.isNotBlank(repoField)) 
			        	try{
			        		Integer maxTokens=1;
			        		try {
			        			maxTokens=Integer.parseInt(ssoToken.getProperty("openam.field.repo.max",true));
			        		}catch (NumberFormatException e) {}
			        		final SSOToken admintoken=AccessController.doPrivileged(AdminTokenAction.getInstance());
				        	final AMIdentity idm=IdUtils.getIdentity(admintoken,uid, realm);
		            		final Map<String,Set<String>> attrMap=new HashMap<String,Set<String>>(1);
		            		final Set<String> values=idm.getAttribute(repoField);
		            		//limit old
		            		while (values.size()>=maxTokens)
		            			values.remove(values.iterator().next());
		            		values.add(ssoToken.getTokenID().toString());
		            		attrMap.put(repoField, values);
		            		idm.setAttributes(attrMap);
		            		idm.store();
			        }catch (IdRepoException e) {
			        		DEBUG.error("Could not save token", e);
					}
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
		if (request==null)
			return;
        try {
            Map<String, Object> config = generateConfig(request, response, ssoToken);
            // The HMAC signing key will be null on logout, but this is rejected by the commons auth module, so
            // replace with a dummy value here. It is not used.
            config.put(JwtSessionModule.HMAC_SIGNING_KEY, Base64.encode(new byte[32]));
            persistentCookieModuleWrapper.initialize(null, config);
            persistentCookieModuleWrapper.deleteSessionJwtCookie(
                    persistentCookieModuleWrapper.prepareMessageInfo(null, response));
        } catch (AuthenticationException | AuthException e) {
            DEBUG.error("Failed to initialise the underlying JASPI Server Auth Module.", e);
        }
    }
}
