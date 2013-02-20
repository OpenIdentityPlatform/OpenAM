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
 * $Id: IConfigurationKeyConstants.java,v 1.9 2008/08/21 23:34:15 huacui Exp $
 *
 */

package com.sun.identity.agents.arch;

/**
 * <p>
 * Constants used for identifying configuration keys. The constant definitions
 * are split into three categories. The first category is the set of common
 * prefix strings that may be used to define the actual configuration keys. The
 * second category defines all the sub-keys that identify the configuration
 * keys to be created. The third category defines the global configuration keys
 * as well as the module specific configuration key formats where needed.
 * </p><p>
 * A typical configuration key can be described as follows:
 * </p>
 * <pre>
 * &lt;common prefix&gt;&lt;optional module name&gt;&lt;configuration subkey&gt;
 * </pre>
 * <p>
 * For example if the common prefix is set to 
 * <code>com.sun.identity.agents.j2ee.</code> and the value of the sub-key is
 * set to <code>some.attribute</code>, then the global configuration key will
 * be <code>com.sun.identity.agents.j2ee.some.attribute</code>. Also, for the 
 * module <code>foo</code>, the format evaluation of the key name using the
 * module name would result in the key: 
 * <code>com.sun.identity.agents.j2ee.foo.some.attribute</code>.
 * </p>
 */
public interface IConfigurationKeyConstants {
    
   //--------- Common Prefix and shared string definitions ----//

    /**
     * The configuration prefix which identifies 
     */
     public static final String AGENT_CONFIG_PREFIX = 
         "com.sun.identity.agents.config.";
     
    /**
     * The prefix for all locale specific configuration.
     */
     public static final String STR_LOCALE_PREFIX =
         "locale.";
     

    //------- Configuration Subkey Definitions -----------------//
    
    // Non-Static subkeys: These keys may be reloaded by hot-swap
     
    /**
     * Configuration subkey for config location
     */ 
     public static final String CONFIG_SUBKEY_CONFIG_REPOSITORY_LOCATION = 
        "repository.location";     
     
    /**
     * Configuration subkey for config load interval.
     */
     public static final String CONFIG_SUBKEY_LOAD_INTERVAL =
         "load.interval";
     
    // Static subkeys: These keys may not be loaded by hot-swap. 
    /**
     * Configuration subkey for service resolver.
     */
     public static final String CONFIG_SUBKEY_SERVICE_RESOLVER =
         "service.resolver";
     
    /**
     * Configuration subkey for user mapping mode.
     */
     public static final String CONFIG_SUBKEY_USER_MAPPING_MODE =
         "user.mapping.mode";
     
    /**
     * Configuration subkey for user attribute name.
     */
     public static final String CONFIG_SUBKEY_USER_ATTRIBUTE_NAME =
         "user.attribute.name";
     
    /**
     * Configuration subkey for use-DN flag.
     */
     public static final String CONFIG_SUBKEY_USER_PRINCIPAL =
         "user.principal";
     
    /**
     * Configuration subkey for locale-language.
     */
     public static final String CONFIG_SUBKEY_LOCALE_LANG =
         STR_LOCALE_PREFIX + "language";
     
    /**
     * Configuration subkey for locale-country.
     */
     public static final String CONFIG_SUBKEY_LOCALE_COUNTRY =
         STR_LOCALE_PREFIX + "country";
          
    /**
     * Configuration subkey for user-id property name.
     */
     public static final String CONFIG_SUBKEY_USER_ID_PROPERTY =
         "user.token";
     
     /**
      * Configuration subkey for audit log mode
      */
     public static final String CONFIG_SUBKEY_AUDIT_LOG_MODE =
         "audit.accesstype";
     
    /**
     * Configuration subkey for client IP header
     */
     public static final String CONFIG_SUBKEY_CLIENT_IP_HEADER =
         "client.ip.header";
     
    /**
     * Configuration subkey for client hostname header
     */
     public static final String CONFIG_SUBKEY_CLIENT_HOSTNAME_HEADER =
         "client.hostname.header";
     
    /**
     * Configuration subkey for organization name.
     */
     public static final String CONFIG_SUBKEY_ORG_NAME =
         "organization.name";
     
     public static final String CONFIG_CENTRALIZED_NOTIFICATION_ENABLE =
        AGENT_CONFIG_PREFIX + "change.notification.enable";

     public static final String DEFAULT_CENTRALIZED_NOTIFICATION_ENABLE = 
        "true";    
     
     /**
      * Configuration subkey to lock agent config in run-time.
      */
     public static final String CONFIG_SUBKEY_LOCK_ENABLE = 
        "lock.enable";    
     
     /**
      * Configuration subkey for agent profile name .
      */
     public static final String CONFIG_SUBKEY_PROFILE_NAME = 
        "profilename";    
     
             
    /**
     * A list of all configuration subkeys that do not participate in hot-swap.
     */
     public static final String[] CONFIG_STATIC_SUBKEY_LIST = new String[] {
             CONFIG_SUBKEY_CONFIG_REPOSITORY_LOCATION,
             CONFIG_SUBKEY_SERVICE_RESOLVER,
             CONFIG_SUBKEY_LOCALE_LANG,
             CONFIG_SUBKEY_LOCALE_COUNTRY,
             CONFIG_SUBKEY_ORG_NAME,
             CONFIG_SUBKEY_LOCK_ENABLE
     };
     
    //------- Configuration Key and Format Definitions -----------//
     
    /**
     * Configuration key for configuration remote repository property.
     */
     public static final String CONFIG_REPOSITORY_LOCATION =
         AGENT_CONFIG_PREFIX + CONFIG_SUBKEY_CONFIG_REPOSITORY_LOCATION;
     
    /**
     * Configuration key for configuration reload interval property.
     */
     public static final String CONFIG_LOAD_INTERVAL =
         AGENT_CONFIG_PREFIX + CONFIG_SUBKEY_LOAD_INTERVAL;
     
    /**
     * Configuration key for service resolver property.
     */
     public static final String CONFIG_SERVICE_RESOLVER =
         AGENT_CONFIG_PREFIX + CONFIG_SUBKEY_SERVICE_RESOLVER;
     
    /**
     * Configuration key for user mapping mode.
     */
     public static final String CONFIG_USER_MAPPING_MODE =
         AGENT_CONFIG_PREFIX + CONFIG_SUBKEY_USER_MAPPING_MODE;
     
    /**
     * Configuration key for user attribute name.
     */
     public static final String CONFIG_USER_ATTRIBUTE_NAME =
         AGENT_CONFIG_PREFIX + CONFIG_SUBKEY_USER_ATTRIBUTE_NAME;
     
    /**
     * Configuration key for use-DN flag.
     */
     public static final String CONFIG_USER_PRINCIPAL =
         AGENT_CONFIG_PREFIX + CONFIG_SUBKEY_USER_PRINCIPAL;
     
    /**
     * Global configuration key for locale-language. 
     */
     public static final String CONFIG_LOCALE_LANG =
         AGENT_CONFIG_PREFIX + CONFIG_SUBKEY_LOCALE_LANG;
     
    /**
     * Global configuration key for locale-country.
     */
     public static final String CONFIG_LOCALE_COUNTRY =
         AGENT_CONFIG_PREFIX + CONFIG_SUBKEY_LOCALE_COUNTRY;
     
    /**
     * Global configuration key for user-id property name.
     */
     public static final String CONFIG_USER_ID_PROPERTY =
         AGENT_CONFIG_PREFIX + CONFIG_SUBKEY_USER_ID_PROPERTY;
     
    /**
     * Global configuration key for audit level property.
     */
     public static final String CONFIG_AUDIT_LOG_MODE =
         AGENT_CONFIG_PREFIX + CONFIG_SUBKEY_AUDIT_LOG_MODE;
     
    /**
     * Configuration subkey for client IP header
     */
     public static final String CONFIG_CLIENT_IP_HEADER =
              AGENT_CONFIG_PREFIX + CONFIG_SUBKEY_CLIENT_IP_HEADER;
     
     /**
      * Configuration key for client hostname header
      */
      public static final String CONFIG_CLIENT_HOSTNAME_HEADER =
               AGENT_CONFIG_PREFIX + CONFIG_SUBKEY_CLIENT_HOSTNAME_HEADER;  
      
     /**
      * Configuration key for organization name.
      */
      public static final String CONFIG_ORG_NAME =
          AGENT_CONFIG_PREFIX + CONFIG_SUBKEY_ORG_NAME;

     /**
      * Configuration key for locking agent config.
      */
      public static final String CONFIG_LOCK_ENABLE =
          AGENT_CONFIG_PREFIX + CONFIG_SUBKEY_LOCK_ENABLE;

     /**
      * Configuration key for agent profile name.
      */
      public static final String CONFIG_PROFILE_NAME =
          AGENT_CONFIG_PREFIX + CONFIG_SUBKEY_PROFILE_NAME;

}
