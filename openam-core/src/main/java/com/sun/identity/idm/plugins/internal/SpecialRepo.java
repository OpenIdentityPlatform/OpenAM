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
 * $Id: SpecialRepo.java,v 1.19 2010/01/06 17:41:00 veiming Exp $
 *
 * Portions Copyrighted 2012-2016 ForgeRock AS.
 */
package com.sun.identity.idm.plugins.internal;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.common.configuration.UnknownPropertyNameException;
import java.io.IOException;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;

import com.iplanet.services.ldap.ServerConfigMgr;
import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.internal.AuthSubject;
import com.sun.identity.authentication.internal.server.SMSAuthModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.common.DNUtils;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdRepoFatalException;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.idm.IdRepoUnsupportedOpException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.RepoSearchResults;
import com.sun.identity.security.AdminPasswordAction;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Hash;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceSchemaManager;

public class SpecialRepo extends IdRepo implements ServiceListener {

    public static final String NAME =
        "com.sun.identity.idm.plugins.internal.SpecialRepo";

    // Status attribute
    private static final String statusAttribute = "inetUserStatus";
    private static final String statusActive = "Active";
    private static final String statusInactive = "Inactive";
    private static final String snAttribute = "sn";
    private static final String cnAttribute = "cn";
    private static final String dnAttribute = "dn";
    private static final String gnAttribute = "givenName";
    private static final String empNumAttribute = "employeeNumber";
    private static final String aliasAttribute = "iplanet-am-user-alias-list";
    private static final String successAttribute = "iplanet-am-user-success-url";
    private static final String failureAttribute = "iplanet-am-user-failure-url";
    private static final String mailAttribute = "mail";
    private static final String addrAttribute = "postalAddress";
    private static final String msisdnAttribute = "sunIdentityMSISDNNumber";
    private static final String phoneAttribute = "telephoneNumber";
    private static final String URL_ACCESS_AGENT = "amService-URLAccessAgent";

    IdRepoListener repoListener;

    Debug debug = Debug.getInstance("amSpecialRepo");

    private Map supportedOps = new HashMap();

    ServiceSchemaManager ssm;

    ServiceConfigManager scm;
    ServiceConfig userConfigCache, roleConfigCache;   
    // Contains the names of the specials users
    Set specialUsers;
    String ssmListenerId, scmListenerId;

    public SpecialRepo() {
        loadSupportedOps();
        if (debug.messageEnabled()) {
            debug.message("SpecialRepo instantiated");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#addListener(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdRepoListener)
     */
    public int addListener(SSOToken token, IdRepoListener listener)
        throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message(": SpecialRepo addListener");
        }
        repoListener = listener;
        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            if (ssm == null) {
                ssm = new ServiceSchemaManager(adminToken,
                    IdConstants.REPO_SERVICE, "1.0");
            }
            if (scm == null) {
                scm = new ServiceConfigManager(adminToken,
                    IdConstants.REPO_SERVICE, "1.0");
            }
            ssmListenerId = ssm.addListener(this);
            scmListenerId = scm.addListener(this);
        } catch (SMSException smse) {
            debug.error("SpecialRepo.addListener: Unable to add listener to" +
                " SM Updates to special users will not reflect", smse);
        }
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#assignService(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      com.sun.identity.sm.SchemaType, java.util.Map)
     */
    public void assignService(SSOToken token, IdType type, String name,
        String serviceName, SchemaType stype, Map attrMap)
        throws IdRepoException, SSOException {
        if (isSpecialUser(type, name)) {
            Object args[] = {NAME, IdOperation.SERVICE.getName(),
                type + " " + name
            };
            throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
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
        Object args[] = {NAME, IdOperation.SERVICE.getName()};
        throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, "305",
            args);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#delete(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public void delete(SSOToken token, IdType type, String name)
        throws IdRepoException, SSOException {
        if (isSpecialUser(type, name)) {
            // Need to support delete for anonymous only
            if (name.equalsIgnoreCase(IdConstants.ANONYMOUS_USER)) {
                try {
                    // Obtain userconfig and delete anonymous user
                    ServiceConfig sc = getUserConfig();
                    sc.removeSubConfig(name);
                } catch (SMSException smse) {
                    debug.error("SpecialRepo: Unable to delete anonymous user ",
                        smse);
                    Object args[] = { NAME };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "200",
                        args);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getAssignedServices(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Map)
     */
    public Set getAssignedServices(SSOToken token, IdType type, String name,
        Map mapOfServicesAndOCs) throws IdRepoException, SSOException {
        if (isSpecialUser(type, name)) {
            Object args[] = {NAME, IdOperation.SERVICE.getName(),
                type + " " + name
            };
            throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
        } else {
            Object args[] = { NAME, IdOperation.SERVICE.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, 
                "305", args);
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
        CaseInsensitiveHashMap allAtt = new CaseInsensitiveHashMap(
            getAttributes(token, type, name));
        Map resultMap = new HashMap();
        Iterator it = attrNames.iterator();
        while (it.hasNext()) {
            String attrName = (String) it.next();
            if (allAtt.containsKey(attrName)) {
                resultMap.put(attrName, allAtt.get(attrName));
            }
        }
        return resultMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public Map getAttributes(SSOToken token, IdType type, String name)
        throws IdRepoException, SSOException {
        if (isSpecialUser(type, name)) {
            try {
                ServiceConfig userConfig = getUserConfig();
                // Get SubConfig of the user
                ServiceConfig usc1 = userConfig.getSubConfig(name);
                if (usc1 != null) {
                    // Return without the userPassword attribute
                    // BugID: 6309830
                    Map answer = usc1.getAttributes();
                    if (name.equalsIgnoreCase(IdConstants.AMADMIN_USER) ||
                        name.equalsIgnoreCase(IdConstants.ANONYMOUS_USER)) {
                        // The passwords for these would
                        // be returned from AMSDK plugin
                        answer.remove("userPassword");
                    }
                    // Add the AMSDK root suffix to the DN attribute
                    replaceDNAttributeIfPresent(answer);
                    return (answer);
                }
                // User not found, thrown exception
                Object args[] = {name};
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "202",
                    args);
            } catch (SMSException smse) {
                debug.error("SpecialRepo: Unable to read user attributes ",
                    smse);
                Object args[] = {NAME};
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "200",
                    args);
            }
        }
        Object args[] = {NAME, IdOperation.READ.getName()};
        throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, "305",
            args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getBinaryAttributes(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Set)
     */
    public Map getBinaryAttributes(SSOToken token, IdType type, String name,
        Set attrNames) throws IdRepoException, SSOException {
        if (isSpecialUser(type, name)) {
            Object args[] = {NAME, IdOperation.SERVICE.getName(),
                type + " " + name
            };
            throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
        } else {
            Object args[] = {NAME, IdOperation.READ.getName()};
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#setBinaryAttributes(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.util.Map, boolean)
     */
    public void setBinaryAttributes(SSOToken token, IdType type, String name,
        Map attributes, boolean isAdd) throws IdRepoException, SSOException {
        if (isSpecialUser(type, name)) {
            Object args[] = {NAME, IdOperation.SERVICE.getName(),
                type + " " + name
            };
            throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
        } else {
            Object args[] = {NAME, IdOperation.EDIT.getName()};
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                "305", args);

        }
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
        Object args[] = {NAME, IdOperation.READ.getName()};
        throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, "305",
            args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getMemberships(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String,
     *      com.sun.identity.idm.IdType)
     */
    public Set getMemberships(SSOToken token, IdType type, String name,
        IdType membershipType) throws IdRepoException, SSOException {
        if (isSpecialUser(type, name)) {
            return (Collections.EMPTY_SET);
        }
        Object args[] = {NAME, IdOperation.READ.getName()};
        throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, "305",
            args);

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
        // Check if the name is present
        if (isSpecialUser(type, name)) {
            return (Collections.EMPTY_MAP);
        }
        // Throw exception otherwise
        Object args[] = {NAME, IdOperation.SERVICE.getName()};
        throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, "305",
            args);
    }

    /* 
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#getBinaryServiceAttributes(
     * com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     * java.lang.String, java.util.Set)
     */
    public Map getBinaryServiceAttributes(SSOToken token, IdType type,
        String name, String serviceName, Set attrNames)
        throws IdRepoException, SSOException {
        if (isSpecialUser(type, name)) {
            Object args[] = {NAME, IdOperation.SERVICE.getName(),
                type + " " + name
            };
            throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
        } else {
            Object args[] = {NAME, IdOperation.READ.getName()};
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#isExists(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public boolean isExists(SSOToken token, IdType type, String name)
        throws IdRepoException, SSOException {
        if (isSpecialUser(type, name)) {
            return true;
        }
        return (false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#modifyMemberShip(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Set,
     *      com.sun.identity.idm.IdType, int)
     */
    public void modifyMemberShip(SSOToken token, IdType type, String name,
        Set members, IdType membersType, int operation)
        throws IdRepoException, SSOException {
        if (isSpecialUser(type, name)) {
            Object args[] = {NAME, IdOperation.SERVICE.getName(),
                type + " " + name
            };
            throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
        } else {
            Object args[] = {NAME, IdOperation.EDIT.getName()};
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#modifyService(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      com.sun.identity.sm.SchemaType, java.util.Map)
     */
    public void modifyService(SSOToken token, IdType type, String name,
        String serviceName, SchemaType sType, Map attrMap)
        throws IdRepoException, SSOException {
        if (isSpecialUser(type, name)) {
            Object args[] = {NAME, IdOperation.SERVICE.getName(),
                type + " " + name
            };
            throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
        } else {
            Object args[] = {NAME, IdOperation.SERVICE.getName()};
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
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
        if (isSpecialUser(type, name)) {
            Object args[] = {NAME, IdOperation.SERVICE.getName(),
                type + " " + name
            };
            throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
        } else {
            Object args[] = {NAME, IdOperation.SERVICE.getName()};
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#removeListener()
     */
    public void removeListener() {
        if (scm != null) {
            scm.removeListener(scmListenerId);
            scm = null;
        }
        if (ssm != null) {
            ssm.removeListener(ssmListenerId);
            ssm = null;   //make sure old reference get GCed asap
            
            //unfortunately, because reposervice is special this is required to 
            //make sure any old lingering object would be cleaned.
            try {
                SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
                ssm = new ServiceSchemaManager(adminToken,
                      IdConstants.REPO_SERVICE, "1.0");
                ssm.removeListener(ssmListenerId);
            } catch (SSOException ssoe) {
                // listener should be removed in first try. ignoring any error
            } catch (SMSException smse) {
            	// listener should be removed in first try. ignoring any error
            }
        }
        repoListener = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#search(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, int, int,
     *      java.util.Set, boolean, int, java.util.Map, boolean)
     */
    public RepoSearchResults search(SSOToken token, IdType type,
        String pattern, int maxTime, int maxResults, Set returnAttrs,
        boolean returnAllAttrs, int filterOp, Map avPairs,
        boolean recursive) throws IdRepoException, SSOException {
        Set userRes = new HashSet();
        Map userAttrs = new HashMap();
        int errorCode = RepoSearchResults.SUCCESS;
        try {
            if (type.equals(IdType.USER)) {
                ServiceConfig userConfig = getUserConfig();
                // Support aliasing for "uid" at least..
                if (pattern.equals("*") && avPairs != null && !avPairs.isEmpty()) {
                    Set uidVals = (Set) avPairs.get("uid");
                    if (uidVals != null && !uidVals.isEmpty()) {
                        pattern = (String) uidVals.iterator().next();
                    } else {
                        // pattern is "*" and avPairs is not empty, so return
                        // empty results
                        return new RepoSearchResults(Collections.EMPTY_SET,
                            RepoSearchResults.SUCCESS,
                            Collections.EMPTY_MAP, type);
                    }
                }

                // If wild card is used for pattern, do a search else a lookup
                if (pattern.indexOf('*') != -1) {
                    userRes = userConfig.getSubConfigNames(pattern);
                } else {
                    for (Iterator items = userConfig.getSubConfigNames()
                        .iterator(); items.hasNext();) {
                        String name = (String) items.next();
                        if (name.equalsIgnoreCase(pattern)) {
                            userRes.add(pattern);
                            break;
                        }
                    }
                }

                if (userRes != null) {
                    Iterator it = userRes.iterator();
                    while (it.hasNext()) {
                        String u = (String) it.next();
                        ServiceConfig thisUser = userConfig.getSubConfig(u);
                        Map attrs = thisUser.getAttributes();
                        // Return without the userPassword attribute
                        // BugID: 6309830
                        if (u.equalsIgnoreCase(IdConstants.AMADMIN_USER) ||
                            u.equalsIgnoreCase(IdConstants.ANONYMOUS_USER)) {
                            // The passwords for these would
                            // be returned from LDAP
                            attrs.remove("userPassword");
                        }
                        // Add the AMSDK root suffix to the DN attribute
                        replaceDNAttributeIfPresent(attrs);
                        userAttrs.put(u, attrs);
                    }
                }
                return new RepoSearchResults(userRes, errorCode, userAttrs,
                    type);
            } else {
                return new RepoSearchResults(Collections.EMPTY_SET,
                    RepoSearchResults.SUCCESS, Collections.EMPTY_MAP, type);
            }
        } catch (SMSException smse) {
            debug.error("SpecialRepo.search: Unable to retrieve entries: ",
                smse);
            Object args[] = {NAME};
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "219", args);
        }
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
        return (search(token, type, pattern, maxTime, maxResults, returnAttrs,
            (returnAttrs == null), OR_MOD, avPairs, recursive));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#setAttributes(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Map,
     *      boolean)
     */
    public void setAttributes(SSOToken token, IdType type, String name,
        Map attributes, boolean isAdd) throws IdRepoException, SSOException {
        if (isSpecialUser(type, name)) {
            boolean isUrlAccessAgent = isUrlAccessAgent(type, name);
            String urlAccessAgentCryptPwd = null;
            if (!isAmAdminUser(token)) {
                Object args[] = { name };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "231",
                    args);
            }

            try {
                ServiceConfig userConfig = getUserConfig();
                // For performance reason check if the user entry
                // is present before getting the subConfig
                CaseInsensitiveHashSet userSet = new CaseInsensitiveHashSet();
                userSet.addAll(userConfig.getSubConfigNames());
                if (userSet.contains(name)) {
                    ServiceConfig usc1 = userConfig.getSubConfig(name);
                    Map attrs = usc1.getAttributes();
                    // can only set "userpassword" and "inetUserStatus"
                    String newPassword = null;
                    Set vals = (Set) attributes.get("userPassword");
                    if ((vals != null) || (vals = (Set) attributes.get(
                        "userpassword")) != null) {
                        Set hashedVals = new HashSet();
                        Iterator it = vals.iterator();
                        while (it.hasNext()) {
                            String val = (String) it.next();
                            hashedVals.add(Hash.hash(val));
                            newPassword = val;

                            // if user is URL Access Agent,
                            // urlAccessAgentCryptPwd will be set; otherwise
                            // urlAccessAgentCryptPwd will be null.
                            if (isUrlAccessAgent) {
                                urlAccessAgentCryptPwd = Crypt.encode(val);
                            }
                        }
                        attrs.put("userPassword", hashedVals);
                    }
                    if ((vals = (Set) attributes.get(statusAttribute))
                        != null || (vals = (Set) attributes.get(
                        statusAttribute)) != null) {
                        attrs.put(statusAttribute, vals);
                    }
                    if ((vals = (Set) attributes.get(cnAttribute))
                        != null || (vals = (Set) attributes.get(
                        cnAttribute)) != null) {
                        attrs.put(cnAttribute, vals);
                    }
                    if ((vals = (Set) attributes.get(snAttribute))
                        != null || (vals = (Set) attributes.get(
                        snAttribute)) != null) {
                        attrs.put(snAttribute, vals);
                    }
                    if ((vals = (Set) attributes.get(gnAttribute))
                        != null || (vals = (Set) attributes.get(
                        gnAttribute)) != null) {
                        attrs.put(gnAttribute, vals);
                    }
                    if ((vals = (Set) attributes.get(empNumAttribute))
                        != null || (vals = (Set) attributes.get(
                        empNumAttribute)) != null) {
                        attrs.put(empNumAttribute, vals);
                    }
                    if ((vals = (Set) attributes.get(aliasAttribute))
                        != null || (vals = (Set) attributes.get(
                        aliasAttribute)) != null) {
                        attrs.put(aliasAttribute, vals);
                    }
                    if ((vals = (Set) attributes.get(successAttribute))
                        != null || (vals = (Set) attributes.get(
                        successAttribute)) != null) {
                        attrs.put(successAttribute, vals);
                    }
                    if ((vals = (Set) attributes.get(failureAttribute))
                        != null || (vals = (Set) attributes.get(
                        failureAttribute)) != null) {
                        attrs.put(failureAttribute, vals);
                    }
                    if ((vals = (Set) attributes.get(mailAttribute))
                        != null || (vals = (Set) attributes.get(
                        mailAttribute)) != null) {
                        attrs.put(mailAttribute, vals);
                    }
                    if ((vals = (Set) attributes.get(addrAttribute))
                        != null || (vals = (Set) attributes.get(
                        addrAttribute)) != null) {
                        attrs.put(addrAttribute, vals);
                    }
                    if ((vals = (Set) attributes.get(msisdnAttribute))
                        != null || (vals = (Set) attributes.get(
                        msisdnAttribute)) != null) {
                        attrs.put(msisdnAttribute, vals);
                    }
                    if ((vals = (Set) attributes.get(phoneAttribute))
                        != null || (vals = (Set) attributes.get(
                        phoneAttribute)) != null) {
                        attrs.put(phoneAttribute, vals);
                    }
                    usc1.setAttributes(attrs);
                    // If password is changed for dsameuser, need to
                    // update serverconfig.xml and directory
                    if (name.equalsIgnoreCase("dsameuser")) {
                        String op = (String) AccessController
                                .doPrivileged(new AdminPasswordAction());
                        try {
                            ServerConfigMgr sscm = new ServerConfigMgr();
                            sscm.setAdminUserPassword(op, newPassword);
                            sscm.save();
                        } catch (Exception e) {
                            debug.error("SpecialRepo: error in "
                                    + "changing password", e);
                        }
                    }
                    updateServiceConfiguration(urlAccessAgentCryptPwd);
                } else {
                    Object args[] = { name };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "202",
                        args);
                }
            } catch (SMSException smse) {
                debug.error("SpecialRepo: Unable to set user attributes ",
                    smse);
                Object args[] = { NAME, type.getName(), name };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "212",
                    args);
            }
        } else {
            Object args[] = {NAME, IdOperation.EDIT.getName()};
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
        }
    }

    private void updateServiceConfiguration(String urlAccessAgentCryptPwd)
        throws IdRepoException, SSOException {
        if (urlAccessAgentCryptPwd != null) {
            Map<String, Set<String>> map = new HashMap<String, Set<String>>();
            Set<String> set = new HashSet<String>();
            set.add(urlAccessAgentCryptPwd);
            map.put(Constants.AM_SERVICES_SECRET, set);
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            String instance = SystemProperties.getServerInstanceName();

            try {
                ServerConfiguration.setServerInstance(adminToken, instance,
                    map);
            } catch (SMSException e) {
                debug.error("SpecialRepo.updateServiceConfiguration", e);
                Object args[] = {NAME, IdOperation.EDIT.getName()};
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                    "232", args);
            } catch (IOException e) {
                debug.error("SpecialRepo.updateServiceConfiguration", e);
                Object args[] = {NAME, IdOperation.EDIT.getName()};
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                    "232", args);
            } catch (ConfigurationException e) {
                debug.error("SpecialRepo.updateServiceConfiguration", e);
                Object args[] = {NAME, IdOperation.EDIT.getName()};
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                    "232", args);
            } catch (UnknownPropertyNameException e) {
                // never happen
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#unassignService(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Map)
     */
    public void unassignService(SSOToken token, IdType type, String name,
        String serviceName, Map attrMap) throws IdRepoException,
        SSOException {
        if (isSpecialUser(type, name)) {
            Object args[] = {NAME, IdOperation.SERVICE.getName(),
                type + " " + name
            };
            throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
        } else {
            Object args[] = {
                "com.sun.identity.idm.plugins.specialusers.SpecialRepo",
                IdOperation.SERVICE.getName()
            };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getSupportedOperations(
     *      com.sun.identity.idm.IdType)
     */
    @Override
    public Set getSupportedOperations(IdType type) {
        return (Set) supportedOps.get(type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getSupportedTypes()
     */
    @Override
    public Set getSupportedTypes() {
        return supportedOps.keySet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#initialize(java.util.Map)
     */
    @Override
    public void initialize(Map configParams) throws IdRepoException {
        super.initialize(configParams);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#isActive(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    @Override
    public boolean isActive(SSOToken token, IdType type, String name)
        throws IdRepoException, SSOException {
        Map attributes = getAttributes(token, type, name);
        if (attributes == null) {
            Object[] args = {NAME, name};
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "202", args);
        }
        Set activeVals = (Set) attributes.get(statusAttribute);
        if (activeVals == null || activeVals.isEmpty()) {
            return true;
        } else {
            Iterator it = activeVals.iterator();
            String active = (String) it.next();
            return (active.equalsIgnoreCase(statusActive) ? true : false);
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
        Map attrs = new HashMap();
        Set vals = new HashSet();
        if (active) {
            vals.add(statusActive);
        } else {
            vals.add(statusInactive);
        }
        attrs.put(statusAttribute, vals);
        setAttributes(token, type, name, attrs, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#shutdown()
     */
    @Override
    public void shutdown() {
        if (scm != null) {
            scm.removeListener(scmListenerId);
        }
        if (ssm != null) {
            ssm.removeListener(ssmListenerId);
        }
    }

    private boolean isUrlAccessAgent(IdType type, String name) {
        return (type.equals(IdType.USER)) && name.equalsIgnoreCase(
            URL_ACCESS_AGENT);
    }

    private boolean isAmAdminUser(SSOToken token) throws SSOException {
        String adminUserDN = DNUtils.normalizeDN(SystemProperties.get(Constants.AUTHENTICATION_SUPER_USER));
        try {
            AMIdentity adminIdentity = new AMIdentity(token, adminUserDN, IdType.USER, "/", null);
            AMIdentity userIdentity = new AMIdentity(token);
            if (adminIdentity.equals(userIdentity)) {
                return true;
            }
        } catch (Exception ex) {
            debug.warning("Unable to create user identity object", ex);
        }

        return false;
    }

    private boolean isSpecialUser(IdType type, String name)
        throws SSOException {
        boolean isSpecUser = false;
        if (type.equals(IdType.USER)) {
            if ((specialUsers == null) || specialUsers.isEmpty()) {
                try {
                    ServiceConfig userConfig = getUserConfig();
                    Set userSet = new CaseInsensitiveHashSet();
                    userSet.addAll(userConfig.getSubConfigNames());
                    specialUsers = userSet;
                } catch (SMSException smse) {
                    isSpecUser = false;
                }
            }
            if ((specialUsers != null) && specialUsers.contains(name)) {
                isSpecUser = true;
            }
        }
        return isSpecUser;
    }

    private void loadSupportedOps() {
        Set opSet = new HashSet();
        opSet.add(IdOperation.EDIT);
        opSet.add(IdOperation.DELETE);
        opSet.add(IdOperation.READ);
        opSet.add(IdOperation.SERVICE);
        supportedOps.put(IdType.USER, Collections.unmodifiableSet(opSet));
        supportedOps.put(IdType.AGENT, Collections.unmodifiableSet(opSet));
        if (debug.messageEnabled()) {
            debug.message("SpecialRepo: loadSupportedOps called " +
                "supportedOps Map = " + supportedOps);
        }
    }

    private void replaceDNAttributeIfPresent(Map attributes) {
        // Check revision number to determine if root suffix needs
        // to be added
        if (ssm == null) {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            try {
                ssm = new ServiceSchemaManager(adminToken,
                    IdConstants.REPO_SERVICE, "1.0");
            } catch (SMSException smse) {
                debug.error("SpecialRepo.replaceDN: Unable to get Schema" +
                    "to determine revision number", smse);
                return;
            } catch (SSOException ssoe) {
                // should not happen
                return;
            }
        }
        if (ssm.getRevisionNumber() >= 30) {
            Set values;
            if ((attributes != null) && ((values =
                (Set) attributes.get(dnAttribute)) != null)) {
                for (Iterator items = values.iterator(); items.hasNext();) {
                    String name = items.next().toString();
                    // In the case of upgrade the DN will have the suffix
                    // Hence check if it ends with SMS root suffix
                    if (name.toLowerCase().endsWith(
                        SMSEntry.getRootSuffix().toLowerCase())) {
                        // Replace only if the they are different
                        if (!SMSEntry.getRootSuffix().equals(
                            SMSEntry.getAMSdkBaseDN())) {
                            name = name.substring(0, name.length() -
                                SMSEntry.getRootSuffix().length());
                            name = name + SMSEntry.getAMSdkBaseDN();
                        }
                    } else {
                        name = name + SMSEntry.getAMSdkBaseDN();
                    }
                    Set newValues = new HashSet();
                    newValues.add(name);
                    attributes.put(dnAttribute, newValues);
                    break;
                }
            }
        }
    }

    private ServiceConfig getUserConfig()
        throws SMSException, SSOException {
        if ((userConfigCache == null) || !userConfigCache.isValid()) {
            if (scm == null) {
                SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
                scm = new ServiceConfigManager(adminToken,
                    IdConstants.REPO_SERVICE, "1.0");
            }
            ServiceConfig globalConfig = scm.getGlobalConfig(null);
            userConfigCache = globalConfig.getSubConfig("users");
        }
        return (userConfigCache);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.sm.ServiceListener#globalConfigChanged(
     *      java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String, int)
     */
    public void globalConfigChanged(String serviceName, String version,
        String groupName, String serviceComponent, int type) {
        // Send notifcations for users in special users
        if ((specialUsers != null) && !specialUsers.isEmpty() &&
            (repoListener != null)) {
            for (Iterator items = specialUsers.iterator(); items.hasNext();) {
                String specialUser = (String) items.next();
                repoListener.objectChanged(specialUser, IdType.USER, type,
                    configMap);
            }
        }

        // Reset special users
        specialUsers = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.sm.ServiceListener#organizationConfigChanged(
     *      java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String, int)
     */
    public void organizationConfigChanged(String serviceName, String version,
        String orgName, String groupName, String serviceComponent, int type) {
        // Since special users are in global configuration
        // Notifications need not be sent
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.sm.ServiceListener#schemaChanged(java.lang.String,
     *      java.lang.String)
     */
    public void schemaChanged(String serviceName, String version) {
        // Since special users are in global configuration, not schema
        // Notifications need not be sent
    }

    @Override
    public String getFullyQualifiedName(SSOToken token, IdType type,
        String name) throws IdRepoException, SSOException {
        if (isSpecialUser(type, name)) {
            return ("sms://specialRepo/" + type.toString() + "/" + name);
        }
        return (null);
    }

    @Override
    public boolean supportsAuthentication() {
        return (true);
    }

    public boolean authenticate(Callback[] credentials) throws IdRepoException,
        AuthLoginException {
        debug.message("SpecialRepo:authenticate called");

        // Obtain user name and password from credentials and authenticate
        String username = null;
        String password = null;
        for (int i = 0; i < credentials.length; i++) {
            if (credentials[i] instanceof NameCallback) {
                username = ((NameCallback) credentials[i]).getName();
                if (debug.messageEnabled()) {
                    debug.message("SpecialRepo:authenticate username: " + username);
                }
            } else if (credentials[i] instanceof PasswordCallback) {
                char[] passwd = ((PasswordCallback) credentials[i]).getPassword();
                if (passwd != null) {
                    password = new String(passwd);
                    debug.message("SpecialRepo:authN passwd present");
                }
            }
        }
        if (username == null || password == null) {
            return (false);
        }
        Map sharedState = new HashMap();
        sharedState.put(ISAuthConstants.SHARED_STATE_USERNAME, username);
        sharedState.put(ISAuthConstants.SHARED_STATE_PASSWORD, password);
        debug.message("SpecialRepo:authenticate inst. SMSAuthModule");

        SMSAuthModule module = new SMSAuthModule();
        debug.message("SpecialRepo:authenticate SMSAuthModule:init");
        module.initialize(new AuthSubject(), null, sharedState,
            Collections.EMPTY_MAP);
        boolean answer = false;
        try {
            answer = module.login();
            if (debug.messageEnabled()) {
                debug.message("SpecialRepo:authenticate login: " + answer);
            }
        } catch (LoginException le) {
            if (debug.warningEnabled()) {
                debug.warning("authentication: login exception", le);
            }
            if (le instanceof AuthLoginException) {
                throw ((AuthLoginException) le);
            }
        }
        return (answer);
    }
}
