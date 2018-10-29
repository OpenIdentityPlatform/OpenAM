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
 * $Id: CachedSMSEntry.java,v 1.16 2009/10/08 20:33:54 hengming Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.sm;

import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.google.common.collect.Ordering;
import org.forgerock.opendj.ldap.DN;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;

/**
 * The class <code>CachedSchemaManagerImpl</code> provides interfaces to
 * manage the SMSEntry. It caches SMSEntries which is used by ServiceSchema and
 * ServiceConfig classes.
 */
public class CachedSMSEntry implements SMSEventListener {

    // Cache of CachedSMSEntries (static)
    private static Map<String,CachedSMSEntry> smsEntries = new ConcurrentHashMap<String,CachedSMSEntry>(1000);

    // Instance variables

    /**
     * Set of listeners that must be updated when the entry changes. As these may have equals/hashCode
     * implementations that change as a result of updates, we use the
     * {@link System#identityHashCode(Object) identity} of the object instances to test for equality.
     */
    private final Set<SMSEntryUpdateListener> serviceObjects = new ConcurrentSkipListSet<>(Ordering.arbitrary());
    private final SMSEventListenerManager.Subscription subscription;

    protected Set principals = Collections.synchronizedSet(
        new HashSet(10)); // Principals who have read access

    protected SSOToken token; // Valid SSOToken used for read

    private String dn2Str;

    private String dnRFCStr;

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
        DN dn = DN.valueOf(e.getDN());
        dn2Str = dn.toString();
        dnRFCStr = dn.toString().toLowerCase();
        token = e.getSSOToken();
        addPrincipal(token);
        valid = true;

        // Set the SMSEntry as read only
        smsEntry.setReadOnly();
        
        // Register for notifications
        subscription = SMSEventListenerManager.registerForNotifyChangesToNode(smsEntry.getDN(), this);

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
            ((currentTimeMillis() - lastUpdate) > ttl)) {
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
                    lastUpdate = currentTimeMillis();
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

            updateServiceListeners();
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
            updateServiceListeners();
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
        subscription.cancel();
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
     * Sends notifications to any object that has added itself as a listener.
     */
    private void updateServiceListeners() {
        SMSEntry.debug.message("CachedSMSEntry::updateServiceListeners method called: {}", dn2Str);

        // Inform the ServiceSchemaManager's of changes to attributes
        for (SMSEntryUpdateListener updateListener : serviceObjects) {
            try {
                updateListener.update();
            } catch (Throwable e) {
                SMSEntry.debug.error("CachedSMSEntry::unable to update service listener ({})", dn2Str, e);
            }
        }
    }

    /**
     * Method to add objects that needs notifications
     */
    protected void addServiceListener(SMSEntryUpdateListener updateListener) {
        serviceObjects.add(updateListener);
    }
    
    /**
     * Method to remove objects that needs notifications
     */
    protected void removeServiceListener(SMSEntryUpdateListener updateListener) {
        serviceObjects.remove(updateListener);
        if (serviceObjects.isEmpty()) {
            subscription.cancel();
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
        String cacheEntry = DN.valueOf(dn).toString().toLowerCase();
        CachedSMSEntry answer = (CachedSMSEntry) smsEntries.get(cacheEntry);
        if ((answer == null) || !answer.isValid()) {
            // Construct the SMS entry. Should be outside the synchronized
            // block since SMSEntry call delegation which in turn calls
            // policy, idrepo, special repo and SMS again
            CachedSMSEntry tmp = new CachedSMSEntry(new SMSEntry(t, dn));
//            synchronized (smsEntries) {
                if (((answer = (CachedSMSEntry) smsEntries.get(cacheEntry))
                    == null) || !answer.isValid()) {
                    // Add it to cache
                    answer = tmp;
                    smsEntries.put(cacheEntry, answer);
                }
//            }
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
        for (CachedSMSEntry entry: smsEntries.values()) {
        	entry.clear(true);
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

    @Override
    public void notifySMSEvent(DN dn, int event) {
        update();
    }

    /**
     * Defines a listener that needs to be updated when an SMSEntry has changed.
     */
    interface SMSEntryUpdateListener {
        /**
         * The update operation to perform when a change has occurred.
         */
        void update();
    }
}
