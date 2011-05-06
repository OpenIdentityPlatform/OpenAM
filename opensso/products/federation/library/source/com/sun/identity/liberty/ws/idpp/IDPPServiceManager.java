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
 * $Id: IDPPServiceManager.java,v 1.2 2008/06/25 05:47:14 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.idpp;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.liberty.ws.idpp.common.*;
import com.sun.identity.liberty.ws.idpp.plugin.*;
import com.sun.identity.liberty.ws.interfaces.Authorizer;
import com.sun.identity.liberty.ws.interfaces.ServiceInstanceUpdate;
import com.sun.identity.liberty.ws.interfaces.ResourceIDMapper;
import com.sun.identity.plugin.configuration.ConfigurationActionEvent;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.plugin.configuration.ConfigurationManager;

/**
 * The class <code>IDPPServiceManager</code> is a manager class for managing
 * IDPP service configuration.
 */

public class IDPPServiceManager implements ConfigurationListener {

    private static IDPPServiceManager instance = null;

    //Authorizer
    private static Authorizer authorizer = null;
    private static final String authorizerKey = "sunIdentityServerPPAuthorizer";
    private static final String defaultAuthorizer = 
            "com.sun.identity.liberty.ws.idpp.plugin.IDPPAuthorizer";

    //ResourceID Mapper.
    private static ResourceIDMapper resourceIDMapper = null;
    private static final String resourceMapperKey = 
            "sunIdentityServerPPResourceIDMapper";
    private static final String defaultResourceIDMapper = 
            "com.sun.identity.liberty.ws.idpp.plugin.IDPPResourceIDMapper";

    //Attribute Mapper.
    private static AttributeMapper attributeMapper = null;
    private static final String attributeMapperKey = 
            "sunIdentityServerPPAttributeMapper";
    private static String defaultAttributeMapper =
            "com.sun.identity.liberty.ws.idpp.plugin.IDPPAttributeMapper";
     
    private static String nameScheme = null;
    private static final String nameSchemeKey = "sunIdentityServerPPNameScheme";
    private static final String defaultNameScheme = 
            "urn:liberty:idpp:nameScheme:firstlast";

    private static Set supportedContainers = new HashSet();
    private static Map containerExtensions = new HashMap(); 
    private static Map containerClasses = new HashMap(); 
    private static final String supportedContainersKey = 
            "sunIdentityServerPPSupportedContainers";

    private static String idppPrefix = null;
    private static final String idppPrefixKey = 
            "sunIdentityServerPPNameSpacePrefix";
    private static final String defaultPrefix = "pp";
    private static ConfigurationInstance ci = null;

    private static Map ppDSMap = null;
    private static final String ppDSMapAttributeKey = 
            "sunIdentityServerPPDSAttributeMapList";

    private static final String queryPolicyEvalKey = 
            "sunIdentityServerPPisQueryPolicyEvalRequired";
    private static final String modifyPolicyEvalKey = 
            "sunIdentityServerPPisModifyPolicyEvalRequired";
    private static boolean isQueryPolicyEval = false;
    private static boolean isModifyPolicyEval = false;

    private static final String providerIDKey =
            "sunIdentityServerPPProviderID";
    private static String providerID = null;

    private static String extensionPrefix = null;
    private static final String extensionPrefixKey = 
            "sunIdentityServerPPExtensionPrefix";
    private static String defaultExtensionPrefix = "ispp";

    private static Set extensionAttributes = null;
    private static final String extensionAttributesKey = 
            "sunIdentityServerPPExtensionAttributes";

    private static ServiceInstanceUpdate serviceInstanceUpdate = null;
    private static String siuClass = 
            "sunIdentityServerPPServiceInstanceUpdateClass";
    private static String defaultSiuClass =
            "com.sun.identity.liberty.ws.idpp.plugin.IDPPServiceInstanceUpdate";
    private static String altEndPoint = null;
    private static String altEndPointKey = 
            "sunIdentityServerPPAlternateEndPoint";
    private static boolean isSIUEnabled = false;
    private static String isSIUKey = 
           "sunIdentityServerPPServiceInstanceUpdateEnabled";
    private static Set altSecMechs = null;
    private static String altSecMechsKey = 
            "sunIdentityServerPPAlternateSecurityMechs";

    static {
        try {
            ci = ConfigurationManager.getConfigurationInstance("IDPP");
            ci.addListener(new IDPPServiceManager());
            initializeService();
         } catch (Exception e) {
            IDPPUtils.debug.error("IDPPServiceManager:Static init failed", e);
         }
    }

    //Initialize the service.
    private static void initializeService() throws IDPPException {
        IDPPUtils.debug.message("IDPPServiceManager:initializeService");
        try {
            Map config = ci.getConfiguration(null, null);
            if(config == null || config.isEmpty()) {
               throw new IDPPException("Configuration is null");
            }
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("IDPPServiceManager:initializeService:" +
               " service config is " + config);
            }

            nameScheme = CollectionHelper.getMapAttr(
                config, nameSchemeKey, defaultNameScheme);
            Set containers = (Set)config.get(supportedContainersKey);
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("IDPPServiceManager:initializeService:" +
               " containers are " + containers);
            }
            parseContainers(containers);

            idppPrefix = CollectionHelper.getMapAttr(
                config,idppPrefixKey,defaultPrefix);
            extensionPrefix = CollectionHelper.getMapAttr(
                config, extensionPrefixKey, defaultExtensionPrefix); 
            extensionAttributes = (Set)config.get(extensionAttributesKey);
            String temp = CollectionHelper.getMapAttr(
                config, queryPolicyEvalKey);
            if (temp != null) {
                isQueryPolicyEval = Boolean.valueOf(temp).booleanValue();
            }
            temp = CollectionHelper.getMapAttr(config, modifyPolicyEvalKey);
            if(temp != null) {
               isModifyPolicyEval = Boolean.valueOf(temp).booleanValue();
            }

            providerID = CollectionHelper.getMapAttr(config, providerIDKey);
            storePPDSMap(config); 
            String param = CollectionHelper.getMapAttr(
                config, authorizerKey, defaultAuthorizer);
            Class authClass = Class.forName(param);
            authorizer = (Authorizer)authClass.newInstance();
             
            param = CollectionHelper.getMapAttr(
                config, resourceMapperKey, defaultResourceIDMapper);
            Class rMapper = Class.forName(param);
            resourceIDMapper = (ResourceIDMapper)rMapper.newInstance();
            
            param = CollectionHelper.getMapAttr(
                config, attributeMapperKey, defaultAttributeMapper);
            Class aMapper = Class.forName(param);
            attributeMapper = (AttributeMapper)aMapper.newInstance(); 

            param = CollectionHelper.getMapAttr(
                config, siuClass, defaultSiuClass);
            Class siUpdateClass = Class.forName(param);
            serviceInstanceUpdate =
                (ServiceInstanceUpdate)siUpdateClass.newInstance();

            altEndPoint = CollectionHelper.getMapAttr(config, altEndPointKey);
            altSecMechs = (Set)config.get(altSecMechsKey);

            temp = CollectionHelper.getMapAttr(config,  isSIUKey);
            if(temp != null) {
               isSIUEnabled = Boolean.valueOf(temp).booleanValue();
            }

        } catch (Exception e) {
            IDPPUtils.debug.error("IDPPServiceManager:initializeService:"+
            "Error while initializing services.", e);
            throw new IDPPException(e);
        }
    }

    /**
     * Store the PP LDAP Attribute Map
     */
    private static void storePPDSMap(Map config) {
        IDPPUtils.debug.message("IDPPServiceManager:storePPDSMap:Init");
        ppDSMap = new HashMap(); 
        Set set = (Set)config.get(ppDSMapAttributeKey);
        if(set == null || set.isEmpty()) {
           IDPPUtils.debug.message("IDPPServiceManager:attribute map is empty");
           return;
        }
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPServiceManager:storePPDSMap: set of"+
           "attributes in DS:" + set);
        }
        Iterator iter = set.iterator();
        while(iter.hasNext()) {
           String attr = (String)iter.next();
           if(attr.indexOf("=") == -1) {
              if(IDPPUtils.debug.messageEnabled()) {
                 IDPPUtils.debug.message("IDPPServiceManager:storePPDSMap:" +
                 "Entry does not have = sign. Ignoring:" + attr);
              }
              continue;
           }
           StringTokenizer st = new StringTokenizer(attr, "=");
           if(st.countTokens() > 2) {
              if(IDPPUtils.debug.messageEnabled()) {
                 IDPPUtils.debug.message("IDPPServiceManager:storePPDSMap:" +
                 "Entry is invalid . Ignoring:" + attr);
              }
              continue;
           }
           ppDSMap.put((String)st.nextToken(), (String)st.nextToken());
        }
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPServiceManager:storePPDSMap: mapped"+
           "attributes:" + ppDSMap);
        }
    }

    /**
     * Parses the container attribute.
     */
    private static void parseContainers(Set containers) {
        if(containers == null || containers.size() == 0) {
           IDPPUtils.debug.error("IDPPServiceManager.parseContainers:" +
           "Container set is empty");
           return;
        }
        Iterator iter = containers.iterator();
        while(iter.hasNext()) {
            String entry = (String)iter.next();

            String containerToken = null;
            String extensionToken = null;
            String pluginToken = null;
            StringTokenizer tokenizer = null;
            String container = null;

            if(entry.indexOf(IDPPConstants.ATTRIBUTE_SEPARATOR) != -1) {
               tokenizer = new StringTokenizer(
                      entry, IDPPConstants.ATTRIBUTE_SEPARATOR);
               while(tokenizer.hasMoreTokens()) {
                   String temp = tokenizer.nextToken();
                   if(temp.startsWith(IDPPConstants.CONTAINER)) {
                      containerToken = temp; 
                   } else if(temp.startsWith(IDPPConstants.EXTENSION)) {
                      extensionToken = temp;
                   } else if(temp.startsWith(IDPPConstants.PLUGIN)) {
                      pluginToken = temp;
                   }
               }
            } else {
               containerToken = entry;
            }

            if((containerToken.indexOf("=") == -1) || 
               !containerToken.startsWith(IDPPConstants.CONTAINER)) {
                if(IDPPUtils.debug.messageEnabled()) {
                   IDPPUtils.debug.message("IDPPServiceManager.parse" +
                   "Containers: Invalid entry." + entry);
                }
                continue;
             } else {
                 tokenizer = new StringTokenizer(containerToken, "=");
                 if(tokenizer.countTokens() != 2) {
                    if(IDPPUtils.debug.messageEnabled()) {
                       IDPPUtils.debug.message("IDPPServiceManager.parse" +
                       "Containers: Invalid entry." + entry);
                    }
                    continue;
                 }
                 tokenizer.nextToken();
                 container = (String)tokenizer.nextToken();
                 if(container == null || container.length() == 0) {
                    if(IDPPUtils.debug.messageEnabled()) {
                       IDPPUtils.debug.message("IDPPServiceManager.parse" +
                       "Containers: Invalid entry." + entry);
                    }
                 }
                 supportedContainers.add(container); 
            }

            if(extensionToken != null && extensionToken.length() != 0 && 
                (extensionToken.indexOf("=") != -1)) {
                tokenizer = new StringTokenizer(extensionToken, "=");
                tokenizer.nextToken();
                try {
                    String ext = (String)tokenizer.nextToken();
                    Class extClass = Class.forName(ext);
                    IDPPExtension containerExtension = 
                              (IDPPExtension)extClass.newInstance();
                    containerExtensions.put(container, containerExtension);
                } catch (Exception ex) {
                    IDPPUtils.debug.error("IDPPServiceManager.parseContainers"+
                    ":Error instantiating extension class:" , ex);
                }
            }

            if(pluginToken != null && pluginToken.length() != 0 &&
                 (pluginToken.indexOf("=") != -1)) { 
                tokenizer = new StringTokenizer(pluginToken, "=");
                tokenizer.nextToken();
                try {
                    String plugin = (String)tokenizer.nextToken();
                    Class pluginClass = Class.forName(plugin);
                    IDPPContainer containerClass = 
                              (IDPPContainer)pluginClass.newInstance();
                    containerClasses.put(container, containerClass);
                } catch (Exception ex) {
                    IDPPUtils.debug.error("IDPPServiceManager.parseContainers"+
                    ":Error instantiating extension class:" , ex);
                }
            }
        }

        if(IDPPUtils.debug.messageEnabled()) {

           IDPPUtils.debug.message("IDPPServiceManager.parseContainers:" +
               "supported containers:" + supportedContainers);
           IDPPUtils.debug.message("IDPPServiceManager.parseContainers:" +
               "container extensions:" + containerExtensions);
           IDPPUtils.debug.message("IDPPServiceManager.parseContainers:" +
               "container classes:" + containerClasses);
        }
    }

    // Default constructor
    private IDPPServiceManager() {}

    /**
     * Gets the instance of IDPPService Manager 
     *  
     * @return IDPPServiceManager instance of service manager.
     */
    public static IDPPServiceManager getInstance() {
        if (instance == null) {
            instance = new IDPPServiceManager();
        }
        return instance;
    }

    /**
     * Gets IDPPAuthorizer.
     *  
     * @return IDPPAuthorizer. 
     */
    public Authorizer getAuthorizer() {
        return authorizer;
    }

    /**
     * Gets IDPP ResourceID Mapper. 
     *  
     * @return IDPPResourceIDMapper
     */
    public ResourceIDMapper getResourceIDMapper() {
        return resourceIDMapper;
    }

    /**
     * Gets IDPP Attribute Mapper.
     *  
     * @return AttributeMapper.
     */
    public AttributeMapper getAttributeMapper() {
        return attributeMapper;
    }
 
    /**
     * Gets IDPP user name scheme.
     *  
     * @return String user name scheme.
     */
    public String getNameScheme() {
        return  nameScheme;
    }

    /**
     * Gets supported IDPP containers.
     *  
     * @return Set set of supported containers.
     */
    public Set getSupportedContainers() {
        return supportedContainers;
    }

    /**
     * Gets container extensions.
     * @return Map A map consists of container extension classes with the key
     *             as container name.
     */
    public Map getContainerExtensions() {
        return containerExtensions;
    }

    /**
     * Gets container classes.
     *
     * @return Map A map that contains container classes with the key
     *             name as container name.
     */
    public Map getContainerClasses() {
        return containerClasses;
    }

    /**
     * Gets idpp prefix.
     *  
     * @return String idpp prefix.
     */
    public String getIDPPPrefix() {
        return idppPrefix;
    }

    /**
     * Gets the PP LDAP Attribute Map
     * 
     * @return Map attribute map
     */
    public Map getPPDSMap() {
        return ppDSMap;
    }

    /**
     * Checks if query policy evaluation is required
     * @return true if the query evaluation is needed
     */
    public boolean isQueryPolicyEvalRequired() {
        return isQueryPolicyEval;
    }

    /**
     * Checks if modify policy evaluation is required
     * @return true if the modify evaluation is needed
     */
    public boolean isModifyPolicyEvalRequired() {
        return isModifyPolicyEval;
    }

    /**
     * Gets the provider id
     * @return String IDPP service provider id
     */
    public String getProviderID() {
       return providerID;
    }

    /**
     * Gets the container extension class for a given container.
     * @return IDPPExtension IDPPExtension implementation class for a specific
     *                       container.
     */
    public IDPPExtension getContainerExtension(String container) {
        return (IDPPExtension)containerExtensions.get(container);
    }

    /**
     * Gets the personal profile attribute extension prefix.
     * @return String extended attribute prefix.
     */ 
    public String getPPExtensionPrefix() {
        return extensionPrefix;
    }

    /**
     * Gets extension container attributes. These are non personal profile
     * attribute set that are defined for the extension container.
     * @return Set set of extension container attributes.
     */ 
    public Set getExtensionAttributes() {
        return extensionAttributes;
    }

    /**
     * Gets the service instance update class
     */
    public ServiceInstanceUpdate getServiceInstanceUpdate() {
        return serviceInstanceUpdate;
    }

    /**
     * Checks if the service is configured to include service instance 
     * update header.
     */
    public boolean isServiceInstanceUpdateEnabled() {
        return isSIUEnabled;
    }

    /**
     * Gets the Alternate end point.
     */
    public String getAlternateEndPoint() {
        return altEndPoint;
    }

    /**
     * Gets Alternate Security Mechanisms.
     * @return Set Set of Alternate Security Mechanisms.
     */
    public Set getAlternateSecurityMechs() {
        return altSecMechs;
    }

    /**
     * This method will be invoked when a component's 
     * configuration data has been changed. The parameters componentName,
     * realm and configName denotes the component name,
     * organization and configuration instance name that are changed 
     * respectively.
     *
     * @param e Configuration action event, like ADDED, DELETED, MODIFIED etc.
     */
    public void configChanged(ConfigurationActionEvent e) {
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPServiceManager:configChanged:");
        }
        try {
            initializeService();
        } catch (IDPPException ie) {
            IDPPUtils.debug.error("IDPPServiceManager:configChanged: " +
            "Error in updating service configuration.", ie);
        }
    }
}
