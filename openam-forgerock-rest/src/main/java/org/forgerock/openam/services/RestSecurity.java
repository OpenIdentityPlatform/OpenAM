/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All rights reserved.
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
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceNotFoundException;
import org.forgerock.openam.forgerockrest.RestDispatcher;
import org.forgerock.openam.forgerockrest.RestUtils;

import java.security.AccessController;

public class RestSecurity {

    private static ServiceConfigManager mgr;
    private RestSecurityConfiguration restSecurityConfiguration;

    private final static String SELF_REGISTRATION = "forgerockRESTSecuritySelfRegistrationEnabled";
    private final static String FORGOT_PASSWORD = "forgerockRESTSecurityForgotPasswordEnabled";
    private final static String SELF_REG_TOKEN_LIFE_TIME = "forgerockRESTSecuritySelfRegTokenTTL";
    private final static String FORGOT_PASSWORD_TOKEN_LIFE_TIME= "forgerockRESTSecurityForgotPassTokenTTL";

    private final static String SERVICE_NAME = "RestSecurity";
    private final static String SERVICE_VERSION = "1.0";

    private final String realm;

    private class RestSecurityChangeListener implements ServiceListener {
        @Override
        public void schemaChanged(String serviceName, String version) {
            RestDispatcher.debug.warning("The schemaChanged ServiceListener method was invoked for service "
                    + serviceName + ". This is unexpected.");
        }

        @Override
        public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent, int type) {
            RestDispatcher.debug.warning("The globalConfigChanged ServiceListener method was invoked for service "
                    + serviceName);
            //if the global config changes, all organizationalConfig change listeners are invoked as well.
        }

        @Override
        public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                                              String serviceComponent, int type) {
            if (currentRealmTargetedByOrganizationUpdate(serviceName, version, orgName, type)) {
                if (RestDispatcher.debug.messageEnabled()) {
                    RestDispatcher.debug.message("Updating RestSecurity service configuration state for realm " + realm);
                }
                initializeSettings(mgr);
            } if (currentRealmTargetedByOrganizaionRemoved(serviceName, version, orgName, type)){
                mgr = null;
            } else {
                if (RestDispatcher.debug.messageEnabled()) {
                    RestDispatcher.debug.message("Got service update message, but update did not target Rest Security settings in " +
                            realm + " realm. ServiceName: " + serviceName + " version: " + version + " orgName: " +
                            orgName + " groupName: " + groupName + " serviceComponent: " + serviceComponent +
                            " type (modified=4, delete=2, add=1): " + type + " realm as DN: " + DNMapper.orgNameToDN(realm));
                }
            }
        }

        private boolean currentRealmTargetedByOrganizaionRemoved(String serviceName, String version, String orgName, int type){
            return serviceName.equalsIgnoreCase(SERVICE_NAME) &&
                    version.equalsIgnoreCase(SERVICE_VERSION) &&
                    (ServiceListener.REMOVED == type) &&
                    (orgName != null) &&
                    orgName.equals(DNMapper.orgNameToDN(realm));
        }
        private boolean currentRealmTargetedByOrganizationUpdate(String serviceName, String version, String orgName, int type) {
            return serviceName.equalsIgnoreCase(SERVICE_NAME) &&
                    version.equalsIgnoreCase(SERVICE_VERSION) &&
                    (ServiceListener.MODIFIED == type) &&
                    (orgName != null) &&
                    orgName.equals(DNMapper.orgNameToDN(realm));
        }
    }

    private static class RestSecurityConfiguration {
        final Long selfRegTokenLifeTime;
        final Long forgotPasswordTokenLifeTime;
        final Boolean selfRegistration;
        final Boolean forgotPassword;

        private RestSecurityConfiguration(Long selfRegTokenLifeTime, Long forgotPasswordLifeTime, Boolean selfRegistration, Boolean forgotPassword) {
            this.selfRegTokenLifeTime = selfRegTokenLifeTime;
            this.forgotPasswordTokenLifeTime = forgotPasswordLifeTime;
            this.selfRegistration = selfRegistration;
            this.forgotPassword = forgotPassword;
        }
    }

    private void initializeSettings(ServiceConfigManager serviceConfigManager) {
        try {
            ServiceConfig serviceConfig = serviceConfigManager.getOrganizationConfig(realm, null);
            boolean selfRegistration = RestUtils.getBooleanAttribute(serviceConfig, SELF_REGISTRATION);
            boolean forgotPassword = RestUtils.getBooleanAttribute(serviceConfig, FORGOT_PASSWORD);
            Long selfRegTokLifeTime = RestUtils.getLongAttribute(serviceConfig, SELF_REG_TOKEN_LIFE_TIME);
            Long forgotPassTokLifeTime = RestUtils.getLongAttribute(serviceConfig, FORGOT_PASSWORD_TOKEN_LIFE_TIME);
            RestSecurityConfiguration newRestSecuritySettings = new RestSecurityConfiguration(
                    selfRegTokLifeTime,
                    forgotPassTokLifeTime,
                    selfRegistration,
                    forgotPassword);

            setProviderConfig(newRestSecuritySettings);
            if (RestDispatcher.debug.messageEnabled()) {
                RestDispatcher.debug.message("Successfully updated rest security service settings for realm " + realm + " with settings " +
                        newRestSecuritySettings);
            }
        } catch (Exception e) {
            String message = "Not able to initialize Rest Security service settings for realm " + realm + " Exception: " + e;
            RestDispatcher.debug.error(message, e);
        }
    }

    private synchronized void setProviderConfig(RestSecurityConfiguration newSettings) {
        restSecurityConfiguration = newSettings;
    }
    /**
     * Default Constructor
     * @param realm in which Rest Security service shall be created
     */
    public RestSecurity(String realm) {
        this.realm = realm;
        try {
            mgr = new ServiceConfigManager(AccessController.doPrivileged(AdminTokenAction.getInstance()),
                    SERVICE_NAME, SERVICE_VERSION);
        } catch (Exception e) {
            RestDispatcher.debug.error("Cannot get ServiceConfigManager", e);
        }
        initializeSettings(mgr);
        if (mgr.addListener(new RestSecurityChangeListener()) == null) {
            RestDispatcher.debug.error("Could not add listener to ServiceConfigManager instance. Rest Security service " +
                    "changes will not be dynamically updated for realm " + realm);
        }
    }

    public boolean isSelfRegistration() throws ServiceNotFoundException {
        if ((restSecurityConfiguration != null) && (restSecurityConfiguration.selfRegistration != null)) {
            return restSecurityConfiguration.selfRegistration;
        } else {
            String message = "RestSecurity::Unable to get provider setting for : "+ SELF_REGISTRATION;
            RestDispatcher.debug.error(message);
            throw new ServiceNotFoundException(message);
        }
    }

    public boolean isForgotPassword() throws ServiceNotFoundException {
        if ((restSecurityConfiguration != null) && (restSecurityConfiguration.forgotPassword != null)) {
            return restSecurityConfiguration.forgotPassword;
        } else {
            String message = "RestSecurity::Unable to get provider setting for : "+ FORGOT_PASSWORD;
            RestDispatcher.debug.error(message);
            throw new ServiceNotFoundException(message);
        }
    }
    /**
     * Retrieves the Self-Registration CTS Token Life Time
     * @return Long representing the time that the Token shall be valid
     * @throws ServiceNotFoundException
     */
    public Long getSelfRegTLT() throws ServiceNotFoundException {
        if ((restSecurityConfiguration != null) && (restSecurityConfiguration.selfRegTokenLifeTime != null)) {
            return restSecurityConfiguration.selfRegTokenLifeTime;
        } else {
            String message = "RestSecurity::Unable to get provider setting for : "+ SELF_REG_TOKEN_LIFE_TIME;
            RestDispatcher.debug.error(message);
            throw new ServiceNotFoundException(message);
        }
    }

    /**
     * Retrieves the Forgotten Password CTS Token Life Time
     * @return Long representing the time that the Token shall be valid
     * @throws ServiceNotFoundException
     */
    public Long getForgotPassTLT() throws ServiceNotFoundException {
        if ((restSecurityConfiguration != null) && (restSecurityConfiguration.forgotPasswordTokenLifeTime != null)) {
            return restSecurityConfiguration.forgotPasswordTokenLifeTime;
        } else {
            String message = "RestSecurity::Unable to get provider setting for : "+ FORGOT_PASSWORD_TOKEN_LIFE_TIME;
            RestDispatcher.debug.error(message);
            throw new ServiceNotFoundException(message);
        }
    }
}
