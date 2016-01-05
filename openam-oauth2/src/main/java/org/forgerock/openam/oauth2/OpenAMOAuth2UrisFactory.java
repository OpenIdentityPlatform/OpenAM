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

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.services.context.Context;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

/**
 * A factory for creating/retrieving OAuth2Uris instances.
 */
public class OpenAMOAuth2UrisFactory implements OAuth2UrisFactory<RealmInfo> {

    private final Map<RealmInfo, OAuth2Uris> providerSettingsMap = new ConcurrentHashMap<>();
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
    public OAuth2Uris get(final OAuth2Request request) throws NotFoundException {
        RealmInfo realmInfo = request.getParameter(RestletRealmRouter.REALM_INFO);
        HttpServletRequest req = ServletUtils.getRequest(request.<Request>getRequest());
        String baseUrlPattern = baseURLProviderFactory.get(realmInfo.getAbsoluteRealm()).getURL(req);
        return get(realmInfo, baseUrlPattern);
    }

    /**
     * Gets a OAuth2Uris instance.
     *
     * @param context TODO
     * @param realmInfo The realm information.
     * @return A UmaProviderSettings instance.
     */
    @Override
    public OAuth2Uris get(Context context, RealmInfo realmInfo) throws NotFoundException {
        String baseUrlPattern = baseURLProviderFactory.get(realmInfo.getAbsoluteRealm()).getURL(context.asContext(HttpContext.class));
        return get(realmInfo, baseUrlPattern);
    }

    @Override
    public OAuth2Uris get(HttpServletRequest request, RealmInfo realmInfo) throws NotFoundException {
        String baseUrlPattern = baseURLProviderFactory.get(realmInfo.getAbsoluteRealm()).getURL(request);
        return get(realmInfo, baseUrlPattern);
    }

    private OAuth2Uris get(RealmInfo realmInfo, String baseUrlPattern) throws NotFoundException {
        OAuth2Uris providerSettings = providerSettingsMap.get(realmInfo);
        if (providerSettings == null) {
            OAuth2ProviderSettings oAuth2ProviderSettings = oAuth2ProviderSettingsFactory.get(realmInfo.getAbsoluteRealm());
            providerSettings = getOAuth2Uris(realmInfo, baseUrlPattern, oAuth2ProviderSettings);
        }
        return providerSettings;
    }

    private OAuth2Uris getOAuth2Uris(RealmInfo realmInfo, String baseUrlPattern,
            OAuth2ProviderSettings oAuth2ProviderSettings) throws NotFoundException {
        OAuth2Uris providerSettings = new OAuth2UrisImpl(baseUrlPattern, realmInfo, oAuth2ProviderSettings);
        providerSettingsMap.put(realmInfo, providerSettings);
        return providerSettings;
    }

    static final class OAuth2UrisImpl implements OAuth2Uris {

        private final String deploymentUrl;
        private final RealmInfo realmInfo;
        private final OAuth2ProviderSettings oAuth2ProviderSettings;
        private final String baseUrl;

        OAuth2UrisImpl(String deploymentUrl, RealmInfo realmInfo, OAuth2ProviderSettings oAuth2ProviderSettings) {
            this.deploymentUrl = deploymentUrl;
            this.realmInfo = realmInfo;
            this.oAuth2ProviderSettings = oAuth2ProviderSettings;
            String baseUrl = deploymentUrl + "/oauth2" + realmInfo.getRealmSubPath();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
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
            return deploymentUrl + "/XUI/?realm=" + realmInfo.getAbsoluteRealm() + "#uma/share/" + resourceSetId;
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
