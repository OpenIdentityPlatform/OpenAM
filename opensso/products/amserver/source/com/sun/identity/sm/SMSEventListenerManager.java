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
 * $Id: SMSEventListenerManager.java,v 1.12 2009/01/28 05:35:03 ww203982 Exp $
 *
 */
package com.sun.identity.sm;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.sso.SSOToken;
import java.util.HashSet;

/**
 * Receives notifications for all SMS object changes from
 * <class>SMSNotificationManager</class> and dispatches the same to <class>
 * CachedSMSEntry</class> and <class>CachedSubEntries</class>. This class
 * also handles the case of sending deleted event for recursive deletes.
 */
class SMSEventListenerManager implements SMSObjectListener {

    // All Notification Objects list
    protected static Map notificationObjects =
        Collections.synchronizedMap(new HashMap());
    
    // CachedSMSEntry objects
    protected static Map nodeChanges =
        Collections.synchronizedMap(new HashMap());
    
    // CachedSubEntries objects
    protected static Map subNodeChanges =
        Collections.synchronizedMap(new HashMap());
    
    // Static Initialization variables
    private static Debug debug = SMSEntry.eventDebug; 
    protected static boolean initialized;

    static void initialize(SSOToken token) {
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

    SMSEventListenerManager() {
        // do nothing
    }

    // Processes object changed notifications
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
        DN sdn = new DN(odn);
        String dn = sdn.toRFCString().toLowerCase();

        // If event is delete, need to send notifications for sub-entries
        // Even if backend datastore notification is enabled, they woould
        // arrive much later causing write-through cache issues.
        if (!isLocal && (event == SMSObjectListener.DELETE)) {
            // Collect the immediate children of the current sdn
            // from nodeChanges. All "subNodeChanges" entries would
            // have an entry in "nodeChanges", hence donot have to
            // iterate throught it
            Set childDNs = new HashSet();
            synchronized (nodeChanges) {
                Iterator keyitems = nodeChanges.keySet().iterator();
                while (keyitems.hasNext()) {
                    String cdn = (String) keyitems.next();
                    if ((new DN(cdn)).isDescendantOf(sdn)) {
                        childDNs.add(cdn);
                    }
                }
            }
            // Send the notifications
            if (debug.messageEnabled()) {
                debug.message("SMSEventListener::objectChanged: Sending " +
                    "delete event of: " + dn + " to child nodes: " + childDNs);
            }
            for (Iterator items = childDNs.iterator(); items.hasNext();) {
                String item = (String) items.next();
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
        sendNotifications((Set) nodeChanges.get(dn), odn, event);
        
        // Process sub-entry changed events, not interested in attribute mods
        if ((event == SMSObjectListener.ADD) ||
            (event == SMSObjectListener.DELETE)) {
            // Send notifications to CachedSubEntries
            if (debug.messageEnabled()) {
                debug.message("SMSEventListener::entry changed for: " + dn +
                    " sending notifications to its parents");
            }
            sendNotifications((Set) subNodeChanges.get((new DN(dn))
                .getParent().toRFCString().toLowerCase()), odn, event);
        }
    }

    public void allObjectsChanged() {
        if (debug.messageEnabled()) {
            debug.message("SMSEventListenerManager::allObjectsChanged called");
        }
        // Collect all the DNs from "nodeChanges" and send notifications
        Set dns = new HashSet();
        synchronized (nodeChanges) {
            for (Iterator items = nodeChanges.keySet().iterator();
                items.hasNext();) {
                dns.add(items.next());
            }
        }
        // Send MODIFY notifications
        for (Iterator items = dns.iterator(); items.hasNext();) {
            objectChanged((String) items.next(), SMSObjectListener.MODIFY);
        }
    }

    /**
     * Registers notification for changes to nodes
     */
    protected static String notifyChangesToNode(SSOToken token, String dn,
        Method method, Object object, Object[] args) {
        initialize(token);
        String ndn = (new DN(dn)).toRFCString().toLowerCase();
        return (addNotificationObject(nodeChanges, ndn, method, object, args));
    }

    /**
     * Registers notification for changes to its sub-nodes
     */
    protected static String notifyChangesToSubNodes(SSOToken token, String dn,
        Object o) {
        initialize(token);
        String ndn = (new DN(dn)).toRFCString().toLowerCase();
        return (addNotificationObject(subNodeChanges, ndn, null, o, null));
    }

    /**
     * Removes notification objects
     */
    protected static void removeNotification(String notificationID) {
        NotificationObject no = (NotificationObject)
            notificationObjects.get(notificationID);
        if (no != null) {
            no.set.remove(no);
            notificationObjects.remove(notificationID);
        }
    }

    /**
     * Adds notification method to the map
     */
    private static String addNotificationObject(Map nChangesMap, String dn,
        Method method, Object object, Object[] args) {
        Set nObjects = null;
        synchronized (nChangesMap) {
            nObjects = (Set) nChangesMap.get(dn);
            if (nObjects == null) {
                nObjects = Collections.synchronizedSet(new HashSet());
                nChangesMap.put(dn, nObjects);
            }
        }
        NotificationObject no = new NotificationObject(method, object, args,
            nObjects);
        nObjects.add(no);
        notificationObjects.put(no.getID(), no);
        return (no.getID());
    }
    
    /**
     * Sends notification to methods and objects within the set
     */
    private static void sendNotifications(Set nObjects, String dn, int event) {
        if ((nObjects == null) || (nObjects.isEmpty())) {
            return;
        }
        HashSet nobjs = new HashSet(2);
        synchronized (nObjects) {
            nobjs.addAll(nObjects);
        }
        Iterator items = nobjs.iterator();

        while (items.hasNext()) {
            try {
                NotificationObject no = (NotificationObject) items.next();
                if ((dn != null) && (no.object instanceof CachedSubEntries)) {
                    CachedSubEntries cse = (CachedSubEntries) no.object;
                    // We do not cache Realm names.
                    // We cache only service names and policy names.
                    if (!dn.startsWith(SMSEntry.ORG_PLACEHOLDER_RDN)) {
                        if (event == SMSObjectListener.ADD) {
                            cse.add(LDAPDN.explodeDN(dn, true)[0]);
                        } else {
                            cse.remove(LDAPDN.explodeDN(dn, true)[0]);
                        }
                    }
                } else {
                    no.method.invoke(no.object, no.args);
                }
            } catch (Exception e) {
                debug.error("SMSEvent notification: " +
                    "Unable to send notification: ", e);
            }
        }
    }

    private static class NotificationObject {

        String id;
        Method method;
        Object object;
        Object[] args;
        Set set;

        NotificationObject(Method m, Object o, Object[] a, Set s) {
            id = SMSUtils.getUniqueID();
            method = m;
            object = o;
            args = a;
            set = s;
        }

        String getID() {
            return (id);
        }

        // @Override
        public int hashCode() {
            int hash = 3;
            hash = 13 * hash + (id != null ? id.hashCode() : 0);
            return hash;
        }

        public boolean equals(Object o) {
            if (o instanceof NotificationObject) {
                NotificationObject no = (NotificationObject) o;
                return (id.equals(no.id));
            }
            return (false);
        }
    }
}
