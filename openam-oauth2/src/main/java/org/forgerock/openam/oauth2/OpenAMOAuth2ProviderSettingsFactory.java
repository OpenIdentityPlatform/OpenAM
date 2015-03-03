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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.utils.RealmNormaliser;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

/**
 * A factory for creating/retrieving OpenAMOAuth2ProviderSettings instances.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMOAuth2ProviderSettingsFactory implements OAuth2ProviderSettingsFactory {

    private final Map<String, OAuth2ProviderSettings> providerSettingsMap = new HashMap<String, OAuth2ProviderSettings>();
    private final RealmNormaliser realmNormaliser;
    private final CookieExtractor cookieExtractor;
    private final ResourceSetStoreFactory resourceSetStoreFactory;
    private final BaseURLProviderFactory baseURLProviderFactory;

    /**
     * Contructs a new OpenAMOAuth2ProviderSettingsFactory.
     *
     * @param realmNormaliser An instance of the RealmNormaliser.
     * @param cookieExtractor An instance of the CookieExtractor.
     * @param resourceSetStoreFactory An instance of the ResourceSetStoreFactory.
     */
    @Inject
    public OpenAMOAuth2ProviderSettingsFactory(RealmNormaliser realmNormaliser, CookieExtractor cookieExtractor,
            ResourceSetStoreFactory resourceSetStoreFactory, BaseURLProviderFactory baseURLProviderFactory) {
        this.realmNormaliser = realmNormaliser;
        this.cookieExtractor = cookieExtractor;
        this.resourceSetStoreFactory = resourceSetStoreFactory;
        this.baseURLProviderFactory = baseURLProviderFactory;
    }

    /**
     * {@inheritDoc}
     */
    public OAuth2ProviderSettings get(OAuth2Request request) throws NotFoundException {
        final String realm = realmNormaliser.normalise(request.<String>getParameter("realm"));
        final HttpServletRequest req = ServletUtils.getRequest(request.<Request>getRequest());
        String baseUrlPattern = baseURLProviderFactory.get(realm).getURL(req);
        return getInstance(realm, baseUrlPattern);
    }

    /**
     * Only to be used internally by AM.
     *
     * @param realm The realm.
     * @return The OAuth2ProviderSettings instance.
     */
    public OAuth2ProviderSettings get(String realm) throws NotFoundException {
        return getInstance(realmNormaliser.normalise(realm), null);
    }

    /**
     * Gets the instance of the OAuth2ProviderSettings.
     * <br/>
     * Cache each provider settings on the realm it was created for.
     *
     * @param realm The realm.
     * @param baseDeploymentUri The base deployment url.
     * @return The OAuth2ProviderSettings instance.
     */
    private OAuth2ProviderSettings getInstance(String realm, String baseDeploymentUri)
            throws NotFoundException {
        synchronized (providerSettingsMap) {
            OAuth2ProviderSettings providerSettings = providerSettingsMap.get(realm);
            if (providerSettings == null) {
                ResourceSetStore resourceSetStore = resourceSetStoreFactory.create(realm);
                providerSettings = new OpenAMOAuth2ProviderSettings(realm, baseDeploymentUri, resourceSetStore,
                        cookieExtractor);
                if (providerSettings.exists()) {
                    providerSettingsMap.put(realm, providerSettings);
                } else {
                    throw new NotFoundException("No OpenID Connect provider for realm " + realm);
                }
            }
            return providerSettings;
        }
    }
}
