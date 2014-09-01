/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AmRealmUserRegistry.java,v 1.5 2009/04/02 00:02:48 leiming Exp $
 *
 */

/*
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.agents.websphere;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.agents.arch.AgentBase;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.IApplicationSSOTokenProvider;
import com.sun.identity.agents.realm.AmRealmManager;
import com.sun.identity.agents.realm.IAmRealm;
import com.sun.identity.agents.realm.IRealmConfigurationConstants;
import com.sun.identity.agents.util.IUtilConstants;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.idm.IdRepoException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * This class delegates User Registry to Acess Manager realm's user repository.
 */
public class AmRealmUserRegistry extends AgentBase
        implements IAmRealmUserRegistry {
    public AmRealmUserRegistry(Manager manager) {
        super(manager);
    }
    
    public void initialize() throws AgentException {
        setAmRealm(AmRealmManager.getAmRealmInstance());
        initPrivilegedAttributeTypes();
        initPrivilegedAttributeTypeCases();
        initDefaultPrivilegedAttributeList();
        initPrivilegedAttributeMappingEnableFlag();
        if (isPrivilegedAttributeMappingEnabled()) {
            initPrivilegedAttributeMap();
        }
        initConfiguredOrganizationName();
        setAppSSOToken();
        if (isLogMessageEnabled()) {
            logMessage("AmRealmUserRegistry: Initialized.");
        }
    }
    
    public String checkCredentials(String userName, String password)
    throws AgentException {
        String result = null;
        if (isLogMessageEnabled()) {
            logMessage("DBG-a: AmRealmUserRegistry: userName: " + userName + 
                    "password: " + password);
        }
        if (userName != null && userName.trim().length() > 0) {
            if (userName.equals(AgentConfiguration.getApplicationUser())) {
                // Application Login
                logMessage("DBG-b: AmRealmUserRegistry: userName: " + 
                    AgentConfiguration.getApplicationUser());
                if (password != null &&
                        password.equals(
                        AgentConfiguration.getApplicationPassword())) {
                    result = userName;
                    if (isLogMessageEnabled()) {
                        logMessage("AmRealmUserRegistry.checkCredentials(): " +
                                "application auth granted for: " +
                                userName);
                    }
                }
            } else {
                boolean loginResult = authenticate(userName, password);
                if (loginResult) {
                    result = userName;
                    if (isLogMessageEnabled()) {
                        logMessage("AmRealmUserRegistry: user auth " +
                                "granted for: " + userName);
                    }
                }
            }
        }
        
        return result;
    }

    private boolean authenticate(String userName, String password) {

        boolean result = false;
        try {
            AuthContext authContext =
                    new AuthContext(AgentConfiguration.getOrganizationName());
            authContext.login(AuthContext.IndexType.MODULE_INSTANCE,
                    IApplicationSSOTokenProvider.MODULE_APPLICATION);
            if (authContext.hasMoreRequirements()) {
                Callback[] callbacks = authContext.getRequirements();
                if (callbacks != null) {
                    addLoginCallbackMessage(callbacks,
                            userName, password);
                    authContext.submitRequirements(callbacks);
                }
            }
            if (authContext.getStatus() == AuthContext.Status.SUCCESS) {
                result = true;
            }
        } catch (Exception ex) {
            logWarning("AmRealmUserRegistry.authenticate(): " +
                    "Failed to authenticate with user: " + userName,
                    ex);
        }

        if (result == true) {
            if (isLogMessageEnabled()) {
                logMessage("AmRealmUserRegistry.authenticate(): " +
                        "Authenticate successfully with user:" +
                        userName);
            }
        }

        return result;
    }

    private void addLoginCallbackMessage(
            Callback[] callbacks, String appUserName, String password)
            throws UnsupportedCallbackException {

        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback) callbacks[i];

                nameCallback.setName(appUserName);
            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback pwdCallback =
                        (PasswordCallback) callbacks[i];

                pwdCallback.setPassword(password.toCharArray());
            }
        }
    }
    
    public String getUserName(X509Certificate[] certs) throws AgentException {
        String result = null;
        if (certs != null && certs.length > 0) {
            X509Certificate x509cert = certs[0];
            if (x509cert != null ) {
                String userName = x509cert.getSubjectDN().getName();
                if (isValidUser(userName)) {
                    result = userName;
                }
            }
        }
        
        if (isLogMessageEnabled()) {
            logMessage("AmRealmUserRegistry: getUserName from certs: "
                    + result);
        }
        
        return result;
    }
    
    
    public List getUsers(String pattern, int limit)
    throws AgentException {
        List users = new ArrayList();
        try {
            SSOToken token = getAppSSOToken();
            if (token == null) {
                // have problem getting app sso token, no search can be done
                logError("AmRealmUserRegistry.getUsers: " +
                        "application auth failed.");
                return users;
            }
            // conduct a search in idrepo with agent's app sso token
            AMIdentityRepository idRepo = new AMIdentityRepository(
                    token, getConfiguredOrganizationName());
            IdSearchControl ctrl = new IdSearchControl();
            ctrl.setRecursive(true);
            ctrl.setMaxResults(limit);
            ctrl.setTimeOut(-1);
            IdSearchResults idsr =
                    idRepo.searchIdentities(IdType.USER, pattern, ctrl);
            Set searchRes = idsr.getSearchResults();
            Iterator iter = searchRes.iterator();
            while (iter.hasNext()) {
                AMIdentity user = (AMIdentity)iter.next();
                String universalId = getUniquePartOfUuid(
                        IdUtils.getUniversalId(user));
                users.add(universalId);
            }
        } catch (SSOException ssoe) {
            logError("AmRealmUserRegistry.getUsers: ", ssoe);
            throw new AgentException(ssoe);
        } catch (IdRepoException ide) {
            logError("AmRealmUserRegistry.getUsers: failed to get users", ide);
            throw new AgentException(ide);
        }
        if (isLogMessageEnabled()) {
            logMessage("AmRealmUserRegistry.getUsers: returned users: " + 
                    users);
        }
        return users;
    }
    
    public List getGroups(String pattern, int limit)
    throws AgentException {
        List groups = new ArrayList();
        try {
            SSOToken token = getAppSSOToken();
            if (token == null) {
                // have problem getting app sso token, no search can be done
                logError("AmRealmUserRegistry.getGroups: " +
                        "application auth failed.");
                return groups;
            }
            // conduct a search in idrepo with agent's app sso token
            AMIdentityRepository idRepo = new AMIdentityRepository(
                    token, getConfiguredOrganizationName());
            IdSearchControl ctrl = new IdSearchControl();
            ctrl.setRecursive(true);
            ctrl.setMaxResults(limit);
            ctrl.setTimeOut(-1);
            IdType[] types = getPrivilegedAttributeTypes();
            for (int i = 0; i < types.length; i++) {
                try {
                    IdSearchResults idsr =
                            idRepo.searchIdentities(types[i], pattern, ctrl);
                    Set searchRes = idsr.getSearchResults();
                    Iterator iter = searchRes.iterator();
                    while (iter.hasNext()) {
                        AMIdentity group = (AMIdentity)iter.next();
                        String universalId = getUniquePartOfUuid(
                                IdUtils.getUniversalId(group));
                        groups.add(universalId);
                    }
                } catch (IdRepoException ide) {
                    if (isLogWarningEnabled()) {
                        logWarning("AmRealmUserRegistry.getGroups: " +
                            "failed to get " + types[i],
                            ide);
                    }
                }
            }
        } catch (SSOException ssoe) {
            logError("AmRealmUserRegistry.getGroups: ", ssoe);
            throw new AgentException(ssoe);
        } catch (IdRepoException ide) {
            logError(
                "AmRealmUserRegistry.getGroups: failed to create Identity " +
                "Repository", ide);
            throw new AgentException(ide);
        }
        if (isLogMessageEnabled()) {
            logMessage("AmRealmUserRegistry.getGroups: returned groups: " + 
                    groups);
        }
        return groups;
    }
    
    public boolean isValidUser(String userName) throws AgentException {
        boolean result = false;
        
        List users = getUsers(userName, 1);
        if (!users.isEmpty()) {
            result = true;
        }
        if (isLogMessageEnabled()) {
            logMessage("AmRealmUserRegistry: isValidUser " + userName
                    + ": " + result);
        }
        
        return result;
    }
    
    public boolean isValidGroup(String groupName) throws AgentException {
        boolean result = false;
        
        List groups = getGroups(groupName, 1);
        if (!groups.isEmpty()) {
            result = true;
        }
        if (isLogMessageEnabled()) {
            logMessage("AmRealmUserRegistry: isValidGroup " + groupName
                    + ": " + result);
        }
        
        return result;
    }
    
    public List getMemberships(String userName) throws AgentException {
        List result = new ArrayList();
        Set members = getAmRealm().getMemberships(userName);
        if ((members == null) || (members.size() == 0)) {
            /* membership info is not found from the membership cache.
             * need to search the user repository with agent's application 
             * token.
             */
            members = new HashSet();
            members.addAll(getDefaultPrivilegedAttributeSet());
            try {
                // get agent app sso token
                SSOToken token = getAppSSOToken();
                if (token == null) {
                    // have problem getting app sso token, no search can be done
                    logError("AmRealmUserRegistry: application auth failed.");
                    throw new AgentException("application auth failed");
                }
                
                if (isAttributeFetchEnabled()) {
                    // conduct a search in idrepo with agent's app sso token
                    AMIdentityRepository idRepo = new AMIdentityRepository(
                            token, getConfiguredOrganizationName());
                    IdSearchControl ctrl = new IdSearchControl();
                    ctrl.setRecursive(true);
                    ctrl.setMaxResults(-1);
                    ctrl.setTimeOut(-1);
                    IdSearchResults idsr =
                            idRepo.searchIdentities(IdType.USER, userName, 
                            ctrl);
                    Set searchRes = idsr.getSearchResults();
                    Iterator iter = searchRes.iterator();
                    /* assume there is just one qualified user,
                     * pick the first one otherwise.
                     */
                    if (iter.hasNext()) {
                        AMIdentity user = (AMIdentity)iter.next();
                        IdType[] types = getPrivilegedAttributeTypes();
                        for (int i=0; i<types.length; i++) {
                            Boolean toLowerCaseStat =
                                (Boolean)getPrivilegedAttributeTypeCases().get(
                                types[i].getName());
                            // If users did not put an entry in the map, we
                            // will cover for that case too
                            if (toLowerCaseStat == null) {
                                toLowerCaseStat = new Boolean(false);
                                // for second time
                                getPrivilegedAttributeTypeCases().put(
                                        types[i].getName().toLowerCase(),
                                        toLowerCaseStat);
                            }
                            Set memberships = user.getMemberships(types[i]);
                            if (isLogMessageEnabled()) {
                                logMessage(
                                        "AmRealmUserRegistry: (" + types[i] +
                                        ") memberships returned from AM=" +
                                        memberships);
                            }
                            if (memberships != null && memberships.size() > 0) {
                                Iterator mIt = memberships.iterator();
                                while (mIt.hasNext()) {
                                    String universalId = getUniquePartOfUuid(
                                            IdUtils.getUniversalId(
                                            (AMIdentity) mIt.next()));
                                    if (toLowerCaseStat.booleanValue()) {
                                        universalId = universalId.toLowerCase();
                                    }
                                    String mappedId =
                                        getPrivilegedMappedAttribute(
                                            universalId);
                                    members.add(mappedId);
                                }
                            }
                        }
                    }
                }
            } catch (SSOException ssoe) {
                logError("AmRealmUserRegistry: " +
                        "failed to authenticate application : ", ssoe);
                throw new AgentException(ssoe);
            } catch (IdRepoException ide) {
                logError("AmRealmUserRegistry: "
                        + "failed to get user memberships for : "
                        + userName, ide);
                throw new AgentException(ide);
            }
        } 
        
        if ((members != null) && (members.size() > 0)) {
            result.addAll(members);
            Iterator mIt = members.iterator();
            while (mIt.hasNext()) {
                String id = (String)mIt.next();
                boolean idChanged = false;
                int idx = id.indexOf("=");
                if (idx >= 0) {
                    id = id.substring(idx + 1);
                    idChanged = true;
                }
                if (id != null) {
                    idx = id.indexOf(",");
                    if (idx >= 0) {
                        id = id.substring(0, idx);
                        idChanged = true;
                    }
                }
                if (idChanged) {
                    // add the common name as well
                    result.add(id);
                }
            }
        }
        if (isLogMessageEnabled()) {
            logMessage("AmRealmUserRegistry: getMemberships for " + userName
                    + ", result: " + result);
        }
        
        return result;
    }
    
    
    private void setAmRealm(IAmRealm amRealm) {
        _amRealm = amRealm;
    }
    
    private IAmRealm getAmRealm() {
        return _amRealm;
    }
    
    
    private void setAppSSOToken() throws AgentException {
        CommonFactory cf = new CommonFactory(getModule());
        IApplicationSSOTokenProvider provider =
                cf.newApplicationSSOTokenProvider();
        
        appSSOToken = provider.getApplicationSSOToken(true);
    }
    
    private SSOToken getAppSSOToken() throws AgentException {
        try {
            if ((appSSOToken == null) ||
                    !SSOTokenManager.getInstance().isValidToken(appSSOToken)) {
                setAppSSOToken();
            }
        } catch (SSOException ssoe) {
            logError("AmRealmUserRegistry.getAppSSOToken: " + " " +
                    "failed to get app sso token", ssoe);
            setAppSSOToken();
        }
        return appSSOToken;
    }
    
    
    private void initPrivilegedAttributeTypes()
    throws AgentException {
        ArrayList types = new ArrayList();
        String[] givenTypes = getConfigurationStrings(
                IRealmConfigurationConstants.CONFIG_FETCH_TYPE);
        try {
            if (givenTypes != null && givenTypes.length > 0) {
                for (int i = 0; i < givenTypes.length; i++) {
                    String nextType = givenTypes[i];
                    if (isLogMessageEnabled()) {
                        logMessage("AmRealmUserRegistry: Next configured type: "
                                + nextType);
                    }
                    IdType nextIdType = IdUtils.getType(nextType);
                    if (nextIdType != null) {
                        types.add(nextIdType);
                    } else {
                        throw new AgentException("Failed to resolve given "
                                + "privileged attribute type: " + nextType);
                    }
                }
            }
        } catch (IdRepoException ex) {
            throw new AgentException(
                    "Failed to identify privileged attribute types", ex);
        }
        IdType[] idTypes = new IdType[types.size()];
        System.arraycopy(types.toArray(), 0, idTypes, 0, types.size());
        setPrivilegedAttributeTypes(idTypes);
    }
    
    
    private void initPrivilegedAttributeTypeCases() {
        Map privAttrTypeCasesMap = getConfigurationMap(
                IRealmConfigurationConstants.CONFIG_PRIVILEGED_ATTR_CASE);
        if (privAttrTypeCasesMap != null) {
            Iterator iter = privAttrTypeCasesMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry me = (Map.Entry)iter.next();
                String key = (String)me.getKey();
                String val = (String)me.getValue();
                if ((key != null) && (key.length() > 0)) {
                    if (val != null) {
                        Boolean caseStat = Boolean.valueOf(val);
                        // We need to convert attr types to lower cases so
                        // that they match with IdType.getName()
                        getPrivilegedAttributeTypeCases().put(
                                key.toLowerCase(),caseStat);
                    }
                }
            }
        }
    }
    
    private void initConfiguredOrganizationName() {
        configuredOrgName = getConfigurationString(CONFIG_ORGANIZATION_NAME, 
                "/");
        
        if (isLogMessageEnabled()) {
            logMessage(
                    "AmRealmUserRegistry: agent configured organization name: "
                    + getConfiguredOrganizationName());
        }
    }
    
    
    private void initDefaultPrivilegedAttributeList() {
        String[] defaultAttributeList = getConfigurationStrings(
            IRealmConfigurationConstants.CONFIG_DEFAULT_PRIVILEGE_ATTR_LIST);
        
        if (defaultAttributeList != null && defaultAttributeList.length >0) {
            for (int i=0; i<defaultAttributeList.length; i++) {
                String nextAttr = defaultAttributeList[i];
                if (nextAttr != null && nextAttr.trim().length() > 0) {
                    getDefaultPrivilegedAttributeSet().add(nextAttr);
                }
            }
        }
        
        if (isLogMessageEnabled()) {
            logMessage("AmRealmUserRegistry: Default privileged attribute set: "
                    + getDefaultPrivilegedAttributeSet());
        }
    }
    
    private boolean isAttributeFetchEnabled() {
        return getPrivilegedAttributeTypes().length > 0;
    }
    
    private void setPrivilegedAttributeTypes(IdType[] types) {
        privilegedAttributeTypes = types;
        
        if (isLogMessageEnabled()) {
            StringBuffer buff = new StringBuffer(
                    "AmRealmUserRegistry: Configured Attribute Types:");
            buff.append(IUtilConstants.NEW_LINE);
            for (int i=0; i<types.length; i++) {
                buff.append("[").append(i).append("]: ");
                buff.append(types[i].getName()).append(
                        IUtilConstants.NEW_LINE);
            }
            buff.append("Total Configugured Attribute Types: " + 
                    types.length);
            logMessage(buff.toString());
        }
    }
    
    
    private IdType[] getPrivilegedAttributeTypes() {
        return privilegedAttributeTypes;
    }
    
    private String getConfiguredOrganizationName() {
        return configuredOrgName;
    }
    
    
    private HashSet getDefaultPrivilegedAttributeSet() {
        return defaultPrivilegedAttributeSet;
    }
    
    
    private HashMap getPrivilegedAttributeTypeCases() {
        return privilegedAttributeTypeCases;
    }
    
    private String getPrivilegedMappedAttribute(String originalAttribute) {
        String mappedAttribute = originalAttribute;
        if (isPrivilegedAttributeMappingEnabled()) {
            Map privilegedAttributeMap = getPrivilegedAttributeMap();
            if (privilegedAttributeMap != null &&
                    originalAttribute != null) {
                mappedAttribute = (String)privilegedAttributeMap.get(
                        originalAttribute);
            }
            if (mappedAttribute == null) {
                mappedAttribute = originalAttribute;
            }
        }
        return mappedAttribute;
    }
    
    private void initPrivilegedAttributeMappingEnableFlag() {
        privilegedAttributeMappingEnabled = getConfigurationBoolean(
                IRealmConfigurationConstants.
                    CONFIG_PRIVILEGED_ATTRIBUTE_MAPPING_ENABLED,
                IRealmConfigurationConstants.
                    DEFAULT_PRIVILEGED_ATTRIBUTE_MAPPING_ENABLED
                );
        if (isLogMessageEnabled()) {
            logMessage(
                    "AmRealmUserRegistry: Using privileged attribute mapping " +
                    "enabled flag: " + privilegedAttributeMappingEnabled);
        }
    }
    
    private void initPrivilegedAttributeMap() {
        privilegedAttributeMap = getConfigurationMap(
            IRealmConfigurationConstants.CONFIG_PRIVILEGED_ATTRIBUTE_MAPPING);
        if (isLogMessageEnabled()) {
            logMessage("AmRealmUserRegistry: privileged attribute mapping: " +
                    privilegedAttributeMap);
        }
    }
    
    private boolean isPrivilegedAttributeMappingEnabled() {
        return privilegedAttributeMappingEnabled;
    }
    
    private Map getPrivilegedAttributeMap() {
        return privilegedAttributeMap;
    }
    
    private String getUniquePartOfUuid(String uuid) {
        
        String uuidStripped = null;
        if ((uuid != null) && (uuid.length() > 0)) {
            int first = uuid.indexOf(AMSDKDN_DELIMITER);
            if(first != -1) {
                uuidStripped =  uuid.substring(0,first);
            } else { // fall back to original string
                uuidStripped = uuid;
            }
        }
        if (uuidStripped != null) {
            uuidStripped = uuidStripped.toLowerCase();
        }
        
        if (isLogMessageEnabled()) {
            logMessage("AmRealm: Unique part of uuid = " +
                    uuidStripped);
        }
        return uuidStripped;
    }
    
    private static String AMSDKDN_DELIMITER = ",amsdkdn=";
    private static String CONFIG_ORGANIZATION_NAME = "organization.name";
    
    private IAmRealm _amRealm;
    private SSOToken appSSOToken = null;
    private IdType[] privilegedAttributeTypes;
    private HashMap privilegedAttributeTypeCases = new HashMap();
    private String configuredOrgName;
    private HashSet defaultPrivilegedAttributeSet = new HashSet();
    private boolean privilegedAttributeMappingEnabled;
    private Map privilegedAttributeMap = null;
}
