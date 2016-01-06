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

import javax.inject.Inject;
import javax.security.auth.Subject;

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
import org.forgerock.oauth2.restlet.RestletOAuth2Request;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.restlet.Request;

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

    private final Map<String, UmaProviderSettingsImpl> providerSettingsMap = new HashMap<>();
    private final OAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory;
    private final UmaTokenStoreFactory tokenStoreFactory;
    private final JacksonRepresentationFactory jacksonRepresentationFactory;

    /**
     * Contructs a new UmaProviderSettingsFactory.
     *  @param oAuth2ProviderSettingsFactory An instance of the OAuth2ProviderSettingFactory.
     * @param tokenStoreFactory An instance of the UmaTokenStoreFactory.
     * @param jacksonRepresentationFactory The factory for {@code JacksonRepresentation} instances.
     */
    @Inject
    UmaProviderSettingsFactory(OAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory,
            UmaTokenStoreFactory tokenStoreFactory, JacksonRepresentationFactory jacksonRepresentationFactory) {
        this.oAuth2ProviderSettingsFactory = oAuth2ProviderSettingsFactory;
        this.tokenStoreFactory = tokenStoreFactory;
        this.jacksonRepresentationFactory = jacksonRepresentationFactory;
    }

    /**
     * Gets a UmaProviderSettings instance.
     *
     * @param req The Restlet request.
     * @return A UmaProviderSettings instance.
     */
    UmaProviderSettings get(Request req) throws NotFoundException {
        return get(new RestletOAuth2Request(jacksonRepresentationFactory, req));
    }

    public UmaProviderSettings get(OAuth2Request request) throws NotFoundException {
        return get(request.<String>getParameter(RestletRealmRouter.REALM));
    }

    /**
     * <p>Gets the instance of the UmaProviderSettings.</p>
     *
     * <p>Cache each provider settings on the realm it was created for.</p>
     *
     * @param realm The realm.
     * @return The OAuth2ProviderSettings instance.
     */
    public UmaProviderSettings get(String realm) throws NotFoundException {
        synchronized (providerSettingsMap) {
            UmaProviderSettingsImpl providerSettings = providerSettingsMap.get(realm);
            if (providerSettings == null) {
                OAuth2ProviderSettings oAuth2ProviderSettings = oAuth2ProviderSettingsFactory.get(realm);
                providerSettings = getUmaProviderSettings(realm, oAuth2ProviderSettings);
            }
            return providerSettings;
        }
    }

    private UmaProviderSettingsImpl getUmaProviderSettings(String realm, OAuth2ProviderSettings oAuth2ProviderSettings) throws NotFoundException {
        UmaProviderSettingsImpl providerSettings;UmaTokenStore tokenStore = tokenStoreFactory.create(realm);
        providerSettings = new UmaProviderSettingsImpl(realm, tokenStore, oAuth2ProviderSettings);
        providerSettingsMap.put(realm, providerSettings);
        return providerSettings;
    }

    static final class UmaProviderSettingsImpl extends UmaSettingsImpl implements UmaProviderSettings {

        private final Debug logger = Debug.getInstance("UmaProvider");
        private final String realm;
        private final UmaTokenStore tokenStore;

        UmaProviderSettingsImpl(String realm, UmaTokenStore tokenStore, OAuth2ProviderSettings oAuth2ProviderSettings)
                throws NotFoundException {
            super(realm);
            this.realm = realm;
            this.tokenStore = tokenStore;
        }

        @Override
        public boolean isEnabled() {
            try {
                return hasConfig(realm);
            } catch (Exception e) {
                logger.message("Could not access realm config", e);
                return false;
            }
        }

        @Override
        public Evaluator getPolicyEvaluator(Subject subject, String clientId) throws EntitlementException {
            return new Evaluator(subject, clientId);
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
