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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import com.google.inject.Injector;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.ext.cts.repo.DefaultOAuthTokenStoreImpl;
import org.forgerock.openam.oauth2.internal.UserIdentityVerifier;
import org.forgerock.openam.oauth2.model.ClientApplication;
import org.forgerock.openam.oauth2.model.JWTToken;
import org.forgerock.openam.oauth2.model.TokenManager;
import org.forgerock.openam.oauth2.model.impl.ClientApplicationImpl;
import org.forgerock.openam.oauth2.model.impl.TokenManagerImpl;
import org.forgerock.openam.oauth2.openid.ConnectClientRegistration;
import org.forgerock.openam.oauth2.openid.EndSession;
import org.forgerock.openam.oauth2.openid.OpenIDConnectDiscovery;
import org.forgerock.openam.oauth2.provider.ClientDAO;
import org.forgerock.openam.oauth2.provider.ClientVerifier;
import org.forgerock.openam.oauth2.provider.OAuth2ProviderSettings;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.oauth2.core.Scope;
import org.forgerock.openam.oauth2.provider.ServerAuthorizer;
import org.forgerock.openam.oauth2.provider.impl.ClientVerifierImpl;
import org.forgerock.openam.oauth2.provider.impl.OAuth2ProviderSettingsImpl;
import org.forgerock.openam.oauth2.provider.impl.OpenAMClientDAO;
import org.forgerock.openam.oauth2.provider.impl.OpenAMIdentityVerifier;
import org.forgerock.openam.oauth2.provider.impl.OpenAMServerAuthorizer;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.resource.ServerResource;
import org.restlet.security.SecretVerifier;
import org.restlet.security.Verifier;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import java.security.AccessController;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OpenAMOAuth2ConfigurationFactory implements OAuth2ConfigurationFactory {

    private enum OAuth2ProviderSettingsHolder {
        INSTANCE;

        private final Map<String, OAuth2ProviderSettings> oAuth2ProviderSettingsMap =
                new HashMap<String, OAuth2ProviderSettings>();

        private static OAuth2ProviderSettings getInstance(final String deploymentUrl, final String realm) {
            synchronized (INSTANCE.oAuth2ProviderSettingsMap) {
                OAuth2ProviderSettings oAuth2ProviderSettings = INSTANCE.oAuth2ProviderSettingsMap.get(realm);
                if (oAuth2ProviderSettings == null) {
                    oAuth2ProviderSettings = new OAuth2ProviderSettingsImpl(deploymentUrl, realm);
                    INSTANCE.oAuth2ProviderSettingsMap.put(realm, oAuth2ProviderSettings);
                }
                return oAuth2ProviderSettings;
            }
        }
    }

    public static OAuth2ConfigurationFactory getOAuth2ConfigurationFactory() {
        return new OpenAMOAuth2ConfigurationFactory();
    }

    public Object getClientIdentity(JWTToken jwtToken) {
        return OAuth2Utils.getClientIdentity(jwtToken.getClientID(), jwtToken.getRealm());
    }

    public ClientApplication createClientApplication(final Object o) {
        return new ClientApplicationImpl((AMIdentity) o);
    }

    public TokenManager getTokenManager() {
        return TokenManagerImpl.getInstance();
    }

    public OAuth2ProviderSettings getOAuth2ProviderSettings(final Request request) {
        final String deploymentUrl = OAuth2Utils.getDeploymentURL(request);
        final String realm = OAuth2Utils.getRealm(request);
        return OAuth2ProviderSettingsHolder.getInstance(deploymentUrl, realm);
    }

    public OAuth2ProviderSettings getOAuth2ProviderSettings(final String deploymentUrl, final String realm) {
        return OAuth2ProviderSettingsHolder.getInstance(deploymentUrl, realm);
    }

    public ServerAuthorizer getServerAuthorizer() {
        return new OpenAMServerAuthorizer();
    }

    public SecretVerifier getSecretVerifier() {
        return new OpenAMIdentityVerifier();
    }

    public KeyPair getServerKeyPair(Request request) {
        return OAuth2Utils.getServerKeyPair(request);
    }

    public KeyPair getServerKeyPair(HttpServletRequest request) {
        return OAuth2Utils.getServerKeyPair(request);
    }

    public OAuth2TokenStore getTokenStore() {
        return InjectorHolder.getInstance(DefaultOAuthTokenStoreImpl.class);
    }

    public String getSSOCookieName() {
        return SystemProperties.get("com.iplanet.am.cookie.name");
    }

    public Class<? extends Scope> getScopePluginClass(final String realm) throws OAuthProblemException {

        String pluginClass = null;
        try {
            SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
            ServiceConfigManager mgr = new ServiceConfigManager(token, OAuth2Constants.OAuth2ProviderService.NAME, OAuth2Constants.OAuth2ProviderService.VERSION);
            ServiceConfig scm = mgr.getOrganizationConfig(realm, null);
            Map<String, Set<String>> attrs = scm.getAttributes();
            pluginClass = attrs.get(OAuth2Constants.OAuth2ProviderService.SCOPE_PLUGIN_CLASS).iterator().next();
        } catch (Exception e) {
            OAuth2Utils.DEBUG.error("ValidationServerResource::Unable to get plugin class", e);
            throw new OAuthProblemException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE.getCode(),
                    "Service unavailable", "Could not create underlying storage", null);
        }

        try {
            return Class.forName(pluginClass).asSubclass(Scope.class);
        } catch (ClassNotFoundException e) {
            throw OAuthProblemException.handleOAuthProblemException(e.getMessage());
        }
    }

    public boolean savedConsent(String userid, String clientId, Set<String> scopes, Request request) {
        OAuth2ProviderSettings settings = OAuth2Utils.getSettingsProvider(request);
        String attribute = settings.getSavedConsentAttributeName();

        AMIdentity id = OAuth2Utils.getIdentity(userid, OAuth2Utils.getRealm(request));
        Set<String> attributeSet = null;

        if (id != null) {
            try {
                attributeSet = id.getAttribute(attribute);
            } catch (Exception e) {
                OAuth2Utils.DEBUG.error("AuthorizeServerResource.saveConsent(): Unable to get profile attribute", e);
                return false;
            }
        }

        //check the values of the attribute set vs the scope and client requested
        //attribute set is in the form of client_id|scope1 scope2 scope3
        for (String consent : attributeSet) {
            int loc = consent.indexOf(" ");
            String consentClientId = consent.substring(0, loc);
            String[] scopesArray = null;
            if (loc + 1 < consent.length()) {
                scopesArray = consent.substring(loc + 1, consent.length()).split(" ");
            }
            Set<String> consentScopes = null;
            if (scopesArray != null && scopesArray.length > 0) {
                consentScopes = new HashSet<String>(Arrays.asList(scopesArray));
            } else {
                consentScopes = new HashSet<String>();
            }

            //if both the client and the scopes are identical to the saved consent then approve
            if (clientId.equals(consentClientId) && scopes.equals(consentScopes)) {
                return true;
            }
        }

        return false;
    }

    public void saveConsent(String userId, String clientId, String scopes, Request request) {
        AMIdentity id = OAuth2Utils.getIdentity(userId, OAuth2Utils.getRealm(request));
        OAuth2ProviderSettings settings = OAuth2Utils.getSettingsProvider(request);
        String consentAttribute = settings.getSavedConsentAttributeName();
        try {

            //get the current set of consents and add our new consent to it.
            Set<String> consents = new HashSet<String>(id.getAttribute(consentAttribute));
            StringBuilder sb = new StringBuilder();
            if (scopes == null || scopes.isEmpty()) {
                sb.append(clientId.trim()).append(" ");
            } else {
                sb.append(clientId.trim()).append(" ").append(scopes.trim());
            }
            consents.add(sb.toString());

            //update the user profile with our new consent settings
            Map<String, Set<String>> attrs = new HashMap<String, Set<String>>();
            attrs.put(consentAttribute, consents);
            id.setAttributes(attrs);
            id.store();
        } catch (Exception e) {
            OAuth2Utils.DEBUG.error("AuthorizeServerResource.saveConsent(): Unable to save consent ", e);
        }
    }

    public Verifier getUserVerifier() {
        return new UserIdentityVerifier();
    }

    public ClientVerifier getClientVerifier() {
        return new ClientVerifierImpl();
    }

    public Class<? extends ServerResource> getConnectionClientRegistration() {
        return ConnectClientRegistration.class;
    }

    public Class<? extends ServerResource> getEndSession() {
        return EndSession.class;
    }

    public Class<? extends ServerResource> getOpenIDConnectionDiscovery() {
        return OpenIDConnectDiscovery.class;
    }

    public ClientDAO newClientDAO(String realm, Request request, Object token) {
        return new OpenAMClientDAO(realm, request, (SSOToken) token);
    }
}
