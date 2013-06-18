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
 * $Id: DirectoryManagerIF.java,v 1.9 2010/01/06 01:58:26 veiming Exp $
 *
 */
/**
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */
package com.iplanet.am.sdk.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;

/*
 * JAX-RPC interfaces for DirectoryManager to make it remotable
 */

public interface DirectoryManagerIF extends Remote {

    // Get operations..
    public boolean doesEntryExists(String ssoToken, String entryDN)
            throws AMRemoteException, SSOException, RemoteException;

    public int getObjectType(String ssoToken, String dn)
            throws AMRemoteException, SSOException, RemoteException;

    public Map getDCTreeAttributes(String token, String entryDN, Set attrNames,
            boolean byteValues, int objectType) throws AMRemoteException,
            SSOException, RemoteException;

    public Map getAttributes1(String token, String entryDN, int profileType)
            throws AMRemoteException, SSOException, RemoteException;

    public Map getAttributes2(String token, String entryDN, Set attrNames,
            int profileType) throws AMRemoteException, SSOException,
            RemoteException;

    public Map getAttributesByteValues1(String token, String entryDN,
            int profileType) throws AMRemoteException, SSOException,
            RemoteException;

    public Map getAttributesByteValues2(String token, String entryDN,
            Set attrNames, int profileType) throws AMRemoteException,
            SSOException, RemoteException;

    public Map getAttributes3(String token, String entryDN,
            boolean ignoreCompliance, boolean byteValues, int profileType)
            throws AMRemoteException, SSOException, RemoteException;

    public Map getAttributes4(String token, String entryDN, Set attrNames,
            boolean ignoreCompliance, boolean byteValues, int profileType)
            throws AMRemoteException, SSOException, RemoteException;

    public String getOrgSearchFilter(String entryDN) throws RemoteException;

    public String getOrganizationDN(String token, String entryDN)
            throws AMRemoteException, RemoteException, SSOException;

    public String verifyAndGetOrgDN(String token, String entryDN, 
            String childDN) throws AMRemoteException, RemoteException, 
            SSOException;

    public Map getExternalAttributes(String token, String entryDN,
            Set attrNames, int profileType) throws AMRemoteException,
            SSOException, RemoteException;

    // Update/create/delete operations
    public void updateUserAttribute(String token, Set members,
            String staticGroupDN, boolean toAdd) throws AMRemoteException,
            SSOException, RemoteException;

    public void createEntry(String token, String entryName, int objectType,
            String parentDN, Map attributes) throws AMRemoteException,
            SSOException, RemoteException;

    public void removeEntry(String token, String entryDN, int objectType,
            boolean recursive, boolean softDelete) throws AMRemoteException,
            SSOException, RemoteException;

    public void removeAdminRole(String token, String dn, boolean recursive)
            throws AMRemoteException, SSOException, RemoteException;

    // Search operations
    public Set search1(String token, String entryDN, String searchFilter,
            int searchScope) throws AMRemoteException, SSOException,
            RemoteException;

    public Map search2(String token, String entryDN, String searchFilter,
            List sortKeys, int startIndex, int beforeCount, int afterCount,
            String jumpTo, int timeOut, int maxResults, int scope,
            boolean allAttributes, String[] attrNames)
            throws AMRemoteException, SSOException, RemoteException;

    public Map search3(String token, String entryDN, String searchFilter,
            List sortKeys, int startIndex, int beforeCount, int afterCount,
            String jumpTo, int timeOut, int maxResults, int scope,
            boolean allAttributes, Set attrNamesSet) throws AMRemoteException,
            SSOException, RemoteException;

    public void setAttributes(String token, String entryDN, int objectType,
            Map stringAttributes, Map byteAttributes, boolean isAdd)
            throws AMRemoteException, SSOException, RemoteException;

    public void changePassword(String token, String entryDN, String attrName,
        String oldPassword, String newPassword)
        throws AMRemoteException, SSOException, RemoteException;

    // Role/group operations
    public Set getMembers(String token, String entryDN, int objectType)
            throws AMRemoteException, SSOException, RemoteException;

    public String renameEntry(String token, int objectType, String entryDN,
            String newName, boolean deleteOldName) throws AMRemoteException,
            SSOException, RemoteException;

    public LinkedList getGroupFilterAndScope(String token, String entryDN,
            int profileType) throws AMRemoteException, SSOException,
            RemoteException;

    public void setGroupFilter(String token, String entryDN, String filter)
            throws AMRemoteException, SSOException, RemoteException;

    public void modifyMemberShip(String token, Set members, String target,
            int type, int operation) throws AMRemoteException, SSOException,
            RemoteException;

    // Services related operations
    public Set getRegisteredServiceNames(String token, String entryDN)
            throws AMRemoteException, SSOException, RemoteException;

    public void registerService(String token, String orgDN, String serviceName)
            throws AMRemoteException, SSOException, RemoteException;

    // TODO Origingal code passes an AMTemplate object. Need to take care
    // of that.
    public void unRegisterService(String token, String entryDN, int objectType,
            String serviceName, int type) throws AMRemoteException,
            SSOException, RemoteException;

    public String getAMTemplateDN(String token, String entryDN, int objectType,
            String serviceName, int type) throws AMRemoteException,
            SSOException, RemoteException;

    public String createAMTemplate(String token, String entryDN,
            int objectType, String serviceName, Map attributes, int priority)
            throws AMRemoteException, SSOException, RemoteException;

    public String getNamingAttr(int objectType, String orgDN)
            throws RemoteException;

    public String getCreationTemplateName(int objectType)
            throws RemoteException;

    public String getObjectClassFromDS(int objectType) throws RemoteException;

    public Set getAttributesForSchema(String objectclass)
            throws RemoteException;

    public String getSearchFilterFromTemplate(int objectType, String orgDN,
            String searchTemplateName) throws RemoteException;

    public Set getTopLevelContainers(String token) throws AMRemoteException,
            SSOException, RemoteException;

    // DCTree related operations
    public String getOrgDNFromDomain(String token, String domain)
            throws AMRemoteException, SSOException, RemoteException;

    // AMCompliance related operations
    public boolean isAncestorOrgDeleted(String token, String dn, 
            int profileType) throws AMRemoteException, SSOException, 
            RemoteException;

    public void verifyAndDeleteObject(String token, String dn)
            throws AMRemoteException, SSOException, RemoteException;

    public String getDeletedObjectFilter(int objecttype)
            throws AMRemoteException, SSOException, RemoteException;

    // Notification methods
    public Set objectsChanged(int time) throws RemoteException;

    public String registerNotificationURL(String url) throws RemoteException;

    public void deRegisterNotificationURL(String notificationID)
            throws RemoteException;

    public String create_idrepo(String token, String type, String name,
            Map attrMap, String amOrgName) throws RemoteException,
            IdRepoException, SSOException;

    public void delete_idrepo(String token, String type, String name,
            String orgName, String amsdkDN) throws RemoteException,
            IdRepoException, SSOException;

    public Map getAttributes1_idrepo(String token, String type, String name,
            Set attrNames, String amOrgName, String amsdkDN)
            throws RemoteException, IdRepoException, SSOException;

    public Map getAttributes2_idrepo(String token, String type, String name,
            String amOrgName, String amsdkDN) throws RemoteException,
            IdRepoException, SSOException;

    /**
     * Returns attributes in binary format for a given identity using the IdRepo API.
     *
     * @param token Token identifying the requester.
     * @param type The identity type we need to query the attributes for.
     * @param name The name of the identity.
     * @param attrNames The attribute names that needs to be queried.
     * @param amOrgName The realm identifier.
     * @param amsdkDN The AM SDK DN, may be null.
     * @return A map of attribute names and values, where the values are all in Base64 encoded format.
     * @throws RemoteException If there was a communication problem.
     * @throws IdRepoException If there was a problem while retrieving the attributes from the identity repository.
     * @throws SSOException If there was an error with the provided token.
     */
    public Map<String, Set<String>> getBinaryAttributes_idrepo(String token, String type, String name,
            Set<String> attrNames, String amOrgName, String amsdkDN)
            throws RemoteException, IdRepoException, SSOException;

    public Set getMembers_idrepo(String token, String type, String name,
            String amOrgName, String membersType, String amsdkDN)
            throws RemoteException, IdRepoException, SSOException;

    public Set getMemberships_idrepo(String token, String type, String name,
            String membershipType, String amOrgName, String amsdkDN)
            throws RemoteException, IdRepoException, SSOException;

    public boolean isExists_idrepo(String token, String type, String name,
            String amOrgName) throws RemoteException, SSOException,
            IdRepoException;

    public boolean isActive_idrepo(String token, String type, String name,
            String amOrgName, String amsdkDN) throws RemoteException,
            IdRepoException, SSOException;

    public void setActiveStatus_idrepo(String token, String type, String name,
            String amOrgName, String amsdkDN, boolean active)
            throws RemoteException, IdRepoException, SSOException;

    public void modifyMemberShip_idrepo(String token, String type, String name,
            Set members, String membersType, int operation, String amOrgName)
            throws RemoteException, IdRepoException, SSOException;

    public void removeAttributes_idrepo(String token, String type, String name,
            Set attrNames, String amOrgName, String amsdkDN)
            throws RemoteException, IdRepoException, SSOException;

    public Map search1_idrepo(String token, String type, String pattern,
            Map avPairs, boolean recursive, int maxResults, int maxTime,
            Set returnAttrs, String amOrgName) throws RemoteException,
            IdRepoException, SSOException;

    public Map search2_idrepo(String token, String type, String pattern,
            int maxTime, int maxResults, Set returnAttrs,
            boolean returnAllAttrs, int filterOp, Map avPairs,
            boolean recursive, String amOrgName) throws RemoteException,
            IdRepoException, SSOException;

    public void setAttributes_idrepo(String token, String type, String name,
            Map attributes, boolean isAdd, String amOrgName, String amsdkDN)
            throws RemoteException, IdRepoException, SSOException;

    public void setAttributes2_idrepo(String token, String type, String name,
            Map attributes, boolean isAdd, String amOrgName, String amsdkDN,
            boolean isString) throws RemoteException, IdRepoException,
            SSOException;

    public void changePassword_idrepo(String token, String type,
            String entryDN, String attrName, String oldPassword,
            String newPassword, String amsdkDN) throws RemoteException,
            IdRepoException, SSOException;

    public Set getAssignedServices_idrepo(String token, String type,
            String name, Map mapOfServiceNamesAndOCs, String amOrgName,
            String amsdkDN) throws RemoteException, IdRepoException,
            SSOException;

    public void assignService_idrepo(String token, String type, String name,
            String serviceName, String stype, Map attrMap, String amOrgName,
            String amsdkDN) throws RemoteException, IdRepoException,
            SSOException;

    public void unassignService_idrepo(String token, String type, String name,
            String serviceName, Map attrMap, String amOrgName, String amsdkDN)
            throws RemoteException, IdRepoException, SSOException;

    public Map getServiceAttributes_idrepo(String token, String type,
            String name, String serviceName, Set attrNames, String amOrgName,
            String amsdkDN) throws RemoteException, IdRepoException,
            SSOException;

    public Map getBinaryServiceAttributes_idrepo(String token, String type,
            String name, String serviceName, Set attrNames, String amOrgName,
                        String amsdkDN )
    throws RemoteException, IdRepoException, SSOException;

    /**
     * Non-javadoc, non-public methods
     * Get the service attributes of the name identity. Traverse to the global
     * configuration if necessary until all attributes are found or reached
     * the global area whichever occurs first.
     *
     * @param token is the sso token of the person performing this operation.
     * @param type is the identity type of the name parameter.
     * @param name is the identity we are interested in.
     * @param serviceName is the service we are interested in
     * @param attrNames are the name of the attributes wer are interested in.
     * @param amOrgName is the orgname.
     * @param amsdkDN is the amsdkDN.
     * @throws RemoteException if there are problem connecting with remote site.
     * @throws IdRepoException if there are repository related error conditions.
     * @throws SSOException if user's single sign on token is invalid.
     */
    public Map getServiceAttributesAscending_idrepo(String token, String type,
            String name, String serviceName, Set attrNames, String amOrgName,
            String amsdkDN )
    throws RemoteException, IdRepoException, SSOException;

    public void modifyService_idrepo(String token, String type, String name,
            String serviceName, String stype, Map attrMap, String amOrgName,
            String amsdkDN) throws RemoteException, IdRepoException,
            SSOException;

    public Set getSupportedTypes_idrepo(String token, String amOrgName)
            throws RemoteException, IdRepoException, SSOException;

    public Set getSupportedOperations_idrepo(String token, String type,
            String amOrgName) throws RemoteException, IdRepoException,
            SSOException;

    public Set getFullyQualifiedNames_idrepo(String token, String type,
        String name, String orgName)
        throws RemoteException, IdRepoException, SSOException;

    public String registerNotificationURL_idrepo(String url)
            throws RemoteException;

    public void deRegisterNotificationURL_idrepo(String notificationID)
            throws RemoteException;

    public Set objectsChanged_idrepo(int time) throws RemoteException;

    public Map getSpecialIdentities_idrepo(String token,
        String type, String orgName)
        throws RemoteException, IdRepoException, SSOException;
}
