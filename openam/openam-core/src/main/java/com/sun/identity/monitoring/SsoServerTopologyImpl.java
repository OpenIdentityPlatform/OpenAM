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
 * $Id: SsoServerTopologyImpl.java,v 1.2 2009/10/21 00:03:15 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.monitoring;

import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.agent.SnmpMib;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * This class extends the "SsoServerTopology" class.
 */
public class SsoServerTopologyImpl extends SsoServerTopology {
    private static Debug debug = null;

    /**
     * Constructor
     */
    public SsoServerTopologyImpl (SnmpMib myMib) {
        super(myMib);
        init(myMib, null);
    }

    public SsoServerTopologyImpl (SnmpMib myMib, MBeanServer server) {
        super(myMib, server);
        init(myMib, server);
    }

    private void init (SnmpMib myMib, MBeanServer server) {
        if (debug == null) {
            debug = Debug.getInstance("amMonitoring");
        }
        String classModule = "SsoServerTopologyImpl.init:";

        /*
         *  server topology has the
         *    servers table (all servers known to this server)
         *      SsoServerServerTable
         *    sites table (sites and servers)
         *      SsoServerSiteMapTable
         *    site map table (site-to-server mapping)
         *      SsoServerSitesTable
         *
         *  this init should get instances of
         *    SsoServerServerEntryImpl
         *    SsoServerSitesEntryImpl
         *    SsoServerSiteMapEntryImpl
         *  and add them to their corresponding tables.
         */
        
        Hashtable<String, String> ntbl = Agent.getNamingTable();
        Hashtable<String, String> sidtbl = Agent.getSiteIdTable();

        for (Map.Entry<String, String> entry : sidtbl.entrySet()) {
            String svrId = entry.getKey();
            String svrURL = ntbl.get(svrId);
            String siteId = sidtbl.get(svrId);

            URL url = null;
            String proto = null;
            String host = null;
            int port = 0;
            try {
                url = new URL(svrURL);
                proto = url.getProtocol();
                host = url.getHost();
                port = url.getPort();
            } catch (MalformedURLException mue) {
                debug.error(classModule + "invalid URL: " +
                    svrURL + "; " + mue.getMessage());
            }
            Integer iport = Integer.valueOf(1);
            Integer iid = Integer.valueOf(0);
            try {
                iport = Integer.valueOf(port);
                iid = Integer.valueOf(svrId);
            } catch (NumberFormatException nfe) {
                debug.error(classModule + "invalid port (" +
                    port + ") or server id (" + svrId + "): " +
                    nfe.getMessage(), nfe);
            }
            SsoServerServerEntryImpl ssrv =
                new SsoServerServerEntryImpl(myMib);
            ssrv.ServerPort = iport;
            ssrv.ServerHostName = host;
            ssrv.ServerProtocol = proto;
            ssrv.ServerId = iid;
            /* need a way to know what the real status is */
            ssrv.ServerStatus = Integer.valueOf(1);

            final ObjectName svrName =
                ssrv.createSsoServerServerEntryObjectName(server);
            try {
                SsoServerServerTable.addEntry(ssrv, svrName);
                if ((server != null) && (svrName != null)) {
                    server.registerMBean(ssrv, svrName);
                }
             } catch (Exception ex) {
                debug.error(classModule  + svrURL, ex);
             }

            /*
             *  fill the SsoServerSitesTable.
             *  entries have siteid, site name, and site state
             *  sidKeys has the serverIDs; the values are the site they
             *  belong to.
             *
             *  unfortunately, SiteConfiguration.getSites(SSOToken) needs
             *  an SSOToken, which can be gotten after the server is more
             *  closer to being operational than when the Agent is started,
             *  so that part will have to be updated at a later time.
             *
             *  where the key == value in sidKeys is the one that is the site
             */
            if (debug.messageEnabled()) {
                debug.message(classModule + "svrId = " + svrId
                        + ", siteId = " + siteId);
            }

            try {
                Integer.valueOf(siteId);
            } catch (NumberFormatException nfe) {
                debug.error(classModule + "invalid siteid ("
                        + siteId + "): " + nfe.getMessage(), nfe);
            }
        }
    }
}
