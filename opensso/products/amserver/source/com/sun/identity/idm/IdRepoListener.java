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
 * $Id: IdRepoListener.java,v 1.16 2009/01/28 05:34:59 ww203982 Exp $
 *
 */
/**
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.idm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManager;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.controls.LDAPPersistSearchControl;
import com.sun.identity.shared.ldap.util.DN;

/**
 * Provides methods that can be called by IdRepo plugins to notify change
 * events. Used to update cache and also to send notifications to registered
 * listeners. Each IdRepo plugin will be given a unique instance of this object.
 * 
 * Additionally, this class maintains the configuration data for the IdRepo
 * plugin and also to store the SMS Service attributes for the organization.
 *
 * @supported.all.api
 */
public final class IdRepoListener {

    // Configuration data for the IdRepo plugin
    // Must have "realm" key to correctly send the notifications to clients
    private Map configMap = null;

    // Listener registed by JAXRPC Impl to send notifications
    private static IdEventListener remoteListener = null;

    private static Debug debug = Debug.getInstance("idrepoListener");

    // To serialize and deserialize configMap
    protected static SOAPClient sclient;
    
    // Configured Identity Types
    private static IdType[] defaultIdTypes;
    
    // Flags to check if caching is enabled and to clear them
    private static boolean cacheChecked;
    private static boolean cacheEnabled;
    private static IdServices idServices;

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.AMObjectListener#allObjectsChanged()
     */
    public void allObjectsChanged() {
        if (debug.messageEnabled()) {
            debug.message("IdRepoListener: allObjectsChanged Called!");
        }

        // Check if caching is enabled
        if (!cacheChecked) {
            idServices = IdServicesFactory.getDataStoreServices();
            if (idServices instanceof IdCachedServices) {
                // If Caching was enabled - then clear the cache!!
                cacheEnabled = true;
            }
            cacheChecked = true;
        }
        if (cacheEnabled) {
            // If Caching was enabled - then clear the cache!!
            ((IdCachedServices) idServices).clearCache();
        }

        // Get the list of listeners setup with idRepo
        String org = (String) configMap.get("realm");
        ArrayList list = (ArrayList) AMIdentityRepository.listeners.get(org);
        // Update any listeners registered with IdRepo
        if (list != null) {
            int size = list.size();
            for (int j = 0; j < size; j++) {
                IdEventListener l = (IdEventListener) list.get(j);
                l.allIdentitiesChanged();
            }
        }
        if (remoteListener != null) {
            remoteListener.allIdentitiesChanged();
        }
    }

    /**
     * 
     * This method has been deprecated as of OpenSSO Enterprise 8.0.
     * 
     * @param name name of the identity that changed
     * @param type change type i.e., add, delete, modify, etc.
     * @param cMap configuration map that contains realm and plugin-name
     *
     * @deprecated  As of Sun Java System Access Manager 7.1.
     */
    public void objectChanged(String name, int type, Map cMap) {
        objectChanged(name, null, type, cMap);
    }
    
    /**
     * Notification mechanism for IdRepo plugins to specify the identiy name
     * and identity type that has been changed.
     * 
     * @param name name of the identity that changed
     * @param idType IdType i.e., user, group, etc.
     * @param changeType change type i.e., add, delete, modify, etc.
     * @param cMap configuration map that contains realm and plugin-name
     */
    public void objectChanged(String name, IdType idType, int changeType,
        Map cMap) {
        if (debug.messageEnabled()) {
            debug.message("objectChanged called with IdType= name: " + name +
                " IdType: " + idType + " ChangeType: " + changeType +
                "\nConfigmap = " + cMap);
        }
        // Get the list of listeners setup with idRepo
        String org = (String) configMap.get("realm");
        ArrayList list = (ArrayList) AMIdentityRepository.listeners.get(org);

        // Check if caching is enabled
        if (!cacheChecked) {
            idServices = IdServicesFactory.getDataStoreServices();
            if (idServices instanceof IdCachedServices) {
                // If Caching was enabled - then clear the cache!!
                cacheEnabled = true;
            }
            cacheChecked = true;
        }
        
        if (name.length() > 0) {
            String[] changed = getChangedIds(name, idType, cMap);
            for (int i = 0; i < changed.length; i++) {

                if (cacheEnabled) {
                    ((IdCachedServices) idServices).dirtyCache(changed[i],
                        changeType, false, false, Collections.EMPTY_SET);
                }

                // Update any listeners registered with IdRepo
                if (list != null) {
                    int size = list.size();
                    for (int j = 0; j < size; j++) {
                        IdEventListener l = (IdEventListener) list.get(j);
                        switch (changeType) {
                        case OBJECT_CHANGED:
                        case OBJECT_ADDED:
                            l.identityChanged(changed[i]);
                            break;
                        case OBJECT_REMOVED:
                            l.identityDeleted(changed[i]);
                            break;
                        case OBJECT_RENAMED:
                            l.identityRenamed(changed[i]);
                        }
                    }
                }
                
                // Handle remote listener, should not be mixed with
                // IdRepo listeners, since it can null or empty
                if (remoteListener != null) {
                    switch (changeType) {
                        case OBJECT_CHANGED:
                        case OBJECT_ADDED:
                            remoteListener.identityChanged(changed[i]);
                            break;
                        case OBJECT_REMOVED:
                            remoteListener.identityDeleted(changed[i]);
                            break;
                        case OBJECT_RENAMED:
                            remoteListener.identityRenamed(changed[i]);
                    }
                }
            }
        }
    }

    public static void addRemoteListener(IdEventListener l) {
        remoteListener = l;
    }
    
    /*
     * Returns the configurations for the IdRepo plugins
     */
    public Map getConfigMap() {
        return configMap;
    }

    /*
     * Maintains the configurations for the IdRepo plugins
     */
    public void setConfigMap(Map cMap) {
        configMap = cMap;
    }

    /**
     * Stores service's dynamic attributes within the IdRepo plugin
     * configuration. In the current implementation changes to dynamic
     * attributes to LDAPv3Repo restart the plugin, since it triggers
     * a configuration change notification.
     * 
     * @param sName service name for which attributes are being set
     * @param attrs service synamic attributes
     * @throws com.sun.identity.idm.IdRepoException
     */
    public void setServiceAttributes(String sName, Map attrs)
            throws IdRepoException {
        String realm = (String) configMap.get("realm");
        String pluginName = (String) configMap.get("plugin-name");
        if (realm == null || pluginName == null) {
            AMIdentityRepository.debug.error(
                    "IdRepoListener.setServiveAttribute: realm or plugin name"
                    + " is null");
            Object[] args = { sName, IdType.ROLE.getName() };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "105", args);
        }
        try {
            SSOToken token = (SSOToken) AccessController
                    .doPrivileged(AdminTokenAction.getInstance());
            ServiceConfigManager scm = new ServiceConfigManager(token,
                    IdConstants.REPO_SERVICE, "1.0");
            ServiceConfig sc = scm.getOrganizationConfig(realm, null);
            if (sc == null) {
                return;
            }
            
            ServiceConfig subConfig = sc.getSubConfig(pluginName);
            if (subConfig == null) {
                return;
            }
            Map attributes = subConfig.getAttributes();
            Set vals = (Set) attributes.get(IdConstants.SERVICE_ATTRS);
            if (vals == null || vals == Collections.EMPTY_SET) {
                vals = new HashSet();
            }
            if (sclient == null) {
                sclient = new SOAPClient("dummy");    
            }
            String mapStr = sclient.encodeMap("result", attrs);
            vals = new HashSet();
            vals.add(mapStr);
            attributes.put(IdConstants.SERVICE_ATTRS, vals);
            subConfig.setAttributes(attributes);
        } catch (SMSException smse) {
            AMIdentityRepository.debug.error(
                    "IdRepoListener: Unable to set service attributes", smse);
            Object[] args = { sName, IdType.ROLE.getName() };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "105", args);
        } catch (SSOException ssoe) {
            AMIdentityRepository.debug.error(
                    "IdRepoListener: Unable to set service attributes", ssoe);
            Object[] args = { sName, IdType.ROLE.getName() };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "105", args);
        }
    }

    private String[] getChangedIds(String name, IdType type, Map cMap) {
        int size = IdUtils.supportedTypes.size();
        // If configMap is null, then this is a "remote" cache update
        if ((cMap == null) || cMap.isEmpty()) {
            String ct[] = new String[1];
            if (DN.isDN(name)) {
                // Name should be the universal id
                ct[0] = name;
            } else {
                if (type == null) {
                    // Default to user
                    type = IdType.USER;
                }
                ct[0] = "id=" + name + ",ou=" + type.getName() + "," +
                        ServiceManager.getBaseDN();
            }
            return ct;
        }
        String changedTypes[] = null;
        IdType types[] = null;
        if (type == null) {
            changedTypes = new String[size];
            if (defaultIdTypes  == null) {
                Set idtypes = IdUtils.supportedTypes;
                defaultIdTypes = new IdType[idtypes.size()];
                defaultIdTypes = (IdType[]) idtypes.toArray(defaultIdTypes);
            }
            types = defaultIdTypes;
        } else {
            changedTypes = new String[1];
            types = new IdType[1];
            types[0] = type;
        }
        String realm = (String) cMap.get("realm");
        String Amsdk = (String) cMap.get("amsdk");
        boolean isAmsdk = (Amsdk == null) ? false : true;

        for (int i = 0; i < types.length; i++) {
            IdType itype = types[i];
            String n = DN.isDN(name) ? LDAPDN.explodeDN(name, true)[0] : name;
            String id = "id=" + n + ",ou=" + itype.getName() + "," + realm;
            if (isAmsdk) {
                id = id + ",amsdkdn=" + name;
            }
            changedTypes[i] = id;
        }
        return changedTypes;
    }
    
    // Constants for change type recevied from the IdRepo plugins
    
    /**
     * Represents an object addition event type.
     */
    public static final int OBJECT_ADDED = LDAPPersistSearchControl.ADD;

    /**
     * Represents an object change event type.
     */
    public static final int OBJECT_CHANGED = LDAPPersistSearchControl.MODIFY;

    /**
     * Represents an object removal event type.
     */
    public static final int OBJECT_REMOVED = LDAPPersistSearchControl.DELETE;

    /**
     * Represents an object renaming event type.
     */
    public static final int OBJECT_RENAMED = LDAPPersistSearchControl.MODDN;
}
