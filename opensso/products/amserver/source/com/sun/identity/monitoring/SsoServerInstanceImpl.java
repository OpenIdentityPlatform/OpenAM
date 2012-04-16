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
 * $Id: SsoServerInstanceImpl.java,v 1.1 2009/06/19 02:23:16 bigfatrat Exp $
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
 * This class extends the "SsoServerInstance" class.
 */
public class SsoServerInstanceImpl extends SsoServerInstance {
    private static Debug debug = null;

    /**
     * Constructor
     */
    public SsoServerInstanceImpl (SnmpMib myMib) {
        super (myMib);
        init(myMib, null);
    }

    public SsoServerInstanceImpl (SnmpMib myMib, MBeanServer server) {
        super (myMib, server);
        init(myMib, server);
    }

    private void init(SnmpMib myMib, MBeanServer server) {
        if (debug == null) {
            debug = Debug.getInstance("amMonitoring");
        }
        String classMethod = "SsoServerInstanceImpl.init:";

        /*
         *  Agent has received all the information to set up
         *  this instance's attributes prior to this instance
         *  being created.
         */
        SSOServerInfo svrinfo = Agent.getAgentSvrInfo();
        if (Agent.getSFOStatus()) {
            SsoServerSFOStatus = "on";
        } else {
            SsoServerSFOStatus = "off";
        }

        String temp = "embedded";
        if (!svrinfo.isEmbeddedDS) {
            temp = "remote";
        }
        SsoServerConfigStoreType = temp;

        SsoServerPort = new Integer(svrinfo.serverPort);
        // siteID is a String...?
        try {
            SsoServerMemberOfSite = new Integer(svrinfo.siteID);
        } catch (NumberFormatException ex) {
            debug.error(classMethod + " svrinfo.siteID = " +
                svrinfo.siteID + " is not a number");
        }
        SsoServerHostname = svrinfo.serverName;
        SsoServerId = svrinfo.serverID;

        // svrinfo.startDate is a String of form "YYYY-MM-DD HH:MM:SS"
        SsoServerStartDate = MonitoringUtil.convertDate(svrinfo.startDate);
    }
}
