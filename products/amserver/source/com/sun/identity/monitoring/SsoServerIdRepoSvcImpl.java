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
 * $Id: SsoServerIdRepoSvcImpl.java,v 1.2 2009/10/21 00:02:10 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.monitoring;

import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.agent.SnmpMib;
import javax.management.MBeanServer;

/**
 * This class extends the "SsoServerIdRepoSvc" class.
 */
public class SsoServerIdRepoSvcImpl extends SsoServerIdRepoSvc {
    private static Debug debug = null;

    /**
     * Constructor
     */
    public SsoServerIdRepoSvcImpl (SnmpMib myMib) {
        super(myMib);
        init(myMib, null);
    }

    public SsoServerIdRepoSvcImpl (SnmpMib myMib, MBeanServer server) {
        super(myMib, server);
        init(myMib, server);
    }

    private void init(SnmpMib myMib, MBeanServer server) {
        if (debug == null) {
            debug = Debug.getInstance("amMonitoring");
        }
    }

    /*
     * corresponds to idm's updateSearchHitCount, which
     * increments totalSearchHits and totalIntervalHits
     */
    public void incSearchCacheHits (long cacheEntries) {
        long li = IdRepoSearchCacheHits.longValue();
        li++;
        IdRepoSearchCacheHits = Long.valueOf(li);
        IdRepoCacheEntries = Long.valueOf(cacheEntries);
    }

    /*
     * corresponds to idm's incrementGetRequestCount, which
     * increments totalGetRequests and intervalCount
     */
    public void incGetRqts (long cacheEntries) {
        long li = IdRepoGetRqts.longValue();
        li++;
        IdRepoGetRqts = Long.valueOf(li);
        IdRepoCacheEntries = Long.valueOf(cacheEntries);
    }

    /*
     * corresponds to idm's updateGetHitCount, which
     * increments totalGetCacheHits and totalIntervalHits
     */
    public void incCacheHits (long cacheEntries) {
        long li = IdRepoCacheHits.longValue();
        li++;
        IdRepoCacheHits = Long.valueOf(li);
        IdRepoCacheEntries = Long.valueOf(cacheEntries);
    }

    /*
     * corresponds to idm's incrementSearchRequestCount, which
     * increments totalSearchRequests and intervalCount
     */
    public void incSearchRqts (long cacheEntries) {
        long li = IdRepoSearchRqts.longValue();
        li++;
        IdRepoSearchRqts = Long.valueOf(li);
        IdRepoCacheEntries = Long.valueOf(cacheEntries);
    }
}
