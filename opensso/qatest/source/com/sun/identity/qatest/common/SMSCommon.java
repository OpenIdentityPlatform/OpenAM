/* The contents of this file are subject to the terms
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
 * $Id: SMSCommon.java,v 1.22 2009/06/02 17:09:26 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * This class has helper functions related to service management and
 * datastore configuration for user store.
 */
public class SMSCommon extends TestCommon {
    private SSOToken admintoken;
    private Map globalCfgMap;
    
    /**
     * Class constructor with SSOToken as input parameter.
     */
    public SMSCommon(SSOToken token)
    throws Exception{
        super("SMSCommon");
        admintoken = token;
    }

    /**
     * Class constructor with file name as input parameter.
     */    
    public SMSCommon(String globalCfgFile)
    throws Exception {
        super("SMSCommon");
         globalCfgMap = getMapFromResourceBundle(globalCfgFile);
    }
    
    /**
     * Class constructor with file name and SSOToken as input parameter.
     */
    public SMSCommon(SSOToken token, String globalCfgFile)
    throws Exception {
        super("SMSCommon");
        try {
            globalCfgMap = new HashMap();
            globalCfgMap = getMapFromResourceBundle(globalCfgFile);
            admintoken = token;
            if (!validateToken(admintoken)) {
                log(Level.SEVERE, "SMSCommon", "SSO token is invalid");
                assert false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Returns attribute value as a set for an attribute in a service using
     * ServiceSchema API. Service type can be one of the following: Global,
     * Organization, Dynamic, User or Policy.
     */
    public Set getAttributeValueFromSchema(String serviceName,
            String attributeName, String type)
            throws Exception {
        ServiceManager sm = new ServiceManager(admintoken);
        ServiceSchemaManager ssm = sm.getSchemaManager(serviceName, "1.0");
        ServiceSchema ss = null;
        if (type.equals("Global"))
            ss = ssm.getGlobalSchema();
        else if (type.equals("Organization"))
            ss = ssm.getOrganizationSchema();
        else if (type.equals("Dynamic"))
            ss = ssm.getDynamicSchema();
        else if (type.equals("User"))
            ss = ssm.getUserSchema();
        else if (type.equals("Policy"))
            ss = ssm.getPolicySchema();
        Map map = ss.getAttributeDefaults();
        if (map.containsKey(attributeName))
            return (((Set)map.get(attributeName)));
        else
            return (null);
    }
    
    /**
     * Returns all attributes and their values in a service
     * which have sub configurations. Service type can be one of the following:
     * Global or Organization.
     * dsRealm - a String containing the name of the realm from which the
     * service attribute value should be retrieved.
     * serviceName - a String containing the name of service from which the
     * attribute value should be retrieved.
     * type - a String set to "Global" or "Organization".
     * @return a Map containing all the atributes and their values
     */
    public Map getAttributes(String serviceName, String dsRealm,
            String type)
            throws Exception {
        ServiceConfigManager scm = new ServiceConfigManager(admintoken,
                serviceName, "1.0");
        ServiceConfig sc = null;
        if (type.equals("Global"))
            sc = scm.getGlobalConfig(null);
        else if (type.equals("Organization")) {
            if (dsRealm != null)
                sc = scm.getOrganizationConfig(dsRealm, null);
            else
                sc = scm.getOrganizationConfig(realm, null);
        }
        return (sc.getAttributes());
    }

    /**
     * Returns attribute value as a set for an attribute in a service
     * which have sub configurations. 
     * serviceRealm - a String containing the name of the realm from which the
     * service attribute value should be retrieved.
     * serviceName - a String containing the name of service from which the
     * attribute value should be retrieved.
     * attributeName - a String containing the name of the attribute
     * type - a String set to "Global" or "Organization".
     * @return a Set containing the value(s) of the attribute attributeName in
     * the service serviceName.
     */
    public Set getAttributeValue(String serviceRealm, String serviceName, 
            String attributeName, String type)
            throws Exception {
        ServiceConfigManager scm = new ServiceConfigManager(admintoken,
                serviceName, "1.0");
        ServiceConfig sc = null;
        if (type.equals("Global"))
            sc = scm.getGlobalConfig(null);
        else if (type.equals("Organization"))
            sc = scm.getOrganizationConfig(serviceRealm, null);
        Map map = sc.getAttributes();
        if (map.containsKey(attributeName))
            return (((Set)map.get(attributeName)));
        else
            return (null);
    }
    
    /**
     * Returns an attribute value for an attribute in a service.
     * serviceName - a String containing the name of service from which the
     * attribute value should be retrieved.
     * attributeName - a String containing the name of the attribute
     * type - a String set to "Global" or "Organization".
     * @return a Set containing the value(s) of the attribute attributeName in
     * the service serviceName.
     */
    public Set getAttributeValue(String serviceName, String attributeName, 
            String type)
    throws Exception {
        return getAttributeValue(realm, serviceName, attributeName, type);
    }

    /**
     * Method updates a given set of attributes in any sepcified service.
     * This is only valid for Global and Organization level attributes.
     * It does not update Dynamic attributes. This updates attribute
     * values for global and organization services which have sub
     * configurations.
     */
    public void updateSvcAttribute(String serviceRealm, String serviceName,
            Map map, String type)
            throws Exception {
        ServiceConfigManager scm = new ServiceConfigManager(serviceName,
                admintoken);
        ServiceConfig sc = null;
        if (type.equals("Global"))
            sc = scm.getGlobalConfig(null);
        else if (type.equals("Organization"))
            sc = scm.getOrganizationConfig(serviceRealm, null);
        sc.setAttributes(map);
    }
    
    /**
     * Method updates a given attribute in any sepcified service.
     * This is only valid for Global and Organization level attributes.
     * It does not update Dynamic attributes. This updates attribute
     * values for global and organization services which have sub
     * configurations.
     */
    public void updateSvcAttribute(String serviceRealm, String serviceName,
            String attributeName, Set set, String type)
            throws Exception {
        ServiceConfigManager scm = new ServiceConfigManager(serviceName,
                admintoken);
        ServiceConfig sc = null;
        if (type.equals("Global"))
            sc = scm.getGlobalConfig(null);
        else if (type.equals("Organization"))
            sc = scm.getOrganizationConfig(serviceRealm, null);
        Map map = new HashMap();
        map.put(attributeName, set);
        sc.removeAttribute(attributeName);
        sc.setAttributes(map);
    }
    
    /**
     * Method updates a given attribute in any sepcified service.
     * This is only valid for Global and Organization level attributes.
     * It does not update Dynamic attributes. This updates attribute
     * values for global and organization services which have sub
     * configurations.
     */
    public void updateSvcAttribute(String serviceName, String attributeName,
            Set set, String type)
    throws Exception {    
        updateSvcAttribute(realm, serviceName, attributeName, set, type);
    }
    
    /**
     * Method updates a given attribute in any sepcified service.
     * This is only valid for Global, Organization, Dynamic, User and Policy
     * attributes. This directly updates the schema entry for these services.
     * Returns true if the update is successfull else false.
     */
    public boolean updateSvcSchemaAttribute(String serviceName,
            Map attrMap, String type)
    throws Exception {
        log(Level.FINEST, "updateSvcSchemaAttribute", "Update " + type +
                " values " + " in service " + serviceName + " to " + attrMap);
        Set keySet = attrMap.keySet();
        Iterator attrItr = keySet.iterator();
        while (attrItr.hasNext()) {
            String key = (String)attrItr.next();
            updateSvcSchemaAttribute(serviceName, key, (Set)attrMap.get(key),
                    type);
        }
        ServiceManager sm = new ServiceManager(admintoken);
        ServiceSchemaManager ssm = sm.getSchemaManager(serviceName, "1.0");
        ServiceSchema ss = null;
        if (type.equals("Global"))
            ss = ssm.getGlobalSchema();
        else if (type.equals("Organization"))
            ss = ssm.getOrganizationSchema();
        else if (type.equals("Dynamic"))
            ss = ssm.getDynamicSchema();
        else if (type.equals("User"))
            ss = ssm.getUserSchema();
        else if (type.equals("Policy"))
            ss = ssm.getPolicySchema();
        Map updatedMap = ss.getAttributeDefaults();
        log(Level.FINEST, "updateSvcSchemaAttribute", "Updated values " + 
                updatedMap);
        return isAttrValuesEqual(attrMap, updatedMap);
    }
    
    /**
     * Method updates a given attribute in any sepcified service.
     * This is only valid for Global, Organization, Dynamic, User and Policy
     * attributes. This directly updates the schema entry for these services.
     */
    public void updateSvcSchemaAttribute(String serviceName,
            String attributeName, Set set, String type)
            throws Exception {
        ServiceManager sm = new ServiceManager(admintoken);
        ServiceSchemaManager ssm = sm.getSchemaManager(serviceName, "1.0");
        ServiceSchema ss = null;
        if (type.equals("Global"))
            ss = ssm.getGlobalSchema();
        else if (type.equals("Organization"))
            ss = ssm.getOrganizationSchema();
        else if (type.equals("Dynamic"))
            ss = ssm.getDynamicSchema();
        else if (type.equals("User"))
            ss = ssm.getUserSchema();
        else if (type.equals("Policy"))
            ss = ssm.getPolicySchema();
        ss.setAttributeDefaults(attributeName, set);
    }
    
    /**
     * Method removes values for a given attribute in any sepcified service
     */
    public void removeServiceAttributeValues(String serviceName,
            String attributeName, String type)
            throws Exception {
        ServiceConfigManager scm = new ServiceConfigManager(serviceName,
                admintoken);
        ServiceConfig sc = null;
        if (type.equals("Global"))
            sc = scm.getGlobalConfig(null);
        else if (type.equals("Organization"))
            sc = scm.getOrganizationConfig(realm, null);
        sc.removeAttribute(attributeName);
    }
    
    /**
     * Assign a service which has dynamic attributes like session or user from
     * a realm
     */
    public Map getDynamicServiceAttributeRealm(String serviceName, String realm)
    throws Exception {
        AMIdentityRepository idrepo = new AMIdentityRepository(admintoken,
                realm);
        AMIdentity realmIdentity = idrepo.getRealmIdentity();
        Set set = realmIdentity.getAssignedServices();
        if (set.contains(serviceName))
            return (realmIdentity.getServiceAttributes(serviceName));
        else
            return (null);
    }
    
    /**
     * Assign a service which has dynamic attributes like session or user from
     * a realm
     */
    public boolean assignDynamicServiceRealm(String serviceName, String realm,
            Map map)
            throws Exception {
        AMIdentityRepository idrepo = new AMIdentityRepository(admintoken,
                realm);
        AMIdentity realmIdentity = idrepo.getRealmIdentity();
        Set set = realmIdentity.getAssignedServices();
        if (!set.contains(serviceName))
            realmIdentity.assignService(serviceName, map);
        set = realmIdentity.getAssignedServices();
        return (set.contains(serviceName));
    }
    
    /**
     * Update service attributes of the realm
     */
    public void updateServiceAttrsRealm(String serviceName, String realm, 
            Map map)
    throws Exception {
        AMIdentityRepository idrepo = new AMIdentityRepository(
                admintoken, realm);
        AMIdentity realmIdentity = idrepo.getRealmIdentity();
        Set set = realmIdentity.getAssignedServices();
        if(set.contains(serviceName))
            realmIdentity.modifyService(serviceName, map);
    }
    
    /**
     * Assign a service without dynamic attributes to a realm
     */
    public void assignServiceRealm(String serviceName, String realm, Map map)
    throws Exception {
        if (!isServiceAssigned(serviceName, realm)) {
            OrganizationConfigManager ocm =
                    new OrganizationConfigManager(admintoken, realm);
            ocm.assignService(serviceName, map);
        }
    }
    
    /**
     * Unassign a service which has dynamic attributes like session or user
     * from a realm
     */
    public boolean unassignDynamicServiceRealm(String serviceName, String realm)
    throws Exception {
        AMIdentityRepository idrepo = new AMIdentityRepository(admintoken,
                realm);
        AMIdentity realmIdentity = idrepo.getRealmIdentity();
        Set set = realmIdentity.getAssignedServices();
        if (set.contains(serviceName))
            realmIdentity.unassignService(serviceName);
        set = realmIdentity.getAssignedServices();
        return (!set.contains(serviceName));
    }
    
    /**
     * Unassign a service without dynamic attributes from a realm
     */
    public void unassignServiceRealm(String serviceName, String realm)
    throws Exception {
        if (isServiceAssigned(serviceName, realm)) {
            OrganizationConfigManager ocm =
                    new OrganizationConfigManager(admintoken, realm);
            ocm.unassignService(serviceName);
        }
    }
    
    /**
     * Checks whether a service is assigned to a realm
     */
    public boolean isServiceAssigned(String serviceName, String realm)
    throws Exception {
        OrganizationConfigManager ocm =
                new OrganizationConfigManager(admintoken, realm);
        Set set = ocm.getAssignedServices();
        if (set.contains(serviceName))
            return (true);
        else
            return (false);
    }
    
    /**
     * Sets dynamic attributes for a service at the global leval
     */
    public boolean updateGlobalServiceDynamicAttributes(String serviceName,
            Map map)
            throws Exception {
        ServiceManager sm = new ServiceManager(admintoken);
        ServiceSchemaManager ssm = sm.getSchemaManager(serviceName , "1.0");
        ServiceSchema ss = ssm.getDynamicSchema();
        log(Level.FINEST, "updateGlobalServiceDynamicAttributes", 
                "Setting dynamic attributes of " + serviceName + " to " + map);
        ss.setAttributeDefaults(map);
        Map attrMap = ss.getAttributeDefaults();
        log(Level.FINEST, "updateGlobalServiceDynamicAttributes", 
                "Dynamic attributes after update " + attrMap);
        return isAttrValuesEqual(map, attrMap);
    }
    
    /**
     * This method create one or multiple datastores by datastore index number
     * from configuration data specified in the properties file
     * @param cdsIndex datastore index to be retrieved from the properties file
     * @param propertyFileName properties file name (without extenstion)
     */
    public void createDataStore(int cdsIndex, String propertyFileName)
    throws Exception {
        entering("createDataStore", null);
        createDataStore(getDataStoreConfigByIndex(cdsIndex, propertyFileName));
        exiting("createDataStore");
    }

    /**
     * This method create one or multiple datastores from configuration data
     * specified in a Map
     * @param cdsMap a map contains datstore configuration data
     */
    public void createDataStore(Map cdsMap)
    throws Exception {
        entering("createDataStore", null);
        String dsCount = (String)cdsMap.get(SMSConstants.UM_DATASTORE_COUNT);
        for (int j = 0; j < Integer.parseInt(dsCount); j++)
            createDataStoreImpl(setDataStoreConfigData(j, cdsMap));
        exiting("createDataStore");
    }
    
    /**
     * This method calls Service Management methods to a create datastore.
     */
    private void createDataStoreImpl(Map cdsiMap)
    throws Exception {
        entering("createDataStoreImpl", null);
        LDAPCommon ldc = null;
        try {
            String realmName = (String)cdsiMap.
                    get(SMSConstants.UM_DATASTORE_REALM);
            String dsName = (String)cdsiMap.
                    get(SMSConstants.UM_DATASTORE_NAME);
            if (!doesDataStoreExists(realmName, dsName)) {
                String dsType = (String)cdsiMap.
                        get(SMSConstants.UM_DATASTORE_TYPE);
                
                // Retrieve the LDAP server information and call the
                // method to load the AM user schema
                String dsHost = (String)cdsiMap.
                        get(SMSConstants.UM_LDAPv3_LDAP_SERVER);
                String dsPort = (String)cdsiMap.
                        get(SMSConstants.UM_LDAPv3_LDAP_PORT);
                String dsDirmgrdn = (String)cdsiMap.
                        get(SMSConstants.UM_DATASTORE_ADMINID);
                String dsDirmgrpwd = (String)cdsiMap.
                        get(SMSConstants.UM_DATASTORE_ADMINPW);
                String dsRootSuffix = (String)cdsiMap.
                        get(SMSConstants.UM_LDAPv3_ORGANIZATION_NAME);
                String sslmode = (String)cdsiMap.
                        get(SMSConstants.UM_LDAPv3_LDAP_SSL_ENABLED);
                String keystore = null;
                if (sslmode.equals("true"))
                    keystore = (String)cdsiMap.
                            get(SMSConstants.UM_DATASTORE_KEYSTORE);
                ldc = new LDAPCommon(dsHost, dsPort,
                        dsDirmgrdn, dsDirmgrpwd, dsRootSuffix, keystore);
                String schemaString = (String)globalCfgMap.
                        get(SMSConstants.UM_SCHEMNA_LIST + "." + dsType);
                log(Level.FINEST, "createDataStoreImpl", "Schema files to" +
                        " be loaded into the datastore: " + schemaString);
                String schemaAttributes = (String)globalCfgMap.
                        get(SMSConstants.UM_SCHEMNA_ATTR + "." + dsType);
                log(Level.FINEST, "createDataStoreImpl", "Schema" +
                        " attributes to check whether schema is  already" +
                        "loaded: " + schemaAttributes);                    
                if (schemaString != null && schemaAttributes != null ) {
                    ldc.loadAMUserSchema(schemaString, schemaAttributes);
                    ldc.disconnectDServer();
                }
                log(Level.FINE, "createDataStoreImpl", "Creating datastore " +
                        dsName +  "..." + cdsiMap);
                ServiceConfig cfg = getServiceConfig(admintoken, realmName,
                        true);
                cfg.addSubConfig(dsName,
                        dsType, 0,
                        setDataStoreAttributes(cdsiMap));
                if (doesDataStoreExists(realmName, dsName))
                    log(Level.FINE, "createDataStoreImpl", "Datastore " +
                            dsName +  " is created successfully.");
                else {
                    log(Level.SEVERE, "createDataStoreImpl",
                            "Failed to create datastore " + dsName);
                    assert false;
                }
            } else {
                log(Level.FINE, "createDataStoreImpl", "Datastore " +
                        dsName + " exists");
            }
        } catch (Exception e) {
            log(Level.SEVERE, "createDataStoreImpl", e.getMessage());
            e.printStackTrace();
	    if (ldc != null) {
		ldc.disconnectDServer();
	    }
            throw e;
        }
        exiting("createDataStoreImpl");
    }

    /**
     * This method updates a datastore with datastore attribute list
     * @param udsRealm is the realm name where the datastore belongs to
     * @param serviceName the name of service to query for
     * @param subConfigName the sub config name to query for
     * @param attributeName the attribute name to query for
     */
    public Set getAttributeValueServiceConfig(String udsRealm,
            String serviceName, String subConfigName, String attributeName)
    throws Exception {
        Object[] params = {udsRealm, serviceName, subConfigName, attributeName};
        entering("getAttributeValueServiceConfig", params);
        Set valSet = null;
        try {
            ServiceConfig cfg = getServiceConfig(admintoken, udsRealm,
                    serviceName);
            if (cfg != null) {
                ServiceConfig sc = cfg.getSubConfig(subConfigName);
                if (sc != null) {
                    Map map = sc.getAttributes();
                    if (map.containsKey(attributeName)) {
                        valSet = (Set)map.get(attributeName);
                    } else {
                        log(Level.SEVERE, "getAttributeValueServiceConfig",
                                "Cannot find " + attributeName + " in service" +
                                serviceName + " under realm " +  udsRealm);
                    }
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "getAttributeValueServiceConfig", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("getAttributeValueServiceConfig");
        return (valSet);
    }
    
    /**
     * This method updates a datastore with datastore attribute list
     * @param udsRealm is the realm name where the datastore belongs to
     * @param udsName datastore name to be updated
     * @param updatedAttrMap a list of attributes to be updated
     */
    public void updateDataStore(String udsRealm, String udsName, Map updAttrMap,
            boolean concat)
    throws Exception {
        entering("updateDataStore", null);
        try {
            log(Level.FINE, "updateDataStore", "Updating datastore " +
                    udsName + " in realm " + udsRealm + "...");
            ServiceConfig cfg = getServiceConfig(admintoken, udsRealm);
            if (cfg != null) {
                ServiceConfig sc = cfg.getSubConfig(udsName);
                if (sc != null) {
                    Map currentAttr = sc.getAttributes();
                    Set keys = updAttrMap.keySet();
                    Iterator keyIter = keys.iterator();
                    String key;
                    Set curValSet;
                    Set newValSet;
                    while (keyIter.hasNext()) {
                        key = (String)keyIter.next();
                        curValSet = (Set)currentAttr.get(key);
                        newValSet = (Set)updAttrMap.get(key);
                        if (!curValSet.contains(null)) {
                            if (concat) {
                                concatSet(newValSet, curValSet);
                            }
                        }
                        updAttrMap.put(key, newValSet);
                    }
                    sc.setAttributes(updAttrMap);
                    Map existingAttrMap = sc.getAttributesForRead();
                    log(Level.FINEST, "updateDatastore",
                            "Verifying the updated attribute(s)");
                    if (doesMapContainsKeysValues(updAttrMap,
                            existingAttrMap))
                        log(Level.FINE, "updateDataStore", "Datastore " +
                                udsName + " is updated successfully");
                    else {
                        log(Level.SEVERE, "updateDataStore",
                                "Failed to update datastore: " + udsName);
                        assert false;
                    }
                } else {
                    log(Level.SEVERE, "updateDataStore", "Datastore not" +
                            " found: " + udsName);
                    assert false;
                }
            } else {
                log(Level.SEVERE, "UpdateDataStore", "Datastore not found: " +
                        udsName);
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "UpdateDataStore", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("updateDataStore");
    }
    
    /**
     * This method checks if datastore exists in a realm
     * @param idseRealm realm name where datastore belongs to
     * @param idseName datastore name to be checked
     * @return true if datastore exists
     */
    public boolean doesDataStoreExists(String idseRealm, String idseName)
    throws Exception {
        entering("doesDataStoreExists", null);
        boolean datastoreFound = false;
        log(Level.FINEST, "doesDataStoreExists", "Realm = " + idseRealm + " " +
                "Datastore name " + idseName);
        datastoreFound = listDataStore(idseRealm).contains(idseName);
        if (datastoreFound)
            log(Level.FINEST, "doesDataStoreExists", "Datastore" + idseName +
                    "exists");
        else
            log(Level.FINEST, "doesDataStoreExists", "Datastore" + idseName +
                    "does not exists");
        exiting("doesDataStoreExists");
        return datastoreFound;
    }
    
    /**
     * This method lists all datastore(s) in a realm
     * @param ldsRealm realm name where datastore belongs to
     * @return a set of datastore
     */
    public Set listDataStore(String ldsRealm)
    throws Exception {
        entering("listDataStore", null);
        Set datastoreNameSet = null;
        try {
            log(Level.FINE, "listDataStore", "Listing datastore for realm " +
                    ldsRealm + "...");
            ServiceConfig cfg = getServiceConfig(admintoken, ldsRealm);
            datastoreNameSet = (cfg != null) ? cfg.getSubConfigNames() :
                Collections.EMPTY_SET;
            if ((datastoreNameSet != null) && !datastoreNameSet.isEmpty()) {
                for (Iterator i = datastoreNameSet.iterator(); i.hasNext();) {
                    String dsname = (String)i.next();
                    log(Level.FINEST, "listDataStore", "Datastore is " +
                            dsname);
                }
            } else
                log(Level.FINE, "listDataStore", "Datastore list empty");
        } catch (Exception e) {
            log(Level.SEVERE, "listDataStore", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("listDataStore");
        return datastoreNameSet;
    }

    /**
     * Get the names of datastores which need to be deleted. This is called
     * one wants to delete all the datastores except for the one's supplied
     * by the qatest list
     * @param set list of all the datastores currently in the system
     * @param idx index referring to server index as defined in resources/
     * config/UMGlobalDatastoreConfig resource bundle. All operations will be
     * for/on this server
     * @return List list of datastore names to be deleted
     * @throws java.lang.Exception
     */
    public List getDatastoreDeleteList(Set set, int idx)
    throws Exception {
        entering("getDatastoreDeleteList", null);
        log(Level.FINEST, "getDatastoreDeleteList", "All existing" +
                " datastores: " + set.toString());
        List list = new ArrayList();
        List dList = getCreatedDatastoreNames(idx);
        log(Level.FINEST, "getDatastoreDeleteList", "Datastores not to be" +
                " deleted: " + dList.toString());   
        Iterator key = set.iterator();
        String item;
        while (key.hasNext()) {
            item = (String)key.next();
            if (!dList.contains(item))
                list.add(item);
        }
        exiting("getDatastoreDeleteList");
        return (list);
    }

    /**
     * This method deletes all the datastores at a particular realm. If user
     * specifies a server index, all qatest created datastores for that server
     * are not deleted.
     * @param realm realm in which to delete datastores
     * @param idx index referring to server index as defined in resources/
     * config/UMGlobalDatastoreConfig resource bundle. All operations will be
     * for/on this server
     * @throws java.lang.Exception
     */
    public void deleteAllDataStores(String realm, int idx)
    throws Exception {
        entering("deleteAllDataStore", null);
        List dList = null;
        if (idx != -1) {
            dList = getCreatedDatastoreNames(idx);
            log(Level.FINEST, "deleteAllDataStores", "Datastores not to be" +
                    " deleted: " + dList.toString());            
        }
        Set set = listDataStore(realm);
        log(Level.FINEST, "deleteAllDataStores", "Datastores to be  deleted: " +
                set.toString());
        Iterator key = set.iterator();
        String item;
        while (key.hasNext()) {
            item = (String)key.next();
            if (idx != -1) {
                if (!dList.contains(item))
                    deleteDataStore(realm, item);
            } else
                deleteDataStore(realm, item);
        }
        exiting("deleteAllDataStore");
    }

    /**
     * Deletes all the datastores for a sepcified server at a specified realm
     * created by qatest.
     * @param realm realm in which to delete datastores
     * @param idx index referring to server index as defined in resources/
     * config/UMGlobalDatastoreConfig resource bundle. All operations will be
     * for/on this server.
     * @throws java.lang.Exception
     */
    public void deleteCreatedDataStores(String realm, int idx)
    throws Exception {
        entering("deleteCreatedDataStore", null);
        List dList = getCreatedDatastoreNames(idx);
        log(Level.FINEST, "deleteCreatedDataStores", "Datastores to be" +
                " deleted: " + dList.toString());
        Iterator key = dList.iterator();
        String item;
        while (key.hasNext()) {
            item = (String)key.next();
            deleteDataStore(realm, item);
        }
        exiting("deleteCreatedDataStore");
    }
    
    /**
     * This method delete one or multiple datastore(s) that specified in a
     * properties file.
     * @param ddsIndex realm name where datastore belongs to
     * @param propertyFileName properties file with datastore configuration data
     */
    public void deleteDataStore(int ddsIndex, String propertyFileName)
    throws Exception {
        entering("deleteDataStore", null);
        deleteDataStore(getDataStoreConfigByIndex(ddsIndex,
                propertyFileName));
        exiting("deleteDataStore");
    }
    
    /**
     * This method delete a datastore that defined in a map.
     * @param ddsMap a map with datastore configuration data
     */
    public void deleteDataStore(Map ddsMap)
    throws Exception {
        entering("deleteDataStore", null);
        Map oneCfgMap = null;
        String ddsRealm;
        String ddsName;
        String dsCount = (String)ddsMap.get(SMSConstants.UM_DATASTORE_COUNT);
        for (int j = 0; j < Integer.parseInt(dsCount); j++) {
            oneCfgMap = setDataStoreConfigData(j, ddsMap);
            ddsRealm = (String)oneCfgMap.get(SMSConstants.UM_DATASTORE_REALM);
            ddsName = (String)oneCfgMap.get(SMSConstants.UM_DATASTORE_NAME);
            deleteDataStore(ddsRealm, ddsName);
        }
        exiting("deleteDataStore");
    }
    
    /**
     * This method delete a datastore.
     * @param ddsRealm realm name where datastore belongs
     * @param ddsName datastore name to be deleted
     */
    public void deleteDataStore(String ddsRealm, String ddsName)
    throws Exception {
        entering("deleteDataStore", null);
        try {
            log(Level.FINE, "deleteDataStore", "Deleting datastore " +
                    ddsName + "...");
            ServiceConfig cfg = getServiceConfig(admintoken, ddsRealm);
            if (cfg != null)
                cfg.removeSubConfig(ddsName);
            if (!doesDataStoreExists(ddsRealm, ddsName))
                log(Level.FINE, "deleteDataStore", "Datastore " + ddsName +
                        " was deleted successfully");
            else {
                log(Level.SEVERE, "DeleteDataStore",
                        "Failed to delete datastore " + ddsName);
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "deleteDataStore", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("deleteDataStore");
    }
    
    /**
     * This method retrieves datastore configuration data from a properties file
     * by index number and store them in a map
     * @param gdscbiIndex datastore index
     * @param cfgFileName properties file name
     * @return a map that contains the datastore configuration without the
     * prefix, SMSConstants.UM_DATASTORE_PARAMS_PREFIX
     */
    public Map getDataStoreConfigByIndex(int gdscbiIndex, String cfgFileName)
    throws Exception {
        entering("getDataStoreConfigByIndex", null);
        Map ldapMap = new HashMap();
        Map fileMap = new HashMap();
        try {
            log(Level.FINE, "getDataStoreConfigByIndex",
                    "Retrieving datastore with index " + gdscbiIndex +
                    " from property file " + cfgFileName);
            fileMap = getMapFromProperties(getBaseDir() + fileseparator +
                                serverName + fileseparator + "built" + 
                                fileseparator + "classes" + fileseparator + 
                                cfgFileName + ".properties");
            Set keys = fileMap.keySet();
            Iterator keyIter = keys.iterator();
            String key;
            String value;
            String prefixCfgParams;
            while (keyIter.hasNext()) {
                key = keyIter.next().toString();
                value = fileMap.get(key).toString();
                prefixCfgParams = SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                        gdscbiIndex;
                if (key.toString().startsWith(prefixCfgParams)) {
                    ldapMap.put(key.toString().
                            substring(prefixCfgParams.length() + 1), value);
                }
            }
            log(Level.FINEST, "getDataStoreConfigByIndex",
                    "Datastore config data " + ldapMap.toString());
            if (ldapMap.isEmpty()) {
                log(Level.SEVERE, "getDataStoreConfigByIndex",
                        "Could not retrieve datastore for index" + gdscbiIndex);
                assert false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        exiting("getDataStoreConfigByIndex");
        return ldapMap;
    }

    /**
     * This method creates the generated UMGlobalDatastoreConfig file under the 
     * dynamically generated built/classes/config directory. The file name is
     * UMGlobalDatastoreConfig-Generated.properties. This file contains all the
     * inherited properties for datastore configuration for all the server which
     * can be configured using qatest
     * @param fileName This file is UMGlobalDatastoreConfig.properties. This 
     * could be from global config level (resources/config) or a module level
     * (resources/module)
     * @throws java.lang.Exception
     */
    public void createUMDatastoreGlobalMap(String fileName)
    throws Exception {
        createUMDatastoreGlobalMap(fileName, null, null);
    }

    /**
     * This method creates the generated UMGlobalDatastoreConfig file under the 
     * dynamically generated built/classes/config directory. The file name is
     * UMGlobalDatastoreConfig-Generated.properties. This file contains all the
     * inherited properties for datastore configuration for all the server which
     * can be configured using qatest
     * @param fileName This file is UMGlobalDatastoreConfig.properties. This 
     * could be from global config level (resources/config) or a module level
     * (resources/module)
     * @param map Map contains the index and name of servers for which
     * datastore will be configured. Server refrence the servers defined in
     * resources/config/UMGlobalDatastoreConfig.properties file
     * @param sIdx The mode in which qatest is being executed
     * @throws java.lang.Exception
     */
    public void createUMDatastoreGlobalMap(String fileName, Map map,
            String sIdx)
    throws Exception {
        createUMDatastoreGlobalMap(fileName, map, sIdx, null);
    }
    
    /**
     * This method creates the generated UMGlobalDatastoreConfig file under the 
     * dynamically generated built/classes/config directory. The file name is
     * UMGlobalDatastoreConfig-Generated.properties. This file contains all the
     * inherited properties for datastore configuration for all the server which
     * can be configured using qatest
     * @param fileName This file is UMGlobalDatastoreConfig.properties. This 
     * could be from global config level (resources/config) or a module level
     * (resources/module)
     * @param map Map contains the index and name of servers for which
     * datastore will be configured. Server refrence the servers defined in
     * resources/config/UMGlobalDatastoreConfig.properties file
     * @param sIdx The mode in which qatest is being executed
     * @param module Module name for which datstore in being created
     * @throws java.lang.Exception
     */    
    public void createUMDatastoreGlobalMap(String fileName, Map umDMap,
            String sIdx, String module)
    throws Exception {
        Map moduleMap = null;
        Map newModuleMap = null;
        Map globalMap = null;
        Map finalMap = null;

        globalMap = getMapFromResourceBundle("config" + fileseparator +
                    fileName);
        log(Level.FINEST, "createUMDatastoreGlobalMap", "Map containing end" +
                "user specified values: " + globalMap.toString());
        log(Level.FINEST, "createUMDatastoreGlobalMap", "Mode in which qatest" +
                "is executing: " + sIdx);

        if (module != null) {
            moduleMap = getMapFromResourceBundle(module + fileseparator +
                    fileName); 
            checkForRealm(moduleMap, module);
            log(Level.FINEST, "createUMDatastoreGlobalMap", "Map containing" +
                    " end user specified values for module " + module + ": "
                    + moduleMap.toString());
            newModuleMap = new HashMap();
            newModuleMap.putAll(globalMap);
            newModuleMap.putAll(moduleMap);
            finalMap = createSwappedMap(newModuleMap, fileName, umDMap, sIdx);
        } else
            finalMap = createSwappedMap(globalMap, fileName, umDMap, sIdx);
        
        if (module == null)
            createFileFromMap(finalMap, getBaseDir() + fileseparator +
                    serverName + fileseparator + "built" + fileseparator +
                    "classes" + fileseparator + "config" + fileseparator +
                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                    "-Generated.properties");
        else
            createFileFromMap(finalMap, getBaseDir() + fileseparator +
                    serverName + fileseparator + "built" + fileseparator +
                    "classes" + fileseparator + module + fileseparator +
                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                    "-Generated.properties");
    }

    /**
     * This method craeted the final map by inheriting all the user and default
     * datastore properties. It further checks the following:
     * - The minimum specified properties and their values are specified for
     * the server(s) for which datastore is(are) being configured. This
     * includes datastore count, datastore type, datastore root suffix,
     * datastore admin password, datastore server name and datastore server
     * port.
     * - It also checks, if user config store is set to embedded, the created 
     * datastore points to configuration datastore properties
     * @param gblMap Map containing all the datastore attributes and their
     * values specified by the end user
     * @param fileName This file is UMGlobalDatastoreConfig.properties. This 
     * could be from global config level (resources/config) or a module level
     * (resources/module)
     * @param umDMap  Map contains the index and name of servers for which
     * datastore will be configured. Server refrence the servers defined in
     * resources/config/UMGlobalDatastoreConfig.properties file
     * @param sIdx The mode in which qatest is being executed
     * @return Map map containing all the inherited values. This includes end
     * user specified and the default
     * @throws java.lang.Exception
     */
    public Map createSwappedMap(Map gblMap, String fileName, Map umDMap,
            String sIdx)
    throws Exception {
        Set keys = null;
        Iterator keyIter;
        String key;
        String newKey;
        String value;
        String dataType = null;
        String rootSuffix = null;
        String strPassword = null;
        int minD = -1;
        int maxD = -1;

        Map newDefaultMap = new HashMap();
        Map sCfgDetails = new HashMap();
        
        // The numbers refer to server index as defined in resources/config/
        // UMGlobalDatastoreConfig.properties resource bundle
        if (sIdx.equals(SMSConstants.QATEST_EXEC_MODE_SINGLE)) {
            minD = 1;
            maxD = 2;
        } else if (sIdx.equals(SMSConstants.QATEST_EXEC_MODE_DUAL)) {
            minD = 0;
            maxD = 2;
        } else if (sIdx.equals(SMSConstants.QATEST_EXEC_MODE_ALL)) {
            minD = 0;
            maxD = 4;
        }

        for (int mCount = minD; mCount < maxD; mCount++) {
            log(Level.FINEST, "createSwappedMap", "Server index for which " +
                    "datastore is being configured: " + mCount);
  
            String serverName = (String)umDMap.get(Integer.toString(mCount)); 
            ResourceBundle cfgData =
                ResourceBundle.getBundle("Configurator-" + serverName +
                "-Generated");
            String umdatastore = cfgData.getString("umdatastore");

            if (!umdatastore.equals("embedded")) {
                if (!gblMap.containsKey(
                        SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                        mCount + "." + SMSConstants.UM_DATASTORE_COUNT)) {
                        log(Level.SEVERE, "createSwappedMap",
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                mCount + "." + SMSConstants.UM_DATASTORE_COUNT +
                                " key is mandatory.");
                        assert false;
                }
                if ((gblMap.get(SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount
                        + "." + SMSConstants.UM_DATASTORE_COUNT).toString()).
                        equals("")) {
                        log(Level.SEVERE, "createSwappedMap",
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount
                                + "." + SMSConstants.UM_DATASTORE_COUNT +
                                " value is mandatory.");
                        assert false;
                }

                // Get number of datastores to be configured for this server
                int dCount = new Integer(gblMap.get(
                        SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount + "." +
                        SMSConstants.UM_DATASTORE_COUNT).toString()).intValue();
                for (int i = 0; i < dCount; i++) {
                    log(Level.FINEST, "createSwappedMap", "Datastore index" +
                            " being configured: " + dCount);

                    if (!gblMap.containsKey(
                            SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount +
                            "." + SMSConstants.UM_DATASTORE_TYPE + "." + i)) {
                        log(Level.SEVERE, "createSwappedMap",
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                mCount + "." + SMSConstants.UM_DATASTORE_TYPE +
                                "." + i + " key is mandatory.");
                        assert false;
                    }
                    if ((gblMap.get(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                            mCount + "." + SMSConstants.UM_DATASTORE_TYPE +
                            "." + i).toString()).equals("")) {
                        log(Level.SEVERE, "createSwappedMap",
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                mCount + "." + SMSConstants.UM_DATASTORE_TYPE +
                                "." + i + " value is mandatory.");
                        assert false;
                    }

                    // Check that datatype key and value are specified
                    dataType = (String)gblMap.get(
                            SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount +
                            "." + SMSConstants.UM_DATASTORE_TYPE + "." + i);
                    if (!gblMap.containsKey(
                            SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount +
                            "." + SMSConstants.UM_DATASTORE_ROOT_SUFFIX + "." +
                            i)) {
                        log(Level.SEVERE, "createSwappedMap",
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount
                                + "." + SMSConstants.UM_DATASTORE_ROOT_SUFFIX +
                                "." + i + " key is mandatory.");
                        assert false;
                    }
                    if ((gblMap.get(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                            mCount + "." + SMSConstants.UM_DATASTORE_ROOT_SUFFIX
                            + "." + i).toString()).equals("")) {
                        log(Level.SEVERE, "createSwappedMap",
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount
                                + "." + SMSConstants.UM_DATASTORE_ROOT_SUFFIX +
                                "." + i + " value is mandatory.");
                        assert false;
                    }

                    // Check that root suffix key and value are specified
                    rootSuffix = (String)gblMap.get(
                            SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount +
                            "." + SMSConstants.UM_DATASTORE_ROOT_SUFFIX + "." +
                            i);
                    if (!gblMap.containsKey(
                            SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount +
                            "." + SMSConstants.UM_DATASTORE_ADMINPW + "." + i))
                    {
                        log(Level.SEVERE, "createSwappedMap",
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount
                                + "." + SMSConstants.UM_DATASTORE_ADMINPW + "."
                                + i + " key is mandatory.");
                        assert false;
                    }
                    if ((gblMap.get(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                            mCount + "." + SMSConstants.UM_DATASTORE_ADMINPW +
                            "." + i).toString()).equals("")) {
                        log(Level.SEVERE, "createSwappedMap",
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount
                                + "." + SMSConstants.UM_DATASTORE_ADMINPW + "."
                                + i + " value is mandatory.");
                        assert false;
                    }

                    // Check that datastore admin password key and value are
                    // specified
                    strPassword = (String)gblMap.get(
                            SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount +
                            "." + SMSConstants.UM_DATASTORE_ADMINPW + "." + i);
                    if (!gblMap.containsKey(
                            SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount +
                            "." + SMSConstants.UM_DATASTORE_ADMINPW + "." + i))
                    {
                        log(Level.SEVERE, "createSwappedMap",
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount
                                + "." + SMSConstants.UM_DATASTORE_ADMINPW + "."
                                + i + " key is mandatory.");
                        assert false;
                    }
                    if ((gblMap.get(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                            mCount + "." + SMSConstants.UM_DATASTORE_ADMINPW +
                            "." + i).toString()).equals("")) {
                        log(Level.SEVERE, "createSwappedMap",
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount
                                + "." + SMSConstants.UM_DATASTORE_ADMINPW + "."
                                + i + " value is mandatory.");
                        assert false;
                    }

                    // Check that datastore server name key and value are
                    // specified
                    if (!gblMap.containsKey(
                            SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount +
                            "." + SMSConstants.UM_LDAPv3_LDAP_SERVER + "." + i))
                    {
                        log(Level.SEVERE, "createSwappedMap",
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount
                                + "." + SMSConstants.UM_LDAPv3_LDAP_SERVER +
                                "." + i + " key is mandatory.");
                        assert false;
                    }
                    if ((gblMap.get(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                            mCount + "." + SMSConstants.UM_LDAPv3_LDAP_SERVER +
                            "." + i).toString()).equals("")) {
                        log(Level.SEVERE, "createSwappedMap",
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount
                                + "." + SMSConstants.UM_LDAPv3_LDAP_SERVER +
                                "." + i + " value is mandatory.");
                        assert false;
                    }

                    // Check that datastore server port key and value are
                    // specified
                    if (!gblMap.containsKey(
                            SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount +
                            "." + SMSConstants.UM_LDAPv3_LDAP_PORT + "." + i)) {
                        log(Level.SEVERE, "createSwappedMap",
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount
                                + "." + SMSConstants.UM_LDAPv3_LDAP_PORT + "."
                                + i + " key is mandatory.");
                        assert false;
                    }
                    if ((gblMap.get(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                            mCount + "." + SMSConstants.UM_LDAPv3_LDAP_PORT +
                            "." + i).toString()).equals("")) {
                        log(Level.SEVERE, "createSwappedMap",
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount
                                + "." + SMSConstants.UM_LDAPv3_LDAP_PORT + "."
                                + i + " value is mandatory.");
                        assert false;
                    }
                    Map defaultMap = getMapFromResourceBundle("config" +
                            fileseparator + "default" + fileseparator +
                            fileName, dataType);
                    log(Level.FINEST, "createSwappedMap", "Default datastore" +
                            " properties and values: " + defaultMap.toString());

                    keys = defaultMap.keySet();
                    keyIter = keys.iterator();

                    while (keyIter.hasNext()) {
                        key = keyIter.next().toString();
                        if (key.indexOf(dataType) != -1) {
                            value = defaultMap.get(key).toString().trim();

                            // One can specify ROOT_SUFFIX tag in any key in the
                            // default datastore properties. That tag will be
                            // replaced by user specified value for root suffix
                            if (value.indexOf("ROOT_SUFFIX") != -1)
                                value = value.replace("ROOT_SUFFIX",
                                        rootSuffix);

                            if (key.indexOf(SMSConstants.UM_LDAPv3_AUTHPW) != -1
                                    &&  value.equals(""))
                                value = strPassword;

                            newKey = key.replace(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + dataType,
                                        SMSConstants.UM_DATASTORE_PARAMS_PREFIX
                                        +  mCount) + "." + i;
                            newDefaultMap.put(newKey, value);
                        }
                    }
                }
                newDefaultMap.putAll(gblMap);
            } else {
                dataType = SMSConstants.UM_DATASTORE_SCHEMA_TYPE_LDAP;

                log(Level.FINEST, "createSwappedMap", "Server name and index" +
                    " map: " + umDMap.toString());

                Map defaultMap = getMapFromResourceBundle("config" +
                        fileseparator + "default" + fileseparator + fileName,
                        dataType); 
                log(Level.FINEST, "createSwappedMap", "Default datastore" +
                        " properties and values: " + defaultMap.toString());

                // If user datastore type is set to embedded, set ldap details
                // to point to configuration server details and remove filtered
                // role and normal role from supported operations as they are
                // supported by any other datastore except Sun Directory Server
                String strCfgServer = (String)umDMap.get(mCount + "." +
                        SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                        SMSConstants.UM_LDAPv3_LDAP_SERVER);
                String strCfgPort =  (String)umDMap.get(mCount + "." +
                        SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                        SMSConstants.UM_LDAPv3_LDAP_PORT);
                String strCfgRootSuffix =  (String)umDMap.get(mCount + "." +
                        SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                        SMSConstants.UM_LDAPv3_ORGANIZATION_NAME);

                String strNamingURL =
                        (String)cfgData.getString(
                        TestConstants.KEY_AMC_NAMING_URL);

                int iFirstSep = strNamingURL.indexOf(":");
                int iSecondSep = strNamingURL.indexOf(":", iFirstSep + 1);
                String strHost = strNamingURL.substring(iFirstSep + 3,
                        iSecondSep);

                boolean hMatch = true;
                if (!strCfgServer.equals(null) || !strCfgServer.equals("")) {
                    String strHostAdd =
                            InetAddress.getByName(strHost).toString();
                    String strHostIPAdd = strHostAdd.substring(
                            strHostAdd.indexOf("/") + 1, strHostAdd.length());
                    String strDSAdd =
                            InetAddress.getByName(strCfgServer).toString();
                    String strDSIPAdd = strDSAdd.substring(
                            strDSAdd.indexOf("/") + 1, strDSAdd.length());
                    if (strDSIPAdd.indexOf("127") == -1) {
                        if (!strHostIPAdd.equals(strDSIPAdd))
                            hMatch = false;
                    }
                }

                String strCfgPassword = null;
                if (!hMatch)
                    strCfgPassword =  cfgData.getString(
                            TestConstants.KEY_ATT_DS_DIRMGRPASSWD);
                else
                    strCfgPassword =  cfgData.getString(
                            TestConstants.KEY_ATT_AMADMIN_PASSWORD);

                String strCfgDirMgrDN =  (String)umDMap.get(mCount + "." +
                        SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                        SMSConstants.UM_LDAPv3_AUTHID);
                Map chgGblData = new HashMap();
                chgGblData.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                        mCount + "." +
                        SMSConstants.UM_DATASTORE_ROOT_SUFFIX + "." + 0,
                        strCfgRootSuffix);
                chgGblData.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                        mCount + "." + SMSConstants.UM_DATASTORE_ADMINPW +
                        "." + 0, strCfgPassword);
                chgGblData.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                        mCount + "." + SMSConstants.UM_DATASTORE_ADMINID +
                        "." + 0, strCfgDirMgrDN);
                chgGblData.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                        mCount + "." + SMSConstants.UM_LDAPv3_LDAP_SERVER +
                        "." + 0, strCfgServer);
                chgGblData.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                        mCount + "." + SMSConstants.UM_LDAPv3_LDAP_PORT +
                        "." + 0, strCfgPort);
                if (!hMatch) {
                    chgGblData.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                        mCount + "." + SMSConstants.UM_LDAPv3_AUTHID + "." +
                        0, strCfgDirMgrDN);
                }
                chgGblData.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                        mCount + "." + SMSConstants.UM_DATASTORE_COUNT, 1);

                keys = defaultMap.keySet();
                keyIter = keys.iterator();

                while (keyIter.hasNext()) {
                    key = keyIter.next().toString();
                    if (key.indexOf(dataType) != -1) {
                        value = defaultMap.get(key).toString().trim();

                        // One can specify ROOT_SUFFIX tag in any key in the
                        // default datastore properties. That tag will be
                        // replaced by user specified value for root suffix
                        if (value.indexOf("ROOT_SUFFIX") != -1)
                            value = value.replace("ROOT_SUFFIX",
                                    strCfgRootSuffix);

                        if (key.indexOf(SMSConstants.UM_LDAPv3_AUTHPW) != -1 &&
                                value.equals(""))
                            value = strCfgPassword;
                        newKey = key.replace(
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                                dataType,
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                mCount) + "." + 0;
                        newDefaultMap.put(newKey, value);
                    }
                }
                newDefaultMap.putAll(chgGblData);
            }
        }

        log(Level.FINEST, "createSwappedMap", "Final datastore properties" +
                " and values: " + newDefaultMap.toString());

        return (newDefaultMap);
    }
        
    /**
     * Gets the names of all datastores created by qatest for the sepcified 
     * server index
     * @param idx
     * @return List list of datastore names
     * @throws java.lang.Exception
     */
    public List getCreatedDatastoreNames(int idx)
    throws Exception {
        return (getCreatedDatastoreNames(idx, "config"));
    }

    /**
     * Gets the names of all datastores created by qatest for the sepcified
     * server index
     * @param idx
     * @return List list of datastore names
     * @throws java.lang.Exception
     */
    public List getCreatedDatastoreNames(int idx, String module)
    throws Exception {
        String dName;
        Map map = getMapFromResourceBundle(module + fileseparator +
                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "-Generated");
        List list = new ArrayList();
        int dCount = new Integer(map.get(
                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + idx + "." +
                SMSConstants.UM_DATASTORE_COUNT).toString()).intValue();
        for (int i = 0; i < dCount; i++) {
            dName = (String)map.get(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                    idx + "." + SMSConstants.UM_DATASTORE_NAME + "." + i);
            list.add(dName);
        }
        log(Level.FINEST, "getCreatedDatastoreNames", "Datastore names for" +
                "server index " + idx + "is: " + list.toString());
        return (list);
    }

    /**
     * This method checks whether a given type of datastore plugin is
     * configured at the selected realm using the existing admin
     * <code>SSOToken</code>.
     * The supported type of pluings are :
     *  - amSDK
     *  - LDAVPv3forAMDS
     *  - LDAPv3ForAD
     *  - LDAPv3
     *  - files
     * @param pluginName Plugin name
     * @param subRealmName subRealm name
     * @return true if Plugin type to be checked is supported to the given realm
     */
    public boolean isPluginConfigured(String pluginName, String subRealmName)
    throws Exception {
        return isPluginConfigured(admintoken, pluginName, subRealmName);
    }

    /**
     * This method checks whether a given type of datastore plugin is 
     * configured at the selected realm
     * The supported type of pluings are :
     *  - amSDK
     *  - LDAVPv3forAMDS
     *  - LDAPv3ForAD
     *  - LDAPv3
     *  - files
     * @param ssotoken SSO token
     * @param pluginName Plugin name
     * @param subRealmName subRealm name
     * 
     * @return true if Plugin type to be checked is supported to the given realm
     */
    public boolean isPluginConfigured(SSOToken ssoToken, String pluginName, 
            String subRealmName)
    throws Exception {
        entering("isPluginConfigured", null);
        ServiceConfig subConfig;
        OrganizationConfigManager orgMgr =
                new OrganizationConfigManager(ssoToken, subRealmName);
        ServiceConfig sc =
                orgMgr.getServiceConfig(SMSConstants.REALM_SERVICE);        
        boolean isPluginConfigured = false;
        if (sc != null) {
            try {
                Iterator items = sc.getSubConfigNames().iterator();
                while (items.hasNext()) {
                    subConfig = sc.getSubConfig((String) items.next());
                    if (subConfig.getSchemaID().equalsIgnoreCase(pluginName)) {
                        isPluginConfigured = true;
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
        exiting("isPluginConfigured");
        return isPluginConfigured;
    }
    
    /**
     * Check if the FAM is deployed against AM DIT. 
     * Return true if it is AM DIT.
     */
    public boolean isAMDIT()
    throws Exception {
        boolean result = false;
            ServiceSchemaManager idRepoServiceSchemaManager;
            idRepoServiceSchemaManager = new ServiceSchemaManager(admintoken,
                    "sunIdentityRepositoryService", "1.0");
            int svcRevisionNumber = idRepoServiceSchemaManager
                    .getRevisionNumber(); 
            if (svcRevisionNumber <= 20) {
                log(Level.FINE, "isAMDIT", "This is AM 7.x DIT"); 
                result = true;
            } else {
                log(Level.FINE, "isAMDIT", "This is FAM 8.x DIT"); 
            }
        return result;
    }    
  
    /**
     * Returns a map containing the config store datastore attributes. These
     * values are retrived from serverconfig.xml file.
     * @param url server url
     * @return Map containing config store configuration attributes
     * @throws java.lang.Exception
     */
    public Map getServerConfigData(String url)
    throws Exception {
        ServiceConfigManager scm = new ServiceConfigManager(
                "iPlanetAMPlatformService", admintoken);
        ServiceConfig globalSvcConfig = scm.getGlobalConfig(null);
        ServiceConfig all = (globalSvcConfig != null) ?
            globalSvcConfig.getSubConfig("com-sun-identity-servers") : null;
        ServiceConfig cfg = (all != null) ? all.getSubConfig(url) : null;
        Properties prop = getPropertiesFromXML(
                (Set)(cfg.getAttributes()).get("serverconfigxml"));
        String strServerXML = prop.toString();
        int StartIndx = strServerXML.indexOf("{");
        strServerXML = strServerXML.substring(StartIndx + 1,
                strServerXML.indexOf("}"));
        StartIndx = strServerXML.indexOf("<!--");
        strServerXML = strServerXML.substring(0, StartIndx)
                + strServerXML.substring(strServerXML.indexOf("-->") + 3,
                strServerXML.length());
        StartIndx = strServerXML.indexOf("<!--");
        strServerXML = strServerXML.substring(0, StartIndx)
                + strServerXML.substring(strServerXML.indexOf("-->") + 3,
                strServerXML.length());

        return (getConfigServerDetails(strServerXML));
    }

    /**
     * This method parses the ServerConfigXML
     * @param Set set containing the ServerConfigXML
     * @return set of properties
     */
    public Map getConfigServerDetails(String strXMLFile)
    throws Exception {
        try {
            DocumentBuilderFactory factory =
            DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(
                    new StringReader(strXMLFile)));
            Element topElement = document.getDocumentElement();
            Map map = parseServerConfigXML(
                (Node)topElement);
            return map;
        } catch (Exception e) {
            log(Level.SEVERE, "getConfigServerDetails", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * This method parses the ServerConfigXML
     * @param Set set containing the ServerConfigXML
     * @return set of properties
     */
    public Properties getPropertiesFromXML(Set set)
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
     * This method parses the ServerConfigXML
     * @param Node parentNode of the ServerConfigXML
     * @return map
     */
    public Map parseServerConfigXML(Node parentNode) {

        Set smSet = new HashSet();
        NodeList avList = parentNode.getChildNodes();
        Map<String, String> map = new HashMap<String, String>();
        int numAVPairs = avList.getLength();

        if (numAVPairs <= 0) {
            return null;
        }

        for (int l = 0; l < numAVPairs; l++) {
            Node avPair = avList.item(l);
            // now reset values to prepare for the next AV pair.
            if ((avPair.getNodeType() == Node.ELEMENT_NODE) &&
                    avPair.getNodeName().equals("ServerGroup")) {
                NamedNodeMap nnmap = avPair.getAttributes();
                if (((nnmap.getNamedItem("name")).getNodeValue()).
                        contains("sms")) {
                    NodeList smsList = avPair.getChildNodes();
                    for (int j = 0; j < smsList.getLength(); j++) {
                        Node smsNode = smsList.item(j);
                        if (smsNode.getNodeName().equals("Server")) {
                            NamedNodeMap mappy = smsNode.getAttributes();
                            map.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_LDAP_SERVER,
                                    (mappy.getNamedItem("host")).
                                    getNodeValue());
                            map.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_LDAP_PORT,
                                    (mappy.getNamedItem("port")).
                                    getNodeValue());
                            if (((mappy.getNamedItem("type")).getNodeValue()).
                                    contains("@")) {
                                map.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX
                                        + "." +
                                        SMSConstants.UM_LDAPv3_LDAP_SSL_ENABLED,
                                        "false");
                            } else {
                                map.put(
                                        SMSConstants.UM_DATASTORE_PARAMS_PREFIX
                                        + "." +
                                        SMSConstants.UM_LDAPv3_LDAP_SSL_ENABLED,
                                        "true");
                            }
                        }
                        if (smsNode.getNodeName().equals("User")) {
                            NodeList userList = smsNode.getChildNodes();
                            for (int m = 0; m < userList.getLength(); m++) {
                                Node userNode = userList.item(m);
                                if ((userNode.getNodeName()).equals("DirDN")) {
                                    map.put(SMSConstants.
                                            UM_DATASTORE_PARAMS_PREFIX + "." +
                                            SMSConstants.UM_LDAPv3_AUTHID,
                                            userNode.getTextContent());
                                    map.put(SMSConstants.
                                            UM_DATASTORE_PARAMS_PREFIX + "." +
                                            SMSConstants.UM_DATASTORE_ADMINID,
                                            userNode.getTextContent());
                                }
                            }
                        }
                        if (smsNode.getNodeName().equals("BaseDN")) {
                            map.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." +
                                    SMSConstants.UM_LDAPv3_ORGANIZATION_NAME,
                                    smsNode.getTextContent());
                        }
                    }
                }
            }
        }
        return (map == null) ? null : map;
    }

    /**
     * This method applies only when configuring datatsores for a module.
     * Current qatest framework allowes a module to create a datastore only at a
     * sub realm. If a module tries to create a datastore at root realm, an
     * error is thrown.
     * @param map Map containing all the datastore attributes
     * @param module Module which is creating the datastore
     * @throws java.lang.Exception
     */
    private void checkForRealm(Map map, String module)
    throws Exception {
        String realm;
        int maxDatastores = new Integer((String)globalCfgMap.get(
                "UMGlobalConfig.maxDatastoreConfig")).intValue();
        for (int mCount = 0; mCount < maxDatastores; mCount++) {
            if (map.containsKey(SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                    mCount + "." + SMSConstants.UM_DATASTORE_COUNT)) {
                int dCount = new Integer(map.get(
                        SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount +
                        "." + SMSConstants.UM_DATASTORE_COUNT).toString()).
                        intValue();
                for (int i = 0; i < dCount; i++) {
                    boolean bkey =  map.containsKey(
                            SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount +
                            "." + SMSConstants.UM_DATASTORE_REALM + "." + i);
                    if (!bkey) {
                        log(Level.SEVERE, "checkForRealm", "Module " + module +
                                "datastore " +
                                SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount
                                + " with index " + dCount + "doesn't have" +
                                " realm attribute. Realm attribute is" +
                                " mandatory for creating datastore at module" +
                                " level");
                        assert false;
                    }
                    realm = (String)map.get(
                            SMSConstants.UM_DATASTORE_PARAMS_PREFIX + mCount +
                            "." + SMSConstants.UM_DATASTORE_TYPE + "." + i);
                    if (realm.equals(TestCommon.realm)) {
                        log(Level.SEVERE, "checkForRealm", "Module " + module +
                                "cannot change the datastore configuration at" +
                                " the root realm. Modules can configure" +
                                " datastores only in sub realms");
                        assert false;
                    }
                }
            }
        }
    }
    
    /**
     * This method get a Service Configuration object from Service
     * Management methods
     */
    private ServiceConfig getServiceConfig(SSOToken admToken, String gscRealm)
    throws Exception {
        return (getServiceConfig(admToken, gscRealm, false,
                IdConstants.REPO_SERVICE));
    }

    /**
     * This method get a Service Configuration object from Service
     * Management methods
     */
    private ServiceConfig getServiceConfig(SSOToken admToken, String gscRealm,
            boolean createIfNull)
    throws Exception {
        return (getServiceConfig(admToken, gscRealm, createIfNull,
                IdConstants.REPO_SERVICE));
    }

    /**
     * This method get a Service Configuration object from Service
     * Management methods
     */
    private ServiceConfig getServiceConfig(SSOToken admToken,
            String gscRealm, String serviceName)
            throws Exception {
        return (getServiceConfig(admToken, gscRealm, false, serviceName));
    }

    /**
     * This method get a Service Configuration object from Service Management
     * methods
     */
    private ServiceConfig getServiceConfig(SSOToken admToken,
            String gscRealm, boolean createIfNull, String serviceName)
            throws Exception {
        ServiceConfig svcfg = null;
        IDMCommon idmObj = new IDMCommon();
        if (!gscRealm.equals(realm)) {
            log(Level.FINE, "getServiceConfig", "Checking sub realm " +
                    gscRealm + " ...");
            // Check the realm to make sure it exists before creating object
            // for ServiceConfig
            if (idmObj.searchRealms(admToken, gscRealm.
                    substring(gscRealm.lastIndexOf(realm) + 1),
                    idmObj.getParentRealm(gscRealm)).isEmpty()) {
                log(Level.SEVERE, "getServiceConfig", "Realm " + gscRealm +
                        " not found");
                assert false;
            } else
                log(Level.FINEST, "getServiceConfig", "Found realm " +
                        gscRealm);
        }
        ServiceConfigManager scm = new ServiceConfigManager(
                serviceName, admToken);
        svcfg = scm.getOrganizationConfig(gscRealm, null);
        if (createIfNull && svcfg == null) {
            OrganizationConfigManager orgCfgMgr = new
                    OrganizationConfigManager(admToken, gscRealm);
            Map attrValues = getDefaultAttributeValues(admToken);
            svcfg = orgCfgMgr.addServiceConfig(serviceName, attrValues);
        }
        return svcfg;
    }
    
    /**
     * This method set the datastore attributes and store them in a map
     */
    private Map setDataStoreAttributes(Map sdscfmMap)
    throws Exception {
        String newTempKey;
        Map dsAttributeMap = new HashMap<String, Set<String>>();
        String dsType = (String)sdscfmMap.get(SMSConstants.UM_DATASTORE_TYPE);
        try {
            Set keys = sdscfmMap.keySet();
            Iterator keyIter = keys.iterator();
            String key;
            String value;
            String portNumber;
            while (keyIter.hasNext()) {
                key = keyIter.next().toString();
                value = sdscfmMap.get(key).toString();
                if (!key.startsWith(SMSConstants.UM_DATASTORE_KEY_PREFIX)) {
                    if ((dsType == null) ||
                            (dsType.equalsIgnoreCase(
                            SMSConstants.UM_DATASTORE_SCHEMA_TYPE_AMSDK))) {
                        putSetIntoMap(key, dsAttributeMap, value, "|");
                    } else if (!key.equals(SMSConstants.UM_LDAPv3_LDAP_PORT)) {
                        if (key.equals(SMSConstants.UM_LDAPv3_LDAP_SERVER)) {
                            portNumber = (String)sdscfmMap.
                                    get(SMSConstants.UM_LDAPv3_LDAP_PORT);
                            value = (portNumber == null) ? value + ":389" :
                                value + ":" + portNumber;
                        }
                        putSetIntoMap(key, dsAttributeMap, value, "|");
                    }
                }
            }
            log(Level.FINEST, "setDataStoreAttributes",
                    "Datastore attributes " + dsAttributeMap.toString());
        } catch (Exception e) {
            log(Level.SEVERE, "setDataStoreAttributes", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return dsAttributeMap;
    }
    
    /**
     * This method set the datastore configuration data and store it in a map.
     * It will remove the datastore number from the key and append LDAPv3
     * attribute prefix in LDAPv3 attribute keys.
     */
    private Map setDataStoreConfigData(int sdscdIndex, Map sdscfmMap)
    throws Exception {
        Map dsMap = new HashMap();
        String dsType = (String)sdscfmMap.
                get(SMSConstants.UM_DATASTORE_TYPE + "." + sdscdIndex);
        if (dsType == null)
            dsType = (String)sdscfmMap.
                    get(SMSConstants.UM_DATASTORE_TYPE);
        try {
            Set keys = sdscfmMap.keySet();
            Iterator keyIter = keys.iterator();
            String newTempKey;
            String key;
            String value;
            int posOfLastPeriodIndex = 0;
            while (keyIter.hasNext()) {
                key = keyIter.next().toString();
                value = sdscfmMap.get(key).toString();
                posOfLastPeriodIndex = key.lastIndexOf("." + sdscdIndex);
                if (posOfLastPeriodIndex >= 0) {
                    newTempKey = (posOfLastPeriodIndex >= 0) ?
                        key.substring(0, posOfLastPeriodIndex) : key;
                    dsMap.put(newTempKey, value);
                }
            }
            if (dsMap.isEmpty()) {
                log(Level.SEVERE, "setDataStoreConfigData",
                        "Could not find config data for datastore " +
                        sdscdIndex);
                assert false;
            } else
                log(Level.FINEST, "setDataStoreConfigData", dsMap.toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return dsMap;
    }
    
    /**
     * This method compare if keys and values in a Map are also in another Map
     */
    private boolean doesMapContainsKeysValues(Map sMap, Map lMap)
    throws Exception {
        boolean foundKeysValues = true;
        Set keys = sMap.keySet();
        String key;
        Set sValue;
        Set lValue;
        Iterator keyIter = keys.iterator();
        while (keyIter.hasNext()) {
            key = keyIter.next().toString();
            sValue = (Set)sMap.get(key);
            lValue = (Set)lMap.get(key);
            log(Level.FINEST, "doesMapContainsKeysValues", "Key = " + key);
            log(Level.FINEST, "doesMapContainsKeysValues",
                    "Small set value = " + sValue.toString());
            log(Level.FINEST, "doesMapContainsKeysValues",
                    "Large set value = " + lValue.toString());
            // if one of the value of the key does not match or empty, set the
            // flag to false.
            if (!lMap.get(key).equals(sValue)) {
                foundKeysValues = false;
                break;
            }
        }
        return foundKeysValues;
    }
    
    /**
     * This method get a default attribute value for CreateDataStore
     */
    private Map getDefaultAttributeValues(SSOToken adminSSOToken)
    throws SMSException, SSOException {
        log(Level.FINEST, "getDefaultAttributeValues", null);
        ServiceSchemaManager schemaMgr = new
                ServiceSchemaManager(IdConstants.REPO_SERVICE, adminSSOToken);
        ServiceSchema orgSchema = schemaMgr.getOrganizationSchema();
        Set attrs = orgSchema.getAttributeSchemas();
        Map values = new HashMap(attrs.size() * 2);
        AttributeSchema as;
        for (Iterator iter = attrs.iterator(); iter.hasNext();) {
            as = (AttributeSchema)iter.next();
            values.put(as.getName(), as.getDefaultValues());
        }
        return values;
    }

    /**
     * Create a service sub-configuration in a realm.
     * @param realmName - the realm in which the subconfiguration should be
     * created.
     * @param serviceName - the name of the service
     * @param subConfigName - the name of the subconfiguration to be created.
     * @param subConfigId - the ID of parent configuration
     * @param attrValues - a Map containing the attributes values to be set
     * in the authentication module to be created
     */
    public void addSubConfig(String realmName,
            String serviceName,
            String subConfigName,
            String subConfigId,
            Map attrValues)
    throws Exception {
        Object[] params = {realmName, serviceName, subConfigName,
                subConfigId, attrValues};
        entering("addSubConfig", params);
        SSOToken serviceToken = getToken(adminUser, adminPassword, basedn);

        try {
            log(Level.FINE, "addSubConfig",
                    "Retrieving ServiceConfigManager for service " +
                    serviceName + " ...");
            ServiceConfigManager scm = new ServiceConfigManager(
                    serviceName, serviceToken);

            ServiceConfig sc = scm.getOrganizationConfig(realmName, null);
            if (sc == null) {
                sc = scm.createOrganizationConfig(realmName, null);
            }

            if ((subConfigName != null) && (subConfigName.length() > 0)) {
                StringTokenizer st = new StringTokenizer(subConfigName, "/");
                int tokenCount = st.countTokens();

                for (int i = 1; i <= tokenCount; i++) {
                    String scn = unescapeName(st.nextToken());
                    log(Level.FINEST, "addSubConfig",
                            "Sub Config Name = " + scn);
                    if (i != tokenCount) {
                        sc = sc.getSubConfig(scn);
                    } else {
                        if (subConfigId == null) {
                            subConfigId = subConfigName;
                        }
                        log(Level.FINEST, "addSubConfig",
                                "Sub Config ID = " + subConfigId);
                        sc.addSubConfig(scn, subConfigId, 0, attrValues);
                    }
                }
            } else {
                log(Level.SEVERE, "addSubConfig",
                        "subConfigName is set to null or has a 0 length");
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "addSubConfig", "Exception message = " +
                    e.getMessage());
            e.printStackTrace();
        } finally {
            if (serviceToken != null) {
                destroyToken(serviceToken);
            }
        }
    }

    /**
     * The method reverses the escape sequence "&#47;" to the character "/".
     * @param txtName - the String to be unescaped
     */
    private static String unescapeName(String txtName) {
        if (txtName == null) {
            return txtName;
        }

        int len = txtName.length();
        if (len == 0) {
            return txtName;
        }

        int indx;
        for (int i = 0; i < txtName.length(); i++) {
            indx = txtName.indexOf("&#47;");
            if (indx >= 0) {
                String prefixID = txtName.substring(0, indx);
                String postfixID = txtName.substring(indx + 5);
                txtName = prefixID + "/" + postfixID;
            }
        }
        return (txtName);
    }

    /**
     * Get the absolute realm
     * @param realmName - a String containing the name of the realm
     * @return the absolute realm name
     */
    protected String getAbsoluteRealm(String realmName) {
        String absoluteRealm = realmName;
        if ((realmName != null) && (realmName.indexOf("/") != 0)) {
            absoluteRealm = "/" + realmName;
        }
        return absoluteRealm;
    }

     /**
      * Delete a subconfiguration in a realm.
      * @param realmName - the name of the realm in which the subconfiguration
      * should be removed.
      * @param serviceName - the name of the service
      * @param subConfigName - the name of the subconfiguration to be removed.
      * @throws Exception
      */
     public void deleteSubConfig(String realmName,
             String serviceName,
             String subConfigName)
     throws Exception {
         Object[] params = {realmName, serviceName, subConfigName};
         entering("deleteSubConfig", params);
         SSOToken serviceToken = null;
         try {
            serviceToken = getToken(adminUser, adminPassword, basedn);
            ServiceConfigManager scm = new ServiceConfigManager(serviceName,
                    serviceToken);
            ServiceConfig sc = scm.getOrganizationConfig(realmName, null);
            if (sc == null) {
                sc = scm.createOrganizationConfig(realmName, null);
            }

            StringTokenizer st = new StringTokenizer(subConfigName, "/");
            int tokenCount = st.countTokens();

            for (int i = 1; i <= tokenCount; i++) {
                String scn = unescapeName(st.nextToken());

                if (i != tokenCount) {
                    sc = sc.getSubConfig(scn);
                } else {
                    sc.removeSubConfig(scn);
                }
            }
         } catch (Exception e) {
            log(Level.SEVERE, "deleteSubConfig", "Exception message = " +
                    e.getMessage());
            e.printStackTrace();
         } finally {
             if (serviceToken != null) {
                 destroyToken(serviceToken);
             }
         }
     }

     /**
      * Modify attribute values in a service sub configuration
      * @param realmName - the realm in which the service sub configuration
      * exists
      * @param serviceName - the name of the service
      * @param subConfigName - the name of the subconfiguration to be updated
      * @param attrValues - a <code>Map</code> containing the attribute names
      * as keys and <code>Set</code> objects containing attribute values as
      * values in the <code>Map</code>
      * @throws Exception
      */
     public void modifySubConfig(String realmName, String serviceName,
             String subConfigName, Map attrValues)
     throws Exception {
         Object[] params = {realmName, serviceName, subConfigName, attrValues};
         entering("modifySubConfig", params);
         SSOToken serviceToken = null;
         try {
            serviceToken = getToken(adminUser, adminPassword, basedn);
            ServiceConfigManager scm = new ServiceConfigManager(serviceName,
                    serviceToken);
            ServiceConfig sc = scm.getOrganizationConfig(realmName, null);
            if (sc == null) {
                sc = scm.createOrganizationConfig(realmName, null);
            }

            StringTokenizer st = new StringTokenizer(subConfigName, "/");
            int tokenCount = st.countTokens();
            for (int i = 1; i <= tokenCount; i++) {
                String scn = unescapeName(st.nextToken());
                sc = sc.getSubConfig(scn);
            }
            sc.setAttributes(attrValues);
         } catch (Exception e) {
            log(Level.SEVERE, "modifySubConfig", "Exception message = " +
                    e.getMessage());
            e.printStackTrace();
         } finally {
             if (serviceToken != null) {
                 destroyToken(serviceToken);
             }
         }
     }

     /**
      * Retrieve the attribute values of a sub-configuration.
      * @param realmName - the name of the realm in which the sub-configuration
      * exists.
      * @param serviceName - the name of the service for the sub-configuration
      * @param subConfigName - the name of the sub-configuration
      * @return a <code>Map</code> containing the attribute values for the
      * sub-configuration.
      * @throws Exception
      */
     public Map getSubConfigAttrs(String realmName,
             String serviceName,
             String subConfigName)
     throws Exception {
         Object[] params = {realmName, serviceName, subConfigName};
         entering("getSubConfigAttrs", params);
         SSOToken serviceToken = null;
         Map attrValues = null;

         try {
            serviceToken = getToken(adminUser, adminPassword, basedn);
            ServiceConfig cfg = getServiceConfig(serviceToken, realmName,
                    serviceName);
            if (cfg != null) {
                ServiceConfig sc = cfg.getSubConfig(subConfigName);
                if (sc != null) {
                    attrValues = sc.getAttributes();
                    log(Level.FINEST, "getSubConfigAttrs",
                            "Attribute map = " + attrValues);
                }
            } else {
                log(Level.SEVERE, "getSubConfigAttrs",
                        "Unable to retrieve the ServiceConfig for sub-config " +
                        subConfigName + " of service " + serviceName +
                        " in realm " + realmName);
                assert false;
            }
            exiting("getSubConfigAttrs");
        } catch (Exception e) {
            log(Level.SEVERE, "getSubConfigAttrs", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (serviceToken != null) {
                destroyToken(serviceToken);
            }
            return (attrValues);
        }
     }
}
