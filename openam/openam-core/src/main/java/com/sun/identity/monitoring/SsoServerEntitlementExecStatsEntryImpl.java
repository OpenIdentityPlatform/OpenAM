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
 * $Id: SsoServerEntitlementExecStatsEntryImpl.java,v 1.1 2009/10/20 23:54:57 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.monitoring;

import com.sun.identity.entitlement.util.NetworkMonitor;
import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * This class extends the "SsoServerEntitlementExecStatsEntry" class.
 */
public class SsoServerEntitlementExecStatsEntryImpl extends
    SsoServerEntitlementExecStatsEntry
{
    private static Debug debug = null;
    private static String myMibName;

    /**
     * Constructor
     */
    public SsoServerEntitlementExecStatsEntryImpl(SnmpMib myMib) {
        super(myMib);
        myMibName = myMib.getMibName();
        init();
    }

    private void init() {
        if (debug == null) {
            debug = Debug.getInstance("amMonitoring");
        }
    }

    public ObjectName
        createSsoServerEntitlementExecStatsEntryObjectName (MBeanServer server)
    {
        String classModule = "SsoServerEntitlementExecStatsEntryImpl." +
            "createSsoServerEntitlementExecStatsEntryObjectName: ";
        String prfx = "ssoServerEntitlementExecStatsEntry.";

        if (debug.messageEnabled()) {
            debug.message(classModule +
                "\n    SsoServerEntitlementMonName = " +
                    EntitlementNetworkMonitorName);
        }

        String objname = myMibName +
            "/ssoServerEntitlementExecStatsTable:" +
            prfx + "EntitlementNetworkMonitorName=" +
                EntitlementNetworkMonitorName;

        try {
            if (server == null) {
                return null;
            } else {
                // is the object name sufficiently unique?
                return new ObjectName(objname);
            }
        } catch (Exception ex) {
            debug.error(classModule + objname, ex);
            return null;
        }
    }

    /**
     * Getter for the "EntitlementMonitorThruPut" variable.
     */
    public Long getEntitlementMonitorThruPut() throws SnmpStatusException {
        // see if this network monitor has been instantiated
        if (MonitoringUtil.networkMonitorExist(EntitlementNetworkMonitorName)) {
            NetworkMonitor nm =
                NetworkMonitor.getInstance(EntitlementNetworkMonitorName);
            // "current" doesn't seem to be all that interesting
            float[] fa = nm.getHistoryThroughput();
            float fi = 0;
            for (int i = 0; i < fa.length; i++) {
                fi += fa[i];
            }
            Float fFt = Float.valueOf(fi);
            EntitlementMonitorThruPut = Long.valueOf(fFt.longValue());
        } else {
            if (debug.warningEnabled()) {
                debug.warning("SsoServerEntitlementExecStatsEntryImpl: " +
                    EntitlementNetworkMonitorName + " doesn't exist yet.");
            }
        }
        return EntitlementMonitorThruPut;
    }

    /**
     * Getter for the "EntitlementMonitorTotalTime" variable.
     */
    public Long getEntitlementMonitorTotalTime() throws SnmpStatusException {
        if (MonitoringUtil.networkMonitorExist(EntitlementNetworkMonitorName)) {
            NetworkMonitor nm =
                NetworkMonitor.getInstance(EntitlementNetworkMonitorName);
            
            float[] fa = nm.getHistoryResponseTime();
            float fi = 0;
            for (int i = 0; i < fa.length; i++) {
                fi += fa[i];
            }
            Float fFtm = Float.valueOf(fi);
            EntitlementMonitorTotalTime = Long.valueOf(fFtm.longValue());
        } else {
            if (debug.warningEnabled()) {
                debug.warning("SsoServerEntitlementExecStatsEntryImpl: " +
                    EntitlementNetworkMonitorName + " doesn't exist yet.");
            }
        }
        return EntitlementMonitorTotalTime;
    }
}
