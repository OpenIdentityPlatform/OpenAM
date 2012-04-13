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
 * $Id: EntitlementsStats.java,v 1.2 2009/10/13 22:36:30 veiming Exp $
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.entitlement.opensso;

import java.util.Set;

import com.sun.identity.entitlement.util.NetworkMonitor;
import com.sun.identity.shared.stats.StatsListener;
import com.sun.identity.shared.stats.Stats;

public class EntitlementsStats implements StatsListener {
	
	private Stats stats;
	
	public EntitlementsStats(Stats stats) {
		this.stats = stats;
	}

	@Override
	public void printStats() {
		StringBuilder sb = new StringBuilder(100);
		sb.append("Entitlements statistics:");
		sb.append("\n-----------------------------");
		Set<String> ntwStats = NetworkMonitor.getInstanceNames();
		for (String ntwStat : ntwStats) {
			NetworkMonitor nm = NetworkMonitor.getInstance(ntwStat);
			sb.append("\nNetworkMonitor: ").append(ntwStat);
			sb.append("\nResponse Time(ms): ").append(nm.responseTime());
			sb.append("\nThroughput: ").append(nm.throughput());
			nm.reset();
			sb.append("\n-----------------------------");
		}
		// Cache statistics
		sb.append("\nPolicyCache: ");
		sb.append(OpenSSOIndexStore.getNumCachedPolicies());
		sb.append("\nReferralCache: ");
		sb.append(OpenSSOIndexStore.getNumCachedReferrals());
		sb.append("\nTotal policies: ");
		sb.append(DataStore.getNumberOfPolicies());
		sb.append("\nTotal referrals: ");
		sb.append(DataStore.getNumberOfReferrals());

        sb.append("\n-----------------------------\n");
		stats.record(sb.toString());
	}
}
