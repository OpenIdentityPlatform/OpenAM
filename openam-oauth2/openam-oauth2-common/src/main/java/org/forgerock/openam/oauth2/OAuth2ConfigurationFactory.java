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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import org.forgerock.openam.oauth2.model.ClientApplication;
import org.forgerock.openam.oauth2.model.JWTToken;
import org.forgerock.openam.oauth2.model.TokenManager;
import org.forgerock.openam.oauth2.provider.ClientDAO;
import org.forgerock.openam.oauth2.provider.ClientVerifier;
import org.forgerock.openam.oauth2.provider.OAuth2ProviderSettings;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.openam.oauth2.provider.Scope;
import org.forgerock.openam.oauth2.provider.ServerAuthorizer;
import org.restlet.Request;
import org.restlet.resource.Resource;
import org.restlet.resource.ServerResource;
import org.restlet.security.SecretVerifier;
import org.restlet.security.Verifier;

import javax.servlet.http.HttpServletRequest;
import java.security.KeyPair;
import java.util.Set;

public interface OAuth2ConfigurationFactory {

    public static enum Holder {
        INSTANCE;

        private OAuth2ConfigurationFactory configurationFactory;

        public static void setConfigurationFactory(final OAuth2ConfigurationFactory configurationFactory) {
            INSTANCE.configurationFactory = configurationFactory;
        }

        public static OAuth2ConfigurationFactory getConfigurationFactory() {
            return INSTANCE.configurationFactory;
        }
    }

    Object getClientIdentity(JWTToken jwtToken);

    ClientApplication createClientApplication(final Object o);

    TokenManager getTokenManager();

    OAuth2ProviderSettings getOAuth2ProviderSettings(final Request request);

    ServerAuthorizer getServerAuthorizer();

    SecretVerifier getSecretVerifier();

    KeyPair getServerKeyPair(final Request request);

    KeyPair getServerKeyPair(final HttpServletRequest request);

    OAuth2TokenStore getTokenStore();

    String getSSOCookieName();

    Class<? extends Scope> getScopePluginClass(final String realm);

    boolean savedConsent(final String userid, final String clientId, final Set<String> scopes, final Request request);

    void saveConsent(final String userId, final String clientId, final String scopes, final Request request);

    Verifier getUserVerifier();

    ClientVerifier getClientVerifier();

    Class<? extends ServerResource> getConnectionClientRegistration();

    Class<? extends ServerResource>getEndSession();

    Class<? extends ServerResource>getOpenIDConnectionDiscovery();

    ClientDAO newClientDAO(String realm, Request request, Object token);
}
