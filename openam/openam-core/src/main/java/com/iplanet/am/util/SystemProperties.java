/**
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
 * $Id: SystemProperties.java,v 1.21 2009/10/12 17:55:06 alanchu Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2013 ForgeRock, Inc.
 */
package com.iplanet.am.util;

import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.common.AttributeStruct;
import com.sun.identity.common.PropertiesFinder;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.SMSEntry;
import org.forgerock.openam.cts.api.CoreTokenConstants;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.security.AccessController;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    private static String instanceName;
    private static ReentrantReadWriteLock rwLock = new
        ReentrantReadWriteLock();
    private static Map attributeMap = new HashMap();
    private static boolean sitemonitorDisabled = false;
    private final static String TRUE = "true";
    
    static {
        initAttributeMapping();
    }
    
    private static void initAttributeMapping() {
        try {
        ResourceBundle rb = ResourceBundle.getBundle("serverAttributeMap");
        for (Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
            String propertyName = (String)e.nextElement();
            attributeMap.put(propertyName, new AttributeStruct(
                rb.getString(propertyName)));
        }
        } catch(java.util.MissingResourceException mse) {
            // No Resource Bundle Found, Continue.
            // Could be in Test Mode.
        }
    }
    

    private static Properties props;

    private static long lastModified;

    private static String initError;

    private static String initSecondaryError;

    private static final String SERVER_NAME_PROPERTY = "server.name";

    private static final String CONFIG_NAME_PROPERTY = "amconfig";

    private static final String AMCONFIG_FILE_NAME = "AMConfig";

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

    private static Map mapTagswap = new HashMap();
    private static Map tagswapValues;

    /**
     * Initialization to load the properties file for config information before
     * anything else starts.
     */
    static {
        mapTagswap.put("%SERVER_PORT%",  Constants.AM_SERVER_PORT);
        mapTagswap.put("%SERVER_URI%",   Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        mapTagswap.put("%SERVER_HOST%",  Constants.AM_SERVER_HOST);
        mapTagswap.put("%SERVER_PROTO%", Constants.AM_SERVER_PROTOCOL);
        mapTagswap.put("%BASE_DIR%", CONFIG_PATH);
        mapTagswap.put("%SESSION_ROOT_SUFFIX%",
                CoreTokenConstants.SYS_PROPERTY_SESSION_HA_REPOSITORY_ROOT_SUFFIX);
        mapTagswap.put("%SESSION_STORE_TYPE%",
                CoreTokenConstants.SYS_PROPERTY_SESSION_HA_REPOSITORY_TYPE);

        try {
            // Initialize properties
            props = new Properties();

            // Load properties from file
            String serverName = System.getProperty(SERVER_NAME_PROPERTY);
            String configName = System.getProperty(CONFIG_NAME_PROPERTY,
                    AMCONFIG_FILE_NAME);
            String fname = null;
            FileInputStream fis = null;
            if (serverName != null) {
                serverName = serverName.replace('.', '_');
                fname = configName + "-" + serverName;
            } else {
                fname = configName;
            }
            initializeProperties(fname);

            // Get the location of the new configuration file in case
            // of single war deployment
            try {
                String newConfigFileLoc = props
                        .getProperty(Constants.AM_NEW_CONFIGFILE_PATH);
                if ((newConfigFileLoc != null) &&
                    (newConfigFileLoc.length() > 0) && 
                    !newConfigFileLoc.equals(NEWCONFDIR)
                ) {
                    String hostName = InetAddress.getLocalHost().getHostName()
                            .toLowerCase();
                    String serverURI = props.getProperty(
                            Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
                    serverURI = serverURI.replace('/', '_').toLowerCase();
                    StringBuilder fileName = new StringBuilder();
                    fileName.append(newConfigFileLoc).append("/").append(
                            AMCONFIG_FILE_NAME).append(serverURI).append(
                            hostName).append(
                            props.getProperty(Constants.AM_SERVER_PORT))
                            .append(".").append(PROPERTIES);
                    Properties modProp = new Properties();
                    try {
                        fis = new FileInputStream(fileName.toString());
                        modProp.load(fis);
                        props.putAll(modProp);
                    } catch (IOException ioe) {
                        StringBuilder fileNameOrig = new StringBuilder();
                        fileNameOrig.append(newConfigFileLoc).append("/")
                                .append(AMCONFIG_FILE_NAME).append(".").append(
                                        PROPERTIES);
                        try {
                            fis = new FileInputStream(fileNameOrig.toString());
                            modProp.load(fis);
                            props.putAll(modProp);
                        } catch (IOException ioexp) {
                            saveException(ioexp);
                        }
                    } finally {
                        if (fis != null) {
                            fis.close();
                        }
                    }
                }
            } catch (Exception ex) {
                saveException(ex);
            }
        } catch (MissingResourceException e) {
            // Can't print the message to debug due to dependency
            // Save it as a String and provide when requested.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));
            initError = baos.toString();
            try {
                baos.close();
            } catch (IOException ioe) {
                // Should not happend, ignore the exception
            }
        }
        sitemonitorDisabled = Boolean.valueOf(getProp(
               Constants.SITEMONITOR_DISABLED, "false")).booleanValue();
    }

    /**
     * Helper function to handle associated exceptions during initialization of
     * properties using external properties file in a single war deployment.
     */
    static void saveException(Exception ex) {
        // Save it as a String and provide when requested.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ex.printStackTrace(new PrintStream(baos));
        initSecondaryError = baos.toString();
        try {
            baos.close();
        } catch (IOException ioe) {
            // Should not happend, ignore the exception
        }
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
        rwLock.readLock().lock();

        try {
            String answer = null;

            // look up values in SMS services only if in server mode.
            if (isServerMode() || sitemonitorDisabled) {
                AttributeStruct ast = (AttributeStruct) attributeMap.get(key);
                if (ast != null) {
                    answer = PropertiesFinder.getProperty(key, ast);
                }
            }

            if (answer == null) {
                answer = getProp(key);

                if ((answer != null) && (tagswapValues != null)) {
                    Set set = new HashSet();
                    set.addAll(tagswapValues.keySet());

                    for (Iterator i = set.iterator(); i.hasNext();) {
                        String k = (String) i.next();
                        String val = (String) tagswapValues.get(k);

                        if (k.equals("%SERVER_URI%")) {
                            if ((val != null) && (val.length() > 0)) {
                                if (val.charAt(0) == '/') {
                                    answer = answer.replaceAll("/%SERVER_URI%",
                                        val);
                                    String lessSlash = val.substring(1);
                                    answer = answer.replaceAll("%SERVER_URI%",
                                        lessSlash);
                                } else {
                                    answer = answer.replaceAll(k, val);
                                }
                            }
                        } else {
                            answer = answer.replaceAll(k, val);
                        }
                    }

                    if (answer.indexOf("%ROOT_SUFFIX%") != -1) {
                        answer = answer.replaceAll("%ROOT_SUFFIX%",
                            SMSEntry.getAMSdkBaseDN());
                    }
                }
            }


            return (answer);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private static String getProp(String key, String def) {
        String value = getProp(key);
        return ((value == null) ? def : value);
    }

    private static String getProp(String key) {
        String answer = System.getProperty(key);
        if (answer == null) {
            answer = props.getProperty(key);
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
        return ((value == null) ? def : value);
    }

    /**
     * Returns the property value as a boolean
     *
     * @param key the key whose value one is looking for.
     * @return the boolean value if the key exists; otherwise returns false
     */
    public static boolean getAsBoolean(String key) {
        String value = get(key);

        if (value == null)
            return false;

        return (value.equalsIgnoreCase(TRUE) ? true : false);
    }

    /**
     * Returns the property value as a boolean
     *
     * @param key
     * @param defaultValue value if key is not found.
     * @return the boolean value if the key exists; otherwise the default value
     */
    public static boolean getAsBoolean(String key, boolean defaultValue) {
        String value = get(key);

        if (value == null)
            { return defaultValue; }

        return (value.equalsIgnoreCase(TRUE) ? true : false);
    }
    
    /**
     * Returns all the properties defined and their values.
     * 
     * @return Properties object with all the key value pairs.
     */
    public static Properties getProperties() {
        rwLock.readLock().lock();
        try {
            Properties properties = new Properties();
            properties.putAll(props);
            return properties;
        } finally {
            rwLock.readLock().unlock();
        }
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
        rwLock.readLock().lock();

        try {
            Properties properties = new Properties();
            properties.putAll(props);
            // Iterate over the System Properties & add them in result obj
            Iterator it = System.getProperties().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String key = (String) entry.getKey();
                String val = (String) entry.getValue();
                if ((key != null) && (key.length() > 0)) {
                    properties.setProperty(key, val);
                }
            }
            return properties;
        } finally {
            rwLock.readLock().unlock();
        }
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

    private static void updateTagswapMap(Properties properties) {
        tagswapValues = new HashMap();
        for (Iterator i = mapTagswap.keySet().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            String rgKey = (String)mapTagswap.get(key);
            String val = System.getProperty(rgKey);
            if (val == null) {
                val = (String)properties.get(rgKey);
            }
            tagswapValues.put(key, val);
        }
    }

    /**
     * Initializes properties bundle from the <code>file<code> 
     * passed.
     *
     * @param file type <code>String</code>, file name for the resource bundle
     * @exception MissingResourceException
     */
    public static void initializeProperties(String file)
        throws MissingResourceException {
        rwLock.writeLock().lock();
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(file);
            // Copy the properties to props
            Enumeration e = bundle.getKeys();
            Properties newProps = new Properties();
            newProps.putAll(props);
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                newProps.put(key, bundle.getString(key));
            }
            // Reset the last modified time
            props = newProps;
            updateTagswapMap(props);
            lastModified = System.currentTimeMillis();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public static void initializeProperties(Properties properties){
        initializeProperties(properties, false);
    }
    
    /**
     * Initializes the properties to be used by OpenSSO. Ideally this
     * must be called first before any other method is called within OpenSSO
     * Enterprise. This method provides a programmatic way to set the
     * properties, and will override similar properties if loaded for a
     * properties file.
     * 
     * @param properties properties for OpenSSO
     * @param reset <code>true</code> to reset existing properties.
     */
    public static void initializeProperties(
        Properties properties,
        boolean reset) 
    {
        initializeProperties(properties, reset, false);
    }
    
    /**
     * Initializes the properties to be used by OpenSSO. Ideally this
     * must be called first before any other method is called within OpenSSO
     * Enterprise. This method provides a programmatic way to set the
     * properties, and will override similar properties if loaded for a
     * properties file.
     * 
     * @param properties properties for OpenSSO.
     * @param reset <code>true</code> to reset existing properties.
     * @param withDefaults <code>true</code> to include default properties.
     */
    public static void initializeProperties(
        Properties properties,
        boolean reset,
        boolean withDefaults) {
        Properties defaultProp = null;
        if (withDefaults) {
            SSOToken appToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            defaultProp = ServerConfiguration.getDefaults(appToken);
        }

        rwLock.writeLock().lock();

        try {
            Properties newProps = new Properties();
            if (defaultProp != null) {
                newProps.putAll(defaultProp);
            }


            if (!reset) {
                newProps.putAll(props);
            }

            newProps.putAll(properties);
            props = newProps;
            updateTagswapMap(props);
            lastModified = System.currentTimeMillis();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Initializes the property to be used by OpenSSO. Ideally this
     * must be called first before any other method is called within OpenSSO
     * Enterprise.
     * This method provides a programmatic way to set a specific property, and
     * will override similar property if loaded for a properties file.
     * 
     * @param propertyName property name.
     * @param propertyValue property value.
     */
    public static void initializeProperties(
        String propertyName,
        String propertyValue
    ) {
        rwLock.writeLock().lock();

        try {
            Properties newProps = new Properties();
            newProps.putAll(props);
            newProps.put(propertyName, propertyValue);
            props = newProps;
            updateTagswapMap(props);
            lastModified = System.currentTimeMillis();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Returns a counter for last modification. The counter is incremented if
     * the properties are changed by calling the following method
     * <code>initializeProperties</code>. This is a convenience methods for
     * applications to track changes to OpenSSO properties.
     * 
     * @return counter of the last modification
     */
    public static long lastModified() {
        return (lastModified);
    }

    /**
     * Returns error messages during initialization, else <code>null</code>.
     * 
     * @return error messages during initialization
     */
    public static String getInitializationError() {
        return (initError);
    }

    /**
     * Returns error messages during initialization using the single war
     * deployment, else <code>null</code>.
     * 
     * @return error messages during initialization of AM as single war
     */
    public static String getSecondaryInitializationError() {
        return (initSecondaryError);
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
        // use getProp and not get method to avoid infinite loop
        return Boolean.valueOf(getProp(
            Constants.SERVER_MODE, "false")).booleanValue();
    }
    
    /**
     * Returns the property name to service attribute schema name mapping.
     *
     * @return Property name to service attribute schema name mapping.
     */
    public static Map getAttributeMap() {
        rwLock.readLock().lock();
        try {
            return attributeMap;
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
