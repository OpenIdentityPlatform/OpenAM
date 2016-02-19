/*
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
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 * Portions Copyrighted 2014 Nomura Research Institute, Ltd.
 */

package com.iplanet.dpro.session.service.cluster;

import static org.forgerock.openam.utils.Time.*;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.forgerock.openam.utils.IOUtils;

/**
 * The <code>ClusterStateService</code> monitors the state of Server instances
 * that are part of the current Site. It also monitors the state of remote
 * Sites via the same mechanism.
 *
 * This functionality is used as part of making "internal request routing"
 * requests.
 *
 * Note: This service will monitor all provided Sites regardless of whether they are
 * the same Site as the current Server.
 */
public class ClusterStateService extends GeneralTaskRunnable {

    // Inner Class definition of StateInfo Object.
    // Contains information about each Server.
    private class StateInfo implements Comparable {
        String id;

        String protocol;

        URL url;

        InetSocketAddress address;

        boolean isUp;

        boolean isLocal;

        public int compareTo(Object o) {
            return id.compareTo(((StateInfo) o).id);
        }

        /**
         * toString Override.
         * @return String representation of this Inner Object Class.
         */
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("StateInfo ID:[").append(this.id).append("], ");
            sb.append("Protocol:[").append(this.protocol).append("], ");
            sb.append("URL:[").append(((this.url == null) ? "null" : this.url.toString()));
            sb.append("], ");
            sb.append("Address:[");
            if (this.address == null)
                { sb.append("null], "); }
            else
            {
                sb.append(this.address.toString()).append("], Unresolved:[");
                sb.append(this.address.isUnresolved()).append("], ");
            }
            sb.append("Local:[").append(this.isLocal).append("], ");
            sb.append("Up:[").append(this.isUp).append("].\n");
            return sb.toString();
        }
    } //. End of Inner Class Definition.

    /**
     * Service Globals
     */
    public static Debug sessionDebug = null;

    /**
     * Servers in the cluster environment
     */
    private static Map<String, StateInfo> servers = new HashMap<String, StateInfo>();

    /**
     * Sites in the environment
     */
    private static Map<String, StateInfo> sites = new HashMap<String, StateInfo>();

    /**
     * Sites and Servers that are currently down.
     */
    private static Set<String> down = new HashSet<String>();

    /**
     * Server Information
     */
    private static StateInfo[] serverSelectionList = new StateInfo[0];

    /**
     * Last selected Server
     */
    private static int lastSelected = -1;

    /**
     * individual server wait default time out 10 milliseconds
     */
    public static final int DEFAULT_TIMEOUT = 1000;

    private static boolean doRequest = true;
    private static final String doRequestFlag = SystemProperties.
            get(Constants.URLCHECKER_DOREQUEST, "false");

    private int timeout = DEFAULT_TIMEOUT; // in milliseconds

    /**
     * default StateInfo check time 10 milliseconds
     */
    public static final long DEFAULT_PERIOD = 1000;

    private static long period = DEFAULT_PERIOD; // in milliseconds

    // server instance id 
    private static String localServerId = null;

    // SessionService
    private static volatile SessionService sessionService = null;

    static {
        sessionDebug = Debug.getInstance("amSession");

        if (doRequestFlag != null) {
            doRequest = !doRequestFlag.equals("false");
        }
    }

    /**
     * Get Servers within Cluster
     * @return Map<String, StateInfo>
     */
    protected Map<String, StateInfo> getServers() {
        return servers;
    }

    /**
     * Get Server and Site IDs which are in a Down State.
     *
     * @return Possibly empty set of IDs.
     */
    protected Set<String> getDownServersAndSites() {
        return down;
    }

    /**
     * Get the Server Selection List, common to all Servers
     * in Cluster.
     * @return StateInfo[] Array of Servers in Selection list in
     *         proper order.
     */
    protected StateInfo[] getServerSelectionList() {
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
     * @return boolean indicating true if the serverId matches the local server ID.
     */
    protected boolean isLocalServerId(String serverId) {
        return (serverId != null && serverId.equalsIgnoreCase(localServerId));
    }

    /**
     * Constructs an instance for the cluster service
     * @param localServerId id of the server instance in which this
     *                      ClusterStateService instance is running
     * @param timeout timeout for waiting on an individual server (millisec)
     * @param period checking cycle period (millisecs)
     * @param serverMembers map of Server ID to URL for all cluster Server members.
     * @param siteMembers Mapping of Site ID to URL for all Sites.
     * @throws Exception If there was an unexpected error initialising the ClusterStateService.
     */
    protected ClusterStateService(SessionService sessionService, String localServerId,
                                  int timeout, long period, Map<String, String> serverMembers,
                                  Map<String, String> siteMembers) throws Exception {
        if ( (localServerId == null)||(localServerId.isEmpty()) )
        {
            String message = "ClusterStateService: Local Server Id argument is null, unable to instantiate Cluster State Service!";
            sessionDebug.error(message);
            throw new IllegalArgumentException(message);
        }
        // Ensure we Synchronize this Instantiation.
        synchronized (this) {
            this.sessionService = sessionService;
            this.localServerId = localServerId;
            this.timeout = timeout;
            this.period = period;

            serverSelectionList = new StateInfo[serverMembers.size() + siteMembers.size()];

            for (Map.Entry<String, String> entry : serverMembers.entrySet()) {
                populateMap(servers, getServerInfo(entry.getKey(), entry.getValue()));
            }

            for (Map.Entry<String, String> entry : siteMembers.entrySet()) {
                populateMap(sites, getServerInfo(entry.getKey(), entry.getValue()));
            }

            // to ensure that ordering in different server instances is identical
            Arrays.sort(serverSelectionList);
            SystemTimer.getTimer().schedule(this, new Date((
                    currentTimeMillis() / 1000) * 1000));
        } // End of Synchronized Block.
    }

    /**
     * Generates a StateInfo instance based on the ServerID and ServerURL.
     *
     * @param serverId Non null.
     * @param serverUrl Non null.
     * @return A non null StateInfo instance.
     * @throws MalformedURLException
     */
    private StateInfo getServerInfo(String serverId, String serverUrl) throws MalformedURLException {
        StateInfo info = new StateInfo();
        info.id = serverId;
        URL url = new URL(serverUrl + "/namingservice");
        info.url = url;
        info.protocol = url.getProtocol();
        info.address = new InetSocketAddress(url.getHost(), url.getPort());

        // Fix for Deadlock. If this is our server, set to true, else false.
        info.isUp = isLocalServerId(info.id);
        info.isLocal = info.isUp; // Set our Local Server Indicator, per above interrogation.

        if (sessionDebug.messageEnabled()) {
            sessionDebug.error("Added Server to ClusterStateService: " + info.toString());
        }

        return info;
    }

    /**
     * Populates the StateInfo into the required data structures for this class.
     * @param map Non null map to store the StateInfo in.
     * @param stateInfo Non null StateInfo to store.
     */
    private void populateMap(Map<String, StateInfo> map, StateInfo stateInfo) {
        String serverId = stateInfo.id;

        // Add Server to Server List.
        map.put(serverId, stateInfo);

        // Check for Down Servers.
        if (!stateInfo.isUp) {
            down.add(serverId);
        }

        // Associate to a Server Selection Bucket.
        serverSelectionList[getNextSelected()] = stateInfo;
    }

    /**
     * Implements "wrap-around" lastSelected index advancement
     *
     * @return updated lastSelected index value
     */
    private int getNextSelected() {
        return lastSelected = (lastSelected + 1) % serverSelectionList.length;
    }

    /**
     * Returns currently known status of the server instance identified by
     * serverId
     *
     * @param serverId server instance id
     * @return true if server is up, false otherwise
     */
    boolean isUp(String serverId) {
        return isUp(servers, serverId);
    }

    /**
     * Indicates the state of a given Site ID.
     *
     * This method functions in the same way as {@link ClusterStateService#isUp(String)}
     * however, it actively validates that the ID provided is a Site ID.
     *
     *
     */
    public boolean isSiteUp(String siteId) {
        return isUp(sites, siteId);
    }

    private boolean isUp(Map<String, StateInfo> map, String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        if (map == null || map.isEmpty()) {
            return false;
        }
        return map.get(id) != null ? map.get(id).isUp : false;
    }

    /**
     * Actively checks and updates the status of the server instance identified
     * by serverId
     *
     * @param serverId server instance id
     * @return true if server is up, false otherwise
     */
    boolean checkServerUp(String serverId) {
        if ( (serverId == null) || (serverId.isEmpty()) ) {
            return false;
        }
        if (serverId.equalsIgnoreCase(localServerId)) {
            return true;
        }
        if ( (servers == null) || servers.isEmpty() )
            { return false; }
        StateInfo info = servers.get(serverId);
        info.isUp = checkServerUp(info);
        return info.isUp;
    }

    /**
     * Returns size of the server list
     *
     * @return size of the server list
     */
    int getServerSelectionListSize() {
        return (serverSelectionList == null) ? 0 : serverSelectionList.length;
    }

    /**
     * Returns server id for a given index inside the server list
     * or null if out of bounds.
     * @param index index in the server list, relative to Zero.
     * @return server id
     */
    String getServerSelection(int index) {
        if ( (getServerSelectionListSize() <= 0) || (index < 0) || (index >= getServerSelectionListSize()) ) {
            return null;
        }
        return serverSelectionList[index].id;
    }

    /**
     * Implements for GeneralTaskRunnable
     *
     * @return The run period of the task.
     */
    public long getRunPeriod() {
        return period;
    }

    /**
     * Implements for GeneralTaskRunnable.
     *
     * @return false since this class will not be used as container.
     */
    public boolean addElement(Object obj) {
        return false;
    }

    /**
     * Implements for GeneralTaskRunnable.
     *
     * @return false since this class will not be used as container.
     */
    public boolean removeElement(Object obj) {
        return false;
    }

    /**
     * Implements for GeneralTaskRunnable.
     *
     * @return true since this class will not be used as container.
     */
    public boolean isEmpty() {
        return true;
    }

    /**
     * Monitoring logic used by background thread
     */
    public void run() {
        try {
            boolean cleanRemoteSessions = false;
            synchronized (this) {

                Collection<StateInfo> infos = new ArrayList<StateInfo>();
                infos.addAll(servers.values());
                infos.addAll(sites.values());

                for (StateInfo info : infos) {
                    info.isUp = checkServerUp(info);

                    if (!info.isUp) {
                        down.add(info.id);
                    } else {
                        if (!down.isEmpty() && down.remove(info.id)) {
                            cleanRemoteSessions = true;
                        }
                    }
                }

            }
            if (cleanRemoteSessions) {
                sessionService.cleanUpRemoteSessions();
            }
        } catch (Exception ex) {
            sessionDebug.error("cleanRemoteSessions Background thread has encountered an Exception: " + ex.getMessage(), ex);
        }
    }

    /**
     * Internal method for checking health status using sock.connect()
     * <p/>
     * TODO -- Use a better mechanism for alive status. 10.1+.
     *
     * @param info server info instance
     * @return true if server is up, false otherwise
     */
    private boolean checkServerUp(StateInfo info) {
        if (info == null) {
            return false;
        }
        if (localServerId.equals(info.id)) {
            return true;
        }

        boolean result = false;
        Socket sock = null;
        InputStream is = null;

        try {
            /*
             * If we need to check for a front end proxy, we need
             * to send a request.  
             */
            if (!doRequest) {
                sock = new Socket();
                sock.connect(info.address, timeout);
                result = true;
            } else {
                HttpURLConnection connection = null;
                int responseCode = 0;

                try {
                    connection = (HttpURLConnection) info.url.openConnection();
                    connection.setConnectTimeout(timeout);
                    connection.setReadTimeout(timeout);

                    if (connection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) connection).setHostnameVerifier(new HostnameVerifier() {

                            @Override
                            public boolean verify(String hostname, SSLSession session) {
                                return true;
                            }
                        });
                    }
                    is = connection.getInputStream();
                    responseCode = connection.getResponseCode();
                    readStream(is);
                } catch (IOException ioe) {
                    if (connection != null) {
                        readStream(connection.getErrorStream());
                    }
                }
                result = responseCode == HttpURLConnection.HTTP_OK;
            }
        } catch (Exception ex) {
            result = false;
        } finally {
            if (sock != null) {
                try {
                    sock.close();
                } catch (IOException ioe) {
                    //ignored
                }
            }
            IOUtils.closeIfNotNull(is);
        }
        return result;
    }

    private void readStream(InputStream is) {
        if (is != null) {
            byte[] buf = new byte[512];
            try {
                while (is.read(buf) > 0) {
                    // do nothing
                }
            } catch (IOException ioe) {
                //ignore
            } finally {
                IOUtils.closeIfNotNull(is);
            }
        }
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("ClusterStateService: ");
        sb.append("{ lastSelected=").append(lastSelected);
        sb.append(", timeout=").append(timeout).append("\n");
        sb.append(" Current Server Selection List:").append("\n");
        for (StateInfo stateInfo : getServerSelectionList()) {
            sb.append(stateInfo.toString());
        }
        sb.append('}');
        return sb.toString();
    }
}
