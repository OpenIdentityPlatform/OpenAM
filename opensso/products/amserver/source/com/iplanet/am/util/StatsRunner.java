/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: StatsRunner.java,v 1.3 2008/06/25 05:41:28 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.util;

import java.util.Vector;

/**
 * @deprecated As of OpenSSO version 8.0
 *             {@link com.sun.identity.shared.stats.StatsRunner}
 */
public class StatsRunner extends Thread {

    static long period = 3600000; // in milliseconds

    static {

        String statsInterval = SystemProperties
                .get("com.iplanet.am.stats.interval");
        try {
            period = Long.parseLong(statsInterval);
            if (period <= 5) {
                period = 5;
            }
            period = period * 1000;
        } catch (Exception pe) {
        }
    }

    public StatsRunner() {
        setDaemon(true);
    }

    public void run() {

        while (true) {

            long nextRun = System.currentTimeMillis() + period;
            try {
                long sleeptime = nextRun - System.currentTimeMillis();
                if (sleeptime > 0) {
                    sleep(sleeptime);
                }
            } catch (Exception ex) {
            }

            if (!Stats.statsListeners.isEmpty()) {
                Vector lsnrs = Stats.statsListeners;
                for (int i = 0; i < lsnrs.size(); i++) {
                    StatsListener lsnr = (StatsListener) (lsnrs.elementAt(i));
                    lsnr.printStats();
                }
            }
        }
    }
}
