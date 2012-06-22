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
 * $Id: CachedSMSEntry.java,v 1.16 2009/10/08 20:33:54 hengming Exp $
 *
 */

package com.sun.identity.sm;

import com.iplanet.am.util.SystemProperties;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.ldap.util.DN;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import java.util.Collections;
import java.util.HashMap;

/**
 * The class <code>CachedSchemaManagerImpl</code> provides interfaces to
 * manage the SMSEntry. It caches SMSEntries which is used by ServiceSchema and
 * ServiceConfig classes.
 */
public class CachedSMSEntry {

    // Notification method that will be called for ServiceSchemaManagerImpls
    protected static final String UPDATE_METHOD = "update";

    // Cache of CachedSMSEntries (static)
    protected static Map smsEntries = Collections.synchronizedMap(
        new HashMap(1000));

    // Instance variables
    
    // Set of ServiceSchemaManagerImpls and ServiceConfigImpls
    // that must be updated where entry changes
    protected Set serviceObjects = Collections.synchronizedSet(
        new HashSet());
    protected String notificationID;

    protected Set principals = Collections.synchronizedSet(
        new HashSet(10)); // Principals who have read access

    protected SSOToken token; // Valid SSOToken used for read

    protected String dn2Str;

    protected String dnRFCStr;

    protected SMSEntry smsEntry;

    // Flag that determines if this object can be used
    private boolean valid;
    
    // Flag to determine if the cached entry is dirty and 
    // must be refreshed along with the last update time & TTL
    private boolean dirty;
    private Object dirtyLock = new Object();
    static boolean ttlEnabled;
    static long lastUpdate;
    static long ttl = 1800000;  // 30 minutes
    
    
    // Private constructor, can be instantiated only via getInstance
    private CachedSMSEntry(SMSEntry e) {
        smsEntry = e;
        DN dn = new DN(e.getDN());
        dn2Str = dn.toString();
        dnRFCStr = dn.toRFCString().toLowerCase();
        token = e.getSSOToken();
        addPrincipal(token);
        valid = true;

        // Set the SMSEntry as read only
        smsEntry.setReadOnly();
        
        // Register for notifications
        notificationID = SMSEventListenerManager.notifyChangesToNode(
            token, smsEntry.getDN(), UPDATE_FUNC, this, null);

        // Write debug messages
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("CachedSMSEntry: New instance: " + dn);
        }
    }

    // ----------------------------------------------
    // Protected instance methods
    // ----------------------------------------------
    
    boolean isValid() {
        return valid;
    }
    
    // Used by JAXRPCObjectImpl
    public boolean isDirty() {
        if (ttlEnabled && !dirty &&
            ((System.currentTimeMillis() - lastUpdate) > ttl)) {
            synchronized (dirtyLock) {
                dirty = true;
            }
        }
        return dirty;
    }

    /**
     * Invoked by SMSEventListenerManager when entry has been changed.
     * Mark the entry as dirty and return. The method refresh() must be
     * called to read the attributes.
     */
    void update() {
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("CachedSMSEntry: update "
                    + "method called: " + dn2Str );
        }
        synchronized (dirtyLock) {
            dirty = true;
        }
    }
    
    /**
     * Reads the attributes from the datastore and send notifications to
     * objects caching this entry. Used by JAXRPCObjectImpl
     */
    public void refresh() {
        synchronized (dirtyLock) {
            if (SMSEntry.debug.messageEnabled()) {
                SMSEntry.debug.message("CachedSMSEntry: refresh "
                    + "method called: " + dn2Str );
            }

            // Read the LDAP attributes and update listeners
            boolean updated = false;
            dirty = true;
            try {
                SSOToken t = getValidSSOToken();
                if (t != null) {
                    smsEntry.read(t);
                    lastUpdate = System.currentTimeMillis();
                    updated = true;
                } else if (SMSEntry.debug.warningEnabled()) {
                    SMSEntry.debug.warning("CachedSMSEntry:update No VALID " +
                        "SSOToken found for dn: " + dn2Str);
                }
            } catch (SMSException e) {
                // Error in reading the attribtues, entry could be deleted
                // or does not have permissions to read the object
                SMSEntry.debug.error("Error in reading entry attributes: " +
                    dn2Str, e);
            } catch (SSOException ssoe) {
                // Error in reading the attribtues, SSOToken problem
                // Might have timed-out
                SMSEntry.debug.error("SSOToken problem in reading entry "
                    + "attributes: " + dn2Str, ssoe);
            }
            if (!updated) {
                // No valid SSOToken were foung
                // this entry is no long valid, remove from cache
                clear();
            }
            // Update service listeners either success or failure
            // updateServiceListeners(UPDATE_METHOD);
            updateServiceListeners(UPDATE_METHOD);
            dirty = false;
        }
    }
    
    /**
     * Updates the attributes from the provided <class>SMSEntry</class>
     * and marks the entry as non-dirty.
     * @param e object that contains the updated values for the attributes
     * @throws com.sun.identity.sm.SMSException
     */
    void refresh(SMSEntry e) throws SMSException {
        synchronized (dirtyLock) {
            smsEntry.refresh(e);
            updateServiceListeners(UPDATE_METHOD);
            dirty = false;
        }
    }
    
    /**
     * Clears the local variables and marks the entry as invalid
     * Called by the SMS objects that have an instance of CachedSMSEntry
     */
    void clear() {
        clear(true);
    }
    
    /**
     * Marks the object to be invalid and deregisters for notifications.
     * Called by the static method clearCache()
     * @param removeFromCache remove itself from cache if set to true
     */
    private void clear(boolean removeFromCache) {
        // this entry is no long valid, remove from cache
        SMSEventListenerManager.removeNotification(notificationID);
        notificationID = null;
        valid = false;
        synchronized(dirtyLock) {
            dirty = true;
        }
        // Remove from cache
        if (removeFromCache) {
            smsEntries.remove(dnRFCStr);
        }
    }
    
    // Returns a valid SSOToken that can be used for reading
    private SSOToken getValidSSOToken() {
        // Check if the cached SSOToken is valid
        if (!SMSEntry.tm.isValidToken(token)) {
            // Get a valid ssoToken from cached TokenIDs
            synchronized (principals) {
                for (Iterator items = principals.iterator(); items.hasNext();) {
                    String tokenID = (String) items.next();
                    try {
                        token = SMSEntry.tm.createSSOToken(tokenID);
                        if (SMSEntry.tm.isValidToken(token)) {
                            break;
                        }
                    } catch (SSOException ssoe) {
                        // SSOToken has expired, remove from list
                        items.remove();
                    }
                }
            }
        }
        // If there are no valid SSO Tokens return null
        if (principals.isEmpty()) {
            return (null);
        }
        return (token);
    }

    /**
     * Sends notifications to ServiceSchemaManagerImpl and ServiceConfigImpl
     * The method determines the object's method that would be invoked.
     * It could be either update() -- in which only the local instances are
     * updated, or updateAndNotify() -- in which case the listeners are also
     * notified.
     * @param method either "update" or "updateAndNotify"
     */
    private void updateServiceListeners(String method) {
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("CachedSMSEntry::updateServiceListeners "
                    + "method called: " + dn2Str);
        }
        // Inform the ServiceSchemaManager's of changes to attributes
        ArrayList tmpServiceObjects = new ArrayList();
        synchronized (serviceObjects) {
            for(Iterator objs = serviceObjects.iterator(); objs.hasNext();) {
                tmpServiceObjects.add(objs.next());
            }
        }

        for(Iterator objs = tmpServiceObjects.iterator(); objs.hasNext();){
            try {
                Object obj = objs.next();
                Method m = obj.getClass().getDeclaredMethod(
                    method, (Class[]) null);
                m.invoke(obj, (Object[]) null);
            } catch (Throwable e) {
                SMSEntry.debug.error("CachedSMSEntry::unable to " +
                    "deliver notification(" + dn2Str + ")", e);
            }
        }
    }

    /**
     * Method to add objects that needs notifications
     */
    protected void addServiceListener(Object o) {
        serviceObjects.add(o);
    }
    
    /**
     * Method to remove objects that needs notifications
     */
    protected void removeServiceListener(Object o) {
        serviceObjects.remove(o);
        if (serviceObjects.isEmpty()) {
            SMSEventListenerManager.removeNotification(notificationID);
            notificationID = null;
        }
    }

    synchronized void addPrincipal(SSOToken t) {
        // Making a local copy to avoid synchronization problems
        principals.add(t.getTokenID().toString());
    }

    boolean checkPrincipal(SSOToken t) {
        return (principals.contains(t.getTokenID().toString()));
    }

    public SMSEntry getSMSEntry() {
        return (smsEntry);
    }

    public SMSEntry getClonedSMSEntry() {
        if (isDirty()) {
            refresh();
        }
        try {
            return ((SMSEntry) smsEntry.clone());
        } catch (CloneNotSupportedException c) {
            SMSEntry.debug.error("Unable to clone SMSEntry: " + smsEntry, c);
        }
        return (null);
    }

    boolean isNewEntry() {
        if (isDirty()) {
            refresh();
        }
        return (smsEntry.isNewEntry());
    }

    String getDN() {
        return (dn2Str);
    }

    // ----------------------------------------------
    // protected static methods
    // ----------------------------------------------
    // Used by ServiceSchemaManager
    static CachedSMSEntry getInstance(SSOToken t, ServiceSchemaManagerImpl ssm,
            String serviceName, String version) throws SMSException {
        CachedSMSEntry entry = null;
        String dn = ServiceManager.getServiceNameDN(serviceName, version);
        try {
            entry = getInstance(t, dn);
            entry.addServiceListener(ssm);
        } catch (SSOException ssoe) {
            SMSEntry.debug.error("SMS: Invalid SSOToken: ", ssoe);
        }
        return (entry);
    }

    public static CachedSMSEntry getInstance(SSOToken t, String dn)
            throws SMSException, SSOException {
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("CachedSMSEntry::getInstance: " + dn);
        }
        String cacheEntry = (new DN(dn)).toRFCString().toLowerCase();
        CachedSMSEntry answer = (CachedSMSEntry) smsEntries.get(cacheEntry);
        if ((answer == null) || !answer.isValid()) {
            // Construct the SMS entry. Should be outside the synchronized
            // block since SMSEntry call delegation which in turn calls
            // policy, idrepo, special repo and SMS again
            CachedSMSEntry tmp = new CachedSMSEntry(new SMSEntry(t, dn));
            synchronized (smsEntries) {
                if (((answer = (CachedSMSEntry) smsEntries.get(cacheEntry))
                    == null) || !answer.isValid()) {
                    // Add it to cache
                    answer = tmp;
                    smsEntries.put(cacheEntry, answer);
                }
            }
        }
        
        // Check if user has permissions
        if (!answer.checkPrincipal(t)) {
            // Read the SMS entry as that user, and ignore the results
            new SMSEntry(t, dn);
            answer.addPrincipal(t);
        }
        
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("CachedSMSEntry: obtained instance: " + dn);
        }
        if (answer.isNewEntry()) {
            SMSEntry sEntry = answer.getSMSEntry();
            sEntry.dn = dn;
        }
        return (answer);
    }
    
    static void initializeProperties() {
        // Initialize the TTL
        String ttlEnabledString = SystemProperties.get(
            Constants.SMS_CACHE_TTL_ENABLE, "false");
        ttlEnabled = Boolean.valueOf(ttlEnabledString).booleanValue();
        if (ttlEnabled) {
            String cacheTime = SystemProperties.get(Constants.SMS_CACHE_TTL);
            if (cacheTime != null) {
                try {
                    ttl = Long.parseLong(cacheTime);
                    // Convert minutes to milliseconds
                    ttl = ttl * 60* 1000;
                } catch (NumberFormatException nfe) {
                    SMSEntry.debug.error("CachedSMSEntry:init Invalid time " +
                        "for SMS Cache TTL: " + cacheTime);
                    ttl = 1800000; // 30 minutes, default
                }
            }
        }
    }

    // Clears the cache
    static void clearCache() {
        synchronized (smsEntries) {
            for (Iterator items = smsEntries.values().iterator();
                items.hasNext();) {
                CachedSMSEntry cEntry = (CachedSMSEntry) items.next();
                // this entry is no long valid
                cEntry.clear(false);
            }
            // Remove all entries from cache
            smsEntries.clear();
        }
    }

    // ----------------------------------------------
    // protected instance methods for ServiceSchemaManager
    // ----------------------------------------------
    String getXMLSchema() {
        String[] schema = smsEntry.getAttributeValues(SMSEntry.ATTR_SCHEMA);
        if (schema == null) {
            // The entry could be deleted, hence return null
            return (null);
        }
        // Since schema is a single valued attribute
        return (schema[0]);
    }

    void writeXMLSchema(SSOToken token, InputStream xmlServiceSchema)
            throws SSOException, SMSException, IOException {
        int lengthOfStream = xmlServiceSchema.available();
        byte[] byteArray = new byte[lengthOfStream];
        xmlServiceSchema.read(byteArray, 0, lengthOfStream);
        writeXMLSchema(token, new String(byteArray));
    }

    void writeXMLSchema(SSOToken token, String xmlSchema) throws SSOException,
            SMSException {
        // Validate SSOtoken
        SMSEntry.validateToken(token);
        // Replace the attribute in the directory
        String[] attrValues = { xmlSchema };
        SMSEntry e = getClonedSMSEntry();
        e.setAttribute(SMSEntry.ATTR_SCHEMA, attrValues);
        e.save(token);
        refresh(e);
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("CachedSMSEntry::writeXMLSchema: "
                    + "successfully wrote the XML schema for dn: " + e.getDN());
        }
    }
    
    // Protected static variables
    private static Class CACHED_SMSENTRY;
    private static Method UPDATE_FUNC;
    static {
        try {
            CACHED_SMSENTRY = Class.forName(
                "com.sun.identity.sm.CachedSMSEntry");
            UPDATE_FUNC = CACHED_SMSENTRY.getDeclaredMethod(
                UPDATE_METHOD, (Class[]) null);
            initializeProperties();
        } catch (Exception e) {
            // Should not happen, ignore
        }
    }
}
