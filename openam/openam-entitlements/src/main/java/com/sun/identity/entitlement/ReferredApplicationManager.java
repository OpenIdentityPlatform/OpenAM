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
 * $Id: ReferredApplicationManager.java,v 1.2 2010/01/20 17:01:35 veiming Exp $
 */

/*
 * Portions Copyrighted 2013 ForgeRock AS
 */
package com.sun.identity.entitlement;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This singleton contains information of all referred applications.
 */
public class ReferredApplicationManager {
    private static final ReferredApplicationManager instance = new
        ReferredApplicationManager();
    private Map<String, Set<ReferredApplication>> mapRealmToReferredAppls =
        new ConcurrentHashMap<String, Set<ReferredApplication>>();
    private ReadWriteLock rwlock = new ReentrantReadWriteLock();

    private ReferredApplicationManager() {
        clearCache();
    }

    public static ReferredApplicationManager getInstance() {
        return instance;
    }


    public Set<ReferredApplication> getReferredApplications(String realm)
        throws EntitlementException {
        Set<ReferredApplication> set = mapRealmToReferredAppls.get(realm);
        if (set != null) {
            return set;
        }

        constructApplications(realm);
        return mapRealmToReferredAppls.get(realm);
    }

    void clearCache(String realm) {
        mapRealmToReferredAppls.remove(realm);
        ApplicationManager.clearCache(realm);
    }

    public void clearCache() {
        if (mapRealmToReferredAppls.isEmpty()) {
            mapRealmToReferredAppls.put("/", Collections.EMPTY_SET);
        } else {
            for (Iterator<String> i = mapRealmToReferredAppls.keySet().
                iterator(); i.hasNext();) {
                String realm = i.next();
                if (!realm.equals("/")) {
                    i.remove();
                    ApplicationManager.clearCache(realm);
                }
            }
        }
    }

    private void constructApplications(String realm)
        throws EntitlementException {
        Set<ReferredApplication> set = mapRealmToReferredAppls.get(realm);
        if (set != null) {
            return;
        }
        rwlock.writeLock().lock();
        try {
            Map<String, ReferredApplication> tmpMap = new
                HashMap<String, ReferredApplication>();
            set = new HashSet<ReferredApplication>();
            mapRealmToReferredAppls.put(realm, set);

            ReferralPrivilegeManager rm = new ReferralPrivilegeManager(realm,
                PrivilegeManager.superAdminSubject);
            Map<String, Set<ReferralPrivilege>> mapRealmToReferralPrivileges =
                rm.getReferredPrivileges(realm);

            for (String r : mapRealmToReferralPrivileges.keySet()) {
                createApplication(realm, r,
                    mapRealmToReferralPrivileges.get(r), set, tmpMap);
            }
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    private void createApplication(
        String realm,
        String r,
        Set<ReferralPrivilege> referralPrivileges,
        Set<ReferredApplication> applications,
        Map<String, ReferredApplication> tmpMap
    ) throws EntitlementException {
        for (ReferralPrivilege rp : referralPrivileges) {
            Map<String, Set<String>> raMap =
                rp.getMapApplNameToResources();

            for (String applName : raMap.keySet()) {
                Set<String> res = raMap.get(applName);
                ReferredApplication ra = tmpMap.get(applName);

                if (ra == null) {
                    Application appl = ApplicationManager.getApplication(
                        PrivilegeManager.superAdminSubject, r, applName);
                    ra = new ReferredApplication(realm, applName,
                        appl, res);
                    tmpMap.put(applName, ra);
                    applications.add(ra);
                } else {
                    ra.addResources(res);
                }
            }
        }
    }


    private void deleteApplication(String realm, ReferredApplication ra) {
        Set<ReferredApplication> set = mapRealmToReferredAppls.get(realm);
        if (set != null) {
            set.remove(ra);
        }
    }

    private void addApplication(
        String parentRealm,
        String realm,
        String applName,
        Set<String> resources
    ) throws EntitlementException {
        Application appl = ApplicationManager.getApplication(
            PrivilegeManager.superAdminSubject,
            parentRealm, applName);
        ReferredApplication ra = new ReferredApplication(realm, applName,
            appl, resources);
        Set<ReferredApplication> set = mapRealmToReferredAppls.get(realm);
        set.add(ra);
    }

    private ReferredApplication getApplication(String realm, String applName) {
        Set<ReferredApplication> set = mapRealmToReferredAppls.get(realm);
        for (ReferredApplication ra : set) {
            if (ra.getName().equals(applName)) {
                return ra;
            }
        }
        return null;
    }
}
