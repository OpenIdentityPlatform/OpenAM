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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;

import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.core.RealmInfo;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

/**
 * A factory for creating/retrieving OpenAMOAuth2ProviderSettings instances.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMOAuth2ProviderSettingsFactory implements OAuth2ProviderSettingsFactory<RealmInfo>, ServiceListener {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final Map<RealmInfo, OAuth2ProviderSettings> providerSettingsMap = new HashMap<>();
    private final RealmNormaliser realmNormaliser;
    private final CookieExtractor cookieExtractor;
    private final ResourceSetStoreFactory resourceSetStoreFactory;
    private final BaseURLProviderFactory baseURLProviderFactory;

    /**
     * Constructs a new OpenAMOAuth2ProviderSettingsFactory.
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
     * {@inheritDoc}
     */
    public OAuth2ProviderSettings get(OAuth2Request request) throws NotFoundException {
        final OpenAMAccessToken accessToken = (OpenAMAccessToken) request.getToken(AccessToken.class);
        final RealmInfo realmInfo;
        if (accessToken != null) {
            realmInfo = new RealmInfo(accessToken.getRealm(), null, null);
        } else {
            realmInfo = request.getParameter(RestletRealmRouter.REALM_INFO);
        }
        final HttpServletRequest req = ServletUtils.getRequest(request.<Request>getRequest());
        return get(realmInfo, req);
    }

    /**
     * Cache each provider settings on the realm it was created for.
     * {@inheritDoc}
     */
    public OAuth2ProviderSettings get(RealmInfo realmInfo) throws NotFoundException {
        OAuth2ProviderSettings providerSettings = providerSettingsMap.get(realmInfo);
        if (providerSettings == null) {
            throw new IllegalStateException("Realm provider settings have not yet been constructed.");
        }
        return providerSettings;
    }

    /**
     * Cache each provider settings on the realm it was created for.
     * {@inheritDoc}
     */
    public OAuth2ProviderSettings get(RealmInfo realmInfo, HttpServletRequest req) throws NotFoundException {
        Reject.ifNull(realmInfo, "Realm cannot be null");
        Reject.ifNull(req, "Request cannot be null");
        String baseDeploymentUri = baseURLProviderFactory.get(realmInfo.getAbsoluteRealm()).getURL(req);
        return getProviderSettings(realmInfo, baseDeploymentUri);
    }

    @Override
    public OAuth2ProviderSettings get(Context context) throws NotFoundException {
        Reject.ifFalse(context.containsContext(RealmContext.class), "Must contain a RealmContext cannot be null");
        Reject.ifNull(context, "Context cannot be null");
        String realm = RealmContext.getRealm(context);
        RealmInfo realmInfo = RealmContext.asRealmInfo(context);
        String baseDeploymentUri = baseURLProviderFactory.get(realm).getURL(context.asContext(HttpContext.class));
        return getProviderSettings(realmInfo, baseDeploymentUri);
    }

    @Override
    public OAuth2ProviderSettings get(Context context, RealmInfo overrideRealmInfo) throws NotFoundException {
        Reject.ifNull(context, "Context cannot be null");
        String baseDeploymentUri = baseURLProviderFactory.get(overrideRealmInfo.getAbsoluteRealm()).getURL(context.asContext(HttpContext.class));
        return getProviderSettings(overrideRealmInfo, baseDeploymentUri);
    }

    private OAuth2ProviderSettings getProviderSettings(RealmInfo realmInfo, String baseDeploymentUri) throws NotFoundException {
        synchronized (providerSettingsMap) {
            OAuth2ProviderSettings providerSettings = providerSettingsMap.get(realmInfo);
            if (providerSettings == null) {
                ResourceSetStore resourceSetStore = resourceSetStoreFactory.create(realmInfo.getAbsoluteRealm());
                providerSettings = new OpenAMOAuth2ProviderSettings(realmInfo, baseDeploymentUri, resourceSetStore,
                        cookieExtractor);
                if (providerSettings.exists()) {
                    providerSettingsMap.put(realmInfo, providerSettings);
                } else {
                    throw new NotFoundException("No OpenID Connect provider for realm " + realmInfo.getAbsoluteRealm());
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
            final String realm = DNMapper.orgNameToRealmName(orgName);
            logger.message("Removing OAuth2 provider for realm {}", realm);
            providerSettingsMap.remove(realm);
        }
    }
}
