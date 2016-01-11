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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.uma;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2Uris;
import org.forgerock.oauth2.core.OAuth2UrisFactory;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.restlet.RestletOAuth2Request;
import org.forgerock.openam.core.RealmInfo;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.services.baseurl.InvalidBaseUrlException;
import org.forgerock.services.context.Context;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

import com.google.inject.Singleton;

/**
 * <p>A factory for creating/retrieving UmaUris instances.</p>
 *
 * @since 13.0.0
 */
@Singleton
public class UmaUrisFactory {

    private final Map<String, UmaUris> urisMap = new HashMap<>();
    private final OAuth2UrisFactory<RealmInfo> oAuth2UriFactory;
    private final UmaProviderSettingsFactory umaProviderSettingsFactory;
    private final BaseURLProviderFactory baseURLProviderFactory;
    private final JacksonRepresentationFactory jacksonRepresentationFactory;

    /**
     * Constructs a new UmaUrisFactory.
     *
     * @param oAuth2UriFactory An instance of the OAuth2UrisFactory.
     * @param umaProviderSettingsFactory An instance of the UmaProviderSettingsFactory.
     * @param baseURLProviderFactory An instance of the BaseURLProviderFactory.
     */
    @Inject
    UmaUrisFactory(OAuth2UrisFactory<RealmInfo> oAuth2UriFactory, UmaProviderSettingsFactory umaProviderSettingsFactory,
            BaseURLProviderFactory baseURLProviderFactory, JacksonRepresentationFactory jacksonRepresentationFactory) {
        this.oAuth2UriFactory = oAuth2UriFactory;
        this.umaProviderSettingsFactory = umaProviderSettingsFactory;
        this.baseURLProviderFactory = baseURLProviderFactory;
        this.jacksonRepresentationFactory = jacksonRepresentationFactory;
    }

    /**
     * Gets a UmaUris instance.
     *
     * @param req The Restlet request.
     * @return A UmaProviderSettings instance.
     */
    UmaUris get(Request req) throws NotFoundException, ServerException {
        return get(new RestletOAuth2Request(jacksonRepresentationFactory, req));
    }

    public UmaUris get(OAuth2Request request) throws NotFoundException, ServerException {
        RealmInfo realmInfo = request.getParameter(RestletRealmRouter.REALM_INFO);
        return get(ServletUtils.getRequest(request.<Request>getRequest()), realmInfo);
    }

    /**
     * <p>Gets the instance of the UmaUris.</p>
     *
     * <p>Cache each provider settings on the realm it was created for.</p>
     *
     * @param request The request instance from which the base URL can be deduced.
     * @param realmInfo The realm.
     * @return The OAuth2ProviderSettings instance.
     */
    public UmaUris get(HttpServletRequest request, RealmInfo realmInfo) throws NotFoundException, ServerException {
        String absoluteRealm = realmInfo.getAbsoluteRealm();
        String baseUrl;
        try {
            baseUrl = baseURLProviderFactory.get(absoluteRealm).getRealmURL(request, "/uma", absoluteRealm);
        } catch (InvalidBaseUrlException e) {
            throw new ServerException("Configuration error");
        }
        UmaUris uris = urisMap.get(baseUrl);
        if (uris == null) {
            OAuth2Uris oAuth2Uris = oAuth2UriFactory.get(request, realmInfo);
            uris = getUmaUris(absoluteRealm, oAuth2Uris, baseUrl);
        }
        return uris;
    }

    /**
     * <p>Gets the instance of the UmaProviderSettings.</p>
     *
     * <p>Cache each provider settings on the realm it was created for.</p>
     *
     * @param context The context instance from which the base URL can be deduced.
     * @param realmInfo The realm.
     * @return The OAuth2ProviderSettings instance.
     */
    public UmaUris get(Context context, RealmInfo realmInfo) throws NotFoundException, ServerException {
        String absoluteRealm = realmInfo.getAbsoluteRealm();
        HttpContext httpContext = context.asContext(HttpContext.class);
        String baseUrl;
        try {
            baseUrl = baseURLProviderFactory.get(absoluteRealm).getRealmURL(httpContext, "/uma", absoluteRealm);
        } catch (InvalidBaseUrlException e) {
            throw new ServerException("Configuration error");
        }
        UmaUris uris = urisMap.get(baseUrl);
        if (uris == null) {
            OAuth2Uris oAuth2Uris = oAuth2UriFactory.get(context, realmInfo);
            uris = get(absoluteRealm, oAuth2Uris, baseUrl);
        }
        return uris;
    }

    private UmaUris get(String absoluteRealm, OAuth2Uris oAuth2Uris, String baseUrlPattern) throws NotFoundException {
        UmaUris uris = urisMap.get(baseUrlPattern);
        if (uris == null) {
            uris = getUmaUris(absoluteRealm, oAuth2Uris, baseUrlPattern);
        }
        return uris;
    }

    private synchronized UmaUris getUmaUris(String absoluteRealm, OAuth2Uris oAuth2Uris, String baseUrlPattern)
            throws NotFoundException {
        UmaUris uris = urisMap.get(baseUrlPattern);
        if (uris != null) {
            return uris;
        }
        UmaProviderSettings umaProviderSettings = umaProviderSettingsFactory.get(absoluteRealm);
        UmaUris providerSettings = new UmaUrisImpl(baseUrlPattern, umaProviderSettings, oAuth2Uris);
        urisMap.put(baseUrlPattern, providerSettings);
        return providerSettings;
    }

    static final class UmaUrisImpl implements UmaUris {

        private final UmaProviderSettings umaProviderSettings;
        private final OAuth2Uris oauth2Uris;
        private final String baseUrl;

        UmaUrisImpl(String deploymentUrl, UmaProviderSettings umaProviderSettings,
                OAuth2Uris oauth2Uris) throws NotFoundException {
            this.umaProviderSettings = umaProviderSettings;
            this.oauth2Uris = oauth2Uris;
            this.baseUrl = deploymentUrl;
        }

        @Override
        public boolean isEnabled() {
            return umaProviderSettings.isEnabled();
        }

        @Override
        public URI getIssuer() throws ServerException {
            return URI.create(oauth2Uris.getIssuer());
        }

        @Override
        public URI getTokenEndpoint() {
            return URI.create(oauth2Uris.getTokenEndpoint());
        }

        @Override
        public URI getAuthorizationEndpoint() {
            return URI.create(oauth2Uris.getAuthorizationEndpoint());
        }

        @Override
        public URI getTokenIntrospectionEndpoint() {
            return URI.create(oauth2Uris.getIntrospectionEndpoint());
        }

        @Override
        public URI getResourceSetRegistrationEndpoint() {
            return URI.create(oauth2Uris.getResourceSetRegistrationEndpoint());
        }

        @Override
        public URI getPermissionRegistrationEndpoint() {
            return URI.create(baseUrl + "/permission_request");
        }

        @Override
        public URI getRPTEndpoint() {
            return URI.create(baseUrl + "/authz_request");
        }

        @Override
        public URI getDynamicClientEndpoint() {
            return URI.create(oauth2Uris.getClientRegistrationEndpoint());
        }

        /**
         * OpenAM currently does not support requesting party claims so no endpoint isEnabled.
         *
         * @return {@code null}.
         */
        @Override
        public URI getRequestingPartyClaimsEndpoint() {
            return null;
        }

    }
}
