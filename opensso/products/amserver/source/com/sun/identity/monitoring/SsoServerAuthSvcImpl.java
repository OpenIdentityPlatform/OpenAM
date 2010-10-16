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

package com.sun.identity.monitoring;

import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.agent.SnmpMib;
import javax.management.MBeanServer;

/**
 * This class extends the "SsoServerAuthSvc" class.
 */
public class SsoServerAuthSvcImpl extends SsoServerAuthSvc {
    private static Debug debug = null;
    private static String myMibName;

    /**
     * Constructors
     */
    public SsoServerAuthSvcImpl (SnmpMib myMib) {
        super(myMib);
        myMibName = myMib.getMibName();
        init(myMib, null);
    }

    /**
     * Constructor
     */
    public SsoServerAuthSvcImpl (SnmpMib myMib, MBeanServer server) {
        super(myMib, server);
        myMibName = myMib.getMibName();
        init(myMib, server);
    }

    private void init(SnmpMib myMib, MBeanServer server) {
        if (debug == null) {
            debug = Debug.getInstance("amMonitoring");
        }
        AuthenticationFailureRate = new Long(0);
        AuthenticationSuccessRate = new Long(0);
        AuthenticationFailureCount = new Long(0);
        AuthenticationSuccessCount = new Long(0);
    }

    /*
     *  need a method to create auth module entries
     */

    /*
     *  these need to be updated to take/figure out a realm index
     */
    public void incSsoServerAuthenticationFailureCount() {
        if (!Agent.isRunning()) {
            return;
        }

        long li = AuthenticationFailureCount.longValue();
        li++;
        AuthenticationFailureCount = Long.valueOf(li);
    }

    public void incSsoServerAuthenticationSuccessCount() {
        if (!Agent.isRunning()) {
            return;
        }
        long li = AuthenticationSuccessCount.longValue();
        li++;
        AuthenticationSuccessCount = Long.valueOf(li);
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
                "    AgentIsRunning = " + Agent.isRunning() + "\n" +
                "    moduleName = " + moduleName + "\n" +
                "    realmName = " + rName + "\n" +
                "    success = " + success + "\n" +
                "    orgDN = " + orgDN);
        }
        if (debug.warningEnabled()) {
            debug.warning(classMethod + "\n" +
                "    AgentIsRunning = " + Agent.isRunning() + "\n" +
                "    moduleName = " + moduleName + "\n" +
                "    realmName = " + rName + "\n" +
                "    success = " + success + "\n" +
                "    orgDN = " + orgDN);
        }

        if (!Agent.isRunning()) {
            return;
        }

        if (rName == null) {
            if (debug.messageEnabled()) {
                debug.message(classMethod + "no realm configuration for org " +
                    orgDN + " set up yet.");
            }
            if (debug.warningEnabled()) {
                debug.warning(classMethod + "no realm configuration for org " +
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
            } else if (debug.messageEnabled()) {
                debug.message(classMethod + "got auth module instance for " +
                    rlmAuthInst);
            }
            if (debug.warningEnabled()) {
                debug.warning(classMethod + "got auth module instance for " +
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

