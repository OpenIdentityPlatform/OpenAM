/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package com.sun.identity.agents.common;

import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.filter.IFilterConfigurationConstants;
import com.sun.identity.common.PeriodicCleanUpMap;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.common.TimerPool;
import java.util.Date;
/**
 *
 * @author mad
 */
public class PDPCache implements IPDPCache, IFilterConfigurationConstants {
    public PDPCache(Manager manager) {
        if (_cache == null) {
            _ttl = manager.getConfigurationLong(CONFIG_POSTDATA_PRESERVE_TTL,
                                                DEFAULT_POSTDATA_PRESERVE_TTL);
            _cache = new PeriodicCleanUpMap(_ttl, _ttl);
            doSchedule();
        }
    }

    public void initialize() {
   }

    public boolean addEntry(String key, IPDPCacheEntry entry) {
        return _cache.put(key, entry) != null;
    }

    public boolean removeEntry(String key) {
        if (_cache.containsKey(key)) {
           _cache.remove(key);
           return true;
        }
        return false;
    }

    public IPDPCacheEntry getEntry(String key) {
        IPDPCacheEntry entry = (IPDPCacheEntry)_cache.get(key);
        return entry;
    }

    /* Schedule the periodic containers to SystemTimerPool. */
    private static void doSchedule() {
        TimerPool pool = SystemTimerPool.getTimerPool();
        Date nextRun = new Date(((System.currentTimeMillis() +
            _ttl) / 1000) * 1000);
        pool.schedule(_cache, nextRun);
    }


    private static PeriodicCleanUpMap _cache;
    private static long _ttl;
}
