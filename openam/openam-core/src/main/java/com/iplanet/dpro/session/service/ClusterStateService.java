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
 * $Id: ClusterStateService.java,v 1.3 2008/06/25 05:41:30 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */

package com.iplanet.dpro.session.service;

import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * A <code>ClusterStateService </code> class implements monitoring the state of 
 * server instances that are participating in the cluster environment. It is 
 * used in making routing decisions in "internal request routing" mode
 * 
 */
public class ClusterStateService extends GeneralTaskRunnable {

    // Inner Class definition of ServerInfo Object.
    // Contains information about each Server.
    private class ServerInfo implements Comparable {
        String id;
        
        String protocol;
        
        URL url;

        InetSocketAddress address;

        boolean isUp;

        boolean isLocal;

        @Override
        public int compareTo(Object o) {
            return id.compareTo(((ServerInfo) o).id);
        }
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ServerInfo ID:["+this.id+"], ");
            sb.append("Protocol:["+this.protocol+"], ");
            sb.append("URL:["+((this.url==null)?"null], ":this.url.toString())+"], ");
            sb.append("Address:["+((this.address==null)?"null], ":this.address.toString()+"], Resolved:["+
                    (!this.address.isUnresolved()?"true":"false"))+"], \n");
            sb.append("Local:["+this.isLocal+"\n");
            sb.append("Up:["+this.isUp+"].\n");
            return sb.toString();
        }
    } //. End of Inner Class Definition.

    /**
     * Service Globals
     */
    public static Debug sessionDebug = null;

    /** Servers in the cluster environment */
    private static final Map<String, ServerInfo> servers =
            new HashMap<String, ServerInfo>();
    
    /** Servers are down in the cluster environment*/
    private static Set<String> downServers = new HashSet<String>();

    /** Server Information */
    private static ServerInfo[] serverSelectionList = new ServerInfo[0];

    /** Last selected Server*/
    private static final int lastSelectedNone = -1;
    private int lastSelected = lastSelectedNone;

    /** individual server wait default time out 10 milliseconds */
    public static final int DEFAULT_TIMEOUT = 1000;

    private static String GET_REQUEST = "";
    private final static String EMPTY_STRING = "";
    private final static String SUCCESS_200 = "200";
    private final static String HTTP = "http";

    private static String hcPath = SystemProperties.
                                    get(Constants.URLCHECKER_TARGET_URL, null);

    private static boolean doRequest = true;
    private static final String doRequestFlag = SystemProperties.
                                    get(Constants.URLCHECKER_DOREQUEST, "false");

    private int timeout = DEFAULT_TIMEOUT; // in milliseconds

    /** default ServerInfo check time 10 milliseconds */
    public static final long DEFAULT_PERIOD = 1000;

    private static long period = DEFAULT_PERIOD; // in milliseconds

    // server instance id 
    private static String localServerId = null;
    
    // SessionService
    private static volatile SessionService sessionService = null;

    // Static Initialization Stanza.
    static {
        sessionDebug = Debug.getInstance("amSession");

        if (doRequestFlag != null) {
            if (doRequestFlag.equals("false"))
                doRequest = false;
        }
        if (hcPath == null) {
            String deployuri = SystemProperties.get
                (Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR, "/openam");
            hcPath = deployuri + "/namingservice";
            if (!hcPath.startsWith("/")) {
                hcPath += "/" + hcPath;
            }

            GET_REQUEST = "GET " + hcPath + " HTTP/1.0";
        }
    }  // End of Static Initialization Stanza

    /**
     * Get Servers within Cluster
     * @return Map<String, ServerInfo>
     */
    protected Map<String, ServerInfo> getServers() {
        return servers;
    }

    /**
     * Get Server IDs which are in a Down State.
     * @return Set<String>
     */
    protected Set<String> getDownServers() {
        return downServers;
    }
    /**
     * Get the Server Selection List, common to all Servers
     * in Cluster.
     * @return ServerInfo[] Array of Servers in Selection list in
     * proper order.
     */
    protected ServerInfo[] getServerSelectionList() {
        return serverSelectionList;
    }

    /**
     * Get our Local Server Id
     * @return String of Local Server Id.
     */
    protected String getLocalServerId() {
        return localServerId;
    }

    /**
     * Is Specified ServerId our Local Server Id?
     * @param serverId
     * @return boolean indicating true if specified server id is our local Server Id or false ir not.
     */
    protected boolean isLocalServerId(String serverId) {
        return ((serverId==null)||(!serverId.equalsIgnoreCase(this.localServerId)))?false:true;
    }

    /**
     * Constructs an instance for the cluster service
     * @param localServerId id of the server instance in which this 
     * ClusterStateService instance is running
     * @param timeout
     *            timeout for waiting on an individual server (millisec)
     * @param period
     *            checking cycle period (millisecs)
     * @param members
     *            map if server id - > url for all cluster members
     * @throws Exception
     */
    protected ClusterStateService(SessionService sessionService, String localServerId,
            int timeout, long period, Map<String, String> members) throws Exception {
        this.sessionService = sessionService;
        this.localServerId = localServerId;
        this.timeout = timeout;
        this.period = period;
        serverSelectionList = new ServerInfo[members.size()];
        
        for (Map.Entry<String, String> entry : members.entrySet()) {
            ServerInfo info = new ServerInfo();
            info.id = entry.getKey();
            URL url = new URL(entry.getValue() + "/namingservice");
            info.url = url;
            info.protocol = url.getProtocol();
            info.address = new InetSocketAddress(url.getHost(), url.getPort());
            // Fix for Deadlock. If this is our server, set to true, else false.
            info.isUp = localServerId.equalsIgnoreCase(info.id) ? true : false;
            info.isLocal = info.isUp; // Set our Local Server Indicator.
            
            if (!info.isUp) {
                downServers.add(info.id);
            }
            
            servers.put(info.id, info);
            serverSelectionList[getNextSelected()] = info;
            sessionDebug.error("Adding Server to ClusterStateService: "+info.toString());

            // TODO - Persist the Server to the Directory in ou=amsessiondb container.

        }

        // to ensure that ordering in different server instances is identical
        Arrays.sort(serverSelectionList);
        SystemTimer.getTimer().schedule(this, new Date((
            System.currentTimeMillis() / 1000) * 1000));
    }

    /**
     * Implements "wrap-around" lastSelected index advancement
     * 
     * @return updated lastSelected index value
     */
    private int getNextSelected() {
        if (lastSelected == lastSelectedNone)
            { return 0;}
        else {
                lastSelected = (lastSelected + 1) % serverSelectionList.length;
            }
        return lastSelected;
     }

    /**
     * Returns currently known status of the server instance identified by
     * serverId
     * 
     * @param serverId
     *            server instance id
     * @return true if server is up, false otherwise
     */
    boolean isUp(String serverId) {
        if ((serverId == null)||(serverId.isEmpty())||(servers==null))
            { return false; }
        if (serverId.equalsIgnoreCase(this.localServerId))
            { return true; }
        return servers.get(serverId).isUp;
    }

    /**
     * Actively checks and updates the status of the server instance identified
     * by serverId
     * 
     * @param serverId
     *            server instance id
     * @return true if server is up, false otherwise
     */
    boolean checkServerUp(String serverId) {
        if ((serverId == null)||(serverId.isEmpty())||(servers==null))
            { return false; }
        if (serverId.equalsIgnoreCase(this.localServerId))
            { return true; }
        ServerInfo info = servers.get(serverId);
        info.isUp = checkServerUp(info);
        return info.isUp;
    }

    /**
     * Returns size of the server list
     * 
     * @return size of the server list
     */
    int getServerSelectionListSize() {
        return (serverSelectionList==null) ? 0 : serverSelectionList.length;
    }

    /**
     * Returns server id for a given index inside the server list
     * 
     * @param index
     *            index in the server list
     * @return server id
     */
    String getServerSelection(int index) {
        if ( (serverSelectionList == null)||(serverSelectionList.length <= 0)||(index < 0) )
            { return null; }
        return serverSelectionList[index].id;
    }

    /**
     * Implements for GeneralTaskRunnable
     * 
     * @return The run period of the task.
     */
    @Override
    public long getRunPeriod() {
        return period;
    }
    
    /**
     * Implements for GeneralTaskRunnable.
     *
     * @return false since this class will not be used as container.
     */
    @Override
    public boolean addElement(Object obj) {
        return false;
    }
    
    /**
     * Implements for GeneralTaskRunnable.
     *
     * @return false since this class will not be used as container.
     */
    @Override
    public boolean removeElement(Object obj) {
        return false;
    }
    
    /**
     * Implements for GeneralTaskRunnable.
     *
     * @return true since this class will not be used as container.
     */
    @Override
    public boolean isEmpty() {
        return true;
    }
    
    /**
     * Monitoring logic used by background thread
     */
    @Override
    public void run() {
        try {
            boolean cleanRemoteSessions = false;
            synchronized (servers) {
                for (Map.Entry<String, ServerInfo> server : servers.entrySet()) {
                    ServerInfo info = server.getValue();
                    info.isUp = checkServerUp(info);
                    
                    if (!info.isUp) {
                        downServers.add(info.id);
                    } else {
                        if (!downServers.isEmpty() &&
                            downServers.remove(info.id)) {
                            cleanRemoteSessions = true;
                        }
                    }
                }
            }
            if (cleanRemoteSessions) {
                sessionService.cleanUpRemoteSessions();
            }
        } catch (Exception ex) {
            sessionDebug.error("cleanRemoteSessions Background thread has encountered an Exceptin: "+ex.getMessage(),ex);
        }
    }

    /**
     * Internal method for checking health status using sock.connect()
     *
     * TODO -- Use a better mechanism for alive status. 10.1+.
     *
     * @param info
     *            server info instance
     * @return true if server is up, false otherwise
     */
    private boolean checkServerUp(ServerInfo info) {
        if (info == null)
            { return false; }
        if (localServerId.equals(info.id)) {
            return true;
        }

        boolean result = false;
        Socket sock = new Socket();
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            sock.connect(info.address, timeout);
            /*
             * If we need to check for a front end proxy, we need
             * to send a request.  
             */
            if (doRequest) {
                if (info.protocol.equals(HTTP)) {
                    out = new PrintWriter(sock.getOutputStream());
                    in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                    out.println(GET_REQUEST);
                    out.println(EMPTY_STRING);
                    out.flush();

                    String response = in.readLine();

                    if (response.contains(SUCCESS_200)) {
                        result = true;
                    } else {
                        result = false;
                    }
                } else {
                    HttpsURLConnection connection = null;
                    int responseCode = 0;
                    InputStream is = null;

                    try {
                        connection = (HttpsURLConnection) info.url.openConnection();
                        

                        connection.setHostnameVerifier(new HostnameVerifier() {
                            public boolean verify(String hostname, SSLSession session) {
                                return true;
                            }
                        });
                
                        responseCode = connection.getResponseCode();
                        is = connection.getInputStream();
                        int ret = 0;
                        byte[] buf = new byte[512];

                        // clear the stream
                        while ((ret = is.read(buf)) > 0) {
                            // do nothing
                        }

                        // close the inputstream
                        is.close();
                    } catch (IOException ioe) {
                        InputStream es = null;

                        try {
                            es = ((HttpsURLConnection)connection).getErrorStream();
                            int ret = 0;
                            byte[] buf = new byte[512];

                            // read the response body to clear
                            while ((ret = es.read(buf)) > 0) {
                                // do nothing
                            }

                            // close the errorstream
                            es.close();
                        } catch(IOException ex) {
                            // deal with the exception
                        }
                    }
                    
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        result = true;
                    } else {
                        result = false;
                    }
                }
            } else {
                result = true;
            }
        } catch (Exception ex) {
            result = false;
        } finally {
            try {
                sock.close();
            } catch (Exception ex) {
            }
        }
        return result;
    }

    // TODO -- Develop Method to write our Server State to the Session Persistence Store.

    /**
     * Override toString
     *
     * @return
     */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("ClusterStateService: ");
        sb.append("{ lastSelected=").append(lastSelected);
        sb.append(", timeout=").append(timeout).append("\n");
        sb.append(" Current Server Selection List:").append("\n");
        for(ServerInfo serverInfo : getServerSelectionList())
            { sb.append(serverInfo.toString()); }
        sb.append('}');
        return sb.toString();
    }
}
