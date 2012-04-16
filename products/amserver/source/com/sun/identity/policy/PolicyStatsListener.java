/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PolicyStatsListener.java,v 1.3 2008/06/25 05:43:44 qcheng Exp $
 *
 */



package com.sun.identity.policy;

import com.sun.identity.shared.stats.Stats;
import com.sun.identity.shared.stats.StatsListener;

import com.sun.identity.policy.remote.PolicyRequestHandler;

/**
 * Implementation of <code> com.iplanet.am.util.StatsListener</code>,
 * to record policy cache stats
 *
 * We would potentially record cache stats of
 * PolicyCache: policies, policyManagers,  policyListenersMap
 * PolicyEvaluator:  policyResultsCache,  ssoListenerRegistry,  
 *                   policyListenerRegistry, userNSRoleCache, 
 *                   resouceNamesMap
 * PolicyRequestHandler:  policyEvaluators, listenerRegistry
 *
 * An instance of PolicyStatsListener is constructed 
 * and registered with Stats service at the first invocation of 
 * PolicyCache.getInstance() call
 */
public class PolicyStatsListener implements StatsListener {

    private Stats policyStats;

    /**
     * Constructs PolicyStatsListener
     *
     * @param policyStats <code>Stats</code> instance that would be 
     *        used to record cache stats
     */
    public PolicyStatsListener(Stats policyStats) {
        this.policyStats = policyStats;
    }

    /**
     * Records policy cache stats. 
     * This method will be invoked  by stats service
     * if the stats service is enabled
     */
    public void printStats() {
        PolicyCache.printStats(policyStats);
        PolicyEvaluator.printStats(policyStats);
        SubjectEvaluationCache.printStats(policyStats);
        PolicyRequestHandler.printStats(policyStats);
    }

}

