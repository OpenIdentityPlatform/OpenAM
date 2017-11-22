/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: AMAdminConstants.java,v 1.11 2009/09/28 19:01:24 babysunil Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */
package com.sun.identity.console.base.model;

/* - NEED NOT LOG - */

/**
 * This interface contains a set of constants used by console classes.
 */
public interface AMAdminConstants {
    /**
     * Console Debug file name
     */
    String CONSOLE_DEBUG_FILENAME = "amConsole";

    /**
     *  Current Realm Location
     */
    String CURRENT_REALM = "currentRealm";

    /**
     *  DN of the object profile currently being viewed in the UM console.
     */
    String CURRENT_PROFILE = "CurrentProfileView";

    /** 
     * The current organization location in the directory management view 
     */
    String CURRENT_ORG = "currentOrganization";

    /**
     *  Previous Realm Location
     */
    String PREVIOUS_REALM = "previousRealm";

    /** 
     * Current service being edited in role view
     */
    String SERVICE_NAME = "currentServiceName";

    /**
     * Page session attribute to track page trail.
     */
    String PG_SESSION_INIT_PAGETRAIL = "initPageTrail";

    /**
     * User Service
     */
    String USER_SERVICE = "iPlanetAMUserService";

    /**
     * SAML Service
     */
    String SAML_SERVICE = "iPlanetAMSAMLService";

    /**
     * Core Authentication Service
     */
    String CORE_AUTH_SERVICE = "iPlanetAMAuthService";

    /**
     * Locale SSO Token property
     */
    String SSO_TOKEN_LOCALE_ATTRIBUTE_NAME = "Locale";

    /**
     * Organization SSO Token property
     */
    String SSO_TOKEN_ORGANIZATION_ATTRIBUTE_NAME = "Organization";

    /**
     * Client detection module content type property name
     */
    String CDM_CONTENT_TYPE_PROPERTY_NAME = "contentType";

    /**
     * Default resource bundle name
     */
    String DEFAULT_RB = "amConsole";

    /**
     * <code>amSDK</code> Debug file name
     */
    String AMSDK_DEBUG_FILENAME = "amSDK";

    /**
     * Login URL
     */
    String URL_LOGIN = "/UI/Login";

    /**
     * Logout URL
     */
    String URL_LOGOUT = "/UI/Logout";

    /**
     * Active value.
     */
    String STRING_ACTIVE = "Active";

    /**
     * Active value.
     */  
    String STRING_INACTIVE = "Inactive";

    /**
     * Required attribute value in any attribute.
     */
    String REQUIRED_ATTRIBUTE = "required";

    /**
     * Option attribute value in any attribute.
     */
    String OPTIONAL_ATTRIBUTE = "optional";

    /**
     * End User display attribute value in any attribute.
     */
    String DISPLAY_ATTRIBUTE = "display";

    /**
     * End User display attribute value READ ONLY in any attribute.
     */
    String DISPLAY_READONLY_ATTRIBUTE = "displayRO";

    /**
     * Administrator display attribute value in any attribute.
     */
    String ADMIN_DISPLAY_ATTRIBUTE = "adminDisplay";

    /**
     * Administrator display attribute value READ ONLYin any attribute.
     */
    String ADMIN_DISPLAY_READONLY_ATTRIBUTE = "adminDisplayRO";

    /**
     * Literal false string.
     */
    String STRING_FALSE = "false";

    /**
     * String used for passing choices 
     */
     String CHOICES = "choices";

    /**
     * String used for passing choice values
     */
     String VALUES = "values";

     /**
      * Operation name for add service to realm.
      */
    String OPERATION_ADD = "add";

     /**
      * Operation name for edit service to realm.
      */
    String OPERATION_EDIT = "edit";

    /**
     * Last tab visited before opening a profile object.
     */
    String PREVIOUS_TAB_ID = "PreviousTabID";

    /**
     * Tab Id of Federation tab
     */
    String FED_TAB_ID = "2"; 
    
    /**
     * Organization node identifier.
     */
    int ORGANIZATION_NODE_ID = 7;

    /**
     * Realm node identifier.
     */
    int REALM_NODE_ID = 1;
    
    /**
     * Configuration node identifier.
     */
    int CONFIGURATION_NODE_ID = 4;

    /**
     * Agent node identifier.
     */
    int AGENTS_NODE_ID = 118;

    /**
     * Agent tab Id.
     */
    String TAB_AGENT_PREFIX = "18";

    /**
     * Agent tab Id.
     */
    int TAB_AGENT_PREFIX_INT = 18;
    
    /**
     * Subjects node identifier.
     */
    int SUBJECTS_NODE_ID = 17;

    /**
     * Sessions Node Identifier.
     */
     int SESSIONS_NODE_ID = 5;

    /**
     * Last tab visited before opening a profile object.
     */
    String PREVIOUS_ORGANIZATION = "PreviousOrganization";

    /**
     * Component name for dynamic link.
     */
    String DYN_LINK_COMPONENT_NAME = "dynLink";

    /**
     * Password Reset Service Name.
     */
    String PW_RESET_SERVICE = "iPlanetAMPasswordResetService";

    /**
     * User password attribute name.
     */
    String ATTR_USER_PASSWORD = "userpassword";

    /**                                         
     * User status attribute name.
     */
    String ATTR_USER_STATUS = "inetuserstatus";

    /**
     * User resource offering attribute name.
     */
    String ATTR_USER_RESOURCE_OFFERING = "sunIdentityServerDiscoEntries";

    /**
     * Discovery service name.
     */
    String DISCOVERY_SERVICE = "sunIdentityServerDiscoveryService";

    /**
     * Discovery service's Server Dynamic Discovery Entry attribute name.
     */
    String DISCOVERY_SERVICE_NAME_DYNAMIC_DISCO_ENTRIES =
        "sunIdentityServerDynamicDiscoEntries";

    /**
     * Discovery service's Server Bootstrapping Discovery Entry attribute name.
     */
    String DISCOVERY_SERVICE_NAME_BOOTSTRAP_RES_OFF =
        "sunIdentityServerBootstrappingDiscoEntry";

    /**
     * Discovery service's Provider Resource ID Mapper attribute name.
     */
    String DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER =
        "sunIdentityServerDiscoProviderResourceIDMapper";

    /**
     * Discovery service's Provider Resource ID Mapper attribute's provider Id.
     */
    String DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER_PROVIDER_ID =
        "providerid";

    /**
     * Discovery service's Provider Resource ID Mapper attribute's Id Mapper.
     */
    String DISCOVERY_SERVICE_PROVIDER_RESOURCE_ID_MAPPER_ID_MAPPER =
        "idmapper";

    /**
     * Authentication Configuration service name.
     */
    String AUTH_CONFIG_SERVICE = "iPlanetAMAuthConfiguration";

    /**
     * Policy Configuration service name.
     */
    String POLICY_SERVICE = "iPlanetAMPolicyConfigService";

    /** 
     * User Search key 
     */
    String CONSOLE_USER_SEARCH_KEY =
        "iplanet-am-admin-console-user-search-key";

    /**
      * Enable password reset service.
      */
     String ATTR_USER_OLD_PASSWORD =
         "iplanet-am-admin-console-password-reset-enabled";

    /** 
     * User Search Result return key 
     */
    String CONSOLE_USER_SEARCH_RETURN_KEY =
        "iplanet-am-admin-console-user-return-attribute";

    /**
     * Adminstration Console service name.
     */
    String ADMIN_CONSOLE_SERVICE =
        "iPlanetAMAdminConsoleService";

    String CONSOLE_LOCATION_DN =
        "com-iplanet-am-console-location-dn";

    int DEFAULT_SEARCH_TIMEOUT = 5;


    /** plain text password attribute */
    String PASSWORD = "iPlanetPlainTextPassword";

    /** encrypted password attribute */
    String ENCRYPTED = "iPlanetEncryptedPassword";

    String G11N_SERVICE_NAME = "iPlanetG11NSettings";

    /** Common Name format attribute in Globalization service. */
    String G11N_SERIVCE_COMMON_NAME_FORMAT =
        "sun-identity-g11n-settings-common-name-format";

    /**
     * Preferred Locale attribute name in user service
     */
    String USER_SERVICE_PREFERRED_LOCALE = "preferredlocale";

    /** 
     * Customized Organization JSP directory 
     */
    String CONSOLE_ORG_CUSTOM_JSP_DIRECTORY = 
        "iplanet-am-admin-console-custom-jsp-dir";

    /**
     * ID Repo Service Name.
     */
    String IDREPO_SERVICE_NAME = "sunIdentityRepositoryService";

    /**
     * Platform Service.
     */
    String PLATFORM_SERVICE = "iPlanetAMPlatformService";

    /**
     * Session Service.
     */
    String SESSION_SERVICE = "iPlanetAMSessionService";

    /**
     * Read permission.
     */
    String PERMISSION_READ = "READ";

    /**
     * Write permission.
     */
    String PERMISSION_MODIFY = "MODIFY";

    /**
     * Delegation permission.
     */
    String PERMISSION_DELEGATE = "DELEGATE";

    /**
     * Name of the calling viewbean. Used to return to the previous view
     */
    String SAVE_VB_NAME = "returnToViewBean";

    /**
     * Name of selected response provider in Policy Configuration Service.
     */
    String POLICY_SELECTED_RESPONSE_PROVIDER =
        "iplanet-am-policy-selected-responseproviders";

    /**
     * Role Display Options Attribute Name.
     */
    String ROLE_DISPLAY_OPTION_ATTRIBUTE_NAME =
        "iplanet-am-role-display-options";

    /**
     * User Management Enabled Attribute name. This attribute is found in
     * Administration Console service.
     */
    String CONSOLE_UM_ENABLED_ATTR = "iplanet-am-admin-console-um-enabled";

    /**
     * Agent Configuration property which tells where the agent configuration
     * is being stored.
     */
    String AGENT_REPOSITORY_LOCATION_ATTR =
            "com.sun.identity.agents.config.repository.location";

    /**
     * Image displayed in version popup.
     */
    final String IMAGES_PRIMARY_PRODUCT_NAME_PNG = "/images/PrimaryProductName.png";

}
