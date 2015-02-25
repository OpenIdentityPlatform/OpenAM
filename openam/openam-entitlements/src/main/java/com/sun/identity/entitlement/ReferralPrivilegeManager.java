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
 * If applicable, addReferral the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: ReferralPrivilegeManager.java,v 1.7 2010/01/20 17:01:35 veiming Exp $
 *
 * Portions Copyrighted 2012-2014 ForgeRock AS
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.entitlement.util.SearchFilter;
import java.security.Principal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

import static org.forgerock.openam.utils.CollectionUtils.asSet;

/**
 * Referral Privilege Manager manages referral privilege.
 */
public class ReferralPrivilegeManager implements IPrivilegeManager<ReferralPrivilege> {

    private String realm;
    private Subject adminSubject;

    /**
     * Constructor.
     *
     * @param realm Realm.
     * @param subject subject to initilialize the privilege manager with
     */
    public ReferralPrivilegeManager(String realm, Subject subject) {
        this.realm = realm;
        this.adminSubject = subject;
    }

    /**
     * Add a referral privilege.
     *
     * @param referral referral privilege to add.
     * @throws EntitlementException if referral privilege cannot be added.
     */
    @Override
    public void add(ReferralPrivilege referral) throws EntitlementException {
        Date date = new Date();
        referral.setCreationDate(date.getTime());
        referral.setLastModifiedDate(date.getTime());

        Set<Principal> principals = adminSubject.getPrincipals();
        String principalName = ((principals != null) &&
            !principals.isEmpty()) ? principals.iterator().next().getName() : null;

        if (principalName != null) {
            referral.setCreatedBy(principalName);
            referral.setLastModifiedBy(principalName);
        }

        PolicyDataStore pdb = PolicyDataStore.getInstance();
        pdb.addReferral(adminSubject, realm, referral);

        for (String r : referral.getRealms()) {
            ReferredApplicationManager.getInstance().clearCache(r);
        }
        notifyPrivilegeChanged(null, referral);
    }

    /**
     * Finds a referral privilege by its unique name.
     *
     * @param name name of the referral privilege to be returned
     * @throws com.sun.identity.entitlement.EntitlementException if referral privilege is not found.
     */
    @Override
    public ReferralPrivilege findByName(String name) throws EntitlementException {
        PolicyDataStore pdb = PolicyDataStore.getInstance();
        return pdb.getReferral(adminSubject, realm, name);
    }

    /**
     * Checks if a privilege with the specified name can be found.
     *
     * @param name name of the privilege.
     * @throws com.sun.identity.entitlement.EntitlementException if search failed.
     * @return true if a privilege with the specified name exists, false otherwise.
     */
    @Override
    public boolean canFindByName(String name) throws EntitlementException {
        SearchFilter filter = new SearchFilter("name", name);
        return !searchNames(asSet(filter)).isEmpty();
    }

    /**
     * Remove a referral privilege.
     *
     * @param name name of the referral privilege to be removed.
     * @throws EntitlementException if referral privilege cannot be removed.
     */
    @Override
    public void remove(String name) throws EntitlementException {
        ReferralPrivilege referral = findByName(name);

        if (referral != null) {
            for (String r : referral.getRealms()) {
                ReferredApplicationManager.getInstance().clearCache(r);
            }
            PolicyDataStore pdb = PolicyDataStore.getInstance();
            pdb.removeReferral(adminSubject, realm, referral);
            notifyPrivilegeChanged(null, referral);
        }
    }

    /**
     * Modify a referral privilege.
     *
     * @param referral the referral privilege to be modified
     * @throws com.sun.identity.entitlement.EntitlementException if referral privilege cannot be modified.
     */
    @Override
    public void modify(ReferralPrivilege referral) throws EntitlementException {
        ReferralPrivilege orig = findByName(referral.getName());
        if (orig != null) {
            referral.setCreatedBy(orig.getCreatedBy());
            referral.setCreationDate(orig.getCreationDate());
        }
        Date date = new Date();
        referral.setLastModifiedDate(date.getTime());

        Set<Principal> principals = adminSubject.getPrincipals();
        if ((principals != null) && !principals.isEmpty()) {
            referral.setLastModifiedBy(principals.iterator().next().getName());
        }

        PolicyDataStore pdb = PolicyDataStore.getInstance();
        pdb.removeReferral(adminSubject, realm, referral);
        pdb.addReferral(adminSubject, realm, referral);

        for (String r : referral.getRealms()) {
            ReferredApplicationManager.getInstance().clearCache(r);
        }
        notifyPrivilegeChanged(orig, referral);
    }

    /**
     * Returns a set of referral privilege names for a given search criteria.
     *
     * @param filter Set of search filter.
     * @param searchSizeLimit Search size limit.
     * @param searchTimeLimit Search time limit in seconds.
     * @return a set of referral privilege names for a given search criteria.
     * @throws EntitlementException if search failed.
     */
    @Override
    public Set<String> searchNames(Set<SearchFilter> filter, int searchSizeLimit, int searchTimeLimit)
            throws EntitlementException {

        PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(adminSubject, realm);
        return pis.searchReferralPrivilegeNames(filter, true, searchSizeLimit, false, false);
        //TODO Search size and time limit
    }

    /**
     * Returns a set of privilege names for a given search criteria.
     *
     * @param filter Set of search filter.
     * @return a set of privilege names for a given search criteria.
     * @throws EntitlementException if search failed.
     */
    @Override
    public Set<String> searchNames(Set<SearchFilter> filter) throws EntitlementException {
        PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(adminSubject, realm);
        return pis.searchReferralPrivilegeNames(filter, true, 0, false, false);
        //TODO Search size and time limit
    }

    protected void notifyPrivilegeChanged(ReferralPrivilege previous, ReferralPrivilege current) {

        Map<String, Set<String>> mapApplNameToRes = new HashMap<String, Set<String>>();

        if (previous != null) {
            Map<String, Set<String>> m = previous.getMapApplNameToResources();
            if (m != null) {
                mapApplNameToRes.putAll(m);
            }
        }

        Map<String, Set<String>> m = current.getMapApplNameToResources();
        if (m != null) {
            combineMap(mapApplNameToRes, m);
        }

        String name = current.getName();
        for (String app : mapApplNameToRes.keySet()) {
            Set<String> resourceNames = mapApplNameToRes.get(app);
            PrivilegeChangeNotifier.getInstance().notify(adminSubject, realm, app, name, resourceNames);
        }
    }

    private void combineMap(Map<String, Set<String>> m1, Map<String, Set<String>> m2) {
        Set<String> keys = new HashSet<String>();
        keys.addAll(m1.keySet());
        keys.addAll(m2.keySet());

        for (String k : keys) {
            Set<String> s1 = m1.get(k);
            if (s1 == null) {
                s1 = new HashSet<String>();
                m1.put(k, s1);
            }

            Set<String> s2 = m2.get(k);
            if (s2 != null) {
               s1.addAll(s2);
            }
        }
    }

    /**
     * Returns the referred privileges for a given realm.
     */
    public Map<String, Set<ReferralPrivilege>> getReferredPrivileges(String targetRealm) throws EntitlementException {
        if ((realm == null) || (realm.trim().isEmpty()) || (realm.trim().equals("/"))) {
            return Collections.EMPTY_MAP;
        }

        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(PrivilegeManager.superAdminSubject, realm);
        Set<String> names = ec.getParentAndPeerRealmNames();
        targetRealm = ec.getRealmName(targetRealm);
        Map<String, Set<ReferralPrivilege>> results = new HashMap<String, Set<ReferralPrivilege>>();
        
        for (String name : names) {
            if (!name.startsWith("/")) {
                name = "/" + name;
            }
            results.put(name, getReferredPrivileges(name, targetRealm));
        }

        return results;
    }

    private Set<ReferralPrivilege> getReferredPrivileges(String baseRealm, String targetRealm)
            throws EntitlementException {

        ReferralPrivilegeManager mgr = new ReferralPrivilegeManager(baseRealm, PrivilegeManager.superAdminSubject);
        Set<String> names = mgr.searchNames(Collections.EMPTY_SET);
        Set<ReferralPrivilege> results = new HashSet<ReferralPrivilege>();

        for (String name : names) {
            ReferralPrivilege p = mgr.findByName(name);
            for (String r : p.getRealms()) {
                if (r.equalsIgnoreCase(targetRealm)) {
                    results.add(p);
                    break;
                }
            }
        }

        return results;
    }

}
