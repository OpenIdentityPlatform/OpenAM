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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.server.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.radius.server.spi.AccessRequestHandler;

import com.google.inject.ConfigurationException;
import com.google.inject.ProvisionException;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.datastruct.ValueNotFoundException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * Loads configuration values from openam's admin console maintained values into pojos and registers a change listener
 * for changes that may happen in the future .
 */
public class ConfigLoader {

    private static final Debug LOG = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    /**
     * The ID of the listener in case we ever need to unregister it. If it is null then we haven't yet installed our
     * listener for admin console handlerConfig changes to our constructs.
     */
    private final String listenerId = null;

    /**
     * Service config manager from which RADIUS Server config may be loaded.
     */
    private final ServiceConfigManager serviceConfigManager;

    /**
     * Constructor.
     *
     * @param serviceConfigManager - Service config manager from which RADIUS Server config may be loaded.
     */
    @Inject
    public ConfigLoader(@Named("RadiusServer") ServiceConfigManager serviceConfigManager) {
        this.serviceConfigManager = serviceConfigManager;
    }

    /**
     * Loads the configured global RADIUS Service values and declared clients as specified in openAM's admin console via
     * registration of those properties via the amRadiusServer.xml file. We load them here into simple pojos for caching
     * in memory. If we are unable to do so this method will return a null value.
     *
     * @return an object containing the configuration parameters for the radius service, or null if the config can't be
     *         loaded.
     */
    public RadiusServiceConfig loadConfig() {
        try {

            // now get the fields in the Configuration tab, Global sub-tab, Global Properties table, RADIUS client
            // page
            RadiusServiceConfig cfg = null;
            final ServiceConfig serviceConf = serviceConfigManager.getGlobalConfig("default");

            if (serviceConf != null) {
                final Map<String, Set<String>> configAttributes = serviceConf.getAttributes();
                final boolean isEnabled = "YES".equals(CollectionHelper.getMapAttrThrows(configAttributes,
                        RadiusServerConstants.GBL_ATT_LISTENER_ENABLED));
                final int listenerPort = CollectionHelper.getIntMapAttr(configAttributes,
                        RadiusServerConstants.GBL_ATT_LISTENER_PORT, -1, LOG);
                final int coreThreads = CollectionHelper.getIntMapAttr(configAttributes,
                        RadiusServerConstants.GBL_ATT_THREADS_CORE_SIZE, -1, LOG);
                final int maxThreads = CollectionHelper.getIntMapAttr(configAttributes,
                        RadiusServerConstants.GBL_ATT_THREADS_MAX_SIZE, -1, LOG);
                final int queueSize = CollectionHelper.getIntMapAttr(configAttributes,
                        RadiusServerConstants.GBL_ATT_QUEUE_SIZE, -1, LOG);
                final int keepaliveSeconds = CollectionHelper.getIntMapAttr(configAttributes,
                        RadiusServerConstants.GBL_ATT_THREADS_KEEPALIVE_SECONDS, -1, LOG);

                final ThreadPoolConfig poolCfg = new ThreadPoolConfig(coreThreads, maxThreads, queueSize,
                        keepaliveSeconds);

                // now get the RADIUS client instances from the secondary configuration instances table in the
                // Configuration tab, Global sub-tab, Global Properties table, RADIUS client page
                final Set<String> clientConfigNames = serviceConf.getSubConfigNames();
                final List<ClientConfig> definedClientConfigs = new ArrayList<ClientConfig>();
                for (final String clientConfigName : clientConfigNames) {
                    try {
                        final ClientConfig clientConfig = new ClientConfig(); // create object for holding values in
                                                                              // memory
                        clientConfig.setName(clientConfigName);
                        final ServiceConfig clientCfg = serviceConf.getSubConfig(clientConfigName); // go get our admin
                        // console values
                        final Map<String, Set<String>> map = clientCfg.getAttributes();

                        clientConfig.setIpaddr(CollectionHelper.getMapAttrThrows(map,
                                RadiusServerConstants.CLIENT_ATT_IP_ADDR));
                        clientConfig.setSecret(CollectionHelper.getMapAttrThrows(map,
                                RadiusServerConstants.CLIENT_ATT_SECRET));
                        final Boolean setLogPackets = "YES".equals(CollectionHelper.getMapAttrThrows(map,
                                RadiusServerConstants.CLIENT_ATT_LOG_PACKETS));
                        clientConfig.setLogPackets(setLogPackets);
                        clientConfig.setAccessRequestHandlerClassname(CollectionHelper.getMapAttrThrows(map,
                                RadiusServerConstants.CLIENT_ATT_CLASSNAME));

                        final Class accessRequestHandlerClass = validateClass(clientConfig);
                        if (accessRequestHandlerClass == null) {
                            throw new ClientConfigurationException(clientConfigName,
                                    RadiusServerConstants.CLIENT_ATT_CLASSNAME);
                        } else {
                            clientConfig.setAccessRequestHandler(accessRequestHandlerClass);
                            clientConfig.setClassIsValid(true);
                        }

                        final Set<String> properties = map.get(RadiusServerConstants.CLIENT_ATT_PROPERTIES);
                        if (properties != null) {
                            clientConfig.setHandlerConfig(extractProperties(properties));
                        } else {
                            LOG.warning("No properties defined for handler.");
                        }

                        definedClientConfigs.add(clientConfig);
                    } catch (final ValueNotFoundException vnfe) {
                        LOG.error(vnfe.getMessage() + " in RADIUS client config '" + clientConfigName + "'. Requests "
                                + "from this client will be ignored.");
                    } catch (final ClientConfigurationException e) {
                        LOG.error(e.getMessage());
                    }
                }
                cfg = new RadiusServiceConfig(isEnabled, listenerPort, poolCfg,
                        definedClientConfigs.toArray(new ClientConfig[0]));
            }
            return cfg;

        } catch (final Exception e) {
            LOG.error("Unable to load RADIUS Service Configuration", e);
        }
        return null;
    }

    /**
     * Validates that the specified class can be loaded and implements the proper interface so that we don't have to do
     * that for every request.
     *
     * @param cfg
     * @return the class of the RequestHandler to be used for this client config, or null if the class is invalid.
     */
    @SuppressWarnings("unchecked")
    private Class validateClass(ClientConfig cfg) {
        Class clazz = null;
        try {
            clazz = Class.forName(cfg.getAccessRequestHandlerClassname());
        } catch (final ClassNotFoundException e) {
            LOG.error("Unable to load Handler Class '" + cfg.getAccessRequestHandlerClassname()
                    + "' for RADIUS client '" + cfg.getName() + "'. Requests from this client will be ignored.", e);
            return null;
        }
        Object inst = null;

        try {
            inst = InjectorHolder.getInstance(clazz);
        } catch (ConfigurationException | ProvisionException e) {
            LOG.error("Unable to instantiate Handler Class '" + cfg.getAccessRequestHandlerClassname()
                    + "' for RADIUS client '" + cfg.getName() + "'. Requests from this client will be ignored.", e);
            return null;
        }

        AccessRequestHandler handler = null;
        try {
            handler = (AccessRequestHandler) inst;
        } catch (final ClassCastException e) {
            LOG.error("Unable to use Handler Class '" + cfg.getAccessRequestHandlerClassname()
                    + "' for RADIUS client '" + cfg.getName() + "'. Requests from this client will be ignored.", e);
            return null;
        }

        return clazz;
    }

    /**
     * Utility method to extract multiple values from the Set wrapper and place them in a java Properties object. Each
     * item is parsed as a key followed by an equals character followed by a value. If there is no equals sign then the
     * item is entered into a the properties object with an empty string value
     *
     * @param wrappingSet
     * @return
     */
    private Properties extractProperties(Set<String> wrappingSet) {
        final String[] vals = wrappingSet.toArray(RadiusServerConstants.EMPTY_STRING_ARY);
        final Properties cfg = new Properties();

        for (final String val : vals) {
            final int idx = val.indexOf('=');

            if (idx == -1) {
                cfg.setProperty(val, "");
            } else {
                cfg.setProperty(val.substring(0, idx), val.substring(idx + 1));
            }
        }
        return cfg;
    }
}
