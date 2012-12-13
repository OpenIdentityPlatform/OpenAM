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
 * $Id: DefaultClientTypesManager.java,v 1.4 2008/09/04 16:16:34 dillidorai Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.cdm;

import com.iplanet.services.cdm.clientschema.AMClientCapData;
import com.iplanet.services.cdm.clientschema.AMClientCapException;
import com.iplanet.services.cdm.clientschema.AMClientDataListener;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * This class gives out instances of Client object so that it hides the
 * implementation from clients
 */
public class DefaultClientTypesManager implements ClientTypesManager,
        AMClientDataListener {
    // for debug
    private static String CLASS = "DefaultClientTypesManager: ";

    // client detection module service name
    protected static final String CDM_SERVICE_NAME = "iPlanetAMClientDetection";

    private static final String CDM_DEFAULT_CLIENT_TYPES_ATTR = 
        "iplanet-am-client-detection-default-client-type";

    private static Debug debug = Debug.getInstance("amClientDetection");

    private static SSOToken internalToken = null;

    /**
     * Get the defaultClientType from iPlanetAMClientDetection Service
     */
    private static String defaultClientType = null;

    //
    // Holds all the instances from InternalDB
    //
    private static Map internalClientData = new Hashtable();

    //
    // Holds all the instances from ExternalDB
    //
    private static Map externalClientData = new Hashtable();

    private static Map mergedClientData = new Hashtable();

    //
    // indexed by userAgent
    //
    private static Map userAgentMap = new Hashtable();

    //
    // indexed by clientType - may not be "full" client
    //
    private static Map clientTypeMap = new Hashtable();

    //
    // keep track of update clients.(for not re-creating client on
    // Event notification).
    //
    private static Set updatedClients = new HashSet();

    //
    // To keep track of partial matches. Key = <actual_user-agent>
    // Value = clientType String matched previously
    //
    private static Map partialMatchMap = new Hashtable();

    //
    // To keep track of all the clients that have been completely loaded from
    // the both internal and external DB. Caches Client objects.
    //
    private static Map loadedClientsMap = new Hashtable();

    // To keep track of all the clients that have been loaded from
    // the internal DB. Caches Maps of client properties.
    //
    private static Map loadedInternalClients = new Hashtable();

    // To keep track of all the clients that have been loaded from
    // the external DB. Caches Maps of client properties.
    //
    private static Map loadedExternalClients = new Hashtable();

    //
    // Key = baseprofile clients;
    // [ Value of Map (Styles Key = style name; Value = client) ]
    // Need a TreeMap since the Console Plugin wants a sorted order.
    //
    private static Map baseProfiles = new TreeMap();

    private static final String PARENT_ID = "parentId";

    private static final String CLIENT_TYPE = "clientType";

    private static final String USER_AGENT = "userAgent";

    //
    // Instances for Client Schema API
    //
    private static AMClientCapData intCapInstance = null;

    private static AMClientCapData extCapInstance = null;

    static {
        try {
            internalToken = getInternalToken();
            defaultClientType = getDefaultClientTypeFromService();

            try {
                intCapInstance = AMClientCapData.getInternalInstance();
            } catch (AMClientCapException ce) {
                if (debug.warningEnabled()) {
                    debug.warning(CLASS 
                            + "Unable to get instance of InternalData");
                }
            }

            try {
                extCapInstance = AMClientCapData.getExternalInstance();
            } catch (AMClientCapException ce) {
                if (debug.warningEnabled()) {
                    debug.warning(CLASS 
                        + "Unable to get instance of ExternalData");
                }
            }

        } catch (Throwable t) {
            debug.error(CLASS + "init() failed: ", t);
        }
    }

    private static boolean isInitialized = false;

    public DefaultClientTypesManager() {
    }

    /**
     * Do stuff that needs to get the Manager working.
     */
    public void initManager() {
        synchronized (userAgentMap) // any static object to sync
        {
            if (!isInitialized) {
                isInitialized = true;

                long st = System.currentTimeMillis();

                // Load minimal client info for all internal clients
                if (intCapInstance != null) {
                    initMinimalInternalClientTypesData();
                }

                // Load minimal client info for all external clients
                if (extCapInstance != null) {
                    initMinimalExternalClientTypesData();
                }

                // Merge internal client data with the external client data
                mergeInternalWithExternal();

                // Load maps for searching by useragent, clientType
                loadMaps();

                long end = System.currentTimeMillis();
                if (debug.messageEnabled()) {
                    debug.message(CLASS + "Load AllClients Time (ms) = "
                            + (end - st));
                }

                if (intCapInstance != null) {
                    intCapInstance.addListener(this); // register to internal
                }
                if (extCapInstance != null) {
                    extCapInstance.addListener(this); // register to external
                }
            }
        }
    }

    /**
     * read service config data from SMS
     */
    private static String getDefaultClientTypeFromService()
            throws SMSException, SSOException {
        // read iPlanetAMClientDetection service using SMS API
        ServiceSchemaManager serviceSchemaManager = new ServiceSchemaManager(
                CDM_SERVICE_NAME, internalToken);

        ServiceSchema gsc = serviceSchemaManager.getGlobalSchema();

        Map data = gsc.getAttributeDefaults();

        String defaultCT = (String) ((Set) data
                .get(CDM_DEFAULT_CLIENT_TYPES_ATTR)).toArray()[0];

        return defaultCT;
    }

    /**
     * Get internal sso token, use admin DN & password
     */
    private static SSOToken getInternalToken() throws SSOException {
        return ((SSOToken) AccessController.doPrivileged(AdminTokenAction
                .getInstance()));
    }

    /**
     * Initialize the minimal info for the internal clientTypes data
     * <ol type="1">
     * <li>Get minimal info for all clients using AMClientData API
     * <li>Merge all the clients data with its parents'.
     * </ol>
     */
    private void initMinimalInternalClientTypesData() {

        Set clients = intCapInstance.getMinimalClientInfo();
        Iterator iter = clients.iterator();

        while (iter.hasNext()) {
            // Get client map
            Map clientData = (Map) iter.next();

            // Get clientType for the client
            String clientType = getClientType(clientData);
            if ((clientType != null) && (clientType.length() > 0)) {
                // Add client to internal client data collection
                internalClientData.put(clientType, clientData);
            } else {
                // Skip client if the clientType attribute is null
                debug.error(CLASS + "Found clientType == NULL in internal DB");
                continue;
            }

            //
            // POOL all baseProfiles. Since we haven't yet merged parent data,
            // the parentIdSet will contain only 1 element.
            //
            Set parentIdSet = (Set) clientData.get(PARENT_ID);
            if (parentIdSet != null && parentIdSet.contains(clientType)) {
                Map t1 = new TreeMap();
                Map t2 = new TreeMap();

                t1.put(clientType, t2); // add to styles
                baseProfiles.put(clientType, t1); // and add to baseProfiles

                if (debug.messageEnabled()) {
                    debug.message(CLASS + "BaseProfile: " + clientType);
                }
            }
        }
    }

    /**
     * Initialize the minimal info for the external clientTypes data
     * <ol type="1">
     * <li>Get minimal info for all clients using AMClientData API
     * <li>Merge all the external clients data with internal clients'.
     * </ol>
     */
    private void initMinimalExternalClientTypesData() {
        Set clients = extCapInstance.getMinimalClientInfo();
        Iterator iter = clients.iterator();

        while (iter.hasNext()) {
            // Get client map
            Map clientData = (Map) iter.next();

            // Get clientType for the client
            String clientType = getClientType(clientData);
            if ((clientType != null) && (clientType.length() > 0)) {
                Map mMap = mergeWithInternal(clientType, clientData);
                if (mMap != null) {
                    clientData = mMap;
                }

                // Add client to external client data collection
                externalClientData.put(clientType, clientData);
            } else {
                // Skip client if the clientType attribute is null
                debug.error(CLASS + "Found clientType == NULL in external DB");
                continue;
            }
        }
    }

    /**
     * Util method to get CLIENT_TYPE from MAP
     */
    private String getClientType(Map m) {
        return getFirstString((Set) m.get(CLIENT_TYPE));
    }

    /**
     * Merge the Client with its Parent. The parent could be in either in
     * internal or external db. Used to merge uaprofile devices.
     * 
     * @param cMap
     *            Map of the client data.
     * 
     * @return The Merged map.
     */
    private Map mergeWithParent(Map cMap) {
        String ct = getClientType(cMap);
        String parentId = getParentID(cMap);

        Map rMap = cMap;
        Map pMap = null;
        OrderedSet os = new OrderedSet();
        while ((parentId != null) && (!ct.equals(parentId))) {
            pMap = (Map) mergedClientData.get(parentId);
            if (pMap == null) {
                debug.error(CLASS + "clientdata null for: " + parentId);
                rMap = null;
                break;
            }

            rMap = mergeMap(pMap, cMap);
            os.add(parentId);

            cMap = rMap;
            ct = getClientType(pMap);
            parentId = getParentID(pMap);
        }

        if (rMap != null) {
            rMap.put(PARENT_ID, os);
        }

        return rMap;
    }

    /**
     * Merge the Client in External to its peer in Internal. Calls mergeMap()
     * and copies the peer's parents to itself. Logic: Get client properties map
     * for the client type from internalClientData if internalClientData does
     * not contain properties for this client type return null else merge client
     * properties with parent properties return merged properties end if
     * 
     * @param sMap
     *            Source Map (typically from internalDB)
     * @param dMap
     *            Destination Map
     */
    private Map mergeWithInternal(String ct, Map dMap) {
        Map rMap = null;
        Map sMap = (Map) internalClientData.get(ct);
        if (sMap != null) {
            rMap = mergeMap(sMap, dMap);
        }

        return rMap;
    }

    /**
     * Merge the mergeHash with the baseHash. This function mergers the
     * mergeHash and the baseHash by over riding any attributes found in the
     * baseHash.
     * 
     * @return a MergedMap
     */
    private Map mergeMap(Map baseHash, Map mergeHash) {
        Map results = null;

        if (baseHash != null) {
            results = new HashMap(baseHash);
        }

        if (mergeHash != null) {
            if (results != null) {
                results.putAll(mergeHash);
            } else {
                results = mergeHash;
            }
        }

        return results;
    }

    private void mergeInternalWithExternal() {
        mergedClientData = mergeMap(internalClientData, externalClientData);
    }

    /**
     * Refactor method Load the userAgentMap. The userAgentMap is used by the
     * client detector to query clientType.
     */
    protected void loadMaps() {
        Set clientTypes = mergedClientData.keySet();
        Iterator keys = clientTypes.iterator();
        String clientType = null;
        Map clientDataMap = null;

        // Set the parent and styles for each client
        while (keys.hasNext()) {
            clientType = (String) keys.next();
            clientDataMap = (Map) mergedClientData.get(clientType);
            setParentStyles(clientDataMap);
        }

        // add client to the useragent and clientdata maps
        keys = clientTypes.iterator();
        while (keys.hasNext()) {
            clientType = (String) keys.next();
            clientDataMap = (Map) mergedClientData.get(clientType);

            addToClientMap(clientType, clientDataMap);
        }
    }

    /**
     * Add to the userAgentMap, clientTypeMap & ProfilesMap.
     * 
     * @return the created Client Object
     */
    protected Client addToClientMap(String ct, Map cMap) {
        return addToClientMap(ct, cMap, true);
    }

    /**
     * @param addToStyles
     *            Indicates if the Client should be added to the Styles Map.
     *            Will be false when a Client is added from CC/PP module, whose
     *            added clients shouldn't be visible in the admin console.
     */
    protected Client addToClientMap(String ct, Map cMap, boolean addToStyles) {
        Client client = new Client(ct, cMap);
        String userAgent = client.getProperty(USER_AGENT);

        if (userAgent != null) {
            userAgentMap.put(userAgent, ct);
        }

        clientTypeMap.put(ct, client);

        if (addToStyles) {
            storeInProfilesMap(client);
        }

        return client;
    }

    /**
     * complements addToClientMap()
     */
    protected void removeFromClientMap(String ct, Client c) {
        clientTypeMap.remove(ct);
        mergedClientData.remove(ct);
        String ua = null;
        if ((c != null) && ((ua = c.getProperty(USER_AGENT)) != null)) {
            userAgentMap.remove(ua);
        }

        removeFromProfilesMap(ct, c);
    }

    /**
     * Adds the client to its appropriate baseProfileMap & styleMap. Ex:
     * Nokia7110, with Nokia style & WML for its baseProfile, will go into
     * wmlProfilesMap under Nokia styles. (Used only for displaying Styles &
     * Clients under Base in the console plug-in).
     * 
     * Currently Aligo only supports 1 level of Style. Only the baseProfiles
     * immediate children are eligible to be Styles.
     */
    protected void storeInProfilesMap(Client client) {
        Set parentSet = client.getProperties(PARENT_ID);
        String clientType = client.getClientType();

        Map m = getStylesProfileMap(clientType, parentSet);
        if (m != null) {
            m.put(clientType, client);
        }

        return;
    }

    /**
     * Rearranges a client and puts it under a different style if parent
     * changed.
     * 
     * @param ct
     *            Client Type
     * @param nMap
     *            Client properties Map.
     */
    private void handleParentChange(String ct, Map nMap) {
        Client oClient = (Client) clientTypeMap.get(ct);

        if (oClient == null) {
            addToClientMap(ct, nMap);
            mergedClientData.put(ct, nMap);
            return;
        }

        Set oParents = oClient.getProperties(PARENT_ID);
        String oParent = getFirstString(oParents);

        String nParentId = getParentID(nMap);
        if (!nParentId.equals(oParent)) {
            removeFromProfilesMap(ct, oClient);
            if ((nMap = mergeWithParent(nMap)) != null) {
                addToClientMap(ct, nMap);
            }
        }

        mergedClientData.put(ct, nMap); // update the merged Map
        return;
    }

    /**
     * complements storeInProfilesMap
     */
    protected void removeFromProfilesMap(String ct, Client c) {
        if (c == null) {
            return;
        }

        Set parentSet = c.getProperties(PARENT_ID);
        Map m = getStylesProfileMap(ct, parentSet);
        if (m != null) {
            m.remove(ct);
        }
    }

    /**
     * Get the Style Map corresponding to the parentSet p.
     */
    protected Map getStylesProfileMap(String ct, Set p) {
        String base = null;
        String style = null;

        if ((ct == null) || (p == null)) {
            return null;
        }
        Iterator itr = p.iterator();
        while (itr.hasNext()) {
            style = base;
            base = (String) itr.next(); // get the last one
        }

        if (style == null) {
            style = base; // baseProfile is also a style
        }
        Map m = null;
        Map profMap = (Map) baseProfiles.get(base);
        if (profMap != null) {
            /**
             * We dont want to add the base/style Client to their own Maps.
             * Change this to profMap.get (ct) if we need to.
             */
            if (profMap.containsKey(ct)) {
                profMap = null;
            } else if ((m = (Map) profMap.get(style)) != null) {
                profMap = m;
            }
        }

        return profMap;
    }

    private String getFirstString(Set set) {
        String retVal = null;
        if ((set != null) && (set.iterator().hasNext())) {
            retVal = (String) set.iterator().next();
        }
        return retVal;
    }

    /**
     * Create Client Object & add to all our indexes. Called only by addClient &
     * on clientChanged notification.
     */
    protected Client addToIndexes(String ct, Map cMap) {
        return addToIndexes(ct, cMap, true);
    }

    /**
     * @param addtoStyles
     *            true only if the data is stored in ldap false for uaprof
     *            clients which only stay in memory.
     */
    protected Client addToIndexes(String ct, Map cMap, boolean addtoStyles) {
        mergedClientData.put(ct, cMap); // add to mergedClientData
        Client client = addToClientMap(ct, cMap, addtoStyles);

        String ua = client.getProperty(USER_AGENT);

        if (ua != null) {
            partialMatchMap.remove(ua); // ok if it doesn't exist
            if (partialMatchMap.containsValue(ct)) {
                Iterator itr = partialMatchMap.values().iterator();
                while (itr.hasNext()) {
                    String cType = (String) itr.next();
                    if (cType != null && cType.equals(ct)) {
                        itr.remove(); // dont break - might have more
                    }
                }
            }
        }

        return client;
    }

    //
    // Interfaces used by ClientDetector (Default & MAP)
    //
    public Client getFromUserAgentMap(String ua) {
        String clientType = (ua != null) ? (String) userAgentMap.get(ua) : null;
        Client c = null;
        if (clientType != null) {
            c = getClientInstance(clientType);
        }
        return c;
    }

    public Set userAgentSet() {
        Set keys = userAgentMap.keySet();
        return keys;
    }

    /**
     * Get a previously "partially" matched userAgent String
     * 
     * @return the clientType string matched previously for this ua.
     */
    public String getPartiallyMatchedClient(String ua) {
        String ct = (String) partialMatchMap.get(ua);
        return ct;
    }

    /**
     * Add client to our partialMatchMap.
     */
    public void addToPartialMatchMap(String ua, String clientType) {
        partialMatchMap.put(ua, clientType);
    }

    /**
     * Add the new client to internal DB, if store == true, else keep it only in
     * memory.
     */
    public Client addClient(SSOToken token, String clientType, Map cMap,
            boolean store) throws AMClientCapException {
        Client client = null;
        if ((cMap == null) || (cMap.isEmpty())) {
            return client; // NO-OP
        }

        if (debug.messageEnabled()) {
            debug.message(CLASS + "Adding new Client: " + cMap);
        }

        if (store) {
            synchronized (internalClientData) {
                intCapInstance.addClient(token, cMap);
                cMap = mergeWithParent(cMap);
                internalClientData.put(clientType, cMap);
            }
        } else {
            /**
             * The parent for the new client could be in external db, since the
             * uaprof devices are parented with the devices corresponding to the
             * user-agent match. (bug id: 4967877).
             */
            cMap = mergeWithParent(cMap);
        }

        client = addToIndexes(clientType, cMap, store);
        if (!store) {
            //
            // Since the data is not going into ldap, store it directly in
            // loadedClientsMap, for the getClientInstance() to find it
            //
            loadedClientsMap.put(clientType, client);
        }

        return client;
    }

    /**
     * Check if we can create clients and add them to internal DB.
     */
    public boolean canCreateClients() {
        return (intCapInstance.canCreateInternalClients());
    }

    //
    // The ClientTypesManager interfaces
    //
    public Map getAllClientInstances() {
        return clientTypeMap;
    }

    public Set getAllClientTypes() {
        return clientTypeMap.keySet();
    }

    /**
     * Get the clientType from clientTypeMap.
     */
    public Client getClientInstance(String clientType) {
        if (clientType.equals("default")) {
            clientType = defaultClientType; // change to the client pointed to.
        }

        Client client = null;

        //
        // Load client if it has not been already loaded
        //
        if ((client = (Client) loadedClientsMap.get(clientType)) == null) {
            client = loadClient(clientType);
        }

        return client;
    }

    public Client getClientInstance(String clientType, SSOToken token) {
        return getClientInstance(clientType);
    }

    public Map getClientTypeData(String clientType) {
        if (clientType.equals("default")) {
            clientType = defaultClientType; // change to the client pointed to.
        }

        // Load client if it has not been already loaded
        if (mergedClientData.get(clientType) == null) {
            loadClient(clientType);
        }
        Map map = (Map) mergedClientData.get(clientType);
        return map;
    }

    /**
     * 1. Load internal client data and set parent data 2. Load External client
     * data and merge with internal client data 3. Load merged client data to
     * mergedClientData set 4. Construct Client object and add to
     * loadedClientsMap
     */
    protected Client loadClient(String clientType) {
        Client client = null;
        long st = System.currentTimeMillis();
        Map iMap = loadInternalClient(clientType);

        Map eMap = loadExternalClient(clientType);
        if ((iMap == null) && (eMap == null)) {
            return client;
        }

        Map mergedMap = mergeMap(iMap, eMap);

        //
        // Recursively load the parent profiles
        // Warning: Looks for parents only in the internal db.
        // 
        String ct = clientType;
        String parentID = getParentID(mergedMap);
        OrderedSet os = new OrderedSet();
        while ((parentID != null) && (!(ct.equals(parentID)))) {
            os.add(parentID);

            Map pMap = loadInternalClient(parentID);
            if (pMap == null) {
                break;
            } else {
                mergedMap = mergeMap(pMap, mergedMap);
                ct = getClientType(pMap);
                parentID = getParentID(pMap);
            }
        }

        if (os.size() > 0) {
            //
            // for base Clients (os.size() == 0): we wont come in here,
            // so the PARENT_ID in the Map wont get replaced.
            //
            mergedMap.put(PARENT_ID, os);
        }

        mergedClientData.put(clientType, mergedMap);
        client = new Client(clientType, mergedMap);
        loadedClientsMap.put(clientType, client);

        long end = System.currentTimeMillis();

        if (debug.messageEnabled()) {
            debug.message(CLASS + "Load Client " + clientType + " Time (ms) = "
                    + (end - st));
        }

        return client;
    }

    /**
     * Load client properties recursively from internal DB
     * 
     * @return Map iMap
     */
    protected Map loadInternalClient(String clientType) {
        Map iMap = (Map) loadedInternalClients.get(clientType);

        // If client was not loaded earlier then load from internal DB
        if (iMap == null) {
            if (intCapInstance != null) {
                iMap = intCapInstance.getProperties(clientType);
            }
            if (iMap == null) {
                return null;
            }

            loadedInternalClients.put(clientType, iMap);
            internalClientData.put(clientType, iMap);
        }

        return iMap;
    }

    /**
     * Load client properties recursively from external DB
     * 
     * @return Map eMap
     */
    protected Map loadExternalClient(String clientType) {
        Map eMap = (Map) loadedExternalClients.get(clientType);

        // If client was not loaded earlier then load from external DB
        if (eMap == null) {
            if (extCapInstance != null) {
                eMap = extCapInstance.getProperties(clientType);
            }
            if (eMap == null) {
                return null;
            }

            loadedExternalClients.put(clientType, eMap);
            externalClientData.put(clientType, eMap);
        }

        return eMap;
    }

    /**
     * Retrieve parent id
     */
    private String getParentID(Map clientData) {
        // childs' parents
        Set cParents = (Set) clientData.get(PARENT_ID);
        String parentId = getFirstString(cParents);

        return parentId;
    }

    /**
     * Get default client type name
     */
    public String getDefaultClientType() {
        return defaultClientType;
    }

    public void updateClientData() throws ClientException {
    }

    public void store(SSOToken token) throws SMSException, SSOException {
    }

    public void setDirty(String ct, Map data) {
    }

    // END ClientTypesManager interface's method impl.

    /**
     * AMClientDataListener method for client change notification
     * 
     * @param clientType
     * @param dbType
     * @param opType
     */
    public void clientChanged(String clientType, int dbType, int opType) {
        if (debug.messageEnabled()) {
            debug.message(CLASS + "clientChanged() Notification: "
                    + "clientType = " + clientType + " :DB = " + dbType
                    + " : Op = " + opType);
        }

        if ((clientType == null) || (clientType.length() == 0)) {
            return;
        }

        if (opType == AMClientCapData.ADDED) {
            /**
             * Notifications can be generated when clients are added either to
             * the internal or external db. If notification is for addition to
             * internal then check if the client is already loaded in
             * internalclients map. If not loaded yet then force load it.
             * 
             * since the synch() is done in addClientXX() the containsKey() will
             * block
             */
            if (dbType == AMClientCapData.INTERNAL) {
                if (!internalClientData.containsKey(clientType)) {
                    Map iMap = intCapInstance.loadMinimalClient(clientType);
                    Map oMap = (Map) mergedClientData.get(clientType);
                    Map mMap = mergeMap(oMap, iMap);

                    handleParentChange(clientType, mMap);
                    unloadClient(clientType);
                }
            } else if (dbType == AMClientCapData.EXTERNAL) {
                /**
                 * If notification is for addition to external then check if the
                 * client is already loaded in externalclients map.
                 * 
                 * Note: The Client console uses modifyClientExternal() to add
                 * clients to external. Since modifyClientExternal sync's on
                 * updatedClients, we do the same here.
                 */
                synchronized (updatedClients) {
                    if (!externalClientData.containsKey(clientType)) {
                        Map eMap = extCapInstance.loadMinimalClient(clientType);
                        Map oMap = (Map) mergedClientData.get(clientType);
                        Map mMap = mergeMap(oMap, eMap);

                        handleParentChange(clientType, mMap);
                        unloadClient(clientType);
                    }
                }
            }

        } else if (opType == AMClientCapData.MODIFIED) {
            /**
             * if the client has been flagged updated then remove flag else
             * force the client data to be reloaded for the next request for
             * this client type. We don't need to relaod the UA and clienttype
             * maps because the two minimal properties - clientType & UA cannot
             * be modified.
             */
            synchronized (updatedClients) {
                if (updatedClients.contains(clientType)) {
                    updatedClients.remove(clientType);
                } else {
                    Map cMap = extCapInstance.loadMinimalClient(clientType);
                    Map oMap = (Map) mergedClientData.get(clientType);
                    Map mMap = mergeMap(oMap, cMap);

                    handleParentChange(clientType, mMap);
                    unloadClient(clientType);
                }
            }
        } else if (opType == AMClientCapData.REMOVED) {
            /**
             * If client is still present then remove all references
             */
            if (externalClientData.containsKey(clientType)) {
                removeFromMaps(clientType);
            }
        } else {
            debug.warning(CLASS + "clientChanged(): unknown OpType");
        }
    }

    //
    // Methods used by console plug-in
    //

    /**
     * @return a Set of baseProfileNames. sorted in ascending order.
     */
    public Set getBaseProfileNames() {
        return baseProfiles.keySet();
    }

    /**
     * @param baseProfileName
     *            A valid base profile name
     * @return A Set of all styles belonging to baseProfile
     */
    public Set getStyles(String baseProfileName) {
        Set styles = null;

        Map styleMap = (Map) baseProfiles.get(baseProfileName);
        if (styleMap != null) {
            styles = styleMap.keySet();
        }
        return styles;
    }

    /**
     * @param baseProfileName
     *            A valid base profile name
     * @param style
     *            A valid style name
     * 
     * @return A Map of all clientTypes belonging to baseProfile & style
     */
    public Map getClients(String baseProfileName, String style) {
        Map cMap = null;
        Map styleMap = (Map) baseProfiles.get(baseProfileName);

        if (styleMap != null) {
            cMap = (Map) styleMap.get(style);
        }

        return cMap;
    }

    /**
     * Wrapper methods for add/modify/remove APIs in AMClientCapData. We have it
     * here, so we dont have to wait for an event notification to arrive when
     * this is called by the plugin.
     */
    public int addClientExternal(SSOToken token, Map props)
            throws AMClientCapException {
        synchronized (externalClientData) {
            extCapInstance.addClient(token, props);

            String ct = getClientType(props);
            Map eMap = mergeWithParent(props);
            externalClientData.put(ct, eMap);

            addToIndexes(ct, eMap);
        }

        return 0;
    }

    /**
     * 
     * @param token
     * @param props
     * @return 0 if everything went fine
     * @throws AMClientCapException
     */
    public int modifyClientExternal(SSOToken token, Map props)
            throws AMClientCapException {
        if ((props == null) || (props.isEmpty())) {
            return 0; // NO-OP
        }

        synchronized (updatedClients) {
            extCapInstance.modifyClient(token, props);

            String ct = getClientType(props);

            if (!externalClientData.containsKey(ct)) {
                //
                // Workaround for AMEntity timing issue.
                //
                externalClientData.put(ct, props);
            } else {
                updatedClients.add(ct);
            }

            Map oMap = (Map) mergedClientData.get(ct); // need the whole data

            props = mergeMap(oMap, props);
            handleParentChange(ct, props);

            //
            // unload the client data so that modified data can be
            // loaded next time
            //
            unloadClient(ct);
        }

        return 0;
    }

    /**
     * 
     * @param token
     * @param clientType
     * @return 0 if everything went fine
     * @throws AMClientCapException
     */
    public int removeClientExternal(SSOToken token, String clientType)
            throws AMClientCapException {
        synchronized (externalClientData) {
            extCapInstance.removeClient(token, clientType);
            removeFromMaps(clientType);
        }

        return 0;
    }

    /**
     * Remove all references to this client
     * 
     * @param clientType The removable clientType
     */
    protected void removeFromMaps(String clientType) {
        Client client = (Client) clientTypeMap.get(clientType);

        externalClientData.remove(clientType);
        Map map = (Map) internalClientData.get(clientType);

        //
        // Internal data clients cannot be deleted.
        //
        if (map == null) {
            removeFromClientMap(clientType, client);
        } else {
            //
            // For cases when user chooses the 'default' option and
            // if the parent id was changed earlier then the maps
            // have to be reset to the old parent id for the client
            //

            handleParentChange(clientType, map);
        }

        unloadClient(clientType);
    }

    /**
     * Sets the styles and parent ids tree set for a client
     * 
     * @param clientData
     */
    protected void setParentStyles(Map clientData) {
        if (clientData == null) {
            return;
        }

        // childs' parents
        Set cParents = (Set) clientData.get(PARENT_ID);
        String parentId = getFirstString(cParents);

        String clientType = getClientType(clientData);
        Map parentMap = (Map) mergedClientData.get(parentId);

        if (parentMap == null) {
            debug.error("ParentMap for clientType = " + clientType
                    + ", parentId = " + parentId + " was null");
            return;
        }

        //
        // add parent's parents' to our parent list (if the parent is not base).
        // If the parent is the base and we are a style, create a Map for the
        // style and add it to the baseProfiles.
        //
        if (baseProfiles.containsKey(parentId)) {
            if (clientData.get(USER_AGENT) == null) {
                Map s = (Map) baseProfiles.get(parentId);
                if (s.get(clientType) == null) {
                    s.put(clientType, new TreeMap()); // valid style

                    if (debug.messageEnabled()) {
                        debug.message(CLASS + "Creating Style: " + clientType
                                + " : Parent : " + parentId);
                    }
                }
            }
        } else {
            //
            // Add the parent first and then add parent's parents'.
            // NOTE: this will work only for a max of 2-level parent
            //
            OrderedSet os = new OrderedSet();
            os.add(parentId);

            Set pParents = (Set) parentMap.get(PARENT_ID);
            os.addAll(pParents);

            clientData.put(PARENT_ID, os);
        }
    }

    /**
     * Removes client from all the loaded client maps
     * 
     * @param clientType
     */
    private void unloadClient(String clientType) {
        // remove from internal clients list
        loadedInternalClients.remove(clientType);

        // remove from external clients list
        loadedExternalClients.remove(clientType);

        // remove from merged and loaded clients list
        loadedClientsMap.remove(clientType);
    }
}
