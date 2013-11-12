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
 * $Id: DirectoryManagerImpl.java,v 1.20 2009/07/02 20:26:16 hengming Exp $
 *
 */

/**
 * Portions Copyrighted 2011-2013 ForgeRock AS
 */
package com.iplanet.am.sdk.remote;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iplanet.am.sdk.AMDirectoryAccessFactory;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObjectListener;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.common.IComplianceServices;
import com.iplanet.am.sdk.common.IDCTreeServices;
import com.iplanet.am.sdk.common.IDirectoryServices;
import com.iplanet.services.comm.server.PLLServer;
import com.iplanet.services.comm.server.SendNotificationException;
import com.iplanet.services.comm.share.Notification;
import com.iplanet.services.comm.share.NotificationSet;
import com.iplanet.services.naming.ServerEntryNotFoundException;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.SearchControl;
import com.iplanet.ums.SortKey;
import com.sun.identity.idm.server.IdRepoJAXRPCObjectImpl;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSUtils;


public class DirectoryManagerImpl extends IdRepoJAXRPCObjectImpl implements
    AMObjectListener {
    
    protected static Debug debug = Debug.getInstance("amProfile_Server");
    
    protected static SSOTokenManager tm;
    
    protected static boolean initialized;
    
    // Handle to all the new DirectoryServices implementations.
    protected static IDirectoryServices dsServices;
    
    protected static IDCTreeServices dcTreeServices;
    
    protected static IComplianceServices complianceServices;
    
    // Cache of modifications for last 30 minutes & notification URLs    
    static LinkedList cacheIndices = new LinkedList();
        
    static HashMap cache = null;
        
    static Map<String, URL> notificationURLs = new HashMap<String, URL>();
    
    public DirectoryManagerImpl() {
        // Empty constructor
    }
    
    private static void initialize_cache() {
        initialize_cacheSize();
        if (cache == null && cacheSize > 0) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl::initialize_cache EventNotification cache size is set to " + cacheSize);
            }
            cache = new HashMap(cacheSize);
        }
    }

    protected void initialize() throws RemoteException {
        initialize_idrepo();
        if (initialized) {
            return;
        }
        
        dsServices = AMDirectoryAccessFactory.getDirectoryServices();
        dcTreeServices = AMDirectoryAccessFactory.getDCTreeServices();
        complianceServices = AMDirectoryAccessFactory.getComplianceServices();
        
        // Get TokenManager and register this class for events
        try {
            tm = SSOTokenManager.getInstance();
            dsServices.addListener((SSOToken) AccessController
                .doPrivileged(AdminTokenAction.getInstance()), this, null);
            
            initialized = true;
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl::init success: "
                    + serverURL);
            }
        } catch (Exception e) {
            debug.error("DirectoryManagerImpl::init ERROR", e);
        }
    }
    
    public String createAMTemplate(String token, String entryDN,
        int objectType, String serviceName, Map attributes, int priority)
        throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.createAMTemplate(ssoToken, entryDN, objectType,
                serviceName, attributes, priority);
        } catch (AMException e) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.createAMTemplate."
                    + " Caught Exception: " + e);
            }
            throw convertException(e);
        }
        
    }
    
    public void createEntry(String token, String entryName, int objectType,
        String parentDN, Map attributes) throws AMRemoteException,
        SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            dsServices.createEntry(ssoToken, entryName, objectType, parentDN,
                attributes);
        } catch (AMException e) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.createEntry."
                    + " Caught Exception: " + e);
            }
            throw convertException(e);
        }
        
    }
    
    public boolean doesEntryExists(String token, String entryDN)
        throws AMRemoteException, SSOException, RemoteException {
        SSOToken ssoToken = getSSOToken(token);
        initialize();
        return dsServices.doesEntryExists(ssoToken, entryDN);
    }
    
    public String getAMTemplateDN(String token, String entryDN, int objectType,
        String serviceName, int type) throws AMRemoteException,
        SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.getAMTemplateDN(ssoToken, entryDN, objectType,
                serviceName, type);
        } catch (AMException e) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getAMTemplateDN."
                    + " Caught Exception: " + e);
            }
            throw convertException(e);
        }
        
    }
    
    public Map getAttributes3(String token, String entryDN,
        boolean ignoreCompliance, boolean byteValues, int profileType)
        throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.getAttributes(ssoToken, entryDN,
                ignoreCompliance, byteValues, profileType);
        } catch (AMException e) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getAttributes3."
                    + " Caught Exception: " + e);
            }
            throw convertException(e);
        }
    }
    
    public Map getAttributes1(String token, String entryDN, int profileType)
        throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.getAttributes(ssoToken, entryDN, profileType);
        } catch (AMException e) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getAttributes1."
                    + " Caught Exception: " + e);
            }
            throw convertException(e);
        }
        
    }
    
    public Map getAttributes2(
        String token,
        String entryDN,
        Set attrNames,
        int profileType
    ) throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.getAttributes(ssoToken, entryDN, attrNames,
                profileType);
        } catch (AMException e) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getAttributes2."
                    + " Caught Exception: " + e);
            }
            throw convertException(e);
        }
        
    }
    
    public Map getAttributesByteValues1(
        String token,
        String entryDN,
        int profileType
    ) throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.getAttributesByteValues(ssoToken, entryDN,
                profileType);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getAttributesByteValues1."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public Map getAttributesByteValues2(
        String token,
        String entryDN,
        Set attrNames,
        int profileType
    ) throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.getAttributesByteValues(ssoToken, entryDN,
                attrNames, profileType);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getAttributesByteValues2."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    
    public Set getAttributesForSchema(String objectclass)
        throws RemoteException {
        initialize();
        return dsServices.getAttributesForSchema(objectclass);
    }
    
    
    public String getCreationTemplateName(int objectType)
        throws RemoteException {
        initialize();
        return dsServices.getCreationTemplateName(objectType);
    }
    
    public Map getDCTreeAttributes(
        String token,
        String entryDN,
        Set attrNames,
        boolean byteValues,
        int objectType
    ) throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.getDCTreeAttributes(ssoToken, entryDN, attrNames,
                byteValues, objectType);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getDCTreeAttributes."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public String getDeletedObjectFilter(int objecttype)
        throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            return complianceServices.getDeletedObjectFilter(objecttype);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getDeletedObjectFilter."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public Map getExternalAttributes(
        String token,
        String entryDN,
        Set attrNames,
        int profileType
    ) throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.getExternalAttributes(ssoToken, entryDN,
                attrNames, profileType);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getExternalAttributes."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public LinkedList getGroupFilterAndScope(
        String token,
        String entryDN,
        int profileType
    ) throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            String[] array = dsServices.getGroupFilterAndScope(ssoToken,
                entryDN, profileType);
            LinkedList list = new LinkedList();
            list.addAll(Arrays.asList(array));
            return list;
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getGroupFilterAndScope."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
    }
    
    public Set getMembers(String token, String entryDN, int objectType)
        throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.getMembers(ssoToken, entryDN, objectType);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getMembers."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
    }
    
    public String getNamingAttr(int objectType, String orgDN)
        throws RemoteException {
        initialize();
        return dsServices.getNamingAttribute(objectType, orgDN);
    }
    
    public String getObjectClassFromDS(int objectType) throws RemoteException {
        initialize();
        return dsServices.getObjectClass(objectType);
    }
    
    public int getObjectType(String token, String dn)
        throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.getObjectType(ssoToken, dn);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getObjectType."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public String getOrganizationDN(String token, String entryDN)
        throws AMRemoteException, RemoteException, SSOException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.getOrganizationDN(ssoToken, entryDN);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getOrganizationDN."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public String verifyAndGetOrgDN(
        String token,
        String entryDN,
        String childDN
    ) throws AMRemoteException, RemoteException, SSOException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.verifyAndGetOrgDN(ssoToken, entryDN, childDN);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.verifyAndGetOrgDN."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public String getOrgDNFromDomain(String token, String domain)
        throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dcTreeServices.getOrganizationDN(ssoToken, domain);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getOrgDNFromDomain."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public String getOrgSearchFilter(String entryDN)
        throws RemoteException {
        initialize();
        return dsServices.getOrgSearchFilter(entryDN);
    }
    
    public Set getRegisteredServiceNames(String token, String entryDN)
        throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            return dsServices.getRegisteredServiceNames(null, entryDN);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getRegisteredServiceNames."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public String getSearchFilterFromTemplate(
        int objectType,
        String orgDN,
        String searchTemplateName
    ) throws RemoteException {
        initialize();
        return dsServices.getSearchFilterFromTemplate(objectType, orgDN,
            searchTemplateName);
    }
    
    public Set getTopLevelContainers(String token)
        throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.getTopLevelContainers(ssoToken);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getTopLevelContainers."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public boolean isAncestorOrgDeleted(
        String token,
        String dn,
        int profileType
    ) throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return complianceServices.isAncestorOrgDeleted(ssoToken, dn,
                profileType);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.isAncestorOrgDeleted."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public void modifyMemberShip(
        String token,
        Set members,
        String target,
        int type,
        int operation
    ) throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            dsServices.modifyMemberShip(ssoToken, members, target, type,
                operation);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.modifyMemberShip."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public void registerService(
        String token,
        String orgDN,
        String serviceName
    ) throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            dsServices.registerService(ssoToken, orgDN, serviceName);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.registerService."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public void removeAdminRole(String token, String dn, boolean recursive)
        throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            dsServices.removeAdminRole(ssoToken, dn, recursive);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.removeAdminRole."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public void removeEntry(
        String token,
        String entryDN,
        int objectType,
        boolean recursive,
        boolean softDelete
    ) throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            dsServices.removeEntry(ssoToken, entryDN, objectType, recursive,
                softDelete);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.removeEntry."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public String renameEntry(
        String token,
        int objectType,
        String entryDN,
        String newName,
        boolean deleteOldName
    ) throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.renameEntry(ssoToken, objectType, entryDN,
                newName, deleteOldName);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.renameEntry."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public Set search1(
        String token,
        String entryDN,
        String searchFilter,
        int searchScope
    ) throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.search(ssoToken, entryDN, searchFilter,
                searchScope);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.search1."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public Map search2(String token, String entryDN, String searchFilter,
        List sortKeys, int startIndex, int beforeCount, int afterCount,
        String jumpTo, int timeOut, int maxResults, int scope,
        boolean allAttributes, String[] attrNames)
        throws AMRemoteException, SSOException, RemoteException {
        // Construct the SortKeys
        initialize();
        SortKey[] keys = null;
        int keysLength = 0;
        if (sortKeys != null && (keysLength = sortKeys.size()) != 0) {
            keys = new SortKey[keysLength];
            for (int i = 0; i < keysLength; i++) {
                String data = (String) sortKeys.get(i);
                keys[i] = new SortKey();
                keys[i].reverse = data.startsWith("true:");
                keys[i].attributeName = data.substring(5);
            }
        }
        // Construct SearchControl
        SearchControl sc = new SearchControl();
        if (keys != null) {
            sc.setSortKeys(keys);
        }
        if (jumpTo == null) {
            sc.setVLVRange(startIndex, beforeCount, afterCount);
        } else {
            sc.setVLVRange(jumpTo, beforeCount, afterCount);
        }
        sc.setTimeOut(timeOut);
        sc.setMaxResults(maxResults);
        sc.setSearchScope(scope);
        sc.setAllReturnAttributes(allAttributes);
        
        // Perform the search
        try {
            AMSearchResults results = dsServices.search(tm
                .createSSOToken(token), entryDN, searchFilter, sc,
                attrNames);
            // Convert results to Map
            Map answer = results.getResultAttributes();
            if (answer == null) {
                answer = new HashMap();
            }
            answer.put(com.iplanet.am.sdk.remote.RemoteServicesImpl.AMSR_COUNT,
                Integer.toString(results.getTotalResultCount()));
            answer.put(
                com.iplanet.am.sdk.remote.RemoteServicesImpl.AMSR_RESULTS,
                results.getSearchResults());
            answer.put(com.iplanet.am.sdk.remote.RemoteServicesImpl.AMSR_CODE,
                Integer.toString(results.getErrorCode()));
            return (answer);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DMI::search(with SearchControl):  entryDN="
                    + entryDN + "the exception is: " +  amex);
            }
            throw convertException(amex);
        }
    }
    
    public Map search3(String token, String entryDN, String searchFilter,
        List sortKeys, int startIndex, int beforeCount, int afterCount,
        String jumpTo, int timeOut, int maxResults, int scope,
        boolean allAttributes, Set attrNamesSet) throws AMRemoteException,
        SSOException, RemoteException {
        // Construct the SortKeys
        initialize();
        SortKey[] keys = null;
        int keysLength = 0;
        if (sortKeys != null && (keysLength = sortKeys.size()) != 0) {
            keys = new SortKey[keysLength];
            for (int i = 0; i < keysLength; i++) {
                String data = (String) sortKeys.get(i);
                keys[i] = new SortKey();
                keys[i].reverse = data.startsWith("true:");
                keys[i].attributeName = data.substring(5);
            }
        }
        // Construct SearchControl
        SearchControl sc = new SearchControl();
        if (keys != null) {
            sc.setSortKeys(keys);
        }
        if (jumpTo == null) {
            sc.setVLVRange(startIndex, beforeCount, afterCount);
        } else {
            sc.setVLVRange(jumpTo, beforeCount, afterCount);
        }
        sc.setTimeOut(timeOut);
        sc.setMaxResults(maxResults);
        sc.setSearchScope(scope);
        sc.setAllReturnAttributes(allAttributes);
        
        String[] attrNames = new String[attrNamesSet.size()];
        attrNames = (String[]) attrNamesSet.toArray(attrNames);
        
        // Perform the search
        try {
            AMSearchResults results = dsServices.search(tm
                .createSSOToken(token), entryDN, searchFilter, sc,
                attrNames);
            // Convert results to Map
            Map answer = results.getResultAttributes();
            if (answer == null) {
                answer = new HashMap();
            }
            answer.put(com.iplanet.am.sdk.remote.RemoteServicesImpl.AMSR_COUNT,
                Integer.toString(results.getTotalResultCount()));
            answer.put(
                com.iplanet.am.sdk.remote.RemoteServicesImpl.AMSR_RESULTS,
                results.getSearchResults());
            answer.put(com.iplanet.am.sdk.remote.RemoteServicesImpl.AMSR_CODE,
                Integer.toString(results.getErrorCode()));
            return (answer);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DMI::search(with SearchControl3): entryDN="
                    + entryDN + "the exception is: " +  amex);
            }
            throw convertException(amex);
        }
    }
    
    public void setAttributes(
        String token,
        String entryDN,
        int objectType,
        Map stringAttributes,
        Map byteAttributes,
        boolean isAdd
    ) throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            dsServices.setAttributes(ssoToken, entryDN, objectType,
                stringAttributes, byteAttributes, isAdd);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.setAttributes."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
    }
    
    public void changePassword(String token, String entryDN, String attrName,
        String oldPassword, String newPassword)
        throws AMRemoteException, SSOException, RemoteException {

        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            dsServices.changePassword(ssoToken, entryDN, attrName,
                oldPassword, newPassword);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.setAttributes."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
    }

    public void setGroupFilter(String token, String entryDN, String filter)
        throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            dsServices.setGroupFilter(ssoToken, entryDN, filter);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.setGroupFilter."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public void unRegisterService(
        String token,
        String entryDN,
        int objectType,
        String serviceName,
        int type
    ) throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            // TODO FIX LATER
            dsServices.unRegisterService(ssoToken, entryDN, objectType,
                serviceName, type);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.unRegisterService."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public void updateUserAttribute(
        String token,
        Set members,
        String staticGroupDN,
        boolean toAdd
    ) throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            dsServices.updateUserAttribute(ssoToken, members, staticGroupDN,
                toAdd);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.updateUserAttribute."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    public void verifyAndDeleteObject(String token, String dn)
        throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            complianceServices.verifyAndDeleteObject(ssoToken, dn);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.verifyAndDeleteObject."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
        
    }
    
    private AMRemoteException convertException(AMException amex) {
        String ldapErrCodeString = null;
        if ((ldapErrCodeString = amex.getLDAPErrorCode()) == null) {
            
            return new AMRemoteException(amex.getMessage(),
                amex.getErrorCode(), 0, copyObjectArrayToStringArray(amex
                .getMessageArgs()));
        } else {
            return new AMRemoteException(amex.getMessage(),
                amex.getErrorCode(), Integer.parseInt(ldapErrCodeString),
                copyObjectArrayToStringArray(amex.getMessageArgs()));
        }
    }
    
    private String[] copyObjectArrayToStringArray(Object[] objArray) {
        if ((objArray != null) && (objArray.length != 0)) {
            int count = objArray.length;
            String[] strArray = new String[count];
            for (int i = 0; i < count; i++) {
                strArray[i] = (String) objArray[i];
            }
            return strArray;
        }
        return null;
    }
    
    public Map getAttributes4(
        String token,
        String entryDN,
        Set attrNames,
        boolean ignoreCompliance,
        boolean byteValues,
        int profileType
    ) throws AMRemoteException, SSOException, RemoteException {
        initialize();
        try {
            SSOToken ssoToken = getSSOToken(token);
            return dsServices.getAttributes(ssoToken, entryDN, attrNames,
                ignoreCompliance, byteValues, profileType);
        } catch (AMException amex) {
            if (debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.getAttributes4."
                    + " Caught Exception: " + amex);
            }
            throw convertException(amex);
        }
    }
    
    // Notification methods
    public Set objectsChanged(int time) throws RemoteException {
        // Since clients tend to request for objects changed
        // donot initialize which might thrown an excpetion
        // initialize();
        initialize_cache();
        Set answer = new HashSet();
        // Get the cache index for times upto time+2
        long cacheIndex = System.currentTimeMillis() / 60000;
        for (int i = 0; i < time + 3; i++) {
            Set modDNs = (Set)cache.get(Long.toString(cacheIndex));
            if (modDNs != null)
                answer.addAll(modDNs);
            cacheIndex--;
        }
        if (debug.messageEnabled()) {
            debug.message("DirectoryManagerImpl:objectsChanged in time: "
                + time + " minutes:\n" + answer);
        }
        return (answer);
    }
    
    public String registerNotificationURL(String url) throws RemoteException {
       return registerNotificationURL(url, notificationURLs);
    }
    
    public void deRegisterNotificationURL(String notificationID) throws RemoteException {
        synchronized (notificationURLs) {
            URL url = notificationURLs.remove(notificationID);
            if (url != null && debug.messageEnabled()) {
                debug.message("DirectoryManagerImpl.deRegisterNotificationURL() - URL "
                        + url + " de-registered for ID " + notificationID);
            }
        }
    }
    
    // Implementation to process entry changed events
    protected static synchronized void processEntryChanged(String method,
        String name, int type, Set attrNames) {
        
        debug.message("DirectoryManagerImpl.processEntryChaged method "
                + "processing");
        initialize_cache();
        
        // Obtain the cache index
        long currentTime = System.currentTimeMillis() / 60000;
        String cacheIndex = Long.toString(currentTime);
        Set modDNs = (Set) cache.get(cacheIndex);
        if (modDNs == null) {
            modDNs = new HashSet();
            cache.put(cacheIndex, modDNs);
            // Maintain cacheIndex
            cacheIndices.addFirst(cacheIndex);
            cleanupCache(cacheIndices, cache, currentTime);
        }
        
        // Construct the XML document for the event change
        StringBuilder sb = new StringBuilder(100);
        sb.append("<EventNotification><AttributeValuePair>").append(
            "<Attribute name=\"method\" /><Value>").append(method).append(
            "</Value></AttributeValuePair>").append(
            "<AttributeValuePair><Attribute name=\"entityName\" />")
            .append("<Value>").append(name).append(
            "</Value></AttributeValuePair>");
        if (method.equalsIgnoreCase("objectChanged")
        || method.equalsIgnoreCase("objectsChanged")) {
            sb.append("<AttributeValuePair><Attribute name=\"eventType\" />")
            .append("<Value>").append(type).append(
                "</Value></AttributeValuePair>");
            if (method.equalsIgnoreCase("objectsChanged")) {
                sb.append("<AttributeValuePair><Attribute ").append(
                    "name=\"attrNames\"/>");
                for (Iterator items = attrNames.iterator(); items.hasNext();) {
                    String attr = (String) items.next();
                    sb.append("<Value>").append(attr).append("</Value>");
                }
                sb.append("</AttributeValuePair>");
            }
        }
        sb.append("</EventNotification>");
        // Add to cache
        modDNs.add(sb.toString());
        if (debug.messageEnabled()) {
            debug.message("DirectoryManagerImpl::processing entry change: "
                + sb.toString());
            debug.message("DirectoryManagerImpl = notificationURLS" 
                    + notificationURLs.values());

        }
        
        // If notification URLs are present, send notifications
        NotificationSet ns = null;
        synchronized (notificationURLs) {
            for (Map.Entry<String, URL> entry : notificationURLs.entrySet()) {
                String id = entry.getKey();
                URL url = entry.getValue();
            
                // Construct NotificationSet
                if (ns == null) {
                    Notification notification = 
                        new Notification(sb.toString());
                    ns = new NotificationSet(
                        com.iplanet.am.sdk.remote.RemoteServicesImpl
                        .SDK_SERVICE);
                    ns.addNotification(notification);
                }
                try {
                    PLLServer.send(url, ns);
                    if (debug.messageEnabled()) {
                        debug.message("DirectorManagerImpl:sentNotification "
                            + "URL: " + url + " Data: " + ns);
                    }
                } catch (SendNotificationException ne) {
                    if (debug.warningEnabled()) {
                        debug.warning("DirectoryManagerImpl: failed sending "
                            + "notification to: " + url + "\nRemoving "
                            + "URL from notification list.", ne);
                    }
                    // Remove the URL from Notification List
                    notificationURLs.remove(id);
                }
            }
        }
    }

    // Implementation for AMObjectListener
    public void objectChanged(String name, int type, Map configMap) {
        processEntryChanged(EventListener.OBJECT_CHANGED, name, type, null);
    }
    
    public void objectsChanged(String name, int type, Set attrNames,
        Map configMap) {
        processEntryChanged(EventListener.OBJECTS_CHANGED, name, type,
            attrNames);
    }
    
    public void permissionsChanged(String name, Map configMap) {
        processEntryChanged(EventListener.PERMISSIONS_CHANGED, name, 0, null);
    }
    
    public void allObjectsChanged() {
        processEntryChanged(EventListener.ALL_OBJECTS_CHANGED, "", 0, null);
    }

    public void setConfigMap(Map cmap) {
        // Do nothing
    }

    public Map getConfigMap() {
        return (null);
    }
}
