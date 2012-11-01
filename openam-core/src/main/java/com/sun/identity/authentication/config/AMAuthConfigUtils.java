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
 * $Id: AMAuthConfigUtils.java,v 1.5 2008/06/25 05:41:51 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.config;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMTemplate;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.security.auth.login.AppConfigurationEntry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Authentication Configuration Utility.
 */
public class AMAuthConfigUtils {
    private static Debug debug = Debug.getInstance("amAuthConfig");

    protected static final String SERVICE_NAME = "iPlanetAMAuthConfiguration";
    protected static final String NAMED_CONFIGURATION = "Configurations";
    protected static final String NAMED_CONFIGURATION_ID = "NamedConfiguration";
    protected static final String SERVICE_VERSION = "1.0";
    protected static final String ATTR_VALUE_PAIR_NODE = "AttributeValuePair";
    protected static final String ATTR_VALUE_NODE = "Value";
    public static final String ATTR_NAME = "iplanet-am-auth-configuration";

    protected static final String MODULE_KEY = "MODULE";
    protected static final String USER_KEY = "USER";
    protected static final String ORG_KEY = "ORGANIZATION";
    protected static final String SERVICE_KEY = "SERVICE";
    protected static final String ROLE_KEY = "ROLE";
    protected static final String CLIENT_KEY = "CLIENT";
    private static final String AUTH_SERVICE = "iPlanetAMAuthService";
    private static final String AUTH_MODULES_ATTR = 
        "iplanet-am-auth-allowed-modules";
    private static final String AUTH_AUTHENTICATOR_ATTR = 
        "iplanet-am-auth-authenticators";
    private static final String APPLICATION_CLASS_NAME =
        "com.sun.identity.authentication.modules.application.Application";

    protected static String bundleName = "amAuthConfig";

    /**
     * Parses the string value for the authentication configuration 
     * based on the attribute DTD and return an array of
     * <code>AppConfigurationEntry</code> which could be used to retrieve
     * module name, flag and options. Empty array of
     * <code>AppConfigurationEntry</code> will be returned if the XML value
     * could not be parsed.
     *
     * @param xmlValue XML string value for the authentication configuration.
     * @return Array of <code>AppConfigurationEntry</code> each contains module
     *         name, flag and options.
     */
    public static AppConfigurationEntry[] parseValues(String xmlValue) {
        if (debug.messageEnabled()) {
            debug.message(
                "AuthConfigUtil.AppConfigurationEntry, xml=" + xmlValue);
        }
        // call util method to parse the document
        Document document = XMLUtils.toDOMDocument(xmlValue, debug);
        if (document == null) { 
            AppConfigurationEntry[] entries = new AppConfigurationEntry[0];
            return entries;
        }

        // get document elements of the documents
        Element valuePair = document.getDocumentElement();

        // retrieve child elements (<Value>) of the root (<AttributeValuePair>)
        // each element corresponding to one AppConfigurationEntry 
        NodeList children = valuePair.getChildNodes();
        final int number = children.getLength();

        // new AppConfigurationEntry[] according to children number
        AppConfigurationEntry[] entries = new AppConfigurationEntry[number]; 

        // process each child
        for (int i = 0; i < number; i++) {
            entries[i] = processValue(children.item(i));
        }
        return entries;
    }

    /**
     * Processes value of the Auth Configuration.
     * The value consists of thress part :
     * module_name flag options
     * there could only be one A/V pair in options, e.g. instance=/iplanet/ldap
     */
    private static AppConfigurationEntry processValue(Node node) { 
        if (debug.messageEnabled()) {
            debug.message("ConfigUtils.processValue, value=" + node.toString());
        }
        String value = node.getFirstChild().getNodeValue();
        if (value == null || value.length() == 0) {
            debug.error("ConfigUtils.processValue, invalid value=" + value);
            return null;
        } 

        // construct string tokenizer
        StringTokenizer st = new StringTokenizer(value);

        int len = st.countTokens();
        if (len < 2) {
            debug.error("ConfigUtils.processValue, wrong config : " + value);
            return null;
        }

        // set module & flag
        String moduleName = st.nextToken();
        String flag = st.nextToken();
        Map options = new HashMap();

        // check control flag
        AppConfigurationEntry.LoginModuleControlFlag cFlag = null;
        if (flag.equals("REQUIRED")) {
            cFlag = AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
        } else if (flag.equals("OPTIONAL")) {
            cFlag = AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL;
        } else if (flag.equals("REQUISITE")) {
            cFlag = AppConfigurationEntry.LoginModuleControlFlag.REQUISITE;
        } else if (flag.equals("SUFFICIENT")) {
            cFlag = AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT;
        } else {
            debug.error("ConfigUtils.processValue, invalid flag : " + value);
            return null;
        }

        // process options if any
        while (st.hasMoreElements()) {
            // process next options 
            String opt = st.nextToken();
            int k = opt.indexOf("=");
            if (k != -1) {
                HashSet set = new HashSet();
                //set.add("Empty");
                set.add(opt.substring(k + 1));
                options.put(opt.substring(0, k), set);
            } 
        } 

        return new AppConfigurationEntry(moduleName, cFlag, options);
    }

    /**
     * Returns the authentication configuration name given the 
     * <code>AuthContext.IndexType</code> and <code>indexName</code>. The
     * authentication configuration name will be used as the
     * <code>configName</code> for <code>getAppConfigurationEntry()</code>
     * function in <code>AMConfiguration</code>.
     *
     * @param indexType The <code>AuthContext.IndexType</code>, one of the
     *        following values:
     *        <code>AuthContext.IndexType.MODULE_INSTANCE</code>,
     *        <code>AuthContext.IndexType.SERVICE</code>,
     *        <code>AuthContext.IndexType.ROLE</code> or
     *        <code>AuthContext.IndexType.USER </code>.
     * @param indexName The corresponding index value for the
     *        <code>IndexType</code>, for <code>ROLE</code> and
     *        <code>USER</code>, DNs must be passed in. 
     * @param organizationDN DN for the login organization. 
     * @param clientType Client type, example <code>genericHTML</code>. 
     * @throws AMConfigurationException if <code>indexType</code> is not
     *         supported.
     * @return Corresponding authentication configuration name.
     */
    public static String getAuthConfigName(
        AuthContext.IndexType indexType,
        String indexName,
        String organizationDN,
        String clientType
    ) throws AMConfigurationException {
        if (indexType == AuthContext.IndexType.MODULE_INSTANCE) {
            return MODULE_KEY + "=" + indexName + ";" + ORG_KEY + "=" + 
                organizationDN.toLowerCase()+";"+CLIENT_KEY + "=" + clientType;
        } else if (indexType == AuthContext.IndexType.ROLE) { 
            return ROLE_KEY + "=" + indexName + ";" + ORG_KEY + "=" + 
                organizationDN .toLowerCase()+";"+CLIENT_KEY + "=" + clientType;
        } else if (indexType == AuthContext.IndexType.SERVICE) { 
            return SERVICE_KEY + "=" + indexName + ";" + ORG_KEY + "=" + 
                organizationDN .toLowerCase()+";"+CLIENT_KEY + "=" + clientType;
        } else if (indexType == AuthContext.IndexType.USER) { 
            return USER_KEY + "=" + indexName + ";" + ORG_KEY + "=" + 
                organizationDN.toLowerCase()+";"+CLIENT_KEY + "=" + clientType;
        }

        // Invalid IndexType, throw exception
        throw new AMConfigurationException(bundleName, "invalidIndexType");
    }

    /**
     * Returns the authentication configuration name for the organization based
     * authentication. The authentication configuration name will be used as
     * the <code>configName</code> for <code>getAppConfigurationEntry()</code>.
     * function in <code>AMConfiguration</code>.
     *
     * @param organizationDN  DN for the login organization. 
     * @param clientType
     * @return Corresponding authentication configuration name. 
     */
    public static String getAuthConfigName(
        String organizationDN, 
        String clientType) {
        return ORG_KEY + "=" + organizationDN + ";" + CLIENT_KEY + "=" + 
            clientType;
    }

    /**
     * Converts a List of authentication configuration to XML string
     * representation according to following DTD.
     * <pre>
     * &lt;!-- AttributeValuePair defines the values used to specify 
     *     authentication configuration information. --&gt;
     * &lt;!ELEMENT AttributeValuePair (Value*) &gt;
     *  
     * &lt;!-- Value defines one authentication configuration  --&gt;
     *     &lt;!ELEMENT Value (#PCDATA) &gt;
     * </pre> 
     *
     * @param configs List of configurations to be processed, each value
     *        consists of following parts separated by blank space:
     *        <code>module_name</code> flag <code>option1</code>,
     *        <code>option2</code>.
     * @return XML representation of the configuration .
     */
    public static String convertToXMLString(List configs) {  
        if (debug.messageEnabled()) {
            debug.message("convertToXMLString : " + configs.toString());
        }
        StringBuilder sb = new StringBuilder(100);
        Iterator it = configs.iterator();
        if (it != null) {
            sb.append('<').append(ATTR_VALUE_PAIR_NODE).append('>');
            while (it.hasNext()) {
                sb.append('<').append(ATTR_VALUE_NODE).append('>').append(it.next()).
                        append("</").append(ATTR_VALUE_NODE).append('>');
            }
            sb.append("</").append(ATTR_VALUE_PAIR_NODE).append('>');
        }
        if (debug.messageEnabled()) {
            debug.message("convertToXMLString : return " + sb.toString());
        }
        return sb.toString();
    }

    /**
     * Creates an authentication configuration in
     * <code>iPlanetAMAuthConfiguration</code> service. This method will be
     * used by console to manage configurations for different services.
     *
     * @param configName Name of the authentication configuration.
     * @param priority Priority of this authentication configuration.
     * @param attributeDataMap Map of authentication service attributes.
     * @param orgName Organization DN.
     * @param token Single sign on token.
     * @throws SMSException if failed to store the configuration because 
     *         of SM Exception.
     * @throws SSOException if single sign on token is not valid.
     * @throws AMConfigurationException if the <code>configName</code> is null.
     */
    public static void createNamedConfig(
        String configName,
        int priority,
        Map attributeDataMap,
        String orgName,
        SSOToken token
    ) throws SMSException, SSOException, AMConfigurationException {
        if (debug.messageEnabled()) {
            debug.message("createNamedConfig name=" + configName + ", value=" +
                attributeDataMap);
        }
        // Check if name is valid
        if (configName == null || configName.length() == 0) {
            throw new AMConfigurationException(bundleName, "null-name");
        }
        ServiceConfigManager scm = new ServiceConfigManager(token, 
                SERVICE_NAME, SERVICE_VERSION);
            
        ServiceConfig orgConfig = scm.getOrganizationConfig(orgName, null); 
        if (orgConfig == null) {
            orgConfig = scm.createOrganizationConfig(orgName, null);
        }
        ServiceConfig authConfig = orgConfig.getSubConfig(NAMED_CONFIGURATION);
        if (authConfig == null) {
            orgConfig.addSubConfig(NAMED_CONFIGURATION, null, 0, null);
            authConfig = orgConfig.getSubConfig(NAMED_CONFIGURATION);
        }
       
        debug.message("Got auth config"); 
        /*Map map = new HashMap();
        Set set = new HashSet();
        // construct the xml for value, and add it as value for the map 
        set.add(convertToXMLString(configs));
        map.put(ATTR_NAME, set);  */
        
        // add sub config
        authConfig.addSubConfig(configName, NAMED_CONFIGURATION_ID, 
            priority, attributeDataMap);
    }

    /**
     * Replaces an existing authentication configuration defined in 
     * <code>iPlanetAMAuthConfiguration</code> service. This method will be
     * used by console to manage configurations for different services.
     *
     * @param configName Name of the authentication configuration.
     * @param priority Priority of the configuration.
     * @param attributeDataMap Map of authentication service attributes.
     * @param orgName Organization DN.
     * @param token Single sign on token.
     * @throws SMSException if failed to set the configuration because
     *         of SM Exception.
     * @throws SSOException if single sign on token is not valid.
     * @throws AMConfigurationException if <code>configName</code> is null or
     *         not defined.
     */
    public static void replaceNamedConfig(
        String configName,
        int priority,
        Map attributeDataMap,
        String orgName,
        SSOToken token
    ) throws SMSException, SSOException, AMConfigurationException {

        if (debug.messageEnabled()) {
            debug.message("replaceNamedConfig name=" + configName + ", value=" +
                attributeDataMap + ",org=" + orgName);
        }
        // Check if name is valid
        if (configName == null) {
            throw new AMConfigurationException(bundleName, "null-name");
        }
        // Get the named config node
        ServiceConfigManager scm = new ServiceConfigManager(token,
            SERVICE_NAME, SERVICE_VERSION);
        ServiceConfig oConfig = scm.getOrganizationConfig(orgName, null);
        if (oConfig == null) {
            // service not registered
            throw new AMConfigurationException(
                bundleName, "service-not-registered");
        }
        ServiceConfig namedConfig = oConfig.getSubConfig(NAMED_CONFIGURATION);
        if (namedConfig == null) {
            // named configuration not exists 
            throw new AMConfigurationException(
                bundleName, "named-config-not-defined");
        }
        ServiceConfig pConfig = namedConfig.getSubConfig(configName);
        if (pConfig == null) {
            // configuration does not exist
            throw new AMConfigurationException(bundleName, "config-not-exists");
        }

        // Construct the named config 
        /*String configXml = convertToXMLString(configs);
        Map attrs = new HashMap();
        Set set = new HashSet();
        set.add(configXml);
        attrs.put(ATTR_NAME, set);*/

        // do the replacement in named config 
        pConfig.setAttributes(attributeDataMap);
        // return the xml string
        //return configXml;
    }

    /**
     * Removes an authentication configuration defined in 
     * <code>iPlanetAMAuthConfiguration</code> service. This method will be
     * used by console to manage configurations for different services.
     *
     * @param configName Name of the authentication configuration.
     * @param orgName Organization DN.
     * @param token Single Sign On token.
     * @throws SMSException if failed to delete the configuration because
     *         of SM Exception.
     * @throws SSOException if single sign on token is not valid.
     * @throws AMConfigurationException if <code>configName</code> is null
     *         or not defined .
     */
    public static void removeNamedConfig(
        String configName,
        String orgName,
        SSOToken token
    ) throws SMSException, SSOException, AMConfigurationException {

        if (debug.messageEnabled()) {
            debug.message("removeNamedConfig name=" + configName + ",org=" +
                orgName);
        }
        // Check if name is valid
        if (configName == null) {
            throw new AMConfigurationException(bundleName, "null-name");
        }

        // Get service config for named config node
        ServiceConfigManager scm = new ServiceConfigManager(
            SERVICE_NAME, token);
        ServiceConfig oConfig = scm.getOrganizationConfig(orgName, null);
        if (oConfig == null) {
            // service not registered
            throw new AMConfigurationException(
                bundleName, "service-not-registered");
        }
        ServiceConfig namedConfig = oConfig.getSubConfig(NAMED_CONFIGURATION);
        if (namedConfig == null) {
            // named configuration not exists
            throw new AMConfigurationException(
                bundleName, "named-config-not-defined");
        }

        // get the  config 
        ServiceConfig pConfig = namedConfig.getSubConfig(configName);
        if (pConfig == null) {
            // configuration does not exist
            throw new AMConfigurationException(bundleName, "config-not-exists");
        }

        // do the removal of config 
        namedConfig.removeSubConfig(configName);
    }

    /**
     * Returns all the authentication configurations defined in
     * <code>iPlanetAMAuthConfiguration</code> service. This method will be
     * used by console to manage configurations for different services.
     *
     * @param orgName Organization DN.
     * @param token Single Sign On token.
     * @return Set which contains all the configuration names 
     * @throws SMSException if failed to get configurations because
     *         of SM Exception.
     * @throws SSOException if single sign on token is not valid.
     */
    public static Set getAllNamedConfig(String orgName, SSOToken token)
            throws SMSException, SSOException {
        if ((orgName != null) && (orgName.length() != 0)) {
            orgName = orgName.toLowerCase();
        }
        if (debug.messageEnabled()) {
            debug.message("getAllNamedConfig org=" + orgName);
        }

        // Get the named config node
        ServiceConfigManager scm = new ServiceConfigManager(token,
            SERVICE_NAME, SERVICE_VERSION);
        ServiceConfig oConfig = scm.getOrganizationConfig(orgName, null);
        if (oConfig == null) {
            // service not registered
            return Collections.EMPTY_SET;
        }
        ServiceConfig namedConfig = oConfig.getSubConfig(NAMED_CONFIGURATION);
        if (namedConfig == null) {
            // named configuration not exists 
            return Collections.EMPTY_SET;
        }

        // get all sub config names
        return namedConfig.getSubConfigNames("*");
    }

    /**
     * Returns the authentication configuration defined in 
     * <code>iPlanetAMAuthConfiguration</code> service as XML string.
     * This method will be used by console to manage configurations for
     * different services.
     * <p>
     * Here is a sample XML string for an authentication configuration
     * <pre>
     * &lt;AttributeValuePair> <br>
     * &lt;Value>com.sun.identity.authentication.modules.LDAP required 
     *    debug=true&lt;/Value><br>
     * &lt;Value>com.sun.identity.authentication.modules.RADIUS
     *    optional&lt;/Value> 
     * &lt;/AttributeValuePair>
     * </pre>
     * This means user need to pass a required LDAP Login module, then an 
     * optional RADIUS Login module. 
     *
     * @param configName Name of the authentication configuration.
     * @param orgName Organization DN.
     * @param token Single Sign On token.
     * @return Map containing authentication service attributes.
     * @throws SMSException if failed to get the configuration because
     *         of SM Exception.
     * @throws SSOException if single sign on token is not valid.
     * @throws AMConfigurationException if <code>configName</code> is null or
     *         not defined.
     */
    public static Map getNamedConfig(
        String configName,
        String orgName,
        SSOToken token
    ) throws SMSException, SSOException, AMConfigurationException {
        if (debug.messageEnabled()) {
            debug.message(
                "getNamedConfig name=" + configName + ",org=" + orgName);
        }
        // Check if name is valid
        if (configName == null) {
            throw new AMConfigurationException(bundleName, "null-name");
        }

        // get configuration using SM API
        ServiceConfigManager scm = new ServiceConfigManager(token,
            SERVICE_NAME, SERVICE_VERSION);
        // retrieve subconfig 
        ServiceConfig orgConfig = scm.getOrganizationConfig(orgName, null);
        if (orgConfig == null) {
            // service not registered
            throw new AMConfigurationException(
                bundleName, "service-not-registered");
        }

        ServiceConfig authConfig = orgConfig.getSubConfig(NAMED_CONFIGURATION);
        if (authConfig == null) {
            // named configuration not exists
            throw new AMConfigurationException(
                bundleName, "named-config-not-defined");
        }
        ServiceConfig conf = authConfig.getSubConfig(configName);
        if (conf == null) {
            // configuration does not exist
            throw new AMConfigurationException(bundleName, "config-not-exists");
        }

        // retrieve attribute
        Map attributeDataMap = conf.getAttributes();
        /*Set value = (Set) map.get(ATTR_NAME);
        if (value == null || value.isEmpty()) {
            return null;
        } else {
            return (String) value.iterator().next();
        }*/
        return attributeDataMap;
    }

    /**
     * Returns module name from complete class name.
     *
     * @param className Class name, example
     *        <code>com.sun.identity.authentication.modules.ldap.LDAP</code>.
     * @return module name, e.g. "LDAP"
     */
    public static String getModuleName(String className) {
        int dot = className.lastIndexOf(".");
        if (dot == -1) {
            return className;
        } else if (dot == (className.length() - 1)) {
            // dot is the last character in className
            return "";
        } else {
            return className.substring(dot + 1);
        }
    }

    /**
     * Returns SM service name based on module name
     *
     * @param module Login module name, e.g. "LDAP"
     * @return Service name for the login module, example
     *         <code>iPlanetAMAuthLDAPService</code>
     */
    public static String getModuleServiceName(String module) {
        if ( module.equals("RADIUS")) {
            return "iPlanetAMAuthRadiusService";
        }
        return "iPlanetAMAuth" + module + "Service";
    }

    public static String getNewModuleServiceName(String module) {
        return ISAuthConstants.AUTH_ATTR_PREFIX_NEW + module + "Service";
    }

    /**
     * Returns authentication level attribute name for module name.
     *
     * @param attrs parameter map of the module service.
     * @param module Login module name, e.g. "LDAP".
     * @return attribute name for authentication level
     *         example <code>iplanet-am-auth-ldap-auth-level</code> or 
     *         <code>sunIdentityServerLDAPAuthLevel</code>.
     */
    public static String getAuthLevelAttribute(Map attrs, String module) {
        // auth level attribute must follow this naming convention
        String attrName =  ISAuthConstants.AUTH_ATTR_PREFIX + 
            module.toLowerCase() + "-auth-level";
        if (attrs.get(attrName) == null) {
            attrName = ISAuthConstants.AUTH_ATTR_PREFIX_NEW + module + 
            "AuthLevel";
        }
        return attrName;
    }

    /**
     * Returns service schema object for the authentication configuration
     * subschema.
     *
     * @param token Single Sign On token.
     * @return Service Schema.
     * @throws AMConfigurationException if there are errors accessing
     *         authentication configuration.
     */
    public static ServiceSchema getServiceSchema(SSOToken token) 
            throws AMConfigurationException {
        try {
            ServiceSchemaManager scm = 
                new ServiceSchemaManager(SERVICE_NAME, token); 
            ServiceSchema orgSchema = scm.getOrganizationSchema();
            ServiceSchema schema = 
                orgSchema.getSubSchema(NAMED_CONFIGURATION);
            ServiceSchema configSchema = 
                schema.getSubSchema(NAMED_CONFIGURATION_ID);
            return configSchema;
        } catch (Exception e) {
            debug.error("getServiceSubSchema", e);
            throw new AMConfigurationException(e);
        } 
    }

    /**
     * Returns all supported authentication modules
     *
     * @param token Single Sign On token to be using for accessing configuration
     *        information.
     * @return Map contains all modules, key is the module name (e.g. LDAP),
     *         value is the complete class name (example
     *         <code>com.sun.identity.authentication.modules.ldap.LDAP</code>)
     */
    public static Map getAllAuthModules(SSOToken token) {
        Map modules = new HashMap();
        // get auth global attribute
        // if this is too slow, might need to consider listener option
        try {
            ServiceSchemaManager scm = new ServiceSchemaManager(
                "iPlanetAMAuthService", token);
            ServiceSchema global = scm.getGlobalSchema();
            Map attrs = global.getAttributeDefaults();
            Set classes = (Set) attrs.get("iplanet-am-auth-authenticators");
            if (classes == null) {
                return modules;
            }
            Iterator iter = classes.iterator();
            while(iter.hasNext()) {
                String name = (String)iter.next();
                // skip Application module here since it is internal
                if (name.equals(
               "com.sun.identity.authentication.modules.application.Application"
                )) {
                    continue;
                }
                if (debug.messageEnabled()) {
                    debug.message("getAllAuthModules. process " + name);
                }
                int dot = name.lastIndexOf('.');
                if (dot > -1) {
                    String tmp = name.substring(dot + 1, name.length());
                    modules.put(tmp, name);
                } else {
                    modules.put(name, name);
                }
            }
        } catch (Exception e) {
            // ignore exception
            debug.error("getAllAuthModules", e);
        }
        return modules;
    }

    /**
     * Returns all supported authentication modules in an Organization
     * If there are not modules configured at the Organization level
     * then the authentication modules set at Global level will be returned.
     *
     * @param orgDN organization DN.
     * @param token single sign on token. 
     * @return Map contains all modules, key is the module name (e.g. LDAP),
     *         value is the complete class name (e.g.
     *         <code>com.sun.identity.authentication.modules.ldap.LDAP</code>)
     */
    public static Map getAllAuthModules(String orgDN,SSOToken token) {
        Map modules = new HashMap();
        // get auth global attribute
        Set authenticators=null;
        try {
            AMStoreConnection dpStore = new AMStoreConnection(token);
            AMOrganization org =
                        (AMOrganization) dpStore.getOrganization(orgDN);
            AMTemplate template = org.getTemplate(AUTH_SERVICE,
                        AMTemplate.ORGANIZATION_TEMPLATE);
            Map attrs = template.getAttributes();
            authenticators = (Set)attrs.get(AUTH_MODULES_ATTR);
        } catch (Exception e) {
            debug.error("getAllAuthModules", e);
        }
        Set globalAuth = getGlobalAuthenticators(token);

        if ((authenticators != null) && (!authenticators.isEmpty())) {
            modules = constructModulesList(authenticators, globalAuth);
        } else {
            modules = constructModulesList(globalAuth, null);
        }
    
        if (debug.messageEnabled()) {
            debug.message("Returning modules : " + modules);
        }

        return modules;

    }

    /**
     * Parses the string value for the authentication configuration 
     * based on the attribute DTD and return a List of
     * <code>AuthConfigurationEntry</code> which could be used to retrieve
     * module name, flag and options. Empty List will be returned if the XML
     * value could  not be parsed.
     *
     * @param xmlValue XML value for the authentication configuration.
     * @return List of <code>AuthConfigurationEntry</code> contains module
     *         name, flag and options.
     */
    public static List xmlToAuthConfigurationEntry(String xmlValue) {
        if (debug.messageEnabled()) {
            debug.message("AuthConfUtil.xmltoentries, xml=" +xmlValue);
        }
        List entries = new ArrayList();
        // call util method to parse the document
        Document document = XMLUtils.toDOMDocument(xmlValue, debug);
        if (document == null) { 
            return entries;
        }

        // get document elements of the documents
        Element valuePair = document.getDocumentElement();
        // retrieve child elements (<Value>) of the root (<AttributeValuePair>)
        // each element corresponding to one AuthConfigurationEntry 
        NodeList children = valuePair.getChildNodes();
        final int number = children.getLength();
        // process each child
        for (int i = 0; i < number; i++) {
            try {
                entries.add(new AuthConfigurationEntry(children.item(i)));
            } catch (Exception e) {
                debug.error("parseValue", e);
                // continue next item
            }
        }
        return entries;
    }

    /**
     * Converts a List of <code>AuthConfigurationEntry</code> to XML 
     * representation according to following DTD.
     * <pre>
     * &lt;!-- AttributeValuePair defines the values used to specify 
     *     authentication configuration information. --&gt;
     * &lt;!ELEMENT AttributeValuePair (Value*) &gt;
     *
     * &lt;!-- Value defines one authentication configuration  --&gt;
     * &lt;!ELEMENT Value (#PCDATA) &gt;
     * </pre> 
     *
     * @param entries List of <code>AuthConfigurationEntry</code> to be
     *        processed.
     * @return XML representation of the configuration.
     */
    public static String authConfigurationEntryToXMLString(List entries) {  
        if (debug.messageEnabled()) {
            debug.message("convertToXMLString : " + entries);
        }
        if (entries != null) {
            StringBuilder sb = new StringBuilder(100);
            sb.append('<').append(ATTR_VALUE_PAIR_NODE).append('>');
            int len = entries.size();
            for (int i = 0; i < len; i++) {
                AuthConfigurationEntry entry = 
                    (AuthConfigurationEntry)entries.get(i);
                sb.append('<').append(ATTR_VALUE_NODE).append('>')
                  .append(entry.getLoginModuleName()).append(' ')
                  .append(entry.getControlFlag().toString()).append(' ');
                String options = entry.getOptions();
                if (options != null) {
                  sb.append(options.toString());
                }
                sb.append("</").append(ATTR_VALUE_NODE).append('>');
            }
            sb.append("</").append(ATTR_VALUE_PAIR_NODE).append('>');
            if (debug.messageEnabled()) {
                debug.message("convertToXMLString : return " + sb.toString());
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
     * Creates a map where key is the module name and value is the fully
     * qualified class name of the module.
     *
     * @param classes Set of class name.
     * @param globalAuth
     */
    private static Map constructModulesList(Set classes, Set globalAuth) {
        if (debug.messageEnabled()) {
            debug.message("constructModulesList : classes : " + classes);
        }
        Iterator iter = classes.iterator();
        HashMap modules = new HashMap();
        while(iter.hasNext()) {
            String name = (String)iter.next();
            // skip Application module here since it is internal
            if (name.equals(APPLICATION_CLASS_NAME)) {
                continue;
            }
            if (debug.messageEnabled()) {
                debug.message("getAllAuthModules. process " + name);
            }
            int dot = name.lastIndexOf('.');
            if (dot > -1) {
                String tmp = name.substring(dot + 1, name.length());
                modules.put(tmp, name);
            } else {
                if ((globalAuth != null) &&  (!globalAuth.isEmpty())) {
                    String className = 
                            getAuthenticatorClassName(name,globalAuth);
                    if (debug.messageEnabled()) {
                        debug.message("className : " + className);
                    }
                    modules.put(name, className);
                } else {
                    modules.put(name,name);
                }
            }
        }

        return modules;
    }


    /**
     * Returns the fully qualified class name of the Module. Returns the module
     * Name if class name is not found.
     *
     * @param moduleName Name of authentication module.
     * @param globalAuth
     */
    private static String getAuthenticatorClassName(
        String moduleName,
        Set globalAuth) {
        Map attrs ;
        String fullClassName =null;

        if (globalAuth == null) {
            return moduleName;
        }

        Iterator iter = globalAuth.iterator();
        while(iter.hasNext()) {
            fullClassName = null;
            String name = (String)iter.next();
            // skip Application module here since it is internal
            if (name.equals(APPLICATION_CLASS_NAME)) {
                continue;
            }
            int dot = name.lastIndexOf('.');
            if (dot > -1) {
                String tmp = name.substring(dot+1,name.length());
                if (tmp.equals(moduleName)) {
                    fullClassName = name;
                } 
            }
            if (fullClassName != null) {
                break;
            }                                
        }

        if (debug.messageEnabled()) {
            debug.message("fullClassName is : " + fullClassName);
        }
        if (fullClassName != null) {
            return fullClassName;
        } else {
            return moduleName;
        }
    }

    /**
     * Returns the global authenticators.
     *
     * @param token Single sign on token to access configuration information.
     * @return the global Authenticators.
     */
    public static Set getGlobalAuthenticators(SSOToken token) {
        Set globalAuth=null;

        try {
            ServiceSchemaManager scm = new ServiceSchemaManager(
                AUTH_SERVICE,token);
            ServiceSchema global = scm.getGlobalSchema();
            Map attrs = global.getAttributeDefaults();
            globalAuth = (Set)attrs.get(AUTH_AUTHENTICATOR_ATTR);
        } catch (Exception e) {
            debug.error("getAllAuthModules",e);
        }

        return globalAuth;
    }
}
