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
 * $Id: SsoServerAuthSvcImpl.java,v 1.2 2009/10/21 00:02:10 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011-13 ForgeRock Inc.
 */
package com.sun.identity.monitoring;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.agent.SnmpMib;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import javax.management.MBeanServer;

/**
 * This class extends the "SsoServerAuthSvc" class.
 */
public class SsoServerAuthSvcImpl extends SsoServerAuthSvc {
    private static Debug debug = null;
    
    /*
     * This is the interval over which the authehticate rate will be averaged
     * in seconds
     */
    private long interval;
    private long frequency;
    private long lastCheckpoint = System.currentTimeMillis();
    private static long DEFAULT_INTERVAL = 3600;
    private static int MINIMUM_FREQUENCY = 250;
    private static int AVERAGE_RECORD_COUNT = 1000;
    private Deque<Long> historicSuccessRecords;
    private Deque<Long> historicFailureRecords;

    /**
     * Constructors
     */
    public SsoServerAuthSvcImpl (SnmpMib myMib) {
        super(myMib);
        init(myMib, null);
    }

    /**
     * Constructor
     */
    public SsoServerAuthSvcImpl (SnmpMib myMib, MBeanServer server) {
        super(myMib, server);
        init(myMib, server);
    }

    private void init(SnmpMib myMib, MBeanServer server) {
        if (debug == null) {
            debug = Debug.getInstance("amMonitoring");
        }

        String intervalValue =
            SystemProperties.get(Constants.AUTH_RATE_MONITORING_INTERVAL);

        try {
            interval = Long.parseLong(intervalValue);
        } catch (NumberFormatException nfe) {
            debug.message("SsoServerAuthSvcImpl::init interval value is not " +
                    " a number " + intervalValue + " set to default of " +
                    DEFAULT_INTERVAL);
            interval = DEFAULT_INTERVAL;
        }

        if (((interval * 1000) / AVERAGE_RECORD_COUNT) < MINIMUM_FREQUENCY) {
            frequency = MINIMUM_FREQUENCY;
        } else {
            frequency = (interval * 1000) / AVERAGE_RECORD_COUNT;
        }

        if (debug.messageEnabled()) {
            debug.message("Monitoring interval set to " + interval + "ms.");
        }

        historicFailureRecords = new LinkedBlockingDeque<Long>();
        historicSuccessRecords = new LinkedBlockingDeque<Long>();

    }


    /*
    Update both success and failure rates at the same time
     */
    protected void updateSsoServerAuthenticationRates() {

        // if our checkpoint is stale, replace it with a new one
        if (System.currentTimeMillis() > (lastCheckpoint + (interval * 1000))) {
            lastCheckpoint = System.currentTimeMillis();
        }

        // remove success records that are older than the interval we're interested in
        while (historicSuccessRecords.size() > 0 &&
                historicSuccessRecords.peekFirst() < (lastCheckpoint - (interval * 1000))) {
            historicSuccessRecords.removeFirst();
        }

        // remove failure records that are older than the interval we're interested in
        while (historicFailureRecords.size() > 0 &&
                historicFailureRecords.peekFirst() < (lastCheckpoint - (interval * 1000))) {
            historicFailureRecords.removeFirst();
        }

        // get count of successful and failed logins
        long lis = historicSuccessRecords.size();
        long lif = historicFailureRecords.size();

        long total = lis + lif;

        // if there are no logins then assign 0 so we don't divide by 0
        if (total == 0) {
            AuthenticationSuccessRate = 0;
            AuthenticationFailureRate = 0;
        } else {
            AuthenticationSuccessRate = (int) (100 * lis/total);
            AuthenticationFailureRate = (int) (100 * lif/total);

            // make the addition of both rates == 100
            if (AuthenticationFailureRate + AuthenticationSuccessRate < 100) {
                if (AuthenticationFailureRate > AuthenticationSuccessRate) {
                    AuthenticationFailureRate += (100 - (AuthenticationFailureRate + AuthenticationSuccessRate));
                } else {
                    AuthenticationSuccessRate += (100 - (AuthenticationFailureRate + AuthenticationSuccessRate));
                }
            }
        }
    }

    /*
     *  need a method to create auth module entries
     */

    /*
     *  these need to be updated to take/figure out a realm index
     */
    public void incSsoServerAuthenticationFailureCount() {
        long li = AuthenticationFailureCount.longValue();
        li++;
        AuthenticationFailureCount = Long.valueOf(li);
        historicFailureRecords.add(System.currentTimeMillis());

        updateSsoServerAuthenticationRates();
    }

    public void incSsoServerAuthenticationSuccessCount() {
        long li = AuthenticationSuccessCount.longValue();
        li++;
        AuthenticationSuccessCount = Long.valueOf(li);
        historicSuccessRecords.add(System.currentTimeMillis());

        updateSsoServerAuthenticationRates();
    }

    /**
     *  incrementer for all auth modules' "commits" and "aborts", with
     *  orgDN
     */
    public void incModuleCounter(String moduleName, boolean success,
        String orgDN)
    {
        String classMethod =
            "SsoServerAuthSvcImpl.incModuleCounter:";

        /*
         *  given the orgDN and module's name, need to get the corresponding
         *  entry in the auth modules' table.
         *
         *  the orgDN must be converted to the realm name ("/"-separated)
         *  then can get the realm's index into its Table
         */

        String rName = Agent.getRealmNameFromDN(orgDN);

        if (debug.messageEnabled()) {
            debug.message(classMethod + "\n" +
                "    moduleName = " + moduleName + "\n" +
                "    realmName = " + rName + "\n" +
                "    success = " + success + "\n" +
                "    orgDN = " + orgDN);
        }

        if (rName == null) {
            if (debug.messageEnabled()) {
                debug.message(classMethod + "no realm configuration for org " +
                    orgDN + " set up yet.");
            }
            return;
        }

        if ((moduleName != null) && (moduleName.length() > 0)) {
            String rlmAuthInst = rName + "|" + moduleName;
            SsoServerAuthModulesEntryImpl mei =
                Agent.getAuthModuleEntry(rlmAuthInst);
            if (mei == null) {
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "did not find auth module instance for " + moduleName +
                        " in realm " + rName);
                }
                return;
            }
            if (debug.messageEnabled()) {
                debug.message(classMethod + "got auth module instance for " +
                    rlmAuthInst);
            }

            if (success) {
                mei.incModuleSuccessCount();
                incSsoServerAuthenticationSuccessCount();
            } else {
                mei.incModuleFailureCount();
                incSsoServerAuthenticationFailureCount();
            }
        } else {
            if (debug.warningEnabled()) {
                debug.warning(classMethod + "no module name provided");
            }
        }
    }
}

