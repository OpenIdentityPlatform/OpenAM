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
 * $Id: ReferralPrivilegeManager.java,v 1.4 2009/08/14 22:46:19 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.entitlement.util.PrivilegeSearchFilter;
import java.security.Principal;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * Referral Privilege Manager manages referral privilege.
 */
public final class ReferralPrivilegeManager {
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
     * Adds referral privilege.
     *
     * @param privilege Referral privilege.
     * @throws EntitlementException if privilege cannot be added.
     */
    public void add(ReferralPrivilege referral)
        throws EntitlementException {
        validateReferral(referral);
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

        addApplicationToSubRealm(referral);
    }

    private void validateReferral(ReferralPrivilege referral)
        throws EntitlementException {
        if (!realm.equals("/")) {
            Map<String, Set<String>> map =
                referral.getOriginalMapApplNameToResources();
            for (String appName : map.keySet()) {
                Application appl = ApplicationManager.getApplication(
                    adminSubject, realm, appName);
                ResourceName comp = appl.getResourceComparator();
                Set<String> resources = appl.getResources();
                Set<String> refResources = map.get(appName);

                for (String r : resources) {
                    validateReferral(referral, comp, r, refResources);
                }
            }
        }
    }

    private void validateReferral(
        ReferralPrivilege referral,
        ResourceName comp,
        String res,
        Set<String> refResources) throws EntitlementException {
        if (!res.endsWith("*")) {
            res += "*";
        }
        for (String rr : refResources) {
            ResourceMatch match = comp.compare(rr, res, true);
            if (match.equals(ResourceMatch.EXACT_MATCH) ||
                match.equals(ResourceMatch.WILDCARD_MATCH) ||
                match.equals(ResourceMatch.SUB_RESOURCE_MATCH)) {
                return;
            }
        }
        Object[] param = {referral.getName()};
        throw new EntitlementException(267, param);
    }

    public void addApplicationToSubRealm(ReferralPrivilege referral)
        throws EntitlementException {
        Map<String, Set<String>> map = referral.getMapApplNameToResources();
        for (String appName : map.keySet()) {
            Set<String> resources = map.get(appName);

            for (String r : referral.getRealms()) {
                ApplicationManager.referApplication(
                    adminSubject, realm, r, appName, resources);
            }
        }
    }

    /**
     * Returns a referral privilege.
     *
     * @param name Name for the referral privilege to be returned
     * @throws EntitlementException if referral privilege is not found.
     */
    public ReferralPrivilege getReferral(String name)
        throws EntitlementException {
        PolicyDataStore pdb = PolicyDataStore.getInstance();
        return pdb.getReferral(adminSubject, realm, name);
    }

    /**
     * Removes a referral privilege.
     *
     * @param name name of referral privilege to be removed.
     * @throws EntityExistsException if referral privilege cannot be removed.
     */
    public void delete(String name) throws EntitlementException {
        ReferralPrivilege referral = getReferral(name);

        if (referral != null) {
            removeApplicationFromSubRealm(referral);
            PolicyDataStore pdb = PolicyDataStore.getInstance();
            pdb.removeReferral(adminSubject, realm, name);
        }
    }

    private void removeApplicationFromSubRealm(ReferralPrivilege referral)
        throws EntitlementException {
        Map<String, Set<String>> map = referral.getMapApplNameToResources();
        for (String appName : map.keySet()) {
            Set<String> resources = map.get(appName);

            for (String r : referral.getRealms()) {
                ApplicationManager.dereferApplication(
                    adminSubject, r, appName, resources);
            }
        }
    }

    /**
     * Modifies a referral privilege.
     *
     * @param privilege the referral privilege to be modified
     * @throws EntitlementException if privilege cannot be modified.
     */
    public void modify(ReferralPrivilege referral)
        throws EntitlementException {
        ReferralPrivilege orig = getReferral(referral.getName());
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
        pdb.removeReferral(adminSubject, realm, referral.getName());
        pdb.addReferral(adminSubject, realm, referral);
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
    public Set<String> searchReferralPrivilegeNames(
        Set<PrivilegeSearchFilter> filter,
        int searchSizeLimit,
        int searchTimeLimit
    ) throws EntitlementException {
        PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(
            adminSubject, realm);
        return pis.searchReferralPrivilegeNames(filter, true, searchSizeLimit,
            false, false);//TODO Search size and time limit
    }

    /**
     * Returns a set of privilege names for a given search criteria.
     *
     * @param filter Set of search filter.
     * @return a set of privilege names for a given search criteria.
     * @throws EntitlementException if search failed.
     */
    public Set<String> searchReferralPrivilegeNames(
        Set<PrivilegeSearchFilter> filter
    ) throws EntitlementException {
        PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(
            adminSubject, realm);
        return pis.searchReferralPrivilegeNames(filter, true, 0, false, false);
        //TODO Search size and time limit
    }
}
