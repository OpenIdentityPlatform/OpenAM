/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 */
package com.iplanet.am.util;

import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.openam.utils.Time.*;

import com.iplanet.sso.SSOToken;
import com.sun.identity.common.AttributeStruct;
import com.sun.identity.common.PropertiesFinder;
import com.sun.identity.common.configuration.ConfigurationListener;
import com.sun.identity.common.configuration.ConfigurationObserver;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.SMSEntry;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

import org.forgerock.guava.common.base.Predicate;
import org.forgerock.guava.common.collect.ImmutableMap;
import org.forgerock.guava.common.collect.Maps;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.utils.StringUtils;

/**
 * This class provides functionality that allows single-point-of-access to all
 * related system properties.
 * <p>
 * The system properties can be set in couple of ways: programmatically by
 * calling the <code>initializeProperties</code> method, or can be statically
 * loaded at startup from a file named: 
 * <code>AMConfig.[class,properties]</code>.
 * Setting the properties through the API takes precedence and will replace the
 * properties loaded via file. For statically loading the properties via a file,
 * this class tries to first find a class, <code>AMConfig.class</code>, and
 * then a file, <code>AMConfig.properties</code> in the CLASSPATH accessible
 * to this code. The <code>AMConfig.class</code> takes precedence over the
 * flat file <code>AMConfig.properties</code>.
 * <p>
 * If multiple servers are running, each may have their own configuration file.
 * The naming convention for such scenarios is
 * <code>AMConfig-&lt;serverName></code>.
 * @supported.all.api
 */
public class SystemProperties {

    /**
     * Runtime flag to be set, in order to override the path of the
     * configuration file.
     */
    public static final String CONFIG_PATH = "com.iplanet.services.configpath";

    /**
     * Default name of the configuration file.
     */
    public static final String CONFIG_FILE_NAME = "serverconfig.xml";

    /**
     * New configuration file extension
     */
    public static final String PROPERTIES = "properties";

    public static final String NEWCONFDIR = "NEW_CONF_DIR";

    private static final String SERVER_NAME_PROPERTY = "server.name";

    private static final String CONFIG_NAME_PROPERTY = "amconfig";

    private static final String AMCONFIG_FILE_NAME = "AMConfig";

    /** Regular expression pattern for a sequence of 1 or more white space characters. */
    private static final String WHITESPACE = "\\s+";

    private static final Map<String, AttributeStruct> ATTRIBUTE_MAP = initAttributeMapping();

    private static final int TAG_START = '%';

    /**
     * Maps from tags to the system properties that they should be replaced with. System property values containing
     * these tags will be replaced with the actual values of these properties by {@link #get(String)}.
     */
    private static final Map<String, String> TAG_SWAP_PROPERTIES = ImmutableMap.<String, String>builder()
            .put("%SERVER_PORT%", Constants.AM_SERVER_PORT)
            .put("%SERVER_URI%", Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR)
            .put("%SERVER_HOST%", Constants.AM_SERVER_HOST)
            .put("%SERVER_PROTO%", Constants.AM_SERVER_PROTOCOL)
            .put("%BASE_DIR%", CONFIG_PATH)
            .put("%SESSION_ROOT_SUFFIX%", CoreTokenConstants.SYS_PROPERTY_SESSION_HA_REPOSITORY_ROOT_SUFFIX)
            .put("%SESSION_STORE_TYPE%", CoreTokenConstants.SYS_PROPERTY_SESSION_HA_REPOSITORY_TYPE)
            .build();

    private static final boolean SITEMONITOR_DISABLED;

    /**
     * Reference to the current properties map and tagswap values.
     */
    private static final AtomicReference<PropertiesHolder> propertiesHolderRef =
            new AtomicReference<>(new PropertiesHolder());

    private static String initError = null;
    private static String initSecondaryError = null;
    private static String instanceName = null;

    /*
     * Initialization to load the properties file for config information before
     * anything else starts.
     */
    static {
        try {
            // Load properties from file
            String serverName = System.getProperty(SERVER_NAME_PROPERTY);
            String configName = System.getProperty(CONFIG_NAME_PROPERTY, AMCONFIG_FILE_NAME);
            String fname = null;
            if (serverName != null) {
                serverName = serverName.replace('.', '_');
                fname = configName + "-" + serverName;
            } else {
                fname = configName;
            }
            initializeProperties(fname);
            PropertiesHolder props = propertiesHolderRef.get();

            // Get the location of the new configuration file in case
            // of single war deployment
            try {
                String newConfigFileLoc = props.getProperty(Constants.AM_NEW_CONFIGFILE_PATH);

                if (!StringUtils.isEmpty(newConfigFileLoc) && !NEWCONFDIR.equals(newConfigFileLoc)) {
                    String hostName = InetAddress.getLocalHost().getHostName().toLowerCase();
                    String serverURI = props.getProperty(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR).replace('/', '_')
                                            .toLowerCase();

                    String fileName = newConfigFileLoc + "/" + AMCONFIG_FILE_NAME + serverURI + hostName +
                            props.getProperty(Constants.AM_SERVER_PORT) + "." + PROPERTIES;

                    try {
                        props = loadProperties(props, fileName);
                    } catch (IOException ioe) {
                        try {
                            props = loadProperties(props, newConfigFileLoc + "/" + AMCONFIG_FILE_NAME + "." +
                                    PROPERTIES);
                        } catch (IOException ioe2) {
                            saveException(ioe2);
                        }
                    }
                    propertiesHolderRef.set(props);
                }
            } catch (Exception ex) {
                saveException(ex);
            }
        } catch (MissingResourceException e) {
            // Can't print the message to debug due to dependency
            // Save it as a String and provide when requested.
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            initError = sw.toString();
        }
        SITEMONITOR_DISABLED = Boolean.parseBoolean(getProp(Constants.SITEMONITOR_DISABLED, "false"));
    }

    private static PropertiesHolder loadProperties(PropertiesHolder props, String file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            Properties temp = new Properties();
            temp.load(fis);
            return props.putAll(temp);
        }
    }

    /**
     * Helper function to handle associated exceptions during initialization of
     * properties using external properties file in a single war deployment.
     */
    static void saveException(Exception ex) {
        // Save it as a String and provide when requested.
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        initSecondaryError = sw.toString();
    }

    /**
     * This method lets you query for a system property whose value is same as
     * <code>String</code> key. The method first tries to read the property
     * from java.lang.System followed by a lookup in the config file.
     *
     * @param key
     *            type <code>String</code>, the key whose value one is
     *            looking for.
     * @return the value if the key exists; otherwise returns <code>null</code>
     */
    public static String get(String key) {

        String answer = null;

        // look up values in SMS services only if in server mode.
        if (isServerMode() || SITEMONITOR_DISABLED) {
            AttributeStruct ast = ATTRIBUTE_MAP.get(key);
            if (ast != null) {
                answer = PropertiesFinder.getProperty(key, ast);
            }
        }

        if (answer == null) {
            answer = getProp(key);

            final Map<String, String> tagswapValues = propertiesHolderRef.get().tagSwapValues;
            if (answer != null && tagswapValues != null && answer.indexOf(TAG_START) != -1) {
                for (Map.Entry<String, String> tagSwapEntry : tagswapValues.entrySet()) {
                    String k = tagSwapEntry.getKey();
                    String val = tagSwapEntry.getValue();

                    if (k.equals("%SERVER_URI%")) {
                        if (!StringUtils.isEmpty(val)) {
                            if (val.charAt(0) == '/') {
                                answer = answer.replaceAll("/%SERVER_URI%", val);
                                String lessSlash = val.substring(1);
                                answer = answer.replaceAll("%SERVER_URI%", lessSlash);
                            } else {
                                answer = answer.replaceAll(k, val);
                            }
                        }
                    } else {
                        answer = answer.replaceAll(k, val);
                    }
                }

                if (answer.contains("%ROOT_SUFFIX%")) {
                    answer = answer.replaceAll("%ROOT_SUFFIX%", SMSEntry.getAMSdkBaseDN());
                }
            }
        }

        return answer;
    }

    private static String getProp(String key, String def) {
        String value = getProp(key);
        return ((value == null) ? def : value);
    }

    private static String getProp(String key) {
        String answer = System.getProperty(key);
        if (answer == null) {
            answer = propertiesHolderRef.get().getProperty(key);
        }
        return answer;
    }

    /**
     * This method lets you query for a system property whose value is same as
     * <code>String</code> key.
     *
     * @param key the key whose value one is looking for.
     * @param def the default value if the key does not exist.
     * @return the value if the key exists; otherwise returns default value.
     */
    public static String get(String key, String def) {
        String value = get(key);
        return value == null ? def : value;
    }

    /**
     * Returns the property value as a boolean
     *
     * @param key the key whose value one is looking for.
     * @return the boolean value if the key exists; otherwise returns false
     */
    public static boolean getAsBoolean(String key) {
        String value = get(key);
        return Boolean.parseBoolean(value);
    }

    /**
     * Returns the property value as a boolean
     *
     * @param key the property name.
     * @param defaultValue value if key is not found.
     * @return the boolean value if the key exists; otherwise the default value
     */
    public static boolean getAsBoolean(String key, boolean defaultValue) {
        String value = get(key);

        if (value == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }

    /**
     * @param key The System Property key to lookup.
     * @param defaultValue If the property was not set, or could not be parsed to an int.
     * @return Either the defaultValue, or the numeric value assigned to the System Property.
     */
    public static int getAsInt(String key, int defaultValue) {
        String value = get(key);

        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * @param key The System Property key to lookup.
     * @param defaultValue If the property was not set, or could not be parsed to a long.
     * @return Either the defaultValue, or the numeric value assigned to the System Property.
     */
    public static long getAsLong(String key, long defaultValue) {
        String value = get(key);

        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Parses a system property as a set of strings by splitting the value on the given delimiter expression.
     *
     * @param key The System Property key to lookup.
     * @param delimiterRegex The regular expression to use to split the value into elements in the set.
     * @param defaultValue The default set to return if the property does not exist.
     * @return the value of the property parsed as a set of strings.
     */
    public static Set<String> getAsSet(String key, String delimiterRegex, Set<String> defaultValue) {
        String value = get(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return asSet(value.split(delimiterRegex));
    }

    /**
     * Parses a system property as a set of strings by splitting the value on the given delimiter expression.
     *
     * @param key The System Property key to lookup.
     * @param delimiterRegex The regular expression to use to split the value into elements in the set.
     * @return the value of the property parsed as a set of strings or an empty set if no match is found.
     */
    public static Set<String> getAsSet(String key, String delimiterRegex) {
        return getAsSet(key, delimiterRegex, Collections.<String>emptySet());
    }

    /**
     * Parses a system property as a set of strings by splitting the value on white space characters.
     *
     * @param key The System Property key to lookup.
     * @return the value of the property parsed as a set of strings or an empty set if no match is found.
     */
    public static Set<String> getAsSet(String key) {
        return getAsSet(key, WHITESPACE);
    }

    /**
     * Returns all the properties defined and their values. This is a defensive copy of the properties and so updates
     * to the returned object will not be reflected in the actual properties used by OpenAM.
     *
     * @return Properties object with a copy of all the key value pairs.
     */
    public static Properties getProperties() {
        Properties properties = new Properties();
        properties.putAll(propertiesHolderRef.get().properties);
        return properties;
    }

    /**
     * This method lets you get all the properties defined and their values. The
     * method first tries to load the properties from java.lang.System followed
     * by a lookup in the config file.
     *
     * @return Properties object with all the key value pairs.
     *
     */
    public static Properties getAll() {
        Properties properties = new Properties();
        properties.putAll(propertiesHolderRef.get().properties);
        // Iterate over the System Properties & add them in result obj
        properties.putAll(System.getProperties());
        return properties;
    }

    /**
     * This method lets you query for all the platform properties defined and
     * their values. Returns a Properties object with all the key value pairs.
     *
     * @deprecated use <code>getAll()</code>
     *
     * @return the platform properties
     */
    public static Properties getPlatform() {
        return getAll();
    }

    /**
     * Initializes properties bundle from the <code>file<code> 
     * passed.
     *
     * @param file type <code>String</code>, file name for the resource bundle
     * @exception MissingResourceException
     */
    public static void initializeProperties(String file) throws MissingResourceException {
        Properties props = new Properties();
        ResourceBundle bundle = ResourceBundle.getBundle(file);
        // Copy the properties to props
        for (String key : bundle.keySet()) {
            props.put(key, bundle.getString(key));
        }
        initializeProperties(props, false, false);
    }

    public static void initializeProperties(Properties properties) {
        initializeProperties(properties, false);
    }

    /**
     * Initializes the properties to be used by OpenAM. Ideally this
     * must be called first before any other method is called within OpenAM.
     * This method provides a programmatic way to set the properties, and will
     * override similar properties if loaded for a properties file.
     *
     * @param properties properties for OpenAM
     * @param reset <code>true</code> to reset existing properties.
     */
    public static void initializeProperties(Properties properties, boolean reset) {
        initializeProperties(properties, reset, false);
    }

    /**
     * Initializes the properties to be used by OpenAM. Ideally this
     * must be called first before any other method is called within OpenAM.
     * This method provides a programmatic way to set the properties, and will
     * override similar properties if loaded for a properties file.
     *
     * @param properties properties for OpenAM.
     * @param reset <code>true</code> to reset existing properties.
     * @param withDefaults <code>true</code> to include default properties.
     */
    public static void initializeProperties(Properties properties, boolean reset, boolean withDefaults) {
        Properties defaultProp = null;
        if (withDefaults) {
            SSOToken appToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            defaultProp = ServerConfiguration.getDefaults(appToken);
        }

        PropertiesHolder oldProps;
        PropertiesHolder newProps;
        do {
            oldProps = propertiesHolderRef.get();
            final Properties combined = new Properties();
            if (defaultProp != null) {
                combined.putAll(defaultProp);
            }

            if (!reset) {
                combined.putAll(oldProps.properties);
            }

            combined.putAll(properties);

            newProps = new PropertiesHolder(Maps.fromProperties(combined));
        } while (!propertiesHolderRef.compareAndSet(oldProps, newProps));
    }

    /**
     * Initializes a property to be used by OpenAM. Ideally this
     * must be called first before any other method is called within OpenAM.
     * This method provides a programmatic way to set a specific property, and
     * will override similar property if loaded for a properties file.
     *
     * @param propertyName property name.
     * @param propertyValue property value.
     */
    public static void initializeProperties(String propertyName, String propertyValue) {
        Properties newProps = new Properties();
        newProps.put(propertyName, propertyValue);
        initializeProperties(newProps, false, false);
    }

    /**
     * Returns a counter for last modification. The counter is incremented if
     * the properties are changed by calling the following method
     * <code>initializeProperties</code>. This is a convenience method for
     * applications to track changes to OpenAM properties.
     *
     * @return counter of the last modification
     */
    public static long lastModified() {
        return propertiesHolderRef.get().lastModified;
    }

    /**
     * Returns error messages during initialization, else <code>null</code>.
     *
     * @return error messages during initialization
     */
    public static String getInitializationError() {
        return initError;
    }

    /**
     * Returns error messages during initialization using the single war
     * deployment, else <code>null</code>.
     *
     * @return error messages during initialization of OpenAM as single war
     */
    public static String getSecondaryInitializationError() {
        return initSecondaryError;
    }

    /**
     * Sets the server instance name of which properties are retrieved
     * to initialized this object.
     *
     * @param name Server instance name.
     */
    public static void setServerInstanceName(String name) {
        instanceName = name;
    }

    /**
     * Returns the server instance name of which properties are retrieved
     * to initialized this object.
     *
     * @return Server instance name.
     */
    public static String getServerInstanceName() {
        return instanceName;
    }

    /**
     * Returns <code>true</code> if instance is running in server mode.
     *
     * @return <code>true</code> if instance is running in server mode.
     */
    public static boolean isServerMode() {
        return IsServerModeHolder.isServerMode;
    }

    /**
     * Returns the property name to service attribute schema name mapping.
     *
     * @return Property name to service attribute schema name mapping.
     */
    public static Map getAttributeMap() {
        return ATTRIBUTE_MAP;
    }

    private static Map<String, AttributeStruct> initAttributeMapping() {
        final Map<String, AttributeStruct> attributeMapping = new HashMap<>();
        try {
            ResourceBundle rb = ResourceBundle.getBundle("serverAttributeMap");
            for (String propertyName : rb.keySet()) {
                attributeMapping.put(propertyName, new AttributeStruct(rb.getString(propertyName)));
            }
        } catch (MissingResourceException mse) {
            // No Resource Bundle Found, Continue.
            // Could be in Test Mode.
        }
        return Collections.unmodifiableMap(attributeMapping);
    }


    /**
     * Lazy initialisation holder idiom for server mode flag as this is read frequently but never changes.
     */
    private static final class IsServerModeHolder {
        // use getProp and not get method to avoid infinite loop
        private static final boolean isServerMode = Boolean.parseBoolean(getProp(Constants.SERVER_MODE, "false"));
    }

    /**
     * A singleton enum for the configuration listeners, which will be lazily initialized on first use. The code
     * here cannot be added to the {@code SystemProperties} class initialization as it would create a cyclic
     * dependency on the static initialization of {@code ConfigurationObserver}.
     */
    private enum Listeners {
        INSTANCE;

        private final Map<String, ServicePropertiesConfigurationListener> servicePropertiesListeners;
        private final ServicePropertiesConfigurationListener platformServicePropertiesListener;

        Listeners() {
            servicePropertiesListeners = new HashMap<>();
            platformServicePropertiesListener = new ServicePropertiesConfigurationListener();

            Map<String, Set<String>> services = new HashMap<>();
            for (Map.Entry<String, AttributeStruct> property : ATTRIBUTE_MAP.entrySet()) {
                String serviceName = property.getValue().getServiceName();
                if (!services.containsKey(serviceName)) {
                    services.put(serviceName, new HashSet<String>());
                }
                services.get(serviceName).add(property.getKey());
            }

            ConfigurationObserver configurationObserver = ConfigurationObserver.getInstance();

            for (final Map.Entry<String, Set<String>> service : services.entrySet()) {
                if (!Constants.SVC_NAME_PLATFORM.equals(service.getKey())) {
                    Set<String> properties = service.getValue();
                    ServicePropertiesConfigurationListener listener =
                            new ServicePropertiesConfigurationListener(properties);
                    for (String property : properties) {
                        servicePropertiesListeners.put(property, listener);
                    }
                    configurationObserver.addServiceListener(listener, new Predicate<String>() {
                        @Override
                        public boolean apply(@Nullable String s) {
                            return s != null && s.equals(service.getKey());
                        }
                    });
                }
            }

            configurationObserver.addServiceListener(platformServicePropertiesListener, new Predicate<String>() {
                @Override
                public boolean apply(@Nullable String s) {
                    return Constants.SVC_NAME_PLATFORM.equals(s);
                }
            });
        }
    }

    /**
     * A listener for the properties that are provided by a single service. Property values are cached so that
     * property listeners are only notified when the property(-ies) they are observing have changed.
     */
    private static final class ServicePropertiesConfigurationListener implements ConfigurationListener {

        private final Map<String, Set<ConfigurationListener>> propertyListeners = new HashMap<>();
        private final Map<String, String> propertyValues = new HashMap<>();

        private ServicePropertiesConfigurationListener(Set<String> propertyNames) {
            for (String propertyName : propertyNames) {
                registerPropertyName(propertyName);
            }
        }

        private void registerPropertyName(String propertyName) {
            propertyListeners.put(propertyName, Collections.synchronizedSet(new HashSet<ConfigurationListener>()));
        }

        private ServicePropertiesConfigurationListener() {
            // nothing to see here
        }

        @Override
        public void notifyChanges() {
            Set<ConfigurationListener> affectedListeners = new HashSet<>();
            for (Map.Entry<String, Set<ConfigurationListener>> propertyListeners : this.propertyListeners.entrySet()) {
                String propertyName = propertyListeners.getKey();
                String value = get(propertyName);
                String oldValue = propertyValues.get(propertyName);
                if (value != null && !value.equals(oldValue) || value == null && oldValue != null) {
                    Set<ConfigurationListener> listeners = propertyListeners.getValue();
                    for (ConfigurationListener listener : listeners) {
                        affectedListeners.add(listener);
                    }
                    propertyValues.put(propertyName, value);
                }
            }
            for (ConfigurationListener listener : affectedListeners) {
                listener.notifyChanges();
            }
        }
    }

    /**
     * Listen for runtime changes to a system property value. Only values that are stored in the SMS will
     * be changed at runtime. See {@code serverdefaults.properties}, {@code amPlatform.xml} and
     * {@code serverAttributeMap.properties}.
     *
     * @param listener The listener to call when one of the provided properties has changed.
     * @param properties The list of properties that should be observed. A change in any one of these properties
     *                   will cause the listener to be notified.
     */
    public static void observe(ConfigurationListener listener, String... properties) {
        for (String property : properties) {
            ServicePropertiesConfigurationListener serviceListener =
                    Listeners.INSTANCE.servicePropertiesListeners.get(property);
            if (serviceListener == null) {
                serviceListener = Listeners.INSTANCE.platformServicePropertiesListener;
            }
            synchronized (serviceListener) {
                if (!serviceListener.propertyListeners.containsKey(property)) {
                    serviceListener.registerPropertyName(property);
                }
                serviceListener.propertyListeners.get(property).add(listener);
            }
        }
    }

    /**
     * Holds the current properties map together with the tagswap values and last updated timestamp to allow atomic
     * updates of all three as one unit without locking. This is an immutable structure that is intended to be used
     * with an AtomicReference.
     */
    private static final class PropertiesHolder {
        private final Map<String, String> properties;
        private final Map<String, String> tagSwapValues;
        private final long lastModified;

        PropertiesHolder() {
            this.properties = Collections.emptyMap();
            this.tagSwapValues = Collections.emptyMap();
            this.lastModified = currentTimeMillis();
        }

        PropertiesHolder(final Map<String, String> properties) {
            Map<String, String> tagSwapMap = new HashMap<>();
            for (Map.Entry<String, String> tagSwapEntry : TAG_SWAP_PROPERTIES.entrySet()) {
                String tag = tagSwapEntry.getKey();
                String propertyName = tagSwapEntry.getValue();
                String val = System.getProperty(propertyName);
                if (val == null) {
                    val = properties.get(propertyName);
                }
                tagSwapMap.put(tag, val);
            }
            this.properties = Collections.unmodifiableMap(properties);
            this.tagSwapValues = Collections.unmodifiableMap(tagSwapMap);
            this.lastModified = currentTimeMillis();
        }

        String getProperty(String name) {
            return properties.get(name);
        }

        PropertiesHolder putAll(Properties newProperties) {
            return new PropertiesHolder(Maps.fromProperties(newProperties));
        }
    }
}
