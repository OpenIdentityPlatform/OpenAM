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
 * $Id: SMSJAXRPCObjectImpl.java,v 1.22 2009/10/28 04:24:27 hengming Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2013 ForgeRock AS
 */

package com.sun.identity.sm.jaxrpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import com.iplanet.services.naming.ServerEntryNotFoundException;
import com.iplanet.services.naming.WebtopNaming;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.comm.server.PLLServer;
import com.iplanet.services.comm.server.SendNotificationException;
import com.iplanet.services.comm.share.Notification;
import com.iplanet.services.comm.share.NotificationSet;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.jaxrpc.JAXRPCUtil;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.CachedSMSEntry;
import com.sun.identity.sm.CachedSubEntries;
import com.sun.identity.sm.SMSDataEntry;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSNotificationManager;
import com.sun.identity.sm.SMSObjectListener;
import com.sun.identity.sm.SMSUtils;
import com.sun.identity.sm.ServiceAttributeValidator;

public class SMSJAXRPCObjectImpl implements SMSObjectIF, SMSObjectListener {

    static Debug debug = Debug.getInstance("amSMSServerImpl");

    static Map<String, URL> notificationURLs = new HashMap<String, URL>();

    static SSOTokenManager tokenMgr;

    static SSOException initializationError;

    static String baseDN;

    static String amsdkbaseDN;

    static boolean initialized;

    static String serverURL;

    // Cache of modified DNs for the last 30 minutes
    static int cacheSize = 30;

    static LinkedList cacheIndices = new LinkedList();

    static HashMap cache = new HashMap(cacheSize);

    // Default constructor
    public SMSJAXRPCObjectImpl() {
        // Empty constructor to avoid bootstraping when JAX-RPC
        // initialized this object on the Serve
    }

    // Initialization to register the callback handler
    private void initialize() {
        if (!initialized) {
            try {
                tokenMgr = SSOTokenManager.getInstance();
            } catch (SSOException ssoe) {
                debug.error("SMSJAXRPCObject: "
                        + "Unable to get SSO Token Manager");
                initializationError = ssoe;
            }
            
            // Register for notifications & polling cache
            if (SMSNotificationManager.isCacheEnabled()) {
                SMSNotificationManager.getInstance()
                    .registerCallbackHandler(this);
                if (debug.messageEnabled()) {
                    debug.message("SMSJAXRPCObjectImpl.init " +
                        "Registered for notifications");
                }
                
                // Obtain the cache size, if configured
                String cacheSizeStr = SystemProperties.get(
                    Constants.EVENT_LISTENER_REMOTE_CLIENT_BACKLOG_CACHE);
                try {
                    cacheSize = Integer.parseInt(cacheSizeStr);
                    if (cacheSize < 0) {
                        cacheSize = 30;
                    }
                } catch (NumberFormatException e) {
                    //do nothing
                }
                if (debug.messageEnabled()) {
                    debug.message("SMSJAXRPCObjectImpl.init  " +
                        "EventNotification cache size is set to " + cacheSize);
                }
            }
            
            // Construct server URL
            String namingURL = SystemProperties.get(Constants.AM_NAMING_URL);
            if (namingURL != null) {
                int index = namingURL.toLowerCase().indexOf("/namingservice");
                if (index != -1) {
                    serverURL = namingURL.substring(0, index);
                } else {
                    serverURL = "";
                }
            } else {
                serverURL = SystemProperties.getServerInstanceName();
                if (serverURL == null) {
                    serverURL = "";
                }
            }
            if ((serverURL == null) || (serverURL.length() == 0)) {
                debug.error("SMSJAXRPCObjectImpl.init Server URL IS NULL");
            } else if (debug.messageEnabled()) {
                debug.message("SMSJAXRPCObjectImpl.init ServerURL: " +
                    serverURL);
            }
            initialized = true;
        }
    }

    // Method to check if service is local and also to
    // test if the server is down
    public void checkForLocal() {
        SMSJAXRPCObject.isLocal = true;
    }

    /**
     * Returns the attribute names and values of the provided object using the
     * identity of the provided SSO Token
     */
    public Map read(String tokenID, String objName)
        throws SMSException, SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::read dn: " + objName);
        }

        Map returnAttributes = null;
        if (objName.equals("o=" + SMSJAXRPCObject.AMJAXRPCVERSIONSTR)) {

            returnAttributes = new HashMap();
            returnAttributes.put(SMSJAXRPCObject.AMJAXRPCVERSIONSTR,
                   SMSJAXRPCObject.AMJAXRPCVERSION);
        } else {
            CachedSMSEntry ce = CachedSMSEntry.getInstance(getToken(tokenID),
                objName);
            if (ce.isDirty()) {
                ce.refresh();
            }
            Map attrs = ce.getSMSEntry().getAttributes();
            if ((attrs != null) && (attrs instanceof CaseInsensitiveHashMap)) {
                returnAttributes = new HashMap();
                for (Iterator items = attrs.keySet().iterator(); 
                    items.hasNext();) {
                    String attrName = items.next().toString();
                    Object o = attrs.get(attrName);
                    returnAttributes.put(attrName, o);
                }            
            } else { // could be null or instance of HashMap - return as it is.
                returnAttributes = attrs;
            }                             
        }
        return returnAttributes;

    }

    /**
     * Creates an entry in the persistent store. Throws an exception if the
     * entry already exists
     */
    public void create(String tokenID, String objName, Map attributes)
            throws SMSException, SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::create dn: " + objName);
        }
        SMSEntry entry = new SMSEntry(getToken(tokenID), objName);
        entry.setAttributes(attributes);
        entry.save();
    }

    /**
     * Modifies the attributes to the object.
     */
    public void modify(String tokenID, String objName, String mods)
            throws SMSException, SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::modify dn: " + objName);
        }
        SMSEntry entry = new SMSEntry(getToken(tokenID), objName);
        entry.modifyAttributes(getModItems(mods));
        entry.save();
    }

    /**
     * Delete the entry in the datastore. This should delete sub-entries also
     */
    public void delete(String tokenID, String objName) throws SMSException,
            SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::delete dn: " + objName);
        }
        SMSEntry entry = new SMSEntry(getToken(tokenID), objName);
        entry.delete();
    }

    /**
     * Returns the suborganization names. Returns a set of SMSEntry objects that
     * are suborganization names. The paramter <code>numOfEntries</code>
     * identifies the number of entries to return, if code>0</code> returns
     * all the entries.
     */
    public Set searchSubOrgNames(String tokenID, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder,
            boolean recursive) throws SMSException, SSOException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::searchSubOrgNames dn: " + dn);
        }
        CachedSubEntries ce = CachedSubEntries.getInstance(
            getToken(tokenID), dn);
        return (ce.searchSubOrgNames(getToken(tokenID), filter, recursive));
    }

    /**
     * Returns the organization names. Returns a set of SMSEntry objects that
     * are organization names. The paramter <code>numOfEntries</code>
     * identifies the number of entries to return, if <code>0</code> returns
     * all the entries.
     */
    public Set searchOrganizationNames(String tokenID, String dn,
            int numOfEntries, boolean sortResults, boolean ascendingOrder,
            String serviceName, String attrName, Set values)
            throws SMSException, SSOException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::searchOrganizationNames dn: "
                    + dn);
        }

        CachedSubEntries ce = CachedSubEntries.getInstance(
                getToken(tokenID), dn);
        return (ce.searchOrgNames(getToken(tokenID), serviceName,
                attrName, values));
    }

    /**
     * Returns the sub-entries. Returns a set of SMSEntry objects that are
     * sub-entries. The paramter <code>numOfEntries</code> identifies the
     * number of entries to return, if <code>0</code> returns all the entries.
     */
    public Set subEntries(String tokenID, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder)
            throws SMSException, SSOException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::subentries dn: " + dn);
        }
        
        CachedSubEntries ce = CachedSubEntries.getInstance(
                getToken(tokenID), dn);
        return (ce.getSubEntries(getToken(tokenID), filter));        
    }

    /**
     * Returns the sub-entries matching the schema id. Returns a set of SMSEntry
     * objects that are sub-entries for the provided schema id. The paramter
     * <code>numOfEntries</code> identifies the number of entries to return,
     * if <code>0</code> returns all the entries.
     */
    public Set schemaSubEntries(String tokenID, String dn, String filter,
            String sidFilter, int numOfEntries, boolean sortResults, boolean ao)
            throws SMSException, SSOException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::subentries dn: " + dn);
        }
        
        CachedSubEntries ce = CachedSubEntries.getInstance(
                getToken(tokenID), dn);
        return (ce.getSchemaSubEntries(getToken(tokenID), filter, sidFilter));
    }

      /**
       * Searchs the data store for objects that match the filter
       */
    public Set search(String tokenID, String startDN, String filter)
            throws SMSException, SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::search dn: " + startDN
                    + " filter: " + filter);
        }
        return (SMSEntry.search(getToken(tokenID), startDN, filter, 0, 0,
            false, false));
    }     

    /**
     * Searchs the data store for objects that match the filter
     */
    public Set search2(String tokenID, String startDN, String filter,
        int numOfEntries, int timeLimit, boolean sortResults,
        boolean ascendingOrder)
            throws SMSException, SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::search dn: " + startDN
                    + " filter: " + filter);
        }
        return (SMSEntry.search(getToken(tokenID), startDN, filter,
            numOfEntries, timeLimit, sortResults, ascendingOrder));
    }

    /**
     * Searches the data store for objects that match the filter with an exclude set
     */
    public Set search3(String tokenID, String startDN, String filter,
        int numOfEntries, int timeLimit, boolean sortResults,
        boolean ascendingOrder, Set excludes)
            throws SMSException, SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::search dn: " + startDN
                    + " filter: " + filter + " excludes: " + excludes);
        }
        
        Iterator i = SMSEntry.search(getToken(tokenID), startDN, filter,
            numOfEntries, timeLimit, sortResults, ascendingOrder, excludes);
        
        Set<String> result = new HashSet<String>();
        
        while (i.hasNext()) {
            SMSDataEntry e = (SMSDataEntry)i.next();
            try {
                result.add(e.toJSONString());
            } catch (JSONException ex) {
                debug.error("SMSJAXRPCObjectImpl::problem performing search dn: " + startDN
                    + " filter: " + filter + " excludes: " + excludes, ex);
            }
        }
        
        return result;
    }

    /**
     * Checks if the provided DN exists. Used by PolicyManager.
     */
    public boolean entryExists(String tokenID, String objName)
            throws SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::exists dn: " + objName);
        }
        boolean entryExists = false;
        try {
            CachedSMSEntry ce = CachedSMSEntry.getInstance(getToken(tokenID),
                objName);
            if (ce.isDirty()) {
                ce.refresh();
            }
            entryExists = !(ce.getSMSEntry().isNewEntry());
        } catch (SMSException smse) {
            // Ignore the exception
        }
        return (entryExists);
    }

    /**
     * Returns the root suffix (i.e., base DN) for the SMS objects. All
     * SMSEntries will end with this root suffix.
     */
    public String getRootSuffix() throws RemoteException {
        if (baseDN == null) {
            baseDN = SMSEntry.getRootSuffix();
        }
        return (baseDN);
    }

    /**
     * Returns the root suffix (i.e., amsdkbase DN) for the UMS objects.
     * All UMSEntries will end with this root suffix.
     */
    public String getAMSdkBaseDN() throws RemoteException {
        if (amsdkbaseDN == null) {
            amsdkbaseDN = SMSEntry.getAMSdkBaseDN();
        }
        return (amsdkbaseDN);
    }

    /**
     * Validates service configuration attributes.
     *
     * @param token Single Sign On token.
     * @param validatorClass validator class name.
     * @param values Values to be validated.
     * @return <code>true</code> of values are valid.
     * @throws SMSException if value is not valid.
     * @throws SSOException if single sign on token is in valid.
     * @throws RemoteException if remote method cannot be invoked.
     */
    public boolean validateServiceAttributes(
        String token,
        String validatorClass,
        Set values
    ) throws SMSException, SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::validateServiceAttributes: " +
                validatorClass + " Values: " + values);
        }
        try {
            Class clazz = Class.forName(validatorClass);
            ServiceAttributeValidator v = (ServiceAttributeValidator)
                clazz.newInstance();
            return v.validate(values);
        } catch (InstantiationException ex) {
            throw new SMSException("sms-validator_cannot_instantiate_class");
        } catch (IllegalAccessException ex) {
            throw new SMSException("sms-validator_cannot_instantiate_class");
        } catch (ClassNotFoundException ex) {
            throw new SMSException("sms-validator_cannot_instantiate_class");
        }

    }

    // Implementation to receive requests from clients
    // Returns changes in the past <i>time</i> minutes
    public synchronized Set objectsChanged(int time) throws RemoteException {
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::objectsChanged: " + time);
        }
        Set answer = new HashSet();
        // Get the cache index for times upto time+2
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        // Add 1 minute to offset, the initial lookup
        calendar.add(Calendar.MINUTE, 1);
        for (int i = 0; i < time + 3; i++) {
            calendar.add(Calendar.MINUTE, -1);
            String cacheIndex = calendarToString(calendar);
            Set modDNs = (Set) cache.get(cacheIndex);
            if (modDNs != null)
                answer.addAll(modDNs);
        }
        return (answer);
    }

    // Implementation for SMSObjectListener
    public synchronized void objectChanged(String name, int type) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        String cacheIndex = calendarToString(calendar);
        Set modDNs = (Set) cache.get(cacheIndex);
        if (modDNs == null) {
            modDNs = new HashSet();
            cache.put(cacheIndex, modDNs);
            // Maintain cacheIndex
            cacheIndices.addFirst(cacheIndex);
            if (cacheIndices.size() > cacheSize) {
                String index = (String) cacheIndices.removeLast();
                cache.remove(index);
            }
        }
        String modItem = null;
        switch (type) {
        case ADD:
            modItem = "ADD:" + name;
            break;
        case DELETE:
            modItem = "DEL:" + name;
            break;
        default:
            modItem = "MOD:" + name;
        }
        modDNs.add(modItem);

        // If notification URLs are present, send notifications
        synchronized (notificationURLs) {
            for (Map.Entry<String, URL> entry : notificationURLs.entrySet()) {
                String id = entry.getKey();
                URL url = entry.getValue();

                // Construct NotificationSet
                Notification notification = new Notification(modItem);
                NotificationSet ns =
                    new NotificationSet(JAXRPCUtil.SMS_SERVICE);
                ns.addNotification(notification);
                try {
                    PLLServer.send(url, ns);
                    if (debug.messageEnabled()) {
                        debug.message("SMSJAXRPCObjectImpl:objectChanged sent notification to " +
                            "URL: " + url + " Data: " + ns);
                    }
                } catch (SendNotificationException ne) {
                    if (debug.warningEnabled()) {
                        debug.warning("SMSJAXRPCObjectImpl:objectChanged failed sending " +
                            "notification to: " + url + "\nRemoving " +
                            "URL from notification list.", ne);
                    }
                    // Remove the URL from Notification List
                    notificationURLs.remove(id);
                }
            }
        }
    }

    public void allObjectsChanged() {
        // do nothing. Not sending to remote clients
    }

    // Methods to register notification URLs
    public String registerNotificationURL(String url) throws RemoteException {
        initialize();
        // Default value if there are any issues with the registration process.
        String id = "0";
        try {
            // Check URL is not the local server
            if (!url.toLowerCase().startsWith(serverURL)) {
                synchronized (notificationURLs) {
                    URL notificationUrl = new URL(url);
                    // Don't add the URL again if we already have it registered
                    boolean alreadyRegistered = false;
                    for (Map.Entry<String, URL> entry : notificationURLs.entrySet()) {
                        if (notificationUrl.equals(entry.getValue())) {
                            // This allows us to return the existing entry ID to support clients being able to
                            // de-register the correct entry.
                            id = entry.getKey();
                            alreadyRegistered = true;
                            if (debug.messageEnabled()) {
                                debug.message("SMSJAXRPCObjectImpl:registerNotificationURL() - URL "
                                        + url + " already registered, returning existing ID " + id);
                            }
                            break;
                        }
                    }
                    // If we didn't find the url in our list, add it
                    if (!alreadyRegistered) {
                        String serverID = "";
                        try {
                            serverID = WebtopNaming.getAMServerID();
                        } catch (ServerEntryNotFoundException e) {
                            if (debug.messageEnabled()) {
                                debug.message("SMSJAXRPCObjectImpl:registerNotificationURL - " +
                                        "had a problem getting our serverID ", e);
                            }
                        }
                        // Generate a unique value that includes the serverID to have a better chance of being unique
                        // in a cluster should a de-register request end up on the wrong server.
                        id = SMSUtils.getUniqueID() + "_" + serverID;
                        notificationURLs.put(id, notificationUrl);
                        if (debug.messageEnabled()) {
                            debug.message("SMSJAXRPCObjectImpl:registerNotificationURL - " +
                                    "registered notification URL: " + url + " with ID " + id);
                        }
                    }
                }
            } else {
                // Cannot add this server for notifications
                if (debug.warningEnabled()) {
                    debug.warning("SMSJAXRPCObjectImpl:registerNotificationURL "
                            + "cannot add local server: " + url);
                }
            }
        } catch (MalformedURLException e) {
            if (debug.warningEnabled()) {
                debug.warning("SMSJAXRPCObjectImpl:registerNotificationURL "
                        + " invalid URL: " + url, e);
            }
        }
        return id;
    }

    public void deRegisterNotificationURL(String id) throws RemoteException {
        synchronized (notificationURLs) {
            URL url = notificationURLs.remove(id);
            if (url != null && debug.messageEnabled()) {
                debug.message("SMSJAXRPCObjectImpl.deRegisterNotificationURL() - URL "
                        + url + " de-registered for ID " + id);
            }
        }
    }

    /**
     * Processes object changed events from other severs
     * 
     * @param name DN of the object changed
     * @param type change type
     * @throws java.rmi.RemoteException
     */
    public void notifyObjectChanged(String name, int type)
            throws RemoteException {
        SMSNotificationManager.getInstance().objectChanged(name, type);
    }

    private static String calendarToString(Calendar calendar) {
        // Get year, month, date, hour and minute
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int date = calendar.get(Calendar.DATE);
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        StringBuilder sb = new StringBuilder(200);
        sb.append(serverURL);
        sb.append(":").append(year).append(month).append(date);
        sb.append(hour).append(minute);
        return (sb.toString());
    }

    /**
     * Returns SSOToken from token ID
     */
    private static SSOToken getToken(String tokenID) throws SSOException {
        if (initializationError != null)
            throw (initializationError);
        return (tokenMgr.createSSOToken(tokenID));
    }

    /**
     * Returns an array of ModificationItems converted from string
     * representation of mods. The string representation is of the format:
     * <pre>
     * <Modifications size="xx"> <AttributeValuePair event="ADD | REPLACE |
     * DELETE"> <Attribute name="attrName" /> <Value>...</Value>
     * </AttributeValuePair> </Modifications>
     * </pre>
     */
    static ModificationItem[] getModItems(String mods) throws SMSException {
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObject::StringToMods: " + mods);
        }
        ModificationItem[] answer = null;
        try {
            if (mods != null) {
                mods = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + mods;
                Document doc = XMLUtils.toDOMDocument(mods, debug);
                Node root = XMLUtils.getRootNode(doc, "Modifications");
                int modsSize = Integer.parseInt(XMLUtils.getNodeAttributeValue(
                        root, "size"));
                answer = new ModificationItem[modsSize];
                NodeList nl = root.getChildNodes();
                for (int i = 0; i < modsSize; i++) {
                    Node node = nl.item(i);
                    if (node.getNodeName().equals("AttributeValuePair")) {
                        String eventS = XMLUtils.getNodeAttributeValue(node,
                                "event");
                        int event = DirContext.ADD_ATTRIBUTE;
                        if (eventS.equals("REPLACE"))
                            event = DirContext.REPLACE_ATTRIBUTE;
                        else if (eventS.equals("DELETE"))
                            event = DirContext.REMOVE_ATTRIBUTE;
                        Node attrNode = XMLUtils
                                .getChildNode(node, "Attribute");
                        String attrName = XMLUtils.getNodeAttributeValue(
                                attrNode, "name");
                        Set vals = XMLUtils.getAttributeValuePair(node, false);
                        // Construct ModificationItem
                        BasicAttribute attr = new BasicAttribute(attrName);
                        for (Iterator it = vals.iterator(); it.hasNext();)
                            attr.add(it.next());
                        answer[i] = new ModificationItem(event, attr);
                    }
                }
            }
        } catch (Exception e) {
            throw (new SMSException(e,
                    "sms-JAXRPC-cannot-copy-fromModStringToModItem"));
        }
        return (answer);
    }
}
