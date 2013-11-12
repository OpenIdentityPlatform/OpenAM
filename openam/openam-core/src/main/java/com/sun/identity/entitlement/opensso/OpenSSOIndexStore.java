/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: OpenSSOIndexStore.java,v 1.13 2010/01/25 23:48:15 veiming Exp $
 *
 * Portions copyright 2011-2013 ForgeRock, Inc.
 */
package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.ApplicationPrivilege;
import com.sun.identity.entitlement.ApplicationPrivilegeManager;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementThreadPool;
import com.sun.identity.entitlement.IPrivilege;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeIndexStore;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ReferredApplicationManager;
import com.sun.identity.entitlement.ResourceSaveIndexes;
import com.sun.identity.entitlement.ResourceSearchIndexes;
import com.sun.identity.entitlement.SequentialThreadPool;
import com.sun.identity.entitlement.SubjectAttributesManager;
import com.sun.identity.entitlement.interfaces.IThreadPool;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.policy.PolicyConfig;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.BufferedIterator;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.sm.DNMapper;

import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;


public class OpenSSOIndexStore extends PrivilegeIndexStore {
    private static final int DEFAULT_CACHE_SIZE = 100000;
    private static final int DEFAULT_THREAD_SIZE = 1;
    private static final int DEFAULT_IDX_CACHE_SIZE = 100000;
    private static final PolicyCache policyCache;
    private static final PolicyCache referralCache;
    private static final int policyCacheSize;
    private static final Map indexCaches;
    private static final Map referralIndexCaches;
    private static final int indexCacheSize;
    private static final DataStore dataStore = DataStore.getInstance();
    private static IThreadPool threadPool;
    private static boolean isMultiThreaded;
    private Subject superAdminSubject;

    // Initialize the caches
    static {
        Subject adminSubject = SubjectUtils.createSuperAdminSubject();
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            adminSubject, "/");

        policyCacheSize = getInteger(ec,
            EntitlementConfiguration.POLICY_CACHE_SIZE, DEFAULT_CACHE_SIZE);
        if (policyCacheSize > 0) {
            policyCache = new PolicyCache("PolicyCache", policyCacheSize);
            referralCache = new PolicyCache("ReferralPolicyCache",
                policyCacheSize);
        } else {
            policyCache = null;
            referralCache = null;
        }

        indexCacheSize = getInteger(ec,
            EntitlementConfiguration.INDEX_CACHE_SIZE, DEFAULT_IDX_CACHE_SIZE);
        if (indexCacheSize > 0) {
            indexCaches = new CaseInsensitiveHashMap();
            referralIndexCaches = new CaseInsensitiveHashMap();
        } else {
            indexCaches = null;
            referralIndexCaches = null;
        }

        int threadSize = getInteger(ec,
            EntitlementConfiguration.POLICY_SEARCH_THREAD_SIZE,
            DEFAULT_THREAD_SIZE);
        isMultiThreaded = (threadSize > 1);
        threadPool = (isMultiThreaded) ? new EntitlementThreadPool(
            threadSize) : new SequentialThreadPool();
        // Register listener for realm deletions
        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            ServiceConfigManager serviceConfigManager =
                new ServiceConfigManager(PolicyManager.POLICY_SERVICE_NAME,
                adminToken);
            serviceConfigManager.addListener(new EntitlementsListener());
        } catch (Exception e) {
            PrivilegeManager.debug.error("OpenSSOIndexStore.init " +
                "Unable to register for SMS notifications", e);
        }
    }

    private static int getInteger(EntitlementConfiguration ec, String key,
        int defaultVal) {
        Set<String> set = ec.getConfiguration(key);
        if ((set == null) || set.isEmpty()) {
            return defaultVal;
        }
        String str = set.iterator().next();
        return getNumeric(str, defaultVal);
    }

    // Instance variables
    private String realmDN;
    private IndexCache indexCache;
    private IndexCache referralIndexCache;
    private EntitlementConfiguration entitlementConfig;

    /**
     * Constructor.
     *
     * @param realm Realm Name
     */
    public OpenSSOIndexStore(Subject adminSubject, String realm) {
        super(adminSubject, realm);
        superAdminSubject = SubjectUtils.createSuperAdminSubject();
        realmDN = DNMapper.orgNameToDN(realm);
        entitlementConfig = EntitlementConfiguration.getInstance(
            adminSubject, realm);

        // Get Index caches based on realm
        if (indexCacheSize > 0) {

            synchronized (indexCaches) {
                indexCache = (IndexCache)indexCaches.get(realmDN);
                if (indexCache == null) {
                    indexCache = new IndexCache(indexCacheSize);
                    indexCaches.put(realmDN, indexCache);
                }
            }
            synchronized (referralIndexCaches) {
                referralIndexCache = (IndexCache)referralIndexCaches.get(
                    realmDN);
                if (referralIndexCache == null) {
                    referralIndexCache = new IndexCache(indexCacheSize);
                    referralIndexCaches.put(realmDN, referralIndexCache);
                }
            }
        }
    }

    private static int getNumeric(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Adds a set of privileges to the data store. Proper indexes will be
     * created to speed up policy evaluation.
     *
     * @param privileges Privileges to be added.
     * @throws com.sun.identity.entitlement.EntitlementException if addition
     * failed.
     */
    public void add(Set<IPrivilege> privileges)
        throws EntitlementException {

        for (IPrivilege p : privileges) {
            if (p instanceof Privilege) {
                add((Privilege)p);
            } else if (p instanceof ReferralPrivilege) {
                add((ReferralPrivilege)p);
            }
        }
    }

    private void add(Privilege privilege) throws EntitlementException {
        Subject adminSubject = getAdminSubject();
        String realm = getRealm();
        privilege.canonicalizeResources(adminSubject,
            DNMapper.orgNameToRealmName(realm));
        dataStore.add(adminSubject, realmDN, privilege);
        entitlementConfig.addSubjectAttributeNames(
            privilege.getEntitlement().getApplicationName(),
            SubjectAttributesManager.getRequiredAttributeNames(privilege));
    }

    private void add(ReferralPrivilege referral)
        throws EntitlementException {
        Subject adminSubject = getAdminSubject();
        String realm = getRealm();

        // clone so that canonicalized resource name will be localized.
        ReferralPrivilege clone = (ReferralPrivilege)referral.clone();
        clone.canonicalizeResources(adminSubject,
            DNMapper.orgNameToRealmName(realm));
        dataStore.addReferral(adminSubject, realm, clone);
    }

    /**
     * Deletes a set of privileges from data store.
     *
     * @param privilegeName Name of privilege to be deleted.
     * @throws EntitlementException if deletion
     * failed.
     */
    public void delete(String privilegeName)
        throws EntitlementException {
        delete(privilegeName, true);
    }

    /**
     * Deletes a referral privilege from data store.
     *
     * @param privilegeName Name of referral to be deleted.
     * @throws EntitlementException if deletion
     * failed.
     */
    public void deleteReferral(String privilegeName)
        throws EntitlementException {
        deleteReferral(privilegeName, true);
    }

    /**
     * Deletes a privilege from data store.
     *
     * @param privileges Privileges to be deleted.
     * @throws EntitlementException if deletion
     * failed.
     */
    public void delete(Set<IPrivilege> privileges)
        throws EntitlementException {
        Subject adminSubject = getAdminSubject();
        String realm = getRealm();

        for (IPrivilege p : privileges) {
            String dn = null;
            if (p instanceof Privilege) {
                dn = delete(p.getName(), true);
            } else {
                dn = deleteReferral(p.getName(), true);
            }
            if (indexCacheSize > 0) {
                ResourceSaveIndexes sIndex = p.getResourceSaveIndexes(
                    adminSubject, DNMapper.orgNameToRealmName(realm));
                if (sIndex != null) {
                    if (p instanceof Privilege) {
                        indexCache.clear(sIndex, dn);
                    } else {
                        referralIndexCache.clear(sIndex, dn);
                    }
                }
            }
        }
    }

    public String delete(String privilegeName, boolean notify)
        throws EntitlementException {
        Subject adminSubject = getAdminSubject();
        String realm = getRealm();
        String dn = DataStore.getPrivilegeDistinguishedName(
            privilegeName, realm, null);
        if (notify) {
            dataStore.remove(adminSubject, realmDN, privilegeName);
        } else {
        }

        if (policyCacheSize > 0) {
            policyCache.decache(dn, realmDN);
        }
        return dn;
    }


    public String deleteReferral(String privilegeName, boolean notify)
        throws EntitlementException {
        Subject adminSubject = getAdminSubject();
        String realm = getRealm();
        String dn = DataStore.getPrivilegeDistinguishedName(
            privilegeName, realm, DataStore.REFERRAL_STORE);
        if (notify) {
            dataStore.removeReferral(adminSubject, realm, privilegeName);
        }
        if (policyCacheSize > 0) {
            referralCache.decache(dn, realmDN);
        }
        return dn;
    }

    private void cache(
        IPrivilege eval,
        Set<String> subjectSearchIndexes,
        String realm
    ) throws EntitlementException {
        if (eval instanceof Privilege) {
            cache((Privilege)eval, subjectSearchIndexes, realm);
        } else if (eval instanceof ReferralPrivilege) {
            cache((ReferralPrivilege)eval, realm);
        }
    }

    private void cache(
        Privilege p,
        Set<String> subjectSearchIndexes,
        String realm
    ) throws EntitlementException {
        String dn = DataStore.getPrivilegeDistinguishedName(
            p.getName(), realm, null);
        String realmName = DNMapper.orgNameToRealmName(realm);
        indexCache.cache(p.getEntitlement().getResourceSaveIndexes(
            superAdminSubject, realmName), subjectSearchIndexes, dn);
        policyCache.cache(dn, p, realmDN);
    }

    private void cache(
        ReferralPrivilege p,
        String realm
    ) throws EntitlementException {
        String dn = DataStore.getPrivilegeDistinguishedName(
            p.getName(), realm, DataStore.REFERRAL_STORE);
        referralIndexCache.cache(p.getResourceSaveIndexes(superAdminSubject,
            DNMapper.orgNameToRealmName(realm)), null, dn);
        referralCache.cache(dn, p, realmDN);
    }

    /**
     * Returns an iterator of matching privilege objects.
     *
     * @param realm Realm Name.
     * @param indexes Resource search indexes.
     * @param subjectIndexes Subject search indexes.
     * @param bSubTree <code>true</code> for sub tree evaluation.
     * @return an iterator of matching privilege objects.
     * @throws com.sun.identity.entitlement.EntitlementException if results
     * cannot be obtained.
     */
    public Iterator<IPrivilege> search(String realm,
        ResourceSearchIndexes indexes,
        Set<String> subjectIndexes, boolean bSubTree)
        throws EntitlementException {
        return search(realm, indexes, subjectIndexes, bSubTree, true);
    }

    /**
     * Search for policies.
     *
     * @param realm
     *         The realm of which the policy resides.
     * @param indexes
     *         Policy indexes.
     * @param subjectIndexes
     *         Subject indexes.
     * @param bSubTree
     *         Whether in subtree mode.
     * @param bReferral
     *         Whether there is a policy referral.
     * @return An iterator of policies.
     * @throws EntitlementException
     *         Should an error occur searching for policies.
     */
    public Iterator<IPrivilege> search(String realm,
                                       ResourceSearchIndexes indexes,
                                       Set<String> subjectIndexes,
                                       boolean bSubTree,
                                       boolean bReferral
    ) throws EntitlementException {
        BufferedIterator iterator = (isMultiThreaded) ? new BufferedIterator() : new SimpleIterator();

        // When not in subtree mode path indexes should be available.
        if (!bSubTree && indexes.getPathIndexes().isEmpty()) {
            return iterator;
        }

        // When in subtree mode parent path indexes should be available.
        if (bSubTree && indexes.getParentPathIndexes().isEmpty()) {
            return iterator;
        }

        Set setDNs = new HashSet();
        if (indexCacheSize > 0) {
            setDNs.addAll(searchPrivileges(indexes, subjectIndexes, bSubTree, iterator));
            setDNs.addAll(searchReferrals(indexes, bSubTree, iterator));
        }

        if (bReferral) {
            String tmp = DN.isDN(realm) ? DNMapper.orgNameToRealmName(realm) : realm;

            if (tmp.equals("/")) {
                ReferralPrivilege ref = getOrgAliasReferral(indexes);
                if (ref != null) {
                    iterator.add(ref);
                }
            }
        }

        if (indexCacheSize == 0 || doDSSearch()) {
            threadPool.submit(new SearchTask(this, iterator, indexes, subjectIndexes, bSubTree, setDNs));
        } else {
            iterator.isDone();
        }

        return iterator;
    }

    private ReferralPrivilege getOrgAliasReferral(ResourceSearchIndexes indexes
        ) throws EntitlementException {
        ReferralPrivilege result = null;
        SSOToken adminToken = SubjectUtils.getSSOToken(superAdminSubject);

        //TOFIX check if it is webagent service
        if (OpenSSOIndexStore.isOrgAliasMappingResourceEnabled(adminToken)) {
            try {
                Set<String> realms = getReferredRealmNames(
                    adminToken, indexes);
                if ((realms != null) && !realms.isEmpty()) {
                    Map<String, Set<String>> map =
                        new HashMap<String, Set<String>>();
                    Set<String> res = new HashSet<String>();
                    res.add("http*://" +
                        getReferralURL(indexes.getHostIndexes()) + ":*");
                    map.put(
                        ApplicationTypeManager.URL_APPLICATION_TYPE_NAME,
                        res);
                    result = new ReferralPrivilege("referralprivilege111",
                        map, realms);
                }
            } catch (SSOException e) {
                PrivilegeManager.debug.error(
                    "OpenSSOIndexStore.getOrgAliasReferral", e);
            } catch (SMSException e) {
                PrivilegeManager.debug.error(
                    "OpenSSOIndexStore.getOrgAliasReferral", e);
            }
        }
        return result;
    }

    private String getReferralURL(Set<String> indexes) {
        int len = -1;
        String result = null;
        for (String s : indexes) {
            if (s.length() > len) {
                result = s;
                len = s.length();
            }
        }
        return result;
    }

    private Set<String> getReferredRealmNames(
        SSOToken adminToken,
        ResourceSearchIndexes indexes)
        throws SMSException, SSOException {
        Set<String> searchIndexes = new HashSet<String>();
        for (String s : indexes.getHostIndexes()) {
            if (s.startsWith("://")) {
                s = s.substring(3);
            }

            if (s.length() > 0) {
                searchIndexes.add(s);
            }
        }
        Set<String> searchSet = new HashSet<String>();
        searchSet.add(getReferralURL(searchIndexes));

        ServiceManager sm = new ServiceManager(adminToken);
        Set<String> realmNames = sm.searchOrganizationNames(
            PolicyManager.ID_REPO_SERVICE, PolicyManager.ORG_ALIAS,
            searchSet);
        if ((realmNames != null) && !realmNames.isEmpty()) {
            Set<String> realms = new HashSet<String>();
            for (String r : realmNames) {
                if (!r.equals("/")) {
                    if (!r.startsWith("/")) {
                        r = "/" + r;
                    }
                    realms.add(r);
                }
            }
            return realms;
        } else {
            return Collections.EMPTY_SET;
        }
    }

    private boolean doDSSearch() {
        if (!CacheTaboo.isEmpty()) {
            return true;
        }
        String realm = getRealm();

        // check if PolicyCache has all the entries for the realm
        int cacheEntries = policyCache.getCount(realm);
        int totalPolicies = DataStore.getNumberOfPolicies(realm);
        if ((totalPolicies > 0) &&(cacheEntries < totalPolicies)) {
            return true;
        }

        cacheEntries = referralCache.getCount(realm);
        int totalReferrals = DataStore.getNumberOfReferrals(realm);
        if ((totalReferrals > 0) && (cacheEntries < totalReferrals)) {
            return true;
        }

        return false;
    }

    private Set<String> searchReferrals(ResourceSearchIndexes indexes,
        boolean bSubTree, BufferedIterator iterator)
    {
        Set<String> setDNs = referralIndexCache.getMatchingEntries(indexes,
                null, bSubTree);
        for (Iterator<String> i = setDNs.iterator(); i.hasNext();) {
            String dn = (String) i.next();
            ReferralPrivilege r = referralCache.getReferral(dn);
            if (r != null) {
                iterator.add(r);
            } else {
                i.remove();
            }
        }
        return setDNs;
    }

    private Set<String> searchPrivileges(ResourceSearchIndexes indexes,
        Set<String> subjectIndexes, boolean bSubTree, BufferedIterator iterator)
    {
        Set<String> setDNs = indexCache.getMatchingEntries(indexes,
            subjectIndexes, bSubTree);
        for (Iterator<String> i = setDNs.iterator(); i.hasNext();) {
            String dn = (String) i.next();
            Privilege p = policyCache.getPolicy(dn);
            if (p != null) {
                iterator.add(p);
            } else {
                i.remove();
            }
        }
        return setDNs;
    }

    /**
     * Returns a set of privilege names that satifies a search filter.
     *
     * @param filters Search filters.
     * @param boolAnd <code>true</code> to have filters as exclusive.
     * @param numOfEntries Number of max entries.
     * @param sortResults <code>true</code> to have result sorted.
     * @param ascendingOrder <code>true</code> to have result sorted in
     * ascending order.
     * @return a set of privilege names that satifies a search filter.
     * @throws EntitlementException if search failed.
     */
    public Set<String> searchPrivilegeNames(
        Set<SearchFilter> filters,
        boolean boolAnd,
        int numOfEntries,
        boolean sortResults,
        boolean ascendingOrder
    ) throws EntitlementException {
        Subject adminSubject = getAdminSubject();
        String realm = getRealm();

        // constraint the search with the resources that the users can read.
        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance(realm, adminSubject);
        Set<String> applicationNames = apm.getApplications(
            ApplicationPrivilege.Action.READ);
        if ((applicationNames == null) || applicationNames.isEmpty()) {
            return Collections.EMPTY_SET;
        }
        
        Map<String, Set<String>> appNameToResources = 
            new HashMap<String, Set<String>>();
        for (String applName : applicationNames) {
            appNameToResources.put(applName, apm.getResources(applName,
                ApplicationPrivilege.Action.READ));
        }

        String searchFilters = getSearchFilter(filters, boolAnd);
        String ldapFilters = "(&" +
            getResourceSearchFilter(appNameToResources, realm) + searchFilters + ")";

        return dataStore.search(adminSubject, realm, ldapFilters,
                numOfEntries * (2), sortResults, ascendingOrder);
    }

    /**
     * Constructs the directory search filter.
     *
     * @param map
     *      Map of applications.
     * @param realm
     *      The realm for which the search filter is to be applied.
     * @return The search filter.
     * @throws EntitlementException
     *      Should an underlying error occur during entitlement processing.
     */
    private String getResourceSearchFilter(Map<String, Set<String>> map, String realm) 
        throws EntitlementException {
        StringBuilder buff = new StringBuilder();

        for (String applName : map.keySet()) {
            Application appl = ApplicationManager.getApplication(
                PrivilegeManager.superAdminSubject, "/", applName);
            if (appl != null) {
                for (String res : map.get(applName)) {
                    ResourceSearchIndexes idx =
                        appl.getResourceSearchIndex(res, realm);
                    buff.append(DataStore.getFilter(idx, null, true));
                }
            }
        }

        return "(|" + buff.toString() + ")";
    }

    private String getSearchFilter(Set<SearchFilter> filters, boolean boolAnd) {
        StringBuilder strFilter = new StringBuilder();
        if ((filters == null) || filters.isEmpty()) {
            strFilter.append("(ou=*)");
        } else {
            if (filters.size() == 1) {
                strFilter.append(filters.iterator().next().getFilter());
            } else {
                if (boolAnd) {
                    strFilter.append("(&");
                } else {
                    strFilter.append("(|");
                }
                for (SearchFilter psf : filters) {
                    strFilter.append(psf.getFilter());
                }
                strFilter.append(")");
            }
        }




        return strFilter.toString();
    }


    /**
     * Returns a set of referral privilege names that satifies a search filter.
     *
     * @param filters Search filters.
     * @param boolAnd <code>true</code> to have filters as exclusive.
     * @param numOfEntries Number of max entries.
     * @param sortResults <code>true</code> to have result sorted.
     * @param ascendingOrder <code>true</code> to have result sorted in
     * ascending order.
     * @return a set of referral privilege names that satifies a search filter.
     * @throws EntitlementException if search failed.
     */
    public Set<String> searchReferralPrivilegeNames(
        Set<SearchFilter> filters,
        boolean boolAnd,
        int numOfEntries,
        boolean sortResults,
        boolean ascendingOrder
    ) throws EntitlementException {
        return searchReferralPrivilegeNames(filters, getAdminSubject(),
            getRealm(), boolAnd, numOfEntries, sortResults, ascendingOrder);
    }

    /**
     * Returns a set of referral privilege names that matched a set of search
     * criteria.
     *
     * @param filters Set of search filter (criteria).
     * @param boolAnd <code>true</code> to be inclusive.
     * @param numOfEntries Number of maximum search entries.
     * @param sortResults <code>true</code> to have the result sorted.
     * @param ascendingOrder  <code>true</code> to have the result sorted in
     *        ascending order.
     * @return a set of referral privilege names that matched a set of search
     *         criteria.
     * @throws EntitlementException if search failed.
     */
    public Set<String> searchReferralPrivilegeNames(
        Set<SearchFilter> filters,
        Subject adminSubject,
        String currentRealm,
        boolean boolAnd,
        int numOfEntries,
        boolean sortResults,
        boolean ascendingOrder
    ) throws EntitlementException {
        StringBuilder strFilter = new StringBuilder();
        if (filters.isEmpty()) {
            strFilter.append("(ou=*)");
        } else {
            if (filters.size() == 1) {
                strFilter.append(filters.iterator().next().getFilter());
            } else {
                if (boolAnd) {
                    strFilter.append("(&");
                } else {
                    strFilter.append("(|");
                }
                for (SearchFilter psf : filters) {
                    strFilter.append(psf.getFilter());
                }
                strFilter.append(")");
            }
        }
        return dataStore.searchReferral(adminSubject, currentRealm,
                strFilter.toString(), numOfEntries, sortResults, ascendingOrder);
    }

    /**
     * Returns a set of resources that are referred to this realm.
     *
     * @param applicationTypeName Application type name,
     * @return a set of resources that are referred to this realm.
     * @throws EntitlementException if resources cannot be returned.
     */
    @Override
    public Set<String> getReferredResources(String applicationTypeName)
        throws EntitlementException {
        String realm = getRealm();
        if (realm.equals("/")) {
            return Collections.EMPTY_SET;
        }
        
        if (DN.isDN(realm)) {
            realm = DNMapper.orgNameToRealmName(realm);
        }

        SSOToken adminToken = SubjectUtils.getSSOToken(superAdminSubject);

        try {
            Set<String> results = new HashSet<String>();
            Set<String> realms = getPeerRealms(realm);
            realms.addAll(getParentRealms(realm));
            String filter = "(&(ou=" + DataStore.REFERRAL_APPLS + "="
                + applicationTypeName + ")(ou=" + DataStore.REFERRAL_REALMS +
                "=" + realm + "))";

            Map<String, Set<ReferralPrivilege>> referrals = new
                HashMap<String, Set<ReferralPrivilege>>();
            for (String rlm : realms) {
                referrals.put(rlm, dataStore.searchReferrals(
                    adminToken, rlm, filter));
            }

            for (String rlm : referrals.keySet()) {
                Set<ReferralPrivilege> rPrivileges = referrals.get(rlm);
                String realmName = (DN.isDN(rlm)) ?
                    DNMapper.orgNameToRealmName(rlm) : rlm;
                for (ReferralPrivilege r : rPrivileges) {
                    Map<String, Set<String>> map =
                        r.getOriginalMapApplNameToResources();
                    for (String a : map.keySet()) {
                        Application appl = ApplicationManager.getApplication(
                            PrivilegeManager.superAdminSubject, realmName, a);
                        if (appl.getApplicationType().getName().equals(
                            applicationTypeName)) {
                            results.addAll(map.get(a));
                        }
                    }
                }
            }

            results.addAll(getOrgAliasMappingResources(
                realm, applicationTypeName));

            return results;
        } catch (SMSException ex) {
            PrivilegeManager.debug.error(
                "OpenSSOIndexStore.getReferredResources", ex);
            Object[] param = {realm};
            throw new EntitlementException(275, param);
        }
    }

    private Set<String> getParentRealms(String realm) throws SMSException {
        Set<String> results = new HashSet<String>();
        SSOToken adminToken = SubjectUtils.getSSOToken(superAdminSubject);
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, realm);
        while (true) {
            ocm = ocm.getParentOrgConfigManager();
            String name = DNMapper.orgNameToRealmName(
                ocm.getOrganizationName());
            results.add(name);
            if (name.equals("/")) {
                break;
            }
        }
        return results;
    }

    private Set<String> getPeerRealms(String realm) throws SMSException {
        SSOToken adminToken = SubjectUtils.getSSOToken(superAdminSubject);
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            adminToken, realm);
        OrganizationConfigManager parentOrg = ocm.getParentOrgConfigManager();
        String base = DNMapper.orgNameToRealmName(
            parentOrg.getOrganizationName());
        if (!base.endsWith("/")) {
            base += "/";
        }
        Set<String> results = new HashSet<String>();
        Set<String> subrealms = parentOrg.getSubOrganizationNames();
        for (String s : subrealms) {
            results.add(base + s);
        }
        results.remove(getRealm());
        return results;
    }

    static Set<String> getOrgAliasMappingResources(
        String realm, String applicationTypeName
    ) throws SMSException {
        Set<String> results = new HashSet<String>();

        if (applicationTypeName.equalsIgnoreCase(
                ApplicationTypeManager.URL_APPLICATION_TYPE_NAME)) {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());

            if (isOrgAliasMappingResourceEnabled(adminToken)) {
                OrganizationConfigManager m = new
                    OrganizationConfigManager(adminToken, realm);
                Map<String, Set<String>> map = m.getAttributes(
                    PolicyManager.ID_REPO_SERVICE);
                Set<String> orgAlias = map.get(PolicyManager.ORG_ALIAS);

                if ((orgAlias != null) && !orgAlias.isEmpty()) {
                    for (String s : orgAlias) {
                        results.add(PolicyManager.ORG_ALIAS_URL_HTTPS_PREFIX +
                            s.trim() + PolicyManager.ORG_ALIAS_URL_SUFFIX);
                        results.add(PolicyManager.ORG_ALIAS_URL_HTTP_PREFIX +
                            s.trim() + PolicyManager.ORG_ALIAS_URL_SUFFIX);
                    }
                }
            }
        }
        return results;
    }

    public static boolean isOrgAliasMappingResourceEnabled(SSOToken adminToken)
    {
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(
                PolicyConfig.POLICY_CONFIG_SERVICE, adminToken);
            ServiceSchema globalSchema = ssm.getGlobalSchema();
            Map<String, Set<String>> map =
                globalSchema.getAttributeDefaults();
            Set<String> values = map.get(
                PolicyConfig.ORG_ALIAS_MAPPED_RESOURCES_ENABLED);
            if ((values != null) && !values.isEmpty()) {
                String val = values.iterator().next();
                return Boolean.valueOf(val);
            } else {
                return false;
            }
        } catch (SMSException ex) {
            PrivilegeManager.debug.error(
                "OpenSSOIndexStore.isOrgAliasMappingResourceEnabled", ex);
            return false;
        } catch (SSOException ex) {
            PrivilegeManager.debug.error(
                "OpenSSOIndexStore.isOrgAliasMappingResourceEnabled", ex);
            return false;
        }
    }

    String getRealmDN() {
        return (realmDN);
    }

    // Monitoring
    public static int getNumCachedPolicies(String realm) {
        return policyCache.getCount(realm);
    }
    public static int getNumCachedReferrals(String realm) {
        return referralCache.getCount(realm);
    }
    public static int getNumCachedPolicies() {
        return policyCache.getCount();
    }
    public static int getNumCachedReferrals() {
        return referralCache.getCount();
    }

    @Override
    public boolean hasPrivilgesWithApplication(
        String realm, String applName) throws EntitlementException {
        return dataStore.hasPrivilgesWithApplication(getAdminSubject(), realm,
                applName);
    }

    public class SearchTask implements Runnable {
        private OpenSSOIndexStore parent;
        private BufferedIterator iterator;
        private ResourceSearchIndexes indexes;
        private Set<String> subjectIndexes;
        private boolean bSubTree;
        private Set<String> excludeDNs;

        public SearchTask(
            OpenSSOIndexStore parent,
            BufferedIterator iterator,
            ResourceSearchIndexes indexes,
            Set<String> subjectIndexes,
            boolean bSubTree,
            Set<String> excludeDNs
        ) {
            this.parent = parent;
            this.iterator = iterator;
            this.indexes = indexes;
            this.subjectIndexes = subjectIndexes;
            this.bSubTree = bSubTree;
            this.excludeDNs = excludeDNs;
        }

        public void run() {
            try {
                Set<IPrivilege> results = dataStore.search(
                    parent.getAdminSubject(), parent.getRealmDN(), iterator,
                    indexes, subjectIndexes, bSubTree, excludeDNs);
                if (indexCacheSize > 0) {
                    for (IPrivilege eval : results) {
                        parent.cache(eval, subjectIndexes, parent.getRealmDN());
                    }
                }
            } catch (EntitlementException ex) {
                iterator.isDone();
                PrivilegeManager.debug.error(
                    "OpenSSOIndexStore.SearchTask.runPolicy", ex);
            }
        }
    }

    // SMS Listener to clear cache when realms are deleted
    static class EntitlementsListener implements ServiceListener {

        public void schemaChanged(String serviceName, String version) {
        }

        public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int type) {
        }

        public void organizationConfigChanged(String serviceName,
            String version, String orgName, String groupName,
            String serviceComponent, int type) {
            if ((type == ServiceListener.REMOVED) &&
                ((serviceComponent == null) ||
                (serviceComponent.trim().length() == 0) ||
                serviceComponent.equals("/"))) {
                // Realm has been deleted, clear the indexCaches &
                indexCaches.remove(orgName);
                referralIndexCaches.remove(orgName);
                ReferredApplicationManager.getInstance().clearCache();
            }
        }
    }
}
