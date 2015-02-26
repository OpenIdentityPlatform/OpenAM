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
 * $Id: IDMCommon.java,v 1.17 2009/06/02 17:09:26 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.idm.IDMConstants;
import com.sun.identity.sm.OrganizationConfigManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Level;

/**
 * This class contains helper methods related to identity objects
 */
public class IDMCommon extends TestCommon {
    
    /**
     * Empty Constructor
     */
    public IDMCommon() {
        super("IDMCommon");
    }
    
    public IDMCommon(String componentName) {
        super(componentName);
    }
    
    /**
     * Creates a new Identity
     */
    public AMIdentity createIdentity(
            SSOToken ssoToken,
            String parentRealm,
            IdType idType,
            String entityName,
            Map values)
            throws Exception {
        AMIdentityRepository repo = new AMIdentityRepository(
                ssoToken, parentRealm);
        AMIdentity amid = repo.createIdentity(idType, entityName, values);
        assert (amid.getName().equals(entityName));
        return amid;
    }
    
    /**
     * Gets realm identity
     */
    public AMIdentity getRealmIdentity(
            SSOToken ssoToken,
            String parentRealm)
            throws Exception {
        AMIdentityRepository repo = new AMIdentityRepository(
                ssoToken, parentRealm);
        return (repo.getRealmIdentity());
    }
    
    /**
     * Returns the value of specified attribute for an Identity
     */
    public Set getIdentityAttribute(
            SSOToken ssoToken,
            String serviceName,
            String attributeName)
            throws Exception {
        AMIdentity amid = new AMIdentity(ssoToken);
        Map attrValues = amid.getServiceAttributes(serviceName);
        log(Level.FINEST, "getIdentityAttribute", "Attributes List" +
                attrValues);
        return (Set)attrValues.get(attributeName);
    }
    
    /**
     * Returns Map with IdentityAttributes
     * based on realm and user name
     */
    public Map getIdentityAttributes(String userName, String realm)
    throws Exception {
        SSOToken admintoken = getToken(adminUser, adminPassword, basedn);
        Set<AMIdentity> set = getAMIdentity(admintoken, userName,
                IdType.USER, realm);
        AMIdentity amid = null;
        for (Iterator itr = set.iterator(); itr.hasNext();) {
            amid = (AMIdentity)itr.next();
        }
        Map attrMap = new HashMap();
        attrMap = amid.getAttributes();
        return attrMap;
    }
    
    /**
     * Modifies specified identity attributes
     */
    public void modifyIdentity(AMIdentity amid, Map values)
    throws Exception {
        try {
            amid.setAttributes(values);
            amid.store();
        } finally {
            Thread.sleep(notificationSleepTime);
        }
        
    }
    
    /**
     * Modifies specified identity attributes for a realm
     */
    public void modifyRealmIdentity(SSOToken token, String realm, Map values)
    throws Exception {
        AMIdentity amid = getRealmIdentity(token, realm);
        amid.setAttributes(values);
        amid.store();
    }
    
    /**
     * Deletes an identity based on specified ssotoken, realm, id type and
     * entity name
     */
    public void deleteIdentity(
            SSOToken ssoToken,
            String parentRealm,
            IdType idType,
            String entityName)
            throws Exception {
        AMIdentityRepository repo = new AMIdentityRepository(
                ssoToken, parentRealm);
        repo.deleteIdentities(getAMIdentity(
                ssoToken, entityName, idType, parentRealm));
    }
    
    /**
     * Deletes multiple identities based on specified ssotoken, realm, id type,
     * and a list of entity names
     */
    public void deleteIdentity(
            SSOToken ssoToken,
            String parentRealm,
            List<IdType> idType,
            List entityName)
            throws Exception {
        AMIdentityRepository repo = new AMIdentityRepository(
                ssoToken, parentRealm);
        Iterator iterNameSet = entityName.iterator();
        Iterator iterTypeSet = idType.iterator();
        Set amid = new HashSet<AMIdentity>();
        while (iterNameSet.hasNext()) {
            amid.add(getFirstAMIdentity(ssoToken, (String)iterNameSet.next(),
                    (IdType)iterTypeSet.next(), parentRealm));
        }
        repo.deleteIdentities(amid);
    }
    
    /**
     * Returns AMIdentity based in ssotoken, id name , id type and  sepcified
     * realm
     */
    public Set<AMIdentity> getAMIdentity(
            SSOToken ssoToken,
            String name,
            IdType idType,
            String realm)
            throws Exception {
        Set<AMIdentity> set = new HashSet<AMIdentity>();
        set.add(new AMIdentity(ssoToken, name, idType, realm, null));
        return set;
    }
    
    /**
     * Returns First AMIdentity based in ssotoken, id name , id type and
     * sepcified realm
     */
    public AMIdentity getFirstAMIdentity(
            SSOToken ssoToken,
            String name,
            IdType idType,
            String realm)
            throws Exception {
        Set<AMIdentity> set = getAMIdentity(ssoToken, name, idType, realm);
        AMIdentity amid = null;
        for (Iterator itr = set.iterator(); itr.hasNext();) {
            amid = (AMIdentity)itr.next();
        }
        return (amid);
    }
    
    /**
     * Creates a dummy user in the specified realm.
     * (a) The user name (sn and cn) are equal to  sn or cn plus the supplied
     *     suffix string
     * (b) The user password is concatanation of entityName and suffix
     * (c) User status is set to Active
     */
    public AMIdentity createDummyUser(
            SSOToken ssoToken,
            String parentRealm,
            String entityName,
            String suffix)
            throws Exception {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        putSetIntoMap("sn", map, "sn" + suffix);
        putSetIntoMap("cn", map, "cn" + suffix);
        putSetIntoMap("userpassword", map, entityName + suffix);
        putSetIntoMap("inetuserstatus", map, "Active");
        return createIdentity(ssoToken, parentRealm, IdType.USER, entityName +
                suffix, map);
    }
    
    /**
     * Returns the parent realm
     */
    public String getParentRealm(String realmName)
    throws Exception {
        if (realmName.lastIndexOf("/") == -1) {
            throw new RuntimeException("Incorrect Realm, " + realmName);
        }
        StringTokenizer tokens = new StringTokenizer(realmName, "/");
        int noRealms = tokens.countTokens();
        String parentRealm = realm;
        if (noRealms == 1) {
            return parentRealm;
        } else {
            for (int i = 1; i < noRealms; i++ ){
                parentRealm = tokens.nextToken();
            }
        }
        log(Level.FINEST, "getParentRealm", "Parent realm for" + realmName +
                " is " + parentRealm );
        return parentRealm;
    }
    
    /**
     * Creates a new realm under the specified realm
     */
    public void createSubRealm(SSOToken ssoToken, String realm)
    throws Exception {
        if ((realm != null) && !realm.equals("/")) {
            String parentRealm = getParentRealm(realm);
            createSubRealm(ssoToken, parentRealm);
            OrganizationConfigManager orgMgr = new OrganizationConfigManager(
                    ssoToken, parentRealm);
            int idx = realm.lastIndexOf("/");
            orgMgr.createSubOrganization(realm.substring(idx+1), null);
            assert (orgMgr.getSubOrganizationNames().contains(realm));
        }
    }
    
    /**
     * Deletes a realm
     */
    public void deleteRealm(SSOToken ssoToken, String realm)
    throws Exception {
        if ((realm != null) && !realm.equals("/")) {
            String parentRealm = getParentRealm(realm);
            OrganizationConfigManager orgMgr = new
                    OrganizationConfigManager(ssoToken, parentRealm);
            int idx = realm.lastIndexOf("/");
            orgMgr.deleteSubOrganization(realm.substring(idx+1), true);
            deleteRealm(ssoToken, parentRealm);
        }
    }
    
    /**
     * Adds a User Identity to a Group or a Role Identity on a root realm
     */
    public void addUserMember(SSOToken ssotoken, String userName,
            String memberName, IdType memberType)
            throws Exception {
        addUserMember(ssotoken, userName, memberName, memberType, realm);
    }
    
    /**
     * Adds a User Identity to a Group or a Role Identity
     */
    public void addUserMember(SSOToken ssotoken, String userName,
            String memberName, IdType memberType, String tRealm)
            throws Exception {
        Set setUser = getAMIdentity(ssotoken, userName, IdType.USER, tRealm);
        Set setMember = getAMIdentity(ssotoken, memberName, memberType, tRealm);
        AMIdentity amidUser = null;
        AMIdentity amidMember = null;
        Iterator itr;
        for (itr = setUser.iterator(); itr.hasNext();) {
            amidUser = (AMIdentity)itr.next();
        }
        for (itr = setMember.iterator(); itr.hasNext();) {
            amidMember = (AMIdentity)itr.next();
        }
        amidMember.addMember(amidUser);
    }
    
    /**
     * Remove a User Identity from a Group or a Role Identity
     * @param ssotoken SSO token
     * @param userName user name to be removed
     * @param memberName member name
     * @param memberType member type
     * @param tRealm realm name
     */
    public void removeUserMember(SSOToken ssotoken, String userName,
            String memberName, IdType memberType, String tRealm)
            throws Exception {
        Set setUser = getAMIdentity(ssotoken, userName, IdType.USER, tRealm);
        Set setMember = getAMIdentity(ssotoken, memberName, memberType, tRealm);
        AMIdentity amidUser = null;
        AMIdentity amidMember = null;
        Iterator itr;
        for (itr = setUser.iterator(); itr.hasNext();) {
            amidUser = (AMIdentity)itr.next();
        }
        for (itr = setMember.iterator(); itr.hasNext();) {
            amidMember = (AMIdentity)itr.next();
        }
        amidMember.removeMember(amidUser);
    }
    
    /**
     * Retrieve a list of members
     * @param ssotoken SSO token
     * @param idName identity name that retrieve a list of members
     * @param idType identity type
     * @param memberType member type
     * @param tRealm realm name
     */
    public Set<AMIdentity> getMembers(SSOToken ssotoken, String idName,
            IdType idType, IdType memberType, String tRealm)
            throws Exception {
        Set setId = getAMIdentity(ssotoken, idName, idType, tRealm);
        AMIdentity amid = null;
        Iterator itr;
        for (itr = setId.iterator(); itr.hasNext();) {
            amid = (AMIdentity)itr.next();
        }
        return amid.getMembers(memberType);
    }
    
    /**
     * This method searches and retrieves a list of realm
     * @param ssotoken SSO token object
     * @param pattern realm name or pattern
     * @parm realm under which search has to be perfomed
     * @return a set of realm name
     */
    public Set searchRealms(SSOToken ssotoken, String pattern, String realm)
    throws Exception  {
        entering("searchRealms", null);
        Set realmNames = searchIdentities(ssotoken, pattern, IdType.REALM,
                realm);
        exiting("searchRealms");
        return realmNames;
    }
    
    /**
     * This method searches and retrieves a list of realm
     * @param ssotoken SSO token object
     * @param pattern realm name or pattern
     * @return a set of realm name
     */
    public Set searchRealms(SSOToken ssotoken, String pattern)
    throws Exception  {
        entering("searchRealms", null);
        Set realmNames = searchRealms(ssotoken, pattern, realm);
        exiting("searchRealms");
        return realmNames;
    }
    
    /**
     * This method searches and retrieves a list of identity
     * @param ssotoken SSO token object
     * @param pattern realm name or pattern
     * @param type identity type - user, role, filtered role, group, agent
     * @return a set of identity name
     */
    public Set searchIdentities(SSOToken ssotoken, String pattern, IdType type)
    throws Exception  {
        return searchIdentities(ssotoken, pattern, type, realm);
    }
    
    /**
     * This method searches and retrieves a list of identity
     * @param ssotoken SSO token object
     * @param pattern realm name or pattern
     * @param type identity type - user, role, filtered role, group, agent
     * @param realmName - realm name
     * @return a set of identity name
     */
    public Set searchIdentities(SSOToken ssotoken, String pattern, IdType type,
            String realmName)
            throws Exception  {
        entering("searchIdentities", null);
        AMIdentityRepository repo = new AMIdentityRepository(
                ssotoken, realmName);
        IdSearchControl searchControl = new IdSearchControl();
        IdSearchResults results = repo.searchIdentities(type, pattern,
                searchControl);
        log(Level.FINE, "searchIdentities", "Searching for " + type.getName() +
                " " + pattern + "... under realm "+realmName);
        Set idNames = results.getSearchResults();
        if ((idNames != null) && (!idNames.isEmpty())) {
            Iterator iter = idNames.iterator();
            AMIdentity amIdentity;
            while (iter.hasNext()) {
                amIdentity = (AMIdentity) iter.next();
                log(Level.FINEST, "searchIdentities", "Id name: " +
                        amIdentity.getName() +
                        " & UID = "  + amIdentity.getUniversalId());
            }
        } else {
            log(Level.FINE, "searchIdentities",
                    "Could not find identity name " + pattern);
        }
        exiting("searchIdentities");
        return idNames;
    }
    
    /**
     * This method retrieves the configuration key and values by the prefix
     * string and store them in a map.
     * @param prefixName key prefix string
     * @param cfgFileName properties config file name
     * @return map of configuration
     */
    public Map getDataFromCfgFile(String prefixName, String cfgFileName)
    throws Exception {
        entering("getDataFromCfgFile", null);
        Map cfgMapTemp = new HashMap();
        Map cfgMapNew = new HashMap();
        cfgMapTemp = getMapFromResourceBundle(cfgFileName);
        Set keys = cfgMapTemp.keySet();
        Iterator keyIter = keys.iterator();
        String key;
        String value;
        while (keyIter.hasNext()) {
            key = keyIter.next().toString();
            value = cfgMapTemp.get(key).toString();
            if (key.substring(0, prefixName.length()).equals(prefixName))
                cfgMapNew.put(key, value);
        }
        log(Level.FINEST, "getDataFromCfgFile", cfgMapNew.toString());
        if (cfgMapNew.isEmpty()) {
            log(Level.SEVERE, "getDataFromCfgFile",
                    "Config data map is empty");
            assert false;
        }
        exiting("getDataFromCfgFile");
        return cfgMapNew;
    }
    
    /**
     * This method checks and compare IDM exception error message and error
     * code with expected error message and code.
     * @param IdRepoException idm exception
     * @param eMessage expected error message
     * @param eCode expected error code
     * @return true if match
     */
    public boolean checkIDMExpectedErrorMessageCode(IdRepoException e,
            String eMessage)
            throws Exception {
        return (checkIDMExpectedErrorMessageCode(e, eMessage, null));
    }

    /**
     * This method checks and compare IDM exception error message and error
     * code with expected error message and code.
     * @param IdRepoException idm exception
     * @param eMessage expected error message
     * @param eCode expected error code
     * @return true if match
     */
    public boolean checkIDMExpectedErrorMessageCode(IdRepoException e,
            String eMessage, String eCode)
            throws Exception {
        entering("checkIDMExpectedErrorMessageCode", null);
        boolean isMatch = false;
        String errorCode = e.getErrorCode();
        String errorMessage = e.getMessage();
        
        log(Level.FINEST, "checkExpectedMessageErrorCode", "Actual error" +
                " message: " + errorMessage);
        log(Level.FINEST, "checkExpectedMessageErrorCode", "Expected error" +
                " message: " + eMessage);
        
        if (eCode != null) {
            log(Level.FINEST, "checkExpectedMessageErrorCode", "Actual error" +
                    " code: " +  e.getErrorCode());
            log(Level.FINEST, "checkExpectedMessageErrorCode", "Expected" +
                    " error code: " + eCode);
            
            if (errorCode.equals(eCode) && errorMessage.indexOf(eMessage) >= 0)
            {
                log(Level.FINE, "checkExpectedMessageErrorCode", "Error code" +
                        " and message match");
                isMatch = true;
            } else {
                log(Level.SEVERE, "checkExpectedMessageErrorCode", "Error" +
                        " code and message are not a match");
            }
        } else {
            if (errorMessage.indexOf(eMessage) >= 0)
            {
                log(Level.FINE, "checkExpectedMessageErrorCode", "Error" +
                        " messages match");
                isMatch = true;
            } else {
                log(Level.SEVERE, "checkExpectedMessageErrorCode", "Error" +
                        " messages are not a match");
            }
        }        
        exiting("checkIDMExpectedErrorMessageCode");
        return isMatch;
    }
    
    /**
     * This method checks the support identity type in current deployment.
     * @param ssotoken SSO token
     * @param realmName realm name
     * @param idtype identity type
     * @return true if identity type to be checked is supported identity type
     */
    public boolean isIdTypeSupported(SSOToken ssotoken, String realmName,
            String idtype)
            throws Exception {
        entering("isIdTypeSupported", null);
        boolean supportsIDType = false;
        AMIdentityRepository idrepo =
                new AMIdentityRepository(ssotoken, realmName);
        Set types = idrepo.getSupportedIdTypes();
        log(Level.FINEST, "isIdTypeSupported", "Support id type is " +
                types.toString());
        Iterator iter = types.iterator();
        IdType type;
        while (iter.hasNext()) {
            type =(IdType)iter.next();
            if (type.getName().equalsIgnoreCase(idtype)) {
                supportsIDType = true;
                break;
            }
        }
        exiting("isIdTypeSupported");
        return supportsIDType;
    }
    
    /**
     * This method addes an user member to an identity with identity name, type,
     * and member name.
     * @param idName identity name
     * @param idType identity type
     * @param memberName user member name
     * @return true if member is added to an identity successfully
     */
    public boolean addMembers(String idName, String idType, String memberName,
            SSOToken ssoToken,
            String realmName)
            throws Exception {
        entering("addMembers", null);
        boolean opSuccess = false;
        log(Level.FINE, "addMembers", "Adding a user member name " +
                memberName + " to " + idType + " " + idName + "...");
        addUserMember(ssoToken, memberName, idName, getIdType(idType),
                realmName);
        AMIdentity memid = getFirstAMIdentity(ssoToken, memberName,
                IdType.USER, realmName);
        AMIdentity id = getFirstAMIdentity(ssoToken, idName, getIdType(idType),
                realmName);
        opSuccess = memid.isMember(id);
        if (opSuccess) {
            log(Level.FINE, "addMembers", "User member " + memberName +
                    " is added to " + idType + " " + idName + " successfully");
        } else {
            log(Level.FINE, "addMembers", "Failed to add member");
        }
        exiting("addMembers");
        return opSuccess;
    }
    
    /**
     * This method removes an user member from an identity with identity name,
     * type, and member name.
     * @param idName identity name
     * @param idType identity type
     * @param memberName user member name
     * @return true if member is removed from an identity successfully
     */
    public boolean removeMembers(String idName, String idType,
            String memberName,
            SSOToken ssoToken,
            String realmName)
            throws Exception {
        entering("removeMembers", null);
        boolean opSuccess = false;
        log(Level.FINE, "removeMembers", "Removing a user member name " +
                memberName + " from " + idType + " " + idName + "...");
        removeUserMember(ssoToken, memberName, idName, getIdType(idType),
                realmName);
        Thread.sleep(notificationSleepTime);
        AMIdentity memid = getFirstAMIdentity(ssoToken, memberName,
                IdType.USER, realmName);
        AMIdentity id = getFirstAMIdentity(ssoToken, idName, getIdType(idType),
                realmName);
        opSuccess = (!memid.isMember(id)) ? true : false;
        if (opSuccess) {
            log(Level.FINE, "removeMembers", "User member " + memberName +
                    " is removed from " + idType + " " + idName +
                    " successfully");
        } else {
            log(Level.FINE, "removeMembers", "Failed to remove member");
        }
        exiting("removeMembers");
        return opSuccess;
    }
    
    /**
     * This method creates a identity with given name and type and verifies
     * that user exists.
     * @param idName    identity name
     * @param idType    identity type - user, group, role, filtered role, agent
     * @param userAttr  identity attributes. If null, default attributes is used
     * @param ssoToken  admin SSOtoken for creating identity
     * @param realmName realm name in which identity has be created.
     * @return true if the identity created successfully.
     */
    public boolean createID(String idName, String idType, String userAttr,
            SSOToken ssoToken,
            String realmName)
            throws Exception {
        entering("createID", null);
        boolean opSuccess = false;
        IdType id = getIdType(idType); 
        log(Level.FINE, "createID", "Creating identity " + idType +
                " name " + idName + "...");
        Map userAttrMap;
        if (userAttr == null) {
            userAttrMap = setDefaultIdAttributes(idType, idName);
        } else {
            userAttrMap = setIDAttributes(userAttr);
        }
        log(Level.FINEST, "createID", "realm = " + realmName
                + " type = " + id.getName() + " attributes = " + 
                userAttrMap.toString());
        createIdentity(ssoToken, realmName, id, idName,
                userAttrMap);
        opSuccess = (doesIdentityExists(idName, idType, ssoToken,
                realmName)) ? true : false;
        if (opSuccess) {
            log(Level.FINE, "createID", idType + " " + idName +
                    " is created successfully.");
        } else {
            log(Level.FINE, "createID", "Failed to create " + idType +
                    " " + idName);
        }
        exiting("createID");
        return (opSuccess);
    }
    
    /**
     * This method creates a map of identity attributes
     */
    public Map setIDAttributes(String idAttrList)
    throws Exception {
        log(Level.FINEST, "setIDAttributes", "Attributes string " + idAttrList);
        Map tempAttrMap = new HashMap();
        Map idAttrMap = getAttributeMap(idAttrList,
                IDMConstants.IDM_KEY_SEPARATE_CHARACTER);
        Set keys = idAttrMap.keySet();
        Iterator keyIter = keys.iterator();
        String key;
        String value;
        Set idAttrSet;
        while (keyIter.hasNext()) {
            key = (String)keyIter.next();
            value = (String)idAttrMap.get(key);
            putSetIntoMap(key, tempAttrMap, value);
        }
        return tempAttrMap;
    }
    
    /**
     * This method create a map with default identity attributes.  This map
     * is used to create an identity
     */
    public Map setDefaultIdAttributes(String siaType, String idName)
    throws Exception {
        Map<String, Set<String>> tempMap = new HashMap<String, Set<String>>();
        log(Level.FINEST, "setDefaultIdAttributes", "for " + idName);
        if (siaType.equals("user")) {
            putSetIntoMap("sn", tempMap, idName);
            putSetIntoMap("cn", tempMap, idName);
            putSetIntoMap("givenname", tempMap, idName);
            putSetIntoMap("userpassword", tempMap, idName);
            putSetIntoMap("inetuserstatus", tempMap, "Active");
        } else if (siaType.equals("agent")) { 
            putSetIntoMap("userpassword", tempMap, idName);
            putSetIntoMap("sunIdentityServerDeviceStatus", tempMap, "Active");
        } else if (siaType.equals("agentonly") ) { 
            putSetIntoMap("userpassword", tempMap, idName);
            putSetIntoMap("sunIdentityServerDeviceStatus", tempMap, "Active");
            putSetIntoMap("AgentType", tempMap, "webagent");
        } else if (siaType.equals("filteredrole")) {
            putSetIntoMap("cn", tempMap, idName);
            putSetIntoMap("nsRoleFilter", tempMap,
                    "(objectclass=inetorgperson)");
        } else if (siaType.equals("role") || siaType.equals("group")) {
            putSetIntoMap("description", tempMap, siaType + " description");
        } else {
            log(Level.SEVERE, "setIdAttributes", "Invalid identity type " +
                    siaType);
            assert false;
        }
        return tempMap;
    }
    
    /**
     * This method checks if an identity exists.  It accepts identity name and
     * type from the arguments.
     * @param idName identity name
     * @param idType identity type - user, agent, role, filtered role, group
     * @return true if identity exists
     */
    public boolean doesIdentityExists(String idName, String idType,
            SSOToken ssoToken,
            String realmName)
            throws Exception {
        return doesIdentityExists(idName, getIdType(idType), ssoToken,
                realmName);
    }
    
    /**
     * This method checks if an identity exists.  It accepts identity name and
     * type from the arguments.
     * @param idName identity name
     * @param idType Idtype of identity type
     * @param ssoToken admin SSOToken
     * @parm realmName realm Name in which identity present.
     * @return true if identity exists
     */
    public boolean doesIdentityExists(String idName, IdType idType,
            SSOToken ssoToken,
            String realmName)
            throws Exception {
        entering("doesIdentityExists", null);
        boolean idFound = false;
        Set idRes = searchIdentities(ssoToken, idName, idType,
                realmName);
        Iterator iter = idRes.iterator();
        AMIdentity amIdentity;
        while (iter.hasNext()) {
            amIdentity = (AMIdentity) iter.next();
            if (amIdentity.getName().equals(idName)) {
                idFound = true;
                break;
            }
            log(Level.FINEST, "searchIdentities", "Search result - name: " +
                    amIdentity.getName());
        }
        exiting("doesIdentityExists");
        return (idFound);
    }
    
    /**
     * This method deletes one or multiple identities with identity name and
     * type
     * @param idName identity name
     * @param idType identity type - user, agent, role, filtered role, group
     * @param ssoToken admin SSOToken
     * @param realmName realm Name from which identity has to be deleted.
     * @return true if identity is deleted successfully
     */
    public boolean deleteID(String idName, String idType, SSOToken ssoToken,
            String realmName)
            throws Exception {
        entering("deleteID", null);
        boolean opSuccess = false;
        if (idType == null) {
            log(Level.FINE, "deleteID", "Failed to delete idType cannot be null"
                    + idType + " " + idName);
            return false;
        }
        List idTypeList = getAttributeList(idType,
                IDMConstants.IDM_KEY_SEPARATE_CHARACTER);
        List idNameList = getAttributeList(idName,
                IDMConstants.IDM_KEY_SEPARATE_CHARACTER);
        log(Level.FINEST, "deleteID", idNameList.toString());
        log(Level.FINE, "deleteID", "Deleting identity " + idType +
                " name " + idName + "...");
        Iterator iterName = idNameList.iterator();
        Iterator iterType = idTypeList.iterator();
        List newidTypeList = new ArrayList();
        while (iterName.hasNext()) {
            if (iterType.hasNext()) {
                newidTypeList.add(getIdType((String)iterType.next()));
            } else {
                newidTypeList.add(getIdType(idType));
            }
            iterName.next();
        }
        deleteIdentity(ssoToken, realmName, newidTypeList, idNameList);
        Iterator iterN = idNameList.iterator();
        Iterator iterT = newidTypeList.iterator();
        Thread.sleep(notificationSleepTime);
        while (iterN.hasNext()) {
            if (doesIdentityExists((String)iterN.next(),
                    (IdType)iterT.next(), ssoToken, realmName)) {
                opSuccess = false;
                break;
            } else{
                opSuccess = true;
            }
        }
        if (opSuccess) {
            log(Level.FINE, "deleteID", idType + " " + idName +
                    " is deleted successfully.");
        } else {
            log(Level.FINE, "deleteID", "Failed to delete " + idType +
                    " " + idName);
        }
        exiting("deleteID");
        return opSuccess;
    }
    
    /**
     * This method return type IdType of identity type
     */
    public IdType getIdType(String gidtType)
    throws Exception {
        SMSCommon smsc;
        SSOToken admintoken = null;
        if (gidtType.equals("user")) {
            return IdType.USER;
        } else if (gidtType.equals("role")) {
            return IdType.ROLE;
        } else if (gidtType.equals("filteredrole")) {
            return IdType.FILTEREDROLE;
        } else if (gidtType.equals("agent") || gidtType.equals("agentonly")) {
            admintoken = getToken(adminUser, adminPassword, basedn);
            try { 
                smsc = new SMSCommon(admintoken);
                if (smsc.isAMDIT()) {
                    destroyToken(admintoken);
                    return IdType.AGENT;                
                } else {
                    destroyToken(admintoken);
                    return IdType.AGENTONLY;                            
                }
            } catch(Exception e) { 
                destroyToken(admintoken);
                log(Level.SEVERE, "getIdType", e.getMessage());
                e.printStackTrace();
                throw e;
            }
        } else if (gidtType.equals("group")) {
            return IdType.GROUP;
        } else {
            log(Level.SEVERE, "getIdType", "Invalid id type " + gidtType);
            assert false;
            return null;
        }


    }
    
    /*
     * This method creates the subrealms if they dont exists.
     * realmsToCreate can be /aaa/bbb/ccc
     */
    public void createSubRealms(WebClient webClient, FederationManager fm,
            String realmToCreate)
            throws Exception {
        //If execution_realm is different than root realm (/)
        //then create the realm
        if (!realmToCreate.equals("/")) {
            String topRealm = "/";
            StringTokenizer stRealms = new StringTokenizer(realmToCreate, "/");
            while (stRealms.hasMoreTokens()) {
                String subRealm = stRealms.nextToken();
                HtmlPage spRealmPage = fm.listRealms(webClient, topRealm, "",
                        false);
                if (FederationManager.getExitCode(spRealmPage) != 0) {
                    log(Level.SEVERE, "configureSAMLv2", "ListRealms " +
                            "command failed at SP");
                    assert false;
                }
                //Append / if topRealm doesnt end with /
                if (!topRealm.endsWith("/")) {
                    topRealm = topRealm + "/";
                }
                if (spRealmPage.getWebResponse().getContentAsString().contains(
                        subRealm)) {
                    log(Level.FINE, "configureSAMLv2", "Execution realm " +
                            "already exists at SP");
                } else {
                    spRealmPage = fm.createRealm(webClient,
                            topRealm + subRealm);
                    if (FederationManager.getExitCode(spRealmPage) != 0) {
                        log(Level.SEVERE, "configureSAMLv2", "Execution realm " +
                                "creation failed at SP");
                        assert false;
                    }
                }
                topRealm = topRealm + subRealm;
            }
        }
    }
    
    /*
     * Based on the subrealm_recursive_delete_flag this method deletes the
     * subrealms.
     * If this flag is true, whole subrealm tree is deleted.
     * If this flag is false, only leaf subrealm is deleted.
     */
    public void deleteSubRealms(WebClient webClient, FederationManager fm,
            String realmsToDelete, String recursiveDeleteFlag)
            throws Exception {
        if (!realmsToDelete.equals("/")) {
            HtmlPage spRealmPage;
            //If the recursive flag is set to true, delete the whole tree
            if (recursiveDeleteFlag.equalsIgnoreCase("true")) {
                StringTokenizer stSPRealms = new StringTokenizer(realmsToDelete,
                        "/");
                String subRealm = "/" + stSPRealms.nextToken();
                spRealmPage = fm.deleteRealm(webClient, subRealm, true);
            } else {
                //delete the last subrealm
                spRealmPage = fm.deleteRealm(webClient, realmsToDelete, false);
            }
            if (FederationManager.getExitCode(spRealmPage) != 0) {
                log(Level.SEVERE, "deleteSubRealms", "Execution " +
                        "realm deletion failed");
            }
        }
    }

    /**
     * Determines if ROLE,FILTEREDROLE are supported for this realm
     */
    public boolean isFilteredRolesSupported()
    throws Exception {
        try {
            SSOToken admintoken = getToken(adminUser, adminPassword, basedn);
            if (!isIdTypeSupported(admintoken, "/", "FILTEREDROLE") ||
                !isIdTypeSupported(admintoken, "/", "ROLE")) {
                log(Level.SEVERE, "isFilteredRolesSupported",
                        "Roles or Filtered Roles " + 
                        "are not supported in this configuration");
                return false;
            }
            return true;
        } catch (Exception e) {
            log(Level.SEVERE, "isFilteredRolesSupported", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Hot Swaps the property strPropName with value strPropValue.
     * In case of multi valued properties, the | seperated values in 
     * strPropValue are added to the exisiting list
     */
    public void hotSwapProperty(AMIdentity amid, String strPropName, 
            String strPropValue, String strRbl)
    throws Exception {
        entering("hotSwapProperty", null);
        Set set;
        Set origSet;
        Map map;
        String strPropValueOrig;
        boolean isAutoGenKey;
        try {
            log(Level.FINE, "hotSwapProperty", "Inside IDMCommon");
            map = new HashMap();
            set = new HashSet();
            origSet = new HashSet();
            origSet = amid.getAttribute(strPropName);
            isAutoGenKey = isPropertyNumberedList(strRbl, strPropName);
            SortedSet sortset;
            Iterator itr;
            String maxSetValue = "0";
            log(Level.FINE, "hotSwapProperty", "origSet : " + origSet);
            if (origSet.size() != 0) { 
                if (isAutoGenKey) {
                    sortset = new TreeSet();
                    itr = origSet.iterator();                
                    while (itr.hasNext()) {
                        strPropValueOrig = (String)itr.next();
                        sortset.add(strPropValueOrig);
                    }
                    maxSetValue = (String)sortset.last();
                }
                int pos;
                int pos1;
                int maxSetVal = 0;
                if (isAutoGenKey) {
                    pos = maxSetValue.indexOf("[");
                    pos1 = maxSetValue.indexOf("]");
                    maxSetVal = new Integer(maxSetValue.substring(pos + 1 , 
                        pos1)).intValue();                        
                }                    
                map = new HashMap();
                String strProp;
                if (origSet.size() > 1) {
                    set = amid.getAttribute(strPropName);
                    if (strPropValue.contains("|")) {
 
                        String strSplit[] = strPropValue.split("\\|");
                        for (int i = 0; i < strSplit.length; i++) {
                            log(Level.FINE, "hotSwapProperty", "strProp : " + 
                                strSplit[i]);
                            if (isAutoGenKey) {
                                strProp = "[" + (maxSetVal + i + 1) + "]=" + 
                                        strSplit[i];
                                log(Level.FINE, "hotSwapProperty", "strProp : " 
                                        + strProp);
                                set.add(strProp);                              
                            } else {
                                set.add(strSplit[i]);                  
                                log(Level.FINE, "hotSwapProperty", "strProp(" +
                                        "Adding multi value to multi valued " +
                                        "attribute): " + strSplit[i]);
                            }
                        }                            
                    } else {
                        if (isAutoGenKey) {
                            set.add("[" + (maxSetVal + 1) + "]=" + 
                                    strPropValue);
                            log(Level.FINE, "hotSwapProperty", "strProp : [0]=" 
                                    + strPropValue);
                        } else {

                            strProp = strPropValue;
                            set.add(strPropValue);                  
                            log(Level.FINE, "hotSwapProperty", "strProp(Adding "
                                    + "single value to multi valued attribute):"
                                    + strProp);
                        }
                    }
                } else {
                    if (isAutoGenKey) {
                        set.add("[0]=" + strPropValue);
                        log(Level.FINE, "hotSwapProperty", "strProp : [0]=" 
                                + strPropValue);
                    } else {
                       log(Level.FINE, "hotSwapProperty", "Setting " +
                                "single valued attribute): " + strPropValue);
                        set.add(strPropValue);
                    }
                }
                map.put(strPropName, set);
                modifyIdentity(amid, map);
            } else {
                log(Level.SEVERE, "hotSwapProperty", "Property : " + 
                        strPropName + "is NOT present. Check if property name" +
                        "is correct or if present, check the default " +
                        "initialisation value.");
            }
        } catch (Exception e) {
            log(Level.SEVERE, "hotSwapProperty", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
        exiting("hotSwapProperty");
    }
    
    /**
     * Checks if the property to be hotswapped is a list that needs an .
     * autogenerated key. 
     * strPropBundle       File which lists the properties that need auto gen 
     * keys
     * strProp             Property which needs to be checked.
     */
    public boolean isPropertyNumberedList(String strPropBundle, String strProp) 
            throws Exception {
        boolean isTrue=false;
        try {
            ResourceBundle rbl = ResourceBundle.getBundle(strPropBundle);
            ArrayList alProp = new ArrayList();
            String strLocalRB = strPropBundle.substring(strPropBundle.indexOf(
                    fileseparator) + 1, strPropBundle.length());
            int noofProp = new Integer(rbl.getString(strLocalRB + 
                    ".noOfProperties")).intValue();
            for (int i = 0; i < noofProp; i++) {
                alProp.add(rbl.getString(strLocalRB + i + ".name"));
            }
            if (alProp.contains(strProp)) {
                isTrue = true;
            } 
             return isTrue;       
        } catch (Exception e) {
            log(Level.SEVERE, "hotSwapProperty", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
    }

    /**
     * Assign a service to an identity such as a user or a role.
     * @param ssoToken - an <code>SSOToken</code> used to obtain the
     * <code>AMIdentity</code> object.
     * @param idName - the name of the identity to which the service should be
     * assigned.
     * @param idType - the type of the identity to which the service should be
     * assigned.
     * @param serviceName - the name of the service which should be assigned to
     * the identity.
     * @param idRealm - the name of the realm which the identity exist.
     * @param attrValues - a <code>String</code> containing the attribute values
     * which should be set in the service to be assigned.
     * @throws java.lang.Exception
     */
    public void assignSvcIdentity(SSOToken ssoToken, String idName,
            String idType, String serviceName, String idRealm,
            String attrValues)
    throws Exception {
        try {
            if (idName == null) {
                log(Level.SEVERE, "assignSvcIdentity",
                        "The identity name is null.");
                assert false;
            }
            if (idType == null) {
                log(Level.SEVERE, "assignSvcIdentity",
                        "The identity type is null.");
                assert false;
            }
            if (idRealm == null) {
                log(Level.SEVERE, "assignSvcIdentity",
                        "The identity realm is null.");
                assert false;
            }
            if (serviceName == null) {
                log(Level.SEVERE, "assignSvcIdentity",
                        "The service name is null.");
                assert false;
            }
            if (attrValues == null) {
                log(Level.SEVERE, "assignSvcIdentity",
                        "The attribute values is null.");
                assert false;
            }
            IdType type = getIdType(idType);
            if (!doesIdentityExists(idRealm, "realm", ssoToken, realm)) {
                log(Level.SEVERE, "assignSvcIdentity", "The realm " +
                        idRealm + " does not exist.");
                assert false;
            }
            if (!doesIdentityExists(idName, type, ssoToken, idRealm)) {
                log(Level.SEVERE, "assignSvcIdentity", "The " + idType +
                        " identity " + idName + " does not exist in realm " +
                        idRealm);
                assert false;
            }
            if (!doesRealmServiceExist(ssoToken, idRealm, serviceName)) {
                log(Level.SEVERE, "assignSvcIdentity", "The service " +
                        serviceName + " does not exist in realm " + idRealm);
                assert false;
            }
            AMIdentity amid = new AMIdentity(
                    ssoToken, idName, type, idRealm, null);
            amid.assignService(serviceName, setIDAttributes(attrValues));
        } catch (Exception e) {
            log(Level.SEVERE, "assignSvcIdentity", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Retrieve the services that can be assigned to a realm
     * @param idToken - an <code>SSOToken</code>
     * @param idRealm - a String containing the name of a realm
     * @param serviceName - a String containing the name of the service
     * which should be found in the realm
     * @return a <code>boolean</code> indicating whether the service can be
     * assigned in the realm
     * @throws Exception
     */
    public boolean doesRealmServiceExist(SSOToken idToken, String realm,
            String serviceName)
    throws Exception {
        try {
            OrganizationConfigManager ocm =
                    new OrganizationConfigManager(idToken, realm);
            Set serviceNames = ocm.getAssignableServices();
            AMIdentityRepository repo =
                    new AMIdentityRepository(idToken, realm);
            AMIdentity realmID = repo.getRealmIdentity();
            Set dynamicServices = realmID.getAssignableServices();
            if ((dynamicServices != null) && (!dynamicServices.isEmpty())) {
                if ((serviceNames != null) && (!serviceNames.isEmpty())) {
                    serviceNames.addAll(dynamicServices);
                } else {
                    serviceNames = dynamicServices;
                }
            }
            return serviceNames.contains(serviceName);
        } catch (Exception e) {
            log(Level.SEVERE, "doesRealmServiceExist", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
