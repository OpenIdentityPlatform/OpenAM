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

import static org.forgerock.openam.uma.UmaConstants.*;

import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.inject.Singleton;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.restlet.RestletOAuth2Request;
import org.forgerock.openam.oauth2.OAuthProblemException;
import org.forgerock.openam.utils.OpenAMSettingsImpl;
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

    /**
     * Contructs a new UmaProviderSettingsFactory.
     *
     * @param realmNormaliser An instance of the RealmNormaliser.
     * @param oAuth2ProviderSettingsFactory An instance of the OAuth2ProviderSettingFactory.
     * @param tokenStoreFactory An instance of the UmaTokenStoreFactory.
     */
    @Inject
    UmaProviderSettingsFactory(RealmNormaliser realmNormaliser,
            OAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory, UmaTokenStoreFactory tokenStoreFactory) {
        this.realmNormaliser = realmNormaliser;
        this.oAuth2ProviderSettingsFactory = oAuth2ProviderSettingsFactory;
        this.tokenStoreFactory = tokenStoreFactory;
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
                String contextPath = httpReq.getContextPath();
                String requestUrl = httpReq.getRequestURL().toString();
                String baseUrlPattern = requestUrl.substring(0, requestUrl.indexOf(contextPath) + contextPath.length());
                if (baseUrlPattern.endsWith("/")) {
                    baseUrlPattern = baseUrlPattern.substring(0, baseUrlPattern.length() - 1);
                }
                UmaTokenStore tokenStore = tokenStoreFactory.create(realm);
                providerSettings = new UmaProviderSettingsImpl(realm, baseUrlPattern, tokenStore,
                        oAuth2ProviderSettings);
                if (providerSettings.exists()) {
                    providerSettingsMap.put(realm, providerSettings);
                } else {
                    throw new NotFoundException("No UMA provider for realm " + realm);
                }
            }
            return providerSettings;
        }
    }

    private static final class UmaProviderSettingsImpl extends OpenAMSettingsImpl implements UmaProviderSettings {

        private final Debug logger = Debug.getInstance("UmaProvider");
        private final String realm;
        private final String deploymentUrl;
        private final UmaTokenStore tokenStore;
        private final OAuth2ProviderSettings oAuth2ProviderSettings;
        private static final Set<String> supportedGrantTypes = new HashSet<String>();

        static {
            supportedGrantTypes.add("authorization_code");
            supportedGrantTypes.add("implicit");
            supportedGrantTypes.add("password");
            supportedGrantTypes.add("client_credentials");
        }

        public UmaProviderSettingsImpl(String realm, String contextDeploymentUri,
                UmaTokenStore tokenStore, OAuth2ProviderSettings oAuth2ProviderSettings) {
            super(SERVICE_NAME, SERVICE_VERSION);
            this.realm = realm;
            this.deploymentUrl = contextDeploymentUri;
            this.tokenStore = tokenStore;
            this.oAuth2ProviderSettings = oAuth2ProviderSettings;
            addServiceListener();
        }

        private void addServiceListener() {
            try {
                final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
                final ServiceConfigManager serviceConfigManager = new ServiceConfigManager(token, SERVICE_NAME,
                        SERVICE_VERSION);
                if (serviceConfigManager.addListener(new UmaProviderSettingsChangeListener()) == null) {
                    logger.error("Could not add listener to ServiceConfigManager instance. UMA provider service " +
                            "changes will not be dynamically updated for realm " + realm);
                }
            } catch (Exception e) {
                String message = "Unable to construct ServiceConfigManager: " + e;
                logger.error(message, e);
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
            }
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
        public String getVersion() {
            return "1.0";
        }

        @Override
        public URI getIssuer() throws ServerException {
            return URI.create(oAuth2ProviderSettings.getIssuer());
        }

        private Set<String> getSetSetting(String realm, String attributeName) throws ServerException {
            try {
                return getSetting(realm, attributeName);
            } catch (SMSException e) {
                logger.error(e.getMessage());
                throw new ServerException(e);
            } catch (SSOException e) {
                logger.error(e.getMessage());
                throw new ServerException(e);
            }
        }

        @Override
        public Set<String> getSupportedPATProfiles() throws ServerException {
            return getSetSetting(realm, SUPPORTED_PAT_PROFILES_ATTR_NAME);
        }

        @Override
        public Set<String> getSupportedAATProfiles() throws ServerException {
            return getSetSetting(realm, SUPPORTED_AAT_PROFILES_ATTR_NAME);
        }

        @Override
        public Set<String> getSupportedRPTProfiles() throws ServerException {
            return getSetSetting(realm, SUPPORTED_RPT_PROFILES_ATTR_NAME);
        }

        @Override
        public String getAuditLogConfig() throws SMSException, SSOException {
            return this.getStringSetting(realm, AUDIT_CONNECTION_CONFIG);
        }

        @Override
        public Set<String> getSupportedPATGrantTypes() throws ServerException {
            return supportedGrantTypes;
        }

        @Override
        public Set<String> getSupportedAATGrantTypes() throws ServerException {
            return supportedGrantTypes;
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
        public Set<String> getSupportedClaimTokenProfiles() throws ServerException {
            return getSetSetting(realm, SUPPORTED_CLAIM_TOKEN_PROFILES_ATTR_NAME);
        }

        public Set<URI> getSupportedUmaProfiles() throws ServerException {
            Set<URI> supportedProfiles = new HashSet<URI>();
            for (String profile : getSetSetting(realm, SUPPORTED_UMA_PROFILES_ATTR_NAME)) {
                supportedProfiles.add(URI.create(profile));
            }
            return supportedProfiles;
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
        public long getRPTLifetime() throws ServerException {
            try {
                return getLongSetting(realm, RPT_LIFETIME_ATTR_NAME);
            } catch (SMSException e) {
                logger.error(e.getMessage());
                throw new ServerException(e);
            } catch (SSOException e) {
                logger.error(e.getMessage());
                throw new ServerException(e);
            }
        }

        @Override
        public long getPermissionTicketLifetime() throws ServerException {
            try {
                return getLongSetting(realm, PERMISSION_TIKCET_LIFETIME_ATTR_NAME);
            } catch (SMSException e) {
                logger.error(e.getMessage());
                throw new ServerException(e);
            } catch (SSOException e) {
                logger.error(e.getMessage());
                throw new ServerException(e);
            }
        }

        @Override
        public Evaluator getPolicyEvaluator(Subject subject) throws EntitlementException {
            return new Evaluator(subject);
        }

        @Override
        public UmaTokenStore getUmaTokenStore() {
            return tokenStore;
        }

        @Override
        public boolean onDeleteResourceServerDeletePolicies() throws ServerException {
            try {
                return getBooleanSetting(realm, DELETE_POLICIES_ON_RESOURCE_SERVER_DELETION);
            } catch (SMSException e) {
                logger.error(e.getMessage());
                throw new ServerException(e);
            } catch (SSOException e) {
                logger.error(e.getMessage());
                throw new ServerException(e);
            }
        }

        @Override
        public boolean onDeleteResourceServerDeleteResourceSets() throws ServerException {
            try {
                return getBooleanSetting(realm, DELETE_RESOURCE_SETS_ON_RESOURCE_SERVER_DELETION);
            } catch (SMSException e) {
                logger.error(e.getMessage());
                throw new ServerException(e);
            } catch (SSOException e) {
                logger.error(e.getMessage());
                throw new ServerException(e);
            }
        }

        /**
         * ServiceListener implementation to clear cache when it changes.
         */
        private final class UmaProviderSettingsChangeListener implements ServiceListener {

            public void schemaChanged(String serviceName, String version) {
                logger.warning("The schemaChanged ServiceListener method was invoked for service " + serviceName
                        + ". This is unexpected.");
            }

            public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent,
                    int type) {
                logger.warning("The globalConfigChanged ServiceListener method was invoked for service " + serviceName);
                //if the global config changes, all organizationalConfig change listeners are invoked as well.
            }

            public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                    String serviceComponent, int type) {
                if (currentRealmTargetedByOrganizationUpdate(serviceName, version, orgName, type)) {
                    if (logger.messageEnabled()) {
                        logger.message("Updating UMA service configuration state for realm " + realm);
                    }
                } else {
                    if (logger.messageEnabled()) {
                        logger.message("Got service update message, but update did not target UmaProvider in " +
                                realm + " realm. ServiceName: " + serviceName + " version: " + version + " orgName: " +
                                orgName + " groupName: " + groupName + " serviceComponent: " + serviceComponent +
                                " type (modified=4, delete=2, add=1): " + type + " realm as DN: "
                                + DNMapper.orgNameToDN(realm));
                    }
                }
            }

            /*
            The listener receives updates for all changes for each service instance in a given realm. We want to be sure
            that we only pull updates as necessary if the update pertains to this particular realm.
             */
            private boolean currentRealmTargetedByOrganizationUpdate(String serviceName, String version, String orgName,
                    int type) {
                return SERVICE_NAME.equals(serviceName) && SERVICE_VERSION.equals(version)
                        && ((ServiceListener.MODIFIED == type) || (ServiceListener.ADDED == type)
                        || (ServiceListener.REMOVED == type))
                        && (orgName != null) && orgName.equals(DNMapper.orgNameToDN(realm));
            }
        }
    }
}
