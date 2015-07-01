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
 * $Id: ConfigurationInstanceImpl.java,v 1.12 2009/10/29 00:03:50 exu Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.plugin.configuration.impl;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.shared.Constants; 

/**
 * <code>ConfigurationInstanceImpl</code> is the implementation that provides
 * the operations on service configuration. 
 */
public class ConfigurationInstanceImpl implements ConfigurationInstance {

    private static Map serviceNameMap = new HashMap();
    private ServiceSchemaManager ssm;
    private ServiceConfigManager scm;
    private String componentName = null;
    private String subConfigId = null;
    private boolean hasOrgSchema = false;
    private static final int SUBCONFIG_PRIORITY = 0;
    private static final String RESOURCE_BUNDLE = "fmConfigurationService";
    static Debug debug = Debug.getInstance("libPlugins");

    private SSOToken ssoToken;

    static {
        serviceNameMap = new HashMap();
        serviceNameMap.put("SAML1", "iPlanetAMSAMLService");
        serviceNameMap.put("SAML2", "sunFMSAML2MetadataService");
        serviceNameMap.put("WS-FEDERATION", "sunFMWSFederationMetadataService");
        serviceNameMap.put("ID-FF_META", "sunFMIDFFMetadataService");
        serviceNameMap.put("LIBCOT","sunFMCOTConfigService");
        serviceNameMap.put("ID-FF", "iPlanetAMProviderConfigService");
        serviceNameMap.put("AUTHN_SVC", "sunIdentityServerAuthnService");
        serviceNameMap.put("DISCO", "sunIdentityServerDiscoveryService");
        serviceNameMap.put("IDPP", "sunIdentityServerLibertyPPService");
        serviceNameMap.put("SOAP_BINDING", "sunIdentityServerSOAPBinding");
        serviceNameMap.put("PLATFORM", "iPlanetAMPlatformService");
        serviceNameMap.put("NAMING", "iPlanetAMNamingService");
        serviceNameMap.put("AUTHN", "iPlanetAMAuthService");
        serviceNameMap.put("SAML2_SOAP_BINDING","sunfmSAML2SOAPBindingService");
        serviceNameMap.put("MULTI_PROTOCOL","sunMultiFederationProtocol");
        serviceNameMap.put("STS_CONFIG","sunFAMSTSService");
        serviceNameMap.put("SAML2_CONFIG", "sunFAMSAML2Configuration"); 
    }

    private SSOToken getSSOToken() {
        return (ssoToken != null) ? ssoToken :
            (SSOToken)AccessController.doPrivileged(
                AdminTokenAction.getInstance());
    }

    /**
     * Initializer.
     * @param componentName Name of the components, e.g. SAML1, SAML2, ID-FF
     * @param session FM Session object.
     * @exception ConfigurationException if could not initialize the instance.
     */
    public void init(String componentName, Object session) 
        throws ConfigurationException {

        String serviceName = (String)serviceNameMap.get(componentName);
        if (serviceName == null) {
            throw new ConfigurationException(RESOURCE_BUNDLE,
                "componentNameUnsupported", null);
        }
        if ((session != null) && (session instanceof SSOToken)) {
            ssoToken = (SSOToken)session;
        }

        try {
            SSOToken adminToken = getSSOToken();
            ssm = new ServiceSchemaManager(serviceName, adminToken);
            ServiceSchema oss = ssm.getOrganizationSchema();
            if (oss != null) {
                hasOrgSchema = true;
                Set subSchemaNames = oss.getSubSchemaNames();
                if ((subSchemaNames != null) && (subSchemaNames.size() == 1)) {
                    subConfigId = (String)subSchemaNames.iterator().next();
                }
            }
            scm = new ServiceConfigManager(serviceName, adminToken);
        } catch (SMSException smsex) {
            debug.error("ConfigurationInstanceImpl.init:", smsex);
            throw new ConfigurationException(smsex);
        } catch (SSOException ssoex) {
            debug.error("ConfigurationInstanceImpl.init:", ssoex);
            throw new ConfigurationException(ssoex);
        } 

        this.componentName = componentName;       
    }

    /**
     * Returns Configurations.
     * @param realm the name of organization at which the configuration resides.
     * @param configName configuration instance name. e.g. "/sp".
     *     The configName could be null or empty string, which means the default
     *     configuration for this components. 
     * @return Map of key/value pairs, key is the attribute name, value is
     *     a Set of attribute values or null if service configuration doesn't
     *     doesn't exist. If the configName parameter is null or empty, and OrganizationalConfig state is present,
     *     this state will be merged with the GlobalConfig attributes, with the OrganizationConfig attributes
     *     over-writing the GlobalConfig attributes, in case GlobalConfig and OrganizationConfig attributes share the
     *     same key.
     * @exception ConfigurationException if an error occurred while getting
     *     service configuration.
     */
    public Map getConfiguration(String realm, String configName)
        throws ConfigurationException {

        if (debug.messageEnabled()) {
            debug.message("ConfigurationInstanceImpl.getConfiguration: " +
                "componentName = " + componentName + ", realm = " + realm +
                ", configName = " + configName);
        }

        try {
            if (hasOrgSchema) {
                ServiceConfig organizationConfig = null;
                organizationConfig = scm.getOrganizationConfig(realm, null);

                if (organizationConfig == null) {
                    return null;
                }

                if ((configName == null) || (configName.length() == 0)) {
                    Map organizationAttributes = organizationConfig.getAttributes();
                    ServiceConfig globalConfig = scm.getGlobalConfig(configName);
                    if (globalConfig != null) {
                        Map mergedAttributes = globalConfig.getAttributes();
                        mergedAttributes.putAll(organizationAttributes);
                        return mergedAttributes;
                    }
                    return organizationAttributes;
                } else {
                    if (subConfigId == null) {
                        if (debug.messageEnabled()) {
                            debug.message("ConfigurationInstanceImpl." +
                                "getConfiguration: sub configuraton not " +
                                "supported.");
                        }
                        String[] data = { componentName };
                        throw new ConfigurationException(RESOURCE_BUNDLE,
                            "noSubConfig", data);
                    }
                    organizationConfig = organizationConfig.getSubConfig(configName);
                    if (organizationConfig == null) {
                        return null;
                    }

                    return organizationConfig.getAttributes();
                }
            } else {
                if ((realm != null) && (!realm.equals("/"))) {
                    if (debug.messageEnabled()) {
                        debug.message("ConfigurationInstanceImpl." +
                            "getConfiguration: organization configuraton not "+
                            "supported.");
                    }
                    String[] data = { componentName };
                    throw new ConfigurationException(RESOURCE_BUNDLE,
                        "noOrgConfig", data);
                }
                ServiceSchema ss = ssm.getGlobalSchema();
                if (ss == null) {
                    if (debug.messageEnabled()) {
                        debug.message("ConfigurationInstanceImpl." +
                            "getConfiguration: configuraton not " +
                            "supported.");
                    }

                    String[] data = { componentName };
                    throw new ConfigurationException(RESOURCE_BUNDLE,
                        "noConfig", data);
                }

                Map retMap = ss.getAttributeDefaults();
                if (componentName.equals("PLATFORM")) {
                    SSOToken token = getSSOToken();
                    retMap.put(Constants.PLATFORM_LIST, 
                        ServerConfiguration.getServerInfo(token));
                    retMap.put(Constants.SITE_LIST, 
                        SiteConfiguration.getSiteInfo(token));
                 }
                 return retMap;
            }
        } catch (SMSException smsex) {
            debug.error("ConfigurationInstanceImpl.getConfiguration:", smsex);
            String[] data = { componentName, realm };
            throw new ConfigurationException(RESOURCE_BUNDLE,
                "failedGetConfig", data);
        } catch (SSOException ssoex) {
            debug.error("ConfigurationInstanceImpl.getConfiguration:", ssoex);
            String[] data = { componentName, realm };
            throw new ConfigurationException(RESOURCE_BUNDLE,
                "failedGetConfig", data);
        }
    }

    /**
     * Sets Configurations.
     * @param realm the name of organization at which the configuration resides.
     * @param configName configuration instance name. e.g. "/sp"
     *     The configName could be null or empty string, which means the default
     *     configuration for this components.
     * @param avPairs Map of key/value pairs to be set in the service
     *     configuration, key is the attribute name, value is
     *     a Set of attribute values. 
     * @exception ConfigurationException if could not set service configuration
     *     or service configuration doesn't exist.
     */
    public void setConfiguration(String realm,
        String configName, Map avPairs)
        throws ConfigurationException {

        if (debug.messageEnabled()) {
            debug.message("ConfigurationInstanceImpl.setConfiguration: " +
                "componentName = " + componentName + ", realm = " + realm +
                ", configName = " + configName + ", avPairs = " + avPairs);
        }

        try {
            if (hasOrgSchema) {
                ServiceConfig sc = null;
                sc = scm.getOrganizationConfig(realm, null);

                if (sc == null) {
                    String[] data = { componentName, realm };
                    throw new ConfigurationException(RESOURCE_BUNDLE, 
                        "configNotExist", data);
                }

                if ((configName == null) || (configName.length() == 0)) {
                    sc.setAttributes(avPairs);
                } else {
                    if (subConfigId == null) {
                        if (debug.messageEnabled()) {
                            debug.message("ConfigurationInstanceImpl." +
                                "setConfiguration: sub configuraton not " +
                                "supported.");
                        }
                        String[] data = { componentName };
                        throw new ConfigurationException(RESOURCE_BUNDLE,
                            "noSubConfig", data);
                    }
                    sc = sc.getSubConfig(configName);
                    if (sc == null) {
                        String[] data = { componentName, realm };
                        throw new ConfigurationException(RESOURCE_BUNDLE, 
                            "configNotExist", data);
                    }

                    sc.setAttributes(avPairs);
                }
            } else {
                if ((realm != null) && (!realm.equals("/"))) {
                    if (debug.messageEnabled()) {
                        debug.message("ConfigurationInstanceImpl." +
                            "setConfiguration: organization configuraton not "+
                            "supported.");
                    }
                    String[] data = { componentName };
                    throw new ConfigurationException(RESOURCE_BUNDLE,
                        "noOrgConfig", data);
                }
                ServiceSchema ss = ssm.getGlobalSchema();
                if (ss == null) {
                    if (debug.messageEnabled()) {
                        debug.message("ConfigurationInstanceImpl." +
                            "setConfiguration: configuraton not " +
                            "supported.");
                    }
                    String[] data = { componentName };
                    throw new ConfigurationException(RESOURCE_BUNDLE,
                        "noConfig", data);
                }

                ss.setAttributeDefaults(avPairs);
            }
        } catch (SMSException smsex) {
            debug.error("ConfigurationInstanceImpl.setConfiguration:", smsex);
            String[] data = { componentName, realm };
            throw new ConfigurationException(RESOURCE_BUNDLE, 
                "failedSetConfig", data);
        } catch (SSOException ssoex) {
            debug.error("ConfigurationInstanceImpl.setConfiguration:", ssoex);
            String[] data = { componentName, realm };
            throw new ConfigurationException(RESOURCE_BUNDLE, 
                "failedSetConfig", data);
        }
    }

    /**
     * Creates Configurations.
     * @param realm the name of organization at which the configuration resides.
     * @param configName service configuration name. e.g. "/sp"
     *     The configName could be null or empty string, which means the
     *     default configuration for this components.
     * @param avPairs Map of key/value pairs to be set in the service
     *     configuration, key is the attribute name, value is
     *     a Set of attribute values. 
     * @exception ConfigurationException if could not create service 
     *     configuration.
     */
    public void createConfiguration(String realm, String configName,
        Map avPairs)
        throws ConfigurationException {

        if (debug.messageEnabled()) {
            debug.message("ConfigurationInstanceImpl.createConfiguration: " +
                "componentName = " + componentName + ", realm = " + realm +
                ", configName = " + configName + ", avPairs = " + avPairs);
        }

        try {
            if (hasOrgSchema) {
                ServiceConfig sc = null;
                sc = scm.getOrganizationConfig(realm, null);

                if ((configName == null) || (configName.length() == 0)) {
                    scm.createOrganizationConfig(realm, avPairs);
                } else {
                    if (subConfigId == null) {
                        if (debug.messageEnabled()) {
                            debug.message("ConfigurationInstanceImpl." +
                                "createConfiguration: sub configuraton not " +
                                "supported.");
                        }
                        String[] data = { componentName };
                        throw new ConfigurationException(RESOURCE_BUNDLE,
                            "noSubConfig", data);
                    }

                    if (sc == null) {
                        sc = scm.createOrganizationConfig(realm, null);
                    } else if (sc.getSubConfigNames().contains(configName)) {
                        String[] data = { componentName, realm, configName };
                        throw new ConfigurationException(RESOURCE_BUNDLE, 
                            "configExist", data);
                    }

                    sc.addSubConfig(configName, subConfigId,
                        SUBCONFIG_PRIORITY, avPairs);
                }
            } else {
                if (debug.messageEnabled()) {
                    debug.message("ConfigurationInstanceImpl." +
                        "createConfiguration: configuraton creation not " +
                        "supported.");
                }
                String[] data = { componentName };
                throw new ConfigurationException(RESOURCE_BUNDLE,
                    "noConfigCreation", data);
            }
        } catch (SMSException smsex) {
            debug.error("ConfigurationInstanceImpl.createConfiguration:",
                smsex);
            String[] data = { componentName, realm };
            throw new ConfigurationException(RESOURCE_BUNDLE,
                "failedCreateConfig", data);
        } catch (SSOException ssoex) {
            debug.error("ConfigurationInstanceImpl.createConfiguration:",
                ssoex);
            String[] data = { componentName, realm };
            throw new ConfigurationException(RESOURCE_BUNDLE,
                "failedCreateConfig", data);
        }
    }

    /**
     * Deletes Configuration.
     * @param realm the name of organization at which the configuration resides.
     * @param configName service configuration name. e.g. "/sp"
     *     The configName could be null or empty string, which means the default
     *     configuration for this components.
     * @param attributes A set of attributes to be deleted from the Service
     *     configuration. If the value is null or empty, deletes all service 
     *     configuration.
     * @exception ConfigurationException if could not delete service 
     *     configuration.
     */
    public void deleteConfiguration(String realm, 
        String configName, Set attributes)
        throws ConfigurationException {

        if (debug.messageEnabled()) {
            debug.message("ConfigurationInstanceImpl.deleteConfiguration: " +
                "componentName = " + componentName + ", realm = " + realm +
                ", configName = " + configName + ", attributes = " +
                attributes);
        }

        boolean removeConfig = (attributes == null) || (attributes.isEmpty());
        try {
            if (hasOrgSchema) {
                ServiceConfig sc = null;
                if ((configName == null) || (configName.length() == 0)) {
                    if (removeConfig) {
                        scm.removeOrganizationConfiguration(realm, null);
                    } else {
                        sc = scm.getOrganizationConfig(realm, null);
                        if (sc != null) {
                            sc.removeAttributes(attributes);
                        }
                    }
                } else {
                    if (subConfigId == null) {
                        if (debug.messageEnabled()) {
                            debug.message("ConfigurationInstanceImpl." +
                                "deleteConfiguration: sub configuraton not " +
                                "supported.");
                        }
                        String[] data = { componentName };
                        throw new ConfigurationException(RESOURCE_BUNDLE,
                            "noSubConfig", data);
                    }

                    sc = scm.getOrganizationConfig(realm, null);
                    if (sc != null) {
                        if (removeConfig) {
                            sc.removeSubConfig(configName);
                        } else {
                            sc = sc.getSubConfig(configName);
                            if (sc != null) {
                                sc.removeAttributes(attributes);
                            }
                        }
                    }
                }
            } else {
                if (debug.messageEnabled()) {
                    debug.message("ConfigurationInstanceImpl." +
                        "deleteConfiguration: configuraton deletion not " +
                        "supported.");
                }
                String[] data = { componentName };
                throw new ConfigurationException(RESOURCE_BUNDLE,
                    "noConfigDeletion", data);
            }
        } catch (SMSException smsex) {
            debug.error("ConfigurationInstanceImpl.deleteConfiguration:",
                smsex);
            String[] data = { componentName, realm };
            throw new ConfigurationException(RESOURCE_BUNDLE, 
                "failedDeleteConfig", data);
        } catch (SSOException ssoex) {
            debug.error("ConfigurationInstanceImpl.deleteConfiguration:",
                ssoex);
            String[] data = { componentName, realm };
            throw new ConfigurationException(RESOURCE_BUNDLE, 
                "failedDeleteConfig", data);
        }

    }

    /**
     * Returns all service config name for this components.
     * @param realm the name of organization at which the configuration resides.
     * @return Set of service configuration names. Return null if there 
     *     is no service configuration for this component, return empty set
     *     if there is only default configuration instance.
     * @exception ConfigurationException if could not get all service 
     *     configuration names.
     */
    public Set getAllConfigurationNames(String realm) 
        throws ConfigurationException {

        if (debug.messageEnabled()) {
            debug.message("ConfigurationInstanceImpl.getAllConfigurationNames"+
                ": realm = " + realm + ", componentName = " + componentName);
        }
        try {
            if (hasOrgSchema) {
                ServiceConfig sc = scm.getOrganizationConfig(realm, null);
                if (sc == null) {
                    return null;
                }
                Set subConfigNames = sc.getSubConfigNames();
                if ((subConfigNames != null) && (subConfigNames.size() > 0)) {
                    return subConfigNames;
                } else {
                    return Collections.EMPTY_SET;
                }
            } else {
                if ((realm != null) && (!realm.equals("/"))) {
                    return null;
                }
                ServiceSchema ss = ssm.getGlobalSchema();
                if (ss == null) {
                    return null;
                } else {
                    return Collections.EMPTY_SET;
                }
            }
        } catch (SMSException smsex) {
            debug.error("ConfigurationInstanceImpl.getAllConfigurationNames:",
                smsex);

            String[] data = { componentName, realm };
            throw new ConfigurationException(RESOURCE_BUNDLE,
                "failedGetConfigNames", data);
        } catch (SSOException ssoex) {
            debug.error("ConfigurationInstanceImpl.getAllConfigurationNames:",
                ssoex);

            String[] data = { componentName, realm };
            throw new ConfigurationException(RESOURCE_BUNDLE,
                "failedGetConfigNames", data);
        }
    }

    /**
     * Registers for changes to the component's configuration. The object will
     * be called when configuration for this component is changed.
     * @return the registered id for this listener instance.
     * @exception ConfigurationException if could not register the listener.
     */
    public String addListener(ConfigurationListener listener)
        throws ConfigurationException {

        if (hasOrgSchema) {
            return scm.addListener(new ServiceListenerImpl(listener,
                                                           componentName));
        } else {
            return ssm.addListener(new ServiceListenerImpl(listener,
                                                           componentName));
        }
    }

    /**
     * Unregisters the listener from the component for the given
     * listener ID. The ID was issued when the listener was registered.
     * @param listenerID the returned id when the listener was registered.
     * @exception ConfigurationException if could not register the listener.
     */
    public void removeListener(String listenerID)
        throws ConfigurationException {

        if (hasOrgSchema) {
            scm.removeListener(listenerID);
        } else {
            ssm.removeListener(listenerID);
        }
    }
}
