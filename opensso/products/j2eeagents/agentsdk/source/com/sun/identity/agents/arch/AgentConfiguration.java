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
 * $Id: AgentConfiguration.java,v 1.39 2009/04/02 00:02:11 leiming Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2013 ForgeRock Inc.
 */

package com.sun.identity.agents.arch;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.comm.client.AlreadyRegisteredException;
import com.iplanet.services.comm.client.PLLClient;
import com.iplanet.services.naming.URLNotFoundException;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.IApplicationSSOTokenProvider;
import com.sun.identity.agents.util.AgentRemoteConfigUtils;
import com.sun.identity.agents.util.ResourceReader;

import com.sun.identity.common.DebugPropertiesObserver;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.install.tools.util.FileUtils;

import org.forgerock.openam.util.Version;

/**
 * <p>
 * Provides access to the configuration as set in the system.
 * </p><p>
 * It uses the agent bootstrap configuration file called
 * OpenSSOAgentBootstrap.properties to get the agent startup configuration that 
 * includes the OpenSSO (OpenSSO) server information and the
 * agent user credential. It uses these information to authenticate to 
 * the OpenSSO server. The OpenSSO Authentication service completes the agent 
 * authentication and sends a SSO token back to the agent. Using the SSO
 * token, the agent calls the OpenSSO Attribute service to fetch its 
 * configuration. The OpenSSO Attribute service sends the agent configuration
 * back to the agent. The agent configuration returned contains the agent
 * configuration repository location. If the location is "centralized",
 * then agent will use the agent configuration just returned. If the 
 * location is "local" or not present, then the agent knows that its 
 * configuration is at the local. It reads the rest of its configuration
 * from the local configuration file OpenSSOAgentConfiguration.properties.
 * </p><p>
 * Most of the agent configuration properties are hot swappable. The
 * changes to these properties are effective without having to restart
 * the agent container. The property changes are updated at the agent 
 * in two ways. The agent configuration change notification and agent 
 * configuration change polling.
 * </p><p>
 * The agent configuration change notification is available in the case
 * of the agent configuration is centralized. Anytime one or more agent 
 * properties are changed in the OpenSSO, a notification is sent to the agent.
 * Upon receiving the notification, the affected agent will refetch its
 * from the OpenSSO server and updates its configuration using the new values. 
 * </p><p>
 * Configuration polling updates are available if the configuration setting 
 * <code>com.sun.identity.agents.j2ee.config.load.interval</code> has been set
 * to a non-zero positive value indicating the number of seconds after which
 * the system will poll to identify configuration changes. If this value is set
 * to zero, the configuration reloads will be disabled. Every time a 
 * configuration reload occurs, the value of the configuration setting that 
 * governs the reload interval is also recalculated, thereby making it possible
 * to dynamically disable reloads by setting the value to zero when reloads are
 * active. Note that in the event of a reload, only the configuration keys that
 * are designated for Agent operation are reloaded. These keys begin with the
 * <code>com.sun.identity.agents.config</code> prefix. The keys that do
 * not match this criteria are kept unchanged and reflect the values that were
 * present during system initialization. Certain keys which meet this crieteria
 * would still not be reloaded due to potential security concerns associated
 * with the swapping of the associated values.
 * </p><p>
 * General access methods in this class are package protected and thus cannot 
 * be directly invoked by classes that are not within the same package. It is
 * expected and required that all configuration access be done via a designated 
 * <code>Manager</code> of the subsystem which in turn acts as an intermediate
 * caching point for configuration values. Public methods are available for
 * configuration settings that are considered core settings.
 * </p>
 * 
 * @see com.sun.identity.agents.arch.Manager
 * @see com.sun.identity.agents.arch.IConfigurationListener
 */
public class AgentConfiguration implements 
        IAgentConfigurationConstants, 
        IConfigurationKeyConstants, 
        IClientConfigurationKeyConstants,
        IConfigurationDefaultValueConstants
{
    
   /**
    * A constant value used to identify the default web application context
    * for configuration settings that are application specific.
    */
    public static final String DEFAULT_WEB_APPLICATION_NAME = "DefaultWebApp";
    private static final String ATTRIBUTE_SERVICE_NAME = "idsvcs-rest";
    private static final String AGENT_CONFIG_CENTRALIZED = "centralized";
    private static final String AGENT_CONFIG_LOCAL = "local";
    
   /**
    * Returns a header name that contains the client IP address. If no header
    * name is specified in the configuration file, this method will return
    * <code>null</code>.
    * 
    * @return a header name that contains client IP address or <code>null</code>
    * if no header name is specified.
    */
    public static String getClientIPAddressHeader() {
        return _clientIPAddressHeader;
    }
    
   /**
    * Returns a header name that contains the client hostname. If no header
    * name is specified in the configuration file, this method will return
    * <code>null</code>.
    * 
    * @return a header name that contains client hostname or <code>null</code>
    * if no header name is specified.
    */
    public static String getClientHostNameHeader() {
        return _clientHostNameHeader;
    }
    
   /**
    * Returns the name of the organization that can be used for authenticating
    * the Agent services. This value represents the organization name or the
    * realm name to which the Agent profile belongs.
    * 
    * @return the organization or realm name to which the Agent profile belongs.
    */
    public static String getOrganizationName() {
        return _organizationName;
    }    

    public static String getServerHost() {
	 return getProperty("com.iplanet.am.server.host");
    }

    public static String getServerPort() {
	 return getProperty("com.iplanet.am.server.port");
    }
    public static String getServerProtocol() {
	 return getProperty("com.iplanet.am.server.protocol");
    }


    public static String getPolicyAdmLocation() {
	 return _policyAdminLoc;
    }

    
   /**
    * Returns a boolean indicating if the notifications for Policy changes
    * have been set as enabled.
    * @return <code>true</code> if notifications for Policy are enabled, 
    * <code>false</code> otherwise.
    */
    public static boolean isPolicyNotificationEnabled() {
        return _policyNotificationEnabledFlag;
    }
    
   /**
    * Returns the URL that will be used by the Server to send all 
    * notifications to agents. This will include policy, session, and agent 
    * configuration change notifications.
    * @return the client notification URL.
    */
    public static String getClientNotificationURL() {
        return _clientNotificationURL;
    }
    
   /**
    * Returns a boolean indicating if the notificatiosn for Sessino changes have
    * been set as enabled.
    * 
    * @return <code>true</code> if notifications for Session are enabled, 
    * <code>false</code> otherwise.
    */
    public static boolean isSessionNotificationEnabled() {
        return _sessionNotificationEnabledFlag;
    }
    
   /**
    * Returns the name of the OpenSSO Session property that is used by
    * the Agent runtime to identify the user-id of the current user.
    * 
    * @return the Session property name that identifies the user-id of the 
    * current user.
    */
    public static String getUserIdPropertyName() {
        return _userIdPropertyName;
    }
    
   /**
    * A method that ensures that any class that directly depends
    * upon Client SDK can first initialize the <code>AgentConfiguration</code>.
    * Failing to initialize the <code>AgentConfiguration</code> can result
    * in the malfunction of the Client SDK due to configuration dependancies.
    */
    public static void initialize() {
        // No processing requried
    }
    
   /**
    * Returns the name of the cookie or URI parameter that holds the users
    * SSO token.
    * 
    * @return the SSO token cookie or parameter name.
    */
    public static synchronized String getSSOTokenName() {
        return _ssoTokenCookieName;
    }
     
   /**
    * Returns the <code>ServiceResolver</code> instance associated with the
    * Agent runtime.
    * @return the configured <code>ServiceResolver</code>.
    */
    public static ServiceResolver getServiceResolver() {
        return _serviceResolver;
    }
    
   /**
    * Returns the <code>UserMapingMode</code> configured in the system. This
    * setting is not hot-swappable and is initialized during system 
    * initialization and never changed thereafter.
    * 
    * @return the configured <code>UserMappingMode</code>.
    */
    public static UserMappingMode getUserMappingMode() {
        return _userMappingMode;
    }
    
   /**
    * Returns the <code>AuditLogMode</code> configured in the system. This
    * setting is not hot-swappable and is initialized during system
    * initialization and never changed thereafeter.
    * 
    * @return the configured <code>AuditLogMode</code>.
    */
    public static AuditLogMode getAuditLogMode() {
        return _auditLogMode;
    }
     
   /**
    * Returns the user attribute value configured in the system. This setting
    * is not hot-swappable and is initialized during system initialization and
    * never changed thereafter.
    * 
    * @return the configured user attribute value.
    */
    public static String getUserAttributeName() {
        return _userAttributeName;
    }  
    
   /**
    * Returns <code>true</code> if the runtime is configured to use the user's
    * <code>DN</code> instead of the regular <code>userid</code> for 
    * identification purposes. This setting is not hot-swappable and is 
    * initialized during system initialization and never changed thereafter.
    * 
    * @return <code>true</code> if the system is configured to use the user's
    * <code>DN</code> for identification purposes, <code>false</code> otherwise.
    */
    public static boolean isUserPrincipalEnabled() {
        return _userPrincipalEnabled;
    }    
    
   /**
    * Allows other parts of the subsystem to register for configuration
    * change events by registering the specified 
    * <code>IConfigurationListener</code>.
    * 
    * @param listener the <code>IConfigurationListener</code> to be registered.
    */
    public static void addConfigurationListener(
            IConfigurationListener listener) {

        if(isLogMessageEnabled()) {
            logMessage("AgentConfiguration: Adding listener for : "
                       + listener.getName());
        }

        Vector configurationListeners = getModuleConfigurationListeners();

        synchronized(configurationListeners) {
            configurationListeners.add(listener);
        }
    }
    
    /**
     * Returns the application user name to be used to identify the Agent
     * runtime.
     * 
     * @return the application user name.
     */
     public static String getApplicationUser() {
         return _applicationUser;
     }
     
    /**
     * Returns the application password to be used to identify the Agent
     * runtime.
     * 
     * @return the application password.
     */
     public static String getApplicationPassword() {
         return _applicationPassword;
     }    
        
   /**
    * Would be called by agent configuration notification handler when the 
    * agent housekeeping app receives configuration update notifications from
    * the OpenSSO server
    */
    public static void updatePropertiesUponNotification() {       
        if(isAgentConfigurationRemote()) {
            hotSwapAgentConfiguration(true);
            if (isLogMessageEnabled()) {
                logMessage(
                    "AgentConfiguration.updatePropertiesUponNotification():" +
                    " updating configuration from a notification while" +
                    " in centralized mode.");
            }      
        } else {
            if (isLogMessageEnabled()) {
                logMessage(
                    "AgentConfiguration.updatePropertiesUponNotification():" +
                    " caller trying to update configuration from a" +
                    " notification while in local mode." +
                    " Should only be called when in centralized mode.");
            }     
        }
    }
    
   /**
    * Returns the configuration value corresponding to the specified 
    * <code>id</code> or the supplied <code>defaultValue</code> if not 
    * present.
    * 
    * @param id the configuration key to be looked up.
    * @param defaultValue the default value to be used in case no configuration
    * is specified for the given <code>id</code>.
    * 
    * @return the associated configuration value with the specified 
    * <code>id</code> or the <code>defaultValue</code> if no value is specified
    * in the configuration.
    */
    static String getProperty(String id, String defaultValue) {
        String value = getProperty(id);
        if (value == null) {
            value = defaultValue;
        }
        
        return value;
    }
    
   /**
    * Returns the configuration value corresponding to the specified 
    * <code>id</code> or <code>null</code> if no value is present.
    * 
    * @param id the configuration key to be looked up.
    * 
    * @return the associated configuration value with the specified
    * <code>id</code> or <code>null</code> if no value is specified in the
    * configuration.
    */
    static String getProperty(String id) {
        String result = null;
        String value = SystemProperties.get(id);
        if (value == null) {
            value = getPropertyInternal(id);
        }
        
        if (value != null && value.trim().length() > 0) {
            result = value.trim();
        }
        
        return result;
    }
    
   /**
    * Returns a <code>Properties</code> instance that holds all the available
    * configuration as available in the system.
    * 
    * @return the configuration as a <code>Properties</code> instance.
    */
    static Properties getAll() {
        Properties result = getAllInternal();
        Iterator it = SystemProperties.getProperties().keySet().iterator();
        while (it.hasNext()) {
            String nextKey = (String) it.next();
            result.put(nextKey, SystemProperties.get(nextKey));
        }
        return result;
    }
        
    private synchronized static String getPropertyInternal(String id) {
        return getProperties().getProperty(id);
    }
    
    private synchronized static Properties getAllInternal() {
        Properties properties = new Properties();
        properties.putAll(getProperties());
        return properties;
    }    

    /*
     * This method will check for the JVM option:
     *
     * <code>openam.agents.bootstrap.dir</code>
     *
     * before falling back to trying to load the file from the CLASSPATH
     */
    private static synchronized void setConfigurationFilePath() {
        if (!isInitialized()) {
            String bootstrapDir = System.getProperty(CONFIG_JVM_OPTION_NAME);

            // try to load the bootstrap from the JVM option
            if (bootstrapDir != null) {
                String configFile = bootstrapDir + System.getProperty("file.separator") +
                        CONFIG_FILE_NAME;
                String localConfigFile = bootstrapDir + System.getProperty("file.separator") +
                        LOCAL_CONFIG_FILE_NAME;

                if (isFileValid(configFile) && isFileValid(localConfigFile)) {
                    setConfigFilePath(configFile);
                    setLocalConfigFilePath(localConfigFile);

                    return;
                }
            }

            // fallback to loading the bootstrap file from the classpath
            String result = null;
            URL resUrl = ClassLoader.getSystemResource(CONFIG_FILE_NAME);
            if(resUrl == null) {
                ClassLoader cl =
                    Thread.currentThread().getContextClassLoader();
                if(cl != null) {
                    resUrl = cl.getResource(CONFIG_FILE_NAME);
                }
            }

            if (resUrl == null) {
                throw new RuntimeException(
                    "Failed to get configuration file:" + CONFIG_FILE_NAME);
            }
            result = resUrl.getPath();
            try {
                if (System.getProperty("file.separator").equals("\\") &&
                        result.startsWith("/")) {
                    result = resUrl.toURI().getPath().substring(1);
                }
            } catch (Exception ex) {
               throw new RuntimeException(
                    "Failed to get absolute file path:" + CONFIG_FILE_NAME);
            }
            if (result == null) {
                throw new RuntimeException(
                    "Failed to get configuration file:" + CONFIG_FILE_NAME);
            }
            setConfigFilePath(result);
            int index = result.lastIndexOf(CONFIG_FILE_NAME);
            if (index < 0) {
                throw new RuntimeException(
                "Failed to find the agent bootstrap file:" + CONFIG_FILE_NAME);
            }
            String pathDir = result.substring(0, index);
            setLocalConfigFilePath(pathDir + LOCAL_CONFIG_FILE_NAME);
        }
    }

    /*
     * Is file valid @param filename
     *
     * @return boolean
     */
    private static boolean isFileValid(String filename) {

        boolean result = false;
        if ((filename != null) && (filename.length() > 0)) {
            File file = new File(filename);
            if (file.exists() && file.isFile() && file.canRead()) {
                result = true;
            }
        }
        return result;
    }
    
    /**
     * Load from OpenSSOAgentBootstrap.properties for start up properties.
     * This method should only be called once at start up time
     * since bootstrap properties are not hot swappable by editing the
     * properties file without a restart.
     * If it is called more than once(already initialized, then it will just
     * return the bootstrap properties that were read and saved at start 
     * up time.
     **/
    private static Properties getPropertiesFromConfigFile() 
    throws Exception {
        Properties result = new Properties();
        if (!isInitialized()) {
            BufferedInputStream instream = null;
            try {
                instream = new BufferedInputStream(
                    new FileInputStream(getConfigFilePath()));
                result.load(instream);
                setBootstrapProperties(result);
            } catch (Exception ex) {
                throw ex;
            } finally {
                if (instream != null) {
                    try {
                        instream.close();
                    } catch (Exception ex) {
                        // No handling required
                    }
                }
            }
           
        } else { //already initialized
            //this is to enforce that coders do not accidently try to re-read 
            //the agents bootstrap configuration file. If already initialized 
            //then will return the original set of bootstrap properties
            result = getBootstrapProperties();
        }
        return result;
    }
    
    private static Properties getPropertiesFromRemote(Vector urls) 
        throws AgentException {
        Properties result = new Properties();
        String tokenId = getAppSSOToken().getTokenID().toString();
        result = AgentRemoteConfigUtils.getAgentProperties(
               urls, tokenId, getProfileName(), getOrganizationName());
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration: Centralized agent properties =" 
                    + result);
        }
        return result; 
    }
           
    private static Properties getPropertiesFromLocal() 
        throws AgentException {
        Properties result = new Properties();
        BufferedInputStream instream = null;
        try {
            instream = new BufferedInputStream(
                    new FileInputStream(getLocalConfigFilePath()));
            result.load(instream);
        } catch (Exception ex) {
            throw new AgentException(ex);
        } finally {
                if (instream != null) {
                    try {
                        instream.close();
                    } catch (Exception ex) {
                        // No handling required
                    }
                }
        }
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration: Local config properties =" + result);
        }
        return result;
    }
    
    private static void setAppSSOToken() throws AgentException {
        CommonFactory cf = new CommonFactory(BaseModule.getModule());
        IApplicationSSOTokenProvider provider =  
            cf.newApplicationSSOTokenProvider();
        
        _appSSOToken = provider.getApplicationSSOToken(true);
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration: appSSOToken =" + _appSSOToken);
        }
    }
    
    public static synchronized SSOToken getAppSSOToken() throws AgentException {
        if (_appSSOToken != null) {
            try {
               // check if token is still valid with the session server.
               // This refreshSession call throws a SSOException if the token
               // is not valid any more.
               SSOTokenManager.getInstance().refreshSession(_appSSOToken);
            } catch (SSOException se) {
                if (isLogMessageEnabled()) {
                    logMessage("AgentConfiguration.getAppSSOToken: " +
                       "The app SSO token is invalid, indicating openam " +
                       "server may have restarted, so need to " + 
                       "reauthenticate to get a new app SSO token");
                }
                setAppSSOToken();
            }
        } else {
            setAppSSOToken();
        }
        return _appSSOToken;
    }
    
    private static Vector getAttributeServiceURLs() throws AgentException {
        if (_attributeServiceURLs == null) {
            try {
                _attributeServiceURLs = WebtopNaming.getServiceAllURLs(
                                          ATTRIBUTE_SERVICE_NAME);
                if (isLogMessageEnabled()) {
                    logMessage("AgentConfiguration: attribute service urls"
                                + _attributeServiceURLs);
                }    
            } catch (URLNotFoundException ue) {
                throw new AgentException(ue);
            }
        }
        return _attributeServiceURLs;
    }
    /**
     * Collect all configuration info. Store all config properties, including 
     * OpenSSOAgentBootstrap.properties bootstrap small set of props and also 
     * agent config props (from OpenSSO server or if local config file 
     * OpenSSOAgentConfiguration.properties) and store ALL the properties in a 
     * class field for later use, plus set a few fields on this class for some
     * props that are used throughout agent code and accessed from this class.
     * Also, for any clientsdk properties, push them into the JVM system 
     * properties so they can be accessed by clientsdk.
     * All non-system properties start with AGENT_CONFIG_PREFIX and this is how
     * we distinguish between agent properties and clientsdk properties.
     * Note, a few clientsdk props (like notification url and notification
     * enable flags) are ALSO stored by this class in fields since
     * they are also use throughout agent code as well as by clientsdk.
     */   
    private static synchronized void bootStrapClientConfiguration() {
        if (!isInitialized()) {
            HashMap sysPropertyMap = null;
            setConfigurationFilePath();
            try {
                sysPropertyMap = new HashMap();
                Properties properties = getProperties();
                properties.clear();
                properties.putAll(getPropertiesFromConfigFile());
               
                //debug level can optionally be set in OpenSSOAgentBootstrap.properties
                //but by default is not set, so we provide default if no value
                //This debug level(either default or prop in OpenSSOAgentBootstrap.properties)
                //file is only used for bootup time logging.
                //Real runtime debug level value is later retrieved with rest of 
                //agent config from OpenSSO server
                String initialDebugLevel = properties.getProperty(
                    Constants.SERVICES_DEBUG_LEVEL);
                if ((initialDebugLevel == null) || 
                    (initialDebugLevel.trim().length() == 0)) {
                    properties.setProperty(
                        Constants.SERVICES_DEBUG_LEVEL, Debug.STR_MESSAGE);
                }

                //push the bootstrap properties to JVM system properties
                Iterator iter = properties.keySet().iterator();
                while (iter.hasNext()) {
                    String nextKey = (String) iter.next(); 
                    if (!nextKey.startsWith(AGENT_CONFIG_PREFIX)) {
                        String nextValue = 
                                getProperties().getProperty(nextKey);
                        SystemProperties.initializeProperties(nextKey, nextValue);
                        //save in sysPropertyMap for upcoming log messages
                        sysPropertyMap.put(nextKey, nextValue);
                    }
                }
                //app sso token provider plugin property and value are not 
                //exposed to users so not in bootstrap property file
                SystemProperties.initializeProperties(
                      ClientSDKAppSSOProvider.CLIENT_SDK_ADMIN_TOKEN_PROPERTY, 
                        ClientSDKAppSSOProvider.APP_SSO_PROVIDER_PLUGIN);
                
                setDebug(Debug.getInstance(IBaseModuleConstants.BASE_RESOURCE));
                setOrganizationName();
                setServiceResolver();
                setApplicationUser();
                setApplicationPassword();               
		setPolicyAdminLoc();
                setAppSSOToken();
                setLockConfig();
                setProfileName();
                
                // instantiate the instance of DebugPropertiesObserver
                debugObserver = DebugPropertiesObserver.getInstance();

                Vector attrServiceURLs = getAttributeServiceURLs();
                //if OpenSSO server 8.0
                if (attrServiceURLs != null) {
                    Properties propsFromOpenSSOserver = 
                            getPropertiesFromRemote(attrServiceURLs);
                    String agentConfigLocation = 
                     propsFromOpenSSOserver.getProperty(CONFIG_REPOSITORY_LOCATION);
                    
                    //if agent profile on OpenSSO server is 2.2 style(null or 
                    //blank value)-maybe to help agent upgrade use case)
                    //OR agent profile is 3.0 style(common case) with local
                    //config flag set
                    if ((agentConfigLocation == null)
                         || (agentConfigLocation.trim().equals(""))
                         || (agentConfigLocation.equalsIgnoreCase(
                            AGENT_CONFIG_LOCAL))) {  
                            properties.putAll(getPropertiesFromLocal());
                    } else if (agentConfigLocation.equalsIgnoreCase(
                            AGENT_CONFIG_CENTRALIZED)) {
                        markAgentConfigurationRemote();
                        properties.putAll(propsFromOpenSSOserver);
                    } else {
                        throw new AgentException("Invalid agent config"
                             + "location: does not specify local or centralized");
                    }
                } else {    //else if Access Manager 7.1/7.0 server
                        // Need to read the rest of agent config from its local
                        // configuration file
                        properties.putAll(getPropertiesFromLocal());
                }

                Iterator it = properties.keySet().iterator();
                while (it.hasNext()) {
                    String nextKey = (String) it.next();
                    if (!nextKey.startsWith(AGENT_CONFIG_PREFIX)) {
                        String nextValue = 
                               getProperties().getProperty(nextKey);
                        SystemProperties.initializeProperties(nextKey, nextValue);
                        //save in sysPropertyMap for upcoming log messages
                        sysPropertyMap.put(nextKey, nextValue);
                    }
                }
                // notify possible debug level change
                if (debugObserver != null) {
                    debugObserver.notifyChanges();
                }
                //used by agentsdk and clientsdk, not hot swappable
                setClientNotificationURL();   
                
                String modIntervalString = getProperty(CONFIG_LOAD_INTERVAL);
                try {
                    long modInterval = Long.parseLong(modIntervalString);
                    setModInterval(modInterval * 1000L);
                } catch (NumberFormatException nfex) {
                    System.err.println(
                          "AgentConfiguration: Exception while reading "
                           + "new mod interval: \"" + modIntervalString + "\"");
                }
                markCurrent();
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                throw new RuntimeException("Failed to load configuration: "
                        + ex.getMessage());
            }
        
            if (isLogMessageEnabled()) {
                if (sysPropertyMap != null) {
                    logMessage(
                        "AgentConfiguration: The following properties "
                        + "were added to system: " + sysPropertyMap);
                } else {
                    logMessage(
                        "AgentConfiguration: No properties were added "
                        + " to system.");
                }
                logMessage("AgentConfiguration: Mod Interval is set to: "
                        + getModInterval() + " ms.");
            }
        
            //Start the Configuration Monitor if necessary
            if (!getLockConfig()) {
                GeneralTaskRunnable monitorRunnable = new
                    ConfigurationMonitor();
                SystemTimer.getTimer().schedule(monitorRunnable, new Date((
                    System.currentTimeMillis() / 1000) * 1000));
            }
        }
    }
   
   /**
    * Registers the agent config notification handler with PLLClient. The 
    * handler is registered once and exists for continuous hot swaps if an 
    * agent is configured to enable agent configuration updates from the OpenSSO 
    * server. The handler is used by notification filter task handler when 
    * the filter receives agent configuration XML notifications. This method
    * only needs to be called once when the agent boots up and initializes.
    */
    private static void registerAgentNotificationHandler () {       
        AgentConfigNotificationHandler handler =
                new AgentConfigNotificationHandler();
        try {
            PLLClient.addNotificationHandler(
                    AgentConfigNotificationHandler.AGENT_CONFIG_SERVICE, 
                    handler);   
            if (isLogMessageEnabled()) {
                logMessage(
                    "AgentConfiguration.registerAgentNotificationHandler():" +
                    " registered handler for accepting agent configuration" +
                    " notifications while in centralized mode.");
                } 
        } catch (AlreadyRegisteredException arex) {
            //should only be one handler per VM since static & global
            //so probably will never happen
            if(isLogWarningEnabled()){
              logWarning("AgentConfiguration.registerAgentNotificationHandler" +
                    " Tried to register the AgentConfigNotificationHandler" +
                    " with PLL Client but PLL client already has it" +
                    " registered." , arex );
            }         
        }
    }
       
    private static synchronized void setServiceResolver() {
        if (!isInitialized()) {
            String serviceResolverClassName =
                    getProperty(CONFIG_SERVICE_RESOLVER);        
                try {
                    if (isLogMessageEnabled()) {
                        logMessage(
                            "AgentConfiguration: service resolver set to: "
                            + serviceResolverClassName);
                    }
                    _serviceResolver = (ServiceResolver) Class.forName(
                    serviceResolverClassName).newInstance();
                    
                    if (isLogMessageEnabled()) {
                        logMessage(
                               "AgentConfiguration: service resolver reports "
                               + "EJBContext available: " 
                               + _serviceResolver.isEJBContextAvailable());
                    }
                } catch (Exception ex) {
                    logError(
                        "AgentConfiguration: Failed to set Service Resolver: "
                        + serviceResolverClassName, ex);
                    throw new RuntimeException(
                        "Failed to set Service Resolver: "
                        + serviceResolverClassName + ": "
                        + ex.getMessage());
                }
        }
    }
    
    private static synchronized void setOrganizationName() {
        if (!isInitialized()) {
            _organizationName = getProperty(CONFIG_ORG_NAME);   
            if (_organizationName == null || 
                                  _organizationName.trim().length() == 0) {
                _organizationName =  DEFAULT_ORG_NAME;
                if (isLogMessageEnabled()) {
                      logMessage("AgentConfiguration.setOrganizationName:"
                              + " organization name for realm is not set in the"
                              + " agent bootstrap file, so using the default"
                              + " root realm = "
                              + _organizationName);
                }
            } else {        
                if (isLogMessageEnabled()) {
                    logMessage("AgentConfiguration.setOrganizationName:"
                            + " organization name for realm is set to: "
                            + _organizationName); 
                }
            }
        }
    }
    
    private static synchronized void setUserMappingMode() {
        UserMappingMode mode = UserMappingMode.get(
        getProperty(CONFIG_USER_MAPPING_MODE));
        
        if (mode == null) {
            throw new RuntimeException("Unknown User Mapping Mode: "
                    + getProperty(CONFIG_USER_MAPPING_MODE));
        }
        _userMappingMode = mode;
        
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration.setUserMappingMode: User Mapping"
                    + " mode set to: "
                    + _userMappingMode);
        }
    }
    
    private static synchronized void setAuditLogMode() {
        AuditLogMode mode = AuditLogMode.get(
                getProperty(CONFIG_AUDIT_LOG_MODE));
        
        if (mode == null) {
            throw new RuntimeException("Unknown Audit Log Mode: "
                    + getProperty(CONFIG_AUDIT_LOG_MODE));
        }
        _auditLogMode = mode;
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration.setAuditLogMode: Audit Log mode"
                    + " set to: "
                    + _auditLogMode);
        }
    }
    
    private static synchronized void setUserAttributeName() {
        String userAttributeName = getProperty(CONFIG_USER_ATTRIBUTE_NAME);
        if (userAttributeName == null ||
                userAttributeName.trim().length() == 0) {
            userAttributeName = DEFAULT_USER_ATTRIBUTE_NAME;
            logError("AgentConfiguation.setUserAttributeName: Unable to load"
                    + " user attribute name. Using default value: " 
                    + userAttributeName);
        }
        _userAttributeName = userAttributeName;
        
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration.setUserAttributeName: User"
                    + " attribute name set to: "
                    + _userAttributeName);
        }
    }
    
    private static synchronized void setUserPrincipalEnabledFlag() {
        String userPrinsipalFlagString = getProperty(CONFIG_USER_PRINCIPAL);
        if (userPrinsipalFlagString == null ||
                userPrinsipalFlagString.trim().length() == 0) {
            userPrinsipalFlagString = DEFAULT_USE_DN;
        }
        
        _userPrincipalEnabled = Boolean.valueOf(
                userPrinsipalFlagString).booleanValue();
        
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration.setUserPrincipalEnabledFlag: use-DN"
                    + " User Principal Enabled Flag is set to: " 
                    + _userPrincipalEnabled);
        }
    }
    
    private static synchronized void setSSOTokenName() {
        if (!isInitialized()) {
            _ssoTokenCookieName = getProperty(SDKPROP_SSO_COOKIE_NAME);
            if (_ssoTokenCookieName == null || 
                    _ssoTokenCookieName.trim().length() == 0) {
                throw new RuntimeException("Invalid SSO Cookie name set");
            }
        
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: SSO Token name set to: "
                        + _ssoTokenCookieName);
            }
        }
    }

    private static synchronized void setPolicyAdminLoc() {
	if (!isInitialized()) {
            _policyAdminLoc = getProperty("com.sun.identity.agent.policyadmin.location");
            if (isLogMessageEnabled()) {
		logMessage("AgentConfiguration: Policy Admin Location: "
			+ _policyAdminLoc);
            }
	}
    }

    
    private static synchronized void setApplicationUser() {
        if (!isInitialized()) {
            _applicationUser = getProperty(SDKPROP_APP_USERNAME);
            if (_applicationUser == null || _applicationUser.trim().length()==0)
            {
                throw new RuntimeException(
                        "Invalid application user specified");
            }
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: Application User: "
                        + _applicationUser);
            }
        }
    }
    
    private static synchronized void setApplicationPassword() {           
        if (!isInitialized()) {
            try {
                    _crypt = ServiceFactory.getCryptProvider();
                if(_crypt != null) {
                    String encodedPass = getProperty(SDKPROP_APP_PASSWORD);
                    _applicationPassword = 
                        _crypt.decrypt(encodedPass);
                }
            } catch (Exception ex) {
                logError("AgentConfiguration: Unable to create new instance of "
                    + "Crypt class with exception ", ex);
            }

            if (_applicationPassword == null || 
                _applicationPassword.trim().length() == 0) {
                throw new RuntimeException(
                        "Invalid application password specified");
            }
        }
    }
    
    private static synchronized void setProfileName() {
        if (!isInitialized()) {
            _profileName = getProperty(CONFIG_PROFILE_NAME);
            if ((_profileName == null) ||(_profileName.trim().length() == 0)) {
                _profileName = getApplicationUser();
            }
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: Profile Name: " + _profileName);
            }
        }
    }
    
    private static synchronized void setUserIdPropertyName() {
        String propertyName = getProperty(CONFIG_USER_ID_PROPERTY);
        if (propertyName == null || propertyName.trim().length() == 0) {
            propertyName = DEFAULT_USER_ID_PROPERTY;
            if (isLogWarningEnabled()) {
                logWarning("AgentConfiguration.setUserIdPropertyName: No value"
                     + " specified for user id property name. Using default: " 
                     + propertyName);
            }
        }
        _userIdPropertyName = propertyName;       
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration.setUserIdPropertyName: User id"
                    + " property name set to: "
                    + _userIdPropertyName);
            }
    }
    
    //this property is a hot swappable ClientSDK property
    private static synchronized void setSessionNotificationEnabledFlag() {
            _sessionNotificationEnabledFlag = true;
            boolean pollingEnabled = false;
            String flag = getProperty(SDKPROP_SESSION_POLLING_ENABLE);
            if (flag != null && flag.trim().length() > 0) {
                pollingEnabled = Boolean.valueOf(flag).booleanValue();
            }
            
            _sessionNotificationEnabledFlag = !pollingEnabled;
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: Session notification enable: " 
                        + _sessionNotificationEnabledFlag);
            }        
    }
    

    
    //this property is used by both the agentsdk code and also the clientsdk
    private static synchronized void setClientNotificationURL() {
        if (!isInitialized()) {
            String url = getProperty(SDKPROP_CLIENT_NOTIFICATION_URL);
            if (url != null && url.trim().length() > 0) {
                _clientNotificationURL = url;
            } else {
                if (isLogWarningEnabled()) {
                    logWarning(
                         "AgentConfiguration: No client notification URL set");
                }
            }
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: Client notification URL: "
                        + _clientNotificationURL);
            }
        }
    }
    
    //this property is a hot swappable ClientSDK property
    private static synchronized void setPolicyNotificationEnabledFlag() {
            boolean enable = false;
            String flag = getProperty(SDKPROP_POLICY_NOTIFICATION_ENABLE);
            if (flag != null && flag.trim().length() > 0) {
                enable = Boolean.valueOf(flag).booleanValue();
            }
            
            _policyNotificationEnabledFlag = enable;
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: Policy notification enable: " 
                        + _policyNotificationEnabledFlag);
            }
    }
    
    private static synchronized void setClientIPAddressHeader() {
            _clientIPAddressHeader = getProperty(CONFIG_CLIENT_IP_HEADER);
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration.setClientIPAddressHeader:"
                        + " Client IP Address Header: "
                        + _clientIPAddressHeader);
            }
    }
    
    private static synchronized void setClientHostNameHeader() {
        _clientHostNameHeader = getProperty(CONFIG_CLIENT_HOSTNAME_HEADER);
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration.setClientHostNameHeader: Client"
                    + " Hostname Header: "
                    + _clientHostNameHeader);
            }
    }
    
    private static synchronized void initializeConfiguration() {
        if (!isInitialized()) {
            //read in all properties, save all props & values in map to use 
            //later and push some to system for clientsdk
            bootStrapClientConfiguration();  
            registerAgentNotificationHandler();          
            //now set some class fields with property values         
            setHotSwappableConfigProps();          
            //set fields as some clientsdk props also used by agent code
            setSSOTokenName();              
            
            setHotSwappableClientSDKProps();     
            logAgentVersion();
            logServerVersion();
            logAgentEnv();
            markInitialized(); 
        }
    }
    
    /**
     * Logs the version information for the running agent.
     */
    private static void logAgentVersion() {
        logError(Version.getVersion());
    }
    
    /**
     * Logs some of the system env information for the running agent.
     */
    private static void logAgentEnv() {  
        if (isLogMessageEnabled()) {
            logMessage("Agent Env information:");
            logMessage("Java version=" + System.getProperty("java.version"));
            logMessage("Java vendor=" + System.getProperty("java.vendor"));
            logMessage("OS arch info=" + System.getProperty("os.arch"));
            logMessage("OS name=" + System.getProperty("os.name"));
            logMessage("OS version=" + System.getProperty("os.version"));
        }
    }
    
    /**
     * Logs the version of OpenSSO Server.
     */
    private static void logServerVersion() {
        String version = null;
        try {
            Vector attrServiceURLs = getAttributeServiceURLs();
            version = AgentRemoteConfigUtils.getServerVersion(attrServiceURLs,
                    getDebug());
            if (version == null || version.length() == 0) {
                version  = "Access Manager 7.x Server";
            }
        } catch (AgentException ex) {
            version = "Unknown Server Version due to: " + ex.getMessage();
        }
        
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration.logServerVersion() - \n\n" +
                    "------------------------------------------------\n" +
                    "OpenAM Server Version:" + version + "\n" +
                    "------------------------------------------------\n");
        }
    }
    
    /**
     * Some properties managed by this class are hotswappable and should be
     * set on initialization and reload of configuration property values
     */
    private static void setHotSwappableConfigProps() {
        setAuditLogMode();
        setUserMappingMode();
        setUserAttributeName();
        setUserPrincipalEnabledFlag();
        setUserIdPropertyName();
        setClientIPAddressHeader();
        setClientHostNameHeader();
    }
    
    /**
     * some of clientSDK props are hot-swappable and are used by clientSDK thru
     * SystemProperties helper class and are ALSO used by agent code and hence 
     * we store their current values in some fields.
     */
    private static void setHotSwappableClientSDKProps() {
        setSessionNotificationEnabledFlag();
        setPolicyNotificationEnabledFlag();
    }
    
    
    private static void logMessage(String msg) {
        getDebug().message(msg);
    }
    
    private static void logMessage(String msg, Throwable th) {
        getDebug().message(msg, th);
    }
    
    private static void logWarning(String msg) {
        getDebug().warning(msg);
    }
    
    private static void logWarning(String msg, Throwable th) {
        getDebug().warning(msg, th);
    }
    
    private static void logError(String msg) {
        getDebug().error(msg);
    }
    
    private static void logError(String msg, Throwable th) {
        getDebug().error(msg, th);
    }
    
    private static boolean isLogWarningEnabled() {
        return getDebug().warningEnabled();
    }
    
    private static boolean isLogMessageEnabled() {
        return getDebug().messageEnabled();
    }    
   
    private static void updatePropertiesUponPolling() {
        if (needToRefresh()) {
            if (!isAgentConfigurationRemote()) {
                File configFile = new File(getLocalConfigFilePath());
                if (!configFile.exists()) {
                    configFile = new File(getConfigFilePath());
                }
                if(getLastLoadTime() > configFile.lastModified()) {
                    markCurrent();
                    return; 
                } 
            }
            hotSwapAgentConfiguration(false);
        }
    }
    
    private static void hotSwapAgentConfiguration(boolean fromNotification) {
        
        // if lock config is enabled, there will be no config change.
        if (getLockConfig()) {
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration.hotSwapAgentConfiguration() - " +
                        "Agent config is locked, there's no config update.");
            }
            return;
        }
        
        if (loadProperties(fromNotification)) {
            notifyModuleConfigurationListeners();
            // notify possible debug level change
            if (debugObserver != null) {
                debugObserver.notifyChanges();
            }
        }
    }
    
    private static void notifyModuleConfigurationListeners() {

        Vector configurationListeners = getModuleConfigurationListeners();
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration: Notifying all listeners");
        }

        synchronized(configurationListeners) {
            for(int i = 0; i < configurationListeners.size(); i++) {
                IConfigurationListener nextListener =
                    (IConfigurationListener) configurationListeners.get(i);

                nextListener.configurationChanged();

                if(isLogMessageEnabled()) {
                    logMessage("AgentConfiguration: Notified listener for "
                               + nextListener.getName());
                }
            }
        }
    }    

    private synchronized static boolean loadProperties(
            boolean fromNotification) {
        boolean result = false;
        try {
            Properties properties = new Properties();
            properties.clear();     
            //add in bootstrap properties which were saved on
            //agent initial start up
            properties.putAll(getBootstrapProperties());
            
            Properties tempProperties;
            if (!isAgentConfigurationRemote()) {          
                tempProperties = getPropertiesFromLocal();
            } else {
                tempProperties = getPropertiesFromRemote(
                        getAttributeServiceURLs());
            }

            // check and return if notification.enabled is false.
            String notificationEnabled = tempProperties.getProperty(
                CONFIG_CENTRALIZED_NOTIFICATION_ENABLE,
                DEFAULT_CENTRALIZED_NOTIFICATION_ENABLE);      
            if (fromNotification &&
                    !notificationEnabled.equalsIgnoreCase("true")) {
                if (isLogMessageEnabled()) {
                    logMessage("AgentConfiguration: received config " + 
                        "notification, but no update " + 
                        "since notification enabled is false");
                }
                return false;
            }
            
            String modIntervalString = tempProperties.getProperty(
                    CONFIG_LOAD_INTERVAL);
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: interval=" + modIntervalString);
            }
            if (modIntervalString != null && 
                    modIntervalString.trim().length()>0) {
                modIntervalString = modIntervalString.trim();
            } else {
                logWarning("AgentConfiguration: No mod interval setting found");
                modIntervalString = "0";
            }
            long modInterval = 0L;
            try {
                modInterval = Long.parseLong(modIntervalString);
            } catch (NumberFormatException nfex) {
                logWarning("AgentConfiguration: Exception while reading "
                        + "new mod interval: \"" + modIntervalString + "\"");
            }
            
            if (!fromNotification && modInterval == 0) {
                setModInterval(modInterval);
                return false;
            }
            
            properties.putAll(tempProperties);
            
            setModInterval(modInterval*1000L);
            getProperties().clear();
            getProperties().putAll(properties);
            markCurrent();

            Iterator it = properties.keySet().iterator();
            while (it.hasNext()) {
                String nextKey = (String) it.next();
                if (!nextKey.startsWith(AGENT_CONFIG_PREFIX)) {
                    String nextValue = getProperties().getProperty(nextKey);
                    SystemProperties.initializeProperties(nextKey, nextValue);
                }
            }
            //set local copies of config property values stored by this class
            setHotSwappableConfigProps();
            //set local copies of some clientsdk property values we store
            setHotSwappableClientSDKProps();
            
            result = true;            
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: loaded new configuration.");
            }          
        } catch (Exception ex) {
            result = false;
            logError("AgentConfiguration: Exception during reload:", ex);
            logError("AgentConfiguration: Setting reload interval to 0");
            setModInterval(0L);
        }
        return result;
    }

    private static boolean needToRefresh() {
        return((System.currentTimeMillis() - getLastLoadTime())
               >= getModInterval());
    }
    
    
    private static class ConfigurationMonitor extends GeneralTaskRunnable {

        public ConfigurationMonitor() {

            if(isLogMessageEnabled()) {
                logMessage("AgentConfiguration: Monitor initialized");
            }
        }

        public boolean isEmpty() {
            return true;
        }

        public boolean addElement(Object obj) {
            return false;
        }

        public boolean removeElement(Object obj) {
            return false;
        }

        public long getRunPeriod() {
            long interval = getModInterval();
            if (interval == 0L) { interval = 3600000L; } // 1 hour.
            return interval;
        }

        public void run() {

            if(isLogMessageEnabled()) {
                logMessage("AgentConfiguration: Monitor started");
            }
            updatePropertiesUponPolling();
        }
    }    
    
    private static boolean isInitialized() {
        return _initialized;
    }
    
    private static void markInitialized() {
        if (!isInitialized()) {
            _initialized = true;
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: initialized.");
            }
        }
    }
    
    private static boolean isAgentConfigurationRemote() {
        return _isAgentConfigurationRemote;
    }
   
    private static void markAgentConfigurationRemote() {
        _isAgentConfigurationRemote = true; 
    }
   
    private static String getConfigFilePath() {
        return _configFilePath;
    }
    
    private static void setConfigFilePath(String configFilePath) {
        _configFilePath = configFilePath;
    }
    
    private static String getLocalConfigFilePath() {
        return _localConfigFilePath;
    }
    
    private static void setLocalConfigFilePath(String localConfigFilePath) {
        _localConfigFilePath = localConfigFilePath;
    }
    
    private static Properties getProperties() {
        return _properties;
    }
    
    private static void setDebug(Debug debug) {
        _debug = debug;
    }
    
    private static Debug getDebug() {
        return _debug;
    }  
    
    private static void setModInterval(long modInterval) {
        _modInterval = modInterval;
    }
    
    private static long getModInterval() {
        return _modInterval;
    }
    
    private static long getLastLoadTime() {
        return _lastLoadTime;
    }
    
    private static void markCurrent() {
        _lastLoadTime = System.currentTimeMillis();
    }    
    
    private static Vector getModuleConfigurationListeners() {
        return _moduleConfigListeners;
    }
    
    private static void setBootstrapProperties(Properties bootstrapProperties){
        //ensure its only set once at start up, not on configuration reloads
        if (!isInitialized()) {
            _bootstrapProperties.putAll(bootstrapProperties);
        }
    }
    
    private static Properties getBootstrapProperties() {
        return _bootstrapProperties;
    }
     
    private static boolean getLockConfig() {
        return _lockConfig;
    }
    
    private static String getProfileName() {
        return _profileName;
    }
    
    private static void setLockConfig() {
        if (!isInitialized()) {
            String lockConfig = getProperty(CONFIG_LOCK_ENABLE);
            if (lockConfig != null && lockConfig.equalsIgnoreCase("true")) {
                _lockConfig = true;
            }
        }
    }
      
    private static boolean _isAgentConfigurationRemote = false;
    private static boolean _initialized;
    private static String _configFilePath;
    private static String _localConfigFilePath;
    private static Properties _properties = new Properties();
    private static Properties _bootstrapProperties = new Properties();
    private static Debug _debug;
    private static long _modInterval = 0L;
    private static long _lastLoadTime = 0L;
    private static Vector _moduleConfigListeners = new Vector();
    private static ServiceResolver _serviceResolver;
    private static String _organizationName = DEFAULT_ORG_NAME;
    private static UserMappingMode _userMappingMode = 
        UserMappingMode.MODE_USER_ID;
    private static String _userAttributeName;
    private static boolean _userPrincipalEnabled;
    private static String _ssoTokenCookieName;
    private static String _applicationUser;
    private static String _applicationPassword;
    private static String _policyAdminLoc;
    private static String _userIdPropertyName;
    private static AuditLogMode _auditLogMode = AuditLogMode.MODE_BOTH;
    private static String _clientNotificationURL;
    private static boolean _policyNotificationEnabledFlag;
    private static boolean _sessionNotificationEnabledFlag;
    private static String _clientIPAddressHeader;
    private static String _clientHostNameHeader;
    private static ICrypt _crypt;
    private static SSOToken _appSSOToken = null;
    private static Vector _attributeServiceURLs = null;
    private static DebugPropertiesObserver debugObserver; 
    private static boolean _lockConfig = false;
    private static String _profileName;
    
    static {
        initializeConfiguration();
    }
}
