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
 * $Id: ServiceConfigManagerImpl.java,v 1.13 2009/01/28 05:35:03 ww203982 Exp $
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */
package com.sun.identity.sm;

import static org.forgerock.openam.ldap.LDAPUtils.rdnValue;

import javax.naming.event.NamingEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.iplanet.am.util.Cache;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.common.DNUtils;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.RDN;

/**
 * The class <code>ServiceConfigurationManagerImpl</code> provides interfaces
 * to read the service's configuration data. It provides access to
 * <code>ServiceConfigImpl</code> which represents a single "configuration" in
 * the service. It manages configuration data only for GLOBAL and ORGANIZATION
 * types.
 */
class ServiceConfigManagerImpl implements SMSObjectListener {
    // Instance variables
    private String serviceName;

    private String version;

    // Pointer to ServiceSchemaManangerImpl
    private ServiceSchemaManagerImpl ssm;

    // Pointer to schema changes listeners
    private String listenerId;
    private HashMap listenerObjects;

    // Notification search string
    private String orgNotificationSearchString;
    private String glbNotificationSearchString;
    private String schemaNotificationSearchString;

    // Service Instances & Groups
    private CachedSubEntries instances;

    private CachedSubEntries groups;

    // LRU caches for global and org configs
    Cache globalConfigs;
    Cache orgConfigs;
    
    // Validity of this object
    private boolean valid = true;

    /**
     * Constructs an instance of <code>ServiceConfigManagerImpl</code> for the
     * given service and version. It requires an user identity that will be used
     * to perform read operations. It is assumed that the application calling
     * this constructor should authenticate the user.
     */
    private ServiceConfigManagerImpl(SSOToken token, String serviceName,
            String version) throws SMSException, SSOException {
        this.serviceName = serviceName;
        this.version = version;

        // If caching is allowed, cache global & org configs
        if (SMSEntry.cacheSMSEntries) {
            globalConfigs = new Cache(10);
            orgConfigs = new Cache(10);
        }
    }

    /**
     * Returns ServiceSchemaManagerImpl
     */
    ServiceSchemaManagerImpl getServiceSchemaManagerImpl(SSOToken token)
        throws SMSException, SSOException {
        if ((ssm == null) || !ssm.isValid()) {
            // Get the ServiceSchemaManagerImpl
            ssm = ServiceSchemaManagerImpl.getInstance(
                token, serviceName, version);
        }
        return (ssm);
    }

    String getName() {
        return (serviceName);
    }

    String getVersion() {
        return (version);
    }

    /**
     * Returns the service instance names
     */
    Set getInstanceNames(SSOToken t) throws SMSException, SSOException {
        if (instances == null) {
            String dn = CreateServiceConfig.INSTANCES_NODE +
                ServiceManager.getServiceNameDN(serviceName, version);
            instances = CachedSubEntries.getInstance(t, dn);
        }
        return (instances.getSubEntries(t));
    }

    /**
     * Returns the configuration group names
     */
    Set getGroupNames(SSOToken t) throws SMSException, SSOException {
        if (groups == null) {
            String dn = CreateServiceConfig.GLOBAL_CONFIG_NODE +
                ServiceManager.getServiceNameDN(serviceName, version);
            groups = CachedSubEntries.getInstance(t, dn);
        }
        return (groups.getSubEntries(t));
    }

    ServiceInstanceImpl getInstance(SSOToken token, String instanceName)
            throws SMSException, SSOException {
        return (ServiceInstanceImpl.getInstance(token, serviceName, version,
                instanceName));
    }

    /**
     * Returns the global configuration for the given service instance.
     */
    ServiceConfigImpl getGlobalConfig(SSOToken token, String instanceName)
            throws SMSException, SSOException {
        // Get group name
    	String groupName = SMSUtils.DEFAULT;
        if ((instanceName != null) && !instanceName.equals(SMSUtils.DEFAULT)) {
            groupName = ServiceInstanceImpl.getInstance(token, 
            		serviceName, version,instanceName, null).getGroup();
        }
        
        
        String cacheName = null;
        ServiceConfigImpl answer = null;
        // Check the cache
        if (SMSEntry.cacheSMSEntries) {
            StringBuilder sb = new StringBuilder(50);
            cacheName = sb.append(token.getTokenID().toString()).append(
                    groupName).toString().toLowerCase();
            if (((answer = (ServiceConfigImpl) globalConfigs.get(cacheName))
                != null) && answer.isValid() && !answer.isNewEntry()) {
                return (answer);
            } else {
                // remove entry from cache
                globalConfigs.remove(cacheName);
                answer = null;
            }

        }
        
        // Not in cache, check global schema
        if ((ssm == null) || !ssm.isValid()) {
            // Get the ServiceSchemaManagerImpl
            ssm = ServiceSchemaManagerImpl.getInstance(
                token, serviceName, version);
        }
        ServiceSchemaImpl ss = ssm.getSchema(SchemaType.GLOBAL);
        if (ss == null) {
            return (null);
        }
        // Construct the sub-config
        String gdn = constructServiceConfigDN(groupName,
                CreateServiceConfig.GLOBAL_CONFIG_NODE, null);
        answer = ServiceConfigImpl.getInstance(token, this, ss, gdn, null,
                groupName, "", true);
        // Add to cache if needed
        if (SMSEntry.cacheSMSEntries) {
            globalConfigs.put(cacheName, answer);
        }
        return (answer);
    }

    /**
     * Returns the organization configuration for the given organization and
     * instance name.
     */
    ServiceConfigImpl getOrganizationConfig(SSOToken token, String orgName,
            String instanceName) throws SMSException, SSOException {
        // Construct the group name
    	String groupName = SMSUtils.DEFAULT;
        if ((instanceName != null) && !instanceName.equals(SMSUtils.DEFAULT)) {
            groupName = ServiceInstanceImpl.getInstance(token, 
            		serviceName, version, instanceName, orgName).getGroup();
        }
        String cacheName = null;
        ServiceConfigImpl answer = null;
        // Check the cache
        String orgdn = DNMapper.orgNameToDN(orgName);
        if (SMSEntry.cacheSMSEntries) {
            StringBuilder sb = new StringBuilder(50);
            cacheName = sb.append(token.getTokenID().toString()).append(
                    groupName).append(orgdn).toString().toLowerCase();
            if (((answer = (ServiceConfigImpl) orgConfigs.get(cacheName))
                != null) && answer.isValid() && !answer.isNewEntry()) {
                return (answer);
            } else {
                // remove entry from cache
                orgConfigs.remove(cacheName);
                answer = null;
            }
        }
        
        // Not in cache, check organization schema
        if ((ssm == null) || !ssm.isValid()) {
            // Get the ServiceSchemaManagerImpl
            ssm = ServiceSchemaManagerImpl.getInstance(
                token, serviceName, version);
        }
        ServiceSchemaImpl ss = ssm.getSchema(SchemaType.ORGANIZATION);
        if (ss == null) {
            return (null);
        }
        
        // Construct org config
        String orgDN = constructServiceConfigDN(groupName,
                CreateServiceConfig.ORG_CONFIG_NODE, orgdn);
        answer = ServiceConfigImpl.getInstance(token, this, ss, orgDN, orgName,
                groupName, "", false);
        if (answer == null)
            return null;
        // Add to cache if needed
        if (SMSEntry.cacheSMSEntries) {
            orgConfigs.put(cacheName, answer);
        }
        return (answer);
    }

    /**
     * Returns the PluginConfig for configured for the serivce
     */
    PluginConfigImpl getPluginConfig(SSOToken token, String name,
            String schemaName, String interfaceName, String orgName)
            throws SMSException, SSOException {
        PluginSchemaImpl psi = PluginSchemaImpl.getInstance(token, serviceName,
                version, schemaName, interfaceName, orgName);
        // If null, throw an exception
        if (psi == null) {
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-invalid-plugin-schema-name", null));
        }
        // Construct the DN
        StringBuilder groupName = new StringBuilder(100);
        groupName.append(name).append(",ou=").append(schemaName).append(",ou=")
                .append(interfaceName);
        String dn = constructServiceConfigDN(groupName.toString(),
                CreateServiceConfig.PLUGIN_CONFIG_NODE, DNMapper
                        .orgNameToDN(orgName));
        return (PluginConfigImpl.getInstance(token, psi, dn, orgName));
    }

    /**
     * Register for changes to service's configuration. The object will be
     * called when configuration for this service and version is changed.
     */
    String addListener(SSOToken token, ServiceListener listener) {
        registerListener(token);
        String id = SMSUtils.getUniqueID();
        synchronized (listenerObjects) {
            listenerObjects.put(id, listener);
        }
        if (debug.messageEnabled()) {
            debug.message("ServiceConfigManagerImpl(" + serviceName +
                "):addListener Class: " +  listener.getClass().getName() +
                " Listener ID: " + id);
        }
        return (id);
    }
    
    private synchronized void registerListener(SSOToken token) {
        if (listenerId == null) {
            // Regsiter for notifications
            listenerId = SMSNotificationManager.getInstance()
                .registerCallbackHandler(this);
            // Construct strings for determining notifications types
            DN notifyDN = DN.valueOf("ou=" + version + ",ou=" + serviceName + "," + SMSEntry.SERVICES_RDN);
            String sdn = notifyDN.toString().toLowerCase();
            orgNotificationSearchString =
                CreateServiceConfig.ORG_CONFIG_NODE.toLowerCase() + sdn;
            glbNotificationSearchString =
                CreateServiceConfig.GLOBAL_CONFIG_NODE.toLowerCase() + sdn +
                "," + SMSEntry.getRootSuffix();
            schemaNotificationSearchString = sdn + "," +
                SMSEntry.getRootSuffix();
            
            // Initialize instance variables
            listenerObjects = new HashMap();
        }
    }

    /**
     * Unregisters the listener from the service for the given listener ID. The
     * ID was issued when the listener was registered.
     */
    void removeListener(String listenerID) {
        if (listenerObjects != null) {
            synchronized (listenerObjects) {
                listenerObjects.remove(listenerID);
                if (listenerObjects.isEmpty()) {
                    deregisterListener();
                }
            }
            if (debug.messageEnabled()) {
                debug.message("ServiceConfigManagerImpl(" + serviceName +
                    "):removeListener ListenerId: " +  listenerID);
            }
        }
    }
    
    private synchronized void deregisterListener() { 
        if (listenerId != null) {
            SMSNotificationManager.getInstance().removeCallbackHandler(
                listenerId);
            listenerId = null;
        }
    }

    // Used by ServiceInstance
    boolean containsGroup(SSOToken token, String groupName)
            throws SMSException, SSOException {
        if (groups == null) {
            String dn = CreateServiceConfig.GLOBAL_CONFIG_NODE +
                ServiceManager.getServiceNameDN(serviceName, version);
            groups = CachedSubEntries.getInstance(token, dn);
        }
        return (groups.contains(token, groupName));
    }
    
    // Implementations for SMSObjectListener
    public void allObjectsChanged() {
        // Ignore, do nothing
        if (SMSEntry.eventDebug.messageEnabled()) {
            SMSEntry.eventDebug.message("ServiceConfigManagerImpl:" +
                "allObjectsChanged called. Ignoring the notification");
        }
    }

    public void objectChanged(String dn, int type) {
        // Check for listeners
        if ((listenerObjects == null) || listenerObjects.isEmpty()) {
            // No listeners registered
            return;
        }
        if (SMSEntry.eventDebug.messageEnabled()) {
            SMSEntry.eventDebug.message("ServiceConfigManagerImpl(" +
                serviceName + "):objectChanged Received notification for " +
                "DN: " + dn);
        }

        // check for service name, version and type
        boolean globalConfig = false;
        boolean orgConfig = false;
        int index = 0, orgIndex = 0;
        dn = DNUtils.normalizeDN(dn);
        if ((index = dn.indexOf(orgNotificationSearchString)) != -1) {
            orgConfig = true;
            if (index == 0) {
                // Organization config node is created
                // No data is stored in this node
                return;
            }
            orgIndex = orgNotificationSearchString.length();
        } else if ((index = dn.indexOf(glbNotificationSearchString)) != -1) {
            globalConfig = true;
        } else if ((index = dn.indexOf(schemaNotificationSearchString)) != -1) {
            // Global schema changes, resulting in config change
            globalConfig = true;
            orgConfig = true;
        } else if (serviceName.equalsIgnoreCase("sunidentityrepositoryservice")
                && (dn.startsWith(SMSEntry.ORG_PLACEHOLDER_RDN) || dn
                        .equalsIgnoreCase(DNMapper.serviceDN))) {
            // Since sunIdentityRepositoryService has realm creation
            // attributes, we need to send notification
            orgConfig = true;
        } else {
            // Notification DN does not match the servic ename
            return;
        }

        // Get the group and component name
        String groupName = "";
        String compName = "";
        if (index > 1) {
            DN compDn = DN.valueOf(dn.substring(0, index - 1));
            List<RDN> rdns = new ArrayList<>();
            for (RDN rdn : compDn) {
                rdns.add(rdn);
            }
            groupName = rdnValue(rdns.get(rdns.size() - 1));
            for (int i = rdns.size() - 2; i > -1; i--) {
                compName = compName + "/" + rdnValue(rdns.get(i));
            }
        }

        // Convert changeType from JNDI to com.sun.identity.shared.ldap
        switch (type) {
        case NamingEvent.OBJECT_ADDED:
            type = ServiceListener.ADDED;
            break;
        case NamingEvent.OBJECT_REMOVED:
            type = ServiceListener.REMOVED;
            break;
        default:
            type = ServiceListener.MODIFIED;
        }

        // Get organization name
        String orgName = dn;
        if (globalConfig && orgConfig) {
            // Schema change, use base DN
            orgName = ServiceManager.getBaseDN();
        } else if ((index >= 0) && orgConfig) {
            // Get org name
            orgName = dn.substring(index + orgIndex + 1);
        }
        if (globalConfig) {
            notifyGlobalConfigChange(groupName, compName, type);
            if (SMSEntry.eventDebug.messageEnabled()) {
                SMSEntry.eventDebug.message(
                    "ServiceConfigManagerImpl(" + serviceName +
                    "):entryChanged Sending global config change " +
                    "notifications for DN "+ dn);
            }
        }
        if (orgConfig) {
            notifyOrgConfigChange(orgName, groupName, compName, type);
            if (SMSEntry.eventDebug.messageEnabled()) {
                SMSEntry.eventDebug.message(
                    "ServiceConfigManagerImpl(" + serviceName +
                    "):entryChanged Sending org config change " +
                    "notifications for DN " + dn);
            }
        }
    }

    void notifyGlobalConfigChange(String groupName, String comp, int type) {
        HashSet lObject = new HashSet();
        synchronized (listenerObjects) {
            lObject.addAll(listenerObjects.values());
        }
        Iterator items = lObject.iterator();
        while (items.hasNext()) {
            ServiceListener sl = (ServiceListener) items.next();
            try {
                sl.globalConfigChanged(serviceName, version, groupName,
                        comp, type);
            } catch (Throwable t) {
                SMSEntry.eventDebug.error("ServiceConfigManagerImpl(:" +
                        serviceName + ") notifyGlobalConfigChange Error " +
                        "sending notification to ServiceListener: " +
                        sl.getClass().getName(), t);
            }
        }
    }

    void notifyOrgConfigChange(String orgName, String groupName, String comp,
        int type) {
        HashSet lObject = new HashSet();
        synchronized (listenerObjects) {        
            lObject.addAll(listenerObjects.values());
        }
        Iterator items = lObject.iterator();
        while (items.hasNext()) {
            ServiceListener sl = (ServiceListener) items.next();
            try {
                sl.organizationConfigChanged(serviceName, version, orgName,
                        groupName, comp, type);
            } catch (Throwable t) {
                SMSEntry.eventDebug.error("ServiceConfigManagerImpl(:" +
                        serviceName + ") notifyOrgConfigChange Error " +
                        "sending notification to ServiceListener: " +
                        sl.getClass().getName(), t);
            }
        }
    }

    String constructServiceConfigDN(String groupName, String configName,
            String orgName) throws SMSException {
        StringBuilder sb = new StringBuilder(50);
        sb.append("ou=").append(groupName).append(SMSEntry.COMMA).append(
                configName).append("ou=").append(version)
                .append(SMSEntry.COMMA).append("ou=").append(serviceName)
                .append(SMSEntry.COMMA).append(SMSEntry.SERVICES_RDN).append(
                        SMSEntry.COMMA);
        if ((orgName == null) || (orgName.length() == 0)) {
            orgName = SMSEntry.baseDN;
        } else if (LDAPUtils.isDN(orgName)) {
            // Do nothing
        } else if (orgName.startsWith("/")) {
            orgName = DNMapper.orgNameToDN(orgName);
        } else {
            String[] args = { orgName };
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-invalid-org-name", args));
        }
        sb.append(orgName);
        return (sb.toString());
    }
    
    protected boolean isValid() {
        return (valid);
    }
    
    /**
     * Clears instance cache and deregisters listeners
     */
    private void clear() {
        valid = false;
        // Deregister only if there are no listener, else listeners
        // will get not get any notifications
        if ((listenerObjects == null) || listenerObjects.isEmpty()) {
            deregisterListener();
        }
        ssm = null;
        if (SMSEntry.cacheSMSEntries) {
            orgConfigs.clear();
            globalConfigs.clear();
        }
    }
    
    // @Override
    public int hashCode() {
        int hash = 4;
        hash = 29 * hash + (this.serviceName != null ?
            this.serviceName.hashCode() : 0);
        hash = 29 * hash + (this.version != null ?
            this.version.hashCode() : 0);
        return hash;
    }

    /**
     * Compares this object with the given object.
     * 
     * @param o
     *            object for comparison.
     * @return true if objects are equals.
     */
    public boolean equals(Object o) {
        if (o instanceof ServiceConfigManager) {
            ServiceConfigManagerImpl oscm = (ServiceConfigManagerImpl) o;
            if (serviceName.equals(oscm.serviceName)
                    && version.equals(oscm.version)) {
                return (true);
            }
        }
        return (false);
    }

    /**
     * Returns String representation of the service's name and version.
     * 
     * @return String representation of the service's name and version
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ServiceConfigManagerImpl: ").append(serviceName).append(
                " Version: ").append(version);
        return (sb.toString());
    }

    // ---------------------------------------------------------
    // Static Protected Methods
    // ---------------------------------------------------------
    protected static ServiceConfigManagerImpl getInstance(SSOToken token,
        String serviceName, String version) throws SSOException, SMSException {
        if (debug.messageEnabled()) {
            debug.message("ServiceConfigMgrImpl::getInstance: called: " +
                serviceName + "(" + version + ")");
        }
        // Construct the cache name, and check in cache
        String cName = ServiceManager.getCacheIndex(serviceName, version);
        ServiceConfigManagerImpl answer = getFromCache(
            cName, serviceName, version, token);
        if (answer != null) {
            return (answer);
        }
            
        // Not in cache, need to construct the entry and add to cache
        // Check if user has permissions to this object. This call will
        // throw an exception if the user does not have permissions
        checkAndUpdatePermission(cName, serviceName, version, token);
        
        // User has permissions,
        // Construct ServiceConfigManagerImpl and add to cache
            answer = new ServiceConfigManagerImpl(
                    token, serviceName, version);
            configMgrImpls.put(cName, answer);
        // Debug messages
        if (debug.messageEnabled()) {
            debug.message("ServiceConfigMgrImpl::getInstance: success: " +
                serviceName + "(" + version + ")");
        }
        return (answer);
    }

    private static ServiceConfigManagerImpl getFromCache(String cacheName,
        String sName, String version, SSOToken t)
        throws SMSException, SSOException {
        ServiceConfigManagerImpl answer = (ServiceConfigManagerImpl)
            configMgrImpls.get(cacheName);
        if ((answer != null) && (t != null)) {
            // Validate SCM
            if (!answer.isValid()) {
                configMgrImpls.remove(cacheName);
                answer = null;
            } else {
                // Check if the user has permissions
                Set principals = (Set) userPrincipals.get(cacheName);
                if ((principals == null) ||
                    !principals.contains(t.getTokenID().toString())) {
                    // Principal check not done
                    // Call to check and update permission will throw an
                    // exception if the user does not have permissions
                    if (debug.messageEnabled()) {
                        debug.message("ServiceConfigMgrImpl:getFromCache " +
                            "SN: " + sName + " found in cache. " +
                            "Check permission");
                    }
                    checkAndUpdatePermission(cacheName, sName, version, t);
                }
            }
        }
        return (answer);
    }

    private static boolean checkAndUpdatePermission(String cacheName,
        String sName, String version, SSOToken t)
        throws SMSException, SSOException {
        String dn = ServiceManager.getServiceNameDN(sName, version);
        // Check permissions for the SSOToken, throws exception if user
        // does not have permissions. If backend proxy is enabled, a read
        // operation must be performed
        if (SMSEntry.backendProxyEnabled) {
            CachedSMSEntry.getInstance(t, dn);
        } else {
            SMSEntry.getDelegationPermission(t, dn, SMSEntry.readActionSet);
        }
        
        // User has permissions, add principal to cache
       
            Set sudoPrincipals = (Set) userPrincipals.get(cacheName);
            if (sudoPrincipals == null) {
                sudoPrincipals = new LinkedHashSet(20);
                userPrincipals.put(cacheName, sudoPrincipals);
            }
            sudoPrincipals.add(t.getTokenID().toString());
            if (sudoPrincipals.size() > PRINCIPALS_CACHE_SIZE) {
                // Remove the first entry
                Iterator items = sudoPrincipals.iterator();
                if (items.hasNext()) {
                    items.next();
                    items.remove();
                }
            }
        // In the case failed permissions, exception would be thrown
        return (true);
    }

    static void clearCache() {
        // Clear the internal caches
        for (ServiceConfigManagerImpl sc : configMgrImpls.values()) {
        	sc.clear();
		}
        userPrincipals.clear();
    }

    private static Map<String,ServiceConfigManagerImpl> configMgrImpls = new ConcurrentHashMap<String, ServiceConfigManagerImpl>();

    private static Map<String,Set> userPrincipals = new ConcurrentHashMap<String, Set>();
    
    private static int PRINCIPALS_CACHE_SIZE = 20;

    private static Debug debug = SMSEntry.debug;
}
