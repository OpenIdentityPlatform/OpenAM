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
 * $Id: Agent.java,v 1.9 2009/11/10 01:33:22 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.monitoring;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.JMRuntimeException;
import javax.management.ObjectName;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.RuntimeOperationsException;
import javax.management.remote.*;

import com.sun.jdmk.comm.AuthInfo;
import com.sun.jdmk.comm.HtmlAdaptorServer;
import com.sun.management.comm.SnmpAdaptorServer;
import com.sun.management.snmp.SnmpStatusException;

//import java.rmi.registry.Registry;
//import java.rmi.registry.LocateRegistry;
//import java.rmi.RemoteException;
//import java.rmi.server.UnicastRemoteObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.Server;
import com.iplanet.services.ldap.ServerGroup;
import com.sun.identity.cli.CLIConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;


/**
 * The Agent class provides a simple example on how to use the SNMP 
 * protocol adaptor.
 *
 * A subset of MIB II (RFC1213) is implemented. The MIB is loaded and
 * initialized. As such you can now see the MIB using your favorite
 * SNMP manager, or you can use a web browser and see the MIB through
 * the HTML adaptor.
 *
 * When calling the program, you can specify:
 *      - nb_traps: number of traps the SNMP agent will send.
 * If not specified, the agent will send traps continuously.
 *
 * In this example, the SNMP adaptor is started on port 8085, and the
 * traps are sent to the port 8086, i.e. non standard ports for SNMP.
 * As such you do not need to be root to start the agent.
 */

public class Agent {

    static SnmpAdaptorServer snmpAdaptor = null;
    static HtmlAdaptorServer htmlAdaptor = null;
    private static Debug debug;
    private static OpenSSOMonitoringUtil omu;
    
    /**
     * This variable defines the number of traps this agent has to send.
     * If not specified in the command line arguments, the traps will be 
     * sent continuously.
     */
    private static int nbTraps = -1;
    private static boolean agentStarted;
    private static boolean webtopConfig;
    private static boolean monitoringConfig;
    private static MBeanServer server;
    private static ObjectName htmlObjName;
    private static ObjectName snmpObjName;
    private static ObjectName mibObjName;
    private static ObjectName trapGeneratorObjName;
    private static int monHtmlPort;
    private static int monSnmpPort;
    private static int monRmiPort;
    private static String monAuthFilePath;
    private static String ssoProtocol;
    private static String ssoName;
    private static String ssoPort;
    private static String ssoURI;
    private static String ssoSiteID;
    private static String ssoServerID;
    private static boolean dsIsEmbedded;
    private static boolean siteIsEnabled;
    private static Hashtable siteIdTable;
    private static Hashtable serverIDTable;
    private static Hashtable namingTable;
    private static HashMap siteToURL;
    private static HashMap URLToSite;
    private static String startDate;
    private static JMXConnectorServer cs;

    static SUN_OPENSSO_SERVER_MIB mib2;
    private static SSOServerInfo agentSvrInfo;
    private static Map realm2Index = new HashMap();  // realm name to index map
    private static Map index2Realm = new HashMap();  // index to realm name map
    private static Map realm2DN = new HashMap();  // realm name to DN map
    private static Map DN2Realm = new HashMap();  // DN to realm name map
    private static Map realmAuthInst = new HashMap(); // realm|authname entries
    private static Map realmSAML2IDPs = new HashMap(); // realm|idp entries
    private static Map realmSAML2SPs = new HashMap(); // realm|sp entries

    private static boolean monitoringEnabled;
    private static boolean monHtmlPortEnabled;
    private static boolean monSnmpPortEnabled;
    private static boolean monRmiPortEnabled;
    private static boolean isSessFOEnabled;

    private static SimpleDateFormat sdf =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final int MON_CONFIG_DISABLED   =    -1;
    public static final int MON_MBEANSRVR_PROBLEM =    -2;
    public static final int MON_RMICONNECTOR_PROBLEM = -3;
    public static final int MON_CREATEMIB_PROBLEM =    -4;
    public static final int MON_READATTRS_PROBLEM =    -5;

    static final String NotAvail = "NotAvailable";
    static final String None = "NONE";


    static {
        if (debug == null) {
            debug = Debug.getInstance("amMonitoring");
        }
        omu = new OpenSSOMonitoringUtil();
    }

    /*
     * Agent constructor
     */

    private Agent() {
    }

    public static void stopRMI() {
        if (monitoringEnabled && monRmiPortEnabled && (cs != null)) {
            if ((server != null) && (mibObjName != null)) {
                try {
                    server.unregisterMBean(mibObjName);
                } catch (InstanceNotFoundException ex) {
                    if (debug.warningEnabled()) {
                        debug.warning(
                            "Agent.stopRMI: error unregistering OpenSSO:" +
                                ex.getMessage());
                    }
                } catch (MBeanRegistrationException ex) {
                    if (debug.warningEnabled()) {
                        debug.warning(
                            "Agent.stopRMI: error unregistering OpenSSO:" +
                                ex.getMessage());
                    }
                }
            }
            try {
                cs.stop();
                debug.warning("Agent.stopRMI:rmi adaptor stopped.");
            } catch (Exception ex) {
                debug.error("Agent.stopRMI: error stopping monitoring " +
                    " agent RMI server: ", ex);
            }
        } else {
            debug.warning("Agent.stopRMI: cs is null, or " +
                "monitoring or RMI port not enabled.");
        }
        if (monitoringEnabled && monSnmpPortEnabled && (snmpAdaptor != null)) {
            snmpAdaptor.stop();
            debug.warning("Agent.stopRMI:snmp adaptor stopped.");
        }
        if (monitoringEnabled && monHtmlPortEnabled && (htmlAdaptor != null)) {
            htmlAdaptor.stop();
            debug.warning("Agent.stopRMI:html adaptor stopped.");
        }
    }

    /**
     *  Receives Site and Server configuration information from
     *  WebtopNaming.  Information is saved and the corresponding
     *  Monitoring MBeans are created after the Agent ports are started.
     */
    public static int siteAndServerInfo (SSOServerInfo svrInfo) {
        agentSvrInfo = svrInfo;
        return 0;
    }

    public static int startMonitoringAgent (SSOServerInfo svrInfo) {
        agentSvrInfo = svrInfo;
        String serverID = svrInfo.serverID;
        String siteID = svrInfo.siteID;
        String serverProtocol = svrInfo.serverProtocol;
        String serverName = svrInfo.serverName;
        String serverURI = svrInfo.serverURI;
        String serverPort = svrInfo.serverPort;
        boolean isEmbeddedDS = svrInfo.isEmbeddedDS;
        Hashtable siteIDTable = svrInfo.siteIDTable;
        Hashtable serverIDTable = svrInfo.serverIDTable;
        Hashtable namingTable = svrInfo.namingTable;
        String startDate = svrInfo.startDate;

        /*
         *  ServerIDTable has form:
         *    <proto>://<host>:<port>/<uri>=nn,
         *  while NamingTable has form
         *    nn=<proto>://<host>:<port>/<uri>
         */
        if (debug.messageEnabled()) {
            StringBuilder sb =
                new StringBuilder("Agent.startMonitoringAgent:ServerInfo:\n");
            sb.append("  ServerID = ").append(serverID).append("\n").
            append("  SiteID = ").append(siteID).append("\n").
            append("  ServerProtocol = ").append(serverProtocol).
                append("\n").
            append("  ServerName = ").append(serverName).append("\n").
            append("  ServerURI = ").append(serverURI).append("\n").
            append("  IsEmbeddedDS = ").append(isEmbeddedDS).append("\n").
            append("\n");

            /*
             *  this server's siteID is "siteID"
             */
            Hashtable tht = siteIDTable;
            Set keySet = tht.keySet();
            sb.append("  SiteID Table:\n");
            for (Iterator it = keySet.iterator(); it.hasNext(); ) {
                String key = (String)it.next();
                String value = (String)tht.get(key);
                sb.append("    key = ").append(key).
                    append(", value = ").append(value).append("\n");
            }

            /*
             *  can get this server's URL from the naming table, using
             *  its serverID.  get the site's URL with siteID
             */
            String svrURL = (String)namingTable.get(serverID);
            sb.append("  Naming table entry for serverID ").
                append(serverID).append(" is ");
            if ((svrURL != null) && (svrURL.length() > 0)) {
                sb.append(svrURL).append("\n");
            } else {
                sb.append("NULL!\n");
            }
            svrURL = (String)namingTable.get(siteID);
            sb.append("  Naming table entry for siteID ").
                append(siteID).append(" is ");
            if ((svrURL != null) && (svrURL.length() > 0)) {
                sb.append(svrURL).append("\n");
            } else {
                sb.append("NULL!\n");
            }
            debug.message(sb.toString());
        }


        return startMonitoringAgent (serverID, serverProtocol, serverName,
            serverPort, serverURI, siteID, isEmbeddedDS,
            siteIDTable, serverIDTable, namingTable, startDate);
    }

    /*
     *  This method starts up the monitoring agent.  Returns either
     *  zero (0) if intialization has completed successfully, or one (1)
     *  if not.
     *  @param OpenSSOServerID The OpenSSO server's ID in the site
     *  @param svrProtocol OpenSSO server's protocol (http/https)
     *  @param svrName OpenSSO server's hostname
     *  @param svrPort OpenSSO server's port
     *  @param svrURI OpenSSO server's URI
     *  @param siteID OpenSSO server's Site ID
     *  @param serverID OpenSSO server's ID
     *  @param isEmbeddedDS Whether the OpenSSO server is using an embedded DS
     *  @param siteIdTbl the Site ID table for this installation
     *  @param serverIdTbl the Server ID table for this installation
     *  @param namingTbl the Naming table for this installation
     *  @param stDate start date/time for this OpenSSO server
     *  @return Success (0) or Failure (1)
     */

    public static int startMonitoringAgent (
        String openSSOServerID,
        String svrProtocol,
        String svrName,
        String svrPort,
        String svrURI,
        String siteID,
        boolean isEmbeddedDS,
        Hashtable siteIdTbl,
        Hashtable serverIdTbl,
        Hashtable namingTbl,
        String stDate)
    {
        String classMethod = "Agent.startMonitoringAgent:";

        ssoServerID = openSSOServerID;
        ssoProtocol = svrProtocol;
        ssoName = svrName;
        ssoPort = svrPort;
        ssoURI = svrURI;
        ssoSiteID = siteID;
        dsIsEmbedded = isEmbeddedDS;
        siteIsEnabled = !ssoServerID.equals(siteID);
        siteIdTable = siteIdTbl;
        serverIDTable = serverIdTbl;
        namingTable = namingTbl;
        startDate = stDate;

        if (debug.messageEnabled()) {
            String svrURL = (String)namingTable.get(ssoServerID);
            StringBuilder sb2 =
                new StringBuilder( "    Naming table entry for serverID ");
            sb2.append(ssoServerID).append(": ");
            if ((svrURL != null) && (svrURL.length() > 0)) {
                sb2.append(svrURL).append("\n");
            } else {
                sb2.append("NULL!\n");
            }
            svrURL = (String)namingTable.get(siteID);
            sb2.append("    Naming table entry for siteID ").
                append(siteID).append(": ");
            if ((svrURL != null) && (svrURL.length() > 0)) {
                sb2.append(svrURL).append("\n");
            } else {
                sb2.append("NULL!\n");
            }

            debug.message(classMethod + "parameters are:\n" +
                "    serverID = " + openSSOServerID + "\n" +
                "    protocol = " + svrProtocol + "\n" +
                "    svrName = " + svrName + "\n" +
                "    port = " + svrPort + "\n" +
                "    siteID = " + siteID + "\n" +
                "    isEmbeddedDS = " + isEmbeddedDS + "\n" +
                "    siteIsEnabled = " + siteIsEnabled + "\n" +
                sb2.toString() + "\n" +
                "    start date/time = " + stDate);
        }

        /*
         *  start date/time, stDate, format is "YYYY-MM-DD HH:MM:SS"
         *  SsoServerStartDate should be an 8-Byte array, where
         *    bytes 0-1: year
         *    byte    2: month
         *    byte    3: day
         *    byte    4: hours
         *    byte    5: minutes
         *    byte    6: seconds
         *    byte    7: deci-seconds (will be 0)
         *
         *  however, need to wait for the SUN_OPENSSO_SERVER_MIB
         *  instantiation, so that there's an SsoServerInstanceImpl
         *  instance around to update.
         *
         *  or could have the SsoServerInstanceImpl.init() do it...
         */

        /*
         * if there's a site configured, then siteIdTable will contain
         * the serverIDs
         */

        if (debug.messageEnabled()) {
            if ((siteIdTable != null) && !siteIdTable.isEmpty()) {
                debug.message(classMethod + "siteIdTable -> " +
                    siteIdTable.toString());

                Set ks = siteIdTable.keySet();
                StringBuffer sb = new StringBuffer("Site ID Table:\n");
                for (Iterator it = ks.iterator(); it.hasNext(); ) {
                    String siteid = (String)it.next();
                    String svrid = (String)siteIdTable.get(siteid);
                    String sURL = (String)namingTable.get(siteid);
                    sb.append("  ").append(siteid).append("(").
                        append(sURL).append(")").append(" = ").
                        append(svrid).append("\n");
                }
                debug.message(classMethod + sb.toString());
            } else {
                debug.message (classMethod + "siteIdTable is null or empty");
            }

            /*
             *  print out the serverIDTable
             */
            if ((serverIDTable != null) && !serverIDTable.isEmpty()) {
                Set ks = serverIDTable.keySet();
                StringBuilder sb = new StringBuilder("Server ID Table:\n");
                for (Iterator it = ks.iterator(); it.hasNext(); ) {
                    String svr = (String)it.next();
                    String svrid = (String)serverIDTable.get(svr);
                    sb.append("  server ").append(svr).append(" ==> svrid ").
                        append(svrid).append("\n");
                }
                debug.message (classMethod + sb.toString());
            } else {
                debug.message (classMethod + "ServerIdTable is null or empty");
            }

            /*
             *  print out the namingTable
             */
            if ((namingTable != null) && !namingTable.isEmpty()) {
                Set ks = namingTable.keySet();
                StringBuilder sb = new StringBuilder("Naming Table:\n");
                for (Iterator it = ks.iterator(); it.hasNext(); ) {
                    String svr = (String)it.next();
                    String svrid = (String)namingTable.get(svr);
                    sb.append("  key ").append(svr).append(" ==> value ").
                        append(svrid).append("\n");
                }
                debug.message (classMethod + sb.toString());
            } else {
                debug.message (classMethod + "NamingTable is null or empty");
            }
        }

        return (0);
    }


    /**
     *  This method starts up the monitoring agent from the
     *  common/ConfigMonitoring module (load-on-startup or at the
     *  end of AMSetupServlet/configuration).  Since web-app startup
     *  is sensitive to exceptions in load-on-startup stuff, this has
     *  quite a few try/catch blocks.
     * 
     *  If any of HTML, SNMP, or RMI adaptors has a problem getting created
     *  or started, attempts to create/start the others will be made; If
     *  at least one adaptor is started, monitoring will be "active"
     *  (Agent.isRunning() will return true).
     *
     *  @param monConfig SSOServerMonConfig structure of OpenSSO configuration
     *  @return 0 (zero) if at least one of HTML/SNMP/RMI adaptors started up;
     *     MON_CONFIG_DISABLED:
     *       if monitoring configured as disabled
     *     MON_MBEANSRVR_PROBLEM:
     *       if MBeanServer problem encountered
     *     MON_RMICONNECTOR_PROBLEM:
     *       if RMI connector problem
     *             (MIB not registered with MBeanServer)
     *     MON_CREATEMIB_PROBLEM:
     *       if problem creating/registering MIB
     */
    public static int startAgent (SSOServerMonConfig monConfig) {
        monHtmlPort = monConfig.htmlPort;
        monSnmpPort = monConfig.snmpPort;
        monRmiPort = monConfig.rmiPort;
        monitoringEnabled = monConfig.monitoringEnabled;
        monHtmlPortEnabled = monConfig.monHtmlPortEnabled;
        monSnmpPortEnabled = monConfig.monSnmpPortEnabled;
        monRmiPortEnabled = monConfig.monRmiPortEnabled;
        monAuthFilePath = monConfig.monAuthFilePath;
        String classMethod = "Agent.startAgent:";
        // OpenSSO server port comes from WebtopNaming.siteAndServerInfo
        String serverPort = agentSvrInfo.serverPort;

        /*
         *  there are a lot of exception checks in this method, as
         *  it's invoked from a load-on-startup servlet.  if it
         *  chokes in here, OpenSSO won't start up.
         */
        if (debug.messageEnabled()) {
            debug.message(classMethod + "entry:\n" +
                "    htmlPort = " + monHtmlPort + "\n" +
                "    authFilePath = " + monAuthFilePath + "\n" +
                "    snmpPort = " + monSnmpPort + "\n" +
                "    rmiPort = " + monRmiPort + "\n" +
                "    monEna = " + monitoringEnabled + "\n" +
                "    htmlEna = " + monHtmlPortEnabled + "\n" +
                "    snmpEna = " + monSnmpPortEnabled + "\n" +
                "    rmiEna = " + monRmiPortEnabled + "\n" +
                "    serverPort = " + serverPort + "\n"
                );
        }

        if (!monitoringEnabled) {
            debug.warning(classMethod + "Monitoring configured as disabled.");
            return MON_CONFIG_DISABLED;
        }

        /*
         *  verify that the HTML, SNMP and RMI ports aren't the same as
         *  the OpenSSO server port.  if HTML or SNMP conflict with it,
         *  then they'll be disabled (warning message).  if the RMI port
         *  conflicts, then all of monitoring is disabled.  there might
         *  be other ports that should be checked.
         */
        try {
            int sport = Integer.parseInt(serverPort);

            if (monRmiPort == sport) {
                debug.error(classMethod +
                    "RMI port conflicts with OpenSSO server port (" +
                    sport + "); Monitoring disabled.");
                return MON_RMICONNECTOR_PROBLEM;
            }
            if (monHtmlPort == sport) {
                monHtmlPortEnabled = false;
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "HTML port conflicts with OpenSSO server port (" +
                        sport + "); Monitoring HTML port disabled.");
                }
            }
            if (monSnmpPort == sport) {
                monSnmpPortEnabled = false;
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "SNMP port conflicts with OpenSSO server port (" +
                        sport + "); Monitoring SNMP port disabled.");
                }
            }
        } catch (NumberFormatException nfe) {
            /*
             * odd.  if serverPort's not a valid int, then there'll be
             * other problems
             */
            debug.error(classMethod + "Server port (" + serverPort + 
            " is invalid: " + nfe.getMessage());
        }

        if (debug.messageEnabled()) {
            debug.message(classMethod + "config:\n" +
                "    monitoring Enabled = " + monitoringEnabled + "\n" +
                "    HTML Port = " + monHtmlPort +
                ", enabled = " + monHtmlPortEnabled + "\n" +
                "    SNMP Port = " + monSnmpPort +
                ", enabled = " + monSnmpPortEnabled + "\n" +
                "    RMI Port = " + monRmiPort +
                ", enabled = " + monRmiPortEnabled + "\n");
        }

        /*
         *  if OpenSSO's deployed on a container that has MBeanServer(s),
         *  will the findMBeanServer(null) "find" those?  if so,
         *  is using the first one the right thing to do?
         */
        ArrayList servers = null;
        try {
            servers = MBeanServerFactory.findMBeanServer(null);
        } catch (SecurityException ex) {
            /*
             * if can't find one, try creating one below, although
             * if there's no findMBeanServer permission, it's unlikely
             * that there's a createMBeanServer permission...
             */
            if (debug.warningEnabled()) {
                debug.warning(classMethod +
                    "findMBeanServer permission error: " + ex.getMessage());
            }
        }

        if (debug.messageEnabled()) {
            debug.message(classMethod + "MBeanServer list is not empty: " +
                ((servers != null) && !servers.isEmpty()));
        }

        if ((servers != null) && !servers.isEmpty()) {
            server = (MBeanServer)servers.get(0);
        } else {
            try {
                server = MBeanServerFactory.createMBeanServer();
            } catch (SecurityException ex) {
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "createMBeanServer permission error: " +
                        ex.getMessage());
                }
                return MON_MBEANSRVR_PROBLEM;
            } catch (JMRuntimeException ex) {
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "createMBeanServer JMRuntime error: " +
                        ex.getMessage());
                }
                return MON_MBEANSRVR_PROBLEM;
            } catch (ClassCastException ex) {
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "createMBeanServer ClassCast error: " +
                        ex.getMessage());
                }
                return MON_MBEANSRVR_PROBLEM;
            }
        }
        if (server == null) {
            if (debug.warningEnabled()) {
                debug.warning(classMethod + "no MBeanServer");
            }
            return MON_MBEANSRVR_PROBLEM;
        }

        String domain = server.getDefaultDomain();  // throws no exception


        // Create the MIB II (RFC 1213), add to the MBean server.
        try {
            mibObjName =
                new ObjectName("snmp:class=SUN_OPENSSO_SERVER_MIB");
            if (debug.messageEnabled()) {
                debug.message(classMethod +
                    "Adding SUN_OPENSSO_SERVER_MIB to MBean server " +
                    "with name '" + mibObjName + "'");
            }
        } catch (MalformedObjectNameException ex) {
            // from ObjectName
            if (debug.warningEnabled()) {
                debug.warning(classMethod +
                    "Error getting ObjectName for the MIB: " +
                    ex.getMessage());
            }
            return MON_CREATEMIB_PROBLEM;
        }

        // Create an instance of the customized MIB
        try {
            mib2 = new SUN_OPENSSO_SERVER_MIB();
        } catch (RuntimeException ex) {
            debug.error (classMethod + "Runtime error instantiating MIB", ex);
            return MON_CREATEMIB_PROBLEM;
        } catch (Exception ex) {
            debug.error (classMethod + "Error instantiating MIB", ex);
            return MON_CREATEMIB_PROBLEM;
        }

        try {
            server.registerMBean(mib2, mibObjName);
        } catch (RuntimeOperationsException ex) {
            // from registerMBean
            if (debug.warningEnabled()) {
                debug.warning(classMethod +
                    "Null parameter or no object name for MIB specified: " +
                    ex.getMessage());
            }
            return MON_CREATEMIB_PROBLEM;
        } catch (InstanceAlreadyExistsException ex) {
            // from registerMBean
            if (debug.warningEnabled()) {
                debug.warning(classMethod +
                    "Error registering MIB MBean: " +
                    ex.getMessage());
            }
            // probably can just continue
        } catch (MBeanRegistrationException ex) {
            // from registerMBean
            if (debug.warningEnabled()) {
                debug.warning(classMethod +
                    "Error registering MIB MBean: " +
                    ex.getMessage());
            }
            return MON_CREATEMIB_PROBLEM;
        } catch (NotCompliantMBeanException ex) {
            // from registerMBean
            if (debug.warningEnabled()) {
                debug.warning(classMethod +
                    "Error registering MIB MBean: " +
                    ex.getMessage());
            }
            return MON_CREATEMIB_PROBLEM;
        }

        /*
         *  now that we have the MBeanServer, see if the HTML,
         *  SNMP and RMI adaptors specified will start up
         */
        boolean monHTMLStarted = false;
        boolean monSNMPStarted = false;
        boolean monRMIStarted = false;
        // HTML port adaptor
        if (monHtmlPortEnabled) {
            // Create and start the HTML adaptor.
            try {
                htmlObjName = new ObjectName(domain +
                    ":class=HtmlAdaptorServer,protocol=html,port=" +
                    monHtmlPort);

                if (debug.messageEnabled()) {
                    debug.message(classMethod +
                        "Adding HTML adaptor to MBean server with name '" +
                        htmlObjName + "'\n    " +
                        "HTML adaptor is bound on TCP port " + monHtmlPort);
                }

                String[][] users = omu.getMonAuthList(monAuthFilePath);
                if (users != null) {
                    int sz = Array.getLength(users);
                    AuthInfo authInfo[] = new AuthInfo[sz];
                    for (int i = 0; i < sz; i++) {
                        authInfo[i] = new AuthInfo(users[i][0], users[i][1]);
                    }
                    htmlAdaptor = new HtmlAdaptorServer(monHtmlPort, authInfo);
                } else {
                    if (debug.warningEnabled()) {
                        debug.warning(classMethod +
                            "HTML monitoring interface disabled; no " +
                            "authentication file found");
                    }
                    htmlAdaptor = null;
                }

                if (htmlAdaptor == null) {
                    if (debug.warningEnabled()) {
                        debug.warning(classMethod + "HTTP port " +
                            monHtmlPort + " unavailable or invalid. " +
                            "Monitoring HTML adaptor not started.");
                    }
                } else {
                    server.registerMBean(htmlAdaptor, htmlObjName);
                    htmlAdaptor.start();  // throws no exception
                    monHTMLStarted = true;
                }
            } catch (MalformedObjectNameException ex) {
                // from ObjectName
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "Error getting ObjectName for HTML adaptor: " +
                        ex.getMessage());
                }
            } catch (NullPointerException ex) {
                // from ObjectName
                debug.error(classMethod +
                        "NPE getting ObjectName for HTML adaptor", ex);

                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "NPE getting ObjectName for HTML adaptor: " +
                        ex.getMessage());
                }
            } catch (InstanceAlreadyExistsException ex) {
                // from registerMBean
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "Error registering HTML adaptor MBean: " +
                        ex.getMessage());
                }
            } catch (MBeanRegistrationException ex) {
                // from registerMBean
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "Error registering HTML adaptor MBean: " +
                        ex.getMessage());
                }
            } catch (NotCompliantMBeanException ex) {
                // from registerMBean
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "Error registering HTML adaptor MBean: " +
                        ex.getMessage());
                }
            }
        } else {
            debug.warning(classMethod +
                "Monitoring HTML port not enabled in configuration.");
        }

        // SNMP port adaptor
        if (monSnmpPortEnabled) {
            // SNMP specific code:
            /*
             * Create and start the SNMP adaptor.
             * Specify the port to use in the constructor. 
             * The standard port for SNMP is 161.
             */
            try {
                snmpObjName = new ObjectName(domain + 
                    ":class=SnmpAdaptorServer,protocol=snmp,port=" +
                    monSnmpPort);

                if (debug.messageEnabled()) {
                    debug.message(classMethod +
                        "Adding SNMP adaptor to MBean server with name '" +
                        snmpObjName + "'\n    " +
                        "SNMP Adaptor is bound on UDP port " + monSnmpPort);
                }

                snmpAdaptor = new SnmpAdaptorServer(monSnmpPort); // no exc
                if (snmpAdaptor == null) {
                    if (debug.warningEnabled()) {
                        debug.warning(classMethod +
                            "Unable to get SNMP adaptor.");
                    }
                } else {
                    server.registerMBean(snmpAdaptor, snmpObjName);
                    snmpAdaptor.start();  // throws no exception

                    /*
                     *  Send a coldStart SNMP Trap.
                     *  Use port = monSnmpPort+1.
                     */
                    if (debug.messageEnabled()) {
                        debug.message(classMethod +
                            "Sending a coldStart SNMP trap to each " +
                            "destination defined in the ACL file...");
                    }

                    snmpAdaptor.setTrapPort(new Integer(monSnmpPort+1));
                    snmpAdaptor.snmpV1Trap(0, 0, null);

                    if (debug.messageEnabled()) {
                        debug.message(classMethod + "Done sending coldStart.");
                    }
 
                    /*
                     *  Bind the SNMP adaptor to the MIB in order to make the
                     *  MIB accessible through the SNMP protocol adaptor.
                     *  If this step is not performed, the MIB will still live
                     *  in the Java DMK agent:
                     *  its objects will be addressable through HTML but not
                     *  SNMP.
                     */
                    mib2.setSnmpAdaptor(snmpAdaptor);  // throws no exception

                    monSNMPStarted = true;
                }
            } catch (MalformedObjectNameException ex) {
                // from ObjectName
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "Error getting ObjectName for SNMP adaptor: " +
                        ex.getMessage());
                }
            } catch (NullPointerException ex) {
                // from ObjectName
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "NPE getting ObjectName for SNMP adaptor: " +
                        ex.getMessage());
                }
            } catch (InstanceAlreadyExistsException ex) {
                // from registerMBean
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "Error registering SNMP adaptor MBean: " +
                        ex.getMessage());
                }
            } catch (MBeanRegistrationException ex) {
                // from registerMBean
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "Error registering SNMP adaptor MBean: " +
                        ex.getMessage());
                }
            } catch (NotCompliantMBeanException ex) {
                // from registerMBean
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "Error registering SNMP adaptor MBean: " +
                        ex.getMessage());
                }
            } catch (IOException ex) {
                /*
                 * should be from the snmpV1Trap call, which
                 * *shouldn't* affect the rest of snmp operations...
                 */
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "IO Error from SNMP snmpV1Trap: " +
                        ex.getMessage());
                }
                monSNMPStarted = true;
            } catch (SnmpStatusException ex) {
                /*
                 * also should be from the snmpV1Trap call, which
                 * *shouldn't* affect the rest of snmp operations...
                 */
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "Status error from SNMP snmpV1Trap: " +
                        ex.getMessage());
                }
                monSNMPStarted = true;
            }
        } else {
            debug.warning(classMethod +
                "Monitoring SNMP port not enabled.");
        }

//        /*
//         *  adding the create registry for "our" port?
//         */
//        try {
//            XXX xyz = new XXX();
//            xxx stub = (XXX)UnicastRemoteObject.exportObject(xyz,
//                monRmiPort);
//            Registry registry = LocateRegistry.getRegistry();
//            registry.bind("what?", stub);
//        } catch (Exception e) {
//        }

        // RMI port adaptor
        if (monRmiPortEnabled) {
            // Create an RMI connector and start it
            try {
                JMXServiceURL url = new JMXServiceURL(
                    "service:jmx:rmi:///jndi/rmi://localhost:" +
                    monRmiPort + "/server");
                cs = JMXConnectorServerFactory.newJMXConnectorServer(
                    url, null, server);
                cs.start();

                monRMIStarted = true;
//                /*
//                 *  Create a LinkTrapGenerator.
//                 *  Specify the ifIndex to use in the object name.
//                 */
//                String trapGeneratorClass = "LinkTrapGenerator";
//                int ifIndex = 1;
//                trapGeneratorObjName = new ObjectName("trapGenerator" + 
//                    ":class=LinkTrapGenerator,ifIndex=" + ifIndex);
//                if (debug.messageEnabled()) {
//                    debug.message(classMethod +
//                        "Adding LinkTrapGenerator to MBean server " +
//                        "with name '" +
//                        trapGeneratorObjName + "'");
//                }
//
//                LinkTrapGenerator trapGenerator =
//                    new LinkTrapGenerator(nbTraps);
//                server.registerMBean(trapGenerator, trapGeneratorObjName);
//
            } catch (MalformedURLException ex) {
                /*
                 * from JMXServiceURL or
                 * JMXConnectorServerFactory.JMXConnectorServer
                 */
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "Error getting JMXServiceURL or JMXConnectorServer " +
                        "for RMI adaptor: " + ex.getMessage());
                }
            } catch (NullPointerException ex) {
                /*
                 * from JMXServiceURL or
                 * JMXConnectorServerFactory.JMXConnectorServer
                 */
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "Error getting JMXServiceURL or JMXConnectorServer " +
                        "for RMI adaptor: " + ex.getMessage());
                }
            } catch (IOException ex) {
                /*
                 * from JMXConnectorServerFactory.JMXConnectorServer or
                 * JMXConnectorServer.start
                 */
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "Error getting JMXConnectorServer for, or starting " +
                        "RMI adaptor: " + ex.getMessage());
                }
            } catch (IllegalStateException ex) {
                // from JMXConnectorServer.start
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "Illegal State Error from JMXConnectorServer for " +
                        "RMI adaptor: " + ex.getMessage());
                }
            } catch (Exception ex) {
                /*
                 * compiler says that JMXProviderException and
                 * NullPointerException already caught
                 */
                debug.error(classMethod +
                    "Error starting RMI: executing rmiregistry " +
                    monRmiPort + ".", ex);
            }
        } else {
            debug.warning(classMethod + "Monitoring RMI port not enabled.");
        }

        /*
         * the HTML and SNMP adaptors may or may not be started,
         * but if the RMI connector had a problem, monitoring is
         * non-functional, as the opensso MIB didn't get registered.
         */
        if (!monRMIStarted && !monSNMPStarted && !monHTMLStarted) {
            debug.warning(classMethod +
                "No Monitoring interfaces started; monitoring disabled.");
            return MON_RMICONNECTOR_PROBLEM;
        } else {
            agentStarted = true;  // if all/enough has gone well
            startMonitoringAgent(agentSvrInfo);
            return (0);
        }
    }

    public static void setWebtopConfig(boolean state) {
        webtopConfig = state;
    }

    public static void setMonitoringConfig(boolean state) {
        monitoringConfig = state;
    }

    /**
     *  Return whether agent is "running" or not
     */
    public static boolean isRunning() {
        return agentStarted;
    }

    /*
     *  Return the pointer to the authentication service mbean
     */
    public static Object getAuthSvcMBean() {
        if (mib2 != null) {
            return mib2.getAuthSvcGroup();
        } else {
            return null;
        }
    }

    /*
     *  Return the pointer to the session service mbean
     */
    public static Object getSessSvcMBean() {
        if (mib2 != null) {
            return mib2.getSessSvcGroup();
        } else {
            return null;
        }
    }

    /*
     *  Return the pointer to the logging service mbean
     */
    public static Object getLoggingSvcMBean() {
        if (mib2 != null) {
            return mib2.getLoggingSvcGroup();
        } else {
            return null;
        }
    }

    /*
     *  Return the pointer to the policy service mbean
     */
    public static Object getPolicySvcMBean() {
        if (mib2 != null) {
            return mib2.getPolicySvcGroup();
        } else {
            return null;
        }
    }

    /*
     *  Return the pointer to the IdRepo service mbean
     */
    public static Object getIdrepoSvcMBean() {
        if (mib2 != null) {
            return mib2.getIdrepoSvcGroup();
        } else {
            return null;
        }
    }

    /*
     *  Return the pointer to the service service mbean
     */
    public static Object getSmSvcMBean() {
        if (mib2 != null) {
            return mib2.getSmSvcGroup();
        } else {
            return null;
        }
    }

    /*
     *  Return the pointer to the SAML1 service mbean
     */
    public static Object getSaml1SvcMBean() {
        if (mib2 != null) {
            return mib2.getSaml1SvcGroup();
        } else {
            return null;
        }
    }

    /*
     *  Return the pointer to the SAML2 service mbean
     */
    public static Object getSaml2SvcMBean() {
        if (mib2 != null) {
            return mib2.getSaml2SvcGroup();
        } else {
            return null;
        }
    }

    /*
     *  Return the pointer to the IDFF service mbean
     */
    public static Object getIdffSvcMBean() {
        if (mib2 != null) {
            return mib2.getIdffSvcGroup();
        } else {
            return null;
        }
    }

    /*
     *  Return the pointer to the Topology mbean
     */
    public static Object getTopologyMBean() {
        if (mib2 != null) {
            return mib2.getTopologyGroup();
        } else {
            return null;
        }
    }

    /*
     *  Return the pointer to the Server Instance mbean
     */
    public static Object getSvrInstanceMBean() {
        if (mib2 != null) {
            return mib2.getSvrInstanceGroup();
        } else {
            return null;
        }
    }

    /*
     *  Return the pointer to the Fed COTs mbean
     */
    public static Object getFedCOTsMBean() {
        if (mib2 != null) {
            return mib2.getFedCotsGroup();
        } else {
            return null;
        }
    }

    /*
     *  Return the pointer to the Federation Entities mbean
     */
    public static Object getFedEntsMBean() {
        if (mib2 != null) {
            return mib2.getFedEntitiesGroup();
        } else {
            return null;
        }
    }

    /*
     *  Return the pointer to the SAML2 Service mbean
     */
    public static Object getSAML2SvcGroup() {
        if (mib2 != null) {
            return mib2.getSaml2SvcGroup();
        } else {
            return null;
        }
    }

    /*
     *  Return the pointer to the Entitlements Service mbean
     */
    public static Object getEntitlementsGroup() {
        if (mib2 != null) {
            return mib2.getEntitlementsGroup();
        } else {
            return null;
        }
    }

    public static String getSsoProtocol() {
        if (agentSvrInfo != null) {
            return (agentSvrInfo.serverProtocol);
        } else {
            return null;
        }
    }
    public static String getSsoName() {
        if (agentSvrInfo != null) {
            return (agentSvrInfo.serverName);
        } else {
            return null;
        }
    }
    public static String getSsoPort() {
        if (agentSvrInfo != null) {
            return (agentSvrInfo.serverPort);
        } else {
            return null;
        }
    }
    public static String getSsoURI() {
        if (agentSvrInfo != null) {
            return (agentSvrInfo.serverURI);
        } else {
            return null;
        }
    }
    public static String getSsoSvrID() {
        if (agentSvrInfo != null) {
            return (agentSvrInfo.serverID);
        } else {
            return null;
        }
    }

    public static Hashtable getSiteIdTable() {
        if (agentSvrInfo != null) {
            return (agentSvrInfo.siteIDTable);
        } else {
            return null;
        }
    }

    public static Hashtable getServerIdTable() {
        if (agentSvrInfo != null) {
            return (agentSvrInfo.serverIDTable);
        } else {
            return null;
        }
    }

    public static Hashtable getNamingTable() {
        if (agentSvrInfo != null) {
            return (agentSvrInfo.namingTable);
        } else {
            return null;
        }
    }

    public static HashMap getSiteToURLTable() {
        return (siteToURL);
    }

    public static HashMap getURLToSiteTable() {
        return (URLToSite);
    }

    public static boolean getDsIsEmbedded() {
        return (dsIsEmbedded);
    }

    public static String getStartDate() {
        return (startDate);
    }

    public static String getSiteId() {
        if (agentSvrInfo != null) {
            return (agentSvrInfo.siteID);
        } else {
            return null;
        }
    }

    public static boolean getWebtopConfig() {
        return (webtopConfig);
    }

    public static boolean getMonitoringConfig() {
        return (monitoringConfig);
    }

    /*
     *  receive Set of site names
     *
     *  sNames... site name -> primary URL
     *  urlSites is opposite... primary URL -> site name
     */
    public static void siteNames (HashMap sNames, HashMap urlSites) {
        String classMethod = "Agent.siteNames:";
        if (sNames.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message(classMethod + "no sites");
            }
            return;
        }
        Date startDate = new Date();
        siteToURL = sNames;
        URLToSite = urlSites;

        if (debug.messageEnabled()) {
            Set kset = sNames.keySet();
            StringBuilder sb = new StringBuilder("Site Names and URLs:\n");
            for (Iterator it = kset.iterator(); it.hasNext(); ) {
                String stName = (String)it.next();
                String stURL = (String)sNames.get(stName);

                sb.append("    siteName = ").append(stName).
                    append(", primary URL = ").append(stURL).append("\n");
            }
            debug.message(classMethod + sb.toString());
        }

        /*
         *  with the urlSites map (url => sitename), can do the
         *  SsoServerSitesEntryImpl entries
         *  where the key==value in siteIdTable is a site
         *
         *  where the key!=value, then do the sitemap entries
         */
        Set sidKeys = siteIdTable.keySet();
        int i = 1;
        for (Iterator it = sidKeys.iterator(); it.hasNext(); ) {
            String svrId = (String)it.next();
            String siteId = (String)siteIdTable.get(svrId);
            String svrURL = (String)namingTable.get(siteId);
            String siteName = (String)urlSites.get(svrURL);
            String escSiteName = getEscapedString(siteName);
            SsoServerTopologyImpl tg =
                (SsoServerTopologyImpl)mib2.getTopologyGroup();
            if (siteId.equals(svrId)) { // is a site
                SsoServerSitesEntryImpl ssse =
                    new SsoServerSitesEntryImpl(mib2);
                Integer sid = new Integer(0);
                try {
                    sid = new Integer(siteId);
                } catch (NumberFormatException nfe) {
                    debug.error(classMethod + "invalid siteid (" +
                        siteId + "): " + nfe.getMessage(), nfe);
                }
                ssse.SiteId = sid;
                ssse.SiteName = escSiteName;

                if (debug.messageEnabled()) {
                    debug.message(classMethod + "doing siteName " + siteName +
                        ", svrURL = " + svrURL);
                }

                final ObjectName stName =
                    ssse.createSsoServerSitesEntryObjectName(server);
                if (stName == null) {
                    debug.error(classMethod +
                        "Error creating object for siteName '" + siteName +
                        "'");
                    continue;
                }
                try {
                    TableSsoServerSitesTable stTbl =
                        tg.accessSsoServerSitesTable();
                    stTbl.addEntry(ssse, stName);
                    if ((server != null) && (stName != null)) {
                        server.registerMBean(ssse, stName);
                    }
                } catch (Exception ex) {
                    debug.error(classMethod + siteId, ex);
                }
            } else { // is a server
                SsoServerSiteMapEntryImpl ssse =
                    new SsoServerSiteMapEntryImpl(mib2);
                ssse.MapServerURL = (String)namingTable.get(svrId);
                ssse.MapSiteName = escSiteName;
                ssse.MapId = siteId;
                try {
                    ssse.SiteMapId = new Integer(svrId);
                } catch (NumberFormatException nfe) {
                    debug.error(classMethod + "invalid serverID (" +
                        svrId + "): " + nfe.getMessage(), nfe);
                    continue;
                }
                ssse.SiteMapIndex = new Integer(i++);
                final ObjectName smName =
                    ssse.createSsoServerSiteMapEntryObjectName(server);

                if (smName == null) {
                    debug.error(classMethod +
                        "Error creating object for server siteName '" +
                        siteName + "'");
                    continue;
                }

                if (debug.messageEnabled()) {
                    debug.message(classMethod +
                        "doing servermap entry; sitemapid = " + svrId +
                        ", mapid = " + siteId + ", siteName = " + siteName);
                }

                try {
                    TableSsoServerSiteMapTable stTbl =
                        tg.accessSsoServerSiteMapTable();
                    stTbl.addEntry(ssse, smName);
                    if ((server != null) && (smName != null)) {
                        server.registerMBean(ssse, smName);
                    }
                } catch (Exception ex) {
                    debug.error(classMethod + siteId + "/" + svrId, ex);
                }
            }
        }
        Date stopDate = new Date();
        if (debug.messageEnabled()) {
            String stDate = sdf.format(startDate);
            String endDate = sdf.format(stopDate);
            debug.message("Agent.siteNames:\n    Start Time = " +
                stDate + "\n      End Time = " + endDate);
        }
    }
 
    /*
     *  receive ordered list of realms
     */
    public static int realmsConfig (ArrayList realmList) {
        String classMethod = "Agent.realmsConfig:";

        /*
         *  no realm "service", so have to create the
         *  realm table here.
         */
        Date startDate = new Date();
        StringBuilder sb =
            new StringBuilder("receiving list of realms (size = ");
        sb.append(realmList.size()).append("):\n");
        SsoServerInstanceImpl sig =
            (SsoServerInstanceImpl)mib2.getSvrInstanceGroup();
        TableSsoServerRealmTable rtab = null;
        if (sig != null) {
            try {
                rtab = sig.accessSsoServerRealmTable();
            } catch (SnmpStatusException ex) {
                debug.error(classMethod + "getting realm table: ", ex);
                return (-1);
            } 
        }
        int realmsAdded = 0;
        for (int i = 0; i < realmList.size(); i++) {
            String ss = (String)realmList.get(i);
            SsoServerRealmEntryImpl rei = new SsoServerRealmEntryImpl(mib2);
            rei.SsoServerRealmIndex = new Integer(i+1);
            String ss2 = ss;
            ss2 = getEscapedString(ss2);
            rei.SsoServerRealmName = ss2;
            ObjectName oname = rei.createSsoServerRealmEntryObjectName(server);

            if (oname == null) {
                debug.error(classMethod + "Error creating object for realm '" +
                   ss + "'");
                continue;
            }

            String rlmToDN = DNMapper.orgNameToDN(ss);

            sb.append("  realm #").append(i).append(" = ").append(ss).
                append(", DN = ").append(rlmToDN).append("\n");
            /*
             * each realm gets a realm-to-index, index-to-realm,
             * realm-to-DN and DN-to-realm map entry
             */
            try {
                rtab.addEntry(rei, oname);
                if ((server != null) && (rei != null)) {
                    server.registerMBean(rei, oname);
                }
                realm2Index.put(ss, rei.SsoServerRealmIndex);
                index2Realm.put(rei.SsoServerRealmIndex, ss);
                realm2DN.put(ss, rlmToDN);
                DN2Realm.put(rlmToDN, ss);
            } catch (JMException ex) {
                debug.error(classMethod + ss, ex);
            } catch (SnmpStatusException ex) {
                debug.error(classMethod + ss, ex);
            }
            realmsAdded++;
        }

        /*
         * could have used TableSsoServerRealmTable.getEntries(),
         * but that's a little more complicated than just counting
         * entries as they're successfully added here.
         */
        if (realmsAdded == 0) {
            debug.error(classMethod + "No realms processed successfully.");
            return -2;
        }

        if (debug.messageEnabled()) {
            debug.message (classMethod + sb.toString());
        }

        /*
         * create the Entitlements MBeans for this realm as specified by Ii.
         * the Network Monitors are not per-real.  the set list is in
         * OpenSSOMonitoringUtil.java (getNetworkMonitorNames()).
         * the Policy Stats are realm-based.
         */
        String[] nms = omu.getNetworkMonitorNames();

        if ((nms != null) && (nms.length > 0)) {
            SsoServerEntitlementSvc esi =
                (SsoServerEntitlementSvc)mib2.getEntitlementsGroup();
            if (esi != null) {
                try {
                    TableSsoServerEntitlementExecStatsTable etab =
                        esi.accessSsoServerEntitlementExecStatsTable();

                    for (int i = 0; i < nms.length; i++) {
                        String str = nms[i];
                        SsoServerEntitlementExecStatsEntryImpl ssi =
                            new SsoServerEntitlementExecStatsEntryImpl(mib2);
                        ssi.EntitlementNetworkMonitorName = str;
                        ssi.EntitlementMonitorThruPut = new Long(0);
                        ssi.EntitlementMonitorTotalTime = new Long(0);
                        ssi.EntitlementNetworkMonitorIndex = new Integer(i+1);

                        ObjectName sname =
                            ssi.
                            createSsoServerEntitlementExecStatsEntryObjectName(
                            server);

                        if (sname == null) {
                            debug.error(classMethod +
                                "Error creating object for Entitlements " +
                                "Network Monitor '" + str + "'");
                                   continue;
                        }

                        try {
                            etab.addEntry(ssi, sname);
                            if ((server != null) && (ssi != null)) {
                                server.registerMBean(ssi, sname);
                            }
                        } catch (JMException ex) {
                            debug.error(classMethod +
                                "on Entitlements Network Monitor '" +
                                str + "': ", ex);
                        } catch (SnmpStatusException ex) {
                            debug.error(classMethod +
                                "on Entitlements Network Monitor '" +
                                str + "': ", ex);
                        }
                    }
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod +
                        "Can't get Network Monitor Table: " +
                        ex.getMessage());
                }

                // now the realm-based policy stats

                try {
                    TableSsoServerEntitlementPolicyStatsTable ptab =
                        esi.accessSsoServerEntitlementPolicyStatsTable();
                    for (int i = 0; i < realmList.size(); i++) {
                        String ss = (String)realmList.get(i);
                        Integer Ii = new Integer(i+1);
                        SsoServerEntitlementPolicyStatsEntryImpl ssi =
                            new SsoServerEntitlementPolicyStatsEntryImpl(mib2);
                        ssi.EntitlementPolicyCaches = new Integer(0);
                        ssi.EntitlementReferralCaches = new Integer(0);
                        ssi.EntitlementPolicyStatsIndex = new Integer(i+1);
                        ssi.SsoServerRealmIndex = Ii;
                        ObjectName sname =
                          ssi.
                          createSsoServerEntitlementPolicyStatsEntryObjectName(
                              server);

                        if (sname == null) {
                            debug.error(classMethod +
                                "Error creating object for Entitlements " +
                                "Policy Stats, realm = '" + ss + "'");
                            continue;
                        }

                        try {
                            ptab.addEntry(ssi, sname);
                            if ((server != null) && (ssi != null)) {
                                server.registerMBean(ssi, sname);
                            }
                        } catch (JMException ex) {
                            debug.error(classMethod +
                                "on Entitlements Policy Stats '" +
                                ss + "': ", ex);
                        } catch (SnmpStatusException ex) {
                            debug.error(classMethod +
                                "on Entitlements Policy Stats '" +
                                ss + "': ", ex);
                        }
                    }
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod +
                        "getting Entitlements Policy Stats table: ", ex);
                }
            }
        } else {
            debug.error(classMethod +
                "Entitlement NetworkMonitor list empty.");
        }

        Date stopDate = new Date();
        if (debug.messageEnabled()) {
            String stDate = sdf.format(startDate);
            String endDate = sdf.format(stopDate);
            debug.message("Agent.realmsConfig:\n    Start Time = " +
                stDate + "\n      End Time = " + endDate);
        }
        return 0;
    }

    /*
     *  process configuration for a realm
     */
    public static int realmConfigMonitoringAgent (SSOServerRealmInfo rlmInfo) {
        String classMethod = "Agent.realmConfigMonitoringAgent:";
        String realm = rlmInfo.realmName;
        HashMap authMods = rlmInfo.authModules;

        Integer realmIndex = (Integer)realm2Index.get(realm);
        if (realmIndex == null) {
            debug.error(classMethod + "could not find realm " + realm +
                " in realm2Index map");
            return (-1);
        }
        SsoServerAuthSvcImpl sig =
            (SsoServerAuthSvcImpl)mib2.getAuthSvcGroup();
        TableSsoServerAuthModulesTable atab = null;
        if (sig != null) {
            try {
                atab = sig.accessSsoServerAuthModulesTable();
            } catch (SnmpStatusException ex) {
                debug.error(classMethod + "getting auth table: ", ex);
                return -2;
            } 
        }

        StringBuilder sb = new StringBuilder();
        
        if (debug.messageEnabled()) {
            sb.append("receiving config info for realm = ").
                append(realm).append(":\n  Authentication Modules:\n");
        }
        
        /*
         *  auth module table entries have realm index, and auth module index
         */
        int i = 1;
        Set authMKeys = authMods.keySet();
        for (Iterator it = authMKeys.iterator(); it.hasNext(); ) {
            String modInst = (String)it.next();
            String modType = (String)authMods.get(modInst);

            if (debug.messageEnabled()) {
                sb.append("    instance = ").append(modInst).
                    append(", value(type) = ").append(modType).append("\n");
            }
            SsoServerAuthModulesEntryImpl aei =
                new SsoServerAuthModulesEntryImpl(mib2);
            aei.SsoServerRealmIndex = realmIndex;
            aei.AuthModuleIndex = new Integer(i++);
            aei.AuthModuleName = modInst;
            aei.AuthModuleType = getEscapedString(modType);
            aei.AuthModuleSuccessCount = new Long(0);
            aei.AuthModuleFailureCount = new Long(0);
            ObjectName aname =
                aei.createSsoServerAuthModulesEntryObjectName(server);

            if (aname == null) {
                debug.error(classMethod +
                    "Error creating object for auth module name '" +
                    modInst + "', type '" + modType + "'");
                continue;
            }

            try {
                atab.addEntry(aei, aname);
                if ((server != null) && (aei != null)) {
                    server.registerMBean(aei, aname);
                }
                /* is a Map of realm/authmodule to index needed? */
                String rai = realm + "|" + modInst;
                // aei is this module's SsoServerAuthModulesEntryImpl instance
                realmAuthInst.put(rai, aei);
            } catch (JMException ex) {
                debug.error(classMethod + modInst, ex);
            } catch (SnmpStatusException ex) {
                debug.error(classMethod + modInst, ex);
            }
        }

        // if no realm info added because mbean not created...
        if (realmAuthInst.isEmpty()) {
            return -3;
        }

        if (debug.messageEnabled()) {
            debug.message(classMethod + sb.toString());
        }

        return (0);
    }

    /*
     *  process realm's Agents (only)
     *
     *  the HashMap of attributes/values:
     *    CLIConstants.ATTR_NAME_AGENT_TYPE
     *      type is extracted from the set; can be:
     *        J2EEAgent, WSPAgent, WSCAgent, 2.2_Agent
     *        WSPAgent, STSAgent, WebAgent, DiscoveryAgent
     *        don't do "SharedAgent" (authenticators)
     *
     *    J2EEAgent should have:
     *      "com.sun.identity.agents.config.login.url"
     *      "com.sun.identity.client.notification.url"
     *      "groupmembership"
     *    WSPAgent should have:
     *      "wspendpoint"
     *      "wspproxyendpoint"
     *      "groupmembership"
     *    WSCAgent should have:
     *      "wspendpoint"
     *      "wspproxyendpoint"
     *      "groupmembership"
     *    STSAgent should have:
     *      "stsendpoint"
     *      "groupmembership"
     *    WebAgent should have:
     *      "com.sun.identity.agents.config.agenturi.prefix"
     *      "com.sun.identity.agents.config.login.url"
     *      "groupmembership"
     *    DiscoveryAgent should have:
     *      "discoveryendpoint"
     *      "authnserviceendpoint"
     *      "groupmembership"
     *    2.2_Agent should have:
     *      "groupmembership"
     */
    public static void configAgentsOnly (String realm, Map agtAttrs) {
        String classMethod = "Agent.configAgentsOnly:";
        if ((agtAttrs == null) || agtAttrs.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message(classMethod + "got null attr map for realm " +
                    realm);
            }
            return;
        }

        SsoServerPolicyAgents sss =
            (SsoServerPolicyAgentsImpl)mib2.getPolicyAgentsGroup();
        TableSsoServerPolicy22AgentTable t22tab = null;
        TableSsoServerPolicyJ2EEAgentTable j2eetab = null;
        TableSsoServerPolicyWebAgentTable watab = null;
        SsoServerWSSAgents ssa =
            (SsoServerWSSAgentsImpl)mib2.getWssAgentsGroup();
        TableSsoServerWSSAgentsSTSAgentTable ststab = null;
        TableSsoServerWSSAgentsWSPAgentTable wsptab = null;
        TableSsoServerWSSAgentsWSCAgentTable wsctab = null;
        TableSsoServerWSSAgentsDSCAgentTable dsctab = null;

        /*
         *  get the tables
         */
        if (sss != null) {
            try {
                t22tab = sss.accessSsoServerPolicy22AgentTable();
                j2eetab = sss.accessSsoServerPolicyJ2EEAgentTable();
                watab = sss.accessSsoServerPolicyWebAgentTable();
                ststab = ssa.accessSsoServerWSSAgentsSTSAgentTable();
                wsptab = ssa.accessSsoServerWSSAgentsWSPAgentTable();
                wsctab = ssa.accessSsoServerWSSAgentsWSCAgentTable();
                dsctab = ssa.accessSsoServerWSSAgentsDSCAgentTable();
            } catch (SnmpStatusException ex) {
                debug.error(classMethod + "getting Agents tables: ", ex);
                return; // can't do anything without the tables
            }
        }
        if (ssa != null) {
            try {
                ststab = ssa.accessSsoServerWSSAgentsSTSAgentTable();
                wsptab = ssa.accessSsoServerWSSAgentsWSPAgentTable();
                wsctab = ssa.accessSsoServerWSSAgentsWSCAgentTable();
                dsctab = ssa.accessSsoServerWSSAgentsDSCAgentTable();
            } catch (SnmpStatusException ex) {
                debug.error(classMethod + "getting WSS Agents tables: ", ex);
                return; // can't do anything without the tables
            }
        }

        StringBuilder sb = new StringBuilder(classMethod);

        if (debug.messageEnabled()) {
            sb.append("agents for realm ").append(realm).append(", # = ").
                append(agtAttrs.size()).append("\n");
        }

        Set ks = agtAttrs.keySet();  // the agent names
        int wai = 1;  // index for web agents
        int j2eei = 1; // index for j2ee agents
        int t22i = 1;  // index for 2.2_agents
        int stsi = 1;  // index for STS agents
        int wspi = 1;  // index for WSP agents
        int wsci = 1;  // index for WSC agents
        int dsci = 1;  // index for DSC agents
        Integer ri = getRealmIndexFromName(realm);

        /*
         *  if the realm isn't in the table, there's not much point
         *  in doing the rest
         */
        if (ri == null) {
            debug.error(classMethod + "didn't find index for realm " +
                realm);
            return;
        }

        for (Iterator it = ks.iterator(); it.hasNext(); ) {
            String agtname = (String)it.next();
            HashMap hm = (HashMap)agtAttrs.get(agtname);
            String atype = (String)hm.get(CLIConstants.ATTR_NAME_AGENT_TYPE);
            String grpmem = (String)hm.get("groupmembership");

            //  group and agent name can't have ":" in it, or jdmk gags
            if (grpmem == null) {
                grpmem = None;
            } else {
                grpmem = getEscapedString(grpmem);
            }
            agtname = getEscapedString(agtname);

            if (debug.messageEnabled()) {
                sb.append("  agent name = ").append(agtname).
                    append(", type = ").append(atype).
                    append(", membership = ").append(grpmem).append("\n");
            }
        
            if (atype.equals("WebAgent")) {
                String aurl =
                    (String)hm.get(
                        "com.sun.identity.agents.config.agenturi.prefix");
                String lurl =
                    (String)hm.get("com.sun.identity.agents.config.login.url");
                SsoServerPolicyWebAgentEntryImpl aei =
                    new SsoServerPolicyWebAgentEntryImpl(mib2);
                aei.SsoServerRealmIndex = ri;
                aei.PolicyWebAgentIndex = new Integer(wai++);
                aei.PolicyWebAgentName = agtname;
                aei.PolicyWebAgentGroup = grpmem;
                aei.PolicyWebAgentAgentURL = aurl;
                aei.PolicyWebAgentServerURL = lurl;
                ObjectName aname =
                    aei.createSsoServerPolicyWebAgentEntryObjectName(server);
        
                if (aname == null) {
                    debug.error(classMethod +
                        "Error creating object for Policy WebAgent '" +
                        agtname + "'");
                    continue;
                }

                try {
                    watab.addEntry(aei, aname);
                    if ((server != null) && (aei != null)) {
                        server.registerMBean(aei, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                }
            } else if (atype.equals("2.2_Agent")) {
                SsoServerPolicy22AgentEntryImpl aei =
                    new SsoServerPolicy22AgentEntryImpl(mib2);
                aei.SsoServerRealmIndex = ri;
                aei.Policy22AgentIndex = new Integer(t22i++);
                aei.Policy22AgentName = agtname;

                ObjectName aname =
                    aei.createSsoServerPolicy22AgentEntryObjectName(server);

                if (aname == null) {
                    debug.error(classMethod +
                        "Error creating object for Policy 2.2 Agent '" +
                        agtname + "'");
                    continue;
                }

                try {
                    t22tab.addEntry(aei, aname);
                    if ((server != null) && (aei != null)) {
                        server.registerMBean(aei, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                }
            } else if (atype.equals("J2EEAgent")) {
                SsoServerPolicyJ2EEAgentEntryImpl aei =
                    new SsoServerPolicyJ2EEAgentEntryImpl(mib2);
                String aurl =
                    (String)hm.get("com.sun.identity.client.notification.url");
                if (aurl == null) {
                    aurl = None;
                }
                String lurl =
                    (String)hm.get("com.sun.identity.agents.config.login.url");
                aei.PolicyJ2EEAgentGroup = grpmem;
                aei.PolicyJ2EEAgentAgentURL = aurl;
                aei.PolicyJ2EEAgentServerURL = lurl;
                aei.PolicyJ2EEAgentName = agtname;
                aei.PolicyJ2EEAgentIndex = new Integer(j2eei++);
                aei.SsoServerRealmIndex = ri;
                ObjectName aname =
                    aei.createSsoServerPolicyJ2EEAgentEntryObjectName(server);

                if (aname == null) {
                    debug.error(classMethod +
                        "Error creating object for Policy J2EE Agent '" +
                        agtname + "'");
                    continue;
                }

                try {
                    j2eetab.addEntry(aei, aname);
                    if ((server != null) && (aei != null)) {
                        server.registerMBean(aei, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                }
            } else if (atype.equals("WSPAgent")) {
                SsoServerWSSAgentsWSPAgentEntryImpl aei =
                    new SsoServerWSSAgentsWSPAgentEntryImpl(mib2);
                String wep = (String)hm.get("wsendpoint");
                if (wep == null) {
                    wep = NotAvail;
                }
                String wpep = (String)hm.get("wspproxyendpoint");
                if (wpep == null) {
                    wpep = NotAvail;
                }
                String mgrp = (String)hm.get("groupmembership");
                if (mgrp == null) {
                    mgrp = None;
                }
                aei.WssAgentsWSPAgentName = agtname;
                aei.WssAgentsWSPAgentSvcEndPoint = wep;
                aei.WssAgentsWSPAgentProxy = wpep;
                aei.WssAgentsWSPAgentIndex = new Integer(wspi++);
                aei.SsoServerRealmIndex = ri;
                // no entry for group membership...
                ObjectName aname =
                    aei.createSsoServerWSSAgentsWSPAgentEntryObjectName(server);

                if (aname == null) {
                    debug.error(classMethod +
                        "Error creating object for Policy WSP Agent '" +
                        agtname + "'");
                    continue;
                }

                try {
                    if (wsptab != null) {
                        wsptab.addEntry(aei, aname);
                        if ((server != null) && (aei != null)) {
                            server.registerMBean(aei, aname);
                        }
                    } else {
                        debug.error(classMethod + "WSPAgent: agtname = " +
                            agtname + ", wep = " + wep +
                            ", wpep = " + wpep + ", mgrp = " + mgrp +
                            ", realm = " + realm);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                }
            } else if (atype.equals("WSCAgent")) {
                SsoServerWSSAgentsWSCAgentEntryImpl aei =
                    new SsoServerWSSAgentsWSCAgentEntryImpl(mib2);
                String wep = (String)hm.get("wsendpoint");
                if (wep == null) {
                    wep = None;
                }
                String wpep = (String)hm.get("wspproxyendpoint");
                if (wpep == null) {
                    wpep = None;
                }
                String mgrp = (String)hm.get("groupmembership");
                if (mgrp == null) {
                    mgrp = None;
                }
                aei.WssAgentsWSCAgentName = agtname;
                aei.WssAgentsWSCAgentSvcEndPoint = wep;
                aei.WssAgentsWSCAgentProxy = wpep;
                aei.WssAgentsWSCAgentIndex = new Integer(wsci++);
                aei.SsoServerRealmIndex = ri;
                // no entry for group membership...
                ObjectName aname =
                    aei.createSsoServerWSSAgentsWSCAgentEntryObjectName(server);

                if (aname == null) {
                    debug.error(classMethod +
                        "Error creating object for Policy WSC Agent '" +
                        agtname + "'");
                    continue;
                }

                try {
                    wsctab.addEntry(aei, aname);
                    if ((server != null) && (aei != null)) {
                        server.registerMBean(aei, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                }
            } else if (atype.equals("STSAgent")) {
                SsoServerWSSAgentsSTSAgentEntryImpl aei =
                    new SsoServerWSSAgentsSTSAgentEntryImpl(mib2);
                String sep = (String)hm.get("stsendpoint");
                aei.WssAgentsSTSAgentName = agtname;
                aei.WssAgentsSTSAgentSvcTokenEndPoint = sep;
                aei.WssAgentsSTSAgentIndex = new Integer(stsi++);
                aei.WssAgentsSTSAgentSvcMEXEndPoint = NotAvail; // notretrieved
                aei.SsoServerRealmIndex = ri;
                // no entry for group membership...

                ObjectName aname =
                    aei.createSsoServerWSSAgentsSTSAgentEntryObjectName(server);

                if (aname == null) {
                    debug.error(classMethod +
                        "Error creating object for Policy STS Agent '" +
                        agtname + "'");
                    continue;
                }

                try {
                    ststab.addEntry(aei, aname);
                    if ((server != null) && (aei != null)) {
                        server.registerMBean(aei, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                }
            } else if (atype.equals("DiscoveryAgent")) {
                SsoServerWSSAgentsDSCAgentEntryImpl aei =
                    new SsoServerWSSAgentsDSCAgentEntryImpl(mib2);
                String dep = (String)hm.get("discoveryendpoint");
                if (dep == null) {
                    dep = NotAvail;
                }
                String aep = (String)hm.get("authnserviceendpoint");
                if (aep == null) {
                    aep = NotAvail;
                }
                aei.WssAgentsDSCAgentName = agtname;
                aei.WssAgentsDSCAgentWebSvcEndPoint = dep;
                aei.WssAgentsDSCAgentSvcEndPoint = aep;
                aei.WssAgentsDSCAgentIndex = new Integer(dsci++);
                aei.SsoServerRealmIndex = ri;
                // no entry for group membership...
                ObjectName aname =
                    aei.createSsoServerWSSAgentsDSCAgentEntryObjectName(server);

                if (aname == null) {
                    debug.error(classMethod +
                        "Error creating object for Policy Discovery Agent '" +
                        agtname + "'");
                    continue;
                }

                try {
                    dsctab.addEntry(aei, aname);
                    if ((server != null) && (aei != null)) {
                        server.registerMBean(aei, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                }
            } else if (atype.equals("SharedAgent")) {
                // SharedAgent type are agent authenticators
            } else {
                debug.error(classMethod + "agent type = " + atype +
                    ", agent name = " + agtname + " not supported.");
            }
        }
        if (debug.messageEnabled()) {
            debug.message(sb.toString());
        }
    }

    /*
     *  process realm's Agent Groups
     *
     *  the HashMap of attributes/values:
     *    CLIConstants.ATTR_NAME_AGENT_TYPE
     *      type is extracted from the set; can be:
     *        STSAgent, WSPAgent, WSCAgent, WebAgent
     *        J2EEAgent, DiscoveryAgent
     *        don't do "SharedAgent" (authenticators)
     *    WSPAgent should have:
     *      "wspendpoint"
     *      "wspproxyendpoint"
     *    WSCAgent should have:
     *      "wspendpoint"
     *      "wspproxyendpoint"
     *    WebAgent should have:
     *      "com.sun.identity.agents.config.agenturi.prefix"
     *      "com.sun.identity.agents.config.login.url"
     *    J2EEAgents should have:
     *      "com.sun.identity.agents.config.login.url"
     *      "com.sun.identity.client.notification.url"
     *    DiscoveryAgent should have:
     *      "discoveryendpoint"
     *      "authnserviceendpoint"
     *    STSAgent should have:
     *      "stsendpoint"
     *    2.2_Agent
     *      no groups
     */
    public static void configAgentGroups (String realm, Map agtAttrs) {
        String classMethod = "Agent.configAgentGroups:";
        if ((agtAttrs == null) || agtAttrs.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message(classMethod + "got null attr map for realm " +
                    realm);
            }
            return;
        }

        /*
         *  only doing the J2EEAgent and WebAgent Groups
         *  for now.
         */
        SsoServerPolicyAgents sss =
            (SsoServerPolicyAgentsImpl)mib2.getPolicyAgentsGroup();
        TableSsoServerPolicyJ2EEGroupTable j2eetab = null;
        TableSsoServerPolicyWebGroupTable wgtab = null;
        SsoServerWSSAgents ssa =
            (SsoServerWSSAgentsImpl)mib2.getWssAgentsGroup();
        TableSsoServerWSSAgentsSTSAgtGrpTable ststab = null;
        TableSsoServerWSSAgentsWSPAgtGrpTable wsptab = null;
        TableSsoServerWSSAgentsWSCAgtGrpTable wsctab = null;
        TableSsoServerWSSAgentsDSCAgtGrpTable dsctab = null;

        if (sss != null) {
            try {
                j2eetab = sss.accessSsoServerPolicyJ2EEGroupTable();
                wgtab = sss.accessSsoServerPolicyWebGroupTable();
            } catch (SnmpStatusException ex) {
                debug.error(classMethod +
                    "getting Agent Groups tables: ", ex);
                return; // can't do anything without the tables
            }
        }
        if (ssa != null) {
            try {
                ststab = ssa.accessSsoServerWSSAgentsSTSAgtGrpTable();
                wsptab = ssa.accessSsoServerWSSAgentsWSPAgtGrpTable();
                wsctab = ssa.accessSsoServerWSSAgentsWSCAgtGrpTable();
                dsctab = ssa.accessSsoServerWSSAgentsDSCAgtGrpTable();
            } catch (SnmpStatusException ex) {
                debug.error(classMethod +
                    "getting WSS Agent Groups tables: ", ex);
                return; // can't do anything without the tables
            }
        }

        StringBuilder sb = new StringBuilder(classMethod);
        if (debug.messageEnabled()) {
            sb.append("agents for realm ").append(realm).append(", # = ").
                append(agtAttrs.size()).append("\n");
        }

        Set ks = agtAttrs.keySet();  // the agent group names
        int wai = 1;  // index for web agent groups
        int j2eei = 1; // index for j2ee agent groups
        int stsi = 1;  // index for STS agent groups
        int wspi = 1;  // index for WSP agent groups
        int wsci = 1;  // index for WSC agent groups
        int dsci = 1;  // index for DSC agent groups
        Integer ri = getRealmIndexFromName(realm);

        /*
         *  if the realm isn't in the table, there's not much point
         *  in doing the rest
         */
        if (ri == null) {
            debug.error(classMethod + "didn't find index for realm " +
                realm);
            return;
        }

        for (Iterator it = ks.iterator(); it.hasNext(); ) {
            String agtname = (String)it.next();
            HashMap hm = (HashMap)agtAttrs.get(agtname);
            String atype = (String)hm.get(CLIConstants.ATTR_NAME_AGENT_TYPE);

            if (debug.messageEnabled()) {
                sb.append("  agent group name = ").append(agtname).
                    append(", type = ").append(atype).append("\n");
            }

            agtname = getEscapedString(agtname);

            if (atype.equals("WebAgent")) {
                if (wgtab == null) {
                    continue;  // no table to put it into
                }
                String lurl =
                    (String)hm.get("com.sun.identity.agents.config.login.url");
                SsoServerPolicyWebGroupEntryImpl aei =
                    new SsoServerPolicyWebGroupEntryImpl(mib2);
                aei.SsoServerRealmIndex = ri;
                aei.PolicyWebGroupIndex = new Integer(wai++);
                aei.PolicyWebGroupName = agtname;
                aei.PolicyWebGroupServerURL = lurl;
                ObjectName aname =
                    aei.createSsoServerPolicyWebGroupEntryObjectName(server);

                if (aname == null) {
                    debug.error(classMethod +
                        "Error creating object for Policy Web Agent Group '" +
                        agtname + "'");
                    continue;
                }

                try {
                    wgtab.addEntry(aei, aname);
                    if ((server != null) && (aei != null)) {
                        server.registerMBean(aei, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                }
            } else if (atype.equals("J2EEAgent")) {
                if (j2eetab == null) {
                    continue;  // no table to put it into
                }
                SsoServerPolicyJ2EEGroupEntryImpl aei =
                    new SsoServerPolicyJ2EEGroupEntryImpl(mib2);
                String lurl =
                    (String)hm.get("com.sun.identity.agents.config.login.url");
                aei.PolicyJ2EEGroupServerURL = lurl;
                aei.PolicyJ2EEGroupName = agtname;
                aei.PolicyJ2EEGroupIndex = new Integer(j2eei++);
                aei.SsoServerRealmIndex = ri;
                ObjectName aname =
                    aei.createSsoServerPolicyJ2EEGroupEntryObjectName(server);

                if (aname == null) {
                    debug.error(classMethod +
                        "Error creating object for Policy J2EE Agent Group '" +
                        agtname + "'");
                    continue;
                }

                try {
                    j2eetab.addEntry(aei, aname);
                    if ((server != null) && (aei != null)) {
                        server.registerMBean(aei, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                }
            } else if (atype.equals("WSPAgent")) {
                if (wsptab == null) {
                    continue;  // no table to put it into
                }
                SsoServerWSSAgentsWSPAgtGrpEntryImpl aei =
                    new SsoServerWSSAgentsWSPAgtGrpEntryImpl(mib2);
                String wep = (String)hm.get("wsendpoint");
                if (wep == null) {
                    wep = NotAvail;
                }
                String wpep = (String)hm.get("wspproxyendpoint");
                if (wpep == null) {
                    wpep = NotAvail;
                }
                aei.WssAgentsWSPAgtGrpName = agtname;
                aei.WssAgentsWSPAgtGrpSvcEndPoint = wep;
                aei.WssAgentsWSPAgtGrpProxy = wpep;
                aei.WssAgentsWSPAgtGrpIndex = new Integer(wspi++);
                aei.SsoServerRealmIndex = ri;
                ObjectName aname =
                    aei.createSsoServerWSSAgentsWSPAgtGrpEntryObjectName(
                        server);

                if (aname == null) {
                    debug.error(classMethod +
                        "Error creating object for Policy WSP Agent Group '" +
                        agtname + "'");
                    continue;
                }

                try {
                    wsptab.addEntry(aei, aname);
                    if ((server != null) && (aei != null)) {
                        server.registerMBean(aei, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                }
            } else if (atype.equals("WSCAgent")) {
                if (wsctab == null) {
                    continue;  // no table to put it into
                }
                SsoServerWSSAgentsWSCAgtGrpEntryImpl aei =
                    new SsoServerWSSAgentsWSCAgtGrpEntryImpl(mib2);
                String wep = (String)hm.get("wsendpoint");
                if (wep == null) {
                    wep = NotAvail;
                }
                String wpep = (String)hm.get("wspproxyendpoint");
                if (wpep == null) {
                    wpep = NotAvail;
                }
                aei.WssAgentsWSCAgtGrpName = agtname;
                aei.WssAgentsWSCAgtGrpSvcEndPoint = wep;
                aei.WssAgentsWSCAgtGrpProxy = wpep;
                aei.WssAgentsWSCAgtGrpIndex = new Integer(wsci++);
                aei.SsoServerRealmIndex = ri;
                ObjectName aname =
                    aei.createSsoServerWSSAgentsWSCAgtGrpEntryObjectName(
                        server);

                if (aname == null) {
                    debug.error(classMethod +
                        "Error creating object for Policy WSC Agent Group '" +
                        agtname + "'");
                    continue;
                }

                try {
                    wsctab.addEntry(aei, aname);
                    if ((server != null) && (aei != null)) {
                        server.registerMBean(aei, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                }
            } else if (atype.equals("STSAgent")) {
                if (ststab == null) {
                    continue;  // no table to put it into
                }
                SsoServerWSSAgentsSTSAgtGrpEntryImpl aei =
                    new SsoServerWSSAgentsSTSAgtGrpEntryImpl(mib2);
                String sep = (String)hm.get("stsendpoint");
                if (sep == null) {
                    sep = NotAvail;
                }
                aei.WssAgentsSTSAgtGrpName = agtname;
                aei.WssAgentsSTSAgtGrpSvcEndPoint = sep;
                aei.WssAgentsSTSAgtGrpIndex = new Integer(stsi++);
                aei.WssAgentsSTSAgtGrpSvcMEXEndPoint = NotAvail; //notretrieved
                aei.SsoServerRealmIndex = ri;

                ObjectName aname =
                    aei.createSsoServerWSSAgentsSTSAgtGrpEntryObjectName(
                        server);

                if (aname == null) {
                    debug.error(classMethod +
                        "Error creating object for Policy STS Agent Group '" +
                        agtname + "'");
                    continue;
                }

                try {
                    ststab.addEntry(aei, aname);
                    if ((server != null) && (aei != null)) {
                        server.registerMBean(aei, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                }
            } else if (atype.equals("DiscoveryAgent")) {
                if (dsctab == null) {
                    continue;  // no table to put it into
                }
                SsoServerWSSAgentsDSCAgtGrpEntryImpl aei =
                    new SsoServerWSSAgentsDSCAgtGrpEntryImpl(mib2);
                String dep = (String)hm.get("discoveryendpoint");
                if (dep == null) {
                    dep = NotAvail;
                }
                String aep = (String)hm.get("authnserviceendpoint");
                if (aep == null) {
                    aep = NotAvail;
                }
                aei.WssAgentsDSCAgtGrpName = agtname;
                aei.WssAgentsDSCAgtGrpWebSvcEndPoint = dep;
                aei.WssAgentsDSCAgtGrpSvcEndPoint = aep;
                aei.WssAgentsDSCAgtGrpIndex = new Integer(dsci++);
                aei.SsoServerRealmIndex = ri;
                ObjectName aname =
                    aei.createSsoServerWSSAgentsDSCAgtGrpEntryObjectName(
                        server);

                if (aname == null) {
                    debug.error(classMethod +
                        "Error creating object for Policy Discovery Agent " +
                        "Group '" + agtname + "'");
                    continue;
                }

                try {
                    dsctab.addEntry(aei, aname);
                    if ((server != null) && (aei != null)) {
                        server.registerMBean(aei, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod + agtname + ": " + ex.getMessage());
                }
            } else if (atype.equals("SharedAgent")) {
            } else {
                debug.error(classMethod + "agent group type = " + atype +
                    ", agent group name = " + agtname + " not supported.");
            }
        }

        if (debug.messageEnabled()) {
            debug.message(sb.toString());
        }
    }

    /*
     *  process saml1.x trusted partners (global)
     */
    public static int saml1TPConfig (List s1TPInfo) {
        String classMethod = "Agent.saml1TPConfig:";
        StringBuilder sb = new StringBuilder(classMethod);
        int sz = s1TPInfo.size();
        boolean skipSAML1EndPoints = true;  // until instrumentation done

        Date startDate = new Date();
        if (debug.messageEnabled()) {
            sb.append("number of SAML1 Trusted Partners = ").append(sz).
                append("\n");
        }

        if (server == null) {  // can't do anything without a server
            debug.error(classMethod + "no server");
            return -1;
        }

        for (int i = 0; i < sz; i++) {
            String pName = (String)s1TPInfo.get(i);

            if (debug.messageEnabled()) {
                sb.append("    ").append(pName).append("\n");
            }

            SsoServerSAML1TrustPrtnrsEntryImpl sstpe =
                new SsoServerSAML1TrustPrtnrsEntryImpl(mib2);
            sstpe.SAML1TrustPrtnrIndex = new Integer(i+1);
            sstpe.SAML1TrustPrtnrName = getEscapedString(pName);

            SsoServerSAML1Svc sss =
                (SsoServerSAML1SvcImpl)mib2.getSaml1SvcGroup();
            TableSsoServerSAML1TrustPrtnrsTable tptab = null;
            if (sss != null) {
                try {
                    tptab = sss.accessSsoServerSAML1TrustPrtnrsTable();
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod +
                        "getting SAML1 trusted partner table: ", ex);
                    return -2; // can't do anything without the table
                }
            }
            if (tptab == null) {
                return -2;  // can't do anything without the table
            }

            ObjectName aname =
                sstpe.createSsoServerSAML1TrustPrtnrsEntryObjectName(server);
        
            if (aname == null) {
                debug.error(classMethod +
                    "Error creating object for SAML1 Trusted Partner '" +
                    pName + "'");
                continue;
            }

            try {
                tptab.addEntry(sstpe, aname);
                if (sstpe != null) {
                    server.registerMBean(sstpe, aname);
                }
            } catch (JMException ex) {
                debug.error(classMethod + pName + ": " + ex.getMessage());
            } catch (SnmpStatusException ex) {
                debug.error(classMethod + pName + ": " + ex.getMessage());
            }
        }

        if (debug.messageEnabled()) {
            debug.message(sb.toString());
        }

        /*
         *  while we're here, setup the 
         *    SAML1 Cache table (Artifacts and Assertions)
         *    SAML1 Endpoints for SOAPReceiver, POSTProfile,
         *      SAMLAware/ArtifactProfile
         */
        
        // assertions
        SsoServerSAML1CacheEntryImpl ssce =
                new SsoServerSAML1CacheEntryImpl(mib2);
        ssce.SAML1CacheIndex = new Integer(1);
        ssce.SAML1CacheName = "Assertion_Cache";
        ssce.SAML1CacheMisses = new Long(0);
        ssce.SAML1CacheHits = new Long(0);
        ssce.SAML1CacheWrites = new Long(0);
        ssce.SAML1CacheReads = new Long(0);

        SsoServerSAML1SvcImpl sss =
            (SsoServerSAML1SvcImpl)mib2.getSaml1SvcGroup();
        TableSsoServerSAML1CacheTable tptab = null;

        if (sss != null) {
            try {
                tptab = sss.accessSsoServerSAML1CacheTable();
            } catch (SnmpStatusException ex) {
                debug.error(classMethod + "getting SAML1 Cache table: ", ex);
            }
        }
        if (tptab != null) {  // if sss is null, so will tptab
            sss.assertCache = ssce;

            ObjectName aname =
                ssce.createSsoServerSAML1CacheEntryObjectName(server);
        
            if (aname == null) {
                debug.error(classMethod +
                    "Error creating object for SAML1 Assertion Cache");
            } else {
                try {
                    tptab.addEntry(ssce, aname);
                    if (ssce != null) {
                        server.registerMBean(ssce, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod +
                        "SAML1 Assertion Cache table: " + ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod +
                        "SAML1 Assertion Cache table: " + ex.getMessage());
                }
            }

            // artifacts
            ssce = new SsoServerSAML1CacheEntryImpl(mib2);
            ssce.SAML1CacheIndex = new Integer(2);
            ssce.SAML1CacheName = "Artifact_Cache";
            ssce.SAML1CacheMisses = new Long(0);
            ssce.SAML1CacheHits = new Long(0);
            ssce.SAML1CacheWrites = new Long(0);
            ssce.SAML1CacheReads = new Long(0);

            aname = ssce.createSsoServerSAML1CacheEntryObjectName(server);
            if (aname == null) {
                debug.error(classMethod +
                    "Error creating object for SAML1 Artifact Cache");
            } else {
                try {
                    tptab.addEntry(ssce, aname);
                    if (ssce != null) {
                        server.registerMBean(ssce, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod + "SAML1 Artifact Cache table: " +
                        ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod + "SAML1 Artifact Cache table: " +
                        ex.getMessage());
                }
                sss.artifactCache = ssce;
            }
        }

        // SOAPReceiver endpoint
        if (!skipSAML1EndPoints) {
        SsoServerSAML1EndPointEntryImpl ssee =
                new SsoServerSAML1EndPointEntryImpl(mib2);
        ssee.SAML1EndPointIndex = new Integer(1);
        ssee.SAML1EndPointName = "SOAPReceiver_EndPoint";
        ssee.SAML1EndPointRqtFailed = new Long(0);
        ssee.SAML1EndPointRqtOut = new Long(0);
        ssee.SAML1EndPointRqtIn = new Long(0);
        ssee.SAML1EndPointRqtAborted = new Long(0);
        ssee.SAML1EndPointStatus = "operational";

        TableSsoServerSAML1EndPointTable tetab = null;
        if (sss != null) {
            try {
                tetab = sss.accessSsoServerSAML1EndPointTable();
            } catch (SnmpStatusException ex) {
                debug.error(classMethod +
                    "getting SAML1 EndPoint table: ", ex);
            }
        }
        if (tetab != null) {  // if sss is null, so will tetab
            ObjectName aname =
                ssee.createSsoServerSAML1EndPointEntryObjectName(server);

            if (aname == null) {
                debug.error(classMethod +
                    "Error creating object for SAML1 SOAPReceiver_EndPoint");
            } else {
                try {
                    tetab.addEntry(ssee, aname);
                    if (ssee != null) {
                        server.registerMBean(ssee, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod +
                        "SAML1 SOAPReceiver EndPoint table: " +
                        ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod +
                        "SAML1 SOAPReceiver EndPoint table: " +
                        ex.getMessage());
                }
                sss.soapEP = ssee;
            }

            // POSTProfile table
            ssee = new SsoServerSAML1EndPointEntryImpl(mib2);
            ssee.SAML1EndPointIndex = new Integer(2);
            ssee.SAML1EndPointName = "POSTProfile_EndPoint";
            ssee.SAML1EndPointRqtFailed = new Long(0);
            ssee.SAML1EndPointRqtOut = new Long(0);
            ssee.SAML1EndPointRqtIn = new Long(0);
            ssee.SAML1EndPointRqtAborted = new Long(0);
            ssee.SAML1EndPointStatus = "operational";

            aname = ssee.createSsoServerSAML1EndPointEntryObjectName(server);

            if (aname == null) {
                debug.error(classMethod +
                    "Error creating object for SAML1 POSTProfile_EndPoint");
            } else {
                try {
                    tetab.addEntry(ssee, aname);
                    if (ssee != null) {
                        server.registerMBean(ssee, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod +
                        "SAML1 POSTProfile EndPoint table: " +
                        ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod +
                        "SAML1 POSTProfile EndPoint table: " +
                        ex.getMessage());
                }
                sss.pprofEP = ssee;
            }

            // SAMLAware/ArtifactProfile table
            ssee = new SsoServerSAML1EndPointEntryImpl(mib2);
            ssee.SAML1EndPointIndex = new Integer(3);
            ssee.SAML1EndPointName = "SAMLAware_EndPoint";
            ssee.SAML1EndPointRqtFailed = new Long(0);
            ssee.SAML1EndPointRqtOut = new Long(0);
            ssee.SAML1EndPointRqtIn = new Long(0);
            ssee.SAML1EndPointRqtAborted = new Long(0);
            ssee.SAML1EndPointStatus = "operational";

            aname = ssee.createSsoServerSAML1EndPointEntryObjectName(server);

            if (aname == null) {
                debug.error(classMethod +
                    "Error creating object for SAML1 SAMLAware_EndPoint");
            } else {
                try {
                    tetab.addEntry(ssee, aname);
                    if (ssee != null) {
                        server.registerMBean(ssee, aname);
                    }
                } catch (JMException ex) {
                    debug.error(classMethod +
                        "SAML1 SAMLAware/ArtifactProfile EndPoint table: " +
                        ex.getMessage());
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod +
                        "SAML1 SAMLAware/ArtifactProfile EndPoint table: " +
                        ex.getMessage());
                }
                sss.samlAwareEP = ssee;
            }
        }
        } // if (!skipSAML1EndPoints)

        Date stopDate = new Date();
        if (debug.messageEnabled()) {
            String stDate = sdf.format(startDate);
            String endDate = sdf.format(stopDate);
            debug.message("Agent.saml1TPConfig:\n    Start Time = " +
                stDate + "\n      End Time = " + endDate);
        }

        return (0);
    }

    public static int federationConfig (SSOServerRealmFedInfo srfi)
    {
        String classMethod = "Agent.federationConfig:";

        Date startDate = new Date();
        String realm = srfi.realmName;
        Integer ri = getRealmIndexFromName(realm);
        Set cots = srfi.cots;
        Map saml2Ents = srfi.samlv2Ents;
        Map wsEnts = srfi.wsEnts;
        Map idffEnts = srfi.idffEnts;
        Map cotMembs = srfi.membEnts;

        StringBuilder sb = new StringBuilder(classMethod);
        if (debug.messageEnabled()) {
            sb.append("fed entities for realm ").append(realm).append(":\n");
            sb.append("  Circle of Trusts set has ");
        }

        if (server == null) {  // can't do anything without a server
            debug.error(classMethod + "no server");
            return -1;
        }

        SsoServerFedCOTs ssfc = (SsoServerFedCOTs)getFedCOTsMBean();

        if ((cots != null) && (cots.size() > 0)) {
            if (debug.messageEnabled()) {
                sb.append(cots.size()).append(" entries:\n");
            }
            TableSsoServerFedCOTsTable ftab = null;
            try {
                ftab = ssfc.accessSsoServerFedCOTsTable();
            } catch (SnmpStatusException ex) {
                debug.error(classMethod +
                    "getting fed COTs table: ", ex);
            }
            if (ftab != null) {
                int i = 1;
                for (Iterator it = cots.iterator(); it.hasNext(); ) {
                    String ss = (String)it.next();
                    ss = getEscapedString(ss);

                    if (debug.messageEnabled()) {
                            sb.append("  #").append(i).append(": ").append(ss).
                                append("\n");
                    }

                    SsoServerFedCOTsEntryImpl cei =
                        new SsoServerFedCOTsEntryImpl(mib2);
                    cei.SsoServerRealmIndex = ri;
                    cei.FedCOTName = ss;
                    cei.FedCOTIndex = new Integer(i++);
                    ObjectName oname =
                        cei.createSsoServerFedCOTsEntryObjectName(server);

                    if (oname == null) {
                        debug.error(classMethod +
                            "Error creating object for Fed COT '" + ss + "'");
                        continue;
                    }

                    try {
                        ftab.addEntry(cei, oname);
                        if (cei != null) {
                            server.registerMBean(cei, oname);
                        }
                    } catch (JMException ex) {
                        debug.error(classMethod + ss, ex);
                    } catch (SnmpStatusException ex) {
                        debug.error(classMethod + ss, ex);
                    }
                }
            } else {
                if (debug.messageEnabled()) {
                    sb.append("no entries\n");
                }
            }
        }

        /*
         *  the federation entities all go into the
         *  SsoServerFedEntitiesTable
         */
        
        SsoServerFedEntities ssfe = (SsoServerFedEntities)getFedEntsMBean();
        TableSsoServerFedEntitiesTable ftab = null;
        try {
            ftab = ssfe.accessSsoServerFedEntitiesTable();
        } catch (SnmpStatusException ex) {
            debug.error(classMethod +
                "getting FederationEntities table: ", ex);
            return -1;  // can't proceed without the table
        }

        if (ftab != null) {
            /*
             *  the SAML2 entities map:
             *    entity name -> hashmap of:
             *      key="location"; value="hosted" or "remote"
             *      key="roles"; value=some combo of IDP;SP
             */
        
            int tabinx = 1;  // increments for all entries
            if (debug.messageEnabled()) {
                sb.append("\n  SAML2 entities map has ");
            }

            if ((saml2Ents != null) && (saml2Ents.size() > 0)) {
                TableSsoServerSAML2IDPTable iTab = null;
                TableSsoServerSAML2SPTable sTab = null;
                SsoServerSAML2SvcImpl ss2s =
                    (SsoServerSAML2SvcImpl)getSAML2SvcGroup();
                try {
                    iTab = ss2s.accessSsoServerSAML2IDPTable();
                    sTab = ss2s.accessSsoServerSAML2SPTable();
                } catch (SnmpStatusException ex) {
                    debug.error(classMethod +
                        "getting SAML2 IDP and/or SP tables: ", ex);
                    return -1;  // can't proceed without the tables
                }

                if (debug.messageEnabled()) {
                    sb.append(saml2Ents.size()).append(" entries:\n");
                }

                Set ks = saml2Ents.keySet();
                int idpi = 1;
                int spi = 1;
                for (Iterator it = ks.iterator(); it.hasNext(); ) {
                    String entname = (String)it.next();
                    HashMap hm = (HashMap)saml2Ents.get(entname);
                    String loc = (String)hm.get("location");
                    String roles = (String)hm.get("roles");

                    SsoServerFedEntitiesEntryImpl cei =
                        new SsoServerFedEntitiesEntryImpl(mib2);
                    cei.SsoServerRealmIndex = ri;
                    cei.FedEntityName = getEscapedString(entname);
                    cei.FedEntityIndex = new Integer(tabinx++);
                    cei.FedEntityProto = "SAMLv2";
                    cei.FedEntityType = roles;
                    cei.FedEntityLoc = loc;
                    ObjectName oname =
                        cei.createSsoServerFedEntitiesEntryObjectName(server);

                    if (oname == null) {
                        debug.error(classMethod +
                            "Error creating object for SAML2 Entity '" +
                            entname + "'");
                        continue;
                    }

                    try {
                        ftab.addEntry(cei, oname);
                        if (cei != null) {
                            server.registerMBean(cei, oname);
                        }
                    } catch (JMException ex) {
                        debug.error(classMethod +
                            "JMEx adding SAMLv2 entity " +
                            entname + " in realm " + realm, ex);
                    } catch (SnmpStatusException ex) {
                        debug.error(classMethod +
                            "SnmpEx adding SAMLv2 entity " +
                            entname + " in realm " + realm, ex);
                    }

                    /*
                     * these also need to be added to either (possibly
                     * both if in both roles?) SAML2's IDP or SP table
                     */
                    if (((roles.indexOf("IDP")) >= 0) &&
                        loc.equalsIgnoreCase("hosted"))
                    {
                        if (iTab == null) {
                            continue;
                        }

                        SsoServerSAML2IDPEntryImpl sei =
                            new SsoServerSAML2IDPEntryImpl(mib2);
                        sei.SAML2IDPArtifactsIssued = new Long(0);
                        sei.SAML2IDPAssertionsIssued = new Long(0);
                        sei.SAML2IDPInvalRqtsRcvd = new Long(0);
                        sei.SAML2IDPRqtsRcvd = new Long(0);
                        sei.SAML2IDPArtifactsInCache = new Long(0);
                        sei.SAML2IDPAssertionsInCache = new Long(0);
                        sei.SAML2IDPIndex = new Integer(idpi++);
                        sei.SAML2IDPName = getEscapedString(entname);
                        sei.SsoServerRealmIndex = ri;

                        oname =
                            sei.createSsoServerSAML2IDPEntryObjectName(server);

                        ss2s.incHostedIDPCount();
                        try {
                            iTab.addEntry(sei, oname);
                            if (sei != null) {
                                server.registerMBean(sei, oname);
                            }
                           /* is a Map of realm/saml2idp to index needed? */
                           String rai = realm + "|" + entname;
                           // sei is this bean's instance
                           realmSAML2IDPs.put(rai, sei);
                        } catch (JMException ex) {
                            debug.error(classMethod +
                                "JMEx adding SAMLv2 IDP entity " +
                                entname + " in realm " + realm, ex);
                        } catch (SnmpStatusException ex) {
                            debug.error(classMethod +
                                "SnmpEx adding SAMLv2 IDP entity " +
                                entname + " in realm " + realm, ex);
                        }
                    }
                    if (((roles.indexOf("IDP")) >= 0) &&
                        loc.equalsIgnoreCase("remote"))
                    {
                        ss2s.incRemoteIDPCount();
                    }

                    if (((roles.indexOf("SP")) >= 0) &&
                        loc.equalsIgnoreCase("hosted"))
                    {
                        if (sTab == null) {
                            continue;
                        }
                        SsoServerSAML2SPEntryImpl sei =
                            new SsoServerSAML2SPEntryImpl(mib2);
                        sei.SAML2SPInvalidArtifactsRcvd = new Long(0);
                        sei.SAML2SPValidAssertionsRcvd = new Long(0);
                        sei.SAML2SPRqtsSent = new Long(0);
                        sei.SAML2SPName = getEscapedString(entname);
                        sei.SsoServerRealmIndex = ri;
                        sei.SAML2SPIndex = new Integer(spi++);

                        oname =
                            sei.createSsoServerSAML2SPEntryObjectName(server);
                        try {
                            sTab.addEntry(sei, oname);
                            if (sei != null) {
                                server.registerMBean(sei, oname);
                            }
                           /* is a Map of realm/saml2sp to index needed? */
                           String rai = realm + "|" + entname;
                           // sei is this bean's instance
                               realmSAML2SPs.put(rai, sei);
                        } catch (JMException ex) {
                            debug.error(classMethod +
                                "JMEx adding SAMLv2 SP entity " +
                                entname + " in realm " + realm, ex);
                        } catch (SnmpStatusException ex) {
                            debug.error(classMethod +
                                "SnmpEx adding SAMLv2 SP entity " +
                                entname + " in realm " + realm, ex);
                        }
                    }

                    if (debug.messageEnabled()) {
                        sb.append("    name=").append(entname).
                            append(", loc=").append(loc).append(", roles=").
                            append(roles).append("\n");
                    }
                }
            } else {
                if (debug.messageEnabled()) {
                    sb.append("no entries\n");
                }
            }

            /*
             *  the WSFed entities map:
             *    entity name -> hashmap of:
             *      key="location"; value="hosted" or "remote"
             *      key="roles"; value=some combo of IDP;SP
             */
            if (debug.messageEnabled()) {
                sb.append("\n  WSFed entities map has ");
            }

            if ((wsEnts != null) && (wsEnts.size() > 0)) {
                if (debug.messageEnabled()) {
                    sb.append(wsEnts.size()).append(" entries:\n");
                }

                Set ks = wsEnts.keySet();
                for (Iterator it = ks.iterator(); it.hasNext(); ) {
                    String entname = (String)it.next();
                    HashMap hm = (HashMap)wsEnts.get(entname);
                    String loc = (String)hm.get("location");
                    String roles = (String)hm.get("roles");

                    SsoServerFedEntitiesEntryImpl cei =
                        new SsoServerFedEntitiesEntryImpl(mib2);
                    cei.SsoServerRealmIndex = ri;
                    cei.FedEntityName = getEscapedString(entname);
                    cei.FedEntityIndex = new Integer(tabinx++);
                    cei.FedEntityProto = "WSFed";
                    cei.FedEntityType = roles;
                    cei.FedEntityLoc = loc;
                    ObjectName oname =
                        cei.createSsoServerFedEntitiesEntryObjectName(server);

                    if (oname == null) {
                        debug.error(classMethod +
                            "Error creating object for WSFed Entity '" +
                            entname + "'");
                        continue;
                    }

                    try {
                        ftab.addEntry(cei, oname);
                        if (cei != null) {
                            server.registerMBean(cei, oname);
                        }
                    } catch (JMException ex) {
                        debug.error(classMethod + "JMEx adding WSFed entity " +
                            entname + " in realm " + realm, ex);
                    } catch (SnmpStatusException ex) {
                        debug.error(classMethod +
                            "SnmpEx adding WSFed entity " +
                            entname + " in realm " + realm, ex);
                    }
                    sb.append("    name=").append(entname).append(", loc=").
                        append(loc).append(", roles=").append(roles).
                        append("\n");
                }
            } else {
                if (debug.messageEnabled()) {
                    sb.append("no entries\n");
                }
            }

            /*
             *  the IDFF entities map:
             *    entity name -> hashmap of:
             *      key="location"; value="hosted" or "remote"
             *      key="roles"; value=some combo of IDP;SP
             */
            if (debug.messageEnabled()) {
                sb.append("\n  IDFF entities map has ");
            }

            if ((idffEnts != null) && (idffEnts.size() > 0)) {
                if (debug.messageEnabled()) {
                    sb.append(idffEnts.size()).append(" entries:\n");
                }

                Set ks = idffEnts.keySet();
                for (Iterator it = ks.iterator(); it.hasNext(); ) {
                    String entname = (String)it.next();
                    HashMap hm = (HashMap)idffEnts.get(entname);

                    String loc = (String)hm.get("location");
                    String roles = (String)hm.get("roles");

                    SsoServerFedEntitiesEntryImpl cei =
                        new SsoServerFedEntitiesEntryImpl(mib2);
                    cei.SsoServerRealmIndex = ri;
                    cei.FedEntityName = getEscapedString(entname);
                    cei.FedEntityIndex = new Integer(tabinx++);
                    cei.FedEntityProto = "IDFF";
                    cei.FedEntityType = roles;
                    cei.FedEntityLoc = loc;
                    ObjectName oname =
                        cei.createSsoServerFedEntitiesEntryObjectName(server);

                    if (oname == null) {
                        debug.error(classMethod +
                            "Error creating object for IDFF Entity '" +
                            entname + "'");
                        continue;
                    }

                    try {
                        ftab.addEntry(cei, oname);
                        if (cei != null) {
                            server.registerMBean(cei, oname);
                        }
                    } catch (JMException ex) {
                        debug.error(classMethod + "JMEx adding IDFF entity " +
                            entname + " in realm " + realm, ex);
                    } catch (SnmpStatusException ex) {
                        debug.error(classMethod +
                            "SnmpEx adding IDFF entity " +
                            entname + " in realm " + realm, ex);
                    }
                    if (debug.messageEnabled()) {
                        sb.append("    name=").append(entname).
                            append(", loc=").append(loc).append(", roles=").
                            append(roles).append("\n");
                    }
                }
            } else {
                if (debug.messageEnabled()) {
                    sb.append("no entries\n");
                }
            }
        } else {
            debug.error(classMethod +
                "FederationEntities table is null");
        }

        /*
         *  the COT members map:
         *    cot name -> hashmap of:
         *      key="SAML"; value=Set of member names
         *      key="IDFF"; value=Set of member names
         *      key="WSFed"; value=Set of member names
         */
        if (debug.messageEnabled()) {
            sb.append("\n  COT Members map has ");
        }

        if ((cotMembs != null) && (cotMembs.size() > 0)) {
            if (debug.messageEnabled()) {
                sb.append(cotMembs.size()).append(" entries:\n");
            }
            Set ks = cotMembs.keySet();
            int coti = 1;
            TableSsoServerFedCOTMemberTable mtab = null;
            try {
                mtab = ssfc.accessSsoServerFedCOTMemberTable();
            } catch (SnmpStatusException ex) {
                debug.error(classMethod +
                    "getting fed COT members table: ", ex);
            }
            for (Iterator it = ks.iterator(); it.hasNext(); ) {
                String cotname = (String)it.next();
                HashMap hm = (HashMap)cotMembs.get(cotname);
                cotname = getEscapedString(cotname);

                if (debug.messageEnabled()) {
                    sb.append("  COT name = ").append(cotname).
                        append(", SAML members = ");
                }

                Set fset = (Set)hm.get("SAML");
                int mi = 1;
                Integer cotI = new Integer(coti++);
                if ((fset != null) && fset.size() > 0) {
                    for (Iterator its = fset.iterator(); its.hasNext(); ) {
                        String mbm = (String)its.next();

                        if (debug.messageEnabled()) {
                            sb.append("    ").append(mbm).append("\n");
                        }

                        SsoServerFedCOTMemberEntryImpl cmi =
                            new SsoServerFedCOTMemberEntryImpl(mib2);
                        cmi.FedCOTMemberType = "SAMLv2";
                        cmi.FedCOTMemberName = getEscapedString(mbm);
                        cmi.FedCOTMemberIndex = new Integer(mi++);
                        cmi.SsoServerRealmIndex = ri;
                        cmi.FedCOTIndex = cotI;  // xxx - need to get from tbl
                        ObjectName ceName = 
                            cmi.createSsoServerFedCOTMemberEntryObjectName(
                                server);

                        if (ceName == null) {
                            debug.error(classMethod +
                                "Error creating object for SAMLv2 COT Member '"+
                                mbm + "'");
                            continue;
                        }

                        try {
                            mtab.addEntry(cmi, ceName);
                            if (ceName != null) {
                                server.registerMBean(cmi, ceName);
                            }
                        } catch (Exception ex) {
                            debug.error(classMethod + "cotmember = " +
                                mbm, ex);
                        }
                    }
                } else {
                    if (debug.messageEnabled()) {
                        sb.append("    NONE\n");
                    }
                }

                fset = (Set)hm.get("IDFF");
                if (debug.messageEnabled()) {
                    sb.append("    IDFF members = ");
                }
                if ((fset != null) && fset.size() > 0) {
                    for (Iterator its = fset.iterator(); its.hasNext(); ) {
                        String mbm = (String)its.next();

                        if (debug.messageEnabled()) {
                            sb.append("    ").append(mbm).append("\n");
                        }
                        SsoServerFedCOTMemberEntryImpl cmi =
                            new SsoServerFedCOTMemberEntryImpl(mib2);
                        cmi.FedCOTMemberType = "IDFF";
                        cmi.FedCOTMemberName = getEscapedString(mbm);
                        cmi.FedCOTMemberIndex = new Integer(mi++);
                        cmi.SsoServerRealmIndex = ri;
                        cmi.FedCOTIndex = cotI;  // xxx - need to get from tbl
                        ObjectName ceName = 
                            cmi.createSsoServerFedCOTMemberEntryObjectName(
                                server);

                        if (ceName == null) {
                            debug.error(classMethod +
                                "Error creating object for IDFF COT Member '" +
                                mbm + "'");
                            continue;
                        }

                        try {
                            mtab.addEntry(cmi, ceName);
                            if (ceName != null) {
                                server.registerMBean(cmi, ceName);
                            }
                        } catch (Exception ex) {
                            debug.error(classMethod + "cotmember = " +
                                mbm, ex);
                        }
                    }
                } else {
                    if (debug.messageEnabled()) {
                        sb.append("    NONE\n");
                    }
                }

                fset = (Set)hm.get("WSFed");

                if (debug.messageEnabled()) {
                    sb.append("    WSFed members = ");
                }

                if ((fset != null) && fset.size() > 0) {
                    for (Iterator its = fset.iterator(); its.hasNext(); ) {
                        String mbm = (String)its.next();
                        if (debug.messageEnabled()) {
                            sb.append("    ").append(mbm).append("\n");
                        }
                        SsoServerFedCOTMemberEntryImpl cmi =
                            new SsoServerFedCOTMemberEntryImpl(mib2);
                        cmi.FedCOTMemberType = "WSFed";
                        cmi.FedCOTMemberName = getEscapedString(mbm);
                        cmi.FedCOTMemberIndex = new Integer(mi++);
                        cmi.SsoServerRealmIndex = ri;
                        cmi.FedCOTIndex = cotI;  // xxx - need to get from tbl
                        ObjectName ceName = 
                            cmi.createSsoServerFedCOTMemberEntryObjectName(
                                server);

                        if (ceName == null) {
                            debug.error(classMethod +
                                "Error creating object for WSFed Member '" +
                                mbm + "'");
                            continue;
                        }

                        try {
                            mtab.addEntry(cmi, ceName);
                            if (ceName != null) {
                                server.registerMBean(cmi, ceName);
                            }
                        } catch (Exception ex) {
                            debug.error(classMethod + "cotmember = " +
                                mbm, ex);
                        }
                    }
                } else {
                    if (debug.messageEnabled()) {
                        sb.append("    NONE\n");
                    }
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message(sb.toString());
        }
                
        /*
         *  have to do it here?
         */
        
        if (debug.messageEnabled()) {
            try {
                DSConfigMgr dscm = DSConfigMgr.getDSConfigMgr();
                ServerGroup sgrp = dscm.getServerGroup("sms");
                Collection slist = sgrp.getServersList();
                StringBuilder sbp1 = new StringBuilder("DSConfigMgr:\n");
                for (Iterator it = slist.iterator(); it.hasNext(); ) {
                    Server sobj = (Server)it.next();
                    String svr = sobj.getServerName();
                    int port = sobj.getPort();
                    sbp1.append("  svrname = ").append(svr).
                        append(", port = ").append(port).append("\n");
                }
                debug.message(classMethod + sbp1.toString());
            } catch (Exception d) {
                debug.message(classMethod +
                    "trying to get Directory Server Config");
            }

            Properties props = SystemProperties.getProperties();
            Set kset = props.keySet();
            StringBuilder sbp = new StringBuilder("SYSPROPS:\n");
            for (Iterator it = kset.iterator(); it.hasNext(); ) {
                String entname = (String)it.next();
                String val = (String)props.get(entname);

                sbp.append("  key = ").append(entname).append(", val = ").
                    append(val).append("\n");
            }
            debug.message(classMethod + sbp.toString());

            String dirHost = SystemProperties.get(Constants.AM_DIRECTORY_HOST);
            String dirPort = SystemProperties.get(Constants.AM_DIRECTORY_PORT);
            String drSSL =
                SystemProperties.get(Constants.AM_DIRECTORY_SSL_ENABLED);
            boolean dirSSL = Boolean.valueOf(
                SystemProperties.get(
                    Constants.AM_DIRECTORY_SSL_ENABLED)).booleanValue();

            debug.message(classMethod + "SMS CONFIG:\n    host = " + dirHost +
                "\n    port = " + dirPort + "\n    ssl = " + drSSL +
                "\n    dirSSL = " + dirSSL);

            Date stopDate = new Date();
            String stDate = sdf.format(startDate);
            String endDate = sdf.format(stopDate);
            debug.message("Agent.federationConfig:\n    Start Time = " +
                stDate + "\n      End Time = " + endDate);
        }
        return 0;
    }

    private static String getEscapedString (String str) {
        if (str != null) {
            if (str.indexOf(":") >= 0) {
                str = str.replaceAll(":", "&#58;");
            }
        }
        return (str);
    }

    public static String getRealmNameFromIndex (Integer index) {
        return ((String)index2Realm.get(index));
    }

    public static String getEscRealmNameFromIndex (Integer index) {
        String ss = (String)index2Realm.get(index);
        return (getEscapedString(ss));
    }

    public static Integer getRealmIndexFromName (String name) {
        return ((Integer)realm2Index.get(name));
    }

    public static String getRealmNameFromDN(String rlmDN) {
        return (String)DN2Realm.get(rlmDN);
    }

    public static SsoServerAuthModulesEntryImpl getAuthModuleEntry (
        String rlmAuthInst)
    {
        return (
            (SsoServerAuthModulesEntryImpl)realmAuthInst.get(rlmAuthInst));
    }

    public static SSOServerInfo getAgentSvrInfo() {
        return (agentSvrInfo);
    }

    public static SsoServerSAML2IDPEntryImpl getSAML2IDPEntry (
        String rlmSAMLIDP)
    {
        return ((SsoServerSAML2IDPEntryImpl)realmSAML2IDPs.get(rlmSAMLIDP));
    }

    public static SsoServerSAML2SPEntryImpl getSAML2SPEntry (
        String rlmSAMLSP)
    {
        return ((SsoServerSAML2SPEntryImpl)realmSAML2SPs.get(rlmSAMLSP));
    }

    public static void setSFOStatus (boolean sfoStatus) {
        isSessFOEnabled = sfoStatus;
    }

    public static boolean getSFOStatus() {
        return isSessFOEnabled;
    }

    public static void setMonitoringDisabled () {
        monitoringEnabled = false;
        agentStarted = false; // so Agent.isRunning() is false
    }

    /**
     * Main entry point.
     * When calling the program, you can specify:
     *  1) nb_traps: number of traps the SNMP agent will send.
     * If not specified, the agent will send traps continuously.
     */
    public static void main(String args[]) {
        
        final MBeanServer server;
        final ObjectName htmlObjName;
        final ObjectName snmpObjName;
        final ObjectName mibObjName;
        final ObjectName trapGeneratorObjName;
        int htmlPort = 8082;
        int snmpPort = 11161;

        // Parse the number of traps to be sent.
        //
        if ((args.length != 0) && (args.length != 1)) {
            usage();
            java.lang.System.exit(1);
        } else if (args.length == 1) {
            try {
                nbTraps = (new Integer(args[0])).intValue();
                if (nbTraps < 0) {
                    usage();
                    java.lang.System.exit(1);
                }
            } catch (java.lang.NumberFormatException e) {
                usage();
                java.lang.System.exit(1);
            }
        }
    
        try {
            ArrayList servers = MBeanServerFactory.findMBeanServer(null);
            if ((servers != null) && !servers.isEmpty()) {
                server = (MBeanServer)servers.get(0);
            } else {
                server = MBeanServerFactory.createMBeanServer();
            }
            String domain = server.getDefaultDomain();

            // Create and start the HTML adaptor.
            //
            htmlObjName = new ObjectName(domain +
                   ":class=HtmlAdaptorServer,protocol=html,port=" + htmlPort);
            println("Adding HTML adaptor to MBean server with name \n    " +
                    htmlObjName);
            println("NOTE: HTML adaptor is bound on TCP port " + htmlPort);
            HtmlAdaptorServer htmlAdaptor = new HtmlAdaptorServer(htmlPort);
            server.registerMBean(htmlAdaptor, htmlObjName);
            htmlAdaptor.start();
                  
            //
            // SNMP specific code:
            //
      
            // Create and start the SNMP adaptor.
            // Specify the port to use in the constructor. 
            // If you want to use the standard port (161) comment out the 
            // following line:
            //   snmpPort = 8085;
            //
            snmpPort = 11161;
            snmpObjName = new ObjectName(domain + 
                  ":class=SnmpAdaptorServer,protocol=snmp,port=" + snmpPort);
            println("Adding SNMP adaptor to MBean server with name \n    " +
                    snmpObjName);
            println("NOTE: SNMP Adaptor is bound on UDP port " + snmpPort);
            snmpAdaptor = new SnmpAdaptorServer(snmpPort);
            server.registerMBean(snmpAdaptor, snmpObjName);
            snmpAdaptor.start();

            // Send a coldStart SNMP Trap. 
            // Use port = snmpPort+1.
            //
            print("NOTE: Sending a coldStart SNMP trap" + 
                  " to each destination defined in the ACL file...");
            snmpAdaptor.setTrapPort(new Integer(snmpPort+1));
            snmpAdaptor.snmpV1Trap(0, 0, null);
            println("Done.");

            // Create an RMI connector and start it
            try {
                JMXServiceURL url =
                    new JMXServiceURL(
                        "service:jmx:rmi:///jndi/rmi://localhost:9999/server");
                JMXConnectorServer cs =
                    JMXConnectorServerFactory.newJMXConnectorServer(
                        url, null, server);
                cs.start();
            } catch (Exception ex) {
                System.out.println(
                    "Error starting RMI : execute rmiregistry 9999; ex="+ex);
            }
      
            // Create the MIB II (RFC 1213) and add it to the MBean server.
            //
            mibObjName= new ObjectName("snmp:class=SUN_OPENSSO_SERVER_MIB");
            println(
                "Adding SUN_OPENSSO_SERVER_MIB-MIB to MBean server with name" +
                "\n    " + mibObjName);

            // Create an instance of the customized MIB
            //
            SUN_OPENSSO_SERVER_MIB mib2 = new SUN_OPENSSO_SERVER_MIB();
            server.registerMBean(mib2, mibObjName);
      
            // Bind the SNMP adaptor to the MIB in order to make the MIB 
            // accessible through the SNMP protocol adaptor.
            // If this step is not performed, the MIB will still live in 
            // the Java DMK agent:
            // its objects will be addressable through HTML but not SNMP.
            //
            mib2.setSnmpAdaptor(snmpAdaptor);

            // Create a LinkTrapGenerator.
            // Specify the ifIndex to use in the object name.
            //
            String trapGeneratorClass = "LinkTrapGenerator";
            int ifIndex = 1;
            trapGeneratorObjName = new ObjectName("trapGenerator" + 
                              ":class=LinkTrapGenerator,ifIndex=" + ifIndex);
            println("Adding LinkTrapGenerator to MBean server with name" +
                "\n    " + trapGeneratorObjName);
            LinkTrapGenerator trapGenerator = new LinkTrapGenerator(nbTraps);
            server.registerMBean(trapGenerator, trapGeneratorObjName);

            println("\n>> Press <Enter> if you want to start sending traps.");
            println("   -or-");
            println(">> Press <Ctrl-C> if you want to stop this agent.");
            java.lang.System.in.read();
            
            trapGenerator.start();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Return a reference to the SNMP adaptor server.
     */
    public static SnmpAdaptorServer getSnmpAdaptor() {
        return snmpAdaptor;
    }
    
    /**
     * Return usage of the program.
     */
    public static void  usage() {
        println("java Agent <nb_traps>");
        println("where");
        println("    -nb_traps: " + 
                "number of traps the SNMP agent will send.");
        println("              " + 
                "If not specified, the agent will send traps continuously.");
    }

    /**
     * print/println stuff...
     */
    private final static void println(String msg) {
        java.lang.System.out.println(msg);
    }
    private final static void print(String msg) {
        java.lang.System.out.print(msg);
    }
}
