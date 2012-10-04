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
 * $Id: DelegationPolicyImpl.java,v 1.12 2010/01/16 06:35:25 dillidorai Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2012 ForgeRock Inc
 */
package com.sun.identity.delegation.plugins;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;
import java.security.AccessController;
import com.sun.identity.shared.ldap.util.DN;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOException;

import com.iplanet.am.util.Cache;
import com.iplanet.am.util.SystemProperties;

import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.security.AdminTokenAction;

import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdEventListener;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.OrganizationConfigManager;

import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyConfig;
import com.sun.identity.policy.PolicyEvent;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.policy.ActionDecision;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.Rule;
import com.sun.identity.policy.SubjectEvaluationCache;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.policy.interfaces.PolicyListener;

import com.sun.identity.delegation.interfaces.DelegationInterface;
import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.delegation.ResBundleUtils;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.delegation.DelegationPrivilege;

/**
 * The class <code>DelegationPolicyImpl</code> implements the interface
 * <code>DelegationInterface</code> using OpenSSO Policy
 * Management and Evaluation APIs. It provides access control for access 
 * manager using the OpenSSO's internal policy framework.
 */

public class DelegationPolicyImpl implements DelegationInterface, 
    ServiceListener, IdEventListener, PolicyListener 
{

    private static final String POLICY_REPOSITORY_REALM = 
                                       PolicyManager.DELEGATION_REALM;
    private static final String NAME_DELIMITER = "^^";
    private static final char REPLACEMENT_FOR_COMMA = '^';
    private static final String PREFIX = "sms://";
    private static final String DELIMITER = "/";
    private static final String ACTION_ALLOW = "allow";
    private static final String ACTION_DENY = "deny";
    private static final String DELEGATION_RULE = "delegation-rule";
    private static final String DELEGATION_SUBJECT = "delegation-subject";
    private static final String POLICY_SUBJECT = "AMIdentitySubject";
    private static final String AUTHN_USERS_ID = 
        "id=All Authenticated Users,ou=role," +
        com.sun.identity.sm.ServiceManager.getBaseDN();
    private static final String DELEGATION_AUTHN_USERS = "AuthenticatedUsers";
    private static final String AUTHENTICATED_USERS_SUBJECT = 
                                                  "AuthenticatedUsers";

    /**
     *  To configure the delegation cache size, specify the attribute
     * "com.sun.identity.delegation.cache.size" in AMConfig.properties.
     */
    private static final String CONFIGURED_CACHE_SIZE = 
                           "com.sun.identity.delegation.cache.size";
    private static final int DEFAULT_CACHE_SIZE = 20000;

    /** delegation cache structure:
     *  usertokenidstr (key) ---> resource names (value)
     *  resource name (key) ---> arraylist of two elements (value)
     *  arraylist(0) contains a <code>Map</code> object of env parameters
     *  arraylist(1) contains a <code>PolicyDecision</code> regarding the 
     *  resource.
     *  The cache is a LRU one and is updated based on subject change 
     *  notification and policy change notification.
     */
    private static Cache delegationCache;
    private static int maxCacheSize = DEFAULT_CACHE_SIZE;
    private static Map idRepoListeners = new HashMap();
    private static ServiceConfigManager scm;

    private SSOToken appToken;
    private PolicyEvaluator pe;

   /**
    * Initialize (or configure) the <code>DelegationInterface</code>
    * object. Usually it will be initialized with the environmrnt
    * parameters set by the system administrator via Service management service.
    *
    * @param token <code>SSOToken</code> of an administrator
    * @param configParams configuration parameters as a <code>Map</code>.
    * The values in the <code>Map</code> is <code>java.util.Set</code>,
    * which contains one or more configuration parameters.
    *
    * @throws DelegationException if an error occurred during
    * initialization of <code>DelegationInterface</code> instance
    */

    public void initialize(SSOToken token, Map configParams)
        throws DelegationException {
        this.appToken = token;
        try {
            String cacheSize = SystemProperties.get(CONFIGURED_CACHE_SIZE);
            if (cacheSize != null) {
                 try {
                     maxCacheSize = Integer.parseInt(cacheSize); 
                     // specifying cache size as 0 would virtually 
                     // disable the delegation cache.
                     if (maxCacheSize < 0) {
                         maxCacheSize = DEFAULT_CACHE_SIZE;
                     }
                 } catch (NumberFormatException nfe) {
                     DelegationManager.debug.error(
                     "DelegationPolicyImpl.initialize(): " +
                     "invalid cache size specified in AMConfig.properties."
                     + " Use default cache size " + DEFAULT_CACHE_SIZE);
                     maxCacheSize = DEFAULT_CACHE_SIZE;
                 }
            }
            delegationCache = new Cache(maxCacheSize);
            if (DelegationManager.debug.messageEnabled()) {
                DelegationManager.debug.message(
                "DelegationPolicyImpl.initialize(): cache size="
                 + maxCacheSize);
            }

            pe = new PolicyEvaluator(POLICY_REPOSITORY_REALM,
                       DelegationManager.DELEGATION_SERVICE);
            
            // listen on delegation policy changes. once there is 
            // delegation policy change, we need to update the cache.
            pe.addPolicyListener(this);

            // listen on root realm subject changes.
            AMIdentityRepository idRepo = 
                            new AMIdentityRepository(appToken, "/");
            idRepo.addEventListener(this);
            if (DelegationManager.debug.messageEnabled()) {
                DelegationManager.debug.message(
                 "DelegationPolicyImpl: IdRepo event listener added "
                 + "for root realm.");
            }

            // listen on sub realm subject changes.     
            OrganizationConfigManager ocm = 
                         new OrganizationConfigManager(appToken, "/"); 
            Set orgNames = ocm.getSubOrganizationNames("*", true);
            if ((orgNames != null) && (!orgNames.isEmpty())) {
                Iterator it = orgNames.iterator();
                while (it.hasNext()) {
                    String org = (String)it.next();
                    AMIdentityRepository idr = 
                            new AMIdentityRepository(appToken, org);
                    idr.addEventListener(this);
                    idRepoListeners.put(org, idRepo);
                    if (DelegationManager.debug.messageEnabled()) {
                        DelegationManager.debug.message(
                         "DelegationPolicyImpl: IdRepo event listener "
                         + "added for realm (" + org + ").");
                    }
                }
            }

            scm = new ServiceConfigManager(
                        PolicyConfig.POLICY_CONFIG_SERVICE, token);
                        //DelegationManager.DELEGATION_SERVICE, token);

            /**
             *  listen on org config changes. once there is realm added,
             * or removed, we need to add or remove listeners on the
             * affected realm accordingly.
             */
            scm.addListener(this);

        } catch (Exception e) {
            DelegationManager.debug.error(
                    "DelegationPolicyImpl: initialize() failed");
            throw new DelegationException(e);
        }
    }

    /**
     * Returns all the delegation privileges associated with a realm.
     * 
     * @param  token  The <code>SSOToken</code> of the requesting user
     * @param  orgName The name of the realm from which the 
     *         delegation privileges are fetched.
     * 
     * @return <code>Set</code> of <code>DelegationPrivilege</code> objects 
     *         associated with the realm.
     * 
     * @throws SSOException  invalid or expired single-sign-on token
     * @throws DelegationException  for any abnormal condition
     */
    public Set getPrivileges(SSOToken token, String orgName) 
        throws SSOException, DelegationException {
        try {
            Set privileges = new HashSet();
            // Need to check if user has "delegate" permissions for org
            if (hasDelegationPermissionsForRealm(token, orgName)) {
                // Replace token with AdminToken
                token = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
            }
            PolicyManager pm = new PolicyManager(token,
                POLICY_REPOSITORY_REALM);
            Set pnames = pm.getPolicyNames();
            if (pnames != null) {
                /* the name of the policy is in the form of 
                 * orgName^^privilegeName, the privilegeName is the
                 * name of the delegation privilege that the policy 
                 * is corresponding to. In case the orgName is in a 
                 * DN format, the special char ',' is replaced to avoid
                 * saving problem.
                 */
                String prefix = null;
                if (orgName != null) {
                    prefix = orgName.toLowerCase() + NAME_DELIMITER;
                    prefix = prefix.replace(',', REPLACEMENT_FOR_COMMA);
                } else {
                    prefix = NAME_DELIMITER;
                }  
                int prefixLength = prefix.length();
                Iterator it = pnames.iterator();
                while (it.hasNext()) {
                    String pname = (String)it.next();
                    if (pname.toLowerCase().startsWith(prefix)) {
                        Policy p = pm.getPolicy(pname);
                        // converts the policy to its corresponding 
                        // delegation privilege
                        DelegationPrivilege dp = policyToPrivilege(p);
                        if (dp != null) {
                            dp.setName(pname.substring(prefixLength)); 
                            privileges.add(dp);
                        }
                    }
                } 
            }
            return (privileges);
        } catch (Exception e) {
            DelegationManager.debug.error(
                "unable to get privileges from realm " + orgName);
            throw new DelegationException(e);
        }
    }

    /**
     * Adds a delegation privilege to a specific realm. The permission will be
     * added to the existing privilege in the event that this method is trying
     * to add to an existing privilege.
     *
     * @param token  The <code>SSOToken</code> of the requesting user
     * @param orgName The name of the realm to which the delegation privilege 
     *        is to be added.
     * @param privilege  The delegation privilege to be added.
     * 
     * @throws SSOException invalid or expired single-sign-on token
     * @throws DelegationException if any abnormal condition occurred.
     */
    public void addPrivilege(SSOToken token, String orgName, 
      DelegationPrivilege privilege) throws SSOException, DelegationException {
        if (privilege != null) {
            try {
                // Need to check if user has "delegate" permissions for org
                if (hasDelegationPermissionsForRealm(token, orgName)) {
                    // Replace token with AdminToken
                    token = (SSOToken) AccessController.doPrivileged(
                        AdminTokenAction.getInstance());
                }
                PolicyManager pm = new PolicyManager(token,
                    POLICY_REPOSITORY_REALM);
                Policy p = privilegeToPolicy(pm, privilege, orgName);
                if (p != null) {
                    Set existingPolicies = pm.getPolicyNames();
                    if (existingPolicies.contains(p.getName())) {
                        Set<String> subjectNames = p.getSubjectNames();

                        if ((subjectNames == null) || subjectNames.isEmpty()) {
                            pm.removePolicy(p.getName());
                        } else {
                            pm.replacePolicy(p);
                        }
                    } else {
                        Set<String> subjectNames = p.getSubjectNames();

                        if ((subjectNames != null) && !subjectNames.isEmpty()){
                            pm.addPolicy(p);
                        }
                    }
                } else {
                    throw new DelegationException(ResBundleUtils.rbName,
                        "invalid_delegation_privilege", null, null);
                }
            } catch (Exception e) {
                throw new DelegationException(e);
            }
        } 
    }


    /**
     * Removes a delegation privilege from a specific realm.
     * 
     * @param token The <code>SSOToken</code> of the requesting user
     * @param orgName The name of the realm from which the delegation 
     *         privilege is to be removed.
     * @param privilegeName The name of the delegation privilege to be removed.
     * 
     * @throws SSOException  invalid or expired single-sign-on token
     * @throws DelegationException for any abnormal condition
     */

    public void removePrivilege(SSOToken token, String orgName, 
        String privilegeName) throws SSOException, DelegationException {
        try {
            // Need to check if user has "delegate" permissions for org
            if (hasDelegationPermissionsForRealm(token, orgName)) {
                // Replace token with AdminToken
                token = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
            }            
            PolicyManager pm = new PolicyManager(token,
                                    POLICY_REPOSITORY_REALM);
            String prefix = null;
            if (orgName != null) {
                /* the name of the policy is in the form of 
                 * orgName^^privilegeName, the privilegeName is the
                 * name of the delegation privilege that the policy 
                 * is corresponding to. In case the orgName is in a 
                 * DN format, the special char ',' is replaced to 
                 * avoid saving problem.
                 */
                 prefix = orgName.toLowerCase() + NAME_DELIMITER;
                 prefix = prefix.replace(',', REPLACEMENT_FOR_COMMA);
            } else {
                prefix = NAME_DELIMITER;
            }  
            pm.removePolicy(prefix + privilegeName);
        } catch (Exception e) {
            throw new DelegationException(e);
        }
    }

    /**
     * Returns a set of selected subjects of specified types matching the
     * pattern in the given realm. The pattern accepts "*" as the wild card for
     * searching subjects. For example, "a*c" matches with any subject starting
     * with a and ending with c.
     * 
     * @param token The <code>SSOToken</code> of the requesting user
     * @param orgName The name of the realm from which the subjects are fetched.
     * @param types a set of subject types. e.g. ROLE, GROUP.
     * @param pattern a filter used to select the subjects.
     * 
     * @return a set of subjects associated with the realm.
     * 
     * @throws SSOException invalid or expired single-sign-on token
     * @throws DelegationException for any abnormal condition
     *
     * @return <code>Set</code> of universal Ids of the subjects associated 
     *         with the realm.
     *
     * @throws SSOException invalid or expired single-sign-on token
     * @throws DelegationException for any abnormal condition
     */

    public Set getSubjects(SSOToken token, String orgName, Set types, 
        String pattern) throws SSOException, DelegationException {
        Set results = new HashSet();
        // All Authenticated Users would be returned only if pattern is *
        if ((pattern != null) && pattern.equals("*")) {
            results.add(AUTHN_USERS_ID);
        }

        if (DelegationManager.debug.messageEnabled()) {
            DelegationManager.debug.message(
                "DelegationPolicyImpl.getSubjects(): types=" + types);
        }

        try {
            AMIdentityRepository idRepo = 
                new AMIdentityRepository(appToken, orgName);
            Set supportedTypes = idRepo.getSupportedIdTypes();
            if (DelegationManager.debug.messageEnabled()) {
                DelegationManager.debug.message(
                    "DelegationPolicyImpl.getSubjects(): " +
                    "supported subject types=" + supportedTypes);
            }
            if ((supportedTypes != null) && (!supportedTypes.isEmpty())
                && (types != null) && (!types.isEmpty())) {
                Iterator it = types.iterator();
                while (it.hasNext()) {
                    IdType idType = IdUtils.getType((String)it.next());
                    if (supportedTypes.contains(idType)) {
                        IdSearchControl ctrl = new IdSearchControl();
                        ctrl.setRecursive(true);
                        ctrl.setMaxResults(-1);
                        ctrl.setTimeOut(-1);
                        IdSearchResults idsr = idRepo.searchIdentities(
                                                idType, pattern, ctrl);
                        if (idsr != null) {
                            Set searchRes = idsr.getSearchResults();
                            if ((searchRes !=null) && 
                                (!searchRes.isEmpty())) {
                                Iterator iter = searchRes.iterator(); 
                                while (iter.hasNext()) {
                                    AMIdentity id = (AMIdentity)iter.next();
                                    results.add(IdUtils.getUniversalId(id));
                                }
                            }
                        } 
                    }
                }
            }  
            return results;
        } catch (IdRepoException ide) {
            throw new DelegationException(ide);
        }
    }



    /**
     * Returns a set of realm names, based on the input parameter
     * <code>organizationNames</code>, in which the "user" has some
     * delegation permissions.
     * 
     * @param token The <code>SSOToken</code> of the requesting user
     * @param organizationNames  a <code>Set</code> of realm names.
     * 
     * @return a <code>Set</code> of realm names in which the user has some 
     *         delegation permissions. It is a subset of 
     *         <code>organizationNames</code>
     * 
     * @throws SSOException invalid or expired single-sign-on token
     * @throws DelegationException for any abnormal condition
     */

    public Set getManageableOrganizationNames(SSOToken token, 
      Set organizationNames) throws SSOException, DelegationException {
        Set names = new HashSet();

        if ((organizationNames != null) && 
            (!organizationNames.isEmpty())) {    
            Iterator it = organizationNames.iterator();
            while (it.hasNext()) {
                String orgName = (String)it.next();
                Set perms = getPermissions(token, orgName);
                if ((perms != null) && (!perms.isEmpty())) {
                    names.add(orgName);
                }
            }
        }
        return names; 
    }


    /**
     * Returns a boolean value;  if a user has the specified
     * permission returns true, false otherwise.
     * 
     * @param token Single sign on token of the user evaluating permission.
     * @param permission Delegation permission to be evaluated
     * @param envParams Run-time environment parameters.
     * @return the result of the evaluation as a boolean value
     * 
     * @throws SSOException single-sign-on token invalid or expired.
     * @throws DelegationException for any other abnormal condition.
     */
    public boolean isAllowed(SSOToken token, DelegationPermission permission, 
        Map envParams) throws SSOException, DelegationException {
        SSOTokenID tokenId;
        PolicyDecision pd;
        String resource = null;
        boolean result = false;

        if (DelegationManager.debug.messageEnabled()) {
            DelegationManager.debug.message(
                "DelegationPolicyImpl.isAllowed() is called");
        }

        if ((token != null) && ((tokenId = token.getTokenID()) != null) 
            && (permission != null)) {
            String tokenIdStr = tokenId.toString();
            Set actions = permission.getActions();
            if ((actions != null) && (!actions.isEmpty())) {
                try {
                    resource = getResourceName(permission);
                    pd = getResultFromCache(
                                    tokenIdStr, resource, envParams);
                    if (pd != null) {
                        if (DelegationManager.debug.messageEnabled()) {
                            DelegationManager.debug.message(
                        "got delegation evaluation result from cache.");
                        }
                    } else {
                        // decision not found in the cache. compute it.
                        pd = pe.getPolicyDecision(token, resource, 
                                                  null, envParams);
                        // add the result in the cache.
                        putResultIntoCache(tokenIdStr, resource, 
                                           envParams, pd);
                        if (DelegationManager.debug.messageEnabled()) {
                            DelegationManager.debug.message(
                        "put delegation evaluation result into cache.");
                        }
                    }
                    Map ads = pd.getActionDecisions();
                    if ((ads != null) && (!ads.isEmpty())) {
                        result = true;
                        Iterator it = actions.iterator();
                        while (it.hasNext() && result) {
                            String actionName = (String)it.next();
                            ActionDecision ad = 
                                (ActionDecision)ads.get(actionName);
                            if (ad != null) {
                                Set values = ad.getValues();
                                if ((values == null) || values.isEmpty()
                                         || values.contains(ACTION_DENY)) {
                                    result = false;
                                }
                            } else {
                                result = false;
                            }
                        }
                    }
                } catch (PolicyException pe) {
                    throw new DelegationException(pe);
                }
            }
            if (DelegationManager.debug.messageEnabled()) {
                DelegationManager.debug.message(
                    "DelegationPolicyImpl.isAllowed(): " +
                    "actions=" + actions + 
                    "  resource=" + resource +
                    "  result is:" + result);
            }
        }
        return result;
    }

    /**
     * Returns a policy decision given a resource and the user's token,
     * for the resource from the delegation cache.
     * @param  tokenIdStr <code>String</code> representation of user's token
     * @param  resource resource for which results are sought.
     * @param  envParams  <code>Map</code> of environment params to be
     *         used to fetch the decisions.
     * @return policy decision 
     */
    private static PolicyDecision getResultFromCache(String tokenIdStr,
      String resource, Map envParams) 
      throws SSOException, DelegationException {
        if (resource != null) {
            Map items = (Map)delegationCache.get(tokenIdStr);
            if ((items != null) && (!items.isEmpty())) {
                ArrayList al = (ArrayList)items.get(resource);
                if (al != null) {
                    Map cachedEnv = (Map)al.get(0);
                    if ((envParams == null) || (envParams.isEmpty())) {
                        envParams = Collections.EMPTY_MAP;
                    }
                    if ((cachedEnv == null) || (cachedEnv.isEmpty())) {
                        cachedEnv = Collections.EMPTY_MAP;
                    }
                    if (envParams.equals(cachedEnv)) {
                        PolicyDecision pd = (PolicyDecision)al.get(1);
                        if (pd != null) {
                            long pdTTL = pd.getTimeToLive();
                            long currentTime = 
                                       System.currentTimeMillis(); 
                            if (pdTTL > currentTime) {
                                return pd;
                            } else {
                                if (DelegationManager.debug.messageEnabled()) {
                                    DelegationManager.debug.message(
                                     "DelegationPolicyImpl: delegation "
                                     + "decision expired.  TTL=" + pdTTL
                                     + "; current time=" + currentTime);
                                }
                            }
                        } 
                    }
                }
            }
        }
        return null;
    }


    /**
     * adds the data in the delegation cache.
     * @param  tokenIdStr <code>String</code> representation of user's token
     * @param  resource resource for which results are being put in cache.
     * @param  envParams  <code>Map</code> of environment params applicable
               for the decision.
     * @param pd policy decision being cached.
     * 
     */
    private static void putResultIntoCache(String tokenIdStr,
              String resource, Map envParams, PolicyDecision pd)
        throws SSOException, DelegationException {
        if (resource != null) {
            ArrayList al = new ArrayList(2);
            al.add(0, envParams);
            al.add(1, pd);
            Map items = (Map)delegationCache.get(tokenIdStr);
            if (items == null) {
                items = new HashMap();
            }
            items.put(resource, al);
            delegationCache.put(tokenIdStr, items);
        }
    }



    /**
     * Cleans up the entire delegation cache, gets called
     * when any identity gets changed in the repository.
     */
    private static void cleanupCache() {
        if (delegationCache.size() > 0) {
            delegationCache = new Cache(maxCacheSize);
            if (DelegationManager.debug.messageEnabled()) {
               DelegationManager.debug.message(
                "DelegationPolicyImpl.cleanupCache(): cache cleared");
            }
        }

        // Clear the SubjectEvaluationCache on any identity changes if active and not empty.
        if (SubjectEvaluationCache.subjectEvalCacheTTL > 0 && !SubjectEvaluationCache.subjectEvaluationCache.isEmpty()) {
            SubjectEvaluationCache.subjectEvaluationCache.clear();
            if (DelegationManager.debug.messageEnabled()) {
               DelegationManager.debug.message(
                "DelegationPolicyImpl.cleanupCache(): subjectEvaluationCache cleared");
            }
        }
    }

 
    /**
     * Returns a set of permissions that a user has.
     * 
     * @param token sso token of the user requesting permissions
     * @param orgName The name of the realm from which the delegation 
     *        permissions are fetched.
     * 
     * @return a <code>Set</code> of permissions that a user has
     * 
     * @throws SSOException if single-sign-on token invalid or expired
     * @throws DelegationException for any other abnormal condition
     */
    public Set getPermissions(SSOToken token, String orgName) 
        throws SSOException, DelegationException {

        DelegationPrivilege dp;
        Set perms = new HashSet();
        Set subjects;
        AMIdentity userIdentity = null;
        AMIdentity subjectIdentity = null;
        IdSearchResults results = null;

        if (token == null) {
            if (DelegationManager.debug.warningEnabled()) {
                DelegationManager.debug.warning(
                    "DelegationPolicyImpl.getPermissions():"
                     + "user sso token is null");
            }
            return perms;
        }
        
        try {
            userIdentity = IdUtils.getIdentity(token);
            if (userIdentity == null) {
                if (DelegationManager.debug.warningEnabled()) {
                    DelegationManager.debug.warning(
                      "DelegationPolicyImpl.getPermissions():"
                      + "could not get user's identity from token");
                }
                return perms;
            }
  
            Set privileges = getPrivileges(appToken, orgName);
            if ((privileges != null) && (!privileges.isEmpty())) {
                AMIdentityRepository idRepo = 
                    new AMIdentityRepository(appToken, orgName);
                IdSearchControl ctrl = new IdSearchControl();
                ctrl.setRecursive(true);
                ctrl.setMaxResults(-1);
                ctrl.setTimeOut(-1);
                Iterator it = privileges.iterator();
                while (it.hasNext()) {
                    dp = (DelegationPrivilege)it.next();
                    subjects = dp.getSubjects();
                    if ((subjects != null) && (!subjects.isEmpty())) {
                        Iterator sit = subjects.iterator();
                        while (sit.hasNext()) { 
                            String subject = (String)sit.next();
                            String subjectId = 
                                (new DN(subject)).explodeDN(true)[0];
                            if (subjectId != null) {
                                results = idRepo.searchIdentities(
                                           IdType.ROLE, subjectId, ctrl);
                                if (results != null) {
                                    Set idSet = results.getSearchResults();
                                    if ((idSet != null) && !idSet.isEmpty()) {
                                        subjectIdentity = (AMIdentity)(
                                                 idSet.iterator().next());
                                        if (userIdentity.isMember(
                                                      subjectIdentity)) {
                                            perms.addAll(dp.getPermissions());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new DelegationException(e);
        }
        return perms;
    }

        
    /** 
     * Converts a delegation privilege to a policy.
     * @param pm PolicyManager object to be used to create the <code>Policy
     *         </code> object.
     * @param priv <code>DelegationPrivilege</code> which needs to be
               converted.
     * @return policy object.
     */
    private Policy privilegeToPolicy(
        PolicyManager pm,
        DelegationPrivilege priv,
        String orgName
    ) throws DelegationException {
        try {
            /* the name of the policy is in the form of 
             * orgName^^privilegeName, the privilegeName is the
             * name of the delegation privilege that the policy 
             * is corresponding to. In case the orgName is in a 
             * DN format, the special char ',' is replaced to 
             * avoid saving problem.
             */
            String prefix = null;
            if (orgName != null) {
                prefix = orgName.toLowerCase() + NAME_DELIMITER;
                prefix = prefix.replace(',', REPLACEMENT_FOR_COMMA);
            } else {
                prefix = NAME_DELIMITER;
            }
            String name = prefix + priv.getName();
            Policy policy = new Policy(name);
            
            Set permissions = priv.getPermissions();
            if ((permissions != null) && (!permissions.isEmpty())) {
                Iterator pmit = permissions.iterator();
                int seqNum = 0;
                while (pmit.hasNext()) {
                    DelegationPermission perm =
                        (DelegationPermission)pmit.next();
                    String resourceName = getResourceName(perm);
                    Map actions = new HashMap();
                    Set permActions = perm.getActions();
                    if (permActions != null) {
                        Set values = new HashSet();
                        values.add(ACTION_ALLOW);
                        Iterator it = permActions.iterator();
                        while (it.hasNext()) {
                            String actionName = (String)it.next();
                            actions.put(actionName, values);
                        }
                    }
                    String ruleName = DELEGATION_RULE;
                    if (seqNum != 0) {
                        ruleName += seqNum;
                    } 
                    Rule rule = new Rule(ruleName,
                               DelegationManager.DELEGATION_SERVICE,
                               resourceName, actions);
                    policy.addRule(rule);
                    seqNum++;
                }
            }
            Set sv = new HashSet(priv.getSubjects());
            if ((sv != null) && (sv.contains(AUTHN_USERS_ID))) { 
                Subject allauthNUsers = 
                    pm.getSubjectTypeManager().getSubject(
                        AUTHENTICATED_USERS_SUBJECT);
                policy.addSubject(DELEGATION_AUTHN_USERS, allauthNUsers);
                sv.remove(AUTHN_USERS_ID);
            }
            if ((sv != null) && (!sv.isEmpty())) {  
                Subject subject = 
                    pm.getSubjectTypeManager().getSubject(POLICY_SUBJECT);
                subject.setValues(sv); 
                policy.addSubject(DELEGATION_SUBJECT, subject);
            }
            return policy;
        } catch (Exception e) {
            DelegationManager.debug.error(
                "unable to convert a privilege to a policy", e);
            throw new DelegationException(e);
        }
    }


    /**
     *  Converts a policy to a delegation privilege.
     * @param policy policy to be converted
     * @return priv <code>DelegationPrivilege</code> represting policy.
     */
    private DelegationPrivilege policyToPrivilege(Policy policy) 
        throws DelegationException {

        String pname = null;
        Set permissions = new HashSet();
        Set svalues = new HashSet();

        if (policy == null) {
            return null;
        }
        try {
            // get policy name, which is the privilege name as well
            pname = policy.getName();
 
            // get privilege subjects
            Set snames = policy.getSubjectNames();
            if ((snames != null) && (!snames.isEmpty())) {
                if (snames.contains(DELEGATION_AUTHN_USERS)) {
                    svalues.add(AUTHN_USERS_ID);
                }
                if (snames.contains(DELEGATION_SUBJECT)) {
                    Subject subject = policy.getSubject(DELEGATION_SUBJECT);
                    Set values = subject.getValues();
                    if (values != null) {
                        svalues.addAll(values);
                    }
                }
            }
 
            if (DelegationManager.debug.messageEnabled()) {
                DelegationManager.debug.message(
                    "SubjectValues=" + svalues);
            }

            String realmName = null;
            String serviceName = null;
            String version = null;
            String configType = null;
            String subconfigName = null;
            String resource = null;
            Set actions = null;
            Set ruleNames = policy.getRuleNames();
            if ((ruleNames != null) && (!ruleNames.isEmpty())) {
                Iterator rit = ruleNames.iterator();
                while (rit.hasNext()) {
                    String ruleName = (String)rit.next();
                    // now try to get resource and action names
                    Rule rule = policy.getRule(ruleName);
                    String service = rule.getServiceTypeName();
                    if (service.equalsIgnoreCase(
                        DelegationManager.DELEGATION_SERVICE)) {
                        resource = rule.getResourceName();
                        actions = rule.getActionNames();
                        // parse the resource to get information 
                        // required to construct a delegation permission
                        if (resource.startsWith(PREFIX)) {
                            String suffix = 
                                resource.substring(PREFIX.length());
                            if (suffix != null) {
                                StringTokenizer st = new StringTokenizer(
                                    suffix, DELIMITER);
                                realmName = st.nextToken();
                                if (st.hasMoreTokens()) {
                                    serviceName = st.nextToken();
                                    if (st.hasMoreTokens()) {
                                        version = st.nextToken();
                                        if (st.hasMoreTokens()) {
                                            configType = st.nextToken();
                                            if (st.hasMoreTokens()) {
                                                subconfigName = st.nextToken();
                                                while (st.hasMoreTokens()) {
                                                    subconfigName += 
                                                        DELIMITER 
                                                        + st.nextToken();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (DelegationManager.debug.messageEnabled()) {
                            DelegationManager.debug.message(
                                "DelegationPolicyImpl.policyToPrivilege(): "
                                + "create DelegationPermission object with: "
                                + "realm=" + realmName + "; service=" 
                                + serviceName + "; version=" + version
                                + "; configType=" + configType 
                                + "; subconfig=" + subconfigName 
                                + "; actions=" + actions); 
                        }
                        DelegationPermission dp = new DelegationPermission(
                            realmName, serviceName, version, configType,
                            subconfigName, actions, null); 
                        permissions.add(dp);                
                                
                    }
                }
            }
            return new DelegationPrivilege(pname, permissions, svalues); 
        } catch (Exception e) {
            throw new DelegationException(e);
        }
    }


    /**
     * gets a resource string based on a delegation permission object
     * @param perm <code>DelegationPermission</code> from which resource
              name needs to be determined.
     * @return resource name
     */
    private String getResourceName(DelegationPermission perm) {
        String realmName = perm.getOrganizationName();
        String serviceName = perm.getServiceName();
        String version = perm.getVersion();
        String configType = perm.getConfigType();
        String subConfigName = perm.getSubConfigName();
        StringBuilder sb = new StringBuilder(100);
        sb.append(PREFIX);
        if (realmName != null) {
            sb.append(realmName);
        }
        sb.append(DELIMITER);
        if (serviceName != null) {
            sb.append(serviceName);
        }
        if (version != null) {
            sb.append(DELIMITER);
            sb.append(version);
        }
        if (configType != null) { 
            sb.append(DELIMITER);
            sb.append(configType);
        }
        if (subConfigName != null) {
            sb.append(DELIMITER);
            sb.append(subConfigName);
        }
        return sb.toString();
    }


    // The following three methods implement ServiceListener interface

    /**
     * This method will be invoked when a service's schema has been changed.
     *
     * @param serviceName name of the service
     * @param version version of the service
     */
    public void schemaChanged(String serviceName, String version) {
        // NO-OP
    }

    /**
     * This method will be invoked when a service's global configuration
     * data has been changed. The parameter <code>groupName</code> denote
     * the name of the configuration grouping (e.g. default) and
     * <code>serviceComponent</code> denotes the service's sub-component
     * that changed (e.g. <code>/NamedPolicy</code>, <code>/Templates</code>).
     * 
     * @param serviceName name of the service.
     * @param version version of the service.
     * @param groupName name of the configuration grouping.
     * @param serviceComponent name of the service components that
     *        changed.
     * @param type change type, i.e., ADDED, REMOVED or MODIFIED.
     */
    public void globalConfigChanged(String serviceName, String version,
        String groupName, String serviceComponent, int type) {
        // NO-OP
    }

    /**
     * This method will be invoked when a service's organization
     * configuration data has been changed. The parameters orgName,
     * groupName and serviceComponent denotes the organization name, 
     * configuration grouping name and service's sub-component that 
     * are changed respectively.
     *
     * @param serviceName name of the service
     * @param version version of the service
     * @param orgName organization name as DN
     * @param groupName name of the configuration grouping
     * @param serviceComponent the name of the service components that
     *                          changed
     * @param type change type, i.e., ADDED, REMOVED or MODIFIED
     */
    public void organizationConfigChanged(String serviceName, 
        String version, String orgName, String groupName, 
        String serviceComponent, int type) {
        if (DelegationManager.debug.messageEnabled()) {
            DelegationManager.debug.message(
               "DelegationPolicyImpl: org config changed: " + orgName);
        }
        synchronized(idRepoListeners) {
            if (type == ServiceListener.ADDED) {
                if (idRepoListeners.get(orgName) == null) {
                    try {
                        AMIdentityRepository idRepo = 
                            new AMIdentityRepository(appToken, orgName);
                        idRepo.addEventListener(this);
                        idRepoListeners.put(orgName, idRepo);
                        if (DelegationManager.debug.messageEnabled()) {
                            DelegationManager.debug.message(
                            "DelegationPolicyImpl: IdRepo event listener"
                            + " added for realm (" + orgName + ").");
                        }
                    } catch (Exception e) {
                        DelegationManager.debug.error(
                        "DelegationPolicyImpl: failed to process " +
                        "organization config changes. ", e); 
                    }
                }
            } else if (type == ServiceListener.REMOVED) {
                idRepoListeners.remove(orgName);
                if (DelegationManager.debug.messageEnabled()) {
                    DelegationManager.debug.message(
                    "DelegationPolicyImpl: IdRepo event listener"
                    + " removed for realm (" + orgName + ").");
                }
            }
        }
    }


    // The following four methods implement IdEventListener interface

    /**
     * This method is called back for all identities that are
     * modified in a repository.
     * @param universalId Universal Identifier of the identity.
     */
    public void identityChanged(String universalId) {
        if (DelegationManager.debug.messageEnabled()) {
           DelegationManager.debug.message(
           "DelegationPolicyImpl: changed universalId=" + universalId);
        }
        cleanupCache();
    }
      
    /**
     * This method is called back for all identities that are
     * deleted from a repository. The universal identifier 
     * of the identity is passed in as an argument
     * @param universalId Univerval Identifier
     */
    public void identityDeleted(String universalId) {
        if (DelegationManager.debug.messageEnabled()) {
           DelegationManager.debug.message(
           "DelegationPolicyImpl: deleted universalId=" + universalId);
        }
        cleanupCache();
    }
    
    /**
     * This method is called for all identities that are
     * renamed in a repository. The universal identifier
     * of the identity is passed in as an argument
     * @param universalId Universal Identifier
     */
    public void identityRenamed(String universalId) {
        if (DelegationManager.debug.messageEnabled()) {
           DelegationManager.debug.message(
           "DelegationPolicyImpl: renamed universalId=" + universalId);
        }
        cleanupCache();
    }
 
   /**
    * The method is called when all identities in the repository are
    * changed. This could happen due to a organization deletion or
    * permissions change etc
    *
    */
    public void allIdentitiesChanged() {
        if (DelegationManager.debug.messageEnabled()) {
           DelegationManager.debug.message(
           "DelegationPolicyImpl: all identities changed.");
        }
        cleanupCache();
    }


    // The following two methods implement PolicyListener interface.

    /** Gets the service type name for which this listener wants to get
     *  notifications
     *  @return delegation service name
     */
     public String getServiceTypeName() {
         return DelegationManager.DELEGATION_SERVICE;
     }

    /** This method is called by the policy framework whenever 
     *  a policy is added, removed or changed. The notification
     *  is sent only if the policy has any rule that has the 
     *  <code>serviceTypeName</code> of this listener
     *
     *  @param policyEvent event object sent by the policy framework
     *  @see com.sun.identity.policy.PolicyEvent
     */
     public void policyChanged(PolicyEvent policyEvent) {
        if (DelegationManager.debug.messageEnabled()) {
           DelegationManager.debug.message(
           "DelegationPolicyImpl: delegation policy changed.");
        }
        cleanupCache();
    }
     
    /**
     * Returns true if the user has delegation permissions for the
     * organization
     */
    private boolean hasDelegationPermissionsForRealm(SSOToken token,
        String orgName) throws SSOException, DelegationException {
        // Construct delegation permission object
        Set action = new HashSet();
        action.add("DELEGATE");
        DelegationPermission de = new DelegationPermission(orgName,
            "sunAMRealmService", "1.0", "organizationconfig", null,
             action, Collections.EMPTY_MAP);
        // Call DelegationEvaluator to handle super and internal users
        DelegationEvaluator evaluator = new DelegationEvaluator();
        return (evaluator.isAllowed(token, de, Collections.EMPTY_MAP));
    }
}
