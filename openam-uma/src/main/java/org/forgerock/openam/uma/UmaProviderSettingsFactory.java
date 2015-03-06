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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.uma;

import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Singleton;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.restlet.RestletOAuth2Request;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.utils.RealmNormaliser;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

/**
 * <p>A factory for creating/retrieving UmaProviderSettings instances.</p>
 *
 * <p>It is up to the implementation to provide caching of UmaProviderSettings instance if it wants to supported
 * multiple UMA providers.</p>
 *
 * @since 13.0.0
 */
@Singleton
public class UmaProviderSettingsFactory {

    private final Map<String, UmaProviderSettingsImpl> providerSettingsMap =
            new HashMap<String, UmaProviderSettingsImpl>();
    private final RealmNormaliser realmNormaliser;
    private final OAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory;
    private final UmaTokenStoreFactory tokenStoreFactory;
    private final BaseURLProviderFactory baseURLProviderFactory;

    /**
     * Contructs a new UmaProviderSettingsFactory.
     *
     * @param realmNormaliser An instance of the RealmNormaliser.
     * @param oAuth2ProviderSettingsFactory An instance of the OAuth2ProviderSettingFactory.
     * @param tokenStoreFactory An instance of the UmaTokenStoreFactory.
     */
    @Inject
    UmaProviderSettingsFactory(RealmNormaliser realmNormaliser,
            OAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory, UmaTokenStoreFactory tokenStoreFactory,
            BaseURLProviderFactory baseURLProviderFactory) {
        this.realmNormaliser = realmNormaliser;
        this.oAuth2ProviderSettingsFactory = oAuth2ProviderSettingsFactory;
        this.tokenStoreFactory = tokenStoreFactory;
        this.baseURLProviderFactory = baseURLProviderFactory;
    }

    /**
     * Gets a UmaProviderSettings instance.
     *
     * @param realm The realm.
     * @return A UmaProviderSettings instance.
     * @throws java.lang.IllegalStateException if the realm has not been initialised yet.
     */
    UmaProviderSettings get(String realm) {
        if (providerSettingsMap.containsKey(realm)) {
            return providerSettingsMap.get(realm);
        }
        throw new IllegalStateException("Provider Settings being accessed by realm but does not exist");
    }

    /**
     * Gets a UmaProviderSettings instance.
     *
     * @param req The Restlet request.
     * @return A UmaProviderSettings instance.
     */
    UmaProviderSettings get(Request req) throws NotFoundException {
        return get(new RestletOAuth2Request(req));
    }

    public UmaProviderSettings get(OAuth2Request request) throws NotFoundException {
        String realm = request.getParameter("realm");
        return getInstance(request, realmNormaliser.normalise(realm));
    }

    /**
     * <p>Gets the instance of the UmaProviderSettings.</p>
     *
     * <p>Cache each provider settings on the realm it was created for.</p>
     *
     * @param request The OAuth2Request instance.
     * @param realm The realm.
     * @return The OAuth2ProviderSettings instance.
     */
    private UmaProviderSettings getInstance(OAuth2Request request, String realm) throws NotFoundException {
        synchronized (providerSettingsMap) {
            UmaProviderSettingsImpl providerSettings = providerSettingsMap.get(realm);
            if (providerSettings == null) {
                OAuth2ProviderSettings oAuth2ProviderSettings = oAuth2ProviderSettingsFactory.get(request);
                HttpServletRequest httpReq = ServletUtils.getRequest(request.<Request>getRequest());
                String baseUrlPattern = baseURLProviderFactory.get(realm).getURL(httpReq);
                UmaTokenStore tokenStore = tokenStoreFactory.create(realm);
                providerSettings = new UmaProviderSettingsImpl(realm, baseUrlPattern, tokenStore,
                        oAuth2ProviderSettings);
                providerSettingsMap.put(realm, providerSettings);
            }
            return providerSettings;
        }
    }

    static final class UmaProviderSettingsImpl extends UmaSettingsImpl implements UmaProviderSettings {

        private final Debug logger = Debug.getInstance("UmaProvider");
        private final String realm;
        private final String deploymentUrl;
        private final UmaTokenStore tokenStore;
        private final OAuth2ProviderSettings oAuth2ProviderSettings;

        UmaProviderSettingsImpl(String realm, String contextDeploymentUri,
                UmaTokenStore tokenStore, OAuth2ProviderSettings oAuth2ProviderSettings) throws NotFoundException {
            super(realm);
            this.realm = realm;
            this.deploymentUrl = contextDeploymentUri;
            this.tokenStore = tokenStore;
            this.oAuth2ProviderSettings = oAuth2ProviderSettings;
        }

        private boolean exists() {
            try {
                return hasConfig(realm);
            } catch (Exception e) {
                logger.message("Could not access realm config", e);
                return false;
            }
        }

        @Override
        public URI getIssuer() throws ServerException {
            return URI.create(oAuth2ProviderSettings.getIssuer());
        }

        private String getUmaBaseUrl() {
            return getBaseUrl("/uma");
        }

        private String getBaseUrl(String context) {
            String uri = deploymentUrl + context + realm;
            if (uri.endsWith("/")) {
                uri = uri.substring(0, uri.length() - 1);
            }
            return uri;
        }

        @Override
        public URI getTokenEndpoint() {
            return URI.create(oAuth2ProviderSettings.getTokenEndpoint());
        }

        @Override
        public URI getAuthorizationEndpoint() {
            return URI.create(oAuth2ProviderSettings.getAuthorizationEndpoint());
        }

        @Override
        public URI getTokenIntrospectionEndpoint() {
            return URI.create(oAuth2ProviderSettings.getIntrospectionEndpoint());
        }

        @Override
        public URI getResourceSetRegistrationEndpoint() {
            return URI.create(oAuth2ProviderSettings.getResourceSetRegistrationEndpoint());
        }

        @Override
        public URI getPermissionRegistrationEndpoint() {
            return URI.create(getUmaBaseUrl() + "/permission_request");
        }

        @Override
        public URI getRPTEndpoint() {
            return URI.create(getUmaBaseUrl() + "/authz_request");
        }

        @Override
        public URI getDynamicClientEndpoint() {
            return URI.create(oAuth2ProviderSettings.getClientRegistrationEndpoint());
        }

        /**
         * OpenAM currently does not support requesting party claims so no endpoint exists.
         *
         * @return {@code null}.
         */
        @Override
        public URI getRequestingPartyClaimsEndpoint() {
            return null;
        }

        @Override
        public Evaluator getPolicyEvaluator(Subject subject) throws EntitlementException {
            return new Evaluator(subject);
        }

        @Override
        public UmaTokenStore getUmaTokenStore() {
            return tokenStore;
        }
    }
}
