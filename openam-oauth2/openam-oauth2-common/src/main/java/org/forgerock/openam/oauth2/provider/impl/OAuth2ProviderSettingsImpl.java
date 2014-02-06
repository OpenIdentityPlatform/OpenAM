/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */
package org.forgerock.openam.oauth2.provider.impl;

import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.OAuth2Constants;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceConfigManager;
import java.util.Collections;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.provider.OAuth2ProviderSettings;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

import javax.servlet.http.HttpServletRequest;
import java.security.AccessController;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
  An instance of this class is consulted for OAuth2 provider configuration settings (e.g. token lifetimes) when tokens
 are issued, etc. This class will pull the configuration state from the SMS upon initialization, and update this state
 atomically, when the ServiceListener is invoked with service changes.  The OAuth2ProviderSettingsChangeListener is the ServiceListener
 which will respond to OAuth2Provider settings changes, and create a new instance of the ProviderConfiguration class and update
 the reference if the update applies to the realm corresponding to this instance of the OAuth2ProviderSettingsImpl.


 @author Dirk Hogan
 */
public class OAuth2ProviderSettingsImpl implements OAuth2ProviderSettings {
    private class OAuth2ProviderSettingsChangeListener implements ServiceListener {
        @Override
        public void schemaChanged(String serviceName, String version) {
            OAuth2Utils.DEBUG.warning("The schemaChanged ServiceListener method was invoked for service "
                    + serviceName + ". This is unexpected.");
        }

        @Override
        public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent, int type) {
            OAuth2Utils.DEBUG.warning("The globalConfigChanged ServiceListener method was invoked for service "
                    + serviceName);
            //if the global config changes, all organizationalConfig change listeners are invoked as well.
        }

        @Override
        public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                                              String serviceComponent, int type) {
            if (currentRealmTargetedByOrganizationUpdate(serviceName, version, orgName, type)) {
                if (OAuth2Utils.DEBUG.messageEnabled()) {
                    OAuth2Utils.DEBUG.message("Updating OAuth service configuration state for realm " + realm);
                }
                initializeSettings(!PROPAGATE_EXCEPTIONS);
            } else {
                if (OAuth2Utils.DEBUG.messageEnabled()) {
                    OAuth2Utils.DEBUG.message("Got service update message, but update did not target OAuth2Provider in " +
                            realm + " realm. ServiceName: " + serviceName + " version: " + version + " orgName: " +
                            orgName + " groupName: " + groupName + " serviceComponent: " + serviceComponent +
                            " type (modified=4, delete=2, add=1): " + type + " realm as DN: " + DNMapper.orgNameToDN(realm));
                }
            }
        }

        /*
        The listener receives updates for all changes for each service instance in a given realm. I want to be sure that I only
         pull updates as necessary if the update pertains to this particular realm.
         */
        private boolean currentRealmTargetedByOrganizationUpdate(String serviceName, String version, String orgName, int type) {
            return OAuth2Constants.OAuth2ProviderService.NAME.equals(serviceName) &&
                    OAuth2Constants.OAuth2ProviderService.VERSION.equals(version) &&
                    ((ServiceListener.MODIFIED == type) || (ServiceListener.ADDED == type)) &&
                    (orgName != null) &&
                    orgName.equals(DNMapper.orgNameToDN(realm));
        }
    }

    private static class ProviderConfiguration {
        final Long authorizationCodeLifetime;
        final Long refreshTokenLifetime;
        final Long accessTokenLifetime;
        final Long jwtTokenLifetime;
        final Boolean refreshTokensEnabled;
        final String scopeImplementationClass;
        final Set<String> responseTypes;
        final Set<String> resourceOwnerAuthenticationAttributes;
        final String savedConsentAttribute;
        final String jwksUri;
        final Set<String> supportedSubjectTypes;
        final Set<String> supportedIdSigningAlgorithms;
        final Set<String> supportedClaims;
        final String keyStoreAlias;

        private ProviderConfiguration(Long authorizationCodeLifetime,
                                     Long refreshTokenLifetime,
                                     Long accessTokenLifetime,
                                     Long jwtTokenLifetime,
                                     Boolean refreshTokensEnabled,
                                     String scopeImplementationClass,
                                     Set<String> responseTypes,
                                     Set<String> resourceOwnerAuthenticationAttributes,
                                     String savedConsentAttribute,
                                     String jwksUri,
                                     Set<String> supportedSubjectTypes,
                                     Set<String> supportedIdSigningAlgorithms,
                                     Set<String> supportedClaims,
                                     String keyStoreAlias) {

            this.authorizationCodeLifetime = authorizationCodeLifetime;
            this.refreshTokenLifetime = refreshTokenLifetime;
            this.accessTokenLifetime = accessTokenLifetime;
            this.jwtTokenLifetime = jwtTokenLifetime;
            this.refreshTokensEnabled = refreshTokensEnabled;
            this.scopeImplementationClass = scopeImplementationClass;
            this.responseTypes = (responseTypes != null) ? Collections.unmodifiableSet(responseTypes) : null;
            this.resourceOwnerAuthenticationAttributes =
                    (resourceOwnerAuthenticationAttributes != null) ? Collections.unmodifiableSet(resourceOwnerAuthenticationAttributes) : null;
            this.savedConsentAttribute = savedConsentAttribute;
            this.jwksUri = jwksUri;
            this.supportedSubjectTypes = (supportedSubjectTypes != null) ? Collections.unmodifiableSet(supportedSubjectTypes) : null;
            this.supportedIdSigningAlgorithms = (supportedIdSigningAlgorithms != null) ? Collections.unmodifiableSet(supportedIdSigningAlgorithms) : null;
            this.supportedClaims = (supportedClaims != null) ? Collections.unmodifiableSet(supportedClaims) : null;
            this.keyStoreAlias = keyStoreAlias;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("ProviderConfiguration:\n");
            builder.append("\t").append("authorizationCodeLifetime: ").append(authorizationCodeLifetime).append("\n");
            builder.append("\t").append("refreshTokenLifetime: ").append(refreshTokenLifetime).append("\n");
            builder.append("\t").append("accessTokenLifetime: ").append(accessTokenLifetime).append("\n");
            builder.append("\t").append("jwtTokenLifetime: ").append(jwtTokenLifetime).append("\n");
            builder.append("\t").append("refreshTokensEnabled: ").append(refreshTokensEnabled).append("\n");
            builder.append("\t").append("scopeImplementationClass: ").append(scopeImplementationClass).append("\n");
            builder.append("\t").append("responseTypes: ").append(responseTypes).append("\n");
            builder.append("\t").append("authenticationAttributes: ").append(resourceOwnerAuthenticationAttributes).append("\n");
            builder.append("\t").append("savedConsentAttribute: ").append(savedConsentAttribute).append("\n");
            builder.append("\t").append("jwksUri: ").append(jwksUri).append("\n");
            builder.append("\t").append("supportedSubjectTypes: ").append(supportedSubjectTypes).append("\n");
            builder.append("\t").append("idSigningAlgorithms: ").append(supportedIdSigningAlgorithms).append("\n");
            builder.append("\t").append("supportedClaims: ").append(supportedClaims).append("\n");
            builder.append("\t").append("keyStoreAlias: ").append(keyStoreAlias).append("\n");
            return builder.toString();
        }
    }
    private ProviderConfiguration providerConfiguration;

    private String deploymentUrl;
    private String realm;

    /*
    This boolean is used to determine exception propagation behavior in initializeSettings - don't propagate exceptions
    related to reading service config state when updating due to service changes - don't want
    to propagate exceptions to service notifier thread.
     */
    private static final boolean PROPAGATE_EXCEPTIONS = false;

    public OAuth2ProviderSettingsImpl(Request request) {
        HttpServletRequest req = ServletUtils.getRequest(request);
        initializeClass(req);
    }

    public OAuth2ProviderSettingsImpl(HttpServletRequest request) {
        initializeClass(request);
    }

    private void initializeClass(HttpServletRequest request){

        deploymentUrl = OAuth2Utils.getDeploymentURL(request);
        realm = OAuth2Utils.getRealm(request);
        SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
        ServiceConfigManager serviceConfigManager = null;
        try {
            serviceConfigManager = new ServiceConfigManager(token, OAuth2Constants.OAuth2ProviderService.NAME, OAuth2Constants.OAuth2ProviderService.VERSION);
        } catch (Exception e) {
            String message = "OAuth2Utils::Unable to construct ServiceConfigManager: " + e;
            OAuth2Utils.DEBUG.error(message, e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
        initializeSettings(PROPAGATE_EXCEPTIONS, serviceConfigManager);
        if (serviceConfigManager.addListener(new OAuth2ProviderSettingsChangeListener()) == null) {
            OAuth2Utils.DEBUG.error("Could not add listener to ServiceConfigManager instance. OAuth2 provider service " +
                    "changes will not be dynamically updated for realm " + realm);
        }

    }

    private void initializeSettings(boolean propagateException) {
        SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
        try {
            initializeSettings(propagateException, new ServiceConfigManager(token, OAuth2Constants.OAuth2ProviderService.NAME, OAuth2Constants.OAuth2ProviderService.VERSION));
        } catch (Exception e) {
            OAuth2Utils.DEBUG.error("Exception caught initializing service config for OAuth2 Provider in realm " + realm + ". Exception: " + e, e);
            if (propagateException) {
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "Not able to initialize provider settings.");
            }
        }
    }

    private void initializeSettings(boolean propagateException, ServiceConfigManager serviceConfigManager) {
        try {
            ServiceConfig serviceConfig = serviceConfigManager.getOrganizationConfig(realm, null);
            Long authorizationCodeLifetime = getLongAttribute(serviceConfig, OAuth2Constants.OAuth2ProviderService.AUTHZ_CODE_LIFETIME_NAME);
            Long refreshTokenLifetime = getLongAttribute(serviceConfig, OAuth2Constants.OAuth2ProviderService.REFRESH_TOKEN_LIFETIME_NAME);
            Long accessTokenLifetime = getLongAttribute(serviceConfig, OAuth2Constants.OAuth2ProviderService.ACCESS_TOKEN_LIFETIME_NAME);
            Long jwtTokenLifetime = getLongAttribute(serviceConfig, OAuth2Constants.OAuth2ProviderService.JWT_TOKEN_LIFETIME_NAME);
            boolean issueRefreshToken = getBooleanAttribute(serviceConfig, OAuth2Constants.OAuth2ProviderService.ISSUE_REFRESH_TOKEN);
            String scopeImplementationClass = getStringAttribute(serviceConfig, OAuth2Constants.OAuth2ProviderService.SCOPE_PLUGIN_CLASS);
            Set<String> responseTypes = getStringSetAttribute(serviceConfig, OAuth2Constants.OAuth2ProviderService.RESPONSE_TYPE_LIST);
            Set<String> authenticationAttributes = getStringSetAttribute(serviceConfig, OAuth2Constants.OAuth2ProviderService.AUTHENITCATION_ATTRIBUTES);
            String sharedConsent = getStringAttribute(serviceConfig, OAuth2Constants.OAuth2ProviderService.SAVED_CONSENT_ATTRIBUTE);
            String jkwsUri = getStringAttribute(serviceConfig, OAuth2Constants.OAuth2ProviderService.JKWS_URI);
            Set<String> supportedSubjectTypes = getStringSetAttribute(serviceConfig, OAuth2Constants.OAuth2ProviderService.SUBJECT_TYPES_SUPPORTED);
            Set<String> idTokenSigningAlgorithms = getStringSetAttribute(serviceConfig, OAuth2Constants.OAuth2ProviderService.ID_TOKEN_SIGNING_ALGORITHMS);
            Set<String> supportedClaims = getStringSetAttribute(serviceConfig, OAuth2Constants.OAuth2ProviderService.SUPPORTED_CLAIMS);
            String keyStoreAlias = getStringAttribute(serviceConfig, OAuth2Constants.OAuth2ProviderService.KEYSTORE_ALIAS);

            ProviderConfiguration newProviderSettings = new ProviderConfiguration(
                    authorizationCodeLifetime,
                    refreshTokenLifetime,
                    accessTokenLifetime,
                    jwtTokenLifetime,
                    issueRefreshToken,
                    scopeImplementationClass,
                    responseTypes,
                    authenticationAttributes,
                    sharedConsent,
                    jkwsUri,
                    supportedSubjectTypes,
                    idTokenSigningAlgorithms,
                    supportedClaims,
                    keyStoreAlias);
            setProviderConfig(newProviderSettings);
            if (OAuth2Utils.DEBUG.messageEnabled()) {
                OAuth2Utils.DEBUG.message("Successfully updated OAuth2 provider settings for realm " + realm + " with settings " +
                        newProviderSettings);
            }
        } catch (Exception e) {
            String message = "Not able to initialize OAuth2 provider settings for realm " + realm + " Exception: " + e;
            OAuth2Utils.DEBUG.error(message, e);
            if (propagateException) {
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
            }
        }
    }

    /*
        Method is synchronized:
        1. to exclude concurrent updates
        2. primary reason: to insure visibility of changes made by event propagation thread to threads calling get*
        methods below.
     */
    private synchronized void setProviderConfig(ProviderConfiguration newSettings) {
        providerConfiguration = newSettings;
    }

    private Long getLongAttribute(ServiceConfig serviceConfig, String attributeName) {
        Map<String, Set<String>> attributes = serviceConfig.getAttributes();
        Set<String> attribute = attributes.get(attributeName);
        if (attribute != null && !attribute.isEmpty()) {
            try {
                return Long.decode(attribute.iterator().next());
            } catch (NumberFormatException e) {
                OAuth2Utils.DEBUG.error("Number format exception decoding Long attribute " + attributeName + " Exception: " + e, e);
                return null;
            }
        } else {
            return null;
        }
    }

    private Boolean getBooleanAttribute(ServiceConfig serviceConfig, String attributeName) {
        Map<String, Set<String>> attributes = serviceConfig.getAttributes();
        Set<String> attribute = attributes.get(attributeName);
        if (attribute != null && !attribute.isEmpty()) {
            return Boolean.valueOf(attribute.iterator().next());
        } else {
            return null;
        }
    }

    private String getStringAttribute(ServiceConfig serviceConfig, String attributeName) {
        Map<String, Set<String>> attributes = serviceConfig.getAttributes();
        Set<String> attribute = attributes.get(attributeName);
        if (attribute != null && !attribute.isEmpty()) {
            return attribute.iterator().next();
        } else {
            return null;
        }
    }

    private Set<String> getStringSetAttribute(ServiceConfig serviceConfig, String attributeName) {
        Map<String, Set<String>> attributes = serviceConfig.getAttributes();
        Set<String> attribute = attributes.get(attributeName);
        if (attribute != null && !attribute.isEmpty()) {
            return attribute;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getOpenIDConnectVersion() {
        return "3.0";
    }

    /**
     * {@inheritDoc}
     */
    public String getOpenIDConnectIssuer() {
        return deploymentUrl;
    }

    /**
     * {@inheritDoc}
     */
    public String getCheckSessionEndpoint() {
        return deploymentUrl + "/oauth2/connect/checkSession";
    }

    /**
     * {@inheritDoc}
     */
    public String getEndSessionEndPoint() {
        return deploymentUrl + "/oauth2/connect/endSession";
    }

    @Override
    public long getAuthorizationCodeLifetime() {
        if ((providerConfiguration != null) && (providerConfiguration.authorizationCodeLifetime != null)) {
            return providerConfiguration.authorizationCodeLifetime;
        } else {
            String message = "OAuth2Utils::Unable to get provider setting for : "+
                    OAuth2Constants.OAuth2ProviderService.AUTHZ_CODE_LIFETIME_NAME;
            OAuth2Utils.DEBUG.error(message);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    @Override
    public long getRefreshTokenLifetime() {
        if ((providerConfiguration != null) && (providerConfiguration.refreshTokenLifetime != null)) {
            return providerConfiguration.refreshTokenLifetime;
        } else {
            String message = "OAuth2Utils::Unable to get provider setting for : "+
                    OAuth2Constants.OAuth2ProviderService.REFRESH_TOKEN_LIFETIME_NAME;
            OAuth2Utils.DEBUG.error(message);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    @Override
    public long getAccessTokenLifetime() {
        if ((providerConfiguration != null) && (providerConfiguration.accessTokenLifetime != null)) {
            return providerConfiguration.accessTokenLifetime;
        } else {
            String message = "OAuth2Utils::Unable to get provider setting for : "+
                    OAuth2Constants.OAuth2ProviderService.ACCESS_TOKEN_LIFETIME_NAME;
            OAuth2Utils.DEBUG.error(message);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    @Override
    public long getJWTTokenLifetime() {
        if ((providerConfiguration != null) && (providerConfiguration.jwtTokenLifetime != null)) {
            return providerConfiguration.jwtTokenLifetime;
        } else {
            String message = "OAuth2Utils::Unable to get provider setting for : "+
                    OAuth2Constants.OAuth2ProviderService.JWT_TOKEN_LIFETIME_NAME;
            OAuth2Utils.DEBUG.error(message);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    @Override
    public boolean getRefreshTokensEnabledState() {
        if ((providerConfiguration != null) && (providerConfiguration.refreshTokensEnabled != null)) {
            return providerConfiguration.refreshTokensEnabled;
        } else {
            String message = "OAuth2Utils::Unable to get provider setting for : "+
                    OAuth2Constants.OAuth2ProviderService.ISSUE_REFRESH_TOKEN;
            OAuth2Utils.DEBUG.error(message);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    @Override
    public String getScopeImplementationClass() {
        if ((providerConfiguration != null) && (providerConfiguration.scopeImplementationClass != null)) {
            return providerConfiguration.scopeImplementationClass;
        } else {
            String message = "OAuth2Utils::Unable to get provider setting for : "+
                    OAuth2Constants.OAuth2ProviderService.SCOPE_PLUGIN_CLASS;
            OAuth2Utils.DEBUG.error(message);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    @Override
    public Set<String> getResponseTypes() {
        if ((providerConfiguration != null) && (providerConfiguration.responseTypes != null)) {
            return providerConfiguration.responseTypes;
        } else {
            String message = "OAuth2Utils::Unable to get provider setting for : "+
                    OAuth2Constants.OAuth2ProviderService.RESPONSE_TYPE_LIST;
            OAuth2Utils.DEBUG.error(message);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    @Override
    public Set<String> getListOfAttributesTheResourceOwnerIsAuthenticatedOn() {
        if ((providerConfiguration != null) && (providerConfiguration.resourceOwnerAuthenticationAttributes != null)) {
            return providerConfiguration.resourceOwnerAuthenticationAttributes;
        } else {
            String message = "OAuth2Utils::Unable to get provider setting for : "+
                    OAuth2Constants.OAuth2ProviderService.AUTHENITCATION_ATTRIBUTES;
            OAuth2Utils.DEBUG.error(message);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    @Override
    public String getSharedConsentAttributeName() {
        if ((providerConfiguration != null) && (providerConfiguration.savedConsentAttribute != null)) {
            return providerConfiguration.savedConsentAttribute;
        } else {
            String message = "OAuth2Utils::Unable to get provider setting for : "+
                    OAuth2Constants.OAuth2ProviderService.SAVED_CONSENT_ATTRIBUTE;
            OAuth2Utils.DEBUG.error(message);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    @Override
    public String getJWKSUri() {
        if ((providerConfiguration != null) && (providerConfiguration.jwksUri != null)) {
            return providerConfiguration.jwksUri;
        } else {
            String message = "OAuth2Utils::Unable to get provider setting for : "+
                    OAuth2Constants.OAuth2ProviderService.JKWS_URI;
            OAuth2Utils.DEBUG.error(message);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    @Override
    public Set<String> getSubjectTypesSupported() {
        if ((providerConfiguration != null) && (providerConfiguration.supportedSubjectTypes != null)) {
            return providerConfiguration.supportedSubjectTypes;
        } else {
            String message = "OAuth2Utils::Unable to get provider setting for : "+
                    OAuth2Constants.OAuth2ProviderService.SUBJECT_TYPES_SUPPORTED;
            OAuth2Utils.DEBUG.error(message);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    @Override
    public Set<String> getTheIDTokenSigningAlgorithmsSupported() {
        if ((providerConfiguration != null) && (providerConfiguration.supportedIdSigningAlgorithms != null)) {
            return providerConfiguration.supportedIdSigningAlgorithms;
        } else {
            String message = "OAuth2Utils::Unable to get provider setting for : "+
                    OAuth2Constants.OAuth2ProviderService.ID_TOKEN_SIGNING_ALGORITHMS;
            OAuth2Utils.DEBUG.error(message);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    @Override
    public Set<String> getSupportedClaims() {
        if ((providerConfiguration != null) && (providerConfiguration.supportedClaims != null)) {
            return providerConfiguration.supportedClaims;
        } else {
            String message = "OAuth2Utils::Unable to get provider setting for : "+
                    OAuth2Constants.OAuth2ProviderService.SUPPORTED_CLAIMS;
            OAuth2Utils.DEBUG.error(message);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    @Override
    public String getKeyStoreAlias() {
        if ((providerConfiguration != null) && (providerConfiguration.keyStoreAlias != null)) {
            return providerConfiguration.keyStoreAlias;
        } else {
            String message = "OAuth2Utils::Unable to get provider setting for : "+
                    OAuth2Constants.OAuth2ProviderService.KEYSTORE_ALIAS;
            OAuth2Utils.DEBUG.error(message);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, message);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getClientRegistrationEndpoint() {
        return deploymentUrl + "/oauth2/connect/register";
    }

    /**
     * {@inheritDoc}
     */
    public String getAuthorizationEndpoint() {
        return deploymentUrl + "/oauth2/authorize";
    }

    /**
     * {@inheritDoc}
     */
    public String getTokenEndpoint() {
        return deploymentUrl + "/oauth2/access_token";
    }

    /**
     * {@inheritDoc}
     */
    public String getUserInfoEndpoint() {
        return deploymentUrl + "/oauth2/userinfo";
    }
}
