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
 * $Id: StatsRunner.java,v 1.3 2008/06/25 05:53:05 qcheng Exp $
 *
 */

package com.sun.identity.shared.stats;

import com.sun.identity.common.InstantGroupRunnable;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import java.util.Vector;

public class StatsRunner extends InstantGroupRunnable {
    static long period = 3600000; // in milliseconds

    static {
        String statsInterval = SystemPropertiesManager.get(
            Constants.AM_STATS_INTERVAL);
        try {
            period = Long.parseLong(statsInterval);
            if (period <= 5) {
                period = 5;
            }
            period = period * 1000;
        } catch (Exception pe) {
        }
    }

    public boolean addElement(Object obj) {
        synchronized (actions) {
            return actions.add(obj);
        }
    }
    
    public StatsRunner() {
        super(null, false);
    }
    
    public long getRunPeriod() {
        return period;
    }

    public void doGroupAction(Object obj) {
        ((StatsListener) obj).printStats();
    }

}
