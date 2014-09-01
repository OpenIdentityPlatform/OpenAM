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
 * $Id: AMSDKRepo.java,v 1.28 2009/12/25 05:54:05 hengming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.iplanet.am.sdk;

import com.iplanet.am.sdk.common.IDirectoryServices;
import com.iplanet.am.util.AdminUtils;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.Server;
import com.iplanet.services.ldap.ServerInstance;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdRepoFatalException;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.idm.IdRepoUnsupportedOpException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.idm.RepoSearchResults;
import com.sun.identity.idm.plugins.ldapv3.LDAPAuthUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.LDAPUtilException;

public class AMSDKRepo extends IdRepo {

    protected static Set listeners = new HashSet();

    private Map supportedOps = new HashMap();

    private IdRepoListener myListener = null;

    // private Map configMap = new AMHashMap();
    private String orgDN = "";

    private boolean dataStoreRecursive = false;

    private String pcDN = null;

    private String agentDN = null;

    private static Debug debug =  Debug.getInstance("amsdkRepo");

    private static final String PC_ATTR = "iplanet-am-admin-console-default-pc";

    private static final String AC_ATTR = "iplanet-am-admin-console-default-ac";

    private static final String GC_ATTR = "iplanet-am-admin-console-default-gc";

    private static final String ADMIN_SERVICE = "iPlanetAMAdminConsoleService";

    private static final String CLASS_NAME = "com.iplanet.am.sdk.AMSDKRepo";

    private static SSOToken adminToken = null;

    private static AMStoreConnection sc = null;

    protected String amAuthLDAP = "amAuthLDAP";

    public AMSDKRepo() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#addListener(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.AMObjectListener, java.util.Map)
     */
    public int addListener(SSOToken token, IdRepoListener listnr)
            throws IdRepoException, SSOException {
        // TODO Auto-generated method stub
        synchronized (listeners) {
            listeners.add(listnr);
        }
        myListener = listnr;
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#removeListener()
     */
    public void removeListener() {
        synchronized (listeners) {
            listeners.remove(myListener);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#create(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Map)
     */
    public String create(SSOToken token, IdType type, String name, Map attrMap)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMSDKIdRepo: Create called on " + type + ": "
                    + name);
        }
        String dn = null;
        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        try {
            int orgType = amsc.getAMObjectType(orgDN);
            if (orgType != AMObject.ORGANIZATION) {
                debug.error("AMSDKRepo.create(): Incorrectly configured "
                        + " plugin: Org DN is wrong = " + orgDN);
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "303", 
                        null);
            }
        } catch (AMException ame) {
            debug.error("AMSDKRepo.create(): An exception occured while "
                    + " initializing AM SDK ", ame);
            Object[] args = { CLASS_NAME, IdOperation.CREATE.getName() };
            IdRepoException ide = new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                    "304", args);
            ide.setLDAPErrorCode(ame.getLDAPErrorCode());
            throw ide;
        }
        AMOrganization amOrg = amsc.getOrganization(orgDN);
        Map entityNamesAndAttrs = new HashMap();
        entityNamesAndAttrs.put(name, attrMap);
        try {
            if (type.equals(IdType.USER)) {
                Set res = amOrg.createEntities(AMObject.USER,
                        entityNamesAndAttrs);
                AMEntity entity = (AMEntity) res.iterator().next();
                dn = entity.getDN();
            } else if (type.equals(IdType.AGENT)) {
                Set res = amOrg.createEntities(100, entityNamesAndAttrs);
                AMEntity entity = (AMEntity) res.iterator().next();
                dn = entity.getDN();
            } else if (type.equals(IdType.GROUP)) {
                String gcDN = AMNamingAttrManager
                        .getNamingAttr(AMObject.GROUP_CONTAINER)
                        + "=" + getDefaultGroupContainerName() + "," + orgDN;
                AMGroupContainer amgc = amsc.getGroupContainer(gcDN);
                Set groups = amgc.createStaticGroups(entityNamesAndAttrs);
                AMStaticGroup group = (AMStaticGroup) groups.iterator().next();
                dn = group.getDN();
            } else if (type.equals(IdType.ROLE)) {
                Set roles = amOrg.createRoles(entityNamesAndAttrs);
                AMRole role = (AMRole) roles.iterator().next();
                dn = role.getDN();
            } else if (type.equals(IdType.FILTEREDROLE)) {
                Set roles = amOrg.createFilteredRoles(entityNamesAndAttrs);
                AMFilteredRole role = (AMFilteredRole) roles.iterator().next();
                dn = role.getDN();
            }
        } catch (AMException ame) {
            debug.warning("AMSDKRepo.create(): Caught AMException..", ame);
            throw IdUtils.convertAMException(ame);
        }

        return dn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#delete(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public void delete(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug
                    .message("AMSDKIdRepo: Delete called on " + type + ": "
                            + name);
        }
        AMOrganization amOrg = checkAndGetOrg(token);
        Set entitySet = new HashSet();

        try {
            String eDN = getDN(type, name);
            entitySet.add(eDN);
            if (type.equals(IdType.USER)) {

                amOrg.deleteUsers(entitySet);
            } else if (type.equals(IdType.AGENT)) {

                amOrg.deleteEntities(100, entitySet);
            } else if (type.equals(IdType.GROUP)) {
                amOrg.deleteStaticGroups(entitySet);
            } else if (type.equals(IdType.ROLE)) {
                amOrg.deleteRoles(entitySet);
            } else if (type.equals(IdType.FILTEREDROLE)) {
                amOrg.deleteFilteredRoles(entitySet);
            }
        } catch (AMException ame) {
            debug.error("AMSDKRepo.delete(): Caught AMException...", ame);
            throw IdUtils.convertAMException(ame);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Set)
     */
    public Map getAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {

        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        String dn = getDN(type, name);
        int profileType = getProfileType(type);
        if (debug.messageEnabled()) {
            debug.message("AMSDKIdRepo: getAttributes called" + ": " + type
                    + ": " + name + " DN: '" + dn + "'");
        }

        // Use adminToken if present
        if (adminToken != null) {
            token = adminToken;
        }
        try {
            if (amsc.isValidEntry(dn)) {
                IDirectoryServices dsServices = AMDirectoryAccessFactory
                        .getDirectoryServices();
                return dsServices.getAttributes(token, dn, attrNames, false,
                        false, profileType);
            } else {
                Object[] args = { name };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "202", 
                        args);
            }
        } catch (AMException ame) {
            debug.error("AMSDKRepo.getAttributes(): AMException ", ame);
            throw IdUtils.convertAMException(ame);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public Map getAttributes(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {

        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        String dn = getDN(type, name);
        int profileType = getProfileType(type);

        if (debug.messageEnabled()) {
            debug.message("AMSDKIdRepo: getAttributes called" + ": " + type
                    + ": " + name + " DN: '" + dn + "'");
        }

        // Use adminToken if present
        if (adminToken != null) {
            token = adminToken;
        }
        try {
            if (amsc.isValidEntry(dn)) {
                IDirectoryServices dsServices = AMDirectoryAccessFactory
                        .getDirectoryServices();
                return dsServices.getAttributes(token, dn, false, false,
                        profileType);
            } else {
                Object[] args = { name };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "202", 
                        args);
            }
        } catch (AMException ame) {
            debug.error("AMSDKRepo.getAttributes(): AMException ", ame);
            throw IdUtils.convertAMException(ame);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getBinaryAttributes(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.util.Set)
     */
    public Map getBinaryAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMSDKIdRepo: getBinaryAttributes called" + ": "
                    + type + ": " + name);
        }

        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        String dn = getDN(type, name);
        int profileType = getProfileType(type);
        // Use adminToken if present
        if (adminToken != null) {
            token = adminToken;
        }
        try {
            if (amsc.isValidEntry(dn)) {
                IDirectoryServices dsServices = AMDirectoryAccessFactory
                        .getDirectoryServices();
                return dsServices.getAttributesByteValues(token, dn, attrNames,
                        profileType);
            } else {
                Object[] args = { name };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "202", 
                        args);
            }
        } catch (AMException ame) {
            debug.error("AMSDKRepo.getBinaryAttributes(): AMException ", ame);
            throw IdUtils.convertAMException(ame);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getConfiguration()
     */
    public Map getConfiguration() {
        return super.getConfiguration();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getMembers(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String,
     *      com.sun.identity.idm.IdType)
     */
    public Set getMembers(SSOToken token, IdType type, String name,
            IdType membersType) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: getMembers called" + type + ": " + name
                    + ": " + membersType);
        }
        Set results;
        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        String dn = null;
        if (type.equals(IdType.USER) || type.equals(IdType.AGENT)) {
            debug.error("AMSDKRepo: Membership operation is not supported "
                    + " for Users or Agents");
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "203", null);

        } else if (type.equals(IdType.GROUP)) {
            dn = getDN(type, name);
            AMStaticGroup group = amsc.getStaticGroup(dn);
            if (membersType.equals(IdType.USER)) {
                try {
                    results = group.getUserDNs();
                } catch (AMException ame) {
                    debug.error("AMSDKRepo: Unable to get user memberships"
                            + " for group" + dn, ame);
                    Object[] args = { CLASS_NAME, membersType.getName(),
                            type.getName(), name };
                    IdRepoException ide = new IdRepoException(
                            IdRepoBundle.BUNDLE_NAME, "205", args);
                    ide.setLDAPErrorCode(ame.getLDAPErrorCode());
                    throw ide;
                }
            } else {
                debug.error("AMSDKRepo: Groups do not supported membership "
                        + "for " + membersType.getName());
                Object[] args = { CLASS_NAME, membersType.getName(),
                        type.getName() };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "204", 
                        args);
            }

        } else if (type.equals(IdType.ROLE)) {
            dn = getDN(type, name);
            AMRole role = amsc.getRole(dn);
            if (membersType.equals(IdType.USER)) {
                try {
                    results = role.getUserDNs();
                } catch (AMException ame) {
                    debug.error("AMSDKRepo: Unable to get user memberships "
                            + "for role " + dn, ame);
                    throw IdUtils.convertAMException(ame);
                }
            } else {
                Object[] args = { CLASS_NAME, membersType.getName(),
                        type.getName() };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "204", 
                        args);
            }

        } else if (type.equals(IdType.FILTEREDROLE)) {
            dn = getDN(type, name);
            AMFilteredRole role = amsc.getFilteredRole(dn);
            if (membersType.equals(IdType.USER)) {
                try {
                    results = role.getUserDNs();
                } catch (AMException ame) {
                    debug.error(
                            "AMSDKRepo: Unable to get user memberships for "
                                    + "role " + dn, ame);
                    throw IdUtils.convertAMException(ame);
                }
            } else {
                Object[] args = { CLASS_NAME, membersType.getName(),
                        type.getName() };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "204", 
                        args);
            }

        } else {
            Object[] args = { CLASS_NAME, IdOperation.READ.getName(),
                    type.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }
        return results;
    }

    public Set getMemberships(SSOToken token, IdType type, String name,
            IdType membershipType) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: getMemberships called" + type + ": "
                    + name + ": " + membershipType);
        }
        Set results;
        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        String dn = null;
        if (!type.equals(IdType.USER)) {
            debug.error("AMSDKRepo: Membership for identities other than "
                    + " Users is not allowed ");
            Object[] args = { CLASS_NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "206", args);

        } else {
            dn = getDN(type, name);
            AMUser user = amsc.getUser(dn);
            if (membershipType.equals(IdType.GROUP)) {
                try {
                    results = user.getStaticGroupDNs();
                } catch (AMException ame) {
                    debug.error("AMSDKRepo: Unable to get user's group "
                            + "memberships " + dn, ame);
                    Object[] args = { CLASS_NAME, membershipType.getName(),
                            type.getName(), name };
                    IdRepoException ide = new IdRepoException(
                            IdRepoBundle.BUNDLE_NAME, "207", args);
                    ide.setLDAPErrorCode(ame.getLDAPErrorCode());
                    throw ide;
                }
            } else if (membershipType.equals(IdType.ROLE)) {
                try {
                    results = user.getRoleDNs();
                } catch (AMException ame) {
                    debug.error("AMSDKRepo: Unable to get roles of a user "
                            + dn, ame);
                    throw IdUtils.convertAMException(ame);
                }
            } else if (membershipType.equals(IdType.FILTEREDROLE)) {

                try {
                    results = user.getFilteredRoleDNs();
                } catch (AMException ame) {
                    debug.error("AMSDKRepo: Unable to get user memberships "
                            + "for role " + dn, ame);
                    throw IdUtils.convertAMException(ame);
                }
            } else { // Memberships of any other types not supported for
                // users.
                debug.error("AMSDKRepo: Membership for other types of "
                        + "entities " + " not supported for Users");
                Object args[] = { CLASS_NAME, type.getName(),
                        membershipType.getName() };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "204", 
                        args);
            }
        }
        return results;
    }

    public Set getSupportedOperations(IdType type) {
        return (Set) supportedOps.get(type);
    }

    public Set getSupportedTypes() {

        return supportedOps.keySet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#initialize(java.util.Map)
     */
    public void initialize(Map configParams) throws IdRepoException {

        super.initialize(configParams);
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: Initializing configuration: "
                    + configMap.toString());
        }
        Set orgs = (Set) configMap.get("amSDKOrgName");
        if (orgs != null && !orgs.isEmpty()) {
            orgDN = (String) orgs.iterator().next();
        } else {
            orgDN = AMStoreConnection.getAMSdkBaseDN();
        }
        if (adminToken == null) {
            adminToken = (SSOToken) AccessController
                    .doPrivileged(AdminTokenAction.getInstance());
            try {
                sc = new AMStoreConnection(adminToken);
            } catch (SSOException ssoe) {
                // do nothing ... but log the error
                debug.error("AMSDKRepo:Initialize..Failed to initialize "
                        + " AMStoreConnection...", ssoe);
            }
        }

        Set consoleRecursiveFlg =
            (Set) configMap.get("sun-idrepo-amSDK-config-recursive-enabled");
        if ((consoleRecursiveFlg != null) &&
            (!consoleRecursiveFlg.isEmpty())) {
            if (consoleRecursiveFlg.contains("true")) {
                dataStoreRecursive = true;
            }
        }
        Set pcNameSet = (Set)configMap.get(
            "sun-idrepo-amSDK-config-people-container-name");
        if ((pcNameSet != null) && (!pcNameSet.isEmpty())) {
            String pcName = (String) pcNameSet.iterator().next();
            Set pcValueSet = (Set) configMap.get(
                "sun-idrepo-amSDK-config-people-container-value");
            if ((pcName != null) && (pcValueSet != null) &&
                (!pcValueSet.isEmpty())) {
                String pcValue = (String) pcValueSet.iterator().next();
                pcDN = pcName + "=" + pcValue + "," + orgDN;
            }
        }
        Set agentNameSet = (Set)configMap.get(
            "sun-idrepo-amSDK-config-agent-container-name");
        if ((agentNameSet != null) && (!agentNameSet.isEmpty())) {
            String agentName = (String) agentNameSet.iterator().next();
            Set agentValueSet = (Set) configMap.get(
                "sun-idrepo-amSDK-config-agent-container-value");
            if ((agentName != null) && (agentValueSet != null) &&
                (!agentValueSet.isEmpty())) {
                String agentValue = (String) agentValueSet.iterator().next();
                agentDN = agentName + "=" + agentValue + "," + orgDN;
            }
        }
        loadSupportedOps();
    }

    public boolean isExists(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: isExists called " + type + ": " + name);
        }
        AMStoreConnection amsc = (sc == null) ?
            new AMStoreConnection(token) : sc;
        try {
            String dn = getDN(type, name);
            return amsc.isValidEntry(dn);
        } catch (IdRepoException ide) {
            return false;
        }
    }

    public boolean isActive(SSOToken token, IdType type, String name)
            throws SSOException {
        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        try {
            String dn = getDN(type, name);
            AMEntity entity = amsc.getEntity(dn);
            return entity.isActivated();

        } catch (AMException ame) {
            return false;
        } catch (IdRepoException ide) {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see com.sun.identity.idm.IdRepo#setActiveStatus(
        com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
        java.lang.String, boolean)
     */
    public void setActiveStatus(SSOToken token, IdType type,
        String name, boolean active)
        throws IdRepoException, SSOException {
        AMStoreConnection amsc = (sc == null) ?
            new AMStoreConnection(token) : sc;
        try {
            String dn = getDN(type, name);
            AMEntity entity = amsc.getEntity(dn);
            if (active) {
                entity.activate();
            } else {
                entity.deactivate();
            }
        } catch (AMException ame) {
            debug.error("AMSDKRepo.setActiveStatus: Caught AMException", ame);
            throw IdUtils.convertAMException(ame);
        }
    }

    public void modifyMemberShip(SSOToken token, IdType type, String name,
            Set members, IdType membersType, int operation)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: modifyMemberShip called " + type + ": "
                    + name + ": " + members + ": " + membersType);
        }
        if (members == null || members.isEmpty()) {
            debug.error("AMSDKRepo.modifyMemberShip: Members set is empty");
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "201", null);
        }
        if (type.equals(IdType.USER) || type.equals(IdType.AGENT)) {
            debug.error("AMSDKRepo.modifyMembership: Memberhsip to users and"
                    + " agents is not supported");
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "203", null);
        }
        if (!membersType.equals(IdType.USER)) {
            debug.error("AMSDKRepo.modifyMembership: A non-user type cannot "
                    + " be made a member of any identity"
                    + membersType.getName());
            Object[] args = { CLASS_NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "206", args);
        }
        Set usersSet = new HashSet();
        Iterator it = members.iterator();
        while (it.hasNext()) {
            String curr = (String) it.next();
            String dn = getDN(membersType, curr);
            usersSet.add(dn);
        }
        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        if (type.equals(IdType.GROUP)) {
            String gdn = getDN(type, name);
            AMStaticGroup group = amsc.getStaticGroup(gdn);
            try {
                switch (operation) {
                case ADDMEMBER:
                    group.addUsers(usersSet);
                    break;
                case REMOVEMEMBER:
                    group.removeUsers(usersSet);
                }
            } catch (AMException ame) {
                debug.error("AMSDKRepo.modifyMembership: Caught "
                        + "exception while adding users to groups", ame);
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.ROLE)) {
            String gdn = getDN(type, name);
            AMRole role = amsc.getRole(gdn);
            try {
                switch (operation) {
                case ADDMEMBER:
                    role.addUsers(usersSet);
                    break;
                case REMOVEMEMBER:
                    role.removeUsers(usersSet);
                }
            } catch (AMException ame) {
                debug.error("AMSDKRepo.modifyMembership: Caught "
                        + "exception while " + " adding/removing users"
                        + " to roles", ame);
                throw IdUtils.convertAMException(ame);
            }
        } else {
            // throw an exception
            debug.error("AMSDKRepo.modifyMembership: Memberships cannot be"
                    + "modified for type= " + type.getName());
            Object[] args = { CLASS_NAME, type.getName() };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "209", args);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#removeAttributes(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Set)
     */
    public void removeAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {
        // TODO Auto-generated method stub
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: removeAttributes called " + type + ": "
                    + name + attrNames);
        }

        // Will do later. NOT BEING USED yet.

        // NOT YET Implemented !!!!!!
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#search(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Map, 
     *      boolean, int, int, java.util.Set)
     */
    public RepoSearchResults search(SSOToken token, IdType type,
            String pattern, Map avPairs, boolean recursive, int maxResults,
            int maxTime, Set returnAttrs) throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: search called" + type + ": " + pattern
                    + ": " + avPairs);
        }
        String searchDN = orgDN;
        int profileType = getProfileType(type);
        if (type.equals(IdType.USER)) {
            searchDN = "ou=" + getDefaultPeopleContainerName() + "," + orgDN;
        } else if (type.equals(IdType.AGENT)) {
            searchDN = "ou=" + getDefaultAgentContainerName() + "," + orgDN;
        } else if (type.equals(IdType.GROUP)) {
            searchDN = "ou=" + getDefaultGroupContainerName() + "," + orgDN;
        }
        // String avFilter = AMObjectImpl.constructFilter(avPairs);
        AMSearchControl ctrl = new AMSearchControl();
        ctrl.setMaxResults(maxResults);
        ctrl.setTimeOut(maxTime);
        ctrl.setSearchScope(AMConstants.SCOPE_ONE);
        if (returnAttrs == null || returnAttrs.isEmpty()) {
            ctrl.setAllReturnAttributes(true);
        } else {
            ctrl.setReturnAttributes(returnAttrs);
        }
        AMSearchResults results;
        try {
            AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                    : sc;
            switch (profileType) {
            case AMObject.USER:
                AMPeopleContainer pc = amsc.getPeopleContainer(searchDN);
                if (avPairs == null || avPairs.isEmpty()) {
                    results = pc.searchUsers(pattern, avPairs, ctrl);
                } else {
                    // avPairs is being passed. Create an OR condition
                    // filter.
                    String avFilter = constructFilter(IdRepo.OR_MOD, avPairs);
                    results = pc.searchUsers(pattern, ctrl, avFilter);
                }
                if (recursive) {
                    // It could be an Auth
                    // search and if no matching user found then we need
                    // to do a scope-sub search
                    Set usersFound = results.getSearchResults();
                    if (usersFound == null || usersFound.isEmpty()) {
                        // SCOPE_SUB search to find exactly one user.
                        // Throw an exception if more than one
                        // matching is found.
                        if (avPairs == null || avPairs.isEmpty()) {
                            AMOrganization org = amsc.getOrganization(orgDN);
                            ctrl.setSearchScope(AMConstants.SCOPE_SUB);
                            results = org.searchUsers(pattern, ctrl);
                        } else {
                            String avFilter = constructFilter(IdRepo.OR_MOD,
                                    avPairs);
                            AMOrganization org = amsc.getOrganization(orgDN);
                            ctrl.setSearchScope(AMConstants.SCOPE_SUB);
                            results = org.searchUsers("*", ctrl, avFilter);
                        }
                    }
                }
                break;
            case 100:
                AMOrganizationalUnit ou = amsc.getOrganizationalUnit(searchDN);
                results = ou.searchEntities(pattern, avPairs, null, ctrl);
                // results = ou.searchEntities(pattern, ctrl, avFilter, null);
                break;
            case AMObject.GROUP:
            case AMObject.STATIC_GROUP:
                AMGroupContainer gc = amsc.getGroupContainer(searchDN);
                results = gc.searchGroups(pattern, avPairs, ctrl);
                break;
            case AMObject.ROLE:
                AMOrganization org = amsc.getOrganization(searchDN);
                results = org.searchRoles(pattern, ctrl);
                break;
            case AMObject.FILTERED_ROLE:
                org = amsc.getOrganization(searchDN);
                results = org.searchFilteredRoles(pattern, ctrl);
                break;
            default:
                Object[] args = { CLASS_NAME, type.getName() };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "210", 
                        args);
            }
        } catch (AMException ame) {
            debug.error("AMSDKRepo.search: Unable to perform search operation",
                    ame);
            ;
            throw IdUtils.convertAMException(ame);
        }
        return new RepoSearchResults(results.getSearchResults(), results
                .getErrorCode(), results.getResultAttributes(), type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#search(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, int, int,
     *      java.util.Set, boolean, int, java.util.Map)
     */
    public RepoSearchResults search(SSOToken token, IdType type,
            String pattern, int maxTime, int maxResults, Set returnAttrs,
            boolean returnAllAttrs, int filterOp, Map avPairs, 
            boolean recursive)
            throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: search called" + type + ": " + pattern
                    + ": " + avPairs);
        }
        String searchDN = orgDN;
        int profileType = getProfileType(type);
        if (type.equals(IdType.GROUP)) {
            searchDN = "ou=" + getDefaultGroupContainerName() + "," + orgDN;
        }
        AMSearchControl ctrl = new AMSearchControl();
        ctrl.setMaxResults(maxResults);
        ctrl.setTimeOut(maxTime);
        ctrl.setSearchScope(AMConstants.SCOPE_ONE);
        if (returnAllAttrs) {
            ctrl.setAllReturnAttributes(true);
        } else {
            if (returnAttrs != null && !returnAttrs.isEmpty()) {
                ctrl.setReturnAttributes(returnAttrs);
            }
        }
        AMSearchResults results;
        try {
            AMStoreConnection amsc = 
                (sc == null) ? new AMStoreConnection(token) : sc;
            switch (profileType) {
            case AMObject.USER:
                if (pcDN != null) {
                    if (!dataStoreRecursive) {
                        searchDN = pcDN;
                    } else {
                        ctrl.setSearchScope(AMConstants.SCOPE_SUB);
                    }
                } else {
                    if (!dataStoreRecursive) {
                        searchDN = "ou=" + getDefaultPeopleContainerName() +
                            "," + orgDN;
                    } else {
                        ctrl.setSearchScope(AMConstants.SCOPE_SUB);
                    }
                }

                AMPeopleContainer pc = amsc.getPeopleContainer(searchDN);
                if (avPairs == null || avPairs.isEmpty()) {
                    results = pc.searchUsers(pattern, avPairs, ctrl);
                } else {
                    // avPairs is being passed. Create an OR condition
                    // filter.
                    String avFilter = constructFilter(filterOp, avPairs);
                    results = pc.searchUsers(pattern, ctrl, avFilter);
                }
                break;
            case 100:
                // IdType is Agent.
                if (agentDN != null) {
                    if (!dataStoreRecursive) {
                        searchDN = agentDN;
                    } else {
                        ctrl.setSearchScope(AMConstants.SCOPE_SUB);
                    }
                } else {
                    if (!dataStoreRecursive) {
                        searchDN = "ou=" + getDefaultAgentContainerName() +
                            "," + orgDN;
                    } else {
                        ctrl.setSearchScope(AMConstants.SCOPE_SUB);
                    }
                }
                AMOrganizationalUnit ou = amsc.getOrganizationalUnit(searchDN);
                // fix 6515502
                if (avPairs == null || avPairs.isEmpty()) {
                    results = ou.searchEntities(pattern, avPairs, null, ctrl);
                } else {
                    // avPairs is being passed. Create an OR condition
                    // filter.
                    String avFilter = constructFilter(filterOp, avPairs);
                    results = ou.searchEntities(pattern, ctrl, avFilter, null);
                }
                break;
            case AMObject.GROUP:
            case AMObject.STATIC_GROUP:
                AMGroupContainer gc = amsc.getGroupContainer(searchDN);
                results = gc.searchStaticGroups(pattern, avPairs, ctrl);
                break;
            case AMObject.ROLE:
                AMOrganization org = amsc.getOrganization(searchDN);
                results = org.searchRoles(pattern, ctrl);
                break;
            case AMObject.FILTERED_ROLE:
                org = amsc.getOrganization(searchDN);
                results = org.searchFilteredRoles(pattern, ctrl);
                break;
            default:
                Object[] args = { CLASS_NAME, type.getName() };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "210", 
                        args);
            }
        } catch (AMException ame) {
            String amErrorCode = ame.getErrorCode();
            if (!amErrorCode.equals("341")) {
                debug.error(
                        "AMSDKRepo.search: Unable to perform search operation",
                        ame);
            }
            if (profileType == 100 && amErrorCode.equals("341")) {
                // Agent profile type...if container does not exist
                // then return empty results
                return new RepoSearchResults(new HashSet(),
                        RepoSearchResults.SUCCESS, Collections.EMPTY_MAP, type);
            }
            throw IdUtils.convertAMException(ame);
        }
        return new RepoSearchResults(results.getSearchResults(), results
                .getErrorCode(), results.getResultAttributes(), type);
    }

    /**
     * Sets the Attributes of the named identity. the single sign on
     * token must have the necessary permission to set the attributes.
     * 
     * @param token
     *            single sign on token for this operation.
     * @param type 
     *            type of the identity
     * @param name 
     *            name of the identity
     * @param attributes 
     *            attributes to set.
     * @param isAdd
     *            should attributes values be added to existing values.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public void setAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd) 
        throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            if (attributes.containsKey("userpassword")) {
                AMHashMap removedPasswd = new AMHashMap();
                removedPasswd.copy(attributes);
                removedPasswd.remove("userpassword");
                removedPasswd.put("userpassword", "xxx...");
                debug.message("AMSDKRepo: setAttributes called" + type + ": "
                        + name + ": " + removedPasswd);
            } else {
                debug.message("AMSDKRepo: setAttributes called" + type + ": "
                        + name + ": " + attributes);
            }
        }

        if (attributes == null || attributes.isEmpty()) {
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "201", null);
        }

        String dn = getDN(type, name);
        int profileType = getProfileType(type);
        try {
            if (adminToken != null) {
                token = adminToken;
            }
            IDirectoryServices dsServices = AMDirectoryAccessFactory
                    .getDirectoryServices();
            dsServices.setAttributes(token, dn, profileType, attributes, null,
                    false);
        } catch (AMException ame) {
            debug.error("AMSDKRepo.setAttributes: Unable to set attributes",
                    ame);
            String ldapError = ame.getLDAPErrorCode();
            String errorMessage = ame.getMessage();
            int errCode = Integer.parseInt(ldapError);
            if (errCode == LDAPException.CONSTRAINT_VIOLATION) {
                Object args[] = 
                    { this.getClass().getName(), ldapError, errorMessage };
                //Throw Fatal exception for errCode 19(eg.,Password too short)
                //as it breaks password policy for password length.
                IdRepoFatalException ide = new IdRepoFatalException(
                   IdRepoBundle.BUNDLE_NAME, "313", args);
                ide.setLDAPErrorCode(ldapError);
                throw ide;
            } else {
                throw IdUtils.convertAMException(ame);
            }
        }

    }

    public void setBinaryAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd) 
        throws IdRepoException, SSOException {
        
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: setBinaryAttributes called" + type + ": "
                    + name + ": " + attributes);
        }
        if (attributes == null || attributes.isEmpty()) {
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "201", null);
        }

        String dn = getDN(type, name);
        int profileType = getProfileType(type);
        try {
            if (adminToken != null) {
                token = adminToken;
            }
            IDirectoryServices dsServices = AMDirectoryAccessFactory
                    .getDirectoryServices();
            dsServices.setAttributes(token, dn, profileType, new AMHashMap(
                    false), attributes, false);
        } catch (AMException ame) {
            debug.error(
                    "AMSDKRepo.setBinaryAttributes: Unable to set attributes",
                    ame);
            throw IdUtils.convertAMException(ame);
        }

    }

    public void changePassword(SSOToken token, IdType type,
        String name, String attrName, String oldPassword, String newPassword)
        throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo.changePassword: name = " + name);
        }

        if (!type.equals(IdType.USER)) {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                "229", args);
        }

        String dn = getDN(type, name);
        int profileType = getProfileType(type);

        try {
            IDirectoryServices dsServices = AMDirectoryAccessFactory
                .getDirectoryServices();
            dsServices.changePassword(token, dn, attrName, oldPassword,
                newPassword);
        } catch (AMException ame) {
            debug.error("AMSDKRepo.changePassword:", ame);
            throw IdUtils.convertAMException(ame);
        }
    }

    private void setMixAttributes(SSOToken token, IdType type, String name,
        Map attrMap, boolean isAdd) throws IdRepoException, SSOException{

        // check for binary attributes.
        HashMap binAttrMap = null;
        HashMap strAttrMap = null;
        boolean foundBin = false;
        Iterator itr = attrMap.keySet().iterator();
        while (itr.hasNext()) {
            String tmpAttrName = (String) itr.next();
            if (attrMap.get(tmpAttrName) instanceof byte[][]) {
                if (!foundBin) {
                     // need to seperate into binary and string
                     // attribute map
                     strAttrMap = new HashMap(attrMap);
                     binAttrMap = new HashMap();
                }
                foundBin = true;
                binAttrMap.put(tmpAttrName, attrMap.get(tmpAttrName));
                strAttrMap.remove(tmpAttrName);
            }
        }
        if (foundBin) {
            setAttributes(token, type, name, strAttrMap, false);
            setBinaryAttributes(token, type, name, binAttrMap, false);
        } else {
            setAttributes(token, type, name, attrMap, false);
        }
    }

    private void setTempMixAttributes(AMTemplate templ, Map attrMap)
        throws IdRepoException, SSOException{

        // check for binary attributes.
        HashMap binAttrMap = null;
        HashMap strAttrMap = null;
        boolean foundBin = false;
        Iterator itr = attrMap.keySet().iterator();
        while (itr.hasNext()) {
            String tmpAttrName = (String) itr.next();
            if (attrMap.get(tmpAttrName) instanceof byte[][]) {
                if (!foundBin) {
                     // need to seperate into binary and string
                     // attribute map
                     strAttrMap = new HashMap(attrMap);
                     binAttrMap = new HashMap();
                }
                foundBin = true;
                binAttrMap.put(tmpAttrName, attrMap.get(tmpAttrName));
                strAttrMap.remove(tmpAttrName);
            } else {
                strAttrMap = new HashMap(attrMap);
                binAttrMap = new HashMap();
            }
        }
        try {
            if (foundBin) {
                templ.setAttributes(strAttrMap);
                templ.setAttributesByteArray(binAttrMap);
            } else {
                templ.setAttributes(strAttrMap);
            }
        } catch (AMException ame) {
            throw IdUtils.convertAMException(ame);
        }
    }

    public void assignService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType sType, Map attrMap)
            throws IdRepoException, SSOException {
        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)) {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
        // Use adminToken if present
        if (adminToken != null) {
            token = adminToken;
        }

        attrMap = new CaseInsensitiveHashMap(attrMap);
        if (type.equals(IdType.USER)) {
            Set OCs = (Set) attrMap.get("objectclass");
            Set attrName = new HashSet(1);
            attrName.add("objectclass");
            Map tmpMap = getAttributes(token, type, name, attrName);
            Set oldOCs = (Set) tmpMap.get("objectclass");
            // Set oldOCs = getAttribute("objectclass");
            OCs = AMCommonUtils.combineOCs(OCs, oldOCs);
            attrMap.put("objectclass", OCs);
            if (sType.equals(SchemaType.USER)) {
                setMixAttributes(token, type, name, attrMap, false);
            } else if (sType.equals(SchemaType.DYNAMIC)) {
                // Map tmpMap = new HashMap();
                // tmpMap.put("objectclass", (Set) attrMap.get("objectclass"));
                setMixAttributes(token, type, name, attrMap, false);
            }
        } else if (type.equals(IdType.ROLE) || type.equals(IdType.FILTEREDROLE)
                || type.equals(IdType.REALM)) {
            IDirectoryServices dsServices = AMDirectoryAccessFactory
                    .getDirectoryServices();
            try {
                AMStoreConnection amsc = (sc == null) ?
                    new AMStoreConnection(token) : sc;
                AMOrganization amOrg = amsc.getOrganization(orgDN);
                // Check if service is already assigned
                Set assndSvcs = amOrg.getRegisteredServiceNames();
                if (!assndSvcs.contains(serviceName)) {
                    amOrg.registerService(serviceName, false, false);
                }
            } catch (AMException ame) {
                if (ame.getErrorCode().equals("464")) {
                    // do nothing. Definition already exists. That's OK.
                } else {
                    throw IdUtils.convertAMException(ame);
                }
            }
            String dn = getDN(type, name);
            try {
                // Remove OCs. Those are needed only when setting service
                // for users, not roles.
                attrMap.remove("objectclass");
                int priority = type.equals(IdType.REALM) ? 3 : 0;
                Set values = (Set)attrMap.remove("cospriority");
                if ((values != null) && (!values.isEmpty())) {
                    try {
                        priority = Integer.parseInt(
                            (String)values.iterator().next());
                    } catch (NumberFormatException ex) {
                        if (debug.warningEnabled()) {
                            debug.warning("AMSDKRepo.assignService:", ex);
                        }
                    }
                }
                dsServices.createAMTemplate(token, dn, getProfileType(type),
                        serviceName, attrMap, priority);
            } catch (AMException ame) {
                debug.error("AMSDKRepo.assignService: Caught AMException", ame);
                throw IdUtils.convertAMException(ame);
            }
        }

    }

    public void unassignService(SSOToken token, IdType type, String name,
            String serviceName, Map attrMap) throws IdRepoException,
            SSOException {
        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)) {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
        // Use adminToken if present
        if (adminToken != null) {
            token = adminToken;
        }
        if (type.equals(IdType.USER)) {

            // Get the object classes that need to be remove from Service Schema
            Set removeOCs = (Set) attrMap.get("objectclass");
            Set attrNameSet = new HashSet();
            attrNameSet.add("objectclass");
            Map objectClassesMap = getAttributes(token, type, name, 
                    attrNameSet);
            Set OCValues = (Set) objectClassesMap.get("objectclass");
            removeOCs = AMCommonUtils.updateAndGetRemovableOCs(OCValues,
                    removeOCs);

            // Get the attributes that need to be removed
            Set removeAttrs = new HashSet();
            Iterator iter1 = removeOCs.iterator();
            while (iter1.hasNext()) {
                String oc = (String) iter1.next();
                IDirectoryServices dsServices = AMDirectoryAccessFactory
                        .getDirectoryServices();
                Set attrs = dsServices.getAttributesForSchema(oc);
                Iterator iter2 = attrs.iterator();
                while (iter2.hasNext()) {
                    String attrName = (String) iter2.next();
                    removeAttrs.add(attrName.toLowerCase());
                }
            }

            // Will be AMHashMap, So the attr names will be in lower case
            Map avPair = getAttributes(token, type, name);
            Iterator itr = avPair.keySet().iterator();

            while (itr.hasNext()) {
                String attrName = (String) itr.next();

                if (removeAttrs.contains(attrName)) {
                    try {
                        // remove attribute one at a time, so if the first
                        // one fails, it will keep continue to remove
                        // other attributes.
                        Map tmpMap = new AMHashMap();
                        tmpMap.put(attrName, Collections.EMPTY_SET);
                        setAttributes(token, type, name, tmpMap, false);
                    } catch (Exception ex) {
                        if (debug.messageEnabled()) {
                            debug.message("AMUserImpl.unassignServices()"
                                    + "Error occured while removing attribute: "
                                    + attrName);
                        }
                    }
                }
            }

            // Now update the object class attribute
            Map tmpMap = new AMHashMap();
            tmpMap.put("objectclass", OCValues);
            setAttributes(token, type, name, tmpMap, false);
        } else if (type.equals(IdType.ROLE)) {
            try {
                AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(
                        token) : sc;
                String roleDN = getDN(type, name);
                AMRole role = amsc.getRole(roleDN);
                AMTemplate templ = role.getTemplate(serviceName,
                        AMTemplate.DYNAMIC_TEMPLATE);
                if (templ != null && templ.isExists()) {
                    templ.delete();
                }
                /*
                 * amdm.unRegisterService(token, orgDN, AMObject.ORGANIZATION,
                 * serviceName, AMTemplate.DYNAMIC_TEMPLATE);
                 */
            } catch (AMException ame) {
                debug.error("AMSDKRepo.unassignService: Caught AMException",
                        ame);
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.FILTEREDROLE)
                || type.equals(IdType.REALM)) {
            try {
                AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(
                        token) : sc;
                String roleDN = getDN(type, name);
                AMFilteredRole role = amsc.getFilteredRole(roleDN);
                AMTemplate templ = role.getTemplate(serviceName,
                        AMTemplate.DYNAMIC_TEMPLATE);
                if (templ != null && templ.isExists()) {
                    templ.delete();
                }
                /*
                 * amdm.unRegisterService(token, orgDN, AMObject.ORGANIZATION,
                 * serviceName, AMTemplate.DYNAMIC_TEMPLATE);
                 */
            } catch (AMException ame) {
                debug.error("AMSDKRepo.unassignService: Caught AMException",
                        ame);
                throw IdUtils.convertAMException(ame);
            }
        } else {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }

    }

    public Set getAssignedServices(SSOToken token, IdType type, String name,
            Map mapOfServiceNamesandOCs) throws IdRepoException, SSOException {
        Set resultsSet = new HashSet();

        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)) {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
        if (mapOfServiceNamesandOCs == null
                || mapOfServiceNamesandOCs.isEmpty()) {
            return resultsSet;
        }
        if (type.equals(IdType.USER)) {
            Set OCs = readObjectClass(token, type, name);
            OCs = convertToLowerCase(OCs);
            Iterator iter = mapOfServiceNamesandOCs.keySet().iterator();
            while (iter.hasNext()) {
                String sname = (String) iter.next();
                Set ocSet = (Set) mapOfServiceNamesandOCs.get(sname);
                ocSet = convertToLowerCase(ocSet);
                if (OCs.containsAll(ocSet)) {
                    resultsSet.add(sname);
                }
            }
        } else if (type.equals(IdType.ROLE)) {
            // Check to see if COS template exists.
            Iterator iter = mapOfServiceNamesandOCs.keySet().iterator();
            while (iter.hasNext()) {
                String serviceName = (String) iter.next();
                try {
                    AMStoreConnection amsc = (sc == null) ? 
                            new AMStoreConnection(token)
                            : sc;
                    String roleDN = getDN(type, name);
                    AMRole role = amsc.getRole(roleDN);
                    AMTemplate templ = role.getTemplate(serviceName,
                            AMTemplate.DYNAMIC_TEMPLATE);
                    if (templ != null && templ.isExists()) {
                        resultsSet.add(serviceName);
                    }
                } catch (AMException ame) {
                    // throw IdUtils.convertAMException(ame);
                    // Ignore this exception..the service might not have
                    // dynamic attributes. Continue iterating.
                }
            }

        } else if (type.equals(IdType.FILTEREDROLE)
                || type.equals(IdType.REALM)) {
            // Check to see if COS template exists.
            Iterator iter = mapOfServiceNamesandOCs.keySet().iterator();
            while (iter.hasNext()) {
                String serviceName = (String) iter.next();
                try {
                    AMStoreConnection amsc = (sc == null) ? 
                            new AMStoreConnection(token)
                            : sc;
                    String roleDN = getDN(type, name);
                    AMFilteredRole role = amsc.getFilteredRole(roleDN);
                    AMTemplate templ = role.getTemplate(serviceName,
                            AMTemplate.DYNAMIC_TEMPLATE);
                    if (templ != null && templ.isExists()) {
                        resultsSet.add(serviceName);
                    }
                } catch (AMException ame) {
                    // throw IdUtils.convertAMException(ame);
                    // ignore this exception
                }
            }

        } else {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
        return resultsSet;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#getServiceAttributes(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Set)
     */
    public Map getServiceAttributes(SSOToken token, IdType type, String name,
        String serviceName, Set attrNames) throws IdRepoException,
        SSOException {
        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)) {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(
                IdRepoBundle.BUNDLE_NAME, "213", args);
        } else  {
            return getServiceAttributes(token, type, name, serviceName,
                attrNames, true);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#getBinaryServiceAttributes(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Set)
     */
    public Map getBinaryServiceAttributes(SSOToken token, IdType type,
        String name, String serviceName,  Set attrNames)
        throws IdRepoException,   SSOException {
        return(getServiceAttributes(token, type, name, serviceName,
            attrNames, false));
    }


    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#getServiceAttributes(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Set)
     */
    private Map getServiceAttributes(SSOToken token, IdType type, String name,
        String serviceName, Set attrNames, boolean isString)
        throws IdRepoException, SSOException {
        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)) {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(
                IdRepoBundle.BUNDLE_NAME, "213", args);
        } else if (type.equals(IdType.USER)) {
            return (isString ?
                getAttributes(token, type, name, attrNames)
                : getBinaryAttributes(token, type, name, attrNames));
        } else if (type.equals(IdType.ROLE)) {
            try {
                AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(
                        token) : sc;
                String roleDN = getDN(type, name);
                AMRole role = amsc.getRole(roleDN);
                AMTemplate templ = role.getTemplate(serviceName,
                        AMTemplate.DYNAMIC_TEMPLATE);
                if (templ != null && templ.isExists()) {
                    return (isString ?
                        templ.getAttributes(attrNames)
                        : templ.getAttributesByteArray(attrNames));
                } else {
                    if (debug.messageEnabled()) {
                        debug.message("AMSDKRepo::getServiceAttributes "
                                + "Service: " + serviceName
                                + " is not assigned to DN: " + roleDN);
                    }
                    return (Collections.EMPTY_MAP);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.FILTEREDROLE)
                || type.equals(IdType.REALM)) {
            try {
                AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(
                        token) : sc;
                String roleDN = getDN(type, name);
                AMFilteredRole role = amsc.getFilteredRole(roleDN);
                AMTemplate templ = role.getTemplate(serviceName,
                        AMTemplate.DYNAMIC_TEMPLATE);
                if (templ != null && templ.isExists()) {
                    return (isString ?
                        templ.getAttributes(attrNames)
                        : templ.getAttributesByteArray(attrNames));
                } else {
                    Object args[] = { serviceName };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "101",
                            args);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        } else {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#modifyService(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Map)
     */
    public void modifyService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType sType, Map attrMap)
            throws IdRepoException, SSOException {

        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)) {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        } else if (type.equals(IdType.USER)) {
            if (sType.equals(SchemaType.DYNAMIC)) {
                Object args[] = { this.getClass().getName(), sType.toString(),
                        type.getName() };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "214", 
                        args);
            } else {
                setMixAttributes(token, type, name, attrMap, false);
            }
        } else if (type.equals(IdType.ROLE)) {
            // Need to modify COS definition and COS template.
            if (sType.equals(SchemaType.USER)) {
                Object args[] = { this.getClass().getName(), sType.toString(),
                        type.getName() };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "214", 
                        args);
            }
            try {
                AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(
                        token) : sc;
                String roleDN = getDN(type, name);
                AMRole role = amsc.getRole(roleDN);
                AMTemplate templ = role.getTemplate(serviceName,
                        AMTemplate.DYNAMIC_TEMPLATE);
                if (templ != null && templ.isExists()) {
                    setTempMixAttributes(templ, attrMap);
                    templ.store();
                } else {
                    Object args[] = { serviceName };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "101",
                            args);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.FILTEREDROLE)
                || type.equals(IdType.REALM)) {
            // Need to modify COS definition and COS template.
            if (sType.equals(SchemaType.USER)) {
                Object args[] = { this.getClass().getName(), sType.toString(),
                        type.getName() };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "214", 
                        args);
            }
            try {
                AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(
                        token) : sc;
                String roleDN = getDN(type, name);
                AMFilteredRole role = amsc.getFilteredRole(roleDN);
                AMTemplate templ = role.getTemplate(serviceName,
                        AMTemplate.DYNAMIC_TEMPLATE);
                if (templ != null && templ.isExists()) {
                    setTempMixAttributes(templ, attrMap);
                    templ.store();
                } else {
                    Object args[] = { serviceName };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "101",
                            args);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        }
    }

    public static void notifyObjectChangedEvent(String normalizedDN,
            int eventType) {
        
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo.notifyObjectChangedEvent - Sending "
                    + "event to listeners.");
        }
        
        if (adminToken == null) {
            adminToken = (SSOToken) AccessController
                .doPrivileged(AdminTokenAction.getInstance());
            try {
                sc = new AMStoreConnection(adminToken);
            } catch (SSOException ssoe) {
                // do nothing ... but log the error
                debug.error("AMSDKRepo:notifyObjectChangedEvent. Failed "
                    + "to initialize AMStoreConnection...", ssoe);
            }
        }
        int type = 0;
        try {
            // If entry has been deleted, its type cannot be obtained
            if (eventType != AMObjectListener.DELETE) {
                type = sc.getAMObjectType(normalizedDN);
            }
        } catch (AMException amse) {
            debug.error("AMSDKRepo:notifyObjectChangedEvent Unable "
                + "to convert name to getAMObjectType.");
        } catch (SSOException amsso) {
            debug.error("AMSDKRepo:notifyObjectChangedEvent Unable "
                + "to detemine permission.");
        }

        IdType idType = null;
        switch (type) {
        case AMObject.GROUP: 
        case AMObject.STATIC_GROUP: 
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
        case AMObject.DYNAMIC_GROUP:
            idType = IdType.GROUP;
            break;
        case AMObject.USER:
            idType = IdType.USER;
            break;
        case AMObject.ORGANIZATION: 
        case AMObject.ORGANIZATIONAL_UNIT:
            idType = IdType.REALM;
            break;
        case AMObject.ROLE: 
        case AMObject.MANAGED_ROLE:
            idType = IdType.ROLE;
            break;
        case AMObject.FILTERED_ROLE:
            idType = IdType.FILTEREDROLE;
            break;                
        default:
            if (debug.messageEnabled()) {
                debug.message("AMSDKRepo:notifyObjectChangedEvent. " +
                    "unknown matching type: type=" + type +
                    " Entity: " + normalizedDN + " Eventtype: " + eventType);
            }
            break;
        }

        synchronized (listeners) {
            Iterator it = listeners.iterator();
            while (it.hasNext()) {
                IdRepoListener l = (IdRepoListener) it.next();
                Map configMap = l.getConfigMap();
                if (idType != null) {
                    l.objectChanged(normalizedDN, idType, eventType, configMap);
                    if (idType == IdType.USER) {
                        // agents were treated as users so we have to
                        // send agent change as well.
                        l.objectChanged(normalizedDN, IdType.AGENT, eventType,
                            configMap);
                    }
                } else {
                    // Unknow idType, send notifications for all types
                    l.objectChanged(normalizedDN, eventType, configMap);
                }
            }
        }
    }

    public static void notifyAllObjectsChangedEvent() {
        
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo.notifyAllObjectsChangedEvent -  Sending "
                    + "event to listeners.");
        }
        
        synchronized (listeners) {
            Iterator it = listeners.iterator();
            while (it.hasNext()) {
               IdRepoListener l = (IdRepoListener) it.next();
                l.allObjectsChanged();
            }
        }
    }

    private void loadSupportedOps() {
        Set opSet = new HashSet();
        opSet.add(IdOperation.CREATE);
        opSet.add(IdOperation.DELETE);
        opSet.add(IdOperation.EDIT);
        opSet.add(IdOperation.READ);
        opSet.add(IdOperation.SERVICE);

        supportedOps.put(IdType.USER, Collections.unmodifiableSet(opSet));
        supportedOps.put(IdType.ROLE, Collections.unmodifiableSet(opSet));
        supportedOps.put(IdType.FILTEREDROLE, Collections
                .unmodifiableSet(opSet));
        Set op2Set = new HashSet(opSet);

        op2Set.remove(IdOperation.SERVICE);
        supportedOps.put(IdType.GROUP, Collections.unmodifiableSet(op2Set));

        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(adminToken,
                IdConstants.REPO_SERVICE, "1.0");
            if (ssm.getRevisionNumber() < 30) {
                supportedOps.put(IdType.AGENT,
                    Collections.unmodifiableSet(op2Set));
            }
        } catch (SMSException smse) {
            if (debug.messageEnabled()) {
                debug.message("AMSDKRepo.loadSupportedOps:", smse);
            }
        } catch (SSOException ssoe) {
            // should not happen
        }

        Set op3Set = new HashSet(opSet);
        op3Set.remove(IdOperation.CREATE);
        op3Set.remove(IdOperation.DELETE);
        op3Set.remove(IdOperation.EDIT);
        supportedOps.put(IdType.REALM, Collections.unmodifiableSet(op3Set));
    }

    private String getDefaultPeopleContainerName() {
        String gcName = "People";
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(ADMIN_SERVICE,
                    adminToken);
            if (ssm != null) {
                ServiceSchema ss = ssm.getGlobalSchema();
                if (ss != null) {
                    Map attrs = ss.getAttributeDefaults();
                    Set vals = (Set) attrs.get(PC_ATTR);
                    if (vals != null && !vals.isEmpty())
                        gcName = (String) vals.iterator().next();
                }

            }
        } catch (SMSException smse) {
            debug.error("AMSDKRepo.getDefaultGC: SMSException: ", smse);
        } catch (SSOException ssoe) {
            debug.error("AMSDKRepo.getDefaultGC: SSOException", ssoe);
        }
        return gcName;
    }

    private String getDefaultGroupContainerName() {
        String gcName = "Groups";
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(ADMIN_SERVICE,
                    adminToken);
            if (ssm != null) {
                ServiceSchema ss = ssm.getGlobalSchema();
                if (ss != null) {
                    Map attrs = ss.getAttributeDefaults();
                    Set vals = (Set) attrs.get(GC_ATTR);
                    if (vals != null && !vals.isEmpty())
                        gcName = (String) vals.iterator().next();
                }

            }
        } catch (SMSException smse) {
            debug.error("AMSDKRepo.getDefaultAC: SMSException: ", smse);
        } catch (SSOException ssoe) {
            debug.error("AMSDKRepo.getDefaultAC: SSOException", ssoe);
        }
        return gcName;
    }

    private String getDefaultAgentContainerName() {
        String gcName = "Agent";
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(ADMIN_SERVICE,
                    adminToken);
            if (ssm != null) {
                ServiceSchema ss = ssm.getGlobalSchema();
                if (ss != null) {
                    Map attrs = ss.getAttributeDefaults();
                    Set vals = (Set) attrs.get(AC_ATTR);
                    if (vals != null && !vals.isEmpty())
                        gcName = (String) vals.iterator().next();
                }

            }
        } catch (SMSException smse) {
            debug.error("AMSDKRepo.getDefaultAC: SMSException: ", smse);
        } catch (SSOException ssoe) {
            debug.error("AMSDKRepo.getDefaultAC: SSOException", ssoe);
        }
        return gcName;
    }

    private AMOrganization checkAndGetOrg(SSOToken token)
            throws IdRepoException, SSOException {
        AMStoreConnection amsc = (sc == null) ? new AMStoreConnection(token)
                : sc;
        try {
            int orgType = amsc.getAMObjectType(orgDN);
            if (orgType != AMObject.ORGANIZATION) {
                debug.error("AMSDKRepo.create(): Incorrectly configured "
                        + " plugin: Org DN is wrong = " + orgDN);
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "303", 
                        null);
            }
        } catch (AMException ame) {
            debug.error("AMSDKRepo.create(): An exception occured while "
                    + " initializing AM SDK ", ame);
            Object[] args = { "com.iplanet.am.sdk.AMSDKRepo",
                    IdOperation.CREATE.getName() };

            IdRepoException ide = new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                    "304", args);
            ide.setLDAPErrorCode(ame.getLDAPErrorCode());
            throw ide;
        }
        return amsc.getOrganization(orgDN);

    }

    private String getDN(IdType type, String name) throws IdRepoException,
            SSOException {
        if (!type.equals(IdType.REALM) && DN.isDN(name)
                && (name.indexOf(",") > -1)) {
            // If should contain at least one comma for it to be a DN
            return name;
        }
        String dn;
        if (sc == null) {
            // initialization error. Throw an exception
            throw new IdRepoException(AMSDKBundle.BUNDLE_NAME, "301", null);
        }
        if (type.equals(IdType.USER)) {
            if (pcDN != null) {
                dn = AMNamingAttrManager.getNamingAttr(AMObject.USER) + "=" +
                    name + "," + pcDN;
            } else {
                dn = AMNamingAttrManager.getNamingAttr(AMObject.USER) + "=" +
                    name + ",ou=" + getDefaultPeopleContainerName() + "," +
                        orgDN;
            }

            try {
                int sdkType = sc.getAMObjectType(dn);
                if (sdkType != AMObject.USER) {
                    Object[] args = { sc.getAMObjectName(sdkType) };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "217",
                            args);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.AGENT)) {
            if (agentDN != null) {
                dn = AMNamingAttrManager.getNamingAttr(100) + "=" + name +
                    "," + agentDN;
            } else {
                dn = AMNamingAttrManager.getNamingAttr(100) + "=" + name +
                    ",ou=" + getDefaultAgentContainerName() + "," + orgDN;
            }
            try {
                int sdkType = sc.getAMObjectType(dn);
                if (sdkType != 100) {
                    Object[] args = { sc.getAMObjectName(sdkType) };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "217",
                            args);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.GROUP)) {
            dn = AMNamingAttrManager.getNamingAttr(AMObject.GROUP) + "=" + name
                    + ",ou=" + getDefaultGroupContainerName() + "," + orgDN;
            try {
                int sdkType = sc.getAMObjectType(dn);
                if (sdkType != AMObject.GROUP
                        && sdkType != AMObject.STATIC_GROUP) {
                    Object[] args = { sc.getAMObjectName(sdkType) };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "217",
                            args);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.ROLE)) {
            dn = AMNamingAttrManager.getNamingAttr(AMObject.ROLE) + "=" + name
                    + "," + orgDN;
            try {
                int sdkType = sc.getAMObjectType(dn);
                if (sdkType != AMObject.ROLE) {
                    Object[] args = { sc.getAMObjectName(sdkType) };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "217",
                            args);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.FILTEREDROLE)) {
            dn = AMNamingAttrManager.getNamingAttr(AMObject.FILTERED_ROLE)
                    + "=" + name + "," + orgDN;
            try {
                int sdkType = sc.getAMObjectType(dn);
                if (sdkType != AMObject.FILTERED_ROLE) {
                    Object[] args = { sc.getAMObjectName(sdkType) };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "217",
                            args);
                }
            } catch (AMException ame) {
                throw IdUtils.convertAMException(ame);
            }
        } else if (type.equals(IdType.REALM)) {
            // Hidden filtered role. No check should be done here
            dn = AMNamingAttrManager.getNamingAttr(AMObject.FILTERED_ROLE)
                    + "=" + AMConstants.CONTAINER_DEFAULT_TEMPLATE_ROLE + ","
                    + orgDN;
        } else {
            Object[] args = { CLASS_NAME, IdOperation.READ.getName(),
                    type.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }
        return dn;
    }

    private int getProfileType(IdType type) throws IdRepoException {
        int profileType;
        if (type.equals(IdType.USER)) {
            profileType = AMObject.USER;
        } else if (type.equals(IdType.AGENT)) {
            profileType = 100;
        } else if (type.equals(IdType.GROUP)) {
            profileType = AMObject.GROUP;
        } else if (type.equals(IdType.ROLE)) {
            profileType = AMObject.ROLE;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            profileType = AMObject.FILTERED_ROLE;
        } else if (type.equals(IdType.REALM) || type.equals(IdType.REALM)) {
            profileType = AMObject.FILTERED_ROLE;
        } else {
            Object[] args = { CLASS_NAME, IdOperation.READ.getName(),
                    type.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }
        return profileType;
    }

    private Set readObjectClass(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        Set attrNameSet = new HashSet();
        attrNameSet.add("objectclass");
        Map objectClassesMap = getAttributes(token, type, name, attrNameSet);
        Set OCValues = (Set) objectClassesMap.get("objectclass");
        return OCValues;
    }

    private Set convertToLowerCase(Set vals) {
        if (vals == null || vals.isEmpty()) {
            return vals;
        } else {
            Set tSet = new HashSet();
            Iterator it = vals.iterator();
            while (it.hasNext()) {
                tSet.add(((String) it.next()).toLowerCase());
            }
            return tSet;
        }
    }

    protected static String constructFilter(int filterModifier, Map avPairs) {
        StringBuffer filterSB = new StringBuffer();
        if (avPairs == null || filterModifier == IdRepo.NO_MOD) {
            return null;
        } else if (filterModifier == IdRepo.OR_MOD) {
            filterSB.append("(|");
        } else if (filterModifier == IdRepo.AND_MOD) {
            filterSB.append("(&");
        }

        Iterator iter = avPairs.keySet().iterator();

        while (iter.hasNext()) {
            String attributeName = (String) (iter.next());
            Iterator iter2 = ((Set) (avPairs.get(attributeName))).iterator();

            while (iter2.hasNext()) {
                String attributeValue = (String) iter2.next();
                filterSB.append("(").append(attributeName).append("=").append(
                        attributeValue).append(")");
            }
        }
        filterSB.append(")");
        return filterSB.toString();
    }

    private ServerInstance getDsSvrCfg(LDAPUser.Type authType)
            throws IdRepoException {
        ServerInstance svrCfg = null;
        try {
            DSConfigMgr dsCfg = DSConfigMgr.getDSConfigMgr();
            svrCfg = dsCfg.getServerInstance(authType);
        } catch (LDAPServiceException ldex) {
            if (debug.messageEnabled()) {
                debug.message("AMSDKRepo: getFullyQualifiedName"
                        + " LDAPServiceException: " + ldex.getMessage());
            }
            Object[] args = { CLASS_NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "219", args);
        }
        return (svrCfg);
    }

    /**
     * Returns the fully qualified name for the identity. It is expected that
     * the fully qualified name would be unique, hence it is recommended to
     * prefix the name with the data store name or protocol. Used by IdRepo
     * framework to check for equality of two identities
     * 
     * @param token
     *            administrator SSOToken that can be used by the datastore to
     *            determine the fully qualified name
     * @param type
     *            type of the identity
     * @param name
     *            name of the identity
     * 
     * @return fully qualified name for the identity within the data store
     */
    public String getFullyQualifiedName(SSOToken token,
        IdType type, String name)
        throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: getFullyQualifiedName." +
                " type=" + type + "; name=" + name);
        }
        // given idtype and name, we will do search to get its FDN.
        if ((name == null) || (name.length() == 0)) {
            Object[] args = { CLASS_NAME, "" };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                "220", args);
        }
        String dn;
        AMStoreConnection amsc = (sc == null) ?
            new AMStoreConnection(token) : sc;
        dn = getDN(type, name);
        boolean exists = amsc.isValidEntry(dn);
        ServerInstance svrCfg = getDsSvrCfg(LDAPUser.Type.AUTH_ADMIN);
        return("amsdk://" + svrCfg.getServerName()  + ":" +
                svrCfg.getPort() + "/"  + dn);
    }


    /**
     * Returns <code>true</code> if the data store supports authentication of
     * identities. Used by IdRepo framework to authenticate identities.
     * 
     * @return <code>true</code> if data store supports authentication of of
     *         identities; else <code>false</code>
     */
    public boolean supportsAuthentication() {
        if (debug.messageEnabled()) {
            debug.message("AMSDKRepo: supportsAuthentication."
                    + " authenticationEnabled=" + true);
        }
        return (true);
    }

    public boolean authenticate(Callback[] credentials) throws 
            IdRepoException, AuthLoginException {
        debug.message("AMSDKRepo: authenticate. ");

        // Obtain user name and password from credentials and authenticate
        String username = null;
        String password = null;
        for (int i = 0; i < credentials.length; i++) {
            if (credentials[i] instanceof NameCallback) {
                username = ((NameCallback) credentials[i]).getName();
                if (debug.messageEnabled()) {
                    debug.message("LDPv3Repo:authenticate username: "
                            + username);
                }
            } else if (credentials[i] instanceof PasswordCallback) {
                char[] passwd = ((PasswordCallback) credentials[i])
                        .getPassword();
                if (passwd != null) {
                    password = new String(passwd);
                    debug.message("AMSDKRepo: authenticate passwd XXX.");
                }
            }
        }
        if (username == null || (username.length() == 0) || password == null) {
            Object args[] = { CLASS_NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "221", args);
        }

        ServerInstance svrCfg = getDsSvrCfg(LDAPUser.Type.AUTH_ADMIN);
        boolean ssl = (svrCfg.getConnectionType() == Server.Type.CONN_SSL);

        LDAPAuthUtils ldapAuthUtil = null;
        try {
            ldapAuthUtil = new LDAPAuthUtils(svrCfg.getServerName(), svrCfg
                    .getPort(), ssl, Locale.getDefaultLocale(), debug);
        } catch (LDAPUtilException ldapUtilEx) {
            if (debug.messageEnabled()) {
                debug.message("AMSDKRepo: authenticate"
                        + " LDAPUtilException: " + ldapUtilEx.getMessage());
            }
            Object[] args = { CLASS_NAME, username };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "211", args);
        }
        ldapAuthUtil.setAuthDN(AdminUtils.getAdminDN());
        ldapAuthUtil.setAuthPassword(new String(AdminUtils.getAdminPassword()));
        ldapAuthUtil.setScope(AMConstants.SCOPE_ONE);
        // TODO?do one then sub?

        if (authenticateIt(ldapAuthUtil, IdType.USER, username, password)) {
            if (debug.messageEnabled()) {
                debug.message("AMSDKRepo: IdType.USER authenticateIt=true");
            }
            return (true);
        }

        if (authenticateIt(ldapAuthUtil, IdType.AGENT, username, password)) {
            if (debug.messageEnabled()) {
                debug.message("AMSDKRepo: IdType.AGENT authenticateIt=true");
            }
            return (true);
        }

        return (false);
    }

    private boolean authenticateIt(LDAPAuthUtils ldapAuthUtil, IdType type,
            String username, String password) 
        throws IdRepoException, AuthLoginException {

        String baseDN = null;
        String namingAttr = null;
        String userid = username;
        try {
            if (type.equals(IdType.USER)) {
                String pcNamingAttr = AMStoreConnection
                        .getNamingAttribute(AMObject.PEOPLE_CONTAINER);
                baseDN = pcNamingAttr + "=" + getDefaultPeopleContainerName()
                        + "," + orgDN;
                namingAttr = AMStoreConnection
                        .getNamingAttribute(AMObject.USER);
            } else if (type.equals(IdType.AGENT)) {
                baseDN = "ou=" + getDefaultAgentContainerName() + "," + orgDN;
                namingAttr = AMStoreConnection.getNamingAttribute(100);
            } else {
                return (false);
            }
        } catch (AMException ame) {
            if (debug.messageEnabled()) {
                debug.message("AMSDKRepo: authenticateIt" + "AMException : "
                        + ame.getMessage());
                debug.message("   type=" + type + "; username=" + username);
            }
            return (false);
        }

        try {
            ldapAuthUtil.setUserNamingAttribute(namingAttr);
            Set userSearchAttr = new HashSet();
            userSearchAttr.add(namingAttr);
            ldapAuthUtil.setUserSearchAttribute(userSearchAttr);
            ldapAuthUtil.setBase(baseDN);
            // need to reset filter otherwise it appends
            // new filter to previous.
            ldapAuthUtil.setFilter("");
            String[] attrs = new String[2];
            attrs[0] = "dn";
            attrs[1] = namingAttr;
            ldapAuthUtil.setUserAttrs(attrs);
            if (DN.isDN(username)) {
                userid = LDAPDN.explodeDN(username, true)[0]; 
            }
            ldapAuthUtil.authenticateUser(userid, password);
        } catch (LDAPUtilException ldapUtilEx) {
            switch (ldapUtilEx.getLDAPResultCode()) {
                case LDAPUtilException.NO_SUCH_OBJECT:
                    if (debug.messageEnabled()) {
                        debug.message("AMSDKRepo:authenticateIt. " +
                            "The specified user does not exist. " +
                            "username=" + username);
                    }
                    throw new AuthLoginException(amAuthLDAP,
                            "NoUser", null);
                case LDAPUtilException.INVALID_CREDENTIALS:
                    if (debug.messageEnabled()) {
                        debug.message("AMSDKRepo:authenticateIt." +
                            " Invalid password. username=" + username);
                    }
                    String failureUserID = ldapAuthUtil.getUserId();
                    throw new InvalidPasswordException(amAuthLDAP,
                        "InvalidUP", null, failureUserID, null);
                case LDAPUtilException.UNWILLING_TO_PERFORM:
                    if (debug.messageEnabled()) {
                        debug.message("AMSDKRepo:authenticateIt. " +
                            "Unwilling to perform. Account inactivated." +
                             " username" + username);
                    }
                    throw new AuthLoginException(amAuthLDAP,
                        "FConnect", null);
                case LDAPUtilException.INAPPROPRIATE_AUTHENTICATION:
                    if (debug.messageEnabled()) {
                        debug.message("AMSDKRepo:authenticateIt. " +
                            "Inappropriate authentication. username="
                            + username);
                    }
                    throw new AuthLoginException(amAuthLDAP, "InappAuth",
                        null);
                case LDAPUtilException.CONSTRAINT_VIOLATION:
                    if (debug.messageEnabled()) {
                        debug.message("AMSDKRepo:authenticateIt. " +
                            "Exceed password retry limit. username"
                            + username);
                    }
                    throw new AuthLoginException(amAuthLDAP,
                            "ExceedRetryLimit", null);
                default:
                    if (debug.messageEnabled()) {
                        debug.message("AMSDKRepo:authenticateIt. " +
                            "default exception. username=" + username);
                    }
                    throw new AuthLoginException(amAuthLDAP, "LDAPex", null);
            }
        }

        return (ldapAuthUtil.getState() == LDAPAuthUtils.SUCCESS);
    }
}
