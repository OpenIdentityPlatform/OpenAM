/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ServerConfiguration.java,v 1.16 2010/01/15 18:10:55 veiming Exp $
 *
 */
/**
 * Portions Copyrighted 2011-2013 ForgeRock Inc
 */
package com.sun.identity.common.configuration;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.RemoteServiceAttributeValidator;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.forgerock.openam.upgrade.UpgradeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * This manages server configuration information.
 */
public class ServerConfiguration extends ConfigurationBase {
    private static final String ATTR_PARENT_SITE_ID = "parentsiteid";
    private static final String ATTR_SERVER_CONFIG = "serverconfig";
    private static final String ATTR_SERVER_CONFIG_XML = "serverconfigxml";

    public static final String SERVER_DEFAULTS = "serverdefaults";
    public static final String DEFAULT_SERVER_ID = "00";

    /**
     * Default server configuration.
     */
    public static final String DEFAULT_SERVER_CONFIG = "server-default";

    // prevent instantiation of this class.
    private ServerConfiguration() {
    }

    /**
     * Returns a set of server information where each entry in a set is
     * a string of this format
     * <code>server-instance-name|serverId|siteId1|siteId2|...|siteIdn</code>.
     *
     * @param ssoToken Single Sign-On Token which is used to query the service
     *        management datastore.
     * @return a set of server information.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     */
    public static Set getServerInfo(SSOToken ssoToken) 
        throws SMSException, SSOException {
        Set serverInfo = null;
        
        if (isLegacy(ssoToken)) {
            serverInfo = legacyGetServerInfo(ssoToken);
        } else {
            serverInfo = new HashSet();
            ServiceConfig sc = getRootServerConfig(ssoToken);

            if (sc != null) {
                Set names = sc.getSubConfigNames();
                for (Iterator i = names.iterator(); i.hasNext(); ) {
                    String name = (String)i.next();
                    ServiceConfig cfg = sc.getSubConfig(name);
                    Map attrs = cfg.getAttributes();
                    Set setID = (Set)attrs.get(ATTR_SERVER_ID);
                    String serverId = (String)setID.iterator().next();

                    if (!serverId.equals(DEFAULT_SERVER_ID)) {
                        Set setSiteId = (Set)attrs.get(ATTR_PARENT_SITE_ID);
                        
                        if ((setSiteId != null) && !setSiteId.isEmpty()) {
                            String siteName =
                                (String)setSiteId.iterator().next();
                            Set ids = getSiteConfigurationIds(
                                ssoToken, null, siteName, false);
                            StringBuilder buff = new StringBuilder();
                            for (Iterator it = ids.iterator(); it.hasNext(); ) {
                                buff.append("|").append((String)it.next());
                            }
                            serverInfo.add(name + "|" +  serverId + 
                                buff.toString());
                        } else {
                            serverInfo.add(name + "|" + serverId);
                        }
                    }
                }
            }
        }
        return serverInfo;
    }
    
    /**
    * Returns a map of server name to its load balancer cookie value.
    * @param ssoToken Single Sign-On Token which is used to query the service
    *        management datastore.
    * @return a map of server id to its load balancer cookie value.
    * @throws SMSException if errors access in the service management
    *         datastore.
    * @throws SSOException if the <code>ssoToken</code> is not valid.
    */
    public static Map getLBCookieValues(SSOToken ssoToken)
        throws SMSException, SSOException, IOException {
        Map results = new HashMap();

        if (!isLegacy(ssoToken)) {
            ServiceConfig sc = getRootServerConfig(ssoToken);

            if (sc != null) {
                Set names = sc.getSubConfigNames("*");
                for (Iterator i = names.iterator(); i.hasNext(); ) {
                    String name = (String)i.next();
                    ServiceConfig cfg = sc.getSubConfig(name);
                    Map attrs = cfg.getAttributes();
                    Set setID = (Set)attrs.get(ATTR_SERVER_ID);
                    String serverId = (String)setID.iterator().next();

                    if (!serverId.equals(DEFAULT_SERVER_ID)) {
                        Properties propMap = getProperties(
                            (Set)attrs.get(ATTR_SERVER_CONFIG));
                        String cValue = (String)propMap.get(
                            Constants.PROPERTY_NAME_LB_COOKIE_VALUE);
                        if ((cValue != null) && (cValue.length() > 0)) {
                            results.put(serverId, cValue);
                        }
                    }
                }
            }
        }
        return results;
    }         

    /**
     * Returns a set of server instance name (String).
     *
     * @param ssoToken Single Sign-On Token which is used to query the service
     *        management datastore.
     * @return a set of server instance name.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     */
    public static Set getServers(SSOToken ssoToken) 
        throws SMSException, SSOException {
        Set servers = new HashSet();
        
        if (isLegacy(ssoToken)) {
            Set serverInfo = legacyGetServerInfo(ssoToken);
            if ((serverInfo != null) && !serverInfo.isEmpty()) {
                for (Iterator i = serverInfo.iterator(); i.hasNext(); ) {
                    String server = (String)i.next();
                    int idx = server.indexOf('|');
                    if (idx != -1) {
                        server = server.substring(0, idx);
                    }
                    servers.add(server);
                }
            }
        } else {
            ServiceConfig sc = getRootServerConfig(ssoToken);
            if (sc != null) {
                servers.addAll(sc.getSubConfigNames("*"));
                servers.remove(DEFAULT_SERVER_CONFIG);
            }
            
        }
        return servers;
    }



    /**
     * Creates a server instance.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @param instanceName Name of the server instance.
     * @param values Map of string to set of (String) values.
     * @param serverConfigXML Server configuration XML.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     * @throws IOException if IO operation fails.
     * @throws UnknownPropertyNameException if property names are unknown.
     * @throws ConfigurationException if property names or values are not
     *         valid.
     */
    public static void createServerInstance(
        SSOToken ssoToken,
        String instanceName,
        Map values,
        String serverConfigXML
    ) throws SMSException, SSOException, IOException, ConfigurationException,
        UnknownPropertyNameException {
        createServerInstance(ssoToken, instanceName, getPropertiesSet(values),
            serverConfigXML);
    }
    
    /**
     * Creates a server instance.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @param instanceName Name of the server instance.
     * @param values Set of string with this format <code>key=value</code>.
     * @param serverConfigXML Server configuration XML.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     * @throws UnknownPropertyNameException if property names are unknown.
     * @throws ConfigurationException if property names or values are not
     *         valid.
     */
    public static void createServerInstance(
        SSOToken ssoToken,
        String instanceName,
        Set values,
        String serverConfigXML
    ) throws SMSException, SSOException, ConfigurationException,
        UnknownPropertyNameException {
        if (isLegacy(ssoToken)) {
            ServiceSchemaManager sm = new ServiceSchemaManager(
                Constants.SVC_NAME_PLATFORM, ssoToken);
            String serverId = getNextId(ssoToken);
            ServiceSchema sc = sm.getGlobalSchema();
            Map attrs = sc.getAttributeDefaults();
            Set servers = (Set)attrs.get(OLD_ATTR_SERVER_LIST);
            //need to do this because we are getting Collections.EMPTY.SET;
            if ((servers == null) || servers.isEmpty()) { 
                servers = new HashSet();
            }
            servers.add(instanceName + "|" + serverId);
            sc.setAttributeDefaults(OLD_ATTR_SERVER_LIST, servers);
            updateOrganizationAlias(ssoToken, instanceName, true);
        } else {
            ServiceConfig sc = getRootServerConfig(ssoToken);
            if (sc != null) {
                String serverId = getNextId(ssoToken);
                createServerInstance(ssoToken, instanceName, serverId, values,
                    serverConfigXML);
            }
        }
    }
    
    public static void createDefaults(
        SSOToken ssoToken
    ) throws SSOException, SMSException, UnknownPropertyNameException {
        boolean bCreated = false;
        ServiceConfig sc = getRootServerConfig(ssoToken);
        try {
            bCreated = (sc.getSubConfig(DEFAULT_SERVER_CONFIG) != null);
        } catch (SMSException e) {
            // ignore, default is not created.
        }
        if (!bCreated) {
            ResourceBundle res = ResourceBundle.getBundle(SERVER_DEFAULTS);
            Set values = new HashSet();
            
            for (Enumeration i = res.getKeys(); i.hasMoreElements(); ) {
                String key = (String)i.nextElement();
                String val = (String)res.getString(key);
                if (val.equals(
                    "@" + SetupConstants.CONFIG_VAR_PLATFORM_LOCALE + "@")
                ) {
                    val = Locale.getDefault().toString();
                }
                values.add(key + "=" + val);
            }

            try {
                createServerInstance(ssoToken, DEFAULT_SERVER_CONFIG,
                    DEFAULT_SERVER_ID, values, "");
            } catch (ConfigurationException ex) {
                //ignore, this should not happen because default values
                //are all valid.
            }
        }
    }
    
    public static String getWarFileVersion() {
        ResourceBundle res = ResourceBundle.getBundle(SERVER_DEFAULTS);
        
        if (res != null) {
            return res.getString(Constants.AM_VERSION);
        } else {
            UpgradeUtils.debug.error("Unable to determine war file version");
            return null;
        }
    }
    
    public static Map<String, String> getNewServerDefaults(SSOToken ssoToken) throws SMSException, SSOException {
        boolean bCreated = false;
        ServiceConfig sc = getRootServerConfig(ssoToken);
        try {
            bCreated = (sc.getSubConfig(DEFAULT_SERVER_CONFIG) != null);
        } catch (SMSException smse) {
            // ignore, default is not created.
        }

        if (bCreated) {
            ResourceBundle res = ResourceBundle.getBundle(SERVER_DEFAULTS);
            Map<String, String> newValues = new HashMap<String, String>();

            for (Enumeration<String> i = res.getKeys(); i.hasMoreElements();) {
                String key = i.nextElement();
                String val = res.getString(key);

                if (val.equals(
                        "@" + SetupConstants.CONFIG_VAR_PLATFORM_LOCALE + "@")) {
                    val = Locale.getDefault().toString();
                }

                newValues.put(key, val);
            }
            newValues.put(Constants.PROPERTY_NAME_LB_COOKIE_VALUE, DEFAULT_SERVER_ID);
            return newValues;
        } else {
            return Collections.EMPTY_MAP;
        }
    }

    public static Map getDefaultProperties() {
        Map map = new HashMap();
        ResourceBundle res = ResourceBundle.getBundle(SERVER_DEFAULTS);
        for (Enumeration i = res.getKeys(); i.hasMoreElements(); ) {
            String key = (String)i.nextElement();
            map.put(key, (String)res.getString(key));
        }
        return map;
    }

    /**
     * Returns server Identifier.
     *
     * @return server Identifier. Returns null if server Id is not stored in
     *         centralized datastore.
     */
    public static String getServerID(
        SSOToken ssoToken,
        String instanceName
    ) throws SMSException, SSOException {
        String serverId = null;
        if (!isLegacy(ssoToken)) {
            ServiceConfig cfg = getServerConfig(ssoToken, instanceName);
            if (cfg != null) {
                Map map = cfg.getAttributes();
                Set set = (Set)map.get(ATTR_SERVER_ID);
                serverId = (String)set.iterator().next();
            }
        }
        return serverId;
    }

    /**
     * Returns server configuration XML.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @param instanceName Name of the server instance.
     * @return server configuration XML.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     */
    public static String getServerConfigXML(
        SSOToken ssoToken,
        String instanceName
    ) throws SMSException, SSOException {
        String xml = null;
        if (!isLegacy(ssoToken)) {
            ServiceConfig cfg = getServerConfig(ssoToken, instanceName);
            if (cfg != null) {
                Map map = cfg.getAttributes();
                Set set = (Set)map.get(ATTR_SERVER_CONFIG_XML);
                xml = (String)set.iterator().next();
            }
        }
        return xml;
    }
   
    /**
     * Sets server configuration XML.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @param instanceName Name of the server instance.
     * @param xml Server configuration XML.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     */
    public static void setServerConfigXML(
        SSOToken ssoToken,
        String instanceName,
        String xml
    ) throws SMSException, SSOException, ConfigurationException {
        if (!isLegacy(ssoToken)) {
            ServiceConfig cfg = getServerConfig(ssoToken, instanceName);
            if (cfg != null) {
                Map map = new HashMap(2);
                Set set = new HashSet(2);
                set.add(xml);
                map.put(ATTR_SERVER_CONFIG_XML, set);
                cfg.setAttributes(map);
            }
        }
    }
    
    /**
     * Returns the configuration of a server instance.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @param instanceName Name of the server instance.
     * @return the configuration of the server Instance.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     * @throws IOException if IO operation fails.
     */
    public static Properties getServerInstance(
        SSOToken ssoToken,
        String instanceName
    ) throws SMSException, SSOException, IOException {
        Properties prop = null;
        if (!isLegacy(ssoToken)) {
            ServiceConfig cfg = getServerConfig(ssoToken, instanceName);
            if (cfg != null) {
                Map map = cfg.getAttributes();
                prop = getProperties((Set)map.get(ATTR_SERVER_CONFIG));
            }
        }
        return prop;
    }
    
    /**
     * Returns <code>true</code> if server instance exists.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @param instanceName Name of the server instance.
     * @return <code>true</code> if server instance exists.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     */
    public static boolean isServerInstanceExist(
        SSOToken ssoToken,
        String instanceName
    ) throws SMSException, SSOException {
        Set servers =  getServers(ssoToken);
        return servers.contains(instanceName);
    }
    
    /**
     * Sets configuration to a server instance.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @param instanceName Name of the server instance.
     * @param newValues Map of string to Set of string.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     * @throws UnknownPropertyNameException if property names are unknown.
     * @throws ConfigurationException if property names or values are not
     *         valid.
     */
    public static void setServerInstance(
        SSOToken ssoToken,
        String instanceName,
        Map newValues
    ) throws SMSException, SSOException, IOException, ConfigurationException,
        UnknownPropertyNameException {
        if (!isLegacy(ssoToken)) {
            ServiceConfig cfg = getServerConfig(ssoToken, instanceName);
            if (cfg != null) {
                Map map = cfg.getAttributes();
                Set existingSet = (Set)map.get(ATTR_SERVER_CONFIG);
                Set newSet = getPropertiesSet(newValues);
                try {
                    validateProperty(ssoToken, newSet);
                    map.put(ATTR_SERVER_CONFIG,
                        combineProperties(existingSet, newSet));
                    cfg.setAttributes(map);
                } catch (UnknownPropertyNameException e) {
                    //save the values even if property name is unknown
                    map.put(ATTR_SERVER_CONFIG,
                        combineProperties(existingSet, newSet));
                    cfg.setAttributes(map);
                    throw e;
                }
            }
        }
    }
    
    private static void validateProperty(SSOToken token, Set properties)
        throws UnknownPropertyNameException, ConfigurationException {
        if (SystemProperties.isServerMode()) {
            ServerPropertyValidator.validateProperty(properties);
        } else {
            try {
                if (!RemoteServiceAttributeValidator.validate(token, 
                "com.sun.identity.common.configuration.ServerPropertyValidator",
                properties)
                ) {
                    throw new UnknownPropertyNameException(
                        "invalid.properties", null);
                }
            } catch (SMSException e) {
                throw new ConfigurationException("unable.to.connect.to.server",
                    null);
            }
        }
    }
    /**
     * Removes server configuration. This will result in inheriting from
     * default server configuration.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @param instanceName Name of the server instance.
     * @param propertyNames Collection of property names to be removed.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     */
    public static void removeServerConfiguration(
        SSOToken ssoToken,
        String instanceName,
        Collection propertyNames
    ) throws SMSException, SSOException, IOException {
        if (!isLegacy(ssoToken)) {
            ServiceConfig cfg = getServerConfig(ssoToken, instanceName);
            if (cfg != null) {
                Map map = cfg.getAttributes();
                Set set = (Set)map.get(ATTR_SERVER_CONFIG);
                Properties properties = getProperties(set);
                
                for (Iterator i = properties.keySet().iterator(); i.hasNext(); 
                ) {
                    String key = (String)i.next();
                    if (propertyNames.contains(key)) {
                        i.remove();
                    }
                }
                map.put(ATTR_SERVER_CONFIG, getPropertiesSet(properties));
                cfg.setAttributes(map);
            }
        }
    }

    /**
     * Deletes a server instance.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @param instanceName Name of the server instance.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     */
    public static boolean deleteServerInstance(
        SSOToken ssoToken,
        String instanceName
    ) throws SMSException, SSOException {
        boolean deleted = false;
        
        if (isLegacy(ssoToken)) {
            ServiceSchemaManager sm = new ServiceSchemaManager(
                Constants.SVC_NAME_PLATFORM, ssoToken);
            ServiceSchema sc = sm.getGlobalSchema();
            Map attrs = sc.getAttributeDefaults();
            String serverInstance = instanceName + "|";
            Set servers = (Set)attrs.get(OLD_ATTR_SERVER_LIST);
            
            for (Iterator i = servers.iterator(); i.hasNext() && !deleted; ) {
                String s = (String)i.next();
                if (s.startsWith(serverInstance)) {
                    i.remove();
                    deleted = true;
                }
            }

            if (deleted) {
                sc.setAttributeDefaults(OLD_ATTR_SERVER_LIST, servers);
            }
        } else {
            ServiceConfig cfg = getServerConfig(ssoToken, instanceName);
            if (cfg != null) {
                ServiceConfig sc = getRootServerConfig(ssoToken);
                sc.removeSubConfig(instanceName);
                deleted = true;
            } 
        }

        return deleted;
    }

    /**
     * Creates a server instance.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @param instanceName Name of the server instance.
     * @param instanceId Identifier of the server instance.
     * @param values Set of string with this format <code>key=value</code>.
     * @param serverConfigXML Service configuration XML.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     * @throws UnknownPropertyNameException if property names are unknown.
     * @throws ConfigurationException  if the property name and values are not
     *         valid.
     */
    public static boolean createServerInstance(
        SSOToken ssoToken,
        String instanceName,
        String instanceId,
        Set values,
        String serverConfigXML
    ) throws SMSException, SSOException, ConfigurationException,
        UnknownPropertyNameException {
        boolean created = false;
        if (!instanceName.equals(DEFAULT_SERVER_CONFIG)) {
            validateProperty(ssoToken, values);
        }
        ServiceConfig sc = getRootServerConfig(ssoToken);
        
        if (sc != null) {
            if (!instanceName.equals(DEFAULT_SERVER_CONFIG)) {
                try {
                    new URL(instanceName);
                } catch (MalformedURLException ex) {
                    String[] param = {instanceName};
                    throw new ConfigurationException("invalid.server.name", 
                        param);
                }
            }
            
            Map serverValues = new HashMap(4);
            
            Set setServerId = new HashSet(2);
            setServerId.add(instanceId);
            serverValues.put(ATTR_SERVER_ID, setServerId);
            
            if (values.isEmpty()) {
                values = new HashSet(2);
            }
            values.add(Constants.PROPERTY_NAME_LB_COOKIE_VALUE + "=" +
                instanceId);             
            
            Set setServerConfigXML = new HashSet(2);
            setServerConfigXML.add(serverConfigXML);
            serverValues.put(ATTR_SERVER_CONFIG_XML, setServerConfigXML);
            
            serverValues.put(ATTR_SERVER_CONFIG, values);
            if (!instanceName.equals(DEFAULT_SERVER_CONFIG)) {
                setProtocolHostPortURI(serverValues, instanceName);
            }

            sc.addSubConfig(instanceName, SUBSCHEMA_SERVER, 0, serverValues);
            created = true;
        }

        if (created && !instanceName.equals(DEFAULT_SERVER_CONFIG)) {
            updateOrganizationAlias(ssoToken, instanceName, true);
        }
        return created;
    }
    
    /**
     * Upgrades a server instance.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @param instanceName Name of the server instance.
     * @param instanceId Identifier of the server instance.
     * @param upgradedValues Map of new values for the default server config
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     * @throws UnknownPropertyNameException if property names are unknown.
     * @throws ConfigurationException  if the property name and values are not
     *         valid.
     */
    public static void upgradeServerInstance(
        SSOToken ssoToken,
        String instanceName,
        String instanceId,
        Map<String, String> upgradedValues
    ) throws SMSException, SSOException, ConfigurationException, IOException {
        ServiceConfig sc = getServerConfig(ssoToken, instanceName);
        
        if (sc != null) {
            Map map = sc.getAttributes();
            // remove ATTR_PARENT_SITE_ID as this should be excluded from server-default
            map.remove(ATTR_PARENT_SITE_ID);
            Set newSet = getPropertiesSet(upgradedValues);
            
            map.put(ATTR_SERVER_CONFIG, newSet);
            sc.setAttributes(map);
        } else {
            throw new ConfigurationException("Unable to upgrade server " +
                    "default properties: no properties found!");
        }
    }
   
    /**
     * Returns the default server properties.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @return the default server properties.
     */
    public static Properties getDefaults(SSOToken ssoToken) {
        Properties prop = null;

        try {
            if (!isLegacy(ssoToken)) {
                createDefaults(ssoToken);
                prop = getServerInstance(ssoToken, DEFAULT_SERVER_CONFIG);
            }
        } catch (SSOException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (UnknownPropertyNameException ex) {
            // ignore Default values should not have unknown property names.
        } catch (SMSException ex) {
            // amPlatform does not exist
            ResourceBundle res = ResourceBundle.getBundle(SERVER_DEFAULTS);
            prop = new Properties();
            for (Enumeration i = res.getKeys(); i.hasMoreElements(); ) {
                String key = (String)i.nextElement();
                prop.setProperty(key, res.getString(key));
            }
        }
        return prop;
    }

    /**
     * Returns properties object.
     *
     * @param str String of this format key1=value1\nkey2=value2\n...keyN=valueN
     * @return properties object.
     * @throws IOException if <code>str</code> contains incorrect format.
     */
    public static Properties getProperties(String str)
        throws IOException {
        Properties prop = new Properties();
        ByteArrayInputStream bis = null;
        try {
            bis = new ByteArrayInputStream(str.getBytes());
            prop.load(bis);
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return prop;
    }

    /**
     * Returns properties object.
     *
     * @param set Set of string of this format key=value.
     * @return properties object.
     * @throws IOException if <code>str</code> contains incorrect format.
     */
    public static Properties getProperties(Set set)
        throws IOException {
        Properties prop = new Properties();
        for (Iterator i = set.iterator(); i.hasNext(); ) {
            String str = (String)i.next();
            int idx = str.indexOf('=');
            if (idx != -1) {
                prop.setProperty(str.substring(0, idx), str.substring(idx+1));
            }
        }
        return prop;
    }

    /**
     * Returns set of string with this format, key=value.
     *
     * @param str String of this format key1=value1\nkey2=value2\n...keyN=valueN
     * @return set of formated string.
     * @throws IOException if <code>str</code> contains incorrect format.
     */
    public static Set getPropertiesSet(String str) 
        throws IOException {
        return getPropertiesSet(getProperties(str));
    }

    private static Set getPropertiesSet(Map prop) 
        throws IOException {
        Set set = new HashSet();
        for (Iterator i = prop.keySet().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            Object value = prop.get(key);
            String val = (value instanceof Set) ?
                (String)((Set)value).iterator().next() :
                (String)value;
            set.add(key + "=" + val);
        }
        return set;
    }

    private static Set combineProperties(Set existing, Set newOnes)
        throws IOException {
        Map map = new HashMap();
        Properties existingP = getProperties(existing);
        Properties newP = getProperties(newOnes);

        map.putAll(existingP);
        map.putAll(newP);

        return getPropertiesSet(map);
    }

    /**
     * Adds server to a site.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @param instanceName Name of the server instance.
     * @param siteId Identifier of the site.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     * @throws ConfigurationException if server instance is not found.
     */
    public static void addToSite(
        SSOToken ssoToken,
        String instanceName,
        String siteId
    ) throws SMSException, SSOException, ConfigurationException {
        if (isLegacy(ssoToken)) {
            legacyManageSite(ssoToken, instanceName, siteId, true);
        } else {
            ServiceConfig svr = getServerConfig(ssoToken, instanceName);

            if (svr != null) {
                Map attrs = svr.getAttributes();
                Set setID = (Set)attrs.get(ATTR_SERVER_ID);
                String serverId = (String)setID.iterator().next();

                if (!serverId.equals(DEFAULT_SERVER_ID)) {
                    Set set = new HashSet(2);
                    set.add(siteId);
                    attrs.put(ATTR_PARENT_SITE_ID, set);
                    svr.setAttributes(attrs);
                }
            } else {
                Object[] param = {instanceName};
                throw new ConfigurationException("invalid.server.instance",
                    param);
            }
        }
    }

    /**
     * Returns a site name of which server belongs to.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @param instanceName Name of the server instance.
     * @return a site name.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     */
    public static String getServerSite(SSOToken ssoToken, String instanceName)
        throws SMSException, SSOException {
        String site = null;
        ServiceConfig svr = getServerConfig(ssoToken, instanceName);

        if (svr != null) {
            Map attrs = svr.getAttributes();
            Set sites = (Set)attrs.get(ATTR_PARENT_SITE_ID);
            if ((sites != null) && !sites.isEmpty()) {
                site = (String)sites.iterator().next();
            }
        }
        return site;
    }

    /**
     * Sets site name of which server belongs to.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @param instanceName Name of the server instance.
     * @param siteName Site name.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     */
    public static void setServerSite(
        SSOToken ssoToken, 
        String instanceName,
        String siteName
    ) throws SMSException, SSOException {
        ServiceConfig svr = getServerConfig(ssoToken, instanceName);
        if (svr != null) {
            Map attrs = new HashMap();
            Set sites = new HashSet(2);
            sites.add(siteName);
            attrs.put(ATTR_PARENT_SITE_ID, sites);
            svr.setAttributes(attrs);
        }
    }

    /**
     * Removes server from a site.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @param instanceName Name of the server instance.
     * @param siteId Identifier of the site.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     */
    public static void removeFromSite(
        SSOToken ssoToken,
        String instanceName,
        String siteId
    ) throws SMSException, SSOException {
        if (isLegacy(ssoToken)) {
            legacyManageSite(ssoToken, instanceName, siteId, false);
        } else {
            ServiceConfig svr = getServerConfig(ssoToken, instanceName);
            if (svr != null) {
                Map attrs = svr.getAttributes();
                Set setID = (Set)attrs.get(ATTR_SERVER_ID);
                String serverId = (String)setID.iterator().next();

                if (!serverId.equals(DEFAULT_SERVER_ID)) {
                    attrs.put(ATTR_PARENT_SITE_ID, Collections.emptySet());
                    svr.setAttributes(attrs);
                }
            }
        }
    }

    private static void legacyManageSite(
        SSOToken ssoToken,
        String instanceName,
        String siteId,
        boolean bAdd
    ) throws SMSException, SSOException {
        AttributeSchema as = getLegacyServerAttributeSchema(ssoToken);
        Set servers = as.getDefaultValues();
        String target = null;

        for (Iterator i = servers.iterator(); i.hasNext() && (target == null);){
            String svr = (String)i.next();
            LegacyServer serverObj = new LegacyServer(svr);

            if (serverObj.name.equals(instanceName)) {
                if (bAdd) {
                    serverObj.addSite(target);
                } else {
                    serverObj.removeSite(target);
                }

                target = serverObj.toString();
                i.remove();
            }
        }

        if (target != null) {
            servers.add(target);
        }
        as.setDefaultValues(servers);
    }

    /**
     * Returns <code>true</code> if a server belongs to a site.
     *
     * @param ssoToken Single Sign-On Token which is used to access to the
     *        service management datastore.
     * @param instanceName Name of the server instance.
     * @param siteId Identifier of the site.
     * @throws SMSException if errors access in the service management
     *         datastore.
     * @throws SSOException if the <code>ssoToken</code> is not valid.
     */
    public static boolean belongToSite(
        SSOToken ssoToken,
        String instanceName,
        String siteId
    ) throws SMSException, SSOException {
        boolean belong = false;

        if (isLegacy(ssoToken)) {
            AttributeSchema as = getLegacyServerAttributeSchema(ssoToken);
            Set servers = as.getDefaultValues();
            boolean found = false;

            for (Iterator i = servers.iterator(); i.hasNext() && !found; ) {
                String svr = (String)i.next();
                LegacyServer serverObj = new LegacyServer(svr);

                if (serverObj.name.equals(instanceName)) {
                    found = true;
                    belong = serverObj.belongToSite(siteId);
                }
            }
        } else {
            String site = getServerSite(ssoToken, instanceName);
            belong = (site != null) && site.equals(siteId);
        }
        return belong;
    }

    private static AttributeSchema getLegacyServerAttributeSchema(
        SSOToken ssoToken
    ) throws SMSException, SSOException {
        ServiceSchemaManager scm = new ServiceSchemaManager(
            Constants.SVC_NAME_PLATFORM, ssoToken);
        ServiceSchema global = scm.getSchema(SchemaType.GLOBAL);
        return global.getAttributeSchema(OLD_ATTR_SERVER_LIST);
    }

    /**
     * Clones a server instance.
     *
     * @param serverName Server name to clone.
     * @param cloneName server name.
     */
    public static void cloneServerInstance(
        SSOToken ssoToken,
        String serverName,
        String cloneName
    ) throws SMSException, SSOException, ConfigurationException {
        if (!isLegacy(ssoToken)) {
            URL url = null;
            try {
                url = new URL(cloneName);
            } catch (MalformedURLException ex) {
                String[] param = {cloneName};
                throw new ConfigurationException("invalid.server.name", param);
            }
            
            ServiceConfig cfg = getServerConfig(ssoToken, serverName);
            if (cfg != null) {
                Map map = cfg.getAttributes();
                
                ServiceConfig sc = getRootServerConfig(ssoToken);
                String serverId = getNextId(ssoToken);
                Set setID = new HashSet(2);
                setID.add(serverId);
                map.put(ATTR_SERVER_ID, setID);
                setProtocolHostPortURI(map, cloneName);


                sc.addSubConfig(cloneName, SUBSCHEMA_SERVER, 0, map);
                updateOrganizationAlias(ssoToken, cloneName, true);
            }
        }
    }

    private static void setProtocolHostPortURI(Map map, String serverName)
        throws ConfigurationException {
        URL url = null;
        try {
            url = new URL(serverName);
            Properties propMap = getProperties(
                (Set)map.get(ATTR_SERVER_CONFIG));
            propMap.put(Constants.AM_SERVER_PROTOCOL,
                getSet(url.getProtocol()));
            propMap.put(Constants.AM_SERVER_HOST, getSet(url.getHost()));
            propMap.put(Constants.AM_SERVER_PORT,
                getSet(Integer.toString(url.getPort())));
            propMap.put(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR,
                getSet(url.getPath()));
            map.put(ATTR_SERVER_CONFIG, getPropertiesSet(propMap));
        } catch (MalformedURLException ex) {
            String[] param = {serverName};
            throw new ConfigurationException("invalid.server.name", param);
        } catch (IOException ie) {
            // ignore because the values should be well formated.
        }
    }

    private static Set getSet(String value){
        Set set = new HashSet(2);
        set.add(value);
        return set;
    }
 
    /**
     * Exports a server instance.
     *
     * @param serverName Server name to clone.
     * @return a XML representation of server instance.
     */
    public static String exportServerInstance(
        SSOToken ssoToken,
        String serverName
    ) throws SMSException, SSOException {
        String xml = null;
        if (!isLegacy(ssoToken)) {
            ServiceConfig cfg = getServerConfig(ssoToken, serverName);
            if (cfg != null) {
                Map map = cfg.getAttributes();
                StringBuilder buff = new StringBuilder();
                
                buff.append("<ServerConfiguration>\n");
                for (Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry entry = (Map.Entry)i.next();
                    
                    buff.append("<AttributeValuePair>\n");
                    buff.append("<Attribute name=\"")
                        .append((String)entry.getKey())
                        .append("\" />");
                    for (Iterator it = ((Set)entry.getValue()).iterator();
                        it.hasNext();
                    ) {
                        buff.append("<Value>")
                            .append(XMLUtils.escapeSpecialCharacters(
                                (String)it.next()))
                            .append("</Value>\n");
                    }
                    buff.append("</AttributeValuePair>\n");
                }
                buff.append("</ServerConfiguration>\n");
                xml = buff.toString();
            }
        }
        return xml;
    }
    
    /**
     * Imports a server instance.
     *
     * @param serverName Server name to clone.
     * @param xmlFile File that contains XML representation of server instance.
     */
    public static void importServerInstance(
        SSOToken ssoToken,
        String serverName,
        String xmlFile
    ) throws SMSException, SSOException, IOException, SAXException,
        ParserConfigurationException, ConfigurationException  {
        if (!isLegacy(ssoToken)) {
            
            try {
                new URL(serverName);
            } catch (MalformedURLException ex) {
                String[] param = {serverName};
                throw new ConfigurationException("invalid.server.name", param);
            }
            ServiceConfig cfg = getServerConfig(ssoToken, serverName);
            if (cfg == null) {
                DocumentBuilder builder = XMLUtils.getSafeDocumentBuilder(false);
                Document document = builder.parse(xmlFile);
                Element topElement = document.getDocumentElement();
                Map map = XMLUtils.parseAttributeValuePairTags(
                    (Node)topElement);
                
                ServiceConfig sc = getRootServerConfig(ssoToken);
                String serverId = getNextId(ssoToken);
                Set setID = new HashSet(2);
                setID.add(serverId);
                map.put(ATTR_SERVER_ID, setID);

                sc.addSubConfig(serverName, SUBSCHEMA_SERVER, 0, map);
                updateOrganizationAlias(ssoToken, serverName, true);
            }
        }
    }
}
