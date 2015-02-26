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
 * $Id: FSAccountManager.java,v 1.5 2008/06/25 05:46:39 qcheng Exp $
 *
 */

package com.sun.identity.federation.accountmgmt;

import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.datastore.DataStoreProviderManager;
import com.sun.identity.saml.assertion.NameIdentifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * This class is used for storing & retrieving user account's federation
 * specific information.
 */
public class FSAccountManager {
    
    /**
     * static variable to store AccountManager .
     */
    private static Map instanceMap = new HashMap();
   
    /**
     * additional SP filter to check, this is for the case when two SP
     * are federated with same IDP
     */
    private String SP_FILTER = null;
    private String SP_PROVIDER_ID;

    private IDFFMetaManager metaManager =
        FSUtils.getIDFFMetaManager();
    private DataStoreProvider provider = null;
    private FSUserProvider userProvider = null;

    /**
     * Default Constructor.
     * @param metaAlias hosted provider's meta alias
     * @throws FSAccountMgmtException if error occurred.
     */
    private FSAccountManager(String metaAlias)
        throws FSAccountMgmtException
    {
        try {
            provider = DataStoreProviderManager.getInstance().
                getDataStoreProvider(IFSConstants.IDFF);
            String role = metaManager.getProviderRoleByMetaAlias(metaAlias);
            String realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
            String hostedEntityID = 
                metaManager.getEntityIDByMetaAlias(metaAlias);
            BaseConfigType hostedConfig = null;
            if (role != null && role.equalsIgnoreCase(IFSConstants.IDP)) {
                hostedConfig = 
                    metaManager.getIDPDescriptorConfig(realm, hostedEntityID);
            } else if (role != null &&
                role.equalsIgnoreCase(IFSConstants.SP))
            {
                hostedConfig = 
                    metaManager.getSPDescriptorConfig(realm, hostedEntityID);
                SP_PROVIDER_ID = hostedEntityID;
                SP_FILTER = "|" + SP_PROVIDER_ID + "|";
            }
            if (hostedConfig == null) {
                throw new FSAccountMgmtException(
                    IFSConstants.NULL_HOSTED_CONFIG, null);
            }
            String userPClass = IDFFMetaUtils.getFirstAttributeValueFromConfig(
                hostedConfig, IFSConstants.FS_USER_PROVIDER_CLASS);
            if (userPClass == null || userPClass.length() == 0) {
                userPClass = IFSConstants.FS_USER_PROVIDER_DEFAULT;
            }
            userProvider = (FSUserProvider)
                Class.forName(userPClass).newInstance();
            userProvider.init(hostedEntityID);
        } catch (Exception de) {
            FSUtils.debug.error(
                "FSAccountManager.getInstance() : Exception: ", de);
            throw new FSAccountMgmtException(de.getMessage());
        }
    }
   
    /**
     * Gets an Object for FSAccountManager Class.
     * Used to instantiate the Class.
     * @param metaAlias meta alias of hosted provider
     * @return FSAccountManager Object.
     * @throws FSAccountMgmtException if error occurred.
     */
    public static FSAccountManager getInstance(String metaAlias) 
        throws FSAccountMgmtException
    {
        if ((metaAlias == null) || (metaAlias.length() == 0)) {
            FSUtils.debug.error(
                "FSAccountManager.getInstance: meta aliasis null.");
            throw new FSAccountMgmtException(
                IFSConstants.NULL_META_ALIAS, null);
        }

        FSAccountManager manager = 
            (FSAccountManager) instanceMap.get(metaAlias);
        if (manager == null) {
            synchronized(instanceMap) {
                manager = (FSAccountManager) instanceMap.get(metaAlias);
                if (manager == null) {
                    manager = new FSAccountManager(metaAlias);
                    instanceMap.put(metaAlias, manager);
                }
            }
        }
        return manager;
    }
    
    /**
     * Stores Account's federation Info in data store.
     * @param userID user id
     * @param fedInfo  Account federation info as FSAccountFedInfo object.
     * @param fedInfoKey Account Fed Info Key which contains NameSpace
     *  and opaque handle sent/received.
     * @throws FSAccountMgmtException if illegal argument passed.
     */
    public void writeAccountFedInfo(
        String userID, 
        FSAccountFedInfoKey fedInfoKey,
        FSAccountFedInfo fedInfo)
        throws FSAccountMgmtException
    {
        FSUtils.debug.message(
            "FSAccountManager.writeAccountFedInfo() : called");
        if (userID == null) {
            FSUtils.debug.error("FSAccountManager.writeAccountFedInfo():" +
                "Invalid Argument : user ID is NULL");
            throw new FSAccountMgmtException(IFSConstants.NULL_USER_DN, null);
        }

        if (fedInfoKey == null) {
            FSUtils.debug.error("FSAccountManager.writeAccountFedInfo():" +
                "Invalid Argument : FedInfo key is NULL");
            throw new FSAccountMgmtException(
                IFSConstants.NULL_FED_INFO_KEY_OBJECT,null);
        }
        if (fedInfo == null) {
            FSUtils.debug.error("FSAccountManager.writeAccountFedInfo():" +
                "Invalid Argument : FedInfo is NULL");
            throw new FSAccountMgmtException(
                IFSConstants.NULL_FED_INFO_OBJECT, null);
        }
        
        try {
            Set attrNames = new HashSet();
            attrNames.add(FSAccountUtils.USER_FED_INFO_KEY_ATTR);
            attrNames.add(FSAccountUtils.USER_FED_INFO_ATTR);
            Map attrsMap = provider.getAttributes(userID, attrNames);
            if (attrsMap == null) {
                attrsMap = new HashMap();
            }
 
            Set existFedInfoKeySet = (Set) attrsMap.get(
                FSAccountUtils.USER_FED_INFO_KEY_ATTR);
            if (existFedInfoKeySet == null) {
                existFedInfoKeySet = new HashSet();
            } else if (!existFedInfoKeySet.isEmpty()) {
                Iterator i = existFedInfoKeySet.iterator();
                String existFedInfoKeyStr = "";
                String filter = FSAccountUtils.createFilter(fedInfoKey);
                while(i.hasNext()) {
                    existFedInfoKeyStr = (String)i.next();
                    if (existFedInfoKeyStr.indexOf(filter) >= 0) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSAccountManager.writeAccountFedInfo():" + 
                                "AccountFedInfo Key Already Exists, " +
                                "will overwrite.");
                        }
                        existFedInfoKeySet.remove(existFedInfoKeyStr);
                        break;
                    }
                }
            }
            String fedInfoKeyStr = FSAccountUtils.objectToKeyString(fedInfoKey);
            existFedInfoKeySet.add(fedInfoKeyStr);
            
            Map attrMap = new HashMap();
            attrMap.put(FSAccountUtils.USER_FED_INFO_KEY_ATTR, 
                existFedInfoKeySet);
            
            Set existFedInfoSet = (Set) attrsMap.get(
                FSAccountUtils.USER_FED_INFO_ATTR);
            if (existFedInfoSet == null) {
                existFedInfoSet = new HashSet();
            } else if (!existFedInfoSet.isEmpty()) {
                Iterator i = existFedInfoSet.iterator();
                String existFedInfoStr = "";
                String filter = FSAccountUtils.createFilter(fedInfoKey);
                while(i.hasNext()) {
                    existFedInfoStr = (String)i.next();
                    if (existFedInfoStr.indexOf(filter) >= 0) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSAccountManager.writeAccountFedInfo():" + 
                                " AccountFedInfo Already Exists, will " +
                                "overwrite");
                        }
                        existFedInfoSet.remove(existFedInfoStr);
                        break;
                    }
                }
            }
            String fedInfoStr = FSAccountUtils.objectToInfoString(fedInfo);
            existFedInfoSet.add(fedInfoStr);
            
            attrMap.put(FSAccountUtils.USER_FED_INFO_ATTR, existFedInfoSet);
            
            provider.setAttributes(userID, attrMap);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSAccountManager.writeAccountFedInfo(): Key : " +
                    fedInfoKeyStr + ", Value : " +
                    fedInfoStr + " : Added ");
            }

            String[] args = {userID, fedInfoStr, fedInfoKeyStr };
            LogUtil.access(Level.INFO,LogUtil.WRITE_ACCOUNT_FED_INFO, args);
        } catch (DataStoreProviderException de) {
            FSUtils.debug.error(
                "FSAccountManager.writeAccountFedInfo(): Exception: ", de);
            throw new FSAccountMgmtException(de.getMessage());
        }
    }

    /**
     * Removes Account's federation Info in data store.
     * @param userID user id 
     * @param fedInfo  Account federation info as FSAccountFedInfo object.
     * @throws FSAccountMgmtException if illegal argument passed.
     * TODO, this may remove the wrong info key, as two
     * SP could federation with same IDP
     * use the one with providerID parameter
     */
    public void removeAccountFedInfo(String userID, FSAccountFedInfo fedInfo)
        throws FSAccountMgmtException 
    {
        if (fedInfo == null) {
            FSUtils.debug.error("FSAccountManager.removeAccountFedInfo():" +
                "Invalid Argument : FedInfo is NULL");
            throw new
                FSAccountMgmtException(IFSConstants.NULL_FED_INFO_OBJECT, null);
        }
        try {
            Set existFedInfoSet = provider.getAttribute(
                userID, FSAccountUtils.USER_FED_INFO_ATTR);
            if (existFedInfoSet == null) {
                existFedInfoSet = new HashSet();
            } else if (!existFedInfoSet.isEmpty()) {
                String fedInfoStr = FSAccountUtils.objectToInfoString(fedInfo);
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Account Info to be removed:" 
                    + fedInfoStr);
                }
                if (fedInfoStr != null && existFedInfoSet.contains(fedInfoStr))
                {
                    existFedInfoSet.remove(fedInfoStr);
                }
                HashMap attrMap = new HashMap();
                attrMap.put(FSAccountUtils.USER_FED_INFO_ATTR, existFedInfoSet);
                provider.setAttributes(userID, attrMap);
            }
        } catch (Exception se) {
            FSUtils.debug.error(
                "FSAccountManager.removeAccountFedInfo(): Exception: ", se);
            throw new FSAccountMgmtException(se.getMessage());
        }

    }
    
    /**
     * Stores Account's federation Info in data store.
     * @param userID user id
     * @param fedInfo  Account federation info as FSAccountFedInfo object.
     * @param fedInfoKey Account Fed Info Key which contains NameSpace
     * & opaque handle sent/received.
     * @param oldFedInfoKey Account Fed Info Key which contains NameSpace
     * & opaque handle sent/received, which will be removed.
     * @throws FSAccountMgmtException if illegal argument passed.
     */
    public void writeAccountFedInfo(
        String userID, 
        FSAccountFedInfoKey fedInfoKey,
        FSAccountFedInfo fedInfo,
        FSAccountFedInfoKey oldFedInfoKey)
        throws FSAccountMgmtException
    {
        writeAccountFedInfo(userID, fedInfoKey, fedInfo);
    } 

    /**
     * Removes Account's federation Info Key in data store.
     * @param userID user id
     * @param fedInfoKey Account Fed Info Key which contains NameSpace
     * & opaque handle sent/received, which will be removed.
     * @throws FSAccountMgmtException if illegal argument passed.
     */
    public void removeAccountFedInfoKey(
        String userID, 
        FSAccountFedInfoKey fedInfoKey)
        throws FSAccountMgmtException
    {
        FSUtils.debug.message(
            "FSAccountManager.removeAccountFedInfoKey():called");
        if (userID == null) {
            FSUtils.debug.error("FSAccountManager.removeAccountFedInfoKey():"
                 + "Invalid Argument : user ID is NULL");
            throw new FSAccountMgmtException(IFSConstants.NULL_USER_DN, null);
        }

        if (fedInfoKey == null) {
            FSUtils.debug.error("FSAccountManager.removeAccountFedInfoKey():"
                + "Invalid Argument : FedInfo key is NULL");
            throw new FSAccountMgmtException(
                IFSConstants.NULL_FED_INFO_KEY_OBJECT, null);
        }
        try {
            Map attrMap = new HashMap();
            
            Set existFedInfoKeySet = provider.getAttribute(
                userID, FSAccountUtils.USER_FED_INFO_KEY_ATTR);
            if (existFedInfoKeySet != null && !existFedInfoKeySet.isEmpty()) {
                Iterator i = existFedInfoKeySet.iterator();
                String existFedInfoKeyStr = "";
                String filter = FSAccountUtils.createFilter(fedInfoKey);
                while(i.hasNext()) {
                    existFedInfoKeyStr = (String)i.next();
                    if (existFedInfoKeyStr.indexOf(filter) >= 0) { 
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSAccountManager.removeAccountFedInfoKey():" +
                                "Account Fed Info Key Exists, will remove it");
                        }
                        existFedInfoKeySet.remove(existFedInfoKeyStr);
                        attrMap.put(FSAccountUtils.USER_FED_INFO_KEY_ATTR, 
                            existFedInfoKeySet);
                        provider.setAttributes(userID, attrMap);
                        break;
                    }
                }
            }
        } catch (DataStoreProviderException ame) {
            FSUtils.debug.error(
                "FSAccountManager.removeAccountFedInfoKey():Exception:", ame);
            throw new FSAccountMgmtException(ame.getMessage());
        }
    }
    
    /**
     * Removes Account's federation Info in data store for given providerID
     * in fedInfo object.
     * @param userID user id
     * @param fedInfoKey Account Fed Info Key which contains NameSpace
     * & opaque handle sent/received.
     * @param providerID Remote ProviderID value.
     * @throws FSAccountMgmtException - If Account fed info is not found for 
     * given user & given ProviderID.
     */
    public void removeAccountFedInfo(
        String userID, 
        FSAccountFedInfoKey fedInfoKey,
        String providerID)
        throws FSAccountMgmtException
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSAccountManager.removeAccountFedInfo(): userID=" + userID +
                ", infoKey=" + FSAccountUtils.createFilter(fedInfoKey) + 
                ", providerID=" + providerID);
        }
        if (fedInfoKey == null) {
            FSUtils.debug.error("FSAccountManager.removeAccountFedInfo():" +
                "Invalid Argument : FedInfo key is NULL");
            throw new FSAccountMgmtException(
                IFSConstants.NULL_FED_INFO_KEY_OBJECT, null);
        }
        if ((providerID == null) || (providerID.length() <= 0)) {
            FSUtils.debug.error("FSAccountManager.removeAccountFedInfo():" +
                "Invalid Argument : providerID is NULL");
            throw new
                FSAccountMgmtException(IFSConstants.NULL_PROVIDER_ID, null);
        }
        
        if (userID == null) {
            FSUtils.debug.error("FSAccountManager.removeAccountFedInfo():"
                + "Invalid Argument : user ID is NULL");
            throw new FSAccountMgmtException(IFSConstants.NULL_USER_DN, null);
        }
        try {
            Map attrMap = new HashMap();
            boolean found = false;
            
            Set existFedInfoKeySet = provider.getAttribute(
                userID, FSAccountUtils.USER_FED_INFO_KEY_ATTR);
            String existFedInfoKeyStr = "";
            if (existFedInfoKeySet != null && !existFedInfoKeySet.isEmpty()) {
                String filter = FSAccountUtils.createFilter(fedInfoKey);
                Iterator i = existFedInfoKeySet.iterator();
                while(i.hasNext()) {
                    existFedInfoKeyStr = (String)i.next();
                    if (existFedInfoKeyStr.indexOf(filter) >= 0) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSAccountManager.removeAccountFedInfo():" +
                                "Account Fed Info Key Exists, will remove it");
                        }
                        existFedInfoKeySet.remove(existFedInfoKeyStr);
                        attrMap.put(FSAccountUtils.USER_FED_INFO_KEY_ATTR, 
                            existFedInfoKeySet);
                        found = true;
                        break;
                    }
                }
            }

            String nameIDValue = fedInfoKey.getName();
            Set existFedInfoSet = provider.getAttribute(
                userID, FSAccountUtils.USER_FED_INFO_ATTR);
            if (existFedInfoSet != null && !existFedInfoSet.isEmpty()) {
                Iterator i = existFedInfoSet.iterator();
                String existFedInfoStr = "";
                String filter = FSAccountUtils.createFilter(providerID);
                while(i.hasNext()) {
                    existFedInfoStr = (String)i.next();
                    if ((existFedInfoStr.indexOf(filter) >= 0) &&
                        (existFedInfoStr.indexOf(nameIDValue) >= 0))
                    {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSAccountManager.removeAccountFedInfo():" +
                                "Account Fed Info Exists, will remove it");
                        }
                        existFedInfoSet.remove(existFedInfoStr);
                        attrMap.put(FSAccountUtils.USER_FED_INFO_ATTR, 
                            existFedInfoSet);
                        found = true;
                        break;
                    }
                }
            }
            if (found) {
                provider.setAttributes(userID, attrMap);
                String[] args = {userID, providerID, existFedInfoKeyStr };
                LogUtil.access(
                    Level.INFO, LogUtil.REMOVE_ACCOUNT_FED_INFO, args);
            } else {
                FSUtils.debug.error("FSAccountManager.removeAccountFedInfo():" +
                    "Account Federation Info not Found");
                throw new FSAccountMgmtException(
                    IFSConstants.ACT_FED_INFO_NOT_FOUND,null);
            }
        } catch (DataStoreProviderException ame) {
            FSUtils.debug.error(
                "FSAccountManager.removeAccountFedInfo():Exception:", ame);
            throw new FSAccountMgmtException(ame.getMessage());
        }
    }
    
    /**
     * Reads Account's federation Info from data store for given 
     * providerID and returns value as fedInfo object.
     * Returns null if value not found for given providerID
     * @param  userID user ID.
     * @param providerID Remote ProviderID value.
     * @return Account's federation Info.
     * Null if no Account Federation info value for given providerID.
     * @throws FSAccountMgmtException if an error occurred.
     */
    public FSAccountFedInfo readAccountFedInfo(
        String userID,
        String providerID) 
        throws FSAccountMgmtException 
    {
        return readAccountFedInfo(userID, providerID, null);
    }

    /**
     * Reads Account's federation Info from data store for given 
     * providerID and returns value as fedInfo object.
     * Returns null if value not found for given providerID
     * @param  userID user ID.
     * @param providerID Remote ProviderID value.
     * @param nameIDValue fedinfo with this name ID value is to be found.
     * @return Account's federation Info.
     * Null if no Account Federation info value for given providerID.
     * @throws FSAccountMgmtException if an error occurred.
     */
    public FSAccountFedInfo readAccountFedInfo(
        String userID,
        String providerID,
        String nameIDValue) 
        throws FSAccountMgmtException 
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSAccountManager.readAccountFedInfo() : user=" + userID +
                ", providerID=" + providerID + ", nameIDValue=" + nameIDValue);
        }
        if (userID == null) {
            FSUtils.debug.error("FSAccountManager.readAccountFedInfo():" +
                "Invalid Argument : user ID is NULL");
            throw new FSAccountMgmtException(IFSConstants.NULL_USER_DN, null);
        }

        if ((providerID == null) || (providerID.length() <= 0)) {
            FSUtils.debug.error("FSAccountManager.readAccountFedInfo():" +
                "Invalid Argument : providerID is NULL");
            throw new
                FSAccountMgmtException(IFSConstants.NULL_PROVIDER_ID, null);
        }
        Set existFedInfoSet = null;
        try {
            existFedInfoSet = provider.getAttribute(
                userID, FSAccountUtils.USER_FED_INFO_ATTR);
        } catch (DataStoreProviderException ame) {
            FSUtils.debug.error(
                "FSAccountManager.readAccountFedInfo():Exception:", ame);
            throw new FSAccountMgmtException(ame.getMessage());
        }
        
        if (existFedInfoSet != null && !existFedInfoSet.isEmpty()) {
            String filter = FSAccountUtils.createFilter(providerID);
            Iterator i = existFedInfoSet.iterator();
            while(i.hasNext()) {
                String existFedInfoStr = (String)i.next();
                if (existFedInfoStr.indexOf(filter) >= 0 && 
                    (SP_FILTER == null || 
                        existFedInfoStr.indexOf(SP_FILTER) >= 0) &&
                    (nameIDValue == null ||
                        existFedInfoStr.indexOf(nameIDValue) >= 0)) 
                {
                    // accountFedInfo exists for given providerID
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSAccountManager.readAccountFedInfo(): " + 
                            " value found: " + existFedInfoStr);
                    }
                    FSAccountFedInfo afi = 
                        FSAccountUtils.stringToObject(existFedInfoStr);
                    if (!afi.isFedStatusActive()) {
                        return null;
                    }
                    return afi;
                }
            }
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSAccountManager.readAccountFedInfo(): value with user: " +
                userID + " and providerID : " + providerID + " not found");
        }

        return null;
    }

    /**
     * Reads All Account's federation Info from data store for given
     * user identity and returns a Set of ProviderIDs with which user
     * is federated (FedStatus is Active).
     * @param userID user identity
     * @return Set of ProviderIDs with which user is federated (FedStatus
     * is Active).
     * @throws FSAccountMgmtException if an error occurred.
     */
    public Set readAllFederatedProviderID (String userID) 
        throws FSAccountMgmtException
    {
        if (SP_PROVIDER_ID != null && SP_PROVIDER_ID.length() != 0) {
            return readAllFederatedProviderID(SP_PROVIDER_ID, userID);
        } else {
            return readAllFederatedProviderID(null, userID);
        }
    }

    /**
     * Reads All Account's federation Info from data store for given
     * user identity and providerID. Returns a Set of ProviderIDs 
     * with which user is federated (FedStatus is Active).
     * @param userID user identity
     * @param providerID local provider ID
     * @return Set of ProviderIDs with which user is federated (FedStatus
     *  is Active).
     * @throws FSAccountMgmtException if error occurred.
     */
    public Set readAllFederatedProviderID(String providerID, String userID) 
        throws FSAccountMgmtException 
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSAccountManager.readAllFedProviderID() : userID=" + userID +
                ", providerID=" + providerID);
        }

        if (userID == null) {
            FSUtils.debug.error("FSAccountManager.readAllFederatedProviderID()"
                + ":Invalid Argument : user ID is NULL");
            throw new FSAccountMgmtException(IFSConstants.NULL_USER_DN, null);
        }

        Set existFedInfoSet = null;
        try {
            existFedInfoSet = provider.getAttribute(
                userID, FSAccountUtils.USER_FED_INFO_ATTR);
        } catch (DataStoreProviderException ame) {
            FSUtils.debug.error(
                "FSAccountManager.readAllFederatedProviderID():Exception:",
                ame);
            throw new FSAccountMgmtException(ame.getMessage());
        }
        
        Set providerIDSet = new HashSet();
        if (existFedInfoSet != null && !existFedInfoSet.isEmpty()) {
            Iterator i = existFedInfoSet.iterator();
            String existFedInfoStr = "";
            while(i.hasNext()) {
                existFedInfoStr = (String)i.next();
                FSAccountFedInfo afi = 
                    FSAccountUtils.stringToObject(existFedInfoStr);
                if (afi.isFedStatusActive()) { 
                    if (providerID == null) { 
                        providerIDSet.add(afi.getProviderID());
                    } else if (
                        existFedInfoStr.indexOf("|" + providerID + "|") != -1) 
                    {
                        providerIDSet.add(afi.getProviderID());
                    }
                }
            }
        }
        return providerIDSet;
    }

    /**
     * Returns true/false if Account's federation Status is Active / Inactive
     * for given providerID.
     * @param userID user identity
     * @param providerID Remote ProviderID value.
     * @return true/false if Account's federation Status is Active / Inactive
     *  for given providerID.
     * @throws FSAccountMgmtException - If Account fed info is not found for 
     *  given user & given ProviderID.
     */
    public boolean isFederationActive(
        String userID, 
        String providerID)
        throws FSAccountMgmtException 
    {
        FSUtils.debug.message("FSAccountManager.isFederationActive() : called");

        if (userID == null) {
            FSUtils.debug.error("FSAccountManager.isFederationActive():" +
                "Invalid Argument : user ID is NULL");
            throw new FSAccountMgmtException(IFSConstants.NULL_USER_DN, null);
        }

        if ((providerID == null) || (providerID.length() <= 0)) {
            FSUtils.debug.error("FSAccountManager.isFederationActive() : " +
                "Invalid Argument : ProviderID is NULL");
            throw new FSAccountMgmtException(
                IFSConstants.NULL_PROVIDER_ID, null);
        }
        Set existFedInfoSet = null;
        try {
            existFedInfoSet = provider.getAttribute(
                userID, FSAccountUtils.USER_FED_INFO_ATTR);
        } catch (DataStoreProviderException ame) {
            FSUtils.debug.error(
                "FSAccountManager.isFederationActive() :Exception: ", ame);
            throw new FSAccountMgmtException(ame.getMessage());
        }
        
        if (existFedInfoSet != null && !existFedInfoSet.isEmpty()) {
            String filter = FSAccountUtils.createFilter(providerID);
            Iterator i = existFedInfoSet.iterator();
            while(i.hasNext()) {
                String existFedInfoStr = (String)i.next();
                if (existFedInfoStr.indexOf(filter) >= 0) {
                    // accountFedInfo exists for given providerID
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSAccountManager.isFederationActive(): " +
                            "value found: " + existFedInfoStr);
                    }
                    FSAccountFedInfo afi = 
                        FSAccountUtils.stringToObject(existFedInfoStr);
                    if (afi.isFedStatusActive()) {
                        return true;
                    }
                    return false;
                }
            }
        }
        FSUtils.debug.error("FSAccountManager.isFederationActive() : " +
            "Account Federation Info not Found");
        throw new FSAccountMgmtException(
            IFSConstants.ACT_FED_INFO_NOT_FOUND,null);
    }
   
    /**
     * Returns true If Any Active federation is found where idpRole is true
     * means local deployment worked as SP in that federation and federation
     * is still Active.
     * @param userID user id
     * @return true If Any Active federation is found where idpRole is true
     *  means local deployment worked as SP in that federation and 
     *  federation is still Active.
     * @throws FSAccountMgmtException - If Account fed info is not found for 
     *  given user.
     */
    public boolean hasAnyActiveFederation(
        String userID)
        throws FSAccountMgmtException 
    { 
        FSUtils.debug.message(
            "FSAccountManager.hasAnyActiveFederation():called");
        if (userID == null) {
            FSUtils.debug.error("FSAccountManager.hasAnyActiveFederation():" +
                "Invalid Argument : user ID is NULL");
            throw new FSAccountMgmtException(IFSConstants.NULL_USER_DN, null);
        }

        Set existFedInfoSet = null;
        try {
            existFedInfoSet = provider.getAttribute(
                userID, FSAccountUtils.USER_FED_INFO_ATTR);
        } catch (DataStoreProviderException ame) {
            FSUtils.debug.error(
                "FSAccountManager.hasAnyActiveFederation():Exception: ", ame);
            throw new FSAccountMgmtException(ame.getMessage());
        }
        
        if (existFedInfoSet != null && !existFedInfoSet.isEmpty()) {
            Iterator i = existFedInfoSet.iterator();
            String existFedInfoStr = "";
            while(i.hasNext()) {
                existFedInfoStr = (String)i.next();
                FSAccountFedInfo afi = 
                    FSAccountUtils.stringToObject(existFedInfoStr);
                // If Any Active federation is found where idpRole is true, 
                // return true.
                // Means local deployment worked as SP in that federation.
                if  (afi.isFedStatusActive() && afi.isRoleIDP()) {
                    return true;
                }
            }
            return false;
        }
        // return false in case user account federation info not found
        // since all federtation info will be cleaned up once terminated
        return false;
    }
    
    /**
     * Searches user with given combination of ProviderID & Opaque handle
     * in Default Organization.
     * @param fedInfoKey Account Fed Info Key which contains NameSpace
     * & opaque handle sent/received.
     * @param env - Extra parameters that can be used for user mapping.
     * @throws FSAccountMgmtException - If Unable to get Organization
     * @return User DN if user found with given combination else returns null.
     * If Some error occurs returns null.
     */
    public String getUserID(
        FSAccountFedInfoKey fedInfoKey,
        Map env)
        throws FSAccountMgmtException
    {
        return getUserID(fedInfoKey, null, env);
    }

    /**
     * Searches user with given combination of ProviderID & Opaque handle
     * in given Organization.
     * @param fedInfoKey Account Fed Info Key which contains NameSpace
     * & opaque handle sent/received.
     * @param orgDN organization DN.
     * @param env Extra parameters that can be used for user mapping.
     * @throws FSAccountMgmtException - If Unable to get Organization.
     * @return User DN if user found with given combination else returns null.
     * If Some error occurs returns null.
     */
    public String getUserID(
        FSAccountFedInfoKey fedInfoKey,
        String orgDN,
        Map env)
        throws FSAccountMgmtException
    {
        FSUtils.debug.message("FSAccountManager.getUserID() : called");
        if (fedInfoKey == null) {
            FSUtils.debug.error("FSAccountManager.getUserID():" +
                "Invalid Argument : fedInfoKey is NULL");
            throw new FSAccountMgmtException(
                IFSConstants.NULL_FED_INFO_KEY_OBJECT, null);
        }

        Map avPairs = new HashMap();
        Set valueSet = new HashSet();
        valueSet.add(FSAccountUtils.objectToKeyString(fedInfoKey));

        avPairs.put(FSAccountUtils.USER_FED_INFO_KEY_ATTR, valueSet);
        return userProvider.getUserID(orgDN, avPairs, env);
    }

   /**
    * Gets the user by using a given search filter.
    * @param avPair Attribute Value Pair to be used in finding the user.
    * @param orgDN Organization DN.
    * @param env Extra parameters that can be used for user mapping.
    * @exception FSAccountMgmtException if an error occurred.
    */
    public String getUserID(
        Map avPair,
        String orgDN,
        Map env)
        throws FSAccountMgmtException
    {
        return userProvider.getUserID(orgDN, avPair, env);
    }

}
