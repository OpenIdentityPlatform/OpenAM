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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.oauth2;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2Uris;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.services.baseurl.InvalidBaseUrlException;
import org.forgerock.services.context.Context;
import org.restlet.Request;
import org.forgerock.openam.rest.jakarta.servlet.ServletUtils;

/**
 * A factory for creating/retrieving OAuth2Uris instances.
 */
public class OAuth2UrisFactory {

    private final Map<String, OAuth2Uris> urisMap = new ConcurrentHashMap<>();
    private final OAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory;
    private final BaseURLProviderFactory baseURLProviderFactory;

    /**
     * Constructs a new UmaUrlsFactory.
     *
     * @param oAuth2ProviderSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param baseURLProviderFactory An instance of the BaseURLProviderFactory.
     */
    @Inject
    OAuth2UrisFactory(OAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory,
            BaseURLProviderFactory baseURLProviderFactory) {
        this.oAuth2ProviderSettingsFactory = oAuth2ProviderSettingsFactory;
        this.baseURLProviderFactory = baseURLProviderFactory;
    }

    /**
     * Gets a OAuth2Uris instance.
     *
     * @param request The OAuth2 request.
     * @return A UmaProviderSettings instance.
     */
    public OAuth2Uris get(final OAuth2Request request) throws NotFoundException, ServerException {
        Realm realm = request.getParameter(RestletRealmRouter.REALM_OBJECT);
        return get(request, realm);
    }

    /**
     * Gets a OAuth2Uris instance.
     *
     * @param context The request context.
     * @param realm The realm.
     * @return A UmaProviderSettings instance.
     */
    public OAuth2Uris get(Context context, Realm realm) throws NotFoundException, ServerException {
        BaseURLProvider baseURLProvider = baseURLProviderFactory.get(realm.asPath());
        String baseUrl, deploymentUrl;
        try {
            HttpContext httpContext = context.asContext(HttpContext.class);
            baseUrl = baseURLProvider.getRealmURL(httpContext, "/oauth2", realm);
            deploymentUrl = baseURLProvider.getRootURL(httpContext);
        } catch (InvalidBaseUrlException e) {
            throw new ServerException("Configuration error");
        }
        return get(realm.asPath(), baseUrl, deploymentUrl, oAuth2ProviderSettingsFactory.get(context));
    }

    public OAuth2Uris get(OAuth2Request oAuth2Request, Realm realm) throws NotFoundException, ServerException {
        HttpServletRequest request = ServletUtils.getRequest(oAuth2Request.<Request>getRequest());
        BaseURLProvider baseURLProvider = baseURLProviderFactory.get(realm.asPath());
        String baseUrl, deploymentUrl;
        try {
            baseUrl = baseURLProvider.getRealmURL(request, "/oauth2", realm);
            deploymentUrl = baseURLProvider.getRootURL(request);
        } catch (InvalidBaseUrlException e) {
            throw new ServerException("Configuration error");
        }
        return get(realm.asPath(), baseUrl, deploymentUrl, oAuth2ProviderSettingsFactory.get(oAuth2Request));
    }

    private OAuth2Uris get(String absoluteRealm, String baseUrl, String deploymentUrl,
                           OAuth2ProviderSettings providerSettings) throws NotFoundException {
        OAuth2Uris uris = urisMap.get(baseUrl);
        if (uris == null) {
            uris = getOAuth2Uris(absoluteRealm, baseUrl, deploymentUrl, providerSettings);
        }
        return uris;
    }

    private synchronized OAuth2Uris getOAuth2Uris(String absoluteRealm, String baseUrl, String deploymentUrl,
                                                  OAuth2ProviderSettings providerSettings)
            throws NotFoundException {
        OAuth2Uris uris = urisMap.get(baseUrl);
        if (uris != null) {
            return uris;
        }
        uris = new OAuth2UrisImpl(deploymentUrl, absoluteRealm, providerSettings, baseUrl);
        urisMap.put(baseUrl, uris);
        return uris;
    }

    static final class OAuth2UrisImpl implements OAuth2Uris {

        private final String deploymentUrl;
        private final String absoluteRealm;
        private final OAuth2ProviderSettings oAuth2ProviderSettings;
        private final String baseUrl;

        OAuth2UrisImpl(String deploymentUrl, String absoluteRealm, OAuth2ProviderSettings oAuth2ProviderSettings,
                String baseUrl) {
            this.deploymentUrl = deploymentUrl;
            this.absoluteRealm = absoluteRealm;
            this.oAuth2ProviderSettings = oAuth2ProviderSettings;
            this.baseUrl = baseUrl;
        }

        @Override
        public String getIssuer() throws ServerException {
            return baseUrl;
        }

        @Override
        public String getAuthorizationEndpoint() {
            return baseUrl + "/authorize";
        }

        @Override
        public String getTokenEndpoint() {
            return baseUrl + "/" + OAuth2Constants.Params.ACCESS_TOKEN;
        }

        @Override
        public String getIntrospectionEndpoint() {
            return baseUrl + "/introspect";
        }

        @Override
        public String getResourceSetRegistrationPolicyEndpoint(String resourceSetId) {
            return deploymentUrl + "/XUI/?realm=" + absoluteRealm + "#uma/share/" + resourceSetId;
        }

        @Override
        public String getResourceSetRegistrationEndpoint() {
            return baseUrl + "/resource_set";
        }

        @Override
        public String getUserInfoEndpoint() {
            return baseUrl + "/userinfo";
        }

        @Override
        public String getCheckSessionEndpoint() {
            return baseUrl + "/connect/checkSession";
        }

        @Override
        public String getEndSessionEndpoint() {
            return baseUrl + "/connect/endSession";
        }

        @Override
        public String getJWKSUri() throws ServerException {
            String userDefinedJWKUri = oAuth2ProviderSettings.getJWKSUri();
            if (userDefinedJWKUri != null && !userDefinedJWKUri.isEmpty()) {
                return userDefinedJWKUri;
            }
            return baseUrl + "/connect/jwk_uri";
        }

        @Override
        public String getClientRegistrationEndpoint() {
            return baseUrl + "/connect/register";
        }

        @Override
        public String getDeviceAuthorizationEndpoint() {
            return baseUrl + "/device/code";
        }
    }
}
