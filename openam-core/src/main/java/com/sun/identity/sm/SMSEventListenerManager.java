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
 * $Id: SMSEventListenerManager.java,v 1.12 2009/01/28 05:35:03 ww203982 Exp $
 *
 * Portions Copyright 2015-2016 ForgeRock AS.
 */

package com.sun.identity.sm;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.SearchScope;

import com.sun.identity.shared.debug.Debug;

/**
 * Receives notifications for all SMS object changes from
 * <class>SMSNotificationManager</class> and dispatches the same to <class>
 * CachedSMSEntry</class> and <class>CachedSubEntries</class>. This class
 * also handles the case of sending deleted event for recursive deletes.
 */
class SMSEventListenerManager implements SMSObjectListener {

    // All Notification Objects list
    private static final Map<String, NotificationObject> notificationObjects =
        Collections.synchronizedMap(new HashMap<String, NotificationObject>());
    
    // CachedSMSEntry objects
    private static final Map<String, Set<NotificationObject>> nodeChanges =
            Collections.synchronizedMap(new HashMap<String, Set<NotificationObject>>());
    
    // CachedSubEntries objects
    private static final Map<String, Set<NotificationObject>> subNodeChanges =
            Collections.synchronizedMap(new HashMap<String, Set<NotificationObject>>());

    // Static Initialization variables
    private static Debug debug = SMSEntry.eventDebug; 
    protected static boolean initialized;

    static void initialize() {
        if (!initialized) {
            try {
                if (SMSNotificationManager.isCacheEnabled()) {
                    SMSNotificationManager.getInstance()
                        .registerCallbackHandler(new SMSEventListenerManager());
                }
                if (debug.messageEnabled()) {
                    debug.message("Initialized SMS Event listner");
                }
                initialized = true;
            } catch (Exception e) {
                debug.error("SMSEventListenerManager::initialize " +
                    "Unable to intialize SMS listener: " + e);
            }
        }
    }

    private SMSEventListenerManager() {
        // do nothing
    }

    // Processes object changed notifications
    @Override
    public void objectChanged(String odn, int event) {
        objectChanged(odn, event, false);
    }

    // Processes object changed notifications. The flag isLocal is used
    // distingush the self generated DELETE notifications for recursive
    // deletes, especially when data store notifications are disabled.
    // In which case, delete notifications will never be generated.
    private void objectChanged(String odn, int event, boolean isLocal) {
        if (debug.messageEnabled()) {
            debug.message("SMSEventListener::entry changed for: " + odn +
                " type: " + event);
        }

        // Normalize the DN
        DN sdn = DN.valueOf(odn);
        String dn = sdn.toString().toLowerCase();

        // If event is delete, need to send notifications for sub-entries
        // Even if backend datastore notification is enabled, they woould
        // arrive much later causing write-through cache issues.
        if (!isLocal && (event == SMSObjectListener.DELETE)) {
            // Collect the immediate children of the current sdn
            // from nodeChanges. All "subNodeChanges" entries would
            // have an entry in "nodeChanges", hence donot have to
            // iterate throught it
            Set<String> childDNs = new HashSet<>();
            synchronized (nodeChanges) {
                for (String cdn : nodeChanges.keySet()) {
                    if (DN.valueOf(cdn).isInScopeOf(sdn, SearchScope.SUBORDINATES)) {
                        childDNs.add(cdn);
                    }
                }
            }
            // Send the notifications
            if (debug.messageEnabled()) {
                debug.message("SMSEventListener::objectChanged: Sending " +
                    "delete event of: " + dn + " to child nodes: " + childDNs);
            }
            for (String item : childDNs) {
                objectChanged(item, event, true);
                // Send notifications to external listeners also if
                // data store notification is not enabled
                if (!SMSNotificationManager.isDataStoreNotificationEnabled()) {
                    SMSNotificationManager.getInstance().sendNotifications(
                            item, event, true);
                }
            }
        }

        // Send notifications to CachedSMSEntries
        sendNotifications(nodeChanges.get(dn), odn, event);
        
        // Process sub-entry changed events, not interested in attribute mods
        if ((event == SMSObjectListener.ADD) ||
            (event == SMSObjectListener.DELETE)) {
            // Send notifications to CachedSubEntries
            if (debug.messageEnabled()) {
                debug.message("SMSEventListener::entry changed for: " + dn +
                    " sending notifications to its parents");
            }
            sendNotifications(subNodeChanges.get(DN.valueOf(dn).parent().toString().toLowerCase()), odn, event);
        }
    }

    @Override
    public void allObjectsChanged() {
        if (debug.messageEnabled()) {
            debug.message("SMSEventListenerManager::allObjectsChanged called");
        }
        // Collect all the DNs from "nodeChanges" and send notifications
        Set<String> dns = new HashSet<>();
        synchronized (nodeChanges) {
            for (String item : nodeChanges.keySet()) {
                dns.add(item);
            }
        }
        // Send MODIFY notifications
        for (String item : dns) {
            objectChanged(item, SMSObjectListener.MODIFY);
        }
    }

    /**
     * Registers notification for changes to nodes
     */
    static String registerForNotifyChangesToNode(String dn, SMSEventListener eventListener) {
        initialize();
        String ndn = DN.valueOf(dn).toString().toLowerCase();
        return (addNotificationObject(nodeChanges, ndn, eventListener));
    }

    /**
     * Registers notification for changes to its sub-nodes
     */
    static String registerForNotifyChangesToSubNodes(String dn, SMSEventListener eventListener) {
        initialize();
        String ndn = DN.valueOf(dn).toString().toLowerCase();
        return (addNotificationObject(subNodeChanges, ndn, eventListener));
    }

    /**
     * Removes notification objects
     */
    static void removeNotification(String notificationID) {
        NotificationObject no = notificationObjects.get(notificationID);
        if (no != null) {
            no.set.remove(no);
            notificationObjects.remove(notificationID);
        }
    }

    /**
     * Adds notification method to the map
     */
    private static String addNotificationObject(Map<String, Set<NotificationObject>> nChangesMap, String dn,
                                                SMSEventListener eventListener) {
        Set<NotificationObject> nObjects;
        synchronized (nChangesMap) {
            nObjects = nChangesMap.get(dn);
            if (nObjects == null) {
                nObjects = Collections.synchronizedSet(new HashSet<NotificationObject>());
                nChangesMap.put(dn, nObjects);
            }
        }
        NotificationObject no = new NotificationObject(eventListener, nObjects);
        nObjects.add(no);
        notificationObjects.put(no.getID(), no);
        return (no.getID());
    }
    
    /**
     * Sends notification to methods and objects within the set
     */
    private static void sendNotifications(Set<NotificationObject> nObjects, String dn, int event) {
        if ((nObjects == null) || (nObjects.isEmpty())) {
            return;
        }
        Set<NotificationObject> nobjs = new HashSet<>(2);
        synchronized (nObjects) {
            nobjs.addAll(nObjects);
        }

        for (NotificationObject no : nobjs) {
            try {
                no.notifyEvent(dn, event);
            } catch (Exception e) {
                debug.error("SMSEvent notification: Unable to send notification: ", e);
            }
        }
    }

    private static class NotificationObject {

        String id;
        SMSEventListener eventListener;
        Set<NotificationObject> set;

        NotificationObject(SMSEventListener eventListener, Set<NotificationObject> s) {
            this.id = SMSUtils.getUniqueID();
            this.eventListener = eventListener;
            this.set = s;
        }

        String getID() {
            return (id);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 13 * hash + (id != null ? id.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof NotificationObject) {
                NotificationObject no = (NotificationObject) o;
                return (id.equals(no.id));
            }
            return (false);
        }

        private void notifyEvent(String dn, int event) {
            eventListener.notifySMSEvent(dn, event);
        }
    }
}
