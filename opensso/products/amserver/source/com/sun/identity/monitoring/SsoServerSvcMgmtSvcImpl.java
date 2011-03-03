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
 * $Id: SsoServerSvcMgmtSvcImpl.java,v 1.2 2009/10/21 00:03:15 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.monitoring;

import com.iplanet.services.ldap.ServerGroup;
import com.iplanet.services.ldap.Server;
import com.iplanet.services.ldap.ServerInstance;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.agent.SnmpMib;
import java.util.Collection;
import java.util.Iterator;
import javax.management.MBeanServer;

/**
 * This class extends the "SsoServerSvcMgmtSvc" class.
 */
public class SsoServerSvcMgmtSvcImpl extends SsoServerSvcMgmtSvc {
    private static Debug debug = null;
    private static String myMibName;

    /**
     * Constructor
     */
    public SsoServerSvcMgmtSvcImpl (SnmpMib myMib) {
        super(myMib);
        myMibName = myMib.getMibName();
        init(myMib, null);
    }

    public SsoServerSvcMgmtSvcImpl (SnmpMib myMib, MBeanServer server) {
        super(myMib, server);
        myMibName = myMib.getMibName();
        init(myMib, server);
    }

    private void init (SnmpMib myMib, MBeanServer server) {
        if (debug == null) {
            debug = Debug.getInstance("amMonitoring");
        }
        String classMethod = "SsoServerSvcMgmtSvcImpl.init:";

        boolean dsEmbedded = Agent.getDsIsEmbedded();
        String dirSSL =
            SystemProperties.get(Constants.AM_DIRECTORY_SSL_ENABLED);
        String dsType = "embedded";
        if (!dsEmbedded) {
            dsType = "remote";
        }

        try {
            DSConfigMgr dscm = DSConfigMgr.getDSConfigMgr();
            ServerGroup sgrp = dscm.getServerGroup("sms");
            Collection slist = sgrp.getServersList();
            StringBuffer sbp1 = new StringBuffer("DSConfigMgr:\n");
            int port = 0;
            String svr = null;
            for (Iterator it = slist.iterator(); it.hasNext(); ) {
                Server sobj = (Server)it.next();
                svr = sobj.getServerName();
                port = sobj.getPort();
                if (debug.messageEnabled()) {
                    sbp1.append("  svrname = ").append(svr).
                        append(", port = ").append(port).append("\n");
                }
            }
            if (debug.messageEnabled()) {
                debug.message(classMethod + sbp1.toString());
            }
            ServerInstance si =
                dscm.getServerInstance(LDAPUser.Type.AUTH_BASIC);
            String bindDN = si.getAuthID();
            String orgDN = si.getBaseDN();
            boolean siStat = si.getActiveStatus();
            String conntype = si.getConnectionType().toString();
            if (debug.messageEnabled()) {
                sbp1 = new StringBuffer("ServerInstance:\n");
                sbp1.append("  bindDN = ").append(bindDN).append("\n").
                    append("  orgDN = ").append(orgDN).append("\n").
                    append("  active status = ").append(siStat).append("\n").
                    append("  conn type = ").append(conntype).append("\n");
                debug.message(classMethod + sbp1.toString());
            }

            SvcMgmtRepositoryType = dsType;
            SvcMgmtStatus = "operational";
            if (!siStat) {
                SvcMgmtStatus = "dormant";
            }
            SvcMgmtRepositorySSL = dirSSL;
                SvcMgmtRepositoryOrgDN = orgDN;
                SvcMgmtRepositoryBindDN = bindDN;
            String portS = "0";
            try {
                portS = Integer.toString(port);
            } catch (NumberFormatException nex) {
                debug.error(classMethod + "port retrieved invalid (" +
                    port + ": " + nex.getMessage());
            }
                SvcMgmtRepositoryHostPort = portS;
        } catch (Exception d) {
            debug.error(classMethod + "trying to get Directory Server Config");
        }
    }
}
