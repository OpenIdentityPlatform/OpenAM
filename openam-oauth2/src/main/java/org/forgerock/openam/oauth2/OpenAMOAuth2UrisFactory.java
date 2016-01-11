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
 */

package org.forgerock.openam.oauth2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2Uris;
import org.forgerock.oauth2.core.OAuth2UrisFactory;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.core.RealmInfo;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.services.baseurl.InvalidBaseUrlException;
import org.forgerock.services.context.Context;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

/**
 * A factory for creating/retrieving OAuth2Uris instances.
 */
public class OpenAMOAuth2UrisFactory implements OAuth2UrisFactory<RealmInfo> {

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
    OpenAMOAuth2UrisFactory(OAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory,
            BaseURLProviderFactory baseURLProviderFactory) {
        this.oAuth2ProviderSettingsFactory = oAuth2ProviderSettingsFactory;
        this.baseURLProviderFactory = baseURLProviderFactory;
    }

    /**
     * Gets a OAuth2Uris instance.
     *
     * @param request TODO
     * @return A UmaProviderSettings instance.
     */
    @Override
    public OAuth2Uris get(final OAuth2Request request) throws NotFoundException, ServerException {
        RealmInfo realmInfo = request.getParameter(RestletRealmRouter.REALM_INFO);
        HttpServletRequest req = ServletUtils.getRequest(request.<Request>getRequest());
        return get(req, realmInfo);
    }

    /**
     * Gets a OAuth2Uris instance.
     *
     * @param context TODO
     * @param realmInfo The realm information.
     * @return A UmaProviderSettings instance.
     */
    @Override
    public OAuth2Uris get(Context context, RealmInfo realmInfo) throws NotFoundException, ServerException {
        String absoluteRealm = realmInfo.getAbsoluteRealm();
        BaseURLProvider baseURLProvider = baseURLProviderFactory.get(absoluteRealm);
        String baseUrl;
        try {
            baseUrl = baseURLProvider.getRealmURL(context.asContext(HttpContext.class), "/oauth2", absoluteRealm);
        } catch (InvalidBaseUrlException e) {
            throw new ServerException("Configuration error");
        }
        return get(absoluteRealm, baseUrl);
    }

    @Override
    public OAuth2Uris get(HttpServletRequest request, RealmInfo realmInfo) throws NotFoundException, ServerException {
        String absoluteRealm = realmInfo.getAbsoluteRealm();
        BaseURLProvider baseURLProvider = baseURLProviderFactory.get(absoluteRealm);
        String baseUrl;
        try {
            baseUrl = baseURLProvider.getRealmURL(request, "/oauth2", absoluteRealm);
        } catch (InvalidBaseUrlException e) {
            throw new ServerException("Configuration error");
        }
        return get(absoluteRealm, baseUrl);
    }

    private OAuth2Uris get(String absoluteRealm, String baseUrlPattern) throws NotFoundException {
        OAuth2Uris uris = urisMap.get(baseUrlPattern);
        if (uris == null) {
            uris = getOAuth2Uris(absoluteRealm, baseUrlPattern);
        }
        return uris;
    }

    private synchronized OAuth2Uris getOAuth2Uris(String absoluteRealm, String baseUrlPattern)
            throws NotFoundException {
        OAuth2Uris uris = urisMap.get(baseUrlPattern);
        if (uris != null) {
            return uris;
        }
        OAuth2ProviderSettings oAuth2ProviderSettings = oAuth2ProviderSettingsFactory.get(absoluteRealm);
        uris = new OAuth2UrisImpl(baseUrlPattern, absoluteRealm, oAuth2ProviderSettings);
        urisMap.put(baseUrlPattern, uris);
        return uris;
    }

    static final class OAuth2UrisImpl implements OAuth2Uris {

        private final String deploymentUrl;
        private final String absoluteRealm;
        private final OAuth2ProviderSettings oAuth2ProviderSettings;
        private final String baseUrl;

        OAuth2UrisImpl(String deploymentUrl, String absoluteRealm, OAuth2ProviderSettings oAuth2ProviderSettings) {
            this.deploymentUrl = deploymentUrl;
            this.absoluteRealm = absoluteRealm;
            this.oAuth2ProviderSettings = oAuth2ProviderSettings;
            this.baseUrl = deploymentUrl;
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
    }
}
