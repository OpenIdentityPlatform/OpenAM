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
 * $Id: Manager.java,v 1.4 2008/07/24 23:06:12 huacui Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.agents.arch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * A <code>Manager</code> represents a service front for a subsystem of classes
 * that together belong to the same <code>Module</code>. It also acts as an
 * intermediate caching point for configuration values, thereby facilitating
 * the implementation of a service level hot-swap configuration mechanism that
 * guarantees the completion of requests with an integral view of the system
 * configuration. The <code>Manager</code> also exposes various convenience
 * methods and helper classes that together provide the necessary infrastructure
 * services needed for a subsystem to function correctly.
 */
public class Manager implements IConfigurationKeyConstants,
        IConfigurationAccess {
    
    /**
     * Allows the caller to retreive a <code>ISystemAccess</code> instance that
     * can be used to obtain system services offered by this 
     * <code>Manager</code> and its associated <code>Module</code> instance 
     * without requiring the caller to directly refer to any of them.
     *
     * @return a <code>ISystemAccess</code> instance.
     */
    public ISystemAccess newSystemAccess() {
        return new SystemAccess(this);
    }
    
   /* (non-Javadoc)
    * @see IConfigurationAccess#getSystemConfiguration(String, String)
    */
    public String getSystemConfiguration(String id, String defaultValue) {
        String result = getSystemConfiguration(id);
        if(result == null) {
            if(getModule().isLogMessageEnabled()) {
                getModule().logMessage("Configuration: using default value for:"
                        + id + ", value = \""
                        + defaultValue + "\"");
            }
            
            result = defaultValue;
        }
        return result;
    }
    
   /* (non-Javadoc)
    * @see IConfigurationAccess#getSystemConfiguration(java.lang.String)
    */
    public String getSystemConfiguration(String id) {
        if (id.startsWith(getGlobalConfigurationKeyPrefix())) {
            throw new IllegalArgumentException(
                    "Attempt to bypass configuration access for: " + id);
        }
        return getRawConfiguration(id);
    }
    
   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationMap(java.lang.String)
    */
    public Map getConfigurationMap(String id) {
        Map map = getCachedMap(id);
        if(map == null) {
            map = getConfigurationMapInternal(id);
            cacheObject(id, map);
        }
        return map;
    }
    
   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationStrings(java.lang.String)
    */
    public String[] getConfigurationStrings(String id) {
        String[] result = getCachedStrings(id);
        if(result == null) {
            result = getConfigurationStringsInternal(id);
            cacheStrings(id, result);
        }
        
        return result;
    }
    
   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationString(String, String)
    */
    public String getConfigurationString(String id, String defaultValue) {
        return getConfiguration(id, defaultValue);
    }
    
   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationString(java.lang.String)
    */
    public String getConfigurationString(String id) {
        return getConfiguration(id);
    }

   /* (non-Javadoc)
    * @see IConfigurationAccess#getApplicationConfigurationString(java.lang.String, String applicationName)
    */
    public String getApplicationConfigurationString(String id, String applicationName) {
        String result = null;
        Map map = getConfigurationMap(id);
        if ((map != null) && (applicationName != null) &&
                (applicationName.trim().length() > 0)) {
            result = (String) map.get(applicationName);
        }
        if ((result == null) || (result.trim().length() == 0)) {
            result = getConfigurationString(id);
        }
        return result;
    }
    

   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationLong(java.lang.String, long)
    */
    public long getConfigurationLong(String id, long defaultValue) {
        
        long   result      = defaultValue;
        String stringValue = getConfiguration(id);
        
        if((stringValue != null) && (stringValue.trim().length() > 0)) {
            try {
                result = Long.parseLong(stringValue);
            } catch(NumberFormatException ex) {
                if(getModule().isLogWarningEnabled()) {
                    getModule().logWarning("Invalid value specified for id: "
                            + id + ", string value: " + stringValue
                            + ", using default value: "
                            + defaultValue, ex);
                }
            }
        }
        
        return result;
    }
    
   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationLong(java.lang.String)
    */
    public long getConfigurationLong(String id) {
        long   result      = 0L;
        String stringValue = getConfiguration(id);
        
        if(stringValue != null) {
            try {
                result = Long.parseLong(stringValue);
            } catch(NumberFormatException ex) {
                getModule().logError(
                        "Exception while reading configuartion for id: " + id
                        + ", string value: " + stringValue, ex);
            }
        }
        
        return result;
    }
    
   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationInt(java.lang.String, int)
    */
    public int getConfigurationInt(String id, int defaultValue) {
        
        int    result      = defaultValue;
        String stringValue = getConfiguration(id);
        
        if((stringValue != null) && (stringValue.trim().length() > 0)) {
            try {
                result = Integer.parseInt(stringValue);
            } catch(NumberFormatException ex) {
                if(getModule().isLogWarningEnabled()) {
                    getModule().logWarning("Invalid value specified for id: "
                            + id + ", string value: " + stringValue
                            + ", using default value: \""
                            + defaultValue + "\"");
                }
            }
        }
        
        return result;
    }
    
   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationInt(java.lang.String)
    */
    public int getConfigurationInt(String id) {
        int result = 0;
        String stringValue = getConfiguration(id);
        
        if(stringValue != null && stringValue.trim().length() > 0) {
            try {
                result = Integer.parseInt(stringValue);
            } catch(NumberFormatException ex) {
                getModule().logError(
                        "Exception while reading configuartion for id: " + id
                        + ", string value: " + result, ex);
            }
        }
        
        return result;
    }
    
   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationBoolean(String, boolean)
    */
    public boolean getConfigurationBoolean(String id, boolean defaultValue) {
        return(Boolean.valueOf(getConfiguration(
                id, String.valueOf(defaultValue)))).booleanValue();
    }
    
   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationBoolean(java.lang.String)
    */
    public boolean getConfigurationBoolean(String id) {
        return Boolean.valueOf(getConfiguration(id)).booleanValue();
    }
    
   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfiguration(String, String)
    */
    public String getConfiguration(String id, String defaultValue) {
        String result = getConfiguration(id);
        if(result == null) {
            if(getModule().isLogMessageEnabled()) {
                getModule().logMessage("Configuration: using default value for:"
                        + id + ", value = \""
                        + defaultValue + "\"");
            }
            
            result = defaultValue;
        }
        
        return result;
    }

   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfiguration(java.lang.String)
    */
    public String getConfiguration(String id) {
        String result = getRawConfiguration(getConfigurationKey(id));
        if(result == null) {
            if(getModule().isLogMessageEnabled()) {
                getModule().logMessage("No configuration value specified for: "
                        + getConfigurationKey(id) + ", trying : "
                        + getGlobalConfigurationKey(id));
            }
            result = getRawConfiguration(getGlobalConfigurationKey(id));
        }
        if(result == null) {
            if(getModule().isLogMessageEnabled()) {
                getModule().logMessage("No configuration value found for: "
                        + getConfigurationKey(id) + ", or: "
                        + getGlobalConfigurationKey(id));
            }
        }
        return result;
    }

    /**
     * @{@inheritDoc}
     */
    public Map<String, Set<String>> getParsedConditionalUrls(String id) {
        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        String[] condUrls = getConfigurationStrings(id);
        if (condUrls != null) {
            for (String condUrl : condUrls) {
                int idx;
                if ((idx = condUrl.indexOf('|')) != -1) {
                    String domain = condUrl.substring(0, idx);
                    Set<String> urls = new LinkedHashSet<String>(Arrays.asList(condUrl.substring(idx + 1).split(",")));
                    if (!urls.isEmpty()) {
                        result.put(domain, urls);
                    }
                }
            }
        }
        if (getModule().isLogMessageEnabled()) {
            getModule().logMessage("Configuration: parsing conditional URLs for " + id
                    + " with values " + Arrays.toString(condUrls)
                    + " resulted in: " + result.toString());
        }
        return result;
    }

    /**
     * Constructs a <code>Manager</code> instance using the snapshot of the
     * current system configuraton.
     *
     * @param module the <code>Module</code> instance to which this
     * <code>Manager</code> will be associated with.
     * @param staticConfig an array of configuration keys that will be treated
     * as static configuration keys.
     */
    protected Manager(Module module, String[] staticConfig) {
        setModule(module);
        setModuleConfigurationKeyPrefix();
        
        setStaticConfigurationLookupKeySet(staticConfig);
        setConfigurationProperties(AgentConfiguration.getAll());
        initProcessedConfigurationCache();
    }
    
    private String[] getCachedStrings(String id) {
        String[] result = null;
        String[] cachedEntry = (String[]) getCachedObject(id);
        if (cachedEntry != null) {
            result = new String[cachedEntry.length];
            System.arraycopy(cachedEntry, 0, result, 0, cachedEntry.length);
        }
        
        return result;
    }
    
    private Map getCachedMap(String id) {
        return (Map)getCachedObject(id);
    }
    
    private Object getCachedObject(String id) {
        Object obj = getProcessedConfiugrationCache().get(id);
        if(getModule().isLogMessageEnabled()) {
            getModule().logMessage("Manager: Cache Lookup id= " + id
                    + ", result= " + obj);
        }
        return obj;
    }
    
    private void cacheObject(String id, Object object) {
        getProcessedConfiugrationCache().put(id, object);
        if(getModule().isLogMessageEnabled()) {
            getModule().logMessage("Manager: Cached object for id: "
                    + id + ", object: " + object);
        }
    }
    
    private void cacheStrings(String id, String[] value) {
        
        String[] toCache = new String[value.length];
        
        System.arraycopy(value, 0, toCache, 0, value.length);
        cacheObject(id, toCache);
        
        if(getModule().isLogMessageEnabled()) {
            getModule().logMessage("Manager: Caching String[] for " + id);
        }
    }
    
    private Map getConfigurationMapInternal(String id) {
        
        Properties  properties = getConfigurationProperties(id);
        String      mapId = getConfigurationKey(id) + "[";
        HashMap     map = new HashMap();
        Enumeration keys = properties.propertyNames();
        boolean moduleConfigAvailable = false;
        
        while(keys.hasMoreElements()) {
            String nextKey = keys.nextElement().toString();
            
            if(nextKey.startsWith(mapId)) {
                //first module-level configuration found
                moduleConfigAvailable = true;
                break;
            }
        }
        
        if(!moduleConfigAvailable) {
            //module level configuration not available
            mapId = getGlobalConfigurationKey(id) + "[";
            if(getModule().isLogMessageEnabled()) {
                getModule().logMessage("No configuration value specified for: "
                        + getConfigurationKey(id) + ", trying : "
                        + getGlobalConfigurationKey(id));
            }
        }
        keys = properties.propertyNames();
        
        while(keys.hasMoreElements()) {
            String nextKey = keys.nextElement().toString();
            int index = nextKey.indexOf(']');
            if(nextKey.startsWith(mapId) && index > mapId.length()) {
                String innerKey = nextKey.substring(mapId.length(),index);
                String value = getRawConfiguration(nextKey);
                
                if (innerKey != null && innerKey.trim().length() > 0 &&
                        value != null && value.trim().length() > 0) {
                    Object oldValue = map.get(innerKey);
                    if (oldValue == null) {
                        map.put(innerKey, value);
                    } else {
                        getModule().logError("Duplicate map entry "
                                + innerKey + ", for id: " + id
                                + ", rejecting the value: " + value);
                    }
                }
            }
        }
        
        return Collections.unmodifiableMap(map);
    }
    
    private String[] getConfigurationStringsInternal(String id) {
        
        Properties  properties = getConfigurationProperties(id);
        String      mapId = getConfigurationKey(id) + "[";
        TreeMap     map = new TreeMap();
        Enumeration keys = properties.propertyNames();
        boolean moduleConfigAvailable = false;
        
        while(keys.hasMoreElements()) {
            String nextKey = keys.nextElement().toString();
            
            if(nextKey.startsWith(mapId)) {
                //first module-level configuration found
                moduleConfigAvailable = true;
                break;
            }
        }
        
        if(!moduleConfigAvailable) {
            //module level configuration not available
            mapId = getGlobalConfigurationKey(id) + "[";
            if(getModule().isLogMessageEnabled()) {
                getModule().logMessage("No configuration value specified for: "
                        + getConfigurationKey(id) + ", trying : "
                        + getGlobalConfigurationKey(id));
            }
        }
        
        keys = properties.propertyNames();
        
        while(keys.hasMoreElements()) {
            String nextKey = keys.nextElement().toString();
            int index  = nextKey.indexOf(']');
            if(nextKey.startsWith(mapId) && index > mapId.length()) {
                String innerkey = nextKey.substring(mapId.length(), index);
                String value = getRawConfiguration(nextKey);
                
                if (innerkey != null && innerkey.trim().length() > 0 &&
                        value != null && value.trim().length() > 0) {
                    try {
                        Integer integer = new Integer(innerkey);
                        Object oldValue = map.get(integer);
                        if (oldValue == null) {
                            map.put(integer, value);
                        } else {
                            getModule().logError("Duplicate list index "
                                    + integer + ", for id: " + id
                                    + ", rejecting the value: " + value);
                        }
                    } catch(Exception ex) {
                        getModule().logError("Invalid configuration: "
                                + nextKey + ", " + value + " specified.", ex);
                    }
                }
            }
        }
        
        if(getModule().isLogMessageEnabled()) {
            getModule().logMessage("Configuration Strings Map for id " + id
                    + " is: " + map);
        }
        
        String[] result = new String[map.size()];
        Iterator it     = map.keySet().iterator();
        int      index  = 0;
        
        while(it.hasNext()) {
            Integer integer = (Integer) it.next();
            result[index] = (String) map.get(integer);
            index++;
        }
        
        if(getModule().isLogMessageEnabled()) {
            String newLine = System.getProperty("line.separator", "\n");
            StringBuffer buff =
                    new StringBuffer("Configuration Strings for ");
            
            buff.append(id).append(newLine);
            
            for(int i = 0; i < result.length; i++) {
                buff.append(i).append("=").append(result[i]).append(newLine);
            }
            
            getModule().logMessage(buff.toString());
        }
        
        return result;
    }
    
    private String getRawConfiguration(String id) {
        String value = (String) getConfigurationProperties(id).get(id);
        if (value != null) {
            if (value.trim().length() > 0) {
                value = value.trim();
            } else {
                value = null;
            }
        }
        
        if(getModule().isLogMessageEnabled()) {
            getModule().logMessage("Configuration: id => " + id + ", value => "
                    + value);
        }
        
        return value;
    }
    
    private String getGlobalConfigurationKey(String id) {
        return getGlobalConfigurationKeyPrefix() + id;
    }
    
    private String getConfigurationKey(String id) {
        return getModuleConfigurationKeyPrefix() + id;
    }
    
    private void setModuleConfigurationKeyPrefix() {
        _moduleConfigurationKeyPrefix = AGENT_CONFIG_PREFIX
                + getModule().getModuleFixedName() + ".";
    }
    
    private String getModuleConfigurationKeyPrefix() {
        return _moduleConfigurationKeyPrefix;
    }
    
    private String getGlobalConfigurationKeyPrefix() {
        return AGENT_CONFIG_PREFIX;
    }
    
    private Hashtable getProcessedConfiugrationCache() {
        return _processedConfigurationCache;
    }
    
    private void initProcessedConfigurationCache() {
        _processedConfigurationCache = new Hashtable();
    }
    
    private Properties getConfigurationProperties(String key) {
        Properties result = null;
        String baseKey = key;
        int index = key.indexOf('[');
        if (index != -1) {
            baseKey = key.substring(0, index);
        }
        if (getStaticConfigurationLookupKeySet().contains(baseKey)) {
            if (getModule().isLogMessageEnabled()) {
                getModule().logMessage("Manager: Key: " + key
                        + " is not hot-swappable");
            }
            result = _staticProperties;
        } else {
            result = _dynamicProperties;
        }
        return result;
    }
    
    private void setConfigurationProperties(Properties configurationProperties){
        _dynamicProperties = configurationProperties;
    }
    
    public Module getModule() {
        return _module;
    }
    
    private void setModule(Module module) {
        _module = module;
    }
    
    private HashSet getStaticConfigurationLookupKeySet() {
        return (HashSet) _staticConfigurationKeyTable.get(
                getModule().getModuleFixedName());
    }
    
    private void setStaticConfigurationLookupKeySet(String[] keys) {
        synchronized (Manager.class) {
            HashSet staticKeySet = getStaticConfigurationLookupKeySet();
            if (staticKeySet == null) {
                staticKeySet = new HashSet();
                ArrayList allKeys = new ArrayList();
                if (keys != null && keys.length > 0) {
                    for (int i=0; i<keys.length; i++) {
                        allKeys.add(keys[i]);
                    }
                }
                String[] baseKeys =
                        AgentConfiguration.CONFIG_STATIC_SUBKEY_LIST;
                if (baseKeys != null && baseKeys.length > 0) {
                    for (int i=0; i<baseKeys.length; i++) {
                        allKeys.add(baseKeys[i]);
                    }
                }
                if (getModule().isLogMessageEnabled()) {
                    getModule().logMessage("Manager: base static key list: "
                            + allKeys);
                }
                Iterator it = allKeys.iterator();
                while (it.hasNext()) {
                    String id = (String) it.next();
                    String globalId = getGlobalConfigurationKey(id);
                    String localId = getConfigurationKey(id);
                    staticKeySet.add(id);
                    staticKeySet.add(globalId);
                    staticKeySet.add(localId);
                    if (getModule().isLogMessageEnabled()) {
                        getModule().logMessage(
                                "Manager: adding static keys:"
                                + id + ", " + globalId + ", " + localId);
                    }
                }
                _staticConfigurationKeyTable.put(
                        getModule().getModuleFixedName(), staticKeySet);
                if (getModule().isLogMessageEnabled()) {
                    getModule().logMessage("Manager: Static config keys are: "
                            + staticKeySet);
                }
            } else {
                if (getModule().isLogMessageEnabled()) {
                    getModule().logMessage(
                            "Manager: Using old static config keys: "
                            + staticKeySet);
                }
            }
        }
    }
    
    private Module _module;
    private Properties _dynamicProperties;
    private Hashtable _processedConfigurationCache;
    private String _moduleConfigurationKeyPrefix;
    
    private static Hashtable _staticConfigurationKeyTable = new Hashtable();
    private static Properties _staticProperties;
    
    static {
        _staticProperties = AgentConfiguration.getAll();
    }
}
