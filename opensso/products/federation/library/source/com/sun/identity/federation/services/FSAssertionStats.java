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
 * $Id: FSAssertionStats.java,v 1.3 2008/06/25 05:46:52 qcheng Exp $
 *
 */

package com.sun.identity.federation.services;

import com.sun.identity.shared.stats.StatsListener;
import java.util.Map;

/**
 * Provides statistics for assertions.
 */
public class FSAssertionStats implements StatsListener {

    private Map table;
    private String realm;
    private String providerId;

    /**
     * Constructs a <code>FSAssertionStats</code> object for a given provider.
     * @param table Map of assertion ID and <code>Assertion.Entry</code> object
     * @param realm the realm in which the provier resides
     * @param providerId provider ID
     */
    public FSAssertionStats(Map table, String realm, String providerId) {
        this.table = table;
        this.realm = realm;
        this.providerId = providerId;
    }

    /**
     * Prints number of assertions in the table for a provider when the
     * stats time interval elapsed.
     */
    public void printStats() {
        if (table.size() != 0 ) {
            FSAssertionManager.assrtStats.record(
                "Number of assertion in table for provider " + providerId +
                " under realm " + realm + " : " + table.size());
        } else {
            FSAssertionManager.assrtStats.record(
                "No assertion found in table for provider " + providerId + 
                " under realm " + realm + ".");
        }
    }
}
