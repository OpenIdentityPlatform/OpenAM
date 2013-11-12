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
 * $Id: SMSJAXRPCObject.java,v 1.21 2009/10/28 04:24:26 hengming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.sm.jaxrpc;

import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import com.sun.identity.shared.ldap.util.DN;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.services.comm.client.NotificationHandler;
import com.iplanet.services.comm.client.PLLClient;
import com.iplanet.services.comm.share.Notification;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.jaxrpc.JAXRPCUtil;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSObject;
import com.sun.identity.sm.SMSObjectListener;
import com.sun.identity.sm.SMSSchema;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.sm.SMSDataEntry;
import com.sun.identity.sm.SMSNotificationManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;

public class SMSJAXRPCObject extends SMSObject implements SMSObjectListener {

    private static SOAPClient client;

    /**
     * JAXRPC Version String variable name.
     */
    public static final String AMJAXRPCVERSIONSTR = "AM_JAXRPC_VERSION";

    /**
     * JAXRPC Version String.
     */
    // Since we introduced the new API getAMSdkBaseDN, now the client
    // version is 11 in opensso & AM 7.1 patch 1.
    public static final String AMJAXRPCVERSION = "11";
    
    public static final String NOTIFICATION_PROPERTY = 
        "com.sun.identity.sm.notification.enabled";
    
    public SMSJAXRPCObject() {
        // Construct the SOAP client
        client = new SOAPClient(JAXRPCUtil.SMS_SERVICE);
    }
    
    private void initializeNotification() {
         if (!initializedNotification) {
            // If cache is enabled, register for notification to maintian
            // internal cache of entriesPresent
            // Add this object to receive notifications to maintain
            // internal cache of entries present and not present
            if (SMSNotificationManager.isCacheEnabled()) {
                SMSNotificationManager.getInstance().registerCallbackHandler(
                    this);
            }
            initializedNotification = true;
        }
    }
    
    /**
     * Reads in the object from persistent store. It assumes the object name and
     * the ssoToken are valid. If the entry does not exist the method should
     * return <code>null</code>
     */
    public Map read(SSOToken token, String objName) throws SMSException,
            SSOException {
        try {
            String[] objs = { token.getTokenID().toString(), objName };
            Map attrs = (Map) client.send(client.encodeMessage("read", objs), 
                Session.getLBCookie(token.getTokenID().toString()), null);
            // Return CaseInsesitiveHashMap to be consistent with server side
            return ((attrs == null) ? null : new CaseInsensitiveHashMap(attrs));
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:read -- Exception:", re);
            throw (new SMSException(re, "sms-JAXRPC-cannot-read"));
        }
    }

    /**
     * Creates an entry in the persistent store. Throws an exception if the
     * entry already exists
     */
    public void create(SSOToken token, String objName, Map attributes)
            throws SMSException, SSOException {
        try {
            Object[] objs = { token.getTokenID().toString(), objName,
                    attributes };
            client.send(client.encodeMessage("create", objs), 
                Session.getLBCookie(token.getTokenID().toString()), null);
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:create -- Exception:", re);
            throw (new SMSException(re, "sms-JAXRPC-cannot-create"));
        }
    }

    /**
     * Modifies the attributes to the object.
     */
    public void modify(SSOToken token, String objName, ModificationItem[] mods)
            throws SMSException, SSOException {
        try {
            Object[] objs = { token.getTokenID().toString(), objName,
                    toMods(mods) };
            client.send(client.encodeMessage("modify", objs), 
                Session.getLBCookie(token.getTokenID().toString()), null);
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:modify -- Exception:", re);
            throw (new SMSException(re, "sms-JAXRPC-cannot-modify"));
        }
    }

    /**
     * Delete the entry in the datastore. This should delete sub-entries also
     */
    public void delete(SSOToken token, String objName) throws SMSException,
            SSOException {
        try {
            String[] objs = { token.getTokenID().toString(), objName };
            client.send(client.encodeMessage("delete", objs), 
                Session.getLBCookie(token.getTokenID().toString()), null);
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:delete -- Exception:", re);
            throw (new SMSException(re, "sms-JAXRPC-cannot-delete"));
        }
    }

    /**
     * Returns the suborganization names. Returns a set of SMSEntry objects that
     * are suborganization names. The paramter <code>numOfEntries</code>
     * identifies the number of entries to return, if <code>0</code> returns
     * all the entries.
     */
    public Set searchSubOrgNames(SSOToken token, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder,
            boolean recursive) throws SMSException, SSOException {
        try {
            Object[] objs = { token.getTokenID().toString(), dn, filter,
                    new Integer(numOfEntries), Boolean.valueOf(sortResults),
                    Boolean.valueOf(ascendingOrder), Boolean.valueOf(recursive)
            };
            return ((Set) client.send(client.encodeMessage("searchSubOrgNames",
                    objs), Session.getLBCookie(token.getTokenID().toString()),
                    null));
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject.searchSubOrgNames", re);
            throw (new SMSException(re, "sms-JAXRPC-suborg-cannot-search"));
        }
    }

    /**
     * Returns the organization names. Returns a set of SMSEntry objects that
     * are organization names. The paramter <code>numOfEntries</code>
     * identifies the number of entries to return, if <code>0</code> returns
     * all the entries.
     */
    public Set searchOrganizationNames(SSOToken token, String dn,
            int numOfEntries, boolean sortResults, boolean ascendingOrder,
            String serviceName, String attrName, Set values)
            throws SMSException, SSOException {
        try {
            Object[] objs = { token.getTokenID().toString(), dn,
                    new Integer(numOfEntries), Boolean.valueOf(sortResults),
                    Boolean.valueOf(ascendingOrder), serviceName, attrName, 
                    values};
            return ((Set) client.send(client.encodeMessage(
                    "searchOrganizationNames", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null));
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject.searchOrganizationNames", re);
            throw (new SMSException(re, "sms-JAXRPC-org-cannot-search"));
        }
    }

    /**
     * Returns the sub-entries. Returns a set of SMSEntry objects that are
     * sub-entries. The paramter <code>numOfEntries</code> identifies the
     * number of entries to return, if <code>0</code> returns all the entries.
     */
    public Set subEntries(SSOToken token, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder)
            throws SMSException, SSOException {
        try {
            Object[] objs = { token.getTokenID().toString(), dn, filter,
                    new Integer(numOfEntries), Boolean.valueOf(sortResults),
                    Boolean.valueOf(ascendingOrder) };
            return ((Set) client.send(client.encodeMessage("subEntries", objs),
                    Session.getLBCookie(token.getTokenID().toString()), null));
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:subEntries -- Exception:", re);
            throw (new SMSException(re, "sms-JAXRPC-subentry-cannot-search"));
        }
    }

    /**
     * Returns the sub-entries. Returns a set of SMSEntry objects that are
     * sub-entries. The paramter <code>numOfEntries</code> identifies the
     * number of entries to return, if <code>0</code> returns all the entries.
     */
    public Set schemaSubEntries(SSOToken token, String dn, String filter,
            String sidFilter, int numOfEntries, boolean sortResults,
            boolean ascendingOrder) throws SMSException, SSOException {
        try {
            Object[] objs = { token.getTokenID().toString(), dn, filter,
                sidFilter, new Integer(numOfEntries),
                Boolean.valueOf(sortResults), 
                Boolean.valueOf(ascendingOrder) };
            return ((Set) client.send(client.encodeMessage("schemaSubEntries",
                objs), Session.getLBCookie(token.getTokenID().toString()), 
                null));
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:subEntries -- Exception:", re);
            throw (new SMSException(re,
                    "sms-JAXRPC-schemasubentry-cannot-search"));
        }
    }

    /**
     * Searches the data store for objects that match the filter
     */
    public Iterator search(SSOToken token, String startDN, String filter,
        int numOfEntries, int timeLimit, boolean sortResults,
        boolean ascendingOrder, Set excludes)
        throws SMSException, SSOException {
        try {
            Object[] objs = { token.getTokenID().toString(), startDN, filter,
                Integer.valueOf(numOfEntries), Integer.valueOf(timeLimit),
                Boolean.valueOf(sortResults), Boolean.valueOf(ascendingOrder), excludes };
            
            Set<String> searchResults = ((Set<String>) client.send(client.encodeMessage("search3", objs),
                    Session.getLBCookie(token.getTokenID().toString()),
                    null));
            
            Iterator result = null;
            
            if (searchResults != null && !searchResults.isEmpty()) {
                Set<SMSDataEntry> dataEntries = new HashSet<SMSDataEntry>(searchResults.size());
                for (String jsonString : searchResults) {
                    dataEntries.add(new SMSDataEntry(jsonString));
                }
                result = dataEntries.iterator();
            } else {
                result = Collections.EMPTY_SET.iterator();
            }
            
            return result;
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:search -- Exception:", re);
            throw (new SMSException(re, "sms-JAXRPC-error-in-searching"));
        }
    }

    /**
     * Searchs the data store for objects that match the filter
     */
    public Set search(SSOToken token, String startDN, String filter,
        int numOfEntries, int timeLimit, boolean sortResults,
        boolean ascendingOrder) throws SMSException, SSOException {
        try {
            Object[] objs = { token.getTokenID().toString(), startDN, filter,
                new Integer(numOfEntries), new Integer(timeLimit),
                Boolean.valueOf(sortResults), Boolean.valueOf(ascendingOrder) };
            return ((Set) client.send(client.encodeMessage("search2", objs),
                    Session.getLBCookie(token.getTokenID().toString()),
                    null));
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:search -- Exception:", re);
            throw (new SMSException(re, "sms-JAXRPC-error-in-searching"));
        }
    }

    /**
     * Checks if the provided DN exists. Used by PolicyManager.
     */
    public boolean entryExists(SSOToken token, String dn) {
        dn = (new DN(dn)).toRFCString().toLowerCase();
        // Check the caches
        if (SMSNotificationManager.isCacheEnabled() &&
            entriesPresent.contains(dn)) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject: entry present in cache: " + dn);
            }
            return (true);
        } else if (SMSNotificationManager.isCacheEnabled() &&
            entriesNotPresent.contains(dn)) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject: entry present in "
                        + "not-present-cache: " + dn);
            }
            return (false);
        }

        // Since not present in cache, make a RPC
        boolean entryExists = false;
        try {
            String[] objs = { token.getTokenID().toString(), dn };
            Boolean b = (Boolean) client.send(client.encodeMessage(
                    "entryExists", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null);
            entryExists = b.booleanValue();
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:entryExists -- Exception:", re);
            return (false);
        }

        // Update the cache
        if (entryExists && SMSNotificationManager.isCacheEnabled()) {
            initializeNotification();
            entriesPresent.add(dn);
            if (entriesPresent.size() > entriesPresentCacheSize) {
                // Remove the first entry
                synchronized (entriesPresent) {
                    Iterator items = entriesPresent.iterator();
                    if (items.hasNext()) {
                        items.remove();
                    }
                }
            }
        } else if (SMSNotificationManager.isCacheEnabled()) {
            initializeNotification();
            entriesNotPresent.add(dn);
            if (entriesNotPresent.size() > entriesPresentCacheSize) {
                // Remove the first entry
                synchronized (entriesNotPresent) {
                    Iterator items = entriesNotPresent.iterator();
                    if (items.hasNext()) {
                        items.remove();
                    }
                }
            }
        }
        return (entryExists);
    }

    /**
     * Returns the root suffix (i.e., base DN) for the SMS objects. All
     * SMSEntries will end with this root suffix.
     */
    public String getRootSuffix() {
        if (baseDN == null) {
            try {
                baseDN = (String) client.send(client.encodeMessage(
                        "getRootSuffix", null), null, null);
            } catch (Exception re) {
                debug.error("SMSJAXRPCObject:getRootSuffix:Exception:", re);
            }
        }
        return (baseDN);
    }

    /**
     * Returns the session root suffix (i.e., base DN) for the SMS objects. All
     * SMSEntries will end with this root suffix.
     */
    public String getSessionRootSuffix() {
        if (baseDN == null) {
            try {
                baseDN = (String) client.send(client.encodeMessage(
                        "getSessionRootSuffix", null), null, null);
            } catch (Exception re) {
                debug.error("SMSJAXRPCObject:getSessionRootSuffix:Exception:", re);
            }
        }
        return (baseDN);
    }


    /**
     * Returns the root suffix (i.e., base DN) for the UMS objects.
     * All UMSEntries will end with this root suffix.
     */
    public String getAMSdkBaseDN() {
        if (amsdkbaseDN == null) {
            try {
                amsdkbaseDN = (String) client.send(client.encodeMessage(
                    "getAMSdkBaseDN", null), null, null);
            } catch (Exception re) {
                debug.error("SMSJAXRPCObject:getAMSdkBaseDN:Exception:", re);
            }
        }
        return (amsdkbaseDN);
    }

    /**
     * Validates service configuration attributes.
     *
     * @param token Single Sign On token.
     * @param validatorClass validator class name.
     * @return <code>true</code> of values are valid.
     * @param values Values to be validated.
     * @throws SSOException if single sign on token is in valid.
     * @throws SMSException if value is invalid.
     */
    public boolean validateServiceAttributes(
        SSOToken token,
        String validatorClass, 
        Set values
    ) throws SMSException, SSOException {
        try {
            Object[] objs = {token.getTokenID().toString(), validatorClass, 
                values};
            Boolean b = (Boolean)client.send(client.encodeMessage(
                "validateServiceAttributes", objs), null, null);
            return b.booleanValue();
        } catch (SSOException e) {
            throw e;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObjectvalidateServiceAttributes", re);
            throw new SMSException(re,
                "sms-JAXRPC-attribute-values-validation-failed");
        }
    }


    /**
     * Registration for event change notifications.
     * Only SMSNotificationManager would be calling this method to
     * register itself
     */
    public void registerCallbackHandler(SMSObjectListener changeListener)
        throws SMSException {
        objectListener = changeListener;
        if (!notificationInitialized) {
            // Check if notification is enabled 
            // Default the notification enabled to true if the property
            // is not found for backward compatibility.
            String notificationFlag = SystemProperties.get(
                NOTIFICATION_PROPERTY, "true");

            if (notificationFlag.equalsIgnoreCase("true")) {
                // Check if notification URL is provided
                try {
                    URL url = WebtopNaming.getNotificationURL();
                    // Register with PLLClient for notificaiton
                    PLLClient.addNotificationHandler(
                        JAXRPCUtil.SMS_SERVICE,
                        new SMSNotificationHandler());
                    // Register for notification with SMS Server
                    client.send("registerNotificationURL",
                        url.toString(), null, null);
                    if (debug.messageEnabled()) {
                        debug.message("SMSJAXRPCObject: Using " +
                            "notification mechanism for cache updates: " + url);
                    }
                } catch (Exception e) {
                    // Use polling mechanism to update caches
                    if (debug.warningEnabled()) {
                        debug.warning("SMSJAXRPCObject: Registering for " +
                            "notification via URL failed: " + e.getMessage() +
                            "\nUsing polling mechanism for updates");
                    }

                    // Start Polling thread only if enabled.
                    startPollingThreadIfEnabled(
                        getCachePollingInterval());
                }
            } else {
                // Start Polling thread only if enabled.
                startPollingThreadIfEnabled(getCachePollingInterval());
            }
            notificationInitialized = true;
        }
    }
    
    /**
     * Returns the polling interval in minutes
     * @return polling interval in minutes
     */
    private int getCachePollingInterval() {
        // If the property is not configured, default it to 1 minute. 
        String cachePollingTimeStr = SystemProperties.get(
            Constants.CACHE_POLLING_TIME_PROPERTY);
        int cachePollingInterval = Constants.DEFAULT_CACHE_POLLING_TIME;
        if (cachePollingTimeStr != null) { 
            try {
                cachePollingInterval = Integer.parseInt(cachePollingTimeStr);
            } catch (NumberFormatException nfe) {
                debug.error("EventListener::NotificationRunnable:: "
                        + "Invalid Polling Time: " + cachePollingTimeStr + 
                        " Defaulting to " +
                        Constants.DEFAULT_CACHE_POLLING_TIME  + " minute", nfe);
            }
        }        
        return cachePollingInterval;
    }    
    
    private static void startPollingThreadIfEnabled(int cachePollingInterval) {
        if (cachePollingInterval > 0) {
            if (debug.messageEnabled()) {
                debug.message("EventListener: Polling mode enabled. " +
                        "Starting the polling thread..");
            }
            // Run in polling mode
            NotificationRunnable nr = new NotificationRunnable(
                    cachePollingInterval);
            SystemTimerPool.getTimerPool().schedule(nr, new Date(
                    ((System.currentTimeMillis() + nr.getRunPeriod()) / 1000)
                    * 1000));
        } else {
            if (debug.warningEnabled()) {
                debug.warning("EventListener: Polling mode DISABLED. " +
                    Constants.CACHE_POLLING_TIME_PROPERTY + "=" +
                    cachePollingInterval);
            }
        }
    }

    public void objectChanged(String dn, int type) {
        dn = (new DN(dn)).toRFCString().toLowerCase();
        if (type == DELETE) {
            // Remove from entriesPresent Set
            entriesPresent.remove(dn);
        } else if (type == ADD) {
            entriesNotPresent.remove(dn);
        }
    }

    public void allObjectsChanged() {
        // do nothing
    }

    // Converts ModificationItem to String
    static String toMods(ModificationItem[] mods) throws SMSException {
        if (mods == null)
            return (null);
        StringBuilder sb = new StringBuilder(100);
        sb.append("<Modifications size=\"");
        sb.append(mods.length);
        sb.append("\">");
        for (int i = 0; i < mods.length; i++) {
            sb.append("<AttributeValuePair event=\"");
            switch (mods[i].getModificationOp()) {
            case DirContext.ADD_ATTRIBUTE:
                sb.append("ADD");
                break;
            case DirContext.REPLACE_ATTRIBUTE:
                sb.append("REPLACE");
                break;
            case DirContext.REMOVE_ATTRIBUTE:
                sb.append("DELETE");
                break;
            }
            sb.append("\"><Attribute name=\"");
            Attribute attr = mods[i].getAttribute();
            sb.append(attr.getID());
            sb.append("\"/>");
            int size = attr.size();
            for (int j = 0; j < size; j++) {
                sb.append("<Value>");
                try {
                    sb.append(
                        SMSSchema.escapeSpecialCharacters((String)attr.get(j)));
                } catch (NamingException ne) {
                    throw (new SMSException(ne,
                            "sms-JAXRPC-cannot-copy-fromModItemToString"));
                }
                sb.append("</Value>");
            }
            sb.append("</AttributeValuePair>");
        }
        sb.append("</Modifications>");
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObject::ModsToString: " + sb.toString());
        }
        return (sb.toString());
    }

    // sends notifications
    static void sendNotification(String modItem) {
        String dn = modItem.substring(4);
        int type = SMSObjectListener.MODIFY;
        if (modItem.startsWith("ADD:"))
            type = SMSObjectListener.ADD;
        if (modItem.startsWith("DEL:"))
            type = SMSObjectListener.DELETE;
        // Send notification
        objectListener.objectChanged(dn, type);
    }

    // Static variables
    private static String baseDN;

    private static String amsdkbaseDN;
    
    private static int entriesPresentCacheSize = 1000;

    private static Set entriesPresent = Collections.synchronizedSet(
        new LinkedHashSet(entriesPresentCacheSize));

    private static Set entriesNotPresent = Collections.synchronizedSet(
        new LinkedHashSet(entriesPresentCacheSize));
    
    // Used to update entriesPresent & entriesNotPresent
    private static boolean initializedNotification;
    
    // Used to register for notifications from Server
    private static boolean notificationInitialized;

    protected static boolean isLocal;

    private static Debug debug = Debug.getInstance("amSMSClient");

    private static SMSObjectListener objectListener;

    // Inner class to check for notifications
    static class NotificationRunnable extends GeneralTaskRunnable {

        // 1 minute
        static long WAIT_BEFORE_RETRY = 1 * 1000 * 60;
        
        int pollingTime;

        volatile long sleepTime;
        
        SOAPClient client;

        NotificationRunnable(int interval) {
            pollingTime = interval;            
            sleepTime = pollingTime * 1000 * 60;
            client = new SOAPClient(JAXRPCUtil.SMS_SERVICE);
        }

        // Get the modification list and send notifications
        public void run() {
            try {
                Object obj[] = { new Integer(pollingTime) };
                Set mods = (Set) client.send(client.encodeMessage(
                        "objectsChanged", obj), null, null);
                if (debug.messageEnabled()) {
                    debug.message("SMSJAXRPCObject:"
                            + "NotificationRunnable retrived changes: "
                            + mods);
                }
                Iterator items = mods.iterator();
                while (items.hasNext()) {
                    sendNotification((String) items.next());
                }
                sleepTime = pollingTime * 1000 * 60;
            } catch (NumberFormatException nfe) {
                // Should not happend
                debug.warning("SMSJAXRCPObject::NotificationRunnable:run "
                        + "Number Format Exception for polling Time: "
                        + pollingTime, nfe);
            } catch (SMSException smse) {
                sleepTime = WAIT_BEFORE_RETRY;
                if (smse.getExceptionCode() != 
                    SMSException.STATUS_REPEATEDLY_FAILED)
                debug.warning("SMSJAXRPCObject::NotificationRunnable:run "
                        + "SMSException", smse);
            } catch (InterruptedException ie) {
                sleepTime = WAIT_BEFORE_RETRY;
                debug.warning("SMSJAXRPCObject::NotificationRunnable:run "
                        + "Interrupted Exception", ie);
            } catch (Exception re) {
                sleepTime = pollingTime * 1000 * 60;
                debug.warning("SMSJAXRPCObject::NotificationRunnable:run "
                        + "Exception", re);
            }
        }
        
        public long getRunPeriod() {
            return sleepTime;
        }
        
        public boolean addElement(Object obj) {
            return false;
        }
        
        public boolean removeElement(Object obj) {
            return false;
        }
        
        public boolean isEmpty() {
            return false;
        }
        
    }

    // Inner class handle SMS change notifications
    static class SMSNotificationHandler implements NotificationHandler {
        SMSNotificationHandler() {
            // Empty constructor
        }

        // Process the notification objects
        public void process(Vector notifications) {
            for (int i = 0; i < notifications.size(); i++) {
                Notification notification = (Notification) notifications
                        .elementAt(i);
                String content = notification.getContent();
                if (debug.messageEnabled()) {
                    debug.message("SMSJAXRPCObject:SMSNotificationHandler: "
                            + " received notification: " + content);
                }
                // Send notification
                sendNotification(content);
            }
        }
    }
}
