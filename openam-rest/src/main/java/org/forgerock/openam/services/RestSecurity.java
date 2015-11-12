/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013-2015 ForgeRock AS.
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

package org.forgerock.openam.services;

import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceNotFoundException;
import org.forgerock.openam.rest.ServiceConfigUtils;

import java.security.AccessController;
import java.util.Set;

public class RestSecurity {

    private static Debug debug = Debug.getInstance("frRest");

    private static ServiceConfigManager mgr;
    private RestSecurityConfiguration restSecurityConfiguration;

    private final static String TWO_FACTOR_AUTH_ENABLED = "forgerockRESTSecurityTwoFactorAuthEnabled";
    private final static String SELF_REGISTRATION = "forgerockRESTSecuritySelfRegistrationEnabled";
    private final static String SELF_REG_CONFIRMATION_URL = "forgerockRESTSecuritySelfRegConfirmationUrl";
    private final static String FORGOT_PASSWORD = "forgerockRESTSecurityForgotPasswordEnabled";
    private final static String FORGOT_USERNAME = "forgerockRESTSecurityForgotUsernameEnabled";
    private final static String SELF_REG_TOKEN_LIFE_TIME = "forgerockRESTSecuritySelfRegTokenTTL";
    private final static String FORGOT_PASSWORD_TOKEN_LIFE_TIME = "forgerockRESTSecurityForgotPassTokenTTL";
    private final static String FORGOT_PASSWORD_CONFIRMATION_URL = "forgerockRESTSecurityForgotPassConfirmationUrl";
    private final static String PROTECTED_USER_ATTRIBUTES = "forgerockRESTSecurityProtectedUserAttributes";
    private final static String SUCCESSFUL_USER_REGISTRATION_DESTINATION = "forgerockRESTSecuritySuccessfulUserRegistrationDestination";
    private final static String SELF_REG_VALID_ATTRIBUTES = "forgerockRESTSecuritySelfRegistrationValidUserAttributes";
    private final static String SELF_REG_KBA_ENABLED = "forgerockRESTSecuritySelfRegKbaEnabled";
    private final static String FORGOT_PASSWORD_KBA_ENABLED = "forgerockRESTSecurityForgotPassKbaEnabled";
    private final static String FORGOT_USERNAME_KBA_ENABLED = "forgerockRESTSecurityForgotUsernameKbaEnabled";

    private final static String SERVICE_NAME = "RestSecurity";
    private final static String SERVICE_VERSION = "1.0";

    private final String realm;

    private class RestSecurityChangeListener implements ServiceListener {
        @Override
        public void schemaChanged(String serviceName, String version) {
            debug.warning("The schemaChanged ServiceListener method was invoked for service "
                    + serviceName + ". This is unexpected.");
        }

        @Override
        public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent, int type) {
            debug.warning("The globalConfigChanged ServiceListener method was invoked for service "
                    + serviceName);
            //if the global config changes, all organizationalConfig change listeners are invoked as well.
        }

        @Override
        public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                String serviceComponent, int type) {
            if (currentRealmTargetedByOrganizationUpdate(serviceName, version, orgName, type)) {
                if (debug.messageEnabled()) {
                    debug.message("Updating RestSecurity service configuration state for realm " + realm);
                }
                initializeSettings(mgr);
            }
            if (currentRealmTargetedByOrganizaionRemoved(serviceName, version, orgName, type)) {
                mgr = null;
            } else {
                if (debug.messageEnabled()) {
                    debug.message("Got service update message, but update did not target Rest Security settings in " +
                            realm + " realm. ServiceName: " + serviceName + " version: " + version + " orgName: " +
                            orgName + " groupName: " + groupName + " serviceComponent: " + serviceComponent +
                            " type (modified=4, delete=2, add=1): " + type + " realm as DN: " + DNMapper.orgNameToDN(realm));
                }
            }
        }

        private boolean currentRealmTargetedByOrganizaionRemoved(String serviceName, String version, String orgName, int type) {
            return serviceName.equalsIgnoreCase(SERVICE_NAME) &&
                    version.equalsIgnoreCase(SERVICE_VERSION) &&
                    (ServiceListener.REMOVED == type) &&
                    (orgName != null) &&
                    orgName.equals(DNMapper.orgNameToDN(realm));
        }

        private boolean currentRealmTargetedByOrganizationUpdate(String serviceName, String version, String orgName, int type) {
            return serviceName.equalsIgnoreCase(SERVICE_NAME) &&
                    version.equalsIgnoreCase(SERVICE_VERSION) &&
                    (ServiceListener.MODIFIED == type || ServiceListener.ADDED == type) &&
                    (orgName != null) &&
                    orgName.equals(DNMapper.orgNameToDN(realm));
        }
    }

    private static class RestSecurityConfiguration {
        final Long selfRegTokenLifeTime;
        final String selfRegistrationConfirmationUrl;
        final Long forgotPasswordTokenLifeTime;
        final String forgotPasswordConfirmationUrl;
        final Boolean selfRegistration;
        final Boolean forgotPassword;
        final Boolean forgotUsername;
        final Boolean kbaEnabled;
        final Set<String> protectedUserAttributes;
        final String successfulUserRegistrationDestination;
        final Boolean twoFactorAuthEnabled;
        final Set<String> selfRegistrationValidAttributes;

        private RestSecurityConfiguration(Long selfRegTokenLifeTime, String selfRegistrationConfirmationUrl,
                Long forgotPasswordLifeTime, String forgotPasswordConfirmationUrl, Boolean selfRegistration,
                Boolean forgotPassword, Boolean forgotUsername, Boolean kbaEnabled, Set<String> protectedUserAttributes,
                String successfulUserRegistrationDestination, Boolean twoFactorAuthEnabled,
                Set<String> selfRegistrationValidAttributes) {
            this.selfRegTokenLifeTime = selfRegTokenLifeTime;
            this.selfRegistrationConfirmationUrl = selfRegistrationConfirmationUrl;
            this.forgotPasswordTokenLifeTime = forgotPasswordLifeTime;
            this.forgotPasswordConfirmationUrl = forgotPasswordConfirmationUrl;
            this.selfRegistration = selfRegistration;
            this.forgotPassword = forgotPassword;
            this.forgotUsername = forgotUsername;
            this.kbaEnabled = kbaEnabled;
            this.protectedUserAttributes = protectedUserAttributes;
            this.successfulUserRegistrationDestination = successfulUserRegistrationDestination;
            this.twoFactorAuthEnabled = twoFactorAuthEnabled;
            this.selfRegistrationValidAttributes = selfRegistrationValidAttributes;
        }
    }

    private void initializeSettings(ServiceConfigManager serviceConfigManager) {
        try {
            ServiceConfig serviceConfig = serviceConfigManager.getOrganizationConfig(realm, null);
            Boolean selfRegistration = ServiceConfigUtils.getBooleanAttribute(serviceConfig, SELF_REGISTRATION);
            String selfRegistrationConfirmationUrl = ServiceConfigUtils.getStringAttribute(serviceConfig, SELF_REG_CONFIRMATION_URL);
            Boolean forgotPassword = ServiceConfigUtils.getBooleanAttribute(serviceConfig, FORGOT_PASSWORD);
            String forgotPasswordConfirmationUrl = ServiceConfigUtils.getStringAttribute(serviceConfig, FORGOT_PASSWORD_CONFIRMATION_URL);
            Boolean forgotUsername = ServiceConfigUtils.getBooleanAttribute(serviceConfig, FORGOT_USERNAME);
            Long selfRegTokLifeTime = ServiceConfigUtils.getLongAttribute(serviceConfig, SELF_REG_TOKEN_LIFE_TIME);
            Long forgotPassTokLifeTime = ServiceConfigUtils.getLongAttribute(serviceConfig, FORGOT_PASSWORD_TOKEN_LIFE_TIME);
            Set<String> protectedUserAttributes = ServiceConfigUtils.getSetAttribute(serviceConfig, PROTECTED_USER_ATTRIBUTES);
            String successfulUserRegistrationDestination = ServiceConfigUtils.getStringAttribute(serviceConfig, SUCCESSFUL_USER_REGISTRATION_DESTINATION);
            Boolean twoFactorAuthEnabled = ServiceConfigUtils.getBooleanAttribute(serviceConfig, TWO_FACTOR_AUTH_ENABLED);
            Set<String> selfRegistrationValidAttributes = ServiceConfigUtils.getSetAttribute(serviceConfig, SELF_REG_VALID_ATTRIBUTES);

            Boolean kbaEnabledSelfRegistration = ServiceConfigUtils.getBooleanAttribute(serviceConfig, SELF_REG_KBA_ENABLED);
            Boolean kbaEnabledForgottenPassword = ServiceConfigUtils.getBooleanAttribute(serviceConfig, FORGOT_PASSWORD_KBA_ENABLED);
            Boolean kbaEnabledForgottenUsername = ServiceConfigUtils.getBooleanAttribute(serviceConfig, FORGOT_USERNAME_KBA_ENABLED);

            Boolean kbaEnabled = Boolean.TRUE.equals(kbaEnabledSelfRegistration)
                    || Boolean.TRUE.equals(kbaEnabledForgottenPassword)
                    || Boolean.TRUE.equals(kbaEnabledForgottenUsername);

            RestSecurityConfiguration newRestSecuritySettings = new RestSecurityConfiguration(
                    selfRegTokLifeTime,
                    selfRegistrationConfirmationUrl,
                    forgotPassTokLifeTime,
                    forgotPasswordConfirmationUrl,
                    selfRegistration,
                    forgotPassword,
                    forgotUsername,
                    kbaEnabled,
                    protectedUserAttributes,
                    successfulUserRegistrationDestination,
                    twoFactorAuthEnabled,
                    selfRegistrationValidAttributes);

            setProviderConfig(newRestSecuritySettings);
            if (debug.messageEnabled()) {
                debug.message("Successfully updated rest security service settings for realm " + realm + " with settings " +
                        newRestSecuritySettings);
            }
        } catch (Exception e) {
            String message = "Not able to initialize Rest Security service settings for realm " + realm + " Exception: " + e;
            debug.error(message, e);
        }
    }

    private synchronized void setProviderConfig(RestSecurityConfiguration newSettings) {
        restSecurityConfiguration = newSettings;
    }

    /**
     * Default Constructor
     *
     * @param realm
     *         in which Rest Security service shall be created
     */
    RestSecurity(String realm) {
        this.realm = realm;
        try {
            mgr = new ServiceConfigManager(AccessController.doPrivileged(AdminTokenAction.getInstance()),
                    SERVICE_NAME, SERVICE_VERSION);
        } catch (Exception e) {
            debug.error("Cannot get ServiceConfigManager", e);
        }
        initializeSettings(mgr);
        if (mgr.addListener(new RestSecurityChangeListener()) == null) {
            debug.error("Could not add listener to ServiceConfigManager instance. Rest Security service " +
                    "changes will not be dynamically updated for realm " + realm);
        }
    }

    public boolean isTwoFactorAuthEnabled() throws ServiceNotFoundException {
        if ((restSecurityConfiguration != null) && (restSecurityConfiguration.twoFactorAuthEnabled != null)) {
            return restSecurityConfiguration.twoFactorAuthEnabled;
        } else {
            String message = "RestSecurity::Unable to get provider setting for : " + TWO_FACTOR_AUTH_ENABLED;
            debug.error(message);
            throw new ServiceNotFoundException(message);
        }
    }

    public boolean isSelfRegistration() throws ServiceNotFoundException {
        if ((restSecurityConfiguration != null) && (restSecurityConfiguration.selfRegistration != null)) {
            return restSecurityConfiguration.selfRegistration;
        } else {
            String message = "RestSecurity::Unable to get provider setting for : " + SELF_REGISTRATION;
            debug.error(message);
            throw new ServiceNotFoundException(message);
        }
    }

    public String getSelfRegistrationConfirmationUrl() {
        return restSecurityConfiguration.selfRegistrationConfirmationUrl;
    }

    public boolean isForgotPassword() throws ServiceNotFoundException {
        if ((restSecurityConfiguration != null) && (restSecurityConfiguration.forgotPassword != null)) {
            return restSecurityConfiguration.forgotPassword;
        } else {
            String message = "RestSecurity::Unable to get provider setting for : " + FORGOT_PASSWORD;
            debug.error(message);
            throw new ServiceNotFoundException(message);
        }
    }

    public String getForgotPasswordConfirmationUrl() {
        return restSecurityConfiguration.forgotPasswordConfirmationUrl;
    }

    /**
     * Returns whether or not forgotten username is enabled.
     *
     * @return whether forgotten username is enabled
     *
     * @throws ServiceNotFoundException
     *         if the configuration has not been initialised properly
     */
    public boolean isForgotUsername() throws ServiceNotFoundException {
        if ((restSecurityConfiguration != null) && (restSecurityConfiguration.forgotUsername != null)) {
            return restSecurityConfiguration.forgotUsername;
        } else {
            String message = "RestSecurity::Unable to get provider setting for : " + FORGOT_USERNAME;
            debug.error(message);
            throw new ServiceNotFoundException(message);
        }
    }


    /**
     * Returns whether or not KBA is enabled.
     *
     * @return whether KBA is enabled
     *
     * @throws ServiceNotFoundException
     *         if the configuration has not been initialise properly
     */
    public boolean isKbaEnabled() throws ServiceNotFoundException {
        if (restSecurityConfiguration != null) {
            return restSecurityConfiguration.kbaEnabled;
        } else {
            String message = "RestSecurity::Configuration has not be initialised";
            debug.error(message);
            throw new ServiceNotFoundException(message);
        }
    }

    /**
     * Retrieves the Self-Registration CTS Token Life Time
     *
     * @return Long representing the time that the Token shall be valid
     *
     * @throws ServiceNotFoundException
     */
    public Long getSelfRegTLT() throws ServiceNotFoundException {
        if ((restSecurityConfiguration != null) && (restSecurityConfiguration.selfRegTokenLifeTime != null)) {
            return restSecurityConfiguration.selfRegTokenLifeTime;
        } else {
            String message = "RestSecurity::Unable to get provider setting for : " + SELF_REG_TOKEN_LIFE_TIME;
            debug.error(message);
            throw new ServiceNotFoundException(message);
        }
    }

    public Set<String> getProtectedUserAttributes() {
        return restSecurityConfiguration.protectedUserAttributes;
    }

    public String getSuccessfulUserRegistrationDestination() {
        return restSecurityConfiguration.successfulUserRegistrationDestination;
    }

    /**
     * Retrieves the Forgotten Password CTS Token Life Time
     *
     * @return Long representing the time that the Token shall be valid
     *
     * @throws ServiceNotFoundException
     */
    public Long getForgotPassTLT() throws ServiceNotFoundException {
        if ((restSecurityConfiguration != null) && (restSecurityConfiguration.forgotPasswordTokenLifeTime != null)) {
            return restSecurityConfiguration.forgotPasswordTokenLifeTime;
        } else {
            String message = "RestSecurity::Unable to get provider setting for : " + FORGOT_PASSWORD_TOKEN_LIFE_TIME;
            debug.error(message);
            throw new ServiceNotFoundException(message);
        }
    }

    /**
     * Retrieve the set of attribute names which are valid for self registration (i.e. user creation).
     *
     * @return Set of strings representing valid attributes when creating the user - anything not in this list will be
     * removed when the user is created.
     */
    public Set<String> getSelfRegistrationValidUserAttributes() {
        return restSecurityConfiguration.selfRegistrationValidAttributes;
    }
}
