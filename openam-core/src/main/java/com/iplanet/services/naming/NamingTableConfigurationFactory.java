/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package com.iplanet.services.naming;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Responsible for creating the configuration which is used by WebtopNaming.
 *
 * The objective is to ensure that the configuration is used in an immutable fashion
 * by WebtopNaming, which has a high tendency of leak untyped collections through its
 * API.
 *
 * This class is intentionally gingerly moving code from WebtopNaming to this
 * configuration factory in the attempt to minimise the impact of these changes
 * on what is some of the oldest code in the product.
 *
 * Future direction: Refactor and convert these initialisation methods so that the
 * configuration can be entirely initialsed as final.
 */
public class NamingTableConfigurationFactory {
    private static final Debug debug = Debug.getInstance("amNaming");

    /**
     * The delimiter used to separate server IDs in the service attribute.
     */
    public static final String NODE_SEPARATOR = "|";

    /**
     * Constructs the internal configuration required by WebtopNaming.
     *
     * Thread Safety: This method does not require synchronisation as the result is an immutable
     * data structure.
     *
     * @param namingTable Non null naming table which contains the current server and site configuration.
     * @return Non null NamingTableConfiguration.
     * @throws MalformedURLException TODO
     * @throws ServerEntryNotFoundException TODO
     */
    public NamingTableConfiguration getConfiguration(Hashtable namingTable) throws MalformedURLException, ServerEntryNotFoundException {
        NamingTableConfiguration config = new NamingTableConfiguration();
        config.setNamingTable(namingTable);

        String servers = (String) namingTable.get(Constants.PLATFORM_LIST);

        if (servers != null) {
            StringTokenizer st = new StringTokenizer(servers, ",");
            Set<String> platformServersNEW = new HashSet<>();
            Set<String> lcPlatformServersNEW = new HashSet<>();
            while (st.hasMoreTokens()) {
                String svr = st.nextToken();
                lcPlatformServersNEW.add(svr.toLowerCase());
                platformServersNEW.add(svr);
            }
            config.setPlatformServers(platformServersNEW);
            config.setLcPlatformServers(lcPlatformServersNEW);
        }
        updateServerIdMappings(config);
        updateSiteIdMappings(config);
        updateSiteNameToIDMappings(config);
        updatePlatformServerIDs(config);
        updateLBCookieValueMappings(config);
        updatePlatformSets(config);

        if (debug.messageEnabled()) {
            debug.message("Naming table -> " + namingTable.toString());
            debug.message("Server Id Table -> " + config.serverIdTable.toString());
            debug.message("Site Id Table -> " + config.siteIdTable.toString());
            debug.message("Site Name to Id Table -> " + config.getSiteNameToIdTable().toString());
            debug.message("Platform Servers -> " + config.platformServers.toString());
            debug.message("Platform Server IDs -> "
                    + config.getPlatformServerIDs().toString());
        }

        return config;
    }

    /*
     * This method is to update the servers and their IDs in a seprate
     * hash. It will get updated each time when the naming table gets
     * updated. Note: this table will have all the entries in the
     * naming table but in a reverse order except the platform server
     * list. We can just keep only server ID mappings but we need to
     * exclude each other entry which is there in.
     */
    private static void updateServerIdMappings(NamingTableConfiguration config) {
        Hashtable serverIdTbl = new Hashtable();
        for (Map.Entry<String, String> entry : config.getNamingTable().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if ((key == null) || (value == null)) {
                continue;
            }
            // If the key is server list skip it, since it would
            // have the same value
            if (key.equals(Constants.PLATFORM_LIST)) {
                continue;
            }
            serverIdTbl.put(value, key);
        }

        config.setServerIDTable(serverIdTbl);
    }

    private static void updateSiteIdMappings(NamingTableConfiguration config) {
        Hashtable<String, String> siteIdTbl = new Hashtable<String, String>();
        String serverSet = (String) config.namingTable.get(Constants.SITE_ID_LIST);

        if ((serverSet == null) || (serverSet.length() == 0)) {
            return;
        }

        StringTokenizer tok = new StringTokenizer(serverSet, ",");
        while (tok.hasMoreTokens()) {
            String serverid = tok.nextToken();
            String siteid = serverid;
            int idx = serverid.indexOf(NODE_SEPARATOR);
            if (idx != -1) {
                siteid = serverid.substring(idx + 1, serverid.length());
                serverid = serverid.substring(0, idx);
            }
            siteIdTbl.put(serverid, siteid);
        }

        config.setSiteIdTable(siteIdTbl);
        if (debug.messageEnabled()) {
            debug.message("SiteID table -> " + config.getSiteIDsTable().toString());
        }

        return;
    }

    private static void updateSiteNameToIDMappings(NamingTableConfiguration config) {
        Map siteNameToIdTbl = new HashMap();
        String siteNameToIDs = (String) config.namingTable.get(Constants.SITE_NAMES_LIST);

        if ((siteNameToIDs == null) || (siteNameToIDs.length() == 0)) {
            config.setSiteNameToIdTable(siteNameToIdTbl);
            return;
        }

        StringTokenizer tok = new StringTokenizer(siteNameToIDs, ",");
        while (tok.hasMoreTokens()) {
            String siteNameAndID = tok.nextToken();
            String siteName = siteNameAndID;
            String siteId = siteNameAndID;

            int idx = siteNameAndID.indexOf(NODE_SEPARATOR);
            if (idx != -1) {
                siteId = siteNameAndID.substring(idx + 1, siteNameAndID.length());
                siteName = siteNameAndID.substring(0, idx);
            }
            siteNameToIdTbl.put(siteName, siteId);
        }

        config.setSiteNameToIdTable(siteNameToIdTbl);
        if (debug.messageEnabled()) {
            debug.message("SiteNameToIDs table -> " + config.getSiteNameToIdTable().toString());
        }

        return;
    }

    private static void updatePlatformServerIDs(NamingTableConfiguration config)
            throws MalformedURLException, ServerEntryNotFoundException {
        Iterator it = config.platformServers.iterator();
        Set<String> newPlatformServerIDs = new HashSet<>();
        while (it.hasNext()) {
            String plaformURL = (String) it.next();
            String serverID = getIgnoreCase(config.getServerIDTable(), plaformURL);
            if (serverID !=null && !newPlatformServerIDs.contains(serverID)) {
                newPlatformServerIDs.add(serverID);
            }
        }
        config.setPlatformServerIDs(newPlatformServerIDs);
    }

    private static void updateLBCookieValueMappings(NamingTableConfiguration config) {
        Hashtable lbcookieTbl = new Hashtable();
        String serverSet = (String) config.namingTable.get(
                Constants.SERVERID_LBCOOKIEVALUE_LIST);

        if ((serverSet == null) || (serverSet.length() == 0)) {
            return;
        }

        StringTokenizer tok = new StringTokenizer(serverSet, ",");
        while (tok.hasMoreTokens()) {
            String serverid = tok.nextToken();
            String lbCookieValue = serverid;
            int idx = serverid.indexOf(NODE_SEPARATOR);
            if (idx != -1) {
                lbCookieValue = serverid.substring(idx + 1, serverid.length());
                serverid = serverid.substring(0, idx);
            }
            lbcookieTbl.put(serverid, lbCookieValue);
        }

        config.setLbCookieValuesTable(lbcookieTbl);

        if (debug.messageEnabled()) {
            debug.message("WebtopNaming.updateLBCookieValueMappings():" +
                    "LBCookieValues table -> " + config.getLbCookieValuesTable().toString());
        }

        return;
    }

    private static void updatePlatformSets(NamingTableConfiguration config) {
        Set<String> siteIDSet = new HashSet<String>(config.getSiteNameToIdTable().values());
        Set<String> secondarySiteIDSet = new HashSet<String>();
        Set<String> serverIDSet = new HashSet<String>(config.getPlatformServerIDs());

        for (Map.Entry<String, String> entry : config.siteIdTable.entrySet()) {
            String siteId = entry.getValue();
            if (siteId.indexOf(NODE_SEPARATOR) != -1) {
                StringTokenizer tokenizer = new StringTokenizer(siteId, NODE_SEPARATOR);
                //the first one is always the primary site ID, which we don't need now
                tokenizer.nextToken();
                while (tokenizer.hasMoreTokens()) {
                    secondarySiteIDSet.add(tokenizer.nextToken());
                }
            }
        }
        serverIDSet.removeAll(siteIDSet);
        serverIDSet.removeAll(secondarySiteIDSet);

        config.setSiteIDs(siteIDSet);
        config.setSecondarySiteIDs(secondarySiteIDSet);
        config.setServerIDs(serverIDSet);
    }

    private static String getIgnoreCase(Map<String, String> map, String key) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }


    /**
     * Configuration based on the naming table provided. This configuration is intended to
     * be non-modifiable to ensure that it is used correctly by WebtopNaming.
     */
    public class NamingTableConfiguration {
        private Set<String> platformServers;

        //This is created for ignore case comparison
        private Set<String> lcPlatformServers;
        private Map<String, String> serverIdTable;
        private Map<String, String> siteIdTable;
        private Map siteNameToIdTable;
        private Set<String> platformServerIDs;

        //This is created for storing server id and lbcookievalue mapping
        //key:serverid | value:lbcookievalue
        private Map<String, String> lbCookieValuesTable;
        private Set<String> siteIDs;
        private Set<String> secondarySiteIDs;
        private Set<String> serverIDs;
        private Map<String, String> namingTable;

        /**
         * @return An unmodifiable copy of the naming table for read only access.
         */
        public Map<String, String> getNamingTable() {
            return namingTable;
        }
        public void setNamingTable(Hashtable<String, String> table) {
            namingTable = toUnmodifiableMap(table);
        }


        public Map<String, String> getServerIDTable() {
            return serverIdTable;
        }

        private  void setServerIDTable(Hashtable<String, String> table) {
            serverIdTable = toUnmodifiableMap(table);
        }


        public Set<String> getServerIDs() {
            return serverIDs;
        }

        private  void setServerIDs(Set<String> serverIDs) {
            this.serverIDs = Collections.unmodifiableSet(serverIDs);
        }

        public Set<String> getSiteIDs() {
            return siteIDs;
        }
        private void setSiteIDs(Set<String> siteIDs) {
            this.siteIDs = Collections.unmodifiableSet(siteIDs);
        }

        public Map<String, String> getSiteIDsTable() {
            return siteIdTable;
        }
        private void setSiteIdTable(Hashtable<String, String> table) {
            siteIdTable = toUnmodifiableMap(table);
        }

        public Set<String> getPlatformServers() {
            return platformServers;
        }
        private void setPlatformServers(Set<String> platformServers) {
            this.platformServers = Collections.unmodifiableSet(platformServers);
        }

        public Map<String, String> getSiteNameToIdTable() {
            return siteNameToIdTable;
        }
        private void setSiteNameToIdTable(Map<String, String> siteNameToIdTable) {
            this.siteNameToIdTable = Collections.unmodifiableMap(siteNameToIdTable);
        }

        public Map<String, String> getLbCookieValuesTable() {
            return lbCookieValuesTable;
        }
        private void setLbCookieValuesTable(Hashtable lbCookieValuesTable) {
            this.lbCookieValuesTable = toUnmodifiableMap(lbCookieValuesTable);
        }

        public Set<String> getLcPlatformServers() {
            return lcPlatformServers;
        }
        private void setLcPlatformServers(Set<String> lcPlatformServers) {
            this.lcPlatformServers = Collections.unmodifiableSet(lcPlatformServers);
        }


        public Set<String> getSecondarySiteIDs() {
            return secondarySiteIDs;
        }
        private void setSecondarySiteIDs(Set<String> secondarySiteIDs) {
            this.secondarySiteIDs = Collections.unmodifiableSet(secondarySiteIDs);
        }

        public Set<String> getPlatformServerIDs() {
            return platformServerIDs;
        }
        private void setPlatformServerIDs(Set<String> platformServerIDs) {
            this.platformServerIDs = Collections.unmodifiableSet(platformServerIDs);
        }

        private Map<String, String> toUnmodifiableMap(Hashtable<String, String> table) {
            Map<String, String> conversion = new HashMap<>();
            for (Map.Entry<String, String> entry : table.entrySet()) {
                conversion.put(entry.getKey(), entry.getValue());
            }
            return Collections.unmodifiableMap(conversion);
        }
    }
}
