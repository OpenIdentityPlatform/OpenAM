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
 * $Id: PolicyCache.java,v 1.3 2009/12/12 00:03:13 veiming Exp $
 *
 * Portions copyright 2013 ForgeRock, Inc.
 */

package com.sun.identity.entitlement.opensso;

import com.sun.identity.entitlement.Privilege;
import java.util.HashMap;
import com.sun.identity.entitlement.ReferralPrivilege;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Policy Cache
 */
class PolicyCache {
    private Cache cache;
    private HashMap<String, Integer> countByRealm;
    private ReadWriteLock rwlock = new ReentrantReadWriteLock();

    PolicyCache(String name, int size) {
        int initCapacity = (int) (size * 0.01d);
        cache = new Cache(name, initCapacity, size);
        countByRealm = new HashMap<String, Integer>();
    }

    /**
     * Caches a privilege.
     *
     * @param dn DN of the privilege object.
     * @param p Privilege.
     */
    public void cache(String dn, Privilege p, String realm) {
        rwlock.writeLock().lock();
        try {
            Object e = cache.put(dn, p);
            if (e == null) {
                // Update count only if added, not if replaced
                Integer i = countByRealm.get(realm);
                int count = (i == null) ? 1 : i.intValue() + 1;
                countByRealm.put(realm, count);
            }
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    /**
     * Caches a referral privilege.
     *
     * @param dn DN of the referral privilege object.
     * @param p Referral privilege.
     */
    public void cache(String dn, ReferralPrivilege p, String realm) {
        rwlock.writeLock().lock();
        try {
            cache.put(dn, p);
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    public void cache(Map<String, Privilege> privileges, boolean force) {
        rwlock.writeLock().lock();
        try {
            for (String dn : privileges.keySet()) {
                if (force) {
                    cache.put(dn, privileges.get(dn));
                } else {
                    Privilege p = (Privilege)privileges.get(dn);
                    if (p == null) {
                        cache.put(dn, privileges.get(dn));
                    }
                }
            }
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    public void decache(String dn, String realm) {
        rwlock.writeLock().lock();
        try {
            Object p = cache.remove(dn);
            if (p != null) {
                // Update cache only if entry removed from cache
                Integer i = countByRealm.get(realm);
                if (i != null) {
                    countByRealm.put(realm, i.intValue() - 1);
                }
            }
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    public Privilege getPolicy(String dn) {
        rwlock.readLock().lock();
        try {
            return (Privilege)cache.get(dn);
        } finally {
            rwlock.readLock().unlock();
        }
    }
    
    /**
     * Returns the number of cached policies in the given realm
     * 
     * @param realm
     *            realm name
     * @return cached policies for the realm
     */
    public int getCount(String realm) {
        rwlock.readLock().lock();
        try {
            Integer integer = countByRealm.get(realm);
            return (integer != null) ? integer : 0;
        } finally {
            rwlock.readLock().unlock();
        }
    }

    /**
     * Returns the number of cached policies.
     * @return cached policies.
     */
    public int getCount() {
        rwlock.readLock().lock();
        try {
            int total = 0;
            for (Integer i : countByRealm.values()) {
                total += i;
            }
            return total;
        } finally {
            rwlock.readLock().unlock();
        }
    }

    public ReferralPrivilege getReferral(String dn) {
        rwlock.readLock().lock();
        try {
            return (ReferralPrivilege)cache.get(dn);
        } finally {
            rwlock.readLock().unlock();
        }
    }
}
