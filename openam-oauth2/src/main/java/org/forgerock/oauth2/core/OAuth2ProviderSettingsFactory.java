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

package org.forgerock.oauth2.core;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;

import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.OAuth2ProviderNotFoundException;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.oauth2.CookieExtractor;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuth2RealmResolver;
import org.forgerock.openam.oauth2.OAuthProblemException;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;

/**
 * A factory for creating/retrieving OAuth2ProviderSettings instances.
 * <br/>
 * It is up to the implementation to provide caching of OAuth2ProviderSettings instance if it wants to supported
 * multiple OAuth2 providers.
 *
 * @since 12.0.0
 */
@Singleton
public class OAuth2ProviderSettingsFactory implements ServiceListener {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final Map<String, OAuth2ProviderSettings> providerSettingsMap = new HashMap<>();
    private final CookieExtractor cookieExtractor;
    private final ResourceSetStoreFactory resourceSetStoreFactory;
    private final OAuth2RealmResolver realmResolver;

    /**
     * Constructs a new OpenAMOAuth2ProviderSettingsFactory.
     *
     * @param cookieExtractor An instance of the CookieExtractor.
     * @param resourceSetStoreFactory An instance of the ResourceSetStoreFactory.
     * @param realmResolver An instance of the RealmResolver
     */
    @Inject
    public OAuth2ProviderSettingsFactory(CookieExtractor cookieExtractor,
            ResourceSetStoreFactory resourceSetStoreFactory, OAuth2RealmResolver realmResolver) {
        this.cookieExtractor = cookieExtractor;
        this.resourceSetStoreFactory = resourceSetStoreFactory;
        this.realmResolver = realmResolver;
        addServiceListener();
    }

    private void addServiceListener() {
        try {
            final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
            final ServiceConfigManager serviceConfigManager = new ServiceConfigManager(token,
                    OAuth2Constants.OAuth2ProviderService.NAME, OAuth2Constants.OAuth2ProviderService.VERSION);
            if (serviceConfigManager.addListener(this) == null) {
                logger.error("Could not add listener to ServiceConfigManager instance. OAuth2 provider service " +
                        "removals will not be dynamically updated");
            }
        } catch (Exception e) {
            String message = "OAuth2Utils::Unable to construct ServiceConfigManager: " + e;
            logger.error(message, e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    /**
     * Gets a OAuth2ProviderSettings instance.
     *
     * @param request The OAuth2 request.
     * @return A OAuth2ProviderSettings instance.
     */
    public OAuth2ProviderSettings get(OAuth2Request request) throws NotFoundException {
        return get(realmResolver.resolveFrom(request));
    }

    /**
     * Gets the instance of the OAuth2ProviderSettings.
     *
     * @param context The context that can be used to obtain the base deployment url.
     * @return The OAuth2ProviderSettings instance.
     */
    public OAuth2ProviderSettings get(Context context) throws NotFoundException {
        return get(realmResolver.resolveFrom(context));
    }

    /**
     * Gets the instance of the OAuth2ProviderSettings.
     *
     * @param realm The realm.
     * @return The OAuth2ProviderSettings instance.
     */
    public OAuth2ProviderSettings get(String realm) throws NotFoundException {
        Reject.ifNull(realm, "realm cannot be null");
        return getProviderSettings(realm);
    }

    private OAuth2ProviderSettings getProviderSettings(String realm) throws NotFoundException {
        synchronized (providerSettingsMap) {
            OAuth2ProviderSettings providerSettings = providerSettingsMap.get(realm);
            if (providerSettings == null) {
                ResourceSetStore resourceSetStore = resourceSetStoreFactory.create(realm);
                providerSettings = new OAuth2ProviderSettings(realm, resourceSetStore, cookieExtractor);
                if (providerSettings.exists()) {
                    providerSettingsMap.put(realm, providerSettings);
                } else {
                    throw new OAuth2ProviderNotFoundException("No OpenID Connect provider for realm " + realm);
                }
            }
            return providerSettings;
        }
    }

    @Override
    public void schemaChanged(String serviceName, String version) {

    }

    @Override
    public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent, int type) {

    }

    @Override
    public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
            String serviceComponent, int type) {
        if (type == ServiceListener.REMOVED) {
            String realm = DNMapper.orgNameToRealmName(orgName);
            logger.message("Removing OAuth2 provider for realm {}", realm);
            providerSettingsMap.remove(realm);
        }
    }
}
