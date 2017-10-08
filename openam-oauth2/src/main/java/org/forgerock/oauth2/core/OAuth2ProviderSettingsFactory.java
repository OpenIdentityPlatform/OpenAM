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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.OAuth2ProviderNotFoundException;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.oauth2.AgentClientRegistration;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuth2RealmResolver;
import org.forgerock.openam.oauth2.OAuthProblemException;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.utils.OpenAMSettings;
import org.forgerock.openam.utils.OpenAMSettingsImpl;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

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
    private final ResourceSetStoreFactory resourceSetStoreFactory;
    private final OAuth2RealmResolver realmResolver;
    private final ServiceConfigManagerFactory serviceConfigManagerFactory;
    private volatile AgentOAuth2ProviderSettings agentProviderSettings = null;

    /**
     * Constructs a new {@code OpenAMOAuth2ProviderSettingsFactory} instance.
     *
     * @param resourceSetStoreFactory Factory for creating {@code ResourceSetStore} instances.
     * @param realmResolver Used to resolve the realm.
     * @param serviceConfigManagerFactory Factory for creating {@code ServiceConfigManager} instances.
     */
    @Inject
    public OAuth2ProviderSettingsFactory(ResourceSetStoreFactory resourceSetStoreFactory,
            OAuth2RealmResolver realmResolver, ServiceConfigManagerFactory serviceConfigManagerFactory) {
        this.resourceSetStoreFactory = resourceSetStoreFactory;
        this.realmResolver = realmResolver;
        this.serviceConfigManagerFactory = serviceConfigManagerFactory;
        addServiceListener();
    }

    private void addServiceListener() {
        try {
            final ServiceConfigManager serviceConfigManager =
                    serviceConfigManagerFactory.create(OAuth2Constants.OAuth2ProviderService.NAME,
                            OAuth2Constants.OAuth2ProviderService.VERSION);
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
     * Gets the instance of the OAuth2ProviderSettings defined in the realm.
     *
     * @param realm The realm.
     * @return The OAuth2ProviderSettings instance.
     */
    public OAuth2ProviderSettings getRealmProviderSettings(String realm) throws NotFoundException {
        return getRealmOAuth2ProviderSettings(realm);
    }

    /**
     * Gets the instance of the OAuth2ProviderSettings.
     *
     * @param context The context that can be used to obtain the base deployment url.
     * @return The OAuth2ProviderSettings instance.
     */
    public OAuth2ProviderSettings get(Context context) throws NotFoundException {
        return getRealmOAuth2ProviderSettings(realmResolver.resolveFrom(context));
    }

    /**
     * Gets the instance of the OAuth2ProviderSettings
     *
     * @param request The OAuth2 request.
     * @return The OAuth2ProviderSettings instance.
     */
    public OAuth2ProviderSettings get(OAuth2Request request) throws NotFoundException {
        return isAgentRequest(request)
                ? getAgentOAuth2ProviderSettings()
                : getRealmOAuth2ProviderSettings(realmResolver.resolveFrom(request));
    }

    private boolean isAgentRequest(OAuth2Request request) {
        return (request.getClientRegistration() instanceof AgentClientRegistration);
    }

    private OAuth2ProviderSettings getAgentOAuth2ProviderSettings() {
        if (agentProviderSettings == null) {
            agentProviderSettings = new AgentOAuth2ProviderSettings();
        }
        return agentProviderSettings;
    }

    private OAuth2ProviderSettings getRealmOAuth2ProviderSettings(String realm) throws OAuth2ProviderNotFoundException {
        Reject.ifNull(realm, "realm cannot be null");
        synchronized (providerSettingsMap) {
            OAuth2ProviderSettings providerSettings = providerSettingsMap.get(realm);
            if (providerSettings == null) {
                ResourceSetStore resourceSetStore = resourceSetStoreFactory.create(realm);
                OpenAMSettings settings = new OpenAMSettingsImpl(OAuth2Constants.OAuth2ProviderService.NAME,
                        OAuth2Constants.OAuth2ProviderService.VERSION);
                providerSettings = new RealmOAuth2ProviderSettings(settings, realm,
                        resourceSetStore, serviceConfigManagerFactory);
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
    public void globalConfigChanged(String serviceName, String version, String groupName,
                                    String serviceComponent, int type) {

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